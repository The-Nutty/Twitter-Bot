package com.tomhazell.twitter.console;

import com.tomhazell.twitter.console.tweets.TwitterActionRepository;
import com.tomhazell.twitter.console.users.Account;
import com.tomhazell.twitter.console.users.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import twitter4j.*;
import twitter4j.auth.AccessToken;
import twitter4j.util.function.Consumer;

import java.util.LinkedList;

/**
 * Created by Tom Hazell on 15/01/2017.
 */
public class TwitterBotStreamTask implements Runnable, StatusListener {

    private Logger logger;

    private TwitterActionRepository twitterActionRepository;
    private AccountRepository accountRepository;

    private Account account;
    private TwitterStream twitterStream;
    private Twitter twitter;

    private LinkedList<Status> queue = new LinkedList<>();


    public TwitterBotStreamTask(TwitterActionRepository repository, AccountRepository accountRepository, Account account) {
        this.account = account;
        this.twitterActionRepository = repository;
        this.accountRepository = accountRepository;
        logger = LoggerFactory.getLogger(getClass());
    }


    @Override
    public void run() {
        twitter = new TwitterFactory().getInstance();
        twitter.setOAuthConsumer(account.getConsumerKey(), account.getConsumerSecret());

        AccessToken token = new AccessToken(account.getToken(), account.getTokenSecret());
        twitter.setOAuthAccessToken(token);

        //im not sure what authentication is nessery
        twitterStream = new TwitterStreamFactory().getInstance();
        twitterStream.setOAuthConsumer(account.getConsumerKey(), account.getConsumerSecret());
        twitterStream.setOAuthAccessToken(new AccessToken(account.getToken(), account.getTokenSecret()));
        twitterStream.addListener(this);
        FilterQuery query = new FilterQuery(account.getQuery().split(","));
        twitterStream.filter(query);

        while(accountRepository.findOne(account.getId()).isRunning()){//TODO we may want to do this less regular
            if (queue.size() > 0){
                Status last = queue.getLast();
                queue.removeLast();

                logger.error("Interacting with tweet with ID " + last.getId());

                try {
                    twitterActionRepository.save(TwitterBotUtils.interactWithTweet(twitter, last, account, "Stream:" + account.getQuery()));
                } catch (TwitterException e) {
                    TwitterBotUtils.handleTwitterError(e, account, accountRepository);
                }

            }
        }

    }

    //all the methods from the stream lisener
    @Override
    public void onStatus(Status status) {
        logger.info("got status");
        queue.add(status);
    }

    @Override
    public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {
        //we dont want to do anything here as we are only intrested in statues
    }

    @Override
    public void onTrackLimitationNotice(int numberOfLimitedStatuses) {
        logger.error("Track Limit Notice = " + numberOfLimitedStatuses);
    }

    @Override
    public void onScrubGeo(long userId, long upToStatusId) {
        //we dont want to do anything here as we are only intrested in statues
    }

    @Override
    public void onStallWarning(StallWarning warning) {
        logger.error("Stall Warnings", warning);
    }

    @Override
    public void onException(Exception ex) {
        logger.error("Twiter Stream exception", ex);
    }
}
