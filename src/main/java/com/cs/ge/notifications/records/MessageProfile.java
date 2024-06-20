package com.cs.ge.notifications.records;

public record MessageProfile(
        String id,
        String civility,
        String firstName,
        String lastName,
        String email,
        String phoneIndex,
        String phone,
        boolean trial
) {
}
