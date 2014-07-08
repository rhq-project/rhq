/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.coregui.client.components.carousel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.event.dom.client.KeyCodes;
import com.smartgwt.client.data.Criteria;
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
import com.smartgwt.client.widgets.events.DoubleClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.HiddenItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.SpinnerItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.form.fields.events.KeyPressEvent;
import com.smartgwt.client.widgets.form.fields.events.KeyPressHandler;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.LayoutSpacer;
import com.smartgwt.client.widgets.menu.IMenuButton;
import com.smartgwt.client.widgets.menu.Menu;
import com.smartgwt.client.widgets.menu.MenuItem;
import com.smartgwt.client.widgets.menu.events.MenuItemClickEvent;

import org.rhq.core.domain.search.SearchSubsystem;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.RefreshableView;
import org.rhq.coregui.client.components.buttons.BackButton;
import org.rhq.coregui.client.components.form.EnhancedSearchBarItem;
import org.rhq.coregui.client.util.enhanced.EnhancedHLayout;
import org.rhq.coregui.client.util.enhanced.EnhancedIButton;
import org.rhq.coregui.client.util.enhanced.EnhancedToolStrip;
import org.rhq.coregui.client.util.enhanced.EnhancedUtility;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;
import org.rhq.coregui.client.util.enhanced.EnhancedIButton.ButtonColor;

/**
 * Similar to (i.e. originally a copy of) Table but instead of encapsulating a ListGrid, it manages a list of 
 * {@link CarouselMember}s, offering horizontal presentation of the member canvases, high level filtering, and
 * other member-wide handling. See {@link BookmarkableCarousel} for a subclass providing master-detail support.
 * 
 * @author Jay Shaughnessy
 */
@SuppressWarnings("unchecked")
public abstract class Carousel extends EnhancedHLayout implements RefreshableView {

    private static final String FILTER_CAROUSEL_START = "CarouselStart";
    private static final String FILTER_CAROUSEL_SIZE = "CarouselSize";

    private EnhancedVLayout contents;

    private HLayout titleLayout;
    private Canvas titleComponent;
    private HTMLFlow titleCanvas;
    private String titleString;
    private List<String> titleIcons = new ArrayList<String>();
    private BackButton titleBackButton;

    private Canvas carouselDetails;

    private CarouselFilter filterForm;

    private EnhancedHLayout carouselHolder;

    private Label carouselInfo;

    private boolean showTitle = true;
    private boolean showFooter = true;
    private boolean showFooterRefresh = true;
    private boolean showFilterForm = true;

    private Criteria initialCriteria;
    private boolean initialCriteriaFixed = true;
    private boolean hideSearchBar = false;
    private String initialSearchBarSearchText = null;

    private List<CarouselActionInfo> carouselActions = new ArrayList<CarouselActionInfo>();
    private boolean carouselActionDisableOverride = false;
    protected List<Canvas> extraWidgetsAboveFooter = new ArrayList<Canvas>();
    protected List<Canvas> extraWidgetsInMainFooter = new ArrayList<Canvas>();
    private EnhancedToolStrip footer;
    private EnhancedToolStrip footerExtraWidgets;
    private EnhancedIButton refreshButton;
    private EnhancedIButton nextButton;
    private EnhancedIButton previousButton;
    private EnhancedIButton widthButton;

    // null means no start filter, start with highest 
    private Integer carouselStartFilter = null;
    // null means no end filter, end with lowest 
    private Integer carouselEndFilter = null;

    // null or < 0 indicates no limit to the carousel size. A size limit is always recommended to avoid unbounded view 
    private Integer carouselSizeFilter = getDefaultCarouselSize();
    private boolean carouselUsingFixedWidths = false;

    public Carousel() {
        this(null, null);
    }

    public Carousel(String titleString) {
        this(titleString, null);
    }

    public Carousel(String titleString, Criteria criteria) {
        super();

        setWidth100();
        setHeight100();
        setOverflow(Overflow.HIDDEN);

        this.titleString = titleString;
        this.initialCriteria = criteria;
    }

    /**
     * If this returns true, then even if a {@link #getSearchSubsystem() search subsystem}
     * is defined by the class, the search bar will not be shown.
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

    @Override
    protected void onInit() {
        super.onInit();

        contents = new EnhancedVLayout();
        contents.setWidth100();
        contents.setHeight100();

        addMember(contents);

        filterForm = new CarouselFilter(this);

        /*
         * carousel filters and search bar are currently mutually exclusive
         */

