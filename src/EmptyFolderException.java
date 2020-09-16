public class EmptyFolderException extends Exception {
    public EmptyFolderException(String path) {
        super(String.format("The root '%s' has no files that can be accessed! \nExiting..\n\n", path));
    }
}
