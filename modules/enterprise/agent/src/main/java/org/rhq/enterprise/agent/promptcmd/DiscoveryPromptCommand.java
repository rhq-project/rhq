/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.agent.promptcmd;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import mazz.i18n.Msg;

import org.rhq.core.clientapi.agent.metadata.PluginMetadataManager;
import org.rhq.core.clientapi.agent.metadata.ResourceTypeNotEnabledException;
import org.rhq.core.clientapi.server.discovery.InventoryReport;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.ProcessScan;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pc.inventory.ResourceContainer;
import org.rhq.core.pc.inventory.RuntimeDiscoveryExecutor;
import org.rhq.core.pc.plugin.PluginComponentFactory;
import org.rhq.core.pc.util.DiscoveryComponentProxyFactory;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.PluginContainerDeployment;
import org.rhq.core.pluginapi.inventory.ProcessScanResult;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.system.ProcessInfo;
import org.rhq.core.system.SystemInfo;
import org.rhq.core.system.SystemInfoFactory;
import org.rhq.core.system.pquery.ProcessInfoQuery;
import org.rhq.core.util.exception.ExceptionPackage;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.agent.AgentConfiguration;
import org.rhq.enterprise.agent.AgentMain;
import org.rhq.enterprise.agent.i18n.AgentI18NFactory;
import org.rhq.enterprise.agent.i18n.AgentI18NResourceKeys;

/**
 * Allows the user to ask a plugin to run a discovery just as a means to debug a plugin discovery run.
 *
 * @author John Mazzitelli
 */
public class DiscoveryPromptCommand implements AgentPromptCommand {
    private static final Msg MSG = AgentI18NFactory.getMsg();

    /**
     * @see AgentPromptCommand#getPromptCommandString()
     */
    public String getPromptCommandString() {
        return MSG.getMsg(AgentI18NResourceKeys.DISCOVERY);
    }

    /**
     * @see AgentPromptCommand#execute(AgentMain, String[])
     */
    public boolean execute(AgentMain agent, String[] args) {
        PrintWriter out = agent.getOut();

        if (PluginContainer.getInstance().isStarted()) {
            // strip the first argument, which is the name of our prompt command
            String[] realArgs = new String[args.length - 1];
            System.arraycopy(args, 1, realArgs, 0, args.length - 1);

            // use getAgentName because it is the name of the plugin container
            processCommand(agent, realArgs, out);
        } else {
            out.println(MSG.getMsg(AgentI18NResourceKeys.DISCOVERY_PC_NOT_STARTED));
        }

        return true;
    }

    /**
     * @see AgentPromptCommand#getSyntax()
     */
    public String getSyntax() {
        return MSG.getMsg(AgentI18NResourceKeys.DISCOVERY_SYNTAX);
    }

    /**
     * @see AgentPromptCommand#getHelp()
     */
    public String getHelp() {
        return MSG.getMsg(AgentI18NResourceKeys.DISCOVERY_HELP);
    }

    /**
     * @see AgentPromptCommand#getDetailedHelp()
     */
    public String getDetailedHelp() {
        return MSG.getMsg(AgentI18NResourceKeys.DISCOVERY_DETAILED_HELP);
    }

