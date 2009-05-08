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
package org.rhq.enterprise.server.alert.engine.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Joseph Marques
 */

public abstract class AbstractCacheElement<T> {
    protected final Log log = LogFactory.getLog(getClass());

    protected AlertConditionOperator alertConditionOperator;
    protected Object alertConditionOperatorOption;
    protected T alertConditionValue;
    protected int alertConditionTriggerId;
    protected boolean active;
    protected AbstractCacheElement<?> nextCacheElement;

    public AbstractCacheElement(AlertConditionOperator operator, T value, int conditionTriggerId) {
        this(operator, null, value, conditionTriggerId);
    }

    public AbstractCacheElement(AlertConditionOperator operator, Object operatorOption, T value, int conditionId) {
        if (getOperatorSupportsType(operator) == AlertConditionOperator.Type.NONE) {
            throw new UnsupportedAlertConditionOperatorException("operator '" + operator.toString() + "'"
                + " is unsupported in class " + getClass().getSimpleName() + " for AlertConditionId=" + conditionId);
        }

        /*
         * the value argument is always required.
         *
         * with absolute comparison operators (LESS_THAN, EQUALS, GREATER_THAN) the value represents the user-specified
         * value to compare against the system-collected items; it will not be updated unless the corresponding
         * alertCondition is explicitly updated by the user.
         *
         * with delta comparison operators (CHANGES, CHANGES_TO, or CHANGES_FROM) the value represents the most recently
         * seen value for the particular system-collected item that this cached element is going to be compared against;
         * every time the cache is checked against a particular item, the current value of that item will replace the
         * data stored in alertConditionValue; this allows the cache element itself to keep running knowledge of the
         * current value for this item without hitting the backing store; however, for correct processing, when this
         * cached element is created, it needs to know what the current value for the corresponding system-collected
         * item is; therefore, it's a required argument;
         *
         * unfortunately, for several subsystems null is a valid value (no availability data yet, no measurement data yet,
         * etc); accordingly, even though this argument is semantically required, it seems the most appropriate thing to
         * do would be to lift the null restriction altogether for CHANGES* operators and just educate the callers to
         * pass the proper data.
         */
        if (value == null) {
            if ((operator == AlertConditionOperator.CHANGES) || (operator == AlertConditionOperator.CHANGES_TO)
                || (operator == AlertConditionOperator.CHANGES_FROM)) {
                log.debug("Possible invalid Cache Element: " + "condition with id=" + conditionId + " "
                    + "and operator='" + operator.toString() + "' " + "passed a null value argument");
            } else {
                throw new InvalidCacheElementException("Invalid Cache Element: " + "condition with id=" + conditionId
                    + " " + "and operator='" + operator.toString() + "' " + "requires a non-null value");
            }
        }

        if ((operatorOption == null)
            && ((operator == AlertConditionOperator.CHANGES_TO) || (operator == AlertConditionOperator.CHANGES_FROM))) {
            throw new InvalidCacheElementException("operator '" + operator.toString() + "'"
                + " requires an operator option; it can not be null");
        }

        this.alertConditionOperatorOption = operatorOption;
        this.alertConditionOperator = operator;
        this.alertConditionValue = value;
        this.alertConditionTriggerId = conditionId;
    }

    public void setNextCacheElement(AbstractCacheElement<?> nextCacheElement) {
        this.nextCacheElement = nextCacheElement;
    }

    public AlertConditionOperator getAlertConditionOperator() {
        return alertConditionOperator;
    }

    public Object getAlertConditionOperatorOption() {
        return alertConditionOperatorOption;
    }

    public T getAlertConditionValue() {
        return alertConditionValue;
    }

    public void setAlertConditionValue(T updatedValue) {
        this.alertConditionValue = updatedValue;
    }

    public int getAlertConditionTriggerId() {
        return alertConditionTriggerId;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public final boolean process(T providedValue, Object... extraParams) {
        String beforeString = null;
        String afterString = null;

        if (log.isDebugEnabled()) {
            beforeString = this.toString();
        }
        boolean match = matches(providedValue, extraParams);
        if (log.isDebugEnabled()) {
            afterString = this.toString();
            log.debug("comparing " + ((providedValue == null) ? "<null>" : providedValue) + " " + "against "
                + beforeString + " " + (match ? "match" : ""));
            if (afterString.equals(beforeString) == false) {
                log.debug("element updated: " + afterString);
            }
        }

        return match;
    }

    /*
     * this is where the brunt of the comparison work occurs.  each subclass should override this method, which is
     * called by process(T, boolean) to determine whether or not the incoming value to the cache matches this cache
     * element or not.
     */
    public abstract boolean matches(T providedValue, Object... extras);

    /**
     * For the most part, the operator itself denotes whether it makes comparisons against a sliding scale or not.
     * However, this doesn't hold in every conceivable scenario. Thus, the defaultSupportsType will support 95% of the
     * use cases, and the AbstractCacheElement's getOperatorSupportsType will have a chance to override this.
     *
     * @see AlertConditionOperator
     */
    public abstract AlertConditionOperator.Type getOperatorSupportsType(AlertConditionOperator operator);

    /**
     * convenience method to test whether this cache element instance equals the specified type
     */
    public boolean isType(AlertConditionOperator.Type type) {
        return getOperatorSupportsType(alertConditionOperator) == type;
    }

    @Override
    public String toString() {
        String conditionValue = null;
        try {
            conditionValue = alertConditionValue.toString();
        } catch (Throwable t) {
            conditionValue = t.getClass().getSimpleName() + "(" + alertConditionValue.getClass().getSimpleName() + ")";
        }
        return getClass().getSimpleName() + "[ " + "alertConditionTriggerId=" + alertConditionTriggerId + ", "
            + "alertConditionOperator=" + alertConditionOperator
            + ((alertConditionOperatorOption != null) ? ("(" + alertConditionOperatorOption + ")") : "") + ", "
            + "alertConditionValue=" + conditionValue + " ]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((alertConditionOperator == null) ? 0 : alertConditionOperator.hashCode());
        result = (prime * result)
            + ((alertConditionOperatorOption == null) ? 0 : alertConditionOperatorOption.hashCode());
        result = (prime * result) + alertConditionTriggerId;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        final AbstractCacheElement<?> other = (AbstractCacheElement<?>) obj;
        if (alertConditionOperator == null) {
            if (other.alertConditionOperator != null) {
                return false;
            }
        } else if (!alertConditionOperator.equals(other.alertConditionOperator)) {
            return false;
        }

        if (alertConditionOperatorOption == null) {
            if (other.alertConditionOperatorOption != null) {
                return false;
            }
        } else if (!alertConditionOperatorOption.equals(other.alertConditionOperatorOption)) {
            return false;
        }

        if (alertConditionTriggerId != other.alertConditionTriggerId) {
            return false;
        }

        return true;
    }
}