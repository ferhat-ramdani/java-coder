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
                row.column("id").getInt(),
                row.column("name").getString(),
                row.column("model").getString(),
                row.column("system_prompt").getString(),
                row.column("characteristics").getString(),
                row.column("temp").getDouble(),
                row.column("seed").getInt()
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
