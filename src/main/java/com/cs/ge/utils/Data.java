package com.cs.ge.utils;

import com.cs.ge.enums.QRCodeShapeType;

import java.util.List;
import java.util.Map;

import static com.cs.ge.enums.QRCodeShapeType.TEXT_BOTTOM;
import static com.cs.ge.enums.QRCodeShapeType.TEXT_TOP;

public class Data {
    public static final String EMAIL_FROM = "noreply@zeeven.com";
    public static final int DEFAULT_STOCK_SIZE = 3;
    public static final String IMAGE_FORMAT = "JPG";
    public static final int QRCODE_WIDTH = 500;
    public static final int QRCODE_HEIGHT = 500;
    public static final String PATTERN_FORMAT = "MM/dd/yyyy HH:mm";
    public static final List<String> QRCODE_STATISTICS_KEY = List.of(
            "host",
            "user-agent",
            "x-forwarded-for",
            "x-real-ip"

    );
    public static final Map<String, String> CIVILITY_MAPPING = Map.of(
            "MR_MRS", "Mr & Mme",
            "MME", "Mme",
            "Mme", "Mme",
            "MR", "M.",
            "Mr", "M.",
            "MLLE", "Mlle"
    );
    public static final Map<QRCodeShapeType, Map<String, List<Integer>>> QR_CODE_SHAPE_PARAMS = Map.of(
            TEXT_BOTTOM, Map.of(
                    "shape", List.of(QRCODE_WIDTH + 50, QRCODE_HEIGHT + 150),
                    "image", List.of(25, 25, QRCODE_WIDTH, QRCODE_HEIGHT),
                    "text", List.of(0, QRCODE_HEIGHT + 100)
            ),
            TEXT_TOP, Map.of(
                    "shape", List.of(QRCODE_WIDTH + 50, QRCODE_HEIGHT + 150),
                    "image", List.of(25, 120, QRCODE_WIDTH, QRCODE_HEIGHT),
                    "text", List.of(0, 80)
            )
    );

}
