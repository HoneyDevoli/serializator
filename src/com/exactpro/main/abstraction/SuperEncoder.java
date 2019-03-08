package com.exactpro.main.Interface;

import com.exactpro.main.exception.ReferenceCycleException;

public interface SuperEncoder {
    byte[] serialize(Object anyBean) throws ReferenceCycleException;
    Object deserialize(byte[] data);
}
