public class EmptyFolderException extends Exception {
    public EmptyFolderException() {
        super("The selected folder has no files that can be accessed! \nExiting..\n\n");
    }
}
