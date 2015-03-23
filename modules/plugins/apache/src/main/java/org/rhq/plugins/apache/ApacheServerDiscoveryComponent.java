/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.plugins.apache;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.rhq.augeas.util.Glob;
import org.rhq.augeas.util.GlobFilter;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.ResourceUpgradeReport;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ManualAddFacet;
import org.rhq.core.pluginapi.inventory.ProcessScanResult;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.pluginapi.upgrade.ResourceUpgradeContext;
import org.rhq.core.pluginapi.upgrade.ResourceUpgradeFacet;
import org.rhq.core.pluginapi.util.FileUtils;
import org.rhq.core.system.ProcessInfo;
import org.rhq.plugins.apache.parser.ApacheConfigReader;
import org.rhq.plugins.apache.parser.ApacheDirective;
import org.rhq.plugins.apache.parser.ApacheDirectiveTree;
import org.rhq.plugins.apache.parser.ApacheParser;
import org.rhq.plugins.apache.parser.ApacheParserImpl;
import org.rhq.plugins.apache.util.ApacheBinaryInfo;
import org.rhq.plugins.apache.util.AugeasNodeValueUtil;
import org.rhq.plugins.apache.util.HttpdAddressUtility;
import org.rhq.plugins.apache.util.HttpdAddressUtility.Address;
import org.rhq.plugins.apache.util.OsProcessUtility;
import org.rhq.plugins.apache.util.RuntimeApacheConfiguration;
import org.rhq.plugins.platform.PlatformComponent;
import org.rhq.rhqtransform.impl.PluginDescriptorBasedAugeasConfiguration;

/**
 * The discovery component for Apache 2.x servers.
 *
 * @author Ian Springer
 * @author Lukas Krejci
 */
