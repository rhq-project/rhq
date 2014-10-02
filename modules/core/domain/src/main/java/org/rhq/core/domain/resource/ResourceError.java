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
package org.rhq.core.domain.resource;

import java.io.Serializable;
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
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlTransient;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents some error that has occurred in or is associated with a {@link Resource}.
 *
 * @author John Mazzitelli
 * @author Ian Springer
 */
@Entity
@NamedQueries({
    @NamedQuery(name = ResourceError.QUERY_DELETE_BY_RESOURCES, query = "DELETE From ResourceError re WHERE re.resource.id IN ( :resourceIds )"),
    @NamedQuery(name = ResourceError.QUERY_FIND_BY_RESOURCE_ID, query = "SELECT re FROM ResourceError re WHERE re.resource.id = :resourceId"),
    @NamedQuery(name = ResourceError.QUERY_FIND_BY_RESOURCE_ID_AND_ERROR_TYPE, query = "SELECT re FROM ResourceError re WHERE re.resource.id = :resourceId AND re.errorType = :errorType"),
    @NamedQuery(name = ResourceError.QUERY_FIND_ID_BY_RESOURCE_ID_AND_ERROR_TYPE, query = "" //
        + " SELECT " //
        + "   re.id " //
        + " FROM ResourceError re " //
        + " WHERE " //
        + "   re.resource.id = :resourceId " //
        + "   AND re.errorType = :type " //
        + " ORDER BY re.id ASC"), //
    @NamedQuery(name = ResourceError.QUERY_FIND_ID_BY_RESOURCE_ID_AND_ERROR_TYPE_OLDER_THAN, query = "" //
        + " SELECT " //
        + "   re.id " //
        + " FROM ResourceError re " //
        + " WHERE " //
        + "   re.resource.id = :resourceId " //
        + "   AND re.errorType = :type " //
        + "   AND re.timeOccurred < :upToTime " //
        + " ORDER BY re.id ASC"), //
    @NamedQuery(name = ResourceError.QUERY_FIND_ALL_INVALID_RESOURCE_ERROR_TYPE_COMPOSITE, query = "" //
        + " SELECT " //
        + "   new org.rhq.core.domain.resource.ResourceErrorTypeComposite( " //
        + "     re.resource.id, re.errorType, count(*), max(re.timeOccurred) " //
        + "   ) " //
        + " FROM ResourceError re " //
        + " WHERE " //
        + "   re.errorType IN ( :types ) " //
        + " GROUP BY " //
        + "   re.resource.id, re.errorType " //
        + " HAVING count(*) > 1 "), //
    @NamedQuery(name = ResourceError.QUERY_DELETE_BY_ID, query = "DELETE FROM ResourceError re WHERE re.id = :id") //
})
@SequenceGenerator(allocationSize = org.rhq.core.domain.util.Constants.ALLOCATION_SIZE, name = "RHQ_RESOURCE_ERROR_ID_SEQ", sequenceName = "RHQ_RESOURCE_ERROR_ID_SEQ")
@Table(name = "RHQ_RESOURCE_ERROR")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class ResourceError implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String QUERY_DELETE_BY_RESOURCES = "ResourceError.deleteByResources";
    public static final String QUERY_FIND_BY_RESOURCE_ID = "ResourceError.findByResource";
    public static final String QUERY_FIND_BY_RESOURCE_ID_AND_ERROR_TYPE = "ResourceError.findByResourceAndErrorType";
    public static final String QUERY_FIND_ID_BY_RESOURCE_ID_AND_ERROR_TYPE = "ResourceError.findIdByResourceAndErrorType";
    public static final String QUERY_FIND_ID_BY_RESOURCE_ID_AND_ERROR_TYPE_OLDER_THAN = "ResourceError.findIdByResourceAndErrorTypeOlderThan";
    public static final String QUERY_FIND_ALL_INVALID_RESOURCE_ERROR_TYPE_COMPOSITE = "ResourceError.findAllInvalidResourceErrorTypeComposite";
    public static final String QUERY_DELETE_BY_ID = "ResourceError.deleteById";

    private static final int MAX_SUMMARY_LENGTH = 1000;

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "RHQ_RESOURCE_ERROR_ID_SEQ")
    @Id
    private int id;

    @Column(name = "TIME_OCCURRED", nullable = false)
    private long timeOccurred;

    @JoinColumn(name = "RESOURCE_ID", nullable = false)
    @ManyToOne
    @XmlTransient
    private Resource resource;

    @Column(name = "ERROR_TYPE", nullable = false)
    @Enumerated(EnumType.STRING)
    private ResourceErrorType errorType;

    @Column(name = "SUMMARY", nullable = false, length = ResourceError.MAX_SUMMARY_LENGTH)
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
    public ResourceError(@NotNull Resource resource, @NotNull ResourceErrorType errorType, @NotNull String summary,
        @Nullable String detail, long timeOccurred) {
        setResource(resource);
        setErrorType(errorType);
        setSummary(summary);
        setDetail(detail);
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
        if (summary == null) {
            summary = "An error occurred.";
        } else if (summary.length() > MAX_SUMMARY_LENGTH) {
            summary = summary.substring(0, MAX_SUMMARY_LENGTH - 3) + "...";
        }
        this.summary = summary;
    }

    @Nullable
    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public long getTimeOccurred() {
        return timeOccurred;
    }

    public void setTimeOccurred(long timeOccurred) {
        this.timeOccurred = timeOccurred;
    }

    @Override
    public String toString() {
        return "ResourceError: " + "id=[" + id + "], time-occurred=[" + new Date(timeOccurred) + "], error-type=["
            + errorType + "], resource=[" + resource + "], summary=[" + summary + "], detail=[" + detail + "]";
    }
}
