package org.rhq.plugins.byteman;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.byteman.agent.submit.Submit;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.transfer.ContentResponseResult;
import org.rhq.core.domain.content.transfer.DeployIndividualPackageResponse;
import org.rhq.core.domain.content.transfer.DeployPackageStep;
import org.rhq.core.domain.content.transfer.DeployPackagesResponse;
import org.rhq.core.domain.content.transfer.RemoveIndividualPackageResponse;
import org.rhq.core.domain.content.transfer.RemovePackagesResponse;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.content.ContentContext;
import org.rhq.core.pluginapi.content.ContentFacet;
import org.rhq.core.pluginapi.content.ContentServices;
import org.rhq.core.pluginapi.inventory.CreateChildResourceFacet;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.core.util.exception.ThrowableUtil;

/**
 * Component that represents the remote Byteman agent listening for requests.
 *
 * A note about adding boot/system classpath jars and the content this component supports related to that feature.
 * There are operations this component supports to add jars to the byteman classpath. Those operations tell the
 * byteman agent to add jars, but those jars must already exist and be accessible for the byteman agent to do so.
 * This component will not manage the jars added via the operations. If, however, a user pushes jar content
 * from the RHQ server to this plugin via the content facet, those jars will be managed by this component as they
 * are added to the byteman agent.
 *
 * @author John Mazzitelli
 */
