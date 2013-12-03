/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.enterprise.agent.i18n;

import mazz.i18n.Msg;
import mazz.i18n.annotation.I18NMessage;
import mazz.i18n.annotation.I18NMessages;
import mazz.i18n.annotation.I18NResourceBundle;

import org.rhq.enterprise.agent.AgentConfigurationConstants;
import org.rhq.enterprise.communications.ServiceContainerConfigurationConstants;

/**
 * Constants and resource bundle keys that define the setup instructions when setting up the agent.
 *
 * @author John Mazzitelli
 */
@I18NResourceBundle(baseName = "agent-setup", defaultLocale = "en")
public interface AgentSetupInstructions {
    /**
     * This is the base bundle name of the resource bundle where all of the below messages are stored.
     */
    Msg.BundleBaseName BASE_BUNDLE_NAME = new Msg.BundleBaseName("agent-setup");

    // SERVER TRANSPORT
    String SETUP_INSTRUCTION_SERVERTRANSPORT_PREF = AgentConfigurationConstants.SERVER_TRANSPORT;
    String SETUP_INSTRUCTION_SERVERTRANSPORT_DEFAULT = AgentConfigurationConstants.DEFAULT_SERVER_TRANSPORT;
    @I18NMessages( { @I18NMessage("RHQ Server Transport Protocol") })
    String SETUP_INSTRUCTION_SERVERTRANSPORT_PROMPT = "PromptCommand.setup.instruction.serveruri.transport.prompt";
    @I18NMessages( { @I18NMessage("The transport used to send messages to the RHQ Server.\\n\\\n"
        + "Acceptable values are either servlet or sslservlet. If you want this agent to\\n\\\n"
        + "talk to the server securely over an encrypted repo, use sslservlet.") })
    String SETUP_INSTRUCTION_SERVERTRANSPORT_HELP = "PromptCommand.setup.instruction.serveruri.transport.help";

    // SERVER ADDRESS
    String SETUP_INSTRUCTION_SERVERBINDADDR_PREF = AgentConfigurationConstants.SERVER_BIND_ADDRESS;
    String SETUP_INSTRUCTION_SERVERBINDADDR_DEFAULT = AgentConfigurationConstants.DEFAULT_SERVER_BIND_ADDRESS;
    @I18NMessages( { @I18NMessage("RHQ Server Hostname or IP Address") })
    String SETUP_INSTRUCTION_SERVERBINDADDR_PROMPT = "PromptCommand.setup.instruction.serveruri.bindaddr.prompt";
    @I18NMessages( { @I18NMessage("The hostname or IP address the RHQ Server will bind to in order to\\n\\\n"
        + "listen for incoming messages from agents.") })
    String SETUP_INSTRUCTION_SERVERBINDADDR_HELP = "PromptCommand.setup.instruction.serveruri.bindaddr.help";

    // SERVER PORT
    String SETUP_INSTRUCTION_SERVERPORT_PREF = AgentConfigurationConstants.SERVER_BIND_PORT;
    String SETUP_INSTRUCTION_SERVERPORT_DEFAULT = Integer
        .toString(AgentConfigurationConstants.DEFAULT_SERVER_BIND_PORT);
    @I18NMessages( { @I18NMessage("RHQ Server Port") })
    String SETUP_INSTRUCTION_SERVERPORT_PROMPT = "PromptCommand.setup.instruction.serveruri.bindport.prompt";
    @I18NMessages( { @I18NMessage("The port that the RHQ Server listens to for incoming messages from agents.") })
    String SETUP_INSTRUCTION_SERVERPORT_HELP = "PromptCommand.setup.instruction.serveruri.bindport.help";

    // SERVER TRANSPORT PARAMS
    String SETUP_INSTRUCTION_SERVERTRANSPORTPARAMS_PREF = AgentConfigurationConstants.SERVER_TRANSPORT_PARAMS;
    String SETUP_INSTRUCTION_SERVERTRANSPORTPARAMS_DEFAULT = AgentConfigurationConstants.DEFAULT_SERVER_TRANSPORT_PARAMS;
    @I18NMessages( { @I18NMessage("RHQ Server Transport Parameters") })
    String SETUP_INSTRUCTION_SERVERTRANSPORTPARAMS_PROMPT = "PromptCommand.setup.instruction.serveruri.transportparams.prompt";
    @I18NMessages( { @I18NMessage("A set of transport parameters that is used to further configure\\n\\\n"
        + "how the agent connects to the server. Any value you provide here\\n\\\n"
        + "will overwrite (not augment) the current value.\\n\\\n"
        + "See the documentation for information on the format of this setting\\n\\\n"
        + "and all the different values allowed for the specific transport being used.") })
    String SETUP_INSTRUCTION_SERVERTRANSPORTPARAMS_HELP = "PromptCommand.setup.instruction.serveruri.transportparams.help";

    // SERVER ALIAS
    String SETUP_INSTRUCTION_SERVERALIAS_PREF = AgentConfigurationConstants.SERVER_ALIAS;
    String SETUP_INSTRUCTION_SERVERALIAS_DEFAULT = null;
    @I18NMessages( { @I18NMessage("RHQ Server Alias") })
    String SETUP_INSTRUCTION_SERVERALIAS_PROMPT = "PromptCommand.setup.instruction.serveralias.prompt";
    @I18NMessages( { @I18NMessage("If the RHQ Server hostname or IP address was not set,\\n\\\n"
        + "this DNS alias name will be looked up and used as the RHQ Server host.\\n\\\n"
        + "Not setting this preference will disable this DNS alias lookup feature") })
    String SETUP_INSTRUCTION_SERVERALIAS_HELP = "PromptCommand.setup.instruction.serveralias.help";

    // SERVER AUTO-DETECTION
    String SETUP_INSTRUCTION_SERVERAUTODETECT_PREF = AgentConfigurationConstants.SERVER_AUTO_DETECTION;
    String SETUP_INSTRUCTION_SERVERAUTODETECT_DEFAULT = Boolean
        .toString(AgentConfigurationConstants.DEFAULT_SERVER_AUTO_DETECTION);
    @I18NMessages( { @I18NMessage("Enable RHQ Server Auto-Detection?") })
    String SETUP_INSTRUCTION_SERVERAUTODETECT_PROMPT = "PromptCommand.setup.instruction.serverautodetect.prompt";
    @I18NMessages( { @I18NMessage("If true, the agent will attempt to auto-detect the RHQ Server\\n\\\n"
        + "coming online and going offline.  This is more efficient than\\n\\\n"
        + "server polling but it requires multicast traffic to be enabled on\\n\\\n"
        + "your network and also requires the multicast detector be enabled.") })
    String SETUP_INSTRUCTION_SERVERAUTODETECT_HELP = "PromptCommand.setup.instruction.serverautodetect.help";

    // SERVER POLLING INTERVAL
    String SETUP_INSTRUCTION_SERVERPOLLING_PREF = AgentConfigurationConstants.CLIENT_SENDER_SERVER_POLLING_INTERVAL;
    String SETUP_INSTRUCTION_SERVERPOLLING_DEFAULT = Long
        .toString(AgentConfigurationConstants.DEFAULT_CLIENT_SENDER_SERVER_POLLING_INTERVAL);
    @I18NMessages( { @I18NMessage("RHQ Server Polling Interval") })
    String SETUP_INSTRUCTION_SERVERPOLLING_PROMPT = "PromptCommand.setup.instruction.serverpolling.prompt";
    @I18NMessages( { @I18NMessage("If this value is larger than 0, it indicates the agent\\n\\\n"
        + "should periodically poll the RHQ Server to make sure it is still\\n\\\n"
        + "up or (if it was down) see when it comes back up. The value is\\n\\\n"
        + "the number of milliseconds to wait in between polls.  If the\\n\\\n"
        + "value is 0, server polling is disabled.  Server polling\\n\\\n"
        + "is less efficient that the auto-detection mechanism,\\n\\\n"
        + "but server polling does not use multicasting, and thus might\\n\\\n"
        + "be the only way for the agent to detect the server.") })
    String SETUP_INSTRUCTION_SERVERPOLLING_HELP = "PromptCommand.setup.instruction.serverpolling.help";

    // AGENT DATA DIRECTORY
    String SETUP_INSTRUCTION_AGENTDATADIR_PREF = AgentConfigurationConstants.DATA_DIRECTORY;
    String SETUP_INSTRUCTION_AGENTDATADIR_DEFAULT = AgentConfigurationConstants.DEFAULT_DATA_DIRECTORY;
    @I18NMessages( { @I18NMessage("Data Directory") })
    String SETUP_INSTRUCTION_AGENTDATADIR_PROMPT = "PromptCommand.setup.instruction.datadir.prompt";
    @I18NMessages( { @I18NMessage("Directory location where the agent will persist its data.") })
    String SETUP_INSTRUCTION_AGENTDATADIR_HELP = "PromptCommand.setup.instruction.datadir.help";

    // CLIENT SENDER QUEUE SIZE
    String SETUP_INSTRUCTION_CLIENTSENDERQSIZE_PREF = AgentConfigurationConstants.CLIENT_SENDER_QUEUE_SIZE;
    String SETUP_INSTRUCTION_CLIENTSENDERQSIZE_DEFAULT = Integer
        .toString(AgentConfigurationConstants.DEFAULT_CLIENT_SENDER_QUEUE_SIZE);
    @I18NMessages( { @I18NMessage("Command Queue Size") })
    String SETUP_INSTRUCTION_CLIENTSENDERQSIZE_PROMPT = "PromptCommand.setup.instruction.sender.qsize.prompt";
    @I18NMessages( { @I18NMessage("The maximum number of commands that can be queued for sending to the RHQ Server.\\n\\\n"
        + "If this is 0, then the queue is unbounded (be careful - setting this to 0\\n\\\n"
        + "could cause the agent to use up too much memory if, for some reason,\\n\\\n"
        + "commands are getting queued but are unable to be sent") })
    String SETUP_INSTRUCTION_CLIENTSENDERQSIZE_HELP = "PromptCommand.setup.instruction.sender.qsize.help";

    // CLIENT SENDER MAX CONCURRENT
    String SETUP_INSTRUCTION_CLIENTSENDERMAXCONCURRENT_PREF = AgentConfigurationConstants.CLIENT_SENDER_MAX_CONCURRENT;
    String SETUP_INSTRUCTION_CLIENTSENDERMAXCONCURRENT_DEFAULT = Integer
        .toString(AgentConfigurationConstants.DEFAULT_CLIENT_SENDER_MAX_CONCURRENT);
    @I18NMessages( { @I18NMessage("Maximum Commands To Concurrently Send") })
    String SETUP_INSTRUCTION_CLIENTSENDERMAXCONCURRENT_PROMPT = "PromptCommand.setup.instruction.sender.maxconcurrent.prompt";
    @I18NMessages( { @I18NMessage("The maximum number of commands that can be in the process\\n\\\n"
        + "of being sent to the RHQ Server at any one time.") })
    String SETUP_INSTRUCTION_CLIENTSENDERMAXCONCURRENT_HELP = "PromptCommand.setup.instruction.sender.help";

    // CLIENT SENDER COMMAND TIMEOUT
    String SETUP_INSTRUCTION_CLIENTSENDERTIMEOUT_PREF = AgentConfigurationConstants.CLIENT_SENDER_COMMAND_TIMEOUT;
    String SETUP_INSTRUCTION_CLIENTSENDERTIMEOUT_DEFAULT = Long
        .toString(AgentConfigurationConstants.DEFAULT_CLIENT_SENDER_COMMAND_TIMEOUT);
    @I18NMessages( { @I18NMessage("Command Send Timeout") })
    String SETUP_INSTRUCTION_CLIENTSENDERTIMEOUT_PROMPT = "PromptCommand.setup.instruction.sender.timeout.prompt";
    @I18NMessages( { @I18NMessage("The time in milliseconds that the agent will wait\\n\\\n"
        + "before aborting a command. This is the amount of time in\\n\\\n"
        + "milliseconds that the RHQ Server has in order to process commands.\\n\\\n"
        + "This value is only the default if a command has not specified\\n\\\n"
        + "its own timeout.  A command can override this by setting its\\n\\\n"
        + "own timeout in its command configuration, so this value may\\n\\\n"
        + "not be used for all commands that are sent. If this value is\\n\\\n"
        + "less than or equal to 0, there will be no default timeout\\n\\\n"
        + "and commands will therefore be allowed to take as long as they\\n\\\n"
        + "need (again, this is the default, individual commands may\\n\\\n"
        + "override this and set their own timeout).  While this infinite\\n\\\n"
        + "timeout default could conceivably cause a thread to hang\\n\\\n"
        + "waiting for a rogue command that never finishes, it also reduces\\n\\\n"
        + "the amount of short-lived threads created by the agent\\n\\\n"
        + "and will increase throughput, dramatically in some cases.") })
    String SETUP_INSTRUCTION_CLIENTSENDERTIMEOUT_HELP = "PromptCommand.setup.instruction.sender.timeout.help";

