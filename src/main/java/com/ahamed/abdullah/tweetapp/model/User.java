package com.ahamed.abdullah.tweetapp.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotBlank;
import java.util.List;

@Document("users")
@Data
public class User {
    @Id
    private ObjectId id;
    @Indexed(unique = true)
    private String username;
    private String firstName;
    private String lastName;
    private String password;
    private String email;
    @NotBlank(message = "reset-key cannot be empty")
    private String resetKey;

}
