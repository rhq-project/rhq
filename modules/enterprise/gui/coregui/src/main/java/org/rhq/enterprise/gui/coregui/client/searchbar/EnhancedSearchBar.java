/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.searchbar;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.events.MouseOutEvent;
import com.smartgwt.client.widgets.events.MouseOutHandler;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.FocusEvent;
import com.smartgwt.client.widgets.form.fields.events.FocusHandler;
import com.smartgwt.client.widgets.form.fields.events.IconClickEvent;
import com.smartgwt.client.widgets.form.fields.events.IconClickHandler;
import com.smartgwt.client.widgets.form.fields.events.KeyUpEvent;
import com.smartgwt.client.widgets.form.fields.events.KeyUpHandler;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.toolbar.ToolStrip;
import com.smartgwt.client.widgets.toolbar.ToolStripButton;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.search.SavedSearch;
import org.rhq.core.domain.search.SearchSubsystem;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.IconEnum;
import org.rhq.enterprise.gui.coregui.client.Messages;
import org.rhq.enterprise.gui.coregui.client.UserSessionManager;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.SearchGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.util.Log;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;

/**
 * The class defines the UI component of the search bar.
 * Behavior related to the retrieving/saving of searches is delegated to the Search Strategies.
 * The enumMaps confine us to just setting the search modes and the modes determine the strategies.
 * This is a little safer than typical strategy pattern where we can set any strategy regardless of the
 * search mode (in this case, the search mode and strategies can get out of sync). In fact,
 * there is no setter for the search strategy, as you cant set it is setup by the enumMap only.
 *
 * @author  Mike Thompson
 */
public class EnhancedSearchBar extends ToolStrip {
    private static final Messages MSG = CoreGUI.getMessages();
    private SearchSubsystem searchSubsystem;
    private ToolStripButton saveSearchButton;
    private TextItem searchTextItem;
    private ListGrid pickListGrid;
    private TextItem saveSearchTextItem;
    private final FavoritesSearchStrategy favoritesSearchStrategy;
    private final BasicSearchStrategy basicSearchStrategy;

    private final SearchGWTServiceAsync searchService = GWTServiceLookup.getSearchService();

    /**
     * The amount of time delayed before an actual search result is displayed.
     * So that we are not bombarding the server with ajax requests.
     */
    private static final int SEARCH_KEYUP_DELAY = 1000;

    private static final List<String> IGNORED_KEYS;

    static {
        IGNORED_KEYS = new ArrayList<String>(2);
        IGNORED_KEYS.add("Arrow_Down");
        IGNORED_KEYS.add("Arrow_Up");
    }

    enum SearchMode {
        BASIC_SEARCH_MODE, SAVED_SEARCH_MODE
    }

    private SearchMode searchMode = SearchMode.BASIC_SEARCH_MODE;

    /**
     * The enumMap will act as a simple state machine defining the mappings of search strategy behavior and
     * eliminating any if logic.
     */
    private EnumMap<SearchMode, AbstractSearchStrategy> searchStrategies = new EnumMap<SearchMode, AbstractSearchStrategy>(
        SearchMode.class);


