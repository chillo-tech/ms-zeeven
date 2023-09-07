package com.cs.ge.dto;

public record ProfileDTO(
        String id,
        String civility,
        String firstName,
        String lastName,
        String email,
        boolean trial,
        String phoneIndex,
        String phone
) {
}
