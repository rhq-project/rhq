package org.rhq.enterprise.gui.navigation.contextmenu;

public class MenuItemDescriptor {

    private String name;
    private String url;
    private String menuItemId;

    public MenuItemDescriptor() {
        super();
    }

    /**
     * @return the name of this menu item.
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the URL the menu item points to
     */
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * @return the id uniquely identifying this menu item
     */
    public String getMenuItemId() {
        return menuItemId;
    }

    public void setMenuItemId(String menuItemId) {
        this.menuItemId = menuItemId;
    }

}