    private void processCommand(AgentMain agent, String[] args, PrintWriter out) {
        AgentConfiguration agentConfig = agent.getConfiguration();
        String pcName = agentConfig.getAgentName();
        String pluginName = null;
        String resourceTypeName = null;
        Integer resourceId = null;
        boolean verbose = false;
        boolean full = true;

        String sopts = "-p:i:r:dvb:";
        LongOpt[] lopts = { new LongOpt("plugin", LongOpt.REQUIRED_ARGUMENT, null, 'p'), //
            new LongOpt("resourceId", LongOpt.REQUIRED_ARGUMENT, null, 'i'), //
            new LongOpt("resourceType", LongOpt.REQUIRED_ARGUMENT, null, 'r'), //
            new LongOpt("dry-run", LongOpt.NO_ARGUMENT, null, 'd'), //
            new LongOpt("verbose", LongOpt.NO_ARGUMENT, null, 'v'), //
            new LongOpt("blacklist", LongOpt.REQUIRED_ARGUMENT, null, 'b') };

        Getopt getopt = new Getopt("discovery", args, sopts, lopts);
        int code;

        while ((code = getopt.getopt()) != -1) {
            switch (code) {
            case ':':
            case '?':
            case 1: {
                out.println(MSG.getMsg(AgentI18NResourceKeys.HELP_SYNTAX_LABEL, getSyntax()));
                return;
            }

            case 'p': {
                pluginName = getopt.getOptarg();
                break;
            }

            case 'i': {
                resourceId = Integer.valueOf(getopt.getOptarg());
                break;
            }

            case 'r': {
                resourceTypeName = getopt.getOptarg();
                break;
            }

            case 'd': {
                full = false;
                break;
            }

            case 'b': {
                String opt = getopt.getOptarg();
                InventoryManager inventoryManager = PluginContainer.getInstance().getInventoryManager();
                DiscoveryComponentProxyFactory factory = inventoryManager.getDiscoveryComponentProxyFactory();
                if (opt.equalsIgnoreCase("list")) {
                    HashSet<ResourceType> blacklist = factory.getResourceTypeBlacklist();
                    out.println(MSG.getMsg(AgentI18NResourceKeys.DISCOVERY_BLACKLIST_LIST, blacklist));
                } else if (opt.equalsIgnoreCase("clear")) {
                    factory.clearResourceTypeBlacklist();
                    out.println(MSG.getMsg(AgentI18NResourceKeys.DISCOVERY_BLACKLIST_CLEAR));
                } else {
                    out.println(MSG.getMsg(AgentI18NResourceKeys.HELP_SYNTAX_LABEL, getSyntax()));
                }
                return;
            }

            case 'v': {
                verbose = true;
                break;
            }
            }
        }

        if ((getopt.getOptind() + 1) < args.length) {
            out.println(MSG.getMsg(AgentI18NResourceKeys.HELP_SYNTAX_LABEL, getSyntax()));
            return;
        }

        if (full) {
            // do a full discovery - we ignore the -p and -r and -i options and do everything

            if (!agent.getClientCommandSender().isSending()) {
                out.println(MSG.getMsg(AgentI18NResourceKeys.DISCOVERY_AGENT_NOT_CONNECTED_TO_SERVER));
            }

            InventoryManager inventoryManager = PluginContainer.getInstance().getInventoryManager();
            if (inventoryManager.isDiscoveryScanInProgress()) {
                out.println(MSG.getMsg(AgentI18NResourceKeys.DISCOVERY_SCAN_ALREADY_IN_PROGRESS));
                return;
            }

            HashSet<ResourceType> blacklist = inventoryManager.getDiscoveryComponentProxyFactory()
                .getResourceTypeBlacklist();
            if (!blacklist.isEmpty()) {
                out.println(MSG.getMsg(AgentI18NResourceKeys.DISCOVERY_BLACKLISTED_TYPES, blacklist));
            }
            long start = System.currentTimeMillis();
            InventoryReport scan1 = inventoryManager.executeServerScanImmediately();
            InventoryReport scan2 = inventoryManager.executeServiceScanImmediately();
            out.println(MSG.getMsg(AgentI18NResourceKeys.DISCOVERY_FULL_RUN, (System.currentTimeMillis() - start)));
            printInventoryReport(scan1, out, verbose);
            printInventoryReport(scan2, out, verbose);
        } else {
            try {
                if (resourceId == null) {
                    discovery(pcName, out, pluginName, resourceTypeName, verbose);
                } else {
                    // specifying a resource ID implies we must ignore -r and -p (since type/plugin is determined by the resource)
                    InventoryManager im = PluginContainer.getInstance().getInventoryManager();
                    ResourceContainer resourceContainer = im.getResourceContainer(resourceId);
                    if (resourceContainer != null) {
                        Resource resource = resourceContainer.getResource();
                        PluginContainerConfiguration pcc = agentConfig.getPluginContainerConfiguration();
                        RuntimeDiscoveryExecutor scanner = new RuntimeDiscoveryExecutor(im, pcc, resource);
                        InventoryReport report = scanner.call();
                        out.println(MSG.getMsg(AgentI18NResourceKeys.DISCOVERY_RESOURCE_SERVICES, resource.getName()));
                        printInventoryReport(report, out, verbose);
                    } else {
                        out.println(MSG.getMsg(AgentI18NResourceKeys.DISCOVERY_RESOURCE_ID_INVALID, resourceId));
                    }
                }
            } catch (Exception e) {
                out.println(MSG.getMsg(AgentI18NResourceKeys.DISCOVERY_ERROR, ThrowableUtil.getAllMessages(e)));
            }
        }

        return;
    }

