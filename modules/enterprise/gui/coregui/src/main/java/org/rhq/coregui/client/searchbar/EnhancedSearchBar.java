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
package org.rhq.coregui.client.searchbar;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.form.fields.events.FocusHandler;
import com.smartgwt.client.widgets.form.fields.events.BlurHandler;
import com.smartgwt.client.widgets.form.fields.events.FocusEvent;
import com.smartgwt.client.widgets.form.fields.events.BlurEvent;
import com.smartgwt.client.core.Rectangle;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.KeyUpEvent;
import com.smartgwt.client.widgets.form.fields.events.KeyUpHandler;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.toolbar.ToolStripButton;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.search.SavedSearch;
import org.rhq.core.domain.search.SearchSubsystem;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.IconEnum;
import org.rhq.coregui.client.Messages;
import org.rhq.coregui.client.UserSessionManager;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.gwt.SearchGWTServiceAsync;
import org.rhq.coregui.client.util.Log;
import org.rhq.coregui.client.util.enhanced.EnhancedToolStrip;
import org.rhq.coregui.client.util.message.Message;

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
public class EnhancedSearchBar extends EnhancedToolStrip {
    private static final Messages MSG = CoreGUI.getMessages();
    private static final int SEARCH_WIDTH = 300;
    private static final int SAVED_SEARCH_WIDTH = 150;
    private static final int PICKLIST_HEIGHT = 500;
    private SearchSubsystem searchSubsystem;
    private ToolStripButton searchTextButton;
    private ToolStripButton saveSearchButton;
    private TextItem searchTextItem;
    private ListGrid pickListGrid;
    private TextItem saveSearchTextItem;
    private final FavoritesSearchStrategy favoritesSearchStrategy;
    private final BasicSearchStrategy basicSearchStrategy;
    private String lastSearchTerm;
    private Timer searchDelayTimer;

    private final SearchGWTServiceAsync searchService = GWTServiceLookup.getSearchService();

    /**
     * The amount of time delayed before an actual search result is displayed.
     * So that we are not bombarding the server with ajax requests.
     */
    private static final int SEARCH_KEYUP_DELAY = 1000;

    private static final List<String> IGNORED_KEYS;

