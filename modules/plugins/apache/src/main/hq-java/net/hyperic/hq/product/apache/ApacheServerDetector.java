package net.hyperic.hq.product.apache;

import org.jboss.on.plugins.apache.util.ApacheBinaryInfo;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import net.hyperic.hq.product.AutoServerDetector;
import net.hyperic.hq.product.FileServerDetector;
import net.hyperic.hq.product.PluginException;
import net.hyperic.hq.product.PluginManager;
import net.hyperic.hq.product.ServerDetector;
import net.hyperic.hq.product.ServerResource;
import net.hyperic.hq.product.ServiceResource;
import net.hyperic.hq.product.URLMetric;
import net.hyperic.hq.product.Win32ControlPlugin;

import net.hyperic.snmp.SNMPClient;
import net.hyperic.snmp.SNMPException;

import net.hyperic.util.config.ConfigResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ApacheServerDetector
    extends ServerDetector 
    implements FileServerDetector, AutoServerDetector {

    static final String DEFAULT_SERVICE_NAME = "Apache2";
    
    static final String VHOST_NAME = "VHost";

    static final String PROP_SERVER_NAME = "server.name";

    static final String[] PTQL_QUERIES = {
        "State.Name.eq=httpd,State.Name.Pne=$1",
        "State.Name.eq=apache2,State.Name.Pne=$1",
    };

    static final String[] PTQL_QUERIES_WIN32 = {
        "State.Name.eq=Apache,Args.1.Peq=-k,Args.2.Peq=runservice",
    };

    private static Log log = LogFactory.getLog("ApacheServerDetector");

    private Properties props;
    private String defaultIp;
    private PortRange httpRange;
    private PortRange httpsRange;

    public ApacheServerDetector () { 
        super();
    }

    public void init(PluginManager manager)
            throws PluginException {

        super.init(manager);
        this.props = manager.getProperties();
        this.defaultIp =
            this.props.getProperty("apache.listenAddress", "localhost");

        this.httpRange =
            new PortRange(this.props.getProperty("apache.http.ports"));

        this.httpsRange =
            new PortRange(this.props.getProperty("apache.https.ports"));
    }

    private static String getServerRoot(String[] args) {
        for (int i=1; i<args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("-d")) {
                String root = arg.substring(2, arg.length());
                if (root.length() == 0) {
                    root = args[i+1];
                }
                return root;
            }
        }

        return null;
    }

    private static void findServerProcess(List servers, String query,
                                          String version) {
        long[] pids = getPids(query);

        for (int i=0; i<pids.length; i++) {
            String httpd = getProcExe(pids[i]);

            if (httpd == null) {
                continue;
            }

            ApacheBinaryInfo info = ApacheBinaryInfo.getInfo(httpd);

            if (info == null) {
                continue;
            }

            String root = getServerRoot(getProcArgs(pids[i]));
            if (root != null) {
                //-d overrides compiled in HTTPD_ROOT
                info.root = root;
            }

            if (info.root == null) {
                continue;
            }

            servers.add(info);
        }
    }

    protected static List getServerProcessList(String version,
                                               String[] queries) {
        ArrayList servers = new ArrayList();

        for (int i=0; i<queries.length; i++) {
            findServerProcess(servers, queries[i], version);
        }

        return servers;
    }

    protected void configureServer(ServerResource server)
        throws PluginException {

        String installpath = server.getInstallPath();
        ConfigResponse metricConfig = getMeasurementConfig(installpath);
        ConfigResponse productConfig = getProductConfig(metricConfig);
        ConfigResponse controlConfig = getControlConfig(installpath);

        if (productConfig != null) {
            setProductConfig(server, productConfig);
            setMeasurementConfig(server, metricConfig);
            if (controlConfig != null) {
                setControlConfig(server, controlConfig);
            }

            server.setConnectProperties(new String[] {
                SNMPClient.PROP_PORT,
                SNMPClient.PROP_VERSION, //only need to avoid udp port conflicts
            });
        }
    }

    protected void configureServer(ServerResource server, ApacheBinaryInfo binary)
        throws PluginException {
        
        configureServer(server);
        
        ConfigResponse cprops = new ConfigResponse(binary.toProperties());

        server.setCustomProperties(cprops);
    }

    public List getServerList(String installpath, String fullVersion,
                              ApacheBinaryInfo binary)
        throws PluginException {

        if (new File(installpath, "bin/httpsd.pthread").exists()) {
            //ERS 3.0: dont want this reported as a 1.3 server type
            return null;
        }

        ServerResource sValue = createServerResource(installpath);

        configureServer(sValue, binary);

        //name was already set, but we want to include .minor version too
        String name = getPlatformName() + " Apache " + fullVersion;
        sValue.setName(name);

        List servers = new ArrayList();
        servers.add(sValue);
        return servers;
    }

    protected ConfigResponse getMeasurementConfig(String path) {
        ApacheSNMP.ConfigFile config = null;
        //XXX conf dir is not always relative to installpath
        File file = new File(path, "conf" + File.separator + "snmpd.conf");

        if (file.exists()) {
            try {
                config = ApacheSNMP.getConfig(file.toString());
                log.debug(path + " snmp agent=" + config);
            } catch (IOException e) {
                log.warn("Unable to parse SNMP port from: " + file, e);
            }
        }

        return new ConfigResponse(ApacheSNMP.getConfigProperties(config));
    }

    protected ConfigResponse getProductConfig(ConfigResponse mConfig) {
        ApacheSNMP snmp = new ApacheSNMP();
        List servers;

        try {
            servers = snmp.getServers(mConfig);
        } catch (SNMPException e) {
            log.debug("getServers(" + mConfig + ") failed: " + e, e);
            return null;
        }

        if (servers.size() == 0) {
            log.debug("getServers(" + mConfig + ") == 0");
            return null;
        }

        Properties config = new Properties();

        //first entry will be the main server
        ApacheSNMP.Server server =
            (ApacheSNMP.Server)servers.get(0);

        config.setProperty(URLMetric.PROP_PROTOCOL,
                           guessProtocol(server.port));
        config.setProperty(URLMetric.PROP_HOSTNAME, this.defaultIp);
        config.setProperty(URLMetric.PROP_PORT, server.port);
        config.setProperty(PROP_SERVER_NAME,
                           server.name);

        log.debug("Configured server via snmp: " + server);

        return new ConfigResponse(config);
    }

    protected String getWindowsServiceName() {
        return DEFAULT_SERVICE_NAME;
    }

    protected String getDefaultControlScript() {
        String file = getTypeProperty("DEFAULT_SCRIPT");
        if (file != null) {
            return file;
        }
        else {
            return ApacheControlPlugin.DEFAULT_SCRIPT;
        }
    }

    protected String getDefaultPidFile() {
        String file = getTypeProperty("DEFAULT_PIDFILE");
        if (file != null) {
            return file;
        }
        else {
            return ApacheControlPlugin.DEFAULT_PIDFILE;
        }
    }

    protected ConfigResponse getControlConfig(String path) {
        Properties props = new Properties();

        if (isWin32()) {
            props.setProperty(Win32ControlPlugin.PROP_SERVICENAME,
                              getWindowsServiceName());
        }
        else {
            String script = path + "/" + getDefaultControlScript();

            if (new File(script).exists()) {
                props.setProperty(ApacheControlPlugin.PROP_PROGRAM,
                                  script);
            }
            else {
                return null; //XXX
            }

            props.setProperty(ApacheControlPlugin.PROP_PIDFILE,
                              path + "/" + getDefaultPidFile());
        }

        return new ConfigResponse(props);
    }

    private static String[] getPtqlQueries() {
        if (isWin32()) {
            return PTQL_QUERIES_WIN32;
        }
        else {
            return PTQL_QUERIES;
        }
    }

    public List getServerResources(ConfigResponse platformConfig) throws PluginException {

        String version = getTypeInfo().getVersion();
        List servers = new ArrayList();
        List binaries = getServerProcessList(version, getPtqlQueries());

        for (int i=0; i<binaries.size(); i++) {
            ApacheBinaryInfo info =
                (ApacheBinaryInfo)binaries.get(i);

            List found = getServerList(info.root, info.version, info);
            if (found != null) {
                servers.addAll(found);
            }
        }

        return servers;        
    }
    /**
     * The List returned from this method will either be null
     * (if no servers were found), or it will contain a single
     * AIServerValue (if a server was found).  Currently the 
     * DotOrgDetector does not support detecting multiple instances 
     * of Apache in a single directory.
     */
    public List getServerResources(ConfigResponse platformConfig, String path) throws PluginException {
        String version = getTypeInfo().getVersion();
        ApacheBinaryInfo binary = ApacheBinaryInfo.getInfo(path);
        String fullVersion = binary.version;

        if (fullVersion == null) {
            log.debug("Apache version " + version +
                      " not found in binary: " + path);
            return null;
        }

        //strip "bin/httpd"
        String installpath = getParentDir(path, 2);

        return getServerList(installpath, fullVersion, binary);
    }

    protected List discoverServices(ConfigResponse serverConfig)
        throws PluginException {

        ApacheSNMP snmp = new ApacheSNMP();

        List servers;

        try {
            servers = snmp.getServers(serverConfig);
        } catch (SNMPException e) {
            throw new PluginException(e.getMessage(), e);
        }

        //inherit server hostname if something other than the default
        String serverHostname =
            serverConfig.getValue(URLMetric.PROP_HOSTNAME);
        String hostname =
            "localhost".equals(serverHostname) ?
            this.defaultIp : serverHostname;

        String serverProtocol =
            serverConfig.getValue(URLMetric.PROP_PROTOCOL);
        String protocol =
            "ping".equals(serverProtocol) ?
            serverProtocol : null;

        List services = new ArrayList();
        Map serviceNames = new HashMap();

        for (int i=0; i<servers.size(); i++) {
            ApacheSNMP.Server server =
                (ApacheSNMP.Server)servers.get(i);

            String serviceName = server.toString();

            //XXX should not get any duplicates.
            //but if we do, just fold them, no point in having duplicate
            //services.
            if (serviceNames.get(serviceName) == Boolean.TRUE) {
                log.debug("Discovered duplicate service: " + serviceName);
                continue;
            }
            serviceNames.put(serviceName, Boolean.TRUE);
            log.debug("Discovered service: " + serviceName);

            ServiceResource service = new ServiceResource();
            service.setType(this, VHOST_NAME);
            service.setServiceName(VHOST_NAME + " " + serviceName);

            ConfigResponse config = new ConfigResponse();

            config.setValue(URLMetric.PROP_PORT, server.port);
            //XXX need a better way for configs w/ Listen address:port
            config.setValue(URLMetric.PROP_HOSTNAME, hostname);
            config.setValue(PROP_SERVER_NAME, server.name);
            String proto = protocol == null ?
                guessProtocol(server.port) :
                protocol;
            //XXX snmp does not tell us the protocol
            config.setValue(URLMetric.PROP_PROTOCOL, proto);
            service.setProductConfig(config);
            service.setMeasurementConfig();

            services.add(service);
        }

        if (servers.size() > 0) {
            ApacheSNMP.Server server =
                (ApacheSNMP.Server)servers.get(0);
            ConfigResponse cprops = new ConfigResponse();
            cprops.setValue("version", server.version);
            cprops.setValue("serverTokens", server.description);
            cprops.setValue("serverAdmin", server.admin);
            setCustomProperties(cprops);
        }

        return services;
    }

    private String guessProtocol(String port) {
        int nPort = Integer.parseInt(port);
        if (this.httpRange.hasValue(nPort)) {
            return "http";
        }
        else if (this.httpsRange.hasValue(nPort)) {
            return "https";
        }
        else {
            return URLMetric.guessProtocol(port);
        }
    }

    private static class PortRange {
        int start, end;

        public PortRange(String range) {
            parse(range);
        }

        public void parse(String range) {
            if (range == null) {
                this.start = this.end = 0;
                return;
            }

            int ix = range.indexOf("..");
            if (ix == -1) {
                throw new IllegalArgumentException("Invalid range: " + range);
            }

            this.start = Integer.parseInt(range.substring(0, ix));
            this.end   = Integer.parseInt(range.substring(ix+2,
                                                          range.length()));
        }

        public boolean hasValue(int value) {
            return
                (value >= this.start) &&
                (value <= this.end);
        }

        public String toString() {
            return this.start + ".." + this.end;
        }
    }

    public static void main(String[] args) throws Exception {
        String[] versions = {"1.3", "2.0"};

        for (int i=0; i<versions.length; i++) {
            List servers = getServerProcessList(versions[i],
                                                getPtqlQueries());

            for (int j=0; j<servers.size(); j++) {
                System.out.println(versions[i] + " " + servers.get(j));
            }
        }
    }
}
