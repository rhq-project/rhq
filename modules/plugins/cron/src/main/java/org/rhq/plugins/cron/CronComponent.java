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
package org.rhq.plugins.cron;

import net.augeas.Augeas;

import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.plugins.augeas.AugeasConfigurationComponent;
import org.rhq.plugins.augeas.helper.AugeasNode;
import org.rhq.plugins.platform.PlatformComponent;

/**
 * @author Lukas Krejci 
 */
public class CronComponent extends AugeasConfigurationComponent<PlatformComponent> {

    private static final String CRONTAB_PROP = "..";
    private static final int AUGEAS_FILES_PREFIX_LENGTH = "/files".length();
    
    @Override
    protected Property createPropertySimple(PropertyDefinitionSimple propDefSimple, Augeas augeas, AugeasNode node) {
        if (CRONTAB_PROP.equals(propDefSimple.getName())) {
            //we want the full path to the crontab file the entry is in..
            //the node's path is /files/blah/blah/entry/..
            //we want the /blah/blah part
            String crontabPath = node.getParent().getParent().getPath().substring(AUGEAS_FILES_PREFIX_LENGTH);
            
            return new PropertySimple(CRONTAB_PROP, crontabPath);
        } else {
            return super.createPropertySimple(propDefSimple, augeas, node);
        }
    }

    @Override
    protected void setNodeFromPropertySimple(Augeas augeas, AugeasNode node, PropertyDefinitionSimple propDefSimple,
        PropertySimple propSimple) {
        
        if (CRONTAB_PROP.equals(propDefSimple.getName())) {
            //TODO we want to move this entry into the crontab specified
        } else {
            super.setNodeFromPropertySimple(augeas, node, propDefSimple, propSimple);
        }
    }
}
