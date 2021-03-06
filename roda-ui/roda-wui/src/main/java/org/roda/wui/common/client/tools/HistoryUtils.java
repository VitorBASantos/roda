/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/keeps/roda
 */
/**
 * 
 */
package org.roda.wui.common.client.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.utils.RepresentationInformationUtils;
import org.roda.core.data.v2.index.IsIndexed;
import org.roda.core.data.v2.ip.DIPFile;
import org.roda.core.data.v2.ip.IndexedAIP;
import org.roda.core.data.v2.ip.IndexedDIP;
import org.roda.core.data.v2.ip.IndexedFile;
import org.roda.core.data.v2.ip.IndexedRepresentation;
import org.roda.core.data.v2.ip.TransferredResource;
import org.roda.core.data.v2.jobs.IndexedReport;
import org.roda.core.data.v2.jobs.Job;
import org.roda.core.data.v2.notifications.Notification;
import org.roda.core.data.v2.ri.RepresentationInformation;
import org.roda.core.data.v2.risks.IndexedRisk;
import org.roda.core.data.v2.risks.RiskIncidence;
import org.roda.wui.client.browse.BrowseAIP;
import org.roda.wui.client.browse.BrowseDIP;
import org.roda.wui.client.browse.BrowseFile;
import org.roda.wui.client.browse.BrowseRepresentation;
import org.roda.wui.client.browse.BrowserService;
import org.roda.wui.client.common.utils.AsyncCallbackUtils;
import org.roda.wui.client.ingest.process.ShowJob;
import org.roda.wui.client.ingest.process.ShowJobReport;
import org.roda.wui.client.ingest.transfer.IngestTransfer;
import org.roda.wui.client.ingest.transfer.TransferUpload;
import org.roda.wui.client.management.ShowNotification;
import org.roda.wui.client.planning.ShowRepresentationInformation;
import org.roda.wui.client.planning.ShowRisk;
import org.roda.wui.client.planning.ShowRiskIncidence;
import org.roda.wui.client.search.Search;
import org.roda.wui.common.client.HistoryResolver;
import org.roda.wui.common.client.widgets.Toast;

import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;

/**
 * Useful methods
 * 
 * @author Luis Faria
 */
public class HistoryUtils {

  private HistoryUtils() {

  }

  public static final String HISTORY_SEP = "/";
  public static final String HISTORY_SEP_REGEX = "/";
  public static final String HISTORY_SEP_ESCAPE = "%2F";
  public static final String HISTORY_PERMISSION_SEP = ".";

  public static <T> List<T> tail(List<T> list) {
    return list.subList(1, list.size());
  }

  public static <T> List<T> removeLast(List<T> list) {
    return list.subList(0, list.size() - 1);

  }

  /**
   * Split history string to history path using HISTORY_SEP as the separator
   * 
   * @param history
   * @return the history path
   */
  public static List<String> splitHistory(String history) {
    List<String> historyPath;
    if (history.indexOf(HISTORY_SEP) == -1) {
      historyPath = Arrays.asList(history);
    } else {
      historyPath = Arrays.asList(history.split(HISTORY_SEP_REGEX));
    }
    return historyPath;
  }

  public static List<String> decodeList(List<String> splitted) {
    List<String> tokens = new ArrayList<>();
    for (String item : splitted) {
      tokens.add(URL.decodeQueryString(item));
    }
    return tokens;
  }

  public static List<String> getCurrentHistoryPath() {
    String hash = Window.Location.getHash();
    if (hash.length() > 0) {
      hash = hash.substring(1);
    }
    return decodeList(Arrays.asList(hash.split(HISTORY_SEP_REGEX)));
  }

  public static String createHistoryToken(List<String> tokens) {
    StringBuilder builder = new StringBuilder();
    boolean first = true;
    for (String token : tokens) {
      if (first) {
        first = false;
      } else {
        builder.append(HISTORY_SEP);
      }

      String encodedToken = URL.encodeQueryString(token);
      builder.append(encodedToken);
    }
    return builder.toString();
  }

  public static void newHistory(List<String> path) {
    String hash = createHistoryToken(path);
    Window.Location.assign("#" + hash);
  }

  public static void replaceHistory(List<String> path) {
    String hash = createHistoryToken(path);
    Window.Location.replace("#" + hash);
  }

  public static void newHistory(HistoryResolver resolver) {
    newHistory(resolver.getHistoryPath());
  }

  public static void newHistory(HistoryResolver resolver, String... extrapath) {
    List<String> path = ListUtils.concat(resolver.getHistoryPath(), extrapath);
    newHistory(path);
  }

  public static void newHistory(HistoryResolver resolver, List<String> extrapath) {
    List<String> path = ListUtils.concat(resolver.getHistoryPath(), extrapath);
    newHistory(path);
  }

  public static String createHistoryHashLink(List<String> path) {
    String hash = createHistoryToken(path);
    return "#" + hash;
  }

  public static String createHistoryHashLink(HistoryResolver resolver, String... extrapath) {
    List<String> path = ListUtils.concat(resolver.getHistoryPath(), extrapath);
    return createHistoryHashLink(path);
  }

