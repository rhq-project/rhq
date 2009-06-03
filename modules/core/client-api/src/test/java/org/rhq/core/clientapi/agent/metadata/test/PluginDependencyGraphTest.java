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
package org.rhq.core.clientapi.agent.metadata.test;

import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

import org.rhq.core.clientapi.agent.metadata.PluginDependencyGraph;
import org.rhq.core.clientapi.agent.metadata.PluginDependencyGraph.PluginDependency;

/**
 * Test PluginDependencyGraph.
 *
 * @author John Mazzitelli
 */
@Test
public class PluginDependencyGraphTest {
    public void testOptionalPluginsDeployment() {
        PluginDependencyGraph graph;
        List<String> order;

        // let's assume we have a dependency graph like this:
        //    plugin A depends on plugin B (required)
        //    plugin B depends on plugin D and C (required)
        //    plugin C depends on plugin E and F (optional)
        //    plugin D depends on plugin E and F (optional)
        //    plugin E does not depend on any other plugin
        //    plugin F does not depend on any other plugin
        //    plugin G depends on plugin F (required)
        //    plugin Z does not depend on any other plugin
        // the deployment order should be: Z F G E D C B A

        graph = new PluginDependencyGraph();
        addPlugin(graph, "A", "B");
        addPlugin(graph, "B", "D", "C");
        addPluginWithOptionalDeps(graph, "C", "E", "F");
        addPluginWithOptionalDeps(graph, "D", "E", "F");
        addPlugin(graph, "E");
        addPlugin(graph, "F");
        addPlugin(graph, "G", "F");
        addPlugin(graph, "Z");
        assert graph.isComplete(null);
        order = graph.getDeploymentOrder();
        assert order.get(0).equals("Z") : order;
        assert order.get(1).equals("F") : order;
        assert order.get(2).equals("G") : order;
        assert order.get(3).equals("E") : order;
        assert order.get(4).equals("D") : order;
        assert order.get(5).equals("C") : order;
        assert order.get(6).equals("B") : order;
        assert order.get(7).equals("A") : order;

        // Use the same dependency graph, but do not deploy plugin F.
        // With F missing, G will fail because it required F
        graph = new PluginDependencyGraph();
        addPlugin(graph, "A", "B");
        addPlugin(graph, "B", "D", "C");
        addPluginWithOptionalDeps(graph, "C", "E", "F");
        addPluginWithOptionalDeps(graph, "D", "E", "F");
        addPlugin(graph, "E");
        //addPlugin(graph, "F"); PLUGIN F IS GOING TO BE MISSING FROM THIS GRAPH!
        addPlugin(graph, "G", "F");
        addPlugin(graph, "Z");
        assert !graph.isComplete(null) : "Plugin F was missing, so G should have failed";

        // Use the same dependency graph, but do not deploy plugin F AND make G optionally depend on F.
        // With F missing, but all dependencies on it being optional, this graph should be complete.
        // The deployment order in this case should be: Z G E D C B A
        graph = new PluginDependencyGraph();
        addPlugin(graph, "A", "B");
        addPlugin(graph, "B", "D", "C");
        addPluginWithOptionalDeps(graph, "C", "E", "F");
        addPluginWithOptionalDeps(graph, "D", "E", "F");
        addPlugin(graph, "E");
        //addPlugin(graph, "F"); PLUGIN F IS GOING TO BE MISSING FROM THIS GRAPH!
        addPluginWithOptionalDeps(graph, "G", "F"); // G is optionally dependent on F
        addPlugin(graph, "Z");
        assert graph.isComplete(null);
        order = graph.getDeploymentOrder();
        assert order.get(0).equals("Z") : order;
        assert order.get(1).equals("G") : order;
        assert order.get(2).equals("E") : order;
        assert order.get(3).equals("D") : order;
        assert order.get(4).equals("C") : order;
        assert order.get(5).equals("B") : order;
        assert order.get(6).equals("A") : order;
    }

