package raven.clases;

import java.util.List;

public class PagedResult<T> {
    private List<T> resultList;
    private int totalCount;

    public PagedResult(List<T> resultList, int totalCount) {
        this.resultList = resultList;
        this.totalCount = totalCount;
    }

    public List<T> getResultList() {
        return resultList;
    }

    public void setResultList(List<T> resultList) {
        this.resultList = resultList;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }
}
