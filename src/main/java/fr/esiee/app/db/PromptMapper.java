package fr.esiee.app.db;

import io.helidon.dbclient.DbColumn;
import io.helidon.dbclient.DbMapper;
import io.helidon.dbclient.DbRow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record PromptMapper() implements DbMapper<Prompt> {

    @Override
    public Prompt read(DbRow row) {
        DbColumn id = row.column("id");
        DbColumn userMessage = row.column("user_message");
        DbColumn llmResponse = row.column("llm_response");
        DbColumn idChat = row.column("id_chat");
        DbColumn idLLM = row.column("id_llm");

        return new Prompt(
                id.get(Integer.class),
                userMessage.get(String.class),
                llmResponse.get(String.class),
                idChat.get(Integer.class),
                idLLM.get(Integer.class)
        );
    }

    @Override
    public Map<String, Object> toNamedParameters(Prompt value) {
        Map<String, Object> map = new HashMap<>(5);
        map.put("id", value.id());
        map.put("user_message", value.userMessage());
        map.put("llm_response", value.llmResponse());
        map.put("id_chat", value.chatId());
        map.put("id_llm", value.llmId());
        return map;
    }

    @Override
    public List<Object> toIndexedParameters(Prompt value) {
        List<Object> list = new ArrayList<>(5);
        list.add(value.id());
        list.add(value.userMessage());
        list.add(value.llmResponse());
        list.add(value.chatId());
        list.add(value.llmId());
        return list;
    }
}
