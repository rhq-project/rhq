package org.rhq.core.domain.criteria;

import org.rhq.core.domain.util.PageOrdering;

@SuppressWarnings("unused")
public class PackageVersionCriteria extends Criteria {
    private static final long serialVersionUID = 1L;

    private Integer filterChannelId; // requires overrides
    private Integer filterResourceId; // requires overrides
    private Integer filterPackageTypeId; // requires overrides
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

        filterOverrides.put("channelId", "id IN " //
            + "( SELECT cpv.packageVersion.id" //
            + "    FROM channelPackageVersions cpv " //
            + "   WHERE cpv.channel.id = ? )");
        filterOverrides.put("fileSizeMinimum", "fileSize >= ?");
        filterOverrides.put("fileSizeMaximum", "fileSize <= ?");
        filterOverrides.put("packageTypeId", "generalPackage.packageType.id = ? ");
        filterOverrides.put("resourceId", "id IN " //
            + "( SELECT ip.packageVersion.id" //
            + "    FROM installedPackages ip " //
            + "   WHERE ip.resource.id = ? )");
    }

    public void addFilterChannelId(Integer filterChannelId) {
        this.filterChannelId = filterChannelId;
    }

    public Integer getFilterChannelId() {
        return filterChannelId;
    }

    public void addFilterResourceId(Integer filterResourceId) {
        this.filterResourceId = filterResourceId;
    }

    public Integer getFilterResourceId() {
        return filterResourceId;
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
