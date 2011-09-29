/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.search;

import java.util.List;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.TextBox;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.SavedSearchCriteria;
import org.rhq.core.domain.search.SavedSearch;
import org.rhq.core.domain.search.SearchSubsystem;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.Messages;
import org.rhq.enterprise.gui.coregui.client.UserSessionManager;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.search.favorites.SavedSearchGrid;
import org.rhq.enterprise.gui.coregui.client.search.favorites.SavedSearchGrid.SavedSearchSelectionHandler;
import org.rhq.enterprise.gui.coregui.client.search.suggest.SuggestTextBox_v3;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.message.Message.Severity;

/**
 * @author Joseph Marques
 */
public class FlexSearchBar extends AbstractSearchBar {

    private static final Messages MSG = CoreGUI.getMessages();

    public static final String DEFAULT_PATTERN_NAME = MSG.view_searchBar_defaultPattern();

    private static final String IMAGE_DIR = "/coregui/images/search/";

    private static final String STAR_OFF_URL = IMAGE_DIR + "star1.png";
    private static final String STAR_ACTIVE_URL = IMAGE_DIR + "star2.png";
    private static final String STAR_ON_URL = IMAGE_DIR + "star3.png";

    private static final String ARROW_WHITE_URL = IMAGE_DIR + "menu_arrow.png";
    private static final String ARROW_GRAY_URL = IMAGE_DIR + "menu_arrow_down.png";

    public static final String TRASH = IMAGE_DIR + "trash.png";

    private final TextBox patternField = new TextBox();
    private final SuggestTextBox_v3 autoCompletePatternField = new SuggestTextBox_v3(this, patternField);
    private final TextBox patternNameField = new TextBox();
    private final Label patternNameLabel = new Label();

    private final Image starImage = new Image(STAR_OFF_URL);
    private final Image arrowImage = new Image(ARROW_WHITE_URL);

    private final PopupPanel savedSearchesPanel = new PopupPanel(true);
    private SavedSearchGrid savedSearchesGrid;

    private int currentSearchId = 0;

    private SearchSubsystem searchSubsystem;
    private String defaultSearchText;
    private String defaultSavedSearchPatternId;

    HorizontalPanel sbc;
    HorizontalPanel sbc_sbbgc;
    HorizontalPanel sbc_sbbgc_sbcc;
    HorizontalPanel sbc_sbbgc_sbcc_sbclhs_pfc;
    HorizontalPanel sbc_sbbgc_sbcc_sbcrhs_aic;
    HorizontalPanel sbc_sbbgc_sbcc_sbcrhs_sic;
    HorizontalPanel sbc_sbbgc_sbcc_sbcrhs_pnfc;
    HorizontalPanel sbc_sbbgc_sbcc_sbcrhs_pnlc;
    HorizontalPanel sbc_sbbc;
    HorizontalPanel sbc_pfsc;
    HorizontalPanel sbc_ssc;

    private HorizontalPanel createHPanel(Panel parent, String className, String id) {
        HorizontalPanel panel = new HorizontalPanel();
        if (parent != null) {
            parent.add(panel);
        }
        if (className != null) {
            panel.setStyleName(className);
        }
        if (id != null) {
            panel.getElement().setId(id);
        }
        return panel;
    }

    public FlexSearchBar(SearchSubsystem searchSubsystem) {
        this(searchSubsystem, null);
    }

