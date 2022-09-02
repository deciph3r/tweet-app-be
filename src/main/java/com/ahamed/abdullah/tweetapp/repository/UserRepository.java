package com.ahamed.abdullah.tweetapp.repository;

import com.ahamed.abdullah.tweetapp.model.User;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public interface UserRepository extends MongoRepository<User, ObjectId> {
    Optional<User> findByUsername(String username);

    Boolean existsByUsername(String username);
    @Query(value = "{}",fields = "{username: 1, _id: 0,firstName: 1,lastName: 1,email: 1}")
    List<User> getAllByUsername();

    @Query(fields = "{username: 1, _id: 0,firstName: 1,lastName: 1,email: 1}")
    List<User> findAllByUsernameIsLikeIgnoreCase(String username);
}
