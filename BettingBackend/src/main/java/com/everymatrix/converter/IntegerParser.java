package com.everymatrix.converter;

public class IntegerParser implements ParamParser {
    @Override
    public Object convertParam(String param) {
        return Integer.parseInt(param);
    }
}
