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

import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.alert.AlertConditionCategory;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.enterprise.server.alert.engine.model.AbstractCacheElement;
import org.rhq.enterprise.server.alert.engine.model.AlertConditionOperator;
import org.rhq.enterprise.server.alert.engine.model.UnsupportedAlertConditionOperatorException;

/**
 * @author Joseph Marques
 */
class AlertConditionCacheUtils {

    private static final Log log = LogFactory.getLog(AlertConditionCacheCoordinator.class);

    public static AlertConditionOperator getAlertConditionOperator(AlertConditionCategory category, String comparator,
        String conditionOption) {
        if (category == AlertConditionCategory.CONTROL) {
            // the UI currently only supports one operator for control
            return AlertConditionOperator.EQUALS;
        }

        if (category == AlertConditionCategory.EVENT) {
            // the UI currently only supports one operator for events
            return AlertConditionOperator.GREATER_THAN_OR_EQUAL_TO;
        }

        if (category == AlertConditionCategory.RESOURCE_CONFIG || category == AlertConditionCategory.CHANGE
            || category == AlertConditionCategory.TRAIT) {
            // the model currently supports CHANGE as a category type instead of a comparator
            return AlertConditionOperator.CHANGES;
        }

        if (category == AlertConditionCategory.AVAILABILITY) {
            AvailabilityType conditionOptionType = AvailabilityType.valueOf(conditionOption.toUpperCase());
            if (conditionOptionType == AvailabilityType.DOWN) {
                /*
                 * UI phrases this as "Goes DOWN", but we're going to store the cache element as CHANGES_FROM:UP
                 *
                 * This way, it'll work when the agent's goes suspect and null is persisted for AvailabilityType
                 */
                return AlertConditionOperator.CHANGES_FROM;
            } else if (conditionOptionType == AvailabilityType.UP) {
                /*
                 * UI phrases this as "Goes UP", but we're going to store the cache element as CHANGES_TO:UP
                 *
                 * This way, it'll work when the agent's comes back from being suspect, where it had null for its
                 * AvailabilityType
                 */
                return AlertConditionOperator.CHANGES_TO;
            } else {
                throw new UnsupportedAlertConditionOperatorException("Invalid alertCondition for AVAILABILITY category");
            }
        }

        if (comparator.equals("<")) {
            return AlertConditionOperator.LESS_THAN;
        } else if (comparator.equals(">")) {
            return AlertConditionOperator.GREATER_THAN;
        } else if (comparator.equals("=")) {
            return AlertConditionOperator.EQUALS;
        } else {
            throw new UnsupportedAlertConditionOperatorException("Comparator '" + comparator + "' "
                + "is not supported for ArtifactConditionCategory." + category.name());
        }
    }

    public static String getCacheElementErrorString(int conditionId, AlertConditionOperator operator, Object option,
        Object value) {
        return "id=" + conditionId + ", " + "operator=" + operator + ", "
            + ((option != null) ? ("option=" + option + ", ") : "") + "value=" + value;
    }

    public static <T extends AbstractCacheElement<?>> void printListCache(String cacheName, Map<Integer, List<T>> cache) {
        log.debug("Printing " + cacheName + "...");

        for (Map.Entry<Integer, List<T>> cacheElement : cache.entrySet()) {
            log.debug("key=" + cacheElement.getKey() + ", " + "value=" + cacheElement.getValue());
        }
    }

    public static <T extends AbstractCacheElement<?>> void printNestedCache(String cacheName,
        Map<Integer, Map<Integer, List<T>>> nestedCache) {
        log.debug("Printing " + cacheName + "...");
        for (Map.Entry<Integer, Map<Integer, List<T>>> cache : nestedCache.entrySet()) {
            for (Map.Entry<Integer, List<T>> cacheElement : cache.getValue().entrySet()) {
                log.debug("key1=" + cache.getKey() + ", " + "key2=" + cacheElement.getKey() + ", " + "value="
                    + cacheElement.getValue());
            }
        }
    }

    public static boolean isInvalidDouble(Double d) {
        return (d == null || Double.isNaN(d) || d == Double.POSITIVE_INFINITY || d == Double.NEGATIVE_INFINITY);
    }

    public static <S, T> int getMapListCount(Map<S, List<T>> mapList) {
        int count = 0;
        try {
            for (List<?> listValue : mapList.values()) {
                count += listValue.size();
            }
        } catch (Throwable t) {
            // don't let any exceptions bubble up to the calling SLSB layer
            log.error("Error counting MapList", t);
        }
        return count;
    }

    public static <R, S, T> int getMapMapListCount(Map<R, Map<S, List<T>>> mapMapList) {
        int count = 0;
        try {
            for (Map<S, List<T>> mapListValue : mapMapList.values()) {
                count += getMapListCount(mapListValue);
            }
        } catch (Throwable t) {
            // don't let any exceptions bubble up to the calling SLSB layer
            log.error("Error counting MapMapList", t);
        }
        return count;
    }
}
