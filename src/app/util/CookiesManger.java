package app.util;

import com.sun.net.httpserver.HttpExchange;

import java.util.Map;

public class CookiesManger {
    public static String getSessionIdFromCookies(HttpExchange exchange) {
        String cookieHeader = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookieHeader == null) {
            return null;
        }

        String[] cookies = cookieHeader.split(";");
        for (String cookie : cookies) {
            String[] parts = cookie.trim().split("=");
            if (parts.length == 2 && parts[0].equals("SESSION_ID")) {
                return parts[1];
            }
        }
        return null;
    }
}
