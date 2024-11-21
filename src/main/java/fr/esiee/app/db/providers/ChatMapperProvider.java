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
 * {@link java.util.ServiceLoader} provider implementation for Chat.ts DB mapper.
 * This class is used to provide the ChatMapper to the Helidon DB Client.
 */
@Weight(100)
public class ChatMapperProvider implements DbMapperProvider {


    /*
     * Yes, this part of code is very weird.
     * But we can't do anything about it.
     *
     * We need it to be able to provide the ChatMapper to the Helidon DB Client.
     */
    @Override
    public <T> Optional<DbMapper<T>> mapper(Class<T> type) {
        if (type == Chat.class) {
            var chatType = type.asSubclass(Chat.class);
            return Optional.of(new DbMapper<> () {
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

                @Override
                public Map<String, Object> toNamedParameters(T chatAsT) {
                    Objects.requireNonNull(chatAsT);
                    return recordToMap(chatType.cast(chatAsT));
                }

                @Override
                public List<Object> toIndexedParameters(T chatAsT) {
                    Objects.requireNonNull(chatAsT);
                    return recordToList(chatType.cast(chatAsT));
                }
            });
        }
        return Optional.empty();
    }
}
