package fr.esiee.app.db.providers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MapperUtilsTest {

  private record Avenger(String name, String alias, String weapon) {}
  private record EmptyRecord() {}

  @Test
  void testRecordToMap() {
    var avenger = new Avenger("Tony Stark", "Iron Man", "Iron Man Suit");

    var result = MapperUtils.recordToMap(avenger);

    assertAll(() -> assertEquals(3, result.size()),
            () -> assertEquals(avenger.name, result.get("name")),
            () -> assertEquals(avenger.alias, result.get("alias")),
            () -> assertEquals(avenger.weapon, result.get("weapon")));
  }

  @Test
  public void testRecordToList() {
    var avenger = new Avenger("Steve Rogers", "Captain America", "Vibranium Shield");

    var result = MapperUtils.recordToList(avenger);
    assertAll(() -> assertEquals(3, result.size()),
            () -> assertEquals(avenger.name, result.get(0)),
            () -> assertEquals(avenger.alias, result.get(1)),
            () -> assertEquals(avenger.weapon, result.get(2)));
  }

  @Test
  public void testRecordToMapWithEmptyRecord() {
    var emptyRecord = new EmptyRecord();
    var result = MapperUtils.recordToMap(emptyRecord);
    assertTrue(result.isEmpty());
  }

  @Test
  public void testRecordToListWithEmptyRecord() {
    var emptyRecord = new EmptyRecord();
    var result = MapperUtils.recordToList(emptyRecord);
    assertTrue(result.isEmpty());
  }
}
