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

package org.rhq.core.db.upgrade;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.rhq.core.db.DatabaseType;
import org.rhq.core.util.obfuscation.Obfuscator;

/**
 * Abstract base class for obfuscating the passwords in the database.
 * Because configurations and configuration definitions are only loosely coupled to
 * the entities, this class provides the upgrade functionality only from the point
 * where the configuration - configuration definition pairs are known.
 * 
 * It is the subclasses responsibility to provide those pairs based on the entity type
 * the subclass handles.
 *
 * @author Lukas Krejci
 */
public abstract class AbstractConfigurationObfuscationUpgradeTask implements DatabaseUpgradeTask {

    protected Connection connection;
    protected DatabaseType databaseType;

    enum PropertyType {
        MAP,
        LIST,
        SIMPLE,
        OBFUSCATED;

        static PropertyType fromName(String name) {
            if ("map".equals(name)) {
                return MAP;
            } else if ("list".equals(name)) {
                return LIST;
            } else if ("property".equals(name)) {
                return SIMPLE;
            } else if ("obfuscated".equals(name)) {
                return OBFUSCATED;
            } else {
                return null;
            }
        }
    }

    enum PropertySimpleType {
        VARIOUS,
        PASSWORD;

        static PropertySimpleType fromName(String name) {
            if ("PASSWORD".equals(name)) {
                return PASSWORD;
            } else {
                return VARIOUS;
            }
        }
    }

    class PropertyDefinition {
        int id;
        String name;
        PropertyType type;
        PropertySimpleType simpleType;
        
        List<PropertyDefinition> getChildDefinitions() throws SQLException {
            if (type == PropertyType.SIMPLE || type == PropertyType.OBFUSCATED) {
                return Collections.emptyList();
            }

            String sql = "SELECT id, name, dtype, simple_type FROM rhq_config_prop_def WHERE ";
            switch (type) {
            case MAP:
                sql += "parent_map_definition_id = " + id;
                break;
            case LIST:
                sql += "parent_list_definition_id = " + id;
                break;
            default:
                return Collections.emptyList();
            }

            List<Object[]> results = databaseType.executeSelectSql(connection, sql);
            List<PropertyDefinition> ret = new ArrayList<PropertyDefinition>();
            for (Object[] row : results) {
                PropertyDefinition pd = new PropertyDefinition();
                pd.id = (Integer) row[0];
                pd.name = (String) row[1];
                pd.type = PropertyType.fromName((String) row[2]);
                pd.simpleType = row[3] != null ? PropertySimpleType.fromName((String) row[3]) : null;
                
                ret.add(pd);
            }

            return ret;
        }

        @Override
        public String toString() {
            return "[id: " + id + ", name: '" + name + "', type: " + type.name() + "]";
        }
    }

    class Property {
        int id;
        String name;
        PropertyType type;
        String value;

        List<Property> getChildren() throws SQLException {
            if (type == PropertyType.SIMPLE || type == PropertyType.OBFUSCATED) {
                return Collections.emptyList();
            }

            String sql = "SELECT id, name, dtype, string_value FROM rhq_config_property WHERE ";
            switch (type) {
            case MAP:
                sql += "parent_map_id = " + id;
                break;
            case LIST:
                sql += "parent_list_id = " + id;
                break;
            default:
                return Collections.emptyList();
            }

            List<Object[]> results = databaseType.executeSelectSql(connection, sql);
            List<Property> ret = new ArrayList<Property>();
            for (Object[] row : results) {
                Property p = new Property();
                p.id = (Integer) row[0];
                p.name = (String) row[1];
                p.type = PropertyType.fromName((String) row[2]);
                p.value = (String) row[3];

                ret.add(p);
            }

            return ret;
        }

        @Override
        public String toString() {
            return "[id: " + id + ", name: '" + name + "', type: " + type.name() + "]";
        }
    }

    /**
     * Based on the entity type that the subclass handles, this method will return the pairs
     * of configuration id and the corresponding configuration definition id for each of the
     * entities that the subclass handles.
     * 
     * For example if the subclass handles plugin configuration of the resources, the resulting
     * map will contain the plugin configuration id of each resource as a key and the value
     * of each key will be the plugin configuration definition id of the resource type of the
     * resource.
     *  
     * @throws SQLException
     */
    protected abstract Map<Integer, Integer> getConfigurationIdConfigurationDefinitionIdPairs() throws SQLException;

    /**
     * @return a textual human-friendly description of the entity type this subclass handles. 
     * This is only used in the error reporting.
     */
    protected abstract String getEntityTypeDescription();