    // CLIENT SENDER RETRY INTERVAL
    String SETUP_INSTRUCTION_CLIENTSENDERRETRYINTERVAL_PREF = AgentConfigurationConstants.CLIENT_SENDER_RETRY_INTERVAL;
    String SETUP_INSTRUCTION_CLIENTSENDERRETRYINTERVAL_DEFAULT = Long
        .toString(AgentConfigurationConstants.DEFAULT_CLIENT_SENDER_RETRY_INTERVAL);
    @I18NMessages( { @I18NMessage("Command Send Retry Interval") })
    String SETUP_INSTRUCTION_CLIENTSENDERRETRYINTERVAL_PROMPT = "PromptCommand.setup.instruction.sender.retry.prompt";
    @I18NMessages( { @I18NMessage("This is the minimum amount of time, in milliseconds, the agent\\n\\\n"
        + "will wait before trying to resend a guaranteed command\\n\\\n"
        + "that previously failed.  This is not a guarantee of when\\n\\\n"
        + "a command is retried - all that can be inferred is that a\\n\\\n"
        + "command that fails to be sent will not be retried until at\\n\\\n"
        + "least this amount of time passes.\\n\\\n"
        + "Note: if the agent is currently waiting in this retry pause\\n\\\n"
        + "period, the agent will not be able to be shutdown until that\\n\\\n"
        + "retry period is over. In other words, if the agent is asked\\n\\\n"
        + "to shutdown, it will wait for those commands waiting in this\\n\\\n"
        + "retry interval to wake up. This is to help ensure those\\n\\\n"
        + "commands are not lost.  Keep this time period short enough\\n\\\n"
        + "to make agent shutdowns fairly responsive but long enough\\n\\\n"
        + "to avoid spinning the agent with continuous resending of\\n\\\n"
        + "commands during periods of RHQ Server downtime. It is\\n\\\n"
        + "recommended to use auto-detection or server polling in order\\n\\\n"
        + "to automatically stop the agent from continuously\\n\\\n"
        + "trying to retry commands during long periods of RHQ\\n\\\n" + "Server downtime.") })
    String SETUP_INSTRUCTION_CLIENTSENDERRETRYINTERVAL_HELP = "PromptCommand.setup.instruction.sender.retry.help";

    // CLIENT SENDER MAX RETRIES
    String SETUP_INSTRUCTION_CLIENTSENDERMAXRETRIES_PREF = AgentConfigurationConstants.CLIENT_SENDER_MAX_RETRIES;
    String SETUP_INSTRUCTION_CLIENTSENDERMAXRETRIES_DEFAULT = Integer
        .toString(AgentConfigurationConstants.DEFAULT_CLIENT_SENDER_MAX_RETRIES);
    @I18NMessages( { @I18NMessage("Command Send Max Retries") })
    String SETUP_INSTRUCTION_CLIENTSENDERMAXRETRIES_PROMPT = "PromptCommand.setup.instruction.sender.maxretries.prompt";
    @I18NMessages( { @I18NMessage("If a guaranteed delivery message is sent, but the agent fails\\n\\\n"
        + "to connect to the server and deliver the message, it will\\n\\\n"
        + "always be retried. However, if the error was something other\\n\\\n"
        + "than a 'cannot connect' error, the command will only be retried\\n\\\n"
        + "this amount of times before the command is dropped. When this\\n\\\n"
        + "happens, the guaranteed command will never be delivered. This\\n\\\n"
        + "will normally happen under very odd and rare circumstances.\\n\\\n"
        + "Also, this setting only effects asynchronous messages that are\\n\\\n"
        + "sent with guaranteed delivery.  This setting has no effect\\n\\\n" + "on other messages.") })
    String SETUP_INSTRUCTION_CLIENTSENDERMAXRETRIES_HELP = "PromptCommand.setup.instruction.sender.maxretries.help";

    // CLIENT SENDER COMMAND SPOOL FILE PARAMS
    String SETUP_INSTRUCTION_CLIENTSENDERSPOOLPARAMS_PREF = AgentConfigurationConstants.CLIENT_SENDER_COMMAND_SPOOL_FILE_PARAMS;
    String SETUP_INSTRUCTION_CLIENTSENDERSPOOLPARAMS_DEFAULT = AgentConfigurationConstants.DEFAULT_CLIENT_SENDER_COMMAND_SPOOL_FILE_PARAMS;
    @I18NMessages( { @I18NMessage("Command Spool File Parameters") })
    String SETUP_INSTRUCTION_CLIENTSENDERSPOOLPARAMS_PROMPT = "PromptCommand.setup.instruction.sender.spoolfileparams.prompt";
    @I18NMessages( { @I18NMessage("This defines the parameters for the command spool file.\\n\\\n"
        + "The spool file is where the agent persists commands that\\n\\\n"
        + "are flagged for guaranteed delivery and need to be sent.\\n\\\n"
        + "The format is defined as \'max-file-size:purge-percentage\'.\\n\\\n"
        + "The first number is the size, in bytes, of the maximum file\\n\\\n"
        + "size threshold.  If the spool file grows larger than this, a\\n\\\n"
        + "purge will be triggered in order to shrink the file.\\n\\\n"
        + "The second number is the purge percentage which indicates how\\n\\\n"
        + "large the file is allowed to be after a purge.  This is\\n\\\n"
        + "specified as a percentage of the first parameter - the max\\n\\\n"
        + "file size threshold.  For example, if the max file size is\\n\\\n"
        + "100000 (i.e. 100KB) and the purge percentage is 90, then when\\n\\\n"
        + "the spool file grows larger than 100KB, a purge will be\\n\\\n"
        + "triggered and the file will be shrunk to no more than\\n\\\n"
        + "90% of 100KB - which is 90KB.  In effect, 10KB will be freed\\n\\\n"
        + "to allow room for new commands to be spooled.  When this\\n\\\n"
        + "occurs, unused space is freed first and if that does not\\n\\\n"
        + "free up enough space, the oldest commands in the spool file\\n\\\n"
        + "will be sacrificed in order to make room for the newer\\n\\\n" + "commands.\\n\\\n"
        + "The maximum file size must be at least 10000 bytes.\\n\\\n"
        + "The purge percentage must be between 0 and 99.\\n\\\n" + "") })
    String SETUP_INSTRUCTION_CLIENTSENDERSPOOLPARAMS_HELP = "PromptCommand.setup.instruction.sender.spoolfileparams.help";

    // CLIENT SENDER COMMAND SPOOL FILE COMPRESSED
    String SETUP_INSTRUCTION_CLIENTSENDERCOMPRESSSPOOL_PREF = AgentConfigurationConstants.CLIENT_SENDER_COMMAND_SPOOL_FILE_COMPRESSED;
    String SETUP_INSTRUCTION_CLIENTSENDERCOMPRESSSPOOL_DEFAULT = Boolean
        .toString(AgentConfigurationConstants.DEFAULT_CLIENT_SENDER_COMMAND_SPOOL_FILE_COMPRESSED);
    @I18NMessages( { @I18NMessage("Compress the Spool File?") })
    String SETUP_INSTRUCTION_CLIENTSENDERCOMPRESSSPOOL_PROMPT = "PromptCommand.setup.instruction.sender.compressspool.prompt";
    @I18NMessages( { @I18NMessage("If this flag is true, the commands stored in the spool file\\n\\\n"
        + "will be compressed. This can potentially save about 30%-40% in\\n\\\n"
        + "disk space (give or take), however, it slows down the\\n\\\n"
        + "persistence mechanism considerably. Recommended setting for\\n\\\n"
        + "this should be false unless something on the agent deployment\\n\\\n"
        + "box warrants disk-saving over persistence performance. The\\n\\\n"
        + "performance hit will only appear when unusual conditions occur,\\n\\\n"
        + "such as shutting down while some guaranteed commands have not\\n\\\n"
        + "been sent yet or if the RHQ Server is down. It will not affect\\n\\\n"
        + "the agent under normal conditions (while running with the RHQ\\n\\\n"
        + "Server up and successfully communicating with the agent).\\n\\\n"
        + "In those unusual/rare conditions, having performance degradation\\n\\\n" + "may not be as important.") })
    String SETUP_INSTRUCTION_CLIENTSENDERCOMPRESSSPOOL_HELP = "PromptCommand.setup.instruction.sender.compressspool.help";

    // CLIENT SENDER SEND THROTTLING
    String SETUP_INSTRUCTION_CLIENTSENDERSENDTHROTTLING_PREF = AgentConfigurationConstants.CLIENT_SENDER_SEND_THROTTLING;
    String SETUP_INSTRUCTION_CLIENTSENDERSENDTHROTTLING_DEFAULT = "100:1000";
    @I18NMessages( { @I18NMessage("Send Throttling Parameters") })
    String SETUP_INSTRUCTION_CLIENTSENDERSENDTHROTTLING_PROMPT = "PromptCommand.setup.instruction.sender.sendthrottling.prompt";
    @I18NMessages( { @I18NMessage("If this setting is defined, it will enable send throttling to\\n\\\n"
        + "occur while sending commands to the server.  The format is\\n\\\n"
        + "defined as \'max-commands:quiet-period-milliseconds\'\\n\\\n"
        + "where the maximum commands defines the maximum number\\n\\\n"
        + "of commands that will be sent before the start of a quiet\\n\\\n"
        + "period.  The quiet period defines the number of milliseconds\\n\\\n"
        + "in which no commands should be sent.  After this duration\\n\\\n"
        + "expires, commands can again be sent, up to the maximum defined.\\n\\\n"
        + "Note that send throttling only affects those commands that\\n\\\n"
        + "are throttle-able.  Some commands are sent as soon as\\n\\\n"
        + "possible, regardless of the throttling settings.\\n\\\n"
        + "To disable send throttling, set this to its internal default.\\n\\\n"
        + "The maximum commands must be at least 1.\\n\\\n"
        + "The quiet period must be at least 100 milliseconds.\\n\\\n" + "\\n\\\n"
        + "This affects sending commands synchronously and asynchronously.") })
    String SETUP_INSTRUCTION_CLIENTSENDERSENDTHROTTLING_HELP = "PromptCommand.setup.instruction.sender.sendthrottling.help";

    // CLIENT SENDER QUEUE THROTTLING
    String SETUP_INSTRUCTION_CLIENTSENDERQTHROTTLING_PREF = AgentConfigurationConstants.CLIENT_SENDER_QUEUE_THROTTLING;
    String SETUP_INSTRUCTION_CLIENTSENDERQTHROTTLING_DEFAULT = "50:5000";
    @I18NMessages( { @I18NMessage("Queue Throttling Parameters") })
    String SETUP_INSTRUCTION_CLIENTSENDERQTHROTTLING_PROMPT = "PromptCommand.setup.instruction.sender.qthrottling.prompt";
    @I18NMessages( { @I18NMessage("If this setting is defined, it will enable queue throttling to\\n\\\n"
        + "occur while sending commands to the server.  The format is\\n\\\n"
        + "defined as \'max-commands-per-burst:burst-period-milliseconds\'\\n\\\n"
        + "where the maximum commands per burst defines the maximum number\\n\\\n"
        + "of commands that can be dequeued within a burst period.  The\\n\\\n"
        + "burst period defines the number of milliseconds in which the\\n\\\n"
        + "defined maximum number of commands can be dequeued.  If more\\n\\\n"
        + "than the maximum number of commands are queued within this\\n\\\n"
        + "time period, they will wait until the next burst period starts\\n\\\n"
        + "before being able to be dequeued.\\n\\\n" + "The maximum commands per burst must be at least 1.\\n\\\n"
        + "The burst period must be at least 100 milliseconds.\\n\\\n" + "\\n\\\n"
        + "This does not affect sending commands synchronously.  It only\\n\\\n"
        + "effects commands queued to be sent asynchronously.") })
    String SETUP_INSTRUCTION_CLIENTSENDERQTHROTTLING_HELP = "PromptCommand.setup.instruction.sender.qthrottling.help";

    // MULTICAST DETECTOR ENABLE FLAG
    String SETUP_INSTRUCTION_MULTICASTDETECTOR_PREF = ServiceContainerConfigurationConstants.MULTICASTDETECTOR_ENABLED;
    @I18NMessages( { @I18NMessage("If true, a multicast detector will be started within the agent.\\n\\\n"
        + "This service will listen for new remote servers coming online\\n\\\n"
        + "going offline.  This must be enabled if you enabled server auto-detection.\\n\\\n"
        + "If you enable this, your network must support multicast traffic.") })
    String SETUP_INSTRUCTION_MULTICASTDETECTOR_HELP = "PromptCommand.setup.instruction.multicastdetector.help";

