package com.everymatrix.utils;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class UrlUtils {

    public static Map<String, String> getQueryParams(URI uri) {
        Map<String, String> queryPairs = new HashMap<>();
        String query = uri.getQuery();

        if (query != null) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                int idx = pair.indexOf("=");
                String key = idx > 0 ? pair.substring(0, idx) : pair;
                String value = idx > 0 && pair.length() > idx + 1 ? pair.substring(idx + 1) : null;
                queryPairs.put(key, value);
            }
        }

        return queryPairs;
    }
}
