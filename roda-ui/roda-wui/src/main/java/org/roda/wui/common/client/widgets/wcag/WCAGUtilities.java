/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/keeps/roda
 */
package org.roda.wui.common.client.widgets.wcag;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.user.client.Random;

public class WCAGUtilities {

  private static final Set<String> INPUT_TAGNAMES = new HashSet<>(Arrays.asList("INPUT", "SELECT"));
  private static final String IMG_TAGNAME = "img";

  private static WCAGUtilities instance = null;

  public void makeAccessible(Element element) {
    String alignAttribute = element.getAttribute("align");

    if (alignAttribute != null) {
      String className;
      if ("right".equals(alignAttribute)) {
        className = "alignRight";
      } else if ("left".equals(alignAttribute)) {
        className = "alignLeft";
      } else if ("center".equals(alignAttribute)) {
        className = "alignCenter";
      } else {
        className = "alignJustify";
      }
      element.removeAttribute("align");
      element.addClassName(className);
    }

    if (INPUT_TAGNAMES.contains(element.getTagName())) {
      addAttributeIfNonExistent(element, "title", "t_" + Random.nextInt(1000));
    }

    if (IMG_TAGNAME.equalsIgnoreCase(element.getTagName())) {
      addAttributeIfNonExistent(element, "alt", "img_" + Random.nextInt(10000));
    }

    if (element.getChildCount() > 0) {
      for (int i = 0; i < element.getChildCount(); i++) {
        if (element.getChild(i).getNodeType() == Node.ELEMENT_NODE) {
          makeAccessible((Element) element.getChild(i));
        }
      }
    }

  }

  public static WCAGUtilities getInstance() {
    if (instance == null) {
      instance = new WCAGUtilities();
    }
    return instance;
  }

  public static void addAttributeIfNonExistent(Element element, String attributeName, String attributeValue) {
    if (element.getAttribute(attributeName) == null || "".equals(element.getAttribute(attributeName))) {
      element.setAttribute(attributeName, attributeValue);
    }
  }
}
