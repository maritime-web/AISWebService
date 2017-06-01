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
import dk.dma.ais.message.AisMessage;
import dk.dma.ais.packet.AisPacket;
import dk.dma.ais.sentence.SentenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * Created by Oliver on 30-10-2016.
 */
@RestController
public class AISWebService {

    private final Logger log = LoggerFactory.getLogger(AISWebService.class);

    @Autowired
    private AisBus aisBus;

    @PostConstruct
    private void postConstruct() {
        //AisBusConf aisBusConf = new AisBusConf();
        //AisBus aisBus = aisBusConf.provideAisBus();
        AisBusInitializer aisBusInitializer = new AisBusInitializer(aisBus);
        this.aisBus = aisBusInitializer.getAisBus();
    }

    // to be able to receive message as a list of messages of String or byte[]
    @RequestMapping(method = RequestMethod.POST, path = "/ais", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity postAISBytes(@RequestBody List<Object> messages) {
        Runnable worker = () -> messages.forEach(message -> {
            AisPacket aisPacket = null;
            if (message instanceof byte[]) {
                aisPacket = AisPacket.fromByteArray((byte[]) message);
            } else if (message instanceof String) {
                try {
                    aisPacket = AisPacket.readFromString((String) message);
                } catch (SentenceException e) {
                    log.error(e.getMessage());
                }
            }
            if (aisPacket != null) {
                log.info(aisPacket.getStringMessage());
                aisBus.push(aisPacket);
            }
        });
        // start parsing and pushing messages to AISBus in a new thread
        // and then send response with status 202 accepted to client
        new Thread(worker).start();
        return ResponseEntity.accepted().build();
    }
}
