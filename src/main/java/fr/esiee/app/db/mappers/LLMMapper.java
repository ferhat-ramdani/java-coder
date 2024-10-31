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
                row.column("ID").getInt(),
                row.column("NAME").getString(),
                row.column("MODEL").getString(),
                row.column("SYSTEM_PROMPT").getString(),
                row.column("CHARACTERISTICS").getString(),
                row.column("TEMP").getDouble(),
                row.column("SEED").getInt()
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
