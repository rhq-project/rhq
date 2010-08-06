package org.rhq.plugins.irc;

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

    private Map<String, IRCChannelComponent> channels = new HashMap<String, IRCChannelComponent>();

    private String host;
    private String port;
    private String nick;
    private List<String> activeChannels;

    private Map<String, ChannelInfo> info = new HashMap<String, ChannelInfo>();


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
        activeChannels = Arrays.asList(this.bot.getChannels());

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

    public void registerChannel(IRCChannelComponent channelComponent) {
        this.channels.put(channelComponent.getChannel(), channelComponent);

        this.bot.joinChannel(channelComponent.getChannel());
        updateChannels();
    }

    public void unregisterChannel(IRCChannelComponent channelComponent) {
        this.bot.partChannel(channelComponent.getChannel());
        this.channels.remove(channelComponent.getChannel());
    }

    public boolean isInChannel(String channel) {
        return activeChannels.contains(channel);
    }


    public void sendMessage(String channel, String message) {
        this.bot.sendMessage(channel, message);
    }

    public OperationResult invokeOperation(String name, Configuration parameters) throws InterruptedException, Exception {
        if (name.equals("listChannels")) {
            OperationResult result = new OperationResult();
            Configuration resultConfig = result.getComplexResults();
            PropertyList channelList = new PropertyList("channelList");

            this.bot.listChannels();

            Thread.sleep(5000); // TODO is this long enough... any other way to know when the list is done?

            for (ChannelInfo channelInfo : info.values()) {
                PropertyMap channelMap = new PropertyMap("channelMap");
                channelMap.put(new PropertySimple("channel", channelInfo.channel));
                channelMap.put(new PropertySimple("userCount", channelInfo.userCount));
                channelMap.put(new PropertySimple("topic", channelInfo.topic));
                channelList.add(channelMap);
            }
            resultConfig.put(channelList);
            return result;
        }
        return null;
    }

    public int getUserCount(String channel) {
        return this.bot.getUsers(channel).length;
    }

    public static class ChannelInfo {
        String channel;
        int userCount;
        String topic;

        public ChannelInfo(String channel, int userCount, String topic) {
            this.channel = channel;
            this.userCount = userCount;
            this.topic = topic;
        }
    }

    public class Bot extends PircBot {

        public Bot(String nick) {
            this.setName(nick);
        }

        @Override
        protected void onChannelInfo(String channel, int userCount, String topic) {
            info.put(channel, new ChannelInfo(channel, userCount, topic));
        }

        public void onMessage(String channel, String sender, String login, String hostname, String message) {

            IRCChannelComponent component = IRCServerComponent.this.channels.get(channel);
            if (component != null) {
                component.acceptMessage(sender, login, hostname, message);
            }

            if (message.contains(getName()) && sender.contains("ghinkle")) {
                sendMessage(channel, "monitoring " + channels.size() + " channels");
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


    private void updateChannels() {
        activeChannels = Arrays.asList(this.bot.getChannels());
    }

}