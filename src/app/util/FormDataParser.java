package app.util;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class FormDataParser {

    public static Map<String, String> parse(String requestBody) {
        Map<String, String> formData = new HashMap<>();
        if (requestBody == null || requestBody.trim().isEmpty()) {
            return formData;
        }

        String[] pairs = requestBody.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2) {
                String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
                String value = URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                formData.put(key, value);
            } else if (keyValue.length == 1) {
                String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
                formData.put(key, "");
            }
        }

        return formData;
    }
}