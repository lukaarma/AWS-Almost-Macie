import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;


public class ContentAnalyzer extends Tesseract {
    private final String RED = "\033[0;31m";
    private final String YELLOW = "\033[0;33m";
    private final String RESET = "\033[0m";

    String[] warnings;
    String[] alerts;


    public ContentAnalyzer(String configPath) throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();
        JSONObject config = (JSONObject) jsonParser.parse(new FileReader(configPath));
        JSONArray jsonWarnings = (JSONArray) config.get("warnings");
        JSONArray jsonAlerts = (JSONArray) config.get("alerts");

        this.warnings = new String[jsonWarnings.size()];
        this.alerts = new String[jsonAlerts.size()];

        // match withespace/endline to end the words to match (if present)credit ?cards?
        String regexStart = "(?i)\\s*";
        String regexEnd = "[\\s|\\n]*";
        for (int i = 0; i < warnings.length; i++) {
            this.warnings[i] = regexStart + jsonWarnings.get(i) + regexEnd;
        }
        for (int i = 0; i < alerts.length; i++) {
            this.alerts[i] = regexStart + jsonAlerts.get(i) + regexEnd;
        }

        // Tesseract setup
        this.setDatapath("C:\\bin\\Tesseract-OCR\\tessdata");
        this.setLanguage("eng");
        this.setTessVariable("user_defined_dpi", "100");
    }


    public String[] getAlerts() {
        return alerts;
    }

    public String[] getWarnings() {
        return warnings;
    }


    public void parseText(Map<Path, String> textContentMap) {
        // for each file
        textContentMap.forEach((path, content) -> {
            System.out.printf("Analyzing '%s'\n", path);

            Integer i = 1;

            for (String line : content.split("(\\r?\\n)")) {
                // check alerts
                Optional<String> alert = Arrays.stream(this.alerts).filter(line::matches).findFirst();

                // if no alerts check warnings
                if (alert.isEmpty()) {
                    Optional<String> warning = Arrays.stream(this.warnings).filter(line::matches).findFirst();
                    if (warning.isPresent()) {
                        this.printWarning(i, line);
                    }
                }
                else {
                    this.printAlert(i, line);
                }
                i++;
            }
        });
    }

    public void parseImages (Set<Path> imagesPathSet) {
        Map<Path, String> contentMap = new HashMap<>();

        imagesPathSet.forEach(path -> {
            try {
                contentMap.put(path, this.doOCR(path.toFile()));
            }
            catch (TesseractException e) {
                System.out.println("A Tesseract exception happened: " + e.getMessage());
            }
        });
        if (awsAlmostMacie.argv.hasOption('d')) {
            System.out.println(contentMap);
        }
        this.parseText(contentMap);
    }


    private void printAlert(Integer i, String match) {
        this.print(i, match, this.RED + "[ALERT]" + this.RESET);
    }

    private void printWarning(Integer i, String match) {
        this.print(i, match, this.YELLOW + "[WARNING]" + this.RESET);
    }

    private void print(Integer i, String match, String type) {
        if (match.strip().matches("(?i)[a-z]{2}\\s?\\d{2}\\s?[a-z]\\s?(?:\\d{5}\\s?){2}[a-z0-9]{12}")) {
            match = "IBAN CODE";
        }

        System.out.printf("\t%s found '%s' at line %d\n", type, match.strip(), i);
    }
}
