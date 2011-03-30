/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.core.pc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.agent.configuration.ConfigurationUpdateRequest;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.discovery.AvailabilityReport;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementDataRequest;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.configuration.ConfigurationManager;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pc.inventory.ResourceContainer;
import org.rhq.core.pc.measurement.MeasurementManager;
import org.rhq.core.pc.operation.OperationContextImpl;
import org.rhq.core.pc.plugin.FileSystemPluginFinder;
import org.rhq.core.pluginapi.operation.OperationContext;
import org.rhq.core.pluginapi.operation.OperationServices;
import org.rhq.core.pluginapi.operation.OperationServicesResult;
import org.rhq.core.pluginapi.operation.OperationServicesResultCode;
import org.rhq.core.system.SystemInfoFactory;

/**
 * Starter class to start a standalone PC to help
 * in PluginDevelopment
 *
 * @author Heiko W. Rupp
 */
public class StandaloneContainer {

    /** PLugin container we are working with */
    private PluginContainer pc;
    /** Resource id that need to be set to invoke or measure a resource */
    private int resourceId;
    /** Platform resource as base */
    private Resource platform;
    /** The inventory manager within the plugin container */
    InventoryManager inventoryManager;
    /** Global operation counter */
    Integer opId = 0;
    /** Holder for the command history */
    List<String> history = new ArrayList<String>(10);
    /** Map of resource plugin configurations */
    Map<Integer, Configuration> resConfigMap = new HashMap<Integer, Configuration>();
    /** variable set by find() and which can be used in set() */
    int dollarR = 0;

    private static final String HISTORY_HELP = "!! : repeat the last action\n" + //
        "!? : show the history of commands issued\n" + //
        "!h : show this help\n" + //
        "!nn : repeat history item with number nn\n" + //
        "!w fileName : write history to file with name fileName\n" + //
        "!dnn : delete history item with number nn";

    public static void main(String[] argv) {
        StandaloneContainer sc = new StandaloneContainer();
        BufferedReader br = null;

        if (argv.length == 0)
            br = new BufferedReader(new InputStreamReader(System.in));
        else {
            try {
                br = new BufferedReader(new InputStreamReader(new FileInputStream(argv[0])));
            } catch (FileNotFoundException fnfe) {
                System.err.println("File " + argv[0] + " not found");
                System.exit(1);
            }
        }

        sc.run(br);
    }

    /**
     * Run the show. Sets up the plugin container and runs the main loop
     * If we read input from a file, the method will terminate after all input
     * is consumed.
     * @param br A BufferedReader - either standard in or a file.
     *
     */
    private void run(BufferedReader br) {

        boolean shouldQuit = false;

        // load the PC
        System.out.println("\nStarting the plugin container.");
        pc = PluginContainer.getInstance();
        File pluginDir = new File("plugins");
        File dataDir = new File("data");
        PluginContainerConfiguration pcConfig = new PluginContainerConfiguration();
        pcConfig.setPluginFinder(new FileSystemPluginFinder(pluginDir));
        pcConfig.setPluginDirectory(pluginDir);
        pcConfig.setDataDirectory(dataDir);
        pcConfig.setInsideAgent(false);
        pcConfig.setRootPluginClassLoaderRegex(PluginContainerConfiguration.getDefaultClassLoaderFilter());
        pcConfig.setCreateResourceClassloaders(true); // we need to create these because even though we aren't in the agent, we aren't embedded in a managed resource either
        pcConfig.setServerServices(new ServerServices());
        pc.setConfiguration(pcConfig);

        System.out.println("Loading plugins");
        pc.initialize();
        for (String plugin : pc.getPluginManager().getMetadataManager().getPluginNames()) {
            System.out.println("...Loaded plugin: " + plugin);
        }

        inventoryManager = pc.getInventoryManager();

        platform = inventoryManager.getPlatform();

        System.out.println("\nReady.");

        // Run the main loop
        while (!shouldQuit) {
            try {
                System.out.print("[" + history.size() + "]:" + resourceId + " > ");
                String answer = br.readLine();
                if (answer == null) {
                    break;
                }

                if (answer.equalsIgnoreCase(Command.STDIN.toString())) {
                    br = new BufferedReader(new InputStreamReader(System.in));
                }

                // Check for history commands
                answer = handleHistory(answer);

                // If we have a 'real' command, dispatch it
                if (!answer.startsWith("!")) {
                    String[] tokens = answer.split(" ");
                    if (tokens.length > 0) {
                        shouldQuit = dispatchCommand(tokens);
                    }
                }
            } catch (Throwable throwable) {
                System.err.println("Exception happened: " + throwable + "\n");
            }
        }

        System.out.println("Shutting down ...");
        // unload when we're done
        pc.shutdown();

    }

