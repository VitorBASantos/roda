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

import org.roda.wui.common.client.HistoryResolver;

import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Window;

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

  public static List<String> getCurrentHistoryPath() {
    String hash = Window.Location.getHash();
    if (hash.length() > 0) {
      hash = hash.substring(1);
    }

    List<String> splitted = Arrays.asList(hash.split(HISTORY_SEP_REGEX));
    List<String> tokens = new ArrayList<String>();
    for (String item : splitted) {
      tokens.add(URL.decodeQueryString(item));
    }
    return tokens;
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

      String encodedToken = URL.encode(token).replaceAll(HISTORY_SEP_REGEX, HISTORY_SEP_ESCAPE);
      builder.append(encodedToken);
    }

    return builder.toString();

  }

  public static void newHistory(List<String> path) {
    // History.newItem(createHistoryToken(path)
    String hash = createHistoryToken(path);
    Window.Location.assign("#" + hash);
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

}