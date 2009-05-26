 /*
  * Jopr Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
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
package com.jboss.jbossnetwork.product.jbpm.handlers;

/**
 * Bean stuffed into the execution context whose values are used in the variable substitutions.
 *
 * @author Jason Dobies
 */
public class SoftwareValue {
    private Integer id;
    private String referenceURL;
    private String title;
    private String issueReference;
    private Integer softwareTypeId;
    private String filename;
    private Long fileSize;
    private String mD5;
    private String sHA256;
    private String createdBy;
    private Long createdOn;
    private Long lastModified;
    private String shortDescription;
    private String longDescription;
    private String manualInstallation;
    private String automatedInstallation;
    private Integer distributionStatusTypeId;
    private String supportCaseReference;
    private String licenseName;
    private String licenseVersion;
    private String instructionCompatibilityVersion;
    private String downloadUrl;
    private String automatedDownloadUrl;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getReferenceURL() {
        return referenceURL;
    }

    public void setReferenceURL(String referenceURL) {
        this.referenceURL = referenceURL;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getIssueReference() {
        return issueReference;
    }

    public void setIssueReference(String issueReference) {
        this.issueReference = issueReference;
    }

    public Integer getSoftwareTypeId() {
        return softwareTypeId;
    }

    public void setSoftwareTypeId(Integer softwareTypeId) {
        this.softwareTypeId = softwareTypeId;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getMD5() {
        return mD5;
    }

    public void setMD5(String mD5) {
        this.mD5 = mD5;
    }

    public String getSHA256() {
        return sHA256;
    }

    public void setSHA256(String sHA256) {
        this.sHA256 = sHA256;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Long getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(Long createdOn) {
        this.createdOn = createdOn;
    }

    public Long getLastModified() {
        return lastModified;
    }

    public void setLastModified(Long lastModified) {
        this.lastModified = lastModified;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public String getLongDescription() {
        return longDescription;
    }

    public void setLongDescription(String longDescription) {
        this.longDescription = longDescription;
    }

    public String getManualInstallation() {
        return manualInstallation;
    }

    public void setManualInstallation(String manualInstallation) {
        this.manualInstallation = manualInstallation;
    }

    public String getAutomatedInstallation() {
        return automatedInstallation;
    }

    public void setAutomatedInstallation(String automatedInstallation) {
        this.automatedInstallation = automatedInstallation;
    }

    public Integer getDistributionStatusTypeId() {
        return distributionStatusTypeId;
    }

    public void setDistributionStatusTypeId(Integer distributionStatusTypeId) {
        this.distributionStatusTypeId = distributionStatusTypeId;
    }

    public String getSupportCaseReference() {
        return supportCaseReference;
    }

    public void setSupportCaseReference(String supportCaseReference) {
        this.supportCaseReference = supportCaseReference;
    }

    public String getLicenseName() {
        return licenseName;
    }

    public void setLicenseName(String licenseName) {
        this.licenseName = licenseName;
    }

    public String getLicenseVersion() {
        return licenseVersion;
    }

    public void setLicenseVersion(String licenseVersion) {
        this.licenseVersion = licenseVersion;
    }

    public String getInstructionCompatibilityVersion() {
        return instructionCompatibilityVersion;
    }

    public void setInstructionCompatibilityVersion(String instructionCompatibilityVersion) {
        this.instructionCompatibilityVersion = instructionCompatibilityVersion;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }
}