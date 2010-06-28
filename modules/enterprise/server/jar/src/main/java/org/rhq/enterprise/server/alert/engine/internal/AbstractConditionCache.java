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
package org.rhq.enterprise.server.alert.engine.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.enterprise.server.alert.engine.AlertConditionCacheStats;
import org.rhq.enterprise.server.alert.engine.jms.CachedConditionProducerLocal;
import org.rhq.enterprise.server.alert.engine.model.AbstractCacheElement;
import org.rhq.enterprise.server.alert.engine.model.AlertConditionOperator;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Joseph Marques
 */
abstract class AbstractConditionCache {

    static final Log log = LogFactory.getLog(AlertConditionCacheCoordinator.class);
    static final int PAGE_SIZE = 250;

    private CachedConditionProducerLocal cachedConditionProducer;

    public AbstractConditionCache() {
        cachedConditionProducer = LookupUtil.getCachedConditionProducerLocal();
    }

    <T extends AbstractCacheElement<S>, S> void processCacheElements(List<T> cacheElements, S providedValue,
        long timestamp, AlertConditionCacheStats stats, Object... extraParams) {
        if (cacheElements == null) {
            return; // nothing to do
        }

        int errors = 0;

        for (T cacheElement : cacheElements) {
            boolean matched = cacheElement.process(providedValue, extraParams);

            if (matched) // send positive event in case of a match
            {
                try {
                    /*
                     * Set the active property for alertCondition-based cache elements, and send it on its way;
                     * Thus, even if the element is already active, we're going to send another message with the new
                     * value
                     */
                    cacheElement.setActive(true); // no harm to always set active (though, technically, STATELESS operators don't need it)
                    cachedConditionProducer.sendActivateAlertConditionMessage(
                        cacheElement.getAlertConditionTriggerId(), timestamp, cacheElement
                            .convertValueToString(providedValue), extraParams);

                    stats.matched++;
                } catch (Exception e) {
                    log.error("Error processing matched cache element '" + cacheElement + "': " + e.getMessage());
                    errors++;
                }
            } else // no match, negative event
            {
                /*
                 * but only send negative events if we're, 1) a type of operator that supports STATEFUL events, and
                 * 2) currently active
                 */
                if (cacheElement.isType(AlertConditionOperator.Type.STATEFUL) && cacheElement.isActive()) {
                    cacheElement.setActive(false);

                    try {
                        // send negative message
                        cachedConditionProducer.sendDeactivateAlertConditionMessage(cacheElement
                            .getAlertConditionTriggerId(), timestamp);
                    } catch (Exception e) {
                        log.error("Error sending deactivation message for cache element '" + cacheElement + "': "
                            + e.getMessage());
                        errors++;
                    }
                } else {
                    /*
                     * negative message, but nothing was active...so do nothing.
                     *
                     * this will occur in the overwhelming majority of cases.  in theory, since most of the time
                     * conditions exist to alert people of non-ideal system state, it will not fire in the POSITIVE very
                     * often.  thus, we suppress the firing of negative events unless we know we've already sent a
                     * POSITIVE event that we need to compensate for.
                     */
                }
            }
        }

        if (errors != 0) {
            log.error("There were " + errors + " alert conditions that did not fire. "
                + "Please check the configuration of the JMS subsystem and try again. ");
        }
    }

    <T extends AbstractCacheElement<?>> boolean addTo(String mapName, Map<Integer, List<T>> cache, Integer key,
        T cacheElement, int alertConditionId, AlertConditionCacheStats stats) {
        List<T> cacheElements = cache.get(key);

        if (cacheElements == null) {
            cacheElements = new ArrayList<T>();
            cache.put(key, cacheElements);
        }

        if (log.isTraceEnabled()) {
            log.trace("Inserted '" + mapName + "' element: " + "key=" + key + ", " + "value=" + cacheElement);
        }

        // and finally update stats and return whether it was success
        boolean success = cacheElements.add(cacheElement);
        if (success) {
            stats.created++;
        }

        return success;
    }

    public abstract int getCacheSize(AlertConditionCacheCoordinator.Cache cache);
}
