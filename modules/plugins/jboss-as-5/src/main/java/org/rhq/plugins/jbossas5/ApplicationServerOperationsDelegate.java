package org.rhq.plugins.jbossas5;

import java.io.File;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mc4j.ems.connection.EmsConnection;
import org.mc4j.ems.connection.bean.EmsBean;
import org.mc4j.ems.connection.bean.operation.EmsOperation;
import org.mc4j.ems.connection.bean.parameter.EmsParameter;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.core.pluginapi.util.ProcessExecutionUtility;
import org.rhq.core.system.ProcessExecution;
import org.rhq.core.system.ProcessExecutionResults;
import org.rhq.core.system.SystemInfo;

/**
 * Handles performing operations on a JBossAS instance.
 * 
 * @author Ian Springer
 * @author Jason Dobies
 * @author Jay Shaughnessy
 */
public class ApplicationServerOperationsDelegate {

	/**
	 * max amount of time to wait for server to show as unavailable after
	 * executing stop - in milliseconds
	 */
	private static final long STOP_WAIT_MAX = 1000L * 150; // 2.5 minutes

	/**
	 * amount of time to wait between availability checks when performing a stop
	 * - in milliseconds
	 */
	private static final long STOP_WAIT_INTERVAL = 1000L * 10; // 10 seconds

	/**
	 * amount of time to wait for stop to complete after the loop that checks
	 * for DOWN availability terminates - in milliseconds
	 */
	private static final long STOP_WAIT_FINAL = 1000L * 30; // 30 seconds

	/** max amount of time to wait for start to complete - in milliseconds */
	private static final long START_WAIT_MAX = 1000L * 300; // 5 minutes

	/**
	 * amount of time to wait between availability checks when performing a
	 * start - in milliseconds
	 */
	private static final long START_WAIT_INTERVAL = 1000L * 10; // 10 seconds

	private final Log log = LogFactory
			.getLog(ApplicationServerOperationsDelegate.class);

	private static final String SEPARATOR = "\n-----------------------\n";
	// Attributes --------------------------------------------

	static final String DEFAULT_START_SCRIPT = "bin" + File.separator + "run."
			+ ((File.separatorChar == '/') ? "sh" : "bat");
	static final String DEFAULT_SHUTDOWN_SCRIPT = "bin" + File.separator
			+ "shutdown." + ((File.separatorChar == '/') ? "sh" : "bat");

	/**
	 * Server component against which the operations are being performed.
	 */
	private ApplicationServerComponent serverComponent;

	private Configuration pluginConfig;

	private ResourceContext resourceContext;

	private File configPath;

	/**
	 * Passed in from the resource context for making process calls.
	 */
	private SystemInfo systemInfo;

	// Constructors --------------------------------------------

	public ApplicationServerOperationsDelegate(
			ApplicationServerComponent serverComponent, SystemInfo systemInfo) {
		this.serverComponent = serverComponent;
		this.resourceContext = serverComponent.getResourceContext();
		this.pluginConfig = resourceContext.getPluginConfiguration();
		this.systemInfo = systemInfo;
	}

	// Public --------------------------------------------

	/**
	 * Performs the specified operation. The result of the operation will be
	 * indicated in the return. If there is an error, an
	 * <code>RuntimeException</code> will be thrown.
	 * 
	 * @param operation
	 *            the operation to perform
	 * @param parameters
	 *            parameters to the operation call
	 * 
	 * @return if successful, the result object will contain a success message
	 * 
	 * @throws RuntimeException
	 *             if any errors occur while trying to perform the operation
	 */
	public OperationResult invoke(
			ApplicationServerSupportedOperations operation,
			Configuration parameters) throws InterruptedException {
		String message = null;

		switch (operation) {
		case RESTART: {
			message = restart();
			break;
		}

		case SHUTDOWN: {
			message = shutdown();
			break;
		}

		case START: {
			message = start();
			break;
		}
		}

		OperationResult result = new OperationResult(message);
		return result;
	}

	// Private --------------------------------------------

