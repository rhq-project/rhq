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
package org.rhq.enterprise.gui.admin.config;

import java.util.Properties;
import javax.servlet.http.HttpServletRequest;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.action.BaseValidatorForm;
import org.rhq.enterprise.server.legacy.common.shared.HQConstants;

public class SystemConfigForm extends BaseValidatorForm {
    private String baseUrl = "";
    private String helpUserId = "";
    private String helpPassword = "";
    private String deleteUnitsVal = "0";
    private String deleteUnits = "";
    private String maintIntervalVal = "0";
    private String maintInterval = "";
    private String rtPurgeVal = "0";
    private String rtPurge = "";
    private String alertPurgeVal = "0";
    private String alertPurge = "";
    private String baselineFrequencyVal = "0";
    private String baselineFrequency = "";
    private String baselineDataSetVal = "0";
    private String baselineDataSet = "";
    private String ldapUrl = "";
    private boolean ldapSsl = false;
    private String ldapLoginProperty = "";
    private String ldapSearchBase = "";
    private String ldapSearchFilter = "";
    private String ldapUsername = "";
    private String ldapPassword = "";
    private Boolean ldapEnabled = null;
    private boolean reindex = false;
    private String snmpAuthProtocol = "";
    private String snmpAuthPassphrase = "";
    private String snmpPrivacyPassphrase = "";
    private String snmpCommunity = "";
    private String snmpEngineID = "";
    private String snmpContextName = "";
    private String snmpSecurityName = "";
    private String snmpTrapOID = "";
    private String snmpEnterpriseOID = "";
    private String snmpGenericID = "";
    private String snmpSpecificID = "";
    private String snmpAgentAddress = "";
    private String snmpVersion = "";
    private String snmpPrivacyProtocol = "";

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer(super.toString());

        buf.append(" baseUrl=").append(baseUrl);
        buf.append(" helpUserId=").append(helpUserId);
        buf.append(" helpPassword=").append(helpPassword);
        buf.append(" deleteUnits=").append(deleteUnits);
        buf.append(" ldapEnabled=").append(ldapEnabled);
        buf.append(" ldapUrl=").append(ldapUrl);
        buf.append(" ldapSsl=").append(ldapSsl);
        buf.append(" ldapLoginProperty=").append(ldapLoginProperty);
        buf.append(" ldapSearchBase=").append(ldapSearchBase);
        buf.append(" ldapSearchFilter=").append(ldapSearchFilter);
        buf.append(" ldapUsername=").append(ldapUsername);
        buf.append(" ldapPassword=").append(ldapPassword);
        buf.append(" snmpAuthProtocol =").append(snmpAuthProtocol);
        buf.append(" snmpAuthPassphrase =").append(snmpAuthPassphrase);
        buf.append(" snmpPrivacyPassphrase =").append(snmpPrivacyPassphrase);
        buf.append(" snmpCommunity =").append(snmpCommunity);
        buf.append(" snmpEngineID =").append(snmpEngineID);
        buf.append(" snmpContextName =").append(snmpContextName);
        buf.append(" snmpSecurityName =").append(snmpSecurityName);
        buf.append(" snmpTrapOID =").append(snmpTrapOID);
        buf.append(" snmpEnterpriseOID =").append(snmpEnterpriseOID);
        buf.append(" snmpGenericID =").append(snmpGenericID);
        buf.append(" snmpSpecificID =").append(snmpSpecificID);
        buf.append(" snmpAgentAddress =").append(snmpAgentAddress);
        buf.append(" snmpVersion =").append(snmpVersion);
        buf.append(" snmpPrivacyProtocol =").append(snmpPrivacyProtocol);

