package com.tomhazell.twitter.console.tweets;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 *This is the controller for all of the Tweeting related endpoints
 */
@Controller
public class TweetsController {

    public static final String ENDPOINT_TWEETS = "/tweets";
    public static final String ENDPOINT_TWEETS_CSV = "/tweets.json";

    public static final String VIEW_TWEETS = "allTweets";

    public static final String MODEL_ATTR_TWEETS_LIST = "tweets";

    @Autowired
    private TwitterActionRepository twitterActionRepository;

    @Autowired
    private ObjectMapper mapper;

    @RequestMapping(ENDPOINT_TWEETS)
    public ModelAndView getTweets(Model model) {
        List<TwitterAction> all = twitterActionRepository.findAll();
        model.addAttribute(MODEL_ATTR_TWEETS_LIST, all);
        return new ModelAndView(VIEW_TWEETS);
    }

    @RequestMapping(ENDPOINT_TWEETS_CSV)
    public String csvTweets(Writer writer) throws IOException {
        List<TwitterAction> all = twitterActionRepository.findAll();

        writer.write(mapper.writeValueAsString(all));

        return null;
    }
}
