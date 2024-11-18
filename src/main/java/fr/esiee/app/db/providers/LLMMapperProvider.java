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
 * {@link java.util.ServiceLoader} provider implementation for LLM DB mapper.
 * This class is used to provide the LLMMapper to the Helidon DB Client.
 */
@Weight(100)
public class LLMMapperProvider implements DbMapperProvider {

    /*
     * Yes, this part of code is very weird.
     * But we can't do anything about it.
     *
     * We need it to be able to provide the LLMMapper to the Helidon DB Client.
     */
    @Override
    public <T> Optional<DbMapper<T>> mapper(Class<T> type) {
        if (type.equals(LLM.class)) {
            var llmType = type.asSubclass(LLM.class);
            return Optional.of((new DbMapper<T>() {
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

                @Override
                public Map<String, Object> toNamedParameters(T llmAsT) {
                    Objects.requireNonNull(llmAsT);
                    return recordToMap(llmType.cast(llmAsT));
                }

                @Override
                public List<Object> toIndexedParameters(T llmAsT) {
                    Objects.requireNonNull(llmAsT);
                    return recordToList(llmType.cast(llmAsT));
                }
            }));
        }
        return Optional.empty();
    }
}
