/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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

package org.rhq.core.domain.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ObfuscatedPropertySimple;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;

/**
 * 
 *
 * @author Lukas Krejci
 */
@Test
public class PasswordObfuscationUtilityTest {

    private static final ConfigurationDefinition DEFINITION = new ConfigurationDefinition("test", null);

    private static final Configuration CONFIGURATION = new Configuration();

    static {
        DEFINITION.put(new PropertyDefinitionSimple("topLevelString", null, true, PropertySimpleType.STRING));
        DEFINITION.put(new PropertyDefinitionSimple("topLevelPassword", null, true, PropertySimpleType.PASSWORD));
        DEFINITION.put(new PropertyDefinitionList("topLevelPasswordList", null, true, new PropertyDefinitionSimple(
            "listPassword", null, true, PropertySimpleType.PASSWORD)));
        DEFINITION.put(new PropertyDefinitionList("topLevelListOfMaps", null, true, new PropertyDefinitionMap(
            "mapWithPass", null, true, new PropertyDefinitionSimple("username", null, true, PropertySimpleType.STRING),
            new PropertyDefinitionSimple("password", null, true, PropertySimpleType.PASSWORD))));

        CONFIGURATION.put(new PropertySimple("topLevelString", "topLevelString"));
        CONFIGURATION.put(new PropertySimple("topLevelPassword", "topLevelPassword"));
        CONFIGURATION.put(new PropertyList("topLevelPasswordList", new PropertySimple("listPassword", "0"),
            new PropertySimple("listPassword", "1")));
        CONFIGURATION.put(new PropertyList("topLevelListOfMaps", new PropertyMap("mapWithPass", new PropertySimple(
            "username", "username"), new PropertySimple("password", "password"))));
    }

    private Configuration getFreshConfig() {
        return CONFIGURATION.deepCopy();
    }
    
    public void listMembersSwapped() {
        Configuration testConfig = getFreshConfig();
        
        PasswordObfuscationUtility.obfuscatePasswords(DEFINITION, testConfig);
        
        PropertyList list = testConfig.getList("topLevelPasswordList");
        
        Assert.assertNotNull(list, "Could not find the 'topLevelPasswordList'");
        
        int idx = 0;
        for(Property p : list.getList()) {
            Assert.assertTrue(p instanceof ObfuscatedPropertySimple, "Found a password property that was not swapped to obfuscated form");
            int value = ((PropertySimple) p).getIntegerValue();
            Assert.assertEquals(value, idx++, "Found an out-of-order element after obfuscation processing.");
        }
    }

    public void configurationMembersSwappedAndKeptPositions() {
        Configuration testConfig = getFreshConfig();
        
        //keep track of the order of the properies in the original map
        PropertyMap originalMap = (PropertyMap) testConfig.getList("topLevelListOfMaps").getList().get(0);
        List<Property> originalMapPropertiesInOrder = new ArrayList<Property>(originalMap.getMap().values());
        
        PasswordObfuscationUtility.obfuscatePasswords(DEFINITION, testConfig);
        
        PropertyList list = testConfig.getList("topLevelListOfMaps");
        
        PropertyMap map = (PropertyMap) list.getList().get(0);
        
        Assert.assertTrue(map.get("password") instanceof ObfuscatedPropertySimple, "The password in the map failed to be swapped to obfuscated form");
        
        //now check that the positions in the map actually didn't change
        List<Property> mapPropertiesInOrder = new ArrayList<Property>(map.getMap().values());
        
        Assert.assertEquals(mapPropertiesInOrder.size(), originalMapPropertiesInOrder.size(), "Different number of properties in the map after obfuscation");
        
        for(int i = 0; i < mapPropertiesInOrder.size(); ++i) {
            Property originalProp = originalMapPropertiesInOrder.get(i);
            Property prop = mapPropertiesInOrder.get(i);
            
            Assert.assertEquals(prop.getName(), originalProp.getName(), "Properties seem to be mixed up after obfuscation");
        }
    }

    public void mapMembersSwappedAndKeptPositions() {
        Configuration testConfig = getFreshConfig();
        
        int origPassIdx = 0;
        Iterator<Property> it = testConfig.getMap().values().iterator();
        while (it.hasNext()) {
            if ("topLevelPassword".equals(it.next().getName())) {
                break;
            }
            origPassIdx++;
        }
        
        PasswordObfuscationUtility.obfuscatePasswords(DEFINITION, testConfig);
        
        Assert.assertTrue(testConfig.getSimple("topLevelPassword") instanceof ObfuscatedPropertySimple, "The top level password not obfuscated");
        
        it = testConfig.getMap().values().iterator();        
        int idx = 0;
        while(it.hasNext()) {
            if ("topLevelPassword".equals(it.next().getName())) {
                Assert.assertEquals(idx, origPassIdx, "The topLevelPassword seems to have changed the position in the configuration map after obfuscation");
            }
            idx++;
        }
    }
}
