package fr.esiee.app.utils.llms;

public record LLMConfig(String url, int port) {

  public static LLMConfig defaultConfig() {
    return new LLMConfig("127.0.0.1", 14454);
  }

  public String baseUrl() {
    var tmpUrl = url;
    if (!url.startsWith("http://") || !url.startsWith("https://")) {
      tmpUrl = "http://" + url;
    }
    return tmpUrl + ":" + port;
  }

  public String urlAndPort() {
    return url + ":" + port;
  }

}
