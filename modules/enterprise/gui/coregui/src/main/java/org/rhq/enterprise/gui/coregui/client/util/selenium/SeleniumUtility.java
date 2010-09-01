package org.rhq.enterprise.gui.coregui.client.util.selenium;

import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.UIObject;
import com.smartgwt.client.widgets.BaseWidget;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.layout.Layout;

import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.message.Message.Severity;

/**
 * Utilities for assisting with Selenium Automation
 * 
 * @author Jay Shaughnessy
 */
public class SeleniumUtility {

    private static final boolean USE_DEFAULT_IDS = true;

    /** A default id that is not ecommended as it will clash with any other element set to the default */
    public static final String DEFAULT_ID = "DefaultID";

    /**
     * A utility for assigning an ID to a smartgwt widget. Any current ID will be overwritten.  The algorithm is:
     * <pre>
     * ID Format: "scClassname-locatorId"
     * </pre>
     * @return the updated widget
     */
    static public <T extends BaseWidget> T setID(final T widget, String locatorId) {
        if (USE_DEFAULT_IDS) {
            return widget;
        }

        String unsafeId = widget.getScClassName() + "-" + locatorId;
        String safeId = SeleniumUtility.getSafeId(unsafeId, DEFAULT_ID);
        Canvas canvasWithId = Canvas.getById(safeId);
        if (null != canvasWithId) {
            try {
                canvasWithId.destroy();
                CoreGUI.getMessageCenter().notify(
                    new Message("ID Conflict resolved: " + safeId, getSmallStackTrace(null), Severity.Warning));
            } catch (Throwable t) {
                CoreGUI.getMessageCenter().notify(
                    new Message("ID Conflict unresolved: " + getSmallStackTrace(t), Severity.Info));
            }
        }
        widget.setID(safeId);

        return widget;
    }

    static private String getSmallStackTrace(Throwable t) {
        StringBuilder smallStack = new StringBuilder();

        StackTraceElement[] stack = (null == t) ? new Exception().getStackTrace() : t.getStackTrace();
        for (int i = 1; i < stack.length; i++) {
            StackTraceElement ste = stack[i];
            if (ste.getClassName().startsWith("org.rhq")) {
                smallStack.append(ste.toString());
                smallStack.append("\n");
            }
        }
        return smallStack.toString();
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
        if (USE_DEFAULT_IDS) {
            return uiObject;
        }

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

    static public boolean isUseDefaultIds() {
        return USE_DEFAULT_IDS;
    }

    /**
     * Like Layout.removeMembers() but ensures a synchronous destroy of each member.
     * @param <T>
     * @param layout
     */
    static public <T extends Layout> void destroyMembers(T layout) {
        for (Canvas currentMember : layout.getMembers()) {
            layout.removeMember(currentMember);
            currentMember.destroy();
        }
    }
}
