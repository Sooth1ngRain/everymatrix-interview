package com.everymatrix.model;


import java.lang.reflect.Method;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RouteHandler {
    private final HttpMethod httpMethod;
    private final String path; // e.g., /{customerId}/buy/{orderId}
    private final Method controllerMethod;
    private final Object controller;

    public RouteHandler(HttpMethod httpMethod, String path, Method controllerMethod, Object controller) {
        this.httpMethod = httpMethod;
        this.path = path;
        this.controllerMethod = controllerMethod;
        this.controller = controller;
    }

    public Map<String, String> getPathVariables(URI uri) {
        Map<String, String> pathVariables = new HashMap<>();

        // Create regex pattern for the path template
        String pathRegex = getPathRegex();
        Pattern pattern = Pattern.compile(pathRegex);

        // Match the URI path to the regex pattern
        Matcher matcher = pattern.matcher(uri.getPath());

        if (matcher.matches()) {
            // Extract variable names
            String[] variableNames = extractVariableNames(path);

            // Populate path variables map with actual values from the URI
            for (int i = 0; i < variableNames.length; i++) {
                pathVariables.put(variableNames[i], matcher.group(i + 1));
            }
        }

        return pathVariables;
    }

    public Pattern getPathPattern(){
        return Pattern.compile(this.getPathRegex());
    }

    private String[] extractVariableNames(String path) {
        // Extract variable names enclosed in {}
        Pattern pattern = Pattern.compile("\\{([^/}]+)\\}");
        Matcher matcher = pattern.matcher(path);

        String[] variableNames = new String[matcher.groupCount()];
        int index = 0;

        while (matcher.find()) {
            variableNames[index++] = matcher.group(1);
        }

        return variableNames;
    }

    private String getPathRegex() {
        // Convert path template like /{customerId}/buy/{orderId} to regex /([^/]+)/buy/([^/]+)
        return this.path.replaceAll("\\{([^/}]+)\\}", "([^/]+)");
    }


    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    public Method getControllerMethod() {
        return controllerMethod;
    }

    public Object getController() {
        return controller;
    }
}
