package backend.chessmate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ChessMateApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChessMateApplication.class, args);
    }

}
