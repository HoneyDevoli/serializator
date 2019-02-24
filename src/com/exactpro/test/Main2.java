package com.exactpro.test;

import com.exactpro.main.Serializator;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Main2 {
    public static void main(String[] args) {
        try{

            SomeBean bean = new SomeBean();
            bean.setSomeDecimal(new BigDecimal(42d));
            bean.setSomeInt(322);
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
            innerBean.setSomeInt(322);
            innerBean.setSomeString("hui");
            innerBean.setSomeBean(new SomeBean());
            bean.setSomeBean(bean);

            FileOutputStream fos = new FileOutputStream("student3.xml");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeUTF(bean.toString());
            oos.writeObject((Object)bean);
            oos.close();
            fos.close();
        }catch(Exception e){
            System.out.println(e);
        }

        SomeBean st = null;
        try{
            FileInputStream fis = new FileInputStream("student3.xml");
            ObjectInputStream ois = new ObjectInputStream(fis);
            st = (SomeBean) ois.readObject();
            Serializator s = new Serializator();
            s.serialize(st);
        }catch(Exception e){
            System.out.println(e);
        }
        System.out.println(st.getSomeInt());
        System.out. println(st.getSomeMap());
    }
    }

