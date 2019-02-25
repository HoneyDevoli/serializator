package com.exactpro.main;

public interface SuperEncoder {
    byte[] serialize(Object anyBean) throws  ReferenceCycleException;
    Object deserialize(byte[] data);
}
