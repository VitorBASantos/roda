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
package org.roda.wui.client.planning;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.v2.index.facet.Facets;
import org.roda.core.data.v2.index.facet.SimpleFacetParameter;
import org.roda.core.data.v2.index.filter.Filter;
import org.roda.core.data.v2.index.filter.FilterParameter;
import org.roda.core.data.v2.index.filter.SimpleFilterParameter;
import org.roda.core.data.v2.index.select.SelectedItems;
import org.roda.core.data.v2.jobs.Job;
import org.roda.core.data.v2.ri.RepresentationInformation;
import org.roda.wui.client.browse.BrowserService;
import org.roda.wui.client.common.LastSelectedItemsSingleton;
import org.roda.wui.client.common.UserLogin;
import org.roda.wui.client.common.dialogs.Dialogs;
import org.roda.wui.client.common.lists.RepresentationInformationList;
import org.roda.wui.client.common.lists.utils.AsyncTableCell.CheckboxSelectionListener;
import org.roda.wui.client.common.lists.utils.ClientSelectedItemsUtils;
import org.roda.wui.client.common.search.SearchFilters;
import org.roda.wui.client.common.search.SearchPanel;
import org.roda.wui.client.common.utils.AsyncCallbackUtils;
import org.roda.wui.client.common.utils.JavascriptUtils;
import org.roda.wui.client.ingest.process.ShowJob;
import org.roda.wui.client.process.CreateSelectedJob;
import org.roda.wui.client.process.InternalProcess;
import org.roda.wui.client.search.Search;
import org.roda.wui.common.client.HistoryResolver;
import org.roda.wui.common.client.tools.FacetUtils;
import org.roda.wui.common.client.tools.HistoryUtils;
import org.roda.wui.common.client.tools.ListUtils;
import org.roda.wui.common.client.widgets.HTMLWidgetWrapper;
import org.roda.wui.common.client.widgets.Toast;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.SelectionChangeEvent;

import config.i18n.client.ClientMessages;

/**
 * @author Luis Faria
 *
 */
public class RepresentationInformationNetwork extends Composite {

  public static final HistoryResolver RESOLVER = new HistoryResolver() {

    @Override
    public void resolve(List<String> historyTokens, AsyncCallback<Widget> callback) {
      getInstance().resolve(historyTokens, callback);
    }

    @Override
    public void isCurrentUserPermitted(AsyncCallback<Boolean> callback) {
      UserLogin.getInstance().checkRole(this, callback);
    }

    @Override
    public List<String> getHistoryPath() {
      return ListUtils.concat(Planning.RESOLVER.getHistoryPath(), getHistoryToken());
    }

    @Override
    public String getHistoryToken() {
      return "representation_information_register";
    }
  };

  private static RepresentationInformationNetwork instance = null;

  interface MyUiBinder extends UiBinder<Widget, RepresentationInformationNetwork> {
  }

