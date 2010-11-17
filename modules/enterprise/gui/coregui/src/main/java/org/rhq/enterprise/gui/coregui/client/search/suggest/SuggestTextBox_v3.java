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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.HandlesAllKeyEvents;
import com.google.gwt.event.dom.client.HasAllFocusHandlers;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.HasSelectionHandlers;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.TextBoxBase;
import com.google.gwt.user.client.ui.SuggestOracle.Callback;
import com.google.gwt.user.client.ui.SuggestOracle.Request;
import com.google.gwt.user.client.ui.SuggestOracle.Response;
import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;

import org.rhq.core.domain.search.SearchSuggestion;
import org.rhq.core.domain.search.SearchSuggestion.Kind;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.SearchGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.search.SearchBar;

public class SuggestTextBox_v3 extends Composite implements HasText, HasAllFocusHandlers, HasValue<String>,
    HasSelectionHandlers<Suggestion> {

    private final SearchBar searchBar;
    private final SearchSuggestOracle oracle;

    private int limit = 20;
    private int currentCursorPosition = 0;
    private final SuggestionMenu suggestionMenu;
    private final PopupPanel suggestionPopup;
    private final TextBoxBase box;
    private final Callback callback = new Callback() {
        public void onSuggestionsReady(Request request, Response response) {
            showSuggestions(response.getSuggestions());
        }
    };

    private final String STYLE_NAME_TEXT_BOX = "patternField";
    private final String STYLE_NAME_POPUP_PANEL = "suggestPanel";

    public SuggestTextBox_v3(SearchBar searchBar, TextBoxBase box) {
        super();

        this.searchBar = searchBar;
        this.oracle = new SearchSuggestOracle();

        this.box = box;
        initWidget(box);

        getElement().setAttribute("autocomplete", "off"); // we're producing completion suggestions, not the browser

        // suggestionMenu must be created before suggestionPopup, because
        // suggestionMenu is suggestionPopup's widget
        suggestionMenu = new SuggestionMenu(true);
        suggestionPopup = createPopup();

        // need to fork PopupPanel to access the animation type
        //suggestionPopup.setAnimationType(PopupPanel.AnimationType.ROLL_DOWN);

        addEventsToTextBox();
        setStyleName(STYLE_NAME_TEXT_BOX);
    }

    private PopupPanel createPopup() {
        PopupPanel p = new PopupPanel(true, false);
        p.setWidget(suggestionMenu);
        p.setStyleName(STYLE_NAME_POPUP_PANEL);
        p.setPreviewingAllNativeEvents(true);
        p.addAutoHidePartner(getTextBox().getElement());
        return p;
    }

    class SuggestionCompletionCommand implements Command {

        private Suggestion suggestion;

        public SuggestionCompletionCommand(Suggestion suggestion) {
            this.suggestion = suggestion;
        }

        public void execute() {
            complete(suggestion, currentCursorPosition);
        }
    }

    private void showSuggestions(Collection<? extends Suggestion> suggestions) {
        if (suggestions.size() > 0) {

            // Hide the popup before we manipulate the menu within it. If we do not
            // do this, some browsers will redraw the popup as items are removed
            // and added to the menu.
            boolean isAnimationEnabled = suggestionPopup.isAnimationEnabled();
            if (suggestionPopup.isAttached()) {
                suggestionPopup.hide();
            }

            suggestionMenu.clearItems();

            for (Suggestion curSuggestion : suggestions) {
                final SuggestionMenuItem menuItem = new SuggestionMenuItem(curSuggestion, oracle.isDisplayStringHTML(),
                    new SuggestionCompletionCommand(curSuggestion));

                suggestionMenu.addItem(menuItem);
            }

            class TextBoxSkewWrapper extends TextBox {
                private TextBoxBase wrapped;
                private int skewWidth;
                private int skewHeight;

                public TextBoxSkewWrapper(TextBoxBase textBoxBase, int skewWidth, int skewHeight) {
                    this.wrapped = textBoxBase;
                    this.skewWidth = skewWidth;
                    this.skewHeight = skewHeight;
                }

                @Override
                public int getOffsetWidth() {
                    return wrapped.getOffsetWidth();
                }

                @Override
                public int getOffsetHeight() {
                    return wrapped.getOffsetHeight();
                }

                @Override
                public int getAbsoluteLeft() {
                    return wrapped.getAbsoluteLeft() + skewWidth;
                }

                @Override
                public int getAbsoluteTop() {
                    return wrapped.getAbsoluteTop() + skewHeight;
                }
            }

            suggestionPopup.showRelativeTo(new TextBoxSkewWrapper(getTextBox(), 0, 5));
            suggestionPopup.setAnimationEnabled(isAnimationEnabled);
        } else {
            suggestionPopup.hide();
        }
    }

    private void addEventsToTextBox() {
        class TextBoxEvents extends HandlesAllKeyEvents implements ValueChangeHandler<String>, ClickHandler {

            private boolean isInstructionalCommentSelected() {
                SearchSuggestion searchSuggestion = suggestionMenu.getSearchSuggestion();
                Kind kind = searchSuggestion.getKind();
                return kind == Kind.InstructionalTextComment;
            }

            public void onClick(ClickEvent event) {
                handleSuggestions();
            }

            public void onKeyDown(KeyDownEvent event) {
                /*
                 * Make sure that the menu is actually showing.  These
                 * keystrokes are only relevant when choosing a suggestion.
                 */
                if (suggestionPopup.isAttached()) {
                    switch (event.getNativeKeyCode()) {
                    case KeyCodes.KEY_DOWN:
                        suggestionMenu.moveSelectionDown();
                        if (isInstructionalCommentSelected()) {
                            if (suggestionMenu.getNumItems() == 1) {
                                suggestionMenu.selectItem(null);
                            } else {
                                suggestionMenu.moveSelectionDown();
                            }
                        }
                        event.preventDefault();
                        break;
                    case KeyCodes.KEY_UP:
                        suggestionMenu.moveSelectionUp();
                        if (isInstructionalCommentSelected()) {
                            if (suggestionMenu.getNumItems() == 1) {
                                suggestionMenu.selectItem(null);
                            } else {
                                suggestionMenu.moveSelectionUp();
                            }
                        }
                        event.preventDefault();
                        break;
                    case KeyCodes.KEY_ENTER:
                        if (suggestionMenu.getSelectedItemIndex() < 0) {
                            suggestionPopup.hide();
                        } else {
                            suggestionMenu.doSelectedItemAction();
                        }
                        break;
                    }
                }
                delegateEvent(SuggestTextBox_v3.this, event);
            }

            public void onKeyPress(KeyPressEvent event) {
                delegateEvent(SuggestTextBox_v3.this, event);
            }

            public void onKeyUp(KeyUpEvent event) {
                handleSuggestions();

                delegateEvent(SuggestTextBox_v3.this, event);
            }

            public void onValueChange(ValueChangeEvent<String> event) {
                delegateEvent(SuggestTextBox_v3.this, event);
            }

            private void handleSuggestions() {
                int nextCursorPosition = getTextBox().getCursorPos();
                if (nextCursorPosition != -1) {
                    // refresh suggestions if cursor moved, meaning users want suggestions from a different context
                    if (currentCursorPosition != nextCursorPosition) {
                        currentCursorPosition = nextCursorPosition;
                        showSuggestions();
                    }
                }
            }
        }

        TextBoxEvents events = new TextBoxEvents();
        events.addKeyHandlersTo(box);
        box.addClickHandler(events);
        box.addValueChangeHandler(events);
    }

    /*
     * Composite methods
     */
    public void setLimit(int limit) {
        this.limit = limit;
    }

    /*
     * Handler methods
     */
    public HandlerRegistration addKeyDownHandler(KeyDownHandler handler) {
        return addDomHandler(handler, KeyDownEvent.getType());
    }

    public HandlerRegistration addKeyPressHandler(KeyPressHandler handler) {
        return addDomHandler(handler, KeyPressEvent.getType());
    }

    public HandlerRegistration addKeyUpHandler(KeyUpHandler handler) {
        return addDomHandler(handler, KeyUpEvent.getType());
    }

    public HandlerRegistration addSelectionHandler(SelectionHandler<Suggestion> handler) {
        return addHandler(handler, SelectionEvent.getType());
    }

    public HandlerRegistration addValueChangeHandler(ValueChangeHandler<String> handler) {
        return addHandler(handler, ValueChangeEvent.getType());
    }

    public int getLimit() {
        return limit;
    }

    /*
     * TextBox methods
     */
    public void setText(String text) {
        box.setText(text);
    }

    public void setValue(String newValue) {
        box.setValue(newValue);
    }

    public void setValue(String value, boolean fireEvents) {
        box.setValue(value, fireEvents);
    }

    public String getText() {
        return box.getText();
    }

    public String getValue() {
        return box.getValue();
    }

    public TextBoxBase getTextBox() {
        return box;
    }

    public void setFocus(boolean focused) {
        box.setFocus(focused);
    }

    /*
     * SuggestBox methods
     */
    public void showSuggestionList() {
        if (isAttached()) {
            showSuggestions();
        }
    }

    public void hideSuggestionList() {
        this.suggestionPopup.hide();
    }

    public boolean isSuggestionListShowing() {
        return suggestionPopup.isShowing();
    }

    /**
     * Get the number of suggestions that are currently showing.
     *
     * @return the number of suggestions currently showing, 0 if there are none
     */
    int getSuggestionCount() {
        return isSuggestionListShowing() ? suggestionMenu.getNumItems() : 0;
    }

    void showSuggestions() {
        String query = box.getText();
        int cursorPos = currentCursorPosition;
        if (query.length() == 0) {
            oracle.requestDefaultSuggestions(new SearchSuggestionRequest(null, cursorPos, limit), callback);
        } else {
            oracle.requestSuggestions(new SearchSuggestionRequest(query, cursorPos, limit), callback);
        }
    }

    /**
     * The SuggestionMenu class is used for the display and selection of
     * suggestions in the SuggestBox widget. SuggestionMenu differs from MenuBar
     * in that it always has a vertical orientation, and it has no submenus. It
     * also allows for programmatic selection of items in the menu, and
     * programmatically performing the action associated with the selected item.
     * In the MenuBar class, items cannot be selected programatically - they can
     * only be selected when the user places the mouse over a particlar item.
     * Additional methods in SuggestionMenu provide information about the number
     * of items in the menu, and the index of the currently selected item.
     */
    private static class SuggestionMenu extends MenuBar {

        public SuggestionMenu(boolean vertical) {
            super(vertical);
            // Make sure that CSS styles specified for the default Menu classes
            // do not affect this menu
            setStyleName("");
        }

        public void doSelectedItemAction() {
            // In order to perform the action of the item that is currently
            // selected, the menu must be showing.
            MenuItem selectedItem = getSelectedItem();
            if (selectedItem != null) {
                selectedItem.getCommand().execute();
                //doItemAction(selectedSuggestItem, true);
            }
        }

        public int getNumItems() {
            return getItems().size();
        }

        /**
         * Returns the index of the menu item that is currently selected.
         *
         * @return returns the selected item
         */
        public int getSelectedItemIndex() {
            // The index of the currently selected item can only be
            // obtained if the menu is showing.
            MenuItem selectedItem = getSelectedItem();
            if (selectedItem != null) {
                return getItems().indexOf(selectedItem);
            }
            return -1;
        }

        public SearchSuggestion getSearchSuggestion() {
            SuggestionMenuItem menuItem = (SuggestionMenuItem) getSelectedItem();
            Suggestion suggestion = menuItem.getSuggestion();
            SearchSuggestion searchSuggestion = extraSearchSuggestion(suggestion);
            return searchSuggestion;
        }
    }

    private static SearchSuggestion extraSearchSuggestion(Suggestion suggestion) {
        SearchSuggestionOracleAdapter adapter = (SearchSuggestionOracleAdapter) suggestion;
        SearchSuggestion searchSuggestion = adapter.getSearchSuggestion();
        return searchSuggestion;
    }

    /**
     * Class for menu items in a SuggestionMenu. A SuggestionMenuItem differs from
     * a MenuItem in that each item is backed by a Suggestion object. The text of
     * each menu item is derived from the display string of a Suggestion object,
     * and each item stores a reference to its Suggestion object.
     */
    private static class SuggestionMenuItem extends MenuItem {

        private static final String STYLENAME_DEFAULT = "suggestData";

        private Suggestion suggestion;

        public SuggestionMenuItem(Suggestion suggestion, boolean asHTML, SuggestionCompletionCommand command) {
            super(getFormattedLabel(suggestion), asHTML, command);
            // Each suggestion should be placed in a single row in the suggestion
            // menu. If the window is resized and the suggestion cannot fit on a
            // single row, it should be clipped (instead of wrapping around and
            // taking up a second row).
            DOM.setStyleAttribute(getElement(), "whiteSpace", "nowrap");
            setStyleName(STYLENAME_DEFAULT);
            setSuggestion(suggestion);
        }

        public Suggestion getSuggestion() {
            return suggestion;
        }

        public void setSuggestion(Suggestion suggestion) {
            this.suggestion = suggestion;
        }

        private static String getFormattedLabel(Suggestion suggestion) {
            return getFormattedLabel(extraSearchSuggestion(suggestion));
        }

        private static String getFormattedLabel(SearchSuggestion item) {
            String className = "suggestData ";
            String prefix = "";

            if (item.getKind() == SearchSuggestion.Kind.Simple) {
                className += "suggestDataSimple";
                prefix = "text";
            } else if (item.getKind() == SearchSuggestion.Kind.Advanced) {
                className += "suggestDataAdvanced";
                prefix = "query";
            } else if (item.getKind() == SearchSuggestion.Kind.GlobalSavedSearch
                || item.getKind() == SearchSuggestion.Kind.UserSavedSearch) {
                className += "suggestDataSavedSearch";
                prefix = "saved";
            } else {
            }

            String style = "font-variant: small-caps; font-weight: bold; font-size: 11px; float: left; margin-left: 2px; width: 50px;";
            int marginOffset = 20;
            if (className.endsWith("suggestDataSavedSearch")) {
                style += " color: green;";
                marginOffset += 2;
            } else {
                style += " color: gray;";
                if (className.endsWith("suggestDataSimple")) {
                    marginOffset += 8;
                }
            }

            String decoratedPrefix = wrap(prefix, style);
            String formattedItemLabel = chopWithEvery(item.getLabel(), "<br/>", 100);
            String decoratedItemLabel = decorate(formattedItemLabel, "background-color: yellow;", item.getStartIndex(),
                item.getEndIndex());
            String highlightedSuggestion = colorOperator(decoratedItemLabel);
            String decoratedSuffix = wrap(highlightedSuggestion, "float: left; ");
            String floatClear = "<br style=\"clear: both;\" />";

            String innerHTML = decoratedPrefix + decoratedSuffix + floatClear;
            return innerHTML;
        }

        private static String chopWithEvery(String toChop, String with, int every) {
            //String[] words = toChop.split("\\s");
            List<String> words = chop(toChop, "\\/- \t\r\n");
            StringBuilder results = new StringBuilder();
            int currentLineLength = 0;
            for (String next : words) {
                results.append(next);
                currentLineLength += next.length();
                if (currentLineLength + next.length() > every) {
                    results.append(with);
                    currentLineLength = 0;
                }
            }

            return results.toString();
        }

        // StringTokenzier doesn't exist in GWT, so have to write my own
        private static List<String> chop(String dataToChop, String chopTokens) {
            List<String> words = new ArrayList<String>();

            StringBuilder builder = new StringBuilder();
            for (char next : dataToChop.toCharArray()) {
                if (chopTokens.indexOf(next) != -1) {
                    if (builder.length() > 0) {
                        words.add(builder.toString());
                        builder = new StringBuilder();
                    }
                }
                builder.append(next);
            }

            if (builder.length() > 0) {
                words.add(builder.toString());
            }

            return words;
        }

        private static final List<String> OPERATORS = Arrays.asList("!==", "!=", "==", "=");

        // TODO: fixing coloring strategy
        private static String colorOperator(String data) {
            for (String operator : OPERATORS) {
                int index = -1;
                while ((index = data.indexOf(operator, index + 1)) != -1) {
                    if ((index - 5 >= 0) && data.substring(index - 5, index).equals("style") == false) {
                        break;
                    }
                }
                if (index != -1) {
                    return decorate(data, "color: blue;", index, index + operator.length());
                }
            }
            return data;
        }

        private static String wrap(String data, String style) {
            return "<span style=\"" + style + "\">" + data + "</span>";
        }

        /*
        private static String decorate(String data, String style, int startIndex, int endIndex) {
            if (startIndex == -1) {
                return data; // no match
            }
            String before = data.substring(0, startIndex);
            String highlight = data.substring(startIndex, endIndex);
            String after = data.substring(endIndex);
            return before + "<span style=\"" + style + "\">" + highlight + "</span>" + after;
        }
        */

        private static String decorate(String data, String style, int startIndex, int endIndex) {
            if (startIndex == -1 || (startIndex == endIndex)) {
                return data; // no match or zero-width match
            }

            String[] words = data.split("<br/>");
            int counter = 0;
            int wordIndex = 0;
            int letterIndex = 0;

            StringBuilder results = new StringBuilder();
            while (counter < startIndex) {
                if (wordIndex == words.length) {
                    break;
                }

                if (letterIndex < words[wordIndex].length()) { // more letters left in the current word?
                    results.append(words[wordIndex].charAt(letterIndex)); // append the next char of the current word
                    letterIndex++; // move to the next char in the current word
                    counter++; // only move counter forward when we've added non-BR chars to the results
                } else { // next word
                    results.append("<br/>"); // put the break point back in between words
                    letterIndex = 0; // point to the first char
                    wordIndex++; // of the next word
                }
            }

            // we're at start index, wrap all words and word fragments in the specified style up to endIndex
            results.append("<span style=\"" + style + "\">"); // seed action
            while (counter < endIndex) {
                if (wordIndex == words.length) {
                    break;
                }

                if (letterIndex < words[wordIndex].length()) { // more letters left in the current word?
                    results.append(words[wordIndex].charAt(letterIndex)); // append the next char of the current word
                    letterIndex++; // move to the next char in the current word
                    counter++; // only move counter forward when we've added non-BR chars to the results
                } else { // next word
                    results.append("</span>"); // close the previous word, we don't highlight breaks
                    results.append("<br/>"); // put the break point back in between words
                    letterIndex = 0; // point to the first char
                    wordIndex++; // of the next word
                    results.append("<span style=\"" + style + "\">"); // prepare for next word
                }
            }
            results.append("</span>"); // end last dangling span

            // append the rest of the current word fragment, if any
            if (wordIndex != words.length) {
                results.append(words[wordIndex].substring(letterIndex));
            }

            // append the rest of the words
            while (++wordIndex < words.length) {
                results.append("<br/>"); // put the break point back in between words
                results.append(words[wordIndex]);
            }

            return results.toString();
        }
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
            SearchSuggestionRequest suggestionRequest = (SearchSuggestionRequest) request;
            String expression = suggestionRequest.getQuery();
            int caretPosition = suggestionRequest.getCursorPosition();

            searchService.getTabAwareSuggestions(searchBar.getSearchSubsystem(), expression, caretPosition, searchBar
                .getSelectedTab(), new AsyncCallback<List<SearchSuggestion>>() {

                public void onSuccess(List<SearchSuggestion> results) {
                    adaptAndHandle(results.toArray(new SearchSuggestion[results.size()]));
                }

                public void onFailure(Throwable caught) {
                    SearchSuggestion errorInform = new SearchSuggestion(Kind.InstructionalTextComment, caught
                        .getMessage());
                    adaptAndHandle(errorInform);
                }

                private void adaptAndHandle(SearchSuggestion... searchSuggestionResults) {
                    List<SearchSuggestionOracleAdapter> adaptedResults = new java.util.ArrayList<SearchSuggestionOracleAdapter>();
                    for (SearchSuggestion next : searchSuggestionResults) {
                        adaptedResults.add(new SearchSuggestionOracleAdapter(next));
                    }
                    SuggestOracle.Response response = new SuggestOracle.Response(adaptedResults);
                    callback.onSuggestionsReady(request, response);
                }
            });
        }
    }

    protected void complete(Suggestion suggestion, int cursorPosition) {
        SearchSuggestion searchSuggestion = extraSearchSuggestion(suggestion);
        String currentText = getText().toLowerCase();

        if (searchBar.welcomeMessage.equals(currentText)) {
            setValue("", true);
            return;
        }

        if (searchSuggestion.getKind() == SearchSuggestion.Kind.GlobalSavedSearch
            || searchSuggestion.getKind() == SearchSuggestion.Kind.UserSavedSearch) {
            // execute saved searches immediately, since they presumably constitute complete expressions
            Log.debug("selected '" + searchSuggestion.getLabel() + "' saved search suggestion");
            searchBar.activateSavedSearch(searchSuggestion.getValue());
        } else {
            // selecting a simple suggestion or advanced suggestion

            int previousWhitespaceIndex = cursorPosition;
            if (cursorPosition != 0) {
                while (--previousWhitespaceIndex > 0) {
                    if (currentText.charAt(previousWhitespaceIndex) == ' ') {
                        previousWhitespaceIndex++; // put index right after found whitespace
                        break;
                    }
                }
            }

            int futureWhitespaceIndex = cursorPosition;
            while (futureWhitespaceIndex < currentText.length()) {
                if (currentText.charAt(futureWhitespaceIndex) == ' ') {
                    break;
                }
                futureWhitespaceIndex++;
            }

            String before = getText().substring(0, previousWhitespaceIndex);
            String completion = suggestion.getReplacementString();
            String after = getText().substring(futureWhitespaceIndex);

            setValue(before + completion + after, true);
            currentCursorPosition = before.length() + completion.length();
            getTextBox().setCursorPos(currentCursorPosition);

            showSuggestions();
        }
    }

    class SearchSuggestionRequest extends SuggestOracle.Request {
        private int cursorPosition;

        public SearchSuggestionRequest(String query, int cursorPosition, int limit) {
            super(query, limit);
            this.cursorPosition = cursorPosition;
        }

        public int getCursorPosition() {
            return cursorPosition;
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

        public SearchSuggestion getSearchSuggestion() {
            return suggestion;
        }
    }

    public HandlerRegistration addFocusHandler(FocusHandler handler) {
        // TODO Auto-generated method stub
        return null;
    }

    public HandlerRegistration addBlurHandler(BlurHandler handler) {
        // TODO Auto-generated method stub
        return null;
    }
}
