/*
 * Copyright 2017 Danish Maritime Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dk.dma.enav.ais.web;

import dk.dma.ais.bus.AisBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by oliver on 3/31/17.
 */
public class AisBusInitializer {

    private AisBus aisBus;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private final static Logger LOG = LoggerFactory.getLogger(AisBusInitializer.class);

    public AisBusInitializer(AisBus aisBus) {
        this.aisBus = aisBus;
        this.onContextRefresh();
    }

    private void onContextRefresh() {
        Objects.requireNonNull(aisBus);
        executorService.submit(() -> {
            LOG.info("Starting AisBus");
            aisBus.start();
            aisBus.startConsumers();
            aisBus.startProviders();
        });


        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (aisBus != null) aisBus.cancel();
            executorService.shutdownNow();
            LOG.info("AisBus stopped");
        }));
    }

    public AisBus getAisBus() {
        return this.aisBus;
    }
}
