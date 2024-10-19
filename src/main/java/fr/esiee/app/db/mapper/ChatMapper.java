package fr.esiee.app.db.mapper;

import fr.esiee.app.db.entities.Chat;
import io.helidon.common.mapper.Mapper;
import io.helidon.dbclient.DbColumn;
import io.helidon.dbclient.DbMapper;
import io.helidon.dbclient.DbRow;

import java.lang.reflect.InvocationTargetException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record ChatMapper() implements DbMapper<Chat> {
    @Override
    public Chat read(DbRow row) {
        return new Chat(
                row.column("id").getInt(),
                row.column("title").getString(),
                row.column("last_activity").get(Timestamp.class),
                row.column("llm_id").getInt()
        );
    }

    @Override
    public Map<String, Object> toNamedParameters(Chat chat) {
        return MapperUtils.recordToMap(chat);
    }

    @Override
    public List<Object> toIndexedParameters(Chat chat) {
        return MapperUtils.recordToMap(chat).values().stream().toList();
    }
}
