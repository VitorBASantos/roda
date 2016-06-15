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
package org.roda.wui.management.editor.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.roda.core.data.v2.ip.IndexedAIP;
import org.roda.core.data.v2.user.Group;
import org.roda.core.data.v2.user.RODAMember;
import org.roda.core.data.v2.user.User;
import org.roda.wui.client.ingest.Ingest;
import org.roda.wui.common.client.ClientLogger;
import org.roda.wui.common.client.widgets.LoadingPopup;
import org.roda.wui.common.client.widgets.WUIButton;
import org.roda.wui.management.user.client.GroupMiniPanel;
import org.roda.wui.management.user.client.UserMiniPanel;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import config.i18n.client.ClientMessages;

/**
 * @author Luis Faria
 * 
 */
public class EditProducersPanel extends Composite {

  private static ClientMessages messages = (ClientMessages) GWT.create(ClientMessages.class);

  private ClientLogger logger = new ClientLogger(getClass().getName());

  private final IndexedAIP fondsAip;
  private List<RODAMember> producers;

  private DockPanel layout;
  private Label title;
  private ScrollPanel memberListScroll;
  private VerticalPanel memberListPanel;
  private List<WUIButton> actionButtons;
  private WUIButton addUser;
  private WUIButton addGroup;
  private WUIButton delete;
  private WUIButton getRodaIn;

  private LoadingPopup loading;

  private List<UserMiniPanel> userMiniPanels;
  private List<GroupMiniPanel> groupMiniPanels;

  private Object selected = null;

  /**
   * Create a new edit producers panel
   * 
   * @param fondsAip
   */
  public EditProducersPanel(IndexedAIP fondsAip) {
    this.fondsAip = fondsAip;
    this.producers = new Vector<RODAMember>();
    userMiniPanels = new Vector<UserMiniPanel>();
    groupMiniPanels = new Vector<GroupMiniPanel>();

    layout = new DockPanel();
    title = new Label(messages.editProducersTitle());
    memberListPanel = new VerticalPanel();
    memberListScroll = new ScrollPanel(memberListPanel);

    actionButtons = new ArrayList<WUIButton>();
    addUser = new WUIButton(messages.editProducersAddUser(), WUIButton.Left.ROUND, WUIButton.Right.PLUS);
    addGroup = new WUIButton(messages.editProducersAddGroup(), WUIButton.Left.ROUND, WUIButton.Right.PLUS);
    delete = new WUIButton(messages.editProducersDelete(), WUIButton.Left.ROUND, WUIButton.Right.MINUS);
    getRodaIn = new WUIButton(messages.editProducersRodaIn(), WUIButton.Left.ROUND, WUIButton.Right.ARROW_DOWN);

    actionButtons.add(addUser);
    actionButtons.add(addGroup);
    actionButtons.add(delete);
    actionButtons.add(getRodaIn);

    layout.add(title, DockPanel.NORTH);
    layout.add(memberListScroll, DockPanel.CENTER);

    initWidget(layout);

    addUser.addClickListener(new ClickListener() {

      public void onClick(Widget sender) {
        addUser();
      }

    });

    addGroup.addClickListener(new ClickListener() {

      public void onClick(Widget sender) {
        addGroup();
      }

    });

    delete.addClickListener(new ClickListener() {

      public void onClick(Widget sender) {
        delete();
      }

    });

    getRodaIn.addClickListener(new ClickListener() {

      public void onClick(Widget sender) {
        User user = null;
        for (UserMiniPanel userMiniPanel : userMiniPanels) {
          if (userMiniPanel.isSelected()) {
            user = userMiniPanel.getUser();
            break;
          }
        }
        if (user != null) {
          Ingest.downloadRodaIn(user, null);
        }
      }

    });

    loading = new LoadingPopup(this);
    updateVisibles();

    layout.addStyleName("wui-edit-producers");
    title.addStyleName("title");
    memberListScroll.addStyleName("member-list-scroll");
    memberListPanel.addStyleName("member-list-panel");
    addUser.addStyleName("member-toolbox-addUser");
    addGroup.addStyleName("member-toolbox-addGroup");
    delete.addStyleName("member-toolbox-delete");

  }

  private boolean initialized = false;

  /**
   * Initialize producers panel
   */
  public void init() {
    if (!initialized) {
      initialized = true;
      initializeProducerList();
    }

  }

  /**
   * Add user
   */
  public void addUser() {
    // final SelectUserWindow selectUser = new SelectUserWindow();
    // selectUser.addSuccessListener(new SuccessListener() {
    //
    // public void onCancel() {
    // // do nothing
    //
    // }
    //
    // public void onSuccess() {
    // selectUser.getSelected(new AsyncCallback<RODAMember>() {
    //
    // public void onFailure(Throwable caught) {
    // logger.error("Error adding user", caught);
    // }
    //
    // public void onSuccess(RODAMember member) {
    // addProducer(member);
    // }
    //
    // });
    // }
    //
    // });
    // selectUser.show();
  }

