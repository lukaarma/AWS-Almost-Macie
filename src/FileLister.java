import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;


public class FileLister {
    private final ArrayList<Path> fileList;


    public FileLister(String rootPath) throws EmptyFolderException {
        this.fileList = new ArrayList<>();
        listFiles(Path.of(rootPath));

        if (this.fileList.isEmpty()) {
            throw new EmptyFolderException(rootPath);
        }
    }


    public ArrayList<Path> getFileList() {
        return fileList;
    }


    private void listFiles(Path rootPath) {
        ArrayList<Path> folders = new ArrayList<>(List.of(rootPath));
        AtomicReference<Integer> nextIndex = new AtomicReference<>(folders.size());

        for (int i = 0; i < folders.size(); i++) {
            DirectoryStream<Path> folderContent;
            try {
                folderContent = Files.newDirectoryStream(folders.get(i));
            }
            catch (IOException e) {
                System.out.printf(
                        "Error while opening '%s' as a directory for reading, skipping... \n",
                        folders.get(i)
                );

                continue;
            }

            folderContent.forEach(path -> {
                if (Files.isDirectory(path)) {
                    folders.add(nextIndex.getAndSet(nextIndex.get() + 1), path);
                }
                else if (Files.isReadable(path) && Files.isRegularFile(path)) {
                    this.fileList.add(path);
                }
                else {
                    System.out.printf(
                            "Error while parsing '%s': it's not a directory nor a regular/readable file, skipping \n...",
                            path
                    );
                }
            });
        }
    }
}
