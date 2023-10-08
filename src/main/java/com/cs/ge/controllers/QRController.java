package com.cs.ge.controllers;

import com.cs.ge.entites.QRCodeEntity;
import com.cs.ge.entites.QRCodeStatistic;
import com.cs.ge.enums.QRCodeType;
import com.cs.ge.services.qrcode.QRCodeGeneratorService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping(path = "qr-code", produces = APPLICATION_JSON_VALUE)
public class QRController {
    private final QRCodeGeneratorService qrCodeGeneratorService;


    @ResponseStatus(HttpStatus.OK)
    @PostMapping(consumes = APPLICATION_JSON_VALUE)
    public String add(
            @RequestParam(defaultValue = "true") final boolean simulate,
            @RequestBody final QRCodeEntity qrCodeEntity,
            @RequestHeader final Map<String, String> headers
    ) throws IOException {
        return this.qrCodeGeneratorService.generate(qrCodeEntity, simulate, headers);
    }

    @GetMapping
    public List<QRCodeEntity> search() throws IOException {
        return this.qrCodeGeneratorService.search();
    }

    @GetMapping(value = "/{publicId}")
    public ResponseEntity<Object> get(@PathVariable final String publicId, @RequestHeader final Map<String, String> headers) throws IOException {
        final Map<String, Object> data = this.qrCodeGeneratorService.content(publicId, headers);
        final String result = (String) data.get("result");
        if (data.get("type").equals(QRCodeType.TEXT)) {
            return ResponseEntity.status(HttpStatus.OK).body(result);
        } else {
            final RedirectView redirectView = new RedirectView();
            redirectView.setUrl(result);
            return ResponseEntity.status(HttpStatus.TEMPORARY_REDIRECT).location(URI.create(result)).build();

        }
    }

    @GetMapping(value = "/ip/{publicId}")
    public Map<String, Object> getIp(@PathVariable final String publicId, @RequestHeader final Map<String, String> headers) throws IOException {
        return this.qrCodeGeneratorService.ipdata(publicId);
    }

    @PatchMapping(value = "/{id}")
    public void path(@PathVariable final String id, @RequestBody final QRCodeEntity qrCodeEntity) throws IOException {
        this.qrCodeGeneratorService.patch(id, qrCodeEntity);
    }

    @DeleteMapping(value = "/{id}")
    public void get(@PathVariable final String id) throws IOException {
        this.qrCodeGeneratorService.delete(id);
    }

    @GetMapping(value = "/private/{id}")
    public QRCodeEntity userQrCode(@PathVariable final String id) {
        return this.qrCodeGeneratorService.read(id);
    }

    @GetMapping(value = "/private/{id}/statistics")
    public Stream<QRCodeStatistic> statistics(@PathVariable final String id) {
        return this.qrCodeGeneratorService.statistics(id);
    }

}
