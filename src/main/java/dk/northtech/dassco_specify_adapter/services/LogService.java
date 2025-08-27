package dk.northtech.dassco_specify_adapter.services;

import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class LogService {
    FileService fileService;
    private static final Logger logger = LoggerFactory.getLogger(LogService.class);

    public static final String LOG_LOCATION = "target/logs";
    @Inject
    public LogService(FileService fileService) {
        this.fileService = fileService;

    }

    public List<String> listLogs() {
        return Arrays.stream(new File(LOG_LOCATION).listFiles())
                .map(f -> f.getName())
                .toList();
    }

    public Optional<FileService.FileResult> getFile(String name) {
        return fileService.getFile(LOG_LOCATION + "/" + name);
    }
}
