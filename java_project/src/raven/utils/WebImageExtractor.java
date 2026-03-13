package raven.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Utility class to extract images from web pages.
 * Handles HTML parsing via Regex (robust enough for simple scraping) 
 * and handles SSL certificates leniently to avoid handshake errors.
 */
public class WebImageExtractor {

    // Regex to find img tags and their src attributes
    // Matches <img ... src="URL" ... > or <img ... src='URL' ... >
    private static final Pattern IMG_TAG_PATTERN = Pattern.compile("<img[^>]+src\\s*=\s*['\"]([^'\"]+)['\"][^>]*>", Pattern.CASE_INSENSITIVE);
    
    // Regex to find other potential image links (e.g. data-src, href ending in image)
    private static final Pattern IMAGE_URL_PATTERN = Pattern.compile("(https?://[^\\s'\"]+\\.(?:jpg|jpeg|png|webp|gif))", Pattern.CASE_INSENSITIVE);

    /**
     * Extracts image URLs from a given web page URL.
     * @param urlString The web page URL
     * @return List of absolute image URLs found
     */
    public static List<String> extractImages(String urlString) throws Exception {
        String html = fetchHtml(urlString);
        Set<String> imageUrls = new HashSet<>();
        
        // 1. Find standard <img src="...">
        Matcher imgMatcher = IMG_TAG_PATTERN.matcher(html);
        while (imgMatcher.find()) {
            String src = imgMatcher.group(1);
            String absoluteUrl = resolveUrl(urlString, src);
            if (isValidImageExtension(absoluteUrl)) {
                imageUrls.add(absoluteUrl);
            }
        }
        
        // 2. Find direct image links in the text (backup for lazy loading or other attributes)
        Matcher urlMatcher = IMAGE_URL_PATTERN.matcher(html);
        while (urlMatcher.find()) {
            String match = urlMatcher.group(1);
            imageUrls.add(match);
        }
        
        return new ArrayList<>(imageUrls);
    }

    private static String fetchHtml(String urlString) throws Exception {
        // Trust all certificates to avoid SSL issues with some sites
        trustAllCertificates();
        
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        
        // Set headers to mimic a real browser (important for sites like Adidas)
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        conn.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
        conn.setInstanceFollowRedirects(true);
        
        int status = conn.getResponseCode();
        if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM) {
            String newUrl = conn.getHeaderField("Location");
            return fetchHtml(newUrl);
        }
        
        if (status != HttpURLConnection.HTTP_OK) {
            throw new Exception("HTTP Error: " + status);
        }
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
            return sb.toString();
        }
    }

    private static String resolveUrl(String baseUrl, String relUrl) {
        try {
            if (relUrl.startsWith("http")) {
                return relUrl;
            }
            if (relUrl.startsWith("//")) {
                return "https:" + relUrl;
            }
            
            URL base = new URL(baseUrl);
            return new URL(base, relUrl).toString();
        } catch (Exception e) {
            return relUrl;
        }
    }

    private static boolean isValidImageExtension(String url) {
        String lower = url.toLowerCase();
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp");
    }

    private static void trustAllCertificates() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
                }
            };
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            // Ignore
        }
    }
}
