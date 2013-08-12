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
package org.rhq.plugins.postfix;

import static org.rhq.core.domain.measurement.AvailabilityType.DOWN;
import static org.rhq.core.domain.measurement.AvailabilityType.UP;

import net.augeas.Augeas;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.system.ProcessInfo;
import org.rhq.core.system.ProcessInfo.ProcessInfoSnapshot;
import org.rhq.plugins.augeas.AugeasConfigurationComponent;
import org.rhq.plugins.augeas.helper.AugeasNode;

public class PostfixServerComponent extends AugeasConfigurationComponent {

    private ProcessInfo processInfo;

    public void start(ResourceContext resourceContext) throws Exception {
        super.start(resourceContext);
        processInfo = resourceContext.getNativeProcess();
    }

    public void stop() {
        processInfo = null;
        super.stop();
    }

    public AvailabilityType getAvailability() {
        ProcessInfoSnapshot processInfoSnapshot = getProcessInfoSnapshot();
        return (processInfoSnapshot != null && processInfoSnapshot.isRunning()) ? UP : DOWN;
    }

    private ProcessInfoSnapshot getProcessInfoSnapshot() {
        ProcessInfoSnapshot processInfoSnapshot = (processInfo == null) ? null : processInfo.freshSnapshot();
        if (processInfoSnapshot == null || !processInfoSnapshot.isRunning()) {
            processInfo = getResourceContext().getNativeProcess();
            // Safe to get prior snapshot here, we've just recreated the process info instance
            processInfoSnapshot = (processInfo == null) ? null : processInfo.priorSnaphot();
        }
        return processInfoSnapshot;
    }

    public Configuration loadResourceConfiguration() throws Exception {
        return super.loadResourceConfiguration();
    }

    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        super.updateResourceConfiguration(report);
    }

    protected Object toPropertyValue(PropertyDefinitionSimple propDefSimple, Augeas augeas, AugeasNode node) {
        if (propDefSimple.getType().equals(PropertySimpleType.BOOLEAN)) {
            return "yes".equals(augeas.get(node.getPath()));
        }
        return super.toPropertyValue(propDefSimple, augeas, node);
    }

    @Override
    protected String toNodeValue(Augeas augeas, AugeasNode node, PropertyDefinitionSimple propDefSimple,
        PropertySimple propSimple) {
        if (propDefSimple.getType().equals(PropertySimpleType.BOOLEAN)) {
            if (propSimple.getBooleanValue()) {
                return "yes";
            }
            return "no";
        }
        return super.toNodeValue(augeas, node, propDefSimple, propSimple);
    }

}
