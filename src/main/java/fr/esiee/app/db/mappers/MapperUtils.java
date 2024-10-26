package fr.esiee.app.utils;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapperUtils {
  public static Map<String, Object> recordToMap(Record record) {
    var map = new HashMap<String, Object>();
    for (var field : record.getClass().getRecordComponents()) {
      try {
        map.put(field.getName(), field.getAccessor().invoke(record));
      } catch (InvocationTargetException | IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }
    return Map.copyOf(map);
  }

  public static List<Object> recordToList(Record record) {
    return recordToMap(record).values().stream().toList();
  }
}
