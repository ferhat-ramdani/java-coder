package fr.esiee.app.db.mapper;

import fr.esiee.app.db.entities.LLM;
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
        var id = row.column("id");
        var name = row.column("name");
        var model = row.column("model");
        var systemPrompt = row.column("system_prompt");
        var caracteristics = row.column("caracteristics");
        return new LLM(id.get(Integer.class), name.get(String.class), model.get(String.class), systemPrompt.get(String.class), caracteristics.get(String.class));
    }

    @Override
    public Map<String, Object> toNamedParameters(LLM value) {
        Map<String, Object> map = new HashMap<>(3);
        map.put("id", value.id());
        map.put("name", value.name());
        map.put("model", value.model());
        map.put("systemPrompt", value.systemPrompt());
        map.put("caracteristics", value.caracteristics());
        return map;
    }

    @Override
    public List<Object> toIndexedParameters(LLM value) {
        List<Object> list = new ArrayList<>(3);
        list.add(value.id());
        list.add(value.name());
        list.add(value.model());
        list.add(value.systemPrompt());
        list.add(value.caracteristics());
        return list;
    }
}
