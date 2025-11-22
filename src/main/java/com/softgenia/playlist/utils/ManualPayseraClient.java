package com.softgenia.playlist.utils;

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

    // This method replaces WebToPay.buildRequestUrl()
    public static String buildRequestUrl(Map<String, String> params, String signPassword) {
        try {
            String dataString = params.entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                    .collect(Collectors.joining("&"));

            String data = Base64.getUrlEncoder().withoutPadding().encodeToString(dataString.getBytes(StandardCharsets.UTF_8));
            String sign = generateMd5(data + signPassword);

            return PAYSERA_PAY_URL + "?data=" + data + "&sign=" + sign;
        } catch (Exception e) {
            throw new RuntimeException("Failed to manually build Paysera request URL", e);
        }
    }

    // This method replaces WebToPay.parseResponse()
    public static Map<String, String> parseResponse(Map<String, String[]> parameterMap, String signPassword) {
        String data = parameterMap.get("data")[0];
        String ss2 = parameterMap.get("ss2")[0];

        String expectedSign = generateMd5(data + signPassword);

        if (!expectedSign.equals(ss2)) {
            throw new SecurityException("Invalid Paysera callback signature! The request might be a fake.");
        }

        byte[] decodedBytes = Base64.getUrlDecoder().decode(data);
        String decodedDataString = new String(decodedBytes, StandardCharsets.UTF_8);

        Map<String, String> responseParams = new HashMap<>();
        for (String pair : decodedDataString.split("&")) {
            int idx = pair.indexOf("=");
            try {
                responseParams.put(
                        URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8),
                        URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8)
                );
            } catch (Exception e) {
                // Ignore malformed pairs
            }
        }
        return responseParams;
    }

    private static String generateMd5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate MD5 signature", e);
        }
    }
}