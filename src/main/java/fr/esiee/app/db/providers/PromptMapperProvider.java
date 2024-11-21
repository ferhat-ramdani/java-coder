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
 * {@link java.util.ServiceLoader} provider implementation for PromptMessage DB mapper.
 * This class is used to provide the PromptMapper to the Helidon DB Client.
 */
@Weight(100)
public class PromptMapperProvider implements DbMapperProvider {

    /*
     * Yes, this part of code is very weird.
     * But we can't do anything about it.
     *
     * We need it to be able to provide the PromptMapper to the Helidon DB Client.
     */
    @Override
    public <T> Optional<DbMapper<T>> mapper(Class<T> type) {
        if (type == Prompt.class) {
            var promptType = type.asSubclass(Prompt.class);
            return Optional.of(new DbMapper<> () {
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

                @Override
                public Map<String, Object> toNamedParameters(T promptAsT) {
                    Objects.requireNonNull(promptAsT);
                    return recordToMap(promptType.cast(promptAsT));
                }

                @Override
                public List<Object> toIndexedParameters(T promptAsT) {
                    Objects.requireNonNull(promptAsT);
                    return recordToList(promptType.cast(promptAsT));
                }
            });
        }
        return Optional.empty();
    }
}
