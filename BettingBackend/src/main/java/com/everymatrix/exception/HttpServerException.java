package com.everymatrix.exception;

public class HttpServerException extends RuntimeException{

    private final int HttpStatusCode;

    public HttpServerException(int HttpStatusCode , String message , Throwable t){
        super(message , t);
        this.HttpStatusCode = HttpStatusCode;
    }

    public int getHttpStatusCode() {
        return HttpStatusCode;
    }
}
