package com.tomhazell.twitter.console;

import com.tomhazell.twitter.console.tweets.TwitterActionRepository;
import com.tomhazell.twitter.console.users.Account;
import com.tomhazell.twitter.console.users.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import twitter4j.*;
import twitter4j.auth.AccessToken;

import java.util.LinkedList;

/**
 * This is a reimplementation of the TwitterBot {@link TwitterBotTask} but this is using the streams API, meaning we get much better results.
 * It stores in tweets in a queue, tweets will only be added to it if the queue is under 40 in size and meets some criteria.
 * Becuase we get LOTS of tweets in we can afford to filter them heavily
 */
public class TwitterBotStreamTask implements Runnable, StatusListener {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private TwitterActionRepository twitterActionRepository;
    private AccountRepository accountRepository;

    private Account account;
    private TwitterStream twitterStream;
    private Twitter twitter;

    private LinkedList<Status> queue = new LinkedList<>();
    private boolean isQueueFull = false;//we try to keep the queue size between 20 and 40 in size so we unregester the lisener to not spam the logs
    private int followingCount;
    private User twitterUser;


    public TwitterBotStreamTask(TwitterActionRepository repository, AccountRepository accountRepository, Account account) {
        this.account = account;
        this.twitterActionRepository = repository;
        this.accountRepository = accountRepository;
    }


    @Override
    public void run() {
        try {
            twitter = new TwitterFactory().getInstance();
            twitter.setOAuthConsumer(account.getConsumerKey(), account.getConsumerSecret());

            AccessToken token = new AccessToken(account.getToken(), account.getTokenSecret());
            twitter.setOAuthAccessToken(token);

            //im not sure what authentication is nessery
            twitterStream = new TwitterStreamFactory().getInstance();
            twitterStream.setOAuthConsumer(account.getConsumerKey(), account.getConsumerSecret());
            twitterStream.setOAuthAccessToken(new AccessToken(account.getToken(), account.getTokenSecret()));
            twitterStream.addListener(this);

//        start streams api
            FilterQuery query = new FilterQuery(account.getQuery().split(","));
            twitterStream.filter(query);

            //iterate through the queue while we should be running. and Action on each tweet
            while ((account = accountRepository.findOne(account.getId())).isRunningStream()) {//TODO we may want to do this less regularly?
                if (queue.size() > 0) {
                    Status last = queue.getLast();
                    queue.removeLast();

                    if (queue.size() < 20 && isQueueFull) {
                        logger.info("queue size is smaller than 20 so re adding listener");
                        twitterStream.addListener(this);
                        isQueueFull = false;
                    }

                    logger.info("Interacting with tweet with ID " + last.getId());

                    try {
                        twitterActionRepository.save(TwitterBotUtils.interactWithTweet(twitter, last, account, "Stream:" + account.getQuery()));
                    } catch (TwitterException e) {
                        TwitterBotUtils.handleTwitterError(e, account, accountRepository);
                    }
                }

            }

            //clean up stream
            twitterStream.clearListeners();
            twitterStream.cleanUp();
        } catch (Exception e) {
            logger.error("Twiter Stream loop exception", e);
            //update DB to show stream has died
            account = accountRepository.findOne(account.getId());
            account.setRunningStream(false);
            accountRepository.save(account);
            throw e;//throw exception anyway
        }
    }

    //all the methods from the stream lisener

    /**
     * Here we are going to filter all tweets we get and add them to the queue
     *
     * @param status the tweet in from the stream
     */
    @Override
    public void onStatus(Status status) {

        //get original tweet if its a retweet
        if (status.getRetweetedStatus() != null) {
            status = status.getRetweetedStatus();
        }
        //get if a tweet is quoted the the tweet that is quoted
        if (status.getQuotedStatus() != null) {
            status = status.getQuotedStatus();
        }
        //check if name of account looks like a bot finder
        boolean contains = false;
        for (String partOfName : status.getUser().getName().split(" ")) {
            if (partOfName.toLowerCase().equals("bot") || partOfName.toLowerCase().equals("botfinder")) {
                contains = true;
            }
        }

        //if they look like bot finder then block them
        if (contains) {
            try {
                logger.info("User name'" + status.getUser().getName() + "' looks like a bot finder so blocking them");
                twitter.createBlock(status.getUser().getId());//TODO i cant find if there is or is not a rate limit on this call. I assume there is and so we should sleep after this call
            } catch (TwitterException e) {
                TwitterBotUtils.handleTwitterError(e, account, accountRepository);
            }
            return;
        }

        //since we are now getting loads of tweets we should make sure that the queue dose not get to long and be more picky/filter TODO
        for (String filter : account.getStreamFilters().split(",")) {
            if (status.getText().toLowerCase().contains(filter.toLowerCase())) {
                logger.info(account.getName() + ": Not adding tweet as it contains filtered word: " + filter);
                return;
            }
        }


        //i think there is a beater solution to this but implement a max queue length for the mean time as we get tweets way more than we can action on them
        if (queue.size() > 40) {
            logger.info(account.getName() + ": Not adding tweet to queue as we already have 40 things in it, removing lisener");//try un regesteing the lisener
            twitterStream.removeListener(this);
            isQueueFull = true;
            return;
        }

        //check if we have actioned on this tweet already
        if (twitterActionRepository.findAllByAccountAndTweetId(account, status.getId()).size() == 0) {
            logger.info(account.getName() + ": got status");
            queue.add(status);
        } else {
            logger.info(account.getName() + ": got status we have already used");
        }
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

        //update DB to show stream has died
        account = accountRepository.findOne(account.getId());
        account.setRunningStream(false);
        accountRepository.save(account);

    }
}
