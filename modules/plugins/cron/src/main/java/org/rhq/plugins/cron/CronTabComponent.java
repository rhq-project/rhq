/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package org.rhq.plugins.cron;

import java.io.File;
import java.util.List;

import net.augeas.Augeas;

import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.plugins.augeas.AugeasConfigurationComponent;
import org.rhq.plugins.augeas.helper.AugeasNode;

/**
 * A component representing a single crontab file.
 * 
 * @author Lukas Krejci
 */
public class CronTabComponent extends AugeasConfigurationComponent<CronComponent> {

    public static final String ENVIRONMENT_SETTINGS_PROP = "environmentSettings";
    public static final String ENVIRONMENT_SETTINGS_NODE = ".";
    public static final String VAR_PROP = "var";
    public static final String ENTRIES_PROP = "entries";
    public static final String ENTRIES_NODE = ".";
    public static final String NAME_PROP = "name";
    public static final String VALUE_PROP = "value";

    private File crontabFile;
    private String rootPath;
    
    public void start(ResourceContext<CronComponent> context) throws InvalidPluginConfigurationException, Exception {
        crontabFile = new File(context.getResourceKey());
        rootPath = AugeasNode.SEPARATOR + "files" + crontabFile.getAbsolutePath();
        super.start(context);
    }
    
    public AvailabilityType getAvailability() {
        return crontabFile.exists() ? AvailabilityType.UP : AvailabilityType.DOWN;
    }

    @Override
    protected String getResourceConfigurationRootPath() {
        return rootPath;
    }

    @Override
    protected String getAugeasPathRelativeToParent(PropertyDefinition propDef, AugeasNode parentNode, Augeas augeas) {
        String name = propDef.getName();
        if (ENVIRONMENT_SETTINGS_PROP.equals(name)) {
            return ENVIRONMENT_SETTINGS_NODE;
        } else if (ENTRIES_PROP.equals(name)) {
            return ENTRIES_NODE;
        } else {
            return super.getAugeasPathRelativeToParent(propDef, parentNode, augeas);
        }
    }

    @Override
    protected Property createPropertyList(PropertyDefinitionList propDefList, Augeas augeas, AugeasNode node) {
        if (ENVIRONMENT_SETTINGS_PROP.equals(propDefList.getName())) {
            List<String> envSettings = augeas.match(node.getPath() + "/*[label() != \"entry\" and label() != \"#comment\"]");
            PropertyList ret = new PropertyList(propDefList.getName());
            
            for(String path : envSettings) {
                PropertyMap map = new PropertyMap(VAR_PROP);
                ret.add(map);
                String name = new AugeasNode(path).getName();
                String value = augeas.get(path);
                map.put(new PropertySimple(NAME_PROP, name));
                map.put(new PropertySimple(VALUE_PROP, value));
            }
            
            return ret;
        } else {
            return super.createPropertyList(propDefList, augeas, node);
        }
    }

    @Override
    protected void setNodeFromPropertyList(PropertyDefinitionList propDefList, PropertyList propList, Augeas augeas,
        AugeasNode listNode) {
        if (ENVIRONMENT_SETTINGS_PROP.equals(propDefList.getName())) {
            List<String> currentVars = augeas.match(listNode.getPath() + "/*[label() != \"entry\" and label() != \"#comment\"]");
            for(Property p : propList.getList()) {
                PropertyMap map = (PropertyMap)p;
                String name = map.getSimpleValue(NAME_PROP, null);
                String value = map.getSimpleValue(VALUE_PROP, null);
                String path = listNode.getPath() + AugeasNode.SEPARATOR + name;
                augeas.set(path, value);
                currentVars.remove(path);
            }
            
            //delete the leftovers
            for(String path : currentVars) {
                augeas.remove(path);
            }
        } else if (ENTRIES_PROP.equals(propDefList.getName())) {
            super.setNodeFromPropertyList(propDefList, propList, augeas, listNode);
        }
    }

    @Override
    protected AugeasNode getExistingChildNodeForListMemberPropertyMap(AugeasNode parentNode,
        PropertyDefinitionList propDefList, PropertyMap propMap) {
        if (ENTRIES_PROP.equals(propDefList.getName())) {
            int idx = propMap.getParentList().getList().indexOf(propMap);
            int matchesCnt = getAugeas().match(parentNode.getPath() + AugeasNode.SEPARATOR + "entry").size();
            boolean useIndex = idx != 0 || matchesCnt > 1;
            
            if (idx < matchesCnt) {
                return new AugeasNode(parentNode, propMap.getName() + (useIndex ? "[" + (idx + 1) + "]" : ""));
            } else {
                return null;
            }
        } else {
            return super.getExistingChildNodeForListMemberPropertyMap(parentNode, propDefList, propMap);
        }
    }   
}
