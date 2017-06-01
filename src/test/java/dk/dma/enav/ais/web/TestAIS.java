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

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import dk.dma.enav.ais.web.types.MessageWithTimeStamp;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * Created by Oliver on 01-11-2016.
 *
 * CouchDB must be running before tests can be run
 */
//@RunWith(SpringRunner.class)
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class TestAIS {

    //@LocalServerPort
    private int port;

    private static WebTarget target;

    private static File aisData = new File("target/test-classes/ais.txt");

    private static Scanner scanner;

    // whether the test class has been set up
    private static boolean hasBeenSetup;

    private static Response response;

    //@Before
    public void setUp() throws FileNotFoundException {
        if (!hasBeenSetup) {
            Client client = ClientBuilder.newClient();
            target = client.target("http://localhost:" + port);
            scanner = new Scanner(aisData);
            response = putAIS();
            hasBeenSetup = true;
        }
        scanner = new Scanner(aisData);
    }

    //@After
    public void tearDown() {
        scanner.close();
    }

    //@Test
    public void testPostAis() throws IOException, InterruptedException {
        String aisMessage = scanner.nextLine();

        Response response = target.path("/ais").request(MediaType.TEXT_PLAIN_TYPE).post(Entity.entity(aisMessage,
                MediaType.TEXT_PLAIN_TYPE));

        assertEquals(200, response.getStatus());
    }

//    @Test
//    public void testPutAis() throws InterruptedException {
//        // get the response from the PUT done on startup
//        Response response = this.response;
//
//        /* Make the thread wait a second so the Spring context does not get shut down before the message
//         * has been saved in the database
//         */
//        Thread.sleep(1000);
//
//        assertEquals(202, response.getStatus());
//    }

//    /*
//     * Tests getting the past 24 hours of messages for the mmsi number 477553000
//     */
//    @Test
//    public void testLast24Hours() {
//        String response = target.path("/last24Hours").queryParam("mmsi", "477553000").request().get(String.class);
//        Gson gson = new Gson();
//        JsonArray jsonElements = gson.fromJson(response, JsonArray.class);
//        DateTime now = DateTime.now(DateTimeZone.UTC);
//        DateTime yesterDay = now.minusDays(1);
//        jsonElements.forEach(jsonElement -> {
//            JsonObject jsonObject = jsonElement.getAsJsonObject();
//            DateTime timeStamp = DateTime.parse(jsonObject.get("timeStamp").getAsString());
//            assertTrue(timeStamp.isAfter(yesterDay) && timeStamp.isBefore(now));
//            assertEquals("477553000", jsonObject.getAsJsonObject("aisMessage").get("userId").getAsString());
//        });
//    }
//
//    /*
//     * Tests getting the latest message for the mmsi number 477553000
//     */
//    //@Test
//    public void testGetLatest() {
//        String response = target.path("/latest").queryParam("mmsi", "477553000").request().get(String.class);
//        Gson gson = new Gson();
//        JsonObject jsonObject = gson.fromJson(response, JsonObject.class);
//        DateTime now = DateTime.now(DateTimeZone.UTC);
//        // assert that the latest message is not older than 1 minute
//        DateTime before = now.minusMinutes(1);
//
//        // get the time stamp from the received message and check that it is correct
//        DateTime messageTime = DateTime.parse(jsonObject.get("timeStamp").getAsString());
//        assertTrue(messageTime.isBefore(now) && messageTime.isAfter(before));
//        assertEquals("477553000", jsonObject.getAsJsonObject("aisMessage").get("userId").getAsString());
//    }
//
//    /*
//     * Tests getting all unique mmsi numbers from the CouchDB database
//     */
//    @Test
//    public void testGetAllMMSINumbers() {
//        String response = target.path("/allVessels").request().get(String.class);
//        Gson gson = new Gson();
//        JsonArray jsonArray = gson.fromJson(response, JsonArray.class);
//        // assert that there are at least one element in returned array
//        assertTrue(jsonArray.size() > 0);
//        JsonObject expected = new JsonObject();
//        // assert that the mmsi number of the sample message is in the returned array
//        expected.addProperty("key", 477553000);
//        expected.addProperty("value", true);
//        assertTrue(jsonArray.contains(expected));
//    }
//
    // function used for sending an AIS message to the web service using a PUT operation
    private Response putAIS() {
        byte[] aisBytes = scanner.nextLine().getBytes();

        MessageWithTimeStamp message = new MessageWithTimeStamp();
        message.setMessage(aisBytes);
        message.setTimeStamp(new DateTime(DateTimeZone.UTC).toString());

        List<MessageWithTimeStamp> messageWithTimeStampList = new ArrayList<>();
        messageWithTimeStampList.add(message);

        Gson gson = new Gson();

        String json = gson.toJson(messageWithTimeStampList);

        Response response = target.path("/ais").request().put(Entity.entity(json, MediaType.APPLICATION_JSON_TYPE));
        return response;
    }

}
