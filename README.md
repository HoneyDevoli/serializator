# serializator
Так как ТЗ задания очень абстрактно выбрал самый сложный вариант реализации - написать свой сериализатор который сериализует/десериализует в XML.

Задание:
класс который сериализует/десериализует Java Beans.

Пример сериализованного xml:<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<com.exactpro.test.SomeBean>
    <someString>string</someString>
    <someInt>50</someInt>
    <someFloat>0.0</someFloat>
    <aBoolean>false</aBoolean>
    <someInteger>3222</someInteger>
    <someDecimal>42</someDecimal>
    <someList>
        <com.exactpro.test.SomeBean>
            <someString>string</someString>
            <someInt>60</someInt>
            <someFloat>0.0</someFloat>
            <aBoolean>false</aBoolean>
            <someInteger>null</someInteger>
            <someDecimal>42</someDecimal>
            <someList>null</someList>
            <someMap>null</someMap>
            <someSet>null</someSet>
            <instant>null</instant>
        </com.exactpro.test.SomeBean>
    </someList>
    <instant>2019-02-25T06:35:26Z</instant>
</com.exactpro.test.SomeBean>

В качестве полей в Bean могут быть:
     простые типы, 
     String, Instant, BigDecimal и все классы обертки для простых типов (Short, Integer, Long и т.д.)
     Beans, 
     коллекции Beans: List, Map, Set

В случае наличия циклических ссылок выкинуть exception. Класс должен имплементировать следующий интерфейс:

interface SuperEncoder {
        byte[] serialize(Object anyBean);
        Object deserialize(byte[] data);  

