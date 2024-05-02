package com.cs.ge.dto;

import com.cs.ge.enums.Civility;
import com.cs.ge.enums.Role;

public record ProfileDTO(
        String id,
        Civility civility,
        String firstName,
        String lastName,
        String email,
        boolean trial,
        String phoneIndex,
        String phone,
        Role role,

        StockDTO stock
) {
}
