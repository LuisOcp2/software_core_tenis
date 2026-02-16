package raven.clases.comun;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import raven.controlador.principal.conexion;

public class GenericPaginationService {
    private static final int DEFAULT_PAGE_SIZE = 25;
    private static final int MAX_PAGE_SIZE = 100;
    private static final long CACHE_TTL_MS = 30_000L;

    public static class PagedResult<T> {
        private final List<T> data;
        private final long totalCount;
        private final int currentPage;
        private final int pageSize;
        private final boolean fromCache;
        private final Map<String, Object> metadata;

        public PagedResult(List<T> data, long totalCount, int currentPage, int pageSize, boolean fromCache) {
            this(data, totalCount, currentPage, pageSize, fromCache, new HashMap<>());
        }

        public PagedResult(List<T> data, long totalCount, int currentPage, int pageSize, boolean fromCache, Map<String, Object> metadata) {
            this.data = data;
            this.totalCount = totalCount;
            this.currentPage = currentPage;
            this.pageSize = pageSize;
            this.fromCache = fromCache;
            this.metadata = metadata != null ? metadata : new HashMap<>();
        }

        public List<T> getData() { return data; }
        public long getTotalCount() { return totalCount; }
        public int getCurrentPage() { return currentPage; }
        public int getPageSize() { return pageSize; }
        public boolean isFromCache() { return fromCache; }
        public Map<String, Object> getMetadata() { return metadata; }
        public int getTotalPages() { return (int) Math.ceil((double) totalCount / pageSize); }
        public boolean hasNextPage() { return currentPage < getTotalPages(); }
        public boolean hasPreviousPage() { return currentPage > 1; }
        public int getNextPage() { return hasNextPage() ? currentPage + 1 : currentPage; }
        public int getPreviousPage() { return hasPreviousPage() ? currentPage - 1 : currentPage; }
        public long getStartIndex() { return (long) (currentPage - 1) * pageSize + 1; }
        public long getEndIndex() { return Math.min(currentPage * pageSize, totalCount); }
        public String getPageInfo() { return String.format("Página %d de %d (%d-%d de %d elementos)", currentPage, getTotalPages(), getStartIndex(), getEndIndex(), totalCount); }
        @Override public String toString() { return String.format("PagedResult{page=%d/%d, size=%d, total=%d, cache=%s}", currentPage, getTotalPages(), pageSize, totalCount, fromCache); }
    }

    public static class QueryConfig {
        private final String baseQuery;
        private final String countQuery;
        private final String orderByClause;
        private final List<Object> parameters;
        private final List<Object> countParameters;
        private final BiFunction<ResultSet, Integer, Object> rowMapper;
        private final Map<String, Object> filterParams;
        public QueryConfig(String baseQuery, String countQuery, String orderByClause, List<Object> parameters, List<Object> countParameters, BiFunction<ResultSet, Integer, Object> rowMapper) {
            this.baseQuery = baseQuery; this.countQuery = countQuery; this.orderByClause = orderByClause; 
            this.parameters = parameters != null ? parameters : new ArrayList<>(); 
            this.countParameters = countParameters != null ? countParameters : new ArrayList<>();
            this.rowMapper = rowMapper; this.filterParams = new HashMap<>();
        }
        public static QueryConfigBuilder builder(String baseQuery, String countQuery, BiFunction<ResultSet, Integer, Object> rowMapper) { return new QueryConfigBuilder(baseQuery, countQuery, rowMapper); }
        public String getBaseQuery() { return baseQuery; }
        public String getCountQuery() { return countQuery; }
        public String getOrderByClause() { return orderByClause; }
        public List<Object> getParameters() { return parameters; }
        public List<Object> getCountParameters() { return countParameters.isEmpty() ? parameters : countParameters; }
        public BiFunction<ResultSet, Integer, Object> getRowMapper() { return rowMapper; }
        public Map<String, Object> getFilterParams() { return filterParams; }
    }

    public static class QueryConfigBuilder {
        private final String baseQuery; private final String countQuery; private final BiFunction<ResultSet, Integer, Object> rowMapper; private String orderByClause = "ORDER BY 1"; 
        private List<Object> parameters = new ArrayList<>(); 
        private List<Object> countParameters = new ArrayList<>();
        private Map<String, Object> filterParams = new HashMap<>();
        public QueryConfigBuilder(String baseQuery, String countQuery, BiFunction<ResultSet, Integer, Object> rowMapper) { this.baseQuery = baseQuery; this.countQuery = countQuery; this.rowMapper = rowMapper; }
        public QueryConfigBuilder orderBy(String orderByClause) { this.orderByClause = orderByClause; return this; }
        public QueryConfigBuilder withParam(Object param) { this.parameters.add(param); return this; }
        public QueryConfigBuilder withParams(Object... params) { Collections.addAll(this.parameters, params); return this; }
        public QueryConfigBuilder withCountParams(Object[] params) { Collections.addAll(this.countParameters, params); return this; }
        public QueryConfigBuilder withFilter(String key, Object value) { this.filterParams.put(key, value); return this; }
        public QueryConfig build() { return new QueryConfig(baseQuery, countQuery, orderByClause, parameters, countParameters, rowMapper); }
    }

    public interface CacheStrategy { String generateKey(String entityType, Map<String, Object> filters, int page, int pageSize); Object get(String key); void put(String key, Object data, long ttlMs); void invalidate(String key); void invalidatePattern(String pattern); }

