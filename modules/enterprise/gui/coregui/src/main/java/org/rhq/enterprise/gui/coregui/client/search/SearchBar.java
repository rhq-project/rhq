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
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;

import org.rhq.core.domain.search.SearchSubsystem;
import org.rhq.enterprise.gui.coregui.client.search.favorites.SavedSearchGrid;
import org.rhq.enterprise.gui.coregui.client.search.favorites.SavedSearchManager;
import org.rhq.enterprise.gui.coregui.client.search.favorites.SavedSearchGrid.PatternSelectionHandler;
import org.rhq.enterprise.gui.coregui.client.search.suggest.SuggestTextBox_v3;

/**
 * @author Joseph Marques
 */
public class SearchBar extends SimplePanel {

    public String welcomeMessage;

    public static final String DEFAULT_PATTERN_NAME = "name your pattern";

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
    private final SavedSearchGrid savedSearches = new SavedSearchGrid(this);

    private String currentSearch = "";
    private long lastNameFieldBlurTime = 0;

    private final SavedSearchManager savedSearchManager;
    private SearchSubsystem searchSubsystem;
    private String defaultSearchText;
    private String defaultSavedSearchPattern;

    private Element searchButton;

    public static boolean existsOnPage() {
        return getSearchBarElement() != null;
    }

    private static Element getSearchBarElement() {
        return DOM.getElementById("searchBar");
    }

    public void loadAdditionalDataFromDivAttributes() {
        Element searchBarElement = getSearchBarElement();

        String searchButtonId = searchBarElement.getAttribute("searchButtonId");
        searchButton = DOM.getElementById(searchButtonId);

        Event.addNativePreviewHandler(new NativePreviewHandler() {
            @Override
            public void onPreviewNativeEvent(NativePreviewEvent event) {
                if (event.getNativeEvent().getEventTarget().equals(searchButton)
                    && event.getTypeInt() == Event.ONMOUSEDOWN) {
                    prepareSearchExecution();
                }
            }
        });

        String searchSubsystem = searchBarElement.getAttribute("searchSubsystem");
        setSearchSubsystem(SearchSubsystem.valueOf(searchSubsystem.toUpperCase()));

        String defaultSearchText = searchBarElement.getAttribute("defaultSearchText");
        setDefaultSearchText(defaultSearchText);

        String defaultSavedSearchPattern = searchBarElement.getAttribute("defaultSavedSearchPattern");
        setDefaultSavedSearchPattern(defaultSavedSearchPattern);
    }

    public SearchBar() {
        System.out.println("Loading SearchBar...");

        savedSearchManager = new SavedSearchManager(this);
    }

    public void onSavedSearchManagerLoaded() {
        RootPanel.get("patternFieldContainer").add(autoCompletePatternField);
        RootPanel.get("patternNameFieldContainer").add(patternNameField);
        RootPanel.get("patternNameLabelContainer").add(patternNameLabel);
        RootPanel.get("starImageContainer").add(starImage);
        RootPanel.get("arrowImageContainer").add(arrowImage);
        RootPanel.get("savedSearchesContainer").add(savedSearchesPanel);

        setupAutoCompletingPatternField();
        setupPatternNameField();
        setupPatternNameLabel();
        setupStarImage();
        setupArrowImage();
        setupSavedSearches();

        // in the future, will be instantiated directly from a higher-level widget
        if (existsOnPage()) {
            loadAdditionalDataFromDivAttributes();
        }
        // presume the enclosing page logic loads results without a button click
    }

    public static native String getUserAgent()
    /*-{
        return navigator.userAgent.toLowerCase();
    }-*/;

    public SavedSearchManager getSavedSearchManager() {
        return savedSearchManager;
    }

    public void setSearchSubsystem(SearchSubsystem searchSubsystem) {
        this.searchSubsystem = searchSubsystem;

        this.welcomeMessage = "search for " + searchSubsystem.getName().toLowerCase() + "s";

        this.autoCompletePatternField.setText(welcomeMessage);
    }

    public SearchSubsystem getSearchSubsystem() {
        return searchSubsystem;
    }

    public void setDefaultSearchText(String defaultSearchText) {
        if (defaultSearchText == null || defaultSearchText.trim().equals("")) {
            return; // do nothing
        }

        this.defaultSearchText = defaultSearchText;
        this.autoCompletePatternField.setText(defaultSearchText);
        click(searchButton); // execute the search with this default search expression
    }

    public String getDefaultSearchText() {
        return defaultSearchText;
    }