    @Override
    public void execute(DatabaseType type, Connection connection) throws SQLException {
        this.connection = connection;
        this.databaseType = type;

        for (Map.Entry<Integer, Integer> confAndDef : getConfigurationIdConfigurationDefinitionIdPairs().entrySet()) {
            List<PropertyDefinition> defs = getTopDefinitions(confAndDef.getValue());
            List<Property> props = getTopProperties(confAndDef.getKey());

            Map<PropertyDefinition, Property> pairs = matchDefinitionsAndProperties(defs, props);
            for (Map.Entry<PropertyDefinition, Property> entry : pairs.entrySet()) {
                try {
                    processProperty(entry.getKey(), entry.getValue());
                } catch (Exception e) {
                    throw new SQLException("Failed to obfuscate passwords while processing entities of type ["
                        + getEntityTypeDescription()
                        + "]. The failure happened while processing: configuration definition id: "
                        + confAndDef.getValue() + ", configuration id: " + confAndDef.getKey()
                        + ", property definition: " + entry.getKey() + ", property: " + entry.getValue(), e);
                }
            }
        }
    }

    private void processProperty(PropertyDefinition pd, Property p) throws Exception {
        switch (pd.type) {
        case LIST:
        case MAP:
            List<PropertyDefinition> childDefs = pd.getChildDefinitions();
            List<Property> childProps = p.getChildren();
            Map<PropertyDefinition, Property> pairs = matchDefinitionsAndProperties(childDefs, childProps);
            for (Map.Entry<PropertyDefinition, Property> entry : pairs.entrySet()) {
                processProperty(entry.getKey(), entry.getValue());
            }
            break;
        case SIMPLE:
            if (pd.simpleType == PropertySimpleType.PASSWORD && p.type == PropertyType.SIMPLE) {
                String sql;
                if (p.value != null) {
                    String obfuscatedValue = Obfuscator.encode(p.value);
                    sql =
                        "UPDATE rhq_config_property SET string_value = '" + obfuscatedValue
                            + "', dtype = 'obfuscated' WHERE id = " + p.id;
                } else {
                    sql = "UPDATE rhq_config_property SET dtype='obfuscated' WHERE id = " + p.id;
                }
                
                databaseType.executeSql(connection, sql);
            }
            break;
        }
    }

    private List<PropertyDefinition> getTopDefinitions(int configurationDefinitionId) throws SQLException {
        String sql =
            "SELECT id, name, dtype, simple_type FROM rhq_config_prop_def WHERE config_def_id = " + configurationDefinitionId;

        List<Object[]> results = databaseType.executeSelectSql(connection, sql);

        List<PropertyDefinition> ret = new ArrayList<PropertyDefinition>();
        for (Object[] row : results) {
            PropertyDefinition pd = new PropertyDefinition();
            pd.id = (Integer) row[0];
            pd.name = (String) row[1];
            pd.type = PropertyType.fromName((String) row[2]);
            pd.simpleType = row[3] != null ? PropertySimpleType.fromName((String) row[3]) : null;

            ret.add(pd);
        }

        return ret;
    }

    private List<Property> getTopProperties(int configurationId) throws SQLException {
        String sql =
            "SELECT id, name, dtype, string_value FROM rhq_config_property WHERE configuration_id = " + configurationId;

        List<Object[]> results = databaseType.executeSelectSql(connection, sql);

        List<Property> ret = new ArrayList<Property>();
        for (Object[] row : results) {
            Property p = new Property();
            p.id = (Integer) row[0];
            p.name = (String) row[1];
            p.type = PropertyType.fromName((String) row[2]);
            p.value = (String) row[3];

            ret.add(p);
        }

        return ret;
    }

    private Map<PropertyDefinition, Property> matchDefinitionsAndProperties(List<PropertyDefinition> defs,
        List<Property> props) {
        class DefAndProp {
            PropertyDefinition def;
            Property prop;
        }

        Map<String, DefAndProp> mapping = new HashMap<String, DefAndProp>();

        for (PropertyDefinition pd : defs) {
            DefAndProp dap = new DefAndProp();
            dap.def = pd;
            mapping.put(pd.name, dap);
        }

        for (Property p : props) {
            DefAndProp dap = mapping.get(p.name);
            if (dap != null) {
                dap.prop = p;
            }
        }

        Map<PropertyDefinition, Property> ret = new HashMap<PropertyDefinition, Property>();
        for (DefAndProp dap : mapping.values()) {
            ret.put(dap.def, dap.prop);
        }

        return ret;
    }
}
