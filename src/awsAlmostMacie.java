import org.apache.commons.cli.*;
import org.apache.tika.exception.TikaException;
import org.xml.sax.SAXException;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;


public class awsAlmostMacie {
    public static CommandLine argv = null;

    public static void main(String[] args) {
        Options options = new Options();
        CommandLineParser argvParser = new DefaultParser();

        String analyzerConfigPath;
        String tikaConfigPath;
        String rootPath = null;
        FileLister lister = null;
        FileParser parser = null;
        ContentAnalyzer analyzer = null;

        // separate all output from launch command
        System.out.println("\n__AWS(almost)Macie__\n");

        // Args parsing
        Option analyzerConfigOption = new Option(
                "c",
                "config",
                true,
                "The json config file wth the keywords to match"
        );
        analyzerConfigOption.setRequired(true);
        Option tikaConfigOption = new Option(
                "t",
                "tikaConfig",
                true,
                "The xml config file for Tika"
        );
        tikaConfigOption.setRequired(true);

        options.addOption(analyzerConfigOption);
        options.addOption(tikaConfigOption);
        options.addOption("d", "debug", false, "Print debugging info");
        options.addOption("n", "noTesseract", false, "Use only Tika parsing");

        try {
            argv = argvParser.parse(options, args);
        }
        catch (ParseException e) {
            System.out.println( "Unexpected exception: " + e.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "awsAlmostMacie", options );
            System.exit(1);
        }
        // Done parsing

        // Error out if configs does not exist
        analyzerConfigPath = argv.getOptionValue('c');
        tikaConfigPath = argv.getOptionValue('t');
        if (!Files.exists(Path.of(analyzerConfigPath))) {
            System.out.printf("The config file '%s' does not exist! Exiting... \n\n", analyzerConfigPath);
            System.exit(1);
        }
        else if (!Files.exists(Path.of(tikaConfigPath))) {
            System.out.printf("The config file '%s' does not exist! Exiting... \n\n", tikaConfigPath);
            System.exit(1);
        }

        // FIXME: remove temp path from generator
        // nasty bug, if there are drives or network resources not responding
        // it hangs on startup (source => https://community.oracle.com/thread/1357858)
        JFileChooser rootChooser = new JFileChooser("C:/Users/lukaa/Documents/AWS(almost)Macie/testFolder");
        rootChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        switch (rootChooser.showOpenDialog(null)) {
            case JFileChooser.APPROVE_OPTION ->
                    rootPath = rootChooser.getSelectedFile().getAbsolutePath();

            case JFileChooser.CANCEL_OPTION -> {
                System.out.println("User cancelled folder selection, exiting...\n\n");
                System.exit(0);
            }

            case JFileChooser.ERROR_OPTION -> {
                System.out.println("Undefined error during folder selection, exiting...\n\n");
                System.exit(1);
            }

            default -> {
                System.out.println("Unexpected exit code from folder selection window, exiting...\n\n");
                System.exit(1);
            }
        }

        // create iterative lister of files
        try {
            System.out.printf("Scanning '%s' for all accessible files...\n", rootPath);
            lister = new FileLister(rootPath);
        }
        catch (EmptyFolderException e) {
            System.out.println(e.getMessage());
            System.exit(1);
        }

        // create parser of content to string and filter out images/pdf for Tesseract
        try {
            System.out.println("Scanning files for content and filetype...\n");
            parser = new FileParser(lister.getFileList().toArray(Path[]::new), tikaConfigPath);
        }
        catch (TikaException e) {
            if (argv.hasOption('d')) {
                e.printStackTrace();
            }
            System.out.printf("Failed while initializing Tika with message '%s'! Exiting...", e.getMessage());
            System.exit(1);
        }
        catch (IOException e) {
            if (argv.hasOption('d')) {
                e.printStackTrace();
            }
            System.out.printf("Couldn't read tika config '%s'! Exiting...", tikaConfigPath);
            System.exit(1);
        }
        catch (SAXException e) {
            if (argv.hasOption('d')) {
                e.printStackTrace();
            }
            System.out.printf("Error while parsing tika config '%s'! Exiting...", tikaConfigPath);
            System.exit(1);
        }

        if (argv.hasOption('d')) {
            System.out.println("\n\n<=====================================================>");
            parser.printMimeTypes();
            System.out.println("<=====================================================>");
            System.out.println(parser.getTextContentMap());
            System.out.println("<=====================================================>");
            System.out.println(parser.getImagesPathSet());
            System.out.println("<=====================================================>\n\n");
        }

        try {
            analyzer = new ContentAnalyzer(analyzerConfigPath);
        }
        // TODO: better handling
        catch (IOException e) {
            if (argv.hasOption('d')) {
                e.printStackTrace();
            }
            System.out.printf("Couldn't read analyzer config '%s'! Exiting...", analyzerConfigPath);
            System.exit(1);
        }
        catch (org.json.simple.parser.ParseException e) {
            if (argv.hasOption('d')) {
                e.printStackTrace();
            }
            System.out.printf("Error while parsing config '%s' to JSON object! Exiting...", analyzerConfigPath);
            System.exit(1);
        }

        if (argv.hasOption('d')) {
            System.out.println("\n\n<=====================================================>");
            System.out.println(Arrays.toString(analyzer.getAlerts()));
            System.out.println("<=====================================================>");
            System.out.println(Arrays.toString(analyzer.getWarnings()));
            System.out.println("<=====================================================>\n\n");
        }

        if (argv.hasOption('n')){
            System.out.println("Analyzing text and images files...\n");
            analyzer.parseText(parser.getTextContentMap());
        }
        else {
            System.out.println("Analyzing text and images files...\n");
            analyzer.parseText(parser.getTextContentMap());
            System.out.println("\n\nAnalyzing image files...\n");
            analyzer.parseImages(parser.getImagesPathSet());
        }
    }
}