    public void setDefaultSavedSearchPattern(String defaultSavedSearchPattern) {
        if (defaultSavedSearchPattern == null || defaultSavedSearchPattern.trim().equals("")) {
            return; // do nothing
        }

        this.defaultSavedSearchPattern = defaultSavedSearchPattern;
        activateSavedSearch(defaultSavedSearchPattern);
    }

    public String getDefaultSavedSearchPattern() {
        return defaultSavedSearchPattern;
    }

    public String getWelcomeMessage() {
        return welcomeMessage;
    }

    public void prepareSearchExecution() {
        String searchTerms = autoCompletePatternField.getText().toLowerCase().trim();
        if (searchTerms.equals(welcomeMessage)) {
            autoCompletePatternField.setText("");
        }
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
        savedSearchesPanel.add(savedSearches);
        savedSearchesPanel.setStyleName("savedSearchesPanel");
        savedSearches.addStyleName("savedSearchesPanel");

        // panel position will be re-calculated on down-arrow click
        savedSearchesPanel.show();
        savedSearchesPanel.hide();

        SavedSearchesEventHandler handler = new SavedSearchesEventHandler();
        savedSearchesPanel.addCloseHandler(handler);
        savedSearches.setPatternSelectionHandler(handler);
    }

    private void turnNameFieldIntoLabel() {
        String name = patternNameField.getText();

        if (name.equalsIgnoreCase(DEFAULT_PATTERN_NAME)) {
            name = "";
        }

        arrowImage.setVisible(true);
        patternNameField.setVisible(false);

        if (name.equals("")) {
            savedSearchManager.removePatternByName(currentSearch);
            starImage.setUrl(STAR_OFF_URL);
        } else {
            if (currentSearch.equals("")) {
                String pattern = autoCompletePatternField.getText();
                savedSearchManager.updatePatternByName(name, pattern); // create case
            } else {
                savedSearchManager.renamePattern(currentSearch, name);
            }
            //savedSearchManager.updatePatternByName(name, pattern);
            patternNameLabel.setText(elipse(name));
            patternNameLabel.setVisible(true);
            starImage.setUrl(STAR_ON_URL);
        }
        currentSearch = name;
    }

    private void turnNameLabelIntoField() {
        String name = currentSearch;
        patternNameField.setText(name);
        patternNameField.setVisible(true);
        patternNameLabel.setVisible(false);
        patternNameField.setFocus(true);
    }

    private String elipse(String data) {
        if (data.length() > 14) {
            return data.substring(0, 14) + "...";
        }
        return data;
    }

    /*
     * Event Handlers
     */
    class AutoCompletePatternFieldEventHandler implements KeyPressHandler, FocusHandler, BlurHandler {
        @Override
        public void onKeyPress(KeyPressEvent event) {
            // hide pattern field/label, turn off star
            if (event.getCharCode() == KeyCodes.KEY_ENTER) {
                return;
            }

            patternNameLabel.setText("");
            patternNameLabel.setVisible(false);
            patternNameField.setValue("", true);
            patternNameField.setVisible(false);
            currentSearch = "";
            starImage.setUrl(STAR_OFF_URL);

            if (event.getCharCode() == KeyCodes.KEY_ESCAPE) {
                autoCompletePatternField.hideSuggestionList();
                event.preventDefault();
                event.stopPropagation();
            }
        }

        @Override
        public void onFocus(FocusEvent event) {
            // clear default search text if necessary
            if (autoCompletePatternField.getText().equals(welcomeMessage)) {
                autoCompletePatternField.setValue("", true);
            }
            autoCompletePatternField.showSuggestionList();
            savedSearchesPanel.hide();
        }

        @Override
        public void onBlur(BlurEvent event) {
            if (autoCompletePatternField.getText().equals("")) {
                autoCompletePatternField.setValue(welcomeMessage, true);
            }
            savedSearchesPanel.hide();
        }
    }

    class PatternNameFieldEventHandler implements KeyPressHandler, ClickHandler, BlurHandler {
        @Override
        public void onKeyPress(KeyPressEvent event) {
            if (event.getCharCode() == KeyCodes.KEY_ENTER) {
                SearchLogger.debug("key press pattern name field");
                turnNameFieldIntoLabel();
            }
        }

        @Override
        public void onClick(ClickEvent event) {
            if (patternNameField.getText().equals(DEFAULT_PATTERN_NAME)) {
                patternNameField.setValue("", false);
            }
        }

