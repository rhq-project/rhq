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
package org.rhq.enterprise.server.drift;

import java.util.List;

import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionEnumeration;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.drift.DriftConfigurationDefinition;
import org.rhq.enterprise.server.test.AbstractEJB3Test;

/**
 * Tests that our hard-coded singleton DriftConfigurationDefinition is persisted properly in the DB.
 */
@Test
public class DriftConfigurationDefinitionTest extends AbstractEJB3Test {

    public void testDriftConfigurationDefinition() throws Throwable {
        getTransactionManager().begin();
        try {
            ConfigurationDefinition def = DriftConfigurationDefinition.getInstance();
            ConfigurationDefinition defDATABASE = getEntityManager().find(ConfigurationDefinition.class, def.getId());

            assert defDATABASE.getId() == def.getId();
            assert defDATABASE.getPropertyDefinitions().size() == def.getPropertyDefinitions().size();

            PropertyDefinitionSimple simpleDATABASE;
            PropertyDefinitionSimple simple;
            PropertyDefinitionMap mapDATABASE;
            PropertyDefinitionMap map;
            PropertyDefinitionList listDATABASE;
            PropertyDefinitionList list;

            // NAME
            simpleDATABASE = defDATABASE.getPropertyDefinitionSimple(DriftConfigurationDefinition.PROP_NAME);
            simple = def.getPropertyDefinitionSimple(DriftConfigurationDefinition.PROP_NAME);
            assertSimpleProperty(simpleDATABASE, simple);

            // ENABLED
            simpleDATABASE = defDATABASE.getPropertyDefinitionSimple(DriftConfigurationDefinition.PROP_ENABLED);
            simple = def.getPropertyDefinitionSimple(DriftConfigurationDefinition.PROP_ENABLED);
            assertSimpleProperty(simpleDATABASE, simple);

            // INTERVAL
            simpleDATABASE = defDATABASE.getPropertyDefinitionSimple(DriftConfigurationDefinition.PROP_INTERVAL);
            simple = def.getPropertyDefinitionSimple(DriftConfigurationDefinition.PROP_INTERVAL);
            assertSimpleProperty(simpleDATABASE, simple);

            // BASEDIR
            mapDATABASE = defDATABASE.getPropertyDefinitionMap(DriftConfigurationDefinition.PROP_BASEDIR);
            map = def.getPropertyDefinitionMap(DriftConfigurationDefinition.PROP_BASEDIR);
            assertMapProperty(mapDATABASE, map);

            // BASEDIR VALUECONTEXT (implicitly also tests the enums)
            simpleDATABASE = mapDATABASE
                .getPropertyDefinitionSimple(DriftConfigurationDefinition.PROP_BASEDIR_VALUECONTEXT);
            simple = map.getPropertyDefinitionSimple(DriftConfigurationDefinition.PROP_BASEDIR_VALUECONTEXT);
            assertSimpleProperty(simpleDATABASE, simple);

            // BASEDIR VALUENAME
            simpleDATABASE = mapDATABASE
                .getPropertyDefinitionSimple(DriftConfigurationDefinition.PROP_BASEDIR_VALUENAME);
            simple = map.getPropertyDefinitionSimple(DriftConfigurationDefinition.PROP_BASEDIR_VALUENAME);
            assertSimpleProperty(simpleDATABASE, simple);

            // INCLUDES
            listDATABASE = defDATABASE.getPropertyDefinitionList(DriftConfigurationDefinition.PROP_INCLUDES);
            list = def.getPropertyDefinitionList(DriftConfigurationDefinition.PROP_INCLUDES);
            assertListProperty(listDATABASE, list);

            // INCLUDES INCLUDE
            /*
            PropertyDefinitionMap deleteme = getEntityManager().find(PropertyDefinitionMap.class, 8);

            mapDATABASE = (PropertyDefinitionMap) listDATABASE.getMemberDefinition();
            map = (PropertyDefinitionMap) list.getMemberDefinition();
            assertMapProperty(mapDATABASE, map);

            // INCLUDES INCLUDE PATH
            simpleDATABASE = mapDATABASE.getPropertyDefinitionSimple(DriftConfigurationDefinition.PROP_PATH);
            simple = map.getPropertyDefinitionSimple(DriftConfigurationDefinition.PROP_PATH);
            assertSimpleProperty(simpleDATABASE, simple);
            
            // INCLUDES INCLUDE PATTERN
            simpleDATABASE = mapDATABASE.getPropertyDefinitionSimple(DriftConfigurationDefinition.PROP_PATTERN);
            simple = map.getPropertyDefinitionSimple(DriftConfigurationDefinition.PROP_PATTERN);
            assertSimpleProperty(simpleDATABASE, simple);
            */

            // EXCLUDES
            listDATABASE = defDATABASE.getPropertyDefinitionList(DriftConfigurationDefinition.PROP_EXCLUDES);
            list = def.getPropertyDefinitionList(DriftConfigurationDefinition.PROP_EXCLUDES);
            assertListProperty(listDATABASE, list);

            /*
            // EXCLUDES EXCLUDE
            mapDATABASE = (PropertyDefinitionMap) listDATABASE.getMemberDefinition();
            map = (PropertyDefinitionMap) list.getMemberDefinition();
            assertMapProperty(mapDATABASE, map);

            // EXCLUDES EXCLUDE PATH
            simpleDATABASE = mapDATABASE.getPropertyDefinitionSimple(DriftConfigurationDefinition.PROP_PATH);
            simple = map.getPropertyDefinitionSimple(DriftConfigurationDefinition.PROP_PATH);
            assertSimpleProperty(simpleDATABASE, simple);

            // EXCLUDES EXCLUDE PATTERN
            simpleDATABASE = mapDATABASE.getPropertyDefinitionSimple(DriftConfigurationDefinition.PROP_PATTERN);
            simple = map.getPropertyDefinitionSimple(DriftConfigurationDefinition.PROP_PATTERN);
            assertSimpleProperty(simpleDATABASE, simple);
            */
        } finally {
            getTransactionManager().rollback();
        }
    }

