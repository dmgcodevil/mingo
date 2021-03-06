/**
 * Copyright 2013-2014 The JMingo Team
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jmingo.benchmark;


import java.util.concurrent.TimeUnit;

/**
 * This profiler is used to measure execution time of queries.
 */
public class QueryProfiler implements Profiler {

    private final String queryId;
    private long startTime;
    private MetricsTransfer metricsTransfer;

    /**
     * Constructor with parameters.
     *
     * @param queryId         the query id to profile
     * @param metricsTransfer the metrics transfer to send metrics to a benchmark services
     */
    public QueryProfiler(String queryId, MetricsTransfer metricsTransfer) {
        this.queryId = queryId;
        this.metricsTransfer = metricsTransfer;
    }

    /**
     * Starts profiling and stores start time in nanoseconds.
     *
     * @return current profiler
     */
    @Override
    public Profiler start() {
        this.startTime = System.nanoTime();
        return this;
    }

    /**
     * Stops profiling. Calculates execution time and sends metrics through metrics transfer.
     */
    @Override
    public void stop() {
        long executionTime = System.nanoTime() - startTime;
        Metrics metrics = new Metrics.Builder()
                .name(queryId)
                .timeUnit(TimeUnit.NANOSECONDS)
                .startTime(startTime)
                .executionTime(executionTime).build();
        profile(metrics);
    }

    private void profile(Metrics metrics) {
        metricsTransfer.sendEvent(new MetricsEvent(metrics));
    }

}
