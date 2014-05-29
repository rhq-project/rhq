/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.plugins.apache.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.augeas.node.AugeasNode;
import org.rhq.augeas.tree.AugeasTree;
import org.rhq.core.domain.util.OSGiVersionComparator;
import org.rhq.core.system.ProcessInfo;
import org.rhq.plugins.apache.parser.ApacheDirective;
import org.rhq.plugins.apache.parser.ApacheDirectiveTree;

/**
 * @author Lukas Krejci
 */
public class RuntimeApacheConfiguration {

    private static final Log LOG = LogFactory.getLog(RuntimeApacheConfiguration.class);

    private static final Set<String> LOGGED_UNKNOWN_MODULES = Collections.synchronizedSet(new HashSet<String>());

    private enum ModuleLoadedState {
        LOADED,
        NOT_LOADED,
        UNKNOWN
    }

    private RuntimeApacheConfiguration() {

    }

    /**
     * A result of a node inspection using {@link NodeInspector}
     * 
     *
     * @author Lukas Krejci
     */
    public static class NodeInspectionResult {
        public boolean nodeIsConditional;
        public boolean shouldRecurseIntoConditionalNode;
    }

    /**
     * Node inspector is used to determine how to proceed with the parsing of the configuration file.
     * 
     *
     * @author Lukas Krejci
     */
    public static class NodeInspector {
        private TransformState state;
        final public boolean keepConditional;

        private NodeInspector(TransformState state, boolean keepConditional) {
            this.state = state;
            this.keepConditional = keepConditional;
        }

