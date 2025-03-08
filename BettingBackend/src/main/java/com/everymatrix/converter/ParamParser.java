package com.everymatrix.converter;

/**
 * Convert param in request to route function param
 */
public interface ParamParser {
    Object convertParam(String param);
}
