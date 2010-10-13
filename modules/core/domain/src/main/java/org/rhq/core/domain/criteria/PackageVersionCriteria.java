package org.rhq.core.domain.criteria;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.util.PageOrdering;

@XmlAccessorType(XmlAccessType.FIELD)
@SuppressWarnings("unused")
public class PackageVersionCriteria extends Criteria {
    private static final long serialVersionUID = 1L;

    private Integer filterId;
    private Integer filterPackageId; // requires override    
    private Integer filterRepoId; // requires overrides
    private Integer filterResourceId; // requires overrides
    private Integer filterPackageTypeId; // requires overrides
    private String filterDisplayName;
    private String filterVersion;
    private String filterFileName;
    private Long filterFileSizeMinimum; // requires overrides
    private Long filterFileSizeMaximum; // requires overrides
    private String filterLicenseName;
    private String filterLicenseVersion;

    private boolean fetchGeneralPackage;
    private boolean fetchArchitecture;
    private boolean fetchExtraProperties;
    private boolean fetchRepoPackageVersions;
    private boolean fetchInstalledPackages;
    private boolean fetchInstalledPackageHistory;
    private boolean fetchProductVersionPackageVersions;

    private PageOrdering sortDisplayName;

    public PackageVersionCriteria() {
        String alias = getAlias();
        filterOverrides.put("repoId", "id IN " //
            + "( SELECT cpv.packageVersion.id" //
            + "    FROM " + alias + ".repoPackageVersions cpv " //
            + "   WHERE cpv.repo.id = ? )");
        filterOverrides.put("fileSizeMinimum", "fileSize >= ?");
        filterOverrides.put("fileSizeMaximum", "fileSize <= ?");
        filterOverrides.put("packageId", "generalPackage.id = ? ");
        filterOverrides.put("packageTypeId", "generalPackage.packageType.id = ? ");
        filterOverrides.put("resourceId", "id IN " //
            + "( SELECT ip.packageVersion.id" //
            + "    FROM " + alias + ".installedPackages ip " //
            + "   WHERE ip.resource.id = ? )");
    }

    @Override
    public Class<PackageVersion> getPersistentClass() {
        return PackageVersion.class;
    }

    public void addFilterId(Integer filterId) {
        this.filterId = filterId;
    }

    public void addFilterRepoId(Integer filterRepoId) {
        this.filterRepoId = filterRepoId;
    }

    public Integer getFilterRepoId() {
        return filterRepoId;
    }

    public void addFilterResourceId(Integer filterResourceId) {
        this.filterResourceId = filterResourceId;
    }

    public Integer getFilterResourceId() {
        return filterResourceId;
    }

    public void addFilterPackageId(Integer filterPackageId) {
        this.filterPackageId = filterPackageId;
    }

    public void addFilterPackageTypeId(Integer filterPackageTypeId) {
        this.filterPackageTypeId = filterPackageTypeId;
    }

    public void addFilterDisplayName(String filterDisplayName) {
        this.filterDisplayName = filterDisplayName;
    }

    public void addFilterVersion(String filterVersion) {
        this.filterVersion = filterVersion;
    }

    public void addFilterFileName(String filterFileName) {
        this.filterFileName = filterFileName;
    }

    public void addFilterFileSizeMinimum(Long filterFileSizeMinimum) {
        this.filterFileSizeMinimum = filterFileSizeMinimum;
    }

    public void addFilterFileSizeMaximum(Long filterFileSizeMaximum) {
        this.filterFileSizeMaximum = filterFileSizeMaximum;
    }

    public void addFilterLicenseName(String filterLicenseName) {
        this.filterLicenseName = filterLicenseName;
    }

    public void addFilterLicenseVersion(String filterLicenseVersion) {
        this.filterLicenseVersion = filterLicenseVersion;
    }

    public void fetchGeneralPackage(boolean fetchGeneralPackage) {
        this.fetchGeneralPackage = fetchGeneralPackage;
    }

    public void fetchArchitecture(boolean fetchArchitecture) {
        this.fetchArchitecture = fetchArchitecture;
    }

    public void fetchExtraProperties(boolean fetchExtraProperties) {
        this.fetchExtraProperties = fetchExtraProperties;
    }

    public void fetchRepoPackageVersions(boolean fetchRepoPackageVersions) {
        this.fetchRepoPackageVersions = fetchRepoPackageVersions;
    }

    /**
     * Requires MANAGE_INVENTORY permission.
     * @param fetchInstalledPackages
     */
    public void fetchInstalledPackages(boolean fetchInstalledPackages) {
        this.fetchInstalledPackages = fetchInstalledPackages;
    }

    /**
     * Requires MANAGE_INVENTORY permission.
     * @param fetchInstalledPackages
     */
    public void fetchInstalledPackageHistory(boolean fetchInstalledPackageHistory) {
        this.fetchInstalledPackageHistory = fetchInstalledPackageHistory;
    }

    public void fetchProductVersionPackageVersions(boolean fetchProductVersionPackageVersions) {
        this.fetchProductVersionPackageVersions = fetchProductVersionPackageVersions;
    }

    public void addSortDisplayName(PageOrdering sortDisplayName) {
        addSortField("displayName");
        this.sortDisplayName = sortDisplayName;
    }

    /** subclasses should override as necessary */
    public boolean isInventoryManagerRequired() {
        return (this.fetchInstalledPackages || this.fetchInstalledPackageHistory);
    }

}
