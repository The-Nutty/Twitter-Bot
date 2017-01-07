package com.tomhazell.twitter.console.tweets;

import com.tomhazell.twitter.console.TwitterAction;
import com.tomhazell.twitter.console.TwitterActionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

/**
 * Created by Tom Hazell on 06/01/2017.
 */
@Controller
public class TweetsController {

    public static final String ENDPOINT_TWEETS = "/tweets";

    public static final String VIEW_TWEETS = "allTweets";

    public static final String MODEL_ATTR_TWEETS_LIST = "tweets";
    @Autowired
    TwitterActionRepository twitterActionRepository;

    @RequestMapping(ENDPOINT_TWEETS)
    public ModelAndView getTweets(Model model){
        List<TwitterAction> all = twitterActionRepository.findAll();
        model.addAttribute(MODEL_ATTR_TWEETS_LIST, all);
        return new ModelAndView(VIEW_TWEETS);
    }
}
