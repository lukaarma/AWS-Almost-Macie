import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import java.io.IOException;


public class FileLister {
    private final Path rootPath;
    private final ArrayList<Path> fileList;


    public FileLister(String rootPath) throws EmptyFolderException {
        this.rootPath = Path.of(rootPath);
        this.fileList = new ArrayList<>();
        listFiles(this.rootPath);

        if (this.fileList.isEmpty()) {
            throw new EmptyFolderException();
        }
    }


    public ArrayList<Path> getFileList() {
        return fileList;
    }

    public Path getRootPath() {
        return rootPath;
    }

    // FIXME: make this with iteration not recursive!!
    // I think this may overflow the jvm memory at some point
    // but the directory has to be so big that for now it will work
    private void listFiles(Path rootPath) {
        DirectoryStream<Path> files;
        try {
            files = Files.newDirectoryStream(rootPath);
        }
        catch (IOException e) {
            System.out.printf("Error while opening '%s' as a directory for reading, skipping... \n",
                              rootPath);
            return;
        }

        files.forEach(path -> {
            if (Files.isDirectory(path)) {
                listFiles(path);
            }
            else if (Files.isReadable(path) && Files.isRegularFile(path)) {
                this.fileList.add(path);
            }
            else {
                System.out.printf("Error while parsing '%s': it's not a directory nor a regular file, skipping \n...",
                                  path);
            }
        });

//        return Files.walk(this.rootPath)
//                .filter(Files::isRegularFile)
//                .filter(Files::isReadable)
//                .toArray(Path[]::new);
    }


}
