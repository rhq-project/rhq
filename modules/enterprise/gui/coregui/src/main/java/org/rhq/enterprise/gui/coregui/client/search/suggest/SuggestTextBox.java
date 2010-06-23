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
package org.rhq.enterprise.gui.coregui.client.search.suggest;

import java.util.List;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TextBox;

import org.rhq.core.domain.search.SearchSuggestion;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.SearchGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.search.SearchBar;

public class SuggestTextBox extends TextBox {

    protected PopupPanel choicesPopup = new PopupPanel(true);
    protected ScrollPanel choicesScrollPanel = new ScrollPanel();
    protected SuggestResultsListBox choices = new SuggestResultsListBox();

    private SearchBar searchBar;

    private SearchGWTServiceAsync searchService = GWTServiceLookup.getSearchService();

    public SuggestTextBox(SearchBar searchBar) {
        super();
        this.searchBar = searchBar;
        setupTextBox();
        setupChoicesPopup();
        setupChoicesListBox();
    }

    public void setupTextBox() {
        getElement().setAttribute("autocomplete", "off"); // we're producing completion suggestions, not the browser
        SuggestTextBoxEventHandler handler = new SuggestTextBoxEventHandler();
        this.addKeyUpHandler(handler);
        this.addFocusHandler(handler);
    }

    public void setupChoicesPopup() {
        choicesPopup.removeStyleName("gwt-PopupPanel");
        choicesPopup.addStyleName("suggestPanel");
        choicesPopup.add(choices);
    }

    public PopupPanel getSuggestionComponent() {
        return choicesPopup;
    }

    public void setupChoicesListBox() {
        choices.addStyleName("suggestPanel");

        SuggestionResultsListBoxEventHandler handler = new SuggestionResultsListBoxEventHandler();
        choices.addChangeHandler(handler);
        choices.addClickHandler(handler);
    }

    public void hideInitialSuggestions() {
        show();
        hide();
    }

    public boolean isSuggestionListShowing() {
        return isDisplayed();
    }

    public boolean isItemSelected() {
        return (choices.getSelectedIndex() != -1);
    }

    protected void complete() {
        if (choices.getItemCount() > 0 && choices.getSelectedIndex() != -1) {
            String currentText = this.getText().toLowerCase();
            String completion = choices.getValue(choices.getSelectedIndex());

            int cursorPosition = this.getCursorPos();
            int previousWhitespaceIndex = cursorPosition;
            if (cursorPosition != 0) {
                while (--previousWhitespaceIndex > 0) {
                    if (this.getText().charAt(previousWhitespaceIndex) == ' ') {
                        previousWhitespaceIndex++; // put index right after found whitespace
                        break;
                    }
                }
            }
            String before = this.getText().substring(0, previousWhitespaceIndex);
            String after = this.getText().substring(cursorPosition);
            this.setValue(before + completion + after);

            // TODO: this algo screws up when it does the indexOf search on just a single char from currentText
            //       use case is "availability=dow<enter>" -- is this still true, now that we're completing longer things for advanced search?

            if (currentText.equals(this.getText().toLowerCase())) {
                this.setValue(currentText + completion, true);
            }
        }

        choices.clear();
        hide();

        // if this is a saved search pattern completion, execute that search immediately
        System.out.println("just completed to '" + this.getValue() + "'");
        String patternValue = searchBar.getSavedSearchManager().getPatternByName(this.getValue());
        if (patternValue != null) {
            searchBar.prepareSearchExecution();
        } else {
            // send a 'fake' key, this will rerender PopupPanel with the new completion list
            handleKeyCode(0);
        }
    }

    private void hide() {
        choicesPopup.hide();
    }

    private void show() {
        choicesPopup.show();
    }

    private boolean isDisplayed() {
        return choicesPopup.isShowing();
    }

    public void hidePopup() {
        handleKeyCode(KeyCodes.KEY_ESCAPE);
    }

    class SuggestTextBoxEventHandler implements KeyUpHandler, FocusHandler {
        public void onKeyUp(KeyUpEvent event) {
            int keyCode = event.getNativeKeyCode();
            handleKeyCode(keyCode);

            if (keyCode == KeyCodes.KEY_TAB) {
                event.stopPropagation();
                event.preventDefault();
            }
        }

