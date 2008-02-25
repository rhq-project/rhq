package org.rhq.enterprise.gui.common.converter;

import java.util.List;

import javax.faces.model.SelectItem;

import org.rhq.core.domain.common.composite.OptionItem;
import org.rhq.core.gui.util.FacesContextUtility;

public class SelectItemUtils {

    public static SelectItem ALL = new SelectItem("~~All~~", "All");

    private SelectItemUtils() {
    }

    public static SelectItem[] convertFromListOptionItem(List<? extends OptionItem<?>> items, boolean addAllFilter) {
        SelectItem[] results = init(items.size(), addAllFilter);

        int i = (addAllFilter ? 1 : 0);
        for (OptionItem<?> item : items) {
            results[i++] = new SelectItem(item.getId(), item.getDisplayName());
        }

        return results;
    }

    public static SelectItem[] convertFromListString(List<String> items, boolean addAllFilter) {
        SelectItem[] results = init(items.size(), addAllFilter);

        int i = (addAllFilter ? 1 : 0);
        for (String item : items) {
            results[i++] = new SelectItem(item, item);
        }

        return results;
    }

    public static String getSelectItemFilter(String domIdentifier) {
        String result = FacesContextUtility.getOptionalRequestParameter(domIdentifier);

        if (ALL.getValue().equals(result)) {
            result = null;
        }

        return result;
    }

    private static SelectItem[] init(int size, boolean addAllFilter) {
        SelectItem[] results = null;

        if (addAllFilter) {
            results = new SelectItem[size + 1];
            results[0] = ALL;
        } else {
            results = new SelectItem[size];
        }

        return results;
    }
}
