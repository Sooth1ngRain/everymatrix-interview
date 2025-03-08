package com.everymatrix.converter;

public class LongParser implements ParamParser {
    @Override
    public Object convertParam(String param) {
        return Long.parseLong(param);
    }
}
