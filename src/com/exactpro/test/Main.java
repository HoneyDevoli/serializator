package com.exactpro.test;

import com.exactpro.main.ByteSerializer;
import com.exactpro.main.XMLSerializer;
import com.exactpro.main.exception.ReferenceCycleException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static com.exactpro.main.util.Converter.*;
import static com.exactpro.main.util.FileSaver.*;

public class Main {

    public static void main(String[] args) {

        SomeBean bean = new SomeBean();
        bean.setSomeDecimal(new BigDecimal(4233333333333333d));
        bean.setSomeInt(50);
        bean.setSomeString("string");
        bean.setInstant(Instant.now());
        bean.setSomeInteger(3222);

        Set<Integer> set = new HashSet<>();
        set.add(6);
        bean.setSomeSet(set);

        SomeBean innerBean = new SomeBean();
        innerBean.setSomeDecimal(new BigDecimal(42));
        innerBean.setSomeInt(60);
        innerBean.setSomeString("string");
        innerBean.setSomeBean(new SomeBean());
        bean.setSomeBean(innerBean);

        ArrayList<SomeBean> list = new ArrayList<>();
        list.add(innerBean);

        Map<SomeBean,SomeBean> map = new HashMap<>();
        map.put(innerBean,new SomeBean());
        map.put(new SomeBean(),new SomeBean());
        bean.setSomeMap(map);

        bean.setSomeList(new LinkedList<>(list));


        XMLSerializer xmlSer = new XMLSerializer();
        try {
            saveDocument(byteToDocument(xmlSer.serialize(bean)),"./log/originalObj.xml");

            Document readedDoc = getDocumentByPath("./log/originalObj.xml") ;
            Object deserializeObj = xmlSer.deserialize(documentToByte(readedDoc));

            saveDocument(byteToDocument(xmlSer.serialize(deserializeObj)),"./log/deserialObj.xml");
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


        try {
            ByteSerializer byteSer = new ByteSerializer();
            var bytes = byteSer.serialize(bean);

            saveDocument(byteToDocument(xmlSer.serialize(byteSer.deserialize(bytes))),"./log/bytexml.xml");

        } catch (ReferenceCycleException e) {
            System.out.println("Find cycle referens. Error of serialisation");
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
    }
}
