package raven.clases.productos;

import javax.swing.*;

import raven.clases.productos.barcode.BarcodeConfigOptimizado;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.ArrayList;
import java.util.List;

public class ImpresorTermicaPOSDIG2406T implements Printable {
    public enum ModoImpresion { CAJA, ETIQUETA }

    private final javax.swing.JTable tabla;
    private final ModoImpresion modo;
    private int margenIzquierdo = 2, margenSuperior = 2, margenDerecho = 2, margenInferior = 2;
    private boolean usarTiraLayout;
    private List<InfoEtiqueta> etiquetasTira;
    private int totalTiras;
    private double scaleFactor = 1.0;
    private boolean autoFit;
    private boolean usarTamanoPersonalizado;
    private double customAnchoPoints, customAltoPoints;
    private boolean rotate180;
    private java.awt.print.PageFormat fixedPageFormat;
    private double contentOffsetPointsX;
    private double contentOffsetPointsY;
    private int rotationDegrees;
    
    private double extraPaddingTopPoints;
    private double extraPaddingBottomPoints;

    private int etiquetasPorTira = 3;
    private static final double PT_PER_MM = 72.0 / 25.4;
    private static final double GS1_QUIET_MM = 5.0;
    private static final double GS1_BARCODE_W_MM = 50.0;
    private static final double GS1_BARCODE_H_MM = 25.0;
    /** Resolución estándar para impresoras térmicas (203 DPI) para alineación perfecta de píxeles */
    private static final int PRINTER_DPI = 203;

    public ImpresorTermicaPOSDIG2406T(javax.swing.JTable tabla, ModoImpresion modo) {
        this.tabla = tabla;
        this.modo = modo;
    }

    public void setMargenes(int izquierdo, int superior, int derecho, int inferior) {
        this.margenIzquierdo = izquierdo;
        this.margenSuperior = superior;
        this.margenDerecho = derecho;
        this.margenInferior = inferior;
    }
    public void setMargenes(double izquierdo, double superior, double derecho, double inferior) {
        setMargenes((int)Math.round(izquierdo), (int)Math.round(superior), (int)Math.round(derecho), (int)Math.round(inferior));
    }

    public void setScaleFactor(double s) { this.scaleFactor = (s <= 0) ? 1.0 : s; }
    public void setAutoFit(boolean v) { this.autoFit = v; }
    public void setCustomPaperSizeMM(double anchoMM, double altoMM) {
        usarTamanoPersonalizado = true;
        customAnchoPoints = anchoMM * 72.0 / 25.4;
        customAltoPoints = altoMM * 72.0 / 25.4;
    }

    public void setEtiquetaSizeMM(double anchoEtiquetaMm, double altoEtiquetaMm) {
        double paperAnchoMm = (modo == ModoImpresion.ETIQUETA) ? (anchoEtiquetaMm * getEtiquetasPorTira()) : anchoEtiquetaMm;
        setCustomPaperSizeMM(paperAnchoMm, altoEtiquetaMm);
    }
    public void clearCustomPaperSize() { usarTamanoPersonalizado = false; customAnchoPoints = customAltoPoints = 0; }
    public void setUsarConfiguracionXP420B(boolean usar) { /* compat */ }
    public void setRotate180(boolean r) { this.rotate180 = r; }
    public void setRotationDegrees(int deg) { this.rotationDegrees = deg; }
    public void setContenidoOffsetMM(double mm) { this.contentOffsetPointsX = mm * 72.0 / 25.4; }
    public void setContenidoOffsetYMM(double mm) { this.contentOffsetPointsY = mm * 72.0 / 25.4; }
    public void setPaddingYMM(double topMm, double bottomMm) {
        this.extraPaddingTopPoints = topMm * 72.0 / 25.4;
        this.extraPaddingBottomPoints = bottomMm * 72.0 / 25.4;
    }

    public int getMargenIzquierdoPoints() { return margenIzquierdo; }
    public int getMargenSuperiorPoints() { return margenSuperior; }
    public int getMargenDerechoPoints() { return margenDerecho; }
    public int getMargenInferiorPoints() { return margenInferior; }
    public double getContenidoOffsetPointsX() { return contentOffsetPointsX; }
    public double getContenidoOffsetPointsY() { return contentOffsetPointsY; }
    public double getPaddingTopPoints() { return extraPaddingTopPoints; }
    public double getPaddingBottomPoints() { return extraPaddingBottomPoints; }
    public int getRotationDegrees() { return rotationDegrees; }
    public boolean isAutoFit() { return autoFit; }
    public double getScaleFactor() { return scaleFactor; }
    public ModoImpresion getModo() { return modo; }
    public int getEtiquetasPorTira() { return Math.max(1, etiquetasPorTira); }
    public void setEtiquetasPorTira(int n) { this.etiquetasPorTira = Math.max(1, n); }
    public int getTotalFilasTira() {
        if (modo != ModoImpresion.ETIQUETA) return 0;
        if (etiquetasTira == null) return 0;
        int cols = Math.max(1, getEtiquetasPorTira());
        return (int) Math.ceil(etiquetasTira.size() / (double) cols);
    }
    public String[] getSlotsFilaTira(int pageIndex) {
        int cols = Math.max(1, getEtiquetasPorTira());
        String[] res = new String[cols];
        if (modo != ModoImpresion.ETIQUETA) return res;
        if (etiquetasTira == null || pageIndex < 0) return res;
        int start = pageIndex * cols;
        for (int pos = 0; pos < cols; pos++) {
            int idx = start + pos;
            if (idx >= etiquetasTira.size()) break;
            InfoEtiqueta info = etiquetasTira.get(idx);
            if (info == null || info.esVacia || info.fila < 0 || info.fila >= tabla.getRowCount()) continue;
            String nombre = safeStr(tabla.getValueAt(info.fila, 2));
            String talla = safeStr(tabla.getValueAt(info.fila, 4));
            String color = safeStr(tabla.getValueAt(info.fila, 3));
            String s = (nombre.isEmpty() ? "" : nombre);
            if (!talla.isEmpty()) s = s.isEmpty() ? talla : (s + "  " + talla);
            if (!color.isEmpty()) s = s.isEmpty() ? color : (s + "  " + color);
            res[pos] = s;
        }
        return res;
    }

    public PageFormat buildPageFormat(PrinterJob job) {
        if (fixedPageFormat != null) return fixedPageFormat;
        if (ConfiguracionImpresoraXP420B_Windows.esImpresoraXP420B()) {
            PageFormat pfW = ConfiguracionImpresoraXP420B_Windows.crearPageFormatConservandoDriver(job);
            try { return (job != null ? job : PrinterJob.getPrinterJob()).validatePage(pfW); } catch (Exception ignore) {}
            return pfW;
        }
        PrinterJob j = job == null ? PrinterJob.getPrinterJob() : job;
        PageFormat pf = j.defaultPage();
        if (usarTamanoPersonalizado) {
            Paper p = new Paper();
            p.setSize(customAnchoPoints > 0 ? customAnchoPoints : pf.getPaper().getWidth(),
                      customAltoPoints > 0 ? customAltoPoints : pf.getPaper().getHeight());
            p.setImageableArea(0, 0, p.getWidth(), p.getHeight());
            pf.setPaper(p);
            try {
                if (customAnchoPoints >= customAltoPoints) {
                    pf.setOrientation(PageFormat.LANDSCAPE);
                } else {
                    pf.setOrientation(PageFormat.PORTRAIT);
                }
            } catch (Exception ignore) {}
        }
        try { return j.validatePage(pf); } catch (Exception ignore) {}
        return pf;
    }

    public void setPageFormat(java.awt.print.PageFormat pf) { this.fixedPageFormat = pf; }

    public List<BufferedImage> generarPreviewImagenes() {
        PrinterJob job = PrinterJob.getPrinterJob();
        PageFormat pf = buildPageFormat(job);
        return generarPreviewImagenes(pf);
    }

