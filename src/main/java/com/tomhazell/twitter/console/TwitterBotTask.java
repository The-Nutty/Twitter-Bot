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

    public static final String MIN_RETWEETS = "10";
    public static final String FILTERS = " min_retweets:" + MIN_RETWEETS + " -filter:retweets";
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
            query.setQuery(queryString + FILTERS);
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
                TwitterBotUtils.handleTwitterError(e, account, accountRepository);
            }

            //sleep to evade rate limit
            TwitterBotUtils.sleep(TwitterBotApplication.SEARCH_TIME_OUT);

            enter(queryString);//enter the competition's
        }
    }

    /**
     * This is used to filter tweets we dont what to enter, currently we are just checking if the account looks like a bot finder and in that case exluding the tweet and blocking the user
     *
     * @param tweets a lsit of tweets returned from search
     */
    private void filterAndAddTweets(List<Status> tweets) {
        for (Status tweet : tweets) {
            boolean contains = false;
            for (String partOfName : tweet.getUser().getName().split(" ")) {
                if (partOfName.toLowerCase().equals("bot") || partOfName.toLowerCase().equals("botfinder")) {
                    contains = true;
                }
            }

            if (contains) {
                try {
                    logger.error("User name'" + tweet.getUser().getName() + "' looks like a bot finder so blocking them");
                    twitter.createBlock(tweet.getUser().getId());//TODO i cant find if there is or is not a rate limit on this call. I assume there is and so we should sleep after this call
                } catch (TwitterException e) {
                    TwitterBotUtils.handleTwitterError(e, account, accountRepository);
                }
            } else if (twitterActionRepository.findOneByAccountAndTweetId(account, tweet.getId()) != null) {
                // in this case we have already delt with the tweet so ignore it
                logger.error("We have already actioned on this tweet so ignoring it.");
            } else {

                if (tweet.getRetweetedStatus() != null) {
                    tweetsToEnter.add(tweet.getRetweetedStatus());
                } else {
                    tweetsToEnter.add(tweet);
                }
            }

        }

    }

    /**
     * This will enter alll the tweets in tweetsToEnter
     *
     * @param query The query that was used to produce the list, so that we can store it in the TwitterAction DB
     */
    private void enter(String query) {
        for (Status tweet : tweetsToEnter) {
            //if we have been told to stop then stop
            if (!checkIsRunning()) {
                break;
            }

            logger.error("Interacting with tweet with ID " + tweet.getId());

            try {
                TwitterBotUtils.interactWithTweet(twitter, tweet, account, query);
            } catch (TwitterException e) {
                TwitterBotUtils.handleTwitterError(e, account, accountRepository);
            }
        }

        //clear all of the old tweets
        tweetsToEnter.clear();
    }

    /**
     * Checks if the bot should still be running by checking the running field in Account
     *
     * @return true if we should continue to run false otherwise
     */
    private boolean checkIsRunning() {
        account = accountRepository.findOne(account.getId());//make sure we have the most up to date version
        return account.isRunningTraditional();
    }
}