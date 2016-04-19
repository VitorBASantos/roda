/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/keeps/roda
 */
package org.roda.wui.client.browse;

import java.io.Serializable;
import java.util.List;

import org.roda.core.data.v2.risks.Risk;

public class RiskVersionsBundle implements Serializable {

  private static final long serialVersionUID = -3057191819098099247L;

  private Risk risk;
  private List<BinaryVersionBundle> versions;

  public RiskVersionsBundle() {
    super();
  }

  public RiskVersionsBundle(Risk risk, List<BinaryVersionBundle> versions) {
    super();
    this.risk = risk;
    this.versions = versions;
  }

  public Risk getRisk() {
    return risk;
  }

  public void setRisk(Risk risk) {
    this.risk = risk;
  }

  public List<BinaryVersionBundle> getVersions() {
    return versions;
  }

  public void setVersions(List<BinaryVersionBundle> versions) {
    this.versions = versions;
  }

}