    /**
     * Handle processing of the command history. This gives some csh like commands
     * and records the commands given. Nice side effect is the possibility to write the
     * history to disk and to use this later as input so that testing can be scripted.
     * Commands are:
     * <ul>
     * <li>!! : repeat the last action</li>
     * <li>!? : show the history</li>
     * <li>!h : show history help</li>
     * <li>!<i>nnn</i> : repeat history item with number <i>nnn</i></li>
     * <li>!w <i>file</i> : write the history to the file <i>file</i></li>
     * <li>!d<i>nnn</i> : delete the history item with number <i>nnn</i>
     * </ul>
     * @param answer the input given on the command line
     * @return a command or '!' if no command substitution from the history was possible.
     */
    private String handleHistory(String answer) {

        // Normal command - just return it
        if (!answer.startsWith("!")) {
            history.add(answer);
            return answer;
        }

        // History commands
        if (answer.startsWith("!?")) {
            for (int i = 0; i < history.size(); i++)
                System.out.println("[" + i + "]: " + history.get(i));
        } else if (answer.startsWith("!h")) {
            System.out.println(HISTORY_HELP);
            return "!";
        } else if (answer.startsWith("!!")) {
            String text = history.get(history.size() - 1);
            System.out.println(text);
            history.add(text);
            return text;
        } else if (answer.matches("![0-9]+")) {
            String id = answer.substring(1);
            Integer i;
            try {
                i = Integer.valueOf(id);
            } catch (NumberFormatException nfe) {
                System.err.println(id + " is no valid history position");
                return "!";
            }
            if (i > history.size()) {
                System.err.println(i + " is no valid history position");
                return "!";
            } else {
                String text = history.get(i);
                System.out.println(text);
                history.add(text);
                return text;
            }
        } else if (answer.startsWith("!w")) {
            String[] tokens = answer.split(" ");
            if (tokens.length < 2) {
                System.err.println("Not enough parameters. You need to give a file name");
            }
            File file = new File(tokens[1]);
            try {
                file.createNewFile();
                if (file.canWrite()) {
                    Writer writer = new FileWriter(file);
                    for (String item : history) {
                        writer.write(item);
                        writer.write("\n");
                    }
                    writer.flush();
                    writer.close();
                } else {
                    System.err.println("Can not write to file " + file);
                }
            } catch (IOException ioe) {
                System.err.println("Saving the history to file " + file + " failed: " + ioe.getMessage());
            }
            return "!";
        } else if (answer.matches("!d[0-9]+")) {
            String id = answer.substring(2);
            Integer i;
            try {
                i = Integer.valueOf(id);
            } catch (NumberFormatException nfe) {
                System.err.println(id + " is no valid history position");
                return "!";
            }
            if (i > history.size()) {
                System.err.println(i + " is no valid history position");
                return "!";
            }
            history.remove(i.intValue());
            return "!";
        } else {
            System.err.println(answer + " is no valid history command");
            return "!";
        }
        return "!";
    }

    /**
     * Dispatches the input to various commands that do the actual work.
     * @param tokens The tokens from the command line including the command itself
     * @return true the quit command was given, false otherwise.
     * @throws Exception If anything goes wrong
     */
    private boolean dispatchCommand(String[] tokens) throws Exception {

        if (tokens.length == 0)
            return false;

        if (tokens[0].startsWith("#"))
            return false;

        Command com = Command.get(tokens[0]);
        if (com == null) {
            System.err.println("Command " + tokens[0] + " is unknown");
            return false;
        }
        int minArgs = com.getMinArgs();
        if (tokens.length < minArgs + 1) {
            System.err.println("Command " + com + " needs " + minArgs + " parameter(s): " + com.getArgs());
            return false;
        }

        switch (com) {
        case ASCAN:
            AvailabilityReport aReport = pc.getDiscoveryAgentService().executeAvailabilityScanImmediately(false);

            System.out.println(aReport);
            break;
        case AVAIL:
            avail(tokens);
            break;
        case CHILDREN:
            children(tokens);
            break;
        case DISCOVER:
            discover(tokens);
            break;
        //            case EVENT:
        //                event(tokens);
        //                break;
        case FIND:
            find(tokens);
            break;
        case HELP:
            for (Command comm : EnumSet.allOf(Command.class)) {
                System.out.println(comm + " ( " + comm.getAbbrev() + " ), " + comm.getArgs() + " : " + comm.getHelp());
            }
            System.out.println("Also check out !h for help on history commands");
            break;
        case INVOKE:
            invokeOps(tokens);
            break;
        case MEASURE:
            measure(tokens);
            break;
        case NATIVE:
            doNative(tokens);
            break;
        case QUIT:
            System.out.println("Terminating ..");
            return true;
        case RESOURCES:
            resources();
            break;
        case SET:
            set(tokens);
            break;
        case STDIN:
            // handled in the outer loop
            break;
        case WAIT:
            Thread.sleep(Integer.valueOf(tokens[1]));
            break;
        case P_CONFIG:
            showPluginConfig();
            break;
        case R_CONFIG:
            showResourceConfig();
            break;
        case SR_CONFIG:
            setResourcePluginConfig(tokens, false);
            break;
        case SP_CONFIG:
            setResourcePluginConfig(tokens,true);
            break;
        }

        return false;
    }

