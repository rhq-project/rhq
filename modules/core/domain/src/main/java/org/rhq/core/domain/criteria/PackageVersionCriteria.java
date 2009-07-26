package org.rhq.core.domain.criteria;

import org.rhq.core.domain.util.PageOrdering;

public class PackageVersionCriteria extends Criteria {
    private Integer channelId;
    private String filterDisplayName;
    private String filterVersion;
    private String filterFileName;
    private Long filterFileSizeMinimum; // requires overrides
    private Long filterFileSizeMaximum; // requires overrides
    private String filterLicenseName;
    private String filterLicenseVersion;

    private boolean fetchPackage;
    private boolean fetchArchitecture;
    private boolean fetchExtraProperties;
    private boolean fetchChannelPackageVersions;
    private boolean fetchInstalledPackages;
    private boolean fetchInstalledPackageHistory;
    private boolean fetchProductVersionPackageVersions;

    private PageOrdering sortDisplayName;

    public PackageVersionCriteria() {
        super();

        filterOverrides.put("fileSizeMinimum", "fileSize >= ?");
        filterOverrides.put("fileSizeMaximum", "fileSize <= ?");
    }

    public void addChannelId(Integer channelId) {
        this.channelId = channelId;
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

    public void fetchPackage(boolean fetchPackage) {
        this.fetchPackage = fetchPackage;
    }

    public void fetchArchitecture(boolean fetchArchitecture) {
        this.fetchArchitecture = fetchArchitecture;
    }

    public void fetchExtraProperties(boolean fetchExtraProperties) {
        this.fetchExtraProperties = fetchExtraProperties;
    }

    public void fetchChannelPackageVersions(boolean fetchChannelPackageVersions) {
        this.fetchChannelPackageVersions = fetchChannelPackageVersions;
    }

    public void fetchInstalledPackages(boolean fetchInstalledPackages) {
        this.fetchInstalledPackages = fetchInstalledPackages;
    }

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
}