    private void assertListProperty(PropertyDefinitionList listDb, PropertyDefinitionList list) throws Throwable {

        try {
            assert listDb.getId() == list.getId();
            assert listDb.getActivationPolicy() == list.getActivationPolicy();
            assert listDb.getDescription().equals(list.getDescription());
            assert listDb.getDisplayName().equals(list.getDisplayName());
            assert listDb.getName().equals(list.getName());
            assert listDb.getOrder() == list.getOrder();
        } catch (Throwable t) {
            System.out.println("Lists failed test:\nlistDb=[" + listDb + "]\nlist=[" + list + "]");
            throw t;
        }
    }

    private void assertMapProperty(PropertyDefinitionMap mapDb, PropertyDefinitionMap map) throws Throwable {

        try {
            assert mapDb.getId() == map.getId();
            assert mapDb.getActivationPolicy() == map.getActivationPolicy();
            assert mapDb.getDescription().equals(map.getDescription());
            assert mapDb.getDisplayName().equals(map.getDisplayName());
            assert mapDb.getName().equals(map.getName());
            assert mapDb.getOrder() == map.getOrder();
        } catch (Throwable t) {
            System.out.println("Maps failed test:\nmapDb=[" + mapDb + "]\nmap=[" + map + "]");
            throw t;
        }
    }

    private void assertSimpleProperty(PropertyDefinitionSimple simpleDb, PropertyDefinitionSimple simple)
        throws Throwable {

        try {
            assert simpleDb.getId() == simple.getId();
            assert simpleDb.getAllowCustomEnumeratedValue() == simple.getAllowCustomEnumeratedValue();
            assert simpleDb.getActivationPolicy() == simple.getActivationPolicy();
            assert simpleDb.getDescription().equals(simple.getDescription());
            assert simpleDb.getDisplayName().equals(simple.getDisplayName());
            assert simpleDb.getName().equals(simple.getName());
            assert simpleDb.getOrder() == simple.getOrder();
            assert simpleDb.getType() == simple.getType();
            if (simpleDb.getDefaultValue() == null) {
                assert simpleDb.getDefaultValue() == null && simple.getDefaultValue() == null;
            } else {
                assert simpleDb.getDefaultValue().equals(simple.getDefaultValue());
            }
            assertEnumeratedValues(simpleDb.getEnumeratedValues(), simple.getEnumeratedValues());

        } catch (Throwable t) {
            System.out.println("Simples failed test:\nsimpleDb=[" + simpleDb + "]\nsimple=[" + simple + "]");
            throw t;
        }
    }

    private void assertEnumeratedValues(List<PropertyDefinitionEnumeration> enumsDb,
        List<PropertyDefinitionEnumeration> enums) throws Throwable {

        assert enumsDb.size() == enums.size() : "enum sizes do not match: " + enumsDb.size() + ":" + enums.size();

        for (int i = 0; i < enumsDb.size(); i++) {
            PropertyDefinitionEnumeration eenumDb = enumsDb.get(i);
            PropertyDefinitionEnumeration eenum = enums.get(i);

            try {
                assert eenumDb.getId() == eenum.getId();
                assert eenumDb.getName().equals(eenum.getName());
                assert eenumDb.getOrderIndex() == eenum.getOrderIndex();
                assert eenumDb.getValue().equals(eenum.getValue());
            } catch (Throwable t) {
                System.out.println("Enum failed test:\neenumDb=[" + eenumDb + "]\neenums=[" + eenum + "]");
                throw t;
            }
        }
    }
}