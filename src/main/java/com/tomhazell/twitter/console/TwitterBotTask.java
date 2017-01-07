package com.tomhazell.twitter.console;

import org.slf4j.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import twitter4j.*;
import twitter4j.auth.AccessToken;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

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
     * When called this will proform multaple searches one for each of hte users querys, it will filter them ifd nessery
     * then enter them
     * <p>
     * Currently we just ignore the search going on when we get rate limited should we??
     */
    private void search() {
        for (String queryString : account.getQuerys().split(",")) {
            logger.error(account.getName() + " searching for " + queryString);
            //if we have been told to stop then stop
            if (!checkIsRunning()){
                break;
            }

            Query query = new Query();
            query.setQuery(queryString + " min_retweets:20 -filter:retweets -vote");
            query.setResultType(account.getResultType());
            query.setCount(100);

            //if the sinceId != 0 (meaning we have used this account previously) then only get tweets from the last
            if (sinceId != 0) {
                query.setSinceId(sinceId);
            }else{
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(new Date());
                calendar.add(Calendar.DAY_OF_YEAR, -4);
                query.setSince(new SimpleDateFormat("yyyy-MM-dd").format(calendar.getTime()));

            }

            try {
                QueryResult search = twitter.search(query);
                tweetsToEnter.addAll(search.getTweets());
            } catch (TwitterException e) {
                handleError(e);
            }

            enter();//enter the compotitions

            //sleep to evade rate limit
            try {
                Thread.sleep(randomiseTime(TwitterBotApplication.SEARCH_TIME_OUT));
            } catch (InterruptedException e) {
                logger.error("Failed to sleep", e);
            }
        }
    }

    private void handleError(TwitterException e) {
        logger.error("Failed to search for tweets or retweet, sleeping for 10 mins...", e);

        //if we are rate limited sleep for 10 mins
        if (e.exceededRateLimitation()) {
            //update the account to show it is being rate limited so the user knows
            account = accountRepository.findOne(account.getId());//make sure we have the most up to date version
            account.setOnRatelimitCooldown(true);
            accountRepository.save(account);

            try {
                Thread.sleep(1000 * 60 * 10);//10 mins
            } catch (InterruptedException e1) {
                logger.error("Failed to sleep", e);
            }

            //update the account to show it is no longer being rate limited so the user knows
            account = accountRepository.findOne(account.getId());//make sure we have the most up to date version
            account.setOnRatelimitCooldown(false);
            accountRepository.save(account);
        }
    }

//    private void filterAndAddTweets(List<Status> tweets) {
//        //TODO add things
//        //Filter out tweets containg phrases
//        //min retweets
//        //
//        for (Status tweet : tweets) {
//            if (tweet.getRetweetCount() >= 20) {
//                tweetsToEnter.add(tweet);
////                tweet.getUser().getStatusCount
//            }else{
//                logger.error("Not Adding tweet (" + tweet.getRetweetCount() + "): " + tweet.getText());
//            }
//        }
//    }


    private void enter() {
        logger.error("Entered");
        List<Status> tweetsToRetry = new ArrayList<>();
        for (Status tweet : tweetsToEnter) {
            logger.error("doing tweet stuff");
            //if we have been told to stop then stop
            if (!checkIsRunning()){
                break;
            }

            TwitterAction action = new TwitterAction();
            action.setTweetId(tweet.getId());
            action.setTweetContents(tweet.getText());
            action.setAccount(account);
            action.setUserNameOfTweeter(tweet.getInReplyToScreenName());
            try {
                //check if we need to Rt
                if (tweet.getText().toLowerCase().contains("rt") || tweet.getText().toLowerCase().contains("retweet")) {
                    twitter.retweetStatus(tweet.getId());
                    action.setHasRetweeted(true);

                    try {
                        Thread.sleep(randomiseTime(TwitterBotApplication.RETWEET_TIME_OUT));
                    } catch (InterruptedException e) {
                        logger.error("Failed to sleep", e);
                    }
                }

                //check if we need to like
                if (tweet.getText().toLowerCase().contains("like") || tweet.getText().toLowerCase().contains("fav") || tweet.getText().toLowerCase().contains("favorite")) {
                    twitter.createFavorite(tweet.getId());
                    action.setHasLiked(true);

                    try {
                        Thread.sleep(randomiseTime(TwitterBotApplication.LIKE_TIME_OUT));
                    } catch (InterruptedException e) {
                        logger.error("Failed to sleep", e);
                    }
                }

                //check if we need to reply
                if (tweet.getText().toLowerCase().contains("reply")) {
                    StatusUpdate reply = new StatusUpdate(tweet.getInReplyToScreenName() + " ");//TODO we should have some real body to the tweet  We may also want to set GeoLocation to make it seem more ligit
                    twitter.updateStatus(reply);
                    action.setHasRetweeted(true);

                    try {
                        Thread.sleep(randomiseTime(TwitterBotApplication.REPLY_TIME_OUT));
                    } catch (InterruptedException e) {
                        logger.error("Failed to sleep", e);
                    }
                }

                if (tweet.getText().toLowerCase().contains("follow") || tweet.getText().toLowerCase().contains("following")){
                    twitter.friendsFollowers().createFriendship(tweet.getUser().getId());
                    action.setHasFollowed(true);

                    try {
                        Thread.sleep(randomiseTime(TwitterBotApplication.FOLLOW_TIME_OUT));
                    } catch (InterruptedException e) {
                        logger.error("Failed to sleep", e);
                    }
                }

                twitterActionRepository.save(action);

            } catch (TwitterException e) {
//                tweetsToRetry.add(tweet);//we may wantto black list it in this case not sure
                //TODO i dont think we wantto retry not sure
                handleError(e);
            }

        }

        //clear all of the old tweets
        //add the tweets that failed back..
        tweetsToEnter.clear();
        tweetsToEnter.addAll(tweetsToRetry);
    }

    private boolean checkIsRunning() {
        account = accountRepository.findOne(account.getId());//make sure we have the most up to date version
        return account.isRunning();
    }

    private int randomiseTime(int time){
        return (int) (time * (0.8d + ThreadLocalRandom.current().nextDouble(0.4)));

    }
}