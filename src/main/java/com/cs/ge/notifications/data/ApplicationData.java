package com.cs.ge.notifications.data;

import java.util.Map;

public class ApplicationData {
    public static final String FOOTER_TEXT = "Ce message vous été transmis via ZEEVEN";
    public static final Map<String, String> CIVILITY_MAPPING = Map.of(
            "MR_MRS", "Mr & Mme",
            "MME", "Mme",
            "Mme", "Mme",
            "Mr", "M.",
            "MR", "M.",
            "M", "M.",
            "MLLE", "Mlle"
    );

}