        if (getSearchSubsystem() == null) {
            configureCarouselFilters();

        } else {
            if (!this.hideSearchBar) {
                final EnhancedSearchBarItem searchFilter = new EnhancedSearchBarItem("search", getSearchSubsystem(),
                    getInitialSearchBarSearchText());
                setFilterFormItems(searchFilter);
            }
        }

        carouselHolder = new EnhancedHLayout();
        carouselHolder.setOverflow(Overflow.AUTO);
        carouselHolder.setWidth100();
        contents.addMember(carouselHolder);
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
            this.titleCanvas = new HTMLFlow();
            setTitleString(this.titleString);

            if (showTitle) {
                titleLayout = new EnhancedHLayout();
                titleLayout.setAutoHeight();
                titleLayout.setAlign(VerticalAlignment.BOTTOM);
                titleLayout.setMembersMargin(4);
                contents.addMember(titleLayout, 0);
            }

            if (null != carouselDetails) {
                contents.addMember(carouselDetails);
            }

            if (filterForm.hasContent()) {
                contents.addMember(filterForm);
            }

            contents.addMember(carouselHolder);

            // Footer

            // A second toolstrip that optionally appears before the main footer - it will contain extra widgets.
            // This is hidden from view unless extra widgets are actually added to the carousel above the main footer.
            this.footerExtraWidgets = new EnhancedToolStrip();
            footerExtraWidgets.setPadding(5);
            footerExtraWidgets.setWidth100();
            footerExtraWidgets.setMembersMargin(15);
            footerExtraWidgets.hide();
            contents.addMember(footerExtraWidgets);

            this.footer = new EnhancedToolStrip();
            footer.setPadding(5);
            footer.setWidth100();
            footer.setMembersMargin(15);
            contents.addMember(footer);

            // The ListGrid has been created and configured
            // Now give subclasses a chance to configure the carousel
            configureCarousel();

            Label carouselInfo = new Label();
            carouselInfo.setWrap(false);
            setCarouselInfo(carouselInfo);

            if (showTitle) {
                drawTitle();
            }

