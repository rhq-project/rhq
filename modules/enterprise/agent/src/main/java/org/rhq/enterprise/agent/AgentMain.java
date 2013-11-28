/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
package org.rhq.enterprise.agent;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StreamTokenizer;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.prefs.BackingStoreException;
import java.util.prefs.InvalidPreferencesFormatException;
import java.util.prefs.Preferences;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import mazz.i18n.Logger;
import mazz.i18n.Msg;
import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.xml.DOMConfigurator;

import org.jboss.remoting.invocation.NameBasedInvocation;
import org.jboss.remoting.security.SSLSocketBuilder;
import org.jboss.remoting.transport.http.ssl.HTTPSClientInvoker;

import org.rhq.core.clientapi.server.bundle.BundleServerService;
import org.rhq.core.clientapi.server.configuration.ConfigurationServerService;
import org.rhq.core.clientapi.server.content.ContentServerService;
import org.rhq.core.clientapi.server.core.AgentNotSupportedException;
import org.rhq.core.clientapi.server.core.AgentRegistrationException;
import org.rhq.core.clientapi.server.core.AgentRegistrationRequest;
import org.rhq.core.clientapi.server.core.AgentRegistrationResults;
import org.rhq.core.clientapi.server.core.AgentVersion;
import org.rhq.core.clientapi.server.core.ConnectAgentRequest;
import org.rhq.core.clientapi.server.core.ConnectAgentResults;
import org.rhq.core.clientapi.server.core.CoreServerService;
import org.rhq.core.clientapi.server.core.PingRequest;
import org.rhq.core.clientapi.server.discovery.DiscoveryServerService;
import org.rhq.core.clientapi.server.drift.DriftServerService;
import org.rhq.core.clientapi.server.event.EventServerService;
import org.rhq.core.clientapi.server.inventory.ResourceFactoryServerService;
import org.rhq.core.clientapi.server.measurement.MeasurementServerService;
import org.rhq.core.clientapi.server.operation.OperationServerService;
import org.rhq.core.domain.cloud.composite.FailoverListComposite;
import org.rhq.core.domain.cloud.composite.FailoverListComposite.ServerEntry;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.RebootRequestListener;
import org.rhq.core.pc.ServerServices;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pc.plugin.FileSystemPluginFinder;
import org.rhq.core.pc.util.LoggingThreadFactory;
import org.rhq.core.system.SystemInfoFactory;
import org.rhq.core.util.ObjectNameFactory;
import org.rhq.core.util.StringPropertyReplacer;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.enterprise.agent.AgentRestartCounter.AgentRestartReason;
import org.rhq.enterprise.agent.AgentUtils.ServerEndpoint;
import org.rhq.enterprise.agent.i18n.AgentI18NFactory;
import org.rhq.enterprise.agent.i18n.AgentI18NResourceKeys;
import org.rhq.enterprise.agent.promptcmd.AgentPromptCommand;
import org.rhq.enterprise.agent.promptcmd.AvailabilityPromptCommand;
import org.rhq.enterprise.agent.promptcmd.ConfigPromptCommand;
import org.rhq.enterprise.agent.promptcmd.DebugPromptCommand;
import org.rhq.enterprise.agent.promptcmd.DiscoveryPromptCommand;
import org.rhq.enterprise.agent.promptcmd.DownloadPromptCommand;
import org.rhq.enterprise.agent.promptcmd.DumpSpoolPromptCommand;
import org.rhq.enterprise.agent.promptcmd.ExitPromptCommand;
import org.rhq.enterprise.agent.promptcmd.FailoverPromptCommand;
import org.rhq.enterprise.agent.promptcmd.GCPromptCommand;
import org.rhq.enterprise.agent.promptcmd.GetConfigPromptCommand;
import org.rhq.enterprise.agent.promptcmd.HelpPromptCommand;
import org.rhq.enterprise.agent.promptcmd.IdentifyPromptCommand;
import org.rhq.enterprise.agent.promptcmd.InventoryPromptCommand;
import org.rhq.enterprise.agent.promptcmd.ListDataPromptCommand;
import org.rhq.enterprise.agent.promptcmd.LogPromptCommand;
import org.rhq.enterprise.agent.promptcmd.MetricsPromptCommand;
import org.rhq.enterprise.agent.promptcmd.NativePromptCommand;
import org.rhq.enterprise.agent.promptcmd.PingPromptCommand;
import org.rhq.enterprise.agent.promptcmd.PiqlPromptCommand;
import org.rhq.enterprise.agent.promptcmd.PluginContainerPromptCommand;
import org.rhq.enterprise.agent.promptcmd.PluginsPromptCommand;
import org.rhq.enterprise.agent.promptcmd.RegisterPromptCommand;
import org.rhq.enterprise.agent.promptcmd.SchedulesPromptCommand;
import org.rhq.enterprise.agent.promptcmd.SenderPromptCommand;
import org.rhq.enterprise.agent.promptcmd.SetConfigPromptCommand;
import org.rhq.enterprise.agent.promptcmd.SetupPromptCommand;
import org.rhq.enterprise.agent.promptcmd.ShutdownPromptCommand;
import org.rhq.enterprise.agent.promptcmd.SleepPromptCommand;
import org.rhq.enterprise.agent.promptcmd.StartPromptCommand;
import org.rhq.enterprise.agent.promptcmd.UpdatePromptCommand;
import org.rhq.enterprise.agent.promptcmd.VersionPromptCommand;
import org.rhq.enterprise.agent.promptcmd.aliases.QuitPromptCommand;
import org.rhq.enterprise.communications.Ping;
import org.rhq.enterprise.communications.ServiceContainer;
import org.rhq.enterprise.communications.ServiceContainerConfiguration;
import org.rhq.enterprise.communications.ServiceContainerConfigurationConstants;
import org.rhq.enterprise.communications.ServiceContainerSenderCreationListener;
import org.rhq.enterprise.communications.command.Command;
import org.rhq.enterprise.communications.command.CommandResponse;
import org.rhq.enterprise.communications.command.client.ClientCommandSender;
import org.rhq.enterprise.communications.command.client.ClientCommandSenderConfiguration;
import org.rhq.enterprise.communications.command.client.ClientCommandSenderStateListener;
import org.rhq.enterprise.communications.command.client.ClientRemotePojoFactory;
import org.rhq.enterprise.communications.command.client.CommandPreprocessor;
import org.rhq.enterprise.communications.command.client.JBossRemotingRemoteCommunicator;
import org.rhq.enterprise.communications.command.client.OutgoingCommandTrace;
import org.rhq.enterprise.communications.command.client.RemoteCommunicator;
import org.rhq.enterprise.communications.command.impl.remotepojo.RemotePojoInvocationCommand;
import org.rhq.enterprise.communications.command.server.CommandListener;
import org.rhq.enterprise.communications.command.server.IncomingCommandTrace;
import org.rhq.enterprise.communications.util.CommandTraceUtil;
import org.rhq.enterprise.communications.util.SecurityUtil;

/**
 * The main class of the agent runtime container.
 *
 * @author John Mazzitelli
 */
public class AgentMain {
    private static final Logger LOG = AgentI18NFactory.getLogger(AgentMain.class);

    /**
     * The I18N message builder that creates localized messages for the user.
     */
    private static final Msg MSG = AgentI18NFactory.getMsg();

    // set up our user input prompts that will be used to also show the agent status
    private static final String PROMPT_SHUTDOWN = MSG.getMsg(AgentI18NResourceKeys.PROMPT_STRING_SHUTDOWN) + "> ";
    private static final String PROMPT_STARTED = MSG.getMsg(AgentI18NResourceKeys.PROMPT_STRING_STARTED) + "> ";
    private static final String PROMPT_SENDING = MSG.getMsg(AgentI18NResourceKeys.PROMPT_STRING_SENDING) + "> ";
    private static final String PROMPT_TINY = "> ";

    private static final String FILENAME_SERVER_FAILOVER_LIST = "failover-list.dat";

    /**
     * This Java Logging config file - lives inside the agent jar.
     */
    private static final String JAVA_UTIL_LOGGING_PROPERTIES_RESOURCE_PATH = "java.util.logging.properties";

    // Ensure only one instance of the ping job runs by using a pool size of 1
    private static final String PING_THREAD_POOL_NAME = "RHQ Agent Ping Thread";
    private static final int PING_THREAD_POOL_CORE_POOL_SIZE = 1;
    private static final long PING_INTERVAL_MINIMUM = 60000L;

    static final String PROMPT_INPUT_THREAD_NAME = "RHQ Agent Prompt Input Thread";

    /**
     * The directory where this agent is installed.
     */
    private String m_agentHomeDirectory;

    /**
     * The command line arguments specified by the user.
     */
    private String[] m_commandLineArgs;

    /**
     * Indicates if the agent should run in daemon mode. This simply means it will not infinitely attempt to read
     * commands from stdin. If an input file is provided, commands will be read from it, but not from stdin.
     */
    private boolean m_daemonMode;

    /**
     * The stream where the commands are input.
     */
    private AgentInputReader m_input;

    /**
     * Will be <code>true</code> if the input is coming directly from stdin; <code>false</code> if an input script file
     * is used (see -f command line option).
     */
    private boolean m_stdinInput;

    /**
     * The output stream where results are printed.
     */
    private AgentPrintWriter m_output;

    /**
     * Configuration settings for the agent, including the communications services configuration.
     */
    private AgentConfiguration m_configuration;

    /**
     * This is the name of the node where the agent preferences (i.e. its configuration) is stored under the top level
     * agent node name (which is {@link AgentConfigurationConstants#PREFERENCE_NODE_PARENT}). This allows a user to have
     * multiple configurations of an agent.
     */
    private String m_agentPreferencesNodeName;

    /**
     * The container that manages our server-side communications services.
     */
    private ServiceContainer m_commServices;

    /**
     * The object that can be used to send commands to remote servers.
     */
    private ClientCommandSender m_clientSender;

    /**
     * Flag to indicate if agent is started.
     */
    private final AtomicBoolean m_started = new AtomicBoolean();

    /**
     * Lock held when the m_clientSender needs to be created and destroyed.
     */
     private final Object m_init = new Object();

    /**
     * The time (as reported by <code>System.currentTimeMillis()</code>) when the agent was {@link #start() started}.
     * This value resets to 0 once the agent is {@link #shutdown()}.
     */
    private long m_startTime = 0L;

    /**
     * Map of all the valid prompt commands - the map is keyed on the prompt command name and the value is the <code>
     * Class</code> of the {@link AgentPromptCommand} implementation.
     */
    private Map<String, Class<? extends AgentPromptCommand>> m_promptCommands;

    /**
     * If not <code>null</code>, this is the shutdown hook that the agent has installed.
     */
    private Thread m_shutdownHook;

    /**
     * This is the thread that is running the input loop; it accepts prompt commands from the user.
     */
    private Thread m_inputLoopThread;

    /**
     * The listener that will be used to auto-detect the server coming online and going offline. If this is <code>
     * null</code>, it means the agent is not listening for auto-detection messages.
     */
    private AgentAutoDiscoveryListener m_autoDiscoveryListener;

    /**
     * A list of commands that were previously queued up but not yet when we shutdown the sender. These hold volatile
     * commands that we should try to resend once we restart.
     */
    private LinkedList<Runnable> m_previouslyQueueCommands;

    /**
     * If the agent needs to be setup when it is first initialized, by default the basic setup will run which asks the
     * user for some basic, minimal information necessary to get the agent started. However, if this flag is <code>
     * true</code>, the advanced setup will run as opposed to the basic setup.
     */
    private boolean m_advancedSetup = false;

    /**
     * If the agent was told to setup (via a command line option), this will be true.
     */
    private boolean m_forcedSetup = false;

    /**
     * Determines if the agent should {@link #start()} at boot time. If <code>false</code>, the agent will not be
     * started when the VM starts - it will explicitly wait for the start prompt command to be issued. The default is
     * <code>true</code> meaning as soon as the agent VM starts up, the agent's {@link #start()} method is called.
     */
    private boolean m_startAtBoot = true;

    /**
     * First and only element will be a non-<code>null</code> thread when this agent is actively attempting to register
     * itself with the server. This is an array because the array itself will be used for its lock to synchronize access
     * to the thread.
     */
    private Thread[] m_registrationThread = new Thread[1];

    /**
     * Will be non-<code>null</code> if this agent has successfully registered with the server.
     */
    private AgentRegistrationResults m_registration;

    /**
     * This is the management MBean responsible for managing and monitoring this agent. This is the object the agent
     * plugin interacts with.
     */
    private AgentManagement m_managementMBean;

    /**
     * Contains an iteratable list of servers that this agent should use when it has to failover to
     * other servers.
     */
    private FailoverListComposite m_serverFailoverList;

    /**
     * Array that will always have size 1 whose value is the timestamp of the last time the agent
     * successfully failed over to another server.
     * This is an array because it is also locked when a failover is currently being attempted - thus
     * allowing one failover attempt to happen at any one time regardless of the number of
     * concurrent messages/failovers requested.
     */
    private long[] m_lastFailoverTime = new long[] { 0L };

    /**
     * Thread used to try to maintain connectivity to the primary server as much as possible.
     */
    private PrimaryServerSwitchoverThread m_primaryServerSwitchoverThread;

    /**
     * Object that remembers when the last connect agent message was sent and to which server.
     */
    private LastSentConnectAgent m_lastSentConnectAgent = new LastSentConnectAgent();

    /**
     * This is the number of milliseconds this agent clock differs from its server's clock.
     * A positive number means the agent's clock is ahead of the server.
     * This is only updated at certain times during the agent lifetime but it is useful
     * for the agent management interface to disclose as it will help to determine if
     * agent/server boxes are in sync.
     */
    private long m_agentServerClockDifference = 0L;

    /**
     * Thread used to monitor the agent's VM and to hibernate the agent if the VM seems to be critically ill.
     */
    private VMHealthCheckThread m_vmHealthCheckThread;

    /**
     * Counts the number of times the agent has been restarted and holds the reason for the last restart.
     */
    private final AgentRestartCounter m_agentRestartCounter = new AgentRestartCounter();

    /**
     * The -t command line option was specified.
     */
    private boolean m_disableNativeSystem;

    /**
     * Thread used to repeatedly ping the server for connectivity, agent avail update, and clock sync
     */
    private ScheduledThreadPoolExecutor m_pingThreadPoolExecutor;

    /**
     * Tracks whether we already logged a warning to let the user know SIGAR support isn't available.
     */
    private boolean m_loggedNativeSystemInfoUnavailableWarning;

    /**
     * Plugin update instance, used by management.
     */
    private PluginUpdate m_pluginUpdate;

    /**
     * The main method that starts the whole thing.
     *
     * @param args the arguments passed on the command line (e.g. java org.rhq.enterprise.agent.AgentMain arg1 arg2 arg3)
     */
    public static void main(String[] args) {
        reconfigureJavaLogging();

        AgentMain agent = null;
        int retries = 0;
        final int MAX_RETRIES = 5;

        while (retries++ < MAX_RETRIES) {
            try {
                agent = new AgentMain(args);

                // immediately show/log the version information
                String productNameAndVersion = Version.getProductNameAndVersion();
                String buildNumber = Version.getBuildNumber();
                Date buildDate = Version.getBuildDate();
                agent.getOut().println(productNameAndVersion + " [" + buildNumber + "] (" + buildDate + ")");
                LOG.info(AgentI18NResourceKeys.IDENTIFY_VERSION, productNameAndVersion, buildNumber, buildDate);

                // ask the user to setup the agent if the agent hasn't been setup yet or we are being forced to
                if (agent.m_forcedSetup || (!agent.m_daemonMode && !agent.m_configuration.isAgentConfigurationSetup())) {
                    SetupPromptCommand setup_cmd = new SetupPromptCommand();

                    AgentPromptInfo in = new AgentPromptInfo(agent);
                    AgentPrintWriter out = agent.getOut();
                    Preferences prefs = agent.m_configuration.getPreferences();
                    if (agent.m_advancedSetup) {
                        setup_cmd.performAdvancedSetup(prefs, in, out);
                    } else {
                        setup_cmd.performBasicSetup(prefs, in, out);
                    }
                }

                // start the agent automatically only if we are configured to do so; otherwise, just start the user input loop
                if (agent.m_startAtBoot) {
                    agent.start();
                    agent.m_agentRestartCounter.restartedAgent(AgentRestartReason.PROCESS_START);
                } else {
                    agent.inputLoop();
                }
                retries = MAX_RETRIES; // no need to retry...we're good to go
            } catch (HelpException he) {
                retries = MAX_RETRIES; // do nothing but exit the thread
            } catch (AgentNotSupportedException anse) {
                LOG.fatal(anse, AgentI18NResourceKeys.AGENT_START_FAILURE);
                agent.getOut().println(MSG.getMsg(AgentI18NResourceKeys.AGENT_START_FAILURE));
                anse.printStackTrace(agent.getOut());
                retries = MAX_RETRIES; // this is unrecoverable, we need this main thread to exit *now*
            } catch (Exception e) {
                LOG.fatal(e, AgentI18NResourceKeys.AGENT_START_FAILURE);

                if (agent != null) {
                    agent.getOut().println(MSG.getMsg(AgentI18NResourceKeys.AGENT_START_FAILURE));
                    e.printStackTrace(agent.getOut());
                    if (retries < MAX_RETRIES) {
                        LOG.error(AgentI18NResourceKeys.AGENT_START_RETRY_AFTER_FAILURE);
                        agent.getOut().println(MSG.getMsg(AgentI18NResourceKeys.AGENT_START_RETRY_AFTER_FAILURE));
                        try {
                            Thread.sleep(60000L);
                        } catch (InterruptedException e1) {
                            Thread.currentThread().interrupt();
                        }
                    }
                } else {
                    System.err.println(MSG.getMsg(AgentI18NResourceKeys.AGENT_START_FAILURE));
                    e.printStackTrace(System.err);
                    retries = MAX_RETRIES; // agent could not even be instantiated, this is unrecoverable, just exit
                }

                agent = null;
            }
        }
        return;
    }

