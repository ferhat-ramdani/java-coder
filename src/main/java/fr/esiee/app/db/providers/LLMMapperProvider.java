package fr.esiee.app.db.providers;

import fr.esiee.app.db.entities.LLM;
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
 * Provides a mapper for the LLM entity.
 */
@Weight(100)
public class LLMMapperProvider implements DbMapperProvider {

  /**
   * Provides a mapper for the specified type.
   *
   * @param type the class type to map
   * @param <T>  the type of the entity
   * @return an Optional containing the DbMapper if the type is LLM, otherwise an empty Optional
   */
  @Override
  public <T> Optional<DbMapper<T>> mapper(Class<T> type) {
    if (type == LLM.class) {
      return getDbMapper(type);
    }
    return Optional.empty();
  }

  /**
   * Returns a DbMapper for the specified type if it is a subclass of LLM.
   *
   * @param <T>  the type of the entity
   * @param type the class type to map
   * @return an Optional containing the DbMapper for the LLM type
   */
  private <T> Optional<DbMapper<T>> getDbMapper(Class<T> type) {
    var llmType = type.asSubclass(LLM.class);
    return Optional.of((new DbMapper<>() {
      /**
       * Reads a DbRow and maps it to an instance of LLM.
       *
       * @param row the database row to read
       * @return an instance of LLM
       */
      @Override
      public T read(DbRow row) {
        Objects.requireNonNull(row);
        return type.cast(new LLM(
                row.column("ID").getInt(),
                row.column("NAME").getString(),
                row.column("MODEL").getString(),
                row.column("SYSTEM_PROMPT").getString(),
                row.column("CHARACTERISTICS").getString(),
                row.column("TEMP").getDouble(),
                row.column("SEED").getInt(),
                row.column("TIMEOUT_SEC").getInt()
        ));
      }

      /**
       * Converts an LLM instance to a map of named parameters.
       *
       * @param llmAsT the LLM instance to convert
       * @return a map of named parameters
       */
      @Override
      public Map<String, Object> toNamedParameters(T llmAsT) {
        Objects.requireNonNull(llmAsT);
        return recordToMap(llmType.cast(llmAsT));
      }

      /**
       * Converts an LLM instance to a list of indexed parameters.
       *
       * @param llmAsT the LLM instance to convert
       * @return a list of indexed parameters
       */
      @Override
      public List<Object> toIndexedParameters(T llmAsT) {
        Objects.requireNonNull(llmAsT);
        return recordToList(llmType.cast(llmAsT));
      }
    }));
  }
}