    public EnhancedSearchBar(SearchSubsystem searchSubsystem, String initialSearchText) {
        if (null == searchSubsystem) {
            this.searchSubsystem = SearchSubsystem.RESOURCE; // default to resource
        } else {
            this.searchSubsystem = searchSubsystem;
        }
        setAutoHeight();
        setWidth100();
        addSpacer(40);

        searchTextItem = new TextItem("search", MSG.common_button_search());
        // now that we have searchComboBoxItem setup dependent objects
        favoritesSearchStrategy = new FavoritesSearchStrategy(this);
        basicSearchStrategy = new BasicSearchStrategy(this);
        // now we can fill our enumMap
        searchStrategies.put(SearchMode.BASIC_SEARCH_MODE, basicSearchStrategy);
        searchStrategies.put(SearchMode.SAVED_SEARCH_MODE, favoritesSearchStrategy);

        searchTextItem.setWidth(600);
        searchTextItem.setBrowserSpellCheck(false);

        pickListGrid = new ListGrid();

        searchTextItem.setShowPickerIcon(true);
        searchTextItem.setRedrawOnChange(true);
        // this changes it to autocomplete field from combobox
        searchTextItem.addKeyUpHandler(new KeyUpHandler() {
            @Override
            public void onKeyUp(final KeyUpEvent keyUpEvent) {
                Log.debug("onKeyUp search Mode: " + searchMode + " key: "+keyUpEvent.getKeyName());

                if (IGNORED_KEYS.contains(keyUpEvent.getKeyName())) {
                    return;
                }

                if (keyUpEvent.getKeyName().equals("Enter")) {
                    Log.debug("onKeyUp search Mode Enter key pressed");
                    getSearchStrategy().searchReturnKeyHandler(keyUpEvent);
                    searchTextItem.focusInItem();
                    pickListGrid.hide();
                }
                else {
                    Log.debug("Do searchKeyUpHandler...");
                    // add our own delay
                    new Timer(){
                        /**
                         * This method will be called when a timer fires. Override it to implement the timer's logic.
                         */
                        @Override
                        public void run() {
                            getSearchStrategy().searchKeyUpHandler(keyUpEvent);
                            pickListGrid.show();
                            showPickListGrid();
                        }
                    }.schedule(SEARCH_KEYUP_DELAY);
                }
            }
        });


        searchTextItem.setShowFocused(false);
        searchTextItem.addIconClickHandler(new IconClickHandler() {
            @Override
            public void onIconClick(IconClickEvent iconClickEvent) {
                // toggle
                if(pickListGrid.isVisible()){
                    pickListGrid.hide();
                }else {
                    populateInitialSearch();
                }
            }


        });
        searchTextItem.addFocusHandler(new FocusHandler() {
            @Override
            public void onFocus(FocusEvent event) {
                Log.debug("onFocus search Mode: " + searchMode);
                populateInitialSearch();
            }
        });
        searchTextItem.addClickHandler(new com.smartgwt.client.widgets.form.fields.events.ClickHandler() {
            @Override
            public void onClick(com.smartgwt.client.widgets.form.fields.events.ClickEvent clickEvent) {
                populateInitialSearch();
            }
        });
        configurePickListGrid();

        addFormItem(searchTextItem);

        saveSearchButton = new ToolStripButton();
        saveSearchButton.setIcon(IconEnum.STAR_OFF.getIcon16x16Path());
        saveSearchButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                toggleFavoriteSearch();
            }
        });
        addButton(saveSearchButton);

        saveSearchTextItem = new TextItem("savedSearchName");
        saveSearchTextItem.setShowTitle(false);
        saveSearchTextItem.setWidth(150);
        addFormItem(saveSearchTextItem);
        saveSearchTextItem.hide();

        saveSearchTextItem.addKeyUpHandler(new KeyUpHandler() {
            @Override
            public void onKeyUp(KeyUpEvent keyUpEvent) {
                if (keyUpEvent.getKeyName().equals("Enter")) {
                    saveFavoriteSearch();
                }
            }
        });
        pickListGrid.addMouseOutHandler(new MouseOutHandler() {
            @Override
            public void onMouseOut(MouseOutEvent mouseOutEvent) {
                //Log.debug("Mouseout x: "+ mouseOutEvent.getX());
                pickListGrid.hide();
            }
        });
        // set the default search provider
        switchToBasicSearchMode();

        this.draw();
    }
    private void populateInitialSearch() {
        getSearchStrategy().searchFocusHandler();
        pickListGrid.show();
        showPickListGrid();
    }
    private void saveFavoriteSearch() {
        Log.debug("Saving Favorite Search: " + saveSearchTextItem.getValueAsString());
        createSavedSearch(saveSearchTextItem.getValueAsString(), searchTextItem.getValueAsString());
        toggleFavoriteSearch();
    }

    public SearchMode getSearchMode() {
        return searchMode;
    }

    public void setSearchMode(SearchMode searchMode) {
        this.searchMode = searchMode;
    }

    private void configurePickListGrid() {
        pickListGrid.setCellHeight(getSearchStrategy().getCellHeight());
        pickListGrid.addRecordClickHandler(getSearchStrategy());
        pickListGrid.setCellFormatter(getSearchStrategy());
        pickListGrid.setShowHeader(false);
        pickListGrid.redraw();
    }

    private void showPickListGrid(){
        pickListGrid.setLeft(searchTextItem.getLeft()+307);
        pickListGrid.setTop(145);
        pickListGrid.setWidth(600);
        pickListGrid.setHeight(400);
        pickListGrid.redraw();
    }

    /**
     * This is the one method where the searchMode state is allowed to change.
     */
    public void toggleFavoriteSearch() {
        if (getSearchMode().equals(SearchMode.SAVED_SEARCH_MODE)) {
            switchToBasicSearchMode();
        } else {
            switchToSavedSearchMode();
        }

        Log.debug("toggleFavorites searchMode set to: " + searchMode);
        configurePickListGrid();
        showPickListGrid();

    }

    public void switchToBasicSearchMode() {
        setSearchMode(SearchMode.BASIC_SEARCH_MODE);
        saveSearchButton.setIcon(IconEnum.STAR_OFF.getIcon16x16Path());
        saveSearchTextItem.hide();
        configurePickListGrid();
        showPickListGrid();
    }

    public void switchToSavedSearchMode() {
        setSearchMode(SearchMode.SAVED_SEARCH_MODE);
        saveSearchButton.setIcon(IconEnum.STAR_ON.getIcon16x16Path());
        saveSearchTextItem.show();
        saveSearchTextItem.setValue(MSG.search_name_your_search());
        saveSearchTextItem.setSelectOnFocus(true);
        saveSearchTextItem.selectValue();
        configurePickListGrid();
        showPickListGrid();
    }


    public String getValue() {
        return searchTextItem.getValueAsString();
    }

    private void createSavedSearch(final String name, final String pattern) {
        Subject subject = UserSessionManager.getSessionSubject();
        SavedSearch newSavedSearch = new SavedSearch(searchSubsystem, name, pattern, subject);
        searchService.createSavedSearch(newSavedSearch, new AsyncCallback<Integer>() {
            @Override
            public void onSuccess(Integer newSavedSearchId) {
                Message message = new Message(MSG.search_successfully_saved_search(name),  Message.Severity.Info);
                CoreGUI.getMessageCenter().notify(message);
            }

            @Override
            public void onFailure(Throwable caught) {
                Message message = new Message(MSG.search_failed_to_save_search(name), Message.Severity.Error);
                CoreGUI.getMessageCenter().notify(message);
            }
        });
    }

    public AbstractSearchStrategy getSearchStrategy() {
        return searchStrategies.get(searchMode);
    }

    public ListGrid getPickListGrid() {
        return pickListGrid;
    }

    public TextItem getSearchTextItem() {
        return searchTextItem;
    }

    public TextItem getSaveSearchTextItem() {
        return saveSearchTextItem;
    }
}
