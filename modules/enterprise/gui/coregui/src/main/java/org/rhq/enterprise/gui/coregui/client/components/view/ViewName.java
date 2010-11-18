package org.rhq.enterprise.gui.coregui.client.components.view;

import org.rhq.core.domain.util.StringUtils;

public class ViewName {

    private String name;
    private String title;

    public ViewName(String name) {
        this(name, null);
    }

    public ViewName(String name, String title) {
        super();
        this.name = name;
        this.title = (null == title || "".equals(title.trim())) ? StringUtils.deCamelCase(name) : title;
    }

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }

    /* 
     * Return just the name so String construction of view path works as expected
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return name;
    }

}