    static {
        IGNORED_KEYS = new ArrayList<String>();
        IGNORED_KEYS.add("Arrow_Down");
        IGNORED_KEYS.add("Arrow_Up");
        IGNORED_KEYS.add("Arrow_Left");
        IGNORED_KEYS.add("Arrow_Right");
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

        setOverflow(Overflow.VISIBLE);
        setAutoHeight();
        setWidth100();
        setAlign(Alignment.LEFT);
        favoritesSearchStrategy = new FavoritesSearchStrategy(this);
        basicSearchStrategy = new BasicSearchStrategy(this);
        // now we can fill our enumMap
        searchStrategies.put(SearchMode.BASIC_SEARCH_MODE, basicSearchStrategy);
        searchStrategies.put(SearchMode.SAVED_SEARCH_MODE, favoritesSearchStrategy);

        // set the default search provider
        setSearchMode(SearchMode.BASIC_SEARCH_MODE);

        pickListGrid = new ListGrid();
        configurePickListGrid();

        DynamicForm searchTextForm = new DynamicForm();
        searchTextForm.setStyleName("searchBar");
        searchTextForm.setAutoWidth();
        searchTextForm.setNumCols(1);
        searchTextItem = new TextItem("search", MSG.common_button_search());
        searchTextItem.setShowTitle(false);
        searchTextItem.setWidth(SEARCH_WIDTH);
        searchTextItem.setBrowserSpellCheck(false);

        searchTextItem.addKeyUpHandler(new KeyUpHandler() {

            public void onKeyUp(final KeyUpEvent keyUpEvent) {
                Log.debug("onKeyUp search Mode: " + searchMode + " key: " + keyUpEvent.getKeyName());
                keyUpEvent.cancel();

                if (IGNORED_KEYS.contains(keyUpEvent.getKeyName())) {
                    return;
                }

                if (keyUpEvent.getKeyName().equals("Enter")) {
                    if (getSearchMode().equals(SearchMode.SAVED_SEARCH_MODE)) {
                        return;
                    }

                    String currentSearchTerm = (String) keyUpEvent.getItem().getValue();
                    currentSearchTerm = (null == currentSearchTerm) ? "" : currentSearchTerm; // avoid NPEs
                    Log.debug("onKeyUp search Mode Enter key pressed");

                    // stop any duplicate searches
                    if (!currentSearchTerm.equalsIgnoreCase(lastSearchTerm)) {
                        getSearchStrategy().searchReturnKeyHandler(keyUpEvent);
                        searchTextItem.focusInItem();
                        lastSearchTerm = currentSearchTerm;
                    } else {
                      hideList();
                    }
                } else if (keyUpEvent.getKeyName().equals("Escape")) {
                    searchTextItem.setValue(lastSearchTerm);
                    hideList();
                } else {
                    if (null == searchDelayTimer) {
                        searchDelayTimer = new Timer() {

                            public void run() {
                                getSearchStrategy().searchKeyUpHandler(keyUpEvent);
                                pickListGrid.show();
                            }
                        };
                    } else {
                        searchDelayTimer.cancel();
                    }

                    // wait for some typing quiet time before performing a search
                    searchDelayTimer.schedule(SEARCH_KEYUP_DELAY);
                }
            }
        });

        pickListGrid.addClickHandler(new com.smartgwt.client.widgets.events.ClickHandler() {
          @Override
          public void onClick( com.smartgwt.client.widgets.events.ClickEvent clickEvent) {
              pickListGrid.focus();
          }
        });

        searchTextItem.addClickHandler(new com.smartgwt.client.widgets.form.fields.events.ClickHandler() {
            @Override
            public void onClick(com.smartgwt.client.widgets.form.fields.events.ClickEvent clickEvent) {
                if (pickListGrid.isVisible()) {
                  hideList();
                } else {
                  showList();
                }
            }
        });

        searchTextItem.addFocusHandler( new FocusHandler() {
            @Override
            public void onFocus(FocusEvent event) {
              showList();
            }
        });


        searchTextItem.addBlurHandler( new BlurHandler() {
          @Override
          public void onBlur(BlurEvent event) {
            Timer t = new Timer() {
              public void run() {
                if (!pickListGrid.containsFocus()) {
                  hideList();
                }
              }
            };
            t.schedule(500);
          }
        });


        searchTextForm.setFields(searchTextItem);
        addMember(searchTextForm);
        searchTextItem.setTooltip(MSG.view_searchBar_buttonTooltip());
        searchTextItem.setHint("Search");

        searchTextItem.setShowHintInField(true);

        saveSearchButton = new ToolStripButton();
        saveSearchButton.setIcon(IconEnum.STAR_OFF.getIcon16x16Path());
        saveSearchButton.setTooltip(MSG.view_searchBar_savedSearch_buttonTooltip());
        saveSearchButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                toggleFavoriteSearch();
            }
        });
        addButton(saveSearchButton);

        DynamicForm saveSearchTextForm = new DynamicForm();
        saveSearchTextForm.setAutoWidth();

        saveSearchTextItem = new TextItem("savedSearchName");
        saveSearchTextItem.setShowTitle(false);
        saveSearchTextItem.setWidth(SAVED_SEARCH_WIDTH);

        saveSearchTextForm.setFields(saveSearchTextItem);

        addMember(saveSearchTextForm);
        saveSearchTextItem.hide();

        saveSearchTextItem.addKeyUpHandler(new KeyUpHandler() {
            @Override
            public void onKeyUp(KeyUpEvent keyUpEvent) {
                if (keyUpEvent.getKeyName().equals("Enter")) {
                    saveFavoriteSearch();
                }
            }
        });

        this.draw();
    }

    private void populateInitialSearch() {
        getSearchStrategy().searchFocusHandler();
        pickListGrid.show();
        showPickListGrid();
        searchTextItem.focusInItem();
    }

    private void saveFavoriteSearch() {
        String savedSearchName = saveSearchTextItem.getValueAsString();
        if (null == savedSearchName || savedSearchName.isEmpty()) {
            return;
        }

        String savedSearchPattern = searchTextItem.getValueAsString();
        if (null == savedSearchPattern || savedSearchPattern.isEmpty()) {
            return;
        }

        // This may be a name change, a pattern change or a new saved search. Look at the existing SS list and
        // decided what to do.
        boolean updated = false;
        for (int i = 0, size = pickListGrid.getTotalRows(); !updated && (i < size); ++i) {
            ListGridRecord record = pickListGrid.getRecord(i);
            String name = record.getAttribute(AbstractSearchStrategy.ATTR_NAME);
            String pattern = record.getAttribute(AbstractSearchStrategy.ATTR_PATTERN);

            if (savedSearchName.equalsIgnoreCase(name) && savedSearchPattern.equals(pattern)) {
                return; // SS already exists

            } else if (savedSearchName.equalsIgnoreCase(name)) {
                Integer id = record.getAttributeAsInt(AbstractSearchStrategy.ATTR_ID);
                updateSavedSearchPattern(id, name, savedSearchPattern);
                updated = true;

            } else if (savedSearchPattern.equals(pattern)) {
                Integer id = record.getAttributeAsInt(AbstractSearchStrategy.ATTR_ID);
                updateSavedSearchName(id, savedSearchName);
                updated = true;
            }
        }

        if (!updated) {
            createSavedSearch(savedSearchName, savedSearchPattern);
        }
    }

    private synchronized void showList() {
      if (!pickListGrid.isDrawn() || pickListGrid.isVisible()) {
          populateInitialSearch();
      } else if (!pickListGrid.isVisible()) {
        populateInitialSearch();
      }
    }

    private synchronized void hideList() {
      if (pickListGrid.isVisible()) {
          pickListGrid.hide();
      }
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
        pickListGrid.addRecordDoubleClickHandler(getSearchStrategy());
        pickListGrid.setCellFormatter(getSearchStrategy());
        pickListGrid.setShowHeader(false);
    }

    private void showPickListGrid() {
        Rectangle searchTextRect = searchTextItem.getPageRect();
        pickListGrid.setLeft(searchTextRect.getLeft());
        pickListGrid.setWidth(searchTextRect.getWidth());
        pickListGrid.setTop(searchTextRect.getTop() + searchTextRect.getHeight());
        pickListGrid.setHeight(PICKLIST_HEIGHT);
        pickListGrid.setWidth(PICKLIST_HEIGHT);
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
        pickListGrid.destroy();
        pickListGrid = new ListGrid();

        setSearchMode(SearchMode.BASIC_SEARCH_MODE);
        saveSearchButton.setIcon(IconEnum.STAR_OFF.getIcon16x16Path());
        saveSearchTextItem.hide();
        configurePickListGrid();
        populateInitialSearch();
    }

    public void switchToSavedSearchMode() {
        pickListGrid.destroy();
        pickListGrid = new ListGrid();

        setSearchMode(SearchMode.SAVED_SEARCH_MODE);

        saveSearchButton.setIcon(IconEnum.STAR_ON.getIcon16x16Path());
        saveSearchTextItem.setValue(MSG.view_searchBar_savedSearch_namePrompt());
        saveSearchTextItem.selectValue();
        saveSearchTextItem.show();

        configurePickListGrid();
        populateInitialSearch();
    }

    public String getValue() {
        return searchTextItem.getValueAsString();
    }

    private void createSavedSearch(final String name, final String pattern) {
        Subject subject = UserSessionManager.getSessionSubject();
        SavedSearch newSavedSearch = new SavedSearch(searchSubsystem, name, pattern, subject);
        searchService.createSavedSearch(newSavedSearch, new AsyncCallback<Integer>() {

            public void onSuccess(Integer newSavedSearchId) {
                Message message = new Message(MSG.view_searchBar_savedSearch_save(name), Message.Severity.Info);
                CoreGUI.getMessageCenter().notify(message);
                getSearchStrategy().searchFocusHandler();
            }

            public void onFailure(Throwable caught) {
                Message message = new Message(MSG.view_searchBar_savedSearch_failSave(name), Message.Severity.Error);
                CoreGUI.getMessageCenter().notify(message);
            }
        });
    }

    private void updateSavedSearchName(final int id, final String name) {
        Subject subject = UserSessionManager.getSessionSubject();
        searchService.updateSavedSearchName(id, name, new AsyncCallback<Boolean>() {

            public void onSuccess(Boolean result) {
                Message message = new Message(MSG.view_searchBar_savedSearch_save(name), Message.Severity.Info);
                CoreGUI.getMessageCenter().notify(message);
                getSearchStrategy().searchFocusHandler();
            }

            @Override
            public void onFailure(Throwable caught) {
                Message message = new Message(MSG.view_searchBar_savedSearch_failRename(name), Message.Severity.Error);
                CoreGUI.getMessageCenter().notify(message);
            }
        });
    }

    private void updateSavedSearchPattern(final int id, final String name, final String pattern) {
        Subject subject = UserSessionManager.getSessionSubject();
        searchService.updateSavedSearchPattern(id, pattern, new AsyncCallback<Boolean>() {

            public void onSuccess(Boolean result) {
                Message message = new Message(MSG.view_searchBar_savedSearch_save(name), Message.Severity.Info);
                CoreGUI.getMessageCenter().notify(message);
                getSearchStrategy().searchFocusHandler();
            }

            @Override
            public void onFailure(Throwable caught) {
                Message message = new Message(MSG.view_searchBar_savedSearch_failSave(name), Message.Severity.Error);
                CoreGUI.getMessageCenter().notify(message);
            }
        });
    }

    public AbstractSearchStrategy getSearchStrategy() {
        return searchStrategies.get(searchMode);
    }

    public boolean isFilterEnabled() {
        return !SearchMode.SAVED_SEARCH_MODE.equals(getSearchMode());
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

    @Override
    public void destroy() {
        if (null != pickListGrid) {
            pickListGrid.destroy();
        }
        super.destroy();
    }
}
