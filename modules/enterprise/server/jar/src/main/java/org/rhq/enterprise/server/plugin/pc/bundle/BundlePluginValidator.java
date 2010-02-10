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

package org.rhq.enterprise.server.plugin.pc.bundle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.enterprise.server.plugin.pc.ServerPluginEnvironment;
import org.rhq.enterprise.server.plugin.pc.ServerPluginValidator;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.bundle.BundlePluginDescriptorType;

/**
 * Validate a bundle plugin.
 * 
 * See {@link ServerPluginValidator} for more information.
 * 
 * @author John Mazzitelli
 */
public class BundlePluginValidator implements ServerPluginValidator {
    private final Log log = LogFactory.getLog(BundlePluginValidator.class);

    public boolean validate(ServerPluginEnvironment env) {
        if (!(env.getPluginDescriptor() instanceof BundlePluginDescriptorType)) {
            log.error("Descriptor was not of the bundle type: " + env.getPluginDescriptor());
            return false;
        }
        BundlePluginDescriptorType descriptor = (BundlePluginDescriptorType) env.getPluginDescriptor();
        if (descriptor.getBundle() == null) {
            log.error("Descriptor missing the bundle type");
            return false;
        }
        if (descriptor.getBundle().getType() == null) {
            log.error("Descriptor missing the bundle type's name attribute");
            return false;
        }
        if (descriptor.getBundle().getType().trim().length() == 0) {
            log.error("Descriptor bundle type's name must be non-zero in length");
            return false;
        }
        return true;
    }
}
