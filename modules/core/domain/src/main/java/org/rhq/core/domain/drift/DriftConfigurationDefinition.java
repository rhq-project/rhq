/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
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
package org.rhq.core.domain.drift;

import java.io.Serializable;
import java.util.ArrayList;

import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.ConfigurationFormat;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionEnumeration;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;

/**
 * The drift subsystem has a fixed configuration definition. That is, its property definitions
 * are the same always. There is no metadata that needs to be read in from a descriptor - this definition
 * is fixed and the code requires all the property definitions to follow what is encoded in this POJO.
 * 
 * Note that this class must mimic the definition data as found in the database. The installer
 * will prepopulate the configuration definition tables that match the definitions encoded in this POJO.
 *  
 * @author Jay Shaughnessy
 * @author John Mazzitelli
 */
public class DriftConfigurationDefinition implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String PROP_NAME = "name";
    public static final String PROP_ENABLED = "enabled";
    public static final String PROP_BASEDIR = "basedir";
    public static final String PROP_BASEDIR_VALUECONTEXT = "valueContext";
    public static final String PROP_BASEDIR_VALUENAME = "valueName";
    public static final String PROP_INTERVAL = "interval";
    public static final String PROP_INCLUDES = "includes";
    public static final String PROP_INCLUDES_INCLUDE = "include";
    public static final String PROP_EXCLUDES = "excludes";
    public static final String PROP_EXCLUDES_EXCLUDE = "exclude";
    public static final String PROP_PATH = "path"; // for both include and exclude
    public static final String PROP_PATTERN = "pattern"; // for both include and exclude

    public static final boolean DEFAULT_ENABLED = false;
    public static final long DEFAULT_INTERVAL = 1800L;

    /**
     * The basedir property is specified in two parts - a "context" and a "name". Taken together
     * the value of the basedir can be determined. The value name is just a simple name that
     * is used to look up the basedir value within the appropriate context. A context can be
     * one of four places - either the value is a named property in a resource's plugin configuration,
     * a named property in a resource's resource configuration, a named trait that is emitted by a
     * resource or an absolute path found on a file system.
     */
    public enum BaseDirValueContext {
        pluginConfiguration, resourceConfiguration, measurementTrait, fileSystem
    }

    private static final ConfigurationDefinition INSTANCE = new ConfigurationDefinition("GLOBAL_DRIFT_CONFIG_DEF",
        "The drift configuration definition");

    public static ConfigurationDefinition getInstance() {
        return INSTANCE;
    }

    static {
        INSTANCE.setId(1);
        INSTANCE.setConfigurationFormat(ConfigurationFormat.STRUCTURED);

        INSTANCE.put(createName());
        INSTANCE.put(createEnabled());
        INSTANCE.put(createBasedir());
        INSTANCE.put(createInterval());
        INSTANCE.put(createIncludes());
        INSTANCE.put(createExcludes());

    }

    private static PropertyDefinitionSimple createName() {
        String name = PROP_NAME;
        String description = "The drift configuration name";
        boolean required = true;
        PropertySimpleType type = PropertySimpleType.STRING;

        PropertyDefinitionSimple pd = new PropertyDefinitionSimple(name, description, required, type);
        pd.setId(1);
        pd.setDisplayName("Drift Configuration Name");
        pd.setReadOnly(false);
        pd.setSummary(true);
        pd.setOrder(0);
        pd.setAllowCustomEnumeratedValue(false);
        pd.setConfigurationDefinition(INSTANCE);
        return pd;
    }

    private static PropertyDefinitionSimple createEnabled() {
        String name = PROP_ENABLED;
        String description = "Enables or disables the drift configuration";
        boolean required = true;
        PropertySimpleType type = PropertySimpleType.BOOLEAN;

        PropertyDefinitionSimple pd = new PropertyDefinitionSimple(name, description, required, type);
        pd.setId(2);
        pd.setDisplayName("Enabled");
        pd.setReadOnly(false);
        pd.setSummary(true);
        pd.setOrder(1);
        pd.setAllowCustomEnumeratedValue(false);
        pd.setConfigurationDefinition(INSTANCE);
        pd.setDefaultValue(String.valueOf(DEFAULT_ENABLED));
        return pd;
    }

    private static PropertyDefinitionMap createBasedir() {
        String name = PROP_BASEDIR;
        String description = "The root directory from which snapshots will be generated during drift monitoring.";
        boolean required = true;

        PropertyDefinitionSimple valueContext = createBasedirValueContext();
        PropertyDefinitionSimple valueName = createBasedirValueName();

        PropertyDefinitionMap pd = new PropertyDefinitionMap(name, description, required, valueContext, valueName);
        pd.setId(3);
        pd.setDisplayName("Base Directory");
        pd.setReadOnly(false);
        pd.setSummary(true);
        pd.setOrder(2);
        pd.setConfigurationDefinition(INSTANCE);

        return pd;
    }

    private static PropertyDefinitionSimple createBasedirValueContext() {
        String name = PROP_BASEDIR_VALUECONTEXT;
        String description = "Identifies where the named value can be found.";
        boolean required = true;
        PropertySimpleType type = PropertySimpleType.STRING;

        PropertyDefinitionSimple pd = new PropertyDefinitionSimple(name, description, required, type);
        pd.setId(4);
        pd.setDisplayName("Value Context");
        pd.setReadOnly(false);
        pd.setSummary(true);
        pd.setOrder(0);

        PropertyDefinitionEnumeration pcEnum = new PropertyDefinitionEnumeration(
            BaseDirValueContext.pluginConfiguration.name(), BaseDirValueContext.pluginConfiguration.name());
        pcEnum.setId(1);
        pcEnum.setOrderIndex(0);

        PropertyDefinitionEnumeration rcEnum = new PropertyDefinitionEnumeration(
            BaseDirValueContext.resourceConfiguration.name(), BaseDirValueContext.resourceConfiguration.name());
        rcEnum.setId(2);
        rcEnum.setOrderIndex(1);

        PropertyDefinitionEnumeration mtEnum = new PropertyDefinitionEnumeration(BaseDirValueContext.measurementTrait
            .name(), BaseDirValueContext.measurementTrait.name());
        mtEnum.setId(3);
        mtEnum.setOrderIndex(2);

        PropertyDefinitionEnumeration fsEnum = new PropertyDefinitionEnumeration(BaseDirValueContext.fileSystem.name(),
            BaseDirValueContext.fileSystem.name());
        fsEnum.setId(4);
        fsEnum.setOrderIndex(3);

        ArrayList<PropertyDefinitionEnumeration> pdEnums = new ArrayList<PropertyDefinitionEnumeration>(4);
        pdEnums.add(pcEnum);
        pdEnums.add(rcEnum);
        pdEnums.add(mtEnum);
        pdEnums.add(fsEnum);
        pd.setEnumeratedValues(pdEnums, false);

        return pd;
    }

    private static PropertyDefinitionSimple createBasedirValueName() {
        String name = PROP_BASEDIR_VALUENAME;
        String description = "The name of the value as found in the context";
        boolean required = true;
        PropertySimpleType type = PropertySimpleType.STRING;

        PropertyDefinitionSimple pd = new PropertyDefinitionSimple(name, description, required, type);
        pd.setId(5);
        pd.setDisplayName("Value Name");
        pd.setReadOnly(false);
        pd.setSummary(true);
        pd.setOrder(1);
        pd.setAllowCustomEnumeratedValue(false);
        return pd;
    }

    private static PropertyDefinitionSimple createInterval() {
        String name = PROP_INTERVAL;
        String description = "The frequency in seconds in which drift monitoring should run. Defaults to 1800 seconds (i.e. 30 minutes)";
        boolean required = false;
        PropertySimpleType type = PropertySimpleType.LONG;

        PropertyDefinitionSimple pd = new PropertyDefinitionSimple(name, description, required, type);
        pd.setId(6);
        pd.setDisplayName("Interval");
        pd.setReadOnly(false);
        pd.setSummary(true);
        pd.setOrder(3);
        pd.setAllowCustomEnumeratedValue(false);
        pd.setConfigurationDefinition(INSTANCE);
        pd.setDefaultValue(String.valueOf(DEFAULT_INTERVAL));
        return pd;
    }

    private static PropertyDefinitionList createIncludes() {
        String name = PROP_INCLUDES;
        String description = "A set of patterns that specify files and/or directories to include.";
        boolean required = false;

        PropertyDefinitionMap map = createInclude();

        PropertyDefinitionList pd = new PropertyDefinitionList(name, description, required, map);
        pd.setId(7);
        pd.setDisplayName("Includes");
        pd.setReadOnly(false);
        pd.setSummary(true);
        pd.setOrder(4);
        pd.setConfigurationDefinition(INSTANCE);
        return pd;
    }

    private static PropertyDefinitionMap createInclude() {
        String name = PROP_INCLUDES_INCLUDE;
        String description = "A set of patterns that specify files and/or directories to include.";
        boolean required = true;

        PropertyDefinitionSimple path = createIncludePath();
        PropertyDefinitionSimple pattern = createIncludePattern();

        PropertyDefinitionMap pd = new PropertyDefinitionMap(name, description, required, path, pattern);
        pd.setId(8);
        pd.setDisplayName("Include");
        pd.setReadOnly(false);
        pd.setSummary(true);
        pd.setOrder(0);
        return pd;
    }

    private static PropertyDefinitionSimple createIncludePath() {
        String name = PROP_PATH;
        String description = "A file system path that can be a directory or a file. The path is assumed to be relative to the base directory of the drift configuration.";
        boolean required = true;
        PropertySimpleType type = PropertySimpleType.STRING;

        PropertyDefinitionSimple pd = new PropertyDefinitionSimple(name, description, required, type);
        pd.setId(9);
        pd.setDisplayName("Path");
        pd.setReadOnly(false);
        pd.setSummary(true);
        pd.setOrder(0);
        pd.setAllowCustomEnumeratedValue(false);
        return pd;
    }

    private static PropertyDefinitionSimple createIncludePattern() {
        String name = PROP_PATTERN;
        String description = "Pathname pattern that must match for the items in the directory path to be included.";
        boolean required = false;
        PropertySimpleType type = PropertySimpleType.STRING;

        PropertyDefinitionSimple pd = new PropertyDefinitionSimple(name, description, required, type);
        pd.setId(10);
        pd.setDisplayName("Pattern");
        pd.setReadOnly(false);
        pd.setSummary(true);
        pd.setOrder(1);
        pd.setAllowCustomEnumeratedValue(false);
        return pd;
    }

    private static PropertyDefinitionList createExcludes() {
        String name = PROP_EXCLUDES;
        String description = "A set of patterns that specify files and/or directories to exclude.";
        boolean required = false;

        PropertyDefinitionMap map = createExclude();

        PropertyDefinitionList pd = new PropertyDefinitionList(name, description, required, map);
        pd.setId(11);
        pd.setDisplayName("Excludes");
        pd.setReadOnly(false);
        pd.setSummary(true);
        pd.setOrder(5);
        pd.setConfigurationDefinition(INSTANCE);
        return pd;
    }

    private static PropertyDefinitionMap createExclude() {
        String name = PROP_EXCLUDES_EXCLUDE;
        String description = "A set of patterns that specify files and/or directories to exclude.";
        boolean required = true;

        PropertyDefinitionSimple path = createExcludePath();
        PropertyDefinitionSimple pattern = createExcludePattern();

        PropertyDefinitionMap pd = new PropertyDefinitionMap(name, description, required, path, pattern);
        pd.setId(12);
        pd.setDisplayName("Exclude");
        pd.setReadOnly(false);
        pd.setSummary(true);
        pd.setOrder(0);
        return pd;
    }

    private static PropertyDefinitionSimple createExcludePath() {
        String name = PROP_PATH;
        String description = "A file system path that can be a directory or a file. The path is assumed to be relative to the base directory of the drift configuration.";
        boolean required = true;
        PropertySimpleType type = PropertySimpleType.STRING;

        PropertyDefinitionSimple pd = new PropertyDefinitionSimple(name, description, required, type);
        pd.setId(13);
        pd.setDisplayName("Path");
        pd.setReadOnly(false);
        pd.setSummary(true);
        pd.setOrder(0);
        pd.setAllowCustomEnumeratedValue(false);
        return pd;
    }

    private static PropertyDefinitionSimple createExcludePattern() {
        String name = PROP_PATTERN;
        String description = "Pathname pattern that must match for the items in the directory path to be excluded.";
        boolean required = false;
        PropertySimpleType type = PropertySimpleType.STRING;

        PropertyDefinitionSimple pd = new PropertyDefinitionSimple(name, description, required, type);
        pd.setId(14);
        pd.setDisplayName("Pattern");
        pd.setReadOnly(false);
        pd.setSummary(true);
        pd.setOrder(1);
        pd.setAllowCustomEnumeratedValue(false);
        return pd;
    }

}
