import javax.swing.JFileChooser;

public class awsAlmostMacie {

    public static void main(String[] args) {

        String rootPath;
        // FIXME: remove temp path from generator
        JFileChooser rootChooser = new JFileChooser("C:/Users/lukaa/Documents/AWS(almost)Macie/testFolder");


        // allow only directories to be selected
        rootChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int rootChooserExitCode = rootChooser.showOpenDialog(null);

        switch (rootChooserExitCode) {
            case JFileChooser.APPROVE_OPTION ->
                    rootPath = rootChooser.getSelectedFile().getAbsolutePath();

            case JFileChooser.CANCEL_OPTION -> {
                System.out.println("User cancelled folder selection, exiting...");
                System.exit(0);
            }

            case JFileChooser.ERROR_OPTION -> {
                System.out.println("Undefined error during folder selection, exiting...");
                System.exit(1);
            }

            default -> {
                System.out.println("Unexpected value: " + rootChooserExitCode);
                System.exit(1);
            }
        }

        FileLister lister = new FileLister(rootPath);
    }
}
