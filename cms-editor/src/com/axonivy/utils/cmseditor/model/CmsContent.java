package com.axonivy.utils.cmseditor.model;

import java.io.Serializable;
import java.util.Locale;

import com.axonivy.utils.cmseditor.utils.Utils;

public class CmsContent implements Serializable {

  private static final long serialVersionUID = 1830742314488808118L;

  private int index;

  private Locale locale;

  private String originalContent;

  private String content;

  private boolean isEditting;

  private boolean isHtml;

  public CmsContent(int index, Locale locale, String content) {
    super();
    this.index = index;
    this.locale = locale;
    this.originalContent = content;
    this.content = content;
    this.isEditting = false;
    this.isHtml = Utils.containsHtmlTag(originalContent);
  }

  public int getIndex() {
    return index;
  }

  public void setIndex(int index) {
    this.index = index;
  }

  public Locale getLocale() {
    return locale;
  }

  public void setLocale(Locale locale) {
    this.locale = locale;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    // do nothing. ignore value when submit form, just accept only click save
  }

  public void saveContent(String contents) {
    this.content = Utils.sanitizeContent(originalContent, contents);
    this.isEditting = false;
  }

  public boolean isEditting() {
    return isEditting;
  }

  public void setEditting(boolean isEditting) {
    this.isEditting = isEditting;
  }

  public String getOriginalContent() {
    return originalContent;
  }

  public void setOriginalContent(String originalContent) {
    this.originalContent = originalContent;
  }

  public boolean isHtml() {
    return isHtml;
  }

}