    private void checkTempDir() {
        File tmpDir = new File(System.getProperty("java.io.tmpdir"));
        if (!tmpDir.exists()) {
            LOG.warn("Invalid java.io.tmpdir: [" + tmpDir.getAbsolutePath() + "] does not exist.");
            try {
                LOG.info("Creating java.io.tmpdir: [" + tmpDir.getAbsolutePath() + "]");
                tmpDir.mkdir();
            } catch (Throwable t) {
                throw new RuntimeException("Startup failed: Could not create missing java.io.tmpdir ["
                    + tmpDir.getAbsolutePath() + "]", t);
            }
        }
        if (!tmpDir.isDirectory()) {
            throw new RuntimeException("Startup failed: java.io.tmpdir [" + tmpDir.getAbsolutePath()
                + "] is not a directory");
        }
        if (!tmpDir.canRead() || !tmpDir.canExecute()) {
            throw new RuntimeException("Startup failed: java.io.tmpdir [" + tmpDir.getAbsolutePath()
                + "] is not readable");
        }
        if (!tmpDir.canWrite()) {
            throw new RuntimeException("Startup failed: java.io.tmpdir [" + tmpDir.getAbsolutePath()
                + "] is not writable");
        }
    }

    /**
     * Constructor for {@link AgentMain} that loads the agent configuration and prepare some additional internal data.
     *
     * @throws Exception if failed to load the configuration properly
     */
    public AgentMain() throws Exception {
        this(null);
    }

    /**
     * Constructor for {@link AgentMain} that accepts command line arguments, loads the agent configuration and prepare
     * some additional internal data.
     *
     * @param  args the command line arguments (may be <code>null</code>)
     *
     * @throws Exception if failed to load the configuration properly
     */
    public AgentMain(String[] args) throws Exception {
        LOG.debug(AgentI18NResourceKeys.CREATING_AGENT);

        m_agentHomeDirectory = null;
        m_daemonMode = false;
        m_input = null;
        m_output = new AgentPrintWriter(System.out, true);
        m_stdinInput = true;
        m_configuration = null;
        m_agentPreferencesNodeName = AgentConfigurationConstants.DEFAULT_PREFERENCE_NODE;
        m_previouslyQueueCommands = null;
        m_registration = null;
        m_serverFailoverList = null;
        m_primaryServerSwitchoverThread = null;
        m_vmHealthCheckThread = null;
        m_pingThreadPoolExecutor = null;

        if (args == null) {
            args = new String[0];
        }

        m_commandLineArgs = args;
        processArguments(m_commandLineArgs);

        m_promptCommands = new HashMap<String, Class<? extends AgentPromptCommand>>();
        setupPromptCommandsMap(m_promptCommands);

        // Move this last to allow for commands to be inserted into tab-completion.
        if (m_input == null) {
            m_input = AgentInputReaderFactory.create(this);
        }

        prepareNativeSystem();

        checkTempDir();

        return;
    }

    /**
     * Returns the directory that is considered the "agent home" (i.e. the directory
     * where the agent is installed).
     *
     * @return agent home directory, or empty string if it cannot be determined
     */
    public String getAgentHomeDirectory() {
        if (m_agentHomeDirectory != null) {
            return m_agentHomeDirectory;
        }

        File agentHomeDir = null;

        String env = System.getenv("RHQ_AGENT_HOME");
        if (env != null) {
            agentHomeDir = new File(env);
        } else {
            // usually, RHQ_AGENT_HOME is defined, but it doesn't have to be, so let's try to find it other ways.
            // if AgentMain is in a jar file located in a lib directory then agent home is the lib dir's parent.
            try {
                String resource = AgentMain.class.getName().replace('.', '/').concat(".class");
                URL classUrl = AgentMain.class.getClassLoader().getResource(resource);
                if (classUrl != null) {
                    String pathStr = classUrl.toString();
                    int lastIndexOfLib = pathStr.lastIndexOf("/lib");
                    if (lastIndexOfLib >= 0) {
                        int lastIndexOfFileProtocol = pathStr.lastIndexOf("file:") + 5;
                        pathStr = pathStr.substring(lastIndexOfFileProtocol, lastIndexOfLib);
                        File file = new File(pathStr);
                        if (file.exists()) {
                            agentHomeDir = file;
                        }
                    }
                }
            } catch (Exception e) {
                // lots of reasons to get here but the short of it is, we can't find the agent home this way
            }
        }

        if (agentHomeDir != null) {
            try {
                m_agentHomeDirectory = agentHomeDir.getCanonicalPath(); // try to get canonical path...
            } catch (Exception e) {
                m_agentHomeDirectory = agentHomeDir.getAbsolutePath(); // ...but use absolute as a fallback
            }
            return m_agentHomeDirectory;
        } else {
            return "";
        }
    }

    /**
     * Returns <code>true</code> if the agent has started; <code>false</code> if the agent has either never been started
     * or has been stopped.
     *
     * @return <code>true</code> if the agent is started; <code>false</code> if it is stopped.
     */
    public boolean isStarted() {
        return m_started.get();
    }

    /**
     * Returns <code>true</code> if the embedded plugin container has been {@link #startPluginContainer(long) started}
     * or <code>false</code> if its currently {@link #shutdownPluginContainer() stopped}.
     *
     * @return status of the plugin container
     */
    public boolean isPluginContainerStarted() {
        return PluginContainer.getInstance().isStarted();
    }

    /**
     * Returns the time (as reported by <code>System.currentTimeMillis()</code>) when the agent was
     * {@link #start() started}. This returns 0 if the agent has been {@link #shutdown()}.
     *
     * @return time when the agent was started
     */
    public long getStartTime() {
        return m_startTime;
    }

    /**
     * Returns the number of milliseconds this agent thinks its clock is ahead or behind from
     * its server's clock.  A positive value means the agent clock is ahead.
     *
     * @return time the agent-server clock difference
     */
    public long getAgentServerClockDifference() {
        return m_agentServerClockDifference;
    }

    /**
     * This method should be called whenever the server time is known. This helps
     * keep the {@link #getAgentServerClockDifference()} up-to-date.
     *
     * @param serverTime the currently know value of the server clock (epoch millis)
     */
    public void serverClockNotification(long serverTime) {
        long agentTime = System.currentTimeMillis();
        m_agentServerClockDifference = agentTime - serverTime;
        if (Math.abs(m_agentServerClockDifference) > 30000L) {
            LOG.error(AgentI18NResourceKeys.TIME_NOT_SYNCED, serverTime, new Date(serverTime), agentTime, new Date(
                agentTime));
        }
    }

    /**
     * This method will initialize all services and get the agent up and running.
     *
     * @throws Exception if failed to start
     */
    public void start() throws Exception {
        synchronized (m_init) {
            if (!isStarted()) {
                try {
                    if ((m_configuration.getAgentName() == null) || (m_configuration.getAgentName().length() == 0)) {
                        // the agent name isn't defined yet - let's auto-generate one for the user
                        try {
                            String hostname = InetAddress.getLocalHost().getCanonicalHostName();
                            m_configuration.getPreferences().put(AgentConfigurationConstants.NAME, hostname);
                            m_configuration.getPreferences().flush();
                            LOG.info(AgentI18NResourceKeys.AGENT_NAME_AUTO_GENERATED, hostname);
                        } catch (Exception e) {
                            throw new IllegalStateException(MSG.getMsg(AgentI18NResourceKeys.AGENT_NAME_NOT_DEFINED), e);
                        }
                    }

                    prepareNativeSystem();

                    BootstrapLatchCommandListener latch = new BootstrapLatchCommandListener();
                    startCommServices(latch); // note that we start the comm services before we start the plugin container
                    startManagementServices(); // we start our metric collectors before plugin container so the agent plugin can work
                    prepareStartupWorkRequiringServer();
                    waitForServer(m_configuration.getWaitForServerAtStartupMsecs());

                    if (!m_configuration.doNotStartPluginContainerAtStartup()) {
                        // block indefinitely - we cannot continue until we are registered, we have plugins and the PC starts
                        if (!startPluginContainer(0L)) {
                            throw new Exception(MSG.getMsg(AgentI18NResourceKeys.PLUGIN_CONTAINER_NOT_INITIALIZED));
                        }
                    } else {
                        LOG.info(AgentI18NResourceKeys.NOT_STARTING_PLUGIN_CONTAINER_AT_STARTUP);
                    }

                    // now that our plugin container has been initialized, it can begin to receive incoming commands
                    latch.allowAllCommands(m_commServices);

                    // Now that we are allowing commands to be passed back and forth with the server and
                    // the PC is likely up, start our Ping service
                    m_pingThreadPoolExecutor = new ScheduledThreadPoolExecutor(PING_THREAD_POOL_CORE_POOL_SIZE,
                        new LoggingThreadFactory(PING_THREAD_POOL_NAME, true));
                    long pingInterval = m_configuration.getClientSenderServerPollingInterval();
                    pingInterval = (pingInterval < PING_INTERVAL_MINIMUM) ? PING_INTERVAL_MINIMUM : pingInterval;
                    m_pingThreadPoolExecutor.scheduleWithFixedDelay(new PingExecutor(), 0L, pingInterval,
                        TimeUnit.MILLISECONDS);

                    // prepare our shutdown hook
                    m_shutdownHook = new AgentShutdownHook(this);
                    Runtime.getRuntime().addShutdownHook(m_shutdownHook);

                    // start the thread that tries to keep the agent pointed to its primary server
                    m_primaryServerSwitchoverThread = new PrimaryServerSwitchoverThread(this);
                    long check_interval = m_configuration.getPrimaryServerSwitchoverCheckIntervalMsecs();
                    if (check_interval > 0) {
                        m_primaryServerSwitchoverThread.setInterval(check_interval);
                        m_primaryServerSwitchoverThread.start();
                    }

                    // start the vm health check thread so it can monitor the health of the VM
                    m_vmHealthCheckThread = new VMHealthCheckThread(this);
                    if (m_configuration.getVMHealthCheckIntervalMsecs() > 0) {
                        m_vmHealthCheckThread.start();
                    }

                    // indicate that we have started
                    setStarted(true);

                    // start our input loop thread if it hasn't been started yet
                    if ((m_inputLoopThread == null) || !m_inputLoopThread.isAlive()) {
                        inputLoop();
                    }
                } catch (Exception e) {
                    LOG.fatal(e, AgentI18NResourceKeys.STARTUP_ERROR);
                    setStarted(true); // make sure this is flipped to true so our shutdown actually does something
                    shutdown();
                    throw e;
                }
            }
        }

        return;
    }

    /**
     * Shuts down the agent gracefully.
     */
    public void shutdown() {

        // Temporarily clear the interrupted state to allow clean shutdown
        // This seems to happen from invoking stop from the command prompt
        boolean interrupted = Thread.interrupted();

        synchronized (m_init) {
            if (isStarted()) {
                try {
                    LOG.info(AgentI18NResourceKeys.SHUTTING_DOWN);
                } catch (Throwable t) {
                    // this really would only happen under very bad conditions, like OutOfMemoryError
                    // but if that happens, let's keep going and try to shutdown things anyway
                }

                // Notice that for every component we shutdown, we wrap in a try-catch to ignore exceptions.
                // We want to keep going to ensure we attempt to try to shutdown everything.

                ///////
                // stop the thread that pings the server
                try {
                    if (null != m_pingThreadPoolExecutor) {
                        m_pingThreadPoolExecutor.shutdownNow();
                        m_pingThreadPoolExecutor = null;
                    }
                } catch (Throwable ignore) {
                    LOG.warn(AgentI18NResourceKeys.FAILED_TO_SHUTDOWN_COMPONENT, "Server Ping Thread",
                        ThrowableUtil.getAllMessages(ignore));
                }

                ///////
                // stop the thread that checks the VM health
                try {
                    if (m_vmHealthCheckThread != null) {
                        m_vmHealthCheckThread.stopChecking();
                        m_vmHealthCheckThread.interrupt();
                        m_vmHealthCheckThread = null;
                    }
                } catch (Throwable ignore) {
                    LOG.warn(AgentI18NResourceKeys.FAILED_TO_SHUTDOWN_COMPONENT, "Low Memory Check Thread",
                        ThrowableUtil.getAllMessages(ignore));
                }

                ///////
                // stop the thread that tries to keep the agent pointed to its primary server
                try {
                    if (m_primaryServerSwitchoverThread != null) {
                        m_primaryServerSwitchoverThread.stopChecking();
                        m_primaryServerSwitchoverThread.interrupt();
                        m_primaryServerSwitchoverThread = null;
                    }
                } catch (Throwable ignore) {
                    LOG.warn(AgentI18NResourceKeys.FAILED_TO_SHUTDOWN_COMPONENT, "Server Switchover Thread",
                        ThrowableUtil.getAllMessages(ignore));
                }

                ///////
                // remove our shutdown hook
                if (m_shutdownHook != null) {
                    try {
                        Runtime.getRuntime().removeShutdownHook(m_shutdownHook);
                    } catch (Throwable ignore) {
                        // looks like we are already in the process of shutting down, ignore this exception
                    }
                    m_shutdownHook = null;
                }

                ///////
                // shutdown the PC, tell the server that we are shutting down, and shutdown all comm services, in that order
                try {
                    shutdownPluginContainer();
                } catch (Throwable ignore) {
                    LOG.warn(AgentI18NResourceKeys.FAILED_TO_SHUTDOWN_COMPONENT, "Plugin Container",
                        ThrowableUtil.getAllMessages(ignore));
                }

                ///////
                // notify the server that we are shutting down
                try {
                    if (!m_configuration.doNotNotifyServerOfShutdown()) {
                        notifyServerOfShutdown();
                    } else {
                        LOG.info(AgentI18NResourceKeys.TOLD_TO_NOT_NOTIFY_SERVER_OF_SHUTDOWN);
                    }
                } catch (Throwable ignore) {
                    LOG.warn(AgentI18NResourceKeys.FAILED_TO_SHUTDOWN_COMPONENT, "Server Shutdown Notification",
                        ThrowableUtil.getAllMessages(ignore));
                }

                ///////
                // shutdown our management MBean
                try {
                    stopManagementServices();
                } catch (Throwable ignore) {
                    LOG.warn(AgentI18NResourceKeys.FAILED_TO_SHUTDOWN_COMPONENT, "Agent Management Services",
                        ThrowableUtil.getAllMessages(ignore));

                }

                ///////
                // shutdown our comm layer so we no longer accept incoming messages
                try {
                    shutdownCommServices();
                } catch (Throwable ignore) {
                    LOG.warn(AgentI18NResourceKeys.FAILED_TO_SHUTDOWN_COMPONENT, "Communication Services",
                        ThrowableUtil.getAllMessages(ignore));
                }

                ///////
                // everything should be down - finalize things and notify our input loop that we've stopped
                setStarted(false);

                m_init.notifyAll();

                if ((m_inputLoopThread != null) && m_inputLoopThread.isAlive()) {
                    m_inputLoopThread.interrupt();
                }

                LOG.info(AgentI18NResourceKeys.AGENT_SHUTDOWN);
            }

            // let's make sure we shutdown everything inside the native system
            // do this even if the agent was already shutdown
            SystemInfoFactory.shutdown();
        }

        if (interrupted) {
            Thread.currentThread().interrupt();
        }
        return;
    }

    /**
     * Returns the output stream that displays messages to the user.
     *
     * @return writer to which you can print messages to the user
     */
    public AgentPrintWriter getOut() {
        return m_output;
    }

    /**
     * Returns the current input reader where the agent is getting its input. If <code>null</code>, this agent is in
     * daemon mode and not currently accepting input.
     *
     * @return the input stream or <code>null</code> if the agent is not currently accepting input
     */
    public AgentInputReader getIn() {
        return m_input;
    }

    /**
     * Returns the object that can be used to obtain I18N messages from the agent's resource bundle in the user's
     * locale.
     *
     * @return object that can be used to obtain resource bundle messages
     */
    public Msg getI18NMsg() {
        return MSG;
    }

    /**
     * Returns the set of configuration preferences that the agent will use to configure things.
     *
     * @return configuration settings
     */
    public AgentConfiguration getConfiguration() {
        return m_configuration;
    }

    /**
     * Returns the client sender which can be used to send commands to the server. This may return <code>null</code>,
     * which means the agent has been shutdown and it cannot send commands.
     *
     * <p/>
     * <p>This is the agent's "remoting client".</p>
     *
     * @return clientSender
     */
    public ClientCommandSender getClientCommandSender() {
        return m_clientSender;
    }

    /**
     * Returns the container that is managing our server-side communications services that accept and process incoming
     * messages. This may return <code>null</code>, which means the agent has been shutdown and it cannot receive
     * commands.
     *
     * <p>This is the agent's "remoting server".</p>
     *
     * @return the service container that manages our communications services
     */
    public ServiceContainer getServiceContainer() {
        return m_commServices;
    }

    /**
     * Returns the management MBean for this agent. This is the MBean that provides management and monitoring
     * functionality for this particular agent instance.
     *
     * <p>This will return <code>null</code> if the agent is not {@link #start() started} or the management MBean was
     * {@link AgentConfiguration#doNotEnableManagementServices() disabled}.</p>
     *
     * @return the agent management MBean instance
     */
    public AgentManagement getAgentManagementMBean() {
        return m_managementMBean;
    }

    /**
     * This will enable/disable agent-server communication tracing. This is for
     * use mainly in development but can also be used for troubleshooting problems.
     *
     * @param enabled whether or not to turn on agent comm tracing
     */
    public void agentServerCommunicationsTrace(boolean enabled) {
        org.apache.log4j.Logger outLogger = LogManager.getLogger(OutgoingCommandTrace.class);
        org.apache.log4j.Logger inLogger = LogManager.getLogger(IncomingCommandTrace.class);
        Level newLevel = (enabled) ? Level.TRACE : Level.OFF;

        // set it up so the trace is more detailed, but don't override any properties already set
        if (enabled) {
            if (CommandTraceUtil.getSettingTraceCommandConfig() == null) {
                CommandTraceUtil.setSettingTraceCommandConfig(Boolean.TRUE);
            }
            if (CommandTraceUtil.getSettingTraceCommandResponseResults() == null) {
                CommandTraceUtil.setSettingTraceCommandResponseResults(Integer.valueOf(256));
            }
            if (CommandTraceUtil.getSettingTraceCommandSizeThreshold() == null) {
                CommandTraceUtil.setSettingTraceCommandSizeThreshold(Integer.valueOf(99999));
            }
            if (CommandTraceUtil.getSettingTraceCommandResponseSizeThreshold() == null) {
                CommandTraceUtil.setSettingTraceCommandResponseSizeThreshold(Integer.valueOf(99999));
            }
        }

        outLogger.setLevel(newLevel);
        inLogger.setLevel(newLevel);
    }

