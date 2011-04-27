package org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.operation.schedule;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.DSCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.RadioGroupItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.grid.HoverCustomizer;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.components.form.EnhancedDynamicForm;
import org.rhq.enterprise.gui.coregui.client.components.sorter.ReorderableList;
import org.rhq.enterprise.gui.coregui.client.inventory.common.detail.operation.schedule.AbstractOperationScheduleDetailsView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.AncestryUtil;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDatasource;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * The details view of the Group Operations>Schedules subtab.
 *
 * @author Ian Springer
 */
public class GroupOperationScheduleDetailsView extends AbstractOperationScheduleDetailsView {

    private static final String FIELD_EXECUTION_MODE = "executionMode";

    private static final String EXECUTION_ORDER_PARALLEL = "parallel";
    private static final String EXECUTION_ORDER_SEQUENTIAL = "sequential";

    private ResourceGroupComposite groupComposite;
    private ListGridRecord[] memberResourceRecords;
    private EnhancedDynamicForm executionModeForm;
    private ReorderableList memberExecutionOrderer;

    public GroupOperationScheduleDetailsView(String locatorId, ResourceGroupComposite groupComposite, int scheduleId) {
        super(locatorId, new GroupOperationScheduleDataSource(groupComposite), groupComposite.getResourceGroup()
            .getResourceType(), scheduleId);
        this.groupComposite = groupComposite;
    }

    @Override
    protected boolean hasControlPermission() {
        return this.groupComposite.getResourcePermission().isControl();
    }

    @Override
    protected void init(final boolean isReadOnly) {
        if (isNewRecord()) {
            ResourceDatasource resourceDatasource = new ResourceDatasource();
            Criteria criteria = new Criteria(ResourceDatasource.FILTER_GROUP_ID, String.valueOf(this.groupComposite
                .getResourceGroup().getId()));
            resourceDatasource.fetchData(criteria, new DSCallback() {
                public void execute(DSResponse response, Object rawData, DSRequest request) {
                    if (response.getStatus() != DSResponse.STATUS_SUCCESS) {
                        throw new RuntimeException(MSG.view_group_operationScheduleDetails_failedToLoadMembers());
                    }
                    Record[] data = response.getData();
                    memberResourceRecords = new ListGridRecord[data.length];
                    for (int i = 0, dataLength = data.length; i < dataLength; i++) {
                        Record record = data[i];
                        ListGridRecord listGridRecord = (ListGridRecord) record;
                        memberResourceRecords[i] = listGridRecord;
                    }
                    GroupOperationScheduleDetailsView.super.init(isReadOnly);
                }
            });
        } else {
            super.init(isReadOnly);
        }
    }