  public static String createHistoryHashLink(HistoryResolver resolver, List<String> extrapath) {
    List<String> path = ListUtils.concat(resolver.getHistoryPath(), extrapath);
    return createHistoryHashLink(path);
  }

  public static List<String> getHistoryBrowse(String aipId) {
    List<String> history = new ArrayList<>();
    history.addAll(BrowseAIP.RESOLVER.getHistoryPath());
    history.add(aipId);
    return history;
  }

  public static List<String> getHistoryBrowseDIP(String dipId) {
    List<String> history = new ArrayList<>();
    history.addAll(BrowseAIP.RESOLVER.getHistoryPath());
    history.add(BrowseDIP.RESOLVER.getHistoryToken());
    history.add(dipId);
    return history;
  }

  public static List<String> getHistoryBrowseDIPFile(String dipId, String dipFileUUID) {
    List<String> history = new ArrayList<>();
    history.addAll(BrowseAIP.RESOLVER.getHistoryPath());
    history.add(BrowseDIP.RESOLVER.getHistoryToken());
    history.add(dipId);
    history.add(dipFileUUID);
    return history;
  }

  public static List<String> getHistoryBrowse(DIPFile dipFile) {
    return getHistoryBrowseDIPFile(dipFile.getDipId(), dipFile.getUUID());
  }

  public static void openBrowse(IndexedDIP dip) {
    HistoryUtils.newHistory(getHistoryBrowseDIP(dip.getId()));
  }

  public static void openBrowse(DIPFile dipFile) {
    HistoryUtils.newHistory(getHistoryBrowseDIPFile(dipFile.getDipId(), dipFile.getUUID()));
  }

  public static void openBrowseDIP(String dipId) {
    HistoryUtils.newHistory(getHistoryBrowseDIP(dipId));
  }

  public static void openBrowse(String aipId) {
    HistoryUtils.newHistory(getHistoryBrowse(aipId));
  }

  public static void openBrowse(IndexedAIP aip) {
    openBrowse(aip.getId());
  }

  public static List<String> getHistoryBrowse(String aipId, String representationId) {
    List<String> history = new ArrayList<>();
    history.addAll(BrowseAIP.RESOLVER.getHistoryPath());
    history.add(BrowseRepresentation.RESOLVER.getHistoryToken());
    history.add(aipId);
    history.add(representationId);
    return history;
  }

  public static List<String> getHistoryBrowse(IndexedRepresentation representation) {
    return getHistoryBrowse(representation.getAipId(), representation.getId());
  }

  public static void openBrowse(String aipId, String representationId) {
    HistoryUtils.newHistory(getHistoryBrowse(aipId, representationId));
  }

  public static void openBrowse(IndexedRepresentation representation) {
    openBrowse(representation.getAipId(), representation.getId());
  }

  public static List<String> getHistoryBrowse(String aipId, String representationId, List<String> filePath,
    String fileId) {
    List<String> history = new ArrayList<>();
    history.addAll(BrowseAIP.RESOLVER.getHistoryPath());
    history.add(BrowseFile.RESOLVER.getHistoryToken());
    history.add(aipId);
    history.add(representationId);
    history.addAll(filePath);
    history.add(fileId);
    return history;
  }

  public static List<String> getHistoryBrowse(IndexedFile file) {
    return getHistoryBrowse(file.getAipId(), file.getRepresentationId(), file.getPath(), file.getId());
  }

  public static void openBrowse(String aipId, String representationId, List<String> filePath, String fileId) {
    HistoryUtils.newHistory(getHistoryBrowse(aipId, representationId, filePath, fileId));
  }

  public static void openBrowse(IndexedFile file) {
    openBrowse(file.getAipId(), file.getRepresentationId(), file.getPath(), file.getId());
  }

  public static List<String> getHistoryUpload(IndexedFile folder) {
    List<String> history = new ArrayList<>();
    history.addAll(BrowseAIP.RESOLVER.getHistoryPath());
    history.add(TransferUpload.BROWSE_RESOLVER.getHistoryToken());
    history.add(folder.getAipId());
    history.add(folder.getRepresentationId());
    history.addAll(folder.getPath());
    history.add(folder.getId());
    return history;
  }

  public static void openUpload(IndexedRepresentation representation) {
    HistoryUtils.newHistory(getHistoryUpload(representation));
  }

  public static List<String> getHistoryUpload(IndexedRepresentation representation) {
    List<String> history = new ArrayList<>();
    history.addAll(BrowseAIP.RESOLVER.getHistoryPath());
    history.add(TransferUpload.BROWSE_RESOLVER.getHistoryToken());
    history.add(representation.getAipId());
    history.add(representation.getId());
    return history;
  }

  public static void openUpload(IndexedFile folder) {
    if (folder.isDirectory()) {
      HistoryUtils.newHistory(getHistoryUpload(folder));
    }
  }

  public static List<String> getHistory(List<String> resolverPath, String resourceUUID) {
    List<String> history = new ArrayList<>();
    history.addAll(resolverPath);
    history.add(resourceUUID);
    return history;
  }