    // MULTICAST DETECTOR MULTICAST ADDRESS
    String SETUP_INSTRUCTION_MULTICASTDETECTORMCADDR_PREF = ServiceContainerConfigurationConstants.MULTICASTDETECTOR_ADDRESS;
    String SETUP_INSTRUCTION_MULTICASTDETECTORMCADDR_DEFAULT = "224.16.16.16";
    @I18NMessages( { @I18NMessage("Multicast Detector Multicast Address") })
    String SETUP_INSTRUCTION_MULTICASTDETECTORMCADDR_PROMPT = "PromptCommand.setup.instruction.multicastdetector-mcaddr.prompt";
    @I18NMessages( { @I18NMessage("The multicast address used to broadcast detection messages.\\n\\\n"
        + "To be more specific, it is the IP address of the multicast group\\n\\\n" + "the detector will join.") })
    String SETUP_INSTRUCTION_MULTICASTDETECTORMCADDR_HELP = "PromptCommand.setup.instruction.multicastdetector-mcaddr.help";

    // MULTICAST DETECTOR BIND ADDRESS
    String SETUP_INSTRUCTION_MULTICASTDETECTORBINDADDR_PREF = ServiceContainerConfigurationConstants.MULTICASTDETECTOR_BINDADDRESS;
    String SETUP_INSTRUCTION_MULTICASTDETECTORBINDADDR_DEFAULT = "127.0.0.1";
    @I18NMessages( { @I18NMessage("Multicast Detector Bind Address") })
    String SETUP_INSTRUCTION_MULTICASTDETECTORBINDADDR_PROMPT = "PromptCommand.setup.instruction.multicastdetector-bindaddr.prompt";
    @I18NMessages( { @I18NMessage("The IP address that is bound by the network interface") })
    String SETUP_INSTRUCTION_MULTICASTDETECTORBINDADDR_HELP = "PromptCommand.setup.instruction.multicastdetector-bindaddr.help";

    // MULTICAST DETECTOR PORT
    String SETUP_INSTRUCTION_MULTICASTDETECTORPORT_PREF = ServiceContainerConfigurationConstants.MULTICASTDETECTOR_PORT;
    String SETUP_INSTRUCTION_MULTICASTDETECTORPORT_DEFAULT = "16162";
    @I18NMessages( { @I18NMessage("Multicast Detector Port") })
    String SETUP_INSTRUCTION_MULTICASTDETECTORPORT_PROMPT = "PromptCommand.setup.instruction.multicastdetector-port.prompt";
    @I18NMessages( { @I18NMessage("The port that is used to broadcast detection messages on via multicast.") })
    String SETUP_INSTRUCTION_MULTICASTDETECTORPORT_HELP = "PromptCommand.setup.instruction.multicastdetector-port.help";

    // MULTICAST DETECTOR DEFAULT TIME DELAY
    String SETUP_INSTRUCTION_MULTICASTDETECTORDEFAULTTIMEDELAY_PREF = ServiceContainerConfigurationConstants.MULTICASTDETECTOR_DEFAULT_TIMEDELAY;
    String SETUP_INSTRUCTION_MULTICASTDETECTORDEFAULTTIMEDELAY_DEFAULT = "5000";
    @I18NMessages( { @I18NMessage("Multicast Detector Server-Down Detection Default Time Delay") })
    String SETUP_INSTRUCTION_MULTICASTDETECTORDEFAULTTIMEDELAY_PROMPT = "PromptCommand.setup.instruction.multicastdetector-defaulttimedelay.prompt";
    @I18NMessages( { @I18NMessage("If no RHQ Server heartbeat message is received within this amount\\n\\\n"
        + "of milliseconds, it will be assumed the RHQ Server has gone down.\\n\\\n"
        + "This setting affects the timeliness of the auto-detection mechanism.") })
    String SETUP_INSTRUCTION_MULTICASTDETECTORDEFAULTTIMEDELAY_HELP = "PromptCommand.setup.instruction.multicastdetector-defaulttimedelay.help";

    // MULTICAST DETECTOR HEARTBEAT TIME DELAY
    String SETUP_INSTRUCTION_MULTICASTDETECTORHEARTBEATTIMEDELAY_PREF = ServiceContainerConfigurationConstants.MULTICASTDETECTOR_HEARTBEAT_TIMEDELAY;
    String SETUP_INSTRUCTION_MULTICASTDETECTORHEARTBEATTIMEDELAY_DEFAULT = "1000";
    @I18NMessages( { @I18NMessage("Multicast Detector Heartbeat Time Delay") })
    String SETUP_INSTRUCTION_MULTICASTDETECTORHEARTBEATTIMEDELAY_PROMPT = "PromptCommand.setup.instruction.multicastdetector-heartbeattimedelay.prompt";
    @I18NMessages( { @I18NMessage("This is the time delay that the agent will wait before it\\n\\\n"
        + "emits its own heartbeat message.") })
    String SETUP_INSTRUCTION_MULTICASTDETECTORHEARTBEATTIMEDELAY_HELP = "PromptCommand.setup.instruction.multicastdetector-heartbeattimedelay.help";

    // CONNECTOR TRANSPORT
    String SETUP_INSTRUCTION_CONNECTORTRANSPORT_PREF = ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT;
    String SETUP_INSTRUCTION_CONNECTORTRANSPORT_DEFAULT = ServiceContainerConfigurationConstants.DEFAULT_CONNECTOR_TRANSPORT;
    @I18NMessages( { @I18NMessage("Agent Transport Protocol") })
    String SETUP_INSTRUCTION_CONNECTORTRANSPORT_PROMPT = "PromptCommand.setup.instruction.connector.transport.prompt";
    @I18NMessages( { @I18NMessage("The transport that the agent expects incoming messages to flow over.\\n\\\n"
        + "Typical values are socket and sslsocket.") })
    String SETUP_INSTRUCTION_CONNECTORTRANSPORT_HELP = "PromptCommand.setup.instruction.connector.transport.help";

    // CONNECTOR ADDRESS
    String SETUP_INSTRUCTION_CONNECTORBINDADDR_PREF = ServiceContainerConfigurationConstants.CONNECTOR_BIND_ADDRESS;
    @I18NMessages( { @I18NMessage("Agent Hostname or IP Address") })
    String SETUP_INSTRUCTION_CONNECTORBINDADDR_PROMPT = "PromptCommand.setup.instruction.connector.bindaddr.prompt";
    @I18NMessages( { @I18NMessage("The hostname or IP address the agent will bind to in order to\\n\\\n"
        + "listen for incoming messages. Usually, you will want to leave this undefined so\\n\\\n"
        + "the agent picks up its default local IP address as it is currently configured\\n\\\n"
        + "in the operation system. If, for some reason, the operating system default IP\\n\\\n"
        + "address is not the one you want to use, then you can set an explicit value\\n\\\n"
        + "here. Realize that if you do this, and the agent later changes its assigned IP,\\n\\\n"
        + "you must reconfigure the agent to use the new IP; otherwise, the agent will not\\n\\\n"
        + "be able to communicate with the server.") })
    String SETUP_INSTRUCTION_CONNECTORBINDADDR_HELP = "PromptCommand.setup.instruction.connector.bindaddr.help";

    // CONNECTOR PORT
    String SETUP_INSTRUCTION_CONNECTORPORT_PREF = ServiceContainerConfigurationConstants.CONNECTOR_BIND_PORT;
    String SETUP_INSTRUCTION_CONNECTORPORT_DEFAULT = Integer
        .toString(ServiceContainerConfigurationConstants.DEFAULT_CONNECTOR_BIND_PORT);
    @I18NMessages( { @I18NMessage("Agent Port") })
    String SETUP_INSTRUCTION_CONNECTORPORT_PROMPT = "PromptCommand.setup.instruction.connector.bindport.prompt";
    @I18NMessages( { @I18NMessage("The port that the agent listens to for incoming messages.") })
    String SETUP_INSTRUCTION_CONNECTORPORT_HELP = "PromptCommand.setup.instruction.connector.bindport.help";

    // CONNECTOR TRANSPORT PARAMS
    String SETUP_INSTRUCTION_CONNECTORTRANSPORTPARAMS_PREF = ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT_PARAMS;
    String SETUP_INSTRUCTION_CONNECTORTRANSPORTPARAMS_DEFAULT = ServiceContainerConfigurationConstants.DEFAULT_CONNECTOR_TRANSPORT_PARAMS;
    @I18NMessages( { @I18NMessage("Agent Transport Parameters") })
    String SETUP_INSTRUCTION_CONNECTORTRANSPORTPARAMS_PROMPT = "PromptCommand.setup.instruction.connector.transportparams.prompt";
    @I18NMessages( { @I18NMessage("A set of transport parameters that is used to further configure\\n\\\n"
        + "the agent listener. Any value you provide here will overwrite (not augment)\\n\\\n"
        + "the current value. See the documentation for information on\\n\\\n"
        + "the format of this setting and all the different values allowed\\n\\\n"
        + "for the specific transport being used.") })
    String SETUP_INSTRUCTION_CONNECTORTRANSPORTPARAMS_HELP = "PromptCommand.setup.instruction.connector.transportparams.help";

    // CONNECTOR LEASE PERIOD
    String SETUP_INSTRUCTION_CONNECTORLEASE_PREF = ServiceContainerConfigurationConstants.CONNECTOR_LEASE_PERIOD;
    String SETUP_INSTRUCTION_CONNECTORLEASE_DEFAULT = "-1";
    @I18NMessages( { @I18NMessage("Agent Listener Lease Period") })
    String SETUP_INSTRUCTION_CONNECTORLEASE_PROMPT = "PromptCommand.setup.instruction.connector.leaseperiod.prompt";
    @I18NMessages( { @I18NMessage("The number of milliseconds that the RHQ Server has before it\\n\\\n"
        + "needs to update its lease with this agent.  This lease is only\\n\\\n"
        + "needed during a single connection with the agent.  Once the RHQ\\n\\\n"
        + "Server finishes sending the agent its message and closes its\\n\\\n"
        + "connection with the agent, the lease no longer needs updating.\\n\\\n"
        + "If this is 0 or less, the RHQ Server does not need a lease.") })
    String SETUP_INSTRUCTION_CONNECTORLEASE_HELP = "PromptCommand.setup.instruction.connector.leaseperiod.help";

    // REMOTE STREAM MAX IDLE TIME
    String SETUP_INSTRUCTION_STREAMIDLE_PREF = ServiceContainerConfigurationConstants.REMOTE_STREAM_MAX_IDLE_TIME;
    String SETUP_INSTRUCTION_STREAMIDLE_DEFAULT = Long
        .toString(ServiceContainerConfigurationConstants.DEFAULT_REMOTE_STREAM_MAX_IDLE_TIME);
    @I18NMessages( { @I18NMessage("Remote Stream Max Idle Time") })
    String SETUP_INSTRUCTION_STREAMIDLE_PROMPT = "PromptCommand.setup.instruction.streamidle.prompt";
    @I18NMessages( { @I18NMessage("The maximum amount of milliseconds a remoted stream\\n\\\n"
        + "is allowed to be idle before it is automatically closed and\\n\\\n"
        + "removed from the agent listener. This means that the\\n\\\n"
        + "RHQ Server must attempt to access the remoted stream\\n\\\n"
        + "every X milliseconds (where X is the value of this setting)\\n\\\n"
        + "or that stream will no longer be available. Note that this\\n\\\n"
        + "does not mean the RHQ Server must read or write the\\n\\\n"
        + "entire stream in this amount of time, it only means\\n\\\n"
        + "the RHQ Server must make a request on the stream every\\n\\\n"
        + "X milliseconds (be it to read or write one byte, see how many\\n\\\n"
        + "bytes are available to be read, etc).") })
    String SETUP_INSTRUCTION_STREAMIDLE_HELP = "PromptCommand.setup.instruction.streamidle.help";

    // SERVER-SIDE CLIENT AUTH MODE
    String SETUP_INSTRUCTION_SERVERCLIENTAUTHMODE_PREF = ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_CLIENT_AUTH_MODE;
    String SETUP_INSTRUCTION_SERVERCLIENTAUTHMODE_DEFAULT = ServiceContainerConfigurationConstants.DEFAULT_CONNECTOR_SECURITY_CLIENT_AUTH_MODE;
    @I18NMessages( { @I18NMessage("Client Authentication Mode") })
    String SETUP_INSTRUCTION_SERVERCLIENTAUTHMODE_PROMPT = "PromptCommand.setup.instruction.server-client-auth-mode.prompt";
    @I18NMessages( { @I18NMessage("Client-auth mode determines if the agent should authenticate some or all\\n\\\n"
        + "senders of incoming messages sent to the agent. If authentication is to be\\n\\\n"
        + "performed, the agent must have a server-side truststore.\\n\\\n"
        + "This preference value must be one of the following: none, want, need") })
    String SETUP_INSTRUCTION_SERVERCLIENTAUTHMODE_HELP = "PromptCommand.setup.instruction.server-client-auth-mode.help";

