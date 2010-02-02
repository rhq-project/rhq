/*
* RHQ Management Platform
* Copyright (C) 2009 Red Hat, Inc.
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
package org.rhq.enterprise.server.configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.DynamicConfigurationPropertyValue;
import org.rhq.enterprise.server.RHQConstants;

/**
 * @see DynamicConfigurationPropertyLocal
 *
 * @author Jason Dobies
 */
@Stateless
public class DynamicConfigurationPropertyBean implements DynamicConfigurationPropertyLocal {

    private final Log log = LogFactory.getLog(this.getClass());

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public List<DynamicConfigurationPropertyValue> lookupValues(String key) {

        String queryName = PropertyExpressionEvaluator.getQueryNameForKey(key);

        if (log.isDebugEnabled()) {
            log.debug("Lookup value for key [" + key + "] found query name [" + queryName + "]");
        }

        if (queryName == null) {
            return Collections.emptyList();
        }

        Query query = entityManager.createNamedQuery(queryName);
        List<Object[]> results = query.getResultList();

        List<DynamicConfigurationPropertyValue> values =
            new ArrayList<DynamicConfigurationPropertyValue>(results.size());
        for (Object[] result : results) {
            DynamicConfigurationPropertyValue value = translate(result);
            values.add(value);
        }

        return values;
    }

    public DynamicConfigurationPropertyValue translate(Object[] results) {
        DynamicConfigurationPropertyValue value =
            new DynamicConfigurationPropertyValue((String) results[0], (String) results[1]);
        return value;
    }
}
