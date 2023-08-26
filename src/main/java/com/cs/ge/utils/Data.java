package com.cs.ge.utils;

import java.util.List;
import java.util.Map;

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

}
