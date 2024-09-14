package fr.esiee.app.db;

public record Prompt(int id, String userMessage, String llmResponse, int chatId, int llmId) {
}
