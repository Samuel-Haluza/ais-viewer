package sk.ukf.aisviewer.util;

import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for managing HTTP session cookies for AIS2 requests.
 */
public class CookieManager {

    private final java.net.CookieManager cookieManager;

    public CookieManager() {
        this.cookieManager = new java.net.CookieManager();
        this.cookieManager.setCookiePolicy(java.net.CookiePolicy.ACCEPT_ALL);
    }

    public java.net.CookieManager getJavaCookieManager() {
        return cookieManager;
    }

    public CookieStore getCookieStore() {
        return cookieManager.getCookieStore();
    }

    /**
     * Returns all cookies as a formatted Cookie header string.
     */
    public String getCookieHeader(URI uri) {
        List<HttpCookie> cookies = cookieManager.getCookieStore().get(uri);
        if (cookies == null || cookies.isEmpty()) {
            return "";
        }
        return cookies.stream()
                .map(c -> c.getName() + "=" + c.getValue())
                .collect(Collectors.joining("; "));
    }

    /**
     * Checks whether we have a valid session (JSESSIONID cookie present).
     */
    public boolean hasSession() {
        List<HttpCookie> allCookies = cookieManager.getCookieStore().getCookies();
        return allCookies.stream()
                .anyMatch(c -> c.getName().equalsIgnoreCase("JSESSIONID"));
    }

    /**
     * Clears all stored cookies (logout).
     */
    public void clearCookies() {
        cookieManager.getCookieStore().removeAll();
    }

    /**
     * Returns all cookie names for debugging.
     */
    public List<String> getCookieNames() {
        return cookieManager.getCookieStore().getCookies().stream()
                .map(HttpCookie::getName)
                .collect(Collectors.toList());
    }
}
