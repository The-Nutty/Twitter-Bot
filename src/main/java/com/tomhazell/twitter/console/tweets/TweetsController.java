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

    @Autowired
    TwitterActionRepository twitterActionRepository;

    @RequestMapping("/tweets")
    public ModelAndView getTweets(Model model){
        List<TwitterAction> all = twitterActionRepository.findAll();
        model.addAttribute("tweets", all);
        return new ModelAndView("allTweets");
    }
}
