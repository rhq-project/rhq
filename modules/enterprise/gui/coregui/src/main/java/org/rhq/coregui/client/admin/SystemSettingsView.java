/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.coregui.client.admin;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.rhq.core.domain.common.composite.SystemSetting;
import org.rhq.core.domain.common.composite.SystemSettings;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionEnumeration;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertyGroupDefinition;
import org.rhq.core.domain.configuration.definition.constraint.IntegerRangeConstraint;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.IconEnum;
import org.rhq.coregui.client.components.configuration.ConfigurationEditor;
import org.rhq.coregui.client.components.configuration.PropertyValueChangeEvent;
import org.rhq.coregui.client.components.configuration.PropertyValueChangeListener;
import org.rhq.coregui.client.components.view.ViewName;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.util.enhanced.EnhancedIButton;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;
import org.rhq.coregui.client.util.enhanced.EnhancedIButton.ButtonColor;
import org.rhq.coregui.client.util.message.Message;
import org.rhq.coregui.client.util.message.Message.Severity;

/**
 * A simple form to view and edit the server system settings.
 *
 * @author John Mazzitelli
 */
public class SystemSettingsView extends EnhancedVLayout implements PropertyValueChangeListener {

    public static final ViewName VIEW_ID = new ViewName("SystemSettings", MSG.view_adminConfig_systemSettings(),
        IconEnum.CONFIGURE);
    public static final String VIEW_PATH = AdministrationView.VIEW_ID + "/"
        + AdministrationView.SECTION_CONFIGURATION_VIEW_ID + "/" + VIEW_ID;

    private EnhancedVLayout canvas;
    private ConfigurationEditor editor;
    private IButton saveButton;
    private IButton dumpToLogButton;

