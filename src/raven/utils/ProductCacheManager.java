package raven.utils;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import javax.swing.ImageIcon;
import raven.controlador.productos.ModelProduct;
import raven.controlador.productos.ModelProfile;
import raven.controlador.productos.ModelBrand;
import raven.controlador.productos.ModelCategory;
import raven.controlador.comercial.ModelSupplier;
import raven.controlador.principal.conexion;

public class ProductCacheManager {
    private static File baseDir() {
        return new File(System.getProperty("user.dir"), "cache");
    }
    private static File imgDir() {
        return new File(baseDir(), "img");
    }
    private static File productosFile(Integer idBodega) {
        return new File(baseDir(), idBodega != null ? ("productos_" + idBodega + ".jsonl") : "productos.jsonl");
    }
    private static File variantesFile(Integer idBodega) {
        return new File(baseDir(), idBodega != null ? ("variantes_" + idBodega + ".jsonl") : "variantes.jsonl");
    }

    public static void actualizarCacheAsync() {
        new Thread(() -> {
            Integer idBodega = null;
            try { idBodega = raven.clases.admin.UserSession.getInstance().getIdBodegaUsuario(); } catch (Throwable ignore) {}
            if (idBodega == null || idBodega <= 0) {
                try { idBodega = raven.controlador.admin.SessionManager.getInstance().getCurrentUserBodegaId(); } catch (Throwable ignore) {}
            }
            try {
                if (idBodega != null && idBodega > 0) {
                    exportarProductosPorBodega(idBodega);
                    exportarVariantesPorBodega(idBodega);
                    try { escribirFirma(idBodega, computeSignature(idBodega)); } catch (Exception ignore) {}
                } else {
                    exportarProductosDesdeDB();
                    exportarVariantesDesdeDB();
                    try { escribirFirma(null, computeSignature(null)); } catch (Exception ignore) {}
                }
            } catch (Exception ignore) {}
        }, "CacheProductosWorker").start();
    }

    public static void ensureFullCacheNow() {
        Integer idBodega = null;
        try { idBodega = raven.clases.admin.UserSession.getInstance().getIdBodegaUsuario(); } catch (Throwable ignore) {}
        if (idBodega == null || idBodega <= 0) {
            try { idBodega = raven.controlador.admin.SessionManager.getInstance().getCurrentUserBodegaId(); } catch (Throwable ignore) {}
        }
        ensureFullCache(idBodega);
    }

    public static void ensureCacheForBodega(Integer idBodega) {
        if (idBodega == null || idBodega <= 0) return;
        File pf = productosFile(idBodega);
        File vf = variantesFile(idBodega);
        boolean need = !pf.exists() || !vf.exists() || pf.length() == 0 || vf.length() == 0;
        if (need) {
            try {
                exportarProductosPorBodega(idBodega);
                exportarVariantesPorBodega(idBodega);
                escribirFirma(idBodega, computeSignature(idBodega));
            } catch (Exception ignore) {}
        }
    }

