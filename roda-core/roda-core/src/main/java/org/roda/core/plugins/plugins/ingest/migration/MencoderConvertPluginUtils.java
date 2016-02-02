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

  public static Path runMencoderConvert(InputStream is, String inputFormat, String outputFormat, String commandArguments)
    throws IOException, CommandException {

    // write the inputstream data on a new file (absolute path needed)
    Path input = Files.createTempFile("copy", "." + inputFormat);
    byte[] buffer = new byte[is.available()];
    is.read(buffer);
    OutputStream os = new FileOutputStream(input.toFile());
    os.write(buffer);
    os.close();
    is.close();

    Path output = Files.createTempFile("result", "." + outputFormat);
    return executeMencoder(input, output, commandArguments);
  }

  public static Path runMencoderConvert(Path input, String inputFormat, String outputFormat, String commandArguments)
    throws IOException, CommandException {

    Path output = Files.createTempFile("result", "." + outputFormat);
    return executeMencoder(input, output, commandArguments);
  }

  private static Path executeMencoder(Path input, Path output, String commandArguments) throws CommandException {

    String command = RodaCoreFactory.getRodaConfigurationAsString("tools", "mencoderconvert", "commandLine");
    command = command.replace("{input_file}", input.toString());
    command = command.replace("{output_file}", output.toString());

    if (commandArguments.length() > 0) {
      command = command.replace("{arguments}", commandArguments);
    } else {
      command = command.replace("{arguments}", "-ovc copy -oac copy");
    }

    // filling a list of the command line arguments
    List<String> commandList = Arrays.asList(command.split("\\s+"));

    // running the command
    CommandUtility.execute(commandList);
    return output;
  }

  public static String getVersion() throws CommandException {
    String version = CommandUtility.execute("mencoder", "-of", "help");
    if (version.indexOf('\n') > 0) {
      version = version.substring(0, version.indexOf('\n'));
    }

    version = version.replaceAll("(MEncoder\\s+[0-9.-]+).*", "$1");
    return version.trim();
  }

}
