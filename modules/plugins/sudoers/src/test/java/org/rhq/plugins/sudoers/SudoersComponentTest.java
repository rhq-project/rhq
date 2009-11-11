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
package org.rhq.plugins.sudoers;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.plugins.augeas.test.AbstractAugeasConfigurationComponentTest;

/**
 * @author Partha Aji
 */
public class SudoersComponentTest extends AbstractAugeasConfigurationComponentTest {

    @Override
    protected Configuration getExpectedResourceConfig() {
        Configuration config = new Configuration();
        PropertyList entries = new PropertyList(".");
        config.put(entries);
        PropertyMap entry;
        entry = new PropertyMap("spec");
        entry.put(new PropertySimple("user", "root"));
        entry.put(new PropertySimple("host_group/host", "ALL"));
        entry.put(new PropertySimple("host_group/command", "ALL"));
        entry.put(new PropertySimple("host_group/command/runas_user", "ALL"));
        entry.put(new PropertySimple("host_group/command/tag", true));
        entries.getList().add(entry);

        entry = new PropertyMap("spec");
        entry.put(new PropertySimple("user", "%wheel"));
        entry.put(new PropertySimple("host_group/host", "ALL"));
        entry.put(new PropertySimple("host_group/command", "ALL"));
        entry.put(new PropertySimple("host_group/command/runas_user", "ALL"));
        entry.put(new PropertySimple("host_group/command/tag", false));
        entries.getList().add(entry);

        entry = new PropertyMap("spec");
        entry.put(new PropertySimple("user", "apache"));
        entry.put(new PropertySimple("host_group/host", "ALL"));
        entry.put(new PropertySimple("host_group/command", "/bin/env"));
        entry.put(new PropertySimple("host_group/command/runas_user", "root"));
        entry.put(new PropertySimple("host_group/command/tag", false));
        entries.getList().add(entry);

        entry = new PropertyMap("spec");
        entry.put(new PropertySimple("user", "foo"));
        entry.put(new PropertySimple("host_group/host", "ALL"));
        entry.put(new PropertySimple("host_group/command", "NETWORKING"));
        entry.put(new PropertySimple("host_group/command/runas_user", "root"));
        entry.put(new PropertySimple("host_group/command/tag", true));
        entries.getList().add(entry);
        return config;
    }

    @Override
    protected Configuration getChangedResourceConfig() {
        Configuration config = new Configuration();
        PropertyList entries = new PropertyList(".");
        config.put(entries);
        PropertyMap entry;
        entry = new PropertyMap("spec");
        entry.put(new PropertySimple("user", "root"));
        entry.put(new PropertySimple("host_group/host", "fooo"));
        entry.put(new PropertySimple("host_group/command", "/bin/env"));
        entry.put(new PropertySimple("host_group/command/runas_user", "ALL"));
        entry.put(new PropertySimple("host_group/command/tag", true));
        entries.getList().add(entry);

        entry = new PropertyMap("spec");
        entry.put(new PropertySimple("user", "%wheel"));
        entry.put(new PropertySimple("host_group/host", "ALL"));
        entry.put(new PropertySimple("host_group/command", "ALL"));
        entry.put(new PropertySimple("host_group/command/runas_user", "ALL"));
        entry.put(new PropertySimple("host_group/command/tag", false));
        entries.getList().add(entry);

        entry = new PropertyMap("spec");
        entry.put(new PropertySimple("user", "apache"));
        entry.put(new PropertySimple("host_group/host", "ALL"));
        entry.put(new PropertySimple("host_group/command", "/bin/env"));
        entry.put(new PropertySimple("host_group/command/runas_user", "root"));
        entry.put(new PropertySimple("host_group/command/tag", false));
        entries.getList().add(entry);

        entry = new PropertyMap("spec");
        entry.put(new PropertySimple("user", "foo"));
        entry.put(new PropertySimple("host_group/host", "ALL"));
        entry.put(new PropertySimple("host_group/command", "NETWORKING"));
        entry.put(new PropertySimple("host_group/command/runas_user", "root"));
        entry.put(new PropertySimple("host_group/command/tag", true));
        entries.getList().add(entry);
        return config;
    }

    @Override
    protected String getPluginName() {
        return "Sudoers";
    }

    @Override
    protected String getResourceTypeName() {
        return "Sudoers";
    }

    @Override
    protected String[] getAssociatedConfigs() {
        return new String[] { "/etc/sudoers" };
    }

}
