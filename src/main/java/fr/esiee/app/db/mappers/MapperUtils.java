package fr.esiee.app.db.mappers;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class MapperUtils {
  static Map<String, Object> recordToMap(Record record) {
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

  static List<Object> recordToList(Record record) {
    return recordToMap(record).values().stream().toList();
  }
}
