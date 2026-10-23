package tech.petrepopescu.common.utils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.lang.Nullable;
import org.springframework.web.util.WebUtils;

/**
 * Utility class for retrieving and building secure cookies
 */
public final class CookieUtil {
    private CookieUtil() {
        // Utility class
    }

    /**
     * Return the cookie with the specified name from the request
     * @param request The HTTP Servlet Request
     * @param name The cookie name
     * @return The cookie, if present
     */
    @Nullable
    public static Cookie getCookie(HttpServletRequest request, String name) {
        return WebUtils.getCookie(request, name);
    }

    /**
     * Build an HTTP Only and Secure cookie that can be added to the response
     * @param name The cookie name
     * @param value The value that should be stored on the cookie
     * @return THe cookie
     */
    public static Cookie buildSecureCookie(String name, String value) {
        Cookie newCookie = new Cookie(name, value);
        newCookie.setHttpOnly(true);
        newCookie.setSecure(true);
        return newCookie;
    }
}