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

package org.rhq.enterprise.server.sync.importers;

import javax.persistence.EntityManager;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLStreamException;

import org.rhq.core.clientapi.agent.configuration.ConfigurationUtility;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.core.domain.sync.entity.SystemSettings;
import org.rhq.enterprise.server.sync.ExportReader;
import org.rhq.enterprise.server.sync.NoSingleEntity;
import org.rhq.enterprise.server.system.SystemManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class SystemSettingsImporter implements Importer<NoSingleEntity, SystemSettings> {

    private Subject subject;
    private SystemManagerLocal systemManager;
    private Configuration importConfiguration;
    private Unmarshaller unmarshaller;

    public SystemSettingsImporter(Subject subject, SystemManagerLocal systemManager) {
        try {
            this.subject = subject;
            this.systemManager = systemManager;
            JAXBContext context = JAXBContext.newInstance(SystemSettings.class);
            unmarshaller = context.createUnmarshaller();
        } catch (JAXBException e) {
            throw new IllegalStateException("Failed to initialize JAXB marshaller for MetricTemplate.", e);
        }        
    }
    
    @Override
    public ConfigurationDefinition getImportConfigurationDefinition() {
        ConfigurationDefinition def = new ConfigurationDefinition("SystemSettingsConfiguration", null);
        
        addSwitchToConfigDef(def, "importJAASProvider", false);
        addSwitchToConfigDef(def, "importJDBCJAASProvider", false);
        addSwitchToConfigDef(def, "importLDAPJAASProvider", false);
        addSwitchToConfigDef(def, "imoprtLDAPFactory", true);
        addSwitchToConfigDef(def, "importLDAPUrl", false);
        addSwitchToConfigDef(def, "importLDAPProtocol", false);
        addSwitchToConfigDef(def, "imoprtLDAPLoginProperty", false);
        addSwitchToConfigDef(def, "importLDAPFilter", false);
        addSwitchToConfigDef(def, "importLDAPGroupFilter", false);
        addSwitchToConfigDef(def, "importLDAPGroupMember", false);
        addSwitchToConfigDef(def, "importLDAPBaseDN", false);
        addSwitchToConfigDef(def, "importLDAPBindDN", false);
        addSwitchToConfigDef(def, "importLDAPBindPW", false);
        addSwitchToConfigDef(def, "importBaseURL", false);
        addSwitchToConfigDef(def, "importAgentMaxQuietTimeAllowed", true);
        addSwitchToConfigDef(def, "importEnableAgentAutoUpdate", true);
        addSwitchToConfigDef(def, "importEnableDebugMode", true);
        addSwitchToConfigDef(def, "importEnableExperimentalFeatures", true);
        addSwitchToConfigDef(def, "importDataPurge1Hour", true);
        addSwitchToConfigDef(def, "importDataPurge6Hour", true);
        addSwitchToConfigDef(def, "importDataPurge1Day", true);
        addSwitchToConfigDef(def, "importDataMaintenance", true);
        addSwitchToConfigDef(def, "importDataReindex", true);
        addSwitchToConfigDef(def, "imoprtRtDataPurge", true);
        addSwitchToConfigDef(def, "importAlertPurge", true);
        addSwitchToConfigDef(def, "importEventPurge", true);
        addSwitchToConfigDef(def, "importTraitPurge", true);
        addSwitchToConfigDef(def, "importAvailabilityPurge", true);
        addSwitchToConfigDef(def, "importBaselineFrequency", true);
        addSwitchToConfigDef(def, "importBaselineDataSet", true);
        
        ConfigurationUtility.initializeDefaultTemplate(def);
        
        return def;
    }

    @Override
    public void configure(Configuration importConfiguration) {
        this.importConfiguration = importConfiguration;
    }

    @Override
    public ExportedEntityMatcher<NoSingleEntity, SystemSettings> getExportedEntityMatcher() {
        return new NoSingleEntityMatcher<SystemSettings>();
    }

    @Override
    public void update(NoSingleEntity entity, SystemSettings exportedEntity) throws Exception {
        //TODO take the configuration into account
        systemManager.setSystemConfiguration(subject, exportedEntity.toProperties(), true);
    }

    @Override
    public SystemSettings unmarshallExportedEntity(ExportReader reader) throws XMLStreamException {
        try {
            return (SystemSettings) unmarshaller.unmarshal(reader);
        } catch (JAXBException e) {
            throw new XMLStreamException("Failed to unmarshal system settings.", e);
        }
    }

    @Override
    public void finishImport() {
    }
    
    private void addSwitchToConfigDef(ConfigurationDefinition def, String name, boolean defaultValue) {
        PropertyDefinitionSimple prop = new PropertyDefinitionSimple(name, null, true, PropertySimpleType.BOOLEAN);
        prop.setDefaultValue(Boolean.toString(defaultValue));
        def.put(prop);
    }
}
