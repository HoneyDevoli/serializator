package com.exactpro.main;

public interface SuperEncoder {
    byte[] serialize(Object anyBean);
    Object deserialize(byte[] data);
}
