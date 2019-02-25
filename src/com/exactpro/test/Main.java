package com.exactpro.test;

import com.exactpro.main.ReferenceCycleException;
import com.exactpro.main.Serializator;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static com.exactpro.main.Serializator.*;

public class Main {

    public static void main(String[] args) {

        SomeBean bean = new SomeBean();
        bean.setSomeDecimal(new BigDecimal(42d));
        bean.setSomeInt(50);
        bean.setSomeString("string");
        bean.setInstant(Instant.now());
        bean.setSomeInteger(3222);

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
        innerBean.setSomeString("string");
        innerBean.setSomeBean(new SomeBean());
        bean.setSomeBean(innerBean);

        ArrayList<SomeBean> list = new ArrayList<>();
        list.add(innerBean);

        bean.setSomeList(new LinkedList<>(list));

        Serializator s = new Serializator();

        try {
            saveDocument(byteToDocument(s.serialize(bean)),"./log/originalObj.xml");

            Document readedDoc = getDocumentByPath("./log/originalObj.xml") ;
            Object deserializeObj = s.deserialize(documentToByte(readedDoc));

            saveDocument(byteToDocument(s.serialize(deserializeObj)),"./log/deserialObj.xml");
        } catch (ReferenceCycleException e) {
            System.out.println("Find cycle referens. Error of serialisation");
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }



    }
}
