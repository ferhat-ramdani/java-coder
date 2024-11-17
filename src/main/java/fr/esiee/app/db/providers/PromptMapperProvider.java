package fr.esiee.app.db.providers;

import fr.esiee.app.db.entities.Prompt;
import fr.esiee.app.db.mappers.PromptMapper;
import io.helidon.common.Weight;
import io.helidon.dbclient.DbMapper;
import io.helidon.dbclient.spi.DbMapperProvider;

import java.util.Optional;

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
    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<DbMapper<T>> mapper(Class<T> type) {
        if (type.equals(Prompt.class)) {
            return Optional.of((DbMapper<T>) new PromptMapper());
        }
        return Optional.empty();
    }
}
