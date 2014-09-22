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

package org.rhq.core.domain.bundle;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
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
import javax.xml.bind.annotation.XmlType;

/**
 * This is a many-to-one entity that provides audit capability for a bundle deployment (a bundle-platform pairing).
 * 
 * @author Jay Shaughnessy
 */
@Entity
@NamedQueries({
    @NamedQuery(name = BundleResourceDeploymentHistory.QUERY_DELETE_BY_RESOURCES, query = "DELETE FROM BundleResourceDeploymentHistory brdh "
        + " WHERE brdh.resourceDeployment IN ( SELECT brd FROM BundleResourceDeployment brd WHERE brd.resource.id IN ( :resourceIds ) ) )"),
    @NamedQuery(name = BundleResourceDeploymentHistory.QUERY_FIND_ALL, query = "SELECT brdh FROM BundleResourceDeploymentHistory brdh") //
})
@SequenceGenerator(allocationSize = org.rhq.core.domain.util.Constants.ALLOCATION_SIZE, name = "RHQ_BUNDLE_RES_DEP_HIST_ID_SEQ", sequenceName = "RHQ_BUNDLE_RES_DEP_HIST_ID_SEQ")
@Table(name = "RHQ_BUNDLE_RES_DEP_HIST")
@XmlAccessorType(XmlAccessType.FIELD)
public class BundleResourceDeploymentHistory implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String QUERY_DELETE_BY_RESOURCES = "BundleResourceDeploymentHistory.deleteByResources";
    public static final String QUERY_FIND_ALL = "BundleResourceDeploymentHistory.findAll";

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "RHQ_BUNDLE_RES_DEP_HIST_ID_SEQ")
    @Id
    private int id;

    @JoinColumn(name = "BUNDLE_RES_DEPLOY_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private BundleResourceDeployment resourceDeployment;

    @Column(name = "SUBJECT_NAME", nullable = true)
    protected String subjectName;

    @Column(name = "AUDIT_TIME", nullable = false)
    private Long auditTime = System.currentTimeMillis();

    @Column(name = "ACTION", nullable = false)
    private String action;

    @Column(name = "INFO", nullable = false)
    private String info;

    @Column(name = "CATEGORY", nullable = true)
    @Enumerated(EnumType.STRING)
    private Category category;

    @Column(name = "STATUS", nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "MESSAGE", nullable = true)
    private String message;

    @Column(name = "ATTACHMENT", nullable = true)
    private String attachment;

    // required for JPA
    protected BundleResourceDeploymentHistory() {
    }

    public BundleResourceDeploymentHistory(String subjectName, String action, String info, Category category,
        Status status, String message, String attachment) {

        this.subjectName = subjectName;
        this.action = action;
        this.info = info;
        this.category = category;
        this.status = status;
        this.message = message;
        this.attachment = attachment;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public Long getAuditTime() {
        return this.auditTime;
    }

    public void setAuditTime(Long auditTime) {
        this.auditTime = auditTime;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getAttachment() {
        return attachment;
    }

    public void setAttachment(String attachment) {
        this.attachment = attachment;
    }

    public BundleResourceDeployment getResourceDeployment() {
        return resourceDeployment;
    }

    public void setResourceDeployment(BundleResourceDeployment resourceDeployment) {
        this.resourceDeployment = resourceDeployment;
    }

    @Override
    public String toString() {
        return "BundleDeploymentAudit: " + ", time=[" + this.auditTime + "]" + ", rd=[" + this.resourceDeployment + "]"
            + ", action=[" + this.action + "]" + ", info=[" + this.info + "]" + ", category=[" + this.category + "]"
            + ", status=[" + this.status + "]";
    }

    @XmlType(name = "BundleResourceDeploymentHistoryStatus")
    public enum Status {
        SUCCESS("Success"), //
        FAILURE("Failure"), //
        WARN("Warning"), //
        INFO("Information"); // used mainly for informational audit messages

        private String displayName;

        Status(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    @XmlType(name = "BundleResourceDeploymentHistoryCategory")
    public enum Category {
        DEPLOY_STEP("Deploy Step"), //        
        FILE_ADD("File Add"), //
        FILE_CHANGE("File Change"), //
        FILE_DOWNLOAD("File Download"), //        
        FILE_REMOVE("File Remove"), //
        AUDIT_MESSAGE("Audit Message");

        private String displayName;

        Category(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

}
