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
package org.roda.wui.client.browse;

import java.util.Arrays;
import java.util.List;

import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.exceptions.NotFoundException;
import org.roda.core.data.v2.index.facet.Facets;
import org.roda.core.data.v2.index.filter.Filter;
import org.roda.core.data.v2.index.filter.SimpleFilterParameter;
import org.roda.core.data.v2.index.select.SelectedItems;
import org.roda.core.data.v2.index.select.SelectedItemsList;
import org.roda.core.data.v2.ip.IndexedFile;
import org.roda.core.data.v2.ip.IndexedRepresentation;
import org.roda.wui.client.common.Dialogs;
import org.roda.wui.client.common.LastSelectedItemsSingleton;
import org.roda.wui.client.common.LoadingAsyncCallback;
import org.roda.wui.client.common.UserLogin;
import org.roda.wui.client.common.dialogs.SelectFileDialog;
import org.roda.wui.client.common.lists.AsyncTableCell.CheckboxSelectionListener;
import org.roda.wui.client.common.lists.ClientSelectedItemsUtils;
import org.roda.wui.client.common.lists.SearchFileList;
import org.roda.wui.client.common.search.SearchFilters;
import org.roda.wui.client.common.search.SearchPanel;
import org.roda.wui.client.common.utils.AsyncCallbackUtils;
import org.roda.wui.client.common.utils.JavascriptUtils;
import org.roda.wui.client.ingest.transfer.TransferUpload;
import org.roda.wui.client.main.BreadcrumbPanel;
import org.roda.wui.client.main.BreadcrumbUtils;
import org.roda.wui.client.process.CreateJob;
import org.roda.wui.common.client.ClientLogger;
import org.roda.wui.common.client.HistoryResolver;
import org.roda.wui.common.client.tools.DescriptionLevelUtils;
import org.roda.wui.common.client.tools.Tools;
import org.roda.wui.common.client.widgets.Toast;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.SelectionChangeEvent;

import config.i18n.client.ClientMessages;

/**
 * @author Luis Faria <lfaria@keep.pt>
 * 
 */
public class BrowseFolder extends Composite {

  public static final HistoryResolver RESOLVER = new HistoryResolver() {

    @Override
    public void resolve(List<String> historyTokens, final AsyncCallback<Widget> callback) {
      if (historyTokens.size() == 3) {
        final String aipId = historyTokens.get(0);
        final String representationUUID = historyTokens.get(1);
        final String fileUUID = historyTokens.get(2);

        BrowserService.Util.getInstance().retrieveItemBundle(aipId, LocaleInfo.getCurrentLocale().getLocaleName(),
          new AsyncCallback<BrowseItemBundle>() {

            @Override
            public void onFailure(Throwable caught) {
              errorRedirect(callback);
            }

            @Override
            public void onSuccess(final BrowseItemBundle itemBundle) {
              if (itemBundle != null && verifyRepresentation(itemBundle.getRepresentations(), representationUUID)) {
                BrowserService.Util.getInstance().retrieve(IndexedFile.class.getName(), fileUUID,
                  new AsyncCallback<IndexedFile>() {

                    @Override
                    public void onSuccess(IndexedFile simpleFile) {
                      if (simpleFile.isDirectory()) {
                        BrowseFolder folder = new BrowseFolder(itemBundle, simpleFile);
                        callback.onSuccess(folder);
                      } else {
                        errorRedirect(callback);
                      }
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                      Toast.showError(caught.getClass().getSimpleName(), caught.getMessage());
                      errorRedirect(callback);
                    }
                  });
              } else {
                errorRedirect(callback);
              }
            }
          });
      } else if (historyTokens.size() > 0
        && historyTokens.get(0).equals(TransferUpload.BROWSE_RESOLVER.getHistoryToken())) {
        TransferUpload.BROWSE_RESOLVER.resolve(Tools.tail(historyTokens), callback);
      } else {
        errorRedirect(callback);
      }
    }

    private boolean verifyRepresentation(List<IndexedRepresentation> representations, String representationUUID) {
      boolean exist = false;
      for (IndexedRepresentation representation : representations) {
        if (representation.getUUID().equals(representationUUID)) {
          exist = true;
        }
      }
      return exist;
    }

    @Override
    public void isCurrentUserPermitted(AsyncCallback<Boolean> callback) {
      UserLogin.getInstance().checkRole(this, callback);
    }

    @Override
    public String getHistoryToken() {
      return "folder";
    }

    @Override
    public List<String> getHistoryPath() {
      return Tools.concat(Browse.RESOLVER.getHistoryPath(), getHistoryToken());
    }

    private void errorRedirect(AsyncCallback<Widget> callback) {
      Tools.newHistory(Browse.RESOLVER);
      callback.onSuccess(null);
    }
  };

  public static final SafeHtml FOLDER_ICON = SafeHtmlUtils.fromSafeConstant("<i class='fa fa-folder-o'></i>");

  interface MyUiBinder extends UiBinder<Widget, BrowseFolder> {
  }

