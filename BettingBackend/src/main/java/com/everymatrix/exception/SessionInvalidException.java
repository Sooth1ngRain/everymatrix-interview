package com.everymatrix.exception;

public class SessionInvalidException extends HttpServerException{

    public SessionInvalidException(){
        super(401 , "session invalid!" , null);
    }
}
