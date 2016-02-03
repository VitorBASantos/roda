package org.roda.core.plugins.plugins.ingest.migration;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ghost4j.Ghostscript;
import org.ghost4j.GhostscriptException;
import org.roda.core.RodaCoreFactory;
import org.roda.core.util.CommandException;
import org.roda.core.util.CommandUtility;

public class GhostScriptConvertPluginUtils {

  public static Path runGhostScriptConvert(InputStream is, String inputFormat, String outputFormat,
    String commandArguments) throws IOException, GhostscriptException {

    // write the inputstream data on a new file (absolute path needed)
    Path input = Files.createTempFile("copy", "." + inputFormat);
    byte[] buffer = new byte[is.available()];
    is.read(buffer);
    OutputStream os = new FileOutputStream(input.toFile());
    os.write(buffer);
    os.close();
    is.close();

    Path output = Files.createTempFile("result", "." + outputFormat);
    return executeGS(input, output, commandArguments);
  }

  public static Path runGhostScriptConvert(Path input, String inputFormat, String outputFormat, String commandArguments)
    throws IOException, CommandException, GhostscriptException {

    Path output = Files.createTempFile("result", "." + outputFormat);
    return executeGS(input, output, commandArguments);
  }

  private static Path executeGS(Path input, Path output, String commandArguments) throws GhostscriptException,
    IOException, UnsupportedOperationException {

    String command = RodaCoreFactory.getRodaConfigurationAsString("tools", "ghostscriptconvert", "commandLine");
    command = command.replace("{input_file}", input.toString());
    command = command.replace("{output_file}", output.toString());

    if (commandArguments.length() > 0) {
      command = command.replace("{arguments}", commandArguments);
    } else {
      command = command.replace("{arguments}", "-sDEVICE=pdfwrite");
    }

    // GhostScript transformation command
    String[] gsArgs = command.split("\\s+");
    Ghostscript gs = Ghostscript.getInstance();

    try {
      gs.initialize(gsArgs);
      gs.exit();
    } catch (GhostscriptException e) {
      throw new GhostscriptException("Exception when using GhostScript: ", e);
    }

    return output;
  }

  public static String getVersion() throws CommandException, IOException, UnsupportedOperationException {
    String version = CommandUtility.execute("gs", "--version");
    if (version.indexOf('\n') > 0) {
      version = version.substring(0, version.indexOf('\n'));
    }
    return "GhostScript " + version.trim();
  }

  public static Map<String, List<String>> getPronomToExtension() {
    Map<String, List<String>> map = new HashMap<>();
    // TODO add missing pronoms
    return map;
  }

  public static Map<String, List<String>> getMimetypeToExtension() {
    Map<String, List<String>> map = new HashMap<>();
    // TODO add missing mimetypes
    map.put("application/pdf", new ArrayList<String>(Arrays.asList("pdf")));
    map.put("application/postscript", new ArrayList<String>(Arrays.asList("ps")));
    return map;
  }

  public static List<String> getInputExtensions() {
    // TODO add missing extensions
    return Arrays.asList("pdf", "ps");
  }

}
