package fr.esiee.app.db;

import io.helidon.dbclient.DbColumn;
import io.helidon.dbclient.DbMapper;
import io.helidon.dbclient.DbRow;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record ChatMapper() implements DbMapper<Chat> {

    @Override
    public Chat read(DbRow row) {
        DbColumn id = row.column("id");
        DbColumn title = row.column("title");
        DbColumn lastActivityTimestamp = row.column("last_activity_timestamp");
        DbColumn llmId = row.column("llm_id");
        return new Chat(id.get(Integer.class), title.get(String.class), lastActivityTimestamp.get(Timestamp.class), llmId.get(Integer.class));
    }

    @Override
    public Map<String, Object> toNamedParameters(Chat value) {
        Map<String, Object> map = new HashMap<>(4);
        map.put("id", value.id());
        map.put("title", value.title());
        map.put("last_activity_timestamp", value.lastAcitivityTimestamp());
        map.put("llm_id", value.llmId());
        return map;
    }

    @Override
    public List<Object> toIndexedParameters(Chat value) {
        List<Object> list = new ArrayList<>(4);
        list.add(value.id());
        list.add(value.title());
        list.add(value.lastAcitivityTimestamp());
        list.add(value.llmId());
        return list;
    }
}
