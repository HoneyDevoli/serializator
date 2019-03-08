package com.exactpro.main;

import com.exactpro.main.exception.ReferenceCycleException;
import com.exactpro.main.abstraction.SuperEncoder;
import com.exactpro.main.util.Converter;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;

public class XMLSerializer implements SuperEncoder {
    private Document document;
    private LinkedList<Object> callStack = new LinkedList<>();


    @Override
    public byte[] serialize(Object anyBean) throws ReferenceCycleException {
        DocumentBuilderFactory dbf;


        try {
            if(isCircularObject(anyBean)){
                throw  new ReferenceCycleException("This bean have a circular field "+ anyBean.getClass());
            }

            dbf = DocumentBuilderFactory.newInstance();
            document = dbf.newDocumentBuilder().newDocument();
            document.appendChild(iterateNodes(anyBean));

            return Converter.documentToByte(document);

        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }

        return new byte[0];
    }

    @Override
    public Object deserialize(byte[] data) {

        try {
            Document document = Converter.byteToDocument(data);
            String classname = document.getDocumentElement().getTagName();
            NodeList chiledNodes = document.getDocumentElement().getChildNodes();

            return  deserializeNodeList(chiledNodes,classname);

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
        return null;
    }


    private Element iterateNodes(Object bean) throws IllegalAccessException {
        Class classObject = bean.getClass();
        Element rootNode = document.createElement(classObject.getName());

        for (Field field : classObject.getDeclaredFields()) {

            field.setAccessible(true);

            String nameOfField = field.getName();
            Class<?> typeOfField = field.getType();
            Object valueOfField = field.get(bean);

            Element innerNode = document.createElement(nameOfField);

            if(valueOfField == null){
                innerNode.setTextContent("null");

            } else if( typeOfField.isPrimitive() ){
                innerNode.setTextContent(valueOfField.toString());

            } else if(valueOfField instanceof Number || valueOfField instanceof String || valueOfField instanceof Instant) {
                innerNode.setTextContent(valueOfField.toString());

            } else if(valueOfField instanceof Collection){
                for (Object element : (Collection)valueOfField) {
                    innerNode.appendChild(getNodeFromElementCollection(element));
                }

            } else if(valueOfField instanceof Map){
                Map map= (Map)valueOfField;
                for (Object elementEntry : map.entrySet()) {
                    Map.Entry entry = (Map.Entry)elementEntry;

                    innerNode.appendChild(getNodeFromElementMap(entry));
                }

            } else {
                innerNode.appendChild(iterateNodes(valueOfField));
            }
            rootNode.appendChild(innerNode);
        }
        return  rootNode;

    }

    private Element getNodeFromElementCollection(Object element) throws IllegalAccessException {

        Class classObject = element.getClass();
        String nameOfField= classObject.getName();
        Element node = document.createElement(nameOfField);
        if(element == null){
            node.setTextContent("null");

        } else if(element instanceof Number || element instanceof String || element instanceof Instant) {
            node.setTextContent(element.toString());

        } else if(element instanceof Collection){
            for (Object o : (Collection)element) {
                node.appendChild(getNodeFromElementCollection(o));
            }

        } else if(element instanceof Map){
            Map map= (Map)element;
            for (Object elementEntry : map.entrySet()) {
                Map.Entry entry = (Map.Entry)elementEntry;

                node.appendChild(getNodeFromElementMap(entry));
            }

        } else {
            return iterateNodes(element);
        }

        return node;
    }

    private Element getNodeFromElementMap(Map.Entry entry) throws IllegalAccessException {
        Element entryNode = document.createElement("Entry");

        entryNode.appendChild(getNodeFromElementCollection(entry.getKey()));
        entryNode.appendChild(getNodeFromElementCollection(entry.getValue()));
        return entryNode;
        }

    public boolean isCircularObject(Object node) throws IllegalAccessException {
        Class classObject = node.getClass();

        var fields = classObject.getDeclaredFields();

        callStack.push(node);
        for (Field field : fields) {
            field.setAccessible(true);

            Object valueOfField = field.get(node);

            if(!isSDKClass(valueOfField)){
                if(callStack.indexOf(valueOfField)>=0){
                    return true;
                }

                if(valueOfField instanceof Collection) {
                    if(iterateCollection((Collection)valueOfField)){
                        return true;
                    }
                } else if(valueOfField instanceof Map) {
                    Map map = (Map)valueOfField;
                    Collection keys = map.keySet();
                    Collection values = map.values();

                    if(iterateCollection(keys) || iterateCollection(values)) {
                        return true;
                    }
                } else {
                    if (isCircularObject(valueOfField)) {
                        return true;
                    }
                }
            }
        }
        callStack.pop();
        return false;
    }


    private boolean isSDKClass(Object object){
        return object == null ||
            object instanceof Boolean ||
            object.getClass().isPrimitive()  ||
            object instanceof Number ||
            object instanceof String ||
            object instanceof Instant;
    }

    private boolean iterateCollection(Collection collection) throws IllegalAccessException {
        for (Object element : collection) {
            if (!isSDKClass(element)) {
                if (callStack.indexOf(element) >= 0) {
                    return true;
                } else {
                    if (isCircularObject(element)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private Object deserializeNodeList(NodeList nodeList, String className ) throws NoSuchMethodException, IllegalAccessException,
            InvocationTargetException, InstantiationException, ClassNotFoundException, NoSuchFieldException, ParseException {

        Class<?> parseClass = Class.forName(className);
        var newObject = parseClass.getConstructor().newInstance();

        for (int i = 0 ;i<nodeList.getLength(); i++) {
            Node item = nodeList.item(i);

            if (item.getNodeType() == Node.ELEMENT_NODE) {
                String elem = item.getNodeName();
                String value = item.getTextContent();

                Field field = newObject.getClass().getDeclaredField(elem);
                field.setAccessible(true);
                Class<?> typeOfField = field.getType();

                if(value.equals("null")) {
                } else if(Number.class.isAssignableFrom(typeOfField) || String.class.isAssignableFrom(typeOfField)) {
                    Object number = Class.forName(typeOfField.getName()).getConstructor(new Class[]{String.class}).newInstance(value);

                    field.set(newObject, number);
                } else if (typeOfField.isPrimitive()) {
                    FieldType type = FieldType.valueOf(typeOfField.getName().toUpperCase().replace(".",""));
                    switch (type) {
                        case INT:
                            field.setInt(newObject, Integer.parseInt(value));
                            break;
                        case DOUBLE:
                            field.setDouble(newObject, Double.parseDouble(value));
                            break;
                        case BYTE:
                            field.setByte(newObject, Byte.parseByte(value));
                            break;
                        case SHORT:
                            field.setShort(newObject, Short.parseShort(value));
                            break;
                        case LONG:
                            field.setLong(newObject, Long.parseLong(value));
                            break;
                        case FLOAT:
                            field.setFloat(newObject, Float.parseFloat(value));
                            break;
                        case BOOLEAN:
                            field.setBoolean(newObject, Boolean.parseBoolean(value));
                            break;
                        case CHAR:
                            field.setChar(newObject, value.charAt(0));
                            break;
                    }
                } else if(Instant.class.isAssignableFrom(typeOfField)){
                    Instant time = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(value).toInstant();

                    field.set(newObject, time);
                } else if(Collection.class.isAssignableFrom(typeOfField)){
                    Collection list = getActualCollection(typeOfField);

                    NodeList childNodeList = item.getChildNodes();
                    list =  iterateChildNode(childNodeList, list);

                    field.set(newObject, list);
                } else if (Map.class.isAssignableFrom(typeOfField)){
                    Map map = getActualMap(typeOfField);

                    NodeList childNodeList = item.getChildNodes();
                    for (int j = 0; j < childNodeList.getLength(); j++) {
                        Node childItem = childNodeList.item(j);

                        if (childItem.getNodeType() == Node.ELEMENT_NODE) {
                            NodeList childChildNodeList = childItem.getChildNodes();
                            List<Object> entry = new ArrayList<>(2);
                            entry =  (List) iterateChildNode(childChildNodeList,entry);

                            map.put(entry.get(0),entry.get(1));
                        }
                    }
                    field.set(newObject, map);
                } else {
                    NodeList childNodeList = item.getChildNodes();
                    for (int j = 0; j < childNodeList.getLength(); j++) {

                        if (childNodeList.item(j).getNodeType() == Node.ELEMENT_NODE) {
                            Node childClass = childNodeList.item(j);
                            String childClassName = childClass.getNodeName();

                            field.set(newObject, deserializeNodeList(childClass.getChildNodes(),childClassName));
                        }
                    }
                }
            }
        }
        return newObject;
    }

    private Collection getActualCollection(Class<?> typeOfField) throws ClassNotFoundException, NoSuchMethodException,
            IllegalAccessException, InvocationTargetException, InstantiationException {

        Collection list;
        if(typeOfField.isInterface()) {
            if(Set.class.isAssignableFrom(typeOfField)){
                list = new HashSet();
            } else {
                list = new ArrayList();
            }
        } else {
            list = (List) Class.forName(typeOfField.getName()).getConstructor().newInstance();
        }

        return list;
    }

    private Map getActualMap(Class<?> typeOfField) throws ClassNotFoundException, NoSuchMethodException,
            IllegalAccessException, InvocationTargetException, InstantiationException {

        Map map;
        if(typeOfField.isInterface()){
            map = new HashMap();
        } else {
            map = (Map) Class.forName(typeOfField.getName()).getConstructor().newInstance();
        }

        return map;
    }

    private Collection iterateChildNode(NodeList childNodeList, Collection list) throws ClassNotFoundException, NoSuchMethodException,
            IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchFieldException, ParseException {

        for (int j = 0; j < childNodeList.getLength(); j++) {
            Node childItem = childNodeList.item(j);
            if (childItem.getNodeType() == Node.ELEMENT_NODE) {
                String childClassName = childItem.getNodeName();

                if(Number.class.isAssignableFrom(Class.forName(childClassName)) ||
                    String.class.isAssignableFrom(Class.forName(childClassName))){

                    Object baseType = Class.forName(childClassName).getConstructor(new Class[]{String.class}).newInstance(childItem.getTextContent());
                    list.add(baseType);
                } else {
                    list.add(deserializeNodeList(childItem.getChildNodes(), childClassName));
                }
            }
        }
        return list;
    }

    private enum FieldType {
        INT, DOUBLE, BYTE, SHORT, LONG, FLOAT, BOOLEAN, CHAR
    }
}



