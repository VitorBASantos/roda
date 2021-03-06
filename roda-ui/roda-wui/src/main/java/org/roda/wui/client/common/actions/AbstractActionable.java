/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/keeps/roda
 */
package org.roda.wui.client.common.actions;

import java.util.Arrays;

import org.roda.core.data.v2.index.IsIndexed;
import org.roda.core.data.v2.index.select.SelectedItems;
import org.roda.core.data.v2.index.select.SelectedItemsList;
import org.roda.wui.client.common.utils.AsyncCallbackUtils;
import org.roda.wui.client.common.utils.StringUtils;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public abstract class AbstractActionable<T extends IsIndexed> implements Actionable<T> {

  protected static AsyncCallback<ActionImpact> createDefaultAsyncCallback() {
    return new AsyncCallback<ActionImpact>() {

      @Override
      public void onFailure(Throwable caught) {
        AsyncCallbackUtils.defaultFailureTreatment(caught);
      }

      @Override
      public void onSuccess(ActionImpact impact) {
        // do nothing
      }
    };
  }

  protected SelectedItemsList<T> objectToSelectedItems(T object) {
    return new SelectedItemsList<>(Arrays.asList(object.getUUID()), object.getClass().getName());
  }

  @Override
  public void act(Actionable.Action<T> action, T object) {
    act(action, object, createDefaultAsyncCallback());
  }

  @Override
  public boolean canAct(Actionable.Action<T> action, T object) {
    return canAct(action, objectToSelectedItems(object));
  }

  public boolean canAct(T object, @SuppressWarnings("unchecked") Actionable.Action<T>... actions) {
    boolean canAct = false;
    for (Action<T> action : actions) {
      if (canAct(action, object)) {
        canAct = true;
        break;
      }
    }

    return canAct;
  }

  public boolean canAct(SelectedItems<T> objects, @SuppressWarnings("unchecked") Actionable.Action<T>... actions) {
    boolean canAct = false;
    for (Action<T> action : actions) {
      if (canAct(action, objects)) {
        canAct = true;
        break;
      }
    }

    return canAct;
  }

  @Override
  public void act(Actionable.Action<T> action, T object, AsyncCallback<ActionImpact> callback) {
    act(action, objectToSelectedItems(object), callback);
  }

  @Override
  public void act(Actionable.Action<T> action, SelectedItems<T> objects) {
    act(action, objects, createDefaultAsyncCallback());
  }

  public FlowPanel createLayout() {
    FlowPanel layout = new FlowPanel();
    layout.addStyleName("actions-layout");
    return layout;
  }

  @SafeVarargs
  public final void addTitle(FlowPanel layout, String titleId, String text, T object, Actionable.Action<T>... actions) {
    addTitle(layout, titleId, text, object, null, actions);
  }

  @SafeVarargs
  public final void addTitle(FlowPanel layout, String titleId, String text, T object, String extraCssClass,
    Actionable.Action<T>... actions) {

    if (canAct(object, actions)) {
      Label title = new Label(text);
      title.getElement().setId(titleId);
      title.addStyleName("h4");
      if (StringUtils.isNotBlank(extraCssClass)) {
        title.addStyleName(extraCssClass);
      }
      layout.add(title);
    }
  }

  @SafeVarargs
  public final void addTitle(FlowPanel layout, String titleId, String text, SelectedItems<T> objects,
    Actionable.Action<T>... actions) {
    addTitle(layout, titleId, text, objects, null, actions);
  }

  @SafeVarargs
  public final void addTitle(FlowPanel layout, String titleId, String text, SelectedItems<T> objects,
    String extraCssClass, Actionable.Action<T>... actions) {

    if (canAct(objects, actions)) {
      Label title = new Label(text);
      title.getElement().setId(titleId);
      title.addStyleName("h4");
      if (StringUtils.isNotBlank(extraCssClass)) {
        title.addStyleName(extraCssClass);
      }
      layout.add(title);
    }
  }

  public void addButton(FlowPanel layout, final String buttonId, final String text, final Actionable.Action<T> action,
    final T object, final ActionImpact impact, final AsyncCallback<ActionImpact> callback,
    final String... extraCssClasses) {

    if (canAct(action, object)) {

      // Construct
      Button button = new Button(text);
      button.setTitle(text);
      button.getElement().setId(buttonId);

      // CSS
      button.setStyleName("actions-layout-button");
      button.addStyleName("btn");
      button.addStyleName("btn-block");

      if (ActionImpact.DESTROYED.equals(impact)) {
        button.addStyleName("btn-danger");
      } else if (ActionImpact.UPDATED.equals(impact)) {
        button.addStyleName("btn-primary");
      } else {
        button.addStyleName("btn-default");
      }

      button.addStyleDependentName(impact.name().toLowerCase());

      for (String extraCssClass : extraCssClasses) {
        button.addStyleName(extraCssClass);
      }

      // Action
      button.addClickHandler(new ClickHandler() {

        @Override
        public void onClick(ClickEvent event) {
          act(action, object, callback);
        }
      });

      layout.add(button);
    }
  }

  public void addButton(FlowPanel layout, final String buttonId, final String text, final Actionable.Action<T> action,
    final SelectedItems<T> objects, final ActionImpact impact, final AsyncCallback<ActionImpact> callback,
    final String... extraCssClasses) {

    if (canAct(action, objects)) {

      // Construct
      Button button = new Button(text);
      button.setTitle(text);
      button.getElement().setId(buttonId);

      // CSS
      button.setStyleName("actions-layout-button");
      button.addStyleName("btn");
      button.addStyleName("btn-block");
      button.addStyleName(ActionImpact.DESTROYED.equals(impact) ? "btn-danger" : "btn-default");
      button.addStyleDependentName(impact.name().toLowerCase());

      for (String extraCssClass : extraCssClasses) {
        button.addStyleName(extraCssClass);
      }

      // Action
      button.addClickHandler(new ClickHandler() {

        @Override
        public void onClick(ClickEvent event) {
          act(action, objects, callback);
        }
      });

      layout.add(button);
    }
  }

  @Override
  public Widget createActionsLayout(T object, AsyncCallback<ActionImpact> callback) {
    return createActionsLayout(objectToSelectedItems(object), callback);
  }

  public Widget createActionsLayout(T object) {
    return createActionsLayout(object, createDefaultAsyncCallback());
  }

}
