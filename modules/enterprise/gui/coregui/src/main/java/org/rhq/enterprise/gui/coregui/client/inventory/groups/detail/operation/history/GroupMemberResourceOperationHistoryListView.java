package org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.operation.history;

import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.grid.ListGridField;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.enterprise.gui.coregui.client.inventory.common.detail.operation.history.AbstractOperationHistoryDataSource;
import org.rhq.enterprise.gui.coregui.client.inventory.common.detail.operation.history.AbstractOperationHistoryListView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.operation.history.ResourceOperationHistoryDataSource;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.operation.history.ResourceOperationHistoryDetailsView;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Ian Springer
 */
public class GroupMemberResourceOperationHistoryListView
        extends AbstractOperationHistoryListView<ResourceOperationHistoryDataSource> {

    private ResourceGroupComposite groupComposite;

    public GroupMemberResourceOperationHistoryListView(String locatorId, ResourceGroupComposite groupComposite,
                                                       int groupOperationHistoryId) {
        super(locatorId, new ResourceOperationHistoryDataSource(), null,
            new Criteria(ResourceOperationHistoryDataSource.CriteriaField.GROUP_OPERATION_HISTORY_ID,
            String.valueOf(groupOperationHistoryId)));
        this.groupComposite = groupComposite;
    }

    @Override
    protected List<ListGridField> createFields() {
        List<ListGridField> fields = new ArrayList<ListGridField>();

        ListGridField idField = new ListGridField(AbstractOperationHistoryDataSource.Field.ID);
        idField.setWidth(38);
        fields.add(idField);

        ListGridField resourceField = createResourceField();
        resourceField.setWidth("70%");
        fields.add(resourceField);

        ListGridField statusField = createStatusField();
        fields.add(statusField);

        ListGridField startedTimeField = createStartedTimeField();
        startedTimeField.setWidth("30%");
        fields.add(startedTimeField);

        return fields;
    }

    @Override
    protected boolean hasControlPermission() {
        return groupComposite.getResourcePermission().isControl();
    }

    @Override
    public Canvas getDetailsView(int id) {
        return new ResourceOperationHistoryDetailsView(extendLocatorId("DetailsView"), true);
    }

}
