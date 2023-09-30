package searchengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import searchengine.config.SitesList;
import searchengine.services.indexing.NodeLink;

import java.util.concurrent.CompletableFuture;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
        for(int i = 0; i < 10; i++)
        {

            CompletableFuture.runAsync(() -> {
                for (int j = 0; j < 10; j++)
                {
                    System.out.println("JANE");
                }
            });
            System.err.println("CONT");
        }
    }
}