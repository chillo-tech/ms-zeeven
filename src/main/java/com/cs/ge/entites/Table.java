package com.cs.ge.entites;

import com.cs.ge.enums.Civility;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Table {
    @Id
    //@JsonProperty(access = WRITE_ONLY)
    protected String id;
    private String publicId;
    private String slug;
    private String name;
    protected String position;
    protected String description;
    protected String typeTable;
    protected String places;

}