    @Override
    protected LocatableVLayout buildContentPane() {
        LocatableVLayout contentPane = super.buildContentPane();

        HTMLFlow hr = new HTMLFlow("<p/><hr/><p/>");
        contentPane.addMember(hr);

        this.executionModeForm = new EnhancedDynamicForm(extendLocatorId("ExecutionModeForm"), isReadOnly());
        this.executionModeForm.setNumCols(2);
        this.executionModeForm.setColWidths(FIRST_COLUMN_WIDTH, "*");

        RadioGroupItem executionModeItem = new RadioGroupItem(FIELD_EXECUTION_MODE, MSG
            .view_group_operationScheduleDetails_field_execute());
        LinkedHashMap<String, String> valueMap = new LinkedHashMap<String, String>(2);
        valueMap.put(EXECUTION_ORDER_PARALLEL, MSG.view_group_operationScheduleDetails_value_parallel());
        valueMap.put(EXECUTION_ORDER_SEQUENTIAL, MSG.view_group_operationScheduleDetails_value_sequential());
        executionModeItem.setValueMap(valueMap);
        executionModeItem.setDefaultValue(EXECUTION_ORDER_PARALLEL);
        executionModeItem.setShowTitle(true);

        final CheckboxItem haltOnFailureItem = new CheckboxItem(GroupOperationScheduleDataSource.Field.HALT_ON_FAILURE,
            MSG.view_group_operationScheduleDetails_field_haltOnFailure());
        haltOnFailureItem.setDefaultValue(false);
        haltOnFailureItem.setVisible(false);
        haltOnFailureItem.setLabelAsTitle(true);
        haltOnFailureItem.setShowTitle(true);

        this.executionModeForm.setFields(executionModeItem, haltOnFailureItem);

        contentPane.addMember(this.executionModeForm);

        HLayout hLayout = new HLayout();
        VLayout horizontalSpacer = new VLayout();
        horizontalSpacer.setWidth(140);
        hLayout.addMember(horizontalSpacer);
        ResourceCategory resourceCategory = this.groupComposite.getResourceGroup().getResourceType().getCategory();
        String memberIcon = ImageManager.getResourceIcon(resourceCategory);
        HoverCustomizer nameHoverCustomizer = new HoverCustomizer() {
            public String hoverHTML(Object value, ListGridRecord listGridRecord, int rowNum, int colNum) {
                return AncestryUtil.getAncestryHoverHTML(listGridRecord, 0);
            }
        };
        this.memberExecutionOrderer = new ReorderableList(extendLocatorId("MemberExecutionOrderer"),
            this.memberResourceRecords, null, memberIcon, nameHoverCustomizer);
        this.memberExecutionOrderer.setVisible(false);
        this.memberExecutionOrderer.setNameFieldTitle(MSG.view_group_operationScheduleDetails_memberResource());
        hLayout.addMember(this.memberExecutionOrderer);
        contentPane.addMember(hLayout);

        executionModeItem.addChangedHandler(new ChangedHandler() {
            public void onChanged(ChangedEvent event) {
                if (event.getValue().equals(EXECUTION_ORDER_PARALLEL)) {
                    haltOnFailureItem.hide();
                    memberExecutionOrderer.hide();
                } else {
                    haltOnFailureItem.show();
                    memberExecutionOrderer.show();
                }
            }
        });

        return contentPane;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void editExistingRecord(final Record record) {
        List<Resource> executionOrder = (List<Resource>) record
            .getAttributeAsObject(GroupOperationScheduleDataSource.Field.EXECUTION_ORDER);

        if (executionOrder != null) {
            Integer[] resourceIds = new Integer[executionOrder.size()];
            int i = 0;
            for (Resource resource : executionOrder) {
                resourceIds[i++] = resource.getId();
            }
            ResourceDatasource resourceDatasource = new ResourceDatasource();
            Criteria criteria = new Criteria();
            criteria.addCriteria(ResourceDatasource.FILTER_RESOURCE_IDS, resourceIds);
            resourceDatasource.fetchData(criteria, new DSCallback() {
                public void execute(DSResponse response, Object rawData, DSRequest request) {
                    if (response.getStatus() != DSResponse.STATUS_SUCCESS) {
                        throw new RuntimeException(MSG.view_group_operationScheduleDetails_failedToLoadMembers());
                    }
                    Record[] data = response.getData();
                    ListGridRecord[] resourceRecords = new ListGridRecord[data.length];
                    for (int i = 0, dataLength = data.length; i < dataLength; i++) {
                        Record record = data[i];
                        ListGridRecord listGridRecord = (ListGridRecord) record;
                        resourceRecords[i] = listGridRecord;
                    }

                    executionModeForm.setValue(FIELD_EXECUTION_MODE, EXECUTION_ORDER_SEQUENTIAL);
                    memberExecutionOrderer.setRecords(resourceRecords);
                    memberExecutionOrderer.show();

                    FormItem haltOnFailureItem = executionModeForm
                        .getField(GroupOperationScheduleDataSource.Field.HALT_ON_FAILURE);
                    Object haltOnFailure = getForm().getValue(GroupOperationScheduleDataSource.Field.HALT_ON_FAILURE);
                    haltOnFailureItem.setValue(haltOnFailure);
                    haltOnFailureItem.show();

                    GroupOperationScheduleDetailsView.super.editExistingRecord(record);
                }
            });
        } else {
            this.executionModeForm.setValue(FIELD_EXECUTION_MODE, EXECUTION_ORDER_PARALLEL);

            Object haltOnFailure = getForm().getValue(GroupOperationScheduleDataSource.Field.HALT_ON_FAILURE);
            FormItem haltOnFailureItem = this.executionModeForm
                .getField(GroupOperationScheduleDataSource.Field.HALT_ON_FAILURE);
            haltOnFailureItem.setValue(haltOnFailure);

            super.editExistingRecord(record);
        }

    }

    @Override
    protected void save(DSRequest requestProperties) {
        String executionMode = this.executionModeForm.getValueAsString(FIELD_EXECUTION_MODE);
        List<Resource> executionOrder;
        if (executionMode.equals(EXECUTION_ORDER_SEQUENTIAL)) {
            ListGridRecord[] resourceRecords = this.memberExecutionOrderer.getRecords();
            ResourceDatasource resourceDatasource = new ResourceDatasource();
            Set<Resource> resources = resourceDatasource.buildDataObjects(resourceRecords);
            executionOrder = new ArrayList<Resource>(resources);
        } else {
            executionOrder = null;
        }
        requestProperties
            .setAttribute(GroupOperationScheduleDataSource.RequestProperty.EXECUTION_ORDER, executionOrder);

        Boolean haltOnFailure = (Boolean) this.executionModeForm
            .getValue(GroupOperationScheduleDataSource.Field.HALT_ON_FAILURE);
        getForm().setValue(GroupOperationScheduleDataSource.Field.HALT_ON_FAILURE, haltOnFailure);

        super.save(requestProperties);
    }

}
