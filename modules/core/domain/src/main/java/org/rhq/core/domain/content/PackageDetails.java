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
package org.rhq.core.domain.content;

import java.io.Serializable;
import java.util.Date;
import org.rhq.core.domain.configuration.Configuration;

/**
 * Content source plugin representation of a package. Instances of this class are accessible to the content source to
 * describe the server's last known set of packages for the content source.
 *
 * <p>This is also the same object used in the plugin container to deal with details of a specific package.</p>
 *
 * @author Jason Dobies
 */
public class PackageDetails implements Serializable {
    // Constants  --------------------------------------------

    private static final long serialVersionUID = 1L;

    // Attributes  --------------------------------------------

    /**
     * Contains the data necessary to describe a specific version of a package.
     */
    private PackageDetailsKey key;

    /**
     * Display version of the package. The format of this attribute will vary based on the package type.
     */
    private String displayVersion;

    /**
     * Provides a second means for indicating a package's type. The possible values for this attribute will vary based
     * on package type. For instance, two packages of the same package type may be differentiated as a "bug fix" or a
     * "security update". Another example would be to indicate a grouping means, such as "System Environment" or
     * "Applications/Editors".
     */
    private String classification;

    /**
     * Name of this package version that is suitable for display to the user in the UI.
     */
    private String displayName;

    /**
     * Short free text description of this version of the package. In other words, a summary of the package.
     */
    private String shortDescription;

    /**
     * Long free text description of this version of the package.
     */
    private String longDescription;

    /**
     * Name of the file with the contents of this package.
     */
    private String fileName;

    /**
     * Size of the package's file.
     */
    private Long fileSize;

    /**
     * MD5 hash of the file.
     */
    private String MD5;

    /**
     * SHA256 hash of the file.
     */
    private String SHA265;

    /**
     * Timestamp on when the package's file was created.
     */
    private Date fileCreatedDate;

    /**
     * Name of the license under which the package falls.
     */
    private String licenseName;

    /**
     * Version of the package's license.
     */
    private String licenseVersion;

    /**
     * The package location
     */
    private String location;

    /**
     * XML Metadata
     */
    private byte[] metadata;

    /**
     * Values to fulfill the package type specific extra properties. Values in this object will adhere to the
     * configuration definition in the associated package type
     * {@link org.rhq.core.domain.content.PackageType#packageExtraPropertiesDefinition}.
     */
    private Configuration extraProperties;

    // Constructors  --------------------------------------------

    /**
     * Creates a new <code>PackageDetails</code> object that describes the package version identified in the specified
     * key.
     *
     * @param key identifies the package; cannot be <code>null</code>
     */
    public PackageDetails(PackageDetailsKey key) {
        if (key == null) {
            throw new IllegalArgumentException("key cannot be null");
        }

        this.key = key;
    }

    // Public  --------------------------------------------

    public PackageDetailsKey getKey() {
        return key;
    }

    public String getName() {
        return key.getName();
    }

    public String getVersion() {
        return key.getVersion();
    }

    public String getArchitectureName() {
        return key.getArchitectureName();
    }

    public String getPackageTypeName() {
        return key.getPackageTypeName();
    }

    public String getDisplayVersion() {
        return displayVersion;
    }

    public void setDisplayVersion(String displayVersion) {
        this.displayVersion = displayVersion;
    }

    public String getClassification() {
        return classification;
    }

    public void setClassification(String classification) {
        this.classification = classification;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
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

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getMD5() {
        return MD5;
    }

    public void setMD5(String MD5) {
        this.MD5 = MD5;
    }

    public String getSHA265() {
        return SHA265;
    }

    public void setSHA265(String SHA265) {
        this.SHA265 = SHA265;
    }

    public Date getFileCreatedDate() {
        return fileCreatedDate;
    }

    public void setFileCreatedDate(Date fileCreatedDate) {
        this.fileCreatedDate = fileCreatedDate;
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

    public byte[] getMetadata() {
        return metadata;
    }

    public void setMetadata(byte[] metadata) {
        this.metadata = metadata;
    }

    public Configuration getExtraProperties() {
        return extraProperties;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getLocation() {
        return location;
    }

    public void setExtraProperties(Configuration properties) {
        this.extraProperties = properties;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if ((o == null) || (!(o instanceof PackageDetails))) {
            return false;
        }

        PackageDetails that = (PackageDetails) o;

        if (!key.equals(that.key)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public String toString() {
        return "PackageDetails[Key=" + key + "]";
    }
}