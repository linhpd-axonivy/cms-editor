package com.axonivy.utils.cmseditor.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Cms implements Serializable {

  private static final long serialVersionUID = -88931664585615316L;

  private String uri;

  private List<CmsContent> contents;

  public String getUri() {
    return uri;
  }

  public void setUri(String uri) {
    this.uri = uri;
  }

  public List<CmsContent> getContents() {
    return contents;
  }

  public void setContents(List<CmsContent> contents) {
    this.contents = contents;
  }

  public void addContent(CmsContent content) {
    if (contents == null) {
      contents = new ArrayList<>();
    }
    contents.add(content);
  }

  public boolean isEditing() {
    return contents.stream().anyMatch(CmsContent::isEditting);
  }

}