    /**
     * Invokes an operation
     * @param tokens tokenized command line tokens[0] is the command itself
     * @throws Exception if anything goes wrong
     */
    private void invokeOps(String[] tokens) throws Exception {
        if (resourceId == 0) {
            System.err.println("No resource selected");
            return;
        }

        String operation = tokens[1];
        if (operation.equals("-list")) {
            ResourceType rt = getTypeForResourceId();
            Set<OperationDefinition> opDefs = rt.getOperationDefinitions();
            for (OperationDefinition def :opDefs) {
                System.out.println(def.getName() + " : " + def.getDescription());
                ConfigurationDefinition params = def.getParametersConfigurationDefinition();
                if (params!=null && params.getPropertyDefinitions()!=null && !params.getPropertyDefinitions().isEmpty()) {
                    System.out.println("  Parameters:");
                    for (Map.Entry<String,PropertyDefinition> param : params.getPropertyDefinitions().entrySet()) {
                        System.out.println("    " + param.getKey()); // TODO add more info
                    }
                }
            }
            if (opDefs.isEmpty()) {
                System.out.println("Resource has no operations");
            }
            return;
        }


        OperationContext operationContext = new OperationContextImpl(resourceId);
        OperationServices operationServices = operationContext.getOperationServices();
        opId++;

        Configuration config = null;
        if (tokens.length > 2)
            config = createConfigurationFromString(tokens[2]);

        OperationServicesResult res = operationServices.invokeOperation(operationContext, tokens[1], config, 2000);
        if (res.getResultCode() == OperationServicesResultCode.FAILURE) {
            System.err.println("Failure executing the operation: \n" + res.getErrorStackTrace());
        } else if (res.getResultCode() == OperationServicesResultCode.TIMED_OUT) {
            System.err.println("Operation timed out ");
        } else {
            Configuration result = res.getComplexResults();
            if (result == null)
                System.out.println("Operation did not return a result");
            else
                System.out.println(result.getProperties());
        }

    }

    private void setResourcePluginConfig(String[] tokens,boolean pluginConfig) {
        if (resourceId == 0) {
            System.err.println("No resource set");
            return;
        }


        Configuration config = null;
        if (tokens.length > 1)
            config = createConfigurationFromString(tokens[1]);
        else {
            System.err.println("Need at least 1 token");
            return;
        }

        ConfigurationUpdateRequest request = new ConfigurationUpdateRequest(1,config,resourceId);

        ConfigurationManager cm = pc.getConfigurationManager();

        if (pluginConfig) {
            pc.getInventoryManager().getResourceContainer(resourceId).getResource().setPluginConfiguration(config);
        } else
            cm.updateResourceConfiguration(request);



    }



    private ResourceType getTypeForResourceId() {
        ResourceContainer rc = inventoryManager.getResourceContainer(resourceId);
        Resource res = rc.getResource();
        return res.getResourceType();
    }

