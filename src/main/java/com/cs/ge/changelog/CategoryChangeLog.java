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
        mariage.setLibelle("Mariage");
        this.mongoTemplate.save(mariage);

        Categorie anniversaire = new Categorie();
        anniversaire.setLibelle("Anniversaire");
        this.mongoTemplate.save(anniversaire);

        Categorie salon = new Categorie();
        salon.setLibelle("Salon");
        this.mongoTemplate.save(salon);

    }

    @RollbackExecution
    public void rollback() {
    }
}
