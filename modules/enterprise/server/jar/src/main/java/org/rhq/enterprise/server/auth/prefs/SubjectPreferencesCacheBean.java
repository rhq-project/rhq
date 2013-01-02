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
package org.rhq.enterprise.server.auth.prefs;

import java.util.HashMap;
import java.util.Map;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.common.EntityManagerFacadeLocal;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;

@ConcurrencyManagement(ConcurrencyManagementType.CONTAINER)
@Singleton
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class SubjectPreferencesCacheBean implements SubjectPreferencesCacheLocal {

    protected final Log log = LogFactory.getLog(SubjectPreferencesCacheBean.class);

    private Map<Integer, Configuration> subjectPreferences;

    @EJB
    private SubjectManagerLocal subjectManager;

    @EJB
    private EntityManagerFacadeLocal entityManagerFacade;

    @EJB
    private ConfigurationManagerLocal configurationManager;

    private SubjectPreferencesCacheBean() {
        subjectPreferences = new HashMap<Integer, Configuration>();
    }

    private void load(int subjectId) {
        // if subject ID is 0, it probably means this is a new LDAP user that needs to be registered
        if (subjectId != 0 && !subjectPreferences.containsKey(subjectId)) {
            try {
                Subject subject = subjectManager.loadUserConfiguration(subjectId);
                Configuration configuration = subject.getUserConfiguration();
                subjectPreferences.put(subjectId, configuration);
            } catch (Throwable t) {
                log.warn("Can not get preferences for subject[id=" + subjectId + "], subject does not exist yet");
            }
        }
    }

    @Override
    @Lock(LockType.READ)
    public PropertySimple getUserProperty(int subjectId, String propertyName) {
        load(subjectId);

        Configuration config = subjectPreferences.get(subjectId);
        if (config == null) {
            return null;
        }

        PropertySimple prop = config.getSimple(propertyName);
        if (prop == null) {
            return null;
        }

        return new PropertySimple(propertyName, prop.getStringValue());
    }

    @Override
    @Lock(LockType.WRITE)
    public void setUserProperty(int subjectId, String propertyName, String value) {
        load(subjectId);

        Configuration config = subjectPreferences.get(subjectId);
        if (config == null) {
            return;
        }

        PropertySimple prop = config.getSimple(propertyName);
        if (prop == null) {
            prop = new PropertySimple(propertyName, value);
            config.put(prop); // add new to collection
            mergeProperty(prop);
        } else if (prop.getStringValue() == null || !prop.getStringValue().equals(value)) {
            prop.setStringValue(value);
            mergeProperty(prop);
        }
    }

    private void mergeProperty(PropertySimple prop) {
        // merge will persist if property doesn't exist (i.e., id = 0)
        PropertySimple mergedProperty = entityManagerFacade.merge(prop); // only merge changes
        if (prop.getId() == 0) {
            // so subsequent merges do not continue re-persisting property as new
            prop.setId(mergedProperty.getId());
        }
    }

    @Override
    @Lock(LockType.WRITE)
    public void unsetUserProperty(int subjectId, String propertyName) {
        load(subjectId);

        Configuration config = subjectPreferences.get(subjectId);
        if (config == null) {
            return;
        }

        Property property = config.remove(propertyName);
        // it's possible property was already removed, and thus this operation becomes a no-op to the backing store
        if (property != null && property.getId() != 0) {
            try {
                configurationManager.deleteProperties(new int[] { property.getId() });
            } catch (Throwable t) {
                log.error("Could not remove " + property, t);
            }
        }
    }

    /**    
     * @param subjectId the subject to get preferences of
     * @return the <b>COPY</b> of the configuration object - changes done to that instance will not be reflected in the persisted
     * preferences
     */
    @Override
    @Lock(LockType.READ)
    public Configuration getPreferences(int subjectId) {
        load(subjectId);
        
        Configuration config = subjectPreferences.get(subjectId);
        if (config == null) {
            return new Configuration();
        } else {
            return config.deepCopy();
        }
    }

    @Override
    @Lock(LockType.WRITE)
    public void clearConfiguration(int subjectId) {
        if (log.isTraceEnabled()) {
            log.trace("Removing PreferencesCache For " + subjectId);
        }
        subjectPreferences.remove(subjectId);
    }
}
