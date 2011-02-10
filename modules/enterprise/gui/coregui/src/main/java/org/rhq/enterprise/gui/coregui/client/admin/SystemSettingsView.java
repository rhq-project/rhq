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

package org.rhq.enterprise.gui.coregui.client.admin;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.toolbar.ToolStrip;

import org.rhq.core.domain.common.ProductInfo;
import org.rhq.core.domain.common.ServerDetails;
import org.rhq.core.domain.common.ServerDetails.Detail;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertyGroupDefinition;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.core.domain.configuration.definition.constraint.IntegerRangeConstraint;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.TitleBar;
import org.rhq.enterprise.gui.coregui.client.components.configuration.ConfigurationEditor;
import org.rhq.enterprise.gui.coregui.client.components.configuration.PropertyValueChangeEvent;
import org.rhq.enterprise.gui.coregui.client.components.configuration.PropertyValueChangeListener;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.message.Message.Severity;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableIButton;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * A simple form to view and edit the server system settings.
 * 
 * @author John Mazzitelli
 */
public class SystemSettingsView extends LocatableVLayout implements PropertyValueChangeListener {

    private LocatableVLayout canvas;
    private ConfigurationEditor editor;
    private IButton saveButton;

    private interface Constant {
        // note: all these names of simple properties must match those in the server's RHQConstants
        String BaseURL = "CAM_BASE_URL";
        String AgentMaxQuietTimeAllowed = "AGENT_MAX_QUIET_TIME_ALLOWED";
        String EnableAgentAutoUpdate = "ENABLE_AGENT_AUTO_UPDATE";
        String EnableDebugMode = "ENABLE_DEBUG_MODE";
        String EnableExperimentalFeatures = "ENABLE_EXPERIMENTAL_FEATURES";
        String DataMaintenance = "CAM_DATA_MAINTENANCE";
        String AvailabilityPurge = "AVAILABILITY_PURGE";
        String AlertPurge = "ALERT_PURGE";
        String TraitPurge = "TRAIT_PURGE";
        String RtDataPurge = "RT_DATA_PURGE";
        String EventPurge = "EVENT_PURGE";
        String DataReindex = "DATA_REINDEX_NIGHTLY";
        String BaselineFrequency = "CAM_BASELINE_FREQUENCY";
        String BaselineDataSet = "CAM_BASELINE_DATASET";

        String JAASProvider = "CAM_JAAS_PROVIDER"; // value must be one of JDBCJAASProvider or LDAPJAASProvider
        String JDBCJAASProvider = "JDBC"; // this isn't really a property name, its a value for the JAASProvider property
        String LDAPJAASProvider = "LDAP"; // this isn't really a property name, its a value for the JAASProvider property
        String LDAPUrl = "CAM_LDAP_NAMING_PROVIDER_URL";
        String LDAPProtocol = "CAM_LDAP_PROTOCOL"; // must be either "ssl" or empty string ("")
        String LDAPLoginProperty = "CAM_LDAP_LOGIN_PROPERTY";
        String LDAPFilter = "CAM_LDAP_FILTER";
        String LDAPGroupFilter = "CAM_LDAP_GROUP_FILTER";
        String LDAPGroupMember = "CAM_LDAP_GROUP_MEMBER";
        String LDAPBaseDN = "CAM_LDAP_BASE_DN";
        String LDAPBindDN = "CAM_LDAP_BIND_DN";
        String LDAPBindPW = "CAM_LDAP_BIND_PW";
    }

