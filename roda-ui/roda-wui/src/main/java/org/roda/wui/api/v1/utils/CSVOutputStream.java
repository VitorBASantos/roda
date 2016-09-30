/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/keeps/roda
 */
package org.roda.wui.api.v1.utils;

import org.roda.core.common.ConsumesOutputStream;
import org.roda.core.data.v2.index.IsIndexed;

/**
 * Abstract CSV output stream.
 * 
 * @author Rui Castro <rui.castro@gmail.com>
 */
public abstract class CSVOutputStream implements ConsumesOutputStream {

  /** The filename. */
  private final String filename;

  /**
   * Constructor.
   *
   * @param filename
   *          the filename.
   */
  public CSVOutputStream(final String filename) {
    this.filename = filename;
  }

  @Override
  public String getFileName() {
    return filename;
  }

  @Override
  public String getMediaType() {
    return ExtraMediaType.TEXT_CSV;
  }
}
