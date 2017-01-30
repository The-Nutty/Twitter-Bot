package com.tomhazell.twitter.console.tweets;

import com.tomhazell.twitter.console.users.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TwitterActionRepository extends JpaRepository<TwitterAction, Long> {

    TwitterAction findTopByAccountOrderByTweetIdDesc(Account account);

    List<TwitterAction> findAllByAccountAndTweetId(Account account, Long tweetId);
}
