package com.exactpro.main;

import com.exactpro.main.exception.ReferenceCycleException;
import com.exactpro.main.abstraction.SuperEncoder;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;

public class ByteSerializer implements SuperEncoder {

    private static final byte STRING_TYPE = 0;
    private static final byte INTEGER_TYPE = 1;
    private static final byte LONG_TYPE = 2;
    private static final byte BIG_INTEGER_TYPE = 3;
    private static final byte DOUBLE_TYPE = 4;
    private static final byte BOOLEAN_TYPE = 5;
    private static final byte FLOAT_TYPE = 6;
    private static final byte INSTANT_TYPE = 7;
    private static final byte CHAR_TYPE = 8;
    private static final byte BIGDECIMAL_TYPE = 9;
    private static final byte NULL_TYPE = 10;
    private static final byte MAP_TYPE = 11;
    private static final byte COLLECTION_TYPE = 12;
    private static final byte BEAN_TYPE = 13;

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private LinkedList<Object> callStack = new LinkedList<>();

    private int lvl = 0;


    @Override
    public byte[] serialize(Object anyBean) throws ReferenceCycleException {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             DataOutputStream outputStream = new DataOutputStream(byteStream)
        ) {
            if (isCircularObject(anyBean)) {
                throw new ReferenceCycleException("This bean have a circular field " + anyBean.getClass());
            }

            serializeStream(anyBean, outputStream);
            return byteStream.toByteArray();

        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new byte[0];
    }

