package com.ahamed.abdullah.tweetapp.repository;

import com.ahamed.abdullah.tweetapp.model.Tweet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;


public interface TweetRepository extends MongoRepository<Tweet, String> {
    Page<Tweet> findByUsernameOrderByPostTimeDesc(String username, Pageable pageable);
    Page<Tweet> findAll(Pageable pageable);
}
