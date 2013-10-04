package org.rhq.coregui.client.inventory.groups.detail.operation.history;

import java.util.ArrayList;
import java.util.List;

import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.HoverCustomizer;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.LinkManager;
import org.rhq.coregui.client.inventory.common.detail.operation.history.AbstractOperationHistoryDataSource;
import org.rhq.coregui.client.inventory.common.detail.operation.history.AbstractOperationHistoryListView;
import org.rhq.coregui.client.inventory.resource.AncestryUtil;
import org.rhq.coregui.client.inventory.resource.detail.operation.history.ResourceOperationHistoryDataSource;
import org.rhq.coregui.client.inventory.resource.detail.operation.history.ResourceOperationHistoryDetailsView;

/**
 * @author Ian Springer
 */
public class GroupMemberResourceOperationHistoryListView extends
    AbstractOperationHistoryListView<ResourceOperationHistoryDataSource> {

    private ResourceGroupComposite groupComposite;

    public GroupMemberResourceOperationHistoryListView(ResourceGroupComposite groupComposite,
        int groupOperationHistoryId) {
        super(new ResourceOperationHistoryDataSource(), null, new Criteria(
            ResourceOperationHistoryDataSource.CriteriaField.GROUP_OPERATION_HISTORY_ID,
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
        resourceField.setWidth("25%");
        resourceField.setCellFormatter(new CellFormatter() {
            public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                String url = LinkManager.getResourceLink(listGridRecord.getAttributeAsInt(AncestryUtil.RESOURCE_ID));
                return LinkManager.getHref(url, o.toString());
            }
        });
        resourceField.setShowHover(true);
        resourceField.setHoverCustomizer(new HoverCustomizer() {
            public String hoverHTML(Object value, ListGridRecord listGridRecord, int rowNum, int colNum) {
                return AncestryUtil.getResourceHoverHTML(listGridRecord, 0);
            }
        });
        fields.add(resourceField);

        ListGridField ancestryField = AncestryUtil.setupAncestryListGridField();
        ancestryField.setWidth("35%");
        fields.add(ancestryField);

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
    public Canvas getDetailsView(Integer id) {
        return new ResourceOperationHistoryDetailsView(true);
    }

    @Override
    public void showDetails(ListGridRecord record) {
        if (record == null) {
            throw new IllegalArgumentException("'record' parameter is null.");
        }

        int resourceId = record.getAttributeAsInt(AncestryUtil.RESOURCE_ID);
        int opHistoryId = record.getAttributeAsInt(ResourceOperationHistoryDataSource.Field.ID);

        if (resourceId > 0 && opHistoryId > 0) {
            CoreGUI.goToView(LinkManager.getSubsystemResourceOperationHistoryLink(resourceId, opHistoryId));
        } else {
            String msg = MSG.view_tableSection_error_badId(this.getClass().toString(), Integer.toString(resourceId)
                + "," + Integer.toString(opHistoryId));
            CoreGUI.getErrorHandler().handleError(msg);
            throw new IllegalArgumentException(msg);
        }
    }
}
