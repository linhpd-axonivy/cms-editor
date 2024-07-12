package com.axonivy.utils.cmseditor.managedbean;

import static ch.ivyteam.ivy.environment.Ivy.cms;
import static java.lang.Integer.valueOf;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static javax.faces.application.FacesMessage.SEVERITY_INFO;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import org.apache.commons.lang3.StringUtils;
import org.primefaces.PF;
import org.primefaces.PrimeFaces;
import org.primefaces.model.StreamedContent;

import com.axonivy.utils.cmseditor.model.Cms;
import com.axonivy.utils.cmseditor.model.CmsContent;
import com.axonivy.utils.cmseditor.model.PmvCms;
import com.axonivy.utils.cmseditor.model.SavedCms;
import com.axonivy.utils.cmseditor.repo.SavedCmsRepo;
import com.axonivy.utils.cmseditor.utils.CmsFileUtils;
import com.axonivy.utils.cmseditor.utils.Utils;

import ch.ivyteam.ivy.application.ActivityState;
import ch.ivyteam.ivy.application.IActivity;
import ch.ivyteam.ivy.application.IProcessModel;
import ch.ivyteam.ivy.application.app.IApplicationRepository;
import ch.ivyteam.ivy.cm.ContentObject;
import ch.ivyteam.ivy.cm.ContentObjectReader;
import ch.ivyteam.ivy.cm.ContentObjectValue;
import ch.ivyteam.ivy.cm.exec.ContentManagement;

@ViewScoped
@ManagedBean
public class CmsEditorBean implements Serializable {
  private static final long serialVersionUID = 1L;

  private static final String CONTENT_FORM_SELECTED_URL = "content-form:selected-url";
  private static final String CONTENT_FORM_CMS_VALUES = "content-form:cms-values";
  private static final String CONTENT_FORM_TABLE_CMS_KEYS = "content-form:table-cms-keys";
  private static final String TODO = "TODO";
  private static final String CMS_EDITOR_PMV_NAME = "cms-editor";
  private static final String CMS_EDITOR_DEMO_PMV_NAME = "cms-editor-demo";

  private Map<String, Map<String, PmvCms>> appPmvCmsMap;
  private Map<String, Map<String, SavedCms>> savedCmsMap;
  private List<Cms> cmsList;
  private List<Cms> filteredCMSList;
  private Cms lastSelectedCms;
  private Cms selectedCms;
  private String selectedAppName;
  private String searchKey;
  private boolean isShowOnlyTodo;
  private StreamedContent fileDownload;

  @PostConstruct
  private void init() {
    savedCmsMap = SavedCmsRepo.findAll();
    appPmvCmsMap = new HashMap<>();
    for (var app : IApplicationRepository.instance().all()) {
      app.getProcessModels().stream().filter(processModel -> isActive(processModel))
          .map(IProcessModel::getReleasedProcessModelVersion).filter(pmv -> isActive(pmv))
          .forEach(pmv -> getAllChildren(app.getName(), pmv.getName(), ContentManagement.cms(pmv).root(),
              new ArrayList<>()));
    }
    selectedAppName = appPmvCmsMap.keySet().stream().findFirst().orElse(null);
    onAppChange();
  }

  public void search() {
    if (isEditing()) {
      return;
    }
    filteredCMSList = cmsList.stream().filter(entry -> isCmsMatchSearchKey(entry, searchKey) && isTodoCms(entry))
        .collect(Collectors.toList());
    if (selectedCms != null) {
      selectedCms = filteredCMSList.stream().filter(entry -> entry.getUri().equals(selectedCms.getUri())).findAny()
          .orElse(null);
    }
  }

  public void onAppChange() {
    cmsList = Optional.ofNullable(selectedAppName).map(app -> appPmvCmsMap.get(selectedAppName).values().stream()
        .map(PmvCms::getCmsList).flatMap(List::stream).toList()).orElse(new ArrayList<>());
    search();
  }

