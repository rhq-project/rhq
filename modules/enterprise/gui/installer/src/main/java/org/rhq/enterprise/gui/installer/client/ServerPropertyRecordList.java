package org.rhq.enterprise.gui.installer.client;

import java.util.HashMap;
import java.util.Map;

import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.RecordList;

/**
 * This provides a RecordList that is really a map underneath (each record is nothing more
 * than a name/value pair; that is, each record has two attributes: name and value).
 *
 * We use this because this RecordList will be the list backing the Advanced View grid.
 * So changing this list will be automatically reflected in that grid.
 *
 * @author John Mazzitelli
 */
public class ServerPropertyRecordList extends RecordList {

    public static final String PROPERTY_NAME = "n";
    public static final String PROPERTY_VALUE = "v";

    // this map is kept in sync with the record list
    private HashMap<String, String> map = new HashMap<String, String>();

    public HashMap<String, String> getMap() {
        return map;
    }

    public String getServerProperty(String name) {
        String value = map.get(name);
        return (value == null) ? "" : value;
    }

    public void putServerProperty(String name, String value) {
        // fill in the internal map
        map.put(name, value);

        // now update the record list
        Record found = find(PROPERTY_NAME, name);
        if (found == null) {
            Record record = new Record();
            record.setAttribute(PROPERTY_NAME, name);
            record.setAttribute(PROPERTY_VALUE, value);
            add(record);
        } else {
            found.setAttribute(PROPERTY_VALUE, value);
        }
    }

    public void replaceServerProperties(Map<String, String> newMap) {
        // first update the internal map
        map.clear();
        map.putAll(newMap);

        // now update the record list
        setLength(0);
        for (Map.Entry<String, String> entry : newMap.entrySet()) {
            putServerProperty(entry.getKey(), entry.getValue());
        }
    }
}
