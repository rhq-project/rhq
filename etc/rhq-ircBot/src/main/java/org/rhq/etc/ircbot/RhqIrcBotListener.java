package org.rhq.etc.ircbot;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.hooks.Listener;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.DisconnectEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.NickChangeEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

/**
 * An IRC bot for doing helpful stuff on the Freenode #rhq channel.
 *
 * @author Ian Springer
 * @author Jiri Kremser
 */
public class RhqIrcBotListener extends ListenerAdapter<RhqIrcBot> {

    private static final Pattern BUG_PATTERN = Pattern.compile("(?i)(bz|bug)[ ]*(\\d{6,7})");
    private static final Pattern COMMIT_PATTERN = Pattern.compile("(?i)(\\!commit|cm)[ ]*([0-9a-f]{3,40})");
    private static final Pattern ECHO_PATTERN = Pattern.compile("(?i)echo[ ]+(.+)");
    private static final String COMMIT_LINK = "https://git.fedorahosted.org/cgit/rhq/rhq.git/commit/?id=%s";
    private static final String PTO_LINK = "https://mail.corp.redhat.com/home/ccrouch@redhat.com/JBoss%20ON%20OOO?fmt=rss&view=day&start=0day&end=0day";
    private static final DateFormat monthFormat = new SimpleDateFormat("MMM");
    private static final DateFormat dayInMonthFormat = new SimpleDateFormat("d");

    private static enum Command {

        FORUM("Our forum is available from https://community.jboss.org/en/rhq?view=discussions", true), 
        HELP("You can use one of the following commands: ", true), 
        LISTS("Feel free to enroll to the user list https://lists.fedorahosted.org/mailman/listinfo/rhq-users"
                + " or the devel list https://lists.fedorahosted.org/mailman/listinfo/rhq-devel", true), 
        LOGS("IRC logs are available from http://transcripts.jboss.org/channel/irc.freenode.org/%23rhq/index.html", true),
        PTO,
        SOURCE("The code could be viewed/cloned on https://github.com/rhq-project or https://git.fedorahosted.org/cgit/rhq/rhq.git/", true), 
        SUPPORT, 
        WIKI("Our wiki is available from https://docs.jboss.org/author/display/RHQ/Home", true);

        public static final String PREFIX = "!";
        private final String staticRespond;
        private final boolean includeInHelp;

        Command(String staticRespond, boolean includeInHelp) {
            this.staticRespond = staticRespond;
            this.includeInHelp = includeInHelp;
        }

        Command() {
            this(null, false);
        }
    }

    private static final Set<String> JON_DEVS = new HashSet<String>();
    static {
        JON_DEVS.add("theute");
        JON_DEVS.add("jkremser");
        JON_DEVS.add("jsanda");
        JON_DEVS.add("jshaughn");
        JON_DEVS.add("lkrejci");
        JON_DEVS.add("mazz");
        JON_DEVS.add("mtho11");
        JON_DEVS.add("pilhuhn");
        JON_DEVS.add("spinder");
        JON_DEVS.add("stefan_n");
        JON_DEVS.add("tsegismont");
    }

    private final String server;
    private final String channel;
    private final boolean isRedHatChannel;
    private BugzillaConnector bzConnector = new BugzillaConnector();
    private final Map<Integer, Long> bugLogTimestamps = new HashMap<Integer, Long>();
    private final Map<String, String> names = new HashMap<String, String>();
    private final Map<String, String> ptoCache = new HashMap<String, String>();
    private final Map<String, String> supportCache = new HashMap<String, String>();
    private final Pattern commandPattern;

    public RhqIrcBotListener(String server, String channel) {
        this.server = server;
        this.channel = channel;
        isRedHatChannel = "irc.devel.redhat.com".equals(server);
        if (isRedHatChannel) System.out.print("Red Hat channel");
        StringBuilder commandRegExp = new StringBuilder();
        commandRegExp.append("^(?i)[ ]*").append(Command.PREFIX).append("(");
        for (Command command : Command.values()) {
            commandRegExp.append(command.name()).append('|');
        }
        commandRegExp.deleteCharAt(commandRegExp.length() - 1);
        commandRegExp.append(')');
        commandPattern = Pattern.compile(commandRegExp.toString());
    }

