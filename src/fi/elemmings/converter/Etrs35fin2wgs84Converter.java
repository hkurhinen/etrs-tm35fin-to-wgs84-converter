package fi.elemmings.converter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class Etrs35fin2wgs84Converter {

  public static Logger logger = Logger.getLogger(Etrs35fin2wgs84Converter.class.getSimpleName());

  @SuppressWarnings({ "static-access", "unchecked" })
  public static void main(String[] args) {

    Option inputFile = OptionBuilder.withArgName("file").hasArg().withDescription("use given file as input").create("inputFile");
    Option outputFile = OptionBuilder.withArgName("file").hasArg().withDescription("output coordinates to given file").create("outputFile");
    Option inputType = OptionBuilder.withArgName("type").hasArg().withDescription("csv, json").create("inputType");
    Option outputType = OptionBuilder.withArgName("type").hasArg().withDescription("csv, json").create("outputType");
    
    Options options = new Options();
    options.addOption("h", "help", false, "show this info");
    options.addOption(inputFile);
    options.addOption(outputFile);
    options.addOption(inputType);
    options.addOption(outputType);

    CommandLineParser parser = new GnuParser();

    try {
      CommandLine cmd = parser.parse(options, args);
      if (cmd.hasOption('h') || cmd.hasOption("help")) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar "+Etrs35fin2wgs84Converter.class.getSimpleName()+".jar  [options]", options);
        System.exit(0);
      }

      File file = new File(getCmdValue(cmd, "inputFile", options));
      if (!file.exists()) {
        throw new MissingArgumentException("Input file does not exist");
      }
      File output = new File(getCmdValue(cmd, "outputFile", options));
      String inType = getCmdValue(cmd, "inputType", options);
      String outType = getCmdValue(cmd, "outputType", options);

      ArrayList<Coordinates> convertedCoords = new ArrayList<Coordinates>();
      switch (inType) {
        case "csv":
          BufferedReader br = new BufferedReader(new FileReader(file));
          String line;
          while ((line = br.readLine()) != null) {
            String[] coords = line.split(",");
            double etrs_x = Double.parseDouble(coords[1]);
            double etrs_y = Double.parseDouble(coords[0]);
            convertedCoords.add(toWgs84(etrs_x, etrs_y));
          }
          br.close();
        break;
        case "json":
          JSONParser jparser = new JSONParser();
          JSONArray a = (JSONArray) jparser.parse(new FileReader(file));
          for (Object o : a) {
            JSONObject jsonO = (JSONObject) o;
            double etrs_x = (double) jsonO.get("x");
            double etrs_y = (double) jsonO.get("y");
            convertedCoords.add(toWgs84(etrs_x, etrs_y));
          }
        break;
        default:
          throw new MissingArgumentException("Invalid input format");
      }
      FileWriter writer;
      switch (outType) {
        case "csv":
          writer = new FileWriter(output);
          for (Coordinates c : convertedCoords) {
            writer.write(c.getLng() + "," + c.getLat() + newline);
          }
          writer.flush();
          writer.close();
        break;
        case "json":
          JSONArray a = new JSONArray();
          for (Coordinates c : convertedCoords) {
            JSONObject o = new JSONObject();
            o.put("lat", c.getLat());
            o.put("lng", c.getLng());
            a.add(o);
          }
          writer = new FileWriter(output);
          writer.write(a.toJSONString());
          writer.flush();
          writer.close();
        break;
        default:
          throw new MissingArgumentException("Invalid output format");
      }

    } catch (IOException e) {
      logger.log(Level.SEVERE, "Error reading the source file:" + e.getMessage());
    } catch (ParseException e) {
      logger.log(Level.SEVERE, "Invalid parameters:" + e.getMessage());
    } catch (org.json.simple.parser.ParseException e) {
      logger.log(Level.SEVERE, "Malformed json exception:" + e.getMessage());
    }

  }

  public static Coordinates toWgs84(double x, double y) {

    try {

      double min_x = 6582464.0358;
      double max_x = 7799839.8902;
      double min_y = 50199.4814;
      double max_y = 761274.6247;

      if (x < min_x || x > max_x) { //
        throw new BadCoordinateValueException(x + " is out of range(" + min_x + " - " + max_x + ")");
      }

      if (y < min_y || y > max_y) {
        throw new BadCoordinateValueException(y + " is out of range(" + min_y + " - " + max_y + ")");
      }

      double Ca = 6378137.0;
      double Cf = 1.0 / 298.257223563;
      double Ck0 = 0.9996;
      double Clo0 = Math.toRadians(27.0);
      double CE0 = 500000.0;
      double Cn = Cf / (2.0 - Cf);
      double CA1 = Ca / (1.0 + Cn) * (1.0 + (Math.pow(Cn, 2.0)) / 4.0 + (Math.pow(Cn, 4.0)) / 64.0);
      double Ce = Math.sqrt((2.0 * Cf - Math.pow(Cf, 2.0)));
      double Ch1 = 1.0 / 2.0 * Cn - 2.0 / 3.0 * (Math.pow(Cn, 2.0)) + 37.0 / 96.0 * (Math.pow(Cn, 3.0)) - 1.0 / 360.0 * (Math.pow(Cn, 4.0));
      double Ch2 = 1.0 / 48.0 * (Math.pow(Cn, 2.0)) + 1.0 / 15.0 * (Math.pow(Cn, 3.0)) - 437.0 / 1440.0 * (Math.pow(Cn, 4.0));
      double Ch3 = 17.0 / 480.0 * (Math.pow(Cn, 3.0)) - 37.0 / 840.0 * (Math.pow(Cn, 4.0));
      double Ch4 = 4397.0 / 161280.0 * (Math.pow(Cn, 4.0));

      double E = x / (CA1 * Ck0);
      double nn = (y - CE0) / (CA1 * Ck0);
      double E1p = Ch1 * Math.sin(2.0 * E) * Math.cosh(2.0 * nn);
      double E2p = Ch2 * Math.sin(4.0 * E) * Math.cosh(4.0 * nn);
      double E3p = Ch2 * Math.sin(6.0 * E) * Math.cosh(6.0 * nn);
      double E4p = Ch3 * Math.sin(8.0 * E) * Math.cosh(8.0 * nn);

      double nn1p = Ch1 * Math.cos(2.0 * E) * Math.sinh(2.0 * nn);
      double nn2p = Ch2 * Math.cos(4.0 * E) * Math.sinh(4.0 * nn);
      double nn3p = Ch3 * Math.cos(6.0 * E) * Math.sinh(6.0 * nn);
      double nn4p = Ch4 * Math.cos(8.0 * E) * Math.sinh(8.0 * nn);

      double Ep = E - E1p - E2p - E3p - E4p;

      double nnp = nn - nn1p - nn2p - nn3p - nn4p;
      double be = Math.asin(Math.sin(Ep) / Math.cosh(nnp));

      double Q = asinh(Math.tan(be));
      double Qp = Q + Ce * atanh(Ce * Math.tanh(Q));
      Qp = Q + Ce * atanh(Ce * Math.tanh(Qp));
      Qp = Q + Ce * atanh(Ce * Math.tanh(Qp));
      Qp = Q + Ce * atanh(Ce * Math.tanh(Qp));

      double latitude = Math.toDegrees(Math.atan(Math.sinh(Qp)));

      double longitude = Math.toDegrees(Clo0 + Math.asin(Math.tanh(nnp) / Math.cos(be)));

      return new Coordinates(longitude, latitude);

    } catch (Exception ex) {
      logger.log(Level.SEVERE, "Error while converting coordinates:" + ex.getMessage());
      return null;
    }
  }

  private static double atanh(double value) {
    return Math.log((1 / value + 1) / (1 / value - 1)) / 2;
  }

  private static double asinh(double value) {
    return Math.log(value + Math.sqrt(value * value + 1));
  }

  private static String getCmdValue(CommandLine cmd, String name, Options options) throws MissingArgumentException {
    if (cmd.hasOption(name)) {
      return cmd.getOptionValue(name);
    } else {
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("java -jar "+Etrs35fin2wgs84Converter.class.getSimpleName()+".jar [options]", options);
      throw new MissingArgumentException("Required argument " + name + " is missing!");
    }

  }

  private static String newline = System.getProperty("line.separator");

}
