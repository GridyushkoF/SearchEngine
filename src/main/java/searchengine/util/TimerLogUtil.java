package searchengine.util;

import lombok.extern.log4j.Log4j2;


@Log4j2
public class TimerLogUtil {
    private static String currentMessage;
    private static long startTime;
    public static  void markStart() {
        startTime = System.currentTimeMillis();
    }
    public static void markStart(String logMessage) {
        currentMessage = logMessage;
        startTime = System.currentTimeMillis();
    }
    public static void printDelta() {
        printCurrentMessageIfNotNull();
        log.error("RESULT_TIME: " + (System.currentTimeMillis() - startTime) + " ms");
    }

    private static void printCurrentMessageIfNotNull() {
        if(currentMessage != null) {
            log.error(currentMessage);
            currentMessage = "";
        }
    }
}
