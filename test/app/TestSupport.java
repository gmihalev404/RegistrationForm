package app;

import app.service.CaptchaService;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class TestSupport {
    private TestSupport() {}

    static void inject(Object target, String fieldName, Object replacement) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, replacement);
    }

    static String captchaAnswer(CaptchaService.CaptchaChallenge challenge) {
        String[] parts = challenge.question().split(" [+] ");
        return String.valueOf(Integer.parseInt(parts[0]) + Integer.parseInt(parts[1]));
    }

    static MockHttp request(String method, String path, String cookie, String body) {
        HttpExchange exchange = mock(HttpExchange.class);
        Headers requestHeaders = new Headers();
        Headers responseHeaders = new Headers();
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        if (cookie != null) {
            requestHeaders.set("Cookie", cookie);
        }
        when(exchange.getRequestMethod()).thenReturn(method);
        when(exchange.getRequestURI()).thenReturn(URI.create(path));
        when(exchange.getRequestHeaders()).thenReturn(requestHeaders);
        when(exchange.getResponseHeaders()).thenReturn(responseHeaders);
        when(exchange.getRequestBody()).thenReturn(
                new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
        when(exchange.getResponseBody()).thenReturn(output);
        return new MockHttp(exchange, responseHeaders, output);
    }

    record MockHttp(HttpExchange exchange, Headers headers, ByteArrayOutputStream body) {
        String content() {
            return body.toString(StandardCharsets.UTF_8);
        }
    }
}
