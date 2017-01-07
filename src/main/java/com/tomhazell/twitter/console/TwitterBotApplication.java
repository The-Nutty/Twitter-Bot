package com.tomhazell.twitter.console;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import twitter4j.Query;

@SpringBootApplication
public class TwitterBotApplication implements CommandLineRunner {

    @Autowired
    AccountRepository accountRepository;

    public static int RETWEET_TIME_OUT = 1000 * 38;//seconds
    public static int LIKE_TIME_OUT = 1000 * 5;//seconds
    public static int REPLY_TIME_OUT = 1000 * 38;//seconds
    public static int FOLLOW_TIME_OUT = 1000 * 5;//secondsTODO
    public static int SEARCH_TIME_OUT = 1000 * 15;//seconds

    //TODO gleam.io support by adding a query for all gleam links then prompting manual
    public static void main(String[] args) {
        SpringApplication.run(TwitterBotApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        //create the default account
        //TODO import this from JSON
        Account account = new Account();
        account.setName("Tom Hazell");
        account.setQuerys("rt to win csgo,retweet to win csgo,\"steam code giveaway\", \"#RTtoWIN\", retweet win gift card");//#win???
        account.setToken("ENTER_YOUR_OWN");
        account.setTokenSecret("ENTER_YOUR_OWN");
        account.setConsumerKey("ENTER_YOUR_OWN");
        account.setConsumerSecret("ENTER_YOUR_OWN");
        account.setResultType(Query.ResultType.mixed);
        accountRepository.save(account);
    }
}
