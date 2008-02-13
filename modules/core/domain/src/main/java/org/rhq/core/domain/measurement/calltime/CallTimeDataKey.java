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
package org.rhq.core.domain.measurement.calltime;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rhq.core.domain.measurement.MeasurementSchedule;

/**
 * A key used to lookup all call-time data ((i.e. a set of {@link CallTimeDataValue}s)) for a particular destination
 * (e.g. a URL or an EJB method name).
 *
 * @author Ian Springer
 */
@Entity
@SequenceGenerator(name = "idGenerator", sequenceName = "RHQ_CALLTIME_DATA_KEY_ID_SEQ")
@Table(name = "RHQ_CALLTIME_DATA_KEY")
public class CallTimeDataKey implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final int DESTINATION_MAX_LENGTH = 4000;

    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "idGenerator")
    @Id
    private int id;

    @JoinColumn(name = "SCHEDULE_ID", insertable = false, updatable = false, nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private MeasurementSchedule schedule;

    @Column(name = "CALL_DESTINATION", length = DESTINATION_MAX_LENGTH, nullable = false)
    private String callDestination;

    /**
     * Create a new <code>CallTimeDataKey</code>.
     *
     * @param schedule        the collection schedule corresponding to this call-time data
     * @param callDestination the call destination (e.g. a URL or an EJB name)
     */
    public CallTimeDataKey(@NotNull
    MeasurementSchedule schedule, @NotNull
    String callDestination) {
        this.schedule = schedule;
        this.callDestination = callDestination;
    }

    protected CallTimeDataKey() {
        /* for JPA use only */
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Nullable
    public MeasurementSchedule getSchedule() {
        return this.schedule;
    }

    @NotNull
    public String getCallDestination() {
        return this.callDestination;
    }

    @Override
    public String toString() {
        return "MeasurementCallDestination[" + "id=" + this.id + ", " + "value=" + this.callDestination + "]";
    }
}