	/**
	 * Starts the underlying AS server.
	 * 
	 * @return success message if no errors are encountered
	 * @throws InterruptedException
	 *             if the plugin container stops this operation while its
	 *             executing
	 */
	private String start() throws InterruptedException {

		File startScriptFile = getStartScriptPath();
		validateScriptFile(
				startScriptFile,
				ApplicationServerComponent.PluginConfigPropNames.START_SCRIPT_CONFIG_PROP);

		String prefix = pluginConfig
				.getSimple(
						ApplicationServerComponent.PluginConfigPropNames.SCRIPT_PREFIX_CONFIG_PROP)
				.getStringValue();
		String configName = getConfigurationSet();
		String bindingAddress = pluginConfig.getSimple(
				ApplicationServerComponent.PluginConfigPropNames.BIND_ADDRESS)
				.getStringValue();

		String configArgument = "-c" + configName;
		String bindingAddressArgument = null;
		if (bindingAddress != null)
			bindingAddressArgument = "-b" + bindingAddress;

		ProcessExecution processExecution;

		// prefix is either null or contains ONLY whitespace characters
		if (prefix == null || prefix.replaceAll("\\s", "").equals("")) {
			processExecution = ProcessExecutionUtility
					.createProcessExecution(startScriptFile);

			processExecution.getArguments().add("-c");
			processExecution.getArguments().add(configName);

			if (bindingAddressArgument != null) {
				processExecution.getArguments().add("-b");
				processExecution.getArguments().add(bindingAddress);
			}
		} else {
			// The process execution should be tied to the process represented
			// as the prefix. If there are any other
			// tokens in the prefix, consider them arguments to the prefix
			// process.
			StringTokenizer prefixTokenizer = new StringTokenizer(prefix);
			String processName = prefixTokenizer.nextToken();
			File prefixProcess = new File(processName);

			processExecution = ProcessExecutionUtility
					.createProcessExecution(prefixProcess);

			while (prefixTokenizer.hasMoreTokens()) {
				String prefixArgument = prefixTokenizer.nextToken();
				processExecution.getArguments().add(prefixArgument);
			}

			// Assemble the AS start script and its prefixes as one argument to
			// the prefix
			String startScriptArgument = startScriptFile.getAbsolutePath();
			startScriptArgument += " " + configArgument;

			if (bindingAddressArgument != null) {
				startScriptArgument += " " + bindingAddressArgument;
			}

			processExecution.getArguments().add(startScriptArgument);
		}

		initProcessExecution(processExecution, startScriptFile);

		long start = System.currentTimeMillis();
		if (log.isDebugEnabled()) {
			log.debug("About to execute the following process: ["
					+ processExecution + "]");
		}
		ProcessExecutionResults results = this.systemInfo
				.executeProcess(processExecution);
		logExecutionResults(results);

		AvailabilityType avail;
		if (results.getError() == null) {
			avail = waitForServerToStart(start);
		} else {
			log.error(
					"Error from process execution while starting the AS instance. Exit code ["
							+ results.getExitCode() + "]", results.getError());
			avail = this.serverComponent.getAvailability();
		}

		// If, after the loop, the Server is still down, consider the start to
		// be a failure.
		if (avail == AvailabilityType.DOWN) {
			throw new RuntimeException("Server failed to start: "
					+ results.getCapturedOutput());
		} else {
			return "Server has been started.";
		}
	}

	private String getConfigurationSet() {

		configPath = resolvePathRelativeToHomeDir(getRequiredPropertyValue(
				pluginConfig,
				ApplicationServerComponent.PluginConfigPropNames.SERVER_HOME_DIR));

		if (!configPath.exists()) {
			throw new InvalidPluginConfigurationException(
					"Configuration path '" + configPath + "' does not exist.");
		}
		return pluginConfig.getSimpleValue(
				ApplicationServerComponent.PluginConfigPropNames.SERVER_NAME,
				configPath.getName());
	}

	private void initProcessExecution(ProcessExecution processExecution,
			File scriptFile) {
		// NOTE: For both run.bat and shutdown.bat, the current working dir must
		// be set to the script's parent dir
		// (e.g. ${JBOSS_HOME}/bin) for the script to work.
		processExecution.setWorkingDirectory(scriptFile.getParent());

		// Both scripts require the JAVA_HOME env var to be set.
		File javaHomeDir = getJavaHomePath();
		if (javaHomeDir == null) {
			throw new IllegalStateException(
					"The '"
							+ "JAVA_HOME"
							+ "' connection property must be set in order to start or stop JBossAS via script.");
		}

		validateJavaHomePathProperty();
		processExecution.getEnvironmentVariables().put("JAVA_HOME",
				javaHomeDir.getPath());

		processExecution.setCaptureOutput(true);
		processExecution.setWaitForCompletion(1000L); // 1 second // TODO:
		// Should we wait longer
		// than one second?
		processExecution.setKillOnTimeout(false);
	}

