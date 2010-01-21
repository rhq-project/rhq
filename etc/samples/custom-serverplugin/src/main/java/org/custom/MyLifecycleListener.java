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

package org.custom;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.enterprise.server.plugin.pc.ControlFacet;
import org.rhq.enterprise.server.plugin.pc.ControlResults;
import org.rhq.enterprise.server.plugin.pc.ScheduledJobInvocationContext;
import org.rhq.enterprise.server.plugin.pc.ServerPluginComponent;
import org.rhq.enterprise.server.plugin.pc.ServerPluginContext;

/**
 * A sample lifecycle listener for the sample generic plugin. This listener will be
 * the main plugin component the server uses to start and stop the plugin.
 */
public class MyLifecycleListener implements ServerPluginComponent, ControlFacet {

    private ServerPluginContext context;

    public void initialize(ServerPluginContext context) throws Exception {
        this.context = context;
        System.out.println("The sample plugin has been initialized!!! : " + this);
    }

    public void start() {
        System.out.println("The sample plugin has started!!! : " + this);
    }

    public void stop() {
        System.out.println("The sample plugin has stopped!!! : " + this);
    }

    public void shutdown() {
        System.out.println("The sample plugin has been shut down!!! : " + this);
    }

    public ControlResults invoke(String name, Configuration parameters) {
        ControlResults controlResults = new ControlResults();
        if (name.equals("testControl")) {
            String paramProp = parameters.getSimple("paramProp").getStringValue();
            if (paramProp.equals("fail")) {
                controlResults.setError("simulated failure!");
            } else {
                controlResults.getComplexResults().put(
                    new PropertySimple("resultProp", "the param was [" + paramProp + "]"));
            }
            System.out.println("Invoked 'testControl'!!! : " + this);
        } else if (name.equals("testControlWithNoParams")) {
            controlResults.getComplexResults().put(new PropertySimple("result", "results value"));
            System.out.println("Invoked 'testControlWithNoParams'!!! : " + this);
        } else if (name.equals("testControlWithNoParamsOrResults")) {
            System.out.println("Invoked 'testControlWithNoParamsOrResults'!!! : " + this);
        } else {
            controlResults.setError("Unknown operation name: " + name);
        }
        return controlResults;
    }

    public void myScheduledJobMethod1() throws Exception {
        System.out.println("The sample plugin scheduled job [myScheduledJobMethod1] has triggered!!! : " + this);
    }

    public void myScheduledJobMethod2(ScheduledJobInvocationContext invocation) throws Exception {
        System.out.println("The sample plugin scheduled job [myScheduledJobMethod2] has triggered!!! : " + this
            + " - CALLBACK DATA=" + invocation.getJobDefinition().getCallbackData());
    }

    @Override
    public String toString() {
        if (this.context == null) {
            return "<no context>";
        }

        StringBuilder str = new StringBuilder();
        str.append("plugin-key=").append(this.context.getPluginEnvironment().getPluginKey()).append(",");
        str.append("plugin-url=").append(this.context.getPluginEnvironment().getPluginUrl()).append(",");
        str.append("plugin-config=[").append(getPluginConfigurationString()).append(']'); // do not append ,
        return str.toString();
    }

    private String getPluginConfigurationString() {
        String results = "";
        Configuration config = this.context.getPluginConfiguration();
        for (PropertySimple prop : config.getSimpleProperties().values()) {
            if (results.length() > 0) {
                results += ", ";
            }
            results = results + prop.getName() + "=" + prop.getStringValue();
        }
        return results;
    }
}
