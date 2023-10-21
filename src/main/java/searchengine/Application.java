package searchengine;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import searchengine.services.lemmas.LemmatizationService;

import java.io.IOException;
@SpringBootApplication
@EnableAsync
@EnableTransactionManagement
public class Application {
    public static void main(String[] args) throws IOException {
        SpringApplication.run(Application.class, args);

    }
}