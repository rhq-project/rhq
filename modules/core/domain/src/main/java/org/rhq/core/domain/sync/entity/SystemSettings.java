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

package org.rhq.core.domain.sync.entity;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * A domain class representing the system settings of an RHQ installation.
 *
 * @author Lukas Krejci
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class SystemSettings extends AbstractExportedEntity {

    private static final long serialVersionUID = 1L;

    // The below are copied from RHQConstants (residing in the server jar) but kept here
    // so that we can provide an easy to use conversions to and from a map of values
    private static final String JAASProvider = "CAM_JAAS_PROVIDER";
    private static final String JDBCJAASProvider = "JDBC";
    private static final String LDAPJAASProvider = "LDAP";
    private static final String LDAPFactory = "CAM_LDAP_NAMING_FACTORY_INITIAL";
    private static final String LDAPUrl = "CAM_LDAP_NAMING_PROVIDER_URL";
    private static final String LDAPProtocol = "CAM_LDAP_PROTOCOL";
    private static final String LDAPLoginProperty = "CAM_LDAP_LOGIN_PROPERTY";
    private static final String LDAPFilter = "CAM_LDAP_FILTER";
    private static final String LDAPGroupFilter = "CAM_LDAP_GROUP_FILTER";
    private static final String LDAPGroupMember = "CAM_LDAP_GROUP_MEMBER";
    private static final String LDAPBaseDN = "CAM_LDAP_BASE_DN";
    private static final String LDAPBindDN = "CAM_LDAP_BIND_DN";
    private static final String LDAPBindPW = "CAM_LDAP_BIND_PW";
    private static final String BaseURL = "CAM_BASE_URL";
    private static final String AgentMaxQuietTimeAllowed = "AGENT_MAX_QUIET_TIME_ALLOWED";
    private static final String EnableAgentAutoUpdate = "ENABLE_AGENT_AUTO_UPDATE";
    private static final String EnableDebugMode = "ENABLE_DEBUG_MODE";
    private static final String EnableExperimentalFeatures = "ENABLE_EXPERIMENTAL_FEATURES";
    private static final String DataPurge1Hour = "CAM_DATA_PURGE_1H";
    private static final String DataPurge6Hour = "CAM_DATA_PURGE_6H";
    private static final String DataPurge1Day = "CAM_DATA_PURGE_1D";
    private static final String DataMaintenance = "CAM_DATA_MAINTENANCE";
    private static final String DataReindex = "DATA_REINDEX_NIGHTLY";
    private static final String RtDataPurge = "RT_DATA_PURGE";
    private static final String AlertPurge = "ALERT_PURGE";
    private static final String EventPurge = "EVENT_PURGE";
    private static final String TraitPurge = "TRAIT_PURGE";
    private static final String AvailabilityPurge = "AVAILABILITY_PURGE";
    private static final String BaselineFrequency = "CAM_BASELINE_FREQUENCY";
    private static final String BaselineDataSet = "CAM_BASELINE_DATASET";

    //this is left out from the export, because we have no way of editing this in the UI
    //private static final String AllowResourceGenericPropertiesUpgrade = "RESOURCE_GENERIC_PROPERTIES_UPGRADE";
    
    private String jAASProvider;
    private String jDBCJAASProvider;
    private String lDAPJAASProvider;
    private String lDAPFactory;
    private String lDAPUrl;
    private String lDAPProtocol;
    private String lDAPLoginProperty;
    private String lDAPFilter;
    private String lDAPGroupFilter;
    private String lDAPGroupMember;
    private String lDAPBaseDN;
    private String lDAPBindDN;
    private String lDAPBindPW;
    private String baseURL;
    private String agentMaxQuietTimeAllowed;
    private String enableAgentAutoUpdate;
    private String enableDebugMode;
    private String enableExperimentalFeatures;
    private String dataPurge1Hour;
    private String dataPurge6Hour;
    private String dataPurge1Day;
    private String dataMaintenance;
    private String dataReindex;
    private String rtDataPurge;
    private String alertPurge;
    private String eventPurge;
    private String traitPurge;
    private String availabilityPurge;
    private String baselineFrequency;
    private String baselineDataSet;

    //this is left out from the export, because we have no way of editing this in the UI
    //private String allowresourceGenericPropertiesUpgrade;

    public SystemSettings() {
        setReferencedEntityId(0);
    }

    public SystemSettings(Map<String, String> settings) {
        this();
        initFrom(settings);
    }

    public SystemSettings(Properties settings) {
        this();
        HashMap<String, String> map = new HashMap<String, String>();
        for(Map.Entry<Object, Object> entry : settings.entrySet()) {
            String key = entry.getKey().toString();
            String value = entry.getValue().toString();
            map.put(key, value);
        }
        
        initFrom(map);
    }
    
    public Map<String, String> toMap() {
        HashMap<String, String> settings = new HashMap<String, String>(); 
        
        settings.put(AgentMaxQuietTimeAllowed, agentMaxQuietTimeAllowed);
        settings.put(AlertPurge, alertPurge);
        settings.put(AvailabilityPurge, availabilityPurge);
        settings.put(BaselineDataSet, baselineDataSet);
        settings.put(BaselineFrequency, baselineFrequency);
        settings.put(BaseURL, baseURL);
        settings.put(DataMaintenance, dataMaintenance);
        settings.put(DataPurge1Day, dataPurge1Day);
        settings.put(DataPurge1Hour, dataPurge1Hour);
        settings.put(DataPurge6Hour, dataPurge6Hour);
        settings.put(DataReindex, dataReindex);
        settings.put(EnableAgentAutoUpdate, enableAgentAutoUpdate);
        settings.put(EnableDebugMode, enableDebugMode);
        settings.put(EnableExperimentalFeatures, enableExperimentalFeatures);
        settings.put(EventPurge, eventPurge);
        settings.put(JAASProvider, jAASProvider);
        settings.put(JDBCJAASProvider, jDBCJAASProvider);
        settings.put(LDAPBaseDN, lDAPBaseDN);
        settings.put(LDAPBindDN, lDAPBindDN);
        settings.put(LDAPBindPW, lDAPBindPW);
        settings.put(LDAPFactory, lDAPFactory);
        settings.put(LDAPFilter, lDAPFilter);
        settings.put(LDAPGroupFilter, lDAPGroupFilter);
        settings.put(LDAPGroupMember, lDAPGroupMember);
        settings.put(LDAPJAASProvider, lDAPJAASProvider);
        settings.put(LDAPLoginProperty, lDAPLoginProperty);
        settings.put(LDAPProtocol, lDAPProtocol);
        settings.put(LDAPUrl, lDAPUrl);
        settings.put(RtDataPurge, rtDataPurge);
        settings.put(TraitPurge, traitPurge);
        //settings.put(AllowResourceGenericPropertiesUpgrade, allowResourceGenericPropertiesUpgrade);
        
        return settings;
    }

    public Properties toProperties() {
        Properties ret = new Properties();
        ret.putAll(toMap());
        return ret;
    }
    
    public void initFrom(Map<String, String> settings) {
        agentMaxQuietTimeAllowed = settings.get(AgentMaxQuietTimeAllowed);
        alertPurge = settings.get(AlertPurge);
        availabilityPurge = settings.get(AvailabilityPurge);
        baselineDataSet = settings.get(BaselineDataSet);
        baselineFrequency = settings.get(BaselineFrequency);
        baseURL = settings.get(BaseURL);
        dataMaintenance = settings.get(DataMaintenance);
        dataPurge1Day = settings.get(DataPurge1Day);
        dataPurge1Hour = settings.get(DataPurge1Hour);
        dataPurge6Hour = settings.get(DataPurge6Hour);
        dataReindex = settings.get(DataReindex);
        enableAgentAutoUpdate = settings.get(EnableAgentAutoUpdate);
        enableDebugMode = settings.get(EnableDebugMode);
        enableExperimentalFeatures = settings.get(EnableExperimentalFeatures);
        eventPurge = settings.get(EventPurge);
        jAASProvider = settings.get(JAASProvider);
        jDBCJAASProvider = settings.get(JDBCJAASProvider);
        lDAPBaseDN = settings.get(LDAPBaseDN);
        lDAPBindDN = settings.get(LDAPBindDN);
        lDAPBindPW = settings.get(LDAPBindPW);
        lDAPFactory = settings.get(LDAPFactory);
        lDAPFilter = settings.get(LDAPFilter);
        lDAPGroupFilter = settings.get(LDAPGroupFilter);
        lDAPGroupMember = settings.get(LDAPGroupMember);
        lDAPJAASProvider = settings.get(LDAPJAASProvider);
        lDAPLoginProperty = settings.get(LDAPLoginProperty);
        lDAPProtocol = settings.get(LDAPProtocol);
        lDAPUrl = settings.get(LDAPUrl);
        rtDataPurge = settings.get(RtDataPurge);
        traitPurge = settings.get(TraitPurge);
        //allowresourceGenericPropertiesUpgrade = settings.get(AllowResourceGenericPropertiesUpgrade);
    }
    
    /**
     * @return the jAASProvider
     */
    public String getJAASProvider() {
        return jAASProvider;
    }

    /**
     * @param jAASProvider the jAASProvider to set
     */
    public void setJAASProvider(String jAASProvider) {
        this.jAASProvider = jAASProvider;
    }

    /**
     * @return the jDBCJAASProvider
     */
    public String getJDBCJAASProvider() {
        return jDBCJAASProvider;
    }

    /**
     * @param jDBCJAASProvider the jDBCJAASProvider to set
     */
    public void setJDBCJAASProvider(String jDBCJAASProvider) {
        this.jDBCJAASProvider = jDBCJAASProvider;
    }

    /**
     * @return the lDAPJAASProvider
     */
    public String getLDAPJAASProvider() {
        return lDAPJAASProvider;
    }

    /**
     * @param lDAPJAASProvider the lDAPJAASProvider to set
     */
    public void setLDAPJAASProvider(String lDAPJAASProvider) {
        this.lDAPJAASProvider = lDAPJAASProvider;
    }

    /**
     * @return the lDAPFactory
     */
    public String getLDAPFactory() {
        return lDAPFactory;
    }

    /**
     * @param lDAPFactory the lDAPFactory to set
     */
    public void setLDAPFactory(String lDAPFactory) {
        this.lDAPFactory = lDAPFactory;
    }

    /**
     * @return the lDAPUrl
     */
    public String getLDAPUrl() {
        return lDAPUrl;
    }

    /**
     * @param lDAPUrl the lDAPUrl to set
     */
    public void setLDAPUrl(String lDAPUrl) {
        this.lDAPUrl = lDAPUrl;
    }

    /**
     * @return the lDAPProtocol
     */
    public String getLDAPProtocol() {
        return lDAPProtocol;
    }

    /**
     * @param lDAPProtocol the lDAPProtocol to set
     */
    public void setLDAPProtocol(String lDAPProtocol) {
        this.lDAPProtocol = lDAPProtocol;
    }

    /**
     * @return the lDAPLoginProperty
     */
    public String getLDAPLoginProperty() {
        return lDAPLoginProperty;
    }

    /**
     * @param lDAPLoginProperty the lDAPLoginProperty to set
     */
    public void setLDAPLoginProperty(String lDAPLoginProperty) {
        this.lDAPLoginProperty = lDAPLoginProperty;
    }

    /**
     * @return the lDAPFilter
     */
    public String getLDAPFilter() {
        return lDAPFilter;
    }

    /**
     * @param lDAPFilter the lDAPFilter to set
     */
    public void setLDAPFilter(String lDAPFilter) {
        this.lDAPFilter = lDAPFilter;
    }

    /**
     * @return the lDAPGroupFilter
     */
    public String getLDAPGroupFilter() {
        return lDAPGroupFilter;
    }

    /**
     * @param lDAPGroupFilter the lDAPGroupFilter to set
     */
    public void setLDAPGroupFilter(String lDAPGroupFilter) {
        this.lDAPGroupFilter = lDAPGroupFilter;
    }

    /**
     * @return the lDAPGroupMember
     */
    public String getLDAPGroupMember() {
        return lDAPGroupMember;
    }

    /**
     * @param lDAPGroupMember the lDAPGroupMember to set
     */
    public void setLDAPGroupMember(String lDAPGroupMember) {
        this.lDAPGroupMember = lDAPGroupMember;
    }

    /**
     * @return the lDAPBaseDN
     */
    public String getLDAPBaseDN() {
        return lDAPBaseDN;
    }

    /**
     * @param lDAPBaseDN the lDAPBaseDN to set
     */
    public void setLDAPBaseDN(String lDAPBaseDN) {
        this.lDAPBaseDN = lDAPBaseDN;
    }

    /**
     * @return the lDAPBindDN
     */
    public String getLDAPBindDN() {
        return lDAPBindDN;
    }

    /**
     * @param lDAPBindDN the lDAPBindDN to set
     */
    public void setLDAPBindDN(String lDAPBindDN) {
        this.lDAPBindDN = lDAPBindDN;
    }

    /**
     * @return the lDAPBindPW
     */
    public String getLDAPBindPW() {
        return lDAPBindPW;
    }

    /**
     * @param lDAPBindPW the lDAPBindPW to set
     */
    public void setLDAPBindPW(String lDAPBindPW) {
        this.lDAPBindPW = lDAPBindPW;
    }

    /**
     * @return the baseURL
     */
    public String getBaseURL() {
        return baseURL;
    }

    /**
     * @param baseURL the baseURL to set
     */
    public void setBaseURL(String baseURL) {
        this.baseURL = baseURL;
    }

    /**
     * @return the agentMaxQuietTimeAllowed
     */
    public String getAgentMaxQuietTimeAllowed() {
        return agentMaxQuietTimeAllowed;
    }

    /**
     * @param agentMaxQuietTimeAllowed the agentMaxQuietTimeAllowed to set
     */
    public void setAgentMaxQuietTimeAllowed(String agentMaxQuietTimeAllowed) {
        this.agentMaxQuietTimeAllowed = agentMaxQuietTimeAllowed;
    }

    /**
     * @return the enableAgentAutoUpdate
     */
    public String getEnableAgentAutoUpdate() {
        return enableAgentAutoUpdate;
    }

    /**
     * @param enableAgentAutoUpdate the enableAgentAutoUpdate to set
     */
    public void setEnableAgentAutoUpdate(String enableAgentAutoUpdate) {
        this.enableAgentAutoUpdate = enableAgentAutoUpdate;
    }

    /**
     * @return the enableDebugMode
     */
    public String getEnableDebugMode() {
        return enableDebugMode;
    }

    /**
     * @param enableDebugMode the enableDebugMode to set
     */
    public void setEnableDebugMode(String enableDebugMode) {
        this.enableDebugMode = enableDebugMode;
    }

    /**
     * @return the enableExperimentalFeatures
     */
    public String getEnableExperimentalFeatures() {
        return enableExperimentalFeatures;
    }

    /**
     * @param enableExperimentalFeatures the enableExperimentalFeatures to set
     */
    public void setEnableExperimentalFeatures(String enableExperimentalFeatures) {
        this.enableExperimentalFeatures = enableExperimentalFeatures;
    }

    /**
     * @return the dataPurge1Hour
     */
    public String getDataPurge1Hour() {
        return dataPurge1Hour;
    }

    /**
     * @param dataPurge1Hour the dataPurge1Hour to set
     */
    public void setDataPurge1Hour(String dataPurge1Hour) {
        this.dataPurge1Hour = dataPurge1Hour;
    }

    /**
     * @return the dataPurge6Hour
     */
    public String getDataPurge6Hour() {
        return dataPurge6Hour;
    }

    /**
     * @param dataPurge6Hour the dataPurge6Hour to set
     */
    public void setDataPurge6Hour(String dataPurge6Hour) {
        this.dataPurge6Hour = dataPurge6Hour;
    }

    /**
     * @return the dataPurge1Day
     */
    public String getDataPurge1Day() {
        return dataPurge1Day;
    }

    /**
     * @param dataPurge1Day the dataPurge1Day to set
     */
    public void setDataPurge1Day(String dataPurge1Day) {
        this.dataPurge1Day = dataPurge1Day;
    }

    /**
     * @return the dataMaintenance
     */
    public String getDataMaintenance() {
        return dataMaintenance;
    }

    /**
     * @param dataMaintenance the dataMaintenance to set
     */
    public void setDataMaintenance(String dataMaintenance) {
        this.dataMaintenance = dataMaintenance;
    }

    /**
     * @return the dataReindex
     */
    public String getDataReindex() {
        return dataReindex;
    }

    /**
     * @param dataReindex the dataReindex to set
     */
    public void setDataReindex(String dataReindex) {
        this.dataReindex = dataReindex;
    }

    /**
     * @return the rtDataPurge
     */
    public String getRtDataPurge() {
        return rtDataPurge;
    }

    /**
     * @param rtDataPurge the rtDataPurge to set
     */
    public void setRtDataPurge(String rtDataPurge) {
        this.rtDataPurge = rtDataPurge;
    }

    /**
     * @return the alertPurge
     */
    public String getAlertPurge() {
        return alertPurge;
    }

    /**
     * @param alertPurge the alertPurge to set
     */
    public void setAlertPurge(String alertPurge) {
        this.alertPurge = alertPurge;
    }

    /**
     * @return the eventPurge
     */
    public String getEventPurge() {
        return eventPurge;
    }

    /**
     * @param eventPurge the eventPurge to set
     */
    public void setEventPurge(String eventPurge) {
        this.eventPurge = eventPurge;
    }

    /**
     * @return the traitPurge
     */
    public String getTraitPurge() {
        return traitPurge;
    }

    /**
     * @param traitPurge the traitPurge to set
     */
    public void setTraitPurge(String traitPurge) {
        this.traitPurge = traitPurge;
    }

    /**
     * @return the availabilityPurge
     */
    public String getAvailabilityPurge() {
        return availabilityPurge;
    }

    /**
     * @param availabilityPurge the availabilityPurge to set
     */
    public void setAvailabilityPurge(String availabilityPurge) {
        this.availabilityPurge = availabilityPurge;
    }

    /**
     * @return the baselineFrequency
     */
    public String getBaselineFrequency() {
        return baselineFrequency;
    }

    /**
     * @param baselineFrequency the baselineFrequency to set
     */
    public void setBaselineFrequency(String baselineFrequency) {
        this.baselineFrequency = baselineFrequency;
    }

    /**
     * @return the baselineDataSet
     */
    public String getBaselineDataSet() {
        return baselineDataSet;
    }

    /**
     * @param baselineDataSet the baselineDataSet to set
     */
    public void setBaselineDataSet(String baselineDataSet) {
        this.baselineDataSet = baselineDataSet;
    }
}
