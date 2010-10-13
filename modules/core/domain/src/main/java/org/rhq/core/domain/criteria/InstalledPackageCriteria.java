package org.rhq.core.domain.criteria;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import org.rhq.core.domain.content.InstalledPackage;
import org.rhq.core.domain.util.PageOrdering;

@XmlAccessorType(XmlAccessType.FIELD)
@SuppressWarnings("unused")
public class InstalledPackageCriteria extends Criteria {
    private static final long serialVersionUID = 1L;

    private Integer filterId;
    private Long filterInstallationTimeMinimum; // requires overrides
    private Long filterInstallationTimeMaximum; // requires overrides
    private Integer filterPackageVersionId; // requires overrides
    private Integer filterResourceId; // requires overrides
    private Integer filterUserId; // requires overrides

    private boolean fetchPackageVersion;
    private boolean fetchResource;
    private boolean fetchUser;

    private PageOrdering sortInstallationDate;

    public InstalledPackageCriteria() {
        filterOverrides.put("installationTimeMinimum", "installationDate >= ?");
        filterOverrides.put("installationTimeMaximum", "installationDate <= ?");
        filterOverrides.put("packageVersionId", "packageVersion.id = ? ");
        filterOverrides.put("resourceId", "resource.id = ? ");
        filterOverrides.put("userId", "user.id = ? ");
    }

    @Override
    public Class<InstalledPackage> getPersistentClass() {
        return InstalledPackage.class;
    }

    public void addFilterId(Integer filterId) {
        this.filterId = filterId;
    }

    public void addFilterPackageVersionId(Integer filterPackageVersionId) {
        this.filterPackageVersionId = filterPackageVersionId;
    }

    public void addFilterResourceId(Integer filterResourceId) {
        this.filterResourceId = filterResourceId;
    }

    public Integer getFilterResourceId() {
        return filterResourceId;
    }

    public void addFilterSubjectId(Integer filterSubjectId) {
        this.filterUserId = filterSubjectId;
    }

    public void addFilterInstallationTimeMinimum(Long filterInstallationTimeMinimum) {
        this.filterInstallationTimeMinimum = filterInstallationTimeMinimum;
    }

    public void addFilterInstallationTimeMaximum(Long filterInstallationTimeMaximum) {
        this.filterInstallationTimeMaximum = filterInstallationTimeMaximum;
    }

    public void fetchResource(boolean fetchResource) {
        this.fetchResource = fetchResource;
    }

    public void fetchPackageVersion(boolean fetchPackageVersion) {
        this.fetchPackageVersion = fetchPackageVersion;
    }

    public void fetchSubject(boolean fetchSubject) {
        this.fetchUser = fetchSubject;
    }

    public void addSortInstallationTime(PageOrdering sortInstallationDate) {
        addSortField("installationDate");
        this.sortInstallationDate = sortInstallationDate;
    }

}
