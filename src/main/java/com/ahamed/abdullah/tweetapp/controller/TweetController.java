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
        return tweetRepository.findAll(Sort.by(Sort.Direction.DESC,"postTime")).parallelStream().map((e)-> {
            e.setLikes(likeRepository.countByLikedTweet(e.getId()));
            return e;
        }
        ).collect(Collectors.toList());
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
        String actionBy = SecurityContextHolder.getContext().getAuthentication().getName();
        if(!updateTweet.getUsername().contentEquals(actionBy)){
            log.info("{}",actionBy.contentEquals(updateTweet.getUsername()));
            throw new Exception("unauthorized");
        }
        updateTweet.setTweet(tweet.getTweet());
        updateTweet.setTweetTag(tweet.getTweetTag());
        updateTweet.setPostTime(Instant.now().getEpochSecond());
        Tweet save = tweetRepository.save(updateTweet);
        producer.sendMessage(save.getTweet());
        return ResponseEntity.ok().build();
    }

    @PutMapping("{username}/like/{id}")
    public ResponseEntity<String> likeTweet(@PathVariable String id) throws Exception{

        if(!tweetRepository.existsById(id)){
            throw new Exception("tweet doesnot exist");
        }
        if(likeRepository.existsByLikedTweetAndLikedBy(new ObjectId(id),SecurityContextHolder.getContext().getAuthentication().getName())){
            throw new Exception("already liked by the user");
        }
        Like like = new Like();
        like.setLikedTweet(new ObjectId(id));
        like.setLikedBy(SecurityContextHolder.getContext().getAuthentication().getName());
        likeRepository.save(like);
        return ResponseEntity.ok().build();
    }

    @PutMapping("{username}/unlike/{id}")
    public ResponseEntity<String> unlikeTweet(@PathVariable String id) throws Exception{

        if(!tweetRepository.existsById(id)){
            throw new Exception("tweet doesnot exist");
        }
        likeRepository.deleteByLikedTweetAndLikedBy(new ObjectId(id),SecurityContextHolder.getContext().getAuthentication().getName());

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("{username}/delete/{id}")
    public ResponseEntity<String> deleteTweet(@PathVariable String id) throws Exception{
        Optional<Tweet> oTweet = tweetRepository.findById(id);

        if(oTweet.isEmpty()){
            throw new Exception("tweet doesnot exist");
        }

        Tweet tweet = oTweet.get();
        String actionBy = SecurityContextHolder.getContext().getAuthentication().getName();
        if(!tweet.getUsername().contentEquals(actionBy)){
            throw new Exception("unauthorized");
        }
        tweetRepository.deleteById(id);
        likeRepository.deleteAllByLikedTweet(new ObjectId(id));

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

    @GetMapping("tweet/likedBy/{id}")
    public ResponseEntity<Boolean> isLikedByUser(@PathVariable String id) throws  Exception{
        return ResponseEntity.ok().body(likeRepository.existsByLikedTweetAndLikedBy(new ObjectId(id),SecurityContextHolder.getContext().getAuthentication().getName()));
    }

}