        /**
         * Inspects a node.
         * 
         * @param currentNodeName the name of the node
         * @param allValues the list of all values specified on the node 
         * @param valueAsString the original value as a string (from which the list of values was somehow produced)
         * @return the inspection result or null if there was some unexpected event (which has been logged)
         */
        public NodeInspectionResult inspect(String currentNodeName, List<String> allValues, String valueAsString) {
            NodeInspectionResult result = new NodeInspectionResult();

            result.shouldRecurseIntoConditionalNode = true;

            if (currentNodeName.equalsIgnoreCase("LoadModule")) {
                state.loadedModules.add(allValues.get(0));
                result.nodeIsConditional = false;
            } else if (currentNodeName.equalsIgnoreCase("<IfModule")) {
                String moduleFile = valueAsString;
                boolean negate = false;
                if (moduleFile.startsWith("!")) {
                    negate = true;
                    moduleFile = moduleFile.substring(1);
                }

                boolean moduleLoaded = false;

                switch (isModuleLoaded(moduleFile, state.loadedModules, state.moduleNames, state.moduleFiles)) {
                case LOADED:
                    moduleLoaded = true;
                    break;
                case NOT_LOADED:
                    moduleLoaded = false;
                    break;
                case UNKNOWN:
                    if (state.suppressUnknownModuleWarnings && LOGGED_UNKNOWN_MODULES.contains(moduleFile)) {
                        LOG.debug("Encountered unknown module name in an IfModule directive: " + moduleFile);
                    } else {
                        LOG.warn("Encountered unknown module name in an IfModule directive: "
                            + moduleFile
                            + ". If you are using Apache 2.1 or later, you can try changing the module identifier from the source file to "
                            + "the actual module name as used in the LoadModule directive to get rid of this warning.");
                    }
                    LOGGED_UNKNOWN_MODULES.add(moduleFile);
                    return null;
                }

                result.shouldRecurseIntoConditionalNode = moduleLoaded != negate;

                result.nodeIsConditional = true;
            } else if (currentNodeName.equalsIgnoreCase("<IfDefine")) {
                String define = valueAsString;
                boolean negate = false;
                if (define.startsWith("!")) {
                    negate = true;
                    define = define.substring(1);
                }

                boolean isDefined = state.defines.contains(define);

                result.shouldRecurseIntoConditionalNode = isDefined != negate;

                result.nodeIsConditional = true;
            } else if (currentNodeName.equalsIgnoreCase("<IfVersion")) {
                //<IfVersion [[!]operator] version> ... </IfVersion>
                //operator: =, ==, >, >=, <, <=, ~
                //version major[.minor[.patch]] or /regex/
                //if operator is ~, the version is assumed regex
                //if operator is omitted, = is assumed

                if (isModuleLoaded("mod_version.c", state.loadedModules, state.moduleNames, state.moduleFiles) != ModuleLoadedState.LOADED) {
                    LOG.debug("mod_version not loaded and IfVersion directive encountered. Skipping it.");
                    return null;
                }

                List<String> values = allValues;
                String operator = null;
                String version = null;
                boolean negate = false;
                boolean regex = false;

                if (values.size() == 0) {
                    LOG.warn("Invalid IfVersion directive.");
                    return null;
                }

                if (values.size() == 1) {
                    operator = "=";
                    version = values.get(0);
                } else if (values.size() == 2) {
                    operator = values.get(0);
                    version = values.get(1);
                } else {
                    LOG.warn("Too many arguments to a IfVersion directive: " + values);
                    return null;
                }

                if (operator == null || version == null) {
                    LOG.warn("Invalid IfVersion with parameters: " + values);
                    return null;
                }

                if (operator.charAt(0) == '!') {
                    negate = true;
                    operator = operator.substring(1);
                }

                if ("==".equals(operator)) {
                    operator = "=";
                }

                if (version.charAt(0) == '/') {
                    if ("=".equals(operator) || "~".equals(operator)) {
                        regex = true;
                        version = version.substring(1, version.length() - 1);
                    } else {
                        LOG.warn("Unsupported operator " + operator
                            + " with regex version comparison in IfVersion directive.");
                        return null;
                    }
                }

                OSGiVersionComparator comp = new OSGiVersionComparator();

                boolean hasVersion = false;
                if ("=".equals(operator)) {
                    if (regex) {
                        hasVersion = Pattern.matches(version, state.httpdVersion);
                    } else {
                        hasVersion = comp.compare(version, state.httpdVersion) == 0;
                    }
                } else if ("~".equals(operator)) {
                    hasVersion = Pattern.matches(version, state.httpdVersion);
                } else if (">".equals(operator)) {
                    hasVersion = comp.compare(state.httpdVersion, version) > 0;
                } else if (">=".equals(operator)) {
                    hasVersion = comp.compare(state.httpdVersion, version) >= 0;
                } else if ("<".equals(operator)) {
                    hasVersion = comp.compare(state.httpdVersion, version) < 0;
                } else if ("<=".equals(operator)) {
                    hasVersion = comp.compare(state.httpdVersion, version) <= 0;
                } else {
                    LOG.warn("Unknown operator " + operator + " in an IfVersion directive.");
                    return null;
                }

                result.shouldRecurseIntoConditionalNode = hasVersion != negate;

                result.nodeIsConditional = true;
            } else {
                result.nodeIsConditional = false;
            }

            return result;
        }
    }

    /**
     * This is a node visitor interface to be implemented by the users of the 
     * {@link RuntimeApacheConfiguration#walkRuntimeConfig(ApacheAugeasTree, ProcessInfo, ApacheBinaryInfo, Map)}
     * or {@link RuntimeApacheConfiguration#walkRuntimeConfig(ApacheDirectiveTree, ProcessInfo, ApacheBinaryInfo, Map)}
     * methods.
     */
    public interface NodeVisitor<T> {

        /**
         * This method is called whenever the apache config tree walker encounters one of the If* directives (IfModule, IfDefine, IfVersion).
         * 
         * @param node the If* directive
         * @param isSatisfied true if the directive's condition is satisfied, false otherwise
         */
        void visitConditionalNode(T node, boolean isSatisfied);

        /**
         * This method is called for all "ordinary" directives that the apache config tree walker encounters (i.e. all but the ones handled by the {@link #visitConditionalNode(Object)}
         * method.
         * 
         * @param node the directive
         */
        void visitOrdinaryNode(T node);
    }

