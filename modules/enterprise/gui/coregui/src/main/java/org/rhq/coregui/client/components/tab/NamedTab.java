package org.rhq.coregui.client.components.tab;

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.tab.Tab;

import org.rhq.coregui.client.components.view.ViewName;

/**
 * A Wrapper for {@link Tab} allowing for a Tab that separates internal naming and external title.
 * 
 * @author Jay Shaughnessy
 */
public class NamedTab extends Tab {

    private ViewName viewName;

    public NamedTab(ViewName viewName) {
        super(viewName.getTitle());
        this.viewName = viewName;
    }

    public NamedTab(ViewName viewName, String icon) {
        super(viewName.getTitle(), icon);
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

    public void destroy() {
        Canvas pane = getPane();
        if (null != pane) {
            pane.destroy();
        }
    }

}
