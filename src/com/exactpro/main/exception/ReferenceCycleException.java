package com.exactpro.main.exception;

public class ReferenceCycleException extends Exception {

    public ReferenceCycleException(String message){
        super(message);
    }
}
