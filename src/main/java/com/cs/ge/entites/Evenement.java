package com.cs.ge.entites;

import com.cs.ge.enums.Channel;
import com.cs.ge.enums.EventStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY;

@Document("EVENEMENTS")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Evenement {
    @Id
    @JsonProperty(access = WRITE_ONLY)
    private String id;
    private String publicId;
    @NotBlank(message = "Veuillez donner un nom à votre évènement")
    private String nom;
    @NotBlank(message = "Veuillez écrire un message à votre évènement")
    private String message;
    private EventStatus statut;
    private List<Guest> invites;
    private List<Table> tables;
    private List<Channel> channels;
    private UserAccount userAccount;
    private Map<String, String> variables;
    private Adresse adresse;
    private Category category;
    @NotBlank(message = "Veuillez définir une date de début à votre évènement")
    private Date dateDebut;
    private Date dateFin;
    @NotBlank(message = "Veuillez définir une heure de début à votre évènement")
    private String heureDebut;
    private String heureFin;
}
