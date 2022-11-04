package com.cs.ge.entites;

import com.cs.ge.enums.Channel;
import com.cs.ge.enums.EventStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotBlank;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Document("EVENEMENTS")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Evenement {
    @Id
    private String id;
    @NotBlank(message = "Veuillez donner un nom à votre évènement")
    private String nom;
    @NotBlank(message = "Veuillez écrire un message à votre évènement")
    private String message;
    @NotBlank(message = "Veuillez définir une date de début à votre évènement")
    private Date dateDebut;
    private Date dateFin;
    @NotBlank(message = "Veuillez définir une heure de début à votre évènement")
    private String heureDebut;
    private String heureFin;
    private EventStatus statut;
    private List<Guest> invites;
    private List<Channel> channels;
    private Utilisateur utilisateur;
    private Map<String, String> variables;
    private Adresse adresse;
    private Categorie categorie;


}
