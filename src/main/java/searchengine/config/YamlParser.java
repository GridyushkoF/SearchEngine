package searchengine.config;

import lombok.extern.log4j.Log4j2;
import org.yaml.snakeyaml.Yaml;
import searchengine.util.LogMarkers;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log4j2
public class YamlParser {
    private static final String PATH_TO_PROPERTIES_FILE = "application.yaml";
    public static List<ConfigSite> getSitesFromYaml() {
        Yaml yaml = new Yaml();
        List<ConfigSite> configSiteList = new ArrayList<>();
        try (FileInputStream inputStream = new FileInputStream(PATH_TO_PROPERTIES_FILE)) {
            Map<String, Object> dataFromYamlFile = yaml.load(inputStream);
            Pattern configSitePattern = Pattern.compile("\\{url=(.*?), name=(.*?)\\}");
            String siteList = dataFromYamlFile.get("indexing-settings").toString();
            Matcher matcher = configSitePattern.matcher(siteList);
            while (matcher.find()) {
                String url = matcher.group(1);
                String name = matcher.group(2);
                configSiteList.add(new ConfigSite(url, name));
            }
        } catch (Exception e) {
            log.error(LogMarkers.EXCEPTIONS, "Exception while getting sites from YAML-file", e);
        }
        return configSiteList;
    }
}