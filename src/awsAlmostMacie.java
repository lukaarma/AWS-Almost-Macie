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

        String configPath;
        String rootPath = null;
        FileLister lister = null;
        FileParser parser = null;
        ContentAnalyzer analyzer = null;

        // separate all output from launch command
        System.out.println("\n__AWS(almost)Macie__\n");

        // Args parsing
        Option triggersDict = new Option(
                "f",
                "filter",
                true,
                "The json config file wth the trigger keywords");
        triggersDict.setRequired(true);
        options.addOption(triggersDict);
        options.addOption("d", "debug", false, "Print debugging info");

        try {
            argv = argvParser.parse(options, args);
        }
        catch (ParseException e) {
            System.out.println( "Unexpected exception:" + e.getMessage());
            System.exit(1);
        }
        // Done parsing

        // Error out if config does not exist
        configPath = argv.getOptionValue('f');
        if (!Files.exists(Path.of(configPath))) {
            System.out.printf("The config file '%s' does not exist! Exiting... \n\n", configPath);
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
            parser = new FileParser(lister.getFileList().toArray(Path[]::new));
        }
        // TODO: better handling
        catch (TikaException | IOException | SAXException e) {
            e.printStackTrace();
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
            analyzer = new ContentAnalyzer(configPath);
        }
        // TODO: better handling
        catch (IOException | org.json.simple.parser.ParseException e) {
            e.printStackTrace();
            System.exit(1);
        }

        if (argv.hasOption('d')) {
            System.out.println("\n\n<=====================================================>");
            System.out.println(Arrays.toString(analyzer.getAlerts()));
            System.out.println("<=====================================================>");
            System.out.println(Arrays.toString(analyzer.getWarnings()));
            System.out.println("<=====================================================>\n\n");
        }
        System.out.println("Analyzing text files...\n");
        analyzer.parseText(parser.getTextContentMap());
        System.out.println("\n\nAnalyzing image files...\n");
        analyzer.parseImages(parser.getImagesPathSet());
    }
}