    public SystemSettingsView() {
        super();
        setHeight100();
        setWidth100();

        canvas = new EnhancedVLayout();
        canvas.setHeight100();
        canvas.setWidth100();
        canvas.setMargin(15);
        addMember(canvas);
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        GWTServiceLookup.getSystemService().getSystemSettings(new AsyncCallback<SystemSettings>() {
            @Override
            public void onSuccess(SystemSettings result) {

                canvas.addMember(getServerDetails());

                Configuration config = result.toConfiguration();

                //convert stuff for the display purposes
                PropertySimple prop = config.getSimple(SystemSetting.AGENT_MAX_QUIET_TIME_ALLOWED.getInternalName());
                prop.setStringValue(convertMillisToMinutes(prop.getStringValue()));

                prop = config.getSimple(SystemSetting.RHQ_SESSION_TIMEOUT.getInternalName());
                prop.setStringValue(convertMillisToHours(prop.getStringValue()));

                prop = config.getSimple(SystemSetting.DATA_MAINTENANCE_PERIOD.getInternalName());
                prop.setStringValue(convertMillisToHours(prop.getStringValue()));

                prop = config.getSimple(SystemSetting.AVAILABILITY_PURGE_PERIOD.getInternalName());
                prop.setStringValue(convertMillisToDays(prop.getStringValue()));

                prop = config.getSimple(SystemSetting.ALERT_PURGE_PERIOD.getInternalName());
                prop.setStringValue(convertMillisToDays(prop.getStringValue()));

                prop = config.getSimple(SystemSetting.TRAIT_PURGE_PERIOD.getInternalName());
                prop.setStringValue(convertMillisToDays(prop.getStringValue()));

                prop = config.getSimple(SystemSetting.RT_DATA_PURGE_PERIOD.getInternalName());
                prop.setStringValue(convertMillisToDays(prop.getStringValue()));

                prop = config.getSimple(SystemSetting.EVENT_PURGE_PERIOD.getInternalName());
                prop.setStringValue(convertMillisToDays(prop.getStringValue()));

                prop = config.getSimple(SystemSetting.DRIFT_FILE_PURGE_PERIOD.getInternalName());
                prop.setStringValue(convertMillisToDays(prop.getStringValue()));

                prop = config.getSimple(SystemSetting.BASE_LINE_FREQUENCY.getInternalName());
                prop.setStringValue(convertMillisToDays(prop.getStringValue()));

                prop = config.getSimple(SystemSetting.BASE_LINE_DATASET.getInternalName());
                prop.setStringValue(convertMillisToDays(prop.getStringValue()));

                // build our config definition and populate our config editor
                editor = new ConfigurationEditor(getSystemSettingsDefinition(config, result.getDriftPlugins()), config);
                editor.addPropertyValueChangeListener(SystemSettingsView.this);
                canvas.addMember(editor);

                ToolStrip toolStrip = new ToolStrip();
                toolStrip.setWidth100();
                toolStrip.setMembersMargin(5);
                toolStrip.setLayoutMargin(5);

                saveButton = new EnhancedIButton(MSG.common_button_save(), ButtonColor.BLUE);
                saveButton.addClickHandler(new ClickHandler() {
                    public void onClick(ClickEvent clickEvent) {
                        save();
                    }
                });
                toolStrip.addMember(saveButton);

                dumpToLogButton = new EnhancedIButton(MSG.common_button_dump_sysInfo_to_log());
                dumpToLogButton.addClickHandler(new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent clickEvent) {
                        dumpToLog();
                    }
                });
                toolStrip.addMember(dumpToLogButton);

                canvas.addMember(toolStrip);

            }

            @Override
            public void onFailure(Throwable t) {
                CoreGUI.getErrorHandler().handleError(MSG.view_admin_systemSettings_cannotLoadSettings(), t);
            }
        });
    }

    @Override
    public void propertyValueChanged(PropertyValueChangeEvent event) {
        if (event.isInvalidPropertySetChanged()) {
            Map<String, String> invalidPropertyNames = event.getInvalidPropertyNames();
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
                // -- some other numerical values need to be converted to milliseconds
                if (SystemSetting.AGENT_MAX_QUIET_TIME_ALLOWED.getInternalName().equals(simple.getName())) {
                    value = convertMinutesToMillis(value);
                } else if (SystemSetting.DATA_MAINTENANCE_PERIOD.getInternalName().equals(simple.getName())
                    || SystemSetting.RHQ_SESSION_TIMEOUT.getInternalName().equals(simple.getName())) {
                    value = convertHoursToMillis(value);
                } else if (SystemSetting.AVAILABILITY_PURGE_PERIOD.getInternalName().equals(simple.getName())
                    || SystemSetting.ALERT_PURGE_PERIOD.getInternalName().equals(simple.getName())
                    || SystemSetting.TRAIT_PURGE_PERIOD.getInternalName().equals(simple.getName())
                    || SystemSetting.RT_DATA_PURGE_PERIOD.getInternalName().equals(simple.getName())
                    || SystemSetting.EVENT_PURGE_PERIOD.getInternalName().equals(simple.getName())
                    || SystemSetting.DRIFT_FILE_PURGE_PERIOD.getInternalName().equals(simple.getName())
                    || SystemSetting.BASE_LINE_FREQUENCY.getInternalName().equals(simple.getName())
                    || SystemSetting.BASE_LINE_DATASET.getInternalName().equals(simple.getName())) {
                    value = convertDaysToMillis(value);
                }

                props.put(simple.getName(), value);
            }

            SystemSettings settings = SystemSettings.fromMap(props);

            GWTServiceLookup.getSystemService().setSystemSettings(settings, new AsyncCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    CoreGUI.getMessageCenter().notify(
                        new Message(MSG.view_admin_systemSettings_savedSettings(), Message.Severity.Info));
                }

                @Override
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_admin_systemSettings_saveFailure(), caught);
                }
            });
        } else {
            CoreGUI.getMessageCenter().notify(
                new Message(MSG.view_admin_systemSettings_fixBeforeSaving(), Severity.Warning, EnumSet
                    .of(Message.Option.Transient)));
        }
    }

    private void dumpToLog() {
        GWTServiceLookup.getSystemService().dumpToLog(new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable throwable) {
                CoreGUI.getMessageCenter().notify(
                    new Message(MSG.view_admin_systemSettings_dumpToLogFailed(), Severity.Warning));
            }

            @Override
            public void onSuccess(Void aVoid) {
                CoreGUI.getMessageCenter().notify(
                    new Message(MSG.view_admin_systemSettings_dumpedToLog(), Severity.Info));
            }
        });
    }

    private String convertMinutesToMillis(String num) {
        return num == null ? "0" : String.valueOf(Long.parseLong(num) * 60 * 1000);
    }

    private String convertHoursToMillis(String num) {
        return num == null ? "0" : String.valueOf(Long.parseLong(num) * 60 * 60 * 1000);
    }

    private String convertDaysToMillis(String num) {
        return num == null ? "0" : String.valueOf(Long.parseLong(num) * 24 * 60 * 60 * 1000);
    }

    private String convertMillisToMinutes(String num) {
        return num == null ? "0" : String.valueOf(Long.parseLong(num) / (60 * 1000));
    }

    private String convertMillisToHours(String num) {
        return num == null ? "0" : String.valueOf(Long.parseLong(num) / (60 * 60 * 1000));
    }

    private String convertMillisToDays(String num) {
        return num == null ? "0" : String.valueOf(Long.parseLong(num) / (24 * 60 * 60 * 1000));
    }

    /**
     * This returns the metadata describing the system settings.
     *
     * @param config the current configuration settings from the server. Some values will
     *               be converted to the types the definition will expect - for example,
     *               the JAAS setting will not be "LDAP" or "JDBC" as the server would know it,
     *               rather the value will be "true" or "false" (i.e. is ldap enabled or not?)
     * @param driftPlugins the set of drift server plugins that are currently deployed
     * @return system settings config def
     */
    private ConfigurationDefinition getSystemSettingsDefinition(Configuration config, Map<String, String> driftPlugins) {
        ConfigurationDefinition def = new ConfigurationDefinition("sysset", MSG.view_adminConfig_systemSettings());

        PropertyGroupDefinition generalGroup = new PropertyGroupDefinition("general");
        generalGroup.setDisplayName(MSG.view_admin_systemSettings_group_general());
        generalGroup.setDefaultHidden(false);
        generalGroup.setOrder(0);

        PropertyGroupDefinition dataManagerGroup = new PropertyGroupDefinition("datamanager");
        dataManagerGroup.setDisplayName(MSG.view_admin_systemSettings_group_dataMgr());
        dataManagerGroup.setDefaultHidden(false);
        dataManagerGroup.setOrder(1);

        PropertyGroupDefinition baselineGroup = new PropertyGroupDefinition("baseline");
        baselineGroup.setDisplayName(MSG.view_admin_systemSettings_group_baseline());
        baselineGroup.setDefaultHidden(false);
        baselineGroup.setOrder(2);

        PropertyGroupDefinition ldapGroup = new PropertyGroupDefinition("ldap");
        ldapGroup.setDisplayName(MSG.view_admin_systemSettings_group_ldap());
        ldapGroup.setDefaultHidden(!Boolean.parseBoolean(config.getSimpleValue(
            SystemSetting.LDAP_BASED_JAAS_PROVIDER.getInternalName(), "false"))); // show if LDAP is in use
        ldapGroup.setOrder(3);

        PropertyGroupDefinition driftGroup = new PropertyGroupDefinition("drift");
        driftGroup.setDisplayName(MSG.view_admin_systemSettings_group_drift());
        driftGroup.setOrder(4);
        driftGroup.setDefaultHidden(false);

        PropertyGroupDefinition proxyGroup = new PropertyGroupDefinition("proxy");
        proxyGroup.setDisplayName(MSG.view_admin_systemSettings_group_HttpProxy());
        proxyGroup.setOrder(5);
        proxyGroup.setDefaultHidden(false);

        for (SystemSetting prop : SystemSetting.values()) {

            //don't include the readonly properties in the configuration editor
            if (prop.isReadOnly()) {
                continue;
            }

            PropertyDefinitionSimple pd = prop.createPropertyDefinition();

            def.put(pd);

            switch (prop) {

            ///////////////////////////////////
            // General Configuration Properties

            case BASE_URL:
                pd.setDescription(MSG.view_admin_systemSettings_BaseURL_desc());
                pd.setDisplayName(MSG.view_admin_systemSettings_BaseURL_name());
                pd.setPropertyGroupDefinition(generalGroup);
                pd.setDefaultValue("http://localhost:7080");
                break;
            case AGENT_MAX_QUIET_TIME_ALLOWED:
                pd.setDescription(MSG.view_admin_systemSettings_AgentMaxQuietTimeAllowed_desc());
                pd.setDisplayName(MSG.view_admin_systemSettings_AgentMaxQuietTimeAllowed_name());
                pd.setPropertyGroupDefinition(generalGroup);
                // don't allow less than 3m which is 3 * the ping interval, anything less risks unwanted backfill
                pd.addConstraints(new IntegerRangeConstraint(Long.valueOf(3), null));
                pd.setDefaultValue("5");
                break;
            case RHQ_SESSION_TIMEOUT:
                pd.setDescription(MSG.view_admin_systemSettings_RHQSessionTimeout_desc());
                pd.setDisplayName(MSG.view_admin_systemSettings_RHQSessionTimeout_name());
                pd.setPropertyGroupDefinition(generalGroup);
                pd.addConstraints(new IntegerRangeConstraint(Long.valueOf(1), null));
                pd.setDefaultValue("1");
                break;
            case AGENT_AUTO_UPDATE_ENABLED:
                pd.setDescription(MSG.view_admin_systemSettings_EnableAgentAutoUpdate_desc());
                pd.setDisplayName(MSG.view_admin_systemSettings_EnableAgentAutoUpdate_name());
                pd.setPropertyGroupDefinition(generalGroup);
                pd.setDefaultValue("true");
                break;
            case DEBUG_MODE_ENABLED:
                pd.setDescription(MSG.view_admin_systemSettings_EnableDebugMode_desc());
                pd.setDisplayName(MSG.view_admin_systemSettings_EnableDebugMode_name());
                pd.setPropertyGroupDefinition(generalGroup);
                pd.setDefaultValue("false");
                break;
            case LOGIN_WITHOUT_ROLES_ENABLED:
                pd.setDescription(MSG.view_admin_systemSettings_EnableLoginWithoutRoles_desc());
                pd.setDisplayName(MSG.view_admin_systemSettings_EnableLoginWithoutRoles_name());
                pd.setPropertyGroupDefinition(generalGroup);
                pd.setDefaultValue("false");
                break;
            case EXPERIMENTAL_FEATURES_ENABLED:
                pd.setDescription(MSG.view_admin_systemSettings_EnableExperimentalFeatures_desc());
                pd.setDisplayName(MSG.view_admin_systemSettings_EnableExperimentalFeatures_name());
                pd.setPropertyGroupDefinition(generalGroup);
                pd.setDefaultValue("false");
                break;
            case REMOTE_SSH_USERNAME_DEFAULT:
                pd.setDescription(MSG.view_admin_systemSettings_RemoteSshUsernameDefault_desc());
                pd.setDisplayName(MSG.view_admin_systemSettings_RemoteSshUsernameDefault_name());
                pd.setPropertyGroupDefinition(generalGroup);
                pd.setDefaultValue(null);
                break;
            case REMOTE_SSH_PASSWORD_DEFAULT:
                pd.setDescription(MSG.view_admin_systemSettings_RemoteSshPasswordDefault_desc());
                pd.setDisplayName(MSG.view_admin_systemSettings_RemoteSshPasswordDefault_name());
                pd.setPropertyGroupDefinition(generalGroup);
                pd.setDefaultValue(null);
                break;

            ////////////////////////////////////////
            // Data Manager Configuration Properties

            case DATA_MAINTENANCE_PERIOD:
                pd.setDescription(MSG.view_admin_systemSettings_DataMaintenance_desc());
                pd.setDisplayName(MSG.view_admin_systemSettings_DataMaintenance_name());
                pd.setPropertyGroupDefinition(dataManagerGroup);
                pd.addConstraints(new IntegerRangeConstraint(Long.valueOf(1), null));
                pd.setDefaultValue("1");
                break;

            case AVAILABILITY_PURGE_PERIOD:
                pd.setDescription(MSG.view_admin_systemSettings_AvailabilityPurge_desc());
                pd.setDisplayName(MSG.view_admin_systemSettings_AvailabilityPurge_name());
                pd.setPropertyGroupDefinition(dataManagerGroup);
                pd.addConstraints(new IntegerRangeConstraint(Long.valueOf(1), null));
                pd.setDefaultValue("365");
                break;

            case ALERT_PURGE_PERIOD:
                pd.setDescription(MSG.view_admin_systemSettings_AlertPurge_desc());
                pd.setDisplayName(MSG.view_admin_systemSettings_AlertPurge_name());
                pd.setPropertyGroupDefinition(dataManagerGroup);
                pd.addConstraints(new IntegerRangeConstraint(Long.valueOf(1), null));
                pd.setDefaultValue("31");
                break;

            case TRAIT_PURGE_PERIOD:
                pd.setDescription(MSG.view_admin_systemSettings_TraitPurge_desc());
                pd.setDisplayName(MSG.view_admin_systemSettings_TraitPurge_name());
                pd.setPropertyGroupDefinition(dataManagerGroup);
                pd.addConstraints(new IntegerRangeConstraint(Long.valueOf(1), null));
                pd.setDefaultValue("365");
                break;

            case RT_DATA_PURGE_PERIOD:
                pd.setDescription(MSG.view_admin_systemSettings_RtDataPurge_desc());
                pd.setDisplayName(MSG.view_admin_systemSettings_RtDataPurge_name());
                pd.setPropertyGroupDefinition(dataManagerGroup);
                pd.addConstraints(new IntegerRangeConstraint(Long.valueOf(1), null));
                pd.setDefaultValue("31");
                break;

            case EVENT_PURGE_PERIOD:
                pd.setDescription(MSG.view_admin_systemSettings_EventPurge_desc());
                pd.setDisplayName(MSG.view_admin_systemSettings_EventPurge_name());
                pd.setPropertyGroupDefinition(dataManagerGroup);
                pd.addConstraints(new IntegerRangeConstraint(Long.valueOf(1), null));
                pd.setDefaultValue("14");
                break;

            case DRIFT_FILE_PURGE_PERIOD:
                pd.setDescription(MSG.view_admin_systemSettings_DriftFilePurge_desc());
                pd.setDisplayName(MSG.view_admin_systemSettings_DriftFilePurge_name());
                pd.setPropertyGroupDefinition(dataManagerGroup);
                pd.addConstraints(new IntegerRangeConstraint(Long.valueOf(1), null));
                pd.setDefaultValue("31");
                break;

            case DATA_REINDEX_NIGHTLY:
                pd.setDescription(MSG.view_admin_systemSettings_DataReindex_desc());
                pd.setDisplayName(MSG.view_admin_systemSettings_DataReindex_name());
                pd.setPropertyGroupDefinition(dataManagerGroup);
                pd.setDefaultValue("false");
                break;

            //////////////////////////////////////////////
            // Automatic Baseline Configuration Properties

            case BASE_LINE_FREQUENCY:
                pd.setDescription(MSG.view_admin_systemSettings_BaselineFrequency_desc());
                pd.setDisplayName(MSG.view_admin_systemSettings_BaselineFrequency_name());
                pd.setPropertyGroupDefinition(baselineGroup);
                pd.addConstraints(new IntegerRangeConstraint(Long.valueOf(0), null));
                pd.setDefaultValue("3");
                break;

            case BASE_LINE_DATASET:
                pd.setDescription(MSG.view_admin_systemSettings_BaselineDataSet_desc());
                pd.setDisplayName(MSG.view_admin_systemSettings_BaselineDataSet_name());
                pd.setPropertyGroupDefinition(baselineGroup);
                pd.addConstraints(new IntegerRangeConstraint(Long.valueOf(1), Long.valueOf(14))); // can't do more than 14 days since our 1h table doesn't hold more
                pd.setDefaultValue("7");
                break;

            ////////////////////////////////
            // LDAP Configuration Properties

            case LDAP_BASED_JAAS_PROVIDER:
                pd.setDescription(MSG.view_admin_systemSettings_JAASProvider_desc());
                pd.setDisplayName(MSG.view_admin_systemSettings_JAASProvider_name());
                pd.setPropertyGroupDefinition(ldapGroup);
                pd.setDefaultValue("false");
                break;

            case LDAP_NAMING_PROVIDER_URL:
                pd.setDescription(MSG.view_admin_systemSettings_LDAPUrl_desc());
                pd.setDisplayName(MSG.view_admin_systemSettings_LDAPUrl_name());
                pd.setPropertyGroupDefinition(ldapGroup);
                pd.setDefaultValue("ldap://localhost");
                break;

            case USE_SSL_FOR_LDAP:
                pd.setDescription(MSG.view_admin_systemSettings_LDAPProtocol_desc());
                pd.setDisplayName(MSG.view_admin_systemSettings_LDAPProtocol_name());
                pd.setPropertyGroupDefinition(ldapGroup);
                pd.setDefaultValue("false");
                break;

            case LDAP_LOGIN_PROPERTY:
                pd.setDescription(MSG.view_admin_systemSettings_LDAPLoginProperty_desc());
                pd.setDisplayName(MSG.view_admin_systemSettings_LDAPLoginProperty_name());
                pd.setPropertyGroupDefinition(ldapGroup);
                pd.setDefaultValue("cn");
                break;

            case LDAP_FILTER:
                pd.setDescription(MSG.view_admin_systemSettings_LDAPFilter_desc());
                pd.setDisplayName(MSG.view_admin_systemSettings_LDAPFilter_name());
                pd.setPropertyGroupDefinition(ldapGroup);
                pd.setDefaultValue("");
                break;

            case LDAP_GROUP_FILTER:
                pd.setDescription(MSG.view_admin_systemSettings_LDAPGroupFilter_desc());
                pd.setDisplayName(MSG.view_admin_systemSettings_LDAPGroupFilter_name());
                pd.setPropertyGroupDefinition(ldapGroup);
                pd.setDefaultValue("objectclass=groupOfNames");
                break;

            case LDAP_GROUP_PAGING:
                pd.setDescription(MSG.view_admin_systemSettings_LDAPGroupUsePaging_desc());
                pd.setDisplayName(MSG.view_admin_systemSettings_LDAPGroupUsePaging_name());
                pd.setPropertyGroupDefinition(ldapGroup);
                pd.setDefaultValue("false");
                break;

            case LDAP_GROUP_QUERY_PAGE_SIZE:
                pd.setDescription(MSG.view_admin_systemSettings_LDAPGroupPageSize_desc());
                pd.setDisplayName(MSG.view_admin_systemSettings_LDAPGroupPageSize_name());
                pd.setPropertyGroupDefinition(ldapGroup);
                pd.addConstraints(new IntegerRangeConstraint(Long.valueOf(1), null));
                pd.setDefaultValue("1000");
                break;

            case LDAP_GROUP_MEMBER:
                pd.setDescription(MSG.view_admin_systemSettings_LDAPGroupMember_desc());
                pd.setDisplayName(MSG.view_admin_systemSettings_LDAPGroupMember_name());
                pd.setPropertyGroupDefinition(ldapGroup);
                pd.setDefaultValue("member");
                break;

            case LDAP_GROUP_USE_POSIX:
                pd.setDescription(MSG.view_admin_systemSettings_LDAPGroupUsePosixGroup_desc());
                pd.setDisplayName(MSG.view_admin_systemSettings_LDAPGroupUsePosixGroup_name());
                pd.setPropertyGroupDefinition(ldapGroup);
                pd.setDefaultValue("false");
                break;

            case LDAP_BASE_DN:
                pd.setDescription(MSG.view_admin_systemSettings_LDAPBaseDN_desc());
                pd.setDisplayName(MSG.view_admin_systemSettings_LDAPBaseDN_name());
                pd.setPropertyGroupDefinition(ldapGroup);
                pd.setDefaultValue("o=RedHat,c=US");
                break;

            case LDAP_BIND_DN:
                pd.setDescription(MSG.view_admin_systemSettings_LDAPBindDN_desc());
                pd.setDisplayName(MSG.view_admin_systemSettings_LDAPBindDN_name());
                pd.setPropertyGroupDefinition(ldapGroup);
                pd.setDefaultValue("");
                break;

            case LDAP_BIND_PW:
                pd.setDescription(MSG.view_admin_systemSettings_LDAPBindPW_desc());
                pd.setDisplayName(MSG.view_admin_systemSettings_LDAPBindPW_name());
                pd.setPropertyGroupDefinition(ldapGroup);
                pd.setDefaultValue("");
                break;

            case LDAP_FOLLOW_REFERRALS:
                pd.setDescription(MSG.view_admin_systemSettings_LDAPFollowReferrals_desc());
                pd.setDisplayName(MSG.view_admin_systemSettings_LDAPFollowReferrals_name());
                pd.setPropertyGroupDefinition(ldapGroup);
                pd.setDefaultValue("false");
                break;

            ///////////////////////////////////////////
            // Drift Server Configuration Properties //
            ///////////////////////////////////////////

            case ACTIVE_DRIFT_PLUGIN:
                pd.setDescription(MSG.view_admin_systemSettings_ActiveDriftServerPlugin_desc());
                pd.setDisplayName(MSG.view_admin_systemSettings_ActiveDriftServerPlugin_name());
                pd.setPropertyGroupDefinition(driftGroup);

                List<PropertyDefinitionEnumeration> options = new ArrayList<PropertyDefinitionEnumeration>();
                for (Map.Entry<String, String> entry : driftPlugins.entrySet()) {
                    options.add(new PropertyDefinitionEnumeration(entry.getValue(), entry.getKey()));
                }

                pd.setEnumeratedValues(options, false);
                break;
            /*
             * Proxy Server Settings
             */
            case HTTP_PROXY_SERVER_HOST:
                pd.setDescription(MSG.view_admin_systemSettings_HttpProxyHost_desc());
                pd.setDisplayName(MSG.view_admin_systemSettings_HttpProxyHost_name());
                pd.setPropertyGroupDefinition(proxyGroup);
                pd.setDefaultValue(null);
                break;

            case HTTP_PROXY_SERVER_PORT:
                pd.setDescription(MSG.view_admin_systemSettings_HttpProxyPort_desc());
                pd.setDisplayName(MSG.view_admin_systemSettings_HttpProxyPort_name());
                pd.setPropertyGroupDefinition(proxyGroup);
                pd.addConstraints(new IntegerRangeConstraint(Long.valueOf(0), Long.valueOf(65535)));
                pd.setDefaultValue("0");
                break;

            case HTTP_PROXY_SERVER_USERNAME:
                pd.setDescription(MSG.view_admin_systemSettings_HttpProxyUsername_desc());
                pd.setDisplayName(MSG.view_admin_systemSettings_HttpProxyUsername_name());
                pd.setPropertyGroupDefinition(proxyGroup);
                pd.setDefaultValue(null);
                break;

            case HTTP_PROXY_SERVER_PASSWORD:
                pd.setDescription(MSG.view_admin_systemSettings_HttpProxyPassword_desc());
                pd.setDisplayName(MSG.view_admin_systemSettings_HttpProxyPassword_name());
                pd.setPropertyGroupDefinition(proxyGroup);
                pd.setDefaultValue(null);
                break;

            }
            

        }

        //
        // if the config is missing any properties for which we have defaults, set them to their defaults
        //
        Map<String, PropertyDefinition> allDefinitions = def.getPropertyDefinitions();
        for (Map.Entry<String, PropertyDefinition> defEntry : allDefinitions.entrySet()) {
            String propertyName = defEntry.getKey();
            PropertyDefinition propertyDef = defEntry.getValue();
            if (config.get(propertyName) == null) {
                if (propertyDef instanceof PropertyDefinitionSimple
                    && ((PropertyDefinitionSimple) propertyDef).getDefaultValue() != null) {
                    config.put(new PropertySimple(propertyName, ((PropertyDefinitionSimple) propertyDef)
                        .getDefaultValue()));
                }
            }
        }

        return def;
    }

    private DynamicForm getServerDetails() {
        final DynamicForm form = new DynamicForm();
        form.setWidth100();
        form.setExtraSpace(15);
        form.setIsGroup(true);
        form.setGroupTitle(MSG.view_admin_systemSettings_serverDetails());

        final StaticTextItem productName = new StaticTextItem("productname",
            MSG.view_admin_systemSettings_serverDetails_productName());
        final StaticTextItem productVersion = new StaticTextItem("productversion", MSG.common_title_version());
        final StaticTextItem productBuildNumber = new StaticTextItem("productbuild",
            MSG.view_admin_systemSettings_serverDetails_buildNumber());

        final StaticTextItem serverName = new StaticTextItem("servername",
            MSG.view_admin_systemSettings_serverDetails_serverName());
        final StaticTextItem serverTimezone = new StaticTextItem("timezone",
            MSG.view_admin_systemSettings_serverDetails_tz());
        final StaticTextItem serverTime = new StaticTextItem("localtime",
            MSG.view_admin_systemSettings_serverDetails_time());
        final StaticTextItem serverInstallDir = new StaticTextItem("installdir",
            MSG.view_admin_systemSettings_serverDetails_installDir());
        final StaticTextItem dbUrl = new StaticTextItem("dbUrl", MSG.view_admin_systemSettings_serverDetails_dbUrl());
        final StaticTextItem dbProductName = new StaticTextItem("dbProductName",
            MSG.view_admin_systemSettings_serverDetails_dbName());
        final StaticTextItem dbProductVersion = new StaticTextItem("dbProductVersion",
            MSG.view_admin_systemSettings_serverDetails_dbVersion());
        final StaticTextItem dbDriverName = new StaticTextItem("dbDriverName",
            MSG.view_admin_systemSettings_serverDetails_dbDriverName());
        final StaticTextItem dbDriverVersion = new StaticTextItem("dbDriverVersion",
            MSG.view_admin_systemSettings_serverDetails_dbDriverVersion());

        productName.setWrapTitle(false);
        productVersion.setWrapTitle(false);
        productBuildNumber.setWrapTitle(false);
        serverName.setWrapTitle(false);
        serverTimezone.setWrapTitle(false);
        serverTime.setWrapTitle(false);
        serverInstallDir.setWrapTitle(false);
        dbUrl.setWrapTitle(false);
        dbProductName.setWrapTitle(false);
        dbProductVersion.setWrapTitle(false);
        dbDriverName.setWrapTitle(false);
        dbDriverVersion.setWrapTitle(false);

        form.setItems(productName, productVersion, productBuildNumber, serverName, serverTimezone, serverTime,
            serverInstallDir, dbUrl, dbProductName, dbProductVersion, dbDriverName, dbDriverVersion);

        GWTServiceLookup.getSystemService().getServerDetails(new AsyncCallback<ServerDetails>() {

            @Override
            public void onSuccess(ServerDetails result) {
                ProductInfo productInfo = result.getProductInfo();
                form.setValue(productName.getName(), productInfo.getName());
                form.setValue(productVersion.getName(), productInfo.getVersion());
                form.setValue(productBuildNumber.getName(), productInfo.getBuildNumber());

                Map<Detail, String> details = result.getDetails();
                form.setValue(serverName.getName(), details.get(ServerDetails.Detail.SERVER_IDENTITY));
                form.setValue(serverTimezone.getName(), details.get(ServerDetails.Detail.SERVER_TIMEZONE));
                form.setValue(serverTime.getName(), details.get(ServerDetails.Detail.SERVER_LOCAL_TIME));
                form.setValue(serverInstallDir.getName(), details.get(ServerDetails.Detail.SERVER_INSTALL_DIR));
                form.setValue(dbUrl.getName(), details.get(ServerDetails.Detail.DATABASE_CONNECTION_URL));
                form.setValue(dbProductName.getName(), details.get(ServerDetails.Detail.DATABASE_PRODUCT_NAME));
                form.setValue(dbProductVersion.getName(), details.get(ServerDetails.Detail.DATABASE_PRODUCT_VERSION));
                form.setValue(dbDriverName.getName(), details.get(ServerDetails.Detail.DATABASE_DRIVER_NAME));
                form.setValue(dbDriverVersion.getName(), details.get(ServerDetails.Detail.DATABASE_DRIVER_VERSION));
            }

            @Override
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_admin_systemSettings_cannotLoadServerDetails(), caught);
            }
        });

        return form;
    }

}
