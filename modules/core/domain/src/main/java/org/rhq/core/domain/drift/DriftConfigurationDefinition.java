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
import org.rhq.core.domain.configuration.definition.constraint.RegexConstraint;

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
    public static final String PROP_DESCRIPTION = "description";
    public static final String PROP_ENABLED = "enabled";
    public static final String PROP_BASEDIR = "basedir";
    public static final String PROP_BASEDIR_VALUECONTEXT = "valueContext";
    public static final String PROP_BASEDIR_VALUENAME = "valueName";
    public static final String PROP_INTERVAL = "interval";
    public static final String PROP_DRIFT_HANDLING_MODE = "driftHandlingMode";
    public static final String PROP_PINNED = "pinned";
    public static final String PROP_ATTACHED = "attached";
    public static final String PROP_INCLUDES = "includes";
    public static final String PROP_INCLUDES_INCLUDE = "include";
    public static final String PROP_EXCLUDES = "excludes";
    public static final String PROP_EXCLUDES_EXCLUDE = "exclude";
    public static final String PROP_PATH = "path"; // for both include and exclude
    public static final String PROP_PATTERN = "pattern"; // for both include and exclude

    // because we know drift definition names will actually be used by the agent's plugin container as directories names,
    // we must make sure they are restricted to only be characters valid for file system pathnames.
    // Thus, we only allow config names to only include spaces or "." or "-" or alphanumeric or "_" characters.
    public static final String PROP_NAME_REGEX_PATTERN = "[ \\.\\-\\w]+";

    // because we know drift definition base directory paths and filter paths will actually be used by the agent's
    // plugin container as part of regex expressions, we must make sure they are restricted to only be characters
    // valid for file system paths and regex.  And, additionally for filter patterns, the (ant-style) filter
    // wildcards. Thus, we only allow the base paths to include the separator characters "\", "/", the windows
    // drive character ":", parentheses to support windows (x86) type system directories, and the filename characters
    // (see PROP_NAME_REGEX_PATTERN).  Filter paths can contain the same as the base path, less the drive character.
    // And filter patterns are the same as filter paths, plus the wildcard characters. "*", "?".
    public static final String PROP_BASEDIR_PATH_REGEX_PATTERN = "[ \\.\\-\\(\\)\\w/\\:\\\\]+";
    public static final String PROP_FILTER_PATH_REGEX_PATTERN = "[ \\.\\-\\(\\)\\w/\\\\]+";
    public static final String PROP_FILTER_PATTERN_REGEX_PATTERN = "[ \\.\\-\\(\\)\\w/\\\\\\?\\*]+";

    public static final boolean DEFAULT_ENABLED = true;
    public static final boolean DEFAULT_ATTACHED = true;
    public static final long DEFAULT_INTERVAL = 1800L;
    public static final DriftHandlingMode DEFAULT_DRIFT_HANDLING_MODE = DriftHandlingMode.normal;

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

    /**
     * The enumerated values for drift handling mode property
     */
    public enum DriftHandlingMode {
        normal, plannedChanges
    }

    private static final ConfigurationDefinition INSTANCE = new ConfigurationDefinition("GLOBAL_DRIFT_CONFIG_DEF",
        "The drift detection definition");

    /**
     * For drift definitions that have already been created, this definition can be used for editing those existing configuration.
     * Existing drift definitions cannot have their name changed nor can their base directory or includes/excludes be altered.
     */
    private static final ConfigurationDefinition INSTANCE_FOR_EXISTING_CONFIGS = new ConfigurationDefinition(
        "GLOBAL_DRIFT_CONFIG_DEF", "The drift detection definition");

    private static final ConfigurationDefinition NEW_RESOURCE_INSTANCE_BY_PINNED_TEMPLATE = new ConfigurationDefinition(
        "NEW_RESOURCE_DRIFT_DEF_BY_PINNED_TEMPLATE", "A new resource drift definition created from a pinned template");

    private static final ConfigurationDefinition EXISTING_RESOURCE_INSTANCE_BY_PINNED_TEMPLATE = new ConfigurationDefinition(
        "EXISTING_RESOURCE_DRIFT_DEF_BY_PINNED_TEMPLATE",
        "An existing resource drift definition created from a pinned template");

    private static final ConfigurationDefinition NEW_TEMPLATE_INSTANCE = new ConfigurationDefinition(
        "NEW_TEMPLATE_DRIFT_CONFIG_DEF", "A new template drift definition");

    private static final ConfigurationDefinition EXISTING_TEMPLATE_INSTANCE = new ConfigurationDefinition(
        "EXISTING_TEMPLATE_DRIFT_CONFIG_DEF", "An existing template drift definition");

    private static final ConfigurationDefinition NEW_PINNED_TEMPLATE_INSTANCE = new ConfigurationDefinition(
        "NEW_PINNED_TEMPLATE_DRIFT_CONFIG_DEF", "A new pinned template drift definition");

    /**
     * Returns a configuration definition suitable for showing a new configuration form - that is,
     * a configuration that has not yet been created.
     * This will allow all fields to be editable.
     * If you need a configuration definition to show an existing configuration, use the definition
     * returned by {@link #getInstanceForExistingConfiguration()}.
     * 
     * @return configuration definition
     */
    public static ConfigurationDefinition getInstance() {
        return INSTANCE;
    }

    /**
     * Returns a configuration definition suitable for showing an existing drift definition.
     * This will set certain fields as read-only - those fields which the user is not allowed to
     * edit on exiting drift definition (which includes name, basedir and includes/excludes filters).
     * 
     * @return configuration definition
     */
    public static ConfigurationDefinition getInstanceForExistingConfiguration() {
        return INSTANCE_FOR_EXISTING_CONFIGS;
    }

    public static ConfigurationDefinition getNewResourceInstanceByPinnedTemplate() {
        return NEW_RESOURCE_INSTANCE_BY_PINNED_TEMPLATE;
    }

    public static ConfigurationDefinition getExistingResourceInstanceByPinnedTemplate() {
        return EXISTING_RESOURCE_INSTANCE_BY_PINNED_TEMPLATE;
    }

    public static ConfigurationDefinition getNewTemplateInstance() {
        return NEW_TEMPLATE_INSTANCE;
    }

    public static ConfigurationDefinition getExistingTemplateInstance() {
        return EXISTING_TEMPLATE_INSTANCE;
    }

    public static ConfigurationDefinition getNewPinnedTemplateInstance() {
        return NEW_PINNED_TEMPLATE_INSTANCE;
    }

    static {
        INSTANCE.setConfigurationFormat(ConfigurationFormat.STRUCTURED);
        INSTANCE.put(createName(INSTANCE, false));
        INSTANCE.put(createDescription(INSTANCE));
        INSTANCE.put(createEnabled(INSTANCE));
        INSTANCE.put(createDriftHandlingMode(INSTANCE));
        INSTANCE.put(createInterval(INSTANCE));
        INSTANCE.put(createBasedir(INSTANCE, false));
        INSTANCE.put(createIncludes(INSTANCE, false));
        INSTANCE.put(createExcludes(INSTANCE, false));
        INSTANCE.put(createPinned(INSTANCE, false));
        INSTANCE.put(createAttached(INSTANCE, false));

        INSTANCE_FOR_EXISTING_CONFIGS.setConfigurationFormat(ConfigurationFormat.STRUCTURED);
        INSTANCE_FOR_EXISTING_CONFIGS.put(createName(INSTANCE_FOR_EXISTING_CONFIGS, true));
        INSTANCE_FOR_EXISTING_CONFIGS.put(createDescription(INSTANCE_FOR_EXISTING_CONFIGS));
        INSTANCE_FOR_EXISTING_CONFIGS.put(createEnabled(INSTANCE_FOR_EXISTING_CONFIGS));
        INSTANCE_FOR_EXISTING_CONFIGS.put(createDriftHandlingMode(INSTANCE_FOR_EXISTING_CONFIGS));
        INSTANCE_FOR_EXISTING_CONFIGS.put(createInterval(INSTANCE_FOR_EXISTING_CONFIGS));
        INSTANCE_FOR_EXISTING_CONFIGS.put(createBasedir(INSTANCE_FOR_EXISTING_CONFIGS, true));
        INSTANCE_FOR_EXISTING_CONFIGS.put(createIncludes(INSTANCE_FOR_EXISTING_CONFIGS, true));
        INSTANCE_FOR_EXISTING_CONFIGS.put(createExcludes(INSTANCE_FOR_EXISTING_CONFIGS, true));
        INSTANCE_FOR_EXISTING_CONFIGS.put(createExcludes(INSTANCE_FOR_EXISTING_CONFIGS, true));
        INSTANCE_FOR_EXISTING_CONFIGS.put(createAttached(INSTANCE_FOR_EXISTING_CONFIGS, false));
        INSTANCE_FOR_EXISTING_CONFIGS.put(createPinned(INSTANCE_FOR_EXISTING_CONFIGS, false));

        NEW_RESOURCE_INSTANCE_BY_PINNED_TEMPLATE.setConfigurationFormat(ConfigurationFormat.STRUCTURED);
        NEW_RESOURCE_INSTANCE_BY_PINNED_TEMPLATE.put(createName(NEW_RESOURCE_INSTANCE_BY_PINNED_TEMPLATE, false));
        NEW_RESOURCE_INSTANCE_BY_PINNED_TEMPLATE.put(createDescription(NEW_RESOURCE_INSTANCE_BY_PINNED_TEMPLATE));
        NEW_RESOURCE_INSTANCE_BY_PINNED_TEMPLATE.put(createInterval(NEW_RESOURCE_INSTANCE_BY_PINNED_TEMPLATE));
        NEW_RESOURCE_INSTANCE_BY_PINNED_TEMPLATE.put(createEnabled(NEW_RESOURCE_INSTANCE_BY_PINNED_TEMPLATE));
        NEW_RESOURCE_INSTANCE_BY_PINNED_TEMPLATE.put(createDriftHandlingMode(NEW_RESOURCE_INSTANCE_BY_PINNED_TEMPLATE));
        NEW_RESOURCE_INSTANCE_BY_PINNED_TEMPLATE.put(createBasedir(NEW_RESOURCE_INSTANCE_BY_PINNED_TEMPLATE, true));
        NEW_RESOURCE_INSTANCE_BY_PINNED_TEMPLATE.put(createIncludes(NEW_RESOURCE_INSTANCE_BY_PINNED_TEMPLATE, true));
        NEW_RESOURCE_INSTANCE_BY_PINNED_TEMPLATE.put(createExcludes(NEW_RESOURCE_INSTANCE_BY_PINNED_TEMPLATE, true));
        NEW_RESOURCE_INSTANCE_BY_PINNED_TEMPLATE.put(createPinned(NEW_RESOURCE_INSTANCE_BY_PINNED_TEMPLATE, true));
        NEW_RESOURCE_INSTANCE_BY_PINNED_TEMPLATE.put(createAttached(NEW_RESOURCE_INSTANCE_BY_PINNED_TEMPLATE, true));

        EXISTING_RESOURCE_INSTANCE_BY_PINNED_TEMPLATE.setConfigurationFormat(ConfigurationFormat.STRUCTURED);
        EXISTING_RESOURCE_INSTANCE_BY_PINNED_TEMPLATE.put(createName(EXISTING_RESOURCE_INSTANCE_BY_PINNED_TEMPLATE,
            true));
        EXISTING_RESOURCE_INSTANCE_BY_PINNED_TEMPLATE
            .put(createDescription(EXISTING_RESOURCE_INSTANCE_BY_PINNED_TEMPLATE));
        EXISTING_RESOURCE_INSTANCE_BY_PINNED_TEMPLATE
            .put(createInterval(EXISTING_RESOURCE_INSTANCE_BY_PINNED_TEMPLATE));
        EXISTING_RESOURCE_INSTANCE_BY_PINNED_TEMPLATE.put(createEnabled(EXISTING_RESOURCE_INSTANCE_BY_PINNED_TEMPLATE));
        EXISTING_RESOURCE_INSTANCE_BY_PINNED_TEMPLATE
            .put(createDriftHandlingMode(EXISTING_RESOURCE_INSTANCE_BY_PINNED_TEMPLATE));
        EXISTING_RESOURCE_INSTANCE_BY_PINNED_TEMPLATE.put(createBasedir(EXISTING_RESOURCE_INSTANCE_BY_PINNED_TEMPLATE,
            true));
        EXISTING_RESOURCE_INSTANCE_BY_PINNED_TEMPLATE.put(createIncludes(EXISTING_RESOURCE_INSTANCE_BY_PINNED_TEMPLATE,
            true));
        EXISTING_RESOURCE_INSTANCE_BY_PINNED_TEMPLATE.put(createExcludes(EXISTING_RESOURCE_INSTANCE_BY_PINNED_TEMPLATE,
            true));
        EXISTING_RESOURCE_INSTANCE_BY_PINNED_TEMPLATE.put(createPinned(EXISTING_RESOURCE_INSTANCE_BY_PINNED_TEMPLATE,
            true));
        EXISTING_RESOURCE_INSTANCE_BY_PINNED_TEMPLATE.put(createAttached(EXISTING_RESOURCE_INSTANCE_BY_PINNED_TEMPLATE,
            true));

        NEW_TEMPLATE_INSTANCE.setConfigurationFormat(ConfigurationFormat.STRUCTURED);
        NEW_TEMPLATE_INSTANCE.put(createName(NEW_TEMPLATE_INSTANCE, false));
        NEW_TEMPLATE_INSTANCE.put(createDescription(NEW_TEMPLATE_INSTANCE));
        NEW_TEMPLATE_INSTANCE.put(createInterval(NEW_TEMPLATE_INSTANCE));
        NEW_TEMPLATE_INSTANCE.put(createEnabled(NEW_TEMPLATE_INSTANCE));
        NEW_TEMPLATE_INSTANCE.put(createDriftHandlingMode(NEW_TEMPLATE_INSTANCE));
        NEW_TEMPLATE_INSTANCE.put(createBasedir(NEW_TEMPLATE_INSTANCE, false));
        NEW_TEMPLATE_INSTANCE.put(createIncludes(NEW_TEMPLATE_INSTANCE, false));
        NEW_TEMPLATE_INSTANCE.put(createExcludes(NEW_TEMPLATE_INSTANCE, false));

        EXISTING_TEMPLATE_INSTANCE.setConfigurationFormat(ConfigurationFormat.STRUCTURED);
        EXISTING_TEMPLATE_INSTANCE.put(createName(EXISTING_TEMPLATE_INSTANCE, true));
        EXISTING_TEMPLATE_INSTANCE.put(createDescription(EXISTING_TEMPLATE_INSTANCE));
        EXISTING_TEMPLATE_INSTANCE.put(createInterval(EXISTING_TEMPLATE_INSTANCE));
        EXISTING_TEMPLATE_INSTANCE.put(createEnabled(EXISTING_TEMPLATE_INSTANCE));
        EXISTING_TEMPLATE_INSTANCE.put(createDriftHandlingMode(EXISTING_TEMPLATE_INSTANCE));
        EXISTING_TEMPLATE_INSTANCE.put(createBasedir(EXISTING_TEMPLATE_INSTANCE, true));
        EXISTING_TEMPLATE_INSTANCE.put(createIncludes(EXISTING_TEMPLATE_INSTANCE, true));
        EXISTING_TEMPLATE_INSTANCE.put(createExcludes(EXISTING_TEMPLATE_INSTANCE, true));

        NEW_PINNED_TEMPLATE_INSTANCE.setConfigurationFormat(ConfigurationFormat.STRUCTURED);
        NEW_PINNED_TEMPLATE_INSTANCE.put(createName(NEW_PINNED_TEMPLATE_INSTANCE, false));
        NEW_PINNED_TEMPLATE_INSTANCE.put(createDescription(NEW_PINNED_TEMPLATE_INSTANCE));
        NEW_PINNED_TEMPLATE_INSTANCE.put(createInterval(NEW_PINNED_TEMPLATE_INSTANCE));
        NEW_PINNED_TEMPLATE_INSTANCE.put(createEnabled(NEW_PINNED_TEMPLATE_INSTANCE));
        NEW_PINNED_TEMPLATE_INSTANCE.put(createDriftHandlingMode(NEW_PINNED_TEMPLATE_INSTANCE));
        NEW_PINNED_TEMPLATE_INSTANCE.put(createBasedir(NEW_PINNED_TEMPLATE_INSTANCE, true));
        NEW_PINNED_TEMPLATE_INSTANCE.put(createIncludes(NEW_PINNED_TEMPLATE_INSTANCE, true));
        NEW_PINNED_TEMPLATE_INSTANCE.put(createExcludes(NEW_PINNED_TEMPLATE_INSTANCE, true));
    }

    private static PropertyDefinitionSimple createName(ConfigurationDefinition configDef, boolean readOnly) {
        String name = PROP_NAME;
        String description = "The drift detection definition name";
        boolean required = true;
        PropertySimpleType type = PropertySimpleType.STRING;

        PropertyDefinitionSimple pd = new PropertyDefinitionSimple(name, description, required, type);
        pd.setDisplayName("Drift Definition Name");
        pd.setReadOnly(readOnly);
        pd.setSummary(true);
        pd.setOrder(0);
        pd.setAllowCustomEnumeratedValue(false);
        pd.setConfigurationDefinition(configDef);

        RegexConstraint constraint = new RegexConstraint();
        constraint.setDetails(PROP_NAME_REGEX_PATTERN);
        pd.addConstraints(constraint);

        return pd;
    }

    private static PropertyDefinitionSimple createDescription(ConfigurationDefinition configDef) {
        String name = PROP_DESCRIPTION;
        String description = "A description of the drift detection definition or template";
        boolean required = false;
        PropertySimpleType type = PropertySimpleType.STRING;

        PropertyDefinitionSimple pd = new PropertyDefinitionSimple(name, description, required, type);
        pd.setDisplayName("Drift Definition Description");
        pd.setSummary(true);
        pd.setOrder(1);
        pd.setAllowCustomEnumeratedValue(false);
        pd.setConfigurationDefinition(configDef);

        return pd;
    }

    private static PropertyDefinitionSimple createEnabled(ConfigurationDefinition configDef) {
        String name = PROP_ENABLED;
        String description = "Enables or disables the drift definition";
        boolean required = true;
        PropertySimpleType type = PropertySimpleType.BOOLEAN;

        PropertyDefinitionSimple pd = new PropertyDefinitionSimple(name, description, required, type);
        pd.setDisplayName("Enabled");
        pd.setReadOnly(false);
        pd.setSummary(true);
        pd.setOrder(2);
        pd.setAllowCustomEnumeratedValue(false);
        pd.setConfigurationDefinition(configDef);
        pd.setDefaultValue(String.valueOf(DEFAULT_ENABLED));
        return pd;
    }

    private static PropertyDefinitionSimple createAttached(ConfigurationDefinition configDef, boolean readOnly) {
        String name = PROP_ATTACHED;
        String description = "A flag that indicates whether or not the definition is attached to the template from "
            + "which it is created. When a template is updated, the changes will be propagated to any attached "
            + "definitions. Furthermore, if you pin an existing template to a snapshot, then attached definitions will "
            + "become pinned as well. Finally, if you delete a template, attached definitions will also be deleted.";
        boolean required = true;
        PropertySimpleType type = PropertySimpleType.BOOLEAN;

        PropertyDefinitionSimple pd = new PropertyDefinitionSimple(name, description, required, type);
        pd.setDisplayName("Attached to Template");
        pd.setDefaultValue("true");
        pd.setOrder(3);
        pd.setReadOnly(readOnly);
        pd.setConfigurationDefinition(configDef);

        return pd;
    }

    private static PropertyDefinitionSimple createDriftHandlingMode(ConfigurationDefinition configDef) {
        String name = PROP_DRIFT_HANDLING_MODE;
        String description = "" //
            + "Specifies the way in which drift instances will be handled when reported. Normal " //
            + "handling implies the reported drift is unexpected and as such can trigger alerts, " //
            + "will be present in recent drift reports, etc.  Setting to 'Planned Changes' implies " //
            + "that the reported drift is happening at a time when drift is expected due to " //
            + "planned changes in the monitored environment, such as an application deployment, a " //
            + "configuration change, or something similar.  With this setting drift is only reported " //
            + " for inspection, in drift snapshot views.";
        boolean required = true;
        PropertySimpleType type = PropertySimpleType.STRING;

        PropertyDefinitionSimple pd = new PropertyDefinitionSimple(name, description, required, type);
        pd.setDisplayName("Drift Handling Mode");
        pd.setReadOnly(false);
        pd.setSummary(true);
        pd.setOrder(4);
        pd.setConfigurationDefinition(configDef);

        PropertyDefinitionEnumeration normalEnum = new PropertyDefinitionEnumeration(DriftHandlingMode.normal.name(),
            DriftHandlingMode.normal.name());
        normalEnum.setOrderIndex(0);

        PropertyDefinitionEnumeration plannedEnum = new PropertyDefinitionEnumeration(DriftHandlingMode.plannedChanges
            .name(), DriftHandlingMode.plannedChanges.name());
        plannedEnum.setOrderIndex(1);

        ArrayList<PropertyDefinitionEnumeration> pdEnums = new ArrayList<PropertyDefinitionEnumeration>(2);
        pdEnums.add(normalEnum);
        pdEnums.add(plannedEnum);
        pd.setEnumeratedValues(pdEnums, false);
        pd.setDefaultValue(DEFAULT_DRIFT_HANDLING_MODE.name());

        return pd;
    }

    private static PropertyDefinitionSimple createPinned(ConfigurationDefinition configDef, boolean readOnly) {
        String name = PROP_PINNED;
        String description = "If set, pins the snapshot that the agent uses for comparing files during drift "
            + "detection. Normally, the agent compares those files being monitored for drift against the latest "
            + "snapshot. If you pin a snapshot, the agent will use that pinned version to compare against files "
            + "being monitored for drift";
        boolean required = true;
        PropertySimpleType type = PropertySimpleType.BOOLEAN;

        PropertyDefinitionSimple pd = new PropertyDefinitionSimple(name, description, required, type);
        pd.setDisplayName("Pinned");
        pd.setDefaultValue("false");
        pd.setOrder(5);
        pd.setReadOnly(readOnly);
        pd.setConfigurationDefinition(configDef);

        return pd;
    }

    private static PropertyDefinitionSimple createInterval(ConfigurationDefinition configDef) {
        String name = PROP_INTERVAL;
        String description = "The frequency in seconds in which drift detection should run. Defaults to 1800 seconds (i.e. 30 minutes). This value must be" +
                "higher than (or equal to) agent's interval.";
        boolean required = false;
        PropertySimpleType type = PropertySimpleType.LONG;

        PropertyDefinitionSimple pd = new PropertyDefinitionSimple(name, description, required, type);
        pd.setDisplayName("Interval");
        pd.setReadOnly(false);
        pd.setSummary(true);
        pd.setOrder(6);
        pd.setAllowCustomEnumeratedValue(false);
        pd.setConfigurationDefinition(configDef);
        pd.setDefaultValue(String.valueOf(DEFAULT_INTERVAL));
        return pd;
    }

    private static PropertyDefinitionMap createBasedir(ConfigurationDefinition configDef, boolean readOnly) {
        String name = PROP_BASEDIR;
        String description = "The root directory from which snapshots will be generated during drift monitoring.";
        boolean required = true;

        PropertyDefinitionSimple valueContext = createBasedirValueContext(readOnly);
        PropertyDefinitionSimple valueName = createBasedirValueName(readOnly);

        PropertyDefinitionMap pd = new PropertyDefinitionMap(name, description, required, valueContext, valueName);
        pd.setDisplayName("Base Directory");
        pd.setReadOnly(readOnly);
        pd.setSummary(true);
        pd.setOrder(7);
        pd.setConfigurationDefinition(configDef);

        return pd;
    }

    private static PropertyDefinitionSimple createBasedirValueContext(boolean readOnly) {
        String name = PROP_BASEDIR_VALUECONTEXT;
        String description = "Identifies where the named value can be found.";
        boolean required = true;
        PropertySimpleType type = PropertySimpleType.STRING;

        PropertyDefinitionSimple pd = new PropertyDefinitionSimple(name, description, required, type);
        pd.setDisplayName("Value Context");
        pd.setReadOnly(readOnly);
        pd.setSummary(true);
        pd.setOrder(0);

        PropertyDefinitionEnumeration pcEnum = new PropertyDefinitionEnumeration(
            BaseDirValueContext.pluginConfiguration.name(), BaseDirValueContext.pluginConfiguration.name());
        pcEnum.setOrderIndex(0);

        PropertyDefinitionEnumeration rcEnum = new PropertyDefinitionEnumeration(
            BaseDirValueContext.resourceConfiguration.name(), BaseDirValueContext.resourceConfiguration.name());
        rcEnum.setOrderIndex(1);

        PropertyDefinitionEnumeration mtEnum = new PropertyDefinitionEnumeration(BaseDirValueContext.measurementTrait
            .name(), BaseDirValueContext.measurementTrait.name());
        mtEnum.setOrderIndex(2);

        PropertyDefinitionEnumeration fsEnum = new PropertyDefinitionEnumeration(BaseDirValueContext.fileSystem.name(),
            BaseDirValueContext.fileSystem.name());
        fsEnum.setOrderIndex(3);

        ArrayList<PropertyDefinitionEnumeration> pdEnums = new ArrayList<PropertyDefinitionEnumeration>(4);
        pdEnums.add(pcEnum);
        pdEnums.add(rcEnum);
        pdEnums.add(mtEnum);
        pdEnums.add(fsEnum);
        pd.setEnumeratedValues(pdEnums, false);

        return pd;
    }

    private static PropertyDefinitionSimple createBasedirValueName(boolean readOnly) {
        String name = PROP_BASEDIR_VALUENAME;
        String description = "The name of the value as found in the context.";
        boolean required = true;
        PropertySimpleType type = PropertySimpleType.STRING;

        PropertyDefinitionSimple pd = new PropertyDefinitionSimple(name, description, required, type);
        pd.setDisplayName("Value Name");
        pd.setReadOnly(readOnly);
        pd.setSummary(true);
        pd.setOrder(1);
        pd.setAllowCustomEnumeratedValue(false);

        RegexConstraint constraint = new RegexConstraint();
        constraint.setDetails(PROP_BASEDIR_PATH_REGEX_PATTERN);
        pd.addConstraints(constraint);

        return pd;
    }

    private static PropertyDefinitionList createIncludes(ConfigurationDefinition configDef, boolean readOnly) {
        String name = PROP_INCLUDES;
        String description = "A set of patterns that specify files and/or directories to include.";
        boolean required = false;

        PropertyDefinitionMap map = createInclude(readOnly);

        PropertyDefinitionList pd = new PropertyDefinitionList(name, description, required, map);
        pd.setDisplayName("Includes");
        pd.setReadOnly(readOnly);
        pd.setSummary(true);
        pd.setOrder(8);
        pd.setConfigurationDefinition(configDef);

        return pd;
    }

    private static PropertyDefinitionMap createInclude(boolean readOnly) {
        String name = PROP_INCLUDES_INCLUDE;
        String description = "A pattern that specifies a file or directory to include.";
        boolean required = true;

        PropertyDefinitionSimple path = createIncludePath(readOnly);
        PropertyDefinitionSimple pattern = createIncludePattern(readOnly);

        PropertyDefinitionMap pd = new PropertyDefinitionMap(name, description, required, path, pattern);
        pd.setDisplayName("Include");
        pd.setReadOnly(readOnly);
        pd.setSummary(true);
        pd.setOrder(0);

        return pd;
    }

    private static PropertyDefinitionSimple createIncludePath(boolean readOnly) {
        String name = PROP_PATH;
        String description = "A file system directory path that is relative to (a sub-directory of) the base directory of the drift definition. The default is '.', the base directory itself.  Note that '/' and './' will be normalized to '.' for consistent handling.";
        boolean required = false;
        PropertySimpleType type = PropertySimpleType.STRING;

        PropertyDefinitionSimple pd = new PropertyDefinitionSimple(name, description, required, type);
        pd.setDisplayName("Path");
        pd.setReadOnly(readOnly);
        pd.setSummary(true);
        pd.setOrder(0);
        pd.setAllowCustomEnumeratedValue(false);
        pd.setDefaultValue(".");

        RegexConstraint constraint = new RegexConstraint();
        constraint.setDetails(PROP_FILTER_PATH_REGEX_PATTERN);
        pd.addConstraints(constraint);

        return pd;
    }

    private static PropertyDefinitionSimple createIncludePattern(boolean readOnly) {
        String name = PROP_PATTERN;
        String description = "Pathname pattern that must match for the items in the directory path to be included. '*' matches zero or more characters, '?' matches one character. For example, '*.txt' (no quotes) will match text files in the filter's path directory.  '**/*.txt' will match text files in any subdirectory below the filter's path directory.";
        boolean required = false;
        PropertySimpleType type = PropertySimpleType.STRING;

        PropertyDefinitionSimple pd = new PropertyDefinitionSimple(name, description, required, type);
        pd.setDisplayName("Pattern");
        pd.setReadOnly(readOnly);
        pd.setSummary(true);
        pd.setOrder(1);
        pd.setAllowCustomEnumeratedValue(false);

        RegexConstraint constraint = new RegexConstraint();
        constraint.setDetails(PROP_FILTER_PATTERN_REGEX_PATTERN);
        pd.addConstraints(constraint);

        return pd;
    }

    private static PropertyDefinitionList createExcludes(ConfigurationDefinition configDef, boolean readOnly) {
        String name = PROP_EXCLUDES;
        String description = "A set of patterns that specify files and/or directories to exclude.";
        boolean required = false;

        PropertyDefinitionMap map = createExclude(readOnly);

        PropertyDefinitionList pd = new PropertyDefinitionList(name, description, required, map);
        pd.setDisplayName("Excludes");
        pd.setReadOnly(readOnly);
        pd.setSummary(true);
        pd.setOrder(9);
        pd.setConfigurationDefinition(configDef);
        return pd;
    }

    private static PropertyDefinitionMap createExclude(boolean readOnly) {
        String name = PROP_EXCLUDES_EXCLUDE;
        String description = "A pattern that specifies a file or directory to exclude.";
        boolean required = true;

        PropertyDefinitionSimple path = createExcludePath(readOnly);
        PropertyDefinitionSimple pattern = createExcludePattern(readOnly);

        PropertyDefinitionMap pd = new PropertyDefinitionMap(name, description, required, path, pattern);
        pd.setDisplayName("Exclude");
        pd.setReadOnly(readOnly);
        pd.setSummary(true);
        pd.setOrder(0);
        return pd;
    }

    private static PropertyDefinitionSimple createExcludePath(boolean readOnly) {
        String name = PROP_PATH;
        String description = "A file system directory path that is relative to (a sub-directory of) the base directory of the drift definition. The default is '.', the base directory itself.  Note that '/' and './' will be normalized to '.' for consistent handling.";
        boolean required = true;
        PropertySimpleType type = PropertySimpleType.STRING;

        PropertyDefinitionSimple pd = new PropertyDefinitionSimple(name, description, required, type);
        pd.setDisplayName("Path");
        pd.setReadOnly(readOnly);
        pd.setSummary(true);
        pd.setOrder(0);
        pd.setAllowCustomEnumeratedValue(false);
        return pd;
    }

    private static PropertyDefinitionSimple createExcludePattern(boolean readOnly) {
        String name = PROP_PATTERN;
        String description = "Pathname pattern that must match for the items in the directory path to be excluded.";
        boolean required = false;
        PropertySimpleType type = PropertySimpleType.STRING;

        PropertyDefinitionSimple pd = new PropertyDefinitionSimple(name, description, required, type);
        pd.setDisplayName("Pattern");
        pd.setReadOnly(readOnly);
        pd.setSummary(true);
        pd.setOrder(1);
        pd.setAllowCustomEnumeratedValue(false);
        return pd;
    }

}
