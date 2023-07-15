package com.cs.ge.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ImageService {
    private final String imagesFolder;
    private final String imagesHost;
    private final String imagesRootfolder;

    public ImageService(
            @Value("${resources.images.folder}") String imagesFolder,
            @Value("${resources.images.host}") String imagesHost,
            @Value("${resources.images.root}") String imagesRootfolder
    ) {
        this.imagesHost = imagesHost;
        this.imagesFolder = imagesFolder;
        this.imagesRootfolder = imagesRootfolder;
    }
}
