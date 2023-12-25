package searchengine;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.io.IOException;

@SpringBootApplication
@EnableAsync
@EnableTransactionManagement
public class Application {
    public static void main(String[] args){
        SpringApplication.run(Application.class, args);
        openSite();
    }
    private static void openSite() {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            Runtime rt = Runtime.getRuntime();

            if (os.contains("win")) {
                rt.exec("rundll32 url.dll,FileProtocolHandler " + "http://localhost:8082");
            } else if (os.contains("mac")) {
                rt.exec("open " + "http://localhost:8082");
            } else if (os.contains("nix") || os.contains("nux")) {
                String[] browsers = {"xdg-open", "gnome-open", "kde-open", "x-www-browser", "firefox", "mozilla", "opera", "konqueror", "epiphany", "netscape"};
                boolean opened = false;
                for (String browser : browsers) {
                    try {
                        rt.exec(new String[]{browser, "http://localhost:8082"});
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