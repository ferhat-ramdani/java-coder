package fr.esiee.app.db.mappers;

import fr.esiee.app.db.entities.LLM;
import io.helidon.dbclient.DbMapper;
import io.helidon.dbclient.DbRow;

import java.util.List;
import java.util.Map;

public record LLMMapper() implements DbMapper<LLM> {
    @Override
    public LLM read(DbRow row) {
        return new LLM(
                row.column("id").getInt(),
                row.column("name").getString(),
                row.column("model").getString(),
                row.column("system_prompt").getString(),
                row.column("caracteristics").getString(),
                row.column("temp").getDouble(),
                row.column("seed").getInt()
        );
    }

    @Override
    public Map<String, Object> toNamedParameters(LLM llm) {
        return MapperUtils.recordToMap(llm);
    }

    @Override
    public List<Object> toIndexedParameters(LLM llm) {
        return MapperUtils.recordToList(llm);
    }
}
