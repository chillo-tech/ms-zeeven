package com.cs.ge.notifications.records;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Profile(
        @JsonProperty("id") String id,
        @JsonProperty("civility") String civility,
        @JsonProperty("firstName") String firstName,
        @JsonProperty("lastName") String lastName,
        @JsonProperty("email") String email,
        @JsonProperty("phoneIndex") String phoneIndex,
        @JsonProperty("phone") String phone
) {

}
