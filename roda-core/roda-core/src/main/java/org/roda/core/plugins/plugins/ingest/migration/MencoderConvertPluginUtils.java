package org.roda.core.plugins.plugins.ingest.migration;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.roda.core.RodaCoreFactory;
import org.roda.core.util.CommandException;
import org.roda.core.util.CommandUtility;

public class MencoderConvertPluginUtils {

  public static Path runMencoderConvert(InputStream is, String inputFormat, String outputFormat,
    String conversionProfile) throws IOException, CommandException {

    // write the inputstream data on a new file (absolute path needed)
    Path input = Files.createTempFile("copy", "." + inputFormat);
    byte[] buffer = new byte[is.available()];
    is.read(buffer);
    OutputStream os = new FileOutputStream(input.toFile());
    os.write(buffer);
    os.close();
    is.close();

    Path output = Files.createTempFile("result", "." + outputFormat);
    return executeMencoder(input, output, conversionProfile);
  }

  public static Path runMencoderConvert(Path input, String inputFormat, String outputFormat, String conversionProfile)
    throws IOException, CommandException {

    Path output = Files.createTempFile("result", "." + outputFormat);
    return executeMencoder(input, output, conversionProfile);
  }

  private static Path executeMencoder(Path input, Path output, String conversionProfile) throws CommandException {

    String command = RodaCoreFactory.getRodaConfigurationAsString("tools", "mencoderconvert", conversionProfile,
      "commandLine");
    command = command.replace("{input_file}", input.toString());
    command = command.replace("{output_file}", output.toString());

    // filling a list of the command line arguments
    List<String> commandList = Arrays.asList(command.split(" "));

    // running the command
    CommandUtility.execute(commandList);
    return output;
  }

  public static String getVersion() throws CommandException {
    String version = CommandUtility.execute("mencoder", "-v");
    if (version.indexOf('\n') > 0) {
      version = version.substring(0, version.indexOf('\n'));
    }
    return version;
  }

}