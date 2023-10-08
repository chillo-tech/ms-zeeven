package com.cs.ge.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum Civility {
    MR,
    Mr,
    MRS,
    Mme,
    MME,
    Mlle,
    MLLE,
    MR_MRS,
    @JsonEnumDefaultValue UNKNOWN;

    @JsonCreator
    public static Civility name(final String name) {
        for (final Civility c : values()) {
            if (c.name().equals(name)) { //change accordingly
                return c;
            }
        }
        return null;
    }
}
