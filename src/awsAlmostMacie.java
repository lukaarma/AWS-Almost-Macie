import javax.swing.JFileChooser;
import java.nio.file.Path;

import java.io.IOException;
import java.io.UncheckedIOException;


public class awsAlmostMacie {

    public static void main(String[] args) {
        String rootPath = null;
        FileLister lister = null;

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


        try {
            lister = new FileLister(rootPath);
        }
        catch (IOException | UncheckedIOException e) {
            System.out.printf("Access denied to '%s' ! \nExiting...\n\n",
                              e.getMessage().split(" ", 2)[1]);
            System.exit(1);
        }

        for (Path path: lister.getFileList()) {
            System.out.println(path);
        }
    }
}
