package ivandjoh.online.springfirestore.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.google.firebase.FirebaseOptions;
import javax.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service @Slf4j
public class FirebaseConfig {

    @PostConstruct
    public void initialization() {
        FileInputStream serviceAccount =null;

        try {
            String filename = "src/main/resources/nevyhod-app-firebase.json";
            Path path = Paths.get(filename);
            log.info("file exists: {}", Files.exists(path));
            log.info("filename: {} ", path.getFileName().toString());
            FileInputStream fis = new FileInputStream(path.toFile());

            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(fis))
                    .build();

            FirebaseApp.initializeApp(options);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
