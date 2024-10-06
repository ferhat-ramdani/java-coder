package fr.esiee.app.config;

public record LLMProviderConfig(String url, int port) {

  public static LLMProviderConfig defaultConfig() {
    return new LLMProviderConfig("http://localhost", 11434);
  }

  public String baseUrl() {
    var tmpUrl = url;
    if (!url.startsWith("http://") || !url.startsWith("https://")) {
      tmpUrl = "http://" + url;
    }
    return tmpUrl + ":" + port;
  }

}
