/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.coregui.client.components.table;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.user.client.Timer;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.DSCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.ResultSet;
import com.smartgwt.client.data.SortSpecifier;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.events.DoubleClickEvent;
import com.smartgwt.client.widgets.events.DoubleClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.HiddenItem;
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
import com.smartgwt.client.widgets.layout.Layout;
import com.smartgwt.client.widgets.layout.LayoutSpacer;
import com.smartgwt.client.widgets.menu.IMenuButton;
import com.smartgwt.client.widgets.menu.Menu;
import com.smartgwt.client.widgets.menu.MenuItem;
import com.smartgwt.client.widgets.menu.events.MenuItemClickEvent;

import org.rhq.core.domain.search.SearchSubsystem;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.InitializableView;
import org.rhq.coregui.client.RefreshableView;
import org.rhq.coregui.client.components.TitleBar;
import org.rhq.coregui.client.components.form.DateFilterItem;
import org.rhq.coregui.client.components.form.EnhancedSearchBarItem;
import org.rhq.coregui.client.util.CriteriaUtility;
import org.rhq.coregui.client.util.RPCDataSource;
import org.rhq.coregui.client.util.enhanced.EnhancedHLayout;
import org.rhq.coregui.client.util.enhanced.EnhancedIButton;
import org.rhq.coregui.client.util.enhanced.EnhancedIButton.ButtonColor;
import org.rhq.coregui.client.util.enhanced.EnhancedToolStrip;
import org.rhq.coregui.client.util.enhanced.EnhancedUtility;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;
import org.rhq.coregui.client.util.message.Message;

/**
 * A tabular view of set of data records from an {@link RPCDataSource}.
 *
 * WARNING! If you make _any_ changes to this class, no matter how seemingly
 * trivial, you must get it peer reviewed. Send out your proposed changes
 * to the dev mailing list and ask for comments. Any problems introduced to
 * this class are magnified because it is used in so many UI views and problems
 * are hard to detect due to the various ways it is used.
 *
 * @author Greg Hinkle
 * @author Ian Springer
 */
@SuppressWarnings("rawtypes")
public class Table<DS extends RPCDataSource> extends EnhancedHLayout implements RefreshableView, InitializableView {

    private static final int DATA_PAGE_SIZE = 50;

    protected static final String FIELD_ID = "id";
    protected static final String FIELD_NAME = "name";

    private EnhancedVLayout contents;

    private HTMLFlow titleCanvas;

    private EnhancedVLayout titleLayout;

    private TitleBar titleBar;
    private String titleIcon;

    private TableFilter filterForm;
    private ListGrid listGrid;
    private Label tableInfo;

    private List<String> headerIcons = new ArrayList<String>();

    private boolean showHeader = true;
    private boolean showFooter = true;
    private boolean showFooterRefresh = true;
    private boolean showFilterForm = true;

    private String titleString;
    private Criteria initialCriteria;
    private boolean initialCriteriaFixed = true;
    private SortSpecifier[] sortSpecifiers;
    private String[] excludedFieldNames;
    private boolean autoFetchData;
    private boolean flexRowDisplay = true;
    private boolean hideSearchBar = false;
    private String initialSearchBarSearchText = null;

    private DS dataSource;

    private DoubleClickHandler doubleClickHandler;
    private List<TableActionInfo> tableActions = new ArrayList<TableActionInfo>();
    private boolean tableActionDisableOverride = false;
    protected List<Canvas> extraWidgetsAtTop = new ArrayList<Canvas>();
    protected List<Canvas> extraWidgetsAboveFooter = new ArrayList<Canvas>();
    protected List<Canvas> extraWidgetsInMainFooter = new ArrayList<Canvas>();
    private EnhancedToolStrip topExtraWidgets;
    private EnhancedToolStrip footer;
    private EnhancedToolStrip footerExtraWidgets;
    private EnhancedIButton refreshButton;
    private boolean initialized;

    public Table() {
        this(null, null, null, null, true);
    }

    public Table(String tableTitle) {
        this(tableTitle, null, null, null, true);
    }

    public Table(String tableTitle, String icon) {
        this(tableTitle, null, null, null, true);
        setTitleIcon(icon);
    }

    public Table(String tableTitle, Criteria criteria, String icon) {
        this(tableTitle, criteria, null, null, (criteria == null));
        setTitleIcon(icon);
    }

    public Table(String tableTitle, Criteria criteria) {
        this(tableTitle, criteria, null, null, (criteria == null));
    }

    public Table(String tableTitle, SortSpecifier[] sortSpecifiers) {
        this(tableTitle, null, sortSpecifiers, null, true);
    }

    protected Table(String tableTitle, SortSpecifier[] sortSpecifiers, Criteria criteria) {
        this(tableTitle, criteria, sortSpecifiers, null, (criteria == null));
    }

    public Table(String tableTitle, boolean autoFetchData) {
        this(tableTitle, null, null, null, autoFetchData);
    }

    public Table(String tableTitle, SortSpecifier[] sortSpecifiers, String[] excludedFieldNames) {
        this(tableTitle, null, sortSpecifiers, excludedFieldNames, true);
    }

    public Table(String tableTitle, Criteria criteria, SortSpecifier[] sortSpecifiers, String[] excludedFieldNames) {
        this(tableTitle, criteria, sortSpecifiers, excludedFieldNames, (criteria == null));
    }