    // CLIENT-SIDE SERVER AUTH MODE ENABLED
    String SETUP_INSTRUCTION_CLIENTSERVERAUTHMODEENABLED_PREF = AgentConfigurationConstants.CLIENT_SENDER_SECURITY_SERVER_AUTH_MODE;
    String SETUP_INSTRUCTION_CLIENTSERVERAUTHMODEENABLED_DEFAULT = Boolean
        .toString(AgentConfigurationConstants.DEFAULT_CLIENT_SENDER_SECURITY_SERVER_AUTH_MODE);
    @I18NMessages( { @I18NMessage("Server Authentication Mode Enabled?") })
    String SETUP_INSTRUCTION_CLIENTSERVERAUTHMODEENABLED_PROMPT = "PromptCommand.setup.instruction.client-server-auth-mode-enabled.prompt";
    @I18NMessages( { @I18NMessage("When server authentication mode is true, the agent will attempt to\\n\\\n"
        + "authenticate the RHQ Server everytime it sends a message to it. If\\n\\\n"
        + "this is true, the agent must have a client-side truststore.") })
    String SETUP_INSTRUCTION_CLIENTSERVERAUTHMODEENABLED_HELP = "PromptCommand.setup.instruction.client-server-auth-mode-enabled.help";

    // SERVER-SIDE SECURE SOCKET PROTOCOL
    String SETUP_INSTRUCTION_SERVERSECUREPROTOCOL_PREF = ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_SOCKET_PROTOCOL;
    String SETUP_INSTRUCTION_SERVERSECUREPROTOCOL_DEFAULT = ServiceContainerConfigurationConstants.DEFAULT_CONNECTOR_SECURITY_SOCKET_PROTOCOL;
    @I18NMessages( { @I18NMessage("Incoming Secure Socket Protocol") })
    String SETUP_INSTRUCTION_SERVERSECUREPROTOCOL_PROMPT = "PromptCommand.setup.instruction.server-secure-protocol.prompt";
    @I18NMessages( { @I18NMessage("The secure protocol required when receiving messages from the RHQ Server.") })
    String SETUP_INSTRUCTION_SERVERSECUREPROTOCOL_HELP = "PromptCommand.setup.instruction.server-secure-protocol.help";

    // SERVER-SIDE KEYSTORE FILE
    String SETUP_INSTRUCTION_SERVERKEYSTOREFILE_PREF = ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_KEYSTORE_FILE;
    String SETUP_INSTRUCTION_SERVERKEYSTOREFILE_DEFAULT = ServiceContainerConfigurationConstants.DEFAULT_CONNECTOR_SECURITY_KEYSTORE_FILE_NAME;
    @I18NMessages( { @I18NMessage("Server-side Keystore File") })
    String SETUP_INSTRUCTION_SERVERKEYSTOREFILE_PROMPT = "PromptCommand.setup.instruction.server-keystore-file.prompt";
    @I18NMessages( { @I18NMessage("The agent server-side keystore file that contains a key that is sent to\\n\\\n"
        + "the RHQ Server when the server is sending a message to the agent.\\n\\\n"
        + "This keystore contains the key that identifies the agent and is\\n\\\n"
        + "used if the RHQ Server has its \"server authentication mode\" enabled.\\n\\\n"
        + "This can be the same as the agent client-side keystore file.") })
    String SETUP_INSTRUCTION_SERVERKEYSTOREFILE_HELP = "PromptCommand.setup.instruction.server-keystore-file.help";

    // SERVER-SIDE KEYSTORE ALGORITHM
    String SETUP_INSTRUCTION_SERVERKEYSTOREALGORITHM_PREF = ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_KEYSTORE_ALGORITHM;
    String SETUP_INSTRUCTION_SERVERKEYSTOREALGORITHM_DEFAULT = ServiceContainerConfigurationConstants.DEFAULT_CONNECTOR_SECURITY_KEYSTORE_ALGORITHM;
    @I18NMessages( { @I18NMessage("Server-side Keystore Algorithm") })
    String SETUP_INSTRUCTION_SERVERKEYSTOREALGORITHM_PROMPT = "PromptCommand.setup.instruction.server-keystore-alg.prompt";
    @I18NMessages( { @I18NMessage("The algorithm used to generate the server-side keystore key.") })
    String SETUP_INSTRUCTION_SERVERKEYSTOREALGORITHM_HELP = "PromptCommand.setup.instruction.server-keystore-alg.help";

    // SERVER-SIDE KEYSTORE TYPE
    String SETUP_INSTRUCTION_SERVERKEYSTORETYPE_PREF = ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_KEYSTORE_TYPE;
    String SETUP_INSTRUCTION_SERVERKEYSTORETYPE_DEFAULT = ServiceContainerConfigurationConstants.DEFAULT_CONNECTOR_SECURITY_KEYSTORE_TYPE;
    @I18NMessages( { @I18NMessage("Server-side Keystore Type") })
    String SETUP_INSTRUCTION_SERVERKEYSTORETYPE_PROMPT = "PromptCommand.setup.instruction.server-keystore-type.prompt";
    @I18NMessages( { @I18NMessage("Identifies the server-side keystore file format implementation.") })
    String SETUP_INSTRUCTION_SERVERKEYSTORETYPE_HELP = "PromptCommand.setup.instruction.server-keystore-type.help";

    // SERVER-SIDE KEYSTORE PASSWORD
    String SETUP_INSTRUCTION_SERVERKEYSTOREPASSWORD_PREF = ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_KEYSTORE_PASSWORD;
    String SETUP_INSTRUCTION_SERVERKEYSTOREPASSWORD_DEFAULT = "rhqpwd";
    @I18NMessages( { @I18NMessage("Server-side Keystore Password") })
    String SETUP_INSTRUCTION_SERVERKEYSTOREPASSWORD_PROMPT = "PromptCommand.setup.instruction.server-keystore-pw.prompt";
    @I18NMessages( { @I18NMessage("The password to access the server-side keystore.") })
    String SETUP_INSTRUCTION_SERVERKEYSTOREPASSWORD_HELP = "PromptCommand.setup.instruction.server-keystore-pw.help";

    // SERVER-SIDE KEYSTORE KEY PASSWORD
    String SETUP_INSTRUCTION_SERVERKEYSTOREKEYPASSWORD_PREF = ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_KEYSTORE_KEY_PASSWORD;
    String SETUP_INSTRUCTION_SERVERKEYSTOREKEYPASSWORD_DEFAULT = "rhqpwd";
    @I18NMessages( { @I18NMessage("Server-side Keystore Key Password") })
    String SETUP_INSTRUCTION_SERVERKEYSTOREKEYPASSWORD_PROMPT = "PromptCommand.setup.instruction.server-keystore-key-pw.prompt";
    @I18NMessages( { @I18NMessage("The password to access the key in the server-side keystore.") })
    String SETUP_INSTRUCTION_SERVERKEYSTOREKEYPASSWORD_HELP = "PromptCommand.setup.instruction.server-keystore-key-pw.help";

    // SERVER-SIDE KEYSTORE ALIAS
    String SETUP_INSTRUCTION_SERVERKEYSTOREALIAS_PREF = ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_KEYSTORE_ALIAS;
    String SETUP_INSTRUCTION_SERVERKEYSTOREALIAS_DEFAULT = ServiceContainerConfigurationConstants.DEFAULT_CONNECTOR_SECURITY_KEYSTORE_ALIAS;
    @I18NMessages( { @I18NMessage("Server-side Keystore Key Alias") })
    String SETUP_INSTRUCTION_SERVERKEYSTOREALIAS_PROMPT = "PromptCommand.setup.instruction.server-keystore-alias.prompt";
    @I18NMessages( { @I18NMessage("The alias of the key in the server-side keystore used to identify the agent.") })
    String SETUP_INSTRUCTION_SERVERKEYSTOREALIAS_HELP = "PromptCommand.setup.instruction.server-keystore-alias.help";

    // SERVER-SIDE TRUSTSTORE FILE
    String SETUP_INSTRUCTION_SERVERTRUSTSTOREFILE_PREF = ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_TRUSTSTORE_FILE;
    String SETUP_INSTRUCTION_SERVERTRUSTSTOREFILE_DEFAULT = ServiceContainerConfigurationConstants.DEFAULT_CONNECTOR_SECURITY_TRUSTSTORE_FILE_NAME;
    @I18NMessages( { @I18NMessage("Server-side Truststore File") })
    String SETUP_INSTRUCTION_SERVERTRUSTSTOREFILE_PROMPT = "PromptCommand.setup.instruction.server-truststore-file.prompt";
    @I18NMessages( { @I18NMessage("The agent server-side truststore file contains keys of trusted\\n\\\n"
        + "RHQ Servers that are allowed to send the agent incoming messages. This is\\n\\\n"
        + "used if the agent's client-auth mode is set to something other than none.\\n\\\n"
        + "This can be the same as the agent client-side truststore file.") })
    String SETUP_INSTRUCTION_SERVERTRUSTSTOREFILE_HELP = "PromptCommand.setup.instruction.server-truststore-file.help";

    // SERVER-SIDE TRUSTSTORE ALGORITHM
    String SETUP_INSTRUCTION_SERVERTRUSTSTOREALGORITHM_PREF = ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_TRUSTSTORE_ALGORITHM;
    String SETUP_INSTRUCTION_SERVERTRUSTSTOREALGORITHM_DEFAULT = ServiceContainerConfigurationConstants.DEFAULT_CONNECTOR_SECURITY_TRUSTSTORE_ALGORITHM;
    @I18NMessages( { @I18NMessage("Server-side Truststore Algorithm") })
    String SETUP_INSTRUCTION_SERVERTRUSTSTOREALGORITHM_PROMPT = "PromptCommand.setup.instruction.server-truststore-alg.prompt";
    @I18NMessages( { @I18NMessage("The algorithm used to generate the server-side truststore keys.") })
    String SETUP_INSTRUCTION_SERVERTRUSTSTOREALGORITHM_HELP = "PromptCommand.setup.instruction.server-truststore-alg.help";

    // SERVER-SIDE TRUSTSTORE TYPE
    String SETUP_INSTRUCTION_SERVERTRUSTSTORETYPE_PREF = ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_TRUSTSTORE_TYPE;
    String SETUP_INSTRUCTION_SERVERTRUSTSTORETYPE_DEFAULT = ServiceContainerConfigurationConstants.DEFAULT_CONNECTOR_SECURITY_TRUSTSTORE_TYPE;
    @I18NMessages( { @I18NMessage("Server-side Truststore Type") })
    String SETUP_INSTRUCTION_SERVERTRUSTSTORETYPE_PROMPT = "PromptCommand.setup.instruction.server-truststore-type.prompt";
    @I18NMessages( { @I18NMessage("Identifies the server-side truststore file format implementation.") })
    String SETUP_INSTRUCTION_SERVERTRUSTSTORETYPE_HELP = "PromptCommand.setup.instruction.server-truststore-type.help";

    // SERVER-SIDE TRUSTSTORE PASSWORD
    String SETUP_INSTRUCTION_SERVERTRUSTSTOREPASSWORD_PREF = ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_TRUSTSTORE_PASSWORD;
    String SETUP_INSTRUCTION_SERVERTRUSTSTOREPASSWORD_DEFAULT = "";
    @I18NMessages( { @I18NMessage("Server-side Truststore Password") })
    String SETUP_INSTRUCTION_SERVERTRUSTSTOREPASSWORD_PROMPT = "PromptCommand.setup.instruction.server-truststore-pw.prompt";
    @I18NMessages( { @I18NMessage("The password used to access the server-side truststore.") })
    String SETUP_INSTRUCTION_SERVERTRUSTSTOREPASSWORD_HELP = "PromptCommand.setup.instruction.server-truststore-pw.help";

    // CLIENT-SIDE SECURE SOCKET PROTOCOL
    String SETUP_INSTRUCTION_CLIENTSECUREPROTOCOL_PREF = AgentConfigurationConstants.CLIENT_SENDER_SECURITY_SOCKET_PROTOCOL;
    String SETUP_INSTRUCTION_CLIENTSECUREPROTOCOL_DEFAULT = AgentConfigurationConstants.DEFAULT_CLIENT_SENDER_SECURITY_SOCKET_PROTOCOL;
    @I18NMessages( { @I18NMessage("Outgoing Secure Socket Protocol") })
    String SETUP_INSTRUCTION_CLIENTSECUREPROTOCOL_PROMPT = "PromptCommand.setup.instruction.client-secure-protocol.prompt";
    @I18NMessages( { @I18NMessage("The secure protocol required when sending messages to the RHQ Server.") })
    String SETUP_INSTRUCTION_CLIENTSECUREPROTOCOL_HELP = "PromptCommand.setup.instruction.client-secure-protocol.help";

    // CLIENT-SIDE KEYSTORE FILE
    String SETUP_INSTRUCTION_CLIENTKEYSTOREFILE_PREF = AgentConfigurationConstants.CLIENT_SENDER_SECURITY_KEYSTORE_FILE;
    String SETUP_INSTRUCTION_CLIENTKEYSTOREFILE_DEFAULT = AgentConfigurationConstants.DEFAULT_CLIENT_SENDER_SECURITY_KEYSTORE_FILE_NAME;
    @I18NMessages( { @I18NMessage("Client-side Keystore File") })
    String SETUP_INSTRUCTION_CLIENTKEYSTOREFILE_PROMPT = "PromptCommand.setup.instruction.client-keystore-file.prompt";
    @I18NMessages( { @I18NMessage("The agent client-side keystore file that contains a key that is sent to\\n\\\n"
        + "the RHQ Server when the agent sends a message to the server.\\n\\\n"
        + "This keystore contains the key that identifies the agent and is\\n\\\n"
        + "used if the RHQ Server has its \"client authentication mode\" set.\\n\\\n"
        + "This can be the same as the agent server-side keystore file.") })
    String SETUP_INSTRUCTION_CLIENTKEYSTOREFILE_HELP = "PromptCommand.setup.instruction.client-keystore-file.help";