	/**
	 * Shuts down the server by dispatching to shutdown via script or JMX. Waits
	 * until the server is down.
	 * 
	 * @return The result of the shutdown operation - is successful
	 */
	private String shutdown() {
		ApplicationServerShutdownMethod shutdownMethod = Enum
				.valueOf(
						ApplicationServerShutdownMethod.class,
						pluginConfig
								.getSimple(
										ApplicationServerComponent.PluginConfigPropNames.SHUTDOWN_METHOD_CONFIG_PROP)
								.getStringValue());
		String result = ApplicationServerShutdownMethod.JMX
				.equals(shutdownMethod) ? shutdownViaJmx()
				: shutdownViaScript();
		AvailabilityType avail = waitForServerToShutdown();
		if (avail == AvailabilityType.UP) {
			throw new RuntimeException("Server failed to shutdown");
		} else {
			return result;
		}
	}

	/**
	 * Shuts down the AS server using a shutdown script.
	 * 
	 * @return success message if no errors are encountered
	 */
	private String shutdownViaScript() {
		File shutdownScriptFile = getShutdownScriptPath();
		validateScriptFile(
				shutdownScriptFile,
				ApplicationServerComponent.PluginConfigPropNames.SHUTDOWN_SCRIPT_CONFIG_PROP);
		String prefix = pluginConfig
				.getSimple(
						ApplicationServerComponent.PluginConfigPropNames.SCRIPT_PREFIX_CONFIG_PROP)
				.getStringValue();
		ProcessExecution processExecution = ProcessExecutionUtility
				.createProcessExecution(prefix, shutdownScriptFile);

		initProcessExecution(processExecution, shutdownScriptFile);

		String server = pluginConfig.getSimple(
				ApplicationServerComponent.PluginConfigPropNames.NAMING_URL)
				.getStringValue();
		if (server != null) {
			processExecution.getArguments().add("--server=" + server);
		}

		String user = pluginConfig.getSimple(
				ApplicationServerComponent.PRINCIPAL_CONFIG_PROP)
				.getStringValue();
		if (user != null) {
			processExecution.getArguments().add("--user=" + user);
		}

		String password = pluginConfig.getSimple(
				ApplicationServerComponent.CREDENTIALS_CONFIG_PROP)
				.getStringValue();
		if (password != null) {
			processExecution.getArguments().add("--password=" + password);
		}

		processExecution.getArguments().add("--shutdown");

		/*
		 * This tells shutdown.bat not to call the Windows PAUSE command, which
		 * would cause the script to hang indefinitely waiting for input.
		 * noinspection ConstantConditions
		 */
		processExecution.getEnvironmentVariables().put("NOPAUSE", "1");

		if (log.isDebugEnabled()) {
			log.debug("About to execute the following process: ["
					+ processExecution + "]");
		}
		ProcessExecutionResults results = this.systemInfo
				.executeProcess(processExecution);
		logExecutionResults(results);

		if (results.getError() != null) {
			throw new RuntimeException(
					"Error executing shutdown script while stopping AS instance. Exit code ["
							+ results.getExitCode() + "]", results.getError());
		}

		return "Server has been shut down.";
	}

	private void logExecutionResults(ProcessExecutionResults results) {
		// Always log the output at info level. On Unix we could switch
		// depending on a exitCode being !=0, but ...
		log.info("Exit code from process execution: " + results.getExitCode());
		log.info("Output from process execution: " + SEPARATOR
				+ results.getCapturedOutput() + SEPARATOR);
	}

	/**
	 * Shuts down the AS server via a JMX call.
	 * 
	 * @return success message if no errors are encountered
	 */
	private String shutdownViaJmx() {
		String mbeanName = pluginConfig
				.getSimple(
						ApplicationServerComponent.PluginConfigPropNames.SHUTDOWN_MBEAN_CONFIG_PROP)
				.getStringValue();
		String operationName = pluginConfig
				.getSimple(
						ApplicationServerComponent.PluginConfigPropNames.SHUTDOWN_MBEAN_OPERATION_CONFIG_PROP)
				.getStringValue();

		EmsConnection connection = this.serverComponent.getEmsConnection();
		if (connection == null) {
			throw new RuntimeException("Can not connect to the server");
		}
		EmsBean bean = connection.getBean(mbeanName);
		EmsOperation operation = bean.getOperation(operationName);
		/*
		 * Now see if we got the 'real' method (the one with no param) or the
		 * overloaded one. This is a workaround for a bug in EMS that prevents
		 * finding operations with same name and different signature.
		 * http://sourceforge
		 * .net/tracker/index.php?func=detail&aid=2007692&group_id
		 * =60228&atid=493495
		 * 
		 * In addition, as we offer the user to specify any MBean and any
		 * method, we'd need a clever way for the user to specify parameters
		 * anyway.
		 */
		List<EmsParameter> params = operation.getParameters();
		int count = params.size();
		if (count == 0)
			operation.invoke(new Object[0]);
		else { // overloaded operation
			operation.invoke(new Object[] { 0 }); // return code of 0
		}

		return "Server has been shut down.";
	}

