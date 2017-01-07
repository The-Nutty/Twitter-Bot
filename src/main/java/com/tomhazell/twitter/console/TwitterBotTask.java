package com.tomhazell.twitter.console;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import twitter4j.*;
import twitter4j.auth.AccessToken;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static com.tomhazell.twitter.console.TwitterBotApplication.RATE_LIMIT_COOLDOWN;

/**
 * Created by Tom Hazell on 06/01/2017.
 */
public class TwitterBotTask implements Runnable {

    private Logger logger;

    private TwitterActionRepository twitterActionRepository;
    private AccountRepository accountRepository;

    private Account account;
    private Twitter twitter;
    private long sinceId = 0;

    private List<Status> tweetsToEnter = new ArrayList<>();

    public TwitterBotTask(TwitterActionRepository repository, AccountRepository accountRepository, Account account) {
        this.account = account;
        this.twitterActionRepository = repository;
        this.accountRepository = accountRepository;
        logger = LoggerFactory.getLogger(getClass());
    }

    @Override
    public void run() {
        logger.error("starting to run bot for " + account.getName());
        //initiate twitter instance and login
        twitter = new TwitterFactory().getInstance();
        twitter.setOAuthConsumer(account.getConsumerKey(), account.getConsumerSecret());

        AccessToken token = new AccessToken(account.getToken(), account.getTokenSecret());
        twitter.setOAuthAccessToken(token);

        while (checkIsRunning()) {//check if we should continue
            //get the last tweet seen by this account
            updateSinceId();

            search();
        }
    }

    private void updateSinceId() {
        TwitterAction lastsAction = twitterActionRepository.findTopByAccountOrderByTweetIdDesc(account);
        if (lastsAction != null) {
            sinceId = lastsAction.getTweetId();
        }
    }

    /**
     * When called this will preform multiple searches one for each of hte users query's, it will filter them if nessery
     * then enter them
     */
    private void search() {
        for (String queryString : account.getQuerys().split(",")) {
            logger.error(account.getName() + " searching for " + queryString);
            //if we have been told to stop then stop
            if (!checkIsRunning()){
                break;
            }

            Query query = new Query();
            query.setQuery(queryString + " min_retweets:5 -filter:retweets");//   -vote -filter:retweets
            query.setResultType(account.getResultType());
            query.setCount(99);

            //if the sinceId != 0 (meaning we have used this account previously) then only get tweets from the last
            if (sinceId != 0) {
                query.setSinceId(sinceId);
            }else{
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(new Date());
                calendar.add(Calendar.DAY_OF_YEAR, -4);
//                query.setSince(new SimpleDateFormat("yyyy-MM-dd").format(calendar.getTime()));
            }

            try {
                QueryResult search = twitter.search(query);
                logger.error("Got " + search.getTweets().size() + " Search results");
                tweetsToEnter.addAll(search.getTweets());
            } catch (TwitterException e) {
                handleTwitterError(e);
            }

            //sleep to evade rate limit
            sleep(TwitterBotApplication.SEARCH_TIME_OUT);

            enter();//enter the competition's
        }
    }

    private void handleTwitterError(TwitterException e) {
        //if we are rate limited sleep for 10 mins
        if (e.exceededRateLimitation()) {
            logger.error("Failed to search for tweets or retweet, sleeping for 10 mins...", e);
            //update the account to show it is being rate limited so the user knows
            account = accountRepository.findOne(account.getId());//make sure we have the most up to date version
            account.setOnRatelimitCooldown(true);
            accountRepository.save(account);

            sleep(RATE_LIMIT_COOLDOWN);

            //update the account to show it is no longer being rate limited so the user knows
            account = accountRepository.findOne(account.getId());//make sure we have the most up to date version
            account.setOnRatelimitCooldown(false);
            accountRepository.save(account);
        }else{
            //wait anyway as to not exeded rate limits
            logger.error("An error occurred", e);
            sleep(TwitterBotApplication.RETWEET_TIME_OUT);
        }
    }

    private void enter() {
        for (Status tweet : tweetsToEnter) {
            logger.error("Interacting with tweet with ID " + tweet.getId());
            //if we have been told to stop then stop
            if (!checkIsRunning()){
                break;
            }

            TwitterAction action = new TwitterAction();
            action.setTweetId(tweet.getId());
            action.setTweetContents(tweet.getText());
            action.setAccount(account);
            action.setUserNameOfTweeter(tweet.getUser().getName());
            try {
                //check if we need to Rt
                if (tweet.getText().toLowerCase().contains("rt") || tweet.getText().toLowerCase().contains("retweet")) {
                    twitter.retweetStatus(tweet.getId());
                    action.setHasRetweeted(true);

                    sleep(TwitterBotApplication.RETWEET_TIME_OUT);
                }

                //check if we need to like
                if (tweet.getText().toLowerCase().contains("like") || tweet.getText().toLowerCase().contains("fav") || tweet.getText().toLowerCase().contains("favorite")) {
                    twitter.createFavorite(tweet.getId());
                    action.setHasLiked(true);

                    sleep(TwitterBotApplication.LIKE_TIME_OUT);
                }

                //check if we need to reply
                if (tweet.getText().toLowerCase().contains("reply")) {
                    StatusUpdate reply = new StatusUpdate(tweet.getInReplyToScreenName() + " ");//TODO we should have some real body to the tweet  We may also want to set GeoLocation to make it seem more ligit
                    twitter.updateStatus(reply);
                    action.setHasRetweeted(true);

                    sleep(TwitterBotApplication.REPLY_TIME_OUT);
                }

                if (tweet.getText().toLowerCase().contains("follow") || tweet.getText().toLowerCase().contains("following")){
                    twitter.friendsFollowers().createFriendship(tweet.getUser().getId());
                    action.setHasFollowed(true);

                    sleep(TwitterBotApplication.FOLLOW_TIME_OUT);
                }

                twitterActionRepository.save(action);

            } catch (TwitterException e) {
                handleTwitterError(e);
            }
        }

        //clear all of the old tweets
        tweetsToEnter.clear();
    }

    /**
     * Checks if the bot should still be running by checking the running field in Account
     * @return true if we should continue to run false otherwise
     */
    private boolean checkIsRunning() {
        account = accountRepository.findOne(account.getId());//make sure we have the most up to date version
        return account.isRunning();
    }

    private void sleep(int millis) {
        try {
            Thread.sleep(randomiseTime(millis));
        } catch (InterruptedException e) {
            logger.error("Failed to sleep", e);
        }
    }

    /**
     * This multaplys the input time by 0.8 to 1.2 randomly genarated in order to not seem like a bot
     * @param time the time you want to use
     * @return the time when multiplied by this factor
     */
    private int randomiseTime(int time){
        return (int) (time * (0.8d + ThreadLocalRandom.current().nextDouble(0.4)));

    }
}