package org.rhq.enterprise.gui.coregui.client.util.selenium;

import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.UIObject;
import com.smartgwt.client.widgets.BaseWidget;

/**
 * Utilities for assisting with Selenium Automation
 * 
 * @author Jay Shaughnessy
 */
public class SeleniumUtility {

    /** A default id that is not ecommended as it will clash with any other element set to the default */
    public static final String DEFAULT_ID = "DefaultID";

    /**
     * A utility for assigning an ID to a smartgwt widget. Any current ID will be overwritten.  The algorithm is:
     * <pre>
     * If    the widget has a non-empty title, the ID is set to the title, with spaces removed.
     * Else  the ID is set to the widget's hashcode
     * </pre>
     * @return the updated widget
     * @return the ID.
     */
    static public <T extends BaseWidget> T setId(final T widget, String unsafeId) {
        String id = getSafeId(unsafeId, String.valueOf(widget.hashCode()));
        widget.setID(id);
        return widget;
    }

    /**
     * A utility for assigning an ID to a smartgwt widget. Any current ID will be overwritten.  The algorithm is:
     * <pre>
     * Equivalent to setId(widget, widget.getTitle());
     * </pre>
     * @return the updated widget
     * @return the ID.
     */
    static public <T extends BaseWidget> T setId(final T widget) {
        return setId(widget, widget.getTitle());
    }

    /**
     * A utility for assigning an ID to a gwt uiobject.  For a smartgwt widget use {@link setId(BaseWidget)}.
     * Any current ID will be overwritten.  The algorithm is:
     * <pre>
     * If    the unsafeId parameter is non-empty, the ID is set to the unsafeId, with spaces removed.
     * Else  the ID is set to the uiobject's hashcode
     * </pre>
     * 
     * @param uiObject
     * @param unsafeId
     * @return the updated uiObject
     */
    static public <T extends UIObject> T setHtmlId(final T uiObject, String unsafeId) {
        String id = getSafeId(unsafeId, String.valueOf(uiObject.hashCode()));
        uiObject.getElement().setAttribute("id", id);
        return uiObject;
    }

    /**
     * A utility for assigning an ID to a gwt Hyperlink.  For a smartgwt widget
     * use {@link setId(BaseWidget)}. Any current ID will be overwritten.  The algorithm is:
     * <pre>
     * Equivalent to setHtmlId(hyperlink, hyperlink.getText());
     * </pre>
     * 
     * @param hyperlink
     * @return the updated hyperlink 
     */
    static public <T extends Hyperlink> T setHtmlId(final T hyperlink) {
        return setHtmlId(hyperlink, hyperlink.getText());
    }

    /**
     * Use only if you are sure the unsafeId is not null or empty, or if DEFAULT_ID is acceptable.  
     * <pre>
     * Equivalent to:
     * 
     *   getSafeId( unsafeId, DEFAULT_ID );
     * </pre>
     * @param unsafeId The desired Id but with potential problems
     * @return a safe version of unsafeId, or, if unsafeId is null or empty, DEFAULT_ID
     */
    static public String getSafeId(String unsafeId) {

        return getSafeId(unsafeId, DEFAULT_ID);
    }

    /** 
     * Currently performs the following:
     * <pre>
     * - removes spaces
     * - removes dots
     * </pre>
     * @param unsafeId The desired Id but with potential problems
     * @param defaultId
     * @return a safe version of unsafeId, or, if unsafeId is null or empty, defaultId
     */
    static public String getSafeId(String unsafeId, String defaultId) {
        if ((null == unsafeId || unsafeId.trim().isEmpty())) {
            return defaultId;
        }

        String safeId = unsafeId.replace(" ", "").replace(".", "");
        return safeId;
    }
}
