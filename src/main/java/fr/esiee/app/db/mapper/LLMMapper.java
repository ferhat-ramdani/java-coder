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
        var temp = row.column("temp");
        var seed = row.column("seed");
        return new LLM(id.getInt(),
                name.getString(),
                model.getString(),
                systemPrompt.getString(),
                caracteristics.getString(),
                temp.getDouble(),
                seed.getInt()
        );
    }

    @Override
    public Map<String, Object> toNamedParameters(LLM value) {
        Map<String, Object> map = new HashMap<>(3);
        map.put("id", value.id());
        map.put("name", value.name());
        map.put("model", value.model());
        map.put("systemPrompt", value.systemPrompt());
        map.put("caracteristics", value.caracteristics());
        map.put("temp", value.temp());
        map.put("seed", value.seed());
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
        list.add(value.temp());
        list.add(value.seed());
        return list;
    }
}