    public FlexSearchBar(SearchSubsystem searchSubsystem, String initialSearchText) {
        Log.info("Loading SearchBar...");

        this.searchSubsystem = searchSubsystem;
        this.savedSearchesGrid = new SavedSearchGrid(searchSubsystem);
        // TODO: load default saved search pattern, if user has selected one
        // populate default search text
        // ensure that search subsystem is selected, probably want to force it to be a ctor argument

        sbc = createHPanel(null, "searchBarContainer", null);
        sbc_sbbgc = createHPanel(sbc, "searchBarBackgroundContainer", null);
        sbc_sbbgc_sbcc = createHPanel(sbc_sbbgc, "searchBarComponentsContainer", null);
        sbc_sbbgc_sbcc_sbclhs_pfc = createHPanel(sbc_sbbgc_sbcc, "searchBarComponentLHS", "patternFieldContainer");
        sbc_sbbgc_sbcc_sbcrhs_aic = createHPanel(sbc_sbbgc_sbcc, "searchBarComponentRHS", "arrowImageContainer");
        sbc_sbbgc_sbcc_sbcrhs_sic = createHPanel(sbc_sbbgc_sbcc, "searchBarComponentRHS", "starImageContainer");
        sbc_sbbgc_sbcc_sbcrhs_pnfc = createHPanel(sbc_sbbgc_sbcc, "searchBarComponentRHS", "patternNameFieldContainer");
        sbc_sbbgc_sbcc_sbcrhs_pnlc = createHPanel(sbc_sbbgc_sbcc, "searchBarComponentRHS", "patternNameLabelContainer");
        sbc_pfsc = createHPanel(sbc, null, "patternFieldSuggestionsContainer");
        sbc_ssc = createHPanel(sbc, null, "savedSearchesContainer");

        sbc_sbbgc_sbcc_sbclhs_pfc.add(autoCompletePatternField);
        sbc_sbbgc_sbcc_sbcrhs_sic.add(starImage);
        sbc_sbbgc_sbcc_sbcrhs_aic.add(arrowImage);
        sbc_sbbgc_sbcc_sbcrhs_pnfc.add(patternNameField);
        sbc_sbbgc_sbcc_sbcrhs_pnlc.add(patternNameLabel);
        sbc_ssc.add(savedSearchesPanel);

        initWidget(sbc);

        setupAutoCompletingPatternField();
        setupPatternNameField();
        setupPatternNameLabel();
        setupStarImage();
        setupArrowImage();
        setupSavedSearches();

        setDefaultSearchText(initialSearchText);
        if (getDefaultSearchText() != null) {
            this.autoCompletePatternField.setText(getDefaultSearchText());
        } else if (getDefaultSavedSearchPatternId() != null) {
            try {
                Integer savedSearchId = Integer.valueOf(getDefaultSavedSearchPatternId());
                activateSavedSearch(savedSearchId);
            } catch (Exception e) {
                this.autoCompletePatternField.setText(MSG
                    .view_searchBar_savedSearch_failFind(getDefaultSavedSearchPatternId()));
            }
        }
    }

    public SearchSubsystem getSearchSubsystem() {
        return searchSubsystem;
    }

    public void setDefaultSearchText(String defaultSearchText) {
        if (defaultSearchText == null || defaultSearchText.trim().equals("")) {
            return; // do nothing
        }

        this.defaultSearchText = defaultSearchText;
    }

    public String getDefaultSearchText() {
        return defaultSearchText;
    }

    public void setDefaultSavedSearchPatternId(String defaultSavedSearchPatternId) {
        if (defaultSavedSearchPatternId == null || defaultSavedSearchPatternId.trim().equals("")) {
            return; // do nothing
        }

        this.defaultSavedSearchPatternId = defaultSavedSearchPatternId;
    }

    public String getDefaultSavedSearchPatternId() {
        return defaultSavedSearchPatternId;
    }

    private void setupAutoCompletingPatternField() {
        autoCompletePatternField.getElement().setId("patternField");
        autoCompletePatternField.setStyleName("patternField");

        AutoCompletePatternFieldEventHandler handler = new AutoCompletePatternFieldEventHandler();
        autoCompletePatternField.getTextBox().addFocusHandler(handler);
        autoCompletePatternField.getTextBox().addBlurHandler(handler);
        autoCompletePatternField.addKeyPressHandler(handler);
    }

    private void setupPatternNameField() {
        patternNameField.setStyleName("patternNameField");
        patternNameField.setVisible(false);

        PatternNameFieldEventHandler handler = new PatternNameFieldEventHandler();
        patternNameField.addKeyPressHandler(handler);
        patternNameField.addClickHandler(handler);
        patternNameField.addBlurHandler(handler);
    }

