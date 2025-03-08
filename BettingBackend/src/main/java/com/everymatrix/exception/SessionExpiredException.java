package com.everymatrix.exception;

public class SessionExpiredException extends HttpServerException{

    public SessionExpiredException(){
        super(401 , "session expired!" , null);
    }
}