    public Table(String tableTitle, Criteria criteria, SortSpecifier[] sortSpecifiers, String[] excludedFieldNames,
        boolean autoFetchData) {
        super();

        if (criteria != null && autoFetchData) {
            throw new IllegalArgumentException(
                "Non-null initialCriteria and autoFetchData=true cannot be specified together, due to a bug in SmartGWT.");
        }
        setWidth100();
        setHeight100();
        //setOverflow(Overflow.HIDDEN);

        this.titleString = tableTitle;
        this.initialCriteria = criteria;
        this.sortSpecifiers = sortSpecifiers;
        this.excludedFieldNames = excludedFieldNames;
        this.autoFetchData = autoFetchData;
	//        setStyleName("commontable");
    }

    /**
     * If this returns true, then even if a {@link #getSearchSubsystem() search subsystem}
     * is defined by the table class, the search bar will not be shown.
     *
     * @return true if the search bar is to be hidden (default is false)
     */
    public boolean getHideSearchBar() {
        return this.hideSearchBar;
    }

    public void setHideSearchBar(boolean flag) {
        this.hideSearchBar = flag;
    }

    public String getInitialSearchBarSearchText() {
        return this.initialSearchBarSearchText;
    }

    public void setInitialSearchBarSearchText(String text) {
        this.initialSearchBarSearchText = text;
    }

    public void setFlexRowDisplay(boolean flexRowDisplay) {
        this.flexRowDisplay = flexRowDisplay;
    }

    /**
     * Override Point:
     * Override this method to use a customized layout.
     *
     * @return the Layout for all of the Table contents
     * @see #configureTableContents(Layout)
     */
    protected EnhancedVLayout createTableContents() {
        if (null == contents) {
            contents = new EnhancedVLayout();
        }

        return contents;
    }

    /**
     * Override Point:
     * Override this method to set Layout attributes that can be set at initialization time. By default
     * the contents layout is:
     * <pre>
     * - set to 100% width and height
     * - set to Overflow.AUTO
     * </pre>
     *
     * @param contents
     */
    protected void configureTableContents(Layout contents) {
        contents.setWidth100();
        contents.setHeight100();
        contents.setOverflow(Overflow.AUTO);
    }

    public EnhancedVLayout getTableContents() {
        return contents;
    }

    /**
     * Override Point:
     * Override this method to set grid properties outside of those required by Table or set via Table's constructors.
     * The default implementation:
     * <pre>
     * - sets to 100% width and height
     * </pre>
     * This is called from onInit() and guarantees grid not null.
     *
     * @param grid
     */
    protected void configureListGrid(ListGrid grid) {
        listGrid.setWidth100();
        listGrid.setHeight100();
        listGrid.setLoadingDataMessage("${loadingImage}&nbsp;" + MSG.common_msg_loading());
        listGrid.setEmptyMessage(MSG.common_msg_noItemsToShow());
    }

