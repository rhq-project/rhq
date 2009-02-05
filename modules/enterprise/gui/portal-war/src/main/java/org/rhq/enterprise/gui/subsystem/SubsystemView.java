package org.rhq.enterprise.gui.subsystem;

import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.core.gui.util.StringUtility;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;

public abstract class SubsystemView extends PagedDataTableUIBean {
    protected Integer[] getSelectedItems() {
        String[] stringItems = FacesContextUtility.getRequest().getParameterValues("selectedItems");
        Integer[] integerItems = StringUtility.getIntegerArray(stringItems);
        return integerItems;
    }
}
