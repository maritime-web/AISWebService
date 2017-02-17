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

import com.google.gson.JsonObject;
import dk.dma.ais.binary.SixbitException;
import dk.dma.ais.message.AisMessage;
import dk.dma.ais.message.AisMessageException;
import dk.dma.ais.packet.AisPacket;
import dk.dma.ais.sentence.SentenceException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.lightcouch.CouchDbClient;
import dk.dma.enav.ais.web.types.MessageWithTimeStamp;
import dk.dma.enav.ais.web.types.ParsedMessageWithTimeStamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Oliver on 30-10-2016.
 */
@RestController
public class AISWebService {

    @Autowired
    private CouchDbClient couchDbClient;

    private final Logger log = LoggerFactory.getLogger(AISWebService.class);


    // to be able to receive message as plain string
    @RequestMapping(method = RequestMethod.POST, path = "/ais")
    public ResponseEntity postAIS(@RequestBody String aivdmString) {
        AisMessage aisMessage;
        try {
            aisMessage = AisPacket.readFromString(aivdmString).tryGetAisMessage();
            couchDbClient.save(aisMessage);
        } catch (SentenceException e) {
            log.error(e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        if (aisMessage == null) {
            return ResponseEntity.badRequest().body("AIS string is malformed");
        }
        return ResponseEntity.ok().build();
    }

    // to be able to receive message as a list of messages with timestamps
    @RequestMapping(method = RequestMethod.PUT, path = "/ais", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity putAIS(@RequestBody List<MessageWithTimeStamp> messages) {
        Runnable worker = () -> {
            List<ParsedMessageWithTimeStamp> toBeStored = new ArrayList<>();
            messages.forEach(message -> {
                try {
                    AisMessage aisMessage = AisPacket.fromByteArray(message.getMessage()).getAisMessage();
                    toBeStored.add(new ParsedMessageWithTimeStamp(aisMessage, message.getTimeStamp()));
                } catch (AisMessageException e) {
                    log.error(e.getMessage());
                } catch (SixbitException e) {
                    log.error(e.getMessage());
                }
            });
            couchDbClient.bulk(toBeStored, false);
        };
        // start parsing and saving messages to database in a new thread
        // and then send response with status 202 accepted to client
        new Thread(worker).start();
        return ResponseEntity.accepted().build();
    }

    // Get the past 24 hours of messages of a ship with specified mmsi number
    @RequestMapping(method = RequestMethod.GET, path = "/last24Hours", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getLast24Hours(@RequestParam(name = "mmsi") int mmsi) {
        DateTime now = DateTime.now(DateTimeZone.UTC);
        DateTime yesterDay = now.minusDays(1);

        List<JsonObject> messages = couchDbClient.view("funcs/by_userIdAndDate")
                .includeDocs(true)
                .startKey(mmsi, yesterDay.toString())
                .endKey(mmsi, now.toString())
                .reduce(false).query(JsonObject.class);
        return ResponseEntity.ok(couchDbClient.getGson().toJson(messages));
    }

    // get the latest message of vessel with specified mmsi
    @RequestMapping(method = RequestMethod.GET, path = "/latest", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getLatestMessage(@RequestParam(name = "mmsi") int mmsi) {
        DateTime now = DateTime.now(DateTimeZone.UTC);

        List<JsonObject> messages = couchDbClient.view("funcs/by_userIdAndDate")
                .includeDocs(true)
                .endKey(mmsi, "")
                .startKey(mmsi, now.toString())
                .reduce(false).descending(true).query(JsonObject.class);

        if (messages.isEmpty() || messages == null) {
            return ResponseEntity.notFound().build();
        }
        JsonObject latest = messages.get(0);

        return ResponseEntity.ok(couchDbClient.getGson().toJson(latest));
    }

    // get all unique mmsi numbers that are stored in database
    @RequestMapping(method = RequestMethod.GET, path = "/allVessels", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getAllMMSINumbers() {
        List<JsonObject> response = couchDbClient.view("funcs/by_userId")
                .includeDocs(false)
                .reduce(true).group(true)
                .query(JsonObject.class);

        return ResponseEntity.ok(couchDbClient.getGson().toJson(response));
    }
}