    /**
     * This will hot-deploy a new log4j log configuration file.  Use this to change, at runtime,
     * the log settings so you can, for example, begin logging DEBUG messages to help troubleshoot
     * problems.
     *
     * @param logFilePath the path to the log file - relative to the classloader or filesystem
     *
     * @throws Exception if failed to hot deploy the new log config
     */
    public void hotDeployLogConfigurationFile(String logFilePath) throws Exception {
        URL logFileUrl;

        // the logfile is probably in classpath, check there first; if not, assume its on the filesystem
        logFileUrl = getClass().getClassLoader().getResource(logFilePath);
        if (logFileUrl == null) {
            File file = new File(logFilePath);
            if (!file.exists()) {
                throw new FileNotFoundException(logFilePath);
            }
            logFileUrl = file.toURI().toURL();
        }

        try {
            LogManager.resetConfiguration();
            DOMConfigurator.configure(logFileUrl);
        } catch (Exception e) {
            // something bad happened, let's load the file we know we have in the agent jar
            hotDeployLogConfigurationFile("log4j-debug.xml");
            throw e;
        }

        return;
    }

    /**
     * Returns an iteratable list of servers that can be used as backups when this agent needs to failover
     * to another server.
     *
     * @return list of servers (may be empty but will not be <code>null</code>)
     */
    public FailoverListComposite getServerFailoverList() {
        if (m_serverFailoverList != null) {
            return m_serverFailoverList;
        }

        // we don't have it yet - read our backed up version from the last time we were given the list

        // as a safety measure, return a dummy, empty list if we aren't configured yet; but we really should not
        // be calling this method until we are configured properly
        if (m_configuration == null) {
            return new FailoverListComposite(new ArrayList<ServerEntry>());
        }

        File dataDir = m_configuration.getDataDirectory();
        File failoverListFile = new File(dataDir, FILENAME_SERVER_FAILOVER_LIST);

        FailoverListComposite list;

        if (!failoverListFile.exists()) {
            // a non-existant file implies an empty list (i.e. no failover servers defined)
            list = new FailoverListComposite(new ArrayList<ServerEntry>());
        } else {
            try {
                byte[] bytes = StreamUtil.slurp(new FileInputStream(failoverListFile));
                list = FailoverListComposite.readAsText(new String(bytes));
                LOG.debug(AgentI18NResourceKeys.FAILOVER_LIST_LOADED, failoverListFile, list.size());
            } catch (Exception e) {
                list = new FailoverListComposite(new ArrayList<ServerEntry>());
                LOG.warn(e, AgentI18NResourceKeys.FAILOVER_LIST_CANNOT_BE_LOADED, failoverListFile,
                    ThrowableUtil.getAllMessages(e));
            }
        }

        m_serverFailoverList = list;

        return list;
    }

    /**
     * Downloads a new server failover list from the server and returns the failover list
     * that is now in effect.
     * @return
     *
     * @return the server failover list that is now in effect
     */
    public FailoverListComposite downloadServerFailoverList() {
        try {
            ClientCommandSender sender = getClientCommandSender();
            if (sender != null) {
                String agent_name = this.getConfiguration().getAgentName();
                ClientRemotePojoFactory factory = sender.getClientRemotePojoFactory();
                CoreServerService pojo = factory.getRemotePojo(CoreServerService.class);
                FailoverListComposite list = pojo.getFailoverList(agent_name);
                if (list == null) {
                    list = new FailoverListComposite(new ArrayList<ServerEntry>());
                }
                // if we do not yet have a list or the new list is different than our current one, store the new one
                if (!list.equals(m_serverFailoverList)) {
                    storeServerFailoverList(list);
                    m_serverFailoverList = list;
                    LOG.debug(AgentI18NResourceKeys.FAILOVER_LIST_DOWNLOADED, m_serverFailoverList.size());
                }
            }
        } catch (Throwable t) {
            LOG.warn(AgentI18NResourceKeys.FAILOVER_LIST_DOWNLOAD_FAILURE, t);
        }

        return getServerFailoverList();
    }

    /**
     * Asks the agent to check and see if it is currently connected to its primary server (as opposed
     * to one of its secondary failover servers). If it is not connected to its primary, this
     * call will trigger the agent to attempt to switch over. This switchover will occur asynchronously,
     * and may not have occurred by the time this method returns. A side effect of this method is
     * that the agent will also download a server failover list from its current server to make sure it is up-to-date.
     */
    public void performPrimaryServerSwitchoverCheck() {
        if (m_primaryServerSwitchoverThread != null) {
            m_primaryServerSwitchoverThread.checkNow();
        }
        return;
    }

    /**
     * Reads a line from the input stream and returns it. The given prompt will be displayed prior to reading stdin.
     *
     * <p/>
     * <p>If prompt is <code>null</code>, the standard prompt is output.</p>
     *
     * <p/>
     * <p>If the input stream has been closed, <code>null</code> is returned.</p>
     *
     * @param  prompt the string that will be displayed to stdout before stdin is read
     *
     * @return line entered by the user
     *
     * @throws RuntimeException if failed to read input for some reason
     */
    public String getUserInput(String prompt) {
        String input_string = "";
        boolean use_default_prompt = (prompt == null);

        while ((input_string != null) && (input_string.trim().length() == 0)) {
            if (use_default_prompt) {
                prompt = getDefaultPrompt();
            }

            m_output.print(prompt);

            // just make the prompt look nicer by ensuring there is a space after the prompt string
            if (!prompt.endsWith(" ")) {
                m_output.print(' ');
            }

            try {
                m_output.flush();
                input_string = m_input.readLine();
                if (input_string == null) {
                    LOG.debug(AgentI18NResourceKeys.INPUT_EOF);
                }
            } catch (Exception e) {
                input_string = null;
                LOG.debug(AgentI18NResourceKeys.INPUT_EXCEPTION, ThrowableUtil.getAllMessages(e));
            }
        }

        if (input_string != null) {
            // if we are processing a script, show the input that was just read in
            if (!m_stdinInput) {
                m_output.println(input_string);
            }
        } else if (!m_stdinInput) {
            // if we are processing a script, we hit the EOF, so close the input stream
            try {
                m_input.close();
            } catch (IOException e1) {
            }

            // if we are not in daemon mode, let's now start processing prompt commands coming in via stdin
            if (!m_daemonMode) {
                try {
                    m_input = AgentInputReaderFactory.create(this);
                } catch (IOException e1) {
                    m_input = null;
                    LOG.debug(e1, AgentI18NResourceKeys.INPUT_FACTORY_EXCEPTION);
                }
                m_stdinInput = true;
                input_string = "";
            } else {
                m_input = null;
            }
        }

        return input_string;
    }

    /**
     * Returns the map containing all the valid prompt command definitions. Map is keyed on the command string and the
     * value is the <code>Class</code> of the {@link AgentPromptCommand} implementation.
     *
     * @return map of valid prompt commands
     */
    public Map<String, Class<? extends AgentPromptCommand>> getPromptCommands() {
        return m_promptCommands;
    }

    /**
     * Same as {@link #loadConfigurationFile(String)} except this allows you to indicate which preferences node the
     * configuration is to be stored in. See the <i>Java Preferences API</i> for the definition of what a preferences
     * node is.
     *
     * @param  pref_node_name the node name that much match the node name in the configuration file that is to be loaded
     * @param  file_name      the config file to load (should exist either on the file system or in the current thread's
     *                        classloader)
     *
     * @return the properties that have been loaded from the given file wrapped in an {@link AgentConfiguration} object
     *
     * @throws IllegalArgumentException          if the node name was invalid which occurs if the name has a forward
     *                                           slash (/) in it
     * @throws IOException                       if failed to load the configuration file
     * @throws InvalidPreferencesFormatException if the configuration file had an invalid format
     * @throws BackingStoreException             if failed to access the preferences persistence store
     * @throws Exception                         on other failures
     */
    public AgentConfiguration loadConfigurationFile(String pref_node_name, String file_name) throws Exception {
        setConfigurationPreferencesNode(pref_node_name);
        return loadConfigurationFile(file_name);
    }

    /**
     * Loads the given configuration file into the agent. The file name will first be checked for existence on the file
     * system. If it cannot be found, it will be assumed <code>fileName</code> specifies the file as found in the
     * current class loader and the file will be searched there. An exception is thrown if the file cannot be found
     * anywhere.
     *
     * <p>It is assumed that the agent's preference node name as defined by the <b>-p</b> command line argument is the
     * one that matches the node name in the given configuration file.</p>
     *
     * <p>The agent's configuration will be those settings found in the file - specifically, they are not overridden by
     * system properties. If you want system properties to override any preferences found in the file, then you should
     * call {@link #overlaySystemPropertiesOnAgentConfiguration()} after this method returns</p>
     *
     * @param  file_name the config file to load (should exist either on the file system or in the current thread's
     *                   classloader)
     *
     * @return the properties that have been loaded from the given file wrapped in an {@link AgentConfiguration} object
     *
     * @throws IOException                       if failed to load the configuration file
     * @throws InvalidPreferencesFormatException if the configuration file had an invalid format
     * @throws BackingStoreException             if failed to access the preferences persistence store
     * @throws Exception                         on other failures
     */
    public AgentConfiguration loadConfigurationFile(String file_name) throws Exception {
        InputStream config_file_input_stream;
        File config_file = new File(file_name);

        if (config_file.exists()) {
            config_file_input_stream = new FileInputStream(config_file);
        } else {
            config_file_input_stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(file_name);
        }

        if (config_file_input_stream == null) {
            throw new IOException(MSG.getMsg(AgentI18NResourceKeys.CANNOT_FIND_CONFIG_FILE, file_name));
        }

        LOG.debug(AgentI18NResourceKeys.LOADING_CONFIG_FILE, file_name);

        // We need to clear out any previous configuration in case the current config file doesn't specify a preference
        // that already exists in the preferences node.  In this case, the configuration file wants to fall back on the
        // default value and if we don't clear the preferences, we aren't guaranteed the value stored in the backing
        // store is the default value.
        // But first we need to backup these original preferences in case the config file fails to load -
        // we'll restore the original values in that case.
        // Note that we squirrel away any security token we already have - we need to preserve this when we can
        // because otherwise the agent will not be able to re-register with any previous name is was registered with.
        Preferences preferencesNode = getPreferencesNode();
        String securityToken = preferencesNode.get(AgentConfigurationConstants.AGENT_SECURITY_TOKEN, null);
        ByteArrayOutputStream backup = new ByteArrayOutputStream();
        preferencesNode.exportSubtree(backup);
        preferencesNode.clear();

        // now load in the preferences
        try {
            ByteArrayOutputStream raw_config_file = new ByteArrayOutputStream();
            StreamUtil.copy(config_file_input_stream, raw_config_file, true);

            // this magic is to allow the prefs XML to replace the node name with the -p option
            // without requiring the user to hand-modify the .xml file.  This also adds the ability
            // for the user to use ${var} system property replaces as a bonus.
            Properties replacements = new Properties();
            replacements.putAll(System.getProperties());
            replacements.put("rhq.agent.preferences-node", m_agentPreferencesNodeName);
            String new_config = StringPropertyReplacer.replaceProperties(raw_config_file.toString(), replacements);

            ByteArrayInputStream new_config_input_stream = new ByteArrayInputStream(new_config.getBytes());
            Preferences.importPreferences(new_config_input_stream);

            AgentConfiguration newAgentConfig = new AgentConfiguration(preferencesNode);
            if (newAgentConfig.getAgentConfigurationVersion() == 0) {
                throw new IllegalArgumentException(MSG.getMsg(AgentI18NResourceKeys.BAD_NODE_NAME_IN_CONFIG_FILE,
                    file_name, m_agentPreferencesNodeName));
            }

            // If we had a security token, restore it so we can maintain our known registration with the server.
            // Note that if the configuration file already had a security token defined, it will be used and the old
            // token we had will be thrown away.
            if (securityToken != null) {
                if (newAgentConfig.getAgentSecurityToken() == null) {
                    LOG.debug(AgentI18NResourceKeys.RESTORING_SECURITY_TOKEN);
                    newAgentConfig.setAgentSecurityToken(securityToken);
                } else {
                    LOG.info(AgentI18NResourceKeys.NOT_RESTORING_SECURITY_TOKEN);
                }
            }

            preferencesNode.flush();

        } catch (Exception e) {
            // a problem occurred importing the config file; let's restore our original values
            try {
                Preferences.importPreferences(new ByteArrayInputStream(backup.toByteArray()));
            } catch (Exception e1) {
                // its conceivable the same problem occurred here as with the original exception (backing store problem?)
                // let's throw the original exception, not this one
            }

            throw e;
        }

        AgentConfiguration agent_configuration = new AgentConfiguration(preferencesNode);

        LOG.debug(AgentI18NResourceKeys.LOADED_CONFIG_FILE, file_name);

        m_configuration = agent_configuration;

        return m_configuration;
    }

    /**
     * This is an API that allows the caller to explicitly ensure that system properties are overlaid on top of the
     * current agent configuration. You can use this after a call to {@link #loadConfigurationFile(String)} when you
     * want to allow system properties to override configuration preferences loaded from a file. Note that a runtime
     * exception will be thrown if the agent has not yet loaded an initial configuration.
     *
     * <p>This method ignores {@link AgentConfiguration#doNotOverridePreferencesWithSystemProperties()} - in other
     * words, this method will always overlay the system properties, even though the agent may have been configured not
     * to. Use this method with caution since you are disobeying an agent configuration preference.</p>
     */
    public void overlaySystemPropertiesOnAgentConfiguration() {
        overlaySystemProperties(m_configuration.getPreferences());
    }

