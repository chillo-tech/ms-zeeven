package com.cs.ge.services.files;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Map;

@Slf4j
@Component
public class FilesHandler {
    final String basePath;

    public FilesHandler(@Value("${app.files.base-path:''}") final String basePath) {
        this.basePath = basePath;
    }

    public void handleMessage(final Map<String, Object> params) {
        try {
            final String filePath = String.valueOf(params.get("path"));
            if (filePath != null && !filePath.equals("null") && Strings.isNotEmpty(filePath)) {
                final String fullPath = String.format("%s/%s", this.basePath, filePath);
                final Path folder = Paths.get(fullPath).getParent();
                Files.createDirectories(folder);

                final String fileAsString = String.valueOf(params.get("file"));
                final byte[] decodedFile = Base64.getDecoder().decode(fileAsString);
                final File fullPathAsFile = new File(fullPath);
                if (Files.exists(Paths.get(fullPath))) {
                    FileUtils.delete(fullPathAsFile);
                }
                FileUtils.writeByteArrayToFile(fullPathAsFile, decodedFile);
            }
        } catch (final IOException e) {
            e.printStackTrace();
        }

    }
}
