package com.cs.ge.changelog;

import com.cs.ge.entites.Category;
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
        Category mariage = new Category();
        mariage.setLabel("WEDDING");
        mariage.setImage("wedding.png");
        this.mongoTemplate.save(mariage);

        Category anniversaire = new Category();
        anniversaire.setLabel("BIRTHDATE");
        anniversaire.setImage("birthdate.png");
        this.mongoTemplate.save(anniversaire);

        Category salon = new Category();
        salon.setLabel("EVENT");
        salon.setImage("salon.png");
        this.mongoTemplate.save(salon);

        Category annnonce = new Category();
        annnonce.setLabel("ANNOUNCEMENT");
        annnonce.setImage("announcement.png");
        this.mongoTemplate.save(annnonce);

        Category information = new Category();
        information.setLabel("INFORMATION");
        information.setImage("information.png");
        this.mongoTemplate.save(information);


        Category promotion = new Category();
        promotion.setLabel("PROMOTION");
        promotion.setImage("promotion.png");
        this.mongoTemplate.save(promotion);

    }

    @RollbackExecution
    public void rollback() {
    }
}
