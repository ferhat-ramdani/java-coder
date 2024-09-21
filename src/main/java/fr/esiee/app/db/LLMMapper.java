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
        DbColumn model = row.column("model");
        return new LLM(id.get(Integer.class), name.get(String.class), model.get(String.class));
    }

    @Override
    public Map<String, Object> toNamedParameters(LLM value) {
        Map<String, Object> map = new HashMap<>(3);
        map.put("id", value.id());
        map.put("name", value.name());
        map.put("model", value.model());
        return map;
    }

    @Override
    public List<Object> toIndexedParameters(LLM value) {
        List<Object> list = new ArrayList<>(3);
        list.add(value.id());
        list.add(value.name());
        list.add(value.model());
        return list;
    }
}