    public void testTypicalDeployment() {
        PluginDependencyGraph graph;
        List<String> order;

        graph = new PluginDependencyGraph();
        addPlugin(graph, "apache");
        addPlugin(graph, "Platforms");
        addPlugin(graph, "JBossAS", "JMX", "Tomcat");
        addPlugin(graph, "Tomcat", "JMX");
        addPlugin(graph, "Hibernate", "JMX");
        addPlugin(graph, "JMX");
        addPlugin(graph, "JONAgent");

        assert graph.isComplete(null);
        order = graph.getDeploymentOrder();

        assert order.indexOf("JMX") < order.indexOf("JBossAS") : order;
        assert order.indexOf("JMX") < order.indexOf("Tomcat") : order;
        assert order.indexOf("JMX") < order.indexOf("Hibernate") : order;
        assert order.indexOf("Tomcat") < order.indexOf("JBossAS") : order;
        assert graph.getPlugins().size() == 7 : graph;
        assert order.size() == 7 : order;
    }

    public void testCustomPluginDependsOnAgentGraphWithOthers() {
        PluginDependencyGraph graph;
        List<String> order;

        graph = new PluginDependencyGraph();
        addPlugin(graph, "apache");
        addPlugin(graph, "Platforms");
        addPlugin(graph, "CustomPlugin", "JONAgent");
        addPlugin(graph, "JMX");
        addPlugin(graph, "JONAgent");

        assert graph.isComplete(null);
        order = graph.getDeploymentOrder();

        assert order.indexOf("JONAgent") < order.indexOf("CustomPlugin") : order;
        assert graph.getPlugins().size() == 5 : graph;
        assert order.size() == 5 : order;
    }

    public void testAnotherDependencyGraph() {
        PluginDependencyGraph graph;
        List<String> order;

        graph = new PluginDependencyGraph();
        addPlugin(graph, "CustomPlugin", "JONAgent");
        addPlugin(graph, "JMX");
        addPlugin(graph, "JONAgent");

        assert graph.isComplete(null);
        order = graph.getDeploymentOrder();

        assert order.indexOf("JONAgent") < order.indexOf("CustomPlugin") : order;
        assert order.size() == 3 : order;
        assert graph.getPlugins().size() == 3 : graph;
    }

    public void testSimpleGraph() {
        PluginDependencyGraph graph;
        List<String> order;

        // a graph with no plugins that have any dependencies
        graph = new PluginDependencyGraph();
        addPlugin(graph, "A");
        addPlugin(graph, "B");
        addPlugin(graph, "C");
        assert graph.isComplete(null);
        order = graph.getDeploymentOrder();
        assert order.contains("A") : order;
        assert order.contains("B") : order;
        assert order.contains("C") : order;

        // a graph with no plugins that have any dependencies is ordered by plugin name
        graph = new PluginDependencyGraph();
        addPlugin(graph, "C");
        addPlugin(graph, "A");
        addPlugin(graph, "B");
        assert graph.isComplete(null);
        order = graph.getDeploymentOrder();
        assert order.contains("A") : order;
        assert order.contains("B") : order;
        assert order.contains("C") : order;
    }

    public void testDependencyGraph() {
        PluginDependencyGraph graph;
        List<String> order;

        // let's assume we have a dependency graph like this:
        //    plugin A depends on plugin B
        //    plugin B depends on plugin C
        //    plugin C does not depend on any other plugin
        // the deployment order should be: C B A

        graph = new PluginDependencyGraph();
        addPlugin(graph, "A", "B");
        addPlugin(graph, "B", "C");
        addPlugin(graph, "C");
        assert graph.isComplete(null);
        order = graph.getDeploymentOrder();
        assert order.get(0).equals("C") : order;
        assert order.get(1).equals("B") : order;
        assert order.get(2).equals("A") : order;

        // add them in a different order and see the dependency order doesn't change
        graph = new PluginDependencyGraph();
        addPlugin(graph, "C");
        addPlugin(graph, "B", "C");
        addPlugin(graph, "A", "B");
        assert graph.isComplete(null);
        order = graph.getDeploymentOrder();
        assert order.get(0).equals("C") : order;
        assert order.get(1).equals("B") : order;
        assert order.get(2).equals("A") : order;

        // add them in a different order and see the dependency order doesn't change
        graph = new PluginDependencyGraph();
        addPlugin(graph, "B", "C");
        addPlugin(graph, "C");
        addPlugin(graph, "A", "B");
        assert graph.isComplete(null);
        order = graph.getDeploymentOrder();
        assert order.get(0).equals("C") : order;
        assert order.get(1).equals("B") : order;
        assert order.get(2).equals("A") : order;
    }

