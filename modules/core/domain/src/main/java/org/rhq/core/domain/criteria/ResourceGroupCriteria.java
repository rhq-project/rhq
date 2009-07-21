package org.rhq.core.domain.criteria;

import java.util.List;

import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.util.PageOrdering;

public class ResourceGroupCriteria extends Criteria {
    private Integer filterId;
    private String filterName;
    private Boolean filterRecursive;
    private Integer filterResourceTypeId; // requires overrides
    private String filterResourceTypeName; // requires overrides
    private String filterPluginName; // requires overrides
    private GroupCategory filterGroupCategory;
    private List<Integer> filterExplicitResourceIds; // requires overrides
    private List<Integer> filterImplicitResourceIds; // requires overrides

    private boolean fetchExplicitResources;
    private boolean fetchImplicitResources;
    private boolean fetchOperationHistories;
    private boolean fetchConfigurationUpdates;
    private boolean fetchGroupDefinition;
    private boolean fetchResourceType;

    private PageOrdering sortName;
    private PageOrdering sortResourceTypeName; // requires overrides

    public ResourceGroupCriteria() {
        super();

        filterOverrides.put("resourceTypeId", "resourceType.id = ?");
        filterOverrides.put("resourceTypeName", "resourceType.name like ?");
        filterOverrides.put("pluginName", "resourceType.plugin like ?");
        filterOverrides.put("explicitResourceIds", "" //
            + "id IN ( SELECT explicitGroup.id " //
            + "          FROM Resource res " //
            + "          JOIN res.explicitGroups explicitGroup " //
            + "         WHERE res.id IN ( ? ) )");
        filterOverrides.put("implicitResourceIds", "" //
            + "id IN ( SELECT implicitGroup.id " //
            + "          FROM Resource res " //
            + "          JOIN res.implicitGroups implicitGroup " //
            + "         WHERE res.id IN ( ? ) )");

        sortOverrides.put("resourceTypeName", "resourceType.name");
    }

    public void addFilterId(Integer filterId) {
        this.filterId = filterId;
    }

    public void addFilterName(String filterName) {
        this.filterName = filterName;
    }

    public void addFilterRecursive(Boolean filterRecursive) {
        this.filterRecursive = filterRecursive;
    }

    public void addFilterResourceTypeId(Integer filterResourceTypeId) {
        this.filterResourceTypeId = filterResourceTypeId;
    }

    public void addFilterResourceTypeName(String filterResourceTypeName) {
        this.filterResourceTypeName = filterResourceTypeName;
    }

    public void addFilterPluginName(String filterPluginName) {
        this.filterPluginName = filterPluginName;
    }

    public void addFilterGroupCategory(GroupCategory filterGroupCategory) {
        this.filterGroupCategory = filterGroupCategory;
    }

    public void addFilterExplicitResourceIds(List<Integer> filterExplicitResourceIds) {
        this.filterExplicitResourceIds = filterExplicitResourceIds;
    }

    public void addFilterImplicitResourceIds(List<Integer> filterImplicitResourceIds) {
        this.filterImplicitResourceIds = filterImplicitResourceIds;
    }

    public void fetchExplicitResources(boolean fetchExplicitResources) {
        this.fetchExplicitResources = fetchExplicitResources;
    }

    public void fetchImplicitResources(boolean fetchImplicitResources) {
        this.fetchImplicitResources = fetchImplicitResources;
    }

    public void fetchOperationHistories(boolean fetchOperationHistories) {
        this.fetchOperationHistories = fetchOperationHistories;
    }

    public void fetchConfigurationUpdates(boolean fetchConfigurationUpdates) {
        this.fetchConfigurationUpdates = fetchConfigurationUpdates;
    }

    public void fetchGroupDefinition(boolean fetchGroupDefinition) {
        this.fetchGroupDefinition = fetchGroupDefinition;
    }

    public void fetchResourceType(boolean fetchResourceType) {
        this.fetchResourceType = fetchResourceType;
    }

    public void addSortName(PageOrdering sortName) {
        addSortField("name");
        this.sortName = sortName;
    }

    public void addSortResourceTypeName(PageOrdering sortResourceTypeName) {
        addSortField("resourceTypeName");
        this.sortResourceTypeName = sortResourceTypeName;
    }

}