  private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);
  private static final ClientMessages messages = GWT.create(ClientMessages.class);

  @UiField
  FlowPanel title;

  @UiField
  FlowPanel registerDescription;

  @UiField(provided = true)
  SearchPanel searchPanel;

  @UiField(provided = true)
  RepresentationInformationList representationInformationList;

  @UiField(provided = true)
  FlowPanel facetTags;

  @UiField(provided = true)
  FlowPanel facetSupport;

  @UiField
  Button buttonAdd;

  @UiField
  Button buttonRemove;

  @UiField
  Button startProcess;

  @UiField
  FlowPanel sidebar;

  @UiField
  FlowPanel content;

  private static final Filter DEFAULT_FILTER = SearchFilters.defaultFilter(RepresentationInformation.class.getName());
  private static final String ALL_FILTER = SearchFilters.allFilter(RepresentationInformation.class.getName());

  private final Filter filter = DEFAULT_FILTER;

  /**
   * Create a representation information page
   */
  public RepresentationInformationNetwork() {
    Facets facets = new Facets(new SimpleFacetParameter(RodaConstants.REPRESENTATION_INFORMATION_TAGS),
      new SimpleFacetParameter(RodaConstants.REPRESENTATION_INFORMATION_SUPPORT));

    representationInformationList = new RepresentationInformationList(filter, facets,
      messages.representationInformationTitle(), true);

    searchPanel = new SearchPanel(DEFAULT_FILTER, ALL_FILTER, true,
      messages.representationInformationRegisterSearchPlaceHolder(), false, false, true);
    searchPanel.setList(representationInformationList);

    facetTags = new FlowPanel();
    facetSupport = new FlowPanel();

    Map<String, FlowPanel> facetPanels = new HashMap<>();
    facetPanels.put(RodaConstants.REPRESENTATION_INFORMATION_TAGS, facetTags);
    facetPanels.put(RodaConstants.REPRESENTATION_INFORMATION_SUPPORT, facetSupport);
    FacetUtils.bindFacets(representationInformationList, facetPanels);

    initWidget(uiBinder.createAndBindUi(this));

    representationInformationList.getSelectionModel().addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
      @Override
      public void onSelectionChange(SelectionChangeEvent event) {
        RepresentationInformation selected = representationInformationList.getSelectionModel().getSelectedObject();
        if (selected != null) {
          LastSelectedItemsSingleton selectedItems = LastSelectedItemsSingleton.getInstance();
          selectedItems.setLastHistory(HistoryUtils.getCurrentHistoryPath());
          HistoryUtils.newHistory(ShowRepresentationInformation.RESOLVER, selected.getId());
        }
      }
    });

    representationInformationList
      .addCheckboxSelectionListener(new CheckboxSelectionListener<RepresentationInformation>() {
        @Override
        public void onSelectionChange(SelectedItems<RepresentationInformation> selected) {
          boolean empty = ClientSelectedItemsUtils.isEmpty(selected);
          buttonRemove.setEnabled(!empty);
          startProcess.setEnabled(!empty);
        }
      });

    Label titleLabel = new Label(messages.representationInformationRegisterTitle());
    titleLabel.addStyleName("h1 browseItemText");
    title.add(titleLabel);

    InlineHTML badge = new InlineHTML("<span class='label-warning browseRepresentationOriginalIcon'>Beta</span>");
    title.add(badge);

    registerDescription.add(new HTMLWidgetWrapper("RepresentationInformationNetworkDescription.html"));
  }

  /**
   * Get the singleton instance
   *
   * @return the instance
   */
  public static RepresentationInformationNetwork getInstance() {
    if (instance == null) {
      instance = new RepresentationInformationNetwork();
    }
    return instance;
  }

  @Override
  protected void onLoad() {
    super.onLoad();
    JavascriptUtils.stickSidebar();
  }

  public void resolve(List<String> historyTokens, AsyncCallback<Widget> callback) {
    if (historyTokens.isEmpty()) {
      filter.setParameters(new ArrayList<>());
      searchPanel.setDefaultFilter(filter, true);
      representationInformationList.setFilter(filter);
      searchPanel.clearSearchInputBox();

      callback.onSuccess(this);
    } else {
      String basePage = historyTokens.remove(0);
      if (ShowRepresentationInformation.RESOLVER.getHistoryToken().equals(basePage)) {
        ShowRepresentationInformation.RESOLVER.resolve(historyTokens, callback);
      } else if (CreateRepresentationInformation.RESOLVER.getHistoryToken().equals(basePage)) {
        CreateRepresentationInformation.RESOLVER.resolve(historyTokens, callback);
      } else if (EditRepresentationInformation.RESOLVER.getHistoryToken().equals(basePage)) {
        EditRepresentationInformation.RESOLVER.resolve(historyTokens, callback);
      } else if (RepresentationInformationAssociations.RESOLVER.getHistoryToken().equals(basePage)) {
        RepresentationInformationAssociations.RESOLVER.resolve(historyTokens, callback);
      } else if (Search.RESOLVER.getHistoryToken().equals(basePage)) {
        setFilterFromHistoryTokens(historyTokens);
        searchPanel.setDefaultFilter(filter, true);
        representationInformationList.setFilter(filter);
        searchPanel.clearSearchInputBox();

        callback.onSuccess(this);
      } else {
        HistoryUtils.newHistory(RESOLVER);
        callback.onSuccess(null);
      }
    }
  }

  private void setFilterFromHistoryTokens(List<String> historyTokens) {
    List<FilterParameter> params = new ArrayList<>();
    if (historyTokens.size() == (2)) {
      params.add(new SimpleFilterParameter(historyTokens.get(0), historyTokens.get(1)));
    }

    filter.setParameters(params);
  }

  @UiHandler("buttonAdd")
  void buttonAddHandler(ClickEvent e) {
    LastSelectedItemsSingleton selectedItems = LastSelectedItemsSingleton.getInstance();
    selectedItems.setLastHistory(HistoryUtils.getCurrentHistoryPath());
    HistoryUtils.newHistory(RESOLVER, CreateRepresentationInformation.RESOLVER.getHistoryToken());
  }

  @UiHandler("buttonRemove")
  void buttonRemoveHandler(ClickEvent e) {
    final SelectedItems<RepresentationInformation> selected = representationInformationList.getSelected();

    ClientSelectedItemsUtils.size(RepresentationInformation.class, selected, new AsyncCallback<Long>() {

      @Override
      public void onFailure(Throwable caught) {
        AsyncCallbackUtils.defaultFailureTreatment(caught);
      }

      @Override
      public void onSuccess(final Long size) {
        Dialogs.showConfirmDialog(messages.representationInformationRemoveFolderConfirmDialogTitle(),
          messages.representationInformationRemoveSelectedConfirmDialogMessage(size),
          messages.representationInformationRemoveFolderConfirmDialogCancel(),
          messages.representationInformationRemoveFolderConfirmDialogOk(), new AsyncCallback<Boolean>() {

            @Override
            public void onFailure(Throwable caught) {
              AsyncCallbackUtils.defaultFailureTreatment(caught);
            }

            @Override
            public void onSuccess(Boolean confirmed) {
              if (confirmed) {
                BrowserService.Util.getInstance().deleteRepresentationInformation(selected, new AsyncCallback<Job>() {

                  @Override
                  public void onFailure(Throwable caught) {
                    HistoryUtils.newHistory(InternalProcess.RESOLVER);
                  }

                  @Override
                  public void onSuccess(Job result) {
                    Dialogs.showJobRedirectDialog(messages.removeJobCreatedMessage(), new AsyncCallback<Void>() {

                      @Override
                      public void onFailure(Throwable caught) {
                        Timer timer = new Timer() {
                          @Override
                          public void run() {
                            Toast.showInfo(messages.representationInformationRemoveSuccessTitle(),
                              messages.representationInformationRemoveSuccessMessage(size));
                            representationInformationList.refresh();
                          }
                        };

                        timer.schedule(RodaConstants.ACTION_TIMEOUT);
                      }

                      @Override
                      public void onSuccess(final Void nothing) {
                        HistoryUtils.newHistory(ShowJob.RESOLVER, result.getId());
                      }
                    });
                  }
                });
              }
            }
          });
      }
    });
  }

  @UiHandler("startProcess")
  void handleButtonProcess(ClickEvent e) {
    LastSelectedItemsSingleton selectedItems = LastSelectedItemsSingleton.getInstance();
    selectedItems.setSelectedItems(representationInformationList.getSelected());
    selectedItems.setLastHistory(HistoryUtils.getCurrentHistoryPath());
    HistoryUtils.newHistory(CreateSelectedJob.RESOLVER, RodaConstants.JOB_PROCESS_ACTION);
  }
}
