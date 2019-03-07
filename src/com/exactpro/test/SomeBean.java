package com.exactpro.test;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

public class SomeBean implements Serializable {

    private String someString;
    private float someFloat;
    private boolean aBoolean;
    private Integer someInteger;
    private BigDecimal someDecimal;
    private LinkedList<SomeBean> someList;
    private Map<SomeBean,SomeBean> someMap;
    private Set<Integer> someSet;
    private SomeBean someBean;
    private int someInt;
    private Instant instant;

    public SomeBean(){

    }

    public String getSomeString() {
        return someString;
    }

    public void setSomeString(String someString) {
        this.someString = someString;
    }

    public int getSomeInt() {
        return someInt;
    }

    public void setSomeInt(int someInt) {
        this.someInt = someInt;
    }

    public BigDecimal getSomeDecimal() {
        return someDecimal;
    }

    public void setSomeDecimal(BigDecimal someDecimal) {
        this.someDecimal = someDecimal;
    }

    public List<SomeBean> getSomeList() {
        return someList;
    }

    public void setSomeList(LinkedList<SomeBean> someList) {
        this.someList = someList;
    }

    public SomeBean getSomeBean() {
        return someBean;
    }

    public void setSomeBean(SomeBean someBean) {
        this.someBean = someBean;
    }

    public Instant getInstant() {
        return instant;
    }

    public void setInstant(Instant instant) {
        this.instant = instant;
    }

    public Map<SomeBean, SomeBean> getSomeMap() {
        return someMap;
    }

    public void setSomeMap(Map<SomeBean, SomeBean> someMap) {
        this.someMap = someMap;
    }

    public Set<Integer> getSomeSet() {
        return someSet;
    }

    public void setSomeSet(Set<Integer> someSet) {
        this.someSet = someSet;
    }

    public Integer getSomeInteger() {
        return someInteger;
    }

    public void setSomeInteger(Integer someInteger) {
        this.someInteger = someInteger;
    }

    public float getSomeFloat() {
        return someFloat;
    }

    public void setSomeFloat(float someFloat) {
        this.someFloat = someFloat;
    }
}
