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

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.DeadEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.jmingo.context.Context;
import org.jmingo.exceptions.MingoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Used to transfer events from a profiler to the registered benchmark services using Guava event bus.
 */
public class MetricsTransfer {

    public static final int THREADS = 20; // todo should be configurable
    private ExecutorService eventBusThreadPool = Executors.newFixedThreadPool(THREADS);
    private EventBus eventBus = new AsyncEventBus("EventBus_" + getClass().getSimpleName(), eventBusThreadPool);
    private static final Logger LOGGER = LoggerFactory.getLogger(MetricsTransfer.class);

    public MetricsTransfer() {
        eventBus.register(new MetricsEventHandler());
    }

    /**
     * Sends the given event to the registered benchmark services.
     *
     * @param metricsEvent the metrics event
     */
    public void sendEvent(MetricsEvent metricsEvent) {
        eventBus.post(metricsEvent);
    }

    /**
     * Handler to handle {@link MetricsEvent} events.
     */
    private static class MetricsEventHandler {

        /**
         * Handles metrics events and gets {@link Metrics} from it to send
         * to the registered benchmark services.
         *
         * @param metricsEvent the metrics event
         */
        @Subscribe
        @AllowConcurrentEvents
        public void handle(MetricsEvent metricsEvent) {
            Context context = Context.getCurrent();
            if (context == null) {
                throw new MingoException("Failed to get Mingo Context instance");
            }
            context.getBenchmarkServices().forEach((service) -> {
                service.submit(metricsEvent.getMetrics());
            });

        }

        /**
         * Handles dead events and logs it.
         *
         * @param deadEvent the dead event
         */
        @Subscribe
        @AllowConcurrentEvents
        public void handle(DeadEvent deadEvent) {
            LOGGER.warn("Unknown event: {}", deadEvent);
        }
    }
}