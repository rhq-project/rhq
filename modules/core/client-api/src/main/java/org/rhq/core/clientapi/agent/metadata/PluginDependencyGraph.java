/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.core.clientapi.agent.metadata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class determines the deployment order for plugins by building the dependency graph of the plugins. You use this
 * class by first {@link #addPlugin(String, List) adding} plugins to the graph, and then when all plugins have been
 * added you can get the {@link #getDeploymentOrder() deployment order} that lists all the plugins in the order in which
 * they should be deployed.
 *
 * <p>Note that circular dependencies are NOT allowed nor supported.</p>
 *
 * @author John Mazzitelli
 */
public class PluginDependencyGraph {
    private static final Log log = LogFactory.getLog(PluginDependencyGraph.class);
    /**
     * Keyed on plugin name with the values of their dependencies (which are other plugin names). The values must never
     * be null - if there are no dependencies, an empty list will exist.
     */
    private Map<String, List<PluginDependency>> dependencyMap = new HashMap<String, List<PluginDependency>>();

    /**
     * Adds a plugin to the graph. The plugin names in the dependencies must match names of other plugins that were,
     * or will be, added to this graph.
     *
     * <p>If the plugin already exists, it will be overridden such that the given dependencies replace its old ones.</p>
     *
     * @param pluginName   the name of the plugin getting added to the graph
     * @param dependencies list of plugin names that are dependencies of this plugin
     */
    public void addPlugin(String pluginName, List<PluginDependency> dependencies) {
        // it doesn't make sense that a plugin depends on itself.
        // remove duplicates to avoid erroneous circular dependencies.
        dependencies.remove(new PluginDependency(pluginName, false, false));
        dependencyMap.put(pluginName, dependencies);
    }

    /**
     * Returns the name of the plugin who's classloader will be used as the parent of this plugin. If none is explicitly
     * declared, the last one in the dependency list will be used.
     * 
     * @param the name of the plugin whose required dependency contains classes that are to be accessible by the plugin
     * @return the required dependency for the given plugin, or <code>null</code> if there is no required dependency
     */
    public String getUseClassesDependency(String pluginName) {
        PluginDependency last = null;
        if (this.dependencyMap.containsKey(pluginName)) {
            for (PluginDependency dependency : this.dependencyMap.get(pluginName)) {
                // only required deps can have their classes used - optional deps cannot (since classes may not exist)
                if (dependency.required) {
                    if (dependency.useClasses) {
                        return dependency.name; // classes from only one dep can be used, so we only return one
                    }
                    last = dependency;
                }
            }
        }
        return (last == null) ? null : last.name;
    }

    /**
     * Returns the set of plugin names that have been added to the dependency graph.
     *
     * @return set of all plugin names that were added to this graph
     */
    public Set<String> getPlugins() {
        return new HashSet<String>(dependencyMap.keySet());
    }

    /**
     * Given a plugin that is in this dependency graph, this will return the list of its direct dependencies (the
     * plugins this plugin explicitly depends on). The list will be empty if there are no dependencies or the
     * plugin does not exist in this graph.
     *
     * Note that if a dependency is not required to exist, and it does not exist, it will not be returned
     * in the list.  This is to say that if a plugin was configured to depend on another plugin, but that
     * dependency was not required, that other plugin will not be in the returned list if it hasn't been added
     * to this graph yet.
     * 
     * @param  pluginName the plugin name
     *
     * @return list of plugin dependencies
     */
    public List<String> getPluginDependencies(String pluginName) {
        List<String> dependencies = new ArrayList<String>();
        for (PluginDependency dependency : this.dependencyMap.get(pluginName)) {
            if (dependency.required || this.dependencyMap.containsKey(dependency.name)) {
                dependencies.add(dependency.name);
            }
        }

        return dependencies;
    }

    /**
     * Given a plugin that is in this dependency graph, this will return all those plugins
     * that <i>optionally</i> depend on it. If a plugin has a required dependency on
     * the given plugin, or a plugin does not depend on the given plugin at all, that plugin
     * will not be in the returned list.
     * 
     * @param pluginName the plugin whose dependents are to be returned
     * 
     * @return list of all plugins that optionally depend on the given plugin
     */
    public List<String> getOptionalDependents(String pluginName) {
        List<String> dependents = new ArrayList<String>();
        for (Map.Entry<String, List<PluginDependency>> entry : this.dependencyMap.entrySet()) {
            if (entry.getKey().equals(pluginName)) {
                continue; // don't bother examining the plugin itself
            }

            // see if current plugin optionally depends on the given pluginName, if so, add it to the list
            for (PluginDependency dependency : entry.getValue()) {
                if (!dependency.required && dependency.name.equals(pluginName)) {
                    dependents.add(entry.getKey());
                    break;
                }
            }
        }
        return dependents;
    }

    /**
     * Given a plugin that is in this dependency graph, this will return all those plugins
     * the plugin either directly or indirectly depends on it. This is different
     * than {@link #getPluginDependencies(String)} because this method does a deep
     * dive and returns all direct dependencies and dependencies of those dependencies.
     * 
     * @param pluginName the plugin whose dependencies are to be returned
     * 
     * @return list of all plugins that the given plugin depends on
     */
    public Collection<String> getAllDependencies(String pluginName) {
        if (this.dependencyMap.containsKey(pluginName)) {
            return getDeepDependencies(pluginName, new ArrayList<String>(), true);
        } else {
            return new HashSet<String>();
        }
    }

    /**
     * Given a plugin that is in this dependency graph, this will return all those plugins
     * that either directly or indirectly depend on it (both optional and required dependencies).
     * 
     * @param pluginName the plugin whose dependents are to be returned
     * 
     * @return list of all plugins that depend on the given plugin
     */
    public Collection<String> getAllDependents(String pluginName) {
        Set<String> dependents = new HashSet<String>();
        for (Map.Entry<String, List<PluginDependency>> entry : this.dependencyMap.entrySet()) {
            if (entry.getKey().equals(pluginName)) {
                continue; // don't bother examining the plugin itself
            }

            // see if current plugin depends on the given pluginName, if so, add it to the list
            for (PluginDependency dependency : entry.getValue()) {
                if (dependency.name.equals(pluginName)) {
                    dependents.addAll(getAllDependents(entry.getKey()));
                    dependents.add(entry.getKey());
                    break;
                }
            }
        }
        return dependents;
    }

    /**
     * Returns <code>true</code> if the dependency graph has no missing required plugins.
     * That is to say, all required dependencies of all plugins can be found in this graph. If this returns <code>true</code>,
     * you can safely call {@link #getDeploymentOrder()} and expect it to return an ordered list of plugins.
     * This will return <code>false</code> if one or more required dependencies are missing and still need to be
     * {@link #addPlugin(String, List) added}. This will throw an exception if a circular dependency has been
     * detected.
     *
     * @param  errorBuffer if not <code>null</code> and this method returns <code>false</code>, this will be appended
     *                     with the error message that will contain information on the first plugin found to be missing
     *
     * @return <code>true</code> if there are no missing dependencies and {@link #getDeploymentOrder()} can be called
     *
     * @throws IllegalStateException if a circular dependency has been detected
     */
    public boolean isComplete(StringBuilder errorBuffer) throws IllegalStateException {
        try {
            getDeploymentOrder();
            return true;
        } catch (IllegalArgumentException e) {
            if (errorBuffer != null) {
                errorBuffer.append(e.getMessage());
            }

            return false;
        }
    }

    /**
     * Returns the deployment order for all added plugins. If a required dependency is missing and thus one or
     * more plugins cannot be deployed, an exception is thrown. If an optional dependency is missing, that
     * optional dependency plugin will be ignored and not returned in the list.
     *
     * @return the list of plugin names, in the order in which they can be deployed.
     *
     * @throws IllegalStateException    if a circular dependency has been detected
     * @throws IllegalArgumentException if one or more plugins depend on other plugins that are missing from the graph
     */
    public List<String> getDeploymentOrder() throws IllegalStateException, IllegalArgumentException {
        List<PluginItem> pluginItems = new ArrayList<PluginItem>();

        // Compute the deep dependencies so we know all the plugins that must be deployed before each plugin.
        // We use TreeSet so we can be able to predict the resulting order based on alphabetic ordering of plugins (mainly for tests)
        for (String pluginName : new TreeSet<String>(dependencyMap.keySet())) {
            pluginItems.add(new PluginItem(pluginName, getDeepDependencies(pluginName, new ArrayList<String>(), true)));
        }

        // got through each plugin and put it in the returned list such that it appears
        // as far in the front of the list as it can, but not before any of its dependencies
        List<String> retList = new ArrayList<String>(pluginItems.size());
        for (PluginItem pluginItem : pluginItems) {
            int insertIndex = 0;

            for (String dependency : pluginItem.deepDependencies) {
                int dependencyIndex = retList.indexOf(dependency);
                if ((dependencyIndex > -1) && (insertIndex < (dependencyIndex + 1))) {
                    insertIndex = dependencyIndex + 1;
                }
            }

            retList.add(insertIndex, pluginItem.name);
        }

        return retList;
    }

    /**
     * If the current dependency graph is not yet {@link #isComplete(StringBuilder) complete}, you can call
     * this method to reduce the graph such that plugins with missing required dependencies are removed and
     * only those plugins whose dependencies exist are in the returned graph. In other words, this method will
     * return a dependency graph that is guaranteed to be complete and return a
     * {@link #getDeploymentOrder()} - albeit with only those plugins that currently have all dependencies.
     * 
     * @return a reduced graph that contains only those plugins that have all their dependencies
     */
    public PluginDependencyGraph reduceGraph() {
        PluginDependencyGraph reducedGraph = new PluginDependencyGraph();

        // Compute the deep dependencies so we know all the plugins that must be deployed before each plugin.
        for (String pluginName : new TreeSet<String>(dependencyMap.keySet())) {
            try {
                getDeepDependencies(pluginName, new ArrayList<String>(), true); // throws exception if not complete
                reducedGraph.addPlugin(pluginName, this.dependencyMap.get(pluginName));
            } catch (Exception e) {
                log.info("Reducing dependency graph by not including plugin [" + pluginName + "]. Cause: " + e);
            }
        }

        return reducedGraph;
    }

    public String toString() {
        StringBuffer str = new StringBuffer("Plugin dependency graph:");

        for (Map.Entry<String, List<PluginDependency>> entry : dependencyMap.entrySet()) {
            str.append("\n");
            str.append(entry.getKey());
            str.append(":");
            str.append(entry.getValue());
        }

        return str.toString();
    }

    /**
     * Given a known plugin name, this returns all dependencies of that plugin (including those dependencies of its
     * dependencies, down N levels). If a dependency is missing but is required, an exception is thrown - missing
     * optional plugins are simply ignored and not returned in the set but otherwise no errors occur.
     *
     * @param pluginName
     * @param dependingPlugins set of plugins that are known to be depending on pluginName (must not be <code>
     *                         null</code>)
     * @param required if <code>true</code>, then <code>pluginName</code> must exist in the graph. If it does not, an
     *                 exception will be thrown. Otherwise, it is considered an optional plugin and if it is missing,
     *                 it will be ignored.
     *
     * @return the dependencies
     *
     * @throws IllegalStateException    if the given plugin has a circular dependency
     * @throws IllegalArgumentException if the plugin hasn't been added to the graph yet
     */
    private Set<String> getDeepDependencies(String pluginName, Collection<String> dependingPlugins, boolean required)
        throws IllegalStateException, IllegalArgumentException {
        HashSet<String> results = new HashSet<String>();

        List<PluginDependency> childDependencies = dependencyMap.get(pluginName);
        if (childDependencies == null) {
            if (required) {
                throw new IllegalArgumentException("Plugin [" + pluginName + "] is required by plugins ["
                    + dependingPlugins + "] but it does not exist in the dependency graph yet");
            }

            log.info("Optional plugin [" + pluginName + "] was requested by plugins [" + dependingPlugins
                + "] but it does not exist in the dependency graph yet and will be ignored");
        } else {
            for (PluginDependency childDependency : childDependencies) {
                if (dependingPlugins.contains(childDependency.name)) {
                    throw createCircularDependencyException(childDependency.name);
                }

                dependingPlugins.add(pluginName);
                Set<String> childDeepDependencies = getDeepDependencies(childDependency.name, dependingPlugins,
                    childDependency.required);
                dependingPlugins.remove(pluginName);

                results.add(childDependency.name);
                results.addAll(childDeepDependencies);
            }
        }

        return results;
    }

    private IllegalStateException createCircularDependencyException(String badPlugin) {
        StringBuffer err = new StringBuffer("Circular dependency detected in plugins!\n");
        err.append("Plugin with the circular dependency is [" + badPlugin + "]\n");
        err.append("Circular dependency path is [");
        err.append(getCircularDependencyString(badPlugin, new ArrayList<String>()));
        err.append("]\n");
        err.append(toString());

        return new IllegalStateException(err.toString());
    }

    private String getCircularDependencyString(String startPlugin, List<String> path) {
        boolean gotIt = path.contains(startPlugin);

        if (!gotIt) {
            path.add(startPlugin);
            List<PluginDependency> deps = dependencyMap.get(startPlugin);
            for (PluginDependency dep : deps) {
                List<String> tmpPath = new ArrayList<String>(path);
                String str = getCircularDependencyString(dep.name, tmpPath);
                if (str != null) {
                    return str;
                }
            }

            return null;
        }

        StringBuffer retPath = new StringBuffer();
        for (String pathElement : path) {
            retPath.append(pathElement);
            retPath.append("->");
        }

        retPath.append(startPlugin);
        path.add(startPlugin);

        return retPath.toString();
    }

    /**
     * Used to properly sort our dependencies in a tree map.
     */
    private class PluginItem {
        final String name;
        final Set<String> deepDependencies;

        PluginItem(String name, Set<String> deepDependencies) {
            this.name = name;
            this.deepDependencies = deepDependencies;
        }

        public String toString() {
            return this.name + ':' + this.deepDependencies;
        }
    }

    public static class PluginDependency {
        final String name;
        final boolean useClasses;
        final boolean required;

        public PluginDependency(String name) {
            this(name, false, false);
        }

        public PluginDependency(String name, boolean useClasses, boolean required) {
            this.name = name;
            this.useClasses = useClasses;
            this.required = required;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if ((o == null) || (getClass() != o.getClass())) {
                return false;
            }

            PluginDependency that = (PluginDependency) o;

            if (!name.equals(that.name)) {
                return false;
            }

            return true;
        }

        public int hashCode() {
            return name.hashCode();
        }

        public String toString() {
            return "name=[" + this.name + "], required=[" + this.required + "], useClasses=[" + this.useClasses + "]";
        }
    }
}