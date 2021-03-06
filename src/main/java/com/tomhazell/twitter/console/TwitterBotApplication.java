package com.tomhazell.twitter.console;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tomhazell.twitter.console.users.Account;
import com.tomhazell.twitter.console.users.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

@SpringBootApplication
public class TwitterBotApplication implements CommandLineRunner {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private ApplicationContext appContext;

    //all in seconds
    public static int RETWEET_TIME_OUT = 1000 * 37;//because we can make 2400 tweets a day
    public static int LIKE_TIME_OUT = 1000 * 5;//because we can make 180 actions every 15 mins
    public static int REPLY_TIME_OUT = 1000 * 37;//because we can make 2400 tweets a day
    public static int FOLLOW_TIME_OUT = 1000 * 5;//because we can make 180 actions every 15 mins
    public static int UNFOLLOW_TIME_OUT = 1000 * 7;//because we can make 180 actions every 15 mins.... This is used when we clear the followers if we are full
    public static int SEARCH_TIME_OUT = 1000 * 15;//because we can make 180 actions every 15 mins
    public static int FRIEND_LIST_TIME_OUT = 1000 * 60;//because we can make 15 actions every 15 mins
    public static int RATE_LIMIT_COOLDOWN = 1000 * 60 * 10;//we can make 180 actions every 15 mins, if we hit this limit (which we should not) then wait for 10 mins

    public static void main(String[] args) {
        SpringApplication.run(TwitterBotApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        //create the default accounts as specified in default-users.json

        String usersJson = readResourceFile("classpath:default-users.json");

        Account[] users = mapper.readValue(usersJson, Account[].class);

        for (Account user : users) {
            accountRepository.save(user);
        }
    }

    private String readResourceFile(String filename) throws IOException {
        InputStream is = null;
        StringBuilder sb = new StringBuilder();
        BufferedReader br = null;

        try {
            Resource resource = appContext.getResource(filename);
            is = resource.getInputStream();
            br = new BufferedReader(new InputStreamReader(is));

            String line;

            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        } finally {
            if (is != null) {
                is.close();
            }
            if (br != null) {
                br.close();
            }
        }
        return sb.toString();
    }
}