package fr.esiee.app.db.providers;

import fr.esiee.app.db.entities.Chat;
import io.helidon.common.Weight;
import io.helidon.dbclient.DbMapper;
import io.helidon.dbclient.DbRow;
import io.helidon.dbclient.spi.DbMapperProvider;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static fr.esiee.app.db.providers.MapperUtils.recordToList;
import static fr.esiee.app.db.providers.MapperUtils.recordToMap;

/**
 * A provider class that provides a DbMapper for the Chat entity.
 */
@Weight(100)
public class ChatMapperProvider implements DbMapperProvider {

  /**
   * Provides a mapper for the specified type.
   *
   * @param <T>  the type of the entity
   * @param type the class of the entity
   * @return an Optional containing the DbMapper if the type is Chat, otherwise an empty Optional
   */
  @Override
  public <T> Optional<DbMapper<T>> mapper(Class<T> type) {
    if (type == Chat.class) {
      return getDbMapper(type);
    }
    return Optional.empty();
  }

  /**
   * Provides a DbMapper for the Chat entity.
   *
   * @param <T>  the type of the entity
   * @param type the class of the entity
   * @return an Optional containing the DbMapper for Chat
   */
  private <T> Optional<DbMapper<T>> getDbMapper(Class<T> type) {
    var chatType = type.asSubclass(Chat.class);
    return Optional.of(new DbMapper<>() {
      /**
       * Reads a DbRow and converts it to a Chat entity.
       *
       * @param row the database row
       * @return the Chat entity
       */
      @Override
      public T read(DbRow row) {
        Objects.requireNonNull(row);
        return type.cast(new Chat(
                row.column("ID").getInt(),
                row.column("TITLE").getString(),
                row.column("LAST_ACTIVITY").get(Timestamp.class),
                row.column("LLM_ID").getInt()
        ));
      }

      /**
       * Converts a Chat entity to a map of named parameters.
       *
       * @param chatAsT the Chat entity
       * @return a map of named parameters
       */
      @Override
      public Map<String, Object> toNamedParameters(T chatAsT) {
        Objects.requireNonNull(chatAsT);
        return recordToMap(chatType.cast(chatAsT));
      }

      /**
       * Converts a Chat entity to a list of indexed parameters.
       *
       * @param chatAsT the Chat entity
       * @return a list of indexed parameters
       */
      @Override
      public List<Object> toIndexedParameters(T chatAsT) {
        Objects.requireNonNull(chatAsT);
        return recordToList(chatType.cast(chatAsT));
      }
    });
  }
}
