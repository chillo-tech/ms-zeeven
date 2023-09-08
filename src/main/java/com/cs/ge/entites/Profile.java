package com.cs.ge.entites;

import com.cs.ge.enums.Civility;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY;

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
    protected String partner;
    protected String lastName;
    protected String email;
    protected String phoneIndex;
    protected String phone;
    protected boolean trial;
    protected List<Stock> stocks;
    
    @JsonProperty(access = WRITE_ONLY)
    protected List<UserAccount> contacts;

}
