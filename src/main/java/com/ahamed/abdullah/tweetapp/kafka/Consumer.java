package com.ahamed.abdullah.tweetapp.kafka;

import com.ahamed.abdullah.tweetapp.repository.TweetRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;


@Service
@Slf4j
public class Consumer {

    @Autowired
    private TweetRepository tweetRepository;

    @KafkaListener(topics = "tweets", groupId = "group_id")
    public void consume(String tweet) {
        log.info("Consumed message",tweet);
        log.info(String.format("Consumed message: %s", tweet));
    }
}