        return buf.toString();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.struts.action.ActionForm#reset(org.apache.struts.action.ActionMapping,
     * javax.servlet.http.HttpServletRequest)
     */
    @Override
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        helpUserId = "";
        helpPassword = "";
        deleteUnits = "";
        deleteUnitsVal = null;
        maintInterval = "";
        maintIntervalVal = null;
        rtPurge = "";
        rtPurgeVal = null;
        alertPurge = "";
        alertPurgeVal = null;
        baselineFrequency = "";
        baselineFrequencyVal = null;
        baselineDataSet = "";
        baselineDataSetVal = null;
        ldapEnabled = null;
        ldapUrl = "";
        ldapSsl = false;
        ldapLoginProperty = "";
        ldapSearchBase = "";
        ldapSearchFilter = "";
        ldapUsername = "";
        ldapPassword = "";
        snmpAuthProtocol = "";
        snmpAuthPassphrase = "";
        snmpPrivacyPassphrase = "";
        snmpCommunity = "";
        snmpEngineID = "";
        snmpContextName = "";
        snmpSecurityName = "";
        snmpTrapOID = "";
        snmpEnterpriseOID = "";
        snmpGenericID = "";
        snmpSpecificID = "";
        snmpAgentAddress = "";
        snmpVersion = "";
        snmpPrivacyProtocol = "";

        super.reset(mapping, request);
    }

    public void loadConfigProperties(Properties prop) {
        baseUrl = prop.getProperty(HQConstants.BaseURL);
        helpUserId = prop.getProperty(HQConstants.HelpUser);
        helpPassword = prop.getProperty(HQConstants.HelpUserPassword);

        String deleteUnitsValStr = prop.getProperty(HQConstants.DataPurgeRaw);
        Long deleteUnitInt = new Long(deleteUnitsValStr);
        deleteUnits = findTimeUnit(deleteUnitInt.longValue());
        deleteUnitsVal = calcTimeUnit(deleteUnitInt.longValue());

        String maintIntervalValStr = prop.getProperty(HQConstants.DataMaintenance);
        Long maintIntervalLong = new Long(maintIntervalValStr);
        maintInterval = findTimeUnit(maintIntervalLong.longValue());
        maintIntervalVal = calcTimeUnit(maintIntervalLong.longValue());

        String nightlyReindexStr = prop.getProperty(HQConstants.DataReindex);
        reindex = Boolean.valueOf(nightlyReindexStr).booleanValue();

        String rtPurgeValStr = prop.getProperty(HQConstants.RtDataPurge);
        Long rtPurgeLong = new Long(rtPurgeValStr);
        rtPurge = findTimeUnit(rtPurgeLong.longValue());
        rtPurgeVal = calcTimeUnit(rtPurgeLong.longValue());

        String alertPurgeValStr = prop.getProperty(HQConstants.AlertPurge);
        Long alertPurgeLong = new Long(alertPurgeValStr);
        alertPurge = findTimeUnit(alertPurgeLong.longValue());
        alertPurgeVal = calcTimeUnit(alertPurgeLong.longValue());

        String baselineFrequencyValStr = prop.getProperty(HQConstants.BaselineFrequency);
        Long baselineFrequencyLong = new Long(baselineFrequencyValStr);
        baselineFrequency = findTimeUnit(baselineFrequencyLong.longValue());
        baselineFrequencyVal = calcTimeUnit(baselineFrequencyLong.longValue());

        String baselineDataSetValStr = prop.getProperty(HQConstants.BaselineDataSet);
        Long baselineDataSetLong = new Long(baselineDataSetValStr);
        baselineDataSet = findTimeUnit(baselineDataSetLong.longValue());
        baselineDataSetVal = calcTimeUnit(baselineDataSetLong.longValue());

        ldapUrl = prop.getProperty(HQConstants.LDAPUrl);
        ldapLoginProperty = prop.getProperty(HQConstants.LDAPLoginProperty);
        ldapSearchBase = prop.getProperty(HQConstants.LDAPBaseDN);
        ldapSearchFilter = prop.getProperty(HQConstants.LDAPFilter);
        ldapUsername = prop.getProperty(HQConstants.LDAPBindDN);
        ldapPassword = prop.getProperty(HQConstants.LDAPBindPW);

        String ldapProtocol = prop.getProperty(HQConstants.LDAPProtocol);
        ldapSsl = ldapProtocol.equals("ssl");

        String jaasProvider = prop.getProperty(HQConstants.JAASProvider);
        ldapEnabled = HQConstants.LDAPJAASProvider.equals(jaasProvider) ? Boolean.TRUE : null;

        snmpAuthProtocol = prop.getProperty(HQConstants.SNMPAuthProtocol);
        snmpAuthPassphrase = prop.getProperty(HQConstants.SNMPAuthPassphrase);
        snmpPrivacyPassphrase = prop.getProperty(HQConstants.SNMPPrivacyPassphrase);
        snmpCommunity = prop.getProperty(HQConstants.SNMPCommunity);
        snmpEngineID = prop.getProperty(HQConstants.SNMPEngineID);
        snmpContextName = prop.getProperty(HQConstants.SNMPContextName);
        snmpSecurityName = prop.getProperty(HQConstants.SNMPSecurityName);
        snmpTrapOID = prop.getProperty(HQConstants.SNMPTrapOID);
        snmpEnterpriseOID = prop.getProperty(HQConstants.SNMPEnterpriseOID);
        snmpGenericID = prop.getProperty(HQConstants.SNMPGenericID);
        snmpSpecificID = prop.getProperty(HQConstants.SNMPSpecificID);
        snmpAgentAddress = prop.getProperty(HQConstants.SNMPAgentAddress);
        snmpVersion = prop.getProperty(HQConstants.SNMPVersion);
        snmpPrivacyProtocol = prop.getProperty(HQConstants.SNMPPrivacyProtocol);
    }

    /**
     * find the proper time unit associated with the timeUnit
     *
     * @return time unit label
     */
    private String findTimeUnit(long timeUnitInt) {
        if ((timeUnitInt % Constants.DAYS) == 0) {
            return Constants.DAYS_LABEL;
        } else if ((timeUnitInt % Constants.HOURS) == 0) {
            return Constants.HOURS_LABEL;
        } else {
            return Constants.MINUTES_LABEL;
        }
    }

    /**
     * find the proper time unit associated with the timeUnit
     *
     * @return time unit label
     */
    private String calcTimeUnit(long timeUnitInt) {
        if ((timeUnitInt % Constants.DAYS) == 0) {
            return String.valueOf(timeUnitInt / Constants.DAYS);
        } else if ((timeUnitInt % Constants.HOURS) == 0) {
            return String.valueOf(timeUnitInt / Constants.HOURS);
        } else {
            return String.valueOf(timeUnitInt / Constants.MINUTES);
        }
    }

    /**
     * find the proper time unit associated with the timeUnit
     *
     * @return time unit label
     */
    private long convertToMillisecond(long val, String timeLabel) {
        if (timeLabel.equalsIgnoreCase(Constants.DAYS_LABEL)) {
            return val * Constants.DAYS;
        } else if (timeLabel.equalsIgnoreCase(Constants.HOURS_LABEL)) {
            return val * Constants.HOURS;
        } else {
            return val * Constants.MINUTES;
        }
    }

    public Properties saveConfigProperties(Properties prop) {
        prop.setProperty(HQConstants.BaseURL, baseUrl);
        prop.setProperty(HQConstants.HelpUser, helpUserId);
        prop.setProperty(HQConstants.HelpUserPassword, helpPassword);

        long deleteUnitInt = convertToMillisecond(Integer.parseInt(deleteUnitsVal), deleteUnits);
        prop.setProperty(HQConstants.DataPurgeRaw, String.valueOf(deleteUnitInt));
        prop.setProperty(HQConstants.DataReindex, String.valueOf(reindex));

        long maintIntervalLong = convertToMillisecond(Integer.parseInt(maintIntervalVal), maintInterval);
        prop.setProperty(HQConstants.DataMaintenance, String.valueOf(maintIntervalLong));

        long rtPurgeLong = convertToMillisecond(Long.parseLong(rtPurgeVal), rtPurge);

        prop.setProperty(HQConstants.RtDataPurge, String.valueOf(rtPurgeLong));

        long alertPurgeLong = convertToMillisecond(Long.parseLong(alertPurgeVal), alertPurge);

        prop.setProperty(HQConstants.AlertPurge, String.valueOf(alertPurgeLong));

        long baselineFrequencyLong = convertToMillisecond(Integer.parseInt(baselineFrequencyVal), baselineFrequency);
        prop.setProperty(HQConstants.BaselineFrequency, String.valueOf(baselineFrequencyLong));

        long baselineDataSetLong = convertToMillisecond(Integer.parseInt(baselineDataSetVal), baselineDataSet);
        prop.setProperty(HQConstants.BaselineDataSet, String.valueOf(baselineDataSetLong));

        prop.setProperty(HQConstants.LDAPUrl, ldapUrl);
        prop.setProperty(HQConstants.LDAPLoginProperty, ldapLoginProperty);
        prop.setProperty(HQConstants.LDAPBaseDN, ldapSearchBase);
        prop.setProperty(HQConstants.LDAPFilter, ldapSearchFilter);
        prop.setProperty(HQConstants.LDAPBindDN, ldapUsername);
        prop.setProperty(HQConstants.LDAPBindPW, ldapPassword);
        prop.setProperty(HQConstants.LDAPProtocol, ldapSsl ? "ssl" : "");

        if (ldapEnabled != null) {
            prop.setProperty(HQConstants.JAASProvider, HQConstants.LDAPJAASProvider);
        } else {
            prop.setProperty(HQConstants.JAASProvider, HQConstants.JDBCJAASProvider);
        }

        prop.setProperty(HQConstants.SNMPVersion, snmpVersion);

        if (snmpVersion.length() > 0) {
            prop.setProperty(HQConstants.SNMPTrapOID, snmpTrapOID);

            if ("3".equals(snmpVersion)) {
                prop.setProperty(HQConstants.SNMPAuthProtocol, snmpAuthProtocol);
                prop.setProperty(HQConstants.SNMPAuthPassphrase, snmpAuthPassphrase);
                prop.setProperty(HQConstants.SNMPPrivacyPassphrase, snmpPrivacyPassphrase);
                prop.setProperty(HQConstants.SNMPPrivacyProtocol, snmpPrivacyProtocol);
                prop.setProperty(HQConstants.SNMPContextName, snmpContextName);
                prop.setProperty(HQConstants.SNMPSecurityName, snmpSecurityName);
            } else {
                snmpCommunity = prop.getProperty(HQConstants.SNMPCommunity);

                if ("1".equals(snmpVersion)) {
                    prop.setProperty(HQConstants.SNMPEngineID, snmpEngineID);
                    prop.setProperty(HQConstants.SNMPEnterpriseOID, snmpEnterpriseOID);
                    prop.setProperty(HQConstants.SNMPGenericID, snmpGenericID);
                    prop.setProperty(HQConstants.SNMPSpecificID, snmpSpecificID);
                    prop.setProperty(HQConstants.SNMPAgentAddress, snmpAgentAddress);
                }
            }
        }

        return prop;
    }

    public String getHelpPassword() {
        return helpPassword;
    }

    public String getHelpUserId() {
        return helpUserId;
    }

    public void setHelpPassword(String string) {
        helpPassword = string;
    }

    public void setHelpUserId(String string) {
        helpUserId = string;
    }

    public String getDeleteUnitsVal() {
        return deleteUnitsVal;
    }

    public void setDeleteUnitsVal(String v) {
        deleteUnitsVal = v;
    }

    public String getDeleteUnits() {
        return deleteUnits;
    }

    public void setDeleteUnits(String s) {
        deleteUnits = s;
    }

    public String getMaintIntervalVal() {
        return maintIntervalVal;
    }

    public void setMaintIntervalVal(String v) {
        maintIntervalVal = v;
    }

    public String getMaintInterval() {
        return maintInterval;
    }

    public void setMaintInterval(String s) {
        maintInterval = s;
    }

    public String getRtPurgeVal() {
        return rtPurgeVal;
    }

    public void setRtPurgeVal(String v) {
        rtPurgeVal = v;
    }

    public String getRtPurge() {
        return rtPurge;
    }

    public void setRtPurge(String s) {
        rtPurge = s;
    }

    public String getAlertPurgeVal() {
        return alertPurgeVal;
    }

    public void setAlertPurgeVal(String v) {
        alertPurgeVal = v;
    }

    public String getAlertPurge() {
        return alertPurge;
    }

    public void setAlertPurge(String s) {
        alertPurge = s;
    }

    public String getBaselineFrequencyVal() {
        return baselineFrequencyVal;
    }

    public void setBaselineFrequencyVal(String v) {
        baselineFrequencyVal = v;
    }

    public String getBaselineFrequency() {
        return baselineFrequency;
    }

    public void setBaselineFrequency(String s) {
        baselineFrequency = s;
    }

    public String getBaselineDataSetVal() {
        return baselineDataSetVal;
    }

    public void setBaselineDataSetVal(String v) {
        baselineDataSetVal = v;
    }

    public String getBaselineDataSet() {
        return baselineDataSet;
    }

    public void setBaselineDataSet(String s) {
        baselineDataSet = s;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String string) {
        baseUrl = string;
    }

    public Boolean getLdapEnabled() {
        return ldapEnabled;
    }

    public void setLdapEnabled(Boolean b) {
        if (b.booleanValue()) {
            ldapEnabled = Boolean.TRUE;
        }
    }

    public String getLdapUrl() {
        return ldapUrl;
    }

    public void setLdapUrl(String s) {
        ldapUrl = s;
    }

    public boolean getLdapSsl() {
        return ldapSsl;
    }

    public void setLdapSsl(boolean b) {
        ldapSsl = b;
    }

    public String getLdapLoginProperty() {
        return ldapLoginProperty;
    }

    public void setLdapLoginProperty(String s) {
        ldapLoginProperty = s;
    }

    public String getLdapSearchBase() {
        return ldapSearchBase;
    }

    public void setLdapSearchBase(String s) {
        ldapSearchBase = s;
    }

    public String getLdapSearchFilter() {
        return ldapSearchFilter;
    }

    public void setLdapSearchFilter(String s) {
        ldapSearchFilter = s;
    }

    public String getLdapUsername() {
        return ldapUsername;
    }

    public void setLdapUsername(String s) {
        ldapUsername = s;
    }

    public String getLdapPassword() {
        return ldapPassword;
    }

    public void setLdapPassword(String s) {
        ldapPassword = s;
    }

    public boolean getReindex() {
        return reindex;
    }

    public void setReindex(boolean reindex) {
        this.reindex = reindex;
    }

    public String getSnmpAgentAddress() {
        return snmpAgentAddress;
    }

    public void setSnmpAgentAddress(String snmpAgentAddress) {
        this.snmpAgentAddress = snmpAgentAddress;
    }

    public String getSnmpAuthPassphrase() {
        return snmpAuthPassphrase;
    }

    public void setSnmpAuthPassphrase(String snmpAuthPassphrase) {
        this.snmpAuthPassphrase = snmpAuthPassphrase;
    }

    public String getSnmpAuthProtocol() {
        return snmpAuthProtocol;
    }

    public void setSnmpAuthProtocol(String snmpAuthProtocol) {
        this.snmpAuthProtocol = snmpAuthProtocol;
    }

    public String getSnmpCommunity() {
        return snmpCommunity;
    }

    public void setSnmpCommunity(String snmpCommunity) {
        this.snmpCommunity = snmpCommunity;
    }

    public String getSnmpContextName() {
        return snmpContextName;
    }

    public void setSnmpContextName(String snmpContextName) {
        this.snmpContextName = snmpContextName;
    }

    public String getSnmpEngineID() {
        return snmpEngineID;
    }

    public void setSnmpEngineID(String snmpEngineID) {
        this.snmpEngineID = snmpEngineID;
    }

    public String getSnmpEnterpriseOID() {
        return snmpEnterpriseOID;
    }

    public void setSnmpEnterpriseOID(String snmpEnterpriseOID) {
        this.snmpEnterpriseOID = snmpEnterpriseOID;
    }

    public String getSnmpGenericID() {
        return snmpGenericID;
    }

    public void setSnmpGenericID(String snmpGenericID) {
        this.snmpGenericID = snmpGenericID;
    }

    public String getSnmpPrivacyPassphrase() {
        return snmpPrivacyPassphrase;
    }

    public void setSnmpPrivacyPassphrase(String snmpPrivacyPassphrase) {
        this.snmpPrivacyPassphrase = snmpPrivacyPassphrase;
    }

    public String getSnmpPrivacyProtocol() {
        return snmpPrivacyProtocol;
    }

    public void setSnmpPrivacyProtocol(String snmpPrivacyProtocol) {
        this.snmpPrivacyProtocol = snmpPrivacyProtocol;
    }

    public String getSnmpSecurityName() {
        return snmpSecurityName;
    }

    public void setSnmpSecurityName(String snmpSecurityName) {
        this.snmpSecurityName = snmpSecurityName;
    }

    public String getSnmpSpecificID() {
        return snmpSpecificID;
    }

    public void setSnmpSpecificID(String snmpSpecificID) {
        this.snmpSpecificID = snmpSpecificID;
    }

    public String getSnmpTrapOID() {
        return snmpTrapOID;
    }

    public void setSnmpTrapOID(String snmpTrapOID) {
        this.snmpTrapOID = snmpTrapOID;
    }

    public String getSnmpVersion() {
        return snmpVersion;
    }

    public void setSnmpVersion(String snmpVersion) {
        this.snmpVersion = snmpVersion;
    }

    /* (non-Javadoc)
     * @see org.apache.struts.action.ActionForm#validate(org.apache.struts.action.ActionMapping,
     * javax.servlet.http.HttpServletRequest)
     */
    @Override
    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        ActionErrors errors = super.validate(mapping, request);

        if (errors == null) {
            errors = new ActionErrors();
        }

        if (!empty(snmpVersion) && "3".equals(snmpVersion) && !empty(snmpAuthProtocol) && empty(snmpAuthPassphrase)) {
            ActionMessage errorMessage = new ActionMessage("admin.settings.SNMPAuthPassphrase");
            errors.add("snmpAuthPassphrase", errorMessage);
        }

        if (!empty(snmpVersion) && "3".equals(snmpVersion) && !empty(snmpPrivacyProtocol)
            && empty(snmpPrivacyPassphrase)) {
            ActionMessage errorMessage = new ActionMessage("admin.settings.SNMPPrivPassphrase");
            errors.add("snmpPrivacyPassphrase", errorMessage);
        }

        if (errors.isEmpty()) {
            return null;
        } else {
            return errors;
        }
    }

    private boolean empty(String s) {
        return (s == null) || (s.length() == 0);
    }
}