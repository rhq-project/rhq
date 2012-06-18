package org.rhq.etc.ircbot;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.j2bugzilla.base.Bug;
import com.j2bugzilla.base.BugzillaConnector;
import com.j2bugzilla.base.BugzillaException;
import com.j2bugzilla.rpc.GetBug;

import org.apache.xmlrpc.XmlRpcException;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.hooks.Listener;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.DisconnectEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

/**
 * An IRC bot for doing helpful stuff on the Freenode #rhq channel.
 *
 * @author Ian Springer
 */
public class RhqIrcBot extends ListenerAdapter {

    private static final Pattern BUG_PATTERN = Pattern.compile("(?i)(bz|bug)[ ]*(\\d{6,7})");
    private static final Pattern ECHO_PATTERN = Pattern.compile("(?i)echo[ ]+(.+)");

    private static final Set<String> JON_DEVS = new HashSet<String>();
    static {
        JON_DEVS.add("ccrouch");
        JON_DEVS.add("ips");
        JON_DEVS.add("jkremser");
        JON_DEVS.add("jsanda");
        JON_DEVS.add("jshaughn");
        JON_DEVS.add("lkrejci");
        JON_DEVS.add("mazz");
        JON_DEVS.add("mtho11");
        JON_DEVS.add("pilhuhn");
        JON_DEVS.add("spinder");
        JON_DEVS.add("stefan_n");
    }

    private String server;
    private String channel;
    private BugzillaConnector bzConnector = new BugzillaConnector();
    private Map<Integer, Long> bugLogTimestamps = new HashMap<Integer, Long>();

    public RhqIrcBot(String server, String channel) {
        this.server = server;
        this.channel = channel;
    }

    @Override
    public void onMessage(MessageEvent event) throws Exception {
        PircBotX bot = event.getBot();
        if (!bot.getNick().equals(bot.getName())) {
            bot.changeNick(bot.getName());
        }

        String message = event.getMessage();
        Matcher bugMatcher = BUG_PATTERN.matcher(message);
        while (bugMatcher.find()) {
            int bugId = Integer.valueOf(bugMatcher.group(2));
            GetBug getBug = new GetBug(bugId);
            try {
                bzConnector.executeMethod(getBug);
            } catch (Exception e) {
                bzConnector = new BugzillaConnector();
                bzConnector.connectTo("https://bugzilla.redhat.com");
                try {
                    bzConnector.executeMethod(getBug);
                } catch (BugzillaException e1) {
                    //e1.printStackTrace();
                    Throwable cause = e1.getCause();
                    String details = (cause instanceof XmlRpcException) ? cause.getMessage() : e1.getMessage();
                    bot.sendMessage(event.getChannel(), "Failed to access BZ " + bugId + ": " + details);
                    continue;
                }
            }
            Bug bug = getBug.getBug();
            if (bug != null) {
                String product = bug.getProduct();
                if (product.equals("RHQ Project")) {
                    product = "RHQ";
                } else if (product.equals("JBoss Operations Network")) {
                    product = "JON";
                }
                Long timestamp = bugLogTimestamps.get(bugId);
                if ((timestamp == null) || ((System.currentTimeMillis() - timestamp) > (5 * 60 * 1000L))) {
                    bot.sendMessage(event.getChannel(), "BZ " + bugId + " [product=" + product
                            + ", priority=" + bug.getPriority() + ", status=" + bug.getStatus() + "] "
                            + bug.getSummary() + " [ https://bugzilla.redhat.com/" + bugId + " ]");
                }
                bugLogTimestamps.put(bugId, System.currentTimeMillis());
            } else {
                bot.sendMessage(event.getChannel(), "BZ " + bugId + " does not exist.");
            }
        }

        if (message.matches(".*\\b(jon-team|jboss-on-team)\\b.*")) {
            Set<User> users = bot.getUsers(event.getChannel());
            StringBuilder presentJonDevs = new StringBuilder();
            for (User user : users) {
                String nick = user.getNick();
                if (JON_DEVS.contains(nick) && !nick.equals(event.getUser().getNick())) {
                    presentJonDevs.append(nick).append(' ');
                }
            }
            bot.sendMessage(event.getChannel(), presentJonDevs + ": see message from "
                    + event.getUser().getNick() + " above");
        }
    }

    @Override
    public void onPrivateMessage(PrivateMessageEvent privateMessageEvent) throws Exception {
        PircBotX bot = privateMessageEvent.getBot();
        String message = privateMessageEvent.getMessage();
        Matcher echoMatcher = ECHO_PATTERN.matcher(message);
        if (echoMatcher.matches()) {
            String echoMessage = echoMatcher.group(1);
            bot.sendMessage(this.channel, echoMessage);
        } else {
            bot.sendMessage(privateMessageEvent.getUser(), "Hi, I am " + bot.getFinger() + ".");
        }
        // TODO: Implement a HELP command.
    }

    @Override
    public void onDisconnect(DisconnectEvent disconnectEvent) throws Exception {
        boolean connected = false;
        while (!connected) {
            Thread.sleep(60 * 1000L); // 1 minute
            try {
                PircBotX newBot = createBot(this);
                newBot.connect(this.server);
                newBot.joinChannel(this.channel);

                connected = true;
            } catch (Exception e) {
                System.err.println("Failed to reconnect to " + disconnectEvent.getBot().getServer() + " IRC server: " + e);
            }
        }

        // Try to clean up the old bot, so it can release any memory, sockets, etc. it's using.
        PircBotX oldBot = disconnectEvent.getBot();
        Set<Listener> oldListeners = new HashSet<Listener>(oldBot.getListenerManager().getListeners());
        for (Listener oldListener : oldListeners) {
            oldBot.getListenerManager().removeListener(oldListener);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: RhqIrcBot IRC_SERVER IRC_CHANNEL");
            System.err.println(" e.g.: RhqIrcBot irc.freenode.net '#rhq'");
            System.exit(1);
        }
        String server = args[0];
        String channel = args[1];
        if (channel.charAt(0) != '#') {
            channel = '#' + channel;
        }

        RhqIrcBot rhqBot = new RhqIrcBot(server, channel);

        PircBotX bot = createBot(rhqBot);
        bot.connect(server);
        bot.joinChannel(channel);
    }

    private static PircBotX createBot(RhqIrcBot rhqBot) {
        PircBotX bot = new PircBotX();

        bot.setName("rhq-bot");
        bot.setVersion("1.0");
        bot.setFinger("RHQ IRC bot (source code in RHQ git under etc/rhq-ircBot/)");

        bot.setVerbose(true);
        bot.setAutoNickChange(true);

        bot.getListenerManager().addListener(rhqBot);
        bot.setSocketTimeout(1 * 60 * 1000); // 1 minute
        return bot;
    }

}