    private void setupPatternNameLabel() {
        patternNameLabel.setStyleName("patternNameLabel");
        patternNameLabel.setVisible(false);

        PatternNameLabelEventHandler handler = new PatternNameLabelEventHandler();
        patternNameLabel.addClickHandler(handler);
    }

    private void setupStarImage() {
        StarImageEventHandler handler = new StarImageEventHandler();
        starImage.addClickHandler(handler);
        starImage.addMouseOverHandler(handler);
        starImage.addMouseOutHandler(handler);
    }

    private void setupArrowImage() {
        ArrowImageEventHandler handler = new ArrowImageEventHandler();
        arrowImage.addClickHandler(handler);
    }

    private void setupSavedSearches() {
        savedSearchesPanel.add(savedSearchesGrid);
        savedSearchesPanel.setStyleName("savedSearchesPanel");
        savedSearchesGrid.addStyleName("savedSearchesPanel");

        // panel position will be re-calculated on down-arrow click
        //savedSearchesPanel.show();
        savedSearchesPanel.hide();

        SavedSearchesEventHandler handler = new SavedSearchesEventHandler();
        savedSearchesPanel.addCloseHandler(handler);
        savedSearchesGrid.setSavedSearchSelectionHandler(handler);
    }

    private void turnNameFieldIntoLabel(boolean nameJustUpdated) {
        String name = patternNameField.getText();

        if (name.equalsIgnoreCase(DEFAULT_PATTERN_NAME)) {
            name = "";
        }

        arrowImage.setVisible(true);
        patternNameField.setVisible(false);

        if ("".equals(name)) {
            starImage.setUrl(STAR_OFF_URL);
        } else {
            // NOTE: currently do not support updated a saved search pattern
            if (0 == currentSearchId) {
                String pattern = autoCompletePatternField.getText();
                createSavedSearch(name, pattern);
            } else if (nameJustUpdated) {
                updateSavedSearchName(currentSearchId, name);
            }

            starImage.setUrl(STAR_ON_URL);
            patternNameLabel.setText(elipse(name));
            patternNameLabel.setVisible(true);
            autoCompletePatternField.setFocus(true);
        }
    }

    private void turnNameLabelIntoField() {
        patternNameField.setText(patternNameLabel.getText());
        patternNameField.setVisible(true);
        patternNameLabel.setVisible(false);
        patternNameField.setFocus(true);
    }

    private String elipse(String data) {
        if (data.length() > 14) {
            return data.substring(0, 20) + "...";
        }
        return data;
    }

