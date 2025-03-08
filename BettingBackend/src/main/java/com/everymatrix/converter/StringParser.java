package com.everymatrix.converter;

public class StringParser implements ParamParser {
    @Override
    public Object convertParam(String param) {
        return param;
    }
}
