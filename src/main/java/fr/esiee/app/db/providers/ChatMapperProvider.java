package fr.esiee.app.db.mappers.providers;

import fr.esiee.app.db.entities.Chat;
import fr.esiee.app.db.mappers.ChatMapper;
import io.helidon.common.Weight;
import io.helidon.dbclient.DbMapper;
import io.helidon.dbclient.spi.DbMapperProvider;

import java.util.Optional;

/**
 * {@link java.util.ServiceLoader} provider implementation for Chat.ts DB mapper.
 * This class is used to provide the ChatMapper to the Helidon DB Client.
 */
@Weight(100)
public class ChatMapperProvider implements DbMapperProvider {
    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<DbMapper<T>> mapper(Class<T> type) {
        if (type.equals(Chat.class)) {
            return Optional.of((DbMapper<T>) new ChatMapper());
        }
        return Optional.empty();
    }
}
