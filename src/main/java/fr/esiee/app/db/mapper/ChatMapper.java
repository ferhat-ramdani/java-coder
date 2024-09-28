package fr.esiee.app.db.mapper;

import fr.esiee.app.db.entities.Chat;
import io.helidon.dbclient.DbColumn;
import io.helidon.dbclient.DbMapper;
import io.helidon.dbclient.DbRow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record ChatMapper() implements DbMapper<Chat> {

    @Override
    public Chat read(DbRow row) {
        DbColumn id = row.column("id");
        DbColumn title = row.column("title");
        DbColumn lastActivityTimestamp = row.column("last_activity");
        DbColumn llmId = row.column("llm_id");
        return new Chat(id.get(Integer.class), title.get(String.class), lastActivityTimestamp.get(Integer.class), llmId.get(Integer.class));
    }

    @Override
    public Map<String, Object> toNamedParameters(Chat value) {
        Map<String, Object> map = new HashMap<>(4);
        map.put("id", value.id());
        map.put("title", value.title());
        map.put("lastActivity", value.lastActivity());
        map.put("llmId", value.llmId());
        return map;
    }

    @Override
    public List<Object> toIndexedParameters(Chat value) {
        List<Object> list = new ArrayList<>(4);
        list.add(value.id());
        list.add(value.title());
        list.add(value.lastActivity());
        list.add(value.llmId());
        return list;
    }
}