    /**
     * Extension of the {@link NodeVisitor} that is used internally to abstract out the
     * algorithm from the underlying data model.
     * There's just one transform method that walks any kind of apache config tree representation
     * and produces the runtime config. Different impls of this interface can
     * produce different "side-effects" of that walk.
     *
     * @author Lukas Krejci
     */
    private interface TreeWalker<T> extends NodeVisitor<T> {

        void onBeforeChildrenScan(T node);

        void onAfterChildrenScan(T node);

        Collection<T> getChildren(T node);

        String getValue(T node);

        List<String> getValues(T node);

        String getName(T node);
    }

    /**
     * Impl of {@link TreeWalker} interface that transforms the tree by replacing
     * the conditional directives that are satisfied with their "contents".
     * 
     * @author Lukas Krejci
     */
    private static class TransformingWalker implements TreeWalker<ApacheDirective> {

        private static class NodesToModify {
            ArrayList<ApacheDirective> nodesToRemove = new ArrayList<ApacheDirective>();
            ArrayList<ApacheDirective> nodesToPromote = new ArrayList<ApacheDirective>();
        }

        private Deque<NodesToModify> currentNodeStack = new ArrayDeque<NodesToModify>();

        public void visitConditionalNode(ApacheDirective node, boolean isSatisfied) {
            NodesToModify nodes = currentNodeStack.peek();
            if (isSatisfied) {
                nodes.nodesToPromote.add(node);
            } else {
                nodes.nodesToRemove.add(node);
            }
        }

        public void visitOrdinaryNode(ApacheDirective node) {
        }

        public void onBeforeChildrenScan(ApacheDirective node) {
            currentNodeStack.push(new NodesToModify());
        }

        public void onAfterChildrenScan(ApacheDirective parentNode) {
            NodesToModify nodes = currentNodeStack.pop();

            for (ApacheDirective node : nodes.nodesToRemove) {
                parentNode.getChildDirectives().remove(node);
            }

            //add the children of node as children of parent node at the place node
            //was declared and remove node ... i.e. make it so as if the child nodes
            //of node were directly in the parentNode in the place of node
            for (ApacheDirective node : nodes.nodesToPromote) {
                int nodeIdx = parentNode.getChildDirectives().indexOf(node);

                List<ApacheDirective> childNodes = node.getChildDirectives();
                for (int i = childNodes.size() - 1; i >= 0; --i) {
                    ApacheDirective childNode = childNodes.get(i);
                    parentNode.getChildDirectives().add(nodeIdx, childNode);
                    childNode.setParentNode(parentNode);
                }

                parentNode.getChildDirectives().remove(nodeIdx + childNodes.size());
            }
        }

        public Collection<ApacheDirective> getChildren(ApacheDirective node) {
            return node.getChildDirectives();
        }

        public String getValue(ApacheDirective node) {
            return node.getValuesAsString();
        }

        public List<String> getValues(ApacheDirective node) {
            return node.getValues();
        }

        public String getName(ApacheDirective node) {
            return node.getName();
        }
    }

    /**
     * This is a "wrapping" class for the number of parameters that are needed 
     * in the transform method.
     *
     * @author Lukas Krejci
     */
    private static class TransformState {
        public Set<String> loadedModules;
        public Set<String> defines;
        public Map<String, String> moduleNames;
        public Map<String, String> moduleFiles;
        public String httpdVersion;
        public boolean suppressUnknownModuleWarnings;

