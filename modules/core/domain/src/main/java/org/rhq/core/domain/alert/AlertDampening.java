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
package org.rhq.core.domain.alert;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;

@Embeddable
public class AlertDampening implements java.io.Serializable {
    public enum Category {
        NONE, // Each time condition set is true  
        CONSECUTIVE_COUNT, // If condition set is true X times consecutively
        PARTIAL_COUNT, // If condition set is true X times out of the last Y times it is evaluated
        INVERSE_COUNT, // If condition set is true, but ignoring it the next X times
        DURATION_COUNT, // Once every <count> times conditions are exceeded within a time period of <range> minutes...

        /*
         * These should perhaps be filters, not categories
         */
        NO_DUPLICATES, // Disregard control actions that are defined for related alerts...
        ONCE; // Disable alert until re-enabled manually or by recovery alert...
    }

    public enum TimeUnits {
        MINUTES(60), HOURS(MINUTES.getNumberOfSeconds() * 60), DAYS(HOURS.getNumberOfSeconds() * 24), WEEKS(DAYS
            .getNumberOfSeconds() * 7);

        private long numberOfSeconds;

        private TimeUnits(long numberOfSeconds) {
            this.numberOfSeconds = numberOfSeconds;
        }

        public long getNumberOfSeconds() {
            return numberOfSeconds;
        }
    }

    private static final long serialVersionUID = 1L;

    @Column(name = "DAMPENING_CATEGORY", nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private Category category;

    @Column(name = "DAMPENING_VALUE")
    private int value;

    @Column(name = "DAMPENING_VALUE_UNITS")
    @Enumerated(EnumType.ORDINAL)
    private TimeUnits valueUnits;

    @Column(name = "DAMPENING_PERIOD")
    private int period;

    @Column(name = "DAMPENING_PERIOD_UNITS")
    @Enumerated(EnumType.ORDINAL)
    private TimeUnits periodUnits;

    //@OneToMany(cascade = CascadeType.ALL) <-- used to be unidirectional, but that obfuscated AlertDampeningEvent queries
    @OneToMany(mappedBy = "alertDefinition", cascade = CascadeType.ALL)
    @org.hibernate.annotations.Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    @JoinColumn(name = "ID")
    private Set<AlertDampeningEvent> alertDampeningEvents = new HashSet<AlertDampeningEvent>();

    protected AlertDampening() {
    } // JPA

    public AlertDampening(Category category) {
        this.category = category;
    }

    public AlertDampening(AlertDampening alertDampening) {
        update(alertDampening);
    }

    public void update(AlertDampening alertDampening) {
        clearAlertDampeningEvents(); // for hibernate's all-delete-orphan
        this.category = alertDampening.category;
        this.value = alertDampening.value;
        this.valueUnits = alertDampening.valueUnits;
        this.period = alertDampening.period;
        this.periodUnits = alertDampening.periodUnits;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public TimeUnits getValueUnits() {
        return valueUnits;
    }

    public void setValueUnits(TimeUnits valueUnits) {
        this.valueUnits = valueUnits;
    }

    public int getPeriod() {
        return period;
    }

    public void setPeriod(int period) {
        this.period = period;
    }

    public TimeUnits getPeriodUnits() {
        return periodUnits;
    }

    public void setPeriodUnits(TimeUnits periodUnits) {
        this.periodUnits = periodUnits;
    }

    public Set<AlertDampeningEvent> getAlertDampeningEvents() {
        return alertDampeningEvents;
    }

    public void addAlertDampeningEvent(AlertDampeningEvent alertDampeningEvent) {
        getAlertDampeningEvents().add(alertDampeningEvent);
    }

    public void setAlertDampeningEvents(Set<AlertDampeningEvent> alertDampeningEvents) {
        this.alertDampeningEvents = alertDampeningEvents;
    }

    public void clearAlertDampeningEvents() {
        this.alertDampeningEvents.clear();
    }

    @Override
    public String toString() {
        return "org.rhq.core.domain.alert.AlertDampening" + "[ " + "category=" + category + "[" + toString(category)
            + "]" + " ]";
    }

    private String toString(Category category) {
        if (category == Category.NONE) {
            return "";
        } else if (category == Category.PARTIAL_COUNT) {
            return "count=" + value + ", " + "collections=" + period;
        } else if (category == Category.DURATION_COUNT) {
            return "count=" + value + ", " + "period=" + period + " " + periodUnits;
        } else if (category == Category.CONSECUTIVE_COUNT) {
            return "count=" + value;
        } else {
            return "<unknown details>";
        }
    }

}
