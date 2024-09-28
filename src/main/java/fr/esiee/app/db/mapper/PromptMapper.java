package fr.esiee.app.db.mapper;

import fr.esiee.app.db.entities.AuthorType;
import fr.esiee.app.db.entities.Prompt;
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
        DbColumn message = row.column("message");
        DbColumn authorType = row.column("author_type");
        DbColumn llmResponse = row.column("llm_response");
        DbColumn chatId = row.column("chat_id");
        DbColumn llmId = row.column("llm_id");
        return new Prompt(
                id.get(Integer.class),
                message.get(String.class),
                AuthorType.valueOf(authorType.get(String.class)),
                llmResponse.get(String.class),
                chatId.get(Integer.class),
                llmId.get(Integer.class)
        );
    }

    @Override
    public Map<String, Object> toNamedParameters(Prompt value) {
        Map<String, Object> map = new HashMap<>(6);
        map.put("id", value.id());
        map.put("message", value.message());
        map.put("authorType", value.authorType().name());
        map.put("llmResponse", value.llmResponse());
        map.put("chatId", value.chatId());
        map.put("llmId", value.llmId());
        return map;
    }

    @Override
    public List<Object> toIndexedParameters(Prompt value) {
        List<Object> list = new ArrayList<>(6);
        list.add(value.id());
        list.add(value.message());
        list.add(value.authorType().name());
        list.add(value.llmResponse());
        list.add(value.chatId());
        list.add(value.llmId());
        return list;
    }
}
