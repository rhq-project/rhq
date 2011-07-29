package org.rhq.enterprise.gui.coregui.client.components.table;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.History;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.SortSpecifier;
import com.smartgwt.client.types.AnimationEffect;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.AnimationCallback;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.events.DoubleClickEvent;
import com.smartgwt.client.widgets.events.DoubleClickHandler;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.DetailsView;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.components.buttons.BackButton;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;
import org.rhq.enterprise.gui.coregui.client.util.StringUtility;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.SeleniumUtility;

public abstract class TableSection2 <DS extends RPCDataSource> extends Table<DS> implements BookmarkableView {

    private VLayout detailsHolder;
    private Canvas detailsView;
    private String basePath;
    private boolean escapeHtmlInDetailsLinkColumn;
    private boolean initialDisplay;

    protected TableSection2(String locatorId, String tableTitle) {
        super(locatorId, tableTitle);
    }

    protected TableSection2(String locatorId, String tableTitle, Criteria criteria) {
        super(locatorId, tableTitle, criteria);
    }

    protected TableSection2(String locatorId, String tableTitle, SortSpecifier[] sortSpecifiers) {
        super(locatorId, tableTitle, sortSpecifiers);
    }

    protected TableSection2(String locatorId, String tableTitle, Criteria criteria, SortSpecifier[] sortSpecifiers) {
        super(locatorId, tableTitle, sortSpecifiers, criteria);
    }

    protected TableSection2(String locatorId, String tableTitle, boolean autoFetchData) {
        super(locatorId, tableTitle, autoFetchData);
    }

    protected TableSection2(String locatorId, String tableTitle, SortSpecifier[] sortSpecifiers,
        String[] excludedFieldNames) {
        super(locatorId, tableTitle, null, sortSpecifiers, excludedFieldNames);
    }

    protected TableSection2(String locatorId, String tableTitle, Criteria criteria, SortSpecifier[] sortSpecifiers,
        String[] excludedFieldNames) {
        super(locatorId, tableTitle, criteria, sortSpecifiers, excludedFieldNames);
    }

    protected TableSection2(String locatorId, String tableTitle, Criteria criteria, SortSpecifier[] sortSpecifiers,
        String[] excludedFieldNames, boolean autoFetchData) {
        super(locatorId, tableTitle, criteria, sortSpecifiers, excludedFieldNames, autoFetchData);
    }

    @Override
    protected void onInit() {
        super.onInit();

        this.initialDisplay = true;

        detailsHolder = new LocatableVLayout(extendLocatorId("tableSection"));
        detailsHolder.setAlign(VerticalAlignment.TOP);
        //detailsHolder.setWidth100();
        //detailsHolder.setHeight100();
        detailsHolder.setMargin(4);
        detailsHolder.hide();

        addMember(detailsHolder);

        // if the detailsView is already defined it means we want the details view to be rendered prior to
        // the master view, probably due to a direct navigation or refresh (like F5 when sitting on the details page)
        if (null != detailsView) {
            switchToDetailsView();
        }
    }

    /**
     * The default implementation wraps the {@link #getDetailsLinkColumnCellFormatter()} column with the
     * {@link #getDetailsLinkColumnCellFormatter()}. This is typically the 'name' column linking to the detail
     * view, given the 'id'. Also, establishes a double click handler for the row which invokes
     * {@link #showDetails(com.smartgwt.client.widgets.grid.ListGridRecord)}</br>
     * </br>
     * In general, in overrides, call super.configureTable *after* manipulating the ListGrid fields.
     *
     * @see org.rhq.enterprise.gui.coregui.client.components.table.Table#configureTable()
     */
    @Override
    protected void configureTable() {
        if (isDetailsEnabled()) {
            ListGrid grid = getListGrid();

            // Make the value of some specific field a link to the details view for the corresponding record.
            ListGridField field = (grid != null) ? grid.getField(getDetailsLinkColumnName()) : null;
            if (field != null) {
                field.setCellFormatter(getDetailsLinkColumnCellFormatter());
            }

            setListGridDoubleClickHandler(new DoubleClickHandler() {
                @Override
                public void onDoubleClick(DoubleClickEvent event) {
                    ListGrid listGrid = (ListGrid) event.getSource();
                    ListGridRecord[] selectedRows = listGrid.getSelection();
                    if (selectedRows != null && selectedRows.length == 1) {
                        showDetails(selectedRows[0]);
                    }
                }
            });
        }
    }

