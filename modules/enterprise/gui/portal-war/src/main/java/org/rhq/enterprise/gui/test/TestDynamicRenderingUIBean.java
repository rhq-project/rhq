package org.rhq.enterprise.gui.test;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

import org.rhq.core.gui.util.FacesContextUtility;

@Scope(ScopeType.PAGE)
@Name("TestDynamicRenderingUIBean")
public class TestDynamicRenderingUIBean {
    private final static String childOne = "/rhq/test/rerender/child-1.xhtml";
    private final static String childTwo = "/rhq/test/rerender/child-2.xhtml";
    private final static String childThree = "/rhq/test/rerender/child-3.xhtml";

    private boolean renderChildOne;
    private boolean renderChildTwo;
    private boolean renderChildThree;

    private String childPage;

    public TestDynamicRenderingUIBean() {
        String page = FacesContextUtility.getOptionalRequestParameter("page");
        if (page != null) {
            childPage = page;
        } else {
            childPage = "/rhq/test/rerender/child-1.xhtml";
        }
    }

    public String getChildPage() {
        System.out.println("getting child - " + childPage);
        return childPage;
    }

    public void setChildPage(String childPage) {
        System.out.println("setting child -" + childPage);
        this.childPage = childPage;
        renderChildOne = childPage.equals(childOne);
        renderChildTwo = childPage.equals(childTwo);
        renderChildThree = childPage.equals(childThree);
    }

    public String refresh() {
        System.out.println("refresh");
        return "refresh";
    }

    public boolean getRenderChildOne() {
        System.out.println("should render 1?");
        return getChildPage().equals(childOne);
    }

    public boolean getRenderChildTwo() {
        System.out.println("should render 2?");
        return getChildPage().equals(childTwo);
    }

    public boolean getRenderChildThree() {
        System.out.println("should render 3?");
        return getChildPage().equals(childThree);
    }
}
