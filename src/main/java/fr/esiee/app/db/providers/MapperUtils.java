package fr.esiee.app.db.providers;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for mapping Records to Maps and Lists.
 */
class MapperUtils {

  /**
   * Converts a given Record to a Map with field names as keys and field values as values.
   *
   * @param record the Record to be converted
   * @return an unmodifiable Map containing the field names and values of the Record
   * @throws RuntimeException if an error occurs while accessing the Record's fields
   */
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

  /**
   * Converts a given Record to a List with field values.
   *
   * @param record the Record to be converted
   * @return an unmodifiable List containing the field values of the Record
   * @throws RuntimeException if an error occurs while accessing the Record's fields
   */
  static List<Object> recordToList(Record record) {
    var list = new ArrayList<>();
    for (var field : record.getClass().getRecordComponents()) {
      try {
        list.add(field.getAccessor().invoke(record));
      } catch (InvocationTargetException | IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }
    return List.copyOf(list);
  }
}
