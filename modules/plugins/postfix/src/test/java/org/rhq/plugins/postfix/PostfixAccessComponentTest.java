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

package org.rhq.plugins.postfix;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.plugins.augeas.test.AbstractAugeasConfigurationComponentTest;

/**
 * @author paji
 *
 */
public class PostfixAccessComponentTest extends AbstractAugeasConfigurationComponentTest {

    @Override
    protected String getPluginName() {
        return "Postfix";
    }

    @Override
    protected String getResourceTypeName() {
        return "Access";
    }

    @Override
    protected Configuration getExpectedResourceConfig() {
        Configuration config = new Configuration();
        PropertyList entries = new PropertyList(".");
        config.put(entries);

        PropertyMap entry;

        /*
         * 1.2.3        REJECT
         * 1.2.3.4DISCARD Sorry Not allowed
         */

        entry = new PropertyMap("*[pattern]");
        entry.put(new PropertySimple("pattern", "1.2.3"));
        entry.put(new PropertySimple("action", "REJECT"));
        entry.put(new PropertySimple("parameters", null));
        entries.getList().add(entry);

        entry = new PropertyMap("*[pattern]");
        entry.put(new PropertySimple("pattern", "1.2.3.4"));
        entry.put(new PropertySimple("action", "DISCARD"));
        entry.put(new PropertySimple("parameters", "Sorry Not allowed"));
        entries.getList().add(entry);
        return config;
    }

    @Override
    protected Configuration getUpdatedResourceConfig() {
        Configuration config = new Configuration();
        PropertyList entries = new PropertyList(".");
        config.put(entries);

        PropertyMap entry;
        entry = new PropertyMap("*[pattern]");
        entry.put(new PropertySimple("pattern", "1.2.3.4"));
        entry.put(new PropertySimple("action", "BAR"));
        entries.getList().add(entry);
        return config;
    }
}
