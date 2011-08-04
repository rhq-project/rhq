package org.rhq.core.domain.criteria;

import java.util.Arrays;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import org.rhq.core.domain.content.InstalledPackageHistory;
import org.rhq.core.domain.util.PageOrdering;

@XmlAccessorType(XmlAccessType.FIELD)
@SuppressWarnings("unused")
public class InstalledPackageHistoryCriteria extends Criteria {
    private static final long serialVersionUID = 1L;

    private Integer filterId;
    private Long filterInstallationTimeMinimum; // requires overrides
    private Long filterInstallationTimeMaximum; // requires overrides
    private Integer filterPackageVersionId; // requires overrides
    private Integer filterResourceId; // requires overrides
    private List<Integer> filterResourceGroupIds; // requires overrides
    private Integer filterUserId; // requires overrides

    private boolean fetchPackageVersion;
    private boolean fetchResource;
    private boolean fetchUser;

    private PageOrdering sortInstallationDate;
    private PageOrdering sortStatus;

    public InstalledPackageHistoryCriteria() {
        super();
        filterOverrides.put("installationTimeMinimum", "installationDate >= ?");
        filterOverrides.put("installationTimeMaximum", "installationDate <= ?");
        filterOverrides.put("packageVersionId", "packageVersion.id = ? ");
        filterOverrides.put("resourceId", "resource.id = ? ");
        filterOverrides.put("groupId", "resource.explicitGroups.id = ? ");
        filterOverrides.put("resourceGroupIds", "resource.id IN " //
            + "( SELECT res.id " //
            + "    FROM ResourceGroup rg " //
            + "    JOIN rg.explicitResources res " //
            + "   WHERE rg.id = ? )");
        filterOverrides.put("userId", "user.id = ? ");
    }

    @Override
    public Class<InstalledPackageHistory> getPersistentClass() {
        return InstalledPackageHistory.class;
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

    public void addFilterResourceGroupIds(Integer... filterResourceGroupIds) {
        this.filterResourceGroupIds = Arrays.asList(filterResourceGroupIds);
    }

    public Integer getFilterResourceId() {
        return filterResourceId;
    }

    public List<Integer> getFilterGroupIds() {
        return filterResourceGroupIds;
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

    public void addSortStatus(PageOrdering sortStatus) {
        addSortField("sort");
        this.sortStatus = sortStatus;
    }

}
