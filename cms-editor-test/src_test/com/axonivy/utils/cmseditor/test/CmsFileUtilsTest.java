package com.axonivy.utils.cmseditor.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.primefaces.model.StreamedContent;

import com.axonivy.utils.cmseditor.model.Cms;
import com.axonivy.utils.cmseditor.model.CmsContent;
import com.axonivy.utils.cmseditor.model.PmvCms;
import com.axonivy.utils.cmseditor.utils.CmsFileUtils;

public class CmsFileUtilsTest {

  private Map<String, PmvCms> cmsPmvMap;

  @BeforeEach
  public void setUp() {
    cmsPmvMap = new HashMap<>();
    PmvCms pmvCms = new PmvCms("PmvName", Collections.singletonList(Locale.ENGLISH));
    pmvCms.setCmsList(Collections.singletonList(createMockCms()));
    cmsPmvMap.put("testKey", pmvCms);
  }

  private Cms createMockCms() {
    Cms cms = new Cms();
    cms.setUri("testUri");
    CmsContent content = new CmsContent(0, Locale.ENGLISH, "testContent");
    cms.setContents(Collections.singletonList(content));
    return cms;
  }

  @Test
  public void testWriteCmsToZipStreamedContent() throws Exception {
    StreamedContent result = CmsFileUtils.writeCmsToZipStreamedContent("testApp", cmsPmvMap);
    assertNotNull(result);
    assertEquals("application/zip", result.getContentType());
    assertTrue(result.getName().startsWith("testApp"));
    assertTrue(result.getName().endsWith(".zip"));

    // Further checks to ensure the content is as expected
    try (ByteArrayInputStream bais = (ByteArrayInputStream) result.getStream().get();
        var zipInputStream = new java.util.zip.ZipInputStream(bais)) {
      java.util.zip.ZipEntry zipEntry = zipInputStream.getNextEntry();
      assertNotNull(zipEntry);
      assertEquals("testKey.xlsx", zipEntry.getName());
    }
  }

  @Test
  public void testConvertToZip() throws Exception {
    Map<String, Workbook> workbooks = new HashMap<>();
    Workbook workbook = new XSSFWorkbook();
    workbooks.put("testKey", workbook);

    StreamedContent result = CmsFileUtils.convertToZip("testApp", workbooks);
    assertNotNull(result);
    assertEquals("application/zip", result.getContentType());
    assertTrue(result.getName().startsWith("testApp"));
    assertTrue(result.getName().endsWith(".zip"));

    // Further checks to ensure the content is as expected
    try (ByteArrayInputStream bais = (ByteArrayInputStream) result.getStream().get();
        var zipInputStream = new java.util.zip.ZipInputStream(bais)) {
      java.util.zip.ZipEntry zipEntry = zipInputStream.getNextEntry();
      assertNotNull(zipEntry);
      assertEquals("testKey.xlsx", zipEntry.getName());
    }
  }

}

