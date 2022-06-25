package com.cs.ge.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Component
public class QRCodeGeneratorService {
    private final int WIDTH = 612;
    private final int HEIGHT = 612;

    public String guestQRCODE(String guest, String event) {
        Map<String, String> elements = new HashMap();
        elements.put("guest", guest);
        elements.put("event", event);
        return this.genertateQRCODE(elements);
    }

    private String genertateQRCODE(Map<String, String> elements) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            final QRCodeWriter qrCodeWriter = new QRCodeWriter();
            final BitMatrix bitMatrix = qrCodeWriter.encode(objectMapper.writeValueAsString(elements), BarcodeFormat.QR_CODE, this.WIDTH, this.HEIGHT);
            final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", byteArrayOutputStream);
            return Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray());
        } catch (final WriterException ex) {
            ex.printStackTrace();
        } catch (final IOException ex) {
            ex.printStackTrace();
        }
        return null;
    }
}
