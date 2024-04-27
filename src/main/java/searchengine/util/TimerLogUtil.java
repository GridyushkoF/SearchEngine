package searchengine.util;

import lombok.extern.log4j.Log4j2;


@Log4j2
public class TimerLogUtil {
    private String currentMessage;
    private long startTime;
    private TimerLogUtil() {

    }
    public  void markStart() {
        startTime = System.currentTimeMillis();
    }
    public  void markStart(String logMessage) {
        log.error(logMessage);
        currentMessage = logMessage;
        startTime = System.currentTimeMillis();
    }
    public void printDelta() {
        if(currentMessage != null) {
            log.error(currentMessage + " || " + "RESULT_TIME: " + (System.currentTimeMillis() - startTime) + " ms");
            currentMessage = "";
        } else {
            log.error("RESULT_TIME: " + (System.currentTimeMillis() - startTime) + " ms");
        }
    }
    public static TimerLogUtil newInstance() {
        return new TimerLogUtil();
    }
    public static void surroundWithTimer(Runnable runnable) {
        TimerLogUtil timer = TimerLogUtil.newInstance();
        timer.markStart();
        runnable.run();
        timer.printDelta();
    }
    public static void surroundWithTimer(String message, Runnable runnable) {
        TimerLogUtil timer = TimerLogUtil.newInstance();
        timer.markStart(message);
        runnable.run();
        timer.printDelta();
    }
}
