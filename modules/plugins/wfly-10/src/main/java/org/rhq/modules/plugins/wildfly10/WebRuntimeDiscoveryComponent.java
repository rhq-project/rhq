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

package org.rhq.modules.plugins.wildfly10;

import static org.rhq.core.pluginapi.util.ResponseTimeConfiguration.RESPONSE_TIME_LOG_FILE_CONFIG_PROP;
import static org.rhq.core.pluginapi.util.ResponseTimeLogFinder.FALLBACK_RESPONSE_TIME_LOG_FILE_DIRECTORY;
import static org.rhq.core.pluginapi.util.ResponseTimeLogFinder.findResponseTimeLogFileInDirectory;

import java.io.File;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.util.StringUtil;
import org.rhq.modules.plugins.wildfly10.json.Address;
import org.rhq.modules.plugins.wildfly10.json.ReadAttribute;
import org.rhq.modules.plugins.wildfly10.json.Result;

/**
 * @author Thomas Segismont
 */
public class WebRuntimeDiscoveryComponent extends VersionedRuntimeDiscovery {
    private static final Log LOG = LogFactory.getLog(WebRuntimeDiscoveryComponent.class);

    private static final String CONTEXT_ROOT_ATTRIBUTE = "context-root";
    private static final String PATH_ATTRIBUTE = "path";
    private static final String PATH_DELIM = ",";
    private static final String SERVER_PREFIX = "server=";

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<BaseComponent<?>> context)
        throws Exception {
        Set<DiscoveredResourceDetails> discoveredResources = super.discoverResources(context);
        if (discoveredResources.isEmpty()) {
            return discoveredResources;
        }
        if (discoveredResources.size() != 1) {
            throw new IllegalStateException("Discovered multiple instances of a singleton resource type");
        }

        BaseComponent<?> parentResourceComponent = context.getParentResourceComponent();
        BaseServerComponent serverComponent = parentResourceComponent.getServerComponent();
        DiscoveredResourceDetails resourceDetails = discoveredResources.iterator().next();
        Configuration pluginConfig = resourceDetails.getPluginConfiguration();
        String nodePath = pluginConfig.getSimpleValue(PATH_ATTRIBUTE);
        String contextRoot = getContextRootAttribute(new Address(nodePath), parentResourceComponent.getASConnection());

        discoverResponseTimeLogFile(serverComponent, pluginConfig, nodePath, contextRoot);

        return discoveredResources;
    }

    private void discoverResponseTimeLogFile(BaseServerComponent serverComponent, Configuration pluginConfig,
        String nodePath, String contextRoot) {
        if (serverComponent.isManuallyAddedServer() && !serverComponent.getServerPluginConfiguration().isLocal()) {
            return;
        }
        String rtFilePath = null;
        // First search in server log directory
        if (serverComponent instanceof StandaloneASComponent) {
            File serverLogDir = serverComponent.getServerPluginConfiguration().getLogDir();
            if (serverLogDir != null) {
                File rtDirectory = new File(serverLogDir, "rt");
                rtFilePath = findResponseTimeLogFileInDirectory(contextRoot, rtDirectory);
            }
        } else if (serverComponent instanceof HostControllerComponent) {
            File hostControllerBaseDir = serverComponent.getServerPluginConfiguration().getBaseDir();
            String managedServerName = getManagedServerNameFromPathAttribute(nodePath);
            if (hostControllerBaseDir != null) {
                File rtDirectory = new File(hostControllerBaseDir, "servers" + File.separator + managedServerName
                    + File.separator + "log" + File.separator + "rt");
                rtFilePath = findResponseTimeLogFileInDirectory(contextRoot, rtDirectory);
            }
        } else {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Unknown BaseServerComponent class: " + serverComponent.getClass().getName());
            }
        }
        if (rtFilePath == null) {
            // Search again in FALLBACK_RESPONSE_TIME_LOG_FILE_DIRECTORY
            rtFilePath = findResponseTimeLogFileInDirectory(contextRoot, FALLBACK_RESPONSE_TIME_LOG_FILE_DIRECTORY);
        }
        if (rtFilePath != null) {
            pluginConfig.setSimpleValue(RESPONSE_TIME_LOG_FILE_CONFIG_PROP, rtFilePath);
        }
    }

    private String getContextRootAttribute(Address address, ASConnection asConnection) {
        ReadAttribute readAttribute = new ReadAttribute(address, CONTEXT_ROOT_ATTRIBUTE);
        Result readAttributeResult = asConnection.execute(readAttribute);
        if (!readAttributeResult.isSuccess()) {
            throw new RuntimeException("Could not read [" + CONTEXT_ROOT_ATTRIBUTE + "] attribute of node ["
                + address.getPath() + "]: " + readAttributeResult.getFailureDescription());
        }
        return (String) readAttributeResult.getResult();
    }

    private String getManagedServerNameFromPathAttribute(String nodePath) {
        for (StringTokenizer tokenizer = new StringTokenizer(nodePath, PATH_DELIM); tokenizer.hasMoreTokens();) {
            String token = tokenizer.nextToken();
            if (token.startsWith(SERVER_PREFIX)) {
                String managedServerName = token.substring(SERVER_PREFIX.length());
                if (StringUtil.isBlank(managedServerName)) {
                    throw new RuntimeException("Blank managed server name in path [" + nodePath + "]");
                }
                return managedServerName;
            }
        }
        throw new RuntimeException("Could not determine managed server name from path [" + nodePath + "]");
    }
}
