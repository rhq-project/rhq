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
package org.rhq.plugins.aliases;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.plugins.augeas.test.AbstractAugeasConfigurationComponentTest;

/**
 * An integration test for {@link AliasesComponent}.
 */
public class AliasesComponentTest extends AbstractAugeasConfigurationComponentTest {
    @Override
    protected String getPluginName() {
        return "Aliases";
    }

    @Override
    protected String getResourceTypeName() {
        return "Aliases File";
    }

    @Override
    protected Configuration getExpectedResourceConfig() {
        Configuration config = new Configuration();
        PropertyList entries = new PropertyList(".");
        config.put(entries);

        PropertyMap entry;

        entry = new PropertyMap("*[name]");
        entry.put(new PropertySimple("name", "bin"));
        entry.put(new PropertySimple("value", "root"));
        entries.getList().add(entry);

        entry = new PropertyMap("*[name]");
        entry.put(new PropertySimple("name", "daemon"));
        entry.put(new PropertySimple("value", "root"));
        entries.getList().add(entry);

        entry = new PropertyMap("*[name]");
        entry.put(new PropertySimple("name", "adm"));
        entry.put(new PropertySimple("value", "root"));
        entries.getList().add(entry);

        entry = new PropertyMap("*[name]");
        entry.put(new PropertySimple("name", "lp"));
        entry.put(new PropertySimple("value", "root"));
        entries.getList().add(entry);

        entry = new PropertyMap("*[name]");
        entry.put(new PropertySimple("name", "sync"));
        entry.put(new PropertySimple("value", "root"));
        entries.getList().add(entry);

        entry = new PropertyMap("*[name]");
        entry.put(new PropertySimple("name", "shutdown"));
        entry.put(new PropertySimple("value", "root"));
        entries.getList().add(entry);

        return config;
    }

    @Override
    protected Configuration getUpdatedResourceConfig() {
        Configuration config = new Configuration();
        PropertyList entries = new PropertyList(".");
        config.put(entries);

        PropertyMap entry;

        entry = new PropertyMap("*[name]");
        entry.put(new PropertySimple("name", "shutdown"));
        entry.put(new PropertySimple("value", "parthaa"));
        entries.getList().add(entry);

        return config;
    }
}