        public TransformState(ProcessInfo httpdProcessInfo, ApacheBinaryInfo httpdBinaryInfo,
            Map<String, String> moduleNames, boolean suppressUnknownModuleWarnings) {
            defines = new HashSet<String>(httpdBinaryInfo.getCompiledInDefines());

            if (httpdProcessInfo != null) {
                String[] args = httpdProcessInfo.getCommandLine();
                for (int i = 1; i < args.length; ++i) {
                    String define = null;
                    if (args[i] != null && args[i].startsWith("-D")) {
                        define = args[i].substring(2).trim();
                    }

                    if (define != null && define.isEmpty()) {
                        //this means we saw an empty -D arg. This can happen if there is a space between -D and the value.
                        //That is legal though, so we have to accomodate for that.
                        if (i < args.length - 1) {
                            define = args[i + 1].trim();
                            if (define.startsWith("-")) {
                                //this would be another option
                                define = null;
                            } else {
                                ++i; //we can skip the next arg
                            }
                        } else {
                            define = null; //well -D is the last argument
                        }
                    }

                    if (define != null) {
                        defines.add(define);
                    }
                }
            }

            loadedModules = new HashSet<String>();
            loadedModules.addAll(httpdBinaryInfo.getCompiledInModules());

            this.moduleNames = moduleNames;

            //build a map for reverse lookup we might need in the transform method
            moduleFiles = new HashMap<String, String>(moduleNames.size());
            for (Map.Entry<String, String> e : moduleNames.entrySet()) {
                moduleFiles.put(e.getValue(), e.getKey());
            }

            httpdVersion = httpdBinaryInfo.getVersion();

            this.suppressUnknownModuleWarnings = suppressUnknownModuleWarnings;
        }
    }

    public static NodeInspector getNodeInspector(ProcessInfo httpdProcessInfo, ApacheBinaryInfo httpdBinaryInfo,
        Map<String, String> moduleNames, boolean suppressUnknownModuleWarnings, boolean keepConditional) {
        return new NodeInspector(new TransformState(httpdProcessInfo, httpdBinaryInfo, moduleNames,
            suppressUnknownModuleWarnings), keepConditional);
    }

    /**
     * Given the apache configuration and information about the parameters httpd was executed
     * with this method provides the directive tree that corresponds to the actual
     * runtime configuration as used by httpd.
     * <p>
     * This enables us to see which directives are actually in effect as opposed to just
     * declared.
     *
     * @param tree
     * @param httpdProcessInfo
     * @param httpdBinaryInfo
     * @param moduleNames the mapping from the module filename to the module name
     * (i.e. mapping from the name used in IfModule to the name used in LoadModule)
     * @param suppressUnknownModuleWarnings true if the method should suppress logging the warnings about unknown modules
     * @return a new directive tree that represents the runtime configuration 
     */
    public static ApacheDirectiveTree extract(ApacheDirectiveTree tree, ProcessInfo httpdProcessInfo,
        ApacheBinaryInfo httpdBinaryInfo, Map<String, String> moduleNames, boolean suppressUnknownModuleWarnings) {
        ApacheDirectiveTree ret = tree.clone();
        transform(new TransformingWalker(), ret.getRootNode(),
            getNodeInspector(httpdProcessInfo, httpdBinaryInfo, moduleNames, suppressUnknownModuleWarnings, false));

        return ret;
    }

    public static void walkRuntimeConfig(final NodeVisitor<ApacheDirective> visitor, ApacheDirectiveTree tree,
        ProcessInfo httpdProcessInfo, ApacheBinaryInfo httpdBinaryInfo, Map<String, String> moduleNames,
        boolean suppressUnknownModuleWarnings) {
        TreeWalker<ApacheDirective> walker = new TreeWalker<ApacheDirective>() {
            public void visitConditionalNode(ApacheDirective node, boolean isSatisfied) {
                visitor.visitConditionalNode(node, isSatisfied);
            }

            public void visitOrdinaryNode(ApacheDirective node) {
                visitor.visitOrdinaryNode(node);
            }

            public void onBeforeChildrenScan(ApacheDirective node) {
            }

            public void onAfterChildrenScan(ApacheDirective node) {
            }

            public Collection<ApacheDirective> getChildren(ApacheDirective node) {
                return node.getChildDirectives();
            }

            public String getValue(ApacheDirective node) {
                return node.getValuesAsString();
            }

            public List<String> getValues(ApacheDirective node) {
                return node.getValues();
            }

            public String getName(ApacheDirective node) {
                return node.getName();
            }
        };

        transform(walker, tree.getRootNode(),
            getNodeInspector(httpdProcessInfo, httpdBinaryInfo, moduleNames, suppressUnknownModuleWarnings, false));
    }

