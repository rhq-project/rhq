/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.enterprise.gui.coregui.client.components.table;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.DSCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.SortSpecifier;
import com.smartgwt.client.types.Autofit;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.events.DoubleClickEvent;
import com.smartgwt.client.widgets.events.DoubleClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.form.fields.events.KeyPressEvent;
import com.smartgwt.client.widgets.form.fields.events.KeyPressHandler;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.events.DataArrivedEvent;
import com.smartgwt.client.widgets.grid.events.DataArrivedHandler;
import com.smartgwt.client.widgets.grid.events.SelectionChangedHandler;
import com.smartgwt.client.widgets.grid.events.SelectionEvent;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.LayoutSpacer;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.menu.IMenuButton;
import com.smartgwt.client.widgets.menu.MenuItem;
import com.smartgwt.client.widgets.menu.events.MenuItemClickEvent;
import com.smartgwt.client.widgets.toolbar.ToolStrip;

import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.RefreshableView;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableIButton;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableIMenuButton;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableListGrid;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableMenu;

/**
 * A tabular view of set of data records from an {@link RPCDataSource}.
 *
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class Table<DS extends RPCDataSource> extends LocatableHLayout implements RefreshableView {

    private static final String FIELD_ID = "id";
    private static final String FIELD_NAME = "name";

    private VLayout contents;

    private HTMLFlow title;

    private HLayout titleLayout;
    private Canvas titleComponent;

    private TableFilter filterForm;
    private ListGrid listGrid;
    private Label tableInfo;

    private List<String> headerIcons = new ArrayList<String>();

    private boolean showHeader = true;
    private boolean showFooter = true;
    private boolean showFooterRefresh = true;
    private boolean showFilterForm = true;

    private String tableTitle;
    private Criteria criteria;
    private SortSpecifier[] sortSpecifiers;
    private String[] excludedFieldNames;
    private boolean autoFetchData;
    private boolean flexRowDisplay = true;

    private DS dataSource;

    private DoubleClickHandler doubleClickHandler;
    private List<TableActionInfo> tableActions = new ArrayList<TableActionInfo>();
    private boolean tableActionDisableOverride = false;
    protected List<Canvas> extraWidgets = new ArrayList<Canvas>();
    private ToolStrip footer;

    public Table(String locatorId) {
        this(locatorId, null, null, null, null, true);
    }

    public Table(String locatorId, String tableTitle) {
        this(locatorId, tableTitle, null, null, null, true);
    }

    public Table(String locatorId, String tableTitle, Criteria criteria) {
        this(locatorId, tableTitle, criteria, null, null, true);
    }

    public Table(String locatorId, String tableTitle, SortSpecifier[] sortSpecifiers) {
        this(locatorId, tableTitle, null, sortSpecifiers, null, true);
    }

    public Table(String locatorId, String tableTitle, boolean autoFetchData) {
        this(locatorId, tableTitle, null, null, null, autoFetchData);
    }

    public Table(String locatorId, String tableTitle, SortSpecifier[] sortSpecifiers, String[] excludedFieldNames) {
        this(locatorId, tableTitle, null, sortSpecifiers, excludedFieldNames, true);
    }

    public Table(String locatorId, String tableTitle, Criteria criteria, SortSpecifier[] sortSpecifiers,
        String[] excludedFieldNames) {
        this(locatorId, tableTitle, criteria, sortSpecifiers, excludedFieldNames, true);
    }

    public Table(String locatorId, String tableTitle, Criteria criteria, SortSpecifier[] sortSpecifiers,
        String[] excludedFieldNames, boolean autoFetchData) {
        super(locatorId);

        setWidth100();
        setHeight100();
        setOverflow(Overflow.HIDDEN);

        this.tableTitle = tableTitle;
        this.criteria = criteria;
        this.sortSpecifiers = sortSpecifiers;
        this.excludedFieldNames = excludedFieldNames;
        this.autoFetchData = autoFetchData;
    }

    public void setFlexRowDisplay(boolean flexRowDisplay) {
        this.flexRowDisplay = flexRowDisplay;
    }

    @Override
    protected void onInit() {
        super.onInit();

        filterForm = new TableFilter(this);
        configureTableFilters();

        listGrid = new LocatableListGrid(getLocatorId());
        listGrid.setAutoFetchData(autoFetchData);

        if (criteria != null) {
            listGrid.setInitialCriteria(criteria);
        }
        if (sortSpecifiers != null) {
            listGrid.setInitialSort(sortSpecifiers);
        }
        listGrid.setWidth100();
        listGrid.setHeight100();
        listGrid.setAlternateRecordStyles(true);
        listGrid.setResizeFieldsInRealTime(false);
        listGrid.setSelectionType(getDefaultSelectionStyle());

        if (flexRowDisplay) {
            listGrid.setAutoFitData(Autofit.HORIZONTAL);
            listGrid.setWrapCells(true);
            listGrid.setFixedRecordHeights(false);
        }

        // By default, SmartGWT will disable any rows that have a record named "enabled" with a value of false - setting
        // these fields to a bogus field name will disable this behavior. Note, setting them to null does *not* disable
        // the behavior.
        listGrid.setRecordEnabledProperty("foobar");
        listGrid.setRecordEditProperty("foobar");

        // TODO: Uncomment the below line once we've upgraded to SmartGWT 2.3.
        //listGrid.setRecordCanSelectProperty("foobar");

        if (dataSource != null) {
            listGrid.setDataSource(dataSource);
        }

        contents = new VLayout();
        contents.setWidth100();
        contents.setHeight100();
        addMember(contents);

        contents.addMember(listGrid);
    }

    protected SelectionStyle getDefaultSelectionStyle() {
        return SelectionStyle.MULTIPLE;
    }

    @Override
    protected void onDraw() {
        try {
            super.onDraw();

            for (Canvas child : contents.getMembers()) {
                contents.removeChild(child);
            }

            // Title
            this.title = new HTMLFlow();
            setTableTitle(this.tableTitle);

            if (showHeader) {
                titleLayout = new HLayout();
                titleLayout.setAutoHeight();
                titleLayout.setAlign(VerticalAlignment.BOTTOM);
                contents.addMember(titleLayout, 0);
            }

            if (filterForm.hasContent()) {
                contents.addMember(filterForm);
            }

            contents.addMember(listGrid);

            // Footer
            this.footer = new ToolStrip();
            footer.setPadding(5);
            footer.setWidth100();
            footer.setMembersMargin(15);
            contents.addMember(footer);

            // The ListGrid has been created and configured
            // Now give subclasses a chance to configure the table
            configureTable();

            listGrid.addDoubleClickHandler(new DoubleClickHandler() {
                @Override
                public void onDoubleClick(DoubleClickEvent event) {
                    if (doubleClickHandler != null && !getTableActionDisableOverride()) {
                        doubleClickHandler.onDoubleClick(event);
                    }
                }
            });

            setTableInfo(new Label("Total: " + listGrid.getTotalRows()));

            // NOTE: It is essential that we wait to hide any excluded fields until after super.onDraw() is called, since
            //       super.onDraw() is what actually adds the fields to the ListGrid (based on what fields are defined in
            //       the underlying datasource).
            if (this.excludedFieldNames != null) {
                for (String excludedFieldName : excludedFieldNames) {
                    this.listGrid.hideField(excludedFieldName);
                }
            }

            getTableInfo().setWrap(false);

            if (showHeader) {
                drawHeader();
            }

            if (showFooter) {
                drawFooter();
            }
        } catch (Exception e) {
            CoreGUI.getErrorHandler().handleError(MSG.view_table_drawFail(this.toString()), e);
        }
    }

    private void drawHeader() {
        for (String headerIcon : headerIcons) {
            Img img = new Img(headerIcon, 24, 24);
            img.setPadding(4);
            titleLayout.addMember(img);
        }

        titleLayout.addMember(title);

        if (titleComponent != null) {
            titleLayout.addMember(new LayoutSpacer());
            titleLayout.addMember(titleComponent);
        }
    }

    private void drawFooter() {
        footer.removeMembers(footer.getMembers());

        for (final TableActionInfo tableAction : tableActions) {

            if (null == tableAction.getValueMap()) {
                // button action
                IButton button = new LocatableIButton(tableAction.getLocatorId(), tableAction.getTitle());
                button.setDisabled(true);
                button.setOverflow(Overflow.VISIBLE);
                button.addClickHandler(new ClickHandler() {
                    public void onClick(ClickEvent clickEvent) {
                        if (tableAction.confirmMessage != null) {

                            String message = tableAction.confirmMessage.replaceAll("\\#", String.valueOf(listGrid
                                .getSelection().length));

                            SC.ask(message, new BooleanCallback() {
                                public void execute(Boolean confirmed) {
                                    if (confirmed) {
                                        tableAction.action.executeAction(listGrid.getSelection(), null);
                                    }
                                }
                            });
                        } else {
                            tableAction.action.executeAction(listGrid.getSelection(), null);
                        }
                    }
                });

                tableAction.actionCanvas = button;
                footer.addMember(button);

            } else {
                // menu action
                LocatableMenu menu = new LocatableMenu(tableAction.getLocatorId() + "Menu");
                final Map<String, ? extends Object> menuEntries = tableAction.getValueMap();
                for (final String key : menuEntries.keySet()) {
                    MenuItem item = new MenuItem(key);
                    item.addClickHandler(new com.smartgwt.client.widgets.menu.events.ClickHandler() {

                        @Override
                        public void onClick(MenuItemClickEvent event) {
                            tableAction.getAction().executeAction(listGrid.getSelection(), menuEntries.get(key));
                        }
                    });
                    menu.addItem(item);
                }

                IMenuButton menuButton = new LocatableIMenuButton(tableAction.getLocatorId(), tableAction.getTitle(),
                    menu);
                menuButton.setDisabled(true);
                // this makes it pretty tight, but maybe better than the default, which is pretty wide
                menuButton.setAutoFit(true);
                menuButton.setOverflow(Overflow.VISIBLE);

                tableAction.actionCanvas = menuButton;
                footer.addMember(menuButton);
            }
        }

        for (Canvas extraWidgetCanvas : extraWidgets) {
            footer.addMember(extraWidgetCanvas);
        }

        footer.addMember(new LayoutSpacer());

        if (isShowFooterRefresh()) {
            IButton refreshButton = new LocatableIButton(extendLocatorId("Refresh"), MSG.common_button_refresh());
            refreshButton.addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent clickEvent) {
                    listGrid.invalidateCache();
                }
            });
            footer.addMember(refreshButton);
        }

        footer.addMember(tableInfo);

        // Manages enable/disable buttons for the grid
        listGrid.addSelectionChangedHandler(new SelectionChangedHandler() {
            public void onSelectionChanged(SelectionEvent selectionEvent) {
                refreshTableInfo();
            }
        });

        listGrid.addDataArrivedHandler(new DataArrivedHandler() {
            public void onDataArrived(DataArrivedEvent dataArrivedEvent) {
                refreshTableInfo();
                fieldSizes.clear();
            }
        });

        // Ensure buttons are initially set correctly.
        refreshTableInfo();
    }

    /**
     * Subclasses can use this as a chance to configure the list grid after it has been
     * created but before it has been drawn to the DOM. This is also the proper place to add table
     * actions so that they're rendered in the footer.
     */
    protected void configureTable() {
        return;
    }

    public void setFilterFormItems(FormItem... formItems) {
        this.filterForm.setItems(formItems);
    }

    /**
     * Overriding components can use this as a chance to add {@link FormItem}s which will filter
     * the table that displays their data.
     */
    protected void configureTableFilters() {

    }

    public String getTitle() {
        return this.tableTitle;
    }

    public void setTitle(String title) {
        this.tableTitle = title;
        if (this.title != null) {
            setTableTitle(title);
        }
    }

    /**
     * Returns the encompassing canvas that contains all content for this table component.
     * This content includes the list grid, the buttons, etc.
     */
    public Canvas getTableContents() {
        return this.contents;
    }

    public boolean isShowHeader() {
        return showHeader;
    }

    public void setShowHeader(boolean showHeader) {
        this.showHeader = showHeader;
    }

    public boolean isShowFooter() {
        return showFooter;
    }

    public void setShowFooter(boolean showFooter) {
        this.showFooter = showFooter;
    }

    private ArrayList<Integer> fieldSizes = new ArrayList<Integer>();

    public void refresh(Criteria criteria) {
        if (null != this.listGrid) {
            this.listGrid.invalidateCache();
            this.listGrid.setCriteria(criteria);
            this.listGrid.markForRedraw();
        }
    }

    public void refresh() {
        if (null != this.listGrid) {
            this.listGrid.invalidateCache();
            this.listGrid.markForRedraw();
        }
    }

    public void setTableTitle(String titleString) {
        if (titleString == null) {
            titleString = "";
        }
        if (titleString.length() > 0) {
            title.setWidth100();
            title.setHeight(35);
            title.setContents(titleString);
            title.setPadding(4);
            title.setStyleName("HeaderLabel");
        } else {
            title.setWidth100();
            title.setHeight(0);
            title.setContents(null);
            title.setPadding(0);
            title.setStyleName("normal");
        }

        title.markForRedraw();
    }

    public DS getDataSource() {
        return dataSource;
    }

    public void setDataSource(DS dataSource) {
        this.dataSource = dataSource;
    }

    public ListGrid getListGrid() {
        return listGrid;
    }

    /**
     * Wraps ListGrid.setFields(...) but takes care of "id" field display handling. Equivalent to calling:
     * <pre>
     * setFields( false, fields );
     * </pre>
     * 
     * @param fields the fields
     */
    public void setListGridFields(ListGridField... fields) {
        setListGridFields(false, fields);
    }

    /**
     * Wraps ListGrid.setFields(...) but takes care of "id" field display handling.
     *
     * @param forceIdField if true, and "id" is a defined field, then display it. If false, it is displayed
     *        only in debug mode.  
     * @param fields the fields
     */
    public void setListGridFields(boolean forceIdField, ListGridField... fields) {
        String[] dataSourceFieldNames = this.dataSource.getFieldNames();
        Set<String> dataSourceFieldNamesSet = new LinkedHashSet<String>();
        dataSourceFieldNamesSet.addAll(Arrays.asList(dataSourceFieldNames));
        Map<String, ListGridField> listGridFieldsMap = new LinkedHashMap<String, ListGridField>();
        for (ListGridField listGridField : fields) {
            listGridFieldsMap.put(listGridField.getName(), listGridField);
        }
        dataSourceFieldNamesSet.removeAll(listGridFieldsMap.keySet());

        DataSourceField dataSourceIdField = this.dataSource.getField(FIELD_ID);
        boolean hideIdField = (!CoreGUI.isDebugMode() && !forceIdField);
        if (dataSourceIdField != null && hideIdField) {
            // setHidden() will not work on the DataSource field - use the listGrid.hideField() instead.
            this.listGrid.hideField(FIELD_ID);
        }

        ListGridField listGridIdField = listGridFieldsMap.get(FIELD_ID);
        if (listGridIdField != null) {
            listGridIdField.setHidden(hideIdField);
        }

        if (!dataSourceFieldNamesSet.isEmpty()) {
            ListGridField[] newFields = new ListGridField[fields.length + dataSourceFieldNamesSet.size()];
            int destIndex = 0;
            if (dataSourceFieldNamesSet.contains(FIELD_ID)) {
                listGridIdField = new ListGridField(FIELD_ID, MSG.common_title_id(), 55);
                // Override the DataSource id field metadata for consistent display across all Tables.
                listGridIdField.setType(ListGridFieldType.INTEGER);
                listGridIdField.setCanEdit(false);
                listGridIdField.setHidden(hideIdField);
                newFields[destIndex++] = listGridIdField;
                dataSourceFieldNamesSet.remove(FIELD_ID);
            }
            System.arraycopy(fields, 0, newFields, destIndex, fields.length);
            destIndex += fields.length;
            for (String dataSourceFieldName : dataSourceFieldNamesSet) {
                DataSourceField dataSourceField = this.dataSource.getField(dataSourceFieldName);
                ListGridField listGridField = new ListGridField(dataSourceField.getName());
                this.listGrid.hideField(dataSourceFieldName);
                listGridField.setHidden(true);
                newFields[destIndex++] = listGridField;
            }
            this.listGrid.setFields(newFields);
        } else {
            this.listGrid.setFields(fields);
        }
    }

    public void setTitleComponent(Canvas canvas) {
        this.titleComponent = canvas;
    }

    public void addTableAction(String locatorId, String title, TableAction tableAction) {
        this.addTableAction(locatorId, title, null, null, tableAction);
    }

    public void addTableAction(String locatorId, String title, String confirmation, TableAction tableAction) {
        this.addTableAction(locatorId, title, confirmation, null, tableAction);
    }

    public void addTableAction(String locatorId, String title, String confirmation,
        LinkedHashMap<String, ? extends Object> valueMap, TableAction tableAction) {
        TableActionInfo info = new TableActionInfo(locatorId, title, confirmation, valueMap, tableAction);
        tableActions.add(info);
    }

    public void setListGridDoubleClickHandler(DoubleClickHandler handler) {
        doubleClickHandler = handler;
    }

    public void addExtraWidget(Canvas canvas) {
        this.extraWidgets.add(canvas);
    }

    public void setHeaderIcon(String headerIcon) {
        if (this.headerIcons.size() > 0) {
            this.headerIcons.clear();
        }
        addHeaderIcon(headerIcon);
    }

    public void addHeaderIcon(String headerIcon) {
        this.headerIcons.add(headerIcon);
    }

    /**
     * By default, all table actions have buttons that are enabled or
     * disabled based on if and how many rows are selected. There are
     * times when you don't want the user to be able to press table action
     * buttons regardless of which rows are selected. This method let's
     * you set this override-disable flag.
     * 
     * Note: this also effects the double-click handler - if this disable override
     * is on, the double-click handler is not called.
     * 
     * @param disabled if true, all table action buttons will be disabled
     *                 if false, table action buttons will be enabled based on their predefined
     *                 selection enablement rule.
     */
    public void setTableActionDisableOverride(boolean disabled) {
        this.tableActionDisableOverride = disabled;
        refreshTableInfo();
    }

    public boolean getTableActionDisableOverride() {
        return this.tableActionDisableOverride;
    }

    protected void refreshTableInfo() {
        if (showFooter) {
            if (this.tableActionDisableOverride) {
                this.listGrid.setSelectionType(SelectionStyle.NONE);
            } else {
                this.listGrid.setSelectionType(getDefaultSelectionStyle());
            }

            int count = this.listGrid.getSelection().length;
            for (TableActionInfo tableAction : tableActions) {
                if (tableAction.actionCanvas != null) { // if null, we haven't initialized our buttons yet, so skip this
                    boolean enabled = (!this.tableActionDisableOverride && tableAction.action.isEnabled(this.listGrid
                        .getSelection()));
                    tableAction.actionCanvas.setDisabled(!enabled);
                }
            }
            for (Canvas extraWidget : extraWidgets) {
                if (extraWidget instanceof TableWidget) {
                    ((TableWidget) extraWidget).refresh(this.listGrid);
                }
            }
            if (getTableInfo() != null) {
                getTableInfo().setContents(
                    MSG.view_table_totalRows(String.valueOf(listGrid.getTotalRows()), String.valueOf(count)));
            }
        }
    }

    protected void deleteSelectedRecords() {
        getListGrid().removeSelectedData(new DSCallback() {
            public void execute(DSResponse response, Object rawData, DSRequest request) {
                Record[] deletedRecords = response.getData();
                List<String> recordNames = new ArrayList<String>(deletedRecords.length);
                for (Record deletedRecord : deletedRecords) {
                    String name = deletedRecord.getAttribute(getTitleFieldName());
                    recordNames.add(name);
                }

                Message message = new Message(MSG
                    .widget_recordEditor_info_recordsDeletedConcise(String.valueOf(deletedRecords.length),
                    getDataTypeNamePlural()), MSG
                    .widget_recordEditor_info_recordsDeletedDetailed(String.valueOf(deletedRecords.length),
                        getDataTypeNamePlural(), recordNames.toString()));
                CoreGUI.getMessageCenter().notify(message);
            }
        }, null);
    }

    protected String getDataTypeName() {
        return "item";
    }

    protected String getDataTypeNamePlural() {
        return "items";
    }

    protected String getTitleFieldName() {
        return FIELD_NAME;
    }

    protected String getDeleteConfirmMessage() {
        return MSG.common_msg_deleteConfirm(getDataTypeNamePlural());
    }

    // -------------- Inner utility classes ------------- //

    /**
     * A subclass of SmartGWT's DynamicForm widget that provides a more convenient interface for filtering a {@link Table} 
     * of results.
     *
     * @author Joseph Marques 
     */
    private static class TableFilter extends DynamicForm implements KeyPressHandler, ChangedHandler {

        private Table table;

        public TableFilter(Table table) {
            super();
            setWidth100();
            this.table = table;
            //this.table.setTableTitle(null);
        }

        @Override
        public void setItems(FormItem... items) {
            super.setItems(items);
            setupFormItems(items);
        }

        private void setupFormItems(FormItem... formItems) {
            for (FormItem nextFormItem : formItems) {
                nextFormItem.setWrapTitle(false);
                nextFormItem.setWidth(300); // wider than default
                if (nextFormItem instanceof TextItem) {
                    nextFormItem.addKeyPressHandler(this);
                } else if (nextFormItem instanceof SelectItem) {
                    nextFormItem.addChangedHandler(this);
                }
            }
        }

        private void fetchFilteredTableData() {
            table.refresh(getValuesAsCriteria());
        }

        public void onKeyPress(KeyPressEvent event) {
            if (event.getKeyName().equals("Enter") == false) {
                return;
            }
            fetchFilteredTableData();
        }

        public void onChanged(ChangedEvent event) {
            fetchFilteredTableData();
        }

        public boolean hasContent() {
            return super.getFields().length != 0;
        }

    }

    public static class TableActionInfo {
        private String locatorId;
        private String title;
        private String confirmMessage;
        private LinkedHashMap<String, ? extends Object> valueMap;
        private TableAction action;
        private Canvas actionCanvas;

        protected TableActionInfo(String locatorId, String title, String confirmMessage,
            LinkedHashMap<String, ? extends Object> valueMap, TableAction action) {
            this.locatorId = locatorId;
            this.title = title;
            this.confirmMessage = confirmMessage;
            this.valueMap = valueMap;
            this.action = action;
        }

        public String getLocatorId() {
            return locatorId;
        }

        public String getTitle() {
            return title;
        }

        public String getConfirmMessage() {
            return confirmMessage;
        }

        public LinkedHashMap<String, ? extends Object> getValueMap() {
            return valueMap;
        }

        public Canvas getActionCanvas() {
            return actionCanvas;
        }

        public void setActionCanvas(Canvas actionCanvas) {
            this.actionCanvas = actionCanvas;
        }

        public TableAction getAction() {
            return action;
        }

        public void setAction(TableAction action) {
            this.action = action;
        }

    }

    public boolean isShowFooterRefresh() {
        return showFooterRefresh;
    }

    public void setShowFooterRefresh(boolean showFooterRefresh) {
        this.showFooterRefresh = showFooterRefresh;
    }

    public Label getTableInfo() {
        return tableInfo;
    }

    public void setTableInfo(Label tableInfo) {
        this.tableInfo = tableInfo;
    }

    public boolean isShowFilterForm() {
        return showFilterForm;
    }

    public void setShowFilterForm(boolean showFilterForm) {
        this.showFilterForm = showFilterForm;
    }
}
