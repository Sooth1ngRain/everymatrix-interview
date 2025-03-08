package com.everymatrix.server;

import com.everymatrix.annotation.PathVariable;
import com.everymatrix.annotation.RequestBody;
import com.everymatrix.annotation.RequestParam;
import com.everymatrix.annotation.Route;
import com.everymatrix.config.AppConfig;
import com.everymatrix.converter.ParamParser;
import com.everymatrix.exception.HttpServerException;
import com.everymatrix.model.RouteHandler;
import com.everymatrix.utils.HttpUtils;
import com.everymatrix.utils.IOUtils;
import com.everymatrix.utils.LogUtils;
import com.everymatrix.utils.UrlUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * A lightweight HTTP server designed to register routes and handle HTTP requests, similar to a Spring REST controller.
 * This implementation utilizes custom annotations such as {@link RequestParam}, {@link RequestBody}, {@link PathVariable} to simplify the controller.
 * The {@link Route} annotation mimics the spring @RequestMapping, providing a direct way to define route mappings.
 * <p>
 * Usage Example:
 * <p>
 * CustomHttpServer server = new CustomHttpServer();
 * server.registerRoutes(controllerObject);
 * server.startServer(port);
 */
public class CustomHttpServer {

    private final Logger log = LogUtils.getLogger();
    private final List<RouteHandler> routeHandlers = new ArrayList<>();
    private final Map<Class<? extends ParamParser>, ParamParser> converters = new HashMap<>();

    public CustomHttpServer() {
    }

    /**
     * Gathers invocation metadata information for each route function.
     */
    public void registerRoutes(Object controller) {
        for (Method method : controller.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(Route.class)) {
                Route route = method.getAnnotation(Route.class);
                routeHandlers.add(new RouteHandler(route.method(), route.path(), method, controller));
                log.info("Registered route: " + route.path() + " [" + route.method() + "]");
            }
        }
    }

    /**
     * Starts the HTTP server
     */
    public void startServer(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", this::dispatch);

        server.setExecutor(new ThreadPoolExecutor(
                AppConfig.serverThreadPoolCorePoolSize,
                AppConfig.serverThreadPoolMaxPoolSize,
                AppConfig.serverThreadPoolKeepAliveSeconds,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                new ThreadPoolExecutor.AbortPolicy()
        ));

        log.info("Server started at: http://localhost:" + port);
        server.start();
    }

    /**
     * Dispatches requests to the appropriate route handler based on the registered routes.
     */
    private void dispatch(HttpExchange exchange) throws IOException {
        try {
            String requestPath = exchange.getRequestURI().getPath();
            RouteHandler targetHandler = findTargetHandler(exchange, requestPath);

            if (targetHandler == null) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }

            invokeHandler(exchange, targetHandler);
        } finally {

        }


    }

    /**
     * Finds the matching target handler from registered route handlers based on request path and method
     *
     * @return A matched RouteHandler or null if no match is found
     */
    private RouteHandler findTargetHandler(HttpExchange exchange, String requestPath) {
        for (RouteHandler routeHandler : routeHandlers) {
            Matcher matcher = routeHandler.getPathPattern().matcher(requestPath);
            if (matcher.matches() && routeHandler.getHttpMethod().toString().equalsIgnoreCase(exchange.getRequestMethod())) {
                return routeHandler;
            }
        }
        return null;
    }

    /**
     * Invokes the handler method for the target route.
     *
     * @param targetHandler The matched RouteHandler object
     */
    private void invokeHandler(HttpExchange exchange, RouteHandler targetHandler) throws IOException {
        try {
            Map<String, String> pathVariables = targetHandler.getPathVariables(exchange.getRequestURI());
            String requestBody = IOUtils.readAllBytes(exchange.getRequestBody());
            Map<String, String> urlParams = UrlUtils.getQueryParams(exchange.getRequestURI());

            Object[] args = processArguments(targetHandler, pathVariables, requestBody, urlParams);
            Object response = targetHandler.getControllerMethod().invoke(targetHandler.getController(), args);

            HttpUtils.sendResponse(exchange, response == null ? "" : response.toString(), 200);
        } catch (InvocationTargetException e) {
            handleInvocationException(exchange, e);
        } catch (IllegalArgumentException e) {
            log.severe("Parameter process failed." + e.getMessage());
            HttpUtils.sendResponse(exchange, 400);
        } catch (IOException e) {
            log.severe("io exception occurred:" + e);
            throw e;
        } catch (Exception e) {
            log.severe("General error in handler invocation: " + e.getMessage());
            sendInternalServerError(exchange);
        }
    }

    /**
     * Processes the arguments for the handler function, resolving annotated parameters.
     *
     * @param targetHandler The matched RouteHandler object
     * @return An array of objects representing the function's parameters
     */
    private Object[] processArguments(RouteHandler targetHandler, Map<String, String> pathVariables, String requestBody, Map<String, String> urlParams) {
        return Arrays.stream(targetHandler.getControllerMethod().getParameters())
                .map(param -> resolveParameter(param, pathVariables, requestBody, urlParams))
                .toArray();
    }

    /**
     * Resolves a parameter based on its annotation
     *
     * @param param The parameter to resolve
     * @return route function parameter value
     */
    private Object resolveParameter(java.lang.reflect.Parameter param, Map<String, String> pathVariables, String requestBody, Map<String, String> urlParams) {
        if (param.isAnnotationPresent(PathVariable.class)) {
            return convertParam(param.getAnnotation(PathVariable.class), pathVariables.get(param.getAnnotation(PathVariable.class).value()));
        }
        if (param.isAnnotationPresent(RequestParam.class)) {
            return convertParam(param.getAnnotation(RequestParam.class), urlParams.get(param.getAnnotation(RequestParam.class).value()));
        }
        if (param.isAnnotationPresent(RequestBody.class)) {
            return convertParam(param.getAnnotation(RequestBody.class), requestBody);
        }
        return null;
    }

    /**
     * Converts parameter type with specific annotation（PathVariable，RequestParam，RequestBody） using the giving converter
     */
    private Object convertParam(Annotation annotation, String rawValue) {
        try {
            ParamParser parser = getParser(annotation);
            return parser.convertParam(rawValue);
        } catch (InstantiationException | IllegalAccessException e) {
            log.severe("Error in parameter conversion: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieves or instantiates a ParamParser for the given annotation
     *
     * @param annotation The annotation defining the parameter
     */
    private ParamParser getParser(Annotation annotation) throws InstantiationException, IllegalAccessException {
        Class<? extends ParamParser> converterClass = annotation instanceof PathVariable ? ((PathVariable) annotation).converter() :
                annotation instanceof RequestParam ? ((RequestParam) annotation).converter() :
                        ((RequestBody) annotation).converter();

        converters.putIfAbsent(converterClass, converterClass.newInstance());
        return converters.get(converterClass);
    }

    /**
     * Handles exceptions in function invocation, sending appropriate HTTP responses.
     */
    private void handleInvocationException(HttpExchange exchange, InvocationTargetException e) throws IOException {
        Throwable targetException = e.getTargetException();
        if (targetException instanceof HttpServerException) {
            HttpServerException httpException = (HttpServerException) targetException;
            HttpUtils.sendResponse(exchange, httpException.getMessage(), httpException.getHttpStatusCode());
        } else {
            log.severe("Invocation target exception: " + targetException.getMessage());
            sendInternalServerError(exchange);
        }
    }

    private void sendInternalServerError(HttpExchange exchange) throws IOException {
        HttpUtils.sendResponse(exchange, "Internal Server Error", 500);
    }
}