  private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

  @SuppressWarnings("unused")
  private ClientLogger logger = new ClientLogger(getClass().getName());

  private static ClientMessages messages = (ClientMessages) GWT.create(ClientMessages.class);

  @UiField
  SimplePanel folderIcon;

  @UiField
  Label folderName;

  @UiField
  Label folderId;

  @UiField
  Button rename, createFolder;

  @UiField
  BreadcrumbPanel breadcrumb;

  @UiField(provided = true)
  SearchPanel searchPanel;

  @UiField(provided = true)
  SearchFileList filesList;

  // private BrowseItemBundle itemBundle;
  // private IndexedFile folder;
  private String aipId;
  private String repId;
  private String folderUUID;

  private static final String ALL_FILTER = SearchFilters.allFilter(IndexedFile.class.getName());

  private BrowseFolder(BrowseItemBundle itemBundle, IndexedFile folder) {
    // this.itemBundle = itemBundle;
    // this.folder = folder;
    this.aipId = folder.getAipId();
    this.repId = folder.getRepresentationUUID();
    this.folderUUID = folder.getUUID();

    String summary = messages.representationListOfFiles();
    boolean selectable = true;

    Filter filter = new Filter(new SimpleFilterParameter(RodaConstants.FILE_PARENT_UUID, folder.getUUID()));
    filesList = new SearchFileList(filter, true, Facets.NONE, summary, selectable);

    filesList.getSelectionModel().addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
      @Override
      public void onSelectionChange(SelectionChangeEvent event) {
        IndexedFile selected = filesList.getSelectionModel().getSelectedObject();
        if (selected != null) {
          if (selected.isDirectory()) {
            Tools.newHistory(Browse.RESOLVER, BrowseFolder.RESOLVER.getHistoryToken(), aipId, repId,
              selected.getUUID());
          } else {
            Tools.newHistory(Browse.RESOLVER, BrowseFile.RESOLVER.getHistoryToken(), aipId, repId, selected.getUUID());
          }
        }
      }
    });

    filesList.addCheckboxSelectionListener(new CheckboxSelectionListener<IndexedFile>() {

      @Override
      public void onSelectionChange(SelectedItems<IndexedFile> selected) {
        SelectedItems<IndexedFile> files = (SelectedItems<IndexedFile>) filesList.getSelected();

        if (ClientSelectedItemsUtils.isEmpty(files)) {
          files = new SelectedItemsList<IndexedFile>(Arrays.asList(folderUUID), IndexedFile.class.getName());
        }

        boolean empty = ClientSelectedItemsUtils.isEmpty(selected);
        createFolder.setEnabled(empty);

        ClientSelectedItemsUtils.size(IndexedFile.class, files, new AsyncCallback<Long>() {

          @Override
          public void onFailure(Throwable caught) {
            // do nothing
          }

          @Override
          public void onSuccess(Long result) {
            rename.setEnabled(result == 1);
          }
        });
      }
    });

    searchPanel = new SearchPanel(filter, ALL_FILTER, messages.searchPlaceHolder(), false, false, false);
    searchPanel.setDefaultFilterIncremental(true);
    searchPanel.setList(filesList);

    initWidget(uiBinder.createAndBindUi(this));

    String folderLabel = folder.getOriginalName() != null ? folder.getOriginalName() : folder.getId();

    HTMLPanel itemIconHtmlPanel = new HTMLPanel(
      DescriptionLevelUtils.getElementLevelIconSafeHtml(RodaConstants.VIEW_REPRESENTATION_FOLDER, false));
    itemIconHtmlPanel.addStyleName("browseItemIcon-other");

    folderIcon.setWidget(itemIconHtmlPanel);
    folderName.setText(folderLabel);
    folderId.setText(folder.getUUID());

    breadcrumb.updatePath(BreadcrumbUtils.getFileBreadcrumbs(itemBundle, aipId, repId, folder));
    breadcrumb.setVisible(true);
  }

  @Override
  protected void onLoad() {
    super.onLoad();
    JavascriptUtils.stickSidebar();
  }

  @UiHandler("rename")
  void buttonRenameHandler(ClickEvent e) {
    final String folderUUID;

    if (ClientSelectedItemsUtils.isEmpty(filesList.getSelected())) {
      folderUUID = this.folderUUID;
    } else {
      if (filesList.getSelected() instanceof SelectedItemsList) {
        SelectedItemsList<IndexedFile> fileList = (SelectedItemsList<IndexedFile>) filesList.getSelected();
        folderUUID = (String) fileList.getIds().get(0);
      } else {
        return;
      }
    }

    Dialogs.showPromptDialog(messages.renameItemTitle(), null, messages.renamePlaceholder(), RegExp.compile(".*"),
      messages.cancelButton(), messages.confirmButton(), new AsyncCallback<String>() {

        @Override
        public void onFailure(Throwable caught) {
          Toast.showInfo(messages.dialogFailure(), messages.renameFailed());
        }

        @Override
        public void onSuccess(String newName) {
          BrowserService.Util.getInstance().renameFolder(folderUUID, newName, new LoadingAsyncCallback<String>() {

            @Override
            public void onSuccessImpl(String newUUID) {
              Toast.showInfo(messages.dialogSuccess(), messages.renameSuccessful());
              Tools.newHistory(BrowseFolder.RESOLVER, aipId, repId, newUUID);
            }
          });
        }
      });
  }

  @UiHandler("move")
  void buttonMoveHandler(ClickEvent e) {
    // FIXME missing filter to remove the files themselves
    Filter filter = new Filter(new SimpleFilterParameter(RodaConstants.FILE_REPRESENTATION_UUID, repId),
      new SimpleFilterParameter(RodaConstants.FILE_AIP_ID, aipId),
      new SimpleFilterParameter(RodaConstants.FILE_ISDIRECTORY, Boolean.toString(true)));
    SelectFileDialog selectFileDialog = new SelectFileDialog(messages.moveItemTitle(), filter, true, false);
    selectFileDialog.setSingleSelectionMode();
    selectFileDialog.showAndCenter();
    selectFileDialog.addValueChangeHandler(new ValueChangeHandler<IndexedFile>() {

      @Override
      public void onValueChange(ValueChangeEvent<IndexedFile> event) {
        final IndexedFile toFolder = event.getValue();
        SelectedItems<IndexedFile> selected = filesList.getSelected();

        if (ClientSelectedItemsUtils.isEmpty(selected)) {
          selected = new SelectedItemsList<IndexedFile>(Arrays.asList(folderUUID), IndexedFile.class.getName());
        }

        BrowserService.Util.getInstance().moveFiles(aipId, selected, toFolder, new LoadingAsyncCallback<String>() {

          @Override
          public void onSuccessImpl(String newUUID) {
            Tools.newHistory(BrowseFolder.RESOLVER, aipId, repId, newUUID);
          }

          @Override
          public void onFailureImpl(Throwable caught) {
            if (caught instanceof NotFoundException) {
              Toast.showError(messages.moveNoSuchObject(caught.getMessage()));
            } else {
              AsyncCallbackUtils.defaultFailureTreatment(caught);
            }
          }

        });
      }
    });
  }

  @UiHandler("uploadFiles")
  void buttonUploadFilesHandler(ClickEvent e) {
    Tools.newHistory(RESOLVER, TransferUpload.BROWSE_RESOLVER.getHistoryToken(), aipId, repId, folderUUID);
  }

  @UiHandler("createFolder")
  void buttonCreateFolderHandler(ClickEvent e) {
    Dialogs.showPromptDialog(messages.renameItemTitle(), null, messages.renamePlaceholder(), RegExp.compile(".*"),
      messages.cancelButton(), messages.confirmButton(), new AsyncCallback<String>() {
        @Override
        public void onFailure(Throwable caught) {
          Toast.showInfo(messages.dialogFailure(), messages.renameFailed());
        }

        @Override
        public void onSuccess(String newName) {
          BrowserService.Util.getInstance().createFolder(folderUUID, newName, new LoadingAsyncCallback<String>() {

            @Override
            public void onSuccessImpl(String newUUID) {
              filesList.refresh();
            }

            @Override
            public void onFailureImpl(Throwable caught) {
              if (caught instanceof NotFoundException) {
                Toast.showError(messages.moveNoSuchObject(caught.getMessage()));
              } else {
                AsyncCallbackUtils.defaultFailureTreatment(caught);
              }
            }

          });
        }
      });
  }

  @UiHandler("remove")
  void buttonRemoveHandler(ClickEvent e) {
    SelectedItems<IndexedFile> files = (SelectedItems<IndexedFile>) filesList.getSelected();

    if (ClientSelectedItemsUtils.isEmpty(files)) {
      files = new SelectedItemsList<IndexedFile>(Arrays.asList(folderUUID), IndexedFile.class.getName());
    }

    BrowserService.Util.getInstance().deleteFile(files, new LoadingAsyncCallback<Void>() {

      @Override
      public void onFailureImpl(Throwable caught) {
        AsyncCallbackUtils.defaultFailureTreatment(caught);
        filesList.refresh();
      }

      @Override
      public void onSuccessImpl(Void returned) {
        Toast.showInfo(messages.removeSuccessTitle(), messages.removeAllSuccessMessage());
        filesList.refresh();
      }
    });
  }

  @UiHandler("newProcess")
  void buttonNewProcessHandler(ClickEvent e) {
    SelectedItems<IndexedFile> files = (SelectedItems<IndexedFile>) filesList.getSelected();

    if (ClientSelectedItemsUtils.isEmpty(files)) {
      files = new SelectedItemsList<IndexedFile>(Arrays.asList(folderUUID), IndexedFile.class.getName());
    }

    LastSelectedItemsSingleton selectedItems = LastSelectedItemsSingleton.getInstance();
    selectedItems.setSelectedItems(files);
    Tools.newHistory(CreateJob.RESOLVER, "action");
  }
}