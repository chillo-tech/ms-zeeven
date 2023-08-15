package com.cs.ge.utils;

import java.util.List;

public class Data {
    public static final String EMAIL_FROM = "noreply@zeeven.com";
    public static final int DEFAULT_STOCK_SIZE = 3;
    public static final int QRCODE_WIDTH = 500;
    public static final int QRCODE_HEIGHT = 500;
    public static final List<String> QRCODE_STATISTICS_KEY = List.of(
            "host",
            "user-agent",
            "x-forwarded-for",
            "x-real-ip"

    );
}
