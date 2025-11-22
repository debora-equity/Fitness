package com.softgenia.playlist.utils;

import org.springframework.lang.Nullable;

import java.util.Optional;

public class StringUtils {
    public static String generateRandomString(Integer length) {
        StringBuilder sb = new StringBuilder(length);
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        for (int i = 0; i < length; i++) {
            int index = (int) (characters.length() * Math.random());
            sb.append(characters.charAt(index));
        }

        return sb.toString();
    }

    public static Optional<String> getExtensionByStringHandling(String filename) {
        return Optional.ofNullable(filename)
                .filter(f -> f.contains("."))
                .map(f -> f.substring(filename.lastIndexOf(".") + 1));
    }

    /**
     * @param filter filter to add to a LIKE sql filter
     * @return correctly formatted with '%' only if string is not blank or null
     */
    public static String getFormattedStringFilter(String filter) {
        return filter != null && !filter.isBlank() ? "%" + filter + "%" : null;
    }

    public static String replaceIfEmpty(@Nullable String string, String replacement) {
        return string != null && !string.isBlank() ? string : replacement;
    }

}