    public static int exportarProductosDesdeDB() throws Exception {
        File bd = baseDir();
        File id = imgDir();
        if (!bd.exists()) bd.mkdirs();
        if (!id.exists()) id.mkdirs();
        File jf = productosFile(null);
        if (jf.exists()) jf.delete();
        Connection con = conexion.getInstance().createConnection();
        String q =
                "SELECT p.id_producto, p.codigo_modelo, p.nombre, p.descripcion, p.id_categoria, p.id_marca, p.id_proveedor, p.precio_compra, p.precio_venta, p.stock_minimo, p.talla, p.color, p.genero, p.activo, p.ubicacion, p.fecha_creacion, p.fecha_actualizacion, p.pares_por_caja, p.ubicacion_bodega, p.ubicacion_tienda, " +
                "m.nombre AS marca_nombre, c.nombre AS categoria_nombre, pr.nombre AS proveedor_nombre, " +
                "GROUP_CONCAT(DISTINCT co.nombre ORDER BY co.nombre SEPARATOR ', ') AS colores, " +
                "GROUP_CONCAT(DISTINCT CONCAT(t.numero, ' ', COALESCE(t.sistema,'')) ORDER BY t.numero SEPARATOR ', ') AS tallas, " +
                "COALESCE(SUM(ib.Stock_par),0) AS stock_pares, COALESCE(SUM(ib.Stock_caja),0) AS stock_cajas, MIN(pv.imagen) AS imagen " +
                "FROM productos p " +
                "LEFT JOIN marcas m ON p.id_marca=m.id_marca " +
                "LEFT JOIN categorias c ON p.id_categoria=c.id_categoria " +
                "LEFT JOIN proveedores pr ON p.id_proveedor=pr.id_proveedor " +
                "LEFT JOIN producto_variantes pv ON pv.id_producto=p.id_producto " +
                "LEFT JOIN inventario_bodega ib ON ib.id_variante = pv.id_variante AND ib.activo=1 " +
                "LEFT JOIN colores co ON pv.id_color=co.id_color " +
                "LEFT JOIN tallas t ON pv.id_talla=t.id_talla " +
                "WHERE p.activo=1 GROUP BY p.id_producto ORDER BY p.id_producto";
        PreparedStatement ps = con.prepareStatement(q);
        try { ps.setFetchSize(500); } catch (Exception ignore) {}
        ResultSet rs = ps.executeQuery();
        int count = 0;
        StringBuilder sb = new StringBuilder();
        while (rs.next()) {
            int idp = rs.getInt("id_producto");
            String imgName = "prod_" + idp + ".png";
            byte[] img = rs.getBytes("imagen");
            File out = new File(imgDir(), imgName);
            if (img != null && img.length > 0) { try (OutputStream os = new BufferedOutputStream(new FileOutputStream(out))) { os.write(img); } }

            sb.append('{')
              .append("\"id_producto\":").append(idp).append(',')
              .append("\"codigo_modelo\":\"").append(escape(rs.getString("codigo_modelo"))).append("\",")
              .append("\"nombre\":\"").append(escape(rs.getString("nombre"))).append("\",")
              .append("\"descripcion\":\"").append(escape(rs.getString("descripcion"))).append("\",")
              .append("\"id_categoria\":").append(rs.getInt("id_categoria")).append(',')
              .append("\"id_marca\":").append(rs.getInt("id_marca")).append(',')
              .append("\"id_proveedor\":").append(rs.getInt("id_proveedor")).append(',')
              .append("\"precio_compra\":").append(rs.getBigDecimal("precio_compra")).append(',')
              .append("\"precio_venta\":").append(rs.getBigDecimal("precio_venta")).append(',')
              .append("\"stock_minimo\":").append(rs.getInt("stock_minimo")).append(',')
              .append("\"talla\":\"").append(escape(rs.getString("talla"))).append("\",")
              .append("\"color\":\"").append(escape(rs.getString("color"))).append("\",")
              .append("\"genero\":\"").append(escape(rs.getString("genero"))).append("\",")
              .append("\"activo\":").append(rs.getInt("activo")).append(',')
              .append("\"ubicacion\":\"").append(escape(rs.getString("ubicacion"))).append("\",")
              .append("\"fecha_creacion\":\"").append(escape(String.valueOf(rs.getTimestamp("fecha_creacion")))).append("\",")
              .append("\"fecha_actualizacion\":\"").append(escape(String.valueOf(rs.getTimestamp("fecha_actualizacion")))).append("\",")
              .append("\"pares_por_caja\":").append(rs.getInt("pares_por_caja")).append(',')
              .append("\"ubicacion_bodega\":\"").append(escape(rs.getString("ubicacion_bodega"))).append("\",")
              .append("\"ubicacion_tienda\":\"").append(escape(rs.getString("ubicacion_tienda"))).append("\",")
              .append("\"marca\":\"").append(escape(rs.getString("marca_nombre"))).append("\",")
              .append("\"categoria\":\"").append(escape(rs.getString("categoria_nombre"))).append("\",")
              .append("\"proveedor\":\"").append(escape(rs.getString("proveedor_nombre"))).append("\",")
              .append("\"colores\":\"").append(escape(rs.getString("colores"))).append("\",")
              .append("\"tallas\":\"").append(escape(rs.getString("tallas"))).append("\",")
              .append("\"stock_pares\":").append(rs.getInt("stock_pares")).append(',')
              .append("\"stock_cajas\":").append(rs.getInt("stock_cajas")).append(',')
              .append("\"imagen\":\"").append(escape(out.getAbsolutePath())).append("\"}")
              .append('\n');
            count++;
        }
        rs.close();
        ps.close();
        con.close();
        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(jf))) {
            os.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        }
        return count;
    }

    public static int exportarVariantesDesdeDB() throws Exception {
        File bd = baseDir();
        File id = imgDir();
        if (!bd.exists()) bd.mkdirs();
        if (!id.exists()) id.mkdirs();
        File jf = variantesFile(null);
        if (jf.exists()) jf.delete();
        Connection con = conexion.getInstance().createConnection();
        String q = "SELECT pv.id_variante, pv.id_producto, ib.id_bodega, pv.id_talla, pv.id_color, pv.sku, pv.ean, pv.precio_venta, pv.precio_compra, pv.stock_minimo_variante, ib.Stock_par AS stock_por_pares, ib.Stock_caja AS stock_por_cajas, pv.disponible, pv.fecha_creacion, pv.fecha_actualizacion, pv.imagen, c.nombre AS color_nombre, CONCAT(t.numero, ' ', COALESCE(t.sistema,'')) AS talla_nombre FROM producto_variantes pv LEFT JOIN colores c ON pv.id_color=c.id_color LEFT JOIN tallas t ON pv.id_talla=t.id_talla LEFT JOIN inventario_bodega ib ON ib.id_variante=pv.id_variante AND ib.activo=1";
        PreparedStatement ps = con.prepareStatement(q);
        ResultSet rs = ps.executeQuery();
        int count = 0;
        StringBuilder sb = new StringBuilder();
        while (rs.next()) {
            int idv = rs.getInt("id_variante");
            int idp = rs.getInt("id_producto");
            int idb = rs.getInt("id_bodega");
            int idt = rs.getInt("id_talla");
            int idc = rs.getInt("id_color");
            String codigoBarras = "";
            String sku = rs.getString("sku");
            String ean = rs.getString("ean");
            java.math.BigDecimal precioVenta = rs.getBigDecimal("precio_venta");
            java.math.BigDecimal precioCompra = rs.getBigDecimal("precio_compra");
            int stockMinVar = rs.getInt("stock_minimo_variante");
            int stockPares = rs.getInt("stock_por_pares");
            int stockCajas = rs.getInt("stock_por_cajas");
            int disponible = rs.getInt("disponible");
            String fcrea = String.valueOf(rs.getTimestamp("fecha_creacion"));
            String factual = String.valueOf(rs.getTimestamp("fecha_actualizacion"));
            String colorNombre = rs.getString("color_nombre");
            String tallaNombre = rs.getString("talla_nombre");
            byte[] img = rs.getBytes("imagen");
            String imgName = "var_" + idv + ".png";
            File out = new File(imgDir(), imgName);
            if (img != null && img.length > 0) { try (OutputStream os = new BufferedOutputStream(new FileOutputStream(out))) { os.write(img); } }
            sb.append('{')
              .append("\"id_variante\":").append(idv).append(',')
              .append("\"id_producto\":").append(idp).append(',')
              .append("\"id_bodega\":").append(idb).append(',')
              .append("\"id_talla\":").append(idt).append(',')
              .append("\"talla\":\"").append(escape(tallaNombre)).append("\",")
              .append("\"id_color\":").append(idc).append(',')
              .append("\"color\":\"").append(escape(colorNombre)).append("\",")
              .append("\"codigo_barras\":\"").append(escape(codigoBarras)).append("\",")
              .append("\"sku\":\"").append(escape(sku)).append("\",")
              .append("\"ean\":\"").append(escape(ean)).append("\",")
              .append("\"precio_venta\":").append(precioVenta != null ? precioVenta.toPlainString() : "0").append(',')
              .append("\"precio_compra\":").append(precioCompra != null ? precioCompra.toPlainString() : "0").append(',')
              .append("\"stock_minimo_variante\":").append(stockMinVar).append(',')
              .append("\"stock_par\":").append(stockPares).append(',')
              .append("\"stock_caja\":").append(stockCajas).append(',')
              .append("\"disponible\":").append(disponible).append(',')
              .append("\"fecha_creacion\":\"").append(escape(fcrea)).append("\",")
              .append("\"fecha_actualizacion\":\"").append(escape(factual)).append("\",")
              .append("\"imagen\":\"").append(escape(out.getAbsolutePath())).append("\"}")
              .append('\n');
            count++;
        }
        rs.close();
        ps.close();
        con.close();
        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(jf))) { os.write(sb.toString().getBytes(StandardCharsets.UTF_8)); }
        return count;
    }

    public static List<ModelProduct> cargarProductosDesdeCache() {
        Integer idBodega = null;
        try { idBodega = raven.clases.admin.UserSession.getInstance().getIdBodegaUsuario(); } catch (Throwable ignore) {}
        if (idBodega == null || idBodega <= 0) {
            try { idBodega = raven.controlador.admin.SessionManager.getInstance().getCurrentUserBodegaId(); } catch (Throwable ignore) {}
        }
        return cargarProductosDesdeCache(idBodega);
    }

    public static List<ModelProduct> cargarProductosDesdeCache(Integer idBodega) {
        List<ModelProduct> list = new ArrayList<>();
        ensureFullCache(idBodega);
        File jf = productosFile(idBodega);
        if (!jf.exists()) return list;
        Map<Integer, List<raven.controlador.productos.ModelProductVariant>> variantsByProduct = cargarVariantesDesdeCacheInternal(idBodega);
        try (BufferedReader br = new BufferedReader(new FileReader(jf, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isEmpty()) continue;
                int idp = parseInt(line, "\"id_producto\":", ',');
                String nombre = parseString(line, "\"nombre\":\"", "\"");
                String genero = parseString(line, "\"genero\":\"", "\"");
                String modelo = parseString(line, "\"codigo_modelo\":\"", "\"");
                String marca = parseString(line, "\"marca\":\"", "\"");
                String colores = parseString(line, "\"colores\":\"", "\"");
                String tallas = parseString(line, "\"tallas\":\"", "\"");
                int stockPares = parseInt(line, "\"stock_pares\":", ',');
                int stockCajas = parseInt(line, "\"stock_cajas\":", ',');
                String descripcion = parseString(line, "\"descripcion\":\"", "\"");
                int idCategoria = parseInt(line, "\"id_categoria\":", ',');
                int idMarca = parseInt(line, "\"id_marca\":", ',');
                int idProveedor = parseInt(line, "\"id_proveedor\":", ',');
                double precioCompra = parseDouble(line, "\"precio_compra\":", ',');
                double precioVenta = parseDouble(line, "\"precio_venta\":", ',');
                int stockMinimo = parseInt(line, "\"stock_minimo\":", ',');
                int paresPorCaja = parseInt(line, "\"pares_por_caja\":", ',');
                String ubicacion = parseString(line, "\"ubicacion\":\"", "\"");
                String categoriaNombre = parseString(line, "\"categoria\":\"", "\"");
                String proveedorNombre = parseString(line, "\"proveedor\":\"", "\"");
                String imagen = parseString(line, "\"imagen\":\"", "\"}");
                ModelProfile mp = new ModelProfile();
                try {
                    if (imagen != null && !imagen.isEmpty()) {
                        ImageIcon ic = new ImageIcon(imagen);
                        mp.setIcon(ic);
                        try {
                            java.nio.file.Path pth = java.nio.file.Paths.get(imagen);
                            if (java.nio.file.Files.exists(pth)) {
                                mp.setImageBytes(java.nio.file.Files.readAllBytes(pth));
                            }
                        } catch (Exception ignore2) {}
                    }
                } catch (Exception ignore) {}
                ModelProduct p = new ModelProduct();
                p.setProductId(idp);
                p.setName(nombre);
                p.setGender(genero);
                p.setModelCode(modelo);
                ModelBrand b = new ModelBrand(); b.setBrandId(idMarca); b.setName(marca); p.setBrand(b);
                ModelCategory cat = new ModelCategory(); cat.setCategoryId(idCategoria); cat.setName(categoriaNombre); p.setCategory(cat);
                ModelSupplier sup = new ModelSupplier(); sup.setSupplierId(idProveedor); sup.setName(proveedorNombre); p.setSupplier(sup);
                p.setDescription(descripcion);
                p.setPurchasePrice(precioCompra);
                p.setSalePrice(precioVenta);
                p.setMinStock(stockMinimo);
                p.setColor(colores);
                p.setSize(tallas);
                p.setPairsStock(stockPares);
                p.setBoxesStock(stockCajas);
                p.setPairsPerBox(paresPorCaja);
                p.setProfile(mp);
                p.setUbicacion(ubicacion);
                List<raven.controlador.productos.ModelProductVariant> vlist = variantsByProduct.get(idp);
                if (vlist != null && !vlist.isEmpty()) { p.setVariants(vlist); }
                list.add(p);
            }
        } catch (Exception ignore) {}
        System.out.println("[CACHE] Productos leidos: " + list.size() + ", productos con variantes: " + variantsByProduct.size());
        return list;
    }
    private static double parseDouble(String line, String key, char end) {
        try {
            int i = line.indexOf(key);
            if (i < 0) return 0.0;
            int j = line.indexOf(end, i + key.length());
            String v = j > i ? line.substring(i + key.length(), j) : line.substring(i + key.length());
            v = v.trim();
            if (v.isEmpty()) return 0.0;
            return Double.parseDouble(v);
        } catch (Exception e) { return 0.0; }
    }

    public static List<ModelProduct> cargarProductosDesdeCachePorBodega(Integer idBodega) {
        return cargarProductosDesdeCache(idBodega);
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
    private static int parseInt(String line, String key, char end) {
        try {
            int i = line.indexOf(key);
            if (i < 0) return 0;
            int j = line.indexOf(end, i + key.length());
            String v = j > i ? line.substring(i + key.length(), j) : line.substring(i + key.length());
            return Integer.parseInt(v.trim());
        } catch (Exception e) { return 0; }
    }
    private static String parseString(String line, String start, String end) {
        try {
            int i = line.indexOf(start);
            if (i < 0) return "";
            int j = line.indexOf(end, i + start.length());
            if (j < 0) j = line.length();
            return line.substring(i + start.length(), j);
        } catch (Exception e) { return ""; }
    }

    private static Map<Integer, List<raven.controlador.productos.ModelProductVariant>> cargarVariantesDesdeCacheInternal(Integer idBodega) {
        Map<Integer, List<raven.controlador.productos.ModelProductVariant>> map = new HashMap<>();
        File vf = variantesFile(idBodega);
        if (!vf.exists()) return map;
        try (BufferedReader br = new BufferedReader(new FileReader(vf, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isEmpty()) continue;
                int idp = parseInt(line, "\"id_producto\":", ',');
                int idv = parseInt(line, "\"id_variante\":", ',');
                int idb = parseInt(line, "\"id_bodega\":", ',');
                int idt = parseInt(line, "\"id_talla\":", ',');
                String tallaNombre = parseString(line, "\"talla\":\"", "\"");
                int idc = parseInt(line, "\"id_color\":", ',');
                String colorNombre = parseString(line, "\"color\":\"", "\"");
                String codigoBarras = parseString(line, "\"codigo_barras\":\"", "\"");
                String sku = parseString(line, "\"sku\":\"", "\"");
                String ean = parseString(line, "\"ean\":\"", "\"");
                double precioVenta = parseDouble(line, "\"precio_venta\":", ',');
                double precioCompra = parseDouble(line, "\"precio_compra\":", ',');
                int stockMinVar = parseInt(line, "\"stock_minimo_variante\":", ',');
                int stockPar = parseInt(line, "\"stock_par\":", ',');
                int stockCaja = parseInt(line, "\"stock_caja\":", ',');
                int disponible = parseInt(line, "\"disponible\":", ',');
                raven.controlador.productos.ModelProductVariant v = new raven.controlador.productos.ModelProductVariant();
                v.setVariantId(idv);
                v.setProductId(idp);
                v.setWarehouseId(idb);
                v.setSizeId(idt);
                v.setSizeName(tallaNombre);
                v.setColorId(idc);
                v.setColorName(colorNombre);
                v.setBarcode(codigoBarras);
                v.setSku(sku);
                v.setEan(ean);
                v.setSalePrice(precioVenta);
                v.setPurchasePrice(precioCompra);
                v.setMinStock(stockMinVar);
                v.setStockPairs(stockPar);
                v.setStockBoxes(stockCaja);
                v.setAvailable(disponible == 1);
                List<raven.controlador.productos.ModelProductVariant> list = map.computeIfAbsent(idp, k -> new ArrayList<>());
                list.add(v);
            }
        } catch (Exception ignore) {}
        return map;
    }

    private static File sigFile(Integer idBodega) { return new File(baseDir(), idBodega != null ? ("sig_" + idBodega + ".txt") : "sig.txt"); }
    private static void escribirFirma(Integer idBodega, String sig) throws Exception {
        File f = sigFile(idBodega);
        if (!baseDir().exists()) baseDir().mkdirs();
        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(f))) {
            os.write(sig.getBytes(StandardCharsets.UTF_8));
        }
    }
    public static String leerFirma() { return leerFirma(null); }
    public static String leerFirma(Integer idBodega) {
        File f = sigFile(idBodega);
        if (!f.exists()) return null;
        try (BufferedReader br = new BufferedReader(new FileReader(f, StandardCharsets.UTF_8))) {
            return br.readLine();
        } catch (Exception e) { return null; }
    }
    public static String computeSignature() throws Exception { return computeSignature(null); }
    public static String computeSignature(Integer idBodega) throws Exception {
        Connection con = conexion.getInstance().createConnection();
        String p = "SELECT COUNT(*) c, MAX(COALESCE(fecha_actualizacion, fecha_creacion)) m FROM productos";
        String v;
        if (idBodega != null && idBodega > 0) {
            v = "SELECT COUNT(*) c, MAX(COALESCE(fecha_actualizacion, fecha_creacion)) m FROM producto_variantes WHERE id_bodega = " + idBodega;
        } else {
            v = "SELECT COUNT(*) c, MAX(COALESCE(fecha_actualizacion, fecha_creacion)) m FROM producto_variantes";
        }
        PreparedStatement ps1 = con.prepareStatement(p);
        PreparedStatement ps2 = con.prepareStatement(v);
        ResultSet r1 = ps1.executeQuery();
        ResultSet r2 = ps2.executeQuery();
        String s1 = "0|0";
        String s2 = "0|0";
        if (r1.next()) s1 = r1.getInt(1) + "|" + String.valueOf(r1.getTimestamp(2));
        if (r2.next()) s2 = r2.getInt(1) + "|" + String.valueOf(r2.getTimestamp(2));
        r1.close(); r2.close(); ps1.close(); ps2.close(); con.close();
        return s1 + "#" + s2;
    }
    public static boolean refreshCacheIfChanged() {
        Integer idBodega = null;
        try { idBodega = raven.clases.admin.UserSession.getInstance().getIdBodegaUsuario(); } catch (Throwable ignore) {}
        if (idBodega == null || idBodega <= 0) {
            try { idBodega = raven.controlador.admin.SessionManager.getInstance().getCurrentUserBodegaId(); } catch (Throwable ignore) {}
        }
        return refreshCacheIfChanged(idBodega);
    }
    public static boolean refreshCacheIfChanged(Integer idBodega) {
        try {
            String current = computeSignature(idBodega);
            String stored = leerFirma(idBodega);
            if (stored != null && stored.equals(current)) {
                File pf = productosFile(idBodega);
                boolean full = cacheTieneCamposCompletos(pf);
                if (full) return false;
            }
            if (idBodega != null && idBodega > 0) {
                exportarProductosPorBodega(idBodega);
                exportarVariantesPorBodega(idBodega);
                escribirFirma(idBodega, current);
            } else {
                exportarProductosDesdeDB();
                exportarVariantesDesdeDB();
                escribirFirma(null, current);
            }
            return true;
        } catch (Exception e) { return false; }
    }

    public static boolean refreshCacheIfChangedPorBodega(Integer idBodega) {
        return refreshCacheIfChanged(idBodega);
    }

    private static boolean cacheTieneCamposCompletos(File jf) {
        if (jf == null || !jf.exists()) return false;
        try (BufferedReader br = new BufferedReader(new FileReader(jf, StandardCharsets.UTF_8))) {
            String line = br.readLine();
            if (line == null) return false;
            return line.contains("\"codigo_modelo\"") && line.contains("\"stock_pares\"") && line.contains("\"tallas\"") && line.contains("\"colores\"");
        } catch (Exception e) { return false; }
    }

    private static void ensureFullCache(Integer idBodega) {
        try {
            File pf = productosFile(idBodega);
            boolean full = cacheTieneCamposCompletos(pf);
            File vf = variantesFile(idBodega);
            boolean vfull = variantesTieneCamposCompletos(vf);
            if (!full || !vfull) {
                if (idBodega != null && idBodega > 0) {
                    exportarProductosPorBodega(idBodega);
                    exportarVariantesPorBodega(idBodega);
                    try { escribirFirma(idBodega, computeSignature(idBodega)); } catch (Exception ignore) {}
                } else {
                    exportarProductosDesdeDB();
                    exportarVariantesDesdeDB();
                    try { escribirFirma(null, computeSignature(null)); } catch (Exception ignore) {}
                }
            }
        } catch (Exception ignore) {}
    }

    private static boolean variantesTieneCamposCompletos(File vf) {
        if (vf == null || !vf.exists()) return false;
        try (BufferedReader br = new BufferedReader(new FileReader(vf, StandardCharsets.UTF_8))) {
            String line = br.readLine();
            if (line == null) return false;
            return line.contains("\"id_talla\"") && line.contains("\"id_color\"") && (line.contains("\"stock_par\"") || line.contains("\"stock_por_pares\""));
        } catch (Exception e) { return false; }
    }

    public static int exportarProductosPorBodega(int idBodega) throws Exception {
    File bd = baseDir();
    File id = imgDir();
    if (!bd.exists()) bd.mkdirs();
    if (!id.exists()) id.mkdirs();

    File jf = productosFile(idBodega);
    if (jf.exists()) jf.delete();

    String q =
            "SELECT p.*, m.nombre AS marca_nombre, c.nombre AS categoria_nombre, pr.nombre AS proveedor_nombre, " +
            "(SELECT GROUP_CONCAT(DISTINCT co.nombre ORDER BY co.nombre SEPARATOR ', ') " +
            "   FROM producto_variantes pv " +
            "   LEFT JOIN inventario_bodega ib ON ib.id_variante=pv.id_variante AND ib.activo=1 " +
            "   LEFT JOIN colores co ON pv.id_color=co.id_color " +
            "   WHERE pv.id_producto=p.id_producto AND ib.id_bodega=?) AS colores, " +
            "(SELECT GROUP_CONCAT(DISTINCT ta.numero ORDER BY ta.numero SEPARATOR ', ') " +
            "   FROM producto_variantes pv " +
            "   LEFT JOIN inventario_bodega ib ON ib.id_variante=pv.id_variante AND ib.activo=1 " +
            "   LEFT JOIN tallas ta ON pv.id_talla=ta.id_talla " +
            "   WHERE pv.id_producto=p.id_producto AND ib.id_bodega=?) AS tallas, " +
            "(SELECT COALESCE(SUM(ib.Stock_par),0) " +
            "   FROM inventario_bodega ib JOIN producto_variantes pv ON ib.id_variante=pv.id_variante " +
            "   WHERE pv.id_producto=p.id_producto AND ib.id_bodega=?) AS stock_pares, " +
            "(SELECT COALESCE(SUM(ib.Stock_caja),0) " +
            "   FROM inventario_bodega ib JOIN producto_variantes pv ON ib.id_variante=pv.id_variante " +
            "   WHERE pv.id_producto=p.id_producto AND ib.id_bodega=?) AS stock_cajas, " +
            "(SELECT pv.imagen FROM producto_variantes pv " +
            "   JOIN inventario_bodega ib ON ib.id_variante=pv.id_variante AND ib.id_bodega=? AND ib.activo=1 " +
            "   WHERE pv.id_producto=p.id_producto AND pv.imagen IS NOT NULL LIMIT 1) AS imagen " +
            "FROM productos p " +
            "LEFT JOIN marcas m ON p.id_marca=m.id_marca " +
            "LEFT JOIN categorias c ON p.id_categoria=c.id_categoria " +
            "LEFT JOIN proveedores pr ON p.id_proveedor=pr.id_proveedor " +
            "WHERE p.activo=1 ORDER BY p.id_producto";

    int count = 0;

    try (Connection con = conexion.getInstance().createConnection();
         PreparedStatement ps = con.prepareStatement(q);
         OutputStream os = new BufferedOutputStream(new FileOutputStream(jf))) {

        ps.setInt(1, idBodega);
        ps.setInt(2, idBodega);
        ps.setInt(3, idBodega);
        ps.setInt(4, idBodega);
        ps.setInt(5, idBodega);

        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int idp = rs.getInt("id_producto");

                // imagen
                byte[] img = rs.getBytes("imagen");
                String imgName = "prod_" + idp + "_" + idBodega + ".png";
                File out = new File(imgDir(), imgName);
                if (img != null && img.length > 0) {
                    try (OutputStream ios = new BufferedOutputStream(new FileOutputStream(out))) {
                        ios.write(img);
                    }
                }

                String line =
                        "{" +
                        "\"id_producto\":" + idp + "," +
                        "\"codigo_modelo\":\"" + escape(rs.getString("codigo_modelo")) + "\"," +
                        "\"nombre\":\"" + escape(rs.getString("nombre")) + "\"," +
                        "\"descripcion\":\"" + escape(rs.getString("descripcion")) + "\"," +
                        "\"id_categoria\":" + rs.getInt("id_categoria") + "," +
                        "\"id_marca\":" + rs.getInt("id_marca") + "," +
                        "\"id_proveedor\":" + rs.getInt("id_proveedor") + "," +
                        "\"precio_compra\":" + rs.getBigDecimal("precio_compra") + "," +
                        "\"precio_venta\":" + rs.getBigDecimal("precio_venta") + "," +
                        "\"stock_minimo\":" + rs.getInt("stock_minimo") + "," +
                        "\"talla\":\"" + escape(rs.getString("talla")) + "\"," +
                        "\"color\":\"" + escape(rs.getString("color")) + "\"," +
                        "\"genero\":\"" + escape(rs.getString("genero")) + "\"," +
                        "\"activo\":" + rs.getInt("activo") + "," +
                        "\"ubicacion\":\"" + escape(rs.getString("ubicacion")) + "\"," +
                        "\"fecha_creacion\":\"" + escape(String.valueOf(rs.getTimestamp("fecha_creacion"))) + "\"," +
                        "\"fecha_actualizacion\":\"" + escape(String.valueOf(rs.getTimestamp("fecha_actualizacion"))) + "\"," +
                        "\"pares_por_caja\":" + rs.getInt("pares_por_caja") + "," +
                        "\"ubicacion_bodega\":\"" + escape(rs.getString("ubicacion_bodega")) + "\"," +
                        "\"ubicacion_tienda\":\"" + escape(rs.getString("ubicacion_tienda")) + "\"," +
                        "\"marca\":\"" + escape(rs.getString("marca_nombre")) + "\"," +
                        "\"categoria\":\"" + escape(rs.getString("categoria_nombre")) + "\"," +
                        "\"proveedor\":\"" + escape(rs.getString("proveedor_nombre")) + "\"," +
                        "\"colores\":\"" + escape(rs.getString("colores")) + "\"," +
                        "\"tallas\":\"" + escape(rs.getString("tallas")) + "\"," +
                        "\"stock_pares\":" + rs.getInt("stock_pares") + "," +
                        "\"stock_cajas\":" + rs.getInt("stock_cajas") + "," +
                        "\"imagen\":\"" + escape(out.getAbsolutePath()) + "\"}\n";

                os.write(line.getBytes(StandardCharsets.UTF_8));
                count++;
            }
        }
    }

    System.out.println("[CACHE] Productos exportados para bodega " + idBodega + ": " + count);
    return count;
}


    public static int exportarVariantesPorBodega(int idBodega) throws Exception {
        File jf = variantesFile(idBodega); if (jf.exists()) jf.delete();
        Connection con = conexion.getInstance().createConnection();
    String q = "SELECT pv.id_variante, pv.id_producto, ib.id_bodega, pv.id_talla, pv.id_color, pv.sku, pv.ean, pv.precio_venta, pv.precio_compra, pv.stock_minimo_variante, ib.Stock_par AS stock_por_pares, ib.Stock_caja AS stock_por_cajas, pv.disponible, pv.fecha_creacion, pv.fecha_actualizacion, pv.imagen, c.nombre AS color_nombre, CONCAT(t.numero, ' ', COALESCE(t.sistema,'')) AS talla_nombre FROM producto_variantes pv LEFT JOIN colores c ON pv.id_color=c.id_color LEFT JOIN tallas t ON pv.id_talla=t.id_talla INNER JOIN inventario_bodega ib ON ib.id_variante=pv.id_variante WHERE ib.id_bodega = ? AND ib.activo=1";
        PreparedStatement ps = con.prepareStatement(q); ps.setInt(1, idBodega);
        try { ps.setFetchSize(500); } catch (Exception ignore) {}
        ResultSet rs = ps.executeQuery(); StringBuilder sb = new StringBuilder(); int count = 0;
        while (rs.next()) {
            int idv = rs.getInt("id_variante"); int idp = rs.getInt("id_producto"); int idb = rs.getInt("id_bodega"); int idt = rs.getInt("id_talla"); int idc = rs.getInt("id_color");
            String codigoBarras = ""; String sku = rs.getString("sku"); String ean = rs.getString("ean");
            java.math.BigDecimal precioVenta = rs.getBigDecimal("precio_venta"); java.math.BigDecimal precioCompra = rs.getBigDecimal("precio_compra");
            int stockMinVar = rs.getInt("stock_minimo_variante");
            int stockPares = rs.getInt("stock_por_pares"); int stockCajas = rs.getInt("stock_por_cajas"); int disponible = rs.getInt("disponible");
            String fcrea = String.valueOf(rs.getTimestamp("fecha_creacion")); String factual = String.valueOf(rs.getTimestamp("fecha_actualizacion"));
            String colorNombre = rs.getString("color_nombre"); String tallaNombre = rs.getString("talla_nombre");
            byte[] img = rs.getBytes("imagen"); String imgName = "var_" + idv + "_" + idBodega + ".png"; File out = new File(imgDir(), imgName);
            if (img != null && img.length > 0) { try (OutputStream os = new BufferedOutputStream(new FileOutputStream(out))) { os.write(img); } }
            sb.append('{').append("\"id_variante\":").append(idv).append(',')
              .append("\"id_producto\":").append(idp).append(',')
              .append("\"id_bodega\":").append(idb).append(',')
              .append("\"id_talla\":").append(idt).append(',')
              .append("\"talla\":\"").append(escape(tallaNombre)).append("\",")
              .append("\"id_color\":").append(idc).append(',')
              .append("\"color\":\"").append(escape(colorNombre)).append("\",")
              .append("\"codigo_barras\":\"").append(escape(codigoBarras)).append("\",")
              .append("\"sku\":\"").append(escape(sku)).append("\",")
              .append("\"ean\":\"").append(escape(ean)).append("\",")
              .append("\"precio_venta\":").append(precioVenta != null ? precioVenta.toPlainString() : "0").append(',')
              .append("\"precio_compra\":").append(precioCompra != null ? precioCompra.toPlainString() : "0").append(',')
              .append("\"stock_minimo_variante\":").append(stockMinVar).append(',')
              .append("\"stock_par\":").append(stockPares).append(',')
              .append("\"stock_caja\":").append(stockCajas).append(',')
              .append("\"disponible\":").append(disponible).append(',')
              .append("\"fecha_creacion\":\"").append(escape(fcrea)).append("\",")
              .append("\"fecha_actualizacion\":\"").append(escape(factual)).append("\",")
              .append("\"imagen\":\"").append(escape(out.getAbsolutePath())).append("\"}")
              .append('\n'); count++;
        }
        rs.close(); ps.close(); con.close();
        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(jf))) { os.write(sb.toString().getBytes(StandardCharsets.UTF_8)); }
        return count;
    }
}