  public void rowSelect() {
    if (isEditing()) {
      selectedCms = lastSelectedCms; // Revert to last valid selection
    } else {
      PF.current().ajax().update(CONTENT_FORM_CMS_VALUES, CONTENT_FORM_SELECTED_URL);
    }
  }

  private boolean isEditing() {
    var isEditing = lastSelectedCms != null && lastSelectedCms.isEditing();
    if (isEditing) {
      showHaveNotBeenSavedDialog();
      PF.current().ajax().update(CONTENT_FORM_TABLE_CMS_KEYS);
    }
    return isEditing;
  }

  private void showHaveNotBeenSavedDialog() {
    var editingCmsList = lastSelectedCms.getContents().stream().filter(CmsContent::isEditting)
        .map(CmsContent::getLocale).map(Locale::getDisplayLanguage).collect(Collectors.toList());
    var detail = Utils.convertListToHTMLList(editingCmsList);
    showDialog(cms().co("/Labels/SomeFieldsHaveNotBeenSaved"), detail);
  }

  private void showDialog(String summary, String detail) {
    var message = new FacesMessage(SEVERITY_INFO, summary, detail);
    PrimeFaces.current().dialog().showMessageDynamic(message, false);
  }

  public void getAllChildren(String appName, String pmvName, ContentObject contentObject, List<Locale> locales) {
    // Exclude the CMS of it self
    if (StringUtils.contains(pmvName, CMS_EDITOR_PMV_NAME)
        && !StringUtils.contains(pmvName, CMS_EDITOR_DEMO_PMV_NAME)) {
      return;
    }
    if (contentObject.isRoot()) {
      locales = contentObject.cms().locales().stream().filter(locale -> isNotBlank(locale.getLanguage()))
          .collect(toList());
    }
    var pmvCmsMap = appPmvCmsMap.getOrDefault(appName, new HashMap<>());
    for (ContentObject child : contentObject.children()) {
      if (child.children().size() == 0) {
        // just allow string cms. not file
        if (StringUtils.isBlank(child.meta().fileExtension())) {
          var cms = convertToCms(child, locales);
          if (cms.getContents() != null) {
            var contents = pmvCmsMap.getOrDefault(pmvName, new PmvCms(pmvName, locales));
            contents.addCms(cms);
            pmvCmsMap.putIfAbsent(pmvName, contents);
          }
        }
      }
      appPmvCmsMap.putIfAbsent(appName, pmvCmsMap);
      getAllChildren(appName, pmvName, child, locales);
    }
  }

  private Cms convertToCms(ContentObject contentObject, List<Locale> locales) {
    var cms = new Cms();
    cms.setUri(contentObject.uri());
    for (var i = 0; i < locales.size(); i++) {
      var locale = locales.get(i);
      var value = contentObject.value().get(locale);
      var valueString = ofNullable(value).map(ContentObjectValue::read).map(ContentObjectReader::string).orElse(EMPTY);
      var savedCms = findSavedCms(contentObject.uri(), locale);
      if (savedCms != null) {
        if (valueString.equals(savedCms.getOriginalContent())) {
          valueString = savedCms.getNewContent();
        } else {
          SavedCmsRepo.delete(savedCms);
        }
      }
      cms.addContent(new CmsContent(i, locale, valueString));
    }
    return cms;
  }

  private static boolean isActive(IActivity processModelVersion) {
    return processModelVersion != null && ActivityState.ACTIVE == processModelVersion.getActivityState();
  }

  private boolean isCmsMatchSearchKey(Cms entry, String searchKey) {
    if (StringUtils.isNotBlank(searchKey)) {
      return StringUtils.containsIgnoreCase(entry.getUri(), searchKey) || entry.getContents().stream()
          .anyMatch(value -> StringUtils.containsIgnoreCase(value.getContent(), searchKey));
    }
    return true;
  }

