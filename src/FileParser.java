import org.apache.tika.Tika;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.exception.TikaException;
import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MediaTypeRegistry;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;


public class FileParser extends Tika {
    // filetypes parsed by Tesseract, see here (http://tess4j.sourceforge.net/)
    private final Set<MediaType> imagesMimeTypes = Set.of(
            // disable pdf for now (need ghost4j ??)
            // MediaType.parse("application/pdf")
            MediaType.parse("image/tiff"),
            MediaType.parse("image/jpeg"),
            MediaType.parse("image/gif"),
            MediaType.parse("image/png"),
            MediaType.parse("image/bmp")
    );

    /* these two contain all of the paths, including with no or empty content
    parsedFileContentMap path -> file content parsed to string
    parsedMimeTypesMap path -> MIME type of file */
    private final Map<Path, String> parsedFileContentMap;
    private final Map<Path, String> parsedMimeTypesMap;

    // these contains only text and images
    private final Map<Path, String> textContentMap;
    private final Set<Path> imagesPathSet;

    // Array to store any inaccessible paths
    private final ArrayList<Path> accessErrorArray;


    public FileParser(Path[] pathList) throws TikaException, IOException, SAXException {
        // tika_config will suppress error at startup for missing parsers (not used)
        super(new TikaConfig("tika_config.xml"));

        this.parsedFileContentMap = new HashMap<>();
        this.parsedMimeTypesMap = new HashMap<>();

        this.textContentMap = new HashMap<>();
        this.imagesPathSet = new HashSet<>();

        this.accessErrorArray = new ArrayList<>();

        parseFileContent(pathList);
        populateTextMap();
        populateImageSet();
    }


    public Map<Path, String> getParsedFileContentMap() {
        return this.parsedFileContentMap;
    }

    public Map<Path, String> getParsedMimeTypesMap() {
        return this.parsedMimeTypesMap;
    }

    public ArrayList<Path> getAccessErrorArray() {
        return this.accessErrorArray;
    }

    public Map<Path, String> getTextContentMap() {
        return this.textContentMap;
    }

    public Set<Path> getImagesPathSet() {
        return this.imagesPathSet;
    }

    private void parseFileContent(Path[] pathList) {
        Map<Path, String> result = new HashMap<>();

        for (Path file : pathList) {
            String content;
            String mime;

            // Try to get the parsed file content
            try {
                content = this.parseToString(file);
                this.parsedFileContentMap.put(file, content);
            }
            // If it fails the parsing we place an empty string
            catch (TikaException e) {
                this.parsedFileContentMap.put(file, null);
            }
            // If it fails because we cant access just add to error because
            // we would likely not be able to detect() it either for the same reason
            catch (IOException e) {
                System.out.printf("Error accessing '%s' !\n", file.toString());
                this.accessErrorArray.add(file);
                // if we can't access the file we can't read the mimetype, so null and continue
                this.parsedMimeTypesMap.put(file, null);
            }

            // now we get the mime type
            try {
                mime = this.detect(file);
                this.parsedMimeTypesMap.put(file, mime);
            }
            catch (IOException ignored) {
            }
        }
    }

    private void populateTextMap() {
        this.parsedFileContentMap.forEach((path, content) -> {
            if (content != null && !content.isEmpty()) {
                this.textContentMap.put(path, content);
            }
        });

    }

    private void populateImageSet() {
        MediaTypeRegistry registry = TikaConfig.getDefaultConfig().getMediaTypeRegistry();

        this.parsedMimeTypesMap.forEach((path, mime) -> {
            MediaType type = MediaType.parse(mime);

            while (type != null && !type.equals(MediaType.OCTET_STREAM)) {
                if (this.imagesMimeTypes.contains(type)) {
                    this.imagesPathSet.add(path);
                    type = null;
                }
                else {
                    type = registry.getSupertype(type);
                }
            }
        });
    }

    public void printMimeTypes() {
        int max = 0;

        for (Path path: this.parsedMimeTypesMap.keySet()) {
            if ( path.toString().length() > max) {
                max = path.toString().length();
            }
        }

        int finalMax = max + 1;
        System.out.printf("%-" + finalMax + "s ->   %s   %s\n", "ABSOLUTE PATH", "HAS CONTENT", "MIMETYPE");
        this.parsedMimeTypesMap.forEach((path, mime) -> {
            String content = this.parsedFileContentMap.get(path);
            System.out.printf(
                    "%-" + finalMax + "s ->   %-11b   %s\n",
                    path,
                    (content != null && !content.isEmpty()),
                    mime
            );
        });
    }
}
