package fr.esiee.app.db.mappers;

import fr.esiee.app.db.entities.LLM;
import io.helidon.dbclient.DbMapper;
import io.helidon.dbclient.DbRow;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static fr.esiee.app.db.mappers.MapperUtils.recordToList;
import static fr.esiee.app.db.mappers.MapperUtils.recordToMap;

public record LLMMapper() implements DbMapper<LLM> {
    @Override
    public LLM read(DbRow row) {
        Objects.requireNonNull(row);
        return new LLM(
                row.column("ID").getInt(),
                row.column("NAME").getString(),
                row.column("MODEL").getString(),
                row.column("SYSTEM_PROMPT").getString(),
                row.column("CHARACTERISTICS").getString(),
                row.column("TEMP").getDouble(),
                row.column("SEED").getInt(),
                row.column("TIMEOUT_SEC").getInt()
        );
    }

    @Override
    public Map<String, Object> toNamedParameters(LLM llm) {
        Objects.requireNonNull(llm);
        return recordToMap(llm);
    }

    @Override
    public List<Object> toIndexedParameters(LLM llm) {
        Objects.requireNonNull(llm);
        return recordToList(llm);
    }
}
