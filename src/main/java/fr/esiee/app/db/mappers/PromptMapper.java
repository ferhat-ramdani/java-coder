package fr.esiee.app.db.mappers;

import fr.esiee.app.db.entities.AuthorType;
import fr.esiee.app.db.entities.Prompt;
import io.helidon.dbclient.DbMapper;
import io.helidon.dbclient.DbRow;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static fr.esiee.app.db.mappers.MapperUtils.recordToList;
import static fr.esiee.app.db.mappers.MapperUtils.recordToMap;

public record PromptMapper() implements DbMapper<Prompt> {
    @Override
    public Prompt read(DbRow row) {
        Objects.requireNonNull(row);
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
        Objects.requireNonNull(prompt);
        return recordToMap(prompt);
    }

    @Override
    public List<Object> toIndexedParameters(Prompt prompt) {
        Objects.requireNonNull(prompt);
        return recordToList(prompt);
    }
}