    /**
     * Enables or disables the native layer.
     * @param tokens tokenized command line tokens[0] is the command itself
     */
    private void doNative(String[] tokens) {
        String what = tokens[1];
        if (what.startsWith("e")) {
            SystemInfoFactory.enableNativeSystemInfo();
            System.out.println("Native layer enabled.");
        } else if (what.startsWith("d")) {
            SystemInfoFactory.disableNativeSystemInfo();
            System.out.println("Native layer disabled.");
        } else if (what.startsWith("s")) {
            System.out.println(SystemInfoFactory.isNativeSystemInfoAvailable() ? "Available" : "Not Available");
            System.out.println(SystemInfoFactory.isNativeSystemInfoDisabled() ? "Disabled" : "Enabled");
            System.out.println(SystemInfoFactory.isNativeSystemInfoInitialized() ? "Initialized" : "Not initialized");
        } else {
            System.err.println("Unknown option. Only 'e', 'd' and 's' are applicable (enable/disable/status)");
            return;
        }

    }

    /**
     * Poll events
     * @param tokens tokenized command line tokens[0] is the command itself
     */
    private void event(String[] tokens) {
        if (resourceId == 0)
            return;

        // TODO
        System.err.println("Not yet implemented");
    }

    /**
     * Shows the list of resources known so far
     */
    private void resources() {
        Set<Resource> resources = getResources();
        for (Resource res : resources)
            System.out.println(res);
    }

    /**
     * Shows the list of availabilities known so far
     * for resources that have been discovered
     * @param tokens tokenized command line tokens[0] is the command itself
     */
    private void avail(String[] tokens) {
        Set<Resource> resources = getResources();

        int id = 0;
        if (tokens.length > 1) {
            id = Integer.valueOf(tokens[1]);
        }

        for (Resource res : resources) {
            if (id == 0 || (id != 0 && res.getId() == id)) {
                Availability availability = inventoryManager.getCurrentAvailability(res);
                System.out.println(res.getName() + "( " + res.getId() + " ):" + availability.getAvailabilityType());
            }
        }
    }

    /**
     * Print the direct child resources of a given resource
     * @param tokens tokenized command line tokens[0] is the command itself
     */
    private void children(String[] tokens) {

        int id;
        if (tokens.length>1)
            id = Integer.valueOf(tokens[1]);
        else
            id = resourceId;
        ResourceContainer resourceContainer = inventoryManager.getResourceContainer(id);
        if (resourceContainer != null) {
            Resource r = resourceContainer.getResource();
            Set<Resource> resources = r.getChildResources();
            for (Resource res : resources) {
                System.out.println(res);
            }
        } else {
            System.err.println("There is no resource with id " + id);
        }
    }

    /**
     * Helper to obtain the list of known resources
     * @return Set of resources including the platform
     */
    private Set<Resource> getResources() {

        Set<Resource> resources = new HashSet<Resource>();

        Stack<Resource> stack = new Stack<Resource>();
        stack.push(platform);
        while (!stack.isEmpty()) {
            Resource r = stack.pop();
            resources.add(r);
            stack.addAll(r.getChildResources());
        }

        return resources;
    }

    /**
     * Search resources or resource types by name. Wildcard is the *
     * As side effect - if searching for resources, $r is set to the id of the resource
     * shown last. This can be used in set calls.
     * @param tokens tokenized command line tokens[0] is the command itself
     */
    private void find(String[] tokens) {
        String pattern = tokens[2];
        pattern = pattern.replaceAll("\\*", "\\.\\*");

        if (tokens[1].equals("r")) {
            Set<Resource> resources = getResources();
            for (Resource res : resources) {
                if (res.getName().matches(pattern)) {
                    System.out.println(res.getId() + ": " + res.getName() + " (parent= " + res.getParentResource() + " )");
                    dollarR = res.getId();
                }
            }
        } else if (tokens[1].equals("t")) {
            Set<ResourceType> types = pc.getPluginManager().getMetadataManager().getAllTypes();
            for (ResourceType type : types) {
                if (type.getName().matches(pattern)) {
                    System.out.println(type.getId() + ": " + type.getName() + " (" + type.getPlugin() + " )");
                }
            }
        } else if (tokens[1].equals("rt")) {
            Set<ResourceType> types = pc.getPluginManager().getMetadataManager().getAllTypes();
            Set<Resource> resources = getResources();
            for (ResourceType type : types) {
                if (type.getName().matches(pattern)) {
                    for (Resource res : resources) {
                        if (res.getResourceType().equals(type)) {
                            System.out.println(res.getId() + ": " + res.getName() + " ( " + res.getParentResource()
                                + " )");
                            dollarR = res.getId();
                        }
                    }
                }
            }
        } else {
            System.err.println("'" + tokens[1] + "' is no valid option for find");
        }

    }

