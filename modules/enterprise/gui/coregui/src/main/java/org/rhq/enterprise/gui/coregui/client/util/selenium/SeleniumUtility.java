package org.rhq.enterprise.gui.coregui.client.util.selenium;

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.layout.Layout;

/**
 * Utilities for assisting with Selenium Automation
 * 
 * @author Jay Shaughnessy
 */
public class SeleniumUtility {

    static public String getSimpleClassName(final Object widget) {
        String className = widget.getClass().getName();
        return className.substring(className.lastIndexOf(".") + 1);
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
     * Currently performs the following:
     * <pre>
     * - removes spaces
     * - removes dots
     * - converts '-' to '_'
     * </pre>
     * @param unsafeId The desired Id but with potential problems
     * @param defaultId
     * @return a safe version of unsafeId, or, if unsafeId is null or empty, defaultId
     */
    static public String getSafeId(String unsafeId, String defaultId) {
        if ((null == unsafeId || unsafeId.trim().isEmpty())) {
            return defaultId;
        }

        String safeId = unsafeId.replace(" ", "").replace(".", "").replace("-", "_");
        return safeId;
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
        return getSafeId(unsafeId, "DEFAULTID");
    }

    /**
     * Like Layout.removeMembers() but ensures a synchronous destroy of each member.
     * @param <T>
     * @param layout
     */
    static public <T extends Layout> void destroyMembers(T layout) {
        if (null == layout) {
            return;
        }

        Canvas[] members = layout.getMembers();
        if (null == members) {
            return;
        }

        for (Canvas currentMember : members) {
            layout.removeMember(currentMember);
            currentMember.destroy();
        }
    }

}