    protected boolean isDetailsEnabled() {
        return true;
    }

    public void setEscapeHtmlInDetailsLinkColumn(boolean escapeHtmlInDetailsLinkColumn) {
        this.escapeHtmlInDetailsLinkColumn = escapeHtmlInDetailsLinkColumn;
    }

    /**
     * Override if you don't want FIELD_NAME to be wrapped ina link.
     * @return the name of the field to be wrapped, or null if no field should be wrapped.
     */
    protected String getDetailsLinkColumnName() {
        return FIELD_NAME;
    }

    /**
     * Override if you don't want the detailsLinkColumn to have the default link wrapper.
     * @return the desired CellFormatter.
     */
    protected CellFormatter getDetailsLinkColumnCellFormatter() {
        return new CellFormatter() {
            public String format(Object value, ListGridRecord record, int i, int i1) {
                if (value == null) {
                    return "";
                }
                String recordId = getId(record);
                String detailsUrl = "#" + getBasePath() + "/" + recordId;
                String formattedValue = (escapeHtmlInDetailsLinkColumn) ? StringUtility.escapeHtml(value.toString())
                    : value.toString();
                return SeleniumUtility.getLocatableHref(detailsUrl, formattedValue, null);
            }
        };
    }

    /**
     * Shows the details view for the given record of the table.
     *
     * The default implementation of this method assumes there is an
     * id attribute on the record and passes it to {@link #showDetails(String)}.
     * Subclasses are free to override this behavior. Subclasses usually
     * will need to set the {@link #setDetailsView(Canvas) details view}
     * explicitly.
     *
     * @param record the record whose details are to be shown
     */
    public void showDetails(ListGridRecord record) {
        if (record == null) {
            throw new IllegalArgumentException("'record' parameter is null.");
        }

        String id = getId(record);
        showDetails(id);
    }

    /**
     * Returns the details canvas with information on the item given its list grid record.
     *
     * The default implementation of this method is to assume there is an
     * id attribute on the record and pass that ID to {@link #getDetailsView(String)}.
     * Subclasses are free to override this - which you usually want to do
     * if you know the full details of the item are stored in the record attributes
     * and thus help avoid making a round trip to the DB.
     *
     * @param record the record of the item whose details to be shown; ; null if empty details view should be shown.
     */
    public Canvas getDetailsView(ListGridRecord record) {
        String id = getId(record);
        return getDetailsView(id);
    }

    protected String getId(ListGridRecord record) {
        String id = null;
        if (record != null) {
            id = record.getAttribute("id");
        }
        if (id == null || id.length() == 0) {
            String msg = MSG.view_tableSection_error_noId(this.getClass().toString());
            CoreGUI.getErrorHandler().handleError(msg);
            throw new IllegalStateException(msg);
        }
        return id;
    }

    /**
     * Shows empty details for a new item being created.
     * This method is usually called when a user clicks a 'New' button.
     *
     * @see #showDetails(ListGridRecord)
     */
    public void newDetails() {
        History.newItem(basePath + "/0");
    }

    /**
     * Shows the details for an item has the given ID.
     * This method is usually called when a user goes to the details
     * page via a bookmark, double-cick on a list view row, or direct link.
     *
     * @param id the id of the row whose details are to be shown; Should be a valid id.
     *
     * @see #showDetails(ListGridRecord)
     *
     * @throws IllegalArgumentException if id is null or empty string
     */
    public void showDetails(String id) {
        if (id == null || id.length() == 0) {
            History.newItem(basePath + "/" + id);
        } else {
            String msg = MSG.view_tableSection_error_badId(this.getClass().toString(), id);
            CoreGUI.getErrorHandler().handleError(msg);
            throw new IllegalArgumentException(msg);
        }
    }

