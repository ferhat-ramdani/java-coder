package fr.esiee.app.config;

public record LLMConfig(String url, int port) {

  public static LLMConfig defaultConfig() {
    return new LLMConfig("http://localhost", 11434);
  }

  public String baseUrl() {
    var tmpUrl = url;
    if (!url.startsWith("http://") || !url.startsWith("https://")) {
      tmpUrl = "http://" + url;
    }
    return tmpUrl + ":" + port;
  }
}
