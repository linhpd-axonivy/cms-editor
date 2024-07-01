package com.axonivy.utils.cmseditor.utils;

import java.util.List;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities.EscapeMode;

public class Utils {

	private static final String HTML_TAG_PATTERN = "<.*?>";
	private static final String REMOVE_HTML_TAG_PATTERN = "<[^>]*>";
	private static final String TABLE_ELEMENT = "table";

	public static String reformatHTML(String originalContent, String content) {
		if (containsHtmlTag(originalContent)) {
			Document originalDoc = Jsoup.parseBodyFragment(content);
			Document doc = Jsoup.parseBodyFragment(content);
			migrateTableAttr(originalDoc, doc);
			doc.outputSettings().escapeMode(EscapeMode.xhtml).prettyPrint(true);
			return doc.body().html();
		} else {
			return removeTags(content);
		}
	}

	public static boolean containsHtmlTag(String str) {
		Pattern pattern = Pattern.compile(HTML_TAG_PATTERN);
		return pattern.matcher(str).find();
	}

	public static String removeTags(String text) {
		Pattern pattern = Pattern.compile(REMOVE_HTML_TAG_PATTERN);
		return pattern.matcher(text).replaceAll("");
	}

	private static void migrateTableAttr(Document originalDoc, Document doc) {
		List<Element> originalTables = doc.select(TABLE_ELEMENT);
		List<Element> tables = doc.select(TABLE_ELEMENT);
		int minSize = Math.min(originalTables.size(), tables.size());

		for (int i = 0; i < minSize; i++) {
			Element originalTable = originalTables.get(i);
			Element targetTable = tables.get(i);

			// Copy attributes from originalTable to targetTable
			for (Attribute attr : originalTable.attributes()) {
				targetTable.attr(attr.getKey(), attr.getValue());
			}
		}
	}

	public static String convertListToHTMLList(List<String> stringList) {
		final String unorderedPattern = "<ul> %s </ul>";
		final String listItemPattern = "<li style='padding:0 2rem 0.25rem 0;'> %s </li>";

		StringBuilder htmlStringBuilder = new StringBuilder();
		for (String item : stringList) {
			htmlStringBuilder.append(String.format(listItemPattern, item));
		}

		return String.format(unorderedPattern, htmlStringBuilder.toString());
	}
}