  public static String getSearchHistoryByRepresentationInformationFilter(List<String> filters, String searchType) {
    List<String> history = new ArrayList<>();
    history.addAll(Search.RESOLVER.getHistoryPath());
    history.add(searchType);
    history.add(RodaConstants.OPERATOR_OR);

    for (String filter : filters) {
      String[] splittedFilter = filter
        .split(RepresentationInformationUtils.REPRESENTATION_INFORMATION_FILTER_SEPARATOR);
      history.add(splittedFilter[1]);
      history.add(splittedFilter[2]);
    }

    return createHistoryHashLink(history);
  }

  public static void resolve(final String objectClass, final String objectUUID) {
    resolve(objectClass, objectUUID, false);
  }

  public static <T extends IsIndexed> void resolve(final String objectClass, final String objectUUID,
    final boolean replace) {
    BrowserService.Util.getInstance().retrieveFromModel(objectClass, objectUUID, new AsyncCallback<T>() {

      @Override
      public void onFailure(Throwable caught) {
        AsyncCallbackUtils.defaultFailureTreatment(caught);
      }

      @Override
      public void onSuccess(T object) {
        resolve(object, replace);
      }
    });
  }

  public static <T extends IsIndexed> void resolve(T object) {
    resolve(object, false);
  }

  public static <T extends IsIndexed> void resolve(T object, boolean replace) {
    List<String> path = null;

    if (object instanceof IndexedAIP) {
      IndexedAIP aip = (IndexedAIP) object;
      path = HistoryUtils.getHistoryBrowse(aip.getId());
    } else if (object instanceof IndexedRepresentation) {
      IndexedRepresentation representation = (IndexedRepresentation) object;
      path = HistoryUtils.getHistoryBrowse(representation);
    } else if (object instanceof IndexedFile) {
      IndexedFile file = (IndexedFile) object;
      path = HistoryUtils.getHistoryBrowse(file);
    } else if (object instanceof IndexedDIP) {
      IndexedDIP dip = (IndexedDIP) object;
      path = HistoryUtils.getHistoryBrowseDIP(dip.getId());
    } else if (object instanceof DIPFile) {
      DIPFile dipFile = (DIPFile) object;
      path = HistoryUtils.getHistoryBrowseDIPFile(dipFile.getDipId(), dipFile.getUUID());
    } else if (object instanceof TransferredResource) {
      TransferredResource resource = (TransferredResource) object;
      path = HistoryUtils.getHistory(IngestTransfer.RESOLVER.getHistoryPath(), resource.getUUID());
    } else if (object instanceof IndexedRisk) {
      IndexedRisk risk = (IndexedRisk) object;
      path = HistoryUtils.getHistory(ShowRisk.RESOLVER.getHistoryPath(), risk.getUUID());
    } else if (object instanceof RepresentationInformation) {
      RepresentationInformation ri = (RepresentationInformation) object;
      path = HistoryUtils.getHistory(ShowRepresentationInformation.RESOLVER.getHistoryPath(), ri.getUUID());
    } else if (object instanceof Notification) {
      Notification notification = (Notification) object;
      path = HistoryUtils.getHistory(ShowNotification.RESOLVER.getHistoryPath(), notification.getUUID());
    } else if (object instanceof RiskIncidence) {
      RiskIncidence incidence = (RiskIncidence) object;
      path = HistoryUtils.getHistory(ShowRiskIncidence.RESOLVER.getHistoryPath(), incidence.getUUID());
    } else if (object instanceof Job) {
      Job job = (Job) object;
      path = HistoryUtils.getHistory(ShowJob.RESOLVER.getHistoryPath(), job.getUUID());
    } else if (object instanceof IndexedReport) {
      IndexedReport report = (IndexedReport) object;
      path = HistoryUtils.getHistory(ShowJobReport.RESOLVER.getHistoryPath(), report.getUUID());
    } else {
      Toast.showError("Resolve of class not supported: " + object.getClass().getName());
    }

    if (path != null) {
      if (replace) {
        HistoryUtils.replaceHistory(path);
      } else {
        HistoryUtils.newHistory(path);
      }
    }
  }

  public static final HistoryResolver UUID_RESOLVER = new HistoryResolver() {

    @Override
    public void resolve(List<String> historyTokens, AsyncCallback<Widget> callback) {
      if (historyTokens.size() == 2) {
        String objectClass = historyTokens.get(0);
        String objectUUID = historyTokens.get(1);
        HistoryUtils.resolve(objectClass, objectUUID, true);
      }
      callback.onSuccess(null);
    }

    @Override
    public void isCurrentUserPermitted(AsyncCallback<Boolean> callback) {
      callback.onSuccess(true);
    }

    @Override
    public String getHistoryToken() {
      return "uuid";
    }

    @Override
    public List<String> getHistoryPath() {
      return new ArrayList<>();
    }
  };

  public static List<String> getHistoryUuidResolver(String objectClass, String objectUUID) {
    return Arrays.asList(UUID_RESOLVER.getHistoryToken(), objectClass, objectUUID);
  }

}
