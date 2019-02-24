package com.exactpro.test;

import com.exactpro.main.Serializator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

public class Main {

    public static void main(String[] args) {

        SomeBean bean = new SomeBean();
        bean.setSomeDecimal(new BigDecimal(42d));
        bean.setSomeInt(50);
        bean.setSomeString("hui");
        bean.setInstant(Instant.now());

        Set<Integer> set = new HashSet<>();
        set.add(6);
        bean.setSomeSet(set);

        Map<SomeBean,SomeBean> map = new HashMap<>();
        map.put(new SomeBean(),new SomeBean());
        map.put(new SomeBean(),new SomeBean());
        bean.setSomeMap(map);

        SomeBean innerBean = new SomeBean();
        innerBean.setSomeDecimal(new BigDecimal(42));
        innerBean.setSomeInt(60);
        innerBean.setSomeString("hui");
        innerBean.setSomeBean(new SomeBean());
        bean.setSomeBean(innerBean);

        ArrayList<SomeBean> list = new ArrayList<>();
        list.add(innerBean);
        bean.setSomeList(list);

        Serializator s = new Serializator();
        s.serialize(bean);
//
//        s.deserialize(null);

        try {
            System.out.println(s.isCircularObject(bean));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
