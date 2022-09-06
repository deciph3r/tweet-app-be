package com.ahamed.abdullah.tweetapp.repository;

import com.ahamed.abdullah.tweetapp.model.Like;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;


public interface LikeRepository extends MongoRepository<Like, ObjectId> {
    Long countByLikedTweet(ObjectId id);
    void deleteByLikedTweetAndLikedBy(ObjectId id,String username);

    Boolean existsByLikedTweetAndLikedBy(ObjectId id,String username);
    void deleteAllByLikedTweet(ObjectId id);
}
