package org.rhq.coregui.client.components.view;

import org.rhq.core.domain.util.StringUtils;
import org.rhq.coregui.client.IconEnum;

/**
 * A simple class that ties a private name to a displayed title.  The title may very well change with locale but
 * the name will stay constant.  It's useful any time a viewable item does not itself provide for a name.
 * 
 * @author Jay Shaughnessy
 */
public class ViewName {

    private String name;
    private String title;

    /**
     * As part of the UXD changes icons will be shown the header titles.
     */
    private IconEnum icon;

    public ViewName(String name) {
        this(name, null);
    }

    public ViewName(String name, String title) {
        super();
        this.name = name;
        this.title = buildTitle(name, title);
    }


    public ViewName(String name, String title, IconEnum icon) {
        super();
        this.name = name;
        this.title = buildTitle(name, title);
        this.icon = icon;
    }

    /**
     * creates new instance of ViewName
     * @param title new title
     * @return new instance of ViewName
     */
    public ViewName withTitle(String title) {
        return new ViewName(this.name, title);
    }

    private static String buildTitle(String name, String title) {
        return (null == title || "".equals(title.trim())) ? StringUtils.deCamelCase(name) : title;
    }

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }

    public IconEnum getIcon() {
        return icon;
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
