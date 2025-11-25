package com.softgenia.playlist.utils; // Make sure package matches your structure

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ManualPayseraClient {

    private static final String PAYSERA_PAY_URL = "https://www.paysera.com/pay/";

    public static String buildRequestUrl(Map<String, String> params, String signPassword) {
        try {
            // 1. Build the URL-encoded query string
            String queryString = params.entrySet().stream()
                    .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                    .collect(Collectors.joining("&"));

            // --- DEBUGGING PRINT ---
            System.out.println("DEBUG: Raw Query String before Base64: " + queryString);
            // -----------------------

            // 2. Use URL-Safe Base64 Encoding (This is the FIX)
            // webtopay-lib-java uses URL-safe encoding.
            // We allow padding ('=') because browsers handle it fine, or we can strip it if Paysera is strict.
            // Usually, standard URL encoder is best.
            String data = Base64.getUrlEncoder().encodeToString(queryString.getBytes(StandardCharsets.UTF_8));

            // --- DEBUGGING PRINT ---
            System.out.println("DEBUG: Generated 'data' param: " + data);
            // -----------------------

            // 3. Generate MD5 signature
            String sign = generateMd5(data + signPassword);

            return PAYSERA_PAY_URL + "?data=" + data + "&sign=" + sign;

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Paysera URL", e);
        }
    }

    public static Map<String, String> parseResponse(Map<String, String[]> requestParams, String signPassword) {
        try {
            String data = getParam(requestParams, "data");
            String ss1 = getParam(requestParams, "ss1");
            String ss2 = getParam(requestParams, "ss2"); // Paysera usually sends ss1 or ss2

            if (data == null) {
                throw new SecurityException("Missing 'data' parameter.");
            }

            // Determine which signature to check (usually ss1 for callback)
            String targetSign = (ss2 != null) ? ss2 : ss1;
            if (targetSign == null) {
                throw new SecurityException("Missing signature (ss1 or ss2).");
            }

            // Validate Signature
            String expectedSign = generateMd5(data + signPassword);
            if (!expectedSign.equals(targetSign)) {
                throw new SecurityException("Invalid Paysera signature.");
            }

            // Decode data using URL decoder
            byte[] decodedBytes = Base64.getUrlDecoder().decode(data);
            String decodedString = new String(decodedBytes, StandardCharsets.UTF_8);

            Map<String, String> result = new HashMap<>();
            for (String pair : decodedString.split("&")) {
                int idx = pair.indexOf("=");
                if (idx > 0) {
                    String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                    String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                    result.put(key, value);
                }
            }
            return result;

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Paysera callback", e);
        }
    }

    private static String encode(String value) {
        // Important: Paysera expects spaces to be '%20', not '+' in some contexts,
        // but standard URLEncoder ('+') usually works.
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String getParam(Map<String, String[]> map, String key) {
        if (map.containsKey(key) && map.get(key).length > 0) {
            return map.get(key)[0];
        }
        return null;
    }

    private static String generateMd5(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : digest) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}