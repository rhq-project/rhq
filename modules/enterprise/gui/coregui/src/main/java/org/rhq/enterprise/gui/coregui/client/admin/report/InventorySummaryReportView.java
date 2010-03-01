package org.rhq.enterprise.gui.coregui.client.admin.report;

import com.smartgwt.client.types.ContentsType;
import com.smartgwt.client.widgets.HTMLPane;
import org.rhq.enterprise.gui.coregui.client.BreadCrumb;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.Presenter;
import org.rhq.enterprise.gui.coregui.client.places.Place;

import java.util.List;

/**
 * @author Ian Springer
 */
public class InventorySummaryReportView extends HTMLPane implements Presenter {
    public static final String PATH = "Administration/Reports/InventorySummary";
    public static final Place PLACE = new Place(PATH.substring(PATH.lastIndexOf('/') + 1), null);
    private static final String URL = "/rhq/admin/report/resourceInstallReport-body.xhtml";

    public InventorySummaryReportView() {
        super();
        setContentsType(ContentsType.PAGE);
        setWidth100();
        setHeight100();
        setContentsURL(URL);        
    }

    public boolean fireDisplay(Place base, List<Place> subLocations) {
        return false;
    }

    public Place getPlace() {
        return PLACE;
    }
}
