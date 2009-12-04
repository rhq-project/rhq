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
package org.rhq.plugins.samba;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.plugins.augeas.test.AbstractAugeasConfigurationComponentTest;

public class SambaComponentTest extends AbstractAugeasConfigurationComponentTest {

    @Override
    protected Configuration getExpectedResourceConfig() {
        Configuration config = new Configuration();

        config.put(new PropertySimple("workgroup", "SCOOBY"));
        config.put(new PropertySimple("server string", "MysteryMachine"));
        config.put(new PropertySimple("security", "user"));
        config.put(new PropertySimple("encrypt passwords", "yes"));
        config.put(new PropertySimple("load printers", "yes"));
        config.put(new PropertySimple("cups options", "raw"));
        config.put(new PropertySimple("enableRecycleBin", false));
        return config;
    }

    @Override
    protected String getPluginName() {
        return "Samba";
    }

    @Override
    protected String getResourceTypeName() {
        return "Samba Server";
    }

    @Override
    protected Configuration getUpdatedResourceConfig() {
        Configuration config = new Configuration();

        config.put(new PropertySimple("workgroup", "DUMBCHANGE"));
        config.put(new PropertySimple("server string", "DUMBCHANGE"));
        config.put(new PropertySimple("security", "DUMBCHANGE"));
        config.put(new PropertySimple("encrypt passwords", "DUMBCHANGE"));
        config.put(new PropertySimple("load printers", "DUMBCHANGE"));
        config.put(new PropertySimple("cups options", "DUMBCHANGE"));
        config.put(new PropertySimple("enableRecycleBin", true));

        return config;
    }
}