        @Override
        public void onBlur(BlurEvent event) {
            lastNameFieldBlurTime = System.currentTimeMillis();
            turnNameFieldIntoLabel();
        }
    }

    class PatternNameLabelEventHandler implements ClickHandler {
        @Override
        public void onClick(ClickEvent event) {
            turnNameLabelIntoField();
        }
    }

    class StarImageEventHandler implements ClickHandler, MouseOverHandler, MouseOutHandler {
        @Override
        public void onClick(ClickEvent event) {
            long diff = System.currentTimeMillis() - lastNameFieldBlurTime;
            if (Math.abs(diff) < 750) {
                /* 
                 * This event propagation is annoying.  If the threshold is set too low, then both
                 * the name field blur event and this star image click event fire...but the blur
                 * event fires first, which turns the star white.  Then a click on a white star
                 * triggers edit mode, re-enabling the name field.  However, setting the threshold
                 * too high will prevent the click event from being handled when the user naturally
                 * wants to click on the star in rapid succession within the threshold time frame.
                 * It is hoped that 750ms will strike a nice balance, and that most users will never
                 * experienced any oddities from this trade-off.
                 */
                return;
            }

            if (starImage.getUrl().endsWith(STAR_ACTIVE_URL)) {
                patternNameField.setText(DEFAULT_PATTERN_NAME);
                patternNameField.setVisible(true);
                patternNameField.selectAll();
                patternNameField.setFocus(true);
                patternNameLabel.setVisible(false);
            } else if (starImage.getUrl().endsWith(STAR_ON_URL)) {
                starImage.setUrl(STAR_ACTIVE_URL);
                patternNameField.setVisible(false);
                patternNameLabel.setVisible(false);
                savedSearchManager.removePatternByName(currentSearch);
            }
        }

        @Override
        public void onMouseOver(MouseOverEvent event) {
            if (starImage.getUrl().endsWith(STAR_OFF_URL)) {
                starImage.setUrl(STAR_ACTIVE_URL);

            }
        }

        @Override
        public void onMouseOut(MouseOutEvent event) {
            if (starImage.getUrl().endsWith(STAR_ACTIVE_URL) && !patternNameField.isVisible()) {
                starImage.setUrl(STAR_OFF_URL);
            }
        }
    }

    class ArrowImageEventHandler implements ClickHandler {
        @Override
        public void onClick(ClickEvent event) {
            savedSearches.updateModel();
            int left = autoCompletePatternField.getAbsoluteLeft();
            int top = autoCompletePatternField.getAbsoluteTop() + autoCompletePatternField.getOffsetHeight();
            savedSearchesPanel.setPopupPosition(left, top + 5);
            savedSearchesPanel.show();
            arrowImage.setUrl(ARROW_GRAY_URL);
        }
    }

    class SavedSearchesEventHandler implements CloseHandler<PopupPanel>, PatternSelectionHandler {
        @Override
        public void onClose(CloseEvent<PopupPanel> event) {
            arrowImage.setUrl(ARROW_WHITE_URL);
        }

        @Override
        public void handleSelection(int rowIndex, int columnIndex, String patternName) {
            if (columnIndex == 1) {
                savedSearchManager.removePatternByName(patternName);

                if (currentSearch.equals(patternName)) {
                    currentSearch = "";
                    patternNameField.setValue("", true);
                    patternNameField.setVisible(false);
                    patternNameLabel.setText("");
                    patternNameLabel.setVisible(false);
                    autoCompletePatternField.setFocus(true);
                    starImage.setUrl(STAR_OFF_URL);
                    savedSearchesPanel.hide();
                }

                if (savedSearchManager.getSavedSearchCount() == 0) {
                    savedSearchesPanel.hide();
                }

                savedSearches.removeRow(rowIndex);
            } else {
                activateSavedSearch(patternName); // activating the saved search also clicks the button
            }
        }
    }

    private static native void click(Element button)
    /*-{
        button.click();
    }-*/;

    public void activateSavedSearch(String savedSearchName) {
        currentSearch = "";
        String patternValue = savedSearchManager.getPatternByName(savedSearchName);
        if (patternValue == null) {
            return; // no saved search existing with the specified name
        }
        autoCompletePatternField.setValue(patternValue, true);
        patternNameField.setValue(savedSearchName, true);
        SearchLogger.debug("search results change: [" + savedSearchName + "," + patternValue + "]");
        turnNameFieldIntoLabel();
        savedSearchesPanel.hide();
        click(searchButton);
    }

}
