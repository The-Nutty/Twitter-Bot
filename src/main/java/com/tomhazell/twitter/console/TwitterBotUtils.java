package com.tomhazell.twitter.console;

import com.tomhazell.twitter.console.tweets.TwitterAction;
import com.tomhazell.twitter.console.users.Account;
import com.tomhazell.twitter.console.users.AccountRepository;
import org.slf4j.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import twitter4j.*;


import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.tomhazell.twitter.console.TwitterBotApplication.RATE_LIMIT_COOLDOWN;

/**
 * Created by Tom Hazell on 15/01/2017.
 */
public class TwitterBotUtils {

    private static Logger logger = LoggerFactory.getLogger("TwitterBotUtils");

    public static TwitterAction interactWithTweet(Twitter twitter, Status tweet, Account account, String query) throws TwitterException {
        TwitterAction action = new TwitterAction();
        action.setTweetId(tweet.getId());
        action.setTweetContents(tweet.getText());
        action.setAccount(account);
        action.setUserNameOfTweeter(tweet.getUser().getName());
        action.setQuery(query);
        //check if we need to Rt
        if (tweet.getText().toLowerCase().contains("rt") || tweet.getText().toLowerCase().contains("retweet")) {
            retweet(tweet, action, twitter);
        }

        //check if we need to like
        if (tweet.getText().toLowerCase().contains("like") || tweet.getText().toLowerCase().contains("fav") || tweet.getText().toLowerCase().contains("favorite")) {
            twitter.createFavorite(tweet.getId());
            action.setHasLiked(true);

            sleep(TwitterBotApplication.LIKE_TIME_OUT);
        }

        //check if we need to reply
        if (tweet.getText().toLowerCase().contains("reply") || tweet.getText().toLowerCase().contains("tag ")) {
            StatusUpdate reply = new StatusUpdate("@Nutty007tom @Gooseyboy1234 @hiaitsme");//TODO we should have some real body to the tweet  We may also want to set GeoLocation to make it seem more ligit
            twitter.updateStatus(reply);
            action.setHasRetweeted(true);

            sleep(TwitterBotApplication.REPLY_TIME_OUT);
        }

        //check if we need to follow
        if (tweet.getText().toLowerCase().contains("follow") || tweet.getText().toLowerCase().contains("following")) {
            follow(tweet, action, twitter);
        }

        //common abbreviations for retweet and follow
        if (tweet.getText().toLowerCase().contains("RT+F") || tweet.getText().toLowerCase().contains("RT&F")) {
            if (!action.isHasRetweeted()) {//check that we have not already retweted
                retweet(tweet, action, twitter);
            }
            if (!action.isHasFollowed()) {//check that we have not already followed
                follow(tweet, action, twitter);
            }
        }

        return action;
    }


    public static void sleep(int millis) {
        try {
            Thread.sleep(randomiseTime(millis));
        } catch (InterruptedException e) {
            logger.error("Failed to sleep", e);
        }
    }

    /**
     * This multaplys the input time by 0.8 to 1.2 randomly genarated in order to not seem like a bot
     *
     * @param time the time you want to use
     * @return the time when multiplied by this factor
     */
    private static int randomiseTime(int time) {
        return (int) (time * (0.8d + ThreadLocalRandom.current().nextDouble(0.4)));

    }

    /**
     * This will follow the user that sent the tweet and anybody else tagged in it
     *
     * @param tweet  The tweet from twitter4j
     * @param action the TwitterAction DB entery
     */
    public static void follow(Status tweet, TwitterAction action, Twitter twitter) throws TwitterException {
        Set<String> userToFollow = new HashSet<>();
        if (tweet.getRetweetedStatus() != null) {
            userToFollow.add(tweet.getRetweetedStatus().getUser().getScreenName());
        } else {
            userToFollow.add(tweet.getUser().getScreenName());
        }

        //find all other users in the tweet and follow them to find situations where it says follow me and @thisGuy
        Matcher m = Pattern.compile("@([A-Za-z0-9_]{1,15})").matcher(tweet.getText());
        while (m.find()) {
            userToFollow.add(m.group().substring(1));
        }

        for (String user : userToFollow) {
            twitter.friendsFollowers().createFriendship(user);
            TwitterBotUtils.sleep(TwitterBotApplication.FOLLOW_TIME_OUT);
        }

        action.setHasFollowed(true);
    }

    public static void retweet(Status tweet, TwitterAction action, Twitter twitter) throws TwitterException {
        twitter.retweetStatus(tweet.getId());
        action.setHasRetweeted(true);

        TwitterBotUtils.sleep(TwitterBotApplication.RETWEET_TIME_OUT);
    }

    /**
     * This handles the errors that Twitter4j throws
     * @param e TwitterException
     */
    public static void handleTwitterError(TwitterException e, Account account, AccountRepository accountRepository) {
        //if we are rate limited sleep for 10 mins
        if (e.exceededRateLimitation()) {
            logger.error("Failed to search for tweets or retweet due to rate limits, sleeping for 10 mins...", e);
            //update the account to show it is being rate limited so the user knows
            account = accountRepository.findOne(account.getId());//make sure we have the most up to date version
            account.setOnRatelimitCooldown(true);
            accountRepository.save(account);

            TwitterBotUtils.sleep(RATE_LIMIT_COOLDOWN);

            //update the account to show it is no longer being rate limited so the user knows
            account = accountRepository.findOne(account.getId());//make sure we have the most up to date version
            account.setOnRatelimitCooldown(false);
            accountRepository.save(account);
        } else {
            //wait anyway as to not exeded rate limits
            logger.error("An error occurred", e);
            TwitterBotUtils.sleep(TwitterBotApplication.RETWEET_TIME_OUT);
        }
    }
}
