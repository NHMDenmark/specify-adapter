package dk.northtech.dassco_specify_adapter.services;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Optional;

@Service
public class FileService {

    public record FileResult(InputStream is, String filename) {
    }

    public Optional<FileResult> getFile(String path) {
        File file = new File(path);
        if (file.exists()) {
            try {
                return Optional.of(new FileResult(new FileInputStream(file), file.getName()));
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        } else {
            return Optional.empty();
        }
    }

}