    private void discovery(String pcName, PrintWriter out, String pluginName, String resourceTypeName, boolean verbose)
        throws Exception {
        PluginContainer pc = PluginContainer.getInstance();
        PluginMetadataManager metadataManager = pc.getPluginManager().getMetadataManager();
        Set<ResourceType> typesToDiscover = new TreeSet<ResourceType>(new PluginPrimaryResourceTypeComparator());

        // make sure the plugin exists first (if one was specified)
        Set<String> allPlugins = metadataManager.getPluginNames();
        if (pluginName != null) {
            if (!allPlugins.contains(pluginName)) {
                out.println(MSG.getMsg(AgentI18NResourceKeys.DISCOVERY_BAD_PLUGIN_NAME, pluginName));
                return;
            }
        }

        // determine which resource types are to be discovered
        Set<ResourceType> allTypes = metadataManager.getAllTypes();
        if (resourceTypeName != null) {
            for (ResourceType type : allTypes) {
                if (type.getName().equals(resourceTypeName)) {
                    if ((pluginName == null) || (pluginName.equals(type.getPlugin()))) {
                        typesToDiscover.add(type);
                    }
                }
            }
        } else {
            // if a plugin was specified, only discover its types; otherwise, discover ALL types
            if (pluginName != null) {
                for (ResourceType type : allTypes) {
                    if (pluginName.equals(type.getPlugin())) {
                        typesToDiscover.add(type);
                    }
                }
            } else {
                typesToDiscover.addAll(allTypes);
            }
        }

        if (typesToDiscover.size() == 0) {
            if (pluginName == null) {
                out.println(MSG.getMsg(AgentI18NResourceKeys.DISCOVERY_BAD_RESOURCE_TYPE_NAME, resourceTypeName));
            } else {
                out.println(MSG.getMsg(AgentI18NResourceKeys.DISCOVERY_BAD_PLUGIN_RESOURCE_TYPE_NAME, pluginName,
                    resourceTypeName));
            }

            return;
        }

        InventoryManager inventoryManager = pc.getInventoryManager();
        HashSet<ResourceType> blacklist = inventoryManager.getDiscoveryComponentProxyFactory()
            .getResourceTypeBlacklist();
        Iterator<ResourceType> iterator = blacklist.iterator();
        while (iterator.hasNext()) {
            ResourceType type = iterator.next();
            if (!typesToDiscover.contains(type)) {
                iterator.remove();
            }
        }
        if (!blacklist.isEmpty()) {
            out.println(MSG.getMsg(AgentI18NResourceKeys.DISCOVERY_BLACKLISTED_TYPES, blacklist));
        }

        for (ResourceType typeToDiscover : typesToDiscover) {
            if (typeToDiscover.getCategory().equals(ResourceCategory.SERVER)
                && (typeToDiscover.getParentResourceTypes().size() == 0)) {
                out.println(MSG.getMsg(AgentI18NResourceKeys.DISCOVERY_DISCOVERING_RESOURCE_TYPE, typeToDiscover
                    .getPlugin(), typeToDiscover.getName()));
                discoveryForSingleResourceType(pcName, out, typeToDiscover, verbose);
                out.println(MSG.getMsg(AgentI18NResourceKeys.DISCOVERY_DISCOVERING_RESOURCE_TYPE_DONE, typeToDiscover
                    .getPlugin(), typeToDiscover.getName()));
                out.println();
            }
        }

        return;
    }

