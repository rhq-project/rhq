/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
package org.rhq.core.domain.storage;

import java.io.Serializable;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.PrePersist;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.rhq.core.domain.configuration.Configuration;

/**
 * A Storage Maintenance Job
 *
 * @author Stefan Negrea
 */
@Entity(name = "MaintenanceStep")
@NamedQueries( //
{
    @NamedQuery(name = MaintenanceStep.FIND_BY_JOB_NUM, query =
        "SELECT s FROM MaintenanceStep s LEFT JOIN FETCH s.configuration WHERE s.jobNumber = :jobNumber ORDER BY s.stepNumber"),
    @NamedQuery(name = MaintenanceStep.FIND_ALL, query =
        "SELECT s FROM MaintenanceStep s ORDER BY s.jobNumber, s.stepNumber"),
    @NamedQuery(name = MaintenanceStep.FIND_STEP_AND_CONFIG, query =
        "SELECT s FROM MaintenanceStep s LEFT JOIN FETCH s.configuration WHERE s.id = :stepId"),
    @NamedQuery(name = MaintenanceStep.DELETE_STEP, query =
        "DELETE FROM MaintenanceStep s WHERE s.id = :id"),
    @NamedQuery(name = MaintenanceStep.FIND_BASE_STEPS_BY_JOB_TYPE, query =
        "SELECT s FROM MaintenanceStep s JOIN FETCH s.configuration WHERE s.jobType = :jobType AND s.stepNumber = 0")
})
@SequenceGenerator(allocationSize = org.rhq.core.domain.util.Constants.ALLOCATION_SIZE,
    name = "RHQ_STORAGE_MAINT_STEP_ID_SEQ", sequenceName = "RHQ_STORAGE_MAINT_STEP_ID_SEQ")
@Table(name = "RHQ_STORAGE_MAINT_STEP")
public class MaintenanceStep implements Serializable {

    public static final long serialVersionUID = 1L;

    public static final String FIND_BY_JOB_NUM = "MaintenanceStep.findJob";

    public static final String FIND_ALL = "MaintenanceStep.findAll";

    public static final String FIND_STEP_AND_CONFIG = "MaintenanceStep.findStepAndConfig";

    public static final String DELETE_STEP = "MaintenanceStep.deleteStep";

    public static final String FIND_BASE_STEPS_BY_JOB_TYPE = "MaintenanceStep.findBaseStepsByJobType";

    public static enum JobType {
        DEPLOY,
        UNDEPLOY,
        CHANGE_ENDPOINT,
        CHANGE_RHQ_DATA_DIR,
        FAILED_ANNOUNCE,
        FAILED_REPAIR
    }

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "RHQ_STORAGE_MAINT_STEP_ID_SEQ")
    @Id
    private int id;

    @Column(name = "JOB_NUM", nullable = false)
    private int jobNumber;

    @Column(name = "STEP_NUM", nullable = false)
    private int stepNumber;

    @Column(name = "JOB_TYPE", nullable = false)
    private JobType jobType;

    @Column(name = "DESCRIPTION")
    private String description;

    // I think this should simply be the name of the class that executes the
    // step. The server can then easily create the object to execute the step.
    @Column(name = "NAME", nullable = false)
    private String name;

    @JoinColumn(name = "STEP_CONFIG_ID", referencedColumnName = "ID", nullable = true)
    @OneToOne(cascade = { CascadeType.ALL }, fetch = FetchType.LAZY, optional = true)
    private Configuration configuration;

    // the time this maintenance workflow was created
    @Column(name = "CTIME", nullable = false)
    private long ctime;

    // the time this maintenance workflow was last updated
    @Column(name = "MTIME", nullable = false)
    private long mtime;

    // required for JPA
    public MaintenanceStep() {
    }

    public int getId() {
        return id;
    }

    public MaintenanceStep setId(int id) {
        this.id = id;
        return this;
    }

    public int getJobNumber() {
        return jobNumber;
    }

    public MaintenanceStep setJobNumber(int jobNumber) {
        this.jobNumber = jobNumber;
        return this;
    }

    public int getStepNumber() {
        return stepNumber;
    }

    public MaintenanceStep setStepNumber(int step) {
        this.stepNumber = step;
        return this;
    }

    public JobType getJobType() {
        return jobType;
    }

    public MaintenanceStep setJobType(JobType jobType) {
        this.jobType = jobType;
        return this;
    }

    public String getName() {
        return name;
    }

    public MaintenanceStep setName(String name) {
        this.name = name;
        return this;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public MaintenanceStep setConfiguration(Configuration configuration) {
        this.configuration = configuration;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public MaintenanceStep setDescription(String description) {
        this.description = description;
        return this;
    }

    public boolean isBaseStep() {
        return stepNumber == 0;
    }

    public long getCtime() {
        return ctime;
    }

    public long getMtime() {
        return mtime;
    }

    public MaintenanceStep setMtime(long mtime) {
        this.mtime = mtime;
        return this;
    }

    @Override
    public String toString() {
        return toString(false);
    }

    public String toString(boolean verbose) {
        return "MaintenanceStep[id = " + id + ", jobNumber = " + jobNumber + ", jobType = " + jobType +
            ", stepNumber = " + stepNumber + ", name = " + name + ", ctime = " + ctime + ", mtime = " + mtime +
            ", configuration = " + (configuration == null ? "null" : configuration.toString(verbose)) + "]";
    }

    @PrePersist
    void onPersist() {
        this.ctime = System.currentTimeMillis();
        this.mtime = this.ctime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MaintenanceStep)) return false;

        MaintenanceStep that = (MaintenanceStep) o;

        if (jobNumber != that.jobNumber) return false;
        if (stepNumber != that.stepNumber) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = jobNumber;
        result = 31 * result + stepNumber;
        return result;
    }

}
