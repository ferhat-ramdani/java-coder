package fr.esiee.app.config;

public record LLMConfig(String url, int port, String version) {

  public static LLMConfig defaultConfig() {
    return new LLMConfig("127.0.0.1", 14454, "v0.4.1");
  }

  /**
   * Constructs the base URL by ensuring it starts with "http://" if it does not already
   * start with "http://" or "https://", and appends the port.
   *
   * @return the constructed base URL as a String
   */
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
