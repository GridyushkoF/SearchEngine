package searchengine.services.other;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import searchengine.Application;

import java.util.Arrays;
import java.util.StringJoiner;

public class LogService {
    private static final Marker INFO = MarkerManager.getMarker("INFO");
    private static final Marker EXCEPTIONS = MarkerManager.getMarker("EXCEPTIONS");
    private static final Logger LOGGER = LogManager.getLogger(Application.class);

    public void info(String info) {
        LOGGER.info(INFO, info);
    }

    public void exception(Throwable e) {
        StringJoiner joiner = new StringJoiner("\n");
        joiner.add(e.getMessage());
        Arrays.stream(e.getStackTrace()).toList().forEach(stackTraceElement -> {
            joiner.add(stackTraceElement.toString());
        });
        LOGGER.error(EXCEPTIONS, joiner.toString());
    }
}