        public void onFocus(FocusEvent event) {
            if (getText().equals(searchBar.getWelcomeMessage())) {
                setText("");
            }
            // send a 'fake' key, this will rerender PopupPanel with the new completion list
            handleKeyCode(1);
        }
    }

    class SuggestionResultsListBoxEventHandler implements ChangeHandler, ClickHandler {
        public void onChange(ChangeEvent event) {
            complete();
        }

        public void onClick(ClickEvent event) {
            complete();
        }
    }

    protected void handleKeyCode(int keyCode) {
        if (keyCode == KeyCodes.KEY_TAB) {
            this.setValue(this.getValue() + " ");
            this.setCursorPos(this.getValue().length());
            this.setFocus(true);
            return;
        }

        if (keyCode == KeyCodes.KEY_DOWN) {
            int selectedIndex = choices.getSelectedIndex();
            selectedIndex++;
            if (selectedIndex >= choices.getItemCount()) {
                selectedIndex = 0;
            }
            String value = choices.getValue(selectedIndex);
            if (value.equals(SuggestResultsListBox.FOOTER_MESSAGE)) {
                if (choices.getItemCount() == 1) {
                    selectedIndex = -1; // do not allow the disabled row to be selected via KEY_DOWN
                } else {
                    selectedIndex++;
                }
            }
            choices.setSelectedIndex(selectedIndex);
            return;
        }

        if (keyCode == KeyCodes.KEY_UP) {
            int selectedIndex = choices.getSelectedIndex();
            selectedIndex--;
            if (selectedIndex < 0) {
                selectedIndex = choices.getItemCount() - 1;
            }
            String value = choices.getValue(selectedIndex);
            if (value.equals(SuggestResultsListBox.FOOTER_MESSAGE)) {
                if (choices.getItemCount() == 1) {
                    selectedIndex = -1; // do not allow the disabled row to be selected via KEY_UP
                } else {
                    selectedIndex = choices.getItemCount() - 1;
                }
            }
            choices.setSelectedIndex(selectedIndex);
            return;
        }

        if (keyCode == KeyCodes.KEY_ENTER) {
            int selectedIndex = choices.getSelectedIndex();
            if (selectedIndex != -1) {
                complete();
            }
            return;
        }

        if (keyCode == KeyCodes.KEY_ESCAPE) {
            choices.clear();
            hide();
            return;
        }

        hide();
        String text = this.getText();

        searchService.getSuggestions( //
            searchBar.getSearchSubsystem(), //
            text, //
            this.getCursorPos(), //
            new SearchSuggestionCallback(keyCode));
    }

    class SearchSuggestionCallback implements AsyncCallback<List<SearchSuggestion>> {

        private int keyCode;

        public SearchSuggestionCallback(int keyCode) {
            this.keyCode = keyCode;
        }

        public void onFailure(Throwable caught) {
            choices.setErrorMessage(caught.getMessage());
            commonHandler();
        }

        public void onSuccess(List<SearchSuggestion> result) {
            choices.setSearchSuggestions(result);
            commonHandler();
        }

        private void commonHandler() {
            int suggestionCount = choices.render(100, 15);
            if (suggestionCount == 0) {
                return;
            }

            choicesPopup.setPopupPosition(getAbsoluteLeft(), getAbsoluteTop() + getOffsetHeight() + 5);
            show();

            if (keyCode == 0) {
                /*
                if (choices.getItemCount() > 0) {
                    String value = choices.getValue(0);
                    if (value.equals(SuggestResultsListBox.FOOTER_MESSAGE)) {
                            if (choices.getItemCount() > 1) {
                                    choices.setSelectedIndex(1);
                            } else {
                                    choices.setSelectedIndex(-1); // nothing is selected if FOOTER_MESSAGE is the only suggestion
                            }
                    } else {
                            choices.setSelectedIndex(0);
                    }
                }
                */

                setFocus(true);
            }
        }

    }
}