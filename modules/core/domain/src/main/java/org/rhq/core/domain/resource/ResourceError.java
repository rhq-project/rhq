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
package org.rhq.core.domain.resource;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents some error that has occurred in or is associated with a {@link Resource}.
 *
 * @author John Mazzitelli
 * @author Ian Springer
 */
@Entity
@NamedQueries( {
    @NamedQuery(name = ResourceError.QUERY_DELETE_BY_RESOURCES, query = "DELETE From ResourceError re WHERE re.resource IN (:resources)"),
    @NamedQuery(name = ResourceError.QUERY_FIND_BY_RESOURCE_ID, query = "SELECT re FROM ResourceError re WHERE re.resource.id = :resourceId"),
    @NamedQuery(name = ResourceError.QUERY_FIND_BY_RESOURCE_ID_AND_ERROR_TYPE, query = "SELECT re FROM ResourceError re WHERE re.resource.id = :resourceId AND re.errorType = :errorType") })
@SequenceGenerator(name = "RHQ_RESOURCE_ERROR_SEQ", sequenceName = "RHQ_RESOURCE_ERROR_ID_SEQ")
@Table(name = "RHQ_RESOURCE_ERROR")
public class ResourceError implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String QUERY_DELETE_BY_RESOURCES = "ResourceError.deleteByResources";
    public static final String QUERY_FIND_BY_RESOURCE_ID = "ResourceError.findByResource";
    public static final String QUERY_FIND_BY_RESOURCE_ID_AND_ERROR_TYPE = "ResourceError.findByResourceAndErrorType";

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "RHQ_RESOURCE_ERROR_SEQ")
    @Id
    private int id;

    @Column(name = "TIME_OCCURRED", nullable = false)
    private long timeOccurred;

    @JoinColumn(name = "RESOURCE_ID", nullable = false)
    @ManyToOne
    private Resource resource;

    @Column(name = "ERROR_TYPE", nullable = false)
    @Enumerated(EnumType.STRING)
    private ResourceErrorType errorType;

    @Column(name = "SUMMARY", nullable = false, length = 1000)
    private String summary;

    @Column(name = "DETAIL", nullable = true)
    private String detail;

    protected ResourceError() {
    }

    /**
     * Constructor for {@link ResourceError}.
     *
     * @param resource     the resource that is associated with the error that occurred
     * @param errorType    identifies this kind of error this represents
     * @param summary      a summary of the error
     * @param detail       a detailed description of the error - typically a stack trace; may be null
     * @param timeOccurred the epoch time when the error occurred
     */
    public ResourceError(@NotNull
    Resource resource, @NotNull
    ResourceErrorType errorType, @NotNull
    String summary, @Nullable
    String detail, long timeOccurred) {
        setResource(resource);
        setErrorType(errorType);
        setSummary(summary);
        setDetail(detail);
        setTimeOccurred(timeOccurred);
    }

    /**
     * Constructor for {@link ResourceError} that uses {@link #setDetailFromThrowable(Throwable)} to convert the given
     * exception to an error message.
     *
     * @param resource     the resource that is associated with the error that occurred
     * @param errorType    identifies this kind of error this represents
     * @param exception    exception whose stack will be used as this object's error message
     * @param timeOccurred the epoch time when the error occurred
     */
    public ResourceError(@NotNull
    Resource resource, @NotNull
    ResourceErrorType errorType, @NotNull
    Throwable exception, long timeOccurred) {
        setResource(resource);
        setErrorType(errorType);
        setSummary(exception.getLocalizedMessage());
        setDetailFromThrowable(exception);
        setTimeOccurred(timeOccurred);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public ResourceErrorType getErrorType() {
        return errorType;
    }

    public void setErrorType(ResourceErrorType errorType) {
        this.errorType = errorType;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    @Nullable
    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    /**
     * Convenience method that sets the error message to the given throwable's stack trace.
     *
     * @param  t throwable whose message and stack trace will make up the error message (must not be <code>null</code>)
     *
     * @throws NullPointerException if <code>t</code> is <code>null</code>
     */
    public void setDetailFromThrowable(@NotNull
    Throwable t) {
        //noinspection ConstantConditions
        if (t == null) {
            throw new IllegalArgumentException("t == null");
        }

        StringWriter stringWriter = new StringWriter();
        t.printStackTrace(new PrintWriter(stringWriter));
        String stackTrace = stringWriter.toString();
        setDetail(stackTrace);
    }

    public long getTimeOccurred() {
        return timeOccurred;
    }

    public void setTimeOccurred(long timeOccurred) {
        this.timeOccurred = timeOccurred;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("ResourceError: ");
        str.append("id=[").append(id);
        str.append("], time-occurred=[").append(new Date(timeOccurred));
        str.append("], error-type=[").append(errorType);
        str.append("], resource=[").append(resource);
        str.append("], summary=[").append(summary);
        str.append("], detail=[").append(detail);
        str.append("]");
        return str.toString();
    }
}