    @Override
    protected void onInit() {
        super.onInit();

        contents = createTableContents();
        configureTableContents(contents);
        addMember(contents);

        filterForm = new TableFilter(this);
	//        filterForm.setWidth100();
        filterForm.addStyleName("tableFilter");

        // Table filters and search bar are currently mutually exclusive.
        if (getSearchSubsystem() == null) {
            configureTableFilters();
        } else {
            if (!this.hideSearchBar) {
                final EnhancedSearchBarItem searchFilter = new EnhancedSearchBarItem("search", getSearchSubsystem(),
                    getInitialSearchBarSearchText());
                setFilterFormItems(searchFilter);
            }
        }

        listGrid = createListGrid();
        configureListGrid(listGrid);

        listGrid.setAutoFetchData(autoFetchData);

        if (initialCriteria != null) {
            listGrid.setInitialCriteria(initialCriteria);
        }

        if (sortSpecifiers != null) {
            listGrid.setInitialSort(sortSpecifiers);
        }

        listGrid.setAlternateRecordStyles(true);
        listGrid.setResizeFieldsInRealTime(false);
        listGrid.setSelectionType(getDefaultSelectionStyle());
        listGrid.setDataPageSize(DATA_PAGE_SIZE); // the default is 75 - lower it to speed up data loading
        // Don't fetch more than 200 records for the sake of an attempt to group-by.
        listGrid.setGroupByMaxRecords(200); // the default is 1000
        // Disable the group-by option in the column header context menu, since group-by requires the entire data set to
        // by loaded on the client-side, which isn't practical for most of our list views, since they can contain
        // thousands of records.
        listGrid.setCanGroupBy(false);

        if (flexRowDisplay) {
            //listGrid.setAutoFitData(Autofit.HORIZONTAL); // do NOT set this - smartgwt appears to have a problem that causes it to eat CPU
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

        DS dataSource = getDataSource();
        if (dataSource != null) {
            dataSource.setDataPageSize(DATA_PAGE_SIZE);
            listGrid.setDataSource(dataSource);
        }

        this.initialized = true;
    }

    @Override
    public boolean isInitialized() {
        return this.initialized;
    }

    protected SelectionStyle getDefaultSelectionStyle() {
        return SelectionStyle.MULTIPLE;
    }

    // Table is an InitializableView. This onDraw() waits until we're sure we're initialized and then
    // lays down the canvas.  This gives subclasses a chance to perform initialization (including async calls)
    // required to support the overrides (like configureTable()) they may have provided and that are called in
    // doOnDraw().
    @Override
    protected void onDraw() {
        super.onDraw();

        if (isInitialized()) {
            doOnDraw();

        } else {
            new Timer() {
                final long startTime = System.currentTimeMillis();

                public void run() {
                    if (isInitialized()) {
                        doOnDraw();
                        cancel();

                    } else {
                        // if after 10s we still aren't initialized just keep going, else try again
                        long elapsedMillis = System.currentTimeMillis() - startTime;
                        if (elapsedMillis < 10000L) {
                            schedule(100); // Reschedule the timer.

                        } else {
                            doOnDraw();
                            cancel();
                        }
                    }
                }
            }.run(); // fire the timer immediately
        }
    }

    protected void doOnDraw() {
        try {
            // I'm not sure this is necessary as I'm not sure it's the case that draw()/onDraw() will get called
            // multiple times. But if it did/does, this protects us by removing the current members before they
            // get set below.  Note that by having this here we *can not* add members in onInit, because they will
            // immediately get removed. -jshaughn
            for (Canvas child : contents.getMembers()) {
                contents.removeChild(child);
            }

            // add a toolstrip at the top of screen for navigation, date range controls, etc...
            this.topExtraWidgets = new EnhancedToolStrip();
            topExtraWidgets.setWidth100();
            topExtraWidgets.setMembersMargin(15);
            topExtraWidgets.hide();
            contents.addMember(topExtraWidgets);

            // Title
            this.titleCanvas = new HTMLFlow();
            updateTitleCanvas(this.titleString);

            if (showHeader) {
                // titleLayout not really needed now as TitleBar has a VLayout
                titleLayout = new EnhancedVLayout();
                titleLayout.setAutoHeight();
                titleLayout.setAlign(VerticalAlignment.BOTTOM);
                contents.addMember(titleLayout, 0);
            }

            if (filterForm.hasContent()) {
                contents.addMember(filterForm);
            }
            // add the listGrid defined in onInit
            contents.addMember(listGrid);

            // Footer

            // A second toolstrip that optionally appears before the main footer - it will contain extra widgets.
            // This is hidden from view unless extra widgets are actually added to the table above the main footer.
            this.footerExtraWidgets = new EnhancedToolStrip();
            //footerExtraWidgets.setPadding(5);
            footerExtraWidgets.setWidth100();
            footerExtraWidgets.setMembersMargin(15);
            footerExtraWidgets.hide();
	    //            footerExtraWidgets.setStyleName("footerExtraWidgets");

            contents.addMember(footerExtraWidgets);

            this.footer = new EnhancedToolStrip();
            footer.addStyleName("footer");
            //footer.setPadding(5);
            footer.setWidth100();
            footer.setMembersMargin(15);
            if (!showFooter) {
                footer.hide();
            }
            contents.addMember(footer);

            // The ListGrid has been created and configured - now give subclasses a chance to configure the table.
            configureTable();

            listGrid.addDoubleClickHandler(new DoubleClickHandler() {
                @Override
                public void onDoubleClick(DoubleClickEvent event) {
                    if (doubleClickHandler != null && !getTableActionDisableOverride()) {
                        doubleClickHandler.onDoubleClick(event);
                    }
                }
            });

            Label tableInfo = new Label();
	    //            tableInfo.setStyleName("tableInfo");
            tableInfo.setWrap(false);
            setTableInfo(tableInfo);
            refreshRowCount();

            // NOTE: It is essential that we wait to hide any excluded fields until after super.onDraw() is called, since
            //       super.onDraw() is what actually adds the fields to the ListGrid (based on what fields are defined in
            //       the underlying datasource).
            if (this.excludedFieldNames != null) {
                for (String excludedFieldName : excludedFieldNames) {
                    this.listGrid.hideField(excludedFieldName);
                }
            }

            if (showHeader) {
                drawHeader();
            }

            if (showFooter) {
                drawFooter();
            }

            if (!autoFetchData && (initialCriteria != null)) {
                refresh();
            }
        } catch (Exception e) {
            CoreGUI.getErrorHandler().handleError(MSG.view_table_drawFail(this.toString()), e);
        }


        markForRedraw();
    }

    private void refreshRowCount() {
        Label tableInfo = getTableInfo();
        if (tableInfo != null) {
            boolean lengthIsKnown = false;
            if (listGrid != null) {
                ResultSet results = listGrid.getResultSet();
                if (results != null) {
                    Boolean flag = results.lengthIsKnown();
                    if (flag != null) {
                        lengthIsKnown = flag.booleanValue();
                    }
                } else {
                    lengthIsKnown = (listGrid.getDataSource() == null); // not bound by a datasource, assume we know
                }
            }

            String contents;
            if (lengthIsKnown) {
                int totalRows = this.listGrid.getTotalRows();
                int selectedRows = this.listGrid.getSelectedRecords().length;
                contents = MSG.view_table_totalRows(String.valueOf(totalRows), String.valueOf(selectedRows));
            } else {
                contents = MSG.view_table_totalRowsUnknown();
            }
            tableInfo.setContents(contents);
        }
    }

    @Override
    public void destroy() {
        this.initialized = false;
        // immediately null out the listGrid to stop async refresh requests from executing during the destroy
        // logic. This happens in selenium testing or when a user navs away prior to the refresh.
        this.listGrid = null;

        EnhancedUtility.destroyMembers(createTableContents());
        super.destroy();
    }

    private void drawHeader() {

        // add toolstrip to the top of screen
        topExtraWidgets.removeMembers(topExtraWidgets.getMembers());
        if (!extraWidgetsAtTop.isEmpty()) {
            for (Canvas extraWidgetCanvas : extraWidgetsAtTop) {
                topExtraWidgets.addMember(extraWidgetCanvas);
            }
            topExtraWidgets.show();
        }

        // just use the first icon (not sure use case for multiple icons in title)
        titleBar = new TitleBar(titleString);
        if (titleIcon != null) {
            titleBar.setIcon(titleIcon);
        }
        titleLayout.addMember(titleBar);
        titleLayout.addMember(titleCanvas);
    }

    private void drawFooter() {
        // populate the extraWidgets toolstrip
        footerExtraWidgets.removeMembers(footerExtraWidgets.getMembers());
        if (!extraWidgetsAboveFooter.isEmpty()) {
            for (Canvas extraWidgetCanvas : extraWidgetsAboveFooter) {
                footerExtraWidgets.addMember(extraWidgetCanvas);
            }
            footerExtraWidgets.show();
        }

        footer.removeMembers(footer.getMembers());

        for (final TableActionInfo tableAction : tableActions) {

            if (null == tableAction.getValueMap()) {
                // button action
                IButton button = new EnhancedIButton(tableAction.getTitle());
                // set the id prefix to be able to match in css on this element
                if (tableAction.getButtonColor() != null) {
                    switch (tableAction.getButtonColor()) {
                    case BLUE:
                        button.setID("primary" + id + tableAction.getTitle());
                        break;
                    case RED:
                        button.setID("destructive" + id + tableAction.getTitle());
                        break;
                    default:
                        break;
                    }
                }
                // just in case, someone forgot to explicitly set the red color (works only for English)
                if (tableAction.getTitle().toLowerCase().contains("delete")
                    || tableAction.getTitle().toLowerCase().contains("remove")) {
                    button.setID("destructive" + id + tableAction.getTitle());
                }
                button.setTooltip(tableAction.getTooltip());
                button.setDisabled(true);
                button.setOverflow(Overflow.VISIBLE);
                button.addClickHandler(new ClickHandler() {
                    public void onClick(ClickEvent clickEvent) {
                        disableAllFooterControls();
                        if (tableAction.getConfirmMessage() != null) {
                            String message = tableAction.getConfirmMessage().replaceAll("\\#",
                                String.valueOf(listGrid.getSelectedRecords().length));

                            SC.ask(message, new BooleanCallback() {
                                public void execute(Boolean confirmed) {
                                    if (null == confirmed) {
                                        refreshTableInfo();
                                    } else if (confirmed) {
                                        refreshTableInfo();
                                        tableAction.action.executeAction(listGrid.getSelectedRecords(), null);
                                    } else {
                                        refreshTableInfo();
                                    }
                                }
                            });
                        } else {
                            tableAction.action.executeAction(listGrid.getSelectedRecords(), null);
                        }
                    }
                });

                tableAction.actionCanvas = button;
                footer.addMember(button);

            } else {
                // menu action
                Menu menu = new Menu();
                final Map<String, Object> menuEntries = tableAction.getValueMap();
                for (final String key : menuEntries.keySet()) {
                    MenuItem item = new MenuItem(key);
                    item.addClickHandler(new com.smartgwt.client.widgets.menu.events.ClickHandler() {
                        public void onClick(MenuItemClickEvent event) {
                            disableAllFooterControls();
                            tableAction.getAction().executeAction(listGrid.getSelectedRecords(), menuEntries.get(key));
                        }
                    });
                    menu.addItem(item);
                }

                IMenuButton menuButton = new IMenuButton(tableAction.getTitle());
                menuButton.setID("menuButton" + id + tableAction.getTitle());
                menuButton.setTooltip(tableAction.getTooltip());
                menuButton.setMenu(menu);
                menuButton.setDisabled(true);
                menuButton.setAutoFit(true); // this makes it pretty tight, but maybe better than the default, which is pretty wide
                menuButton.setOverflow(Overflow.VISIBLE);
                menuButton.setShowMenuBelow(false);

                tableAction.actionCanvas = menuButton;
                footer.addMember(menuButton);
            }
        }

        for (Canvas extraWidgetCanvas : extraWidgetsInMainFooter) {
            footer.addMember(extraWidgetCanvas);
        }

        footer.addMember(new LayoutSpacer());

        if (isShowFooterRefresh()) {
            this.refreshButton = new EnhancedIButton(MSG.common_button_refresh());
	    //            refreshButton.setOverflow(Overflow.VISIBLE);

            refreshButton.addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent clickEvent) {
                    disableAllFooterControls();
                    refresh();
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
                if (null != listGrid) {
                    refreshTableInfo();
                }
            }
        });

