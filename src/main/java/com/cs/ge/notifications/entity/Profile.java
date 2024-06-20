package com.cs.ge.notifications.entity;

import com.cs.ge.dto.ProfileParams;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public abstract class Profile {
    protected String id;
    protected String civility;
    protected String firstName;
    protected String lastName;
    protected String email;
    protected boolean trial;
    protected String phoneIndex;
    protected String phone;
    protected List<ProfileParams> others;
}
