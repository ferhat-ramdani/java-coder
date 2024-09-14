package fr.esiee.app.db;

import io.helidon.common.Weight;
import io.helidon.dbclient.DbMapper;
import io.helidon.dbclient.spi.DbMapperProvider;

import java.util.Optional;

/**
 * {@link java.util.ServiceLoader} provider implementation for LLM DB mapper.
 */
@Weight(100)
public class LLMMapperProvider implements DbMapperProvider {
    private static final LLMMapper MAPPER = new LLMMapper();

    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<DbMapper<T>> mapper(Class<T> type) {
        if (type.equals(LLM.class)) {
            return Optional.of((DbMapper<T>) MAPPER);
        }
        return Optional.empty();
    }
}
