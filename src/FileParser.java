import org.apache.tika.Tika;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.exception.TikaException;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;


public class FileParser extends Tika {
    // map path -> file content parsed to string
    private final HashMap<Path, String> parsedFileContentMap;
    // map path -> MIME type of file with no content to parse
    private final HashMap<Path, String> noContentMimeTypesMap;
    private final ArrayList<Path> accessErrorArray;


    public FileParser(Path[] pathList) throws TikaException, IOException, SAXException {
        // tika_config will suppress error at startup for missing parsers (not used)
        super(new TikaConfig("tika_config.xml"));
        this.noContentMimeTypesMap = new HashMap<>();
        this.accessErrorArray = new ArrayList<>();
        this.parsedFileContentMap = parseFileContent(pathList);
    }


    public HashMap<Path, String> getParsedFileContentMap() {
        return parsedFileContentMap;
    }

    public HashMap<Path, String> getNoContentMimeTypesMap() {
        return noContentMimeTypesMap;
    }

    public ArrayList<Path> getAccessErrorArray() {
        return this.accessErrorArray;
    }


    private HashMap<Path, String> parseFileContent(Path[] pathList) {
        HashMap<Path, String> result = new HashMap<>();

        for (Path file : pathList) {
            String content;
            String mime;

            // Try to get the parsed file content
            try {
                content = this.parseToString(file);
                result.put(file, content);
            }
            // If it fails because we cant access just add to error because
            // we would likely not be able to detect() it either for the same reason
            catch (IOException e) {
                System.out.printf("Error accessing '%s' !\n", file.toString());
                this.accessErrorArray.add(file);
            }
            // If it fails the parsing we could still get the mime type
            catch (TikaException e) {
                System.out.printf("Cannot parse '%s' to string!\n", file.toString());

                try {
                    mime = this.detect(file);
                    this.noContentMimeTypesMap.put(file, mime);
                }
                // but to prevent some weird race condition fails let's catch this
                catch (IOException ioException) {
                    System.out.printf("Error accessing '%s' !\n", file.toString());
                    this.accessErrorArray.add(file);
                }
            }
        }

        return result;
    }

}
