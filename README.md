# Twitter-Bot
This is a twitter bot to enter competitions.

##How
First fill out the json in default-users.json with your query's, filters and auth tokens (auth tokens from https://apps.twitter.com/), this can also be done in the web UI also

Run the gradle command bootRun to run it on the local machine. Use build to build a runnable JAR. 

Note by default this uses an in memory DB see here for how to set up a real DB https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-sql.html

Give it 5-20 seconds to load, after which you can go to 127.0.0.1:8080/users, to start a bot go to toggle running modes and select your desired running mode  (streams recommended).
Then go to 127.0.0.1:8080/tweets to view a list of all the tweets your bot has actioned on.

##Why
I wanted to make a twitter bot as all current ones out there are limited in what they do, in effective and dont contain a UI.

##What
This is a twitter bot with a web UI allowing for multiple different users each with different search parameters.
Its implemented in Spring boot using TwitterJ4 for the twitter API access.
The web UI is basic and available at 127.0.0.1:8080

##How dose it do
In my testing using streams mode(recommended) it has a 97%+ efficiency (meaning that it can enter 97%+ of the theoretical max giveaways you could enter based on rate limits)
For comparison all other bots i have tested struggle to do 50% efficiency.
