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
import dk.dma.ais.configuration.bus.AisBusConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.lang.System.exit;

/**
 * Created by oliver on 3/30/17.
 *
 * Functions borrowed from AisTrackServiceConfiguration
 */
public class AisBusConf {

    @Value("${dk.dma.enav.ais.web.AisBusConf.aisbusxml}")
    private String aisBusXml;

    private final static Logger LOG = LoggerFactory.getLogger(AisBusConf.class);

    public AisBus provideAisBus() {
        AisBus aisBus = null;
        try {
            AisBusConfiguration aisBusConf = AisBusConfiguration.load(aisBusConfiguration());
            aisBus = aisBusConf.getInstance();
        } catch (Exception e) {
            LOG.error("Failed to create AisBus", e);
            exit(-1);
        }
        return aisBus;
    }

    private InputStream aisBusConfiguration() throws IOException {
        InputStream is = null;

        if (!StringUtils.isBlank(aisBusXml)) {
            LOG.debug("Application properties say that aisbus.xml can be found in " + aisBusXml);

            Path aisBusXmlFile = Paths.get(aisBusXml);
            if (Files.exists(aisBusXmlFile) && Files.isReadable(aisBusXmlFile) && Files.isRegularFile(aisBusXmlFile) ) {
                LOG.debug(aisBusXml + " exists, is readable and regular. Using that for AisBus configuration.");
                LOG.info("Using " + aisBusXmlFile.toAbsolutePath().toString());
                is = Files.newInputStream(aisBusXmlFile);
            } else {
                LOG.debug("Application properties points to a file which does not exist or is not readable or regular.");
            }
        } else {
            LOG.debug("No location of aisbus.xml given in application properties.");
        }

        if (is == null) {
            LOG.info("Falling back to built-in default AisBus configuration.");
            is = this.getClass().getResourceAsStream("/aisbus.xml");
        }

        return is;
    }
}