    public static class MemoryCacheStrategy implements CacheStrategy {
        private static class CacheEntry { Object data; long timestamp; long ttl; CacheEntry(Object data, long ttl) { this.data = data; this.timestamp = System.currentTimeMillis(); this.ttl = ttl; } boolean isValid() { return (System.currentTimeMillis() - timestamp) < ttl; } }
        private final Map<String, CacheEntry> cache = new HashMap<>();
        private final int maxEntries;
        public MemoryCacheStrategy(int maxEntries) { this.maxEntries = maxEntries; }
        public String generateKey(String entityType, Map<String, Object> filters, int page, int pageSize) {
            String filtersStr = filters.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining(","));
            return String.format("%s:%s:page%d:size%d", entityType, filtersStr, page, pageSize);
        }
        public Object get(String key) { CacheEntry entry = cache.get(key); if (entry != null && entry.isValid()) return entry.data; if (entry != null) cache.remove(key); return null; }
        public void put(String key, Object data, long ttlMs) { if (cache.size() >= maxEntries) { cache.entrySet().removeIf(e -> !e.getValue().isValid()); if (cache.size() >= maxEntries) { cache.entrySet().stream().min(Map.Entry.comparingByValue(Comparator.comparingLong(ce -> ce.timestamp))).ifPresent(entry -> cache.remove(entry.getKey())); } } cache.put(key, new CacheEntry(data, ttlMs)); }
        public void invalidate(String key) { cache.remove(key); }
        public void invalidatePattern(String pattern) { cache.keySet().removeIf(key -> key.startsWith(pattern)); }
    }

    private final CacheStrategy cacheStrategy;
    public GenericPaginationService() { this(new MemoryCacheStrategy(50)); }
    public GenericPaginationService(CacheStrategy cacheStrategy) { this.cacheStrategy = cacheStrategy; }
    
    public CacheStrategy getCacheStrategy() { return cacheStrategy; }
    public void invalidateCache(String pattern) { cacheStrategy.invalidatePattern(pattern); }

    public <T> PagedResult<T> executePagedQuery(QueryConfig config, int page, int pageSize) throws SQLException {
        if (pageSize > MAX_PAGE_SIZE) pageSize = MAX_PAGE_SIZE; if (pageSize <= 0) pageSize = DEFAULT_PAGE_SIZE; if (page <= 0) page = 1;
        String cacheKey = cacheStrategy.generateKey("generic", config.getFilterParams(), page, pageSize);
        @SuppressWarnings("unchecked") PagedResult<T> cached = (PagedResult<T>) cacheStrategy.get(cacheKey);
        if (cached != null) { return cached; }
        try (Connection con = conexion.getInstance().createConnection()) {
            long totalCount = getTotalCount(con, config);
            int offset = (page - 1) * pageSize;
            String finalQuery = buildPagedQuery(config, pageSize, offset);
            List<T> data = executeQuery(con, finalQuery, config.getParameters(), (BiFunction<ResultSet, Integer, T>) config.getRowMapper());
            PagedResult<T> result = new PagedResult<>(data, totalCount, page, pageSize, false, config.getFilterParams());
            cacheStrategy.put(cacheKey, result, CACHE_TTL_MS);
            return result;
        }
    }

    public <T> PagedResult<T> paginate(String tableName, String selectFields, String whereClause, String orderBy, List<Object> params, BiFunction<ResultSet, Integer, T> rowMapper, int page, int pageSize) throws SQLException {
        QueryConfig config = QueryConfig.builder(buildSelectQuery(tableName, selectFields, whereClause, orderBy), buildCountQuery(tableName, whereClause), (rs, index) -> rowMapper.apply(rs, index)).withParams(params.toArray()).build();
        return executePagedQuery(config, page, pageSize);
    }

    private long getTotalCount(Connection con, QueryConfig config) throws SQLException {
        try (PreparedStatement stmt = con.prepareStatement(config.getCountQuery())) { setParameters(stmt, config.getCountParameters()); try (ResultSet rs = stmt.executeQuery()) { if (rs.next()) { return rs.getLong(1); } } }
        return 0;
    }
    private String buildPagedQuery(QueryConfig config, int pageSize, int offset) { return String.format("%s %s LIMIT %d OFFSET %d", config.getBaseQuery(), config.getOrderByClause(), pageSize, offset); }
    private <T> List<T> executeQuery(Connection con, String query, List<Object> params, BiFunction<ResultSet, Integer, T> rowMapper) throws SQLException {
        List<T> results = new ArrayList<>();
        try (PreparedStatement stmt = con.prepareStatement(query)) { setParameters(stmt, params); try (ResultSet rs = stmt.executeQuery()) { int index = 0; while (rs.next()) { T item = rowMapper.apply(rs, index++); if (item != null) results.add(item); } } }
        return results;
    }
    private void setParameters(PreparedStatement stmt, List<Object> params) throws SQLException { for (int i = 0; i < params.size(); i++) { stmt.setObject(i + 1, params.get(i)); } }
    private String buildSelectQuery(String tableName, String selectFields, String whereClause, String orderBy) { StringBuilder sql = new StringBuilder("SELECT ").append(selectFields).append(" FROM ").append(tableName); if (whereClause != null && !whereClause.trim().isEmpty()) { sql.append(" WHERE ").append(whereClause); } if (orderBy != null && !orderBy.trim().isEmpty()) { sql.append(" ").append(orderBy); } return sql.toString(); }
    private String buildCountQuery(String tableName, String whereClause) { StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM ").append(tableName); if (whereClause != null && !whereClause.trim().isEmpty()) { sql.append(" WHERE ").append(whereClause); } return sql.toString(); }

    public void clearCache() { cacheStrategy.invalidatePattern("generic:"); }
    public String getCacheStats() { return String.format("Cache Strategy: %s", cacheStrategy.getClass().getSimpleName()); }
}