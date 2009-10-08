/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.core.domain.configuration.test;

import java.util.HashMap;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.transaction.TransactionManager;

import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionEnumeration;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertyGroupDefinition;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.core.domain.configuration.definition.constraint.Constraint;
import org.rhq.core.domain.configuration.definition.constraint.FloatRangeConstraint;
import org.rhq.core.domain.test.AbstractEJB3Test;

public class ConfigurationDefinitionTest extends AbstractEJB3Test {
    public static final String CONFIG_NAME = "TEST_CONFIG";

    public static final String TEST_CONFIG_CONSTRAINT_NAME = "CONFIG_CONSTRAINT_TEST";

    @Test(groups = "integration.ejb3")
    public void testStoreDefinition() throws Exception {
        TransactionManager transactionManager = getTransactionManager();
        transactionManager.begin();

        try {
            EntityManager em = getEntityManager();
            ConfigurationDefinition definition = new ConfigurationDefinition(CONFIG_NAME,
                "Config definition for the thing");
            definition.put(new PropertyDefinitionSimple("SimpleProp", "My Simple Property", true,
                PropertySimpleType.STRING));

            definition.put(new PropertyDefinitionMap("MapProp", "Map Properties", true, new PropertyDefinitionSimple(
                "IntInMap", "Integer In Map", true, PropertySimpleType.INTEGER)));

            PropertyDefinitionSimple enumeratedString = new PropertyDefinitionSimple("ConnectionType", "My conn type",
                true, PropertySimpleType.STRING);
            enumeratedString.addEnumeratedValues(new PropertyDefinitionEnumeration("Local", "local"),
                new PropertyDefinitionEnumeration("JSR160", "jsr160"), new PropertyDefinitionEnumeration("JBoss",
                    "jboss"));
            enumeratedString.setAllowCustomEnumeratedValue(true);

            PropertyGroupDefinition basicGroup = new PropertyGroupDefinition("Basic Group");
            enumeratedString.setPropertyGroupDefinition(basicGroup);

            definition.put(enumeratedString);

            PropertyDefinitionSimple secondGroupedProperty = new PropertyDefinitionSimple("SimpleBool", "bool", false,
                PropertySimpleType.BOOLEAN);
            secondGroupedProperty.setPropertyGroupDefinition(basicGroup);
            definition.put(secondGroupedProperty);

            em.persist(definition);

            testReadDefinition(em);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            transactionManager.rollback();
        }
    }

    private void testReadDefinition(EntityManager em) throws Exception {
        Query query = em.createQuery("select e from ConfigurationDefinition e where e.name = :name");
        List<ConfigurationDefinition> definitions = query.setParameter("name", CONFIG_NAME).getResultList();

        HashMap<Integer, PropertyGroupDefinition> doomedGroups = new HashMap<Integer, PropertyGroupDefinition>();
        for (ConfigurationDefinition definition : definitions) {
            prettyPrintConfigurationDefinition(definition);
            for (PropertyGroupDefinition def : definition.getGroupDefinitions()) {
                doomedGroups.put(def.getId(), def);
            }
            em.remove(definition);
        }
        for (PropertyGroupDefinition doomed : doomedGroups.values()) {
            em.remove(doomed);
        }
    }

