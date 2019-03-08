package com.exactpro.main;

public class ReferenceCycleException extends Exception {

    public ReferenceCycleException(String message){
        super(message);
    }
}
