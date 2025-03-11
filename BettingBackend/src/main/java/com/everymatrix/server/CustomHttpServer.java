package com.everymatrix.server;

import com.everymatrix.annotation.Route;
import com.everymatrix.config.AppConfig;
import com.everymatrix.exception.HttpServerException;
import com.everymatrix.model.RouteHandler;
import com.everymatrix.utils.HttpUtils;
import com.everymatrix.utils.LogUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * A lightweight HTTP server designed to register routes and handle HTTP requests, similar to a Spring REST controller.
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
        String requestPath = exchange.getRequestURI().getPath();
        RouteHandler targetHandler = findTargetHandler(exchange, requestPath);

        if (targetHandler == null) {
            exchange.sendResponseHeaders(404, -1);
            return;
        }

        try {
            Map<String, String> pathVariables = targetHandler.getPathVariables(exchange.getRequestURI());
            Object response = targetHandler.getControllerMethod().invoke(targetHandler.getController(), exchange, pathVariables);
            HttpUtils.sendResponse(exchange, response == null ? "" : response.toString(), 200);
        } catch (InvocationTargetException e) {
            Throwable targetException = e.getTargetException();
            if (targetException instanceof HttpServerException) {
                HttpServerException httpException = (HttpServerException) targetException;
                HttpUtils.sendResponse(exchange, httpException.getMessage(), httpException.getHttpStatusCode());
            } else if (targetException instanceof IllegalArgumentException) {
                log.severe("Parameter process failed." + e.getMessage());
                HttpUtils.sendResponse(exchange, 400);
            } else {
                log.severe("Invocation target exception: " + targetException.getMessage());
                HttpUtils.sendResponse(exchange, "Internal Server Error", 500);
            }
        } catch (IOException e) {
            log.severe("io exception occurred:" + e);
            throw e;
        } catch (Exception e) {
            log.severe("General error in handler invocation: " + e.getMessage());
            HttpUtils.sendResponse(exchange, "Internal Server Error", 500);
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


}