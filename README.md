# serializator
Так как ТЗ задания очень абстрактно выбрал самый сложный вариант реализации - написать свой сериализатор который сериализует/десериализует в XML.

Задание:
класс который сериализует/десериализует Java Beans.

В качестве полей в Bean могут быть:
     простые типы, 
     String, Instant, BigDecimal и все классы обертки для простых типов (Short, Integer, Long и т.д.)
     Beans, 
     коллекции Beans: List, Map, Set

В случае наличия циклических ссылок выкинуть exception. Класс должен имплементировать следующий интерфейс:

interface SuperEncoder {
        byte[] serialize(Object anyBean);
        Object deserialize(byte[] data);  
