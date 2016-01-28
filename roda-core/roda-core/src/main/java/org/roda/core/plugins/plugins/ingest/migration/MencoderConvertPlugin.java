package org.roda.core.plugins.plugins.ingest.migration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.roda.core.data.v2.ip.AIP;
import org.roda.core.data.v2.jobs.Report;
import org.roda.core.index.IndexService;
import org.roda.core.model.ModelService;
import org.roda.core.plugins.Plugin;
import org.roda.core.plugins.PluginException;
import org.roda.core.storage.Binary;
import org.roda.core.storage.StorageService;
import org.roda.core.util.CommandException;

public abstract class MencoderConvertPlugin extends AbstractConvertPlugin {

  @Override
  public void init() throws PluginException {
    // do nothing
  }

  @Override
  public void shutdown() {
    // do nothing
  }

  @Override
  public String getName() {
    return "Mencoder video conversion";
  }

  @Override
  public String getDescription() {
    return "Generates a video format file from other video format one using Mencoder.";
  }

  @Override
  public String getVersion() {
    return "1.0";
  }

  @Override
  public abstract Plugin<AIP> cloneMe();

  @Override
  public Path executePlugin(Binary binary) throws UnsupportedOperationException, IOException, CommandException {
    Path uriPath = Paths.get(binary.getContent().getURI());
    Path pluginResult;

    if (Files.exists(uriPath)) {
      pluginResult = MencoderConvertPluginUtils.runMencoderConvert(uriPath, inputFormat, outputFormat,
        conversionProfile);
    } else {
      pluginResult = MencoderConvertPluginUtils.runMencoderConvert(binary.getContent().createInputStream(),
        inputFormat, outputFormat, conversionProfile);
    }

    return pluginResult;
  }

  @Override
  public Report beforeExecute(IndexService index, ModelService model, StorageService storage) throws PluginException {
    return null;
  }

  @Override
  public Report afterExecute(IndexService index, ModelService model, StorageService storage) throws PluginException {
    return null;
  }

}