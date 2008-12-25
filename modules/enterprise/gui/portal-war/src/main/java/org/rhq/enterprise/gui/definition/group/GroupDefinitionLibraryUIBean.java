package org.rhq.enterprise.gui.definition.group;

import java.util.ArrayList;
import java.util.List;

import javax.faces.model.SelectItem;

public class GroupDefinitionLibraryUIBean {

    private SelectItem[] groupedItems;
    private SelectItem[] simpleItems;

    public SelectItem getGroupedItemLabel() {
        SelectItem groupedItemLabel = new SelectItem("grouped", "One group for every...");
        groupedItemLabel.setDisabled(true);
        return groupedItemLabel;
    }

    public SelectItem[] getGroupedItems() {
        if (groupedItems == null) {
            List<SelectItem> list = new ArrayList<SelectItem>();
            add(list, "JBossAS clusters in the system", "groupby resource.trait[partitionName]",
                "resource.type.plugin = JBossAS", "resource.type.name = JBossAS Server");
            add(list, "Clustered enterprise application archive (EAR)", "groupby resource.parent.trait[partitionName]",
                "groupby resource.name", "resource.type.plugin = JBossAS",
                "resource.type.name = Enterprise Application (EAR)");
            add(list, "Unique BossAS versions in inventory",
                "groupby resource.trait[jboss.system:type=Server:VersionName]", "resource.type.plugin = JBossAS",
                "resource.type.name = JBossAS Server");
            add(list, "Platform resource in inventory", "resource.type.category = PLATFORM", "groupby resource.name");
            add(list, "Unique resource type in inventory", "groupby resource.type.plugin", "groupby resource.type.name");
            groupedItems = list.toArray(new SelectItem[list.size()]);
        }
        return groupedItems;
    }

    public SelectItem getSimpleItemLabel() {
        SelectItem simpleItemLabel = new SelectItem("simple", "Exactly one group containing...");
        simpleItemLabel.setDisabled(true);
        return simpleItemLabel;
    }

    public SelectItem[] getSimpleItems() {
        if (simpleItems == null) {
            List<SelectItem> list = new ArrayList<SelectItem>();
            add(list, "All JBossAS hosting any version of 'my' app", "resource.type.plugin = JBossAS",
                "resource.type.name = JBossAS Server", "resource.child.name.contains = my");
            add(list, "All Non-secured JBossAS servers", "empty resource.pluginConfiguration[principal]",
                "resource.type.plugin = JBossAS", "resource.type.name = JBossAS Server");
            add(list, "All resources currently down", "resource.availability = DOWN");
            simpleItems = list.toArray(new SelectItem[list.size()]);
        }
        return simpleItems;
    }

    private void add(List<SelectItem> items, String label, String... expressions) {
        StringBuilder builder = new StringBuilder();
        int i = 0;
        for (String expression : expressions) {
            if (i++ != 0) {
                builder.append(";");
            }
            builder.append(expression);
        }
        SelectItem result = new SelectItem(builder.toString(), " - " + label);
        items.add(result);
    }

}
