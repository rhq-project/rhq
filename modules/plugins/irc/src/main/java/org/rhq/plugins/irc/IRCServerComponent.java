package org.rhq.plugins.irc;

import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.jibble.pircbot.PircBot;


/**
 * @author Greg Hinkle
 */
public class IRCServerComponent implements ResourceComponent, OperationFacet {
    private final Log log = LogFactory.getLog(this.getClass());

    private Bot bot;

    private Map<String, IRCRepoComponent> repos = new HashMap<String, IRCRepoComponent>();

    private String host;
    private String port;
    private String nick;
    private List<String> activeRepos;

    private Map<String, RepoInfo> info = new HashMap<String, RepoInfo>();


    /**
     * Return availability of this resource
     *
     * @see org.rhq.core.pluginapi.inventory.ResourceComponent#getAvailability()
     */
    public AvailabilityType getAvailability() {

        if (!this.bot.isConnected()) {
            try {
                this.bot.connect(host);

            } catch (Exception e) {
                log.warn("Failure to connect to IRC server " + host + " reason: " + e.getMessage());
            }
        }
        activeRepos = Arrays.asList(this.bot.getChannels());

        return this.bot.isConnected() ? AvailabilityType.UP : AvailabilityType.DOWN;
    }


    /**
     * Start the resource connection
     *
     * @see org.rhq.core.pluginapi.inventory.ResourceComponent#start(org.rhq.core.pluginapi.inventory.ResourceContext)
     */
    public void start(ResourceContext context) throws InvalidPluginConfigurationException, Exception {

        Configuration conf = context.getPluginConfiguration();

        host = conf.getSimple(IRCServerDiscoveryComponent.CONFIG_HOST).getStringValue();
        port = conf.getSimple(IRCServerDiscoveryComponent.CONFIG_PORT).getStringValue();
        nick = conf.getSimple(IRCServerDiscoveryComponent.CONFIG_NICK).getStringValue();

        this.bot = new Bot(nick);
        // bot.setVerbose(true);
        bot.setAutoNickChange(true);
        bot.connect(host);

    }

    public void registerRepo(IRCRepoComponent repoComponent) {
        this.repos.put(repoComponent.getRepo(), repoComponent);

        this.bot.joinChannel(repoComponent.getRepo());
        updateRepos();
    }

    public void unregisterRepo(IRCRepoComponent repoComponent) {
        this.bot.partChannel(repoComponent.getRepo());
        this.repos.remove(repoComponent.getRepo());
    }

    public boolean isInRepo(String repo) {
        return activeRepos.contains(repo);
    }


    public void sendMessage(String repo, String message) {
        this.bot.sendMessage(repo, message);
    }

    public OperationResult invokeOperation(String name, Configuration parameters) throws InterruptedException, Exception {
        if (name.equals("listRepos")) {
            OperationResult result = new OperationResult();
            Configuration resultConfig = result.getComplexResults();
            PropertyList repoList = new PropertyList("repoList");

            this.bot.listChannels();

            Thread.sleep(5000); // TODO is this long enough... any other way to know when the list is done?

            for (RepoInfo repoInfo : info.values()) {
                PropertyMap repoMap = new PropertyMap("repoMap");
                repoMap.put(new PropertySimple("repo", repoInfo.repo));
                repoMap.put(new PropertySimple("userCount", repoInfo.userCount));
                repoMap.put(new PropertySimple("topic", repoInfo.topic));
                repoList.add(repoMap);
            }
            resultConfig.put(repoList);
            return result;
        }
        return null;
    }

    public int getUserCount(String repo) {
        return this.bot.getUsers(repo).length;
    }

    public static class RepoInfo {
        String repo;
        int userCount;
        String topic;

        public RepoInfo(String repo, int userCount, String topic) {
            this.repo = repo;
            this.userCount = userCount;
            this.topic = topic;
        }
    }

    public class Bot extends PircBot {

        public Bot(String nick) {
            this.setName(nick);
        }

        protected void onRepoInfo(String repo, int userCount, String topic) {
            info.put(repo, new RepoInfo(repo, userCount, topic));
        }

        public void onMessage(String repo, String sender, String login, String hostname, String message) {

            IRCRepoComponent component = IRCServerComponent.this.repos.get(repo);
            if (component != null) {
                component.acceptMessage(sender, login, hostname, message);
            }

            if (message.contains(getName()) && sender.contains("ghinkle")) {
                sendMessage(repo, "monitoring " + repos.size() + " repos");
            }
        }
    }


    /**
     * Tear down the rescource connection
     *
     * @see org.rhq.core.pluginapi.inventory.ResourceComponent#stop()
     */
    public void stop() {
        this.bot.disconnect();
    }


    private void updateRepos() {
        activeRepos = Arrays.asList(this.bot.getChannels());
    }

}