    /**
     * Sets working parameter. If the 2nd argument is $r the variable $r set by the latest
     * #find(String[]) call is used.
     * @param tokens tokenized command line tokens[0] is the command itself
     */
    private void set(String[] tokens) {

        String comm = tokens[1].toLowerCase();
        String arg = tokens[2];

        if (comm.startsWith("plu")) {
            //pluginName = arg;
        } else if (comm.startsWith("r") || comm.equals("id")) {
            try {
                if (arg.equals("$r")) {
                    resourceId = dollarR;
                } else
                    resourceId = Integer.valueOf(arg);
            } catch (NumberFormatException nfe) {
                System.err.println("Sorry, but [" + arg + "] is no valid number");
            }
            ResourceContainer rc = inventoryManager.getResourceContainer(resourceId);
            if (rc==null) {
                System.err.println("No resource with that id exists");
                resourceId=0;
            }
        } else
            System.err.println("Bad command " + tokens[1]);

    }

    /**
     * Perform a discovery scan and return the results. Options are:
     * <ul>
     * <li>s: servers only</li>
     * <li>i: services only</li>
     * <li>all: servers and then services</lI>
     * </ul>
     * @param tokens tokenized command line tokens[0] is the command itself
     */
    private void discover(String[] tokens) {

        Set<Resource> existing = getResources();
        String what = tokens[1];
        long t1 = System.currentTimeMillis();
        if (what.startsWith("s"))
            pc.getInventoryManager().executeServerScanImmediately();
        else if (what.startsWith("i"))
            pc.getInventoryManager().executeServiceScanImmediately();
        else if (what.startsWith("all")) {
            pc.getInventoryManager().executeServerScanImmediately();
            pc.getInventoryManager().executeServiceScanImmediately();
        } else {
            System.err.println("Unknown option. Only 's' and 'i' are applicable");
            return;
        }
        long t2 = System.currentTimeMillis();

        System.out.println("Discovery took: " + (t2 - t1) + "ms");
        // Print the just discovered resources.
        Set<Resource> newOnes = getResources();
        newOnes.removeAll(existing);
        System.out.println(newOnes);
    }

    /**
     * Run collecting of measuremen values. All metrics need to be of the same type,
     * that is given as first argument. Remaining arguments are the names of the metrics.
     * @param tokens tokenized command line tokens[0] is the command itself
     */
    private void measure(String[] tokens) {
        if (resourceId == 0) {
            System.err.println("No resource set");
            return;
        }

        MeasurementManager mm = pc.getMeasurementManager();

        if (tokens[1].equals("-list")) {
            ResourceType rt = getTypeForResourceId();
            Set<MeasurementDefinition> defs = rt.getMetricDefinitions();
            if (defs==null || defs.isEmpty()) {
                System.out.println("Resource has no metrics");
                return;
            }
            for (MeasurementDefinition def : defs) {
                System.out.println(def.getName() + " : " + def.getDataType() + ", " + def.getDescription());
            }
            return;
        }

        if (tokens.length<3) {
            System.err.println("measure needs at least two parameters");
            return;
        }

        DataType dataType = getDataType(tokens[1]);
        if (dataType == null) {
            System.err.println("Unknown DataType " + tokens[1]);
            System.err.println("Valid ones are measurement, trait, calltime, complex");
        }

        String[] metricNames = new String[tokens.length - 2];
        System.arraycopy(tokens, 2, metricNames, 0, tokens.length - 2);

        List<MeasurementDataRequest> requests = new ArrayList<MeasurementDataRequest>();
        for (String metric : metricNames) {
            requests.add(new MeasurementDataRequest(metric, dataType));
        }

        Set<MeasurementData> dataset = mm.getRealTimeMeasurementValue(resourceId, requests);
        if (dataset == null) {
            System.err.println("No data returned");
            return;
        }
        for (MeasurementData data : dataset) {
            System.out.println(data);
        }

    }

    /**
     * Helper to get the DataType from the passed input
     * @param token Input to convert
     * @return A valid DataType or null if no valid DataType can be determined.
     * @see org.rhq.core.domain.measurement.DataType for possible token values
     */
    private DataType getDataType(String token) {
        String c = token.toLowerCase();
        if (c.startsWith("m"))
            return DataType.MEASUREMENT;
        else if (c.startsWith("t"))
            return DataType.TRAIT;
        else if (c.startsWith("ca"))
            return DataType.CALLTIME;
        else if (c.startsWith("co"))
            return DataType.COMPLEX;
        else
            return null;
    }

