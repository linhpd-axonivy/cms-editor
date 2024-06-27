package com.axonivy.utils.cmseditor.repo;

import static java.util.Objects.requireNonNull;

import java.util.Locale;

import com.axonivy.utils.cmseditor.model.SavedCms;

import ch.ivyteam.ivy.environment.Ivy;

public class SavedCmsRepo {

	public static SavedCms findSavedCms(String uri, Locale locale) {
		Ivy.repo().search(SavedCms.class).execute().getAll().forEach(it -> Ivy.repo().delete(it));
		return Ivy.repo().search(SavedCms.class)
				.textFields("uri").containsAllWords(requireNonNull(uri, "uri must not null"))
				.and().textField("locale").containsAllWords(requireNonNull(locale, "locale must not null").toString())
				.execute().getFirst();
	}
}
