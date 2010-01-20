/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.plugins.kickstart;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.core.system.ProcessExecution;
import org.rhq.core.system.ProcessExecutionResults;

/**
 * This can be the start of your own custom plugin's server component. Review the javadoc for {@link ResourceComponent}
 * and all the facet interfaces to learn what you can do in your resource component. This component has a lot of methods
 * in it because it implements all possible facets. If your resource does not support, for example, configuration, you
 * can remove the {@link ConfigurationFacet} from the <code>implements</code> clause and remove all method
 * implementations that that facet required.
 *
 * <p>You should not only read the javadoc in each of this class' methods, but you should also read the javadocs linked
 * by their "see" javadoc tags since those additional javadocs will contain a good deal of additional information you
 * will need to know.</p>
 *
 * @author John Mazzitelli
 */
public class KickstartServerComponent implements ResourceComponent, OperationFacet {
    private final Log log = LogFactory.getLog(KickstartServerComponent.class);

    private ResourceContext resourceContext;

    public void start(ResourceContext context) {
        resourceContext = context;
    }

    public void stop() {
    }

    public AvailabilityType getAvailability() {
        return AvailabilityType.UP;
    }

    public OperationResult invokeOperation(String name, Configuration configuration) {

        OperationResult res = new OperationResult();
        if (name.equals("installGuest")) {
            log.info("Installing a Guest");
            String command = "/usr/bin/koan";
            String virtName = configuration.getSimpleValue("name", "Guest " + System.currentTimeMillis());
            String profile = configuration.getSimpleValue("profile", "profile");
            String server = configuration.getSimpleValue("server", "localhost");
            ProcessExecutionResults pr = this.execute(command, "--virt", "--server", server, "--profile", profile,
                "--virt-name", virtName);
            res.setSimpleResult(pr.getExitCode().toString());
        }

        return res;
    }

    public ProcessExecutionResults execute(String process, String... args) {
        List<String> argsList = Arrays.asList(args);
        if (log.isDebugEnabled()) {
            String argsString = "";
            for (String arg : argsList)
                argsString = argsString + " " + arg;
            log.debug("Executing command " + process + argsString);
        }
        ProcessExecution pe = new ProcessExecution(process);
        pe.setCaptureOutput(true);
        pe.setCheckExecutableExists(true);
        pe.setArguments(Arrays.asList(args));
        ProcessExecutionResults pr = resourceContext.getSystemInformation().executeProcess(pe);
        log.debug("Result " + pr.getExitCode());
        return pr;
    }
}