    /**
     * Creates a configuration object from the passed String. The string must consist
     * of individual key-value pairs, that are separated by || keys and values are separated by
     * =.  Only simple properties are supported for the configuration.
     * @param input The input string, may be null
     * @return a Configuration object or null if input was null or if one of the pairs was invalid.
     */
    private Configuration createConfigurationFromString(String input) {
        if (input == null)
            return null;

        Configuration config = new Configuration();
        String[] pairs = input.split("\\|\\|");
        for (String pair : pairs) {
            String[] kv = pair.split("=");
            if (kv.length %2 ==1 ) {
                System.err.println("Token " + pair + " is invalid as it contains no '='");
                return null;
            }
            PropertySimple ps = new PropertySimple(kv[0], kv[1]);
            config.put(ps);
        }
        return config;
    }

    private void showPluginConfig() {
        if (resourceId == 0) {
            System.err.println("You must first set the resource to work with.");
            return;
        }

        Configuration config = pc.getInventoryManager().getResourceContainer(resourceId).getResource().getPluginConfiguration();
        showConfig(config);
    }

    private void showResourceConfig() throws PluginContainerException {
        if (resourceId == 0) {
            System.err.println("You must first set the resource to work with.");
            return;
        }

        Configuration config = pc.getConfigurationManager().loadResourceConfiguration(resourceId);
        showConfig(config);
    }

    private void showConfig(Configuration config) {
        System.out.println(config.getProperties());
    }

    /**
     * List of possible commands
     */
    private enum Command {
        ASCAN("as", "", 0, "Triggers an availability scan"), //
        AVAIL("a", " ( id )", 0,
            "Shows an availability report. If id is given, only shows availability for resource with id id"), //
        CHILDREN("chi", "[id]", 0, "Shows the direct children of the resource with the passed id, or if no id passed of the current resource"), //
        DISCOVER("disc", " s | i | all", 1, "Triggers a discovery scan for (s)erver, serv(i)ce or all resources"), //
        //      EVENT("e", "", 0,  "Pull events"), // TODO needs to be defined
        FIND("find", "r | t  | rt <name>", 2,
            "Searches a (r)esource, resource (t)ype or resources of (rt)ype. Use * as wildcard.\n"
                + " Will set $r for the last resource shown."), HELP("h", "", 0, "Shows this help"), //
        INVOKE("i", "operation [params]", 1, "Triggers running an operation. If operation is '-list' it shows available operations"), //
        MEASURE("m", "datatype property+", 1, "Triggers getting metric values. All need to be of the same data type. If datatype is '-list' it shows the defined metrics"), //
        NATIVE("n", "e | d | s", 1, "Enables/disables native system or shows native status"), //
        QUIT("quit", "", 0, "Terminates the application"), //
        RESOURCES("res", "", 0, "Shows the discovered resources"), //
        SET("set", "'resource' N", 2,
            "Sets the resource id to work with. N can be a number or '$r' as result of last find resource call. 'id' is an alias for 'res'"), //
        STDIN("stdin","",0, "Stop reading the batch file and wait for commands on stdin"), //
        WAIT("w", "milliseconds", 1, "Waits the given amount of time"),
        P_CONFIG("pc", "", 0, "Shows the plugin configuration of the current resource."),
        R_CONFIG("rc", "", 0, "Shows the resource configuration of the current resource."),
        SR_CONFIG("rcs", "", 0, "[parameters] set resource config "),
        SP_CONFIG("pcs", "", 0, "[parameters] set plugin config ")
        ;

        private String abbrev;
        private String args;
        private String help;
        private int minArgs; // minimum number of args needed

        public String getArgs() {
            return args;
        }

        public String getHelp() {
            return help;
        }

        public int getMinArgs() {
            return minArgs;
        }

        /**
         * Construct a new Command
         * @param abbrev Abbreviation for this command
         * @param args Description of expected arguments
         * @param minArgs Minumum number of arguments that need to be present
         * @param help A short description of the command
         */
        private Command(String abbrev, String args, int minArgs, String help) {
            this.abbrev = abbrev;
            this.args = args;
            this.minArgs = minArgs;
            this.help = help;
        }

        public String getAbbrev() {
            return abbrev;
        }

        public static Command get(String s) {

            String upper = s.toUpperCase();

            for (Command c : EnumSet.allOf(Command.class)) {
                if (c.name().equals(upper) || c.getAbbrev().equals(s.toLowerCase()))
                    return c;
            }
            return null;
        }
    }
}
