package com.tomhazell.twitter.console.users;

import com.tomhazell.twitter.console.TwitterBotStreamTask;
import com.tomhazell.twitter.console.TwitterBotTask;
import com.tomhazell.twitter.console.TwitterBotUnfollowTask;
import com.tomhazell.twitter.console.tweets.TwitterActionRepository;
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
 * This is the controller for all of the account related endpoints
 */
@Controller
public class UsersController {

    public static final String ENDPOINT_USERS = "/users";
    public static final String ENDPOINT_INDEX = "/";
    public static final String ENDPOINT_TOGGLE_BOT_TRADITIONAL = "/users/run/{id}/og";
    public static final String ENDPOINT_TOGGLE_BOT_STREAM = "/users/run/{id}/stream";
    public static final String ENDPOINT_RUN_BOT_UNFOLLOW = "/users/run/{id}/unfollow";
    public static final String ENDPOINT_USERS_UPDATE = "/users/update/{id}";
    public static final String ENDPOINT_USERS_CREATE = "/users/create";

    public static final String VIEW_ACCOUNTS = "allAccounts";
    public static final String VIEW_ADD_USER = "addUser";

    public static final String MODEL_ATTR_USERS_LIST = "users";
    public static final String MODEL_ATTR_IS_EDIT = "isEdit";
    public static final String MODEL_ATTR_ACCOUNT = "account";

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TwitterActionRepository twitterActionRepository;

    @RequestMapping(ENDPOINT_USERS)
    public ModelAndView getAllUsers(Model model) {
        List<Account> all = accountRepository.findAll();

        model.addAttribute(MODEL_ATTR_USERS_LIST, all);
        return new ModelAndView(VIEW_ACCOUNTS);
    }

    @RequestMapping(ENDPOINT_TOGGLE_BOT_TRADITIONAL)
    public RedirectView runTraditionalUser(@PathVariable("id") Long userId) {
        Account account = accountRepository.findOne(userId);
        if (account.isRunningTraditional()) {
            account.setRunningTraditional(false);
        } else {
            account.setRunningTraditional(true);
            TaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
            taskExecutor.execute(new TwitterBotTask(twitterActionRepository, accountRepository, account));
        }
        accountRepository.save(account);
        return new RedirectView(ENDPOINT_USERS);
    }

    @RequestMapping(ENDPOINT_TOGGLE_BOT_STREAM)
    public RedirectView runStreamUser(@PathVariable("id") Long userId) {
        Account account = accountRepository.findOne(userId);
        if (account.isRunningStream()) {
            account.setRunningStream(false);
        } else {
            account.setRunningStream(true);
            TaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
            taskExecutor.execute(new TwitterBotStreamTask(twitterActionRepository, accountRepository, account));
        }
        accountRepository.save(account);
        return new RedirectView(ENDPOINT_USERS);
    }

    @RequestMapping(ENDPOINT_RUN_BOT_UNFOLLOW)
    public RedirectView runUnfollowUser(@PathVariable("id") Long userId) {
        Account account = accountRepository.findOne(userId);
        TaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
        taskExecutor.execute(new TwitterBotUnfollowTask(twitterActionRepository, accountRepository, account));
        return new RedirectView(ENDPOINT_USERS);
    }

    @RequestMapping(ENDPOINT_USERS_CREATE)
    public ModelAndView createUser(Account account, Model model) {
        model.addAttribute(MODEL_ATTR_IS_EDIT, false);
        return new ModelAndView(VIEW_ADD_USER);
    }

    /**
     * This endpoint is used both for saving an updated user or a new user.
     */
    @RequestMapping(value = ENDPOINT_USERS_CREATE, method = RequestMethod.POST)
    public Object postCreateUser(Model model, Account account, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("error", bindingResult.getAllErrors().get(0).toString());
            return new ModelAndView(VIEW_ADD_USER);//TODO show error
        }

        //save the user
        accountRepository.save(account);

        //redirect to the users page
        return new RedirectView(ENDPOINT_USERS);
    }

    @RequestMapping(ENDPOINT_USERS_UPDATE)
    public ModelAndView updateUser(@PathVariable("id") Long userId, Account account, Model model) {
        account = accountRepository.findOne(userId);
        model.addAttribute(MODEL_ATTR_ACCOUNT, account);
        model.addAttribute(MODEL_ATTR_IS_EDIT, true);
        return new ModelAndView(VIEW_ADD_USER);
    }

    /**
     * temp call to redirect to the users page when a user goes to the index
     */
    @RequestMapping(ENDPOINT_INDEX)
    public RedirectView homePage() {
        return new RedirectView(ENDPOINT_USERS);
    }
}