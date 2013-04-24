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

import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertConditionCategory;
import org.rhq.core.domain.alert.AlertConditionOperator;
import org.rhq.enterprise.server.alert.engine.model.AbstractCacheElement;
import org.rhq.enterprise.server.alert.engine.model.UnsupportedAlertConditionOperatorException;

/**
 * @author Joseph Marques
 */
class AlertConditionCacheUtils {

    private static final Log log = LogFactory.getLog(AlertConditionCacheCoordinator.class);

    public static AlertConditionOperator getAlertConditionOperator(AlertCondition alertCondition) {

        AlertConditionCategory category = alertCondition.getCategory();
        String name = alertCondition.getName();
        String comparator = alertCondition.getComparator();

        switch (category) {
        case CONTROL:
            // the UI currently only supports one operator for control
            return AlertConditionOperator.EQUALS;

        case EVENT:
            // the UI currently only supports one operator for events
            return AlertConditionOperator.GREATER_THAN_OR_EQUAL_TO;

        case DRIFT:
            // any drift that is detected infers a change to its previous state
            return AlertConditionOperator.CHANGES;

        case RESOURCE_CONFIG:
        case CHANGE:
            // the model currently supports CHANGE as a category type instead of a comparator
            return AlertConditionOperator.CHANGES;

        case TRAIT:
            String regex = alertCondition.getOption();
            return (null == regex || regex.isEmpty()) ? AlertConditionOperator.CHANGES : AlertConditionOperator.REGEX;

        case AVAILABILITY: {
            AlertConditionOperator operator = AlertConditionOperator.valueOf(name.toUpperCase());

            switch (operator) {
            case AVAIL_GOES_DISABLED:
            case AVAIL_GOES_DOWN:
            case AVAIL_GOES_UNKNOWN:
            case AVAIL_GOES_UP:
            case AVAIL_GOES_NOT_UP:
                return operator;

            default:
                throw new UnsupportedAlertConditionOperatorException(
                    "Invalid alertConditionValue for AVAILABILITY category:" + operator);
            }
        }

        case AVAIL_DURATION: {
            AlertConditionOperator operator = AlertConditionOperator.valueOf(name.toUpperCase());

            switch (operator) {
            case AVAIL_DURATION_DOWN:
            case AVAIL_DURATION_NOT_UP:
                return operator;

            default:
                throw new UnsupportedAlertConditionOperatorException(
                    "Invalid alertConditionValue for AVAILABILITY_DURATION category:" + operator);
            }
        }

        case RANGE:
            // range can support <= and >=, which we look for here. It can also support < and >, which is checked down below further.
            // note that RANGE does not support =, so we throw an exception if caller tries that
            if (comparator.equals("<=")) {
                return AlertConditionOperator.LESS_THAN_OR_EQUAL_TO;

            } else if (comparator.equals(">=")) {
                return AlertConditionOperator.GREATER_THAN_OR_EQUAL_TO;

            } else if (comparator.equals("=")) {
                throw new UnsupportedAlertConditionOperatorException("Comparator [" + comparator + "] "
                    + "is not supported for category: " + category.name());
            }

        default:

            if (comparator.equals("<")) {
                return AlertConditionOperator.LESS_THAN;
            } else if (comparator.equals(">")) {
                return AlertConditionOperator.GREATER_THAN;
            } else if (comparator.equals("=")) {
                return AlertConditionOperator.EQUALS;
            } else {
                throw new UnsupportedAlertConditionOperatorException("Comparator [" + comparator + "] "
                    + "is not supported for category: " + category.name());
            }
        }
    }

    public static String getCacheElementErrorString(int conditionId, AlertConditionOperator operator, Object option,
        Object value, Throwable exception) {
        return "id=" + conditionId + ", " + "operator=" + operator + ", "
            + ((option != null) ? ("option=" + option + ", ") : "") + "value=" + value + ", error message: "
            + exception.getMessage();
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
