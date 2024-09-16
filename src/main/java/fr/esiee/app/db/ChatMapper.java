package fr.esiee.app.db;

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
        return new Chat(id.get(Integer.class), title.get(String.class));
    }

    @Override
    public Map<String, Object> toNamedParameters(Chat value) {
        Map<String, Object> map = new HashMap<>(3);
        map.put("id", value.id());
        map.put("title", value.title());
        return map;
    }

    @Override
    public List<Object> toIndexedParameters(Chat value) {
        List<Object> list = new ArrayList<>(3);
        list.add(value.id());
        list.add(value.title());
        return list;
    }
}
