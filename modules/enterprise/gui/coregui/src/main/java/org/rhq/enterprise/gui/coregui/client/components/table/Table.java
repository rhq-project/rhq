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
import java.util.List;

import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.SortSpecifier;
import com.smartgwt.client.types.Autofit;
import com.smartgwt.client.types.Overflow;
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
import com.smartgwt.client.widgets.grid.events.DataArrivedEvent;
import com.smartgwt.client.widgets.grid.events.DataArrivedHandler;
import com.smartgwt.client.widgets.grid.events.SelectionChangedHandler;
import com.smartgwt.client.widgets.grid.events.SelectionEvent;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.LayoutSpacer;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.toolbar.ToolStrip;

import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.RefreshableView;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableIButton;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableListGrid;

/**
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class Table extends LocatableHLayout implements RefreshableView {

    private static final SelectionEnablement DEFAULT_SELECTION_ENABLEMENT = SelectionEnablement.ALWAYS;

    private VLayout contents;

    private HTMLFlow title;

    private HLayout titleLayout;
    private Canvas titleComponent;

    private TableFilter filterForm;
    private ListGrid listGrid;
    private ToolStrip footer;
    private Label tableInfo;

    private List<String> headerIcons = new ArrayList<String>();

    private boolean showHeader = true;
    private boolean showFooter = true;
    private boolean showFooterRefresh = true;

    private String tableTitle;
    private Criteria criteria;
    private SortSpecifier[] sortSpecifiers;
    private String[] excludedFieldNames;
    private boolean autoFetchData;
    private boolean flexRowDisplay = true;

    private RPCDataSource dataSource;

    /**
     * Specifies how many rows must be selected in order for a {@link TableAction} button to be enabled.
     */
    public enum SelectionEnablement {
        /**
         * Enabled no matter how many rows are selected (zero or more)
         */
        ALWAYS,
        /**
         * One or more rows are selected.
         */
        ANY,
        /**
         * Exactly one row is selected.
         */
        SINGLE,
        /**
         * Two or more rows are selected.
         */
        MULTIPLE,
        /**
         * Never enabled - usually due to the user having a lack of permissions
         */
        NEVER
    }

    ;

    private DoubleClickHandler doubleClickHandler;
    private List<TableActionInfo> tableActions = new ArrayList<TableActionInfo>();
    private boolean tableActionDisableOverride = false;
    protected List<Canvas> extraWidgets = new ArrayList<Canvas>();

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

    @Override
    protected void onDraw() {
        try {
            super.onDraw();

            for (Canvas child : contents.getMembers()) {
                contents.removeChild(child);
            }

            // Title
            title = new HTMLFlow();
            setTableTitle(tableTitle);

            if (showHeader) {
                titleLayout = new HLayout();
                titleLayout.setAutoHeight();
                titleLayout.setAlign(VerticalAlignment.BOTTOM);
            }

            // Add components to the view
            if (showHeader) {
                contents.addMember(titleLayout, 0);
            }

            if (filterForm.hasContent()) {
                contents.addMember(filterForm);
            }

            contents.addMember(listGrid);

            // Footer
            footer = new ToolStrip();
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

            if (showFooter) {

                footer.removeMembers(footer.getMembers());

                for (final TableActionInfo tableAction : tableActions) {
                    IButton button = new LocatableIButton(tableAction.getLocatorId(), tableAction.getTitle());
                    button.setDisabled(true);
                    button.addClickHandler(new ClickHandler() {
                        public void onClick(ClickEvent clickEvent) {
                            if (tableAction.confirmMessage != null) {

                                String message = tableAction.confirmMessage.replaceAll("\\#", String.valueOf(listGrid
                                    .getSelection().length));

                                SC.ask(message, new BooleanCallback() {
                                    public void execute(Boolean confirmed) {
                                        if (confirmed) {
                                            tableAction.action.executeAction(listGrid.getSelection());
                                        }
                                    }
                                });
                            } else {
                                tableAction.action.executeAction(listGrid.getSelection());
                            }
                        }
                    });
                    tableAction.actionButton = button;
                    footer.addMember(button);
                }

                for (Canvas extraWidgetCanvas : extraWidgets) {
                    footer.addMember(extraWidgetCanvas);
                }

                footer.addMember(new LayoutSpacer());

                if (isShowFooterRefresh()) {
                    IButton refreshButton = new LocatableIButton(extendLocatorId("Refresh"), "Refresh");
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
        } catch (Exception e) {
            CoreGUI.getErrorHandler().handleError("Failed to draw Table [" + this + "].", e);
        }
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

    /**
     * Overriding components can use this as a chance to configure the list grid after it has been
     * created but before it has been drawn to the DOM. This is also the proper place to add table
     * actions so that they're rendered in the footer.
     */
    protected void configureTable() {

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

    public RPCDataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(RPCDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public ListGrid getListGrid() {
        return listGrid;
    }

    public void setTitleComponent(Canvas canvas) {
        this.titleComponent = canvas;
    }

    public void addTableAction(String locatorId, String title, TableAction tableAction) {
        this.addTableAction(locatorId, title, null, null, tableAction);
    }

    public void addTableAction(String locatorId, String title, SelectionEnablement enablement, String confirmation,
        TableAction tableAction) {

        if (enablement == null) {
            enablement = DEFAULT_SELECTION_ENABLEMENT;
        }
        TableActionInfo info = new TableActionInfo(locatorId, title, enablement, tableAction);
        info.confirmMessage = confirmation;
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
            int count = this.listGrid.getSelection().length;
            for (TableActionInfo tableAction : tableActions) {
                if (tableAction.actionButton != null) { // if null, we haven't initialized our buttons yet, so skip this
                    boolean enabled;
                    if (!this.tableActionDisableOverride) {
                        switch (tableAction.enablement) {
                        case ALWAYS:
                            enabled = true;
                            break;
                        case NEVER:
                            enabled = false;
                            break;
                        case ANY:
                            enabled = (count >= 1);
                            break;
                        case SINGLE:
                            enabled = (count == 1);
                            break;
                        case MULTIPLE:
                            enabled = (count > 1);
                            break;
                        default:
                            throw new IllegalStateException("Unhandled SelectionEnablement: "
                                + tableAction.enablement.name());
                        }
                    } else {
                        enabled = false;
                    }
                    tableAction.actionButton.setDisabled(!enabled);
                }
            }
            for (Canvas extraWidget : extraWidgets) {
                if (extraWidget instanceof TableWidget) {
                    ((TableWidget) extraWidget).refresh(this.listGrid);
                }
            }
            if (getTableInfo() != null) {
                getTableInfo().setContents("Total: " + listGrid.getTotalRows() + " (" + count + " selected)");
            }
        }
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

    private static class TableActionInfo {

        private String locatorId;
        private String title;
        private SelectionEnablement enablement;
        private TableAction action;
        private String confirmMessage;
        private IButton actionButton;

        protected TableActionInfo(String locatorId, String title, SelectionEnablement enablement, TableAction action) {
            this.locatorId = locatorId;
            this.title = title;
            this.enablement = enablement;
            this.action = action;
        }

        public String getLocatorId() {
            return locatorId;
        }

        public String getTitle() {
            return title;
        }

        public SelectionEnablement getEnablement() {
            return enablement;
        }

        public IButton getActionButton() {
            return actionButton;
        }

        String getConfirmMessage() {
            return confirmMessage;
        }

        void setConfirmMessage(String confirmMessage) {
            this.confirmMessage = confirmMessage;
        }

        void setActionButton(IButton actionButton) {
            this.actionButton = actionButton;
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
}
