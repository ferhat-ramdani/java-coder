package fr.esiee.app.config;

import java.util.List;

public class LLMElem {
  public String url;
  public List<String> models;

  public LLMElem(String url, List<String> models) {
    this.url = url;
    this.models = models;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public void setModels(List<String> models) {
    this.models = models;
  }

  public List<String> getModels() {
    return models;
  }

  @Override
  public String toString() {
    return "LLMElem{" +
            "url='" + url + '\'' +
            ", models=" + models +
            '}';
  }
}
