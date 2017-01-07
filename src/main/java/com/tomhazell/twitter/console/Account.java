package com.tomhazell.twitter.console;

import twitter4j.Query;

import javax.persistence.*;
import java.util.List;

/**
 * Class used to persist all the accounds/bots we have, stored in a DB using {@link AccountRepository}
 */
@Entity
public class Account {
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Long id;

    private String name;

    private String querys;

    @Enumerated(EnumType.STRING)
    private Query.ResultType resultType;

    private String token;
    private String tokenSecret;

    private String consumerKey;
    private String consumerSecret;

    private boolean running;
    private boolean onRatelimitCooldown;

    @OneToMany(mappedBy = "account")
    private List<TwitterAction> actions;


    public Long getId() {
        return id;
    }

    public boolean isOnRatelimitCooldown() {
        return onRatelimitCooldown;
    }

    public void setOnRatelimitCooldown(boolean onRatelimitCooldown) {
        this.onRatelimitCooldown = onRatelimitCooldown;
    }

    public Query.ResultType getResultType() {
        return resultType;
    }

    public void setResultType(Query.ResultType resultType) {
        this.resultType = resultType;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getQuerys() {
        return querys;
    }

    public void setQuerys(String querys) {
        this.querys = querys;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenSecret() {
        return tokenSecret;
    }

    public void setTokenSecret(String tokenSecret) {
        this.tokenSecret = tokenSecret;
    }

    public String getConsumerKey() {
        return consumerKey;
    }

    public void setConsumerKey(String consumerKey) {
        this.consumerKey = consumerKey;
    }

    public String getConsumerSecret() {
        return consumerSecret;
    }

    public void setConsumerSecret(String consumerSecret) {
        this.consumerSecret = consumerSecret;
    }
}
