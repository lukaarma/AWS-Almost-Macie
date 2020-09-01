import org.apache.tika.exception.TikaException;
import org.xml.sax.SAXException;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Path;


public class awsAlmostMacie {

    public static void main(String[] args) {
        String rootPath = null;
        FileLister lister = null;
        FileParser parser = null;

        // FIXME: remove temp path from generator
        // nasty bug, if there are drives or network resources not responding it hangs on startup
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

        // create recursive lister of files
        try {
            System.out.println("Scanning root folder for all accessible files...\n");
            lister = new FileLister(rootPath);
        }
        catch (EmptyFolderException e) {
            System.out.println(e.getMessage());
            System.exit(1);
        }

        // create parser of content to string
        try {
            System.out.println("Scanning files for content and filetype...\n");
            parser = new FileParser(lister.getFileList().toArray(Path[]::new));
        }
        catch (TikaException | IOException | SAXException e) {
            e.printStackTrace();
            System.exit(1);
        }

        parser.printMimeTypes();

        // System.out.println(parser.getTextContentMap());
        // System.out.println(parser.getImagesPathSet());

    }
}
