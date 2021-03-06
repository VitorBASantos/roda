/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/keeps/roda
 */
package org.roda.core;

import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;

public class RodaPropertiesReloadStrategy extends FileChangedReloadingStrategy {

  @Override
  public void reloadingPerformed() {
    RodaCoreFactory.clearRodaCachableObjectsAfterConfigurationChange();
    super.updateLastModified();
  }

}