	private void validateScriptFile(File scriptFile, String scriptPropertyName) {
		if (!scriptFile.exists()) {
			throw new RuntimeException("Script (" + scriptFile
					+ ") specified via '" + scriptPropertyName
					+ "' connection property does not exist.");
		}

		if (scriptFile.isDirectory()) {
			throw new RuntimeException("Script (" + scriptFile
					+ ") specified via '" + scriptPropertyName
					+ "' connection property is a directory, not a file.");
		}
	}

	/**
	 * Restart the server by first trying a shutdown and then a start. This is
	 * fail fast.
	 * 
	 * @return A success message on success
	 */
	private String restart() {

		try {
			shutdown();
		} catch (Exception e) {
			throw new RuntimeException("Shutdown may have failed: " + e);
		}

		try {
			// Perform the restart.
			start();

		} catch (Exception e) {
			throw new RuntimeException("Re-Startup may have failed: " + e);
		}

		return "Server has been restarted.";

	}

	private AvailabilityType waitForServerToStart(long start)
			throws InterruptedException {
		AvailabilityType avail;
		while (((avail = getAvailability()) == AvailabilityType.DOWN)
				&& (System.currentTimeMillis() < (start + START_WAIT_MAX))) {
			try {
				Thread.sleep(START_WAIT_INTERVAL);
			} catch (InterruptedException e) {
				// ignore
			}
		}
		return avail;
	}

	private AvailabilityType waitForServerToShutdown() {
		for (long wait = 0L; (wait < STOP_WAIT_MAX)
				&& (AvailabilityType.UP == getAvailability()); wait += STOP_WAIT_INTERVAL) {
			try {
				Thread.sleep(STOP_WAIT_INTERVAL);
			} catch (InterruptedException e) {
				// ignore
			}
		}

		// After the server shows unavailable, wait a little longer to hopefully
		// ensure shutdown is complete.
		try {
			Thread.sleep(STOP_WAIT_FINAL);
		} catch (InterruptedException e) {
			// ignore
		}

		return getAvailability();
	}

	/**
	 * Return the absolute path of this JBoss server's start script (e.g.
	 * "C:\opt\jboss-5.1.0.GA\bin\run.sh").
	 * 
	 * @return the absolute path of this JBoss server's start script (e.g.
	 *         "C:\opt\jboss-5.1.0.GA\bin\run.sh")
	 */
	@NotNull
	public File getStartScriptPath() {
		Configuration pluginConfig = serverComponent.getResourceContext()
				.getPluginConfiguration();
		String startScript = pluginConfig
				.getSimpleValue(
						ApplicationServerComponent.PluginConfigPropNames.START_SCRIPT_CONFIG_PROP,
						DEFAULT_START_SCRIPT);
		File startScriptFile = resolvePathRelativeToHomeDir(startScript);
		return startScriptFile;
	}

	@NotNull
	private File resolvePathRelativeToHomeDir(@NotNull String path) {
		return resolvePathRelativeToHomeDir(serverComponent
				.getResourceContext().getPluginConfiguration(), path);
	}

	@NotNull
	private File resolvePathRelativeToHomeDir(Configuration pluginConfig,
			@NotNull String path) {
		File configDir = new File(path);
		if (!configDir.isAbsolute()) {
			String jbossHomeDir = getRequiredPropertyValue(pluginConfig,
					ApplicationServerComponent.PluginConfigPropNames.HOME_DIR);
			configDir = new File(jbossHomeDir, path);
		}

		return configDir;
	}

	@NotNull
	private String getRequiredPropertyValue(@NotNull Configuration config,
			@NotNull String propName) {
		String propValue = config.getSimpleValue(propName, null);
		if (propValue == null) {
			// Something's not right - neither autodiscovery, nor the config
			// edit GUI, should ever allow this.
			throw new IllegalStateException("Required property '" + propName
					+ "' is not set.");
		}

		return propValue;
	}

