package org.apache.asterix.external.statistics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.TimerTask;

/**
 * Increments the count for each record seen and logs count + elapsed time at intervals
 * TIME,COUNT
 */

public class Statistics extends TimerTask {

    private Path path;
    private Long startTime;
    private Long count;

    public Statistics() {
        this.path = Paths.get("/Users/thormartin/asterix-machine-learning/src/main/resources/logs/tps.log");
        this.count = 0L;
    }

    public void log(Long count) {
        if (startTime == null) {
            startTime = System.currentTimeMillis();
        }
        Long timeDelta = System.currentTimeMillis() - startTime;
        String logEntry = Long.toString(timeDelta) + "," + Long.toString(count) + "\n";
        try {
            Files.write(path, logEntry.getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addToCount(long i) {
        this.count += i;
    }

    public void run() {
        log(this.count);
        this.count = 0L;
    }
}
