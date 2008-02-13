package net.hyperic.hq.product.apache;

import net.hyperic.hq.product.LogFileTailPlugin;
import net.hyperic.hq.product.TrackEvent;
import net.hyperic.sigar.FileInfo;

public class ApacheErrorLogPlugin
    extends LogFileTailPlugin {

    private static final String[] LOG_LEVELS = {
        "emerg,alert,crit,error", //Error
        "warn", //Warning
        "info,notice", //Info
        "debug" //Debug
    };

    public String[] getLogLevelAliases() {
        return LOG_LEVELS;
    }

    public TrackEvent processLine(FileInfo info, String line) {
        ApacheErrorLogEntry entry = new ApacheErrorLogEntry();
        if (!entry.parse(line)) {
            return null;
        }

        return newTrackEvent(System.currentTimeMillis(), //XXX parse entry.timeStamp
                             entry.level,     
                             info.getName(),
                             entry.message);
    }

    class ApacheErrorLogEntry {
        String message;
        String timeStamp;
        String level;
        
        //XXX need charAt sanity checks
        public boolean parse(String line) {
            //parse "[Wed Jan 21 20:47:35 2004] "
            if (line.charAt(0) != '[') {
                return false;
            }
            int ix = line.indexOf("]");
            if (ix == -1) {
                return false;
            }
            this.timeStamp = line.substring(1, ix);
        
            ix++;
            while (line.charAt(ix) == ' ') {
                ix++;
            }

            //parse "[error] "
            line = line.substring(ix);
            if (line.charAt(0) != '[') {
                return false;
            }
            ix = line.indexOf("]");
            if (ix == -1) {
                return false;
            }
            this.level = line.substring(1, ix);

            //rest is the message
            ix++;
            while (line.charAt(ix) == ' ') {
                ix++;
            }
            this.message = line.substring(ix);

            return true;
        }
    }
}
