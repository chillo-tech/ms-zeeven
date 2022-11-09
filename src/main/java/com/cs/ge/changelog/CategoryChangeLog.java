package com.cs.ge.changelog;

import com.cs.ge.entites.Categorie;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.mongodb.core.MongoTemplate;

@ChangeUnit(order = "002", id = "createCat", author = "athena")
public class CategoryChangeLog {

    private final MongoTemplate mongoTemplate;

    public CategoryChangeLog(final MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Execution
    public void create() {
        Categorie mariage = new Categorie();
        mariage.setLibelle("WEDDING");
        mariage.setImage("wedding.png");
        this.mongoTemplate.save(mariage);

        Categorie anniversaire = new Categorie();
        anniversaire.setLibelle("BIRTHDATE");
        anniversaire.setImage("birthdate.png");
        this.mongoTemplate.save(anniversaire);

        Categorie salon = new Categorie();
        salon.setLibelle("EVENT");
        salon.setImage("salon.png");
        this.mongoTemplate.save(salon);

        Categorie annnonce = new Categorie();
        annnonce.setLibelle("ANNOUNCEMENT");
        annnonce.setImage("announcement.png");
        this.mongoTemplate.save(annnonce);

        Categorie information = new Categorie();
        information.setLibelle("INFORMATION");
        information.setImage("information.png");
        this.mongoTemplate.save(information);


        Categorie promotion = new Categorie();
        promotion.setLibelle("PROMOTION");
        promotion.setImage("promotion.png");
        this.mongoTemplate.save(promotion);

    }

    @RollbackExecution
    public void rollback() {
    }
}
