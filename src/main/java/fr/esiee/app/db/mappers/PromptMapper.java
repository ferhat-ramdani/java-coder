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
                row.column("ID").getInt(),
                row.column("MESSAGE").getString(),
                AuthorType.valueOf(row.column("AUTHOR_TYPE").getString()),
                row.column("CHAT_ID").getInt(),
                row.column("COMPILE").get(Boolean.class)
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
