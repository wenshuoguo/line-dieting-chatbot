package agent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import com.asprise.ocr.Ocr;
import org.languagetool.JLanguageTool;
import org.languagetool.Language;
import org.languagetool.language.AmericanEnglish;
import org.languagetool.rules.RuleMatch;

@Slf4j
@Component
public class ImageMenuParser{

    /**
     * Parse JSON object from Image uri 
     * @param uri URL string to use
     * @return A JSON array
     */
    public static String readJsonFromImage(String uri)
        throws IOException, JSONException {
        log.info("Entering readJsonFromImage");
        URL url = null;
        // handle Exception
        try {
            url = new URL(uri);
        } catch (MalformedURLException e) {
            log.info("The URL is not valid.");
        }
        Ocr.setUp(); // one time setup
        Ocr ocr = new Ocr(); // create a new OCR engine
        ocr.startEngine("eng", Ocr.SPEED_FASTEST); // English
        String s = ocr.recognize(new URL[] {url}
        , Ocr.RECOGNIZE_TYPE_ALL, Ocr.OUTPUT_FORMAT_PLAINTEXT);
        log.info("Result: " + s);
        // ocr more images here ...
        ocr.stopEngine();
        return s;
        
    }

    /**
     * Build menu information from JSON array
     * @param arr A JSON array
     * @return A JSON array; null if no dish is successfully parsed
     * @throws IOException
     */
    public static JSONArray buildMenu(String uri) {
        //langTool.activateDefaultPatternRules(); 
        // this method cannot be called from a static method
        String text = "";
		try {
            log.info("Entering image menu parser build menu");
			text = readJsonFromImage(uri);
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
        JSONArray arr = new JSONArray();
        String[] lines = text.split("\n");
        
        for (String line : lines) {
            log.info("this line has content: {}", line);
            // parse out special characters
            line = line.replaceAll("[^\\p{Alnum}]+", " ");
            // parse out numeric values, left only alpha characters
            line = line.replaceAll("[^A-Za-z]+", " ");  
            log.info("after parse out number and special character: {}", line);
            if (line.equals("")) continue;

            StringBuffer correctSentence = new StringBuffer(line);
            
            JLanguageTool langTool = new JLanguageTool(new AmericanEnglish());
            List<RuleMatch> matches = new ArrayList<RuleMatch>();
			try {
				matches = langTool.check(line);
			} catch (IOException e) {
				e.printStackTrace();
			}
        
            int offset = 0;
            for (RuleMatch match : matches) {
                correctSentence.replace(match.getFromPos() - offset
                , match.getToPos() - offset
                , match.getSuggestedReplacements().get(0));
                offset += (match.getToPos() 
                - match.getFromPos() 
                - match.getSuggestedReplacements().get(0).length());

                log.info("match: {}", match);
            }
            log.info("corrected sentence: {}", correctSentence);
            JSONObject dish = new JSONObject();
            dish.put("name", correctSentence);
            arr.put(dish);
        }
        return arr.length()==0 ? null : arr;
    }
}