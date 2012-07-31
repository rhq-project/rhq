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

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.TextMatchStyle;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.fields.ComboBoxItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.FocusEvent;
import com.smartgwt.client.widgets.form.fields.events.FocusHandler;
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
    private Integer currentSearchId = 0;
    private ToolStripButton saveSearchButton;
    private ComboBoxItem searchComboboxItem;
    private ListGrid pickListGrid;
    private TextItem saveSearchTextItem;
    private final FavoritesSearchStrategy favoritesSearchStrategy;
    private final BasicSearchStrategy basicSearchStrategy;

    private final SearchGWTServiceAsync searchService = GWTServiceLookup.getSearchService();

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

    public EnhancedSearchBar(SearchSubsystem searchSubsystem) {
        this(searchSubsystem, null);
    }

    public EnhancedSearchBar(SearchSubsystem searchSubsystem, String initialSearchText) {
        if (null == searchSubsystem) {
            this.searchSubsystem = SearchSubsystem.RESOURCE; // default to resource
        } else {
            this.searchSubsystem = searchSubsystem;
        }
        setAutoHeight();
        setWidth100();
        addSpacer(40);

        searchComboboxItem = new ComboBoxItem("search", MSG.common_button_search());
        // now that we have searchComboBoxItem setup dependent objects
        favoritesSearchStrategy = new FavoritesSearchStrategy(this);
        basicSearchStrategy = new BasicSearchStrategy(this);
        // now we can fill our enumMap
        searchStrategies.put(SearchMode.BASIC_SEARCH_MODE, basicSearchStrategy);
        searchStrategies.put(SearchMode.SAVED_SEARCH_MODE, favoritesSearchStrategy);

        searchComboboxItem.setWidth(670);
        searchComboboxItem.setBrowserSpellCheck(false);
        //we manually fetch each time we update the picklist values. That makes the delay setting meaningless.
        searchComboboxItem.setAutoFetchData(false);
        searchComboboxItem.setFetchDelay(300); // I'm not sure if this has an affect with no autoFetch

        pickListGrid = new ListGrid();
        configureCommonHandlers();

        searchComboboxItem.setTextMatchStyle(TextMatchStyle.SUBSTRING);
        searchComboboxItem.setShowPickListOnKeypress(true);
        searchComboboxItem.setRedrawOnChange(true);
        // this changes it to autocomplete field from combobox
        searchComboboxItem.setShowPickerIcon(true);
        searchComboboxItem.addKeyUpHandler(new KeyUpHandler() {
            @Override
            public void onKeyUp(KeyUpEvent keyUpEvent) {
                Log.debug("onKeyUp search Mode: " + searchMode);

                if (IGNORED_KEYS.contains(keyUpEvent.getKeyName())) {
                    return;
                }

                if (keyUpEvent.getKeyName().equals("Enter")) {
                    getSearchStrategy().searchReturnKeyHandler(keyUpEvent);
                    searchComboboxItem.focusInItem();
                } else {
                    getSearchStrategy().searchKeyUpHandler(keyUpEvent);
                }
            }
        });

        searchComboboxItem.addFocusHandler(new FocusHandler() {
            @Override
            public void onFocus(FocusEvent event) {
                Log.debug("onFocus search Mode: " + searchMode);

                getSearchStrategy().searchFocusHandler(event);
            }
        });

        searchComboboxItem.setPickListProperties(pickListGrid);
        addFormItem(searchComboboxItem);

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

        // set the default search provider
        switchToBasicSearchMode();

        this.draw();
    }

    private void saveFavoriteSearch() {
        Log.debug("Saving Favorite Search: " + saveSearchTextItem.getValueAsString());
        createSavedSearch(saveSearchTextItem.getValueAsString(), searchComboboxItem.getValueAsString());
        toggleFavoriteSearch();
    }

    public SearchMode getSearchMode() {
        return searchMode;
    }

    public void setSearchMode(SearchMode searchMode) {
        this.searchMode = searchMode;
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
        configureCommonHandlers();

    }

    public void switchToBasicSearchMode() {
        setSearchMode(SearchMode.BASIC_SEARCH_MODE);
        saveSearchButton.setIcon(IconEnum.STAR_OFF.getIcon16x16Path());
        saveSearchTextItem.hide();
        configureCommonHandlers();
    }

    public void switchToSavedSearchMode() {
        setSearchMode(SearchMode.SAVED_SEARCH_MODE);
        saveSearchButton.setIcon(IconEnum.STAR_ON.getIcon16x16Path());
        saveSearchTextItem.show();
        saveSearchTextItem.setValue(MSG.search_name_your_search());
        saveSearchTextItem.setSelectOnFocus(true);
        saveSearchTextItem.selectValue();
        configureCommonHandlers();
    }

    private void configureCommonHandlers() {
        pickListGrid.setCellHeight(getSearchStrategy().getCellHeight());
        pickListGrid.addRecordClickHandler(getSearchStrategy());
        pickListGrid.setCellFormatter(getSearchStrategy());
        pickListGrid.redraw();
    }

    public String getValue() {
        return searchComboboxItem.getValueAsString();
    }

    private void createSavedSearch(final String name, final String pattern) {
        Subject subject = UserSessionManager.getSessionSubject();
        SavedSearch newSavedSearch = new SavedSearch(searchSubsystem, name, pattern, subject);
        searchService.createSavedSearch(newSavedSearch, new AsyncCallback<Integer>() {
            @Override
            public void onSuccess(Integer newSavedSearchId) {
                currentSearchId = newSavedSearchId;
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

    public ComboBoxItem getSearchComboboxItem() {
        return searchComboboxItem;
    }

    public TextItem getSaveSearchTextItem() {
        return saveSearchTextItem;
    }
}
