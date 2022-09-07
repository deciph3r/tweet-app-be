package com.ahamed.abdullah.tweetapp.controller;

import com.ahamed.abdullah.tweetapp.kafka.Producer;
import com.ahamed.abdullah.tweetapp.model.Like;
import com.ahamed.abdullah.tweetapp.model.Tweet;
import com.ahamed.abdullah.tweetapp.model.User;
import com.ahamed.abdullah.tweetapp.repository.LikeRepository;
import com.ahamed.abdullah.tweetapp.repository.TweetRepository;
import com.ahamed.abdullah.tweetapp.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/")
@Slf4j
public class TweetController {
    @Autowired
    Producer producer;

    @Autowired
    TweetRepository tweetRepository;

    @Autowired
    UserRepository userRepository;


    @Autowired
    LikeRepository likeRepository;


    @GetMapping("all")
    public List<Tweet> getAllTweets(){
        log.info("Retrieving all Tweets from the DB");
        return tweetRepository.findAll(Sort.by(Sort.Direction.DESC,"postTime")).parallelStream().map((e)-> {
            e.setLikes(likeRepository.countByLikedTweet(e.getId()));
            return e;
        }
        ).collect(Collectors.toList());
    }

    @GetMapping("{username}")
    public List<Tweet> getAllTweetsOfUser(@PathVariable String username){
        log.info("Retrieving all tweets of {} from DB",username);
        return tweetRepository.findByUsernameOrderByPostTimeDesc(username);
    }

    @GetMapping("users/all")
    public List<User> getAllUsers(){
        log.info("Retrieving all users from DB");
        return userRepository.getAllByUsername();
    }

    @GetMapping("user/search/{username}")
    public List<User> findUsers(@PathVariable String username){
        log.info("Searching users with the key {}",username);
        return userRepository.findAllByUsernameIsLikeIgnoreCase(username);
    }

    @PostMapping("{username}/add")
    public ResponseEntity<String> createTweet(@RequestBody @Valid Tweet tweet){
        log.info("Creating a tweet");
        tweet.setUsername(SecurityContextHolder.getContext().getAuthentication().getName());
        tweet.setPostTime(Instant.now().getEpochSecond());
        log.info("Saving a tweet into DB");
        Tweet save = tweetRepository.save(tweet);
        producer.sendMessage( "New Tweet: "+save.getTweet());
        return ResponseEntity.ok().build();
    }

    @PutMapping("{username}/update/{id}")
    public ResponseEntity<String> updateTweet(@PathVariable String id, @RequestBody @Valid Tweet tweet) throws Exception{
        log.info("Updating a tweet");
        Optional<Tweet> oTweet =  tweetRepository.findById(id);
        if(oTweet.isEmpty()){
            throw new Exception("tweet doesnot exist");
        }

        Tweet updateTweet = oTweet.get();
        String actionBy = SecurityContextHolder.getContext().getAuthentication().getName();
        if(!updateTweet.getUsername().contentEquals(actionBy)){
            throw new Exception("unauthorized");
        }
        log.info("Found the tweet and updating the tweet");
        updateTweet.setTweet(tweet.getTweet());
        updateTweet.setTweetTag(tweet.getTweetTag());
        updateTweet.setPostTime(Instant.now().getEpochSecond());
        log.info("Saving the updated tweet into DB");
        Tweet save = tweetRepository.save(updateTweet);
        producer.sendMessage("Updated Tweet: "+save.getTweet());
        return ResponseEntity.ok().build();
    }

    @PutMapping("{username}/like/{id}")
    public ResponseEntity<String> likeTweet(@PathVariable String id) throws Exception{
        log.info("Liking a Tweet");
        if(!tweetRepository.existsById(id)){
            throw new Exception("tweet doesnot exist");
        }
        if(likeRepository.existsByLikedTweetAndLikedBy(new ObjectId(id),SecurityContextHolder.getContext().getAuthentication().getName())){
            throw new Exception("already liked by the user");
        }
        Like like = new Like();
        like.setLikedTweet(new ObjectId(id));
        like.setLikedBy(SecurityContextHolder.getContext().getAuthentication().getName());
        log.info("Saving the like into DB");
        likeRepository.save(like);
        return ResponseEntity.ok().build();
    }

    @PutMapping("{username}/unlike/{id}")
    public ResponseEntity<String> unlikeTweet(@PathVariable String id) throws Exception{
        log.info("Unliking a Tweet");
        if(!tweetRepository.existsById(id)){
            throw new Exception("tweet doesnot exist");
        }
        log.info("Removing the like from DB");
        likeRepository.deleteByLikedTweetAndLikedBy(new ObjectId(id),SecurityContextHolder.getContext().getAuthentication().getName());

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("{username}/delete/{id}")
    public ResponseEntity<String> deleteTweet(@PathVariable String id) throws Exception{
        log.info("Deleting a Tweet");
        Optional<Tweet> oTweet = tweetRepository.findById(id);

        if(oTweet.isEmpty()){
            throw new Exception("tweet doesnot exist");
        }

        Tweet tweet = oTweet.get();
        String actionBy = SecurityContextHolder.getContext().getAuthentication().getName();
        if(!tweet.getUsername().contentEquals(actionBy)){
            throw new Exception("unauthorized");
        }
        log.info("Found the tweet, Removing the tweet from DB");
        tweetRepository.deleteById(id);
        likeRepository.deleteAllByLikedTweet(new ObjectId(id));

        return ResponseEntity.ok().build();
    }

    @PostMapping("{username}/reply/{id}")
    public ResponseEntity<String> replyTweet(@PathVariable String id, @RequestBody @Valid Tweet replyTweet) throws Exception{
        log.info("Replying to a tweet");
        Optional<Tweet> tweet = tweetRepository.findById(id);
        if(tweet.isEmpty()){
            throw new Exception("tweet doesnot exist");
        }

        Tweet updateTweet = tweet.get();
        log.info("Found the tweet that is being replied to");
        List<ObjectId> tweetReplies = updateTweet.getReplies();
        if(tweetReplies == null){
            tweetReplies = new ArrayList<>();
        }
        replyTweet.setUsername(SecurityContextHolder.getContext().getAuthentication().getName());
        replyTweet.setRepliedTo(new ObjectId(id));
        replyTweet.setPostTime(Instant.now().getEpochSecond());
        log.info("Saving new reply Tweet to DB");
        Tweet save = tweetRepository.save(replyTweet);
        log.info("Updating the replies list of Old Tweet and saving it to DB");
        tweetReplies.add(save.getId());
        updateTweet.setReplies(tweetReplies);
        tweetRepository.save(updateTweet);
        producer.sendMessage(save.getTweet());

        return ResponseEntity.ok().build();
    }

    @GetMapping("tweet/{id}")
    public ResponseEntity<Tweet> getTweet(@PathVariable String id) throws  Exception{
        log.info("Retrieving a Tweet with it's Id");
        Optional<Tweet> oTweet = tweetRepository.findById(id);
        if(oTweet.isEmpty()){
            throw new Exception("Tweet Does Not Exist");
        }
        return ResponseEntity.ok(oTweet.get());
    }

    @GetMapping("tweet/likedBy/{id}")
    public ResponseEntity<Boolean> isLikedByUser(@PathVariable String id) throws  Exception{
        log.info("Finding if a tweet is liked by the user");
        return ResponseEntity.ok().body(likeRepository.existsByLikedTweetAndLikedBy(new ObjectId(id),SecurityContextHolder.getContext().getAuthentication().getName()));
    }

}