public class ApacheServerDiscoveryComponent implements ResourceDiscoveryComponent<PlatformComponent>,
    ManualAddFacet<PlatformComponent>, ResourceUpgradeFacet<PlatformComponent> {
    private static final String PRODUCT_DESCRIPTION = "Apache Web Server";

    private static final Log log = LogFactory.getLog(ApacheServerDiscoveryComponent.class);

    private static class DiscoveryFailureException extends Exception {
        private static final long serialVersionUID = 1L;

        public DiscoveryFailureException(String message) {
            super(message);
        }

        public DiscoveryFailureException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static final Map<String, String> MODULE_SOURCE_FILE_TO_MODULE_NAME_20;
    public static final Map<String, String> MODULE_SOURCE_FILE_TO_MODULE_NAME_13;

    static {
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20 = new LinkedHashMap<String, String>();

        //these are extracted from http://httpd.apache.org/docs/current/mod/
        //and linked pages
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("beos.c", "mpm_beos_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("event.c", "mpm_event_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mpm_netware.c", "mpm_netware_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mpmt_os2.c", "mpm_mpmt_os2_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("prefork.c", "mpm_prefork_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mpm_winnt.c", "mpm_winnt_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("worker.c", "mpm_worker_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_actions.c", "actions_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_alias.c", "alias_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_asis.c", "asis_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_auth_basic.c", "auth_basic_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_auth_digest.c", "auth_digest_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_authn_alias.c", "authn_alias_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_authn_anon.c", "authn_anon_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_authn_dbd.c", "authn_dbd_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_authn_dbm.c", "authn_dbm_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_authn_default.c", "authn_default_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_authn_file.c", "authn_file_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_authnz_ldap.c", "authnz_ldap_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_authz_dbm.c", "authz_dbm_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_authz_default.c", "authz_default_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_authz_groupfile.c", "authz_groupfile_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_authz_host.c", "authz_host_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_authz_owner.c", "authz_owner_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_authz_user.c", "authz_user_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_autoindex.c", "autoindex_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_cache.c", "cache_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_cern_meta.c", "cern_meta_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_cgi.c", "cgi_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_cgid.c", "cgid_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_charset_lite.c", "charset_lite_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_dav.c", "dav_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_dav_fs.c", "dav_fs_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_dav_lock.c", "dav_lock_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_dbd.c", "dbd_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_deflate.c", "deflate_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_dir.c", "dir_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_disk_cache.c", "disk_cache_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_dumpio.c", "dumpio_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_echo.c", "echo_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_env.c", "env_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_example.c", "example_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_expires.c", "expires_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_ext_filter.c", "ext_filter_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_file_cache.c", "file_cache_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_filter.c", "filter_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_headers.c", "headers_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_ident.c", "ident_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_imagemap.c", "imagemap_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_include.c", "include_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_info.c", "info_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_isapi.c", "isapi_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("util_ldap.c", "ldap_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_log_config.c", "log_config_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_log_forensic.c", "log_forensic_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_logio.c", "logio_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_mem_cache.c", "mem_cache_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_mime.c", "mime_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_mime_magic.c", "mime_magic_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_negotiation.c", "negotiation_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_nw_ssl.c", "nwssl_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_proxy.c", "proxy_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_proxy_ajp.c", "proxy_ajp_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_proxy_balancer.c", "proxy_balancer_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_proxy_connect.c", "proxy_connect_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_proxy_ftp.c", "proxy_ftp_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_proxy_http.c", "proxy_http_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_proxy_scgi.c", "proxy_scgi_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_reqtimeout.c", "reqtimeout_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_rewrite.c", "rewrite_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_setenvif.c", "setenvif_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_so.c", "so_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_speling.c", "speling_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_ssl.c", "ssl_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_status.c", "status_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_substitute.c", "substitute_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_suexec.c", "suexec_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_unique_id.c", "unique_id_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_userdir.c", "userdir_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_usertrack.c", "usertrack_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_version.c", "version_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_vhost_alias.c", "vhost_alias_module");

        //some hand picked modules
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_jk.c", "jk_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod-snmpcommon.c", "snmpcommon_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod-snmpagt.c", "snmpagt_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("covalent-snmp-v20.c", "snmp_agt_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_20.put("mod_bmx.c", "bmx_module");

        //this list is for apache 1.3
        MODULE_SOURCE_FILE_TO_MODULE_NAME_13 = new LinkedHashMap<String, String>();
        MODULE_SOURCE_FILE_TO_MODULE_NAME_13.put("mod_access.c", "access_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_13.put("mod_actions.c", "action_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_13.put("mod_alias.c", "alias_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_13.put("mod_asis.c", "asis_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_13.put("mod_auth.c", "auth_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_13.put("mod_auth_anon.c", "anon_auth_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_13.put("mod_auth_db.c", "db_auth_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_13.put("mod_auth_dbm.c", "dbm_auth_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_13.put("mod_auth_digest.c", "digest_auth_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_13.put("mod_autoindex.c", "autoindex_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_13.put("mod_cern_meta.c", "cern_meta_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_13.put("mod_cgi.c", "cgi_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_13.put("mod_digest.c", "digest_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_13.put("mod_dir.c", "dir_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_13.put("mod_env.c", "env_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_13.put("mod_example.c", "example_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_13.put("mod_expires.c", "expires_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_13.put("mod_headers.c", "headers_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_13.put("mod_imap.c", "imap_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_13.put("mod_include.c", "includes_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_13.put("mod_info.c", "info_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_13.put("mod_isapi.c", "isapi_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_13.put("mod_log_agent.c", "agent_log_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_13.put("mod_log_config.c", "config_log_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_13.put("mod_log_forensic.c", "log_forensic_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_13.put("mod_log_referer.c", "referer_log_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_13.put("mod_mime.c", "mime_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_13.put("mod_mime_magic.c", "mime_magic_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_13.put("mod_mmap_static.c", "mmap_static_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_13.put("mod_negotiation.c", "negotiation_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_13.put("mod_proxy.c", "proxy_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_13.put("mod_rewrite.c", "rewrite_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_13.put("mod_setenvif.c", "setenvif_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_13.put("mod_so.c", "so_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_13.put("mod_speling.c", "speling_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_13.put("mod_status.c", "status_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_13.put("mod_unique_id.c", "unique_id_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_13.put("mod_userdir.c", "userdir_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_13.put("mod_usertrack.c", "usertrack_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_13.put("mod_vhost_alias.c", "vhost_alias_module");

        //and some hand-picks
        MODULE_SOURCE_FILE_TO_MODULE_NAME_13.put("mod_jk.c", "jk_module");
        MODULE_SOURCE_FILE_TO_MODULE_NAME_13.put("covalent-snmp-v13.c", "snmp_agt_module");
    }

    public Set<DiscoveredResourceDetails>
        discoverResources(ResourceDiscoveryContext<PlatformComponent> discoveryContext) throws Exception {
        Set<DiscoveredResourceDetails> discoveredResources = new HashSet<DiscoveredResourceDetails>();

        // Process any PC-discovered OS processes...
        List<ProcessScanResult> processes = discoveryContext.getAutoDiscoveredProcesses();
        for (ProcessScanResult process : processes) {
            try {
                DiscoveredResourceDetails apache = discoverSingleProcess(discoveryContext, process);
                if (apache != null) {
                    discoveredResources.add(apache);
                }
            } catch (DiscoveryFailureException e) {
                log.warn("Discovery of Apache process [" + process.getProcessInfo() + "] failed: " + e.getMessage());
            } catch (Exception e) {
                log.error("Discovery of Apache process [" + process.getProcessInfo() + "] failed with an exception.", e);
            }
        }

        return discoveredResources;
    }

    /**
     * Performs discovery on the single process scan result.
     * 
     * @param discoveryContext the discovery context
     * @param process the process discovered by the scan
     * @return resource details
     * @throws DiscoveryFailureException if the discovery failed due to inability to detect necessary data from
     * the process info.
     * @throws Exception other unhandled exception
     */
    private DiscoveredResourceDetails discoverSingleProcess(
        ResourceDiscoveryContext<PlatformComponent> discoveryContext, ProcessScanResult process)
        throws DiscoveryFailureException, Exception {

        if (isWindowsServiceRootInstance(process)) {
            return null;
        }

        File executablePath = getExecutableAbsolutePath(process);
        log.debug("Apache executable path: " + executablePath);

        ApacheBinaryInfo binaryInfo;
        try {
            binaryInfo = ApacheBinaryInfo.getInfo(executablePath.getPath(), discoveryContext.getSystemInformation());
        } catch (Exception e) {
            throw new DiscoveryFailureException("'" + executablePath + "' is not a valid Apache executable (" + e
                + ").");
        }

        if (!isSupportedVersion(binaryInfo.getVersion())) {
            throw new DiscoveryFailureException("Apache " + binaryInfo.getVersion() + " is not supported.");
        }

        String serverRoot = getServerRoot(binaryInfo, process.getProcessInfo());
        if (serverRoot == null) {
            throw new DiscoveryFailureException("Unable to determine server root.");
        }

        File serverConfigFile = getServerConfigFile(binaryInfo, process.getProcessInfo(), serverRoot);
        if (serverConfigFile == null) {
            throw new DiscoveryFailureException("Unable to determine server config file.");
        }

        Configuration pluginConfig = discoveryContext.getDefaultPluginConfiguration();

        PropertySimple executablePathProp =
            new PropertySimple(ApacheServerComponent.PLUGIN_CONFIG_PROP_EXECUTABLE_PATH, executablePath);
        pluginConfig.put(executablePathProp);

        PropertySimple serverRootProp =
            new PropertySimple(ApacheServerComponent.PLUGIN_CONFIG_PROP_SERVER_ROOT, serverRoot);
        pluginConfig.put(serverRootProp);

        PropertySimple configFile =
            new PropertySimple(ApacheServerComponent.PLUGIN_CONFIG_PROP_HTTPD_CONF, serverConfigFile);
        pluginConfig.put(configFile);

        PropertySimple inclusionGlobs =
            new PropertySimple(PluginDescriptorBasedAugeasConfiguration.INCLUDE_GLOBS_PROP, serverConfigFile);
        pluginConfig.put(inclusionGlobs);

        pluginConfig.put(new PropertyList(ApacheServerComponent.PLUGIN_CONFIG_CUSTOM_MODULE_NAMES));

        ApacheDirectiveTree serverConfig =
            parseRuntimeConfiguration(serverConfigFile.getAbsolutePath(), process.getProcessInfo(), binaryInfo);

        String serverUrl = null;
        String vhostsGlobInclude = null;

        //now check if the httpd.conf doesn't redefine the ServerRoot
        List<ApacheDirective> serverRoots = serverConfig.search("/ServerRoot");
        if (!serverRoots.isEmpty()) {
            serverRoot = AugeasNodeValueUtil.unescape(serverRoots.get(0).getValuesAsString());
            serverRootProp.setValue(serverRoot);
        }

        serverUrl = getUrl(serverConfig, binaryInfo.getVersion());
        vhostsGlobInclude = scanForGlobInclude(serverConfig);

        if (serverUrl != null) {
            Property urlProp = new PropertySimple(ApacheServerComponent.PLUGIN_CONFIG_PROP_URL, serverUrl);
            pluginConfig.put(urlProp);
        }

        if (vhostsGlobInclude != null) {
            pluginConfig.put(new PropertySimple(ApacheServerComponent.PLUGIN_CONFIG_PROP_VHOST_FILES_MASK,
                vhostsGlobInclude));
        } else {
            if (serverConfigFile.exists())
                pluginConfig.put(new PropertySimple(ApacheServerComponent.PLUGIN_CONFIG_PROP_VHOST_FILES_MASK,
                    serverConfigFile.getParent() + File.separator + "*"));
        }

        List<InetSocketAddress> snmpAddresses = findSNMPAddresses(serverConfig, new File(serverRoot));
        if (snmpAddresses != null && snmpAddresses.size() > 0) {
            InetSocketAddress addr = snmpAddresses.get(0);
            int port = addr.getPort();

            InetAddress host = ((addr.getAddress() == null) || addr.getAddress().isAnyLocalAddress()) ?
                    InetAddress.getLocalHost() : addr.getAddress();

            pluginConfig.put(new PropertySimple(ApacheServerComponent.PLUGIN_CONFIG_PROP_SNMP_AGENT_HOST, host
                .getHostAddress()));
            pluginConfig.put(new PropertySimple(ApacheServerComponent.PLUGIN_CONFIG_PROP_SNMP_AGENT_PORT, port));
        } else {
            /* try mod_bmx VirtualHost and Location */
            String bmxURL = findBMXURL(serverConfig);
            if (bmxURL != null) {
                pluginConfig.put(new PropertySimple(ApacheServerComponent.PLUGIN_CONFIG_PROP_BMX_URL, bmxURL));
            }
        }

        return createResourceDetails(discoveryContext, pluginConfig, process.getProcessInfo(), binaryInfo);
    }

    public ResourceUpgradeReport upgrade(ResourceUpgradeContext<PlatformComponent> context) {
        String inventoriedResourceKey = context.getResourceKey();

        //check if the inventoried resource has the old format of the resource key.
        //the old format was "server-root", while the new format
        //is "server-root||httpd-conf". Checking for "||" in the resource key is therefore
        //enough.
        if (inventoriedResourceKey.contains("||")) {
            return null;
        }

        //all the information we need for the new style resource key is 
        //actually present in the plugin configuration of the existing resource
        //already, so let's just generate the new style resource key from it.        
        String resourceKey = formatResourceKey(context.getPluginConfiguration());

        ResourceUpgradeReport rep = new ResourceUpgradeReport();
        rep.setNewResourceKey(resourceKey);

        return rep;
    }

    public DiscoveredResourceDetails discoverResource(Configuration pluginConfig,
        ResourceDiscoveryContext<PlatformComponent> discoveryContext) throws InvalidPluginConfigurationException {
        validateServerRootAndServerConfigFile(pluginConfig);

        String executablePath =
            pluginConfig.getSimpleValue(ApacheServerComponent.PLUGIN_CONFIG_PROP_EXECUTABLE_PATH,
                ApacheServerComponent.DEFAULT_EXECUTABLE_PATH);
        String absoluteExecutablePath =
            ApacheServerComponent.resolvePathRelativeToServerRoot(pluginConfig, executablePath).getPath();
        ApacheBinaryInfo binaryInfo;
        try {
            binaryInfo = ApacheBinaryInfo.getInfo(absoluteExecutablePath, discoveryContext.getSystemInformation());
        } catch (Exception e) {
            throw new InvalidPluginConfigurationException("'" + absoluteExecutablePath
                + "' is not a valid Apache executable (" + e + "). Please make sure the '"
                + ApacheServerComponent.PLUGIN_CONFIG_PROP_EXECUTABLE_PATH + "' connection property is set correctly.");
        }

        if (!isSupportedVersion(binaryInfo.getVersion())) {
            throw new InvalidPluginConfigurationException("Version of Apache executable (" + binaryInfo.getVersion()
                + ") is not a supported version; supported versions are 1.3.x and 2.x.");
        }

        ProcessInfo processInfo = null;
        try {
            DiscoveredResourceDetails resourceDetails =
                createResourceDetails(discoveryContext, pluginConfig, processInfo, binaryInfo);
            return resourceDetails;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create resource details during manual add.");
        }
    }

    private boolean isSupportedVersion(String version) {
        // TODO: Compare against a version range defined in the plugin descriptor.
        return (version != null) && (version.startsWith("1.3") || version.startsWith("2."));
    }

    private DiscoveredResourceDetails createResourceDetails(
        ResourceDiscoveryContext<PlatformComponent> discoveryContext, Configuration pluginConfig,
        ProcessInfo processInfo, ApacheBinaryInfo binaryInfo) throws Exception {
        String httpdConf = pluginConfig.getSimple(ApacheServerComponent.PLUGIN_CONFIG_PROP_HTTPD_CONF).getStringValue();
        String version = binaryInfo.getVersion();
        String serverUrl = pluginConfig.getSimpleValue(ApacheServerComponent.PLUGIN_CONFIG_PROP_URL, null);
        //use the server url if we could detect it, otherwise use something unique
        String name;
        if (serverUrl == null) {
            name = httpdConf;
        } else {
            URI uri = new URI(serverUrl);
            name = uri.getHost() + ":" + uri.getPort();
        }

        String key = formatResourceKey(pluginConfig);

        DiscoveredResourceDetails resourceDetails =
            new DiscoveredResourceDetails(discoveryContext.getResourceType(), key, name, version, PRODUCT_DESCRIPTION,
                pluginConfig, processInfo);
        log.debug("Apache Server resource details created: " + resourceDetails);
        return resourceDetails;
    }

    /**
     * Return the root URL as determined from the Httpd configuration loaded by Augeas.
     * he URL's protocol is assumed to be "http" and its path is assumed to be "/".
     *  
     * @return
     *
     * @throws Exception
     */
    private static String getUrl(ApacheDirectiveTree serverConfig, String version) throws Exception {
        Address addr = HttpdAddressUtility.get(version).getMainServerSampleAddress(serverConfig, null, 0);
        return addr == null ? null : addr.toString();
    }

    @Nullable
    private String getServerRoot(@NotNull ApacheBinaryInfo binaryInfo, @NotNull ProcessInfo processInfo) {
        // First see if -d was specified on the httpd command line.
        String[] cmdLine = processInfo.getCommandLine();
        String root = getCommandLineOption(cmdLine, "-d");

        // If not, extract the path from the httpd binary.
        if (root == null) {
            root = binaryInfo.getRoot();
        }

        if (root == null) {
            // We have failed to determine the server root  :(
            return null;
        }

        // If the path is relative, convert it to an absolute path, resolving it relative to the cwd of the httpd process.
        File rootFile = new File(root);
        if (!rootFile.isAbsolute()) {
            String currentWorkingDir;
            try {
                currentWorkingDir = processInfo.getCurrentWorkingDirectory();
            } catch (Exception e) {
                log.error("Unable to determine current working directory of Apache process [" + processInfo
                    + "], which is needed to determine the server root of the Apache instance.", e);
                return null;
            }
            if (currentWorkingDir == null) {
                log.error("Unable to determine current working directory of Apache process [" + processInfo
                    + "], which is needed to determine the server root of the Apache instance.");
                return null;
            } else {
                rootFile = new File(currentWorkingDir, root);
                root = rootFile.getPath();
            }
        }

        // And finally canonicalize the path, but using our own getCanonicalPath() method, which preserves symlinks.
        root = FileUtils.getCanonicalPath(root);

        return root;
    }

    @Nullable
    private File getServerConfigFile(ApacheBinaryInfo binaryInfo, ProcessInfo processInfo, String serverRoot) {
        // First see if -f was specified on the httpd command line.
        String[] cmdLine = processInfo.getCommandLine();
        String serverConfigFile = getCommandLineOption(cmdLine, "-f");

        // If not, extract the path from the httpd binary.
        if (serverConfigFile == null) {
            serverConfigFile = binaryInfo.getCtl();
        }

        if (serverConfigFile == null) {
            // We have failed to determine the config file path  :(
            return null;
        }

        // If the path is relative, convert it to an absolute path, resolving it relative to the server root dir.
        File file = new File(serverConfigFile);
        if (!file.isAbsolute()) {
            file = new File(serverRoot, serverConfigFile);
            serverConfigFile = file.getPath();
        }

        // And finally canonicalize the path, but using our own getCanonicalPath() method, which preserves symlinks.
        serverConfigFile = FileUtils.getCanonicalPath(serverConfigFile);

        return new File(serverConfigFile);
    }

    private static String getCommandLineOption(String[] cmdLine, String option) {
        String root = null;
        for (int i = 1; i < cmdLine.length; i++) {
            String arg = cmdLine[i];
            if (arg.startsWith(option)) {
                root = arg.substring(2, arg.length());
                if (root.length() == 0) {
                    root = cmdLine[i + 1];
                }
                break;
            }
        }
        return root;
    }

    private static String getExecutableName(ProcessScanResult processScanResult) {
        String query = processScanResult.getProcessScan().getQuery().toLowerCase();
        String executableName;
        if (query.contains("apache.exe")) {
            executableName = "apache.exe";
        } else if (query.contains("httpd.exe")) {
            executableName = "httpd.exe";
        } else if (query.contains("apache2")) {
            executableName = "apache2";
        } else if (query.contains("httpd")) {
            executableName = "httpd";
        } else {
            executableName = null;
        }
        return executableName;
    }

    private static File getExecutableAbsolutePath(ProcessScanResult process) throws DiscoveryFailureException {
        //String executablePath = process.getProcessInfo().getName();
        String executableName = getExecutableName(process);
        File executablePath = OsProcessUtility.getProcExe(process.getProcessInfo().getPid(), executableName);
        if (executablePath == null) {
            throw new DiscoveryFailureException("Executable path could not be determined.");
        }
        if (!executablePath.isAbsolute()) {
            //try to figure out the full path... this might fail due to lack of privs
            //if the agent is running as a different user than the httpd process
            String errorMessage =
                "Executable path ("
                    + executablePath
                    + ") is not absolute. "
                    + "Please restart Apache specifying an absolute path for the executable or "
                    + "make sure that the user running the RHQ agent is able to access the commandline parameters of the "
                    + executableName + " process.";
            Throwable cause = null;
            boolean success = false;

            //the OsProcessUtility.getProcExe does an excelent job at figuring the full path and I never saw it fail
            //when the agent process has enough privs to get at the info at all. Nevertheless, let's be paranoid
            //and try yet another method..
            try {
                String cwd = process.getProcessInfo().getCurrentWorkingDirectory();
                if (cwd != null) {
                    executablePath = new File(cwd, executablePath.getPath());

                    success = executablePath.isAbsolute() && executablePath.isFile();
                }
            } catch (Exception e) {
                cause = e;
            }

            if (!success) {
                throw new DiscoveryFailureException(errorMessage, cause);
            }
        }

        return executablePath;
    }

    private static void validateServerRootAndServerConfigFile(Configuration pluginConfig) {
        String serverRoot =
            pluginConfig.getSimple(ApacheServerComponent.PLUGIN_CONFIG_PROP_SERVER_ROOT).getStringValue();
        File serverRootFile;
        try {
            serverRootFile = new File(serverRoot).getCanonicalFile(); // this will resolve symlinks
        } catch (IOException e) {
            serverRootFile = null;
        }
        if (serverRootFile == null || !serverRootFile.isDirectory()) {
            throw new InvalidPluginConfigurationException("'" + serverRoot
                + "' does not exist or is not a directory. Please make sure the '"
                + ApacheServerComponent.PLUGIN_CONFIG_PROP_SERVER_ROOT + "' connection property is set correctly.");
        }
        String httpdConf = pluginConfig.getSimple(ApacheServerComponent.PLUGIN_CONFIG_PROP_HTTPD_CONF).getStringValue();
        File httpdConfFile;
        try {
            httpdConfFile = new File(httpdConf).getCanonicalFile(); // this will resolve symlinks
        } catch (IOException e) {
            httpdConfFile = null;
        }
        if (httpdConfFile == null || !httpdConfFile.isFile()) {
            throw new InvalidPluginConfigurationException("'" + httpdConf
                + "' does not exist or is not a regular file. Please make sure the '"
                + ApacheServerComponent.PLUGIN_CONFIG_PROP_HTTPD_CONF + "' connection property is set correctly.");
        }
    }

    public static ApacheDirectiveTree parseFullConfiguration(String path, String serverRoot) {
        ApacheDirectiveTree tree = new ApacheDirectiveTree();
        ApacheParser parser = new ApacheParserImpl(tree, serverRoot, null);
        ApacheConfigReader.buildTree(path, parser);
        return tree;
    }

    public static ApacheDirectiveTree parseRuntimeConfiguration(String path, ProcessInfo processInfo,
        ApacheBinaryInfo binaryInfo) {

        String httpdVersion = binaryInfo.getVersion();
        Map<String, String> defaultModuleNames = getDefaultModuleNames(httpdVersion);

        return parseRuntimeConfiguration(path, processInfo, binaryInfo, defaultModuleNames, true);
    }

    public static ApacheDirectiveTree parseRuntimeConfiguration(String path, ProcessInfo processInfo,
        ApacheBinaryInfo binaryInfo, Map<String, String> moduleNames, boolean suppressUnknownModuleWarnings) {
        String defaultServerRoot = binaryInfo.getRoot();

        RuntimeApacheConfiguration.NodeInspector insp =
            RuntimeApacheConfiguration.getNodeInspector(processInfo, binaryInfo, moduleNames,
                suppressUnknownModuleWarnings);

        ApacheDirectiveTree tree = new ApacheDirectiveTree();
        ApacheParser parser = new ApacheParserImpl(tree, defaultServerRoot, insp);
        ApacheConfigReader.buildTree(path, parser);
        return tree;
    }

    public static String scanForGlobInclude(ApacheDirectiveTree tree) {
        try {
            List<ApacheDirective> includes = tree.search("/Include");
            for (ApacheDirective n : includes) {
                String include = n.getValuesAsString();
                if (Glob.isWildcard(include)) {
                    //we only take the '*.something' into account here
                    //so that we have a useful mask to base the file names on.

                    //the only special glob character allowed is *.
                    for (char specialChar : GlobFilter.WILDCARD_CHARS) {
                        if (specialChar == '*') {
                            if (include.indexOf(specialChar) != include.lastIndexOf(specialChar)) {
                                //more than 1 star... that's too much
                                break;
                            }
                            //we found what we're looking for...
                            return include;
                        }
                        if (include.indexOf(specialChar) >= 0) {
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Failed to detect glob includes in httpd.conf.", e);
        }
        return null;
    }

    public static String formatResourceKey(Configuration pluginConfiguration) {
        String serverRoot =
            pluginConfiguration.getSimple(ApacheServerComponent.PLUGIN_CONFIG_PROP_SERVER_ROOT).getStringValue();
        String httpdConf =
            pluginConfiguration.getSimple(ApacheServerComponent.PLUGIN_CONFIG_PROP_HTTPD_CONF).getStringValue();

        return formatResourceKey(serverRoot, httpdConf);
    }

    public static String formatResourceKey(String serverRoot, String httpdConf) {
        serverRoot = FileUtils.getCanonicalPath(serverRoot);

        //we could have inherited the configuration from
        //RHQ 1.x times, when the httpdConf was relative.
        File httpdConfF = new File(httpdConf);
        if (!httpdConfF.isAbsolute()) {
            httpdConfF = new File(serverRoot, httpdConf);
            httpdConf = httpdConfF.getAbsolutePath();
        }
        httpdConf = FileUtils.getCanonicalPath(httpdConf);

        return serverRoot + "||" + httpdConf;
    }

    private static List<InetSocketAddress> findSNMPAddresses(ApacheDirectiveTree tree, File serverRoot) {
        List<InetSocketAddress> ret = new ArrayList<InetSocketAddress>();

        List<ApacheDirective> confs = tree.search("/SNMPConf");

        if (confs.size() == 0) {
            log.info("SNMPConf directive not found. Skipping SNMP configuration.");
            return ret;
        }

        String confDirName = confs.get(0).getValuesAsString();
        if (confDirName == null || confDirName.isEmpty()) {
            log.warn("The SNMPConf directive seems to not have a value. Skipping SNMP configuration.");
            return ret;
        }

        File confDir = new File(confDirName);

        if (!confDir.isAbsolute()) {
            confDir = new File(serverRoot, confDirName);
        }

        File snmpdConf = new File(confDir, "snmpd.conf");

        if (!snmpdConf.exists()) {
            log.warn("Could not find a snmpd.conf file under the configured directory '" + confDirName
                + "'. Skipping SNMP configuration.");
            return ret;
        }

        try {
            String agentAddressLine = findSNMPAgentAddressConfigLine(snmpdConf);

            if (agentAddressLine == null) {
                log.warn("Could not find the 'agentaddress' property in the snmpd.conf. Skipping SNMP configuration.");
                return ret;
            }

            int specStartIdx = agentAddressLine.indexOf("agentaddress") + "agentaddress".length() + 1;

            while (Character.isWhitespace(agentAddressLine.charAt(specStartIdx)))
                specStartIdx++;

            String spec = agentAddressLine.substring(specStartIdx);

            String[] addrs = spec.split(",");

            try {
                for (String addr : addrs) {
                    if (addr.startsWith("udp") || addr.startsWith("tcp")) {
                        //this contains the transport spec - either "udp:" or "tcp:"
                        addr = addr.substring(4);
                    }

                    int atIdx = addr.indexOf('@');
                    String port = addr;
                    String host = null;
                    if (atIdx > 0) {
                        host = addr.substring(atIdx + 1);
                        port = addr.substring(0, atIdx);
                    }

                    InetSocketAddress address;
                    if (host != null) {
                        address = new InetSocketAddress(host, Integer.parseInt(port));
                    } else {
                        address = new InetSocketAddress(Integer.parseInt(port));
                    }

                    ret.add(address);
                }
            } catch (Exception e) {
                log.warn("Failed to parse the SNMP 'agentaddress' configuration property: " + agentAddressLine, e);
            }
        } catch (IOException e) {
            log.warn("Failed to read in the configured snmpd.conf file: " + snmpdConf.getAbsolutePath(), e);
        }

        return ret;
    }

    /* Try to find the VirtualHost containing something like:
     *   <Location /bmx>
     *      SetHandler bmx-handler
     *   </Location>
     */
    private static String findBMXURL(ApacheDirectiveTree tree) {
        List<ApacheDirective> virtualhosts = tree.getRootNode().getChildByName("<VirtualHost");
        if (!virtualhosts.isEmpty()) {
            log.debug("findBMXURL: Looking in VirtualHosts");
            for (ApacheDirective virtualhost : virtualhosts) {
                List<ApacheDirective> locations = virtualhost.getChildByName("<Location");
                if (!locations.isEmpty()) {
                    for (ApacheDirective location : locations) {
                        List<ApacheDirective> handlers = location.getChildByName("SetHandler");
                        if (!handlers.isEmpty()) {
                            for (ApacheDirective handler : handlers) {
                                for (String val : handler.getValues()) {
                                    if ("bmx-handler".equalsIgnoreCase(val)) {
                                        /* We have found it */
                                        String servername = null;
                                        String port = null;
                                        port = virtualhost.getValues().get(0);
                                        if (port.startsWith("*:"))
                                            port = port.substring(2);
                                        else {
                                            int index = port.lastIndexOf(':');
                                            servername = port.substring(0, index);
                                            port = port.substring(index+1);
                                        }
                                        if (!virtualhost.getChildByName("ServerName").isEmpty()) {
                                            servername = virtualhost.getChildByName("ServerName").get(0).getValues().get(0);
                                            int index = servername.lastIndexOf(':');
                                            if (index >0) {
                                                // name:port.
                                                servername = servername.substring(0, index);
                                            }
                                        }
                                        if (servername == null)
                                            servername = "localhost";
                                        return "http://" + servername + ":" + port + location.getValues().get(0);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        /* the location isn't in a VirtualHost */
        List<ApacheDirective> locations = tree.getRootNode().getChildByName("<Location");
        if (!locations.isEmpty()) {
            log.debug("findBMXURL: Looking outside VirtualHosts");
            for (ApacheDirective location : locations) {
                List<ApacheDirective> handlers = location.getChildByName("SetHandler");
                if (!handlers.isEmpty()) {
                    for (ApacheDirective handler : handlers) {
                        for (String val : handler.getValues()) {
                            if ("bmx-handler".equalsIgnoreCase(val)) {
                                /* We have found it */
                                String servername = null;
                                String port = null;
                                List<ApacheDirective> ports = tree.getRootNode().getChildByName("Listen");
                                if (ports.isEmpty()) {
                                   log.error("findBMXURL: Can't find Listen directive");
                                   return null;
                                }
                                port = ports.get(0).getValues().get(0); // Use the first one.
                                int index = port.lastIndexOf(':');
                                if (index >0) {
                                    // Use IP:port.
                                    servername = port.substring(0, index);
                                    port = port.substring(index+1);
                                }
                                if (!tree.getRootNode().getChildByName("ServerName").isEmpty()) {
                                    servername = tree.getRootNode().getChildByName("ServerName").get(0).getValues().get(0);
                                    index = servername.lastIndexOf(':');
                                    if (index >0) {
                                        // name:port.
                                        servername = servername.substring(0, index);
                                    }
                                }
                                if (servername == null)
                                    servername = "localhost";
                                return "http://" + servername + ":" + port + location.getValues().get(0);
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    private static String findSNMPAgentAddressConfigLine(File snmpdConf) throws IOException {
        BufferedReader rdr = new BufferedReader(new FileReader(snmpdConf));

        try {
            Pattern search = Pattern.compile("^\\s*agentaddress.*");
            String line;

            while ((line = rdr.readLine()) != null) {
                if (search.matcher(line).matches()) {
                    return line;
                }
            }

            return null;
        } finally {
            rdr.close();
        }
    }

    public static Map<String, String> getDefaultModuleNames(String version) {
        switch (HttpdAddressUtility.get(version)) {
        case APACHE_1_3:
            return MODULE_SOURCE_FILE_TO_MODULE_NAME_13;
        case APACHE_2_x:
            return MODULE_SOURCE_FILE_TO_MODULE_NAME_20;
        default:
            throw new IllegalStateException("Unknown HttpdAddressUtility instance.");
        }
    }

    /**
     * We need this because of PIQL limitations.
     * 
     * On *nixes we need to match all "root" httpd processes (i.e. with ppid of 1) but on windows
     * we need to match all child httpd processes (i.e. processes that are spawned from the "root"
     * httpd process).
     * 
     * The *nix requirement causes the root httpd process be matched on Windows
     * as well, which is undesirable but there is no way of telling PIQL
     * "don't do this on windows" or "match only processes that DO NOT have a -k argument
     * at all or that have it with a value different from runservice".
     * 
     * @param process
     * @return true if the process represents the root httpd instance if run as a windows service
     */
    private static boolean isWindowsServiceRootInstance(ProcessScanResult process) {
        String kArg = getCommandLineOption(process.getProcessInfo().getCommandLine(), "-k");

        return kArg != null && kArg.equalsIgnoreCase("runservice");
    }
}