    // CLIENT-SIDE KEYSTORE ALGORITHM
    String SETUP_INSTRUCTION_CLIENTKEYSTOREALGORITHM_PREF = AgentConfigurationConstants.CLIENT_SENDER_SECURITY_KEYSTORE_ALGORITHM;
    String SETUP_INSTRUCTION_CLIENTKEYSTOREALGORITHM_DEFAULT = AgentConfigurationConstants.DEFAULT_CLIENT_SENDER_SECURITY_KEYSTORE_ALGORITHM;
    @I18NMessages( { @I18NMessage("Client-side Keystore Algorithm") })
    String SETUP_INSTRUCTION_CLIENTKEYSTOREALGORITHM_PROMPT = "PromptCommand.setup.instruction.client-keystore-alg.prompt";
    @I18NMessages( { @I18NMessage("The algorithm used to generate the client-side keystore key.") })
    String SETUP_INSTRUCTION_CLIENTKEYSTOREALGORITHM_HELP = "PromptCommand.setup.instruction.client-keystore-alg.help";

    // CLIENT-SIDE KEYSTORE TYPE
    String SETUP_INSTRUCTION_CLIENTKEYSTORETYPE_PREF = AgentConfigurationConstants.CLIENT_SENDER_SECURITY_KEYSTORE_TYPE;
    String SETUP_INSTRUCTION_CLIENTKEYSTORETYPE_DEFAULT = AgentConfigurationConstants.DEFAULT_CLIENT_SENDER_SECURITY_KEYSTORE_TYPE;
    @I18NMessages( { @I18NMessage("Client-side Keystore Type") })
    String SETUP_INSTRUCTION_CLIENTKEYSTORETYPE_PROMPT = "PromptCommand.setup.instruction.client-keystore-type.prompt";
    @I18NMessages( { @I18NMessage("Identifies the client-side keystore file format implementation.") })
    String SETUP_INSTRUCTION_CLIENTKEYSTORETYPE_HELP = "PromptCommand.setup.instruction.client-keystore-type.help";

    // CLIENT-SIDE KEYSTORE PASSWORD
    String SETUP_INSTRUCTION_CLIENTKEYSTOREPASSWORD_PREF = AgentConfigurationConstants.CLIENT_SENDER_SECURITY_KEYSTORE_PASSWORD;
    String SETUP_INSTRUCTION_CLIENTKEYSTOREPASSWORD_DEFAULT = "rhqpwd";
    @I18NMessages( { @I18NMessage("Client-side Keystore Password") })
    String SETUP_INSTRUCTION_CLIENTKEYSTOREPASSWORD_PROMPT = "PromptCommand.setup.instruction.client-keystore-pw.prompt";
    @I18NMessages( { @I18NMessage("The password to access the client-side keystore.") })
    String SETUP_INSTRUCTION_CLIENTKEYSTOREPASSWORD_HELP = "PromptCommand.setup.instruction.client-keystore-pw.help";

    // CLIENT-SIDE KEYSTORE KEY PASSWORD
    String SETUP_INSTRUCTION_CLIENTKEYSTOREKEYPASSWORD_PREF = AgentConfigurationConstants.CLIENT_SENDER_SECURITY_KEYSTORE_KEY_PASSWORD;
    String SETUP_INSTRUCTION_CLIENTKEYSTOREKEYPASSWORD_DEFAULT = "rhqpwd";
    @I18NMessages( { @I18NMessage("Client-side Keystore Key Password") })
    String SETUP_INSTRUCTION_CLIENTKEYSTOREKEYPASSWORD_PROMPT = "PromptCommand.setup.instruction.client-keystore-key-pw.prompt";
    @I18NMessages( { @I18NMessage("The password to access the key in the client-side keystore.") })
    String SETUP_INSTRUCTION_CLIENTKEYSTOREKEYPASSWORD_HELP = "PromptCommand.setup.instruction.client-keystore-key-pw.help";

    // CLIENT-SIDE KEYSTORE ALIAS
    String SETUP_INSTRUCTION_CLIENTKEYSTOREALIAS_PREF = AgentConfigurationConstants.CLIENT_SENDER_SECURITY_KEYSTORE_ALIAS;
    String SETUP_INSTRUCTION_CLIENTKEYSTOREALIAS_DEFAULT = AgentConfigurationConstants.DEFAULT_CLIENT_SENDER_SECURITY_KEYSTORE_ALIAS;
    @I18NMessages( { @I18NMessage("Client-side Keystore Key Alias") })
    String SETUP_INSTRUCTION_CLIENTKEYSTOREALIAS_PROMPT = "PromptCommand.setup.instruction.client-keystore-alias.prompt";
    @I18NMessages( { @I18NMessage("The alias of the key in the client-side keystore used to identify the agent.") })
    String SETUP_INSTRUCTION_CLIENTKEYSTOREALIAS_HELP = "PromptCommand.setup.instruction.client-keystore-alias.help";

    // CLIENT-SIDE TRUSTSTORE FILE
    String SETUP_INSTRUCTION_CLIENTTRUSTSTOREFILE_PREF = AgentConfigurationConstants.CLIENT_SENDER_SECURITY_TRUSTSTORE_FILE;
    String SETUP_INSTRUCTION_CLIENTTRUSTSTOREFILE_DEFAULT = AgentConfigurationConstants.DEFAULT_CLIENT_SENDER_SECURITY_TRUSTSTORE_FILE_NAME;
    @I18NMessages( { @I18NMessage("Client-side Truststore File") })
    String SETUP_INSTRUCTION_CLIENTTRUSTSTOREFILE_PROMPT = "PromptCommand.setup.instruction.client-truststore-file.prompt";
    @I18NMessages( { @I18NMessage("The agent client-side truststore file contains keys of trusted\\n\\\n"
        + "RHQ Servers to which the agent is allowed to send outgoing messages. This is\\n\\\n"
        + "used if the agent's server authentication mode is enabled.\\n\\\n"
        + "This can be the same as the agent server-side truststore file.") })
    String SETUP_INSTRUCTION_CLIENTTRUSTSTOREFILE_HELP = "PromptCommand.setup.instruction.client-truststore-file.help";

    // CLIENT-SIDE TRUSTSTORE ALGORITHM
    String SETUP_INSTRUCTION_CLIENTTRUSTSTOREALGORITHM_PREF = AgentConfigurationConstants.CLIENT_SENDER_SECURITY_TRUSTSTORE_ALGORITHM;
    String SETUP_INSTRUCTION_CLIENTTRUSTSTOREALGORITHM_DEFAULT = AgentConfigurationConstants.DEFAULT_CLIENT_SENDER_SECURITY_TRUSTSTORE_ALGORITHM;
    @I18NMessages( { @I18NMessage("Client-side Truststore Algorithm") })
    String SETUP_INSTRUCTION_CLIENTTRUSTSTOREALGORITHM_PROMPT = "PromptCommand.setup.instruction.client-truststore-alg.prompt";
    @I18NMessages( { @I18NMessage("The algorithm used to generate the client-side truststore keys.") })
    String SETUP_INSTRUCTION_CLIENTTRUSTSTOREALGORITHM_HELP = "PromptCommand.setup.instruction.client-truststore-alg.help";

    // CLIENT-SIDE TRUSTSTORE TYPE
    String SETUP_INSTRUCTION_CLIENTTRUSTSTORETYPE_PREF = AgentConfigurationConstants.CLIENT_SENDER_SECURITY_TRUSTSTORE_TYPE;
    String SETUP_INSTRUCTION_CLIENTTRUSTSTORETYPE_DEFAULT = AgentConfigurationConstants.DEFAULT_CLIENT_SENDER_SECURITY_TRUSTSTORE_TYPE;
    @I18NMessages( { @I18NMessage("Client-side Truststore Type") })
    String SETUP_INSTRUCTION_CLIENTTRUSTSTORETYPE_PROMPT = "PromptCommand.setup.instruction.client-truststore-type.prompt";
    @I18NMessages( { @I18NMessage("Identifies the client-side truststore file format implementation.") })
    String SETUP_INSTRUCTION_CLIENTTRUSTSTORETYPE_HELP = "PromptCommand.setup.instruction.client-truststore-type.help";

    // CLIENT-SIDE TRUSTSTORE PASSWORD
    String SETUP_INSTRUCTION_CLIENTTRUSTSTOREPASSWORD_PREF = AgentConfigurationConstants.CLIENT_SENDER_SECURITY_TRUSTSTORE_PASSWORD;
    String SETUP_INSTRUCTION_CLIENTTRUSTSTOREPASSWORD_DEFAULT = "";
    @I18NMessages( { @I18NMessage("Client-side Truststore Password") })
    String SETUP_INSTRUCTION_CLIENTTRUSTSTOREPASSWORD_PROMPT = "PromptCommand.setup.instruction.client-truststore-pw.prompt";
    @I18NMessages( { @I18NMessage("The password used to access the client-side truststore.") })
    String SETUP_INSTRUCTION_CLIENTTRUSTSTOREPASSWORD_HELP = "PromptCommand.setup.instruction.client-truststore-pw.help";

    // PLUGINS DIRECTORY
    String SETUP_INSTRUCTION_PLUGINSDIR_PREF = AgentConfigurationConstants.PLUGINS_DIRECTORY;
    String SETUP_INSTRUCTION_PLUGINSDIR_DEFAULT = AgentConfigurationConstants.DEFAULT_PLUGINS_DIRECTORY;
    @I18NMessages( { @I18NMessage("Plugins Directory") })
    String SETUP_INSTRUCTION_PLUGINSDIR_PROMPT = "PromptCommand.setup.instruction.plugins.directory.prompt";
    @I18NMessages( { @I18NMessage("The directory location where the plugins can be found.") })
    String SETUP_INSTRUCTION_PLUGINSDIR_HELP = "PromptCommand.setup.instruction.plugins.directory.help";

    // PLUGINS SERVER DISCOVERY PERIOD
    String SETUP_INSTRUCTION_PLUGINSSERVERDISCOVERYPERIOD_PREF = AgentConfigurationConstants.PLUGINS_SERVER_DISCOVERY_PERIOD;
    String SETUP_INSTRUCTION_PLUGINSSERVERDISCOVERYPERIOD_DEFAULT = Long
        .toString(AgentConfigurationConstants.DEFAULT_PLUGINS_SERVER_DISCOVERY_PERIOD);
    @I18NMessages( { @I18NMessage("Server Discovery Scan Period") })
    String SETUP_INSTRUCTION_PLUGINSSERVERDISCOVERYPERIOD_PROMPT = "PromptCommand.setup.instruction.plugins.server-discovery-period.prompt";
    @I18NMessages( { @I18NMessage("The time in seconds that defines how often a server discovery\\n\\\n"
        + "scan is performed. A server discovery looks for new servers\\n\\\n" + "that can be imported into inventory.") })
    String SETUP_INSTRUCTION_PLUGINSSERVERDISCOVERYPERIOD_HELP = "PromptCommand.setup.instruction.plugins.server-discovery-period.help";

    // PLUGINS SERVER DISCOVERY INITIAL DELAY
    String SETUP_INSTRUCTION_PLUGINSSERVERDISCOVERYINITIALDELAY_PREF = AgentConfigurationConstants.PLUGINS_SERVER_DISCOVERY_INITIAL_DELAY;
    String SETUP_INSTRUCTION_PLUGINSSERVERDISCOVERYINITIALDELAY_DEFAULT = Long
        .toString(AgentConfigurationConstants.DEFAULT_PLUGINS_SERVER_DISCOVERY_INITIAL_DELAY);
    @I18NMessages( { @I18NMessage("Server Discovery Scan Initial Delay") })
    String SETUP_INSTRUCTION_PLUGINSSERVERDISCOVERYINITIALDELAY_PROMPT = "PromptCommand.setup.instruction.plugins.server-discovery-initialdelay.prompt";
    @I18NMessages( { @I18NMessage("The time in seconds before the initial server discovery scan is performed.") })
    String SETUP_INSTRUCTION_PLUGINSSERVERDISCOVERYINITIALDELAY_HELP = "PromptCommand.setup.instruction.plugins.server-discovery-initialdelay.help";