  private boolean isTodoCms(Cms entry) {
    if (isShowOnlyTodo) {
      return entry.getContents().stream().anyMatch(value -> StringUtils.containsIgnoreCase(value.getContent(), TODO));
    }
    return true;
  }

  private SavedCms findSavedCms(String uri, Locale locale) {
    return savedCmsMap.getOrDefault(uri, new HashMap<>()).getOrDefault(locale.toString(), null);
  }

  private void saveCms(SavedCms savedCms) {
    SavedCms savedCmsResult = SavedCmsRepo.save(savedCms);
    Map<String, SavedCms> cmsLocaleMap = savedCmsMap.computeIfAbsent(savedCmsResult.getUri(), key -> new HashMap<>());
    cmsLocaleMap.put(savedCmsResult.getLocale(), savedCmsResult);
  }

  public void save() {
    var context = FacesContext.getCurrentInstance();
    var requestParamMap = context.getExternalContext().getRequestParameterMap();
    var languageIndex = valueOf(requestParamMap.get("languageIndex"));
    selectedCms.getContents().stream().filter(value -> value.getIndex() == languageIndex).findAny()
        .ifPresent(cmsContent -> handleCmsContentSave(requestParamMap, cmsContent));
  }

  private void handleCmsContentSave(Map<String, String> requestParamMap, CmsContent cmsContent) {
    cmsContent.saveContent(requestParamMap.get("contents"));
    var locale = cmsContent.getLocale();
    var savedCms = findSavedCms(selectedCms.getUri(), locale);
    if (savedCms != null) {
      savedCms.setNewContent(cmsContent.getContent());
    } else {
      savedCms = new SavedCms(selectedCms.getUri(), locale.toString(), cmsContent.getOriginalContent(),
          cmsContent.getContent());
    }
    saveCms(savedCms);
  }

  public void setValueChanged() {
    var context = FacesContext.getCurrentInstance();
    var requestParamMap = context.getExternalContext().getRequestParameterMap();
    var languageIndex = valueOf(requestParamMap.get("languageIndex"));
    selectedCms.getContents().get(languageIndex).setEditting(true);
  }

  public void handleBeforeDownloadFile() throws Exception {
    this.fileDownload = CmsFileUtils.writeCmsToZipStreamedContent(selectedAppName, appPmvCmsMap.get(selectedAppName));
  }

  public void downloadFinished() {
    showDialog(cms().co("/Labels/Message"), cms().co("/Labels/CmsDownloaded"));
  }

  public String getActiveIndex() {
    return Optional.ofNullable(selectedCms).map(Cms::getContents).map(
        values -> IntStream.rangeClosed(0, values.size()).mapToObj(Integer::toString).collect(Collectors.joining(",")))
        .orElse(StringUtils.EMPTY);
  }

  public List<Cms> getFilteredCMSKeys() {
    return filteredCMSList;
  }

  public void setFilteredCMSKeys(List<Cms> filteredCMSKeys) {
    this.filteredCMSList = filteredCMSKeys;
  }

  public Cms getSelectedCms() {
    return selectedCms;
  }

  public void setSelectedCms(Cms selectedCms) {
    this.lastSelectedCms = this.selectedCms == null ? selectedCms : this.selectedCms;
    this.selectedCms = selectedCms;
  }

  public boolean isShowOnlyTodo() {
    return isShowOnlyTodo;
  }

  public void setShowOnlyTodo(boolean isShowOnlyTodo) {
    this.isShowOnlyTodo = isShowOnlyTodo;
  }

  public String getSearchKey() {
    return searchKey;
  }

  public void setSearchKey(String searchKey) {
    this.searchKey = searchKey;
  }

  public StreamedContent getFileDownload() {
    return fileDownload;
  }

  public String getSelectedAppName() {
    return selectedAppName;
  }

  public void setSelectedAppName(String selectedAppName) {
    this.selectedAppName = selectedAppName;
  }

  public Set<String> getAppNames() {
    return appPmvCmsMap.keySet();
  }

}
