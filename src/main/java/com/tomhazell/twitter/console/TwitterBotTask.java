package com.tomhazell.twitter.console;

import com.tomhazell.twitter.console.tweets.TwitterAction;
import com.tomhazell.twitter.console.tweets.TwitterActionRepository;
import com.tomhazell.twitter.console.users.Account;
import com.tomhazell.twitter.console.users.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import twitter4j.*;
import twitter4j.auth.AccessToken;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.tomhazell.twitter.console.TwitterBotApplication.RATE_LIMIT_COOLDOWN;

/**
 * This is the call where all the actual botting happens.
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
        for (String queryString : account.getQuery().split(",")) {
            logger.error(account.getName() + " searching for " + queryString);
            //if we have been told to stop then stop
            if (!checkIsRunning()) {
                break;
            }

            Query query = new Query();
            query.setQuery(queryString + " min_retweets:10 -filter:retweets");
            query.setResultType(account.getResultType());
            query.setCount(100);

            //if the sinceId != 0 (meaning we have used this account previously) then only get tweets from the last
            if (sinceId != 0) {
                query.setSinceId(sinceId);
            } else {
                //if this is the first time we are running a query then just get tweets from the last 4 days
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(new Date());
                calendar.add(Calendar.DAY_OF_YEAR, -4);
                query.setSince(new SimpleDateFormat("yyyy-MM-dd").format(calendar.getTime()));
            }

            try {
                QueryResult search = twitter.search(query);
                logger.error("Got " + search.getTweets().size() + " Search results");
                filterAndAddTweets(search.getTweets());
            } catch (TwitterException e) {
                handleTwitterError(e);
            }

            //sleep to evade rate limit
            sleep(TwitterBotApplication.SEARCH_TIME_OUT);

            enter(queryString);//enter the competition's
        }
    }

    /**
     * This is used to filter tweets we dont what to enter, currently we are just checking if the account looks like a bot finder and in that case exluding the tweet and blocking the user
     * @param tweets a lsit of tweets returned from search
     */
    private void filterAndAddTweets(List<Status> tweets) {
        for (Status tweet : tweets) {
            boolean contains = false;
            for (String partOfName : tweet.getUser().getName().split(" ")) {
                if (partOfName.toLowerCase().equals("bot") || partOfName.toLowerCase().equals("botfinder")){
                    contains = true;
                }

                if (contains) {
                    try {
                        logger.error("User name'" + tweet.getUser().getName() + "' looks like a bot finder so blocking them");
                        twitter.createBlock(tweet.getUser().getId());//TODO i cant find if there is or is not a rate limit on this call. I assume there is and so we should sleep after this call
                    } catch (TwitterException e) {
                        handleTwitterError(e);
                    }
                }else if(twitterActionRepository.findOneByAccountAndTweetId(account, tweet.getId()) != null){
                    // in this case we have already delt with the tweet so ignore it
                    logger.error("We have already actioned on this tweet so ignoring it.");
                }else{
                    tweetsToEnter.add(tweet);
                }
            }
        }

    }

    /**
     * This handles the errors that Twitter4j throws
     * @param e TwitterException
     */
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
        } else {
            //wait anyway as to not exeded rate limits
            logger.error("An error occurred", e);
            sleep(TwitterBotApplication.RETWEET_TIME_OUT);
        }
    }

    /**
     * This will enter alll the tweets in tweetsToEnter
     * @param query The query that was used to produce the list, so that we can store it in the TwitterAction DB
     */
    private void enter(String query) {
        for (Status tweet : tweetsToEnter) {
            logger.error("Interacting with tweet with ID " + tweet.getId());
            //if we have been told to stop then stop
            if (!checkIsRunning()) {
                break;
            }

            TwitterAction action = new TwitterAction();
            action.setTweetId(tweet.getId());
            action.setTweetContents(tweet.getText());
            action.setAccount(account);
            action.setUserNameOfTweeter(tweet.getUser().getName());
            action.setQuery(query);
            try {
                //check if we need to Rt
                if (tweet.getText().toLowerCase().contains("rt") || tweet.getText().toLowerCase().contains("retweet")) {
                    retweet(tweet, action);
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
                    follow(tweet, action);
                }

                //common abbreviations for retweet and follow
                if (tweet.getText().toLowerCase().contains("RT+F") || tweet.getText().toLowerCase().contains("RT&F")) {
                    if (!action.isHasRetweeted()) {//check that we have not already retweted
                        retweet(tweet, action);
                    }
                    if (!action.isHasFollowed()) {//check that we have not already followed
                        follow(tweet, action);
                    }
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
     * This will follow the user that sent the tweet and anybody else tagged in it
     * @param tweet The tweet from twitter4j
     * @param action the TwitterAction DB entery
     */
    private void follow(Status tweet, TwitterAction action) throws TwitterException {
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
            sleep(TwitterBotApplication.FOLLOW_TIME_OUT);
        }

        action.setHasFollowed(true);
    }

    private void retweet(Status tweet, TwitterAction action) throws TwitterException {
        twitter.retweetStatus(tweet.getId());
        action.setHasRetweeted(true);

        sleep(TwitterBotApplication.RETWEET_TIME_OUT);
    }

    /**
     * Checks if the bot should still be running by checking the running field in Account
     *
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
     *
     * @param time the time you want to use
     * @return the time when multiplied by this factor
     */
    private int randomiseTime(int time) {
        return (int) (time * (0.8d + ThreadLocalRandom.current().nextDouble(0.4)));

    }
}