    // PLUGINS SERVICE DISCOVERY PERIOD
    String SETUP_INSTRUCTION_PLUGINSSERVICEDISCOVERYPERIOD_PREF = AgentConfigurationConstants.PLUGINS_SERVICE_DISCOVERY_PERIOD;
    String SETUP_INSTRUCTION_PLUGINSSERVICEDISCOVERYPERIOD_DEFAULT = Long
        .toString(AgentConfigurationConstants.DEFAULT_PLUGINS_SERVICE_DISCOVERY_PERIOD);
    @I18NMessages( { @I18NMessage("Service Discovery Scan Period") })
    String SETUP_INSTRUCTION_PLUGINSSERVICEDISCOVERYPERIOD_PROMPT = "PromptCommand.setup.instruction.plugins.service-discovery-period.prompt";
    @I18NMessages( { @I18NMessage("The time in seconds that defines how often a service discovery scan is\\n\\\n"
        + "performed. A service discovery scan looks for resources that were\\n\\\n"
        + "added or removed from existing platform and server resources.") })
    String SETUP_INSTRUCTION_PLUGINSSERVICEDISCOVERYPERIOD_HELP = "PromptCommand.setup.instruction.plugins.service-discovery-period.help";

    // PLUGINS SERVICE DISCOVERY INITIAL DELAY
    String SETUP_INSTRUCTION_PLUGINSSERVICEDISCOVERYINITIALDELAY_PREF = AgentConfigurationConstants.PLUGINS_SERVICE_DISCOVERY_INITIAL_DELAY;
    String SETUP_INSTRUCTION_PLUGINSSERVICEDISCOVERYINITIALDELAY_DEFAULT = Long
        .toString(AgentConfigurationConstants.DEFAULT_PLUGINS_SERVICE_DISCOVERY_INITIAL_DELAY);
    @I18NMessages( { @I18NMessage("Service Discovery Scan Initial Delay") })
    String SETUP_INSTRUCTION_PLUGINSSERVICEDISCOVERYINITIALDELAY_PROMPT = "PromptCommand.setup.instruction.plugins.service-discovery-initialdelay.prompt";
    @I18NMessages( { @I18NMessage("The time in seconds before the initial service discovery scan is performed.") })
    String SETUP_INSTRUCTION_PLUGINSSERVICEDISCOVERYINITIALDELAY_HELP = "PromptCommand.setup.instruction.plugins.service-discovery-initialdelay.help";

    // PLUGINS AVAILABILITY SCAN PERIOD
    String SETUP_INSTRUCTION_PLUGINSAVAILSCANPERIOD_PREF = AgentConfigurationConstants.PLUGINS_AVAILABILITY_SCAN_PERIOD;
    String SETUP_INSTRUCTION_PLUGINSAVAILSCANPERIOD_DEFAULT = Long
        .toString(AgentConfigurationConstants.DEFAULT_PLUGINS_AVAILABILITY_SCAN_PERIOD);
    @I18NMessages( { @I18NMessage("Availability Scan Period") })
    String SETUP_INSTRUCTION_PLUGINSAVAILSCANPERIOD_PROMPT = "PromptCommand.setup.instruction.plugins.avail-scan-period.prompt";
    @I18NMessages( { @I18NMessage("The time in seconds that defines how often an availability scan is\\n\\\n"
        + "performed. An availability scan looks to determine what resources\\n\\\n"
        + "are up and running and what resources have gone down.") })
    String SETUP_INSTRUCTION_PLUGINSAVAILSCANPERIOD_HELP = "PromptCommand.setup.instruction.plugins.avail-scan-period.help";

    // PLUGINS AVAILABILITY SCAN INITIAL DELAY
    String SETUP_INSTRUCTION_PLUGINSAVAILSCANINITIALDELAY_PREF = AgentConfigurationConstants.PLUGINS_AVAILABILITY_SCAN_INITIAL_DELAY;
    String SETUP_INSTRUCTION_PLUGINSAVAILSCANINITIALDELAY_DEFAULT = Long
        .toString(AgentConfigurationConstants.DEFAULT_PLUGINS_AVAILABILITY_SCAN_INITIAL_DELAY);
    @I18NMessages( { @I18NMessage("Availability Scan Initial Delay") })
    String SETUP_INSTRUCTION_PLUGINSAVAILSCANINITIALDELAY_PROMPT = "PromptCommand.setup.instruction.plugins.avail-scan-initialdelay.prompt";
    @I18NMessages( { @I18NMessage("The time in seconds before the initial availability scan is performed.") })
    String SETUP_INSTRUCTION_PLUGINSAVAILSCANINITIALDELAY_HELP = "PromptCommand.setup.instruction.plugins.avail-scan-initialdelay.help";

    // PLUGINS AVAILABILITY SCAN THREAD POOL SIZE
    String SETUP_INSTRUCTION_PLUGINSAVAILSCANTHREADPOOLSIZE_PREF = AgentConfigurationConstants.PLUGINS_AVAILABILITY_SCAN_THREADPOOL_SIZE;
    String SETUP_INSTRUCTION_PLUGINSAVAILSCANTHREADPOOLSIZE_DEFAULT = Integer
        .toString(AgentConfigurationConstants.DEFAULT_PLUGINS_AVAILABILITY_SCAN_THREADPOOL_SIZE);
    @I18NMessages({ @I18NMessage("Availability Scan ThreadPool Size") })
    String SETUP_INSTRUCTION_PLUGINSAVAILSCANTHREADPOOLSIZE_PROMPT = "PromptCommand.setup.instruction.plugins.avail-scan-threadpoolsize.prompt";
    @I18NMessages({ @I18NMessage("The number of threads that can concurrently scan resource availabilities.") })
    String SETUP_INSTRUCTION_PLUGINSAVAILSCANTHREADPOOLSIZE_HELP = "PromptCommand.setup.instruction.plugins.avail-scan-threadpoolsize.help";

    // PLUGINS MEASUREMENT COLLECTION INITIAL DELAY
    String SETUP_INSTRUCTION_PLUGINSMEASUREMENTCOLLINITIALDELAY_PREF = AgentConfigurationConstants.PLUGINS_MEASUREMENT_COLLECTION_INITIAL_DELAY;
    String SETUP_INSTRUCTION_PLUGINSMEASUREMENTCOLLINITIALDELAY_DEFAULT = Long
        .toString(AgentConfigurationConstants.DEFAULT_PLUGINS_MEASUREMENT_COLLECTION_INITIAL_DELAY);
    @I18NMessages( { @I18NMessage("Measurement Collection Initial Delay") })
    String SETUP_INSTRUCTION_PLUGINSMEASUREMENTCOLLINITIALDELAY_PROMPT = "PromptCommand.setup.instruction.plugins.meas-coll-initialdelay.prompt";
    @I18NMessages( { @I18NMessage("The time in seconds before the initial measurement collection is performed.") })
    String SETUP_INSTRUCTION_PLUGINSMEASUREMENTCOLLINITIALDELAY_HELP = "PromptCommand.setup.instruction.plugins.meas-coll-initialdelay.help";

    // REGISTER WITH SERVER AT STARTUP
    String SETUP_INSTRUCTION_REGISTERWITHSERVERATSTARTUP_PREF = AgentConfigurationConstants.REGISTER_WITH_SERVER_AT_STARTUP;
    String SETUP_INSTRUCTION_REGISTERWITHSERVERATSTARTUP_DEFAULT = Boolean
        .toString(AgentConfigurationConstants.DEFAULT_REGISTER_WITH_SERVER_AT_STARTUP);
    @I18NMessages( { @I18NMessage("Register With RHQ Server at Startup") })
    String SETUP_INSTRUCTION_REGISTERWITHSERVERATSTARTUP_PROMPT = "PromptCommand.setup.instruction.registerwithserver.prompt";
    @I18NMessages( { @I18NMessage("If true, the agent will automatically attempt to register\\n\\\n"
        + "itself with the RHQ Server when the agent starts up.\\n\\\n"
        + "If false, you must ensure the agent is either already registered\\n\\\n"
        + "or will be manually registered (see the register prompt command).") })
    String SETUP_INSTRUCTION_REGISTERWITHSERVERATSTARTUP_HELP = "PromptCommand.setup.instruction.registerwithserver.help";

    // WAIT FOR SEVER AT STARTUP MSECS
    String SETUP_INSTRUCTION_WAITFORSERVERATSTARTUPMSECS_PREF = AgentConfigurationConstants.WAIT_FOR_SERVER_AT_STARTUP_MSECS;
    String SETUP_INSTRUCTION_WAITFORSERVERATSTARTUPMSECS_DEFAULT = Long
        .toString(AgentConfigurationConstants.DEFAULT_WAIT_FOR_SERVER_AT_STARTUP_MSECS);
    @I18NMessages( { @I18NMessage("Wait Time for RHQ Server at Startup") })
    String SETUP_INSTRUCTION_WAITFORSERVERATSTARTUPMSECS_PROMPT = "PromptCommand.setup.instruction.waitforservermsecs.prompt";
    @I18NMessages( { @I18NMessage("This defines how many milliseconds the agent should wait at\\n\\\n"
        + "startup for the RHQ Server to be detected. If the RHQ Server\\n\\\n"
        + "has not started up in the given amount of time, the agent will\\n\\\n"
        + "continue initializing and expect the server to come up later.\\n\\\n"
        + "If this is 0, the agent will not wait at all.") })
    String SETUP_INSTRUCTION_WAITFORSERVERATSTARTUPMSECS_HELP = "PromptCommand.setup.instruction.waitforservermsecs.help";

    // PRIMARY SERVER SWITCHOVER CHECK INTERVAL MSECS
    String SETUP_INSTRUCTION_PRIMARYSERVERSWITCHOVERCHECKINTERVAL_PREF = AgentConfigurationConstants.PRIMARY_SERVER_SWITCHOVER_CHECK_INTERVAL_MSECS;
    String SETUP_INSTRUCTION_PRIMARYSERVERSWITCHOVERCHECKINTERVAL_DEFAULT = Long
        .toString(AgentConfigurationConstants.DEFAULT_PRIMARY_SERVER_SWITCHOVER_CHECK_INTERVAL_MSECS);
    @I18NMessages( { @I18NMessage("Primary Server Switchover Check Interval:") })
    String SETUP_INSTRUCTION_PRIMARYSERVERSWITCHOVERCHECKINTERVAL_PROMPT = "PromptCommand.setup.instruction.primaryserverswitchovercheckinterval.prompt";
    @I18NMessages( { @I18NMessage("The agent will periodically check to ensure that the server\\n\\\n"
        + "it is connected to is its primary server (as opposed to one\\n\\\n"
        + "of its failover servers). This preference defines how many\\n\\\n"
        + "milliseconds the agent should wait in between these checks.\\n\\\n"
        + "If 0, this check is never performed.\\n\\\n") })
    String SETUP_INSTRUCTION_PRIMARYSERVERSWITCHOVERCHECKINTERVAL_HELP = "PromptCommand.setup.instruction.primaryserverswitchovercheckinterval.help";

    // DISABLE NATIVE SYSTEM
    String SETUP_INSTRUCTION_DISABLENATIVESYSTEM_PREF = AgentConfigurationConstants.DISABLE_NATIVE_SYSTEM;
    String SETUP_INSTRUCTION_DISABLENATIVESYSTEM_DEFAULT = Boolean
        .toString(AgentConfigurationConstants.DEFAULT_DISABLE_NATIVE_SYSTEM);
    @I18NMessages( { @I18NMessage("Disable Native System") })
    String SETUP_INSTRUCTION_DISABLENATIVESYSTEM_PROMPT = "PromptCommand.setup.instruction.disablenativesystem.prompt";
    @I18NMessages( { @I18NMessage("This will allow you to tell the agent to disable the native system,\\n\\\n"
        + "thus turning off the usage of the native libraries (if they are\\n\\\n"
        + "available on the agent platform).  In fact, disabling the native system\\n\\\n"
        + "will ensure that the native JNI libraries are not even loaded into the\\n\\\n"
        + "agent's Java VM. You normally do not want to disable the native system\\n\\\n"
        + "unless you have a good reason to do so. Disabling the native system will\\n\\\n"
        + "turn off the ability of the plugins to perform auto-discovery using\\n\\\n"
        + "process table scans and will not allow the plugins to obtain\\n\\\n"
        + "any information from the low-level operating system resources.") })
    String SETUP_INSTRUCTION_DISABLENATIVESYSTEM_HELP = "PromptCommand.setup.instruction.disablenativesystem.help";

    // UPDATE PLUGINS AT STARTUP
    String SETUP_INSTRUCTION_UPDATEPLUGINSATSTARTUP_PREF = AgentConfigurationConstants.UPDATE_PLUGINS_AT_STARTUP;
    String SETUP_INSTRUCTION_UPDATEPLUGINSATSTARTUP_DEFAULT = Boolean
        .toString(AgentConfigurationConstants.DEFAULT_UPDATE_PLUGINS_AT_STARTUP);
    @I18NMessages( { @I18NMessage("Update Plugins at Startup") })
    String SETUP_INSTRUCTION_UPDATEPLUGINSATSTARTUP_PROMPT = "PromptCommand.setup.instruction.updateplugins.prompt";
    @I18NMessages( { @I18NMessage("If true, the agent will attempt to update its current set of plugins to their\\n\\\n"
        + "latest versions at startup. If false, the agent will not automatically update\\n\\\n"
        + "the plugins; the agent will use its current plugins.") })
    String SETUP_INSTRUCTION_UPDATEPLUGINSATSTARTUP_HELP = "PromptCommand.setup.instruction.updateplugins.help";