    public void testComplexDependencyGraph() {
        PluginDependencyGraph graph;
        List<String> order;

        // let's assume we have a dependency graph like this:
        //    plugin A depends on plugin B
        //    plugin B depends on plugin D and C
        //    plugin C depends on plugin E and F
        //    plugin D depends on plugin E and F
        //    plugin E does not depend on any other plugin
        //    plugin F does not depend on any other plugin
        //    plugin G depends on plugin F
        //    plugin Z does not depend on any other plugin
        // the deployment order should be: Z F G E D C B A

        graph = new PluginDependencyGraph();
        addPlugin(graph, "A", "B");
        addPlugin(graph, "B", "D", "C");
        addPlugin(graph, "C", "E", "F");
        addPlugin(graph, "D", "E", "F");
        addPlugin(graph, "E");
        addPlugin(graph, "F");
        addPlugin(graph, "G", "F");
        addPlugin(graph, "Z");
        assert graph.isComplete(null);
        order = graph.getDeploymentOrder();
        assert order.get(0).equals("Z") : order;
        assert order.get(1).equals("F") : order;
        assert order.get(2).equals("G") : order;
        assert order.get(3).equals("E") : order;
        assert order.get(4).equals("D") : order;
        assert order.get(5).equals("C") : order;
        assert order.get(6).equals("B") : order;
        assert order.get(7).equals("A") : order;

        // add them in a different order and see the dependency order doesn't change
        graph = new PluginDependencyGraph();
        graph = new PluginDependencyGraph();
        addPlugin(graph, "E");
        addPlugin(graph, "D", "E", "F");
        addPlugin(graph, "B", "C", "D");
        addPlugin(graph, "A", "B");
        addPlugin(graph, "G", "F");
        addPlugin(graph, "Z");
        addPlugin(graph, "F");
        addPlugin(graph, "C", "E", "F");
        assert graph.isComplete(null);
        order = graph.getDeploymentOrder();
        assert order.get(0).equals("Z") : order;
        assert order.get(1).equals("F") : order;
        assert order.get(2).equals("G") : order;
        assert order.get(3).equals("E") : order;
        assert order.get(4).equals("D") : order;
        assert order.get(5).equals("C") : order;
        assert order.get(6).equals("B") : order;
        assert order.get(7).equals("A") : order;
    }

    /* we allow bad graphs now
    public void testBadGraph() {
        PluginDependencyGraph graph = new PluginDependencyGraph();
        StringBuilder error = new StringBuilder();

        addPlugin(graph, "A", "B");
        assert graph.getPlugins().size() == 1;
        assert graph.getPlugins().contains("A");
        assert !graph.isComplete(error);
        assert error.indexOf("[B]") > -1;

        try {
            graph.getDeploymentOrder();
            assert false : "The deployment isn't possible yet - missing plugin B";
        } catch (IllegalArgumentException expected) {
        }

        addPlugin(graph, "B", "C");
        assert graph.getPlugins().size() == 2;
        assert graph.getPlugins().contains("A");
        assert graph.getPlugins().contains("B");
        error.setLength(0);
        assert !graph.isComplete(error);
        assert error.indexOf("[C]") > -1;

        try {
            graph.getDeploymentOrder();
            assert false : "The deployment isn't possible yet - missing plugin C";
        } catch (IllegalArgumentException expected) {
        }

        addPlugin(graph, "C"); // this completes the dependency graph
        assert graph.getPlugins().size() == 3;
        assert graph.getPlugins().contains("A");
        assert graph.getPlugins().contains("B");
        assert graph.getPlugins().contains("C");
        error.setLength(0);
        assert graph.isComplete(error);
        assert error.length() == 0;

        List<String> order = graph.getDeploymentOrder();
        assert order.get(0).equals("C") : order;
        assert order.get(1).equals("B") : order;
        assert order.get(2).equals("A") : order;
    }
    */