    public SystemSettingsView(String locatorId) {
        super(locatorId);
        setHeight100();
        setWidth100();

        TitleBar titleBar = new TitleBar(this, "System Settings", "subsystems/configure/Configure_24.png");
        addMember(titleBar);

        canvas = new LocatableVLayout("innerLayout");
        canvas.setHeight100();
        canvas.setWidth100();
        canvas.setMargin(15);
        addMember(canvas);
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        GWTServiceLookup.getSystemService().getSystemConfiguration(new AsyncCallback<HashMap<String, String>>() {
            @Override
            public void onSuccess(HashMap<String, String> result) {

                canvas.addMember(getServerDetails());

                Configuration config = new Configuration();
                for (Map.Entry<String, String> entry : result.entrySet()) {
                    String name = entry.getKey();
                    String value = (entry.getValue() == null) ? "" : entry.getValue();

                    // some of our properties are actually different values on the server than how they are to be
                    // visualized in the UI.
                    // -- JAASProvider is a boolean in the UI but is "LDAP" if it was true and "JDBC" if it was false
                    // -- LDAPProtocol is a boolean in the UI but is "ssl" if true and "" if it was false
                    // -- some other numerical values need to be converted from milliseconds
                    if (Constant.JAASProvider.equals(name)) {
                        value = Boolean.toString(value.equals(Constant.LDAPJAASProvider));
                    } else if (Constant.LDAPProtocol.equals(name)) {
                        value = Boolean.toString(value.equalsIgnoreCase("ssl"));
                    } else if (Constant.AgentMaxQuietTimeAllowed.equals(name)) {
                        value = convertMillisToMinutes(value);
                    } else if (Constant.DataMaintenance.equals(name)) {
                        value = convertMillisToHours(value);
                    } else if (Constant.AvailabilityPurge.equals(name) || Constant.AlertPurge.equals(name)
                        || Constant.TraitPurge.equals(name) || Constant.RtDataPurge.equals(name)
                        || Constant.EventPurge.equals(name) || Constant.BaselineFrequency.equals(name)
                        || Constant.BaselineDataSet.equals(name)) {
                        value = convertMillisToDays(value);
                    } else if (Constant.EnableAgentAutoUpdate.equals(name)) {
                        if (value.trim().length() == 0) {
                            value = "false"; // if, for some reason, this value was empty, use false - let the user explicitly enable it
                        }
                    } else if (Constant.EnableDebugMode.equals(name)) {
                        if (value.trim().length() == 0) {
                            value = "false";
                        }
                    } else if (Constant.EnableExperimentalFeatures.equals(name)) {
                        if (value.trim().length() == 0) {
                            value = "false";
                        }
                    } else if (Constant.DataReindex.equals(name)) {
                        if (value.trim().length() == 0) {
                            value = "true";
                        }
                    }

                    PropertySimple prop = new PropertySimple(name, value);
                    config.put(prop);
                }

                editor = new ConfigurationEditor(extendLocatorId("configEditor"), getSystemSettingsDefinition(config),
                    config);
                editor.addPropertyValueChangeListener(SystemSettingsView.this);
                canvas.addMember(editor);

                ToolStrip toolStrip = new ToolStrip();
                toolStrip.setWidth100();
                toolStrip.setMembersMargin(5);
                toolStrip.setLayoutMargin(5);

                saveButton = new LocatableIButton(extendLocatorId("Save"), MSG.common_button_save());
                saveButton.addClickHandler(new ClickHandler() {
                    public void onClick(ClickEvent clickEvent) {
                        save();
                    }
                });
                toolStrip.addMember(saveButton);

                canvas.addMember(toolStrip);
            }

            @Override
            public void onFailure(Throwable t) {
                CoreGUI.getErrorHandler().handleError("Cannot obtain the current system settings", t);
            }
        });
    }

    @Override
    public void propertyValueChanged(PropertyValueChangeEvent event) {
        if (event.isValidationStateChanged()) {
            Set<String> invalidPropertyNames = event.getInvalidPropertyNames();
            if (invalidPropertyNames.isEmpty()) {
                this.saveButton.enable();
            } else {
                this.saveButton.disable();
            }
        }
    }

