package fr.esiee.app.db.mappers;

import fr.esiee.app.db.entities.Chat;
import io.helidon.dbclient.DbMapper;
import io.helidon.dbclient.DbRow;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static fr.esiee.app.db.mappers.MapperUtils.recordToList;
import static fr.esiee.app.db.mappers.MapperUtils.recordToMap;

public record ChatMapper() implements DbMapper<Chat> {
    @Override
    public Chat read(DbRow row) {
        Objects.requireNonNull(row);
        return new Chat(
                row.column("ID").getInt(),
                row.column("TITLE").getString(),
                row.column("LAST_ACTIVITY").get(Timestamp.class),
                row.column("LLM_ID").getInt()
        );
    }

    @Override
    public Map<String, Object> toNamedParameters(Chat chat) {
        Objects.requireNonNull(chat);
        return recordToMap(chat);
    }

    @Override
    public List<Object> toIndexedParameters(Chat chat) {
        Objects.requireNonNull(chat);
        return recordToList(chat);
    }
}
