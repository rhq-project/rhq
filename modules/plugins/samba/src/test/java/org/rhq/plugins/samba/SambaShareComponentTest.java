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

package org.rhq.plugins.samba;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.plugins.augeas.test.AbstractAugeasConfigurationComponentTest;

/**
 * @author paji
 *
 */
public class SambaShareComponentTest extends AbstractAugeasConfigurationComponentTest {

    @Override
    protected Configuration getExpectedResourceConfig() {
        Configuration config = new Configuration();
        /*        Configuration[id=0, notes=Loaded from Augeas at Mon Dec 14 22:21:33 EST 2009, 
         * properties[, name=work, path=/work/samba-test, comment=null, browseable=yes, read only=yes, 
         * printable=null,=null, guest ok=yes, share modes=null, valid users=null], rawConfigurations[]]
         */
        config.put(new PropertySimple("name", "work"));
        config.put(new PropertySimple("path", "/work/samba-test"));
        config.put(new PropertySimple("comment", null));
        config.put(new PropertySimple("browseable", "yes"));
        config.put(new PropertySimple("read only", "yes"));
        config.put(new PropertySimple("printable", null));
        config.put(new PropertySimple("write list", null));
        config.put(new PropertySimple("guest ok", "yes"));
        config.put(new PropertySimple("share modes", null));
        config.put(new PropertySimple("valid users", null));
        /*
        [work]
           name = work
           path = /work/samba-test
           browseable = yes
           read only = yes
           guest ok = yes
         * 
         */
        return config;
    }

    @Override
    protected String getPluginName() {
        return "Samba";
    }

    @Override
    protected String getResourceTypeName() {
        return "Samba Share";
    }

    @Override
    protected Configuration getUpdatedResourceConfig() {
        Configuration config = new Configuration();

        config.put(new PropertySimple("name", "work"));
        config.put(new PropertySimple("path", "/work/samba-test"));
        config.put(new PropertySimple("comment", null));
        config.put(new PropertySimple("browseable", "yes"));
        config.put(new PropertySimple("printable", null));
        config.put(new PropertySimple("write list", null));
        config.put(new PropertySimple("guest ok", "yes"));
        config.put(new PropertySimple("share modes", null));
        config.put(new PropertySimple("valid users", "fooo"));

        return config;
    }

}
