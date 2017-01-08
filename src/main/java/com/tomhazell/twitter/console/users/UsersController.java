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
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import java.util.List;

/**
 * Created by Tom Hazell on 06/01/2017.
 */
@Controller
public class UsersController {

    public static final String ENDPOINT_USERS = "/users";
    public static final String ENDPOINT_INDEX = "/";
    public static final String ENDPOINT_TOGGLE_BOT = "/users/run/{id}";

    public static final String VIEW_ACCOUNTS = "allAccounts";

    public static final String MODEL_ATTR_USERS_LIST = "users";
    public static final String VIEW_ADD_USER = "addUser";
    @Autowired
    AccountRepository accountRepository;

    @Autowired
    TwitterActionRepository twitterActionRepository;

    @RequestMapping(ENDPOINT_USERS)
    public ModelAndView getAllUsers(Model model){
        List<Account> all = accountRepository.findAll();

        model.addAttribute(MODEL_ATTR_USERS_LIST, all);
        return new ModelAndView(VIEW_ACCOUNTS);
    }

    @RequestMapping(ENDPOINT_TOGGLE_BOT)
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
        return new RedirectView(ENDPOINT_USERS);
    }

    @RequestMapping("/users/create")
    public ModelAndView createUser(Account account, Model model){
        model.addAttribute("isEdit", false);
        return new ModelAndView(VIEW_ADD_USER);
    }

    /**
     * This endpoint is used both for saving an updated user or a new user.
     */
    @RequestMapping(value = "/users/create", method = RequestMethod.POST)
    public Object postCreateUser(Model model, Account account, BindingResult bindingResult){
        if (bindingResult.hasErrors()){
            model.addAttribute("error", bindingResult.getAllErrors().get(0).toString());
            return new ModelAndView(VIEW_ADD_USER);//TODO show error
        }

        //save the user
        accountRepository.save(account);

        //redirect to the users page
        return new RedirectView(ENDPOINT_USERS);
    }

    @RequestMapping("/users/update/{id}")
    public ModelAndView updateUser(@PathVariable("id") Long userId, Account account, Model model){
        account = accountRepository.findOne(userId);
        model.addAttribute("account", account);
        model.addAttribute("isEdit", true);
        return new ModelAndView(VIEW_ADD_USER);
    }

    /**
     * temp call to redirect to the users page when a user gose to the index
     */
    @RequestMapping(ENDPOINT_INDEX)
    public RedirectView homePage(){
        return new RedirectView(ENDPOINT_USERS);
    }
}