    public void testCatchCircularDependency() {
        PluginDependencyGraph graph;

        // try to add the following to the graph, which should fail due to the circular dependency
        // Plugin A depends on Plugin B
        // Plugin B depends on Plugin A

        graph = new PluginDependencyGraph();

        addPlugin(graph, "A", "B");
        addPlugin(graph, "B", "A");
        assert graph.getPlugins().size() == 2;
        assertCircularDependency(graph);

        // try to add the following to the graph, which should fail due to the circular dependency
        // Plugin A depends on Plugin B
        // Plugin B depends on Plugin C
        // Plugin C depends on Plugin A

        graph = new PluginDependencyGraph();

        addPlugin(graph, "A", "B");
        addPlugin(graph, "B", "C");
        addPlugin(graph, "C", "A");
        assert graph.getPlugins().size() == 3;
        assertCircularDependency(graph);

        // try to add the following to the graph, which should fail due to the circular dependency
        // Plugin A depends on Plugin B
        // Plugin B depends on Plugin C
        // Plugin C depends on Plugin D
        // Plugin D depends on Plugin B

        graph = new PluginDependencyGraph();

        addPlugin(graph, "A", "B");
        addPlugin(graph, "B", "C");
        addPlugin(graph, "C", "D");
        addPlugin(graph, "D", "B");
        assert graph.getPlugins().size() == 4;
        assertCircularDependency(graph);

        // try to add the following to the graph, which should fail due to the circular dependency
        // Plugin A depends on Plugin B, C
        // Plugin B depends on Plugin C, D, E
        // Plugin C depends on nothing
        // Plugin D depends on Plugin F, G
        // Plugin E depends on Plugin F
        // Plugin F depends on Plugin H
        // Plugin G depends on Plugin A // circular! D->G->A->B->D
        // Plugin H depends on nothing

        graph = new PluginDependencyGraph();

        addPlugin(graph, "A", "B", "C");
        addPlugin(graph, "B", "C", "D", "E");
        addPlugin(graph, "C");
        addPlugin(graph, "D", "F", "G");
        addPlugin(graph, "E", "F");
        addPlugin(graph, "F", "H");
        addPlugin(graph, "G"); // let's first see this work
        addPlugin(graph, "H");
        assert graph.getPlugins().size() == 8;
        assert graph.isComplete(null);
        addPlugin(graph, "G", "A"); // now blow it up
        assertCircularDependency(graph);
    }

    private void assertCircularDependency(PluginDependencyGraph graph) {
        try {
            graph.getDeploymentOrder();
            assert false : "The deployment isn't possible yet - there is a circular dependency that should have been caught";
        } catch (IllegalStateException expected) {
        }

        try {
            graph.isComplete(null);
            assert false : "The deployment isn't possible yet - there is a circular dependency that should have been caught";
        } catch (IllegalStateException expected) {
        }
    }

    private void addPlugin(PluginDependencyGraph graph, String pluginName, String... dependencyNames) {
        List<PluginDependency> dependencies = new ArrayList<PluginDependency>();
        for (String name : dependencyNames) {
            dependencies.add(new PluginDependency(name, false, true));
        }

        graph.addPlugin(pluginName, dependencies);
    }

    private void addPluginWithOptionalDeps(PluginDependencyGraph graph, String pluginName, String... dependencyNames) {
        List<PluginDependency> dependencies = new ArrayList<PluginDependency>();
        for (String name : dependencyNames) {
            dependencies.add(new PluginDependency(name, false, false));
        }

        graph.addPlugin(pluginName, dependencies);
    }
}