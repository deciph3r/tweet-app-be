package com.ahamed.abdullah.tweetapp.repository;

import com.ahamed.abdullah.tweetapp.model.Tweet;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TweetRepository extends MongoRepository<Tweet, String> {
    List<Tweet> findByUsernameOrderByPostTimeDesc(String username);
}
