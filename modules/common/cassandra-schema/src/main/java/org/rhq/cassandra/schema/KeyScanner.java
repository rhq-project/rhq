package org.rhq.cassandra.schema;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.datastax.driver.core.Host;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.google.common.base.Objects;
import com.google.common.base.Stopwatch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.cassandra.schema.exception.KeyScanException;
import org.rhq.core.util.exception.ThrowableUtil;

/**
 * @author John Sanda
 */
public class KeyScanner {

    private static final Log log = LogFactory.getLog(KeyScanner.class);

    private static final int QUERY_FAILURE_THRESHOLD = 5;

    private static class TokenRange {
        long startToken;
        long endToken;

        public TokenRange(long startToken, long endToken) {
            this.startToken = startToken;
            this.endToken = endToken;
        }

        @Override
        public String toString() {
            return Objects.toStringHelper("TokenRange").add("start", startToken).add("end", endToken).toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            TokenRange that = (TokenRange) o;

            if (endToken != that.endToken) return false;
            if (startToken != that.startToken) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = (int) (startToken ^ (startToken >>> 32));
            result = 31 * result + (int) (endToken ^ (endToken >>> 32));
            return result;
        }
    }

    private Session session;

    private List<TokenRange> tokenRanges = new ArrayList<TokenRange>();

    private ExecutorService threadPool;

    public KeyScanner(Session session) {
        this.session = session;
        Queue<Host> hosts = new ArrayDeque<Host>(session.getCluster().getMetadata().getAllHosts());
        threadPool = Executors.newFixedThreadPool(getThreadCount(hosts.size()));

        if (hosts.size() == 1) {
            // If it is a single node cluster then we have to query the system.local to get
            // the tokens.
            ResultSet resultSet = session.execute("select tokens from system.local");
            loadTokens(resultSet);
        } else {
            // If we have a multi-node cluster, things are a little more involved. Each
            // node stores its own tokens in system.local, and it stores tokens for all
            // of the other nodes in system.peers. This code assumes we are still using a
            // round robin load balancing policy with the driver. So if we are trying to
            // load tokens for node n1 and if we are also querying n1, then we will get an
            // empty result set. We need to execute the query again so that it is routed to
            // a different node, e.g., n2, which will have a row in system.peers for n1.
            // Switching to a token aware policy would simplify this.

            PreparedStatement query = session.prepare("select tokens from system.peers where peer = ?");

            while (!hosts.isEmpty()) {
                Host host = hosts.poll();
                log.info("Loading tokens for " + host);
                ResultSet resultSet = session.execute(query.bind(host.getAddress()));
                if (resultSet.isExhausted()) {
                    for (Host nextHost : hosts) {
                        resultSet = session.execute(query.bind(host.getAddress()));
                        if (!resultSet.isExhausted()) {
                            break;
                        }
                    }
                }
                if (resultSet.isExhausted()) {
                    throw new IllegalStateException("Failed to load tokens for " + host);
                }
                loadTokens(resultSet);
            }
        }
    }

    private int getThreadCount(int numNodes) {
        String count = System.getProperty("rhq.storage.key-scanner.thread-count");
        if (count == null) {
            return Math.min(4 + ((numNodes - 1) * 4), 16);
        }
        return Integer.parseInt(count);
    }

    private void loadTokens(ResultSet resultSet) {
        List<Row> rows = resultSet.all();
        Set<String> stringTokens = rows.get(0).getSet(0, String.class);
        SortedSet<Long> tokens = new TreeSet<Long>();

        for (String s : stringTokens) {
            tokens.add(Long.parseLong(s));
        }

        Iterator<Long> iterator = tokens.iterator();
        long start = iterator.next();
        long end;

        while (iterator.hasNext()) {
            end = iterator.next();
            tokenRanges.add(new TokenRange(start, end));
            start = end;
        }
        start = tokens.first();
        end = tokens.last();
        tokenRanges.add(new TokenRange(end, start));
    }

    public void shutdown() {
        log.info("Shutting down");
        threadPool.shutdownNow();
    }

    public Set<Integer> scanFor1HourKeys() throws InterruptedException, AbortedException {
        return scanForKeys("one_hour_metrics", 1011);
    }

    public Set<Integer> scanFor6HourKeys() throws InterruptedException, AbortedException {
        return scanForKeys("six_hour_metrics", 375);
    }

    public Set<Integer> scanFor24HourKeys() throws InterruptedException, AbortedException {
        return scanForKeys("twenty_four_hour_metrics", 1098);
    }

    private Set<Integer> scanForKeys(String table, int batchSize) throws InterruptedException, AbortedException {
        log.info("Scanning for keys for table " + table);
        Stopwatch stopwatch = Stopwatch.createStarted();

        Set<Integer> keys = new ConcurrentSkipListSet<Integer>();
        PreparedStatement query = session.prepare(
            "SELECT token(schedule_id), schedule_id " +
            "FROM rhq." + table + " " +
            "WHERE token(schedule_id) >= ? LIMIT " + batchSize);
        TaskTracker taskTracker = new TaskTracker();

        for (TokenRange range : tokenRanges) {
            taskTracker.addTask();
            threadPool.submit(tokenScanner(range, query, keys, taskTracker, batchSize));
        }
        taskTracker.finishedSchedulingTasks();
        taskTracker.waitForTasksToFinish();

        stopwatch.stop();
        log.info("Finished scanning for keys for table " + table + " in " + stopwatch.elapsed(TimeUnit.MILLISECONDS) +
            " ms");

        return keys;
    }

    private Runnable tokenScanner(final TokenRange tokenRange, final PreparedStatement query,
        final Set<Integer> scheduleIds, final TaskTracker taskTracker, final int batchSize) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    long token = tokenRange.startToken;
                    long lastToken = tokenRange.startToken;

                    while (token <= tokenRange.endToken) {
                        ResultSet resultSet = executeTokenQuery(query, token);
                        int count = 0;
                        for (Row row : resultSet) {
                            lastToken = row.getLong(0);
                            scheduleIds.add(row.getInt(1));
                            ++count;
                        }
                        if (count < batchSize) {
                            break;
                        }
                        token = lastToken + 1;
                    }
                    taskTracker.finishedTask();
                } catch (KeyScanException e) {
                    taskTracker.abort(e.getMessage());
                } catch (Exception e) {
                    log.error("There was an unexpected error scanning for tokens", e);
                    taskTracker.abort("Aborting due to unexpected error: " + ThrowableUtil.getRootMessage(e));
                }
            }
        };
    }

    private ResultSet executeTokenQuery(PreparedStatement query, long token) throws KeyScanException {

        for (int i = 0; i < QUERY_FAILURE_THRESHOLD; ++i) {
            try {
                return session.execute(query.bind(token));
            } catch (Exception e) {
                if (log.isDebugEnabled()) {
                    log.debug("Failed to execute token query", e);
                } else {
                    log.info("Failed to execute token query: " + ThrowableUtil.getRootMessage(e));
                }
            }
        }
        throw new KeyScanException("Token query failed " + QUERY_FAILURE_THRESHOLD +
            " times. The key scan will abort due to these failures.");
    }
}