    /**
     * Returns the details canvas with information on the item that has the given ID.
     * Note that an empty details view should be returned if the id passed in is 0 (as would
     * be the case if a new item is to be created using the details view).
     *
     * @param id the id of the details to be shown; will be null if an empty details view should be shown.
     */
    public abstract Canvas getDetailsView(String id);

    @Override
    public void renderView(ViewPath viewPath) {
        this.basePath = viewPath.getPathToCurrent();

        if (!viewPath.isEnd()) {
            String id = viewPath.getCurrent().getPath();
            this.detailsView = getDetailsView(id);
            if (this.detailsView instanceof BookmarkableView) {
                ((BookmarkableView) this.detailsView).renderView(viewPath);
            }
            switchToDetailsView();
        } else {
            switchToTableView();
        }
    }

    protected String getBasePath() {
        return this.basePath;
    }

    /**
     * For use by subclasses that want to define their own details view.
     *
     * @param detailsView the new details view
     */
    protected void setDetailsView(Canvas detailsView) {
        this.detailsView = detailsView;
    }

    /**
     * Switches to viewing the details canvas, hiding the table. This does not
     * do anything with reloading data or switching to the selected row in the table;
     * this only changes the visibility of canvases.
     */
    protected void switchToDetailsView() {
        Canvas contents = getTableContents();

        // If the Table has not yet been initialized then ignore
        if (contents != null) {
            if (contents.isVisible()) {
                contents.animateHide(AnimationEffect.WIPE, new AnimationCallback() {
                    @Override
                    public void execute(boolean b) {
                        buildDetailsView();
                    }
                });
            } else {
                /*
                 * if the programmer chooses to go directly from the detailView in create-mode to the
                 * detailsView in edit-mode, the content canvas will already be hidden, which means the
                 * animateHide would be a no-op (the event won't fire).  this causes the detailsHolder
                 * to keep a reference to the previous detailsView (the one in create-mode) instead of the
                 * newly returned reference from getDetailsView(String) that was called when the renderView
                 * methods were called hierarchically down to render the new detailsView in edit-mode.
                 * therefore, we need to explicitly destroy what's already there (presumably the detailsView
                 * in create-mode), and then rebuild it (presumably the detailsView in edit-mode).
                 */
                SeleniumUtility.destroyMembers(detailsHolder);

                buildDetailsView();
            }
        }
    }

    private void buildDetailsView() {
        detailsView.setWidth100();
        detailsView.setHeight100();

        boolean isEditable = (detailsView instanceof DetailsView && ((DetailsView) detailsView).isEditable());
        if (!isEditable) {
            // Only add the "Back to List" button if the details are definitely not editable, because if they are
            // editable, a Cancel button should already be provided by the details view.
            BackButton backButton = new BackButton(extendLocatorId("BackButton"), MSG.view_tableSection_backButton(),
                basePath);
            detailsHolder.addMember(backButton);
            VLayout verticalSpacer = new LocatableVLayout(extendLocatorId("verticalSpacer"));
            verticalSpacer.setHeight(8);
            detailsHolder.addMember(verticalSpacer);
        }

        detailsHolder.addMember(detailsView);
        detailsHolder.animateShow(AnimationEffect.WIPE);
    }

    /**
     * Switches to viewing the table, hiding the details canvas.
     */
    protected void switchToTableView() {
        final Canvas contents = getTableContents();
        if (contents != null) {
            // If this is not the initial display of the table, refresh the table's data. Otherwise, a refresh would be
            // redundant, since the data was just loaded when the table was drawn.
            if (this.initialDisplay) {
                this.initialDisplay = false;
            } else {
                Log.debug("Refreshing data for Table [" + getClass().getName() + "]...");
                refresh();
            }
            if (detailsHolder != null && detailsHolder.isVisible()) {
                detailsHolder.animateHide(AnimationEffect.WIPE, new AnimationCallback() {
                    @Override
                    public void execute(boolean b) {
                        SeleniumUtility.destroyMembers(detailsHolder);

                        contents.animateShow(AnimationEffect.WIPE);
                    }
                });
            } else {
                contents.animateShow(AnimationEffect.WIPE);
            }
        }
    }



}
