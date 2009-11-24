package org.rhq.enterprise.server.perspective;

import java.util.List;

import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.MenuItemType;

public class MenuItem {

    private MenuItemType item;
    private List<MenuItem> children;

    public MenuItem(MenuItemType item) {
        super();
        this.item = item;
    }

    /**
     * @return the children
     */
    public List<MenuItem> getChildren() {
        return children;
    }

    /**
     * @param children the children to set
     */
    public void setChildren(List<MenuItem> children) {
        this.children = children;
    }

    /**
     * @return the item
     */
    public MenuItemType getItem() {
        return item;
    }
}
