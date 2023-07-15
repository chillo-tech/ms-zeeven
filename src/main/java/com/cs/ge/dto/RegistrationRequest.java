package com.cs.ge.dto;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Getter
@Setter
public class RegistrationRequest {
    @NonNull
    private String lastname;
    @NonNull
    private String firstname;
    @NonNull
    private String email;
    @NonNull
    private String phone;
}