    @Test(groups = "integration.ejb3")
    public void testEnumeratedValues() throws Exception {
        TransactionManager transactionManager = getTransactionManager();
        transactionManager.begin();

        try {
            EntityManager em = getEntityManager();

            String testDefName = "CONFIG_ENUM_VAL_TEST";

            ConfigurationDefinition def = new ConfigurationDefinition(testDefName, "test data");

            PropertyDefinitionSimple prop = new PropertyDefinitionSimple("EnumeratedProperty", "", true,
                PropertySimpleType.STRING);
            prop.addEnumeratedValues(new PropertyDefinitionEnumeration("A", "a"), new PropertyDefinitionEnumeration(
                "B", "b"));

            def.put(prop);
            em.persist(def);

            List<ConfigurationDefinition> definitions = em.createQuery(
                "select e from ConfigurationDefinition e where e.name = :name").setParameter("name", testDefName)
                .getResultList();

            for (ConfigurationDefinition definition : definitions) {
                prettyPrintConfigurationDefinition(definition);
                PropertyDefinitionSimple propDef = definition.getPropertyDefinitionSimple("EnumeratedProperty");
                System.out.println("Before: " + propDef.getEnumeratedValues());

                PropertyDefinitionEnumeration enumVal = propDef.getEnumeratedValues().remove(0);
                propDef.getEnumeratedValues().add(enumVal);

                definition = em.merge(definition);
                System.out.println("After: "
                    + definition.getPropertyDefinitionSimple("EnumeratedProperty").getEnumeratedValues());

                assert (definition.getPropertyDefinitionSimple("EnumeratedProperty").getEnumeratedValues().get(0)
                    .getValue().equals("b")) : "Values were not properly reordered";
                em.remove(definition);
            }
        } finally {
            transactionManager.rollback();
        }
    }

    @Test(groups = "integration.ejb3")
    public void testStoreConstraints() throws Exception {
        TransactionManager transactionManager = getTransactionManager();
        transactionManager.begin();
        try {
            EntityManager em = getEntityManager();

            ConfigurationDefinition def = new ConfigurationDefinition(TEST_CONFIG_CONSTRAINT_NAME, "test data");

            PropertyDefinitionSimple prop = new PropertyDefinitionSimple("ConstrainedProperty", "", true,
                PropertySimpleType.FLOAT);
            prop.addConstraints(new FloatRangeConstraint(1d, 3d));
            def.put(prop);
            em.persist(def);
            testReadConstraints(em);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            transactionManager.rollback();
        }
    }

    private void testReadConstraints(EntityManager em) throws Exception {
        Query query = em.createQuery("select e from ConfigurationDefinition e where e.name = :name");
        List<ConfigurationDefinition> definitions = query.setParameter("name", TEST_CONFIG_CONSTRAINT_NAME)
            .getResultList();

        for (ConfigurationDefinition definition : definitions) {
            prettyPrintConfigurationDefinition(definition);
            PropertyDefinitionSimple propDef = definition.getPropertyDefinitionSimple("ConstrainedProperty");
            for (Constraint constraint : propDef.getConstraints()) {
                System.out.println("Constraint: " + constraint);
            }

            em.remove(definition);
        }
    }

    public static void prettyPrintConfigurationDefinition(ConfigurationDefinition definition) {
        System.out.println("Configuration definition: " + definition.getName() + " (" + definition.getDescription()
            + ")");
        for (PropertyDefinition propertyDefinition : definition.getPropertyDefinitions().values()) {
            prettyPrintPropertyDefinition(propertyDefinition, 1);
        }
    }

    private static void prettyPrintPropertyDefinition(PropertyDefinition propDef, int indent) {
        if (propDef instanceof PropertyDefinitionList) {
            indent(indent);
            System.out.println("List Property [" + propDef.getName() + "]");
            indent(indent);
            prettyPrintPropertyDefinition(((PropertyDefinitionList) propDef).getMemberDefinition(), indent + 1);
        } else if (propDef instanceof PropertyDefinitionMap) {
            for (int i = 0; i < indent; i++) {
                System.out.print("\t");
            }

            System.out.println("Map Property [" + propDef.getName() + "]");
            for (PropertyDefinition p : ((PropertyDefinitionMap) propDef).getPropertyDefinitions().values()) {
                prettyPrintPropertyDefinition(p, indent + 1);
            }
        } else if (propDef instanceof PropertyDefinitionSimple) {
            for (int i = 0; i < indent; i++) {
                System.out.print("\t");
            }

            System.out.println(propDef.toString());
        }
    }

    private static void indent(int indent) {
        for (int i = 0; i < indent; i++) {
            System.out.print("\t");
        }
    }
}