    // TEST FAILOVER LIST AT STARTUP
    String SETUP_INSTRUCTION_TESTFAILOVERLISTATSTARTUP_PREF = AgentConfigurationConstants.TEST_FAILOVER_LIST_AT_STARTUP;
    String SETUP_INSTRUCTION_TESTFAILOVERLISTATSTARTUP_DEFAULT = Boolean
        .toString(AgentConfigurationConstants.DEFAULT_TEST_FAILOVER_LIST_AT_STARTUP);
    @I18NMessages({ @I18NMessage("Test Failover List at Startup") })
    String SETUP_INSTRUCTION_TESTFAILOVERLISTATSTARTUP_PROMPT = "PromptCommand.setup.instruction.testfailoverlist.prompt";
    @I18NMessages({ @I18NMessage("If true, the agent will attempt to connect to all servers found\\n\\\n"
        + "in its failover list. Warning messages will be logged if errors occur while\\n\\\n"
        + "attempting to connect to one or more servers.") })
    String SETUP_INSTRUCTION_TESTFAILOVERLISTATSTARTUP_HELP = "PromptCommand.setup.instruction.testfailoverlist.help";

    // AGENT UPDATE ENABLED
    String SETUP_INSTRUCTION_AGENTUPDATEENABLED_PREF = AgentConfigurationConstants.AGENT_UPDATE_ENABLED;
    String SETUP_INSTRUCTION_AGENTUPDATEENABLED_DEFAULT = Boolean
        .toString(AgentConfigurationConstants.DEFAULT_AGENT_UPDATE_ENABLED);
    @I18NMessages( { @I18NMessage("Enable Agent Updates") })
    String SETUP_INSTRUCTION_AGENTUPDATEENABLED_PROMPT = "PromptCommand.setup.instruction.agentupdateenabled.prompt";
    @I18NMessages( { @I18NMessage("If true, the agent will be allowed to apply agent updates to itself.\\n\\\n"
        + "If false, the agent will never apply updates; therefore any agent updates\\n\\\n" + "must be done manually.") })
    String SETUP_INSTRUCTION_AGENTUPDATEENABLED_HELP = "PromptCommand.setup.instruction.agentupdateenabled.help";

    // AGENT UPDATE VERSION URL
    String SETUP_INSTRUCTION_AGENTUPDATEVERSIONURL_PREF = AgentConfigurationConstants.AGENT_UPDATE_VERSION_URL;
    String SETUP_INSTRUCTION_AGENTUPDATEVERSIONURL_DEFAULT = null;
    @I18NMessages( { @I18NMessage("Agent Update Version URL") })
    String SETUP_INSTRUCTION_AGENTUPDATEVERSIONURL_PROMPT = "PromptCommand.setup.instruction.agentupdateversionurl.prompt";
    @I18NMessages( { @I18NMessage("If this is defined, it will be the URL the agent uses when it\\n\\\n"
        + "needs to retrieve information about the latest available\\n\\\n"
        + "agent update binary.  If this is not defined (i.e. left as 'null'),\\n\\\n"
        + "the default will be a URL to the server the agent is currently\\n\\\n"
        + "connected to at the time the version request is initiated. If you have\\n\\\n"
        + "this set but wish to revert back to the default behavior,\\n\\\n" + "enter !* at the prompt.") })
    String SETUP_INSTRUCTION_AGENTUPDATEVERSIONURL_HELP = "PromptCommand.setup.instruction.agentupdateversionurl.help";

    // AGENT UPDATE DOWNLOAD URL
    String SETUP_INSTRUCTION_AGENTUPDATEDOWNLOADURL_PREF = AgentConfigurationConstants.AGENT_UPDATE_DOWNLOAD_URL;
    String SETUP_INSTRUCTION_AGENTUPDATEDOWNLOADURL_DEFAULT = null;
    @I18NMessages( { @I18NMessage("Agent Update Download URL") })
    String SETUP_INSTRUCTION_AGENTUPDATEDOWNLOADURL_PROMPT = "PromptCommand.setup.instruction.agentupdatedownloadurl.prompt";
    @I18NMessages( { @I18NMessage("If this is defined, it will be the URL the agent uses when it\\n\\\n"
        + "needs to download the latest available agent update binary.\\n\\\n"
        + "If this is not defined (i.e. left as 'null'), the default will be\\n\\\n"
        + "a URL to the server the agent is currently connected to at the time the\\n\\\n"
        + "download is initiated. If you have this set but wish to revert back\\n\\\n"
        + "to the default behavior, enter !* at the prompt.") })
    String SETUP_INSTRUCTION_AGENTUPDATEDOWNLOADURL_HELP = "PromptCommand.setup.instruction.agentupdatedownloadurl.help";

    // AGENT NAME
    String SETUP_INSTRUCTION_AGENTNAME_PREF = AgentConfigurationConstants.NAME;
    @I18NMessages( { @I18NMessage("Agent Name") })
    String SETUP_INSTRUCTION_AGENTNAME_PROMPT = "PromptCommand.setup.instruction.agentname.prompt";
    @I18NMessages( { @I18NMessage("The name that this agent is to be known as.  This must be unique across\\n\\\n"
        + "all agents.  The default is the fully qualified domain name of the host\\n\\\n"
        + "that this agent is running on. However, you can name it anything you\\n\\\n"
        + "want, as long as it is unique among all other agents in the system.") })
    String SETUP_INSTRUCTION_AGENTNAME_HELP = "PromptCommand.setup.instruction.agentname.help";

    // PLUGINS MEASUREMENT COLLECTION THREADPOOL SIZE
    String SETUP_INSTRUCTION_PCMEASUREMENTTHREADCOUNT_PREF = AgentConfigurationConstants.PLUGINS_MEASUREMENT_COLL_THREADPOOL_SIZE;
    String SETUP_INSTRUCTION_PCMEASUREMENTTHREADCOUNT_DEFAULT = Integer
        .toString(AgentConfigurationConstants.DEFAULT_PLUGINS_MEASUREMENT_COLL_THREADPOOL_SIZE);
    @I18NMessages( { @I18NMessage("Measurement Collection Thread Pool Size") })
    String SETUP_INSTRUCTION_PCMEASUREMENTTHREADCOUNT_PROMPT = "PromptCommand.setup.instruction.measurementthreadcount.prompt";
    @I18NMessages( { @I18NMessage("This defines the number of threads within the plugin container's measurement\\n\\\n"
        + "collection thread pool.  The higher the number, the more measurements that\\n\\\n"
        + "can be collected concurrently.") })
    String SETUP_INSTRUCTION_PCMEASUREMENTTHREADCOUNT_HELP = "PromptCommand.setup.instruction.measurementthreadcount.help";

    // PLUGINS OPERATION INVOKER THREADPOOL SIZE
    String SETUP_INSTRUCTION_PCOPERATIONTHREADCOUNT_PREF = AgentConfigurationConstants.PLUGINS_OPERATION_INVOKER_THREADPOOL_SIZE;
    String SETUP_INSTRUCTION_PCOPERATIONTHREADCOUNT_DEFAULT = Integer
        .toString(AgentConfigurationConstants.DEFAULT_PLUGINS_OPERATION_INVOKER_THREADPOOL_SIZE);
    @I18NMessages( { @I18NMessage("Operation Invoker Thread Pool Size") })
    String SETUP_INSTRUCTION_PCOPERATIONTHREADCOUNT_PROMPT = "PromptCommand.setup.instruction.operationthreadcount.prompt";
    @I18NMessages( { @I18NMessage("This defines the number of threads within the plugin container's operation\\n\\\n"
        + "invoker thread pool.  The higher the number, the more operations that\\n\\\n"
        + "can be concurrently invoked.") })
    String SETUP_INSTRUCTION_PCOPERATIONTHREADCOUNT_HELP = "PromptCommand.setup.instruction.operationthreadcount.help";

    // PLUGINS OPERATION INVOCATION TIMEOUT
    String SETUP_INSTRUCTION_PCOPINVOCATIONTIMEOUT_PREF = AgentConfigurationConstants.PLUGINS_OPERATION_INVOCATION_TIMEOUT;
    String SETUP_INSTRUCTION_PCOPINVOCATIONTIMEOUT_DEFAULT = Long
        .toString(AgentConfigurationConstants.DEFAULT_PLUGINS_OPERATION_INVOCATION_TIMEOUT);
    @I18NMessages( { @I18NMessage("Operation Invocation Timeout") })
    String SETUP_INSTRUCTION_PCOPINVOCATIONTIMEOUT_PROMPT = "PromptCommand.setup.instruction.opinvocationtimeout.prompt";
    @I18NMessages( { @I18NMessage("This is the default timeout used for operation invocations. When a plugin\\n\\\n"
        + "invokes an operation on a managed resource, and that invocation does not\\n\\\n"
        + "finish with this amount of seconds, the invocation will be aborted.\\n\\\n"
        + "Note that this is only a default; a plugin may actually override this value\\n\\\n"
        + "by defining its own timeouts within its plugin descriptor.") })
    String SETUP_INSTRUCTION_PCOPINVOCATIONTIMEOUT_HELP = "PromptCommand.setup.instruction.opinvocationtimeout.help";

    // PLUGINS DRIFT DETECTION PERIOD
    String SETUP_INSTRUCTION_PLUGINSDRIFTDETECTIONPERIOD_PREF = AgentConfigurationConstants.PLUGINS_DRIFT_DETECTION_PERIOD;
    String SETUP_INSTRUCTION_PLUGINSDRIFTDETECTIONPERIOD_DEFAULT = Long
        .toString(AgentConfigurationConstants.DEFAULT_PLUGINS_DRIFT_DETECTION_PERIOD);
    @I18NMessages( { @I18NMessage("Drift Detection Period") })
    String SETUP_INSTRUCTION_PLUGINSDRIFTDETECTIONPERIOD_PROMPT = "PromptCommand.setup.instruction.plugins.driftdetection-period.prompt";
    @I18NMessages( { @I18NMessage("The time in seconds that defines how often drift detection scans are run.\\n\\\n"
        + "If 0 or less, drift discovery scans are disabled.") })
    String SETUP_INSTRUCTION_PLUGINSDRIFTDETECTIONPERIOD_HELP = "PromptCommand.setup.instruction.plugins.driftdiscovery-period.help";

    // PLUGINS DRIFT DETECTION INITIAL DELAY
    String SETUP_INSTRUCTION_PLUGINSDRIFTDETECTIONINITIALDELAY_PREF = AgentConfigurationConstants.PLUGINS_DRIFT_DETECTION_INITIAL_DELAY;
    String SETUP_INSTRUCTION_PLUGINSDRIFTDETECTIONINITIALDELAY_DEFAULT = Long
        .toString(AgentConfigurationConstants.DEFAULT_PLUGINS_DRIFT_DETECTION_INITIAL_DELAY);
    @I18NMessages( { @I18NMessage("Drift Detection Initial Delay") })
    String SETUP_INSTRUCTION_PLUGINSDRIFTDETECTIONINITIALDELAY_PROMPT = "PromptCommand.setup.instruction.plugins.driftdetection-initialdelay.prompt";
    @I18NMessages( { @I18NMessage("The time in seconds before the initial drift detection scan is performed.") })
    String SETUP_INSTRUCTION_PLUGINSDRIFTDETECTIONINITIALDELAY_HELP = "PromptCommand.setup.instruction.plugins.driftdetection-initialdelay.help";

    // PLUGINS CONTENT DISCOVERY THREADPOOL SIZE
    String SETUP_INSTRUCTION_PLUGINSCONTENTDISCOVERYTHREADCOUNT_PREF = AgentConfigurationConstants.PLUGINS_CONTENT_DISCOVERY_THREADPOOL_SIZE;
    String SETUP_INSTRUCTION_PLUGINSCONTENTDISCOVERYTHREADCOUNT_DEFAULT = Integer
        .toString(AgentConfigurationConstants.DEFAULT_PLUGINS_CONTENT_DISCOVERY_THREADPOOL_SIZE);
    @I18NMessages( { @I18NMessage("Content Discovery Thread Pool Size") })
    String SETUP_INSTRUCTION_PLUGINSCONTENTDISCOVERYTHREADCOUNT_PROMPT = "PromptCommand.setup.instruction.contentdiscoverythreadcount.prompt";
    @I18NMessages( { @I18NMessage("This defines the number of threads within the plugin container's content\\n\\\n"
        + "discovery thread pool. The higher the number, the more content discoveries\\n\\\n"
        + "that can be collected concurrently.") })
    String SETUP_INSTRUCTION_PLUGINSCONTENTDISCOVERYTHREADCOUNT_HELP = "PromptCommand.setup.instruction.contentdiscoverythreadcount.help";

