package searchengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.io.IOException;

@SpringBootApplication
@EnableAsync
@EnableTransactionManagement
@EnableRetry
@EnableCaching
@EnableScheduling
public class ApplicationStarter {
    private static final int APP_PORT = 8080;

    public static void main(String[] args) {
        SpringApplication.run(ApplicationStarter.class, args);
        openSiteInBrowser();
    }

    private static void openSiteInBrowser() {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            Runtime runtime = Runtime.getRuntime();

            if (os.contains("win")) {
                runtime.exec("rundll32 url.dll,FileProtocolHandler " + "http://localhost:" + APP_PORT);
            } else if (os.contains("mac")) {
                runtime.exec("open " + "http://localhost:" + APP_PORT);
            } else if (os.contains("nix") || os.contains("nux")) {
                String[] browsers = {"xdg-open", "gnome-open", "kde-open", "x-www-browser", "firefox", "mozilla", "opera", "konqueror", "epiphany", "netscape"};
                boolean opened = false;
                for (String browser : browsers) {
                    try {
                        runtime.exec(new String[]{browser, "http://localhost:" + APP_PORT});
                        opened = true;
                        break;
                    } catch (IOException e) {
                        // Продолжаем итерацию в случае ошибки
                    }
                }
                if (!opened) {
                    throw new RuntimeException("Не удалось открыть браузер");
                }
            } else {
                throw new RuntimeException("Неподдерживаемая операционная система");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}