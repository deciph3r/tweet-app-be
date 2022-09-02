package com.ahamed.abdullah.tweetapp.kafka;

import com.ahamed.abdullah.tweetapp.model.Tweet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class Producer
{
    public static final String topic = "tweets";

    @Autowired
    private KafkaTemplate<String,String> kafkaTemp;

    public void sendMessage(String tweet)
    {
        System.out.println("Publishing to topic"+ topic);
        this.kafkaTemp.send(topic,tweet);
    }
}