    private void createSavedSearch(final String name, final String pattern) {
        Subject subject = UserSessionManager.getSessionSubject();
        SavedSearch newSavedSearch = new SavedSearch(searchSubsystem, name, pattern, subject);
        GWTServiceLookup.getSearchService().createSavedSearch(newSavedSearch, new AsyncCallback<Integer>() {
            @Override
            public void onSuccess(Integer newSavedSearchId) {
                CoreGUI.getMessageCenter()
                    .notify(new Message(MSG.view_searchBar_savedSearch_save(name), Severity.Info));
                currentSearchId = newSavedSearchId;
            }

            @Override
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_searchBar_savedSearch_failSave(name), caught);
            }
        });
    }

    private void updateSavedSearchName(final int savedSearchId, final String newName) {
        GWTServiceLookup.getSearchService().updateSavedSearchName(savedSearchId, newName, new AsyncCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean hadUpdates) {
                if (hadUpdates) {
                    CoreGUI.getMessageCenter().notify(
                        new Message(MSG.view_searchBar_savedSearch_rename(newName), Severity.Info));
                }
            }

            @Override
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_searchBar_savedSearch_failRename(newName), caught);
            }
        });
    }

    /*
     * Event Handlers
     */
    class AutoCompletePatternFieldEventHandler implements KeyPressHandler, FocusHandler, BlurHandler {
        public void onKeyPress(KeyPressEvent event) {
            // hide pattern field/label, turn off star
            if (event.getCharCode() == KeyCodes.KEY_ENTER) {
                return;
            }

            patternNameLabel.setText("");
            patternNameLabel.setVisible(false);
            patternNameField.setValue("", true);
            patternNameField.setVisible(false);
            currentSearchId = 0;
            starImage.setUrl(STAR_OFF_URL);

            if (event.getCharCode() == KeyCodes.KEY_ESCAPE) {
                autoCompletePatternField.hideSuggestionList();
                event.preventDefault();
                event.stopPropagation();
            }
        }

        public void onFocus(FocusEvent event) {
            autoCompletePatternField.showSuggestionList();
            savedSearchesPanel.hide();
        }

        public void onBlur(BlurEvent event) {
            savedSearchesPanel.hide();
        }
    }

    class PatternNameFieldEventHandler implements KeyPressHandler, ClickHandler, BlurHandler {
        public void onKeyPress(KeyPressEvent event) {
            if (event.getCharCode() == KeyCodes.KEY_ENTER) {
                Log.debug("key press pattern name field");
                turnNameFieldIntoLabel(true);
            }
        }

        public void onClick(ClickEvent event) {
            if (patternNameField.getText().equals(DEFAULT_PATTERN_NAME)) {
                patternNameField.setValue("", false);
            }
        }

        public void onBlur(BlurEvent event) {
            // If this is a name change update (star on) then return to displaying the unchanged label.
            // otherwise, abort the creation of the new saved search.
            if (starImage.getUrl().endsWith(STAR_ON_URL)) {
                patternNameField.setVisible(false);
                patternNameLabel.setVisible(true);

            } else {
                patternNameField.setVisible(false);
                patternNameLabel.setVisible(false);
            }
        }
    }

    class PatternNameLabelEventHandler implements ClickHandler {
        public void onClick(ClickEvent event) {
            turnNameLabelIntoField();
        }
    }

    class StarImageEventHandler implements ClickHandler, MouseOverHandler, MouseOutHandler {
        public void onClick(ClickEvent event) {

            // note - since hover changes off to active, we never have the star off case here 
            if (starImage.getUrl().endsWith(STAR_ACTIVE_URL)) {
                if (patternNameField.isVisible()) {
                    patternNameField.setVisible(false);

                } else {
                    patternNameField.setText(DEFAULT_PATTERN_NAME);
                    patternNameField.setVisible(true);
                    patternNameField.selectAll();
                    patternNameField.setFocus(true);
                    patternNameLabel.setVisible(false);

                }
            } else if (starImage.getUrl().endsWith(STAR_ON_URL)) {
                starImage.setUrl(STAR_ACTIVE_URL);
                patternNameField.setVisible(false);
                patternNameLabel.setVisible(false);
            }
        }

        public void onMouseOver(MouseOverEvent event) {
            if (starImage.getUrl().endsWith(STAR_OFF_URL) && !"".equals(getValue().trim())) {

                starImage.setUrl(STAR_ACTIVE_URL);
            }
        }

        public void onMouseOut(MouseOutEvent event) {
            if (starImage.getUrl().endsWith(STAR_ACTIVE_URL) && !patternNameField.isVisible()) {
                starImage.setUrl(STAR_OFF_URL);
            }
        }
    }

    class ArrowImageEventHandler implements ClickHandler {
        public void onClick(ClickEvent event) {
            savedSearchesGrid.updateModel(new AsyncCallback<List<SavedSearch>>() {
                @Override
                public void onFailure(Throwable caught) {
                    // nothing needs to be done
                }

                @Override
                public void onSuccess(List<SavedSearch> updatedGridData) {
                    int left = autoCompletePatternField.getAbsoluteLeft();
                    int top = autoCompletePatternField.getAbsoluteTop() + autoCompletePatternField.getOffsetHeight();
                    savedSearchesPanel.setPopupPosition(left, top + 5);
                    savedSearchesPanel.show();
                    arrowImage.setUrl(ARROW_GRAY_URL);
                }
            });
        }
    }

    class SavedSearchesEventHandler implements CloseHandler<PopupPanel>, SavedSearchSelectionHandler {
        public void onClose(CloseEvent<PopupPanel> event) {
            arrowImage.setUrl(ARROW_WHITE_URL);
        }

        public void handleSelection(final int rowIndex, final int columnIndex, final SavedSearch savedSearch) {
            Log.debug("SavedSearchesEventHandler.handleSelection(" + rowIndex + "," + columnIndex + "," + savedSearch
                + ")");
            if (columnIndex == 1) {
                GWTServiceLookup.getSearchService().deleteSavedSearch(savedSearch.getId(), new AsyncCallback<Void>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(
                            MSG.view_searchBar_savedSearch_failDelete(savedSearch.getName()), caught);
                    }

                    @Override
                    public void onSuccess(Void result) {
                        // handle deletion of current saved search
                        if (currentSearchId == savedSearch.getId()) {
                            currentSearchId = 0;
                            patternNameField.setValue("", true);
                            patternNameField.setVisible(false);
                            patternNameLabel.setText("");
                            patternNameLabel.setVisible(false);
                            autoCompletePatternField.setFocus(true);
                            starImage.setUrl(STAR_OFF_URL);
                            savedSearchesPanel.hide();
                        }

                        // handle deletion of the one and only element in the list
                        if (savedSearchesGrid.size() == 1) {
                            savedSearchesPanel.hide();
                        }

                        savedSearchesGrid.removeRow(rowIndex);

                        CoreGUI.getMessageCenter().notify(
                            new Message(MSG.view_searchBar_savedSearch_delete(savedSearch.getName()), Severity.Info));
                    }
                });
            } else {
                activateSavedSearch(savedSearch); // activating the saved search also clicks the button
            }
        }
    }

    public void activateSavedSearch(Integer savedSearchId) {
        activeSavedSearchByIdOrName(savedSearchId, null);
    }

    public void activateSavedSearch(String savedSearchName) {
        activeSavedSearchByIdOrName(null, savedSearchName);
    }

    private void activeSavedSearchByIdOrName(Integer savedSearchId, final String savedSearchName) {
        Subject subject = UserSessionManager.getSessionSubject();
        SavedSearchCriteria criteria = new SavedSearchCriteria();
        criteria.addFilterSubjectId(subject.getId());
        criteria.addFilterId(savedSearchId); // null OK
        criteria.addFilterName(savedSearchName); // null OK
        criteria.setStrict(true);

        GWTServiceLookup.getSearchService().findSavedSearchesByCriteria(criteria,
            new AsyncCallback<List<SavedSearch>>() {
                @Override
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_searchBar_savedSearch_failFind(savedSearchName),
                        caught);
                }

                @Override
                public void onSuccess(List<SavedSearch> results) {
                    if (results.size() == 0) {
                        CoreGUI.getMessageCenter().notify(
                            new Message(MSG.view_searchBar_savedSearch_failFind(savedSearchName), Severity.Error));
                    } else {
                        SavedSearch savedSearch = results.get(0);
                        activateSavedSearch(savedSearch);
                    }
                }
            });
    }

    public void activateSavedSearch(SavedSearch savedSearch) {
        currentSearchId = savedSearch.getId();
        autoCompletePatternField.setValue(savedSearch.getPattern(), true);
        patternNameField.setValue(savedSearch.getName(), true);
        Log.debug("search results change: [" + savedSearch.getName() + "," + savedSearch.getPattern() + "]");
        turnNameFieldIntoLabel(false);
        savedSearchesPanel.hide();
    }

    public String getSelectedTab() {
        return null;
    }

    public void addKeyPressHandler(KeyPressHandler handler) {
        autoCompletePatternField.addKeyPressHandler(handler);
    }

    public String getValue() {
        return autoCompletePatternField.getValue();
    }

}