    public static void walkRuntimeConfig(final NodeVisitor<AugeasNode> visitor, AugeasTree tree,
        ProcessInfo httpdProcessInfo, ApacheBinaryInfo httpdBinaryInfo, Map<String, String> moduleNames,
        boolean suppressUnknownModuleWarnings) {
        TreeWalker<AugeasNode> walker = new TreeWalker<AugeasNode>() {
            public void visitConditionalNode(AugeasNode node, boolean isSatisfied) {
                visitor.visitConditionalNode(node, isSatisfied);
            }

            public void visitOrdinaryNode(AugeasNode node) {
                visitor.visitOrdinaryNode(node);
            }

            public void onBeforeChildrenScan(AugeasNode node) {
            }

            public void onAfterChildrenScan(AugeasNode node) {
            }

            public Collection<AugeasNode> getChildren(AugeasNode node) {
                return node.getChildNodes();
            }

            public String getValue(AugeasNode node) {
                StringBuilder bld = new StringBuilder();
                for (String val : getValues(node)) {
                    bld.append(val);
                }
                return bld.toString();
            }

            public List<String> getValues(AugeasNode node) {
                ArrayList<String> ret = new ArrayList<String>();

                List<AugeasNode> params = node.getChildByLabel("param");

                for (AugeasNode n : params) {
                    ret.add(n.getValue());
                }
                return ret;
            }

            public String getName(AugeasNode node) {
                return node.getLabel();
            }
        };
        transform(walker, tree.getRootNode(),
            getNodeInspector(httpdProcessInfo, httpdBinaryInfo, moduleNames, suppressUnknownModuleWarnings, false));
    }

    private static <T> void transform(TreeWalker<T> walker, T parentNode, NodeInspector inspector) {
        if (walker.getChildren(parentNode).isEmpty()) {
            return;
        }

        walker.onBeforeChildrenScan(parentNode);

        for (T node : walker.getChildren(parentNode)) {
            NodeInspectionResult result =
                inspector.inspect(walker.getName(node), walker.getValues(node), walker.getValue(node));
            if (result == null) {
                continue;
            }
            if (!result.nodeIsConditional) {
                walker.visitOrdinaryNode(node);
            } else {
                walker.visitConditionalNode(node, result.shouldRecurseIntoConditionalNode);
                if (result.shouldRecurseIntoConditionalNode) {
                    transform(walker, node, inspector);
                }
            }
        }
        walker.onAfterChildrenScan(parentNode);
    }

    private static ModuleLoadedState isModuleLoaded(String moduleIdentifier, Set<String> currentlyLoadedModules,
        Map<String, String> moduleNames, Map<String, String> moduleFiles) {
        String moduleName = moduleNames.get(moduleIdentifier);
        if (moduleName == null) {
            //as of apache 2.1 module files and module names can both be used in IfModule
            moduleName = moduleIdentifier;
            moduleIdentifier = moduleFiles.get(moduleName);

            if (moduleIdentifier == null) {
                //reverse lookup failed - there is no such module in the mappings
                //we still have 2 options here.
                //If the identifier we were given is a module name, we can assume that
                //that module just wasn't loaded if we can't find it in the loaded modules set. 
                //If on the other hand the identifier is a module source file, we have no other 
                //option but to give up, because we don't know the mapping from module name to 
                //module source file and thus cannot determine whether there was a LoadModule 
                //directive that would load the module.
                if (moduleName.endsWith(".c")) {
                    return ModuleLoadedState.UNKNOWN;
                } else {
                    return currentlyLoadedModules.contains(moduleName) ? ModuleLoadedState.LOADED
                        : ModuleLoadedState.NOT_LOADED;
                }
            }
        }

        //the compiled in modules are being reported by apache using their source file
        //and the on-demand loaded modules are identified by their 
        //module name - consistent, huh?
        boolean result =
            currentlyLoadedModules.contains(moduleIdentifier) || currentlyLoadedModules.contains(moduleName);

        return result ? ModuleLoadedState.LOADED : ModuleLoadedState.NOT_LOADED;
    }
}
