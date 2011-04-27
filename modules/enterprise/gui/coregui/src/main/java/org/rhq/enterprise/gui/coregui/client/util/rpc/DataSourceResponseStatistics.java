package org.rhq.enterprise.gui.coregui.client.util.rpc;

import java.util.ArrayList;
import java.util.Date;

import com.smartgwt.client.data.DSResponse;

import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 * Tracks responses received by {@link RPCDataSource}.
 * 
 * @author John Mazzitelli
 */
public class DataSourceResponseStatistics {

    public static class Record {
        public Date timestamp;
        public String requestId;
        public int status;
        public Integer totalRows;
        public Integer startRow;
        public Integer endRow;

        private Record(String requestId, DSResponse response) {
            this.timestamp = new Date();
            this.requestId = requestId;
            this.status = response.getStatus();
            this.totalRows = response.getTotalRows();
            this.startRow = response.getStartRow();
            this.endRow = response.getEndRow();
        }
    }

    private static boolean enableCollection = false;
    private static ArrayList<Record> statistics = new ArrayList<Record>();

    private DataSourceResponseStatistics() {
        // static access only
    }

    public static boolean isEnableCollection() {
        return enableCollection;
    }

    public static void setEnableCollection(boolean enabled) {
        enableCollection = enabled;
    }

    public static void record(String requestId, DSResponse response) {
        if (enableCollection) {
            Record record = new Record(requestId, response);
            statistics.add(record);
        }
    }

    public static ArrayList<Record> getAll() {
        return statistics;
    }

    public static void clearAll() {
        statistics.clear();
    }
}