    /**
     * This will send a registration request to the server. This should be called when the agent has been started and
     * sender has been created. It is public to allow anyone to ask the agent to re-register itself (such as via the
     * {@link RegisterPromptCommand register prompt command}).
     *
     * <p>This registration process is asynchronous - this method returns immediately after spawning the thread unless
     * the <code>wait</code> parameter is greater than 0. The registration will not be completed until the agent is able
     * to successfully send a command to the server and get a response back. If the wait period is 0, the caller is not
     * guaranteed this has happened when this method returns - the method return will only guarantee that the thread has
     * been started. If the wait period is greater than 0, then when this method returns, the agent will have been
     * registered or that amount of time has expired prior to successfully registering.</p>
     *
     * @param wait             the amount of time in milliseconds this method will wait for the registration process to
     *                         complete before returning. If 0 or less, this method will not wait and will return as
     *                         soon as the registration thread has started.
     * @param regenerate_token if <code>true</code>, the agent will ask for a new token even if it was already assigned
     *                         one. if <code>false</code>, the agent's existing token will not change.
     */
    public void registerWithServer(long wait, final boolean regenerate_token) {
        Runnable task = new Runnable() {
            public void run() {
                boolean retry = true;
                long retry_interval = 1000L;
                boolean got_registered = false;
                int registrationFailures = 0;
                final int MAX_ALLOWED_REGISTRATION_FAILURES = 5;
                boolean hide_loopback_warning = Boolean.getBoolean("rhq.hide-agent-localhost-warning");
                boolean hide_failover_list_warning = false;

                while (retry) {
                    try {
                        ClientCommandSender sender = getClientCommandSender();

                        if ((sender == null) || Thread.currentThread().isInterrupted()) {
                            LOG.debug(AgentI18NResourceKeys.AGENT_REGISTRATION_ABORTED);
                            retry = false;
                        } else {
                            String token = getAgentSecurityToken();
                            ClientRemotePojoFactory pojo_factory = sender.getClientRemotePojoFactory();
                            CoreServerService remote_pojo = pojo_factory.getRemotePojo(CoreServerService.class);
                            AgentConfiguration agent_config = getConfiguration();
                            ServiceContainerConfiguration server_config = agent_config.getServiceContainerPreferences();
                            String agent_name = agent_config.getAgentName();
                            String address = server_config.getConnectorBindAddress();
                            int port = server_config.getConnectorBindPort();
                            String remote_endpoint = server_config.getConnectorRemoteEndpoint();
                            String version = Version.getProductVersion();
                            String build = Version.getBuildNumber();
                            AgentVersion agentVersion = new AgentVersion(version, build);
                            AgentRegistrationRequest request = new AgentRegistrationRequest(agent_name, address, port,
                                remote_endpoint, regenerate_token, token, agentVersion);

                            Thread.sleep(retry_interval);

                            if (sender.isSending()) {
                                LOG.debug(AgentI18NResourceKeys.AGENT_REGISTRATION_ATTEMPT, request);

                                if (!hide_loopback_warning) {
                                    if (remote_endpoint.contains("localhost") || remote_endpoint.contains("127.0.0.")) {
                                        String msg_id = AgentI18NResourceKeys.REGISTERING_WITH_LOOPBACK;
                                        LOG.warn(msg_id, remote_endpoint);
                                        getOut().println(MSG.getMsg(msg_id, remote_endpoint));
                                        getOut().println();
                                        hide_loopback_warning = true; // don't bother to tell the user more than once
                                    }
                                }

                                // delete any old token so request is unauthenticated to get server to accept it
                                agent_config.setAgentSecurityToken(null);
                                m_commServices.addCustomData(
                                    SecurityTokenCommandAuthenticator.CMDCONFIG_PROP_SECURITY_TOKEN, null);

                                FailoverListComposite failover_list = null;
                                try {
                                    AgentRegistrationResults results = remote_pojo.registerAgent(request);
                                    failover_list = results.getFailoverList();
                                    token = results.getAgentToken(); // make sure our finally block gets this - BZ 963982

                                    // Try to do a simple connect to each server in the failover list
                                    // If only some of the servers are unreachable, just keep going;
                                    //    the agent will eventually switchover to one of the live servers.
                                    // But if all servers in the list are unreachable, we need to keep retrying hoping
                                    // someone fixes the servers' public endpoints so the agent can reach one or more of them.
                                    boolean test_failover_list = m_configuration.isTestFailoverListAtStartupEnabled();
                                    if (test_failover_list) {
                                        List<String> failed = testFailoverList(failover_list);
                                        if (failed.size() > 0) {
                                            if (failed.size() == failover_list.size()) {
                                                retry = true;
                                                retry_interval = 30000L;
                                                if (!hide_failover_list_warning) {
                                                    String msg_id = AgentI18NResourceKeys.FAILOVER_LIST_CHECK_FAILED;
                                                    LOG.warn(msg_id, failed.size(), failed.toString());
                                                    getOut().println(
                                                        MSG.getMsg(msg_id, failed.size(), failed.toString()));
                                                    getOut().println();
                                                    hide_failover_list_warning = true; // don't bother logging more than once
                                                }
                                                continue; // immediately go back and start the retry
                                            }
                                        }
                                    } else {
                                        LOG.info(AgentI18NResourceKeys.TEST_FAILOVER_LIST_AT_STARTUP_DISABLED);
                                    }

                                    m_registration = results;
                                    got_registered = true;
                                    retry = false;
                                    LOG.info(AgentI18NResourceKeys.AGENT_REGISTRATION_RESULTS, results);
                                } finally {
                                    // stores the new one if successful; restores the old one if we failed for some reason to register
                                    // Note that we don't retry even if storing the token fails since this kind
                                    // of failure is probably not recoverable even if we try again.
                                    agent_config.setAgentSecurityToken(token);
                                    m_commServices.addCustomData(
                                        SecurityTokenCommandAuthenticator.CMDCONFIG_PROP_SECURITY_TOKEN, token);

                                    LOG.debug(AgentI18NResourceKeys.NEW_SECURITY_TOKEN, token);
                                }

                                storeServerFailoverList(failover_list);
                                m_serverFailoverList = failover_list;

                                // switch away from the registration server and point this agent to the top of the list
                                // - this is our primary server that we should connect to
                                // note that if we are already pointing to the one at the head of the failover list,
                                // we don't have to failover to another server; the current one is the one we already want
                                if (failover_list.hasNext()) {
                                    String currentAddress = agent_config.getServerBindAddress();
                                    int currentPort = agent_config.getServerBindPort();
                                    String currentTransport = agent_config.getServerTransport();
                                    ServerEntry nextServer = failover_list.peek();

                                    if (currentAddress.equals(nextServer.address)
                                        && currentPort == (SecurityUtil.isTransportSecure(currentTransport) ? nextServer.securePort
                                            : nextServer.port)) {
                                        // we are already pointing to the primary server, so all we have to do is
                                        // call next to move the index to the next in the list for when we have to failover in the future
                                        nextServer = failover_list.next();

                                        // [mazz] I don't think we need to do this here anymore - with the addition of
                                        // the remote communicator's initialize callback, this connect request
                                        // will be made the next time a command is sent by the sender
                                        //sendConnectRequestToServer(sender.getRemoteCommunicator(), false);
                                    } else {
                                        failoverToNewServer(sender.getRemoteCommunicator());
                                    }
                                }
                            }
                        }
                    } catch (AgentNotSupportedException anse) {
                        m_registration = null;
                        retry = false;

                        String cause = ThrowableUtil.getAllMessages(anse);
                        LOG.fatal(AgentI18NResourceKeys.AGENT_NOT_SUPPORTED, cause);
                        getOut().println(MSG.getMsg(AgentI18NResourceKeys.AGENT_NOT_SUPPORTED, cause));

                        // attempt to update the agent immediately.
                        // we are running in a daemon thread, we can hang out and wait (pass in wait=true)
                        AgentUpdateThread.updateAgentNow(AgentMain.this, true);
                    } catch (AgentRegistrationException are) {
                        m_registration = null;

                        String cause = ThrowableUtil.getAllMessages(are);
                        LOG.error(AgentI18NResourceKeys.AGENT_REGISTRATION_REJECTED, cause);

                        // this error is bad - if the agent is starting up, this failure will cause it to hang forever
                        // so let's print a message to the console in addition to the logs to make sure the user sees it
                        getOut().println(MSG.getMsg(AgentI18NResourceKeys.AGENT_REGISTRATION_REJECTED, cause));

                        // under certain conditions we actually should retry this.
                        // For example, if this agent can't ack the server's ping fast enough, the server will think
                        // this agent cannot be connected to and thus reject the registration. If we simply retry
                        // we could eventually succeed with the registration request.
                        registrationFailures++;
                        if (registrationFailures < MAX_ALLOWED_REGISTRATION_FAILURES) {
                            retry = true;
                            retry_interval = 30000L;
                            LOG.error(AgentI18NResourceKeys.AGENT_REGISTRATION_RETRY);
                            getOut().println(MSG.getMsg(AgentI18NResourceKeys.AGENT_REGISTRATION_RETRY));
                        } else {
                            getOut().println(MSG.getMsg(AgentI18NResourceKeys.AGENT_CANNOT_REGISTER));
                            retry = false;
                        }
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        LOG.debug(AgentI18NResourceKeys.AGENT_REGISTRATION_ABORTED);
                        retry = false;
                    } catch (Throwable t) {
                        if (!got_registered) {
                            retry_interval = (retry_interval < 60000L) ? (retry_interval * 2) : 60000L;
                            LOG.warn(t, AgentI18NResourceKeys.AGENT_REGISTRATION_FAILURE, retry, retry_interval,
                                ThrowableUtil.getAllMessages(t));
                        } else {
                            LOG.warn(t, AgentI18NResourceKeys.AGENT_POSTREGISTRATION_FAILURE, m_registration,
                                ThrowableUtil.getAllMessages(t));
                        }
                    }
                }

                // kill the thread since either:
                // a) the thread has been interrupted,
                // b) the registration command was successfully sent
                // c) the agent has shutdown
                return;
            }
        };

        // another paraniod synchronization - just in case multiple threads attempt to concurrently register
        // this agent, this assures that only one registration thread is running - any old thread that
        // may still be running will be interrupted (which will eventually cause it to die).  Its
        // OK if more than one registration command is sent via the task, that is concurrent-safe.
        // This just ensures that only one registration thread continues to run.

        Thread thread;

        synchronized (m_registrationThread) {
            thread = m_registrationThread[0];
            if (thread != null) {
                thread.interrupt(); // make sure the old thread eventually dies
            }

            thread = new Thread(task, "RHQ Agent Registration Thread");
            thread.setDaemon(true);
            m_registrationThread[0] = thread;
            thread.start();
        }

        if (wait > 0L) {
            try {
                thread.join(wait);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        return;
    }

    /**
     * Returns non-<code>null</code> registration information if this agent successfully registered itself during this
     * agent VM's lifetime.
     *
     * @return agent registration information
     *
     * @see    #registerWithServer(long, boolean)
     */
    public AgentRegistrationResults getAgentRegistration() {
        return m_registration;
    }

    /**
     * This sleeps (blocking the calling thread) for at most the given number of milliseconds waiting for the RHQ Server
     * to be discovered. This will return <code>true</code> if the server has come up and detected by the agent; <code>
     * false</code> otherwise.
     *
     * <p/>
     * <p>If <code>wait_ms</code> is 0 or less, this method will not wait at all; it will simply return the state of the
     * server at is is known at the time the call is made.</p>
     *
     * <p/>
     * <p>Note that if the agent is shutdown, this method will not wait no matter what <code>wait_ms</code> is. If the
     * agent is shutdown, <code>false</code> is returned immediately.</p>
     *
     * @param  wait_ms maximum number of milliseconds to wait
     *
     * @return <code>true</code> if the server is up, <code>false</code> if it is not yet up or the agent has shutdown
     *
     * @throws AgentNotSupportedException If the server is up but it told us we are the wrong version, then this is thrown.
     *                                    When this is thrown, the agent is currently in the midst of updating itself.
     */
    public boolean waitForServer(long wait_ms) throws AgentNotSupportedException {
        final CountDownLatch latch = new CountDownLatch(1);

        // when the sender has started, it means the RHQ Server has come up - here's our listener for that
        ClientCommandSenderStateListener listener = new ClientCommandSenderStateListener() {
            public boolean startedSending(ClientCommandSender sender) {
                latch.countDown();
                return false; // no need to keep listening
            }

            public boolean stoppedSending(ClientCommandSender sender) {
                return true; // no-op but keep listening
            }
        };

        ClientCommandSender sender_to_server = getClientCommandSender();

        boolean zero = false;
        if (sender_to_server != null) {
            LOG.debug(AgentI18NResourceKeys.WAITING_FOR_SERVER, wait_ms);
            sender_to_server.addStateListener(listener, true);
            try {
                zero = latch.await(wait_ms, TimeUnit.MILLISECONDS);
                // if this agent is updating, break the loop immediately by throwing exception
                if (AgentUpdateThread.isUpdatingNow()) {
                    throw new AgentNotSupportedException();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                sender_to_server.removeStateListener(listener);
            }
        } else {
            LOG.debug(AgentI18NResourceKeys.CANNOT_WAIT_FOR_SERVER);
        }
        return zero;
    }

    /**
     * Returns <code>true</code> if this agent is known to have previously been registered with the server. A <code>
     * false</code> means this agent needs to register with the server before it can successfully communicate with the
     * server.
     *
     * @return <code>true</code> if the agent is registered with the server
     */
    public boolean isRegistered() {
        return getAgentSecurityToken() != null;
    }

    /**
     * This schedules some startup work to be done that needs the server to be up. This method simply "schedules" this
     * work - the work will actually be performed once the RHQ Server has been detected (technically, it happens when
     * the {@link #getClientCommandSender() sender} has started sending commands to the server but that happens when the
     * RHQ Server has been detected). This method returns immediately, whether or not the work actually was performed or
     * not.
     *
     * <p/>
     * <p>The work this method schedules is:</p>
     *
     * <p/>
     * <ul>
     *   <li>Registering with the server (if the agent needs to do so at startup)</li>
     *   <li>Setup a conditional restart of the plugin container, see {@link PluginContainerConditionalRestartListener}</li>
     * </ul>
     *
     * @return <code>true</code> if the agent is not registered or the agent was asked to re-register with the server;
     *         <code>false</code> if this agent is already registered with the server (i.e. it has a security token
     *         which indicates this agent had previously registered with the server)
     */
    private boolean prepareStartupWorkRequiringServer() {
        // First determine if the agent is configured to register with the server automatically at startup.
        // If it is not, we need to make sure that, if it needs to be, it has been registered already. We do so by
        // first determining if there is a security preprocessor configured and if so, make sure it has a token available.
        // If there is no token available and the agent needs one, it has to register, whether or not we were told to.
        // This happens when someone clears the security token.

        boolean register = m_configuration.isRegisterWithServerAtStartupEnabled();
        if (!register) {
            if (!isRegistered()) {
                register = true;
                LOG.info(AgentI18NResourceKeys.FORCING_AGENT_REGISTRATION);
            }
        }

        // the very first state listener we add MUST be the register state listener
        // because we must first register before we can send any other commands to the server
        if (register) {
            m_clientSender.addStateListener(new RegisterStateListener(), true);
        }

        //the next thing is to setup the conditional restart of the PC if it fails to merge
        //the upgrade results with the server due to some network glitch
        m_clientSender.addStateListener(new PluginContainerConditionalRestartListener(), false);

        return register;
    }

    /**
     * Management method to manually update plugins.
     * This method will fail if the server is down.
     * @throws IllegalStateException if the container is not initialized
     * @throws RuntimeException for any other reason (failed to download, etc.)
     */
    public void updatePlugins() {
        if (m_pluginUpdate == null) {
            throw new IllegalStateException("plugin update uninitialized");
        }
        try {
            m_pluginUpdate.updatePlugins();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This starts up the plugin container. The caller must ensure that the agent is either started or is being started.
     * The plugin container must ensure that the agent is registered before it can be initialized, therefore, this
     * method will ensure the agent is registered before starting the plugin container. If the agent is not registered,
     * this method will wait for the agent to become registered for at most <code>wait_for_registration</code>
     * millisecond. This method will wait indefinitely if that parameter's value is 0 or less. This method will also
     * wait indefinitely until one or more plugins are available.
     *
     * @param  wait_for_registration the amount of milliseconds this method will wait for the agent to become registered
     *                               with the server; if 0 or less, this method blocks <b>indefinitely</b>
     *
     * @return <code>true</code> if the plugin container is started, <code>false</code> if it did not start
     */
    public boolean startPluginContainer(long wait_for_registration) {
        PluginContainer plugin_container = PluginContainer.getInstance();

        if (plugin_container.isStarted()) {
            return true;
        }

        plugin_container.setRebootRequestListener(new RebootRequestListener() {
            public void reboot() {
                try {
                    shutdown();
                } catch (Throwable t) {
                    LOG.error(t, "The plugin container has requested the agent be restarted but the shutdown "
                        + "operation failed.");
                }
                if (isStarted()) {
                    // Pause for a few seconds and try to shut down one more time.
                    try {
                        Thread.sleep(3000);
                        shutdown();
                    } catch (Throwable t) {
                        LOG.error(t, "The plugin container has requested the agent be restarted but the shutdown "
                            + "operation failed again.");
                    }
                }
                if (isStarted()) {
                    // At this point agent shut down failed twice. We can get by with rebooting the plugin container
                    // so we will try that as last ditch effort.

                    LOG.warn("The agent shut down operation has failed twice. Attempting to reboot the plugin "
                        + "container so that stale resource types and deleted plugins are removed.");
                    try {
                        rebootPluginContainer();
                    } catch (Throwable t) {
                        // If rebooting the plugin container fails as well, there is not much else we can do besides
                        // reporting the failures. Manual intervention is needed.
                        LOG.error("The agent could not be shut down and rebooting the plugin container failed. "
                            + "Please check the logs for errors and manually restart the agent as soon as "
                            + "possible.");
                        return;
                    }
                } else {
                    try {
                        cleanDataDirectory();
                    } catch (Throwable t) {
                        LOG.warn(t, "The plugin container has requested the agent be restarted but purging the "
                            + "data directory failed.");
                    }
                    try {
                        start();
                    } catch (Throwable t) {
                        LOG.warn(t, "An error occurred while trying to restart the agent. Attempting restart "
                            + "one more time");
                        try {
                            shutdown();
                            start();
                        } catch (Throwable t1) {
                            LOG.error(t1, "Restarting the agent has failed. Please check the logs for errors and "
                                + "manually restart the agent as soon as possible.");
                            return;
                        }
                    }
                    getAgentRestartCounter().restartedAgent(AgentRestartReason.STALE_INVENTORY);
                }
            }

            private void rebootPluginContainer() throws Throwable {
                PluginContainer pc = PluginContainer.getInstance();

                if (pc.isStarted()) {
                    try {
                        shutdownPluginContainer();
                    } catch (Throwable t) {
                        LOG.error(t, "The plugin container shut down operation failed.");
                        throw t;
                    }
                    try {
                        startPluginContainer(0L);
                    } catch (Throwable t) {
                        LOG.error(t, "The plugin container was shut down but an error occurred trying to restart it.");
                        throw t;
                    }
                } else {
                    LOG.warn("The plugin container is already shut down. Attempting to restart it...");
                    try {
                        startPluginContainer(0L);
                    } catch (Throwable t) {
                        LOG.error(t, "The plugin container was shut down but an error occurred trying to restart it.");
                        throw t;
                    }
                }
            }
        });

        // if wait param is <=0 this BLOCKS INDEFINITELY! If the server is down, this will never return until it comes back up
        try {
            waitToBeRegistered(wait_for_registration);
        } catch (Exception e) {
            LOG.debug(AgentI18NResourceKeys.PC_START_FAILED_WAITING_FOR_REGISTRATION, ThrowableUtil.getAllMessages(e));
            return false;
        }

        // Now we need to build our plugin container configuration.
        // All static configuration settings are retrieved directly from the agent configuration object,
        // but we also have to build our dynamic runtime objects that are also part of the PC config (which
        // includes things like the plugin finder object and the remoted services).
        PluginContainerConfiguration pc_config = m_configuration.getPluginContainerConfiguration();
        pc_config.setPluginFinder(new FileSystemPluginFinder(pc_config.getPluginDirectory()));

        try {
            LOG.debug(AgentI18NResourceKeys.CREATING_PLUGIN_CONTAINER_SERVER_SERVICES);

            // Get remote pojo's for server access and make them accessible in the configuration object
            ClientRemotePojoFactory factory = m_clientSender.getClientRemotePojoFactory();
            CoreServerService coreServerService = factory.getRemotePojo(CoreServerService.class);
            DiscoveryServerService discoveryServerService = factory.getRemotePojo(DiscoveryServerService.class);
            MeasurementServerService measurementServerService = factory.getRemotePojo(MeasurementServerService.class);
            OperationServerService operationServerService = factory.getRemotePojo(OperationServerService.class);
            ConfigurationServerService configurationServerService = factory
                .getRemotePojo(ConfigurationServerService.class);
            ResourceFactoryServerService resourceFactoryServerSerfice = factory
                .getRemotePojo(ResourceFactoryServerService.class);
            ContentServerService contentServerService = factory.getRemotePojo(ContentServerService.class);
            EventServerService eventServerService = factory.getRemotePojo(EventServerService.class);
            BundleServerService bundleServerService = factory.getRemotePojo(BundleServerService.class);
            DriftServerService driftServerService = factory.getRemotePojo(DriftServerService.class);

            ServerServices serverServices = new ServerServices();
            serverServices.setCoreServerService(coreServerService);
            serverServices.setDiscoveryServerService(discoveryServerService);
            serverServices.setMeasurementServerService(measurementServerService);
            serverServices.setOperationServerService(operationServerService);
            serverServices.setConfigurationServerService(configurationServerService);
            serverServices.setResourceFactoryServerService(resourceFactoryServerSerfice);
            serverServices.setContentServerService(contentServerService);
            serverServices.setEventServerService(eventServerService);
            serverServices.setBundleServerService(bundleServerService);
            serverServices.setDriftServerService(driftServerService);

            pc_config.setServerServices(serverServices);
        } catch (Exception e) {
            LOG.error(e, AgentI18NResourceKeys.FAILED_TO_CREATE_PLUGIN_CONTAINER_SERVER_SERVICES, e);
            return false;
        }

        File plugin_dir = pc_config.getPluginDirectory();

        // we block until we get our plugins - there is no sense continuing until we have plugins
        // there may be instances, though, where we don't want to block (in unit tests for example)
        // so allow this to be configurable via the "update plugins at startup" flag.
        m_pluginUpdate = new PluginUpdate(pc_config.getServerServices().getCoreServerService(), pc_config);
        if (m_configuration.isUpdatePluginsAtStartupEnabled()) {
            boolean notified_user = false;
            // this can block forever...perhaps exit after a few tries?
            while (true) {
                if (!notified_user) {
                    LOG.info(AgentI18NResourceKeys.WAITING_FOR_PLUGINS_WITH_DIR, plugin_dir);
                    getOut().println(MSG.getMsg(AgentI18NResourceKeys.WAITING_FOR_PLUGINS));
                    notified_user = true;
                } else {
                    // let's keep logging this at debug level so we don't look hung
                    LOG.debug(AgentI18NResourceKeys.WAITING_FOR_PLUGINS_WITH_DIR, plugin_dir);
                }
                try {
                    m_pluginUpdate.updatePlugins();
                    break;
                } catch (Exception e) {
                    LOG.error(e, AgentI18NResourceKeys.UPDATING_PLUGINS_FAILURE, e);
                }
            }
        } else if (plugin_dir.list().length == 0) {
            LOG.warn(AgentI18NResourceKeys.NO_PLUGINS);
            getOut().println(MSG.getMsg(AgentI18NResourceKeys.NO_PLUGINS));
            return false;
        }

        // tell the plugin container how it should be configured
        plugin_container.setConfiguration(pc_config);

        // add the agent as a listener to the plugin container services lifecycle events and so it can remote streams
        AgentServiceRemoter agentServiceRemoter = new AgentServiceRemoter(this);
        plugin_container.addAgentServiceLifecycleListener(agentServiceRemoter);
        plugin_container.setAgentServiceStreamRemoter(agentServiceRemoter);
        plugin_container.setAgentRegistrar(new AgentRegistrarImpl(this));

        // the plugin container is now fully configured and can be initialized
        plugin_container.initialize();
        LOG.debug(AgentI18NResourceKeys.PLUGIN_CONTAINER_INITIALIZED, pc_config);

        return plugin_container.isStarted();
    }

    /**
     * Tell the agent to immediately switch to another server.  The given
     * server can be a simple hostname in which case, the current transport,
     * port and transport parameters being used to talk to the current server
     * will stay the same.  Otherwise, it will be assumed the server is a
     * full endpoint URL.
     *
     * @param server the host of the server to switch to, or a full server endpoint URL
     *
     * @return <code>true</code> if successfully switched, <code>false</code> otherwise
     */
    public boolean switchToServer(String server) {
        AgentConfiguration config = getConfiguration();
        String currentServerAddress = config.getServerBindAddress();
        int currentServerPort = config.getServerBindPort();
        String currentServerTransport = config.getServerTransport();
        String currentServerTransportParams = config.getServerTransportParams();

        ServerEndpoint newServer;

        try {
            newServer = AgentUtils.getServerEndpoint(config, server);
        } catch (Exception e) {
            LOG.warn(AgentI18NResourceKeys.CANNOT_SWITCH_TO_INVALID_SERVER, server, ThrowableUtil.getAllMessages(e));
            return false;
        }

        RemoteCommunicator comm;

        try {
            comm = getClientCommandSender().getRemoteCommunicator();
            if (comm == null) {
                throw new IllegalStateException(); // i don't think this will ever happen, but just in case
            }
        } catch (Exception e) {
            LOG.warn(AgentI18NResourceKeys.CANNOT_SWITCH_NULL_COMMUNICATOR, server, ThrowableUtil.getAllMessages(e));
            return false;
        }

        // remember this in case we fail - we have to revert back to the original server we were talking to
        String originalServerEndpoint = comm.getRemoteEndpoint();

        // need to synch on last failover time so we don't clash with the real failover stuff
        synchronized (m_lastFailoverTime) {
            boolean ok = switchCommServer(comm, newServer.namePort, newServer.transport, newServer.transportParams);
            if (!ok) {
                try {
                    // we are switching back to the original server because our switch failed
                    comm.setRemoteEndpoint(originalServerEndpoint);
                    config.setServerLocatorUri(currentServerTransport, currentServerAddress, currentServerPort,
                        currentServerTransportParams);
                } catch (Exception e) {
                    // this should never happen
                    LOG.warn(AgentI18NResourceKeys.CANNOT_SWITCH_TO_INVALID_SERVER, originalServerEndpoint, e);
                }
            }
            return ok;
        }
    }

    /**
     * Switches the agent to talk to the next server in the failover list.
     *
     * This is package-scoped so the failover callback can call this.
     *
     * @param comm the communicator object whose endpoint needs to be switched to the next server
     *             the caller must ensure the remote communicator provided to this method is the
     *             same communicator used by this agent's {@link #getClientCommandSender() sender}.
     */
    void failoverToNewServer(RemoteCommunicator comm) {
        synchronized (m_lastFailoverTime) {

            // if we just recently (within the last 10s) switched, don't try to do anything and abort
            if (System.currentTimeMillis() - m_lastFailoverTime[0] < 10000) {
                return;
            }

            FailoverListComposite failoverList = getServerFailoverList();
            if (failoverList.hasNext() == false) {
                return;
            }

            AgentConfiguration config = getConfiguration();
            String currentTransport = config.getServerTransport();
            String currentTransportParams = config.getServerTransportParams();

            ServerEntry nextServer = failoverList.next();

            boolean ok = switchCommServer(comm, nextServer, currentTransport, currentTransportParams);
            if (ok) {
                // we've successfully failed over; the design of the HA system calls for us to start
                // over at the top of the failover list the next time we get a failure.
                failoverList.resetIndex();

                m_lastFailoverTime[0] = System.currentTimeMillis();
            }
            return;
        }
    }

    /**
     * Immediately switches the given communicator to the given server.
     *
     * @param comm the communicator whose server is switched
     * @param newServer the endpoint of the new server
     * @param transport the transport that should be used in the new remote endpoint URL
     * @param transportParams the transport params that should be used in the new remote endpoint URL
     *
     * @return <code>true</code> if successfully switched; <code>false</code> otherwise
     */
    private boolean switchCommServer(RemoteCommunicator comm, ServerEntry newServer, String transport,
        String transportParams) {

        AgentConfiguration config = getConfiguration();

        // our auto-discovery listener needs to be recreated since it needs the new server locator URL
        // so get rid of the old one.
        unprepareAutoDiscoveryListener();

        try {
            // set the new server locator information into config
            config.setServerLocatorUri(transport, newServer.address,
                (SecurityUtil.isTransportSecure(transport)) ? newServer.securePort : newServer.port, transportParams);

            try {
                // tell the comm object about the new server
                // NOTE! callers should be synchronized already on lastFailoverTime - so we are thread safe
                comm.setRemoteEndpoint(config.getServerLocatorUri());

                // send the connect message to tell the server we want to talk to it (also verifies that server is up)
                sendConnectRequestToServer(comm, false);
            } catch (Throwable t) {
                LOG.warn(AgentI18NResourceKeys.FAILOVER_FAILED, t);
                // TODO: I am unsure if this causes deadlocks or other problems, but consider
                // disconnecting the comm object here to force the initialize callback to be triggered again.
                // This will ensure we attempt to send the connect request again.  I left this commented out
                // because I don't know what happens if we are called in a thread that is already currently
                // attempting to send a connect request.
                // comm.disconnect();
                return false;
            }

            LOG.info(AgentI18NResourceKeys.FAILED_OVER_TO_SERVER, config.getServerLocatorUri());

        } finally {
            // now that we switched, re-setup discovery
            try {
                prepareAutoDiscoveryListener();
            } catch (Exception e) {
                LOG.info(AgentI18NResourceKeys.FAILOVER_DISCOVERY_START_FAILURE, ThrowableUtil.getAllMessages(e));
            }
        }

        return true;
    }

    /**
     * This sends a direct "connect" request to the server pointed to by the given communicator.
     * The purpose of this is very important - this request tells the server that this agent
     * is making the server its primary server and will begin sending it messages. The request
     * is sent such that the communicator's initialize callback will never be invoked, however,
     * the caller can ask for the request to attempt failover.
     *
     * <p>This is package scoped so the initialize callback can call this</p>
     *
     * @param comm the communicator used to send the message to the server
     * @param attemptFailover if <code>true</code>, and the connect command fails, server failover will be attempted
     *
     * @throws Throwable
     */
    void sendConnectRequestToServer(RemoteCommunicator comm, boolean attemptFailover) throws Throwable {

        // be careful callers - make sure the comm's remote endpoint won't change underneath us!
        // if the comm's remote endpoint is changed by some other thread while we are in here,
        // we could send the connectAgent to the wrong server and not know it

        // acquire the write lock but abort if its been a while, this should avoid any possible deadlock in the future
        if (!m_lastSentConnectAgent.rwLock.writeLock().tryLock(120, TimeUnit.SECONDS)) {
            throw new IllegalStateException(MSG.getMsg(AgentI18NResourceKeys.TIMEOUT_WAITING_FOR_CONNECT_LOCK));
        }

        try {
            if (m_lastSentConnectAgent.serverEndpoint.equals(comm.getRemoteEndpoint())
                && (System.currentTimeMillis() - m_lastSentConnectAgent.timestamp) < 30000L) {
                LOG.info(AgentI18NResourceKeys.NOT_SENDING_DUP_CONNECT, m_lastSentConnectAgent);
                return;
            }

            Command connectCommand = createConnectAgentCommand();
            getClientCommandSender().preprocessCommand(connectCommand); // important that we are already registered by now!
            CommandResponse connectResponse;

            if (attemptFailover) {
                connectResponse = comm.sendWithoutInitializeCallback(connectCommand);
            } else {
                connectResponse = comm.sendWithoutCallbacks(connectCommand);
            }

            if (!connectResponse.isSuccessful()) {
                Throwable exception = connectResponse.getException();
                if (exception != null) {
                    if (exception.getCause() instanceof AgentNotSupportedException) {
                        exception = exception.getCause(); // this happens if we got InvocationTargetException
                    }
                    throw exception;
                } else {
                    throw new Exception("FAILED: " + connectCommand);
                }
            }

            m_lastSentConnectAgent.timestamp = System.currentTimeMillis();
            m_lastSentConnectAgent.serverEndpoint = comm.getRemoteEndpoint();

            try {
                ConnectAgentResults results = (ConnectAgentResults) connectResponse.getResults();
                long serverTime = results.getServerTime();
                serverClockNotification(serverTime);

                // If the server thinks we are down, we need to do some things to get this agent in sync with the server.
                // Anything we do in here should be very fast.
                boolean serverThinksWeAreDown = results.isDown();
                if (serverThinksWeAreDown) {
                    LOG.warn(AgentI18NResourceKeys.SERVER_THINKS_AGENT_IS_DOWN);
                    PluginContainer plugin_container = PluginContainer.getInstance();
                    if (plugin_container.isStarted()) {
                        // tell the plugin container to send a full avail report up so the server knows we are UP
                        plugin_container.getInventoryManager().requestFullAvailabilityReport();
                    }
                }
            } catch (Throwable t) {
                // should never happen, should always cast to non-null ConnectAgentResults
                LOG.error(AgentI18NResourceKeys.TIME_UNKNOWN, ThrowableUtil.getAllMessages(t));
            }
        } catch (AgentNotSupportedException anse) {
            String cause = ThrowableUtil.getAllMessages(anse);
            LOG.fatal(AgentI18NResourceKeys.AGENT_NOT_SUPPORTED, cause);
            getOut().println(MSG.getMsg(AgentI18NResourceKeys.AGENT_NOT_SUPPORTED, cause));

            AgentUpdateThread.updateAgentNow(AgentMain.this, false);
            m_lastSentConnectAgent.timestamp = 0L;
            Thread.sleep(1000L);
            throw anse;
        } catch (Throwable t) {
            // error, so allow any quick call back to this method to try it again
            m_lastSentConnectAgent.timestamp = 0L;
            throw t;
        } finally {
            m_lastSentConnectAgent.rwLock.writeLock().unlock();
        }

        return;
    }

    private Command createConnectAgentCommand() throws Exception {
        AgentConfiguration config = getConfiguration();
        String agentName = config.getAgentName();
        AgentVersion version = new AgentVersion(Version.getProductVersion(), Version.getBuildNumber());
        ConnectAgentRequest request = new ConnectAgentRequest(agentName, version);

        RemotePojoInvocationCommand connectCommand = new RemotePojoInvocationCommand();
        Method connectMethod = CoreServerService.class.getMethod("connectAgent", ConnectAgentRequest.class);
        NameBasedInvocation inv = new NameBasedInvocation(connectMethod, new Object[] { request });
        connectCommand.setNameBasedInvocation(inv);
        connectCommand.setTargetInterfaceName(CoreServerService.class.getName());
        return connectCommand;
    }

    /**
     * Returns the agent restart counter object.
     *
     * @return the agent restart counter
     */
    public AgentRestartCounter getAgentRestartCounter() {
        return m_agentRestartCounter;
    }

    /**
     * This will purge all plugins in the agent's plugin directory.
     */
    private void cleanPluginsDirectory() {
        try {
            File plugins_dir = m_configuration.getPluginContainerConfiguration().getPluginDirectory();
            if (plugins_dir.exists()) {
                LOG.info(AgentI18NResourceKeys.CLEANING_PLUGINS_DIRECTORY, plugins_dir.getAbsolutePath());
                cleanFile(plugins_dir);
            }
        } catch (Exception e) {
            LOG.warn(AgentI18NResourceKeys.CLEAN_PLUGINS_FAILURE, e);
        }

        return;
    }

    /**
     * This will purge all files in the agent's data directory.
     */
    private void cleanDataDirectory() {
        try {
            File data_dir = m_configuration.getDataDirectory();
            if (data_dir.exists()) {
                LOG.info(AgentI18NResourceKeys.CLEANING_DATA_DIRECTORY, data_dir.getAbsolutePath());
                cleanFile(data_dir);
            }

            // it is concievable the comm services data directory was configured in a different
            // place than where the agent's data directory is - make sure we clean out that other data dir
            File comm_data_dir = m_configuration.getServiceContainerPreferences().getDataDirectory();
            if (!comm_data_dir.getAbsolutePath().equals(data_dir.getAbsolutePath())) {
                if (comm_data_dir.exists()) {
                    LOG.info(AgentI18NResourceKeys.CLEANING_DATA_DIRECTORY, comm_data_dir.getAbsolutePath());
                    cleanFile(comm_data_dir);
                }
            }
        } catch (Exception e) {
            LOG.warn(AgentI18NResourceKeys.CLEAN_DATA_DIR_FAILURE, e);
        }

        return;
    }

    /**
     * This will delete the given file and if its a directory, will recursively delete its contents and its
     * subdirectories.
     *
     * @param file the file/directory to delete
     */
    private void cleanFile(File file) {
        boolean deleted;

        File[] doomed_files = file.listFiles();
        if (doomed_files != null) {
            for (File doomed_file : doomed_files) {
                cleanFile(doomed_file); // call this method recursively
            }
        }

        deleted = file.delete();
        LOG.debug(AgentI18NResourceKeys.CLEANING_FILE, file, deleted);

        return;
    }

    /**
     * This will disable the native system if the agent was actually configured to do so. This method will not actually
     * load in or initialize the native system. That will happen lazily, when the native system is actually needed. Note
     * that if the agent was told to disable the native system from a command line option, the native system will be
     * disabled no matter what this method decides to do.
     */
    private void prepareNativeSystem() {
        if (m_disableNativeSystem || m_configuration.isNativeSystemDisabled()) {
            if (!SystemInfoFactory.isNativeSystemInfoDisabled()) {
                SystemInfoFactory.disableNativeSystemInfo();
                LOG.info(AgentI18NResourceKeys.NATIVE_SYSTEM_DISABLED);
            }
            this.m_loggedNativeSystemInfoUnavailableWarning = false;
        } else {
            if (!SystemInfoFactory.isNativeSystemInfoAvailable()) {
                if (!this.m_loggedNativeSystemInfoUnavailableWarning) {
                    Throwable t = SystemInfoFactory.getNativeLibraryLoadThrowable();
                    if (LOG.isDebugEnabled()) {
                        LOG.debug(AgentI18NResourceKeys.NATIVE_SYSINFO_UNAVAILABLE_DEBUG, t);
                    } else {
                        LOG.warn(AgentI18NResourceKeys.NATIVE_SYSINFO_UNAVAILABLE);
                    }
                    this.m_loggedNativeSystemInfoUnavailableWarning = true;
                }
            } else {
                this.m_loggedNativeSystemInfoUnavailableWarning = false;
            }
        }

        return;
    }

    /**
     * This creates and starts both the server-side comm services and the client-side sender. The given listener will be
     * added to the service container thus allowing the comm services to start up but not begin processing incoming
     * messages. The caller should make sure it opens the latch of the given listener at some point in the future.
     *
     * @param  listener a listener that will be added to the service container
     *
     * @throws Exception if any error causes the startup to fail
     */
    private void startCommServices(BootstrapLatchCommandListener listener) throws Exception {
        // BZ 951382 - if any locations for keystore/truststore are not configured, set our own defaults to the conf/ directory
        assignDefaultLocationToKeystoreTruststoreFiles();

        // create our client sender so we can send commands to our server
        // do this before we create our auto-discovery listener and start the server-side services
        // since it will be needed by the listener very quickly if the server is already online
        m_clientSender = createClientCommandSender();

        // this is our server-side container that will manage our connector, network register and other services
        m_commServices = new ServiceContainer();

        // this listener will enable the agent to complete the setup of the security token preprocessor
        m_commServices.addServiceContainerSenderCreationListener(new SenderCreationListener());

        // add the command listener that was passed to us
        m_commServices.addCommandListener(listener);

        // In case we are already registered from a previous run, store the token in the custom data - this is used
        // by the security token authenticator in order to authenticate the remote endpoint sending us messages
        m_commServices.addCustomData(SecurityTokenCommandAuthenticator.CMDCONFIG_PROP_SECURITY_TOKEN,
            m_configuration.getAgentSecurityToken());

        // this listener will enable our sender as soon as we receive a message from the server
        CommandListenerStateListener commandListenerStateListener = new CommandListenerStateListener();
        m_clientSender.addStateListener(commandListenerStateListener, true);

        // if we are configured to perform auto-detection of the server, create our listener now
        prepareAutoDiscoveryListener();

        // initialize and start the server-side services so we can process incoming commands
        m_commServices.start(m_configuration.getPreferences(), m_configuration.getClientCommandSenderConfiguration());

        // prime the sender so it can be prepared to start sending messages.
        // if auto-discovery is enabled, then the auto-discovery listener will tell the sender when its OK to start
        // sending.  Otherwise start polling and let the poller tell the sender when it is ok to start sending.
        if (!isAutoDiscoveryEnabled()) {
            LOG.info(AgentI18NResourceKeys.NO_AUTO_DETECT);
            m_clientSender.startServerPolling();
        }

        return;
    }

    private void assignDefaultLocationToKeystoreTruststoreFiles() {
        File confDir = new File("conf");
        if (!confDir.exists()) {
            return; // conf/ doesn't exist (perhaps we are running in a test?) - do nothing and just fallback to the standard defaults
        }

        String prefNamesFileNames[][] = {
            { ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_KEYSTORE_FILE,
                ServiceContainerConfigurationConstants.DEFAULT_CONNECTOR_SECURITY_KEYSTORE_FILE_NAME },
            { ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_TRUSTSTORE_FILE,
                ServiceContainerConfigurationConstants.DEFAULT_CONNECTOR_SECURITY_TRUSTSTORE_FILE_NAME },
            { AgentConfigurationConstants.CLIENT_SENDER_SECURITY_KEYSTORE_FILE,
                AgentConfigurationConstants.DEFAULT_CLIENT_SENDER_SECURITY_KEYSTORE_FILE_NAME },
            { AgentConfigurationConstants.CLIENT_SENDER_SECURITY_TRUSTSTORE_FILE,
                AgentConfigurationConstants.DEFAULT_CLIENT_SENDER_SECURITY_TRUSTSTORE_FILE_NAME } };

        Preferences prefs = m_configuration.getPreferences();
        for (String[] prefNameFileName : prefNamesFileNames) {
            String value = prefs.get(prefNameFileName[0], null);
            if (value == null) {
                value = new File(confDir, prefNameFileName[1]).getAbsolutePath();
                prefs.put(prefNameFileName[0], value);
                LOG.debug(AgentI18NResourceKeys.CERT_FILE_LOCATION, prefNameFileName[0], value);
            }
        }

        try {
            prefs.flush();
        } catch (Exception e) {
            LOG.warn(AgentI18NResourceKeys.CANNOT_STORE_PREFERENCES, "keystore/truststore files", e);
        }

        return;
    }

    private boolean isAutoDiscoveryEnabled() {
        return m_autoDiscoveryListener != null;
    }

    /**
     * This will prepare the auto-discovery listener, if server auto-detection is enabled.
     *
     * @throws Exception
     */
    private void prepareAutoDiscoveryListener() throws Exception {
        if (m_configuration.isServerAutoDetectionEnabled()) {
            ServiceContainerConfiguration comm_config = new ServiceContainerConfiguration(
                m_configuration.getPreferences());
            if (comm_config.isMulticastDetectorEnabled()) {
                m_autoDiscoveryListener = new AgentAutoDiscoveryListener(this, createServerRemoteCommunicator(null,
                    false, false));
                m_commServices.addDiscoveryListener(m_autoDiscoveryListener);
                LOG.debug(AgentI18NResourceKeys.SERVER_AUTO_DETECT_ENABLED, m_configuration.getServerLocatorUri());
            } else {
                LOG.warn(AgentI18NResourceKeys.WEIRD_AUTO_DETECT_CONFIG);
            }
        }
        return;
    }

    /**
     * If the auto-discovery listener has been created, this will "unprepare" that listener
     * by deregistering it from the comm services.
     */
    private void unprepareAutoDiscoveryListener() {
        if (m_autoDiscoveryListener != null) {
            if (m_commServices != null) {
                m_commServices.removeDiscoveryListener(m_autoDiscoveryListener);
            }

            m_autoDiscoveryListener = null;
        }
    }

    /**
     * Creates the agent's internal management MBeans so the agent, itself, can be managed and monitored.
     *
     * @throws Exception if failed to create and register the management MBeans
     */
    private void startManagementServices() throws Exception {
        if ((m_managementMBean == null) && !m_configuration.doNotEnableManagementServices()) {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            m_managementMBean = new AgentManagement(this);

            ObjectName objectName = ObjectNameFactory.create(AgentManagementMBean.BASE_OBJECT_NAME + ","
                + AgentManagementMBean.KEY_NAME + "=" + m_configuration.getAgentName());

            mbs.registerMBean(m_managementMBean, objectName);
        }

        return;
    }

    /**
     * Destroys the Agent's internal management MBeans - after this returns, the agent can no longer be managed.
     */
    private void stopManagementServices() {
        if (m_managementMBean != null) {
            MBeanServer mbs = m_managementMBean.getMBeanServer();

            if (mbs != null) {
                try {
                    mbs.unregisterMBean(m_managementMBean.getObjectName());
                } catch (Exception ignore) {
                }

                m_managementMBean = null;
            }
        }

        return;
    }

    /**
     * Shuts down the plugin container and all its child services and plugins. If the plugin container has already been
     * shutdown, this method does nothing and returns.
     *
     * @return true if the plugin container shutdown gracefully (i.e. all threads terminated), or false otherwise
     */
    public boolean shutdownPluginContainer() {
        boolean shutdownGracefully = PluginContainer.getInstance().shutdown();
        LOG.debug(AgentI18NResourceKeys.PLUGIN_CONTAINER_SHUTDOWN);
        return shutdownGracefully;
    }

    /**
     * Shuts down both the server-side comm services and the client-side sender.
     */
    private void shutdownCommServices() {
        // no need to still attempt to auto-detect the server, let's remove our listener if we were listening
        unprepareAutoDiscoveryListener();

        // stop our client from sending messages
        if (m_clientSender != null) {
            m_clientSender.stopServerPolling();
            m_clientSender.stopSending(false);
            m_clientSender.disableQueueThrottling();
            m_clientSender.disableSendThrottling();
            m_previouslyQueueCommands = m_clientSender.drainQueuedCommands();
            m_clientSender = null;
        }

        // shutdown the service container and all its services
        if (m_commServices != null) {
            m_commServices.shutdown();
        }

        return;
    }

    /**
     * This notifies the server that this agent is going down. This is called when the agent is shutting down.
     */
    private void notifyServerOfShutdown() {
        try {
            ClientCommandSender sender = getClientCommandSender();

            if (sender.isSending()) {
                LOG.debug(AgentI18NResourceKeys.NOTIFYING_SERVER_OF_SHUTDOWN);

                String agent_name = getConfiguration().getAgentName();
                ClientRemotePojoFactory pojo_factory = sender.getClientRemotePojoFactory();
                pojo_factory.setSendThrottled(false); // send it immediately, avoid any throttling
                pojo_factory.setTimeout(10000L); // try to be quick about it, if it can't send immediately, fail fast
                CoreServerService remote_pojo = pojo_factory.getRemotePojo(CoreServerService.class);

                remote_pojo.agentIsShuttingDown(agent_name);
            } else {
                LOG.debug(AgentI18NResourceKeys.NOT_NOTIFYING_SERVER_OF_SHUTDOWN);
            }
        } catch (Throwable e) {
            LOG.warn(AgentI18NResourceKeys.FAILED_TO_NOTIFY_SERVER_OF_SHUTDOWN, ThrowableUtil.getAllMessages(e));
        }

        return;
    }

    /**
     * Returns the security token if this agent is known to have previously been registered with the server. Returns
     * <code>null</code> if the agent is not registered or doesn't know its registration's token.
     *
     * @return the agent's registration security token, or <code>null</code> if it is not known
     */
    private String getAgentSecurityToken() {
        String token = m_configuration.getAgentSecurityToken();
        if (token == null || token.length() == 0) {
            return null;
        }
        return token;
    }

    /**
     * This method blocks waiting for the agent to be registered with the server. Once the agent becomes registered,
     * this method returns. If <code>wait</code> is 0 or less, this method will block <b>indefinitely</b>. Otherwise, if
     * the agent hasn't registered in <code>wait</code> milliseconds, an exception is thrown.
     *
     * @param  wait the amount of milliseconds this method should block waiting for the agent to be registered
     *
     * @throws RuntimeException if <code>wait</code> milliseconds has elapsed and the agent is still not registered
     * @throws AgentNotSupportedException if the agent can never be registered because it is a different version
     *                                    than the server supports; when this is thrown, the agent is in the
     *                                    midst of updating itself
     */
    private void waitToBeRegistered(long wait) throws RuntimeException, AgentNotSupportedException {
        boolean notified_user = false;
        long started = System.currentTimeMillis();
        long sleep_time = 500L;

        while (!isRegistered()) {
            // We want to print a message to the console to explicitly notify the user.
            // This is because the user may be waiting a long time. If the server is down,
            // this method will never return until it comes back online and we can register with it.
            if (!notified_user) {
                LOG.info(AgentI18NResourceKeys.WAITING_TO_BE_REGISTERED_BEGIN);
                getOut().println(MSG.getMsg(AgentI18NResourceKeys.WAITING_TO_BE_REGISTERED_BEGIN));
                notified_user = true;
            }

            if (wait > 0) {
                long now = System.currentTimeMillis();
                if ((started + wait) < now) {
                    throw new RuntimeException(
                        MSG.getMsg(AgentI18NResourceKeys.CANNOT_WAIT_TO_BE_REGISTERED_ANY_LONGER));
                }
            }

            try {
                Thread.sleep(sleep_time);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }

            // if this agent was never registered because it is needs to be updated, break the loop immediately
            if (AgentUpdateThread.isUpdatingNow()) {
                throw new AgentNotSupportedException();
            }

            // keep doubling the sleep time so we gradually wait longer and longer - up to 60 seconds max
            sleep_time *= 2L;
            if (sleep_time > 60000L) {
                sleep_time = 60000L;
            }
        }

        // if we notified user that we started to wait, then we need to notify the user that we are done
        // this keeps the console quiet at startup except for the very first time (when it is not registered)
        if (notified_user) {
            LOG.info(AgentI18NResourceKeys.WAITING_TO_BE_REGISTERED_END);
            getOut().println(MSG.getMsg(AgentI18NResourceKeys.WAITING_TO_BE_REGISTERED_END));
        }

        return;
    }

    /**
     * Creates our {@link ClientCommandSender} object so we can be able to send commands to the server.
     *
     * @return the client sender that was created
     *
     * @throws Exception if the configured server locator URI in malformed
     */
    private ClientCommandSender createClientCommandSender() throws Exception {
        RemoteCommunicator remote_comm = createServerRemoteCommunicator(null, true, true);
        ClientCommandSenderConfiguration config = m_configuration.getClientCommandSenderConfiguration();

        ClientCommandSender client_sender = new ClientCommandSender(remote_comm, config, m_previouslyQueueCommands);

        for (CommandPreprocessor preproc : client_sender.getCommandPreprocessors()) {
            if (preproc instanceof SecurityTokenCommandPreprocessor) {
                ((SecurityTokenCommandPreprocessor) preproc).setAgentConfiguration(m_configuration);
            }
        }

        return client_sender;
    }

    /**
     * Creates a raw remote communicator that can talk to the given endpoint.
     *
     * This is public-scoped so the {@link PrimaryServerSwitchoverThread} can use this
     * and the {@link IdentifyPromptCommand} can use this.
     *
     * @param transport
     * @param address
     * @param port
     * @param transportParams
     *
     * @return the remote communicator
     *
     * @throws Exception if the communicator could not be created
     */
    public RemoteCommunicator createServerRemoteCommunicator(String transport, String address, int port,
        String transportParams) throws Exception {

        String uri = AgentConfiguration.buildServerLocatorUri(transport, address, port, transportParams);
        return createServerRemoteCommunicator(uri, false, false);
    }

    /**
     * Returns the remote communicator that can be used to send messages to the server as configured in
     * {@link AgentConfiguration#getServerLocatorUri()}.
     *
     * @param uri the locator URI; if <code>null</code>, the URI is determined by the agent's configuration
     * @param withFailover if <code>true</code>, the communicator will be configured to attempt to failover
     *                     to another server when it can't send commands to the current server. if <code>false</code>,
     *                     the communicator will not try to do any failover attempts
     * @param withInitializer if <code>true</code>, the communicator will be configured with an initializer callback
     *                        that will be invoked the first time the communicator will try to send a command.
     *
     * @return the remote communicator to use to communicate with the server
     *
     * @throws Exception if the configured server locator URI is malformed
     */
    private RemoteCommunicator createServerRemoteCommunicator(String uri, boolean withFailover, boolean withInitializer)
        throws Exception {

        if (uri == null) {
            uri = m_configuration.getServerLocatorUri();
        }

        Map<String, String> config = new HashMap<String, String>();

        if (SecurityUtil.isTransportSecure(uri)) {
            config.put(SSLSocketBuilder.REMOTING_KEY_STORE_FILE_PATH,
                m_configuration.getClientSenderSecurityKeystoreFile());
            config.put(SSLSocketBuilder.REMOTING_KEY_STORE_ALGORITHM,
                m_configuration.getClientSenderSecurityKeystoreAlgorithm());
            config.put(SSLSocketBuilder.REMOTING_KEY_STORE_TYPE, m_configuration.getClientSenderSecurityKeystoreType());
            config.put(SSLSocketBuilder.REMOTING_KEY_STORE_PASSWORD,
                m_configuration.getClientSenderSecurityKeystorePassword());
            config.put(SSLSocketBuilder.REMOTING_KEY_PASSWORD,
                m_configuration.getClientSenderSecurityKeystoreKeyPassword());
            config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_FILE_PATH,
                m_configuration.getClientSenderSecurityTruststoreFile());
            config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_ALGORITHM,
                m_configuration.getClientSenderSecurityTruststoreAlgorithm());
            config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_TYPE,
                m_configuration.getClientSenderSecurityTruststoreType());
            config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_PASSWORD,
                m_configuration.getClientSenderSecurityTruststorePassword());
            config.put(SSLSocketBuilder.REMOTING_SSL_PROTOCOL, m_configuration.getClientSenderSecuritySocketProtocol());
            config.put(SSLSocketBuilder.REMOTING_KEY_ALIAS, m_configuration.getClientSenderSecurityKeystoreAlias());
            config.put(SSLSocketBuilder.REMOTING_SERVER_AUTH_MODE,
                Boolean.toString(m_configuration.isClientSenderSecurityServerAuthMode()));
            config.put(SSLSocketBuilder.REMOTING_SOCKET_USE_CLIENT_MODE, "true");

            // since we do not know the server's client-auth mode, assume we need a keystore and let's make sure we have one
            SSLSocketBuilder dummy_sslbuilder = new SSLSocketBuilder(); // just so we can test finding our keystore
            try {
                // this allows the configured keystore file to be a URL, file path or a resource relative to our classloader
                dummy_sslbuilder.setKeyStoreURL(m_configuration.getClientSenderSecurityKeystoreFile());
            } catch (Exception e) {
                // this probably is due to the fact that the keystore doesn't exist yet - let's prepare one now
                SecurityUtil.createKeyStore(m_configuration.getClientSenderSecurityKeystoreFile(),
                    m_configuration.getClientSenderSecurityKeystoreAlias(), "CN=RHQ, OU=RedHat, O=redhat.com, C=US",
                    m_configuration.getClientSenderSecurityKeystorePassword(),
                    m_configuration.getClientSenderSecurityKeystoreKeyPassword(), "DSA", 36500);

                // now try to set it again, if an exception is still thrown, it's an unrecoverable error
                dummy_sslbuilder.setKeyStoreURL(m_configuration.getClientSenderSecurityKeystoreFile());
            }

            // in case the transport floats over https - we want to make sure a hostname verifier is installed and allows all hosts
            config.put(HTTPSClientInvoker.IGNORE_HTTPS_HOST, "true");
        }

        RemoteCommunicator remote_comm = new JBossRemotingRemoteCommunicator(uri, config);
        if (withFailover) {
            remote_comm.setFailureCallback(new FailoverFailureCallback(this));
        }
        if (withInitializer) {
            remote_comm.setInitializeCallback(new ConnectAgentInitializeCallback(this));
        }

        return remote_comm;
    }

    /**
     * Given a failover list, this makes very rudimentary connection attempts to each server to see if
     * this agent can at least reach the server endpoints. If an endpoint cannot be reached,
     * a warning is logged.
     *
     * @param failoverList the list of servers this agent will potentially need to talk to.
     * @return the servers that failed to be connected to
     */
    private List<String> testFailoverList(FailoverListComposite failoverList) {
        List<String> failedServers = new ArrayList<String>(0);

        if (failoverList != null) {
            for (int i = 0; i < failoverList.size(); i++) {
                ServerEntry server = failoverList.get(i);
                Socket socket = null;
                boolean connectError = true; // assume a failure will occur
                try {
                    LOG.debug(AgentI18NResourceKeys.TEST_FAILOVER_LIST_ENTRY, server.address, server.port);
                    InetAddress inetAddress = InetAddress.getByName(server.address);
                    InetSocketAddress socketAddress = new InetSocketAddress(inetAddress, server.port);
                    socket = new Socket();
                    socket.setSoTimeout(5000);
                    socket.connect(socketAddress, 5000);
                    connectError = false; // we successfully connected to the server
                } catch (UnknownHostException e) {
                    LOG.error(AgentI18NResourceKeys.FAILOVER_LIST_UNKNOWN_HOST, server.address);
                } catch (Exception e) {
                    if (socket != null) {
                        try {
                            socket.close(); // just clean up our last socket connect attempt
                        } catch (Exception ignore) {
                        }
                    }
                    try {
                        LOG.debug(AgentI18NResourceKeys.TEST_FAILOVER_LIST_ENTRY, server.address, server.securePort);
                        InetAddress inetAddress = InetAddress.getByName(server.address);
                        InetSocketAddress socketAddress = new InetSocketAddress(inetAddress, server.securePort);
                        socket = new Socket();
                        socket.setSoTimeout(5000);
                        socket.connect(socketAddress, 5000);
                        connectError = false; // we successfully connected to the server
                    } catch (Exception e1) {
                        String err = ThrowableUtil.getAllMessages(e1);
                        LOG.warn(AgentI18NResourceKeys.FAILOVER_LIST_UNREACHABLE_HOST, server.address, server.port,
                            server.securePort, err);
                    }
                } finally {
                    if (socket != null) {
                        try {
                            socket.close();
                        } catch (Exception e) {
                        }
                    }
                    if (connectError) {
                        failedServers.add(server.toString());
                    }
                }
            }
        }

        return failedServers;
    }

    /**
     * Given a failover list, this will persist it so the agent can recover it if the agent itself fails.
     * If this method fails to persist the list, an error is logged but otherwise this method
     * returns normally.
     *
     * @param failoverList the failover list to persist (may be <code>null</code>)
     */
    private void storeServerFailoverList(FailoverListComposite failoverList) {
        // don't do anything if we aren't configured yet - this is just a safety measure, we really
        // should never be calling this method until the agent has been fully configured
        if (m_configuration == null) {
            LOG.warn(AgentI18NResourceKeys.FAILOVER_LIST_CANNOT_BE_PERSISTED, "?", "configuration==null");
            return;
        }

        File dataDir = m_configuration.getDataDirectory();
        File failoverListFile = new File(dataDir, FILENAME_SERVER_FAILOVER_LIST);

        if (failoverList == null) {
            failoverListFile.delete();
            if (!failoverListFile.exists()) {
                LOG.debug(AgentI18NResourceKeys.FAILOVER_LIST_PERSISTED_EMPTY, failoverListFile);
            } else {
                LOG.warn(AgentI18NResourceKeys.FAILOVER_LIST_CANNOT_BE_DELETED, failoverListFile);
            }
        } else {
            try {
                byte[] failoverListBytes = failoverList.writeAsText().getBytes();
                ByteArrayInputStream byteStream = new ByteArrayInputStream(failoverListBytes);
                FileOutputStream fileStream = new FileOutputStream(failoverListFile);
                StreamUtil.copy(byteStream, fileStream, true);
                LOG.debug(AgentI18NResourceKeys.FAILOVER_LIST_PERSISTED, failoverListFile);
            } catch (Exception e) {
                LOG.warn(e, AgentI18NResourceKeys.FAILOVER_LIST_CANNOT_BE_PERSISTED, failoverListFile,
                    ThrowableUtil.getAllMessages(e));
            }

            // let's be kind to the user - if any server address is "localhost" or "127.0.0.#"
            // or starts with "localhost." (such as localhost.localdomain) then we should output a
            // warning to let the user know that that probably isn't what they want.
            // In cases when someone is demo'ing/testing/developing, and they don't want to see this, provide
            // a way for them to turn off this warning - it could get annoying since it will show up everytime
            // the primary switchover thread triggers and needs to persist the list as well as during initial startup/registration.
            if (!Boolean.getBoolean("rhq.hide-server-localhost-warning")) {
                int numServers = failoverList.size();
                for (int i = 0; i < numServers; i++) {
                    ServerEntry server = failoverList.get(i);
                    String addr = (server.address != null) ? server.address : "";
                    if ("localhost".equals(addr) || addr.startsWith("127.0.0.") || addr.startsWith("localhost.")) {
                        LOG.warn(AgentI18NResourceKeys.FAILOVER_LIST_HAS_LOCALHOST, server.address);
                        getOut().println(MSG.getMsg(AgentI18NResourceKeys.FAILOVER_LIST_HAS_LOCALHOST, server.address));
                        break; // just show the warning once
                    }
                }
            }
        }

        return;
    }

    /**
     * This enters in an infinite loop. Because this never returns, the current thread never dies and hence the agent
     * stays up and running. The user can enter agent commands at the prompt - the commands are sent to the agent as if
     * the user is a remote client.
     */
    private void inputLoop() {
        // we need to start a new thread and run our loop in it; otherwise, our shutdown hook doesn't work
        Runnable loop_runnable = new Runnable() {
            public void run() {
                try {
                    while (true) {
                        // get a command from the user
                        // if in daemon mode, only get input if reading from an input file; ignore stdin
                        String cmd;
                        if ((m_daemonMode == false) || (m_stdinInput == false)) {
                            cmd = getUserInput(null);
                        } else {
                            cmd = null;
                        }

                        if (cmd == null) {
                            // the input stream has been closed or in daemon mode
                            // no more input can be expected so let's just go to sleep forever
                            synchronized (m_init) {
                                while (m_started.get()) {
                                    m_init.wait(0);
                                }
                            }
                            break; // break the input loop thread now that the agent has been stopped
                        }

                        try {
                            // parse the command into separate arguments and execute it
                            String[] cmd_args = parseCommandLine(cmd);
                            boolean can_continue = executePromptCommand(cmd_args);

                            // break the input loop if the prompt command told us to exit
                            // if we are not in daemon mode, this really will end up killing the agent
                            if (!can_continue) {
                                break;
                            }
                        } catch (InterruptedException e) {
                            throw e;
                        } catch (Throwable t) {
                            m_output.println(MSG.getMsg(AgentI18NResourceKeys.COMMAND_FAILURE, cmd,
                                ThrowableUtil.getAllMessages(t)));
                            LOG.debug(t, AgentI18NResourceKeys.COMMAND_FAILURE_STACK_TRACE);
                        }
                    }
                } catch (InterruptedException e) {
                    // exit the thread
                }
            }
        };

        // start the thread
        m_inputLoopThread = new Thread(loop_runnable);
        m_inputLoopThread.setName(PROMPT_INPUT_THREAD_NAME);
        m_inputLoopThread.setDaemon(false);
        m_inputLoopThread.start();

        return;
    }

    /**
     * Returns the default user prompt, which is dynamically determined based on the state of the agent and its client
     * command sender if the agent's logger is in debug mode.  If not, a standard prompt is used no matter
     * what state the sender object is in.
     *
     * @return default prompt string
     */
    private String getDefaultPrompt() {
        String prompt;

        if (LOG.isDebugEnabled()) {
            ClientCommandSender sender = m_clientSender;
            if ((sender != null) && isStarted()) {
                prompt = (sender.isSending()) ? PROMPT_SENDING : PROMPT_STARTED;
            } else {
                prompt = PROMPT_SHUTDOWN;
            }
        } else {
            prompt = PROMPT_TINY;
        }

        return prompt;
    }

    /**
     * A public method that allows any container object that has embedded this agent to execute prompt commands on this
     * agent. This is useful if the container has another means by which it takes input from a user. Note, however, that
     * if a prompt command requires additional input, it will use {@link #getIn()} to read the input; so the caller must
     * either ensure the input stream is valid or it must expect errors if it tries to execute those commands that
     * require additional input.
     *
     * @param  command the full command to execute, including the command name and arguments
     *
     * @return <code>true</code> if the prompt command hasn't shut the agent down, <code>false</code> if the agent was
     *         told by the prompt command to not accept any more input
     *
     * @throws Exception
     */
    public boolean executePromptCommand(String command) throws Exception {
        String[] cmd_args = parseCommandLine(command);
        return executePromptCommand(cmd_args);
    }

    /**
     * Executes a command with the given command/arguments list.
     *
     * <p>This is package-scoped to allow our special {@link TimerPromptCommand} to use it.</p>
     *
     * @param  cmd_args the full command including its name and arguments
     *
     * @return <code>true</code> if the prompt command hasn't shut the agent down, <code>false</code> if the agent was
     *         told by the prompt command to not accept any more input
     *
     * @throws Exception
     */
    boolean executePromptCommand(String[] cmd_args) throws Exception {
        boolean can_continue = true;

        if (cmd_args.length > 0) {
            List<String> cmd_args_as_list = Arrays.asList(cmd_args);
            LOG.info(AgentI18NResourceKeys.PROMPT_COMMAND_INVOKED, cmd_args_as_list);

            Class<? extends AgentPromptCommand> promptCommandClass = m_promptCommands.get(cmd_args[0]);
            if (promptCommandClass != null) {
                AgentPromptCommand prompt_cmd = promptCommandClass.newInstance();
                can_continue = prompt_cmd.execute(AgentMain.this, cmd_args);

                // if the prompt command tells us we should die, then we need to break out of the input loop
                if (!can_continue) {
                    m_output.println(MSG.getMsg(AgentI18NResourceKeys.INPUT_DONE));
                }
            } else {
                m_output.println(MSG.getMsg(AgentI18NResourceKeys.UNKNOWN_COMMAND, cmd_args_as_list));
            }
        } else {
            m_output.println();
        }

        return can_continue;
    }

    /**
     * Displays the help text.
     */
    private void displayUsage() {
        m_output.println(MSG.getMsg(AgentI18NResourceKeys.USAGE, this.getClass().getName()));
    }

    /**
     * Processes the array of command line arguments passed to our Java application.
     *
     * @param  args the command line arguments to process
     *
     * @throws Exception                if the agent should not start
     * @throws IllegalArgumentException if an argument was invalid
     * @throws HelpException            if help was requested and the agent should not be created
     */
    private void processArguments(String[] args) throws Exception {
        String sopts = "-:hdlLasntguD:i:o:c:p:e:";
        LongOpt[] lopts = { new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h'),
            new LongOpt("input", LongOpt.REQUIRED_ARGUMENT, null, 'i'),
            new LongOpt("output", LongOpt.REQUIRED_ARGUMENT, null, 'o'),
            new LongOpt("config", LongOpt.REQUIRED_ARGUMENT, null, 'c'),
            new LongOpt("pref", LongOpt.REQUIRED_ARGUMENT, null, 'p'),
            new LongOpt("console", LongOpt.REQUIRED_ARGUMENT, null, 'e'),
            new LongOpt("daemon", LongOpt.NO_ARGUMENT, null, 'd'),
            new LongOpt("cleanconfig", LongOpt.NO_ARGUMENT, null, 'l'),
            new LongOpt("fullcleanconfig", LongOpt.NO_ARGUMENT, null, 'L'),
            new LongOpt("advanced", LongOpt.NO_ARGUMENT, null, 'a'),
            new LongOpt("setup", LongOpt.NO_ARGUMENT, null, 's'),
            new LongOpt("nostart", LongOpt.NO_ARGUMENT, null, 'n'),
            new LongOpt("nonative", LongOpt.NO_ARGUMENT, null, 't'),
            new LongOpt("purgeplugins", LongOpt.NO_ARGUMENT, null, 'g'),
            new LongOpt("purgedata", LongOpt.NO_ARGUMENT, null, 'u') };

        String config_file_name = null;
        boolean clean_config = false;
        boolean clean_token = false; // only used if clean_config = true
        boolean purge_data = false;
        boolean purge_plugins = false;
        AgentInputReaderFactory.ConsoleType console_type = null;

        Getopt getopt = new Getopt("agent", args, sopts, lopts);
        int code;

        while ((code = getopt.getopt()) != -1) {
            switch (code) {
            case ':':
            case '?': {
                // for now both of these should exit
                displayUsage();
                throw new IllegalArgumentException(MSG.getMsg(AgentI18NResourceKeys.BAD_ARGS));
            }

            case 1: {
                // this will catch non-option arguments (which we don't currently care about)
                System.err.println(MSG.getMsg(AgentI18NResourceKeys.UNUSED_OPTION, getopt.getOptarg()));
                break;
            }

            case 'h': {
                displayUsage();
                throw new HelpException(MSG.getMsg(AgentI18NResourceKeys.HELP_SHOWN));
            }

            case 'D': {
                // set a system property
                String sysprop = getopt.getOptarg();
                int i = sysprop.indexOf("=");
                String name;
                String value;

                if (i == -1) {
                    name = sysprop;
                    value = "true";
                } else {
                    name = sysprop.substring(0, i);
                    value = sysprop.substring(i + 1, sysprop.length());
                }

                System.setProperty(name, value);
                LOG.debug(AgentI18NResourceKeys.SYSPROP_SET, name, value);

                break;
            }

            case 'c': {
                config_file_name = getopt.getOptarg();
                break;
            }

            case 'l': {
                clean_config = true;
                purge_data = true;
                break;
            }

            case 'L': {
                clean_config = true;
                purge_data = true;
                clean_token = true;
                break;
            }

            case 'u': {
                purge_data = true;
                break;
            }

            case 'g': {
                purge_plugins = true;
                break;
            }

            case 'a': {
                m_advancedSetup = true;
                break;
            }

            case 's': {
                m_forcedSetup = true;
                break;
            }

            case 'n': {
                m_startAtBoot = false;
                break;
            }

            case 'p': {
                setConfigurationPreferencesNode(getopt.getOptarg());
                break;
            }

            case 'e': {
                console_type = AgentInputReaderFactory.ConsoleType.valueOf(getopt.getOptarg()); // throws IllegalArgumentException if invalid
                break;
            }

            case 'd': {
                m_daemonMode = true;
                break;
            }

            case 'i': {
                File script = new File(getopt.getOptarg());

                try {
                    m_input = AgentInputReaderFactory.create(this, script);
                    m_stdinInput = false;
                } catch (Exception e) {
                    throw new IllegalArgumentException(MSG.getMsg(AgentI18NResourceKeys.BAD_INPUT_FILE, script, e));
                }

                break;
            }

            case 'o': {
                File output = new File(getopt.getOptarg());

                try {
                    File parentDir = output.getParentFile();
                    if ((parentDir != null) && (!parentDir.exists())) {
                        parentDir.mkdirs();
                    }

                    m_output = new AgentPrintWriter(new FileWriter(output), true);
                } catch (Exception e) {
                    throw new IllegalArgumentException(MSG.getMsg(AgentI18NResourceKeys.BAD_OUTPUT_FILE, output, e));
                }

                break;
            }

            case 't': {
                m_disableNativeSystem = true;
            }
            }
        }

        if (m_daemonMode) {
            AgentInputReaderFactory.setConsoleType(AgentInputReaderFactory.ConsoleType.java); // don't use native libs, no need and jline causes problems
        } else if (console_type != null) {
            AgentInputReaderFactory.setConsoleType(console_type);
        }

        // now that all the arguments were processed, let's load in our config (this allows the -p to come after -c)
        if (clean_config) {
            Preferences prefsNode = getPreferencesNode();
            if (clean_token) {
                prefsNode.removeNode();
            } else {
                // remove everything EXCEPT the security token
                String[] prefKeys = prefsNode.keys();
                if (prefKeys != null && prefKeys.length > 0) {
                    for (String prefKey : prefKeys) {
                        if (!prefKey.equals(AgentConfigurationConstants.AGENT_SECURITY_TOKEN)) {
                            prefsNode.remove(prefKey);
                        }
                    }
                }
            }
            prefsNode.flush();
        }

        if (config_file_name != null) {
            try {
                loadConfigurationFile(config_file_name);
            } catch (Exception e) {
                throw new IllegalArgumentException(MSG.getMsg(AgentI18NResourceKeys.LOAD_CONFIG_FILE_FAILURE,
                    config_file_name, e));
            }
        }

        checkInitialConfiguration();

        // We must do this after we load the config file so we know where the user configured the data dir.
        if (purge_data) {
            cleanDataDirectory();
        }
        if (purge_plugins) {
            cleanPluginsDirectory();
        }

        LOG.debug(AgentI18NResourceKeys.ARGS_PROCESSED, Arrays.asList(m_commandLineArgs));

        return;
    }

    /**
     * This sets the name of the preferences node. This identifies the node where the agent's configuration should be.
     * This node name must match the node as defined in the agent's configuration file. See the <i>Java Preferences
     * API</i> for the definition of a preferences node.
     *
     * <p/>
     * <p>Note that if <code>node_name</code> is <code>null</code> or a blank/empty string, the
     * {@link AgentConfigurationConstants#DEFAULT_PREFERENCE_NODE default node name} will be assumed.</p>
     *
     * @param  node_name the name of the preference node where the agent's configuration should be located
     *
     * @throws IllegalArgumentException if the node name was invalid which occurs if the name has a forward slash (/) in
     *                                  it
     *
     * @see    #loadConfigurationFile(String)
     */
    private void setConfigurationPreferencesNode(String node_name) throws IllegalArgumentException {
        if ((node_name == null) || (node_name.trim().length() == 0)) {
            node_name = AgentConfigurationConstants.DEFAULT_PREFERENCE_NODE;
        }

        if (node_name.indexOf('/') != -1) {
            throw new IllegalArgumentException(MSG.getMsg(AgentI18NResourceKeys.NO_SLASHES_ALLOWED, node_name));
        }

        m_agentPreferencesNodeName = node_name;

        return;
    }

    /**
     * Creates the prompt command map - this defines the prompt commands that are accepted on the agent prompt.
     *
     * @param  prompt_commands the commands map
     *
     * @throws RuntimeException if two or more prompt command names collide
     */
    private void setupPromptCommandsMap(Map<String, Class<? extends AgentPromptCommand>> prompt_commands) {
        prompt_commands.clear();

        AgentPromptCommand[] all_cmds = new AgentPromptCommand[] { new HelpPromptCommand(), new ExitPromptCommand(),
            new QuitPromptCommand(), new VersionPromptCommand(), new SetupPromptCommand(), new StartPromptCommand(),
            new ShutdownPromptCommand(), new GetConfigPromptCommand(), new SetConfigPromptCommand(),
            new ConfigPromptCommand(), new RegisterPromptCommand(), new PluginsPromptCommand(),
            new PluginContainerPromptCommand(), new MetricsPromptCommand(), new NativePromptCommand(),
            /*new ExecutePromptCommand(),*/new DiscoveryPromptCommand(), new InventoryPromptCommand(),
            new AvailabilityPromptCommand(), new PiqlPromptCommand(), new IdentifyPromptCommand(),
            new LogPromptCommand(), new TimerPromptCommand(), new PingPromptCommand(), new DownloadPromptCommand(),
            new DumpSpoolPromptCommand(), new SenderPromptCommand(), new FailoverPromptCommand(),
            new UpdatePromptCommand(), new DebugPromptCommand(), new SleepPromptCommand(), new GCPromptCommand(),
            new SchedulesPromptCommand(), new ListDataPromptCommand() };

        // hold the conflicts
        StringBuilder conflicts = new StringBuilder();

        // compare each command to everyone else, collect up all of the conflicts
        for (int i = 0; i < all_cmds.length; i++) {
            for (int j = i + 1; j < all_cmds.length; j++) {
                String nextConflict = ensureUniqueNamesAndAliases(all_cmds[i], all_cmds[j]);
                if (nextConflict != null) {
                    conflicts.append(System.getProperty("line.separator"));
                    conflicts.append(nextConflict);
                }
            }
        }

        // if there were any conflicts, abort the startup process and inform the developer of all conflicts
        if (conflicts.length() > 0) {
            throw new RuntimeException(conflicts.toString());
        }

        for (int i = 0; i < all_cmds.length; i++) {
            // aliases via extension, so this will register them too
            prompt_commands.put(all_cmds[i].getPromptCommandString(), all_cmds[i].getClass());
        }

        return;
    }

    /**
     * Given a pair of {@link AgentPromptCommand} objects, this method compares them to make sure that their
     * {@link AgentPromptCommand#getPromptCommandString()}s are unique via case insensitive comparison
     *
     * @param  lefty  the first AgentPromptCommand
     * @param  righty the second AgentPromptCommand
     *
     * @return a string detailing the conflicts
     */
    private String ensureUniqueNamesAndAliases(AgentPromptCommand lefty, AgentPromptCommand righty) {
        String result = null;
        if (lefty.getPromptCommandString().equalsIgnoreCase(righty.getPromptCommandString())) {
            result = "Commands '" + lefty.getClass().getSimpleName() + "' and '" + righty.getClass().getSimpleName()
                + "' have overlapping prompt command names: '" + lefty.getPromptCommandString() + "'";
        }

        return result;
    }

    /**
     * Given a command line, this will parse each argument and return the argument array.
     *
     * @param  cmdLine the command line
     *
     * @return the array of command line arguments
     */
    private String[] parseCommandLine(String cmdLine) {
        ByteArrayInputStream in = new ByteArrayInputStream(cmdLine.getBytes());
        StreamTokenizer strtok = new StreamTokenizer(new InputStreamReader(in));
        List<String> args = new ArrayList<String>();
        boolean keep_going = true;

        // we don't want to parse numbers and we want ' to be a normal word character
        strtok.ordinaryChars('0', '9');
        strtok.ordinaryChar('.');
        strtok.ordinaryChar('-');
        strtok.ordinaryChar('\'');
        strtok.wordChars(33, 127);
        strtok.quoteChar('\"');

        // parse the command line
        while (keep_going) {
            int nextToken;

            try {
                nextToken = strtok.nextToken();
            } catch (IOException e) {
                nextToken = StreamTokenizer.TT_EOF;
            }

            if (nextToken == java.io.StreamTokenizer.TT_WORD) {
                args.add(safeArg(strtok.sval));
            } else if (nextToken == '\"') {
                args.add(safeArg(strtok.sval));
            } else if ((nextToken == java.io.StreamTokenizer.TT_EOF) || (nextToken == java.io.StreamTokenizer.TT_EOL)) {
                keep_going = false;
            }
        }

        return args.toArray(new String[args.size()]);
    }

    // perform any other massaging
    private String safeArg(String arg) {
        // remove trailing '=' from long option args. For example --plugin= should just be --plugin for
        // downstream processing.
        String result = (arg.startsWith("--") && arg.endsWith("=")) ? arg.substring(0, arg.length() - 1) : arg;
        return result;
    }

    /**
     * Returns the preferences for this agent. The node returned is where all preferences are to be stored.
     *
     * @return the agent preferences
     */
    private Preferences getPreferencesNode() {
        Preferences topNode = Preferences.userRoot().node(AgentConfigurationConstants.PREFERENCE_NODE_PARENT);
        Preferences preferencesNode = topNode.node(m_agentPreferencesNodeName);

        return preferencesNode;
    }

    /**
     * This performs some steps necessary to complete the configuration. This will overlay system properties on top of
     * the current configuration (i.e. system properties override settings in the configuration file) and will log the
     * configuration after the overlay.
     *
     * @param  agent_configuration the configuration to overlay system properties on top of
     *
     * @throws Exception if failed to get the current preferences
     */
    private void finishConfigurationSetup(AgentConfiguration agent_configuration) throws Exception {
        Preferences prefs = agent_configuration.getPreferences();

        LOG.debug(AgentI18NResourceKeys.PREFERENCES_SCHEMA, agent_configuration.getAgentConfigurationVersion());
        LOG.debug(AgentI18NResourceKeys.PREFERENCES_NODE_PATH, prefs.absolutePath());

        if (!agent_configuration.doNotOverridePreferencesWithSystemProperties()) {
            overlaySystemProperties(prefs);
        }

        // JBoss/Remoting wants to write a jboss.identity file to the filesystem.
        // Let's tell our communication services where to put that file if this property isn't set yet.
        Preferences preferencesNode = agent_configuration.getPreferences();
        ServiceContainerConfiguration service_container_configuration = new ServiceContainerConfiguration(
            preferencesNode);
        if (service_container_configuration.getDataDirectoryIfDefined() == null) {
            String data_dir = agent_configuration.getDataDirectory().toString();
            preferencesNode.put(ServiceContainerConfigurationConstants.DATA_DIRECTORY, data_dir);
        }

        prefs.flush();

        LOG.debug(AgentI18NResourceKeys.CONFIGURATION, agent_configuration);

        return;
    }

    /**
     * This method is called when the agent is initially instantiated. It checks to make sure at least a default
     * configuration is loaded and the configuration is fully upgraded to the latest configuration schema version.
     *
     * @throws Exception
     */
    private void checkInitialConfiguration() throws Exception {
        // Make sure we are configured properly - do this now in case a -c command line argument wasn't specified
        if (m_configuration == null) {
            // let's see if we are already pre-configured; if we are not, load in the default configuration file
            AgentConfiguration current_config = new AgentConfiguration(getPreferencesNode());

            if (current_config.getAgentConfigurationVersion() == 0) {
                loadConfigurationFile(AgentConfigurationConstants.DEFAULT_AGENT_CONFIGURATION_FILE);
            } else {
                m_configuration = current_config;

                LOG.debug(AgentI18NResourceKeys.PREFERENCES_ALREADY_EXIST);
            }
        }

        // finish processing the configuration - this will overlay system properties on top of the config
        finishConfigurationSetup(m_configuration);

        // make sure the configuration is up to date with the latest known, supported schema version
        AgentConfigurationUpgrade.upgradeToLatest(m_configuration.getPreferences());

        return;
    }

    /**
     * Given a set of preferences, this will overlay any system properties on top of them; effectively allowing system
     * properties to override values defined in the given preferences. Only those system properties that are related to
     * the {@link AgentConfigurationConstants#PROPERTY_NAME_PREFIX agent} and
     * {@link ServiceContainerConfigurationConstants#PROPERTY_NAME_PREFIX communications layer} are overlaid.
     *
     * <p>This method ignores {@link AgentConfiguration#doNotOverridePreferencesWithSystemProperties()} - in other
     * words, this method will always overlay the system properties, even though the agent may have been configured not
     * to. Use this method with caution since you are disobeying an agent configuration preference.</p>
     *
     * @param prefs the preferences to be overlaid with system property override values
     */
    private void overlaySystemProperties(Preferences prefs) {
        // we want system properties to override anything in the configuration; but only the agent/service container properties
        for (Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
            String name = entry.getKey().toString();
            String value = entry.getValue().toString();

            if (name.startsWith(ServiceContainerConfigurationConstants.PROPERTY_NAME_PREFIX)
                || (name.startsWith(AgentConfigurationConstants.PROPERTY_NAME_PREFIX))) {
                LOG.debug(AgentI18NResourceKeys.OVERLAY_SYSPROP, name, value);
                prefs.put(name, value);
            }
        }

        return;
    }

    /**
     * Sets the flag to indicate if the agent has started or is stopped.
     *
     * @param started <code>true</code> if the agent has started; <code>false</code> if the agent has stopped
     */
    private void setStarted(boolean started) {
        m_started.set(started);
        m_startTime = (started) ? System.currentTimeMillis() : 0L;
    }

    private static void reconfigureJavaLogging() {
        try {
            LOG.debug(AgentI18NResourceKeys.RECONFIGURE_JAVA_LOGGING_START);
            ClassLoader classLoader = AgentMain.class.getClassLoader();
            InputStream stream = classLoader.getResourceAsStream(JAVA_UTIL_LOGGING_PROPERTIES_RESOURCE_PATH);
            java.util.logging.LogManager logManager = java.util.logging.LogManager.getLogManager();
            logManager.readConfiguration(stream);
            LOG.debug(AgentI18NResourceKeys.RECONFIGURE_JAVA_LOGGING_DONE);
        } catch (Exception e) {
            LOG.error(e, AgentI18NResourceKeys.RECONFIGURE_JAVA_LOGGING_ERROR);
        }
    }

    /**
     * Listener that will register the agent as soon as the sender has started (which means we should be able to connect
     * to the RHQ Server and send the register command).
     */
    private class RegisterStateListener implements ClientCommandSenderStateListener {
        public boolean startedSending(ClientCommandSender sender) {
            // spawn the register thread - wait for it to complete because once we return, the other state listeners will
            // be called and if they want to talk to the server, we must first be registered in order to be able to do that
            registerWithServer(60000L, false);

            // once we are registered once, we really don't need to be registered again
            // the user can always execute the "register" command prompt or clear the agent config
            // or just set this config preference back to true if they want to register
            // 9/17/2008 - we now have to register every time we startup because its how we get the failover list too
            //m_configuration.getPreferences().putBoolean(AgentConfigurationConstants.REGISTER_WITH_SERVER_AT_STARTUP, false);
            return false; // no need to keep listening
        }

        public boolean stoppedSending(ClientCommandSender sender) {
            return true; // no-op but keep listening
        }
    }

    /**
     * Sender listener that will remove the command listener once the sender starts. It will also add the command
     * listener once the sender stops. The command listener will allow us to immediately turn on the sender when the
     * server sends us a message. We don't need this command listener once we know the sender has started (because that
     * must mean that we discovered the server). This object poses as both the command listener and the sender listener.
     */
    private class CommandListenerStateListener implements ClientCommandSenderStateListener, CommandListener {
        private ClientCommandSender senderListeningTo = null;

        public boolean startedSending(ClientCommandSender sender) {
            // the sender has started, which means we might have discovered the server and it is up
            // no need to listen for more commands
            synchronized (this) {
                this.senderListeningTo = null;

                ServiceContainer commServices = getServiceContainer(); // so we don't worry about synchronizing
                if (commServices != null) {
                    commServices.removeCommandListener(this);
                }
            }

            return true; // keep listening in case we stop and start again
        }

        public boolean stoppedSending(ClientCommandSender sender) {
            // the sender has stopped, which means we might have seen the server go down
            // add ourselves as a command listener - if we here from the server, we'll start the sender again
            synchronized (this) {
                if (this.senderListeningTo == null) {
                    this.senderListeningTo = sender;

                    ServiceContainer commServices = getServiceContainer(); // so we don't worry about synchronizing
                    if (commServices != null) {
                        commServices.addCommandListener(this);
                    }
                }
            }

            return true; // keep listening in case we start and stop again
        }

        public void processedCommand(Command command, CommandResponse response) {
            return; // no-op
        }

        public void receivedCommand(Command command) {
            // We are receiving a command from the server! It must be up!
            // If senderListeningTo is not null, the sender is most likely in stopped mode.
            // We can restart it now since we are fairly confident the server is up.
            synchronized (this.senderListeningTo) {
                if (this.senderListeningTo != null) {
                    boolean changed_mode = this.senderListeningTo.startSending();
                    if (changed_mode) {
                        LOG.debug(AgentI18NResourceKeys.RECEIVED_COMMAND_STARTED_SENDER);
                    }
                }
            }

            return;
        }
    }

    /**
     * This listener restarts the plugin container upon establishing the connection with the server
     * if the following conditions are met:
     * <ol>
     * <li> The plugin container is started
     * <li> It performed the resource upgrade but failed to merge the changes to the server
     * </ol>
     * By restarting the plugin container in such conditions, we essentially re-run the resource upgrade
     * and let the plugin container try to re-merge with the server that we know has just connected.
     *
     * @author Lukas Krejci
     */
    private class PluginContainerConditionalRestartListener implements ClientCommandSenderStateListener {
        public boolean startedSending(ClientCommandSender sender) {
            try {
                InventoryManager inventoryManager = PluginContainer.getInstance().getInventoryManager();
                if (inventoryManager != null && inventoryManager.hasUpgradeMergeFailed()) {
                    m_output.println(MSG
                        .getMsg(AgentI18NResourceKeys.RESTARTING_PLUGIN_CONTAINER_AFTER_UPGRADE_MERGE_FAILURE));
                    LOG.info(AgentI18NResourceKeys.RESTARTING_PLUGIN_CONTAINER_AFTER_UPGRADE_MERGE_FAILURE);

                    PluginContainerPromptCommand pcCommand = new PluginContainerPromptCommand();
                    pcCommand.execute(AgentMain.this, new String[] { "stop" });
                    pcCommand.execute(AgentMain.this, new String[] { "start" });
                }
            } catch (Exception e) {
                LOG.error("Failed to restart the plugin container when server connection established.");
            }
            return true;
        }

        public boolean stoppedSending(ClientCommandSender sender) {
            //do nothing but keep listening
            return true;
        }
    }

    /**
     * When the agent starts up, it needs to create the communications servers before starting the plugin container;
     * however, the agent must not process any incoming commands until after the plugin container fully starts. This
     * command listener will actually block indefinitely all commands from being processed until its told to open the
     * floodgates and allow all messages. Note that because the agent will be pinged by the server upon registration,
     * this listener will always allow ping requests to go through.
     */
    private class BootstrapLatchCommandListener implements CommandListener {
        /**
         * Only when this latch is open, will this processor be allowed to start processing incoming messages. When the
         * latch is closed, all threads attempting to ask this processor to execute a command will be blocked.
         */
        private final CountDownLatch m_latch = new CountDownLatch(1);
        private final String PING_INTERFACE_NAME = Ping.class.getName();

        public void allowAllCommands(ServiceContainer container) {
            container.removeCommandListener(this); // don't need this listener anymore, we can remove it
            m_latch.countDown();
        }

        public void receivedCommand(Command command) {
            try {
                if (!(command instanceof RemotePojoInvocationCommand)
                    || !((RemotePojoInvocationCommand) command).getTargetInterfaceName().equals(PING_INTERFACE_NAME)) {
                    m_latch.await();
                }
            } catch (Exception e) {
            }
        }

        public void processedCommand(Command command, CommandResponse response) {
            // no-op
        }
    }

    private class SenderCreationListener implements ServiceContainerSenderCreationListener {
        public void preCreate(ServiceContainer service_container, RemoteCommunicator remote_communicator,
            ClientCommandSenderConfiguration sender_configuration) {
            return; // no-op
        }

        public void postCreate(ServiceContainer service_container, ClientCommandSender sender) {
            for (CommandPreprocessor preproc : sender.getCommandPreprocessors()) {
                if (preproc instanceof SecurityTokenCommandPreprocessor) {
                    ((SecurityTokenCommandPreprocessor) preproc).setAgentConfiguration(m_configuration);
                }
            }

            return;
        }
    }

    private class HelpException extends Exception {
        private static final long serialVersionUID = 1L;

        public HelpException(String msg) {
            super(msg);
        }
    }

    // a simple class that encapsulates when and to whom the last connect agent command was sent
    // also contains a R/W lock that is used so the agent only attempts to send one connect command at a time
    private class LastSentConnectAgent {
        public long timestamp = 0L;
        public String serverEndpoint = "";
        public ReadWriteLock rwLock = new ReentrantReadWriteLock();

        @Override
        public String toString() {
            return this.serverEndpoint + "@" + new Date(this.timestamp);
        }
    }

    private class PingExecutor implements Runnable {

        @Override
        public void run() {
            try {
                // if we can't send to the server ignore the ping
                if (!m_clientSender.isSending()) {
                    // An unlikely state, but if we're not sending, not polling and not performing autoDiscovery
                    // (multicast), then start polling to we eventually get out of this state.
                    if (!(m_clientSender.isServerPolling() || isAutoDiscoveryEnabled())) {
                        LOG.info(AgentI18NResourceKeys.PING_EXECUTOR_STARTING_POLLING);
                        m_clientSender.startServerPolling();
                    }

                    return;
                }

                // we are in sending mode, so make sure the poller is off
                if (m_clientSender.isServerPolling()) {
                    LOG.info(AgentI18NResourceKeys.PING_EXECUTOR_STOPPING_POLLING_RESUME_PING);
                    m_clientSender.stopServerPolling();
                }

                boolean updateAvail = PluginContainer.getInstance().isStarted();
                PingRequest request = new PingRequest(getConfiguration().getAgentName(), updateAvail, true);

                ClientRemotePojoFactory factory = m_clientSender.getClientRemotePojoFactory();
                CoreServerService server = factory.getRemotePojo(CoreServerService.class);
                request = server.ping(request);

                // take this opportunity to check the agent-server clock sync
                serverClockNotification(request.getReplyServerTimestamp());

            } catch (Throwable t) {
                // If the ping fails, typically do to a CannotConnectException, and we're not using autodiscovery,
                // then start the poller to have sending mode re-established when the connection resumes.
                if (!(m_clientSender.isServerPolling() || isAutoDiscoveryEnabled())) {
                    LOG.info(AgentI18NResourceKeys.PING_EXECUTOR_STARTING_POLLING_AFTER_EXCEPTION,
                        ThrowableUtil.getAllMessages(t));
                    m_clientSender.startServerPolling();
                } else {
                    LOG.warn(AgentI18NResourceKeys.PING_EXECUTOR_SERVER_PING_FAILED, t);
                }
            }
        }
    }
}