    public List<BufferedImage> generarPreviewImagenes(PageFormat pf) {
        List<BufferedImage> imgs = new ArrayList<>();
        prepararEtiquetasParaImpresion();
        if (pf == null) return imgs;

        double pageW = pf.getWidth();
        double pageH = pf.getHeight();
        double scale = 2.0;
        int imgW = Math.max(1, (int) Math.round(pageW * scale));
        int imgH = Math.max(1, (int) Math.round(pageH * scale));

        int cols = columnasPorTira(pf);
        int pages = usarTiraLayout
                ? (int) Math.ceil((etiquetasTira == null ? 0 : (double) etiquetasTira.size()) / Math.max(1, cols))
                : countPagesCaja();

        for (int p = 0; p < pages; p++) {
            BufferedImage img = new BufferedImage(imgW, imgH, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, imgW, imgH);
                g.scale(scale, scale);
                int res = print(g, pf, p);
                if (res == Printable.PAGE_EXISTS) imgs.add(img);
            } catch (Exception ignored) {
            } finally {
                g.dispose();
            }
        }
        return imgs;
    }

    private int countPagesCaja() {
        int total = 0;
        for (int fila = 0; fila < tabla.getRowCount(); fila++) {
            if (Boolean.TRUE.equals(tabla.getValueAt(fila, 0))) {
                int cant = 1;
                try { cant = Integer.parseInt(String.valueOf(tabla.getValueAt(fila,5))); } catch (Exception ignore) {}
                total += Math.max(0, cant);
            }
        }
        return total;
    }

    public void prepararEtiquetasParaImpresion() {
        usarTiraLayout = (modo == ModoImpresion.ETIQUETA);
        etiquetasTira = new ArrayList<>();
        if (usarTiraLayout) {
            for (int fila = 0; fila < tabla.getRowCount(); fila++) {
                boolean seleccionado = Boolean.TRUE.equals(tabla.getValueAt(fila,0));
                if (seleccionado) {
                    int cantidad = 1;
                    try { cantidad = Integer.parseInt(String.valueOf(tabla.getValueAt(fila,5))); } catch (Exception ignore) {}
                    for (int k=0;k<cantidad;k++) etiquetasTira.add(new InfoEtiqueta(fila,false));
                }
            }
            int totalEtiquetas = etiquetasTira.size();
            int cols = Math.max(1, getEtiquetasPorTira());
            int vacios = (cols - (totalEtiquetas % cols)) % cols;
            for (int i=0;i<vacios;i++) etiquetasTira.add(new InfoEtiqueta(-1,true));
            totalTiras = etiquetasTira.size() / cols;
        } else {
            totalTiras = 0;
        }
    }

    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
        // Asegurar que las etiquetas estén preparadas
        if (etiquetasTira == null || etiquetasTira.isEmpty()) {
            prepararEtiquetasParaImpresion();
        }

        Graphics2D g2 = (Graphics2D) graphics;
        g2.setColor(Color.BLACK);
        if (!autoFit && scaleFactor != 1.0) g2.scale(scaleFactor, scaleFactor);
        double ix = pageFormat.getImageableX();
        double iy = pageFormat.getImageableY();
        double iw = pageFormat.getImageableWidth();
        double ih = pageFormat.getImageableHeight();
        if (rotationDegrees == 90) {
            g2.translate(ix, iy);
            g2.rotate(Math.toRadians(90));
            g2.translate(0, -iw);
        } else if (rotationDegrees == -90) {
            g2.translate(ix, iy);
            g2.rotate(Math.toRadians(-90));
            g2.translate(-ih, 0);
        } else if (rotationDegrees == 180 || (rotate180 && modo != ModoImpresion.ETIQUETA)) {
            g2.rotate(Math.PI, pageFormat.getWidth()/2.0, pageFormat.getHeight()/2.0);
        }
        g2.setColor(Color.WHITE);
        g2.fillRect((int)Math.round(ix), (int)Math.round(iy), (int)Math.round(iw), (int)Math.round(ih));
        g2.setColor(Color.BLACK);
        if (usarTiraLayout) {
            int cols = columnasPorTira(pageFormat);
            int totalEtiquetas = (etiquetasTira == null ? 0 : etiquetasTira.size());
            int totalPages = (int)Math.ceil((double)totalEtiquetas / Math.max(1, cols));
            
            System.out.printf("Impresion Tira: Cols=%d, TotalEtiquetas=%d, TotalPages=%d%n", cols, totalEtiquetas, totalPages);
            System.out.printf("  Page Dim: ix=%.2f, iy=%.2f, iw=%.2f, ih=%.2f%n", ix, iy, iw, ih);
            
            if (pageIndex < 0 || pageIndex >= totalPages) return NO_SUCH_PAGE;
            double cellW = iw / cols;
            // Ajuste global para centrar mejor en la tira física (mover a la derecha)
            double globalXOffset = 8.0; 
            
            // Calculamos un ancho fijo y estable para todas las columnas
            // Esto es CRÍTICO para que el código de barras se genere idéntico en todas las etiquetas
            // y evitar el "jitter" de 1px por redondeo que cambia el cálculo de barras
            int stableColW = (int) Math.floor(iw / cols);
            
            int cellY = (int) Math.round(iy);
            int cellH = (int) Math.round(ih);
            int start = pageIndex * cols;
            for (int pos = 0; pos < cols; pos++) {
                int idx = start + pos;
                if (idx >= etiquetasTira.size()) break;
                InfoEtiqueta info = etiquetasTira.get(idx);

                double x0 = ix + globalXOffset + pos * cellW;
                // Ajuste fino para evitar solapamiento visual
                x0 += 2.0; 
                
                int cellX = (int) Math.round(x0);
                // Usamos el ancho estable calculado previamente en lugar de recalcular por columna
                int thisCellW = stableColW;

                String nombreProducto = "VACIO";
                if (info.fila >= 0 && info.fila < tabla.getRowCount()) {
                    nombreProducto = safeStr(tabla.getValueAt(info.fila, 2));
                }
                
                System.out.printf("  Etiqueta %d (Pos %d): x=%d, w=%d, Nombre=%s [x0=%.2f (StableW=%d)]%n", 
                        idx, pos, cellX, thisCellW, nombreProducto, x0, stableColW);

                int x = cellX + margenIzquierdo;
                int y = cellY + margenSuperior;
                int w = thisCellW - margenIzquierdo - margenDerecho;
                int h = cellH - margenSuperior - margenInferior;
                
                // FORCE PRINT even if small, for debugging
                if (w > 0 && h > 0) {
                     renderEtiqueta(g2, x, y, w, h, info);
                } else {
                     System.err.printf("  SKIPPED Etiqueta %d: w=%d, h=%d (Margenes too big?)%n", idx, w, h);
                }
            }
            return PAGE_EXISTS;
        } else {
            int global = 0;
            for (int fila=0; fila<tabla.getRowCount(); fila++) {
                if (Boolean.TRUE.equals(tabla.getValueAt(fila,0))) {
                    int cant = 1; try { cant = Integer.parseInt(String.valueOf(tabla.getValueAt(fila,5))); } catch (Exception ignore) {}
                    if (pageIndex >= global && pageIndex < global+cant) {
                        InfoEtiqueta info = new InfoEtiqueta(fila,false);
                        int x = (int)Math.round(ix) + margenIzquierdo;
                        int y = (int)Math.round(iy) + margenSuperior;
                        int w = (int)Math.round(iw) - margenIzquierdo - margenDerecho;
                        int h = (int)Math.round(ih) - margenSuperior - margenInferior;
                        renderEtiqueta(g2, x, y, w, h, info);
                        return PAGE_EXISTS;
                    }
                    global += cant;
                }
            }
            return NO_SUCH_PAGE;
        }
    }

    private int columnasPorTira(PageFormat pf) {
        return Math.max(1, getEtiquetasPorTira());
    }

    private void renderEtiqueta(Graphics2D g2, int x, int y, int w, int h, InfoEtiqueta info) {
        if (modo == ModoImpresion.ETIQUETA) {
            pintarEtiquetaPar(g2, x, y, w, h, info);
            return;
        }
        if (autoFit) {
            // Detectar si es etiqueta pequeña (ancho < 100pts ≈ 35mm)
            boolean esEtiquetaPequena = w < 120 || h < 120;

            final int DW, DH;
            if (esEtiquetaPequena) {
                // Para etiquetas pequeñas usar proporciones reales
                DW = w;  // Usar ancho real
                DH = h;  // Usar alto real
            } else {
                // Para etiquetas grandes usar el diseño original
                DW = 340;
                DH = 110;
            }

            double availH = Math.max(1.0, h - (extraPaddingTopPoints + extraPaddingBottomPoints));
            double s = Math.min((double)w/DW, (double)availH/DH);
            double dx = (w - DW*s) / 2.0;
            double dy = (availH - DH*s) / 2.0 + extraPaddingTopPoints;
            Graphics2D g = (Graphics2D) g2.create();
            g.translate(x + dx + contentOffsetPointsX, y + dy + contentOffsetPointsY);
            g.scale(s, s);
            pintarEtiquetaBase(g, 0, 0, DW, DH, info, esEtiquetaPequena);
            g.dispose();
        } else {
            boolean esEtiquetaPequena = w < 120 || h < 120;
            pintarEtiquetaBase(g2, x, y, w, h, info, esEtiquetaPequena);
        }
    }

    private void pintarEtiquetaBase(Graphics2D g2, int x, int y, int w, int h, InfoEtiqueta info, boolean esEtiquetaPequena) {
        g2.setColor(java.awt.Color.BLACK);

        // Borde ajustado según tamaño
        float sw = esEtiquetaPequena ? 1.5f : (float)Math.max(2.5f, (float)(2.4 * (h/110.0)));
        g2.setStroke(new java.awt.BasicStroke(sw));
        int arc = esEtiquetaPequena ? 4 : 16;
        int pad = esEtiquetaPequena ? 1 : 4;
        g2.drawRoundRect(x+pad, y+pad, w-pad*2, h-pad*2, arc, arc);

        if (info.esVacia) return;

        String nombre = safeStr(tabla.getValueAt(info.fila,2));
        String color = safeStr(tabla.getValueAt(info.fila,3));
        String talla = safeStr(tabla.getValueAt(info.fila,4));
        String ean = safeStr(tabla.getValueAt(info.fila,1));

        int ox = (int)Math.round(contentOffsetPointsX);
        int oy = (int)Math.round(contentOffsetPointsY);

        // Cálculo de escalado proporcional basado en dimensiones reales
        double proporcionAncho = w / 85.0; // Basado en ancho típico de 30mm con margen
        double proporcionAlto = h / 25.0;  // Basado en alto típico de 30mm con margen
        double escala = Math.min(proporcionAncho, proporcionAlto);

        // Asegurar un mínimo de escala para legibilidad
        escala = Math.max(0.5, Math.min(escala, 2.0));

        if (esEtiquetaPequena || escala < 1.0) {
            // DISEÑO PARA ETIQUETAS PEQUEÑAS O ESCALADAS
            // Calcular tamaños de fuente escalados
            int tamFuenteNombre = Math.max(6, (int)(7 * escala));
            int tamFuenteColor = Math.max(5, (int)(6 * escala));
            int tamFuenteTalla = Math.max(8, (int)(12 * escala));

            java.awt.Font fNombre = new java.awt.Font("SansSerif", java.awt.Font.BOLD, tamFuenteNombre);
            java.awt.Font fColor = new java.awt.Font("SansSerif", java.awt.Font.PLAIN, tamFuenteColor);
            java.awt.Font fTalla = new java.awt.Font("SansSerif", java.awt.Font.BOLD, tamFuenteTalla);

            // Ajustar posiciones basadas en escala
            int posX = x + ox + (int)(4 * escala);
            int posYNombre = y + oy + (int)(9 * escala);
            int posYColor = y + oy + (int)(16 * escala);

            // Nombre (truncar si es muy largo según el espacio disponible)
            String nombreCorto = ajustarTexto(nombre, g2.getFontMetrics(fNombre), (int)(w * 0.7));
            g2.setFont(fNombre);
            g2.drawString(nombreCorto, posX, posYNombre);

            // Color
            String colorCorto = ajustarTexto(color, g2.getFontMetrics(fColor), (int)(w * 0.5));
            g2.setFont(fColor);
            g2.drawString("C:" + colorCorto, posX, posYColor);

            // Código de barras (ajustar tamaño según espacio disponible)
            int bcW = Math.max(30, (int)(w * 0.8));
            int bcH = Math.max(8, (int)(12 * escala));
            java.awt.image.BufferedImage bc = generateBarcodeImage(ean, bcW, bcH);
            if (bc != null) {
                int bx = x + ox + (w - bcW) / 2;
                int by = y + h - bcH - (int)(12 * escala) - 15; // Dejar espacio para el EAN debajo
                g2.drawImage(bc, bx, by, bx + bcW, by + bcH, 0, 0, bc.getWidth(), bc.getHeight(), null);

                // Mostrar el número del código de barras debajo del código de barras
                int tamFuenteEan = Math.max(6, (int)(6 * escala * 0.8)); // Fuente más pequeña
                java.awt.Font fontEan = new java.awt.Font("Monospaced", java.awt.Font.PLAIN, tamFuenteEan);
                g2.setFont(fontEan);
                java.awt.FontMetrics fmEan = g2.getFontMetrics(fontEan);
                String eanDisplay = ean != null ? ean : "";
                int eanW = fmEan.stringWidth(eanDisplay);
                int eanX = x + ox + Math.max(0, (w - eanW) / 2); // Centrado
                int eanY = by + bcH + fmEan.getAscent() + (int) Math.round(3.0 * PT_PER_MM);

                g2.setColor(new java.awt.Color(50, 50, 50)); // Color gris profesional
                g2.drawString(eanDisplay, eanX, eanY);
            }

            // Talla (derecha, tamaño ajustado)
            g2.setFont(fTalla);
            String tallaTxt = talla;
            int textW = g2.getFontMetrics().stringWidth(tallaTxt);
            g2.drawString(tallaTxt, x + ox + w - textW - (int)(4 * escala), posYNombre);

        } else {
            // DISEÑO PARA ETIQUETAS GRANDES (sin cambios significativos)
            int baseH = Math.max(80, h);
            double u = baseH / 110.0;
            java.awt.Font fBold = new java.awt.Font("SansSerif", java.awt.Font.BOLD, (int)Math.round(18*u));
            java.awt.Font fSmall = new java.awt.Font("SansSerif", java.awt.Font.PLAIN, (int)Math.round(14*u));

            g2.setFont(fBold);
            g2.drawString(nombre, x+ox+(int)Math.round(12*u), y+oy+(int)Math.round(22*u));
            g2.setFont(fSmall);
            g2.drawString("C: " + color, x+ox+(int)Math.round(12*u), y+oy+(int)Math.round(42*u));

            int bcH = (int)Math.round(40*u);
            int bcW = Math.max(60, w - (int)Math.round(120*u));
            java.awt.image.BufferedImage bc = generateBarcodeImage(ean, bcW, bcH);
            int bx = x+ox+(int)Math.round(12*u);
            int by = y + h - bcH - (int)Math.round(8*u) - 25; // Dejar espacio para el EAN debajo
            if (bc != null) g2.drawImage(bc, bx, by, bx + bcW, by + bcH, 0, 0, bc.getWidth(), bc.getHeight(), null);

            // Mostrar el número del código de barras debajo del código de barras para etiquetas grandes
            int tamFuenteEan = (int)Math.round(14*u * 0.7); // Fuente más pequeña
            java.awt.Font fontEan = new java.awt.Font("Monospaced", java.awt.Font.PLAIN, Math.max(8, tamFuenteEan));
            g2.setFont(fontEan);
            java.awt.FontMetrics fmEan = g2.getFontMetrics(fontEan);
            String eanDisplay = ean != null ? ean : "";
            int eanW = fmEan.stringWidth(eanDisplay);
            int eanX = x + ox + Math.max(0, (w - eanW) / 2); // Centrado
            int eanY = by + bcH + fmEan.getAscent() + (int) Math.round(3.0 * PT_PER_MM);

            g2.setColor(new java.awt.Color(50, 50, 50)); // Color gris profesional
            g2.drawString(eanDisplay, eanX, eanY);

            g2.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, (int)Math.round(26*u)));
            int textW = g2.getFontMetrics().stringWidth(talla + " EU");
            int tx = x + ox + w - textW - (int)Math.round(16*u);
            int ty = y + oy + (int)Math.round(34*u);
            g2.drawString(talla + " EU", tx, ty);
        }
    }

    /**
     * Ajusta el texto para que quepa en el ancho especificado
     */
    private String ajustarTexto(String texto, java.awt.FontMetrics fm, int anchoMaximo) {
        if (texto == null) return "";

        int anchoTexto = fm.stringWidth(texto);
        if (anchoTexto <= anchoMaximo) {
            return texto;
        }

        // Si el texto es demasiado largo, truncarlo y añadir puntos suspensivos
        String sufijo = "..";
        int anchoSufijo = fm.stringWidth(sufijo);

        if (anchoSufijo >= anchoMaximo) {
            return texto.substring(0, 1) + sufijo;
        }

        int longitud = texto.length();
        for (int i = longitud; i > 0; i--) {
            String subTexto = texto.substring(0, i);
            int anchoSub = fm.stringWidth(subTexto + sufijo);
            if (anchoSub <= anchoMaximo) {
                return subTexto + sufijo;
            }
        }

        return texto.substring(0, 1) + sufijo;
    }

    private String safeStr(Object v) { return v == null ? "" : String.valueOf(v); }

    private void pintarEtiquetaGS1(Graphics2D g2, int x, int y, int w, int h, InfoEtiqueta info) {
        double ox = contentOffsetPointsX;
        double oy = contentOffsetPointsY;
        double baseX = x + ox;
        double baseY = y + oy;

        g2.setColor(java.awt.Color.BLACK);
        g2.setStroke(new java.awt.BasicStroke(1.5f));
        g2.drawRect((int) Math.round(baseX), (int) Math.round(baseY), (int) Math.round(w), (int) Math.round(h));

        int reservedWpt = Math.min(w, (int) Math.round(GS1_BARCODE_W_MM * PT_PER_MM));
        int reservedHpt = Math.min(h, (int) Math.round(GS1_BARCODE_H_MM * PT_PER_MM));
        reservedWpt = Math.max(1, reservedWpt);
        reservedHpt = Math.max(1, reservedHpt);

        int quietPt = (int) Math.round(GS1_QUIET_MM * PT_PER_MM);
        quietPt = Math.max(0, Math.min(quietPt, Math.min(reservedWpt / 2, reservedHpt / 2)));

        int bcWpt = Math.max(1, reservedWpt - quietPt * 2);
        int bcHpt = Math.max(1, reservedHpt - quietPt * 2);

        double quietX = baseX + (w - reservedWpt) / 2.0;
        double quietY = baseY + h - reservedHpt;

        java.awt.geom.Rectangle2D.Double quietRect = new java.awt.geom.Rectangle2D.Double(quietX, quietY, reservedWpt, reservedHpt);
        java.awt.geom.Rectangle2D.Double barcodeRect = new java.awt.geom.Rectangle2D.Double(quietX + quietPt, quietY + quietPt, bcWpt, bcHpt);

        if (info.esVacia) return;

        String nombre = safeStr(tabla.getValueAt(info.fila, 2));
        String color = safeStr(tabla.getValueAt(info.fila, 3));
        String talla = safeStr(tabla.getValueAt(info.fila, 4));
        String ean = safeStr(tabla.getValueAt(info.fila, 1));

        String colorCorto = color;
        if (colorCorto.length() > 15) colorCorto = colorCorto.substring(0, 15);

        double padMm = 3.0;
        double pad = padMm * PT_PER_MM;
        double top = baseY + pad;
        double left = baseX + pad;
        double right = baseX + w - pad;
        double maxW = Math.max(1.0, right - left);
        double availH = Math.max(1.0, quietRect.y - top);
        double gap = 3.0 * PT_PER_MM;

        double wMm = w / PT_PER_MM;
        boolean juntaTallaColor = wMm <= 40.0;

        int nameSize = 11;
        int chosenNameSize = 6;
        String[] chosenNameLines = new String[] { "", "" };
        int chosenTallaSize = 6;
        int chosenColorSize = 6;
        for (int s = nameSize; s >= 6; s--) {
            int actualNameSize = Math.max(6, s);
            java.awt.Font fName = new java.awt.Font("SansSerif", java.awt.Font.PLAIN, Math.max(8, actualNameSize));
            java.awt.FontMetrics fmName = g2.getFontMetrics(fName);
            String[] nameLines = splitNombreEnDosLineas(nombre, fmName, (int) Math.round(maxW));
            int nameLinesCount = (nameLines[1] == null || nameLines[1].isEmpty()) ? 1 : 2;
            double nameH = nameLinesCount * fmName.getHeight();

            int tallaSize = Math.max(6, (int) Math.round(fName.getSize() * 0.75));
            int colorSize = Math.max(6, tallaSize);
            java.awt.Font fTalla = new java.awt.Font("SansSerif", java.awt.Font.PLAIN, tallaSize);
            java.awt.Font fColor = new java.awt.Font("SansSerif", java.awt.Font.PLAIN, colorSize);
            java.awt.FontMetrics fmT = g2.getFontMetrics(fTalla);
            java.awt.FontMetrics fmC = g2.getFontMetrics(fColor);

            int detailLines = juntaTallaColor ? 1 : 2;
            double detailH = detailLines == 1 ? Math.max(fmT.getHeight(), fmC.getHeight()) : (fmT.getHeight() + fmC.getHeight());
            double need = nameH + gap + detailH;

            if (need <= availH) {
                chosenNameSize = fName.getSize();
                chosenNameLines = nameLines;
                chosenTallaSize = tallaSize;
                chosenColorSize = colorSize;
                break;
            }
        }

        java.awt.Font fName = new java.awt.Font("SansSerif", java.awt.Font.PLAIN, chosenNameSize);
        java.awt.FontMetrics fmName = g2.getFontMetrics(fName);
        String line1 = chosenNameLines[0] == null ? "" : chosenNameLines[0];
        String line2 = chosenNameLines[1] == null ? "" : chosenNameLines[1];
        int nameLinesCount = line2.isEmpty() ? 1 : 2;
        double yCursor = top;

        g2.setColor(java.awt.Color.BLACK);
        g2.setFont(fName);
        int c1w = fmName.stringWidth(line1);
        int x1 = (int) Math.round(baseX + w / 2.0 - c1w / 2.0);
        int y1 = (int) Math.round(yCursor + fmName.getAscent());
        g2.drawString(line1, x1, y1);
        if (!line2.isEmpty()) {
            int c2w = fmName.stringWidth(line2);
            int x2 = (int) Math.round(baseX + w / 2.0 - c2w / 2.0);
            int y2 = (int) Math.round(yCursor + fmName.getHeight() + fmName.getAscent());
            g2.drawString(line2, x2, y2);
        }
        yCursor += nameLinesCount * fmName.getHeight() + gap;

        java.awt.Font fTalla = new java.awt.Font("SansSerif", java.awt.Font.PLAIN, chosenTallaSize);
        java.awt.Font fColor = new java.awt.Font("SansSerif", java.awt.Font.PLAIN, chosenColorSize);
        java.awt.FontMetrics fmT = g2.getFontMetrics(fTalla);
        java.awt.FontMetrics fmC = g2.getFontMetrics(fColor);

        if (juntaTallaColor) {
            String linea = (talla == null ? "" : talla.trim());
            String c = (colorCorto == null ? "" : colorCorto.trim());
            if (!c.isEmpty()) linea = linea.isEmpty() ? c : (linea + "  " + c);
            g2.setFont(fTalla);
            linea = ajustarTexto(linea, fmT, (int) Math.round(maxW));
            int lw = fmT.stringWidth(linea);
            int lx = (int) Math.round(baseX + w / 2.0 - lw / 2.0);
            int ly = (int) Math.round(yCursor + fmT.getAscent());
            if (ly + fmT.getDescent() <= quietRect.y) g2.drawString(linea, lx, ly);
        } else {
            g2.setFont(fTalla);
            String tTxt = ajustarTexto(talla, fmT, (int) Math.round(maxW));
            int tw = fmT.stringWidth(tTxt);
            int tx = (int) Math.round(baseX + w / 2.0 - tw / 2.0);
            int ty = (int) Math.round(yCursor + fmT.getAscent());
            if (ty + fmT.getDescent() <= quietRect.y) g2.drawString(tTxt, tx, ty);
            yCursor += fmT.getHeight();

            g2.setFont(fColor);
            String cTxt = ajustarTexto(colorCorto, fmC, (int) Math.round(maxW));
            int cw = fmC.stringWidth(cTxt);
            int cx = (int) Math.round(baseX + w / 2.0 - cw / 2.0);
            int cy = (int) Math.round(yCursor + fmC.getAscent());
            if (cy + fmC.getDescent() <= quietRect.y) g2.drawString(cTxt, cx, cy);
        }

        java.awt.image.BufferedImage bc = generateBarcodeImageGS1(ean, bcWpt, bcHpt);
        if (bc != null) {
            int bx = (int) Math.round(barcodeRect.x);
            int by = (int) Math.round(barcodeRect.y);
            g2.drawImage(bc, bx, by, bx + bcWpt, by + bcHpt, 0, 0, bc.getWidth(), bc.getHeight(), null);

            java.awt.Font fontEan = new java.awt.Font("Monospaced", java.awt.Font.PLAIN, Math.max(8, (int)(12 * 0.6)));
            g2.setFont(fontEan);
            java.awt.FontMetrics fmEan = g2.getFontMetrics(fontEan);
            String eanDisplay = ean != null ? ean : "";
            int eanW = fmEan.stringWidth(eanDisplay);
            int eanX = (int) Math.round(baseX + w / 2.0 - eanW / 2.0); // Centrado
            int eanY = by + bcHpt + fmEan.getAscent() + (int) Math.round(3.0 * PT_PER_MM);

            // Asegurar que no se salga del área disponible
            if (eanY + fmEan.getHeight() <= baseY + h) {
                g2.setColor(new java.awt.Color(50, 50, 50)); // Color gris profesional
                g2.drawString(eanDisplay, eanX, eanY);
            }
        }
    }

    private void pintarEtiquetaPar(Graphics2D g2, int x, int y, int w, int h, InfoEtiqueta info) {
        double ox = contentOffsetPointsX;
        double oy = contentOffsetPointsY;
        double baseX = x + ox;
        double baseY = y + oy;

        double designW = w; // Usar el ancho real disponible
        double designH = h; // Usar el alto real disponible
        double s = 1.0; // No aplicar escala adicional ya que usamos las dimensiones reales
        double dx = 0; // No hay desplazamiento adicional
        double dy = 0; // No hay desplazamiento adicional

        Graphics2D g = (Graphics2D) g2.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.translate(baseX + dx, baseY + dy);
        g.scale(s, s);

        // Calcular proporciones basadas en las dimensiones reales
        double propW = designW / (50.0 * PT_PER_MM); // Proporción relativa al diseño original
        double propH = designH / (30.0 * PT_PER_MM);

        int pad = (int) Math.round(1.0 * PT_PER_MM * Math.min(propW, propH));
        int cornerLen = (int) Math.round(2.1 * PT_PER_MM * Math.min(propW, propH));
        int cornerInset = (int) Math.round(0.8 * PT_PER_MM * Math.min(propW, propH));
        float borderSw = (float) (0.35 * PT_PER_MM * Math.min(propW, propH));
        float cornerSw = (float) (0.55 * PT_PER_MM * Math.min(propW, propH));
        int arc = (int) Math.round(1.8 * PT_PER_MM * Math.min(propW, propH));

        g.setColor(new Color(70, 70, 70));
        g.setStroke(new BasicStroke(borderSw));
        g.drawRoundRect(pad, pad, (int) Math.round(designW) - pad * 2, (int) Math.round(designH) - pad * 2, arc, arc);

        g.setStroke(new BasicStroke(cornerSw));
        int left = pad + cornerInset;
        int top = pad + cornerInset;
        int right = (int) Math.round(designW) - pad - cornerInset;
        int bottom = (int) Math.round(designH) - pad - cornerInset;
        g.drawLine(left, top, left + cornerLen, top);
        g.drawLine(left, top, left, top + cornerLen);
        g.drawLine(right - cornerLen, top, right, top);
        g.drawLine(right, top, right, top + cornerLen);
        g.drawLine(left, bottom - cornerLen, left, bottom);
        g.drawLine(left, bottom, left + cornerLen, bottom);
        g.drawLine(right - cornerLen, bottom, right, bottom);
        g.drawLine(right, bottom - cornerLen, right, bottom);

        if (info.esVacia) {
            g.dispose();
            return;
        }

        String nombre = safeStr(tabla.getValueAt(info.fila, 2)).trim().toUpperCase();
        String color = safeStr(tabla.getValueAt(info.fila, 3)).trim().toUpperCase();
        String talla = safeStr(tabla.getValueAt(info.fila, 4)).trim().toUpperCase();
        String ean = safeStr(tabla.getValueAt(info.fila, 1)).trim();

        String tallaNum = talla;
        String tallaSuf = "EU";
        int sp = talla.indexOf(' ');
        if (sp > 0) {
            tallaNum = talla.substring(0, sp).trim();
            String rest = talla.substring(sp + 1).trim();
            if (!rest.isEmpty()) tallaSuf = rest;
        }
        if (!tallaSuf.contains("EU")) tallaSuf = "EU";

        int maxTextW = (int) Math.round(designW) - pad * 2 - cornerInset * 2;
        int contentLeft = pad + cornerInset;
        int contentTop = pad + cornerInset;
        int contentRight = (int) Math.round(designW) - pad - cornerInset;
        int contentBottom = (int) Math.round(designH) - pad - cornerInset;
        int contentW = Math.max(1, contentRight - contentLeft);

        double sBase = Math.min(propW, propH);
        int gapXs = Math.max(1, (int) Math.round(0.25 * PT_PER_MM * sBase));
        int gapSm = Math.max(1, (int) Math.round(0.40 * PT_PER_MM * sBase));
        int gapMd = Math.max(1, (int) Math.round(0.70 * PT_PER_MM * sBase));
        int eanGap = Math.max(0, (int) Math.round(0.20 * PT_PER_MM * sBase));

        Font fTitle = new Font("SansSerif", Font.BOLD, (int) Math.round(3.2 * PT_PER_MM * Math.min(propW, propH)));
        g.setColor(new Color(25, 25, 25));
        g.setFont(fTitle);
        FontMetrics fmTitle = g.getFontMetrics(fTitle);
        String titleLine = ajustarTexto(nombre, fmTitle, maxTextW);
        int tW = fmTitle.stringWidth(titleLine);
        int tX = contentLeft + Math.max(0, (contentW - tW) / 2);
        int titleBaseline = contentTop + fmTitle.getAscent();
        g.drawString(titleLine, tX, titleBaseline);

        String colorLine = color.isEmpty() ? "" : color;
        int tamanoFuenteColor = (int) Math.round(2.35 * PT_PER_MM * sBase);
        if (colorLine.length() > 15) {
            tamanoFuenteColor = Math.max(6, (int) (tamanoFuenteColor * 0.85));
        }
        Font fMeta = new Font("SansSerif", Font.PLAIN, tamanoFuenteColor);
        int eanSizeStart = Math.max(4, (int) Math.round(2.2 * PT_PER_MM * sBase));
        int eanSizeMin = Math.max(4, (int) Math.round(1.7 * PT_PER_MM * sBase));
        Font fontEan = new Font("Monospaced", Font.PLAIN, eanSizeStart);
        FontMetrics fmMeta = g.getFontMetrics(fMeta);
        FontMetrics fmEan = g.getFontMetrics(fontEan);

        int yAfterTitle = titleBaseline + fmTitle.getDescent();
        if (!colorLine.isEmpty()) {
            g.setFont(fMeta);
            g.setColor(new Color(70, 70, 70));
            String colorFitTop = ajustarTexto(colorLine, fmMeta, maxTextW);
            int cW = fmMeta.stringWidth(colorFitTop);
            int cX = contentLeft + Math.max(0, (contentW - cW) / 2);
            int colorBaselineTop = yAfterTitle + gapXs + fmMeta.getAscent();
            g.drawString(colorFitTop, cX, colorBaselineTop);
            yAfterTitle = colorBaselineTop + fmMeta.getDescent() + gapSm;
        } else {
            yAfterTitle = yAfterTitle + gapSm;
        }

        int bcMaxW = (int) Math.round(contentW * 0.98); // Usar hasta 98% del ancho disponible para códigos largos
        int bcW = Math.min(bcMaxW, (int) Math.round(contentW));
        // Aumentado a 13.0mm para mayor legibilidad en supermercados (estándar es 13mm-15mm)
        int bcH = Math.max(1, (int) Math.round(13.0 * PT_PER_MM * sBase));
        int bcX = contentLeft + Math.max(0, (contentW - bcW) / 2);
        int bcY = yAfterTitle;
        int bcYMin = yAfterTitle;

        int numSizeStart = (int) Math.round(6.0 * PT_PER_MM * sBase);
        int numSizeMin = Math.max(5, (int) Math.round(2.6 * PT_PER_MM * sBase));

        int chosenNumSize = numSizeMin;
        int chosenSufSize = Math.max(5, (int) Math.round(chosenNumSize * 0.55));
        int chosenTallaBaseline = contentBottom;
        int chosenTallaTop = contentBottom;
        int chosenEanBaseline = bcY + bcH + eanGap + fmEan.getAscent();
        boolean foundFit = false;

        for (int es = eanSizeStart; es >= eanSizeMin && !foundFit; es--) {
            Font fontEanTry = new Font("Monospaced", Font.PLAIN, es);
            FontMetrics fmEanTry = g.getFontMetrics(fontEanTry);

            for (int ns = Math.max(numSizeMin, numSizeStart); ns >= numSizeMin; ns--) {
                Font fNumTry = new Font("SansSerif", Font.BOLD, ns);
                int sufTry = Math.max(5, (int) Math.round(ns * 0.55));
                Font fSufTry = new Font("SansSerif", Font.BOLD, sufTry);
                FontMetrics fmNumTry = g.getFontMetrics(fNumTry);

                int tallaBaselineTry = contentBottom - fmNumTry.getDescent();
                int tallaTopTry = tallaBaselineTry - fmNumTry.getAscent();

                int eanBaselineTry = bcY + bcH + eanGap + fmEanTry.getAscent();
                int eanBottomTry = eanBaselineTry + fmEanTry.getDescent();

                if (eanBottomTry + gapSm <= tallaTopTry && eanBottomTry <= contentBottom) {
                    chosenNumSize = ns;
                    chosenSufSize = fSufTry.getSize();
                    chosenTallaBaseline = tallaBaselineTry;
                    chosenTallaTop = tallaTopTry;
                    chosenEanBaseline = eanBaselineTry;
                    fontEan = fontEanTry;
                    fmEan = fmEanTry;
                    foundFit = true;
                    break;
                }
            }
        }

        if (!foundFit) {
            Font fontEanTry = new Font("Monospaced", Font.PLAIN, eanSizeMin);
            FontMetrics fmEanTry = g.getFontMetrics(fontEanTry);
            fontEan = fontEanTry;
            fmEan = fmEanTry;
            chosenNumSize = numSizeMin;
            chosenSufSize = Math.max(5, (int) Math.round(chosenNumSize * 0.55));
            Font fNumTry = new Font("SansSerif", Font.BOLD, chosenNumSize);
            FontMetrics fmNumTry = g.getFontMetrics(fNumTry);
            chosenTallaBaseline = contentBottom - fmNumTry.getDescent();
            chosenTallaTop = chosenTallaBaseline - fmNumTry.getAscent();
            // OPTIMIZADO: Asegurar altura mínima de código de barras (5mm) incluso si hay solapamiento
            int minBarcodeH = (int)Math.round(5.0 * PT_PER_MM);
            int maxBarcodeH = Math.max(minBarcodeH, chosenTallaTop - (bcY + eanGap + fmEanTry.getHeight() + gapSm));
            bcH = Math.max(minBarcodeH, Math.min(bcH, maxBarcodeH));
            chosenEanBaseline = bcY + bcH + eanGap + fmEanTry.getAscent();
        }

        Font fNum = new Font("SansSerif", Font.BOLD, chosenNumSize);
        Font fSuf = new Font("SansSerif", Font.BOLD, chosenSufSize);
        FontMetrics fmNum = g.getFontMetrics(fNum);
        FontMetrics fmSuf = g.getFontMetrics(fSuf);
        int tallaBaseline = chosenTallaBaseline;
        int tallaTop = chosenTallaTop;

        // Generar barcode a 203 DPI (nativo de impresora térmica) para evitar aliasing
        BufferedImage bc = generateBarcodeImageParDynamic(ean, bcW, bcH, 203);
        // OPTIMIZADO: Se eliminó la restricción estricta de solapamiento (bcY + bcH < tallaTop) para forzar impresión
        if (bc != null && bcW > 0 && bcH > 0) {
            g.drawImage(bc, bcX, bcY, bcW, bcH, null);

            String eanDisplay = ean != null ? ean : "";
            g.setFont(fontEan);
            int eanW = fmEan.stringWidth(eanDisplay);
            int eanX = bcX + Math.max(0, (bcW - eanW) / 2);
            g.setColor(new Color(70, 70, 70));
            int eanTop = bcY + bcH + eanGap;
            int eanBaseline = eanTop + fmEan.getAscent();
            int eanBottom = eanBaseline + fmEan.getDescent();
            // Mostrar texto EAN solo si cabe razonablemente, pero el código de barras ya se pintó
            if (!eanDisplay.isEmpty() && eanBottom <= contentBottom) {
                g.drawString(eanDisplay, eanX, eanBaseline);
            }
        }

        int numW = fmNum.stringWidth(tallaNum);
        int sufW = fmSuf.stringWidth(tallaSuf);
        int gap = gapSm;
        int totalW = numW + gap + sufW;
        int tx = contentLeft + Math.max(0, (contentW - totalW) / 2);
        g.setColor(new Color(35, 35, 35));
        g.setFont(fNum);
        g.drawString(tallaNum, tx, tallaBaseline);
        g.setFont(fSuf);
        g.drawString(tallaSuf, tx + numW + gap, tallaBaseline);

        g.dispose();
    }

    private BufferedImage generateBarcodeImageParDynamic(String code, int targetWpt, int targetHpt, int dpi) {
        // Usar la lógica centralizada y optimizada de BarcodeConfigOptimizado
        // Esto garantiza consistencia y robustez (Snap to Grid, Adaptive Width)
        return BarcodeConfigOptimizado.generarCodigoOptimizado(code, targetWpt, targetHpt, dpi);
    }


    private String[] splitNombreEnDosLineas(String texto, java.awt.FontMetrics fm, int anchoMaximo) {
        String t = texto == null ? "" : texto.trim();
        if (t.isEmpty()) return new String[] { "", "" };
        if (fm.stringWidth(t) <= anchoMaximo) return new String[] { t, "" };

        String[] parts = t.split("\\s+");
        if (parts.length <= 1) {
            return new String[] { ajustarTexto(t, fm, anchoMaximo), "" };
        }

        String bestA = "";
        String bestB = "";
        for (int cut = 1; cut < parts.length; cut++) {
            StringBuilder a = new StringBuilder();
            for (int i = 0; i < cut; i++) {
                if (i > 0) a.append(' ');
                a.append(parts[i]);
            }
            StringBuilder b = new StringBuilder();
            for (int i = cut; i < parts.length; i++) {
                if (i > cut) b.append(' ');
                b.append(parts[i]);
            }

            String sa = a.toString();
            String sb = b.toString();
            if (fm.stringWidth(sa) <= anchoMaximo) {
                String sbFit = ajustarTexto(sb, fm, anchoMaximo);
                if (fm.stringWidth(sbFit) <= anchoMaximo) {
                    if (sa.length() > bestA.length()) {
                        bestA = sa;
                        bestB = sbFit;
                    }
                }
            }
        }

        if (!bestA.isEmpty()) return new String[] { bestA, bestB };
        return new String[] { ajustarTexto(t, fm, anchoMaximo), "" };
    }



    /**
     * Valida código EAN-13 con checksum
     * Reemplaza el método isEan13() existente (línea ~970 aprox)
     */
    private boolean isEan13(String code) {
        if (code == null || code.trim().length() != 13) {
            return false;
        }
        
        String s = code.trim();
        
        // Validar solo dígitos
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        
        // Validar checksum EAN-13
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int digit = s.charAt(i) - '0';
            sum += (i % 2 == 0) ? digit : digit * 3;
        }
        
        int checksum = (10 - (sum % 10)) % 10;
        return checksum == (s.charAt(12) - '0');
    }


    // ============================================
    // OPCIONAL: MÉTODO DE VALIDACIÓN PRE-IMPRESIÓN
    // ============================================

    /**
     * Valida códigos antes de imprimir (agregar al inicio de prepararEtiquetasParaImpresion())
     * Útil para detectar códigos problemáticos
     */
    private void validarCodigosTabla() {
        for (int fila = 0; fila < tabla.getRowCount(); fila++) {
            if (Boolean.TRUE.equals(tabla.getValueAt(fila, 0))) {
                String ean = safeStr(tabla.getValueAt(fila, 1));
                
                if (ean.isEmpty()) {
                    System.err.println("⚠️  Fila " + fila + ": Código EAN vacío");
                    continue;
                }
                
                // Verificar longitud excesiva
                if (ean.length() > 48) {
                    System.err.println("⚠️  Fila " + fila + ": Código muy largo (" + ean.length() + " chars) - Puede ser difícil de leer");
                }
                
                // Mostrar configuración que se usará
                if (Boolean.getBoolean("barcode.debug")) {
                    String config = BarcodeConfigOptimizado.obtenerInfoConfiguracion(ean);
                    System.out.println("📊 Fila " + fila + ": " + config);
                }
            }
        }
    }


    private java.awt.image.BufferedImage scaleToBinary(java.awt.image.BufferedImage src, int wPx, int hPx) {
        if (src == null) return null;
        wPx = Math.max(1, wPx);
        hPx = Math.max(1, hPx);
        java.awt.image.BufferedImage out = new java.awt.image.BufferedImage(wPx, hPx, java.awt.image.BufferedImage.TYPE_BYTE_BINARY);
        java.awt.Graphics2D g = out.createGraphics();
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(0, 0, wPx, hPx);
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING, java.awt.RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(src, 0, 0, wPx, hPx, null);
        g.dispose();
        return out;
    }

    /**
     * NUEVO MÉTODO 2: Genera código de barras para etiquetas GS1
     * Reemplaza: generateBarcodeImageGS1()
     */
    private BufferedImage generateBarcodeImageGS1(String code, int widthPt, int heightPt) {
        // Usar el configurador optimizado con DPI de impresora
        BufferedImage optimizado = BarcodeConfigOptimizado.generarCodigoOptimizado(
                code, 
                widthPt, 
                heightPt,
                PRINTER_DPI
        );
        
        // Log de configuración (opcional)
        if (Boolean.getBoolean("barcode.debug")) {
            System.out.println("[GS1] " + BarcodeConfigOptimizado.obtenerInfoConfiguracion(code));
        }
        
        return optimizado;
    }



    /**
     * NUEVO MÉTODO 3: Genera código de barras estándar
     * Reemplaza: generateBarcodeImage()
     */
    private BufferedImage generateBarcodeImage(String code, int widthPt, int heightPt) {
        // Usar el configurador optimizado con DPI de impresora
        BufferedImage optimizado = BarcodeConfigOptimizado.generarCodigoOptimizado(
                code, 
                widthPt, 
                heightPt,
                PRINTER_DPI
        );
        
        // Log de configuración (opcional)
        if (Boolean.getBoolean("barcode.debug")) {
            System.out.println("[STD] " + BarcodeConfigOptimizado.obtenerInfoConfiguracion(code));
        }
        
        return optimizado;
    }


    public void exportarPdf(java.io.File destino) {
        try {
            java.io.File archivo = destino;
            if (archivo == null) {
                javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
                fc.setDialogTitle("Guardar etiqueta PDF");
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss");
                String nombreArchivo = "etiquetas_" + sdf.format(new java.util.Date()) + ".pdf";
                fc.setSelectedFile(new java.io.File(nombreArchivo));
                fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Archivos PDF", "pdf"));
                int r = fc.showSaveDialog(null);
                if (r != javax.swing.JFileChooser.APPROVE_OPTION) return;
                archivo = fc.getSelectedFile();
                if (!archivo.getName().toLowerCase().endsWith(".pdf")) {
                    archivo = new java.io.File(archivo.getAbsolutePath() + ".pdf");
                }

                // Verificar si el archivo ya existe y confirmar sobreescritura
                if (archivo.exists()) {
                    int respuesta = javax.swing.JOptionPane.showConfirmDialog(
                        null,
                        "El archivo ya existe. ¿Desea sobrescribirlo?",
                        "Confirmar sobreescritura",
                        javax.swing.JOptionPane.YES_NO_OPTION
                    );
                    if (respuesta != javax.swing.JOptionPane.YES_OPTION) {
                        return;
                    }
                }
            }

            java.awt.print.PrinterJob job = java.awt.print.PrinterJob.getPrinterJob();
            java.awt.print.PageFormat pf = buildPageFormat(job);
            double wPts = pf.getWidth();
            double hPts = pf.getHeight();

            com.itextpdf.text.Document doc = new com.itextpdf.text.Document(new com.itextpdf.text.Rectangle((float) wPts, (float) hPts));
            com.itextpdf.text.pdf.PdfAWriter writer = com.itextpdf.text.pdf.PdfAWriter.getInstance(
                    doc,
                    new java.io.FileOutputStream(archivo),
                    com.itextpdf.text.pdf.PdfAConformanceLevel.PDF_A_1B
            );
            writer.createXmpMetadata();
            doc.open();
            java.awt.color.ICC_Profile icc = java.awt.color.ICC_Profile.getInstance(java.awt.color.ColorSpace.CS_sRGB);
            writer.setOutputIntents("Custom", "", "http://www.color.org", "sRGB IEC61966-2.1", icc.getData());

            prepararEtiquetasParaImpresion();

            boolean prevUsarTira = this.usarTiraLayout;
            java.util.List<InfoEtiqueta> prevEtiquetasTira = this.etiquetasTira;
            int prevTotalTiras = this.totalTiras;
            if (!this.usarTiraLayout) {
                this.etiquetasTira = new java.util.ArrayList<>();
                for (int fila = 0; fila < tabla.getRowCount(); fila++) {
                    if (java.lang.Boolean.TRUE.equals(tabla.getValueAt(fila, 0))) {
                        int cantidad = 1;
                        try { cantidad = java.lang.Integer.parseInt(String.valueOf(tabla.getValueAt(fila,5))); } catch (Exception ignore) {}
                        for (int k=0;k<cantidad;k++) this.etiquetasTira.add(new InfoEtiqueta(fila,false));
                    }
                }
                int totalEtiquetas = this.etiquetasTira == null ? 0 : this.etiquetasTira.size();
                int cols = Math.max(1, getEtiquetasPorTira());
                int vacios = (cols - (totalEtiquetas % cols)) % cols;
                for (int i=0;i<vacios;i++) this.etiquetasTira.add(new InfoEtiqueta(-1,true));
                this.totalTiras = this.etiquetasTira.size() / cols;
                this.usarTiraLayout = true;
            }


            int pages;
            if (usarTiraLayout) {
                pages = totalTiras;
            } else {
                int total = 0;
                for (int fila = 0; fila < tabla.getRowCount(); fila++) {
                    if (Boolean.TRUE.equals(tabla.getValueAt(fila, 0))) {
                        int cant = 1; try { cant = Integer.parseInt(String.valueOf(tabla.getValueAt(fila,5))); } catch (Exception ignore) {}
                        total += cant;
                    }
                }
                pages = total;
            }

            // Verificar si hay páginas para imprimir
            if (pages <= 0) {
                javax.swing.JOptionPane.showMessageDialog(null, "No hay etiquetas para exportar.");
                doc.close();
                if (archivo.exists()) {
                    archivo.delete(); // Eliminar archivo vacío
                }
                return;
            }

            double dpi = 300.0;
            double scale = dpi / 72.0;
            int imgW = Math.max(1, (int) Math.round(wPts * scale));
            int imgH = Math.max(1, (int) Math.round(hPts * scale));

            boolean prevRotate = this.rotate180;

            for (int p = 0; p < pages; p++) {
                java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(imgW, imgH, java.awt.image.BufferedImage.TYPE_INT_RGB);
                java.awt.Graphics2D g2 = img.createGraphics();
                g2.setColor(java.awt.Color.WHITE);
                g2.fillRect(0, 0, imgW, imgH);
                g2.scale(scale, scale);
                try {
                    int res = print(g2, pf, p);
                    g2.dispose();
                    if (res == java.awt.print.Printable.PAGE_EXISTS) {
                        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                        javax.imageio.ImageIO.write(img, "PNG", baos);
                        com.itextpdf.text.Image pdfImg = com.itextpdf.text.Image.getInstance(baos.toByteArray());
                        pdfImg.scaleAbsolute((float) wPts, (float) hPts);
                        pdfImg.setAbsolutePosition(0f, 0f);
                        doc.add(pdfImg);
                        if (p < pages - 1) doc.newPage();
                    } else {
                        g2.dispose();
                    }
                } catch (Exception ex) {
                    g2.dispose();
                    javax.swing.JOptionPane.showMessageDialog(null, "Error al procesar la página " + (p+1) + ": " + ex.getMessage());
                    break; // Detener la generación si hay error
                }
            }

            this.rotate180 = prevRotate;

            doc.close();

            // Mostrar mensaje de éxito y abrir archivo si se generó correctamente
            long fileSize = archivo.length();
            String archivoPath = archivo.getAbsolutePath(); // Variable final para usar en la lambda
            if (fileSize > 0) {
                javax.swing.SwingUtilities.invokeLater(() -> {
                    javax.swing.JOptionPane.showMessageDialog(null,
                        "PDF generado exitosamente.\nTamaño: " + (fileSize / 1024) + " KB\nUbicación: " + archivoPath);

                    // Intentar abrir el archivo
                    try {
                        java.awt.Desktop.getDesktop().open(new java.io.File(archivoPath));
                    } catch (Exception e) {
                        System.out.println("No se pudo abrir el archivo automáticamente: " + e.getMessage());
                    }
                });
            } else {
                javax.swing.JOptionPane.showMessageDialog(null, "El archivo PDF generado está vacío o corrupto.");
            }
        } catch (Exception e) {
            System.err.println("Error exportando PDF: " + e.getMessage());
            e.printStackTrace();
            javax.swing.JOptionPane.showMessageDialog(null, "Error exportando PDF: " + e.getMessage());
        }
    }

    public void exportarPdfSimple(java.io.File destino) {
        try {
            java.io.File archivo = destino;
            if (archivo == null) {
                javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
                fc.setDialogTitle("Guardar etiqueta PDF");
                fc.setSelectedFile(new java.io.File("etiqueta.pdf"));
                int r = fc.showSaveDialog(null);
                if (r != javax.swing.JFileChooser.APPROVE_OPTION) return;
                archivo = fc.getSelectedFile();
                if (!archivo.getName().toLowerCase().endsWith(".pdf")) {
                    archivo = new java.io.File(archivo.getAbsolutePath() + ".pdf");
                }
            }

            java.util.List<java.awt.image.BufferedImage> imgs = generarPreviewImagenes();
            if (imgs == null || imgs.isEmpty()) return;

            com.itextpdf.text.Document doc = new com.itextpdf.text.Document(new com.itextpdf.text.Rectangle(imgs.get(0).getWidth(), imgs.get(0).getHeight()));
            com.itextpdf.text.pdf.PdfWriter writer = com.itextpdf.text.pdf.PdfWriter.getInstance(doc, new java.io.FileOutputStream(archivo));
            doc.open();
            for (int i=0;i<imgs.size();i++) {
                java.awt.image.BufferedImage img = imgs.get(i);
                com.itextpdf.text.Rectangle ps = new com.itextpdf.text.Rectangle(img.getWidth(), img.getHeight());
                doc.setPageSize(ps);
                if (i>0) doc.newPage();
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                javax.imageio.ImageIO.write(img, "PNG", baos);
                com.itextpdf.text.Image pdfImg = com.itextpdf.text.Image.getInstance(baos.toByteArray());
                pdfImg.scaleAbsolute(ps.getWidth(), ps.getHeight());
                pdfImg.setAbsolutePosition(0f, 0f);
                doc.add(pdfImg);
            }
            doc.close();
            String archivoPath = archivo.getAbsolutePath(); // Variable final para usar fuera del alcance actual
            try {
                java.awt.Desktop.getDesktop().open(new java.io.File(archivoPath));
            } catch (Exception ignore) {}
        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(null, "Error exportando PDF (simple): " + e.getMessage());
        }
    }

    public void exportarPdfConMedidas(double etiquetaAnchoMm, double etiquetaAltoMm, java.io.File destino) {
        try {
            java.io.File archivo = destino;
            if (archivo == null) {
                javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
                fc.setDialogTitle("Guardar etiqueta PDF");
                fc.setSelectedFile(new java.io.File("etiqueta.pdf"));
                int r = fc.showSaveDialog(null);
                if (r != javax.swing.JFileChooser.APPROVE_OPTION) return;
                archivo = fc.getSelectedFile();
                if (!archivo.getName().toLowerCase().endsWith(".pdf")) {
                    archivo = new java.io.File(archivo.getAbsolutePath() + ".pdf");
                }
            }

            prepararEtiquetasParaImpresion();
            boolean prevUsarTira = this.usarTiraLayout;
            java.util.List<InfoEtiqueta> prevEtiquetasTira = this.etiquetasTira;
            int prevTotalTiras = this.totalTiras;
            if (!this.usarTiraLayout) {
                this.etiquetasTira = new java.util.ArrayList<>();
                for (int fila = 0; fila < tabla.getRowCount(); fila++) {
                    if (java.lang.Boolean.TRUE.equals(tabla.getValueAt(fila, 0))) {
                        int cantidad = 1;
                        try { cantidad = java.lang.Integer.parseInt(String.valueOf(tabla.getValueAt(fila,5))); } catch (Exception ignore) {}
                        for (int k=0;k<cantidad;k++) this.etiquetasTira.add(new InfoEtiqueta(fila,false));
                    }
                }
                int totalEtiquetas = this.etiquetasTira == null ? 0 : this.etiquetasTira.size();
                int cols = Math.max(1, getEtiquetasPorTira());
                int vacios = (cols - (totalEtiquetas % cols)) % cols;
                for (int i=0;i<vacios;i++) this.etiquetasTira.add(new InfoEtiqueta(-1,true));
                this.totalTiras = this.etiquetasTira.size() / cols;
                this.usarTiraLayout = true;
            }

            double etiquetaWpts = etiquetaAnchoMm * 72.0 / 25.4;
            double etiquetaHpts = etiquetaAltoMm * 72.0 / 25.4;
            int cols = Math.max(1, getEtiquetasPorTira());
            double pageW = etiquetaWpts * cols;
            double pageH = etiquetaHpts;

            com.itextpdf.text.Document doc = new com.itextpdf.text.Document(new com.itextpdf.text.Rectangle((float) pageW, (float) pageH));
            com.itextpdf.text.pdf.PdfAWriter writer = com.itextpdf.text.pdf.PdfAWriter.getInstance(
                    doc,
                    new java.io.FileOutputStream(archivo),
                    com.itextpdf.text.pdf.PdfAConformanceLevel.PDF_A_1B
            );
            writer.createXmpMetadata();
            doc.open();
            java.awt.color.ICC_Profile icc = java.awt.color.ICC_Profile.getInstance(java.awt.color.ColorSpace.CS_sRGB);
            writer.setOutputIntents("Custom", "", "http://www.color.org", "sRGB IEC61966-2.1", icc.getData());

            java.awt.print.PrinterJob job = java.awt.print.PrinterJob.getPrinterJob();
            java.awt.print.PageFormat pf = new java.awt.print.PageFormat();
            java.awt.print.Paper p = new java.awt.print.Paper();
            p.setSize(pageW, pageH);
            p.setImageableArea(0, 0, pageW, pageH);
            pf.setPaper(p);
            pf.setOrientation(java.awt.print.PageFormat.PORTRAIT);

            int pages;
            int totalEti = etiquetasTira == null ? 0 : etiquetasTira.size();
            pages = (int) Math.ceil(Math.max(0.0, (double) totalEti) / cols);

            double dpi = 300.0;
            double scale = dpi / 72.0;
            int imgW = Math.max(1, (int) Math.round(pageW * scale));
            int imgH = Math.max(1, (int) Math.round(pageH * scale));

            boolean prevAutoFit = this.autoFit;
            double prevScaleFactor = this.scaleFactor;
            int prevRotationDegrees = this.rotationDegrees;
            double prevPadTop = this.extraPaddingTopPoints;
            double prevPadBottom = this.extraPaddingBottomPoints;
            this.autoFit = true;
            this.scaleFactor = 1.0;
            this.rotationDegrees = 0;
            this.extraPaddingTopPoints = 3.0 * 72.0 / 25.4;
            this.extraPaddingBottomPoints = 3.0 * 72.0 / 25.4;

            for (int pIndex = 0; pIndex < pages; pIndex++) {
                java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(imgW, imgH, java.awt.image.BufferedImage.TYPE_INT_RGB);
                java.awt.Graphics2D g2 = img.createGraphics();
                g2.setColor(java.awt.Color.WHITE);
                g2.fillRect(0, 0, imgW, imgH);
                g2.scale(scale, scale);
                try {
                    int res = print(g2, pf, pIndex);
                    g2.dispose();
                    if (res == java.awt.print.Printable.PAGE_EXISTS) {
                        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                        javax.imageio.ImageIO.write(img, "PNG", baos);
                        com.itextpdf.text.Image pdfImg = com.itextpdf.text.Image.getInstance(baos.toByteArray());
                        pdfImg.scaleAbsolute((float) pageW, (float) pageH);
                        pdfImg.setAbsolutePosition(0f, 0f);
                        if (pIndex > 0) doc.newPage();
                        doc.add(pdfImg);
                    }
                } catch (Exception ex) {
                    g2.dispose();
                }
            }

            this.autoFit = prevAutoFit;
            this.scaleFactor = prevScaleFactor;
            this.rotationDegrees = prevRotationDegrees;
            this.extraPaddingTopPoints = prevPadTop;
            this.extraPaddingBottomPoints = prevPadBottom;
            this.usarTiraLayout = prevUsarTira;
            this.etiquetasTira = prevEtiquetasTira;
            this.totalTiras = prevTotalTiras;
            
            doc.close();
            String archivoPath = archivo.getAbsolutePath(); // Variable final para usar fuera del alcance actual
            try {
                java.awt.Desktop.getDesktop().open(new java.io.File(archivoPath));
            } catch (Exception ignore) {}
        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(null, "Error exportando PDF (medidas): " + e.getMessage());
        }
    }

    private static class InfoEtiqueta {
        int fila; boolean esVacia;
        InfoEtiqueta(int fila, boolean vacia) { this.fila = fila; this.esVacia = vacia; }
    }
}
