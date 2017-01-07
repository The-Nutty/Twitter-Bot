package com.tomhazell.twitter.console;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Created by Tom Hazell on 06/01/2017.
 */
public interface TwitterActionRepository extends JpaRepository<TwitterAction, Long> {

    TwitterAction findTopByAccountOrderByTweetIdDesc(Account account);
}
