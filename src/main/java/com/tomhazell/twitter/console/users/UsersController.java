package com.tomhazell.twitter.console.users;

import com.tomhazell.twitter.console.Account;
import com.tomhazell.twitter.console.AccountRepository;
import com.tomhazell.twitter.console.TwitterActionRepository;
import com.tomhazell.twitter.console.TwitterBotTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import java.util.List;

/**
 * Created by Tom Hazell on 06/01/2017.
 */
@Controller
public class UsersController {

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    TwitterActionRepository twitterActionRepository;

    @RequestMapping("/users")
    public ModelAndView getAllUsers(Model model){
        List<Account> all = accountRepository.findAll();

        model.addAttribute("users", all);
        return new ModelAndView("allAccounts");
    }

    @RequestMapping("/users/run/{id}")
    public RedirectView runUser(@PathVariable("id") Long userId){
        Account account = accountRepository.findOne(userId);
        if (account.isRunning()) {
            account.setRunning(false);
        }else{
            account.setRunning(true);
            TaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
            taskExecutor.execute(new TwitterBotTask(twitterActionRepository, accountRepository, account));
        }
        accountRepository.save(account);
        return new RedirectView("/users");
    }

    /**
     * temp call to redirect to the users page when a user gose to the index
     */
    @RequestMapping("/")
    public RedirectView homePage(){
        return new RedirectView("/users");
    }
}