public class BytemanAgentComponent implements ResourceComponent<BytemanAgentComponent>, MeasurementFacet,
    OperationFacet, ContentFacet, CreateChildResourceFacet, ConfigurationFacet {

    private static final String PKG_TYPE_NAME_BOOT_JAR = "bootJar";
    private static final String PKG_TYPE_NAME_SYSTEM_JAR = "systemJar";

    private final Log log = LogFactory.getLog(BytemanAgentComponent.class);

    private ResourceContext<BytemanAgentComponent> resourceContext;
    private Submit bytemanClient;
    private File bootJarsDataDir; // where managed boot jars will be persisted
    private File systemJarsDataDir; // where managed system jars will be persisted
    private File scriptsDataDir; // where managed scripts will be persisted
    private Map<String, String> allKnownScripts; // cached copy of currently known scripts

    /**
     * Start the management component. This will immediately attempt to add previously
     * deployed classpath jars, if it is found that the remote Byteman agent no longer
     * has those jars in its classpath.
     *
     * @see ResourceComponent#start(ResourceContext)
     */
    public void start(ResourceContext<BytemanAgentComponent> context) {
        this.resourceContext = context;
        this.bootJarsDataDir = getResourceDataDirectory("boot");
        this.systemJarsDataDir = getResourceDataDirectory("system");
        this.scriptsDataDir = getResourceDataDirectory("script");

        getBytemanClient(); // creates its client now

        // now that we are starting to manage the byteman agent, make sure the managed classpath jars are added
        try {
            addDeployedClasspathJars();
        } catch (Throwable t) {
            log.warn("Failed to add managed classpath jars to the byteman agent - is it up?", t);
        }

        getAvailability(); // forces the scripts cache to load

        return;
    }

    /**
     * Called when the resource component will no longer manage the remote Byteman agent.
     * This method will clean up the resource component.
     *
     * @see ResourceComponent#stop()
     */
    public void stop() {
        this.resourceContext = null;
        this.bytemanClient = null;
        this.allKnownScripts = null;
    }

    /**
     * Determines if the Byteman agent is up by asking it for the current list of all scripts and their rules.
     *
     * @see AvailabilityFacet#getAvailability()
     */
    public AvailabilityType getAvailability() {
        try {
            this.allKnownScripts = getBytemanClient().getAllRules();
            return AvailabilityType.UP;
        } catch (Exception e) {
            this.allKnownScripts = null;
            return AvailabilityType.DOWN;
        }
    }

    /**
     * The plugin container will call this method when metrics are to be collected.
     *
     * @see MeasurementFacet#getValues(MeasurementReport, Set)
     */
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests) {
        Submit client = getBytemanClient();

        // a cache so we don't ask the byteman agent more than once for this while we are in here
        // we don't rely on this.allKnownScripts because we want the latest-n-greatest value for our metrics
        Map<String, String> allScripts = null;

        for (MeasurementScheduleRequest request : requests) {
            String name = request.getName();

            try {
                if (name.equals("TRAIT-clientVersion")) {
                    String clientVersion = client.getClientVersion();
                    report.addData(new MeasurementDataTrait(request, clientVersion));
                } else if (name.equals("totalNumberOfScripts")) {
                    int total = 0;
                    if (allScripts == null) {
                        allScripts = client.getAllRules();
                    }
                    if (allScripts != null) {
                        total += allScripts.size();
                    }
                    report.addData(new MeasurementDataNumeric(request, Double.valueOf(total)));
                } else if (name.equals("totalNumberOfRules")) {
                    int total = 0;
                    if (allScripts == null) {
                        allScripts = client.getAllRules();
                    }
                    if (allScripts != null) {
                        for (String script : allScripts.values()) {
                            total += client.splitAllRulesFromScript(script).size();
                        }
                    }
                    report.addData(new MeasurementDataNumeric(request, Double.valueOf(total)));
                } else if (name.equals("totalNumberOfBootJars")) {
                    int total = 0;
                    List<String> loadedJars = client.getLoadedBootClassloaderJars();
                    if (loadedJars != null) {
                        total = loadedJars.size();
                    }
                    report.addData(new MeasurementDataNumeric(request, Double.valueOf(total)));
                } else if (name.equals("totalNumberOfSystemJars")) {
                    int total = 0;
                    List<String> loadedJars = client.getLoadedSystemClassloaderJars();
                    if (loadedJars != null) {
                        total = loadedJars.size();
                    }
                    report.addData(new MeasurementDataNumeric(request, Double.valueOf(total)));
                } else {
                    throw new Exception("cannot collect unknown metric");
                }
            } catch (Exception e) {
                log.error("Failed to obtain measurement [" + name + "]. Cause: " + e);
            }
        }

        return;
    }

    public Configuration loadResourceConfiguration() throws Exception {
        Properties props = getBytemanClient().listSystemProperties();
        Configuration config = new Configuration();
        PropertyList list = new PropertyList("bytemanSystemProperties");
        for (Map.Entry<Object, Object> entry : props.entrySet()) {
            String name = entry.getKey().toString();
            if (name.startsWith("org.jboss.byteman.")) {
                PropertyMap map = new PropertyMap("bytemanSystemProperty");
                map.put(new PropertySimple("name", name));
                map.put(new PropertySimple("value", entry.getValue().toString()));
                list.add(map);
            }
        }

        if (list.getList().size() > 0) {
            config.put(list);
        }

        return config;
    }

    public void updateResourceConfiguration(ConfigurationUpdateReport report) {

        try {
            Properties propsToSet = new Properties();

            Configuration config = report.getConfiguration();
            PropertyList list = config.getList("bytemanSystemProperties");
            List<Property> maps = list.getList();
            for (Property map : maps) {
                Map<String, Property> nameValue = ((PropertyMap) map).getMap();
                String name = ((PropertySimple) nameValue.get("name")).getStringValue();
                if (name.startsWith("org.jboss.byteman.")) {
                    String value = ((PropertySimple) nameValue.get("value")).getStringValue();
                    // byteman will not allow us to turn off strict mode
                    if (!name.equals("org.jboss.byteman.sysprops.strict") || value.equals("true")) {
                        propsToSet.put(name, value);
                    }
                }
            }

            getBytemanClient().setSystemProperties(propsToSet);
            log.info("Set byteman configuration: " + propsToSet);
            report.setStatus(ConfigurationUpdateStatus.SUCCESS);
        } catch (Exception e) {
            report.setErrorMessageFromThrowable(e);
        }

        return;
    }

    /**
     * The plugin container will call this method when it wants to invoke an operation on
     * the Byteman agent.
     *
     * @see OperationFacet#invokeOperation(String, Configuration)
     */
    public OperationResult invokeOperation(String name, Configuration configuration) {

        OperationResult result = new OperationResult();
        Submit client = getBytemanClient();

        try {
            if ("getRule".equals(name)) {
                //
                // getRule == retrieves the rule definition for a given rule
                String ruleName = configuration.getSimpleValue("ruleName", null);
                if (ruleName == null || ruleName.length() == 0) {
                    throw new Exception("Did not specify the name of the rule to get");
                }
                Map<String, String> allScripts = client.getAllRules();
                for (String script : allScripts.values()) {
                    List<String> rules = client.splitAllRulesFromScript(script);
                    for (String rule : rules) {
                        if (ruleName.equals(client.determineRuleName(rule))) {
                            Configuration resultConfig = result.getComplexResults();
                            resultConfig.put(new PropertySimple("ruleDefinition", rule));
                            return result;
                        }
                    }
                }
                throw new Exception("No rule was found with the name [" + ruleName + "]");
            } else if ("getClientVersion".equals(name)) {
                //
                // getClientVersion == return the version string of the client this plugin is using
                String clientVersion = client.getClientVersion();
                Configuration resultConfig = result.getComplexResults();
                resultConfig.put(new PropertySimple("version", (clientVersion == null) ? "<unknown>" : clientVersion));
                return result;
            } else if ("addJarsToSystemClasspath".equals(name)) {
                //
                // addJarsToSystemClasspath == adds a jar to the remote byteman agent's system classpath
                String jarPaths = configuration.getSimpleValue("jarPathnames", null);
                if (jarPaths == null || jarPaths.length() == 0) {
                    throw new Exception("Did not specify any jars to add");
                }
                String[] jarPathsArr = jarPaths.split(",");
                List<String> jarPathList = new ArrayList<String>();
                for (String jarPathString : jarPathsArr) {
                    jarPathList.add(jarPathString);
                }
                String response = client.addJarsToSystemClassloader(jarPathList);
                result.setSimpleResult(response);
                return result;
            } else if ("addJarsToBootClasspath".equals(name)) {
                //
                // addJarsToBootClasspath == adds a jar to the remote byteman agent's boot classpath
                String jarPaths = configuration.getSimpleValue("jarPathnames", null);
                if (jarPaths == null || jarPaths.length() == 0) {
                    throw new Exception("Did not specify any jars to add");
                }
                String[] jarPathsArr = jarPaths.split(",");
                List<String> jarPathList = new ArrayList<String>();
                for (String jarPathString : jarPathsArr) {
                    jarPathList.add(jarPathString);
                }
                String response = client.addJarsToBootClassloader(jarPathList);
                result.setSimpleResult(response);
                return result;
            } else if ("getAddedClasspathJars".equals(name)) {
                //
                // getAddedClasspathJars == gets all jars that were added to the byteman agent's boot and system classpaths
                Configuration resultConfig = result.getComplexResults();
                List<String> jars;

                jars = client.getLoadedBootClassloaderJars();
                if (jars != null && !jars.isEmpty()) {
                    PropertyList list = new PropertyList("additionalBootClasspathJars");
                    for (String jar : jars) {
                        PropertyMap map = new PropertyMap("additionalBootClasspathJar");
                        map.put(new PropertySimple("jarPathname", jar));
                        list.add(map);
                    }
                    resultConfig.put(list);
                }

                jars = client.getLoadedSystemClassloaderJars();
                if (jars != null && !jars.isEmpty()) {
                    PropertyList list = new PropertyList("additionalSystemClasspathJars");
                    for (String jar : jars) {
                        PropertyMap map = new PropertyMap("additionalSystemClasspathJar");
                        map.put(new PropertySimple("jarPathname", jar));
                        list.add(map);
                    }
                    resultConfig.put(list);
                }

                return result;
            } else {
                throw new UnsupportedOperationException(name);
            }
        } catch (Exception e) {
            result.setErrorMessage(ThrowableUtil.getAllMessages(e));
            return result;
        }
    }

    /**
     * Detects the different content for the Byteman agent. This will only discover
     * content that was previously deployed by the RHQ content facet mechanism. In other words,
     * content already deployed in the Byteman agent, or deployed by some other non-RHQ means,
     * will not be detected.
     *
     * @see ContentFacet#discoverDeployedPackages(PackageType)
     */
    public Set<ResourcePackageDetails> discoverDeployedPackages(PackageType type) {
        Set<ResourcePackageDetails> details = new HashSet<ResourcePackageDetails>();
        String typeName = type.getName();
        try {
            File[] discoveredFiles = null; // will only be non-null if 1 or more jars are discovered
            File dataDir;

            if (PKG_TYPE_NAME_BOOT_JAR.equals(typeName)) {
                dataDir = this.bootJarsDataDir;
            } else if (PKG_TYPE_NAME_SYSTEM_JAR.equals(typeName)) {
                dataDir = this.systemJarsDataDir;
            } else {
                throw new UnsupportedOperationException("Can only deploy boot/system jars");
            }

            File[] files = dataDir.listFiles();
            if (files != null && files.length > 0) {
                discoveredFiles = files;
            }

            if (discoveredFiles != null) {
                for (File file : discoveredFiles) {
                    String shortName = file.getName();
                    String sha256 = null;
                    try {
                        sha256 = new MessageDigestGenerator(MessageDigestGenerator.SHA_256).calcDigestString(file);
                    } catch (Exception e) {
                        if (log.isDebugEnabled()) {
                            log.debug("Failed to generate sha256 for [" + file + "]");
                        }
                    }
                    String version = getVersion(file, sha256);
                    PackageDetailsKey detailsKey = new PackageDetailsKey(shortName, version, typeName, "noarch");
                    ResourcePackageDetails detail = new ResourcePackageDetails(detailsKey);
                    detail.setDisplayName(shortName);
                    detail.setFileCreatedDate(file.lastModified());
                    detail.setFileName(shortName);
                    detail.setFileSize(file.length());
                    detail.setMD5(MessageDigestGenerator.getDigestString(file));
                    detail.setSHA256(sha256);
                    details.add(detail);
                }
            }
        } catch (Exception e) {
            log.error("Failed to perform discovery for packages of type [" + typeName + "]", e);
        }
        return details;
    }

    private String getVersion(File file, String sha256) {
        // Version string in order of preference
        // manifestVersion + sha256, sha256, manifestVersion, "0"
        String version = "0";
        String manifestVersion = null;
        try {
            manifestVersion = BytemanAgentDiscoveryComponent.getJarAttribute(file.getAbsolutePath(),
                java.util.jar.Attributes.Name.IMPLEMENTATION_VERSION.toString(), null);
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("Failed to determine manifestVersion for [" + file + "]");
            }
        }

        if ((null != manifestVersion) && (null != sha256)) {
            // this protects against the occasional differing binaries with poor manifest maintenance
            version = manifestVersion + " [sha256=" + sha256 + "]";
        } else if (null != sha256) {
            version = "[sha256=" + sha256 + "]";
        } else if (null != manifestVersion) {
            version = manifestVersion;
        }

        return version;
    }

    /**
     * Deploys boot and system classpath jars to the Byteman agent.
     *
     * @see ContentFacet#deployPackages(Set, ContentServices)
     */
    public DeployPackagesResponse deployPackages(Set<ResourcePackageDetails> packages, ContentServices contentServices) {
        Submit client = getBytemanClient();
        DeployPackagesResponse response = new DeployPackagesResponse();
        DeployIndividualPackageResponse individualResponse;

        for (ResourcePackageDetails detail : packages) {
            PackageDetailsKey packageKey = detail.getKey();
            String packageType = detail.getPackageTypeName();

            individualResponse = new DeployIndividualPackageResponse(packageKey);
            response.addPackageResponse(individualResponse);

            try {
                Boolean isBootJar = null; // null if not a classpath jar (this is for the future; today, it will always get set)
                // if necessary, create the data directory where the file should be deployed
                if (PKG_TYPE_NAME_BOOT_JAR.equals(packageType)) {
                    this.bootJarsDataDir.mkdirs();
                    isBootJar = Boolean.TRUE;
                } else if (PKG_TYPE_NAME_SYSTEM_JAR.equals(packageType)) {
                    this.systemJarsDataDir.mkdirs();
                    isBootJar = Boolean.FALSE;
                } else {
                    throw new UnsupportedOperationException("Cannot deploy package of type [" + packageType + "]");
                }

                // download the package to our data directory
                File newFile = getPackageFile(detail);
                FileOutputStream fos = new FileOutputStream(newFile);
                try {
                    ContentContext contentContext = this.resourceContext.getContentContext();
                    contentServices.downloadPackageBits(contentContext, packageKey, fos, true);
                } finally {
                    fos.close();
                }

                // tell the byteman agent to add it to the proper classloader
                if (isBootJar != null) {
                    if (isBootJar.booleanValue()) {
                        client.addJarsToBootClassloader(Arrays.asList(newFile.getAbsolutePath()));
                    } else {
                        client.addJarsToSystemClassloader(Arrays.asList(newFile.getAbsolutePath()));
                    }
                }

                // everything is OK
                individualResponse.setResult(ContentResponseResult.SUCCESS);
            } catch (Exception e) {
                individualResponse.setErrorMessage(ThrowableUtil.getStackAsString(e));
                individualResponse.setResult(ContentResponseResult.FAILURE);
            }
        }

        return response;
    }

    /**
     * When a remote client wants to see the actual data content for an installed package, this method will be called.
     * This method must return a stream of data containing the full content of the package.
     *
     * @see ContentFacet#retrievePackageBits(ResourcePackageDetails)
     */
    public InputStream retrievePackageBits(ResourcePackageDetails packageDetails) {
        try {
            File file = getPackageFile(packageDetails);
            return new FileInputStream(file);
        } catch (Exception e) {
            log.error("Cannot retrieve content for package [" + packageDetails + "]");
            throw new RuntimeException(e);
        }
    }

    /**
     * Essentially a no-op - there are no installation steps associated with Byteman content.
     */
    public List<DeployPackageStep> generateInstallationSteps(ResourcePackageDetails packageDetails) {
        return new ArrayList<DeployPackageStep>(0);
    }

    /**
     * Removes the packages, if they are managed by this component. Note that the Byteman agent does not
     * support runtime removal of jars from its classpaths, so the Byteman agent will retain classpath
     * jars in its memory until the VM is restarted, even if this component was asked to remove
     * classpath jars.
     *
     * @see ContentFacet#removePackages(Set)
     */
    public RemovePackagesResponse removePackages(Set<ResourcePackageDetails> packages) {
        RemovePackagesResponse response = new RemovePackagesResponse();
        RemoveIndividualPackageResponse individualResponse;

        for (ResourcePackageDetails detail : packages) {
            individualResponse = new RemoveIndividualPackageResponse(detail.getKey());
            response.addPackageResponse(individualResponse);
            try {
                File packageFile = getPackageFile(detail);
                if (packageFile.delete()) {
                    individualResponse.setResult(ContentResponseResult.SUCCESS);
                } else {
                    individualResponse.setErrorMessage("Failed to delete [" + packageFile.getAbsolutePath() + "]");
                    individualResponse.setResult(ContentResponseResult.FAILURE);
                }
            } catch (Exception e) {
                individualResponse.setErrorMessage(ThrowableUtil.getStackAsString(e));
                individualResponse.setResult(ContentResponseResult.FAILURE);
            }
        }

        return response;
    }

    /**
     * Creates a new script by deploying the script file to the Byteman agent.
     *
     * @see CreateChildResourceFacet#createResource(CreateResourceReport)
     */
    public CreateResourceReport createResource(CreateResourceReport report) {
        try {
            this.scriptsDataDir.mkdirs();

            // determine where to store the script file when we download it;
            // do not allow the file to be placed in a subdirectory under our data dir (i.e. take out file separators)
            ResourcePackageDetails newDetails = report.getPackageDetails();
            String newName = report.getUserSpecifiedResourceName();
            if (newName == null) {
                newName = newDetails.getName();
                if (newName == null) {
                    throw new NullPointerException("was not given a name for the new script");
                }
            }
            newName = newName.replace('/', '-').replace('\\', '-');

            File newFile = new File(this.scriptsDataDir, newName);
            String newFileAbsolutePath = newFile.getAbsolutePath();

            // download the file from the server
            ContentContext contentContext = this.resourceContext.getContentContext();
            ContentServices contentServices = contentContext.getContentServices();
            ResourceType newChildResourceType = report.getResourceType();
            FileOutputStream fos = new FileOutputStream(newFile);
            BufferedOutputStream outputStream = new BufferedOutputStream(fos);
            try {
                contentServices.downloadPackageBitsForChildResource(contentContext, newChildResourceType.getName(),
                    newDetails.getKey(), outputStream);
            } finally {
                outputStream.close();
            }

            // deploy the scripts rules in byteman agent
            getBytemanClient().addRulesFromFiles(Arrays.asList(newFileAbsolutePath));

            // we know where we put the file, fill in the details
            newDetails.setDisplayName(newName);
            newDetails.setFileName(newFileAbsolutePath);
            newDetails.setFileSize(newFile.length());
            newDetails.setInstallationTimestamp(newFile.lastModified());
            newDetails.setMD5(MessageDigestGenerator.getDigestString(newFile));

            // complete the report
            report.setResourceKey(newFileAbsolutePath);
            report.setResourceName(newName);
            report.setStatus(CreateResourceStatus.SUCCESS);
        } catch (Throwable t) {
            log.error("Failed to create child resource [" + report + "]", t);
            report.setException(t);
            report.setStatus(CreateResourceStatus.FAILURE);
        }
        return report;
    }

    /**
     * Returns a client that can be used to talk to the remote Byteman agent.
     *
     * @return client object
     */
    public Submit getBytemanClient() {
        if (this.bytemanClient == null) {
            Configuration pc = this.resourceContext.getPluginConfiguration();

            // get the address/port from the plugin config - defaults will be null to force NPEs which is OK, because nulls are error conditions
            String address = pc.getSimpleValue(BytemanAgentDiscoveryComponent.PLUGIN_CONFIG_PROP_ADDRESS, null);
            String port = pc.getSimpleValue(BytemanAgentDiscoveryComponent.PLUGIN_CONFIG_PROP_PORT, null);

            this.bytemanClient = new Submit(address, Integer.valueOf(port).intValue());
        }

        return this.bytemanClient;
    }

    /**
     * Returns a cached copy of all known scripts since the last availability check was made.
     * Use this if you do not need the most up-to-date list, which helps avoid making unnecessary
     * calls to the remote Byteman agent. If you need the most up-to-date data, call the agent
     * using {@link #getBytemanClient() the client}.
     *
     * @return the last known set of scripts that were loaded in the remote Byteman agent. <code>null</code>
     *         if a problem occurred attempting to get the scripts
     */
    public Map<String, String> getAllKnownScripts() {
        // if we already have a non-null value, use it as-is; otherwise, try to get it now
        if (this.allKnownScripts == null) {
            try {
                this.allKnownScripts = getBytemanClient().getAllRules();
            } catch (Exception ignore) {
            }
        }
        return this.allKnownScripts;
    }

    /**
     * Given a package details, this will attempt to find that package's file.
     * The details "file name" is examined first to figure out where the file is supposed to be.
     * Only if that isn't set will the details general "name" be used as the file name.
     * If the "file name" (or "name") is not absolute, it will be assumed to be in one
     * of the subdirectories under this component's data directory, based on the package type name.
     *
     * @param packageDetails details describing the file
     * @return the file that corresponds to the details object - this file may or may not exist;
     *         existence is not a requirement for this method to return a valid File object
     *
     * @throws Exception if the file could not be determined
     */
    public File getPackageFile(ResourcePackageDetails packageDetails) throws Exception {
        String path = packageDetails.getFileName();
        if (path == null) {
            path = packageDetails.getName(); // if no filename was given, assume the package name is the path
        }

        File file = new File(path);
        if (!file.isAbsolute()) {
            String typeName = packageDetails.getPackageTypeName();
            if (PKG_TYPE_NAME_BOOT_JAR.equals(typeName)) {
                file = new File(this.bootJarsDataDir, path);
            } else if (PKG_TYPE_NAME_SYSTEM_JAR.equals(typeName)) {
                file = new File(this.systemJarsDataDir, path);
            } else if (BytemanScriptComponent.PKG_TYPE_NAME_SCRIPT.equals(typeName)) {
                file = new File(this.scriptsDataDir, path);
            } else {
                throw new Exception("Invalid package type - cannot get package file");
            }
        }
        return file;
    }

    /**
     * @return directory where managed boot classpath jars are persisted
     */
    public File getBootJarsDataDirectory() {
        return this.bootJarsDataDir;
    }

    /**
     * @return directory where managed system classpath jars are persisted
     */
    public File getSystemJarsDataDirectory() {
        return this.systemJarsDataDir;
    }

    /**
     * @return directory where managed scripts are persisted. Scripts are files
     * that contain rules.
     */
    public File getScriptsDataDirectory() {
        return this.scriptsDataDir;
    }

    /**
     * Returns the component's data directory that is used to persist managed content.
     * <code>suffix</code> is the last part of the file path, essentially providing a specific
     * location for different kinds of content for the component.
     *
     * @param suffix identifies a specific location under a general data directory for this component.
     * @return data directory that can be used to persist data for this component
     */
    public File getResourceDataDirectory(String suffix) {
        File pluginDataDir = this.resourceContext.getDataDirectory();
        File resourceDataDir = new File(pluginDataDir, this.resourceContext.getResourceKey().replace(":", "-"));
        if (suffix != null) {
            resourceDataDir = new File(resourceDataDir, suffix);
        }
        return resourceDataDir;
    }

    /**
     * Goes through all jars that were deployed via RHQ and ensures they are still deployed, adding
     * them if need be.
     *
     * @throws Exception
     */
    protected void addDeployedClasspathJars() throws Exception {
        Submit client = getBytemanClient();
        List<String> paths = new ArrayList<String>();

        // do the boot jars first
        File dataDir = this.bootJarsDataDir;
        File[] files = dataDir.listFiles();
        if (files != null && files.length > 0) {
            List<String> loadedJars = client.getLoadedBootClassloaderJars();
            for (File file : files) {
                if (!loadedJars.contains(file.getAbsolutePath())) {
                    paths.add(file.getAbsolutePath());
                }
            }
            client.addJarsToBootClassloader(paths);
        }

        // now do the system jars
        paths.clear();
        dataDir = this.systemJarsDataDir;
        files = dataDir.listFiles();
        if (files != null && files.length > 0) {
            List<String> loadedJars = client.getLoadedSystemClassloaderJars();
            for (File file : files) {
                if (!loadedJars.contains(file.getAbsolutePath())) {
                    paths.add(file.getAbsolutePath());
                }
            }
            client.addJarsToSystemClassloader(paths);
        }

        return;
    }
}
