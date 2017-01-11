package com.tomhazell.twitter.console.tweets;

import com.tomhazell.twitter.console.users.Account;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Created by Tom Hazell on 06/01/2017.
 */
public interface TwitterActionRepository extends JpaRepository<TwitterAction, Long> {

    TwitterAction findTopByAccountOrderByTweetIdDesc(Account account);

    TwitterAction findOneByAccountAndTweetId(Account account, Long tweetId);
}