  /**
   * Add group
   */
  public void addGroup() {
    // final SelectGroupWindow selectGroup = new SelectGroupWindow();
    // selectGroup.addSuccessListener(new SuccessListener() {
    //
    // public void onCancel() {
    // // do nothing
    //
    // }
    //
    // public void onSuccess() {
    // selectGroup.getSelected(new AsyncCallback<RODAMember>() {
    //
    // public void onFailure(Throwable caught) {
    // logger.error("Error adding group", caught);
    // }
    //
    // public void onSuccess(RODAMember member) {
    // addProducer(member);
    // }
    //
    // });
    // }
    //
    // });
    // selectGroup.show();
  }

  /**
   * Delete
   */
  public void delete() {
    List<RODAMember> producersToRemove = new Vector<RODAMember>();
    for (UserMiniPanel userMiniPanel : userMiniPanels) {
      if (userMiniPanel.isSelected()) {
        producersToRemove.add(userMiniPanel.getUser());
      }
    }
    for (GroupMiniPanel groupMiniPanel : groupMiniPanels) {
      if (groupMiniPanel.isSelected()) {
        String groupName = groupMiniPanel.getGroupName();
        producersToRemove.add(new Group(groupName));
      }
    }
    removeProducers(producersToRemove);
  }

  /**
   * Get buttons witch allow edition control
   * 
   * @return the WUI Buttons
   */
  public List<WUIButton> getActionButtons() {
    return actionButtons;
  }

  private void initializeProducerList() {
    loading.show();
    // EditorService.Util.getInstance().getProducers(fondsAip.getId(), new
    // AsyncCallback<List<RODAMember>>() {
    //
    // public void onFailure(Throwable caught) {
    // loading.hide();
    // logger.error("Error initialing producer list", caught);
    // }
    //
    // public void onSuccess(List<RODAMember> producers) {
    // EditProducersPanel.this.producers = producers;
    // updateMemberList();
    // loading.hide();
    // }
    //
    // });
  }

  private void addProducer(RODAMember producer) {
    loading.show();
    // EditorService.Util.getInstance().addProducer(producer, fondsAip.getId(),
    // new AsyncCallback<List<RODAMember>>() {
    //
    // public void onFailure(Throwable caught) {
    // loading.hide();
    // logger.error("Error adding producer", caught);
    // }
    //
    // public void onSuccess(List<RODAMember> producers) {
    // EditProducersPanel.this.producers = producers;
    // updateMemberList();
    // loading.hide();
    // }
    //
    // });
  }

  private void removeProducers(List<RODAMember> producers) {
    loading.show();
    // EditorService.Util.getInstance().removeProducers(producers,
    // fondsAip.getId(),
    // new AsyncCallback<List<RODAMember>>() {
    //
    // public void onFailure(Throwable caught) {
    // loading.hide();
    // logger.error("Error adding producer", caught);
    // }
    //
    // public void onSuccess(List<RODAMember> producers) {
    // EditProducersPanel.this.producers = producers;
    // updateMemberList();
    // loading.hide();
    // }
    //
    // });
  }

  private boolean isSelected(Object miniPanel) {
    boolean ret;
    if (miniPanel instanceof UserMiniPanel) {
      ret = ((UserMiniPanel) miniPanel).isSelected();
    } else if (miniPanel instanceof GroupMiniPanel) {
      ret = ((GroupMiniPanel) miniPanel).isSelected();
    } else {
      throw new IllegalArgumentException("target not a UserMiniPanel nor GroupMiniPanel");
    }
    return ret;
  }

  private void setSelected(Object miniPanel, boolean selected) {
    if (miniPanel instanceof UserMiniPanel) {
      ((UserMiniPanel) miniPanel).setSelected(selected);
    } else if (miniPanel instanceof GroupMiniPanel) {
      ((GroupMiniPanel) miniPanel).setSelected(selected);
    } else {
      throw new IllegalArgumentException("target not a UserMiniPanel nor GroupMiniPanel");
    }
  }

  private void onSelection(Object newSelected) {
    if (selected != newSelected) {
      if (selected != null) {
        setSelected(selected, false);
      }
      selected = newSelected;
    } else if (!isSelected(selected)) {
      selected = null;
    }

    updateVisibles();

  }

  private void updateVisibles() {
    delete.setEnabled(selected != null);
    getRodaIn.setEnabled(selected != null && selected instanceof UserMiniPanel);
  }

  private void updateMemberList() {
    memberListPanel.clear();
    userMiniPanels.clear();
    groupMiniPanels.clear();

    for (RODAMember member : producers) {
      if (member instanceof User) {
        User user = (User) member;
        final UserMiniPanel userMiniPanel = new UserMiniPanel(user);
        memberListPanel.add(userMiniPanel.getWidget());
        userMiniPanels.add(userMiniPanel);
        userMiniPanel.addChangeListener(new ChangeListener() {

          public void onChange(Widget sender) {
            onSelection(userMiniPanel);
          }

        });

      } else if (member instanceof Group) {
        Group group = (Group) member;
        final GroupMiniPanel groupMiniPanel = new GroupMiniPanel(group.getName());
        memberListPanel.add(groupMiniPanel.getWidget());
        groupMiniPanels.add(groupMiniPanel);
        groupMiniPanel.addChangeListener(new ChangeListener() {

          public void onChange(Widget sender) {
            onSelection(groupMiniPanel);
          }

        });
      }
    }
  }

}