    private void save() {
        if (editor.validate()) {
            Map<String, PropertySimple> simpleProperties = editor.getConfiguration().getSimpleProperties();
            HashMap<String, String> props = new HashMap<String, String>();
            for (PropertySimple simple : simpleProperties.values()) {
                String value = (simple.getStringValue() != null) ? simple.getStringValue() : "";

                // some of our properties actually need different values on the server than how they were
                // visualized in the UI.
                // -- JAASProvider is a boolean in the UI but must be "LDAP" if it was true and "JDBC" if it was false
                // -- LDAPProtocol is a boolean in the UI but must be "ssl" if true and "" if it was false
                // -- some other numerical values need to be converted to milliseconds
                if (Constant.JAASProvider.equals(simple.getName())) {
                    if (Boolean.parseBoolean(value)) {
                        value = Constant.LDAPJAASProvider;
                    } else {
                        value = Constant.JDBCJAASProvider;
                    }
                } else if (Constant.LDAPProtocol.equals(simple.getName())) {
                    if (Boolean.parseBoolean(value)) {
                        value = "ssl";
                    } else {
                        value = "";
                    }
                } else if (Constant.AgentMaxQuietTimeAllowed.equals(simple.getName())) {
                    value = convertMinutesToMillis(value);
                } else if (Constant.DataMaintenance.equals(simple.getName())) {
                    value = convertHoursToMillis(value);
                } else if (Constant.AvailabilityPurge.equals(simple.getName())
                    || Constant.AlertPurge.equals(simple.getName()) || Constant.TraitPurge.equals(simple.getName())
                    || Constant.RtDataPurge.equals(simple.getName()) || Constant.EventPurge.equals(simple.getName())
                    || Constant.BaselineFrequency.equals(simple.getName())
                    || Constant.BaselineDataSet.equals(simple.getName())) {
                    value = convertDaysToMillis(value);
                }

                props.put(simple.getName(), value);
            }

            GWTServiceLookup.getSystemService().setSystemConfiguration(props, false, new AsyncCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    CoreGUI.getMessageCenter().notify(
                        new Message("You successfully saved the system properties", Message.Severity.Info));
                }

                @Override
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError("Failed to save the system settings", caught);
                }
            });
        } else {
            CoreGUI.getMessageCenter().notify(
                new Message("Please fix the invalid values before saving", Severity.Warning, EnumSet
                    .of(Message.Option.Transient)));
        }
    }

    private String convertMinutesToMillis(String num) {
        return String.valueOf(Long.parseLong(num) * 60 * 1000);
    }

    private String convertHoursToMillis(String num) {
        return String.valueOf(Long.parseLong(num) * 60 * 60 * 1000);
    }

    private String convertDaysToMillis(String num) {
        return String.valueOf(Long.parseLong(num) * 24 * 60 * 60 * 1000);
    }

    private String convertMillisToMinutes(String num) {
        return String.valueOf(Long.parseLong(num) / (60 * 1000));
    }

    private String convertMillisToHours(String num) {
        return String.valueOf(Long.parseLong(num) / (60 * 60 * 1000));
    }

    private String convertMillisToDays(String num) {
        return String.valueOf(Long.parseLong(num) / (24 * 60 * 60 * 1000));
    }

    /**
     * This returns the metadata describing the system settings.
     *
     * @param config the current configuration settings from the server. Some values will
     *               be converted to the types the definition will expect - for example,
     *               the JAAS setting will not be "LDAP" or "JDBC" as the server would know it,
     *               rather the value will be "true" or "false" (i.e. is ldap enabled or not?)
     *
     * @return system settings config def
     */
    private ConfigurationDefinition getSystemSettingsDefinition(Configuration config) {
        ConfigurationDefinition def = new ConfigurationDefinition("sysset", "System settings");

        ///////////////////////////////////
        // General Configuration Properties

        PropertyGroupDefinition generalGroup = new PropertyGroupDefinition("general");
        generalGroup.setDisplayName("General Configuration Properties");
        generalGroup.setDefaultHidden(false);
        generalGroup.setOrder(0);

        PropertyDefinitionSimple baseUrl = new PropertyDefinitionSimple(Constant.BaseURL,
            "A URL to the server GUI, used mainly within alert email notification", true, PropertySimpleType.STRING);
        baseUrl.setDisplayName("GUI Console URL");
        baseUrl.setPropertyGroupDefinition(generalGroup);
        baseUrl.setDefaultValue("http://localhost:7080");
        def.put(baseUrl);

        PropertyDefinitionSimple agentMaxQuietTimeAllowed = new PropertyDefinitionSimple(
            Constant.AgentMaxQuietTimeAllowed,
            "If this amount of time passes without hearing from an agent, that quiet agent will be considered down. This value is specified in minutes.",
            true, PropertySimpleType.INTEGER);
        agentMaxQuietTimeAllowed.setDisplayName("Agent Max Quiet Time Allowed");
        agentMaxQuietTimeAllowed.setPropertyGroupDefinition(generalGroup);
        agentMaxQuietTimeAllowed.addConstraints(new IntegerRangeConstraint(Long.valueOf(2), null)); // don't allow less than 2m since it will cause too many false backfills 
        agentMaxQuietTimeAllowed.setDefaultValue("15");
        def.put(agentMaxQuietTimeAllowed);

        PropertyDefinitionSimple enableAgentAutoUpdate = new PropertyDefinitionSimple(
            Constant.EnableAgentAutoUpdate,
            "Determines if the server will allow agents to auto-update themselves. You will not be able to download agent distributions from the server if this is disabled.",
            true, PropertySimpleType.BOOLEAN);
        enableAgentAutoUpdate.setDisplayName("Enable Agent Auto-Updates");
        enableAgentAutoUpdate.setPropertyGroupDefinition(generalGroup);
        enableAgentAutoUpdate.setDefaultValue("true");
        def.put(enableAgentAutoUpdate);

        PropertyDefinitionSimple enableDebugMode = new PropertyDefinitionSimple(Constant.EnableDebugMode,
            "If enabled, the server will enter debug mode", true, PropertySimpleType.BOOLEAN);
        enableDebugMode.setDisplayName("Enable Debug Mode");
        enableDebugMode.setPropertyGroupDefinition(generalGroup);
        enableDebugMode.setDefaultValue("false");
        def.put(enableDebugMode);

        PropertyDefinitionSimple enableExperimentalFeatures = new PropertyDefinitionSimple(
            Constant.EnableExperimentalFeatures,
            "If enabled, any experimental features that exist in the current product will be available.", true,
            PropertySimpleType.BOOLEAN);
        enableExperimentalFeatures.setDisplayName("Enable Experimental Features");
        enableExperimentalFeatures.setPropertyGroupDefinition(generalGroup);
        enableExperimentalFeatures.setDefaultValue("false");
        def.put(enableExperimentalFeatures);

        ////////////////////////////////////////
        // Data Manager Configuration Properties

        PropertyGroupDefinition dataManagerGroup = new PropertyGroupDefinition("datamanager");
        dataManagerGroup.setDisplayName("Data Manager Configuration Properties");
        dataManagerGroup.setDefaultHidden(false);
        dataManagerGroup.setOrder(1);

        PropertyDefinitionSimple dataMaintenance = new PropertyDefinitionSimple(
            Constant.DataMaintenance,
            "How often database maintenance is performed (for example, vacuuming if using Postgres). This is specified in hours.",
            true, PropertySimpleType.INTEGER);
        dataMaintenance.setDisplayName("Database Maintenance Period");
        dataMaintenance.setPropertyGroupDefinition(dataManagerGroup);
        dataMaintenance.addConstraints(new IntegerRangeConstraint(Long.valueOf(1), null));
        dataMaintenance.setDefaultValue("1");
        def.put(dataMaintenance);

        PropertyDefinitionSimple availabilityPurge = new PropertyDefinitionSimple(Constant.AvailabilityPurge,
            "How old availability data must be before being purged from the database. This is specified in days.",
            true, PropertySimpleType.INTEGER);
        availabilityPurge.setDisplayName("Delete Availability Data Older Than");
        availabilityPurge.setPropertyGroupDefinition(dataManagerGroup);
        availabilityPurge.addConstraints(new IntegerRangeConstraint(Long.valueOf(1), null));
        availabilityPurge.setDefaultValue("365");
        def.put(availabilityPurge);

        PropertyDefinitionSimple alertPurge = new PropertyDefinitionSimple(Constant.AlertPurge,
            "How old alert history items must be before being purged from the database. This is specified in days.",
            true, PropertySimpleType.INTEGER);
        alertPurge.setDisplayName("Delete Alerts Older Than");
        alertPurge.setPropertyGroupDefinition(dataManagerGroup);
        alertPurge.addConstraints(new IntegerRangeConstraint(Long.valueOf(1), null));
        alertPurge.setDefaultValue("31");
        def.put(alertPurge);

        PropertyDefinitionSimple traitPurge = new PropertyDefinitionSimple(Constant.TraitPurge,
            "How old measurement trait data must be before being purged from the database. This is specified in days.",
            true, PropertySimpleType.INTEGER);
        traitPurge.setDisplayName("Delete Measurement Traits Older Than");
        traitPurge.setPropertyGroupDefinition(dataManagerGroup);
        traitPurge.addConstraints(new IntegerRangeConstraint(Long.valueOf(1), null));
        traitPurge.setDefaultValue("365");
        def.put(traitPurge);

        PropertyDefinitionSimple rtDataPurge = new PropertyDefinitionSimple(Constant.RtDataPurge,
            "How old response time data must be before being purged from the database. This is specified in days.",
            true, PropertySimpleType.INTEGER);
        rtDataPurge.setDisplayName("Delete Response Time Data Older Than");
        rtDataPurge.setPropertyGroupDefinition(dataManagerGroup);
        rtDataPurge.addConstraints(new IntegerRangeConstraint(Long.valueOf(1), null));
        rtDataPurge.setDefaultValue("31");
        def.put(rtDataPurge);

        PropertyDefinitionSimple eventPurge = new PropertyDefinitionSimple(Constant.EventPurge,
            "How old event data must be before being purged from the database. This is specified in days.", true,
            PropertySimpleType.INTEGER);
        eventPurge.setDisplayName("Delete Events Older Than");
        eventPurge.setPropertyGroupDefinition(dataManagerGroup);
        eventPurge.addConstraints(new IntegerRangeConstraint(Long.valueOf(1), null));
        eventPurge.setDefaultValue("14");
        def.put(eventPurge);

        PropertyDefinitionSimple dataReindex = new PropertyDefinitionSimple(Constant.DataReindex,
            "If enabled, certain database tables will be re-indexed periodically.", true, PropertySimpleType.BOOLEAN);
        dataReindex.setDisplayName("Reindex Data Tables Nightly");
        dataReindex.setPropertyGroupDefinition(dataManagerGroup);
        dataReindex.setDefaultValue("true");
        def.put(dataReindex);

        //////////////////////////////////////////////
        // Automatic Baseline Configuration Properties

        PropertyGroupDefinition baselineGroup = new PropertyGroupDefinition("baseline");
        baselineGroup.setDisplayName("Automatic Baseline Configuration Properties");
        baselineGroup.setDefaultHidden(false);
        baselineGroup.setOrder(2);

        PropertyDefinitionSimple baselineFrequency = new PropertyDefinitionSimple(
            Constant.BaselineFrequency,
            "The frequency which the auto-calculation of baselines will be performed. If 0, baseline auto-calculation is disabled. This is specified in days.",
            true, PropertySimpleType.INTEGER);
        baselineFrequency.setDisplayName("Baseline Calculation Frequency");
        baselineFrequency.setPropertyGroupDefinition(baselineGroup);
        baselineFrequency.addConstraints(new IntegerRangeConstraint(Long.valueOf(0), null));
        baselineFrequency.setDefaultValue("3");
        def.put(baselineFrequency);

        PropertyDefinitionSimple baselineDataSet = new PropertyDefinitionSimple(Constant.BaselineDataSet,
            "The amount of past measurement data that is used to determine a baseline. This is specified in days.",
            true, PropertySimpleType.INTEGER);
        baselineDataSet.setDisplayName("Baseline Dataset");
        baselineDataSet.setPropertyGroupDefinition(baselineGroup);
        baselineDataSet.addConstraints(new IntegerRangeConstraint(Long.valueOf(1), Long.valueOf(14))); // can't do more than 14 days since our raw tables don't hold more 
        baselineDataSet.setDefaultValue("7");
        def.put(baselineDataSet);

        ////////////////////////////////
        // LDAP Configuration Properties

        PropertyGroupDefinition ldapGroup = new PropertyGroupDefinition("ldap");
        ldapGroup.setDisplayName("LDAP Configuration Properties");
        ldapGroup.setDefaultHidden(Boolean.parseBoolean(config.getSimpleValue(Constant.JAASProvider, "false"))); // show if LDAP is in use
        ldapGroup.setOrder(3);

        PropertyDefinitionSimple jaasProvider = new PropertyDefinitionSimple(Constant.JAASProvider,
            "Should LDAP be used to determine user identity?", true, PropertySimpleType.BOOLEAN);
        jaasProvider.setDisplayName("Enable LDAP");
        jaasProvider.setPropertyGroupDefinition(ldapGroup);
        jaasProvider.setDefaultValue("false");
        def.put(jaasProvider);

        PropertyDefinitionSimple ldapUrl = new PropertyDefinitionSimple(Constant.LDAPUrl, "URL to the LDAP server",
            true, PropertySimpleType.STRING);
        ldapUrl.setDisplayName("LDAP URL");
        ldapUrl.setPropertyGroupDefinition(ldapGroup);
        ldapUrl.setDefaultValue("ldap://localhost");
        def.put(ldapUrl);

        PropertyDefinitionSimple ldapProtocol = new PropertyDefinitionSimple(Constant.LDAPProtocol,
            "Should communication with the LDAP server be done over SSL", true, PropertySimpleType.BOOLEAN);
        ldapProtocol.setDisplayName("SSL");
        ldapProtocol.setPropertyGroupDefinition(ldapGroup);
        ldapProtocol.setDefaultValue("false");
        def.put(ldapProtocol);

        PropertyDefinitionSimple ldapLoginProperty = new PropertyDefinitionSimple(
            Constant.LDAPLoginProperty,
            "The LDAP property that contains the user name. Defaults to cn. If multiple matches are found, the first entry found is used.",
            false, PropertySimpleType.STRING);
        ldapLoginProperty.setDisplayName("Login Property");
        ldapLoginProperty.setPropertyGroupDefinition(ldapGroup);
        ldapLoginProperty.setDefaultValue("cn");
        def.put(ldapLoginProperty);

        PropertyDefinitionSimple ldapFilter = new PropertyDefinitionSimple(
            Constant.LDAPFilter,
            "Any additional filters to apply when doing the LDAP search. This is useful if the population to authenticate can be identified via a given LDAP property, e.g. RHQUser=true",
            false, PropertySimpleType.STRING);
        ldapFilter.setDisplayName("Search Filter");
        ldapFilter.setPropertyGroupDefinition(ldapGroup);
        ldapFilter.setDefaultValue("");
        def.put(ldapFilter);

        PropertyDefinitionSimple ldapGroupFilter = new PropertyDefinitionSimple(
            Constant.LDAPGroupFilter,
            "LDAP search filter that must return all LDAP groups available for authorization. This is used for LDAP group authorization.",
            false, PropertySimpleType.STRING);
        ldapGroupFilter.setDisplayName("Group Search Filter");
        ldapGroupFilter.setPropertyGroupDefinition(ldapGroup);
        ldapGroupFilter.setDefaultValue("rhqadmin");
        def.put(ldapGroupFilter);

        PropertyDefinitionSimple ldapGroupMember = new PropertyDefinitionSimple(
            Constant.LDAPGroupMember,
            "LDAP search filter that is used in conjunction with the group search filter to determine user authorization. This is used for LDAP group authorization.",
            false, PropertySimpleType.STRING);
        ldapGroupMember.setDisplayName("Group Member Filter");
        ldapGroupMember.setPropertyGroupDefinition(ldapGroup);
        ldapGroupMember.setDefaultValue("");
        def.put(ldapGroupMember);

        PropertyDefinitionSimple ldapBaseDN = new PropertyDefinitionSimple(
            Constant.LDAPBaseDN,
            "The base of the directory tree to search for usernames and passwords while authenticating users, e.g. ou=People,dc=jboss,dc=com",
            false, PropertySimpleType.STRING);
        ldapBaseDN.setDisplayName("Search Base");
        ldapBaseDN.setPropertyGroupDefinition(ldapGroup);
        ldapBaseDN.setDefaultValue("o=RedHat,c=US");
        def.put(ldapBaseDN);

        PropertyDefinitionSimple ldapBindDN = new PropertyDefinitionSimple(
            Constant.LDAPBindDN,
            "The username to connect to the LDAP server when querying the LDAP server's user database. This is typically the full LDAP distinguished name (DN) of a manager user, e.g. cn=Manager,dc=jboss,dc=com.",
            false, PropertySimpleType.STRING);
        ldapBindDN.setDisplayName("Username");
        ldapBindDN.setPropertyGroupDefinition(ldapGroup);
        ldapBindDN.setDefaultValue("");
        def.put(ldapBindDN);

        PropertyDefinitionSimple ldapBindPW = new PropertyDefinitionSimple(
            Constant.LDAPBindPW,
            "The credentials of the user used to connect to the LDAP server when querying the LDAP server's user database.",
            false, PropertySimpleType.PASSWORD);
        ldapBindPW.setDisplayName("Password");
        ldapBindPW.setPropertyGroupDefinition(ldapGroup);
        ldapBindPW.setDefaultValue("");
        def.put(ldapBindPW);

        return def;

    }

    private DynamicForm getServerDetails() {
        DynamicForm form = new LocatableDynamicForm(extendLocatorId("serverDetails"));
        form.setWidth100();
        form.setExtraSpace(15);
        form.setIsGroup(true);
        form.setGroupTitle("Server Details");

        final StaticTextItem productName = new StaticTextItem("productname", "Name");
        final StaticTextItem productVersion = new StaticTextItem("productversion", "Version");
        final StaticTextItem productBuildNumber = new StaticTextItem("productbuild", "Build Number");

        final StaticTextItem serverTimezone = new StaticTextItem("timezone", "Server Time Zone");
        final StaticTextItem serverTime = new StaticTextItem("localtime", "Server Local Time");
        final StaticTextItem dbUrl = new StaticTextItem("dbUrl", "Database Connection URL");
        final StaticTextItem dbProductName = new StaticTextItem("dbProductName", "Database Product Name");
        final StaticTextItem dbProductVersion = new StaticTextItem("dbProductVersion", "Database Product Version");
        final StaticTextItem dbDriverName = new StaticTextItem("dbDriverName", "Database Driver Name");
        final StaticTextItem dbDriverVersion = new StaticTextItem("dbDriverVersion", "Database Driver Version");
        final StaticTextItem currentMeasRawTable = new StaticTextItem("currentMeasRawTable",
            "Current Measurement Raw Table");
        final StaticTextItem nextMeasTableRotation = new StaticTextItem("nextMeasTableRotation",
            "Next Measurement Table Rotation");

        productName.setWrapTitle(false);
        productVersion.setWrapTitle(false);
        productBuildNumber.setWrapTitle(false);
        serverTimezone.setWrapTitle(false);
        serverTime.setWrapTitle(false);
        dbUrl.setWrapTitle(false);
        dbProductName.setWrapTitle(false);
        dbProductVersion.setWrapTitle(false);
        dbDriverName.setWrapTitle(false);
        dbDriverVersion.setWrapTitle(false);
        currentMeasRawTable.setWrapTitle(false);
        nextMeasTableRotation.setWrapTitle(false);

        form.setItems(productName, productVersion, productBuildNumber, serverTimezone, serverTime, dbUrl,
            dbProductName, dbProductVersion, dbDriverName, dbDriverVersion, currentMeasRawTable, nextMeasTableRotation);

        GWTServiceLookup.getSystemService().getServerDetails(new AsyncCallback<ServerDetails>() {

            @Override
            public void onSuccess(ServerDetails result) {
                ProductInfo productInfo = result.getProductInfo();
                productName.setValue(productInfo.getName());
                productVersion.setValue(productInfo.getVersion());
                productBuildNumber.setValue(productInfo.getBuildNumber());

                HashMap<Detail, String> details = result.getDetails();
                serverTimezone.setValue(details.get(ServerDetails.Detail.SERVER_TIMEZONE));
                serverTime.setValue(details.get(ServerDetails.Detail.SERVER_LOCAL_TIME));
                dbUrl.setValue(details.get(ServerDetails.Detail.DATABASE_CONNECTION_URL));
                dbProductName.setValue(details.get(ServerDetails.Detail.DATABASE_PRODUCT_NAME));
                dbProductVersion.setValue(details.get(ServerDetails.Detail.DATABASE_PRODUCT_VERSION));
                dbDriverName.setValue(details.get(ServerDetails.Detail.DATABASE_DRIVER_NAME));
                dbDriverVersion.setValue(details.get(ServerDetails.Detail.DATABASE_DRIVER_VERSION));
                currentMeasRawTable.setValue(details.get(ServerDetails.Detail.CURRENT_MEASUREMENT_TABLE));
                nextMeasTableRotation.setValue(details.get(ServerDetails.Detail.NEXT_MEASUREMENT_TABLE_ROTATION));
            }

            @Override
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError("Cannot get server details", caught);
            }
        });

        return form;
    }
}
