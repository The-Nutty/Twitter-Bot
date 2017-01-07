package com.tomhazell.twitter.console;

import javax.persistence.*;

/**
 * Created by Tom Hazell on 06/01/2017.
 */
@Entity
public class TwitterAction {
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Long id;

    private Long tweetId;
    private String userNameOfTweeter;

    @ManyToOne
    private Account account;

    private boolean hasReplyed;
    private boolean hasLiked;
    private boolean hasRetweeted;
    private boolean hasFollowed;

    private String tweetContents;

    public String getUserNameOfTweeter() {
        return userNameOfTweeter;
    }

    public void setUserNameOfTweeter(String userNameOfTweeter) {
        this.userNameOfTweeter = userNameOfTweeter;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTweetId() {
        return tweetId;
    }

    public void setTweetId(Long tweetId) {
        this.tweetId = tweetId;
    }

    public boolean isHasFollowed() {
        return hasFollowed;
    }

    public void setHasFollowed(boolean hasFollowed) {
        this.hasFollowed = hasFollowed;
    }

    public boolean isHasReplyed() {
        return hasReplyed;
    }

    public void setHasReplyed(boolean hasReplyed) {
        this.hasReplyed = hasReplyed;
    }

    public boolean isHasLiked() {
        return hasLiked;
    }

    public void setHasLiked(boolean hasLiked) {
        this.hasLiked = hasLiked;
    }

    public boolean isHasRetweeted() {
        return hasRetweeted;
    }

    public void setHasRetweeted(boolean hasRetweeted) {
        this.hasRetweeted = hasRetweeted;
    }

    public String getTweetContents() {
        return tweetContents;
    }

    public void setTweetContents(String tweetContents) {
        this.tweetContents = tweetContents;
    }

}
