package com.exactpro.main;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Serializator implements SuperEncoder {
    private static final Logger logger = Logger.getLogger(Serializator.class.getSimpleName());
    private Document document;

    @Override
    public byte[] serialize(Object anyBean) {
        DocumentBuilderFactory dbf;

        try {
            dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            document = dbf.newDocumentBuilder().newDocument();
            document.appendChild(iterateNodes(anyBean));

            writeDocument(document);



        } catch (ParserConfigurationException e) {
            logger.log(Level.WARNING,e.getMessage());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return new byte[0];
    }

    @Override
    public Object deserialize(byte[] data) {
        Document document = getReadedDocument("./log/hui2.xml");
        Object newObject = null;
        var classname = document.getDocumentElement().getTagName();

//        if (document.hasChildNodes()) {
//            printNote(document.getDocumentElement().getChildNodes());
//        }
        try {
//            System.out.println(classname);
//            Class<?> parseClass = Class.forName(classname);
//            newObject = parseClass.getConstructor().newInstance();
//            for (var field: newObject.getClass().getDeclaredFields()) {
//
//                System.out.println(field.getName());
//                field.setAccessible(true);
//                var value = field.get(newObject);
//
//                field.set(newObject,"5");
//            }

            var chiledNodes = document.getDocumentElement().getChildNodes();
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
        } catch (Exception e){
            return newObject;
        }
//        for (int i = 0 ;i<document.getDocumentElement().getChildNodes().getLength(); i++) {
//
//            System.out.println(document.getDocumentElement().getChildNodes().item(i).getTextContent());
//        }
        return classname;
    }

    private static void writeDocument(Document document) {
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT,"yes");
            Result result = new StreamResult(new File("./log/hui1.xml"));
            Source source = new DOMSource(document);
            transformer.transform(source, result);
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }
    }

    private static Document getReadedDocument(String filePath){
        try {
            File xmlFile = new File(filePath);
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            Document document = dbf.newDocumentBuilder().parse(xmlFile);
            return document;
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        return  null;
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


    private LinkedList<Object> callStack = new LinkedList<>();
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
            object.getClass().isPrimitive() ||
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

    private static void printNote(NodeList nodeList) {

        for (int count = 0; count < nodeList.getLength(); count++) {

            Node tempNode = nodeList.item(count);

            // make sure it's element node.
            if (tempNode.getNodeType() == Node.ELEMENT_NODE) {

                // get node name and value
                System.out.println("\nNode Name =" + tempNode.getNodeName() + " [OPEN]");
//                System.out.println("Node Value =" + tempNode.getTextContent());

                if (tempNode.hasChildNodes()) {

                    // loop again if has child nodes
//                    printNote(tempNode.getChildNodes());

                }

                System.out.println("Node Name =" + tempNode.getNodeName() + " [CLOSE]");

            }

        }

    }

    private static Object deserializeNodeList(NodeList nodeList, String className ) throws NoSuchMethodException, IllegalAccessException,
            InvocationTargetException, InstantiationException, ClassNotFoundException, NoSuchFieldException, ParseException {

        Class<?> parseClass = Class.forName(className);
        var newObject = parseClass.getConstructor().newInstance();

        for (int i = 0 ;i<nodeList.getLength(); i++) {
            Node item = nodeList.item(i);

            if (item.getNodeType() == Node.ELEMENT_NODE) {
                var elem = item.getNodeName();
                var value = item.getTextContent();

                    Field field = newObject.getClass().getDeclaredField(elem);
                    field.setAccessible(true);

                    System.out.println(item.getNodeName());
                    if(value.equals("null")) {
                        System.out.println("null");
                    }
                     else if(Number.class.isAssignableFrom(field.getType()) ||
                            String.class.isAssignableFrom(field.getType())) {
                        Object number = Class.forName(field.getType().getName()).getConstructor(new Class[]{String.class}).newInstance(value);

                        field.set(newObject, number);
                    } else if (field.getType().isPrimitive()) {
                        field.setInt(newObject, Integer.parseInt(value));
                    } else if(Instant.class.isAssignableFrom(field.getType())){
                        Instant time = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(value).toInstant();

                        field.set(newObject, time);
                    } else if(List.class.isAssignableFrom(field.getType())){

                             List list = new ArrayList();
                             list.add(new Object());
//                             if(item.hasChildNodes()){
                            field.set(newObject, list);
                        System.out.println("hui1");
                    } else if (Map.class.isAssignableFrom(field.getType())){
                        System.out.println("hui2");
                    } else if (Set.class.isAssignableFrom(field.getType())){
                        System.out.println("hui3");
                    } else {
                        for (int j = 0; j < item.getChildNodes().getLength(); j++) {
                            if (item.getChildNodes().item(j).getNodeType() == Node.ELEMENT_NODE) {
                                Node childClass = item.getChildNodes().item(j);
                                String childClassName = childClass.getNodeName();
                                System.out.println(childClassName);
                                field.set(newObject, deserializeNodeList(childClass.getChildNodes(),childClassName));
                            }
                        }
                    }
                }



        }
        return newObject;
    }
}