    @SuppressWarnings("unchecked")
    private void discoveryForSingleResourceType(String pcName, PrintWriter out, ResourceType resourceType,
        boolean verbose) {

        try {
            // perform auto-discovery PIQL queries now to see if we can auto-detect resources that are running now
            List<ProcessScanResult> scanResults = new ArrayList<ProcessScanResult>();
            SystemInfo systemInfo = SystemInfoFactory.createSystemInfo();

            Set<ProcessScan> processScans = resourceType.getProcessScans();
            if ((processScans != null) && (processScans.size() > 0)) {
                try {
                    ProcessInfoQuery piq = new ProcessInfoQuery(systemInfo.getAllProcesses());
                    if (processScans != null) {
                        for (ProcessScan processScan : processScans) {
                            List<ProcessInfo> queryResults = piq.query(processScan.getQuery());
                            if ((queryResults != null) && (queryResults.size() > 0)) {
                                for (ProcessInfo autoDiscoveredProcess : queryResults) {
                                    scanResults.add(new ProcessScanResult(processScan, autoDiscoveredProcess));
                                    out.println(MSG.getMsg(AgentI18NResourceKeys.DISCOVERY_PROCESS_SCAN, resourceType
                                        .getPlugin(), resourceType.getName(), processScan, autoDiscoveredProcess));
                                }
                            }
                        }
                    }
                } catch (UnsupportedOperationException uoe) {
                    // don't worry if we do not have a native library to support process scans
                }
            }

            PluginComponentFactory componentFactory = PluginContainer.getInstance().getPluginComponentFactory();
            InventoryManager inventoryManager = PluginContainer.getInstance().getInventoryManager();
            ResourceContainer platformContainer = inventoryManager.getResourceContainer(inventoryManager.getPlatform());
            ResourceComponent platformComponent = inventoryManager.getResourceComponent(inventoryManager.getPlatform());
            ResourceDiscoveryComponent discoveryComponent = componentFactory.getDiscoveryComponent(resourceType,
                platformContainer);

            ResourceDiscoveryContext context = new ResourceDiscoveryContext(resourceType, platformComponent,
                platformContainer.getResourceContext(), systemInfo, scanResults, Collections.EMPTY_LIST, pcName,
                PluginContainerDeployment.AGENT);

            Set<DiscoveredResourceDetails> discoveredResources;
            discoveredResources = inventoryManager.invokeDiscoveryComponent(platformContainer, discoveryComponent, context);
            if (discoveredResources != null) {
                for (DiscoveredResourceDetails discoveredResource : discoveredResources) {
                    out.println(MSG.getMsg(AgentI18NResourceKeys.DISCOVERY_COMPONENT_RESULT, discoveredResource
                        .getResourceType().getPlugin(), discoveredResource.getResourceType().getName(),
                        discoveredResource.getResourceKey(), discoveredResource.getResourceName(), discoveredResource
                            .getResourceVersion(), discoveredResource.getResourceDescription()));
                    if (verbose) {
                        printConfiguration(discoveredResource.getPluginConfiguration(), out);
                    }
                }
            }
        } catch (ResourceTypeNotEnabledException rtne) {
            // we are to ignore this type of resource, just skip it
        } catch (Throwable t) {
            out.println(ThrowableUtil.getAllMessages(t));
        }

        return;
    }

    private void printConfiguration(Configuration config, PrintWriter out) {
        for (Property property : config.getMap().values()) {
            StringBuilder builder = new StringBuilder();
            builder.append("    ");
            builder.append(property.getName());
            builder.append("=");
            if (property instanceof PropertySimple) {
                String value = ((PropertySimple) property).getStringValue();
                builder.append((value != null) ? "\"" + value + "\"" : value);
            } else {
                builder.append(property);
            }
            out.println(builder);
        }
    }

    private void printInventoryReport(InventoryReport report, PrintWriter out, boolean verbose) {
        long start = report.getStartTime();
        long end = report.getEndTime();
        boolean isServiceScan = report.isRuntimeReport();
        int count = report.getResourceCount();
        Set<Resource> roots = report.getAddedRoots();
        List<ExceptionPackage> errors = report.getErrors();

        out.println(MSG.getMsg(AgentI18NResourceKeys.DISCOVERY_INVENTORY_REPORT_SUMMARY,
            (isServiceScan ? "Service Scan" : "Server Scan"), new Date(start), new Date(end), count));

        if (verbose) {
            if (roots != null) {
                for (Resource resource : roots) {
                    out.println(MSG.getMsg(AgentI18NResourceKeys.DISCOVERY_INVENTORY_REPORT_RESOURCE, resource));
                }
            }

            if (errors != null) {
                for (ExceptionPackage error : errors) {
                    out.println(MSG.getMsg(AgentI18NResourceKeys.DISCOVERY_INVENTORY_REPORT_ERROR, error
                        .getAllMessages()));
                }
            }
        }

        out.println();
        return;
    }

    private class PluginPrimaryResourceTypeComparator implements Comparator<ResourceType> {
        @Override
        public int compare(ResourceType type1, ResourceType type2) {
            if (type1.getPlugin() == null) {
                return (type2.getPlugin() == null) ? 0 : -1;
            }
            int result = (type2.getPlugin() == null) ? 1 : type1.getPlugin().compareTo(type2.getPlugin());
            if (result != 0) {
                return result;
            }
            if (type1.getName() == null) {
                return (type2.getName() == null) ? 0 : -1;
            }
            return (type2.getName() == null) ? 1 : type1.getName().compareTo(type2.getName());
        }
    }

}