    @Override
    public void onMessage(MessageEvent<RhqIrcBot> event) throws Exception {
        if (event.getUser().getNick().toLowerCase().contains("bot")) {
            return; // never talk with artificial forms of life
        }
        
        PircBotX bot = event.getBot();
        if (!bot.getNick().equals(bot.getName())) {
            bot.changeNick(bot.getName());
        }

        // react to BZs in the messages
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
                    bot.sendMessage(
                        event.getChannel(),
                        "BZ " + bugId + " [product=" + product + ", priority=" + bug.getPriority() + ", status="
                            + bug.getStatus() + "] " + bug.getSummary() + " [ https://bugzilla.redhat.com/" + bugId
                            + " ]");
                }
                bugLogTimestamps.put(bugId, System.currentTimeMillis());
            } else {
                bot.sendMessage(event.getChannel(), "BZ " + bugId + " does not exist.");
            }
        }

        // react to the commit hashs included in the messages
        Matcher commitMatcher = COMMIT_PATTERN.matcher(message);
        while (commitMatcher.find()) {
            String shaHash = commitMatcher.group(2);
            String response = String.format(COMMIT_LINK, shaHash);
            bot.sendMessage(event.getChannel(), event.getUser().getNick() + ": " + response);
        }
        
        if (message.startsWith(event.getBot().getNick())) {
    		// someone asked bot directly, we have to remove that from message
            	message = message.substring(event.getBot().getNick().length());
    		message = message.replaceFirst("[^ ]*", "");
        }
        // react to commands included in the messages
        Matcher commandMatcher = commandPattern.matcher(message);
        while (commandMatcher.find()) {
            Command command = Command.valueOf(commandMatcher.group(1).toUpperCase());
            String response = prepareResponseForCommand(command);
            if (response != null) {
                bot.sendMessage(event.getChannel(), event.getUser().getNick() + ": " + response);
            }
        }

        // ping JON devs
        if (message.matches(".*\\b(jon-team|jboss-on-team)\\b.*")) {
            Set<User> users = bot.getUsers(event.getChannel());
            StringBuilder presentJonDevs = new StringBuilder();
            for (User user : users) {
                String nick = user.getNick();
                if (JON_DEVS.contains(nick) && !nick.equals(event.getUser().getNick())) {
                    presentJonDevs.append(nick).append(' ');
                }
            }
            bot.sendMessage(event.getChannel(), presentJonDevs + ": see message from " + event.getUser().getNick()
                + " above");
        }
    }

    @Override
    public void onPrivateMessage(PrivateMessageEvent<RhqIrcBot> privateMessageEvent) throws Exception {
        PircBotX bot = privateMessageEvent.getBot();
        String message = privateMessageEvent.getMessage();
        Matcher echoMatcher = ECHO_PATTERN.matcher(message);
        if (echoMatcher.matches()) {
	    if (!JON_DEVS.contains(privateMessageEvent.getUser().getNick())) {
		privateMessageEvent.respond("You're not my master, I am your master, go away");
	    } else {
		String echoMessage = echoMatcher.group(1);
		bot.sendMessage(this.channel, echoMessage);
	    }
        } else if (message.equalsIgnoreCase(Command.PREFIX + "listrenames")) {
            //Generate a list of renames in the form of old1 changed to new1, old2 changed to new2, etc
            StringBuilder users = new StringBuilder();
            for (Map.Entry<String, String> curUser : names.entrySet()) {
                users.append(curUser.getKey()).append(" changed to ").append(curUser.getValue()).append(", ");
            }
            String usersString = users.substring(0, users.length() - 3);
            privateMessageEvent.respond("Renamed users: " + usersString);
        } else {
            boolean isCommand = false;
            // react to commands included in the messages
            Matcher commandMatcher = commandPattern.matcher(message);
            while (commandMatcher.find()) {
                isCommand = true;
                Command command = Command.valueOf(commandMatcher.group(1).toUpperCase());
                String response = prepareResponseForCommand(command);
                if (response != null) {
                    bot.sendMessage(privateMessageEvent.getUser(), response);
                }
            }
            if (!isCommand) {
                bot.sendMessage(privateMessageEvent.getUser(), "Hi, I am " + bot.getFinger() + ".\n"
                    + prepareResponseForCommand(Command.HELP));
            }
        }
    }

    @Override
    public void onDisconnect(DisconnectEvent<RhqIrcBot> disconnectEvent) throws Exception {
        boolean connected = false;
        while (!connected) {
            Thread.sleep(60 * 1000L); // 1 minute
            try {
                PircBotX newBot = new RhqIrcBot(this);
                newBot.connect(this.server);
                newBot.joinChannel(this.channel);

                connected = true;
            } catch (Exception e) {
                System.err.println("Failed to reconnect to " + disconnectEvent.getBot().getServer() + " IRC server: "
                    + e);
            }
        }

        // Try to clean up the old bot, so it can release any memory, sockets, etc. it's using.
        PircBotX oldBot = disconnectEvent.getBot();
        Set<Listener> oldListeners = new HashSet<Listener>(oldBot.getListenerManager().getListeners());
        for (Listener oldListener : oldListeners) {
            oldBot.getListenerManager().removeListener(oldListener);
        }
    }

    @Override
    public void onNickChange(NickChangeEvent<RhqIrcBot> event) throws Exception {
        //Store the result
        names.put(event.getOldNick(), event.getNewNick());
    }

    private String prepareResponseForCommand(Command command) {
        if (command.staticRespond != null) {
            String response = command.staticRespond;
            if (command == Command.HELP) {
                for (Command com : Command.values()) {
                    if (com.includeInHelp) {
                        response += Command.PREFIX + com.toString().toLowerCase() + " ";
                    }
                }
            }
            return response;
        }
        switch (command) {
        case SUPPORT:
            if (isRedHatChannel)
            return whoIsOnSupport();
        case PTO:
            if (isRedHatChannel)
            return whoIsOnPto(PTO_LINK);
        default:
            System.err.println("Unknown command:" + command);
            break;
        }
        return null;
    }

    private String whoIsOnSupport() {
        String month = monthFormat.format(new Date());
        String dayInMonth = dayInMonthFormat.format(new Date());
        String cachedValue = supportCache.get(month + "#" + dayInMonth);
        if (cachedValue != null) {
            return cachedValue;
        }
        String onSupport = GDocParser.onSupport1();
        String value = doNotNotify(onSupport + " is on support this week");
        supportCache.put(month + "#" + dayInMonth, value);
        return value;
    }
    
    private String whoIsOnPto(String link) {
        String month = monthFormat.format(new Date());
        String dayInMonth = dayInMonthFormat.format(new Date());
        String cachedValue = ptoCache.get(month + "#" + dayInMonth);
        if (cachedValue != null) {
            return cachedValue;
        }
        try {
            String onPto = "";
            Document doc = Jsoup.connect(link).ignoreContentType(true).get();
            Elements titles = doc.select("rss channel item title");
            for (Element title : titles) {
                onPto += doNotNotify(title.text()) + ", ";
            }
            if (!onPto.isEmpty()) {
                String value = doNotNotify(onPto.substring(0, onPto.length() - 2));
                ptoCache.put(month + "#" + dayInMonth, value);
                return value;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "no one is on PTO today";
    }

    private String doNotNotify(String nick) {
        //replace all vowels with unicode chars that look same not to spam users with notifications
        return nick.toLowerCase().replaceFirst("a", "\u0430").replaceFirst("e", "\u0435").replaceFirst("i", "\u0456").replaceFirst("o", "\u043E").replaceFirst("u", "\u222A").replaceFirst("y", "\u028F");
    }
}
