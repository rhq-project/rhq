package org.rhq.core.pluginapi.event.log;

import static java.util.Arrays.asList;
import static org.testng.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.Test;

/**
 * @author John Sanda
 */
public class Log4JEventsTest {

    static class Producer implements Runnable {

        private Log log = LogFactory.getLog(Producer.class);

        private File logFile;

        private CountDownLatch latch;

        private AtomicInteger failures;

        public Producer(File logFile, CountDownLatch latch, AtomicInteger failures) {
            this.logFile = logFile;
            this.latch = latch;
            this.failures = failures;
        }

        @Override
        public void run() {
            log.info("Processing log file " + logFile);
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(logFile));
                Log4JLogEntryProcessor logProcessor = new Log4JLogEntryProcessor("logEvent", logFile);
                logProcessor.processLines(reader);
                log.info("Finished processing log file " + logFile);
            } catch (Exception e) {
                failures.incrementAndGet();
                log.error("An error occurred processing " + logFile, e);
            } finally {
                latch.countDown();
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                    }
                }
            }
        }
    }

    @Test
    public void processMultipleEventStreams() throws Exception {
        AtomicInteger failures = new AtomicInteger(0);
        List<File> logFiles = asList(
            new File(getClass().getResource("/rhq-storage.log.25").toURI()),
            new File(getClass().getResource("/server.log.2013-09-11-21").toURI()),
            new File(getClass().getResource("/agent.log.1").toURI()),
            new File(getClass().getResource("/rhq-storage.log.20").toURI()),
            new File(getClass().getResource("/server.log.2013-09-11-14").toURI()),
            new File(getClass().getResource("/rhq-storage.log.20").toURI()),
            new File(getClass().getResource("/server.log.2013-09-11-15").toURI()),
            new File(getClass().getResource("/agent.log").toURI()),
            new File(getClass().getResource("/rhq-storage.log.30").toURI())
        );
        CountDownLatch latch = new CountDownLatch(logFiles.size());
        for (File file : logFiles) {
            new Thread(new Producer(file, latch, failures)).start();
        }
        latch.await();
        assertEquals(failures.intValue(), 0, "There were parsing errors");
    }

}
