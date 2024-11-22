package fr.esiee.app.db.providers;

import fr.esiee.app.db.entities.AuthorType;
import fr.esiee.app.db.entities.Prompt;
import io.helidon.common.Weight;
import io.helidon.dbclient.DbMapper;
import io.helidon.dbclient.DbRow;
import io.helidon.dbclient.spi.DbMapperProvider;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static fr.esiee.app.db.providers.MapperUtils.recordToList;
import static fr.esiee.app.db.providers.MapperUtils.recordToMap;

/**
 * Provides a mapper for the Prompt entity.
 */
@Weight(100)
public class PromptMapperProvider implements DbMapperProvider {


  /**
   * Provides a mapper for the specified type.
   *
   * @param type the class type to be mapped
   * @param <T>  the type of the class
   * @return an Optional containing the DbMapper if the type is Prompt, otherwise an empty Optional
   */
  @Override
  public <T> Optional<DbMapper<T>> mapper(Class<T> type) {
    if (type == Prompt.class) {
      return getDbMapper(type);
    }
    return Optional.empty();
  }

  /**
   * Returns an Optional containing a DbMapper for the Prompt class.
   *
   * @param type the class type to be mapped
   * @param <T>  the type of the class
   * @return an Optional containing the DbMapper for the Prompt class
   */
  private <T> Optional<DbMapper<T>> getDbMapper(Class<T> type) {
    var promptType = type.asSubclass(Prompt.class);
    return Optional.of(new DbMapper<>() {
      /**
       * Reads a DbRow and maps it to a Prompt object.
       *
       * @param row the DbRow to be read
       * @return the mapped Prompt object
       */
      @Override
      public T read(DbRow row) {
        Objects.requireNonNull(row);
        return type.cast(new Prompt(
                row.column("ID").getInt(),
                row.column("MESSAGE").getString(),
                AuthorType.valueOf(row.column("AUTHOR_TYPE").getString()),
                row.column("CHAT_ID").getInt(),
                row.column("COMPILE").get(Boolean.class)
        ));
      }

      /**
       * Converts a Prompt object to a map of named parameters.
       *
       * @param promptAsT the Prompt object to be converted
       * @return a map of named parameters
       */
      @Override
      public Map<String, Object> toNamedParameters(T promptAsT) {
        Objects.requireNonNull(promptAsT);
        return recordToMap(promptType.cast(promptAsT));
      }

      /**
       * Converts a Prompt object to a list of indexed parameters.
       *
       * @param promptAsT the Prompt object to be converted
       * @return a list of indexed parameters
       */
      @Override
      public List<Object> toIndexedParameters(T promptAsT) {
        Objects.requireNonNull(promptAsT);
        return recordToList(promptType.cast(promptAsT));
      }
    });
  }
}
