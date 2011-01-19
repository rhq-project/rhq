package org.rhq.enterprise.gui.coregui.client.components.tab;

import com.smartgwt.client.widgets.Canvas;

import org.rhq.enterprise.gui.coregui.client.components.view.ViewName;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableTab;

/**
 * A Wrapper for org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableTab allowing for a Tab that separates
 * internal naming and external title.
 * 
 * @author Jay Shaughnessy
 */
public class NamedTab extends LocatableTab {

    private ViewName viewName;

    public NamedTab(String locatorId, ViewName viewName, String icon) {
        super(locatorId, viewName.getTitle(), icon);
        this.viewName = viewName;
    }

    public ViewName getViewName() {
        return viewName;
    }

    public String getName() {
        return viewName.getName();
    }

    public String getTitle() {
        return viewName.getTitle();
    }

    protected void destroy() {
        Canvas pane = getPane();
        if (null != pane) {
            pane.destroy();
        }
    }

}