            if (showFooter) {
                drawFooter();
            }
        } catch (Exception e) {
            CoreGUI.getErrorHandler().handleError(MSG.view_table_drawFail(this.toString()), e);
        }
    }

    @Override
    public void destroy() {
        EnhancedUtility.destroyMembers(contents);
        super.destroy();
    }

    private void drawTitle() {
        for (String headerIcon : titleIcons) {
            Img img = new Img(headerIcon, 24, 24);
            img.setPadding(4);
            titleLayout.addMember(img);
        }

        if (null != titleBackButton) {
            titleLayout.addMember(titleBackButton);
        }

        titleLayout.addMember(titleCanvas);

        if (titleComponent != null) {
            titleLayout.addMember(new LayoutSpacer());
            titleLayout.addMember(titleComponent);
        }
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

        for (final CarouselActionInfo carouselAction : carouselActions) {

            if (null == carouselAction.getValueMap()) {
                // button action
                IButton button = new EnhancedIButton(carouselAction.getTitle());
                button.setDisabled(true);
                button.setOverflow(Overflow.VISIBLE);
                button.addClickHandler(new ClickHandler() {
                    public void onClick(ClickEvent clickEvent) {
                        disableAllFooterControls();
                        if (carouselAction.confirmMessage != null) {
                            String message = carouselAction.confirmMessage.replaceAll("\\#", "TODO:");

                            SC.ask(message, new BooleanCallback() {
                                public void execute(Boolean confirmed) {
                                    if (confirmed) {
                                        carouselAction.action.executeAction(null);
                                    } else {
                                        refreshCarouselInfo();
                                    }
                                }
                            });
                        } else {
                            carouselAction.action.executeAction(null);
                        }
                    }
                });

                carouselAction.actionCanvas = button;
                footer.addMember(button);

            } else {
                // menu action
                Menu menu = new Menu();
                final Map<String, ? extends Object> menuEntries = carouselAction.getValueMap();
                for (final String key : menuEntries.keySet()) {
                    MenuItem item = new MenuItem(key);
                    item.addClickHandler(new com.smartgwt.client.widgets.menu.events.ClickHandler() {
                        public void onClick(MenuItemClickEvent event) {
                            disableAllFooterControls();
                            carouselAction.getAction().executeAction(menuEntries.get(key));
                        }
                    });
                    menu.addItem(item);
                }

                IMenuButton menuButton = new IMenuButton(carouselAction.getTitle());
                menuButton.setMenu(menu);
                menuButton.setDisabled(true);
                menuButton.setOverflow(Overflow.VISIBLE);
                menuButton.setShowMenuBelow(false);

                carouselAction.actionCanvas = menuButton;
                footer.addMember(menuButton);
            }
        }

        for (Canvas extraWidgetCanvas : extraWidgetsInMainFooter) {
            footer.addMember(extraWidgetCanvas);
        }

        footer.addMember(new LayoutSpacer());

        widthButton = new EnhancedIButton(MSG.common_button_fixedWidth());
        widthButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                carouselUsingFixedWidths = !carouselUsingFixedWidths;
                widthButton.setTitle(carouselUsingFixedWidths ? MSG.common_button_scaleToFit() : MSG
                    .common_button_fixedWidth());
                buildCarousel(true);
            }
        });
        footer.addMember(widthButton);

        if (isShowFooterRefresh()) {
            this.refreshButton = new EnhancedIButton(MSG.common_button_refresh());
            refreshButton.addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent clickEvent) {
                    disableAllFooterControls();
                    refresh();
                }
            });
            footer.addMember(refreshButton);
        }

        previousButton = new EnhancedIButton(MSG.common_button_previous());
        previousButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                disableAllFooterControls();
                previous();
            }
        });
        footer.addMember(previousButton);

        nextButton = new EnhancedIButton(MSG.common_button_next(), ButtonColor.BLUE);
        nextButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                disableAllFooterControls();
                next();
            }
        });
        footer.addMember(nextButton);

        footer.addMember(carouselInfo);

        // Ensure buttons are initially set correctly.
        refreshCarouselInfo();
    }

    private void previous() {
        if (null == carouselSizeFilter) {
            carouselSizeFilter = getDefaultCarouselSize();
        }

        if (null != carouselStartFilter) {
            int newEnd = carouselStartFilter + 1;
            setCarouselEndFilter(newEnd);

            // it's ok if this is higher than the current max, the actual fetch will make sure the
            // values are sane.
            setCarouselStartFilter(carouselStartFilter + carouselSizeFilter);
        }

        buildCarousel(true);
    }

    private void next() {
        if (null == carouselSizeFilter) {
            carouselSizeFilter = getDefaultCarouselSize();
        }

        if (null != carouselEndFilter) {
            int newStart = carouselEndFilter - 1;
            newStart = (newStart < carouselSizeFilter) ? carouselSizeFilter : newStart;
            setCarouselStartFilter(newStart);
        }

        setCarouselEndFilter(null);

        buildCarousel(true);
    }

    protected abstract int getDefaultCarouselSize();

    protected abstract String getCarouselMemberFixedWidth();

    protected abstract String getCarouselStartFilterLabel();

    protected abstract String getCarouselSizeFilterLabel();

    private void disableAllFooterControls() {
        for (CarouselActionInfo action : carouselActions) {
            action.actionCanvas.disable();
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
     * created but before it has been drawn to the DOM. This is also the proper place to add
     * actions so that they're rendered in the footer.
     */
    protected void configureCarousel() {
        return;
    }

    /**
     * If not overriden this will append the standard carousel filters to those already supplied. To ensure
     * the supplied form items end a row, call {@link FormItem#setEndRow(Boolean)} as needed. The filter form is 
     * set to use 6 columns, which allows the carousle filters to fit on one row.
     * @param formItems
     */
    public void setFilterFormItems(FormItem... formItems) {
        // since Arrays.copyOf is unsupported...
        FormItem[] carouselFormItems = new FormItem[2 + formItems.length];
        int i = 0;
        for (FormItem item : formItems) {
            carouselFormItems[i++] = item;
        }

        // drift file path filter
        SpinnerItem startFilter = new SpinnerItem(FILTER_CAROUSEL_START, getCarouselStartFilterLabel());
        startFilter.setMin(1);
        //TextItem startFilter = new TextItem(FILTER_CAROUSEL_START, getCarouselStartFilterLabel());
        TextItem numFilter = new TextItem(FILTER_CAROUSEL_SIZE, getCarouselSizeFilterLabel());
        carouselFormItems[i++] = startFilter;
        carouselFormItems[i++] = numFilter;

        this.filterForm.setNumCols(4);
        this.filterForm.setItems(carouselFormItems);
    }

    /**
     * Overriding components can use this as a chance to add {@link FormItem}s which will filter
     * the carousel members that display their data. If not overriden the standard carousel filters are applied.
     */
    protected void configureCarouselFilters() {
        setFilterFormItems();
    }

    /**
     * Set the Carousel's title string. This will subsequently call {@link #updateTitleCanvas(String)}.
     * @param titleString
     */
    public void setTitleString(String titleString) {
        this.titleString = titleString;
        if (this.titleCanvas != null) {
            updateTitleCanvas(titleString);
        }
    }

    public Canvas getTitleCanvas() {
        return this.titleCanvas;
    }

    /**
     * To set the Carousel's title, call {@link #setTitleString(String)}. This is primarily declared for purposes of
     * override.
     * @param titleString
     */
    public void updateTitleCanvas(String titleString) {
        if (titleString == null) {
            titleString = "";
        }
        if (titleString.length() > 0) {
            titleCanvas.setWidth100();
            titleCanvas.setHeight(35);
            titleCanvas.setContents(titleString);
            titleCanvas.setPadding(4);
            titleCanvas.setStyleName("HeaderLabel");
        } else {
            titleCanvas.setWidth100();
            titleCanvas.setHeight(0);
            titleCanvas.setContents(null);
            titleCanvas.setPadding(0);
            titleCanvas.setStyleName("normal");
        }

        titleCanvas.markForRedraw();
    }

    /**
     * Returns the encompassing canvas that contains all content for this component.
     * This content includes the carousel members, the buttons, etc.
     */
    public Canvas getCarouselContents() {
        return this.contents;
    }

    public boolean isShowTitle() {
        return showTitle;
    }

    public void setShowTitle(boolean showTitle) {
        this.showTitle = showTitle;
    }

    public boolean isShowFooter() {
        return showFooter;
    }

    public void setShowFooter(boolean showFooter) {
        this.showFooter = showFooter;
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

        // If this carousel has a filter form (filters OR search bar),
        // we need to refresh it as per the filtering, combined with any fixed criteria.
        if (this.filterForm != null && this.filterForm.hasContent()) {

            criteria = this.filterForm.getValuesAsCriteria();

            if (this.initialCriteriaFixed) {
                if (criteria != null) {
                    if (this.initialCriteria != null) {
                        // There is fixed criteria - add it to the filter form criteria.
                        addCriteria(criteria, this.initialCriteria);
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

    //TODO move to a utility
    // SmartGWT 2.4's version of Criteria.addCriteria for some reason doesn't have else clauses for the array types
    // and it doesn't handle Object types properly (seeing odd behavior because of this), so this method explicitly
    // supports adding array types and Objects.
    // This method takes the src criteria and adds it to the dest criteria.
    public static void addCriteria(Criteria dest, Criteria src) {
        Map otherMap = src.getValues();
        Set otherKeys = otherMap.keySet();
        for (Iterator i = otherKeys.iterator(); i.hasNext();) {
            String field = (String) i.next();
            Object value = otherMap.get(field);

            if (value instanceof Integer) {
                dest.addCriteria(field, (Integer) value);
            } else if (value instanceof Float) {
                dest.addCriteria(field, (Float) value);
            } else if (value instanceof String) {
                dest.addCriteria(field, (String) value);
            } else if (value instanceof Date) {
                dest.addCriteria(field, (Date) value);
            } else if (value instanceof Boolean) {
                dest.addCriteria(field, (Boolean) value);
            } else if (value instanceof Integer[]) {
                dest.addCriteria(field, (Integer[]) value);
            } else if (value instanceof Double[]) {
                dest.addCriteria(field, (Double[]) value);
            } else if (value instanceof String[]) {
                dest.addCriteria(field, (String[]) value);
            } else {
                // this is the magic piece - we need to get attrib as an object and set that value
                dest.setAttribute(field, src.getAttributeAsObject(field));
            }
        }
    }

    public void setCarouselDetails(Canvas carouselDetails) {
        this.carouselDetails = carouselDetails;
    }

    public Canvas getCarouselDetails() {
        return carouselDetails;
    }

    public void setTitleComponent(Canvas canvas) {
        this.titleComponent = canvas;
    }

    /**
     * Note: To prevent user action while a current action completes, all widgets on the footer are disabled
     * when footer actions take place, typically a button click.  It is up to the action to ensure the page
     * (via refresh() or CoreGUI.refresh()) or footer (via refreshCarouselActions) are refreshed as needed at action
     * completion. Failure to do so may leave the widgets disabled.
     */
    public void addCarouselAction(String title, CarouselAction action) {
        this.addCarouselAction(title, null, null, action);
    }

    /**
     * Note: To prevent user action while a current action completes, all widgets on the footer are disabled
     * when footer actions take place, typically a button click.  It is up to the action to ensure the page
     * (via refresh() or CoreGUI.refresh()) or footer (via refreshCarouselActions) are refreshed as needed at action
     * completion. Failure to do so may leave the widgets disabled.
     */
    public void addCarouselAction(String title, String confirmation, CarouselAction action) {
        this.addCarouselAction(title, confirmation, null, action);
    }

    /**
     * Note: To prevent user action while a current action completes, all widgets on the footer are disabled
     * when footer actions take place, typically a button click.  It is up to the action to ensure the page
     * (via refresh() or CoreGUI.refresh()) or footer (via refreshCarouselActions) are refreshed as needed at action
     * completion. Failure to do so may leave the widgets disabled.
     */
    public void addCarouselAction(String title, String confirmation, LinkedHashMap<String, ? extends Object> valueMap,
        CarouselAction action) {
        CarouselActionInfo info = new CarouselActionInfo(title, confirmation, valueMap, action);
        carouselActions.add(info);
    }

    public void addCarouselMember(Canvas member) {
        member.setWidth(carouselUsingFixedWidths ? getCarouselMemberFixedWidth() : "*");
        this.carouselHolder.addMember(member);
    }

    public void setListGridDoubleClickHandler(DoubleClickHandler handler) {
        //doubleClickHandler = handler;
    }

    /**
     * Adds extra widgets to the bottom of the carousel view.
     * <br/><br/>
     * Note: To prevent user action while a current action completes, all widgets on the footer are disabled
     * when footer actions take place, typically a button click.  It is up to the action to ensure the page
     * (via refresh() or CoreGUI.refresh()) or footer (via refreshCarouselActions) are refreshed as needed at action
     * completion. Failure to do so may leave the widgets disabled.
     *
     * @param widget the new widget to add to the view
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

    public void setHeaderIcon(String headerIcon) {
        if (this.titleIcons.size() > 0) {
            this.titleIcons.clear();
        }
        addHeaderIcon(headerIcon);
    }

    public void addHeaderIcon(String headerIcon) {
        this.titleIcons.add(headerIcon);
    }

    public void setTitleBackButton(BackButton backButton) {
        this.titleBackButton = backButton;
    }

    /**
     * By default, all actions have buttons that are enabled or
     * disabled based on if and how many rows are selected. There are
     * times when you don't want the user to be able to press action
     * buttons regardless of which rows are selected. This method let's
     * you set this override-disable flag.
     * 
     * Note: this also effects the double-click handler - if this disable override
     * is on, the double-click handler is not called.
     * 
     * @param disabled if true, all action buttons will be disabled
     *                 if false, action buttons will be enabled based on their predefined
     *                 selection enablement rule.
     */
    public void setCarouselActionDisableOverride(boolean disabled) {
        this.carouselActionDisableOverride = disabled;
        refreshCarouselInfo();
    }

    public boolean getCarouselActionDisableOverride() {
        return this.carouselActionDisableOverride;
    }

    /**
     * Refreshes the members, filtered by any fixed criteria, as well as any user-specified filters.
     */
    public void refresh() {
        Criteria criteria = getCurrentCriteria();

        Map<String, Object> criteriaMap = (criteria != null) ? criteria.getValues() : Collections
            .<String, Object> emptyMap();

        try {
            carouselSizeFilter = Integer.valueOf((String) criteriaMap.get(FILTER_CAROUSEL_SIZE));
        } catch (Exception e) {
            carouselSizeFilter = null;
        }

        try {
            carouselStartFilter = (Integer) criteriaMap.get(FILTER_CAROUSEL_START);
        } catch (Exception e) {
            carouselStartFilter = null;
        }

        // on refresh remove any end filter as the criteria may have changed completely
        carouselEndFilter = null;

        // Any change to filters means we have to rebuild the carousel because the set of members may change, because
        // "empty" members (i.e. members whose relevant data has been completely filtered) may be omitted completely
        // from the carousel.
        buildCarousel(true);

        // TODO: it would be best if this was actually called after the async return of the member refreshes
        refreshCarouselInfo();
    }

    protected void buildCarousel(boolean isRefresh) {
        if (null != carouselHolder) {
            carouselHolder.destroyMembers();
        }
    }

    public void refreshCarouselInfo() {
        if (this.showFooter && (this.carouselHolder != null)) {
            if (this.carouselActionDisableOverride) {
                //this.listGrid.setSelectionType(SelectionStyle.NONE);
            } else {
                //this.listGrid.setSelectionType(getDefaultSelectionStyle());
            }

            //int selectionCount = this.listGrid.getSelectedRecords().length;
            for (CarouselActionInfo carouselAction : this.carouselActions) {
                if (carouselAction.actionCanvas != null) { // if null, we haven't initialized our buttons yet, so skip this
                    boolean enabled = (!this.carouselActionDisableOverride && carouselAction.action.isEnabled());
                    carouselAction.actionCanvas.setDisabled(!enabled);
                }
            }
            for (Canvas extraWidget : this.extraWidgetsAboveFooter) {
                extraWidget.enable();
                if (extraWidget instanceof CarouselWidget) {
                    ((CarouselWidget) extraWidget).refresh(carouselHolder.getMembers());
                }
            }
            for (Canvas extraWidget : this.extraWidgetsInMainFooter) {
                extraWidget.enable();
                if (extraWidget instanceof CarouselWidget) {
                    ((CarouselWidget) extraWidget).refresh(carouselHolder.getMembers());
                }
            }

            if (isShowFooterRefresh() && this.refreshButton != null) {
                this.refreshButton.enable();
            }
        }
    }

    // -------------- Inner utility classes ------------- //

    /**
     * A subclass of SmartGWT's DynamicForm widget that provides a more convenient interface for filtering a 
     * {@link Carousel} of data.
     *
     * @author Joseph Marques 
     */
    private static class CarouselFilter extends DynamicForm implements KeyPressHandler, ChangedHandler,
        com.google.gwt.event.dom.client.KeyPressHandler {

        private Carousel carousel;
        private EnhancedSearchBarItem searchBarItem;
        private HiddenItem hiddenItem;

        public CarouselFilter(Carousel carousel) {
            super();
            setIsGroup(true);
            setGroupTitle(MSG.common_label_filters());
            setWidth100();
            setPadding(5);
            this.carousel = carousel;
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
                } else if (nextFormItem instanceof EnhancedSearchBarItem) {
                    //searchBarItem = (SearchBarItem) nextFormItem;
                    //searchBarItem.getSearchBar().addKeyPressHandler(this);
                    String name = searchBarItem.getName();
                    searchBarItem.setName(name + "_hidden");
                    hiddenItem = new HiddenItem(name);
                    //hiddenItem.setValue(searchBarItem.getSearchBar().getValue());
                }
            }

            if (hiddenItem != null) {
                FormItem[] tmpItems = new FormItem[items.length + 1];
                System.arraycopy(items, 0, tmpItems, 0, items.length);
                tmpItems[items.length] = hiddenItem;
                items = tmpItems;
            }

            super.setItems(items);
        }

        private void fetchFilteredCarouselData() {
            carousel.refresh();
        }

        public void onKeyPress(KeyPressEvent event) {
            if (event.getKeyName().equals("Enter") == false) {
                return;
            }
            fetchFilteredCarouselData();
        }

        public void onChanged(ChangedEvent event) {
            fetchFilteredCarouselData();
        }

        public boolean hasContent() {
            return super.getFields().length != 0;
        }

        @Override
        public void onKeyPress(com.google.gwt.event.dom.client.KeyPressEvent event) {
            if (event.getCharCode() != KeyCodes.KEY_ENTER) {
                return;
            }
            // TODO: figure out why this event is being sent twice
            //hiddenItem.setValue(searchBarItem.getSearchBar().getValue());
            fetchFilteredCarouselData();
        }
    }

    public static class CarouselActionInfo {
        private String title;
        private String confirmMessage;
        private LinkedHashMap<String, ? extends Object> valueMap;
        private CarouselAction action;
        private Canvas actionCanvas;

        protected CarouselActionInfo(String title, String confirmMessage,
            LinkedHashMap<String, ? extends Object> valueMap, CarouselAction action) {

            this.title = title;
            this.confirmMessage = confirmMessage;
            this.valueMap = valueMap;
            this.action = action;
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

        public CarouselAction getAction() {
            return action;
        }

        public void setAction(CarouselAction action) {
            this.action = action;
        }

    }

    public boolean isShowFooterRefresh() {
        return showFooterRefresh;
    }

    public void setShowFooterRefresh(boolean showFooterRefresh) {
        this.showFooterRefresh = showFooterRefresh;
    }

    public Label getCarouselInfo() {
        return carouselInfo;
    }

    public void setCarouselInfo(Label carouselInfo) {
        this.carouselInfo = carouselInfo;
    }

    public boolean isShowFilterForm() {
        return showFilterForm;
    }

    public void setShowFilterForm(boolean showFilterForm) {
        this.showFilterForm = showFilterForm;
    }

    /*
     * by default, no search bar is shown above this.  if this represents a subsystem that is capable
     * of search, return the specific object here.
     */
    protected SearchSubsystem getSearchSubsystem() {
        return null;
    }

    protected Integer getCarouselStartFilter() {
        return carouselStartFilter;
    }

    protected Integer getCarouselStartFilterMax() {
        Double max = ((SpinnerItem) this.filterForm.getItem(FILTER_CAROUSEL_START)).getMax();
        return (null == max) ? null : max.intValue();
    }

    protected void setCarouselStartFilterMax(Integer startFilterMax) {
        SpinnerItem spinner = (SpinnerItem) this.filterForm.getItem(FILTER_CAROUSEL_START);

        spinner.setMax((Integer) ((null == startFilterMax || startFilterMax < 0) ? null : startFilterMax));
    }

    protected void setCarouselStartFilter(Integer carouselStartFilter) {
        this.carouselStartFilter = carouselStartFilter;

        if (null != carouselStartFilter) {
            this.filterForm.getItem(FILTER_CAROUSEL_START).setValue(String.valueOf(carouselStartFilter));
            this.filterForm.getItem(FILTER_CAROUSEL_START).redraw();
        }
    }

    protected Integer getCarouselEndFilter() {
        return carouselEndFilter;
    }

    protected void setCarouselEndFilter(Integer carouselEndFilter) {
        this.carouselEndFilter = carouselEndFilter;
    }

    protected Integer getCarouselSizeFilter() {
        return carouselSizeFilter;
    }

    protected void setCarouselSizeFilter(Integer carouselSizeFilter) {
        this.carouselSizeFilter = carouselSizeFilter;
        this.filterForm.getItem(FILTER_CAROUSEL_SIZE).setValue(String.valueOf(carouselSizeFilter));
        this.filterForm.getItem(FILTER_CAROUSEL_SIZE).redraw();
    }

    protected boolean isCarouselUsingFixedWidths() {
        return carouselUsingFixedWidths;
    }

    protected void setCarouselUsingFixedWidths(boolean carouselUsingFixedWidths) {
        this.carouselUsingFixedWidths = carouselUsingFixedWidths;
    }

    protected void setFilter(String name, String value) {
        FormItem item = this.filterForm.getItem(name);
        if (null != item) {
            item.setValue(value);
        }
    }

    protected interface CarouselAction {

        /**
         * Returns true if the action should be enabled based on the currently selected record(s).
         *
         * @param selection the currently selected record(s)
         *
         * @return true if the action should be enabled based on the currently selected record(s)
         */
        boolean isEnabled(); //TODO add arg

        /**
         * Execute the action with the currently selected record(s) as the target(s).
         *
         * @param selection the currently selected record(s)
         * @param actionValue a value optionally supplied by the action (for example, a menuItem action's selection)
         */
        void executeAction(Object actionValue); //TODO add arg
    }
}