	/**
	 * Return the absolute path of this JBoss server's shutdown script (e.g.
	 * "C:\opt\jboss-5.1.0.GA\bin\shutdown.sh").
	 * 
	 * @return the absolute path of this JBoss server's shutdown script (e.g.
	 *         "C:\opt\jboss-5.1.0.GA\bin\shutdown.sh")
	 */
	@NotNull
	public File getShutdownScriptPath() {
		Configuration pluginConfig = serverComponent.getResourceContext()
				.getPluginConfiguration();
		String shutdownScript = pluginConfig
				.getSimpleValue(
						ApplicationServerComponent.PluginConfigPropNames.SHUTDOWN_SCRIPT_CONFIG_PROP,
						DEFAULT_SHUTDOWN_SCRIPT);
		File shutdownScriptFile = resolvePathRelativeToHomeDir(shutdownScript);
		return shutdownScriptFile;
	}

	/**
	 * Return the absolute path of this JBoss server's JAVA_HOME directory (e.g.
	 * "C:\opt\jdk1.5.0_14"); will only return null in the rare case when the
	 * "java.home" system property is not set, and when this is the case, a
	 * warning will be logged.
	 * 
	 * @return the absolute path of this JBoss server's JAVA_HOME directory
	 *         (e.g. "C:\opt\jdk1.5.0_14"); will only be null in the rare case
	 *         when the "java.home" system property is not set
	 */
	@Nullable
	public File getJavaHomePath() {
		Configuration pluginConfig = serverComponent.getResourceContext()
				.getPluginConfiguration();
		String javaHomePath = pluginConfig.getSimple(
				ApplicationServerComponent.PluginConfigPropNames.JAVA_HOME)
				.getStringValue();

		if (javaHomePath == null) {
			log
					.warn("The '"
							+ "JAVA_HOME"
							+ "' System property is not set - unable to set default value for the '"
							+ "JAVA_HOME" + "' connection property.");
		}

		File javaHome = (javaHomePath != null) ? new File(javaHomePath) : null;
		return javaHome;
	}

	void validateJavaHomePathProperty() {
		String javaHome = pluginConfig.getSimple(
				ApplicationServerComponent.PluginConfigPropNames.JAVA_HOME)
				.getStringValue();
		if (javaHome != null) {
			File javaHomeDir = new File(javaHome);
			if (!javaHomeDir.isAbsolute()) {
				throw new InvalidPluginConfigurationException(
						ApplicationServerComponent.PluginConfigPropNames.JAVA_HOME
								+ " connection property ('"
								+ javaHomeDir
								+ "') is not an absolute path. Note, on Windows, absolute paths must start with the drive letter (e.g. C:).");
			}

			if (!javaHomeDir.exists()) {
				throw new InvalidPluginConfigurationException(
						ApplicationServerComponent.PluginConfigPropNames.JAVA_HOME
								+ " connection property ('"
								+ javaHomeDir
								+ "') does not exist.");
			}

			if (!javaHomeDir.isDirectory()) {
				throw new InvalidPluginConfigurationException(
						ApplicationServerComponent.PluginConfigPropNames.JAVA_HOME
								+ " connection property ('"
								+ javaHomeDir
								+ "') is not a directory.");
			}
		}
	}

	public AvailabilityType getAvailability() {
		try {
			EmsConnection connection = serverComponent.getEmsConnection();
			EmsBean bean = connection.getBean("jboss.system:type=ServerConfig");

			File serverHomeViaJNP = (File) bean.getAttribute("ServerHomeDir")
					.refresh();

			if (configPath == null)
				getConfigurationSet();

			if (this.configPath.getCanonicalPath().equals(
					serverHomeViaJNP.getCanonicalPath())) {
				return AvailabilityType.UP;
			} else {
				// a different server must have been started on our jnp url
				if (log.isDebugEnabled()) {
					log
							.debug("Availability check for JBAS resource with configPath ["
									+ this.configPath
									+ "] is trying to connect to a different running JBAS which is installed at ["
									+ serverHomeViaJNP
									+ "]. Returning AvailabilityType.DOWN for the former resource.");
				}
			}
			return AvailabilityType.DOWN;
		} catch (Exception e) {
			return AvailabilityType.DOWN;
		}
	}
}
