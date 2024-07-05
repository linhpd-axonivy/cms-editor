package com.axonivy.utils.cmseditor.repo;

import java.util.Map;
import java.util.stream.Collectors;

import com.axonivy.utils.cmseditor.model.SavedCms;

import ch.ivyteam.ivy.environment.Ivy;

public class SavedCmsRepo {

  public static Map<String, Map<String, SavedCms>> findAll() {
    return Ivy.repo().search(SavedCms.class).execute().getAll().stream()
        .collect(Collectors.groupingBy(SavedCms::getUri, Collectors.toMap(SavedCms::getLocale, cms -> cms)));
  }

  public static void delete(SavedCms savedCms) {
    Ivy.repo().delete(savedCms);
  }

  public static SavedCms save(SavedCms savedCms) {
    return Ivy.repo().find(Ivy.repo().save(savedCms).getId(), SavedCms.class);
  }
}
