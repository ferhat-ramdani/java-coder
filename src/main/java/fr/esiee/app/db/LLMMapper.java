package fr.esiee.app.db;

import io.helidon.dbclient.DbColumn;
import io.helidon.dbclient.DbMapper;
import io.helidon.dbclient.DbRow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record LLMMapper() implements DbMapper<LLM> {

    @Override
    public LLM read(DbRow row) {
        DbColumn id = row.column("id");
        DbColumn name = row.column("name");
        return new LLM(id.get(Integer.class), name.get(String.class));
    }

    @Override
    public Map<String, Object> toNamedParameters(LLM value) {
        Map<String, Object> map = new HashMap<>(2);
        map.put("id", value.id());
        map.put("name", value.name());
        return map;
    }

    @Override
    public List<Object> toIndexedParameters(LLM value) {
        List<Object> list = new ArrayList<>(2);
        list.add(value.id());
        list.add(value.name());
        return list;
    }
}
