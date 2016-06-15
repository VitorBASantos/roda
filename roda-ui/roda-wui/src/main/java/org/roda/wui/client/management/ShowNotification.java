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
package org.roda.wui.client.management;

import java.util.ArrayList;
import java.util.List;

import org.roda.core.data.v2.notifications.Notification;
import org.roda.wui.client.browse.BrowserService;
import org.roda.wui.client.common.UserLogin;
import org.roda.wui.common.client.HistoryResolver;
import org.roda.wui.common.client.tools.Tools;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import config.i18n.client.ClientMessages;

/**
 * @author Luis Faria
 *
 */
public class ShowNotification extends Composite {

  public static final HistoryResolver RESOLVER = new HistoryResolver() {

    @Override
    public void resolve(List<String> historyTokens, final AsyncCallback<Widget> callback) {
      if (historyTokens.size() == 1) {
        String notificationId = historyTokens.get(0);
        BrowserService.Util.getInstance().retrieve(Notification.class.getName(), notificationId,
          new AsyncCallback<Notification>() {

            @Override
            public void onFailure(Throwable caught) {
              callback.onFailure(caught);
            }

            @Override
            public void onSuccess(Notification notification) {
              ShowNotification notificationPanel = new ShowNotification(notification);
              callback.onSuccess(notificationPanel);
            }
          });
      } else {
        Tools.newHistory(NotificationRegister.RESOLVER);
        callback.onSuccess(null);
      }
    }

    @Override
    public void isCurrentUserPermitted(AsyncCallback<Boolean> callback) {
      UserLogin.getInstance().checkRoles(new HistoryResolver[] {MemberManagement.RESOLVER}, false, callback);
    }

    public List<String> getHistoryPath() {
      return Tools.concat(NotificationRegister.RESOLVER.getHistoryPath(), getHistoryToken());
    }

    public String getHistoryToken() {
      return "notification";
    }
  };

  interface MyUiBinder extends UiBinder<Widget, ShowNotification> {
  }

  private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

  @SuppressWarnings("unused")
  private static final ClientMessages messages = GWT.create(ClientMessages.class);

  @UiField
  Label notificationId;

  @UiField
  Label notificationSubject;

  @UiField
  HTML notificationBody;

  @UiField
  Label notificationSentOn;

  @UiField
  Label notificationFromUser;

  @UiField
  Label notificationIsAcknowledged;

  @UiField
  Label acknowledgedUsersKey;

  @UiField
  HTML acknowledgedUsersValue;

  @UiField
  Label notAcknowledgedUsersKey;

  @UiField
  HTML notAcknowledgedUsersValue;

  @UiField
  Button buttonCancel;

  /**
   * Create a new panel to view a notification
   *
   */
  public ShowNotification(Notification notification) {
    initWidget(uiBinder.createAndBindUi(this));

    notificationId.setText(notification.getId());
    notificationSubject.setText(notification.getSubject());
    notificationBody.setHTML(notification.getBody());
    notificationSentOn
      .setText(DateTimeFormat.getFormat(PredefinedFormat.DATE_TIME_MEDIUM).format(notification.getSentOn()));
    notificationFromUser.setText(notification.getFromUser());
    notificationIsAcknowledged.setText(Boolean.toString(notification.isAcknowledged()));
    acknowledgedUsersKey.setVisible(false);
    notAcknowledgedUsersKey.setVisible(false);

    List<String> recipientUsers = new ArrayList<String>(notification.getRecipientUsers());

    for (String user : notification.getAcknowledgedUsers().keySet()) {
      String ackDate = notification.getAcknowledgedUsers().get(user);
      acknowledgedUsersKey.setVisible(true);
      acknowledgedUsersValue.setHTML(SafeHtmlUtils.fromSafeConstant(
        acknowledgedUsersValue.getHTML() + "<p>" + user + " <span class='small-and-gray'> " + ackDate + "</span></p>"));
      recipientUsers.remove(user);
    }

    for (String user : recipientUsers) {
      notAcknowledgedUsersKey.setVisible(true);
      notAcknowledgedUsersValue
        .setHTML(SafeHtmlUtils.fromSafeConstant(notAcknowledgedUsersValue.getHTML() + "<p>" + user + "</p>"));
    }

  }

  @UiHandler("buttonCancel")
  void handleButtonCancel(ClickEvent e) {
    cancel();
  }

  private void cancel() {
    Tools.newHistory(NotificationRegister.RESOLVER);
  }

}