    // PLUGINS CONTENT DISCOVERY PERIOD
    String SETUP_INSTRUCTION_PLUGINSCONTENTDISCOVERYPERIOD_PREF = AgentConfigurationConstants.PLUGINS_CONTENT_DISCOVERY_PERIOD;
    String SETUP_INSTRUCTION_PLUGINSCONTENTDISCOVERYPERIOD_DEFAULT = Long
        .toString(AgentConfigurationConstants.DEFAULT_PLUGINS_CONTENT_DISCOVERY_PERIOD);
    @I18NMessages( { @I18NMessage("Content Discovery Period") })
    String SETUP_INSTRUCTION_PLUGINSCONTENTDISCOVERYPERIOD_PROMPT = "PromptCommand.setup.instruction.plugins.contentdiscovery-period.prompt";
    @I18NMessages( { @I18NMessage("The time in seconds that defines how often content discoveries are run.\\n\\\n"
        + "If 0 or less, content discovery is disabled.") })
    String SETUP_INSTRUCTION_PLUGINSCONTENTDISCOVERYPERIOD_HELP = "PromptCommand.setup.instruction.plugins.contentdiscovery-period.help";

    // PLUGINS CONTENT DISCOVERY INITIAL DELAY
    String SETUP_INSTRUCTION_PLUGINSCONTENTDISCOVERYINITIALDELAY_PREF = AgentConfigurationConstants.PLUGINS_CONTENT_DISCOVERY_INITIAL_DELAY;
    String SETUP_INSTRUCTION_PLUGINSCONTENTDISCOVERYINITIALDELAY_DEFAULT = Long
        .toString(AgentConfigurationConstants.DEFAULT_PLUGINS_CONTENT_DISCOVERY_INITIAL_DELAY);
    @I18NMessages( { @I18NMessage("Content Discovery Initial Delay") })
    String SETUP_INSTRUCTION_PLUGINSCONTENTDISCOVERYINITIALDELAY_PROMPT = "PromptCommand.setup.instruction.plugins.contentdiscovery-initialdelay.prompt";
    @I18NMessages( { @I18NMessage("The time in seconds before the initial content discovery is performed.") })
    String SETUP_INSTRUCTION_PLUGINSCONTENTDISCOVERYINITIALDELAY_HELP = "PromptCommand.setup.instruction.plugins.contentdiscovery-initialdelay.help";

    // PLUGINS CONFIGURATION DISCOVERY PERIOD
    String SETUP_INSTRUCTION_PLUGINSCONFIGURATIONDISCOVERYPERIOD_PREF = AgentConfigurationConstants.PLUGINS_CONFIGURATION_DISCOVERY_PERIOD;
    String SETUP_INSTRUCTION_PLUGINSCONFIGURATIONDISCOVERYPERIOD_DEFAULT = Long
        .toString(AgentConfigurationConstants.DEFAULT_PLUGINS_CONFIGURATION_DISCOVERY_PERIOD);
    @I18NMessages( { @I18NMessage("Configuration Discovery Period") })
    String SETUP_INSTRUCTION_PLUGINSCONFIGURATIONDISCOVERYPERIOD_PROMPT = "PromptCommand.setup.instruction.plugins.configurationdiscovery-period.prompt";
    @I18NMessages( { @I18NMessage("The time in seconds that defines how often configuration discoveries are run.\\n\\\n"
        + "If 0 or less, configuration discovery is disabled.") })
    String SETUP_INSTRUCTION_PLUGINSCONFIGURATIONDISCOVERYPERIOD_HELP = "PromptCommand.setup.instruction.plugins.configurationdiscovery-period.help";

    // PLUGINS CONFIGURATION DISCOVERY INITIAL DELAY
    String SETUP_INSTRUCTION_PLUGINSCONFIGURATIONDISCOVERYINITIALDELAY_PREF = AgentConfigurationConstants.PLUGINS_CONFIGURATION_DISCOVERY_INITIAL_DELAY;
    String SETUP_INSTRUCTION_PLUGINSCONFIGURATIONDISCOVERYINITIALDELAY_DEFAULT = Long
        .toString(AgentConfigurationConstants.DEFAULT_PLUGINS_CONFIGURATION_DISCOVERY_INITIAL_DELAY);
    @I18NMessages( { @I18NMessage("Configuration Discovery Initial Delay") })
    String SETUP_INSTRUCTION_PLUGINSCONFIGURATIONDISCOVERYINITIALDELAY_PROMPT = "PromptCommand.setup.instruction.plugins.configurationdiscovery-initialdelay.prompt";
    @I18NMessages( { @I18NMessage("The time in seconds before the initial configuration discovery is performed.") })
    String SETUP_INSTRUCTION_PLUGINSCONFIGURATIONDISCOVERYINITIALDELAY_HELP = "PromptCommand.setup.instruction.plugins.configurationdiscovery-initialdelay.help";

    // PLUGINS EVENT SENDER PERIOD
    String SETUP_INSTRUCTION_PLUGINSEVENTSENDERPERIOD_PREF = AgentConfigurationConstants.PLUGINS_EVENT_SENDER_PERIOD;
    String SETUP_INSTRUCTION_PLUGINSEVENTSENDERPERIOD_DEFAULT = Long
        .toString(AgentConfigurationConstants.DEFAULT_PLUGINS_EVENT_SENDER_PERIOD);
    @I18NMessages( { @I18NMessage("Event Sender Period") })
    String SETUP_INSTRUCTION_PLUGINSEVENTSENDERPERIOD_PROMPT = "PromptCommand.setup.instruction.plugins.eventsender-period.prompt";
    @I18NMessages( { @I18NMessage("Defines how often event reports get sent to the server (in seconds).") })
    String SETUP_INSTRUCTION_PLUGINSEVENTSENDERPERIOD_HELP = "PromptCommand.setup.instruction.plugins.eventsender-period.help";

    // PLUGINS EVENT SENDER INITIAL DELAY
    String SETUP_INSTRUCTION_PLUGINSEVENTSENDERINITIALDELAY_PREF = AgentConfigurationConstants.PLUGINS_EVENT_SENDER_INITIAL_DELAY;
    String SETUP_INSTRUCTION_PLUGINSEVENTSENDERINITIALDELAY_DEFAULT = Long
        .toString(AgentConfigurationConstants.DEFAULT_PLUGINS_EVENT_SENDER_INITIAL_DELAY);
    @I18NMessages( { @I18NMessage("Event Sender Initial Delay") })
    String SETUP_INSTRUCTION_PLUGINSEVENTSENDERINITIALDELAY_PROMPT = "PromptCommand.setup.instruction.plugins.eventsender-initialdelay.prompt";
    @I18NMessages( { @I18NMessage("The delay, in seconds, before the first event report gets sent to the server.") })
    String SETUP_INSTRUCTION_PLUGINSEVENTSENDERINITIALDELAY_HELP = "PromptCommand.setup.instruction.plugins.eventsender-initialdelay.help";

    // PLUGINS EVENT REPORT MAX PER SOURCE
    String SETUP_INSTRUCTION_PLUGINSEVENTREPORTMAXPERSRC_PREF = AgentConfigurationConstants.PLUGINS_EVENT_REPORT_MAX_PER_SOURCE;
    String SETUP_INSTRUCTION_PLUGINSEVENTREPORTMAXPERSRC_DEFAULT = Integer
        .toString(AgentConfigurationConstants.DEFAULT_PLUGINS_EVENT_REPORT_MAX_PER_SOURCE);
    @I18NMessages( { @I18NMessage("Event Report Max Per Source") })
    String SETUP_INSTRUCTION_PLUGINSEVENTREPORTMAXPERSRC_PROMPT = "PromptCommand.setup.instruction.plugins.eventreport-maxpersrc.prompt";
    @I18NMessages( { @I18NMessage("The maximum number of events for any given event source that can be placed\\n\\\n"
        + "in a single event report that is sent up to the server. If this number is\\n\\\n"
        + "larger than the max-total setting, then this setting is ignored.") })
    String SETUP_INSTRUCTION_PLUGINSEVENTREPORTMAXPERSRC_HELP = "PromptCommand.setup.instruction.plugins.eventreport-maxpersrc.help";

    // PLUGINS EVENT REPORT MAX TOTAL
    String SETUP_INSTRUCTION_PLUGINSEVENTREPORTMAXTOTAL_PREF = AgentConfigurationConstants.PLUGINS_EVENT_REPORT_MAX_TOTAL;
    String SETUP_INSTRUCTION_PLUGINSEVENTREPORTMAXTOTAL_DEFAULT = Integer
        .toString(AgentConfigurationConstants.DEFAULT_PLUGINS_EVENT_REPORT_MAX_TOTAL);
    @I18NMessages( { @I18NMessage("Event Report Max Total") })
    String SETUP_INSTRUCTION_PLUGINSEVENTREPORTMAXTOTAL_PROMPT = "PromptCommand.setup.instruction.plugins.eventreport-maxtotal.prompt";
    @I18NMessages( { @I18NMessage("The total maximum number of events that can be placed in a single event\\n\\\n"
        + "report that is sent up to the server.") })
    String SETUP_INSTRUCTION_PLUGINSEVENTREPORTMAXTOTAL_HELP = "PromptCommand.setup.instruction.plugins.eventreport-maxtotal.help";

    // VM HEALTH CHECK INTERVAL
    String SETUP_INSTRUCTION_VMHEALTHCHECKINTERVAL_PREF = AgentConfigurationConstants.VM_HEALTH_CHECK_INTERVAL_MSECS;
    String SETUP_INSTRUCTION_VMHEALTHCHECKINTERVAL_DEFAULT = Long
        .toString(AgentConfigurationConstants.DEFAULT_VM_HEALTH_CHECK_INTERVAL_MSECS);
    @I18NMessages( { @I18NMessage("VM Health Check Interval") })
    String SETUP_INSTRUCTION_VMHEALTHCHECKINTERVAL_PROMPT = "PromptCommand.setup.instruction.vm-health-check.interval.prompt";
    @I18NMessages( { @I18NMessage("The time in milliseconds in between checking the VM's health.") })
    String SETUP_INSTRUCTION_VMHEALTHCHECKINTERVAL_HELP = "PromptCommand.setup.instruction.vm-health-check.interval.help";

    // VM HEALTH CHECK LOW HEAP MEM THRESHOLD
    String SETUP_INSTRUCTION_VMHEALTHCHECKLOWHEAPMEMTHRESHOLD_PREF = AgentConfigurationConstants.VM_HEALTH_CHECK_LOW_HEAP_MEM_THRESHOLD;
    String SETUP_INSTRUCTION_VMHEALTHCHECKLOWHEAPMEMTHRESHOLD_DEFAULT = Float
        .toString(AgentConfigurationConstants.DEFAULT_VM_HEALTH_CHECK_LOW_HEAP_MEM_THRESHOLD);
    @I18NMessages( { @I18NMessage("VM Health Check Low Heap Mem Threshold") })
    String SETUP_INSTRUCTION_VMHEALTHCHECKLOWHEAPMEMTHRESHOLD_PROMPT = "PromptCommand.setup.instruction.vm-health-check.low-heap-mem-threshold.prompt";
    @I18NMessages( { @I18NMessage("The threshold percentage that must be crossed if the agent's VM health check\\n\\\n"
        + "is to consider the JVM with critically low memory. This value is a percentage\\n\\\n"
        + "of used heap memory out of the maximum heap size.") })
    String SETUP_INSTRUCTION_VMHEALTHCHECKLOWHEAPMEMTHRESHOLD_HELP = "PromptCommand.setup.instruction.vm-health-check.low-heap-mem-threshold.help";

    // VM HEALTH CHECK LOW NONHEAP MEM THRESHOLD
    String SETUP_INSTRUCTION_VMHEALTHCHECKLOWNONHEAPMEMTHRESHOLD_PREF = AgentConfigurationConstants.VM_HEALTH_CHECK_LOW_NONHEAP_MEM_THRESHOLD;
    String SETUP_INSTRUCTION_VMHEALTHCHECKLOWNONHEAPMEMTHRESHOLD_DEFAULT = Float
        .toString(AgentConfigurationConstants.DEFAULT_VM_HEALTH_CHECK_LOW_NONHEAP_MEM_THRESHOLD);
    @I18NMessages( { @I18NMessage("VM Health Check Low Non-Heap Mem Threshold") })
    String SETUP_INSTRUCTION_VMHEALTHCHECKLOWNONHEAPMEMTHRESHOLD_PROMPT = "PromptCommand.setup.instruction.vm-health-check.low-nonheap-mem-threshold.prompt";
    @I18NMessages( { @I18NMessage("The threshold percentage that must be crossed if the agent's VM health check\\n\\\n"
        + "is to consider the JVM with critically low memory. This value is a percentage\\n\\\n"
        + "of used nonheap memory out of the maximum nonheap size.") })
    String SETUP_INSTRUCTION_VMHEALTHCHECKLOWNONHEAPMEMTHRESHOLD_HELP = "PromptCommand.setup.instruction.vm-health-check.low-nonheap-mem-threshold.help";
}