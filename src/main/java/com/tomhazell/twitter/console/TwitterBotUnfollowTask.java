package com.tomhazell.twitter.console;

import com.tomhazell.twitter.console.tweets.TwitterActionRepository;
import com.tomhazell.twitter.console.users.Account;
import com.tomhazell.twitter.console.users.AccountRepository;
import org.slf4j.*;
import org.slf4j.LoggerFactory;
import twitter4j.*;
import twitter4j.auth.AccessToken;

import java.util.ArrayList;
import java.util.List;

import static com.tomhazell.twitter.console.TwitterBotApplication.FRIEND_LIST_TIME_OUT;
import static com.tomhazell.twitter.console.TwitterBotApplication.UNFOLLOW_TIME_OUT;

/**
 * Created by Tom Hazell on 17/01/2017.
 */
public class TwitterBotUnfollowTask implements Runnable {

    public static final int FOLLOWING_TIPPING_POINT = 4900;//the followers over which we will remove the
    public static final int FOLLOWERS_TO_REMOVE = 100;//number of followers to remove after we hit the tipping point

    private TwitterActionRepository twitterActionRepository;
    private AccountRepository accountRepository;

    private Account account;
    private Twitter twitter;

    private org.slf4j.Logger logger = LoggerFactory.getLogger(getClass());


    public TwitterBotUnfollowTask(TwitterActionRepository repository, AccountRepository accountRepository, Account account) {
        this.account = account;
        this.twitterActionRepository = repository;
        this.accountRepository = accountRepository;
    }


    @Override
    public void run() {
        twitter = new TwitterFactory().getInstance();
        twitter.setOAuthConsumer(account.getConsumerKey(), account.getConsumerSecret());

        AccessToken token = new AccessToken(account.getToken(), account.getTokenSecret());
        twitter.setOAuthAccessToken(token);

        //get the user
        User user = null;
        try {
            user = twitter.verifyCredentials();//we are not sleeping as we only call this endpoint once
        } catch (TwitterException e) {
            logger.error("Cant login to account" + account.getName(), e);
            return;
        }

        List<User> friends = new ArrayList<>();

        //first we get a list of all of the friends
        long cursor = -1;
        int i = 0;
        while (i < user.getFriendsCount() - 1) {
            try {
                PagableResponseList<User> friendsList = twitter.getFriendsList(user.getId(), cursor, 200);
                cursor = friendsList.getNextCursor();
                friends.addAll(friendsList);
                i = i + 200;
                TwitterBotUtils.sleep(FRIEND_LIST_TIME_OUT);
            } catch (TwitterException e) {
                TwitterBotUtils.handleTwitterError(e, account, accountRepository);
            }
        }

        //un follow all friends
        for (User friend : friends) {
            try {
                twitter.destroyFriendship(friend.getId());
                TwitterBotUtils.sleep(UNFOLLOW_TIME_OUT);
            } catch (TwitterException e) {
                TwitterBotUtils.handleTwitterError(e, account, accountRepository);
            }
        }
    }
}