    @Override
    public Object deserialize(byte[] data) {
        try {
            ByteArrayInputStream byteInStream = new ByteArrayInputStream(data);
            DataInputStream inputStream = new DataInputStream(byteInStream);

            lvl = 0;
            return deserializeBean(inputStream);

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
        }  catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void serializeStream(Object bean, DataOutput dataOutput) throws IllegalAccessException, IOException {
        lvl++;

        Class classObject = bean.getClass();
        serialize(dataOutput, classObject.getName());
        for (Field field : classObject.getDeclaredFields()) {

            field.setAccessible(true);

            String nameOfField = field.getName();
            Object valueOfField = field.get(bean);

            dataOutput.writeByte(lvl);
            serialize(dataOutput, nameOfField);
            serialize(dataOutput, valueOfField);
        }
        lvl--;
    }

    public boolean isCircularObject(Object node) throws IllegalAccessException {
        Class classObject = node.getClass();

        var fields = classObject.getDeclaredFields();

        callStack.push(node);
        for (Field field : fields) {
            field.setAccessible(true);

            Object valueOfField = field.get(node);

            if (!isSDKClass(valueOfField)) {
                if (callStack.indexOf(valueOfField) >= 0) {
                    return true;
                }

                if (valueOfField instanceof Collection) {
                    if (iterateCollection((Collection) valueOfField)) {
                        return true;
                    }
                } else if (valueOfField instanceof Map) {
                    Map map = (Map) valueOfField;
                    Collection keys = map.keySet();
                    Collection values = map.values();

                    if (iterateCollection(keys) || iterateCollection(values)) {
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

    private Object deserializeBean(DataInputStream inputStream) throws NoSuchMethodException, IllegalAccessException,
            InvocationTargetException, InstantiationException, ClassNotFoundException, NoSuchFieldException, ParseException, IOException {

        String className = readString(inputStream);
        Object newObject = null;
        if(!className.equals("")) {
            Class<?> parseClass = Class.forName(className);
            newObject = parseClass.getConstructor().newInstance();
        }

        lvl++;

        while (inputStream.available() > 0) {

            inputStream.mark(1);
            byte readlvl = inputStream.readByte();

            if (lvl == readlvl) {

                String fieldName = readString(inputStream);
                Field field = newObject.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                Class<?> typeOfField = field.getType();

                if (Number.class.isAssignableFrom(typeOfField)) {
                    Object value = wrapperDeserialize(inputStream);
                    if (value != null) {
                        Object number = Class.forName(typeOfField.getName()).getConstructor(new Class[]{String.class}).newInstance(value.toString());
                        field.set(newObject, number);
                    }

                } else {
                    byte typeByte = inputStream.readByte();
                    switch (typeByte) {
                        case STRING_TYPE:
                            int strByteSize = inputStream.readInt();
                            byte[] strBytes = new byte[strByteSize];
                            inputStream.readFully(strBytes, 0, strByteSize);

                            String readString = new String(strBytes, UTF_8);
                            field.set(newObject, readString);
                            break;

                        case INTEGER_TYPE:
                            int readInt = inputStream.readInt();
                            field.setInt(newObject, readInt);
                            break;
                            
                        case LONG_TYPE:
                            long readLong = inputStream.readLong();
                            field.setLong(newObject, readLong);
                            break;

                        case BIG_INTEGER_TYPE:
                            int byteSize = inputStream.readInt();
                            byte[] bytes = new byte[byteSize];
                            inputStream.readFully(bytes, 0, byteSize);

                            BigInteger readBigInt = new BigInteger(bytes);
                            field.set(newObject, readBigInt);
                            break;

                        case FLOAT_TYPE:
                            float readFloat = inputStream.readFloat();
                            field.setFloat(newObject, readFloat);
                            break;

                        case DOUBLE_TYPE:
                            double readDouble = inputStream.readDouble();
                            field.setDouble(newObject, readDouble);
                            break;

                        case BOOLEAN_TYPE:
                            boolean readBoolean = inputStream.readBoolean();
                            field.setBoolean(newObject, readBoolean);
                            break;

                        case NULL_TYPE:
                            break;

                        case CHAR_TYPE:
                            char readChar = inputStream.readChar();
                            field.setChar(newObject, readChar);
                            break;

                        case BIGDECIMAL_TYPE:
                            BigDecimal decimal = new BigDecimal(readString(inputStream));
                            field.set(newObject,decimal);
                            break;

                        case INSTANT_TYPE:
                            String stringTime = readString(inputStream);
                            Instant time = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(stringTime).toInstant();
                            field.set(newObject, time);
                            break;

                        case COLLECTION_TYPE:
                            int size = inputStream.readInt();

                            Collection coll = getActualCollection(field.getType());
                            for (int i = 0; i < size; i++) {
                                inputStream.mark(1);
                                byte type = inputStream.readByte();
                                if (type != BEAN_TYPE) {
                                    inputStream.reset();
                                    coll.add(wrapperDeserialize(inputStream));
                                } else {
                                    coll.add(deserializeBean(inputStream));
                                }

                                field.set(newObject, coll);
                            }
                            break;

                        case MAP_TYPE:
                            size = inputStream.readInt()*2;

                            Map map = getActualMap(field.getType());
                            Object[] obj = new Object[size];
                            for (int i = 0; i < size; i++) {
                                byte type = inputStream.readByte();
                                if (type == BEAN_TYPE) {
                                    obj[i] = deserializeBean(inputStream);
                                } else {
                                    inputStream.reset();
                                    obj[i] = wrapperDeserialize(inputStream);
                                }
                            }
                            for (int i = 0; i < size; i++) {
                                map.put(obj[i],obj[++i]);
                            }
                            field.set(newObject, map);
                            break;

                        case BEAN_TYPE:
                            field.set(newObject, deserializeBean(inputStream));
                            break;

                        default:
                            break;
                    }
                }
            } else {
                --lvl;
                inputStream.reset();
                return newObject;
            }
        }
        return newObject;
    }

    private Collection getActualCollection (Class < ? > typeOfField) throws
        ClassNotFoundException, NoSuchMethodException,
        IllegalAccessException, InvocationTargetException, InstantiationException {

        Collection list;
        if (typeOfField.isInterface()) {
            if (Set.class.isAssignableFrom(typeOfField)) {
                list = new HashSet();
            } else {
                list = new ArrayList();
            }
        } else {
            list = (List) Class.forName(typeOfField.getName()).getConstructor().newInstance();
        }

        return list;
    }

    private Map getActualMap (Class < ? > typeOfField) throws ClassNotFoundException, NoSuchMethodException,
        IllegalAccessException, InvocationTargetException, InstantiationException {

        Map map;
        if (typeOfField.isInterface()) {
            map = new HashMap();
        } else {
            map = (Map) Class.forName(typeOfField.getName()).getConstructor().newInstance();
        }

        return map;
    }


    private void serialize (DataOutput dos, Object obj) throws IOException, IllegalAccessException {
            if (obj instanceof String) {
                String str = (String) obj;
                writeString(str, dos);

            } else if (obj instanceof Integer) {
                dos.writeByte(INTEGER_TYPE);
                dos.writeInt((Integer) obj);

            } else if (obj instanceof Long) {
                dos.writeByte(LONG_TYPE);
                dos.writeLong((Long) obj);

            } else if (obj instanceof BigInteger) {
                byte[] bytes = ((BigInteger) obj).toByteArray();
                int byteSize = bytes.length;
                dos.writeByte(BIG_INTEGER_TYPE);
                dos.writeInt(byteSize);
                dos.write(bytes, 0, byteSize);

            } else if(obj instanceof BigDecimal){
                byte[] bytes = obj.toString().getBytes();
                int byteSize = bytes.length;
                dos.writeByte(BIGDECIMAL_TYPE);
                dos.writeByte(STRING_TYPE);
                dos.writeInt(byteSize);
                dos.write(bytes, 0, byteSize);

            } else if (obj instanceof Float) {
                dos.writeByte(FLOAT_TYPE);
                dos.writeFloat((Float) obj);

            } else if (obj instanceof Double) {
                dos.writeByte(DOUBLE_TYPE);
                dos.writeDouble((Double) obj);

            } else if (obj instanceof Boolean) {
                dos.writeByte(BOOLEAN_TYPE);
                dos.writeBoolean((Boolean) obj);

            } else if (obj == null) {
                dos.writeByte(NULL_TYPE);

            } else if (obj instanceof Instant) {
                dos.writeByte(INSTANT_TYPE);
                writeString(obj.toString(), dos);

            } else if (obj instanceof Map) {
                Map map = (Map) obj;
                dos.writeByte(MAP_TYPE);
                dos.writeInt(map.size());
                for (Object o : map.entrySet()) {
                    Map.Entry entry = ((Map.Entry) o);
                    serialize(dos, entry.getKey());
                    serialize(dos, entry.getValue());
                }

            } else if (obj instanceof Collection) {
                Collection coll = (Collection) obj;
                dos.writeByte(COLLECTION_TYPE);
                dos.writeInt(coll.size());

                for (Object elem : coll) {
                    serialize(dos, elem);
                }

            } else {
                dos.writeByte(BEAN_TYPE);
                serializeStream(obj,dos);
            }
        }

    public Object wrapperDeserialize(DataInputStream inputStream) throws IOException {

        byte typeByte = inputStream.readByte();
        switch (typeByte) {
            case STRING_TYPE:
                return readString(inputStream);

            case INTEGER_TYPE:
                return inputStream.readInt();

            case LONG_TYPE:
                return inputStream.readLong();

            case BIG_INTEGER_TYPE:
                int byteSize = inputStream.readInt();
                byte[] bytes = new byte[byteSize];
                inputStream.readFully(bytes, 0, byteSize);
                return new BigInteger(bytes);

            case FLOAT_TYPE:
                return inputStream.readFloat();

            case DOUBLE_TYPE:
                return inputStream.readDouble();

            case BOOLEAN_TYPE:
                return inputStream.readBoolean();

            case BIGDECIMAL_TYPE:
                BigDecimal decimal = new BigDecimal(readString(inputStream));
                return decimal;

            case NULL_TYPE:
                return null;

            default:
                throw new IOException("Cannot wrapperDeserialize " + typeByte);
        }
    }

    private void writeString (String value, DataOutput dos) throws IOException {
        byte[] bytes = value.getBytes(UTF_8);
        int byteSize = bytes.length;
        dos.writeByte(STRING_TYPE);
        dos.writeInt(byteSize);
        dos.write(bytes, 0, byteSize);
    }

    private String readString (DataInputStream inputStream) throws IOException {
        inputStream.readByte();
        int strByteSize = inputStream.readInt();
        byte[] strBytes = new byte[strByteSize];
        inputStream.readFully(strBytes, 0, strByteSize);

        return new String(strBytes, UTF_8);
    }
}