        // Ensure buttons are initially set correctly.
        refreshTableInfo();
    }

    public void disableAllFooterControls() {
        for (TableActionInfo tableAction : tableActions) {
            tableAction.actionCanvas.disable();
        }
        for (Canvas extraWidget : extraWidgetsAboveFooter) {
            extraWidget.disable();
        }
        for (Canvas extraWidget : extraWidgetsInMainFooter) {
            extraWidget.disable();
        }
        if (isShowFooterRefresh() && this.refreshButton != null) {
            this.refreshButton.disable();
        }
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
        //setShowHeader(false);
        this.filterForm.setItems(formItems);
        this.filterForm.setNumCols(4);
    }

    /**
     * Overriding components can use this as a chance to add {@link FormItem}s which will filter
     * the table that displays their data.
     */
    protected void configureTableFilters() {

    }

    public String getTitleString() {
        return this.titleString;
    }

    /**
     * Set the Table's title string. This will subsequently call {@link #updateTitleCanvas(String)}.
     * @param titleString
     */
    public void setTitleString(String titleString) {
        this.titleString = titleString;
        if (this.titleCanvas != null) {
            updateTitleCanvas(titleString);
        }
    }

    public void setTitleIcon(String titleIcon) {
        this.titleIcon = titleIcon;
    }

    public Canvas getTitleCanvas() {
        return this.titleCanvas;
    }

    /**
     * To set the Table's title, call {@link #setTitleString(String)}. This is primarily declared for purposes of
     * override.
     * @param titleString
     */
    public void updateTitleCanvas(String titleString) {
        if (titleString == null) {
            titleString = "";
        }

        titleCanvas.markForRedraw();
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

    /**
     * Refreshes the list grid's data, filtered by any fixed criteria, as well as any user-specified filters.
     */
    public void refresh() {
        refresh(false);
    }

    /**
     * Refreshes the list grid's data, filtered by any fixed criteria, as well as any user-specified filters.
     * <p/>
     * If resetPaging is true, resets paging on the grid prior to refreshing the data. resetPaging=true should be
     * specified when refreshing right after records have been deleted, since the current paging settings may have
     * become invalid due to the decrease in the total data set size.
     */
    public void refresh(boolean resetPaging) {
        if (!isInitialized()) {
            return;
        }

        final ListGrid listGrid = getListGrid();

        Criteria criteria = getCurrentCriteria();
        listGrid.setCriteria(criteria);

        if (resetPaging) {
            listGrid.scrollToRow(0);
        }

        // Only call invalidateCache() and fetchData() if the ListGrid is backed by a DataSource.
        if (listGrid.getDataSource() != null) {
            // Invalidate the cached records - if listGrid.getAutoFetchData() is true, this will cause the ListGrid to
            // automatically call fetchData().
            listGrid.invalidateCache();
            if (!this.autoFetchData && (initialCriteria != null)) {
                listGrid.fetchData(criteria);
            }
        }
        listGrid.markForRedraw();
    }

    protected Criteria getInitialCriteria() {
        return initialCriteria;
    }

    /**
     * Can be called in constructor to reset initialCriteria.
     * @param initialCriteria
     */
    protected void setInitialCriteria(Criteria initialCriteria) {
        this.initialCriteria = initialCriteria;
    }

    protected boolean isInitialCriteriaFixed() {
        return initialCriteriaFixed;
    }

    /**
     * @param initialCriteriaFixed If true initialCriteria is applied to all subsequent fetch criteria. If false
     * initialCriteria is used only for the initial autoFetch. Irrelevant if autoFetch is false. Default is true.
     */
    protected void setInitialCriteriaFixed(boolean initialCriteriaFixed) {
        this.initialCriteriaFixed = initialCriteriaFixed;
    }

    /**
     *
     * @return the current criteria, which includes any fixed criteria, as well as any user-specified filters; may be
     *         null if there are no fixed criteria or user-specified filters
     */
    protected Criteria getCurrentCriteria() {
        Criteria criteria = null;

        // If this table has a filter form (table filters OR search bar),
        // we need to refresh it as per the filtering, combined with any fixed criteria.
        if (this.filterForm != null && this.filterForm.hasContent()) {
            criteria = this.filterForm.getValuesAsCriteria();
            if (this.initialCriteriaFixed) {
                if (criteria != null) {
                    if (this.initialCriteria != null) {
                        // There is fixed criteria - add it to the filter form criteria.
                        CriteriaUtility.addCriteria(criteria, this.initialCriteria);
                    }
                } else {
                    criteria = this.initialCriteria;
                }
            }
        } else if (this.initialCriteriaFixed) {
            criteria = this.initialCriteria;
        }

        return criteria;
    }

    public DS getDataSource() {
        return dataSource;
    }

    public void setDataSource(DS dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Creates this Table's list grid (called by onInit()). Subclasses can override this if they require a custom
     * subclass of ListGrid.
     *
     * @return this Table's list grid (must be an instance of ListGrid)
     */
    protected ListGrid createListGrid() {
        return new ListGrid();
    }

    /**
     * Returns this Table's list grid - may be null if the Table has not yet been {@link #isInitialized() initialized}.
     * Subclasses should *not* override this method.
     *
     * @return this Table's list grid - may be null if the Table has not yet been {@link #isInitialized() initialized}
     */
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
        if (getDataSource() == null) {
            throw new IllegalStateException("setListGridFields() called on " + getClass().getName()
                + ", which is not a DataSource-backed Table.");
        }
        String[] dataSourceFieldNames = getDataSource().getFieldNames();
        Set<String> dataSourceFieldNamesSet = new LinkedHashSet<String>();
        dataSourceFieldNamesSet.addAll(Arrays.asList(dataSourceFieldNames));
        Map<String, ListGridField> listGridFieldsMap = new LinkedHashMap<String, ListGridField>();
        for (ListGridField listGridField : fields) {
            listGridFieldsMap.put(listGridField.getName(), listGridField);
        }
        dataSourceFieldNamesSet.removeAll(listGridFieldsMap.keySet());

        DataSourceField dataSourceIdField = getDataSource().getField(FIELD_ID);
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
                String datasourceFieldTitle = getDataSource().getField(FIELD_ID).getTitle();
                String listGridFieldTitle = (datasourceFieldTitle != null) ? datasourceFieldTitle : MSG
                    .common_title_id();
                listGridIdField = new ListGridField(FIELD_ID, listGridFieldTitle, 55);
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
                DataSourceField dataSourceField = getDataSource().getField(dataSourceFieldName);
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

    public void setTitleBar(TitleBar titleBar1) {
        this.titleBar = titleBar1;

    }

    /**
     * Note: To prevent user action while a current action completes, all widgets on the footer are disabled
     * when footer actions take place, typically a button click.  It is up to the action to ensure the page
     * (via refresh() or CoreGUI.refresh()) or footer (via refreshTableActions) are refreshed as needed at action
     * completion. Failure to do so may leave the widgets disabled.
     */
    public void addTableAction(String title, TableAction tableAction) {
        this.addTableAction(title, null, null, null, tableAction);
    }
    
    public void addTableAction(String title, ButtonColor buttonColor, TableAction tableAction) {
        this.addTableAction(title, null, null, buttonColor, tableAction);
    }

    /**
     * Note: To prevent user action while a current action completes, all widgets on the footer are disabled
     * when footer actions take place, typically a button click.  It is up to the action to ensure the page
     * (via refresh() or CoreGUI.refresh()) or footer (via refreshTableActions) are refreshed as needed at action
     * completion. Failure to do so may leave the widgets disabled.
     */
    public void addTableAction(String title, String confirmation, TableAction tableAction) {
        this.addTableAction(title, confirmation, null, null, tableAction);
    }
    
    public void addTableAction(String title, String confirmation, ButtonColor buttonColor, TableAction tableAction) {
        this.addTableAction(title, confirmation, null, buttonColor, tableAction);
    }

    /**
     * Note: To prevent user action while a current action completes, all widgets on the footer are disabled
     * when footer actions take place, typically a button click.  It is up to the action to ensure the page
     * (via refresh() or CoreGUI.refresh()) or footer (via refreshTableActions) are refreshed as needed at action
     * completion. Failure to do so may leave the widgets disabled.
     */
    public void addTableAction(String title, String confirmation, Map<String, Object> valueMap, ButtonColor buttonColor, TableAction tableAction) {
        TableActionInfo info = new TableActionInfo.TableActionInfoBuilder(title, tableAction)
            .setConfirmMessage(confirmation)
            .setValueMap(valueMap)
            .setButtonColor(buttonColor == null ? ButtonColor.GRAY : buttonColor)
            .createTableActionInfo();
        tableActions.add(info);
    }

    /**
     * Note: To prevent user action while a current action completes, all widgets on the footer are disabled
     * when footer actions take place, typically a button click.  It is up to the action to ensure the page
     * (via refresh() or CoreGUI.refresh()) or footer (via refreshTableActions) are refreshed as needed at action
     * completion. Failure to do so may leave the widgets disabled.
     */
    public void addTableAction(TableActionInfo info) {
        tableActions.add(info);
    }

    /**
     * Updates the list of table's associated actions <code>tableActions</code>.
     * It automatically updates the gui by calling <code>drawFooter()</code> provided the table has been initialized.
     *
     * Note: To prevent user action while a current action completes, all widgets on the footer are disabled
     * when footer actions take place, typically a button click. It is up to the action to ensure the page
     * (via refresh() or CoreGUI.refresh()) or footer (via refreshTableActions) are refreshed as needed at action
     * completion. Failure to do so may leave the widgets disabled.
     *
     * @param title the title of a modified action
     * @param valueMap the map containing the tuples with name of a select item and <code>actionValue</code> which is
     * then passed to <code>tableAction.executeAction()</code>; use the <code>LinkedHashMap</code> if you want to
     * preserve the order of map items
     * @param tableAction the tableAction object (on this object the <code>executeAction()</code> is actually invoked)
     */
    public void updateTableAction(String title, Map<String, Object> valueMap, TableAction tableAction) {
        if (title == null) {
            return;
        }
        for (TableActionInfo info : tableActions) {
            if (title.equals(info.getTitle())) {
                if (valueMap != null)
                    info.setValueMap(valueMap);
                if (tableAction != null)
                    info.setAction(tableAction);
                // the action listeners have to be re-added
                if (isInitialized())
                    drawFooter();
                break;
            }
        }
    }

    public void setListGridDoubleClickHandler(DoubleClickHandler handler) {
        doubleClickHandler = handler;
    }

    /**
     * Adds extra widgets to the bottom of the table view.
     * <br/><br/>
     * Note: To prevent user action while a current action completes, all widgets on the footer are disabled
     * when footer actions take place, typically a button click.  It is up to the action to ensure the page
     * (via refresh() or CoreGUI.refresh()) or footer (via refreshTableActions) are refreshed as needed at action
     * completion. Failure to do so may leave the widgets disabled.
     *
     * @param widget the new widget to add to the table view
     * @param aboveFooter if true, the widget will be placed in a second toolstrip just above the main footer.
     *                    if false, the widget will be placed in the main footer toolstrip itself. This is
     *                    useful if the widget is really big and won't fit in the main footer along with the
     *                    rest of the main footer members.
     */
    public void addExtraWidget(Canvas widget, boolean aboveFooter) {
        if (aboveFooter) {
            this.extraWidgetsAboveFooter.add(widget);
        } else {
            this.extraWidgetsInMainFooter.add(widget);
        }
    }

    /**
     * Add any widgets to the top of the table for filtering, etc...
     * @param widget
     */
    public void addTopWidget(Canvas widget) {
        this.extraWidgetsAtTop.add(widget);
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

    public void refreshTableInfo() {
        if (this.showFooter && (this.listGrid != null)) {
            if (this.tableActionDisableOverride) {
                this.listGrid.setSelectionType(SelectionStyle.NONE);
            } else {
                this.listGrid.setSelectionType(getDefaultSelectionStyle());
            }

            for (TableActionInfo tableAction : this.tableActions) {
                if (tableAction.actionCanvas != null) { // if null, we haven't initialized our buttons yet, so skip this
                    boolean enabled = (!this.tableActionDisableOverride && tableAction.action.isEnabled(this.listGrid
                        .getSelectedRecords()));
                    tableAction.actionCanvas.setDisabled(!enabled);
                }
            }
            for (Canvas extraWidget : this.extraWidgetsAboveFooter) {
                extraWidget.enable();
                if (extraWidget instanceof TableWidget) {
                    ((TableWidget) extraWidget).refresh(this.listGrid);
                }
            }
            for (Canvas extraWidget : this.extraWidgetsInMainFooter) {
                extraWidget.enable();
                if (extraWidget instanceof TableWidget) {
                    ((TableWidget) extraWidget).refresh(this.listGrid);
                }
            }
            refreshRowCount();
            if (isShowFooterRefresh() && this.refreshButton != null) {
                this.refreshButton.enable();
            }
        }
    }

    protected void deleteSelectedRecords() {
        deleteSelectedRecords(null);
    }

    protected void deleteSelectedRecords(DSRequest requestProperties) {
        ListGrid listGrid = getListGrid();
        final int selectedRecordCount = listGrid.getSelectedRecords().length;
        final List<String> deletedRecordNames = new ArrayList<String>(selectedRecordCount);
        listGrid.removeSelectedData(new DSCallback() {
            public void execute(DSResponse response, Object rawData, DSRequest request) {
                if (response.getStatus() == DSResponse.STATUS_SUCCESS) {
                    Record[] deletedRecords = response.getData();
                    for (Record deletedRecord : deletedRecords) {
                        String name = deletedRecord.getAttribute(getTitleFieldName());
                        deletedRecordNames.add(name);
                    }
                    if (deletedRecordNames.size() == selectedRecordCount) {
                        // all selected schedules were successfully deleted.
                        String deletedMessage = getDeletedMessage(deletedRecordNames.size());
                        String deletedMessageDetail = deletedMessage + ": [" + deletedRecordNames.toString() + "]";
                        Message message = new Message(deletedMessage, deletedMessageDetail);
                        CoreGUI.getMessageCenter().notify(message);
                        refresh();
                    }
                }
                // TODO: Print error messages for failures or partial failures.
            }
        }, requestProperties);
    }

    protected String getTitleFieldName() {
        return FIELD_NAME;
    }

    protected String getDeletedMessage(int numDeleted) {
        String num = String.valueOf(numDeleted);
        String thing = (1 == numDeleted) ? MSG.common_label_item() : MSG.common_label_items();
        return MSG.common_msg_deleted(num, thing);
    }

    protected String getDeleteConfirmMessage() {
        return MSG.common_msg_deleteConfirm(MSG.common_label_items());
    }

    protected void hideField(ListGridField field) {
        getListGrid().hideField(field.getName());
        field.setHidden(true);
    }

    // -------------- Inner utility classes ------------- //

    /**
     * A subclass of SmartGWT's DynamicForm widget that provides a more convenient interface for filtering a
     * {@link Table} of results.
     *
     * @author Joseph Marques
     */
    private static class TableFilter extends DynamicForm implements KeyPressHandler, ChangedHandler {

        private Table<?> table;
        private EnhancedSearchBarItem searchBarItem;
        private HiddenItem hiddenItem;

        public TableFilter(Table<?> table) {
            super();
            setOverflow(Overflow.VISIBLE);
            setAutoWidth();
            this.table = table;
        }

        @Override
        public void setItems(FormItem... items) {
            for (FormItem nextFormItem : items) {
                nextFormItem.setWrapTitle(false);
                nextFormItem.setWidth(300); // wider than default
                if (nextFormItem instanceof TextItem) {
                    nextFormItem.addKeyPressHandler(this);
                } else if (nextFormItem instanceof SelectItem) {
                    nextFormItem.addChangedHandler(this);
                } else if (nextFormItem instanceof DateFilterItem) {
                    nextFormItem.addChangedHandler(this);
                } else if (nextFormItem instanceof EnhancedSearchBarItem) {
                    searchBarItem = (EnhancedSearchBarItem) nextFormItem;
                    searchBarItem.getSearchBar().getSearchTextItem().addKeyPressHandler(this);
                    String name = searchBarItem.getName();
                    // postfix the name of the item so it is not processed by the filters and that the
                    // hidden item is used instead.
                    searchBarItem.setName(name + "_hidden");
                    hiddenItem = new HiddenItem(name);
                    hiddenItem.setValue(searchBarItem.getSearchBar().getSearchTextItem().getValueAsString());
                }
            }

            if (hiddenItem != null) {
                // Add the hidden item if it exists
                FormItem[] tmpItems = new FormItem[items.length + 1];
                System.arraycopy(items, 0, tmpItems, 0, items.length);
                tmpItems[items.length] = hiddenItem;
                items = tmpItems;
            }
            //            for (FormItem item : items) {
            //                Log.debug(" ********     Form Items sent: " + item.getName() + ": " + item.getValue());
            //            }

            super.setItems(items);
        }

        private void fetchFilteredTableData() {
            table.refresh();
        }

        @Override
        public void onKeyPress(KeyPressEvent event) {
            if (event.getKeyName().equals("Enter")) {

                if (null != searchBarItem) {
                    if (searchBarItem.getSearchBar().isFilterEnabled()) {
                        TextItem searchTextItem = searchBarItem.getSearchBar().getSearchTextItem();
                        String searchBarValue = searchTextItem.getValueAsString();
                        String hiddenValue = (String) hiddenItem.getValue();

                        // Only send a fetch request if the user actually changed the search expression.
                        if (!equals(searchBarValue, hiddenValue)) {
                            hiddenItem.setValue(searchBarValue);
                            fetchFilteredTableData();
                        }
                    }
                } else {
                    fetchFilteredTableData();
                }
            }
        }

        @Override
        public void onChanged(ChangedEvent event) {
            fetchFilteredTableData();
        }

        public boolean hasContent() {
            return super.getFields().length != 0;
        }

        private static boolean equals(String string1, String string2) {
            if (string1 == null) {
                return (string2 == null);
            } else {
                return (string1.equals(string2));
            }
        }
    }

    public static class TableActionInfo {
        private String title;
        private String tooltip;
        private String confirmMessage;
        private Map<String, Object> valueMap;
        private TableAction action;
        private Canvas actionCanvas;
        private ButtonColor buttonColor;

        private TableActionInfo(String title, String tooltip, String confirmMessage, Map<String, Object> valueMap,
            TableAction action) {
            this(title, tooltip, confirmMessage,valueMap, action, ButtonColor.GRAY);
        }
        
        private TableActionInfo(String title, String tooltip, String confirmMessage, Map<String, Object> valueMap,
            TableAction action, ButtonColor buttonColor) {
            this.title = title;
            this.tooltip = tooltip;
            this.confirmMessage = confirmMessage;
            this.valueMap = valueMap;
            this.action = action;
            this.buttonColor = buttonColor;
        }

        public String getTitle() {
            return title;
        }

        public String getConfirmMessage() {
            return confirmMessage;
        }

        public String getTooltip() {
            return tooltip;
        }

        public Map<String, Object> getValueMap() {
            return valueMap;
        }

        public void setValueMap(Map<String, Object> valueMap) {
            this.valueMap = valueMap;
        }

        public TableAction getAction() {
            return action;
        }

        public void setAction(TableAction action) {
            this.action = action;
        }
        
        public ButtonColor getButtonColor() {
            return buttonColor;
        }

        public void setButtonColor(ButtonColor buttonColor) {
            this.buttonColor = buttonColor;
        }

        public static class TableActionInfoBuilder {
            private String title;
            private String tooltip;
            private String confirmMessage;
            private Map<String, Object> valueMap;
            private TableAction action;
            private ButtonColor buttonColor;

            public TableActionInfoBuilder(String title, TableAction action) {
                this.title = title;
                this.action = action;
            }

            public TableActionInfoBuilder setTooltip(String tooltip) {
                this.tooltip = tooltip;
                return this;
            }

            public TableActionInfoBuilder setConfirmMessage(String confirmMessage) {
                this.confirmMessage = confirmMessage;
                return this;
            }

            public TableActionInfoBuilder setValueMap(Map<String, Object> valueMap) {
                this.valueMap = valueMap;
                return this;
            }
            
            public TableActionInfoBuilder setButtonColor(ButtonColor buttonColor) {
                this.buttonColor = buttonColor;
                return this;
            }

            public TableActionInfo createTableActionInfo() {
                return new TableActionInfo(title, tooltip, confirmMessage, valueMap, action,
                    buttonColor == null ? ButtonColor.GRAY : buttonColor);
            }
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

    /*
     * By default, no search bar is shown above this table.  if this table represents a subsystem that is capable
     * of search, return the specific object here.
     */
    protected SearchSubsystem getSearchSubsystem() {
        return null;
    }

}
