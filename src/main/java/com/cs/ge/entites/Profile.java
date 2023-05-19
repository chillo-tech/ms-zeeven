package com.cs.ge.entites;

import com.cs.ge.enums.Civility;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Profile {
    @Id
    //@JsonProperty(access = WRITE_ONLY)
    protected String id;
    private String publicId;
    private String slug;
    private Civility civility;
    protected String firstName;
    protected String lastName;
    protected String email;
    protected String phoneIndex;
    protected String phone;
}
