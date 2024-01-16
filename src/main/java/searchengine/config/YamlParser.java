package searchengine.config;

import lombok.extern.log4j.Log4j2;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log4j2

public class YamlParser {

    public static List<ConfigSite> getSitesFromYaml() {
        Yaml yaml = new Yaml();
        List<ConfigSite> url2name = new ArrayList<>();
        try (FileInputStream inputStream = new FileInputStream("application.yaml")) {
            Map<String, Object> data = yaml.load(inputStream);
            Pattern pattern = Pattern.compile("\\{url=(.*?), name=(.*?)\\}");
            String sitesList = data.get("indexing-settings").toString();
            Matcher matcher = pattern.matcher(sitesList);
            while (matcher.find()) {
                String url = matcher.group(1);
                String name = matcher.group(2);
                url2name.add(new ConfigSite(url, name));
            }
        } catch (Exception e) {
            log.error("exception while parsing sites from yaml", e);
        }
        return url2name;
    }
}