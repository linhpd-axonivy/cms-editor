package com.axonivy.utils.cmseditor.utils;

import static java.util.stream.Collectors.toList;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang3.StringUtils;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import com.aspose.cells.Row;
import com.aspose.cells.SaveFormat;
import com.aspose.cells.Workbook;
import com.aspose.cells.Worksheet;
import com.axonivy.utils.cmseditor.model.Cms;
import com.axonivy.utils.cmseditor.model.CmsContent;
import com.axonivy.utils.cmseditor.model.PmvCms;

public class CmsFileUtils {

	private static final String SHEET_NAME = "cms";
	private static final String URI_HEADER = "Uri";
	private static final String ZIP_CONTENT_TYPE = "application/zip";
	private static final String TIMESTAMP_FORMAT = "yyyyMMdd_HHmmss";
	private static final String EXCEL_FILE_NAME = "%s.xlsx";
	private static final String ZIP_FILE_NAME = "%s_%s.zip";

	public static StreamedContent writeCmsToZipStreamedContent(String appName, Map<String, PmvCms> cmsPmvMap)
			throws Exception {
		Map<String, Workbook> workBooks = new HashMap<String, Workbook>();
		for (var entry : cmsPmvMap.entrySet()) {
			var cmsList = entry.getValue().getCmsList();
			List<String> headers = new ArrayList<>();
			headers.add(URI_HEADER);
			headers.addAll(entry.getValue().getLocales().stream().map(Locale::getLanguage)
					.filter(StringUtils::isNotBlank).collect(toList()));
			Workbook workbook = new Workbook(SaveFormat.XLSX);
			Worksheet worksheet = workbook.getWorksheets().get(0); // Assuming only one worksheet
			worksheet.setName(SHEET_NAME);
			// start save data first
			for (int rowCount = 1; rowCount <= cmsList.size(); rowCount++) {
				Row row = worksheet.getCells().getRows().get(rowCount);
				// second row is first cms
				Cms cms = cmsList.get(rowCount - 1);
				for (int columnCount = 0; columnCount < headers.size(); columnCount++) {
					// set uri
					if (columnCount == 0) {
						row.get(columnCount).setValue(cms.getUri());
					} else {
						row.get(columnCount).setValue(getContentValue(cms, headers.get(columnCount)));
					}
				}
			}
			// save header
			Row row = worksheet.getCells().getRows().get(0);
			for (int column = 0; column < headers.size(); column++) {
				row.get(column).setValue(headers.get(column));
			}

			workBooks.put(entry.getKey(), workbook);
		}
		return convertToZip(appName, workBooks);
	}

	private static String getContentValue(Cms cms, String language) {
		var cmsContent = cms.getContents().stream()
				.filter(content -> StringUtils.equals(content.getLocale().getLanguage(), language)).findFirst()
				.orElse(null);
		return Optional.ofNullable(cmsContent).map(CmsContent::getContent).orElse(StringUtils.EMPTY);
	}

	public static StreamedContent convertToZip(String appName, Map<String, Workbook> workbooks) throws Exception {
		String timestamp = new SimpleDateFormat(TIMESTAMP_FORMAT).format(new Date());
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ZipOutputStream zipOut = new ZipOutputStream(baos);) {

			for (Entry<String, Workbook> entry : workbooks.entrySet()) {
				String fileName = String.format(EXCEL_FILE_NAME, entry.getKey());
				zipOut.putNextEntry(new ZipEntry(fileName));
				zipOut.write(convertWorkbookToByteArray(entry.getValue()));
				zipOut.closeEntry();
			}

			zipOut.close();
			byte[] zipBytes = baos.toByteArray();

			return DefaultStreamedContent.builder().name(String.format(ZIP_FILE_NAME, appName, timestamp))
					.contentType(ZIP_CONTENT_TYPE).stream(() -> new ByteArrayInputStream(zipBytes)).build();
		} finally {
			workbooks.forEach((pmv, workbook) -> {
				if (workbook != null) {
					workbook.dispose();
				}
			});
		}
	}

	private static byte[] convertWorkbookToByteArray(Workbook workbook) throws Exception {
		try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			workbook.save(outputStream, SaveFormat.XLSX);
			return outputStream.toByteArray();
		}
	}
}
