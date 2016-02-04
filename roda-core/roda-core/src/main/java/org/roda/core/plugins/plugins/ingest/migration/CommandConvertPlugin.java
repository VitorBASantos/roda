package org.roda.core.plugins.plugins.ingest.migration;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.roda.core.data.exceptions.InvalidParameterException;
import org.roda.core.data.v2.jobs.PluginParameter;
import org.roda.core.data.v2.jobs.PluginParameter.PluginParameterType;
import org.roda.core.data.v2.jobs.Report;
import org.roda.core.index.IndexService;
import org.roda.core.model.ModelService;
import org.roda.core.plugins.Plugin;
import org.roda.core.plugins.PluginException;
import org.roda.core.storage.Binary;
import org.roda.core.storage.StorageService;
import org.roda.core.util.CommandException;

public abstract class CommandConvertPlugin extends AbstractConvertPlugin {

  public String commandArguments;

  public CommandConvertPlugin() {
    super();
    commandArguments = "";
  }

  @Override
  public abstract void init() throws PluginException;

  @Override
  public abstract void shutdown();

  @Override
  public abstract String getName();

  @Override
  public abstract String getDescription();

  @Override
  public abstract String getVersion();

  @Override
  public abstract Plugin<Serializable> cloneMe();

  @Override
  public List<PluginParameter> getParameters() {
    List<PluginParameter> params = new ArrayList<PluginParameter>();

    PluginParameter commandArgs = new PluginParameter("commandArgs", "Command arguments", PluginParameterType.STRING,
      "", true, true, "Command arguments to modify the command to execute");

    params.add(commandArgs);
    params.addAll(super.getParameters());
    return params;
  }

  @Override
  public void setParameterValues(Map<String, String> parameters) throws InvalidParameterException {
    super.setParameterValues(parameters);

    // add command arguments
    if (parameters.containsKey("commandArguments")) {
      commandArguments = parameters.get("commandArguments").trim();
    }
  }

  @Override
  public abstract Path executePlugin(Binary binary, String fileFormat) throws UnsupportedOperationException,
    IOException, CommandException;

  @Override
  public abstract Report beforeExecute(IndexService index, ModelService model, StorageService storage)
    throws PluginException;

}