package com.tomhazell.twitter.console.tweets;

import com.tomhazell.twitter.console.users.Account;

import javax.persistence.*;
import java.util.Calendar;

/**
 * Class used to persist all the competitions we have entered, stored in a DB using {@link TwitterActionRepository}
 */
@Entity
public class TwitterAction {
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Long id;

    @Column(name="CREATION_TS", columnDefinition="TIMESTAMP DEFAULT CURRENT_TIMESTAMP", insertable=false, updatable=false)
    private Calendar dateCreated;

    private Long tweetId;
    private String userNameOfTweeter;
    private String query;

    @ManyToOne
    private Account account;

    private boolean hasReplyed;
    private boolean hasLiked;
    private boolean hasRetweeted;
    private int numFollows;

    //7(Max num of chars required to store a char(in unicode format))*141(max length of tweet + 1) = 987
    @Column(length = 987)
    private String tweetContents;

    public Calendar getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Calendar dateCreated) {
        this.dateCreated = dateCreated;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

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

    public int getNumFollows() {
        return numFollows;
    }

    public void setNumFollows(int numFollows) {
        this.numFollows = numFollows;
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
