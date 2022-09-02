package com.ahamed.abdullah.tweetapp.controller;

import com.ahamed.abdullah.tweetapp.kafka.Producer;
import com.ahamed.abdullah.tweetapp.model.Tweet;
import com.ahamed.abdullah.tweetapp.model.User;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/")
@Slf4j
@CrossOrigin(origins = "http://localhost:3000")
public class TweetController {
    @Autowired
    Producer producer;

    @Autowired
    TweetRepository tweetRepository;

    @Autowired
    UserRepository userRepository;


    @GetMapping("all")
    public List<Tweet> getAllTweets(){
        return tweetRepository.findAll(Sort.by(Sort.Direction.DESC,"postTime"));

    }

    @GetMapping("{username}")
    public List<Tweet> getAllTweetsOfUser(@PathVariable String username){
        return tweetRepository.findByUsernameOrderByPostTimeDesc(username);
    }

    @GetMapping("users/all")
    public List<User> getAllUsers(){
        return userRepository.getAllByUsername();
    }

    @GetMapping("user/search/{username}")
    public List<User> findUsers(@PathVariable String username){
        return userRepository.findAllByUsernameIsLikeIgnoreCase(username);
    }

    @PostMapping("{username}/add")
    public ResponseEntity<String> createTweet(@PathVariable String username, @RequestBody @Valid Tweet tweet){
        tweet.setUsername(SecurityContextHolder.getContext().getAuthentication().getName());
        tweet.setPostTime(Instant.now().getEpochSecond());
        Tweet save = tweetRepository.save(tweet);
        producer.sendMessage(save.getTweet());
        return ResponseEntity.ok().build();
    }

    @PutMapping("{username}/update/{id}")
    public ResponseEntity<String> updateTweet(@PathVariable String id, @RequestBody @Valid Tweet tweet) throws Exception{
        Optional<Tweet> oTweet =  tweetRepository.findById(id);
        if(oTweet.isEmpty()){
            throw new Exception("tweet doesnot exist");
        }
        Tweet updateTweet = oTweet.get();
        updateTweet.setTweet(tweet.getTweet());
        updateTweet.setTweetTag(tweet.getTweetTag());
        updateTweet.setPostTime(Instant.now().getEpochSecond());
        Tweet save = tweetRepository.save(updateTweet);
        producer.sendMessage(save.getTweet());
        return ResponseEntity.ok().build();
    }

    @PutMapping("{username}/like/{id}")
    public ResponseEntity<String> likeTweet(@PathVariable String id) throws Exception{
        Optional<Tweet> tweet = tweetRepository.findById(id);
        if(tweet.isEmpty()){
            throw new Exception("tweet doesnot exist");
        }
        Tweet updateTweet = tweet.get();
        updateTweet.setLikes(updateTweet.getLikes() + 1);
        Tweet save = tweetRepository.save(updateTweet);
        producer.sendMessage(save.getTweet());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("{username}/delete/{id}")
    public ResponseEntity<String> deleteTweet(@PathVariable String id) throws Exception{

        if(!tweetRepository.existsById(id)){
            throw new Exception("tweet doesnot exist");
        }
        tweetRepository.deleteById(id);

        return ResponseEntity.ok().build();
    }

    @PostMapping("{username}/reply/{id}")
    public ResponseEntity<String> replyTweet(@PathVariable String id, @RequestBody @Valid Tweet replyTweet) throws Exception{
        Optional<Tweet> tweet = tweetRepository.findById(id);
        if(tweet.isEmpty()){
            throw new Exception("tweet doesnot exist");
        }

        Tweet updateTweet = tweet.get();
        List<ObjectId> tweetReplies = updateTweet.getReplies();
        if(tweetReplies == null){
            tweetReplies = new ArrayList<>();
        }
        replyTweet.setUsername(SecurityContextHolder.getContext().getAuthentication().getName());
        replyTweet.setRepliedTo(new ObjectId(id));
        replyTweet.setPostTime(Instant.now().getEpochSecond());
        Tweet save = tweetRepository.save(replyTweet);
        tweetReplies.add(save.getId());
        updateTweet.setReplies(tweetReplies);
        tweetRepository.save(updateTweet);
        producer.sendMessage(save.getTweet());

        return ResponseEntity.ok().build();
    }

    @GetMapping("tweet/{id}")
    public ResponseEntity<Tweet> getTweet(@PathVariable String id) throws  Exception{
        Optional<Tweet> oTweet = tweetRepository.findById(id);
        if(oTweet.isEmpty()){
            throw new Exception("Tweet Does Not Exist");
        }
        return ResponseEntity.ok(oTweet.get());
    }

}
