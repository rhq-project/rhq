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

import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;

import org.rhq.core.domain.search.SearchSuggestion;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.SearchGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.search.SearchBar;

public class SuggestTextBox_v2 extends TextBox {

    private final SearchBar searchBar;
    private final SearchSuggestOracle searchSuggestionOracle;
    private final SuggestBox suggestBox;

    public SuggestTextBox_v2(SearchBar searchBar) {
        super();
        getElement().setAttribute("autocomplete", "off"); // we're producing completion suggestions, not the browser
        this.searchBar = searchBar;
        this.searchSuggestionOracle = new SearchSuggestOracle();
        this.suggestBox = new SuggestBox(searchSuggestionOracle, this);

        this.suggestBox.setWidth("785px");
        this.suggestBox.setLimit(15);
        this.suggestBox.setAutoSelectEnabled(false);
        this.suggestBox.setAnimationEnabled(true);

        initHandlers();
    }

    private void initHandlers() {
        this.suggestBox.addSelectionHandler(new SelectionHandler<Suggestion>() {

            public void onSelection(SelectionEvent<Suggestion> event) {

                //event.stopPropagation();
                //event.preventDefault();
                complete(event.getSelectedItem().getReplacementString(), getCursorPos());
                //Suggestion suggestion = event.getSelectedItem();
                //searchBar.executeSearch();
            }
        });
    }

    public void hidePopup() {
        this.suggestBox.hideSuggestionList();
    }

    public boolean isSuggestionListShowing() {
        return this.suggestBox.isSuggestionListShowing();
    }

    public SuggestBox getSuggestionBox() {
        return this.suggestBox;
    }

    class SearchSuggestOracle extends SuggestOracle {
        private SearchGWTServiceAsync searchService = GWTServiceLookup.getSearchService();

        @Override
        public boolean isDisplayStringHTML() {
            return true;
        }

        @Override
        public void requestDefaultSuggestions(Request request, Callback callback) {
            requestSuggestions(request, callback);
        }

        @Override
        public void requestSuggestions(final Request request, final Callback callback) {
            String expression = getText();
            int caretPosition = getCursorPos(); // hack, but it wasn't passed in the request

            searchService.getSuggestions(searchBar.getSearchSubsystem(), expression, caretPosition,
                new AsyncCallback<List<SearchSuggestion>>() {

                    public void onSuccess(List<SearchSuggestion> results) {
                        List<SearchSuggestionOracleAdapter> adaptedResults = new java.util.ArrayList<SearchSuggestionOracleAdapter>();
                        for (SearchSuggestion next : results) {
                            adaptedResults.add(new SearchSuggestionOracleAdapter(next));
                        }
                        SuggestOracle.Response response = new SuggestOracle.Response(adaptedResults);
                        callback.onSuggestionsReady(request, response);
                    }

                    public void onFailure(Throwable caught) {
                        System.out.println("Uh oh");
                    }
                });
        }
    }

    protected void complete(String completion, int cursorPosition) {
        String currentText = getText().toLowerCase();
        int previousWhitespaceIndex = cursorPosition;
        if (cursorPosition != 0) {
            while (--previousWhitespaceIndex > 0) {
                if (getText().charAt(previousWhitespaceIndex) == ' ') {
                    previousWhitespaceIndex++; // put index right after found whitespace
                    break;
                }
            }
        }
        String before = getText().substring(0, previousWhitespaceIndex);
        String after = getText().substring(cursorPosition);
        setValue(before + completion + after);

        // TODO: this algo screws up when it does the indexOf search on just a single char from currentText
        //       use case is "availability=dow<enter>" -- is this still true, now that we're completing longer things for advanced search?

        if (currentText.equals(getText().toLowerCase())) {
            setValue(currentText + completion, true);
        }

        String patternValue = searchBar.getSavedSearchManager().getPatternByName(getValue());
        if (patternValue != null) {
            searchBar.prepareSearchExecution();
        } else {
            suggestBox.showSuggestionList();
        }
    }

    class SearchSuggestionOracleAdapter implements SuggestOracle.Suggestion {
        private final SearchSuggestion suggestion;

        public SearchSuggestionOracleAdapter(SearchSuggestion suggestion) {
            this.suggestion = suggestion;
        }

        public String getDisplayString() {
            return suggestion.getLabel();
        }

        public String getReplacementString() {
            return suggestion.getValue();
        }

    }
}
