package fr.esiee.app.db.mapper;

import fr.esiee.app.db.entities.AuthorType;
import fr.esiee.app.db.entities.Prompt;
import io.helidon.dbclient.DbColumn;
import io.helidon.dbclient.DbMapper;
import io.helidon.dbclient.DbRow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record PromptMapper() implements DbMapper<Prompt> {
    @Override
    public Prompt read(DbRow row) {
        return new Prompt(
                row.column("id").getInt(),
                row.column("message").getString(),
                AuthorType.valueOf(row.column("author_type").getString()),
                row.column("chat_id").getInt(),
                row.column("compile").get(Boolean.class)
        );
    }

    @Override
    public Map<String, Object> toNamedParameters(Prompt prompt) {
        return MapperUtils.recordToMap(prompt);
    }

    @Override
    public List<Object> toIndexedParameters(Prompt prompt) {
        return MapperUtils.recordToMap(prompt).values().stream().toList();
    }
}
