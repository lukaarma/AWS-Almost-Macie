import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileLister {
    private final Path rootPath;
    private final Path[] fileList;

    public FileLister(String rootPath) throws IOException {
        this.rootPath = Path.of(rootPath);
        this.fileList = listFiles();
    }


    public Path[] getFileList() {
        return fileList;
    }


    private Path[] listFiles() throws IOException {
        return Files.walk(this.rootPath)
                .filter(Files::isRegularFile)
                .toArray(Path[]::new);
    }


}
