package org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.operation.history;

import java.util.ArrayList;
import java.util.List;

import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.HoverCustomizer;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.inventory.common.detail.operation.history.AbstractOperationHistoryDataSource;
import org.rhq.enterprise.gui.coregui.client.inventory.common.detail.operation.history.AbstractOperationHistoryListView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.AncestryUtil;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.operation.history.ResourceOperationHistoryDataSource;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.operation.history.ResourceOperationHistoryDetailsView;
import org.rhq.enterprise.gui.coregui.client.util.selenium.SeleniumUtility;

/**
 * @author Ian Springer
 */
public class GroupMemberResourceOperationHistoryListView extends
    AbstractOperationHistoryListView<ResourceOperationHistoryDataSource> {

    private ResourceGroupComposite groupComposite;

    public GroupMemberResourceOperationHistoryListView(String locatorId, ResourceGroupComposite groupComposite,
        int groupOperationHistoryId) {
        super(locatorId, new ResourceOperationHistoryDataSource(), null, new Criteria(
            ResourceOperationHistoryDataSource.CriteriaField.GROUP_OPERATION_HISTORY_ID, String
                .valueOf(groupOperationHistoryId)));
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
                String url = LinkManager.getResourceLink(listGridRecord.getAttributeAsInt("id"));
                return SeleniumUtility.getLocatableHref(url, o.toString(), null);
            }
        });
        resourceField.setShowHover(true);
        resourceField.setHoverCustomizer(new HoverCustomizer() {
            public String hoverHTML(Object value, ListGridRecord listGridRecord, int rowNum, int colNum) {
                return AncestryUtil.getResourceHoverHTML(listGridRecord, 0);
            }
        });
        fields.add(resourceField);

        ListGridField ancestryField = new ListGridField(AncestryUtil.RESOURCE_ANCESTRY, MSG.common_title_ancestry());
        ancestryField.setWidth("35%");
        ancestryField.setCellFormatter(new CellFormatter() {
            public String format(Object o, ListGridRecord listGridRecord, int rowNum, int colNum) {
                return listGridRecord.getAttributeAsString(AncestryUtil.RESOURCE_ANCESTRY_VALUE);
            }
        });
        ancestryField.setShowHover(true);
        ancestryField.setHoverCustomizer(new HoverCustomizer() {

            public String hoverHTML(Object value, ListGridRecord listGridRecord, int rowNum, int colNum) {
                return AncestryUtil.getAncestryHoverHTML(listGridRecord, 0);
            }
        });
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
    public Canvas getDetailsView(int id) {
        return new ResourceOperationHistoryDetailsView(extendLocatorId("DetailsView"), true);
    }

}
