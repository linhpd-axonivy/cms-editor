package com.axonivy.utils.cmseditor.repo;

import java.util.Map;
import java.util.stream.Collectors;

import com.axonivy.utils.cmseditor.model.SavedCms;

import ch.ivyteam.ivy.environment.Ivy;

public class SavedCmsRepo {

	public static Map<String, Map<String, SavedCms>> findAll() {
		return Ivy.repo().search(SavedCms.class).execute().getAll().stream()
				.collect(Collectors.groupingBy(SavedCms::getUri, Collectors.toMap(SavedCms::getLocale, cms -> cms)));
//				.textFields("uri").containsAllWords(requireNonNull(uri, "uri must not null"))
//				.and().textField("locale").containsAllWords(requireNonNull(locale, "locale must not null").toString())
//				.execute().getFirst();
	}
}
