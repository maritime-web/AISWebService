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

/**
 * Created by Oliver on 08-01-2017.
 */
$(document).ready(function () {

    var pointLayer = new ol.layer.Vector({
        source: new ol.source.Vector({
            features: []
        })
    });

    // create the OpenLayers map
    var map = new ol.Map({
        target: 'map',
        layers: [
            new ol.layer.Tile({
                source: new ol.source.OSM()
            }),
            pointLayer
        ],
        view: new ol.View({
            center: ol.proj.fromLonLat([0, 0]),
            zoom: 2
        })
    });

    // get the type selector
    var typeSelect = $('#type');
    //get the submit button
    var submitButton = $('#submit');

    // get the searching selector
    var search = $('#search');

    // create the popup
    var popup = new ol.Overlay({
        element: document.getElementById('popup')
    });
    map.addOverlay(popup);

    // keeps all the available mmsi numbers
    var lastAllVesselsSearch;
    getAllVessels();

    // when the searching field is clicked check if there are any new mmsi numbers
    search.on('shown.bs.select', function () {
        getAllVessels();
    });

    // add click listener for the submit button
    submitButton.click(function (event) {
        event.preventDefault();
        var mmsi = $('#search').val();
        if (mmsi) {
            var type = typeSelect.val();

            switch(type) {
                case 'latest':
                    // get the latest ais message of the specified mmsi number
                    $.getJSON("/latest", {mmsi: mmsi})
                        .done(function (data) {
                            var timeStamp = data.timeStamp;
                            var aisMessage = data.aisMessage;
                            var latitude = aisMessage.pos.rawLatitude / 600000.0;
                            var longitude = aisMessage.pos.rawLongitude / 600000.0;
                            console.log(latitude + " " + longitude);
                            var coordinates = ol.proj.fromLonLat([longitude, latitude]);
                            map.getView().setCenter(coordinates);
                            map.getView().setZoom(10);
                            // render the map before drawing the popup to avoid it being drawn wrong
                            pointLayer.getSource().clear();
                            map.renderSync();
                            var vesselFeature = makeVesselFeature(timeStamp, aisMessage, coordinates);
                            pointLayer.getSource().addFeature(vesselFeature);
                            drawPopup(timeStamp, aisMessage, coordinates);
                        });
                    break;
                case 'past24Hours':
                    $.getJSON("/last24Hours", {mmsi: mmsi})
                        .done(function (data) {
                            if (data.length > 0) {
                                // make a vessel feature for all received messages
                                var vesselFeatures = [];
                                data.forEach(function (message) {
                                    var aisMessage = message.aisMessage;
                                    var position = aisMessage.pos;
                                    var latitude = position.rawLatitude / 600000.0;
                                    var longitude = position.rawLongitude / 600000.0;
                                    var coordinate = ol.proj.fromLonLat([longitude, latitude]);
                                    var timeStamp = message.timeStamp;
                                    var vesselFeature = makeVesselFeature(timeStamp, aisMessage, coordinate);
                                    vesselFeatures.push(vesselFeature);
                                });

                                // destroy the current popup if it is visible
                                var element = popup.getElement();
                                $(element).popover('destroy');
                                map.getView().setCenter(vesselFeatures[0].getGeometry().getCoordinates());
                                map.getView().setZoom(8);
                                pointLayer.getSource().clear();
                                map.renderSync();
                                pointLayer.getSource().addFeatures(vesselFeatures);

                                // draw a line between all the vessel features
                                var coordinates = [];
                                vesselFeatures.forEach(function (feature) {
                                    coordinates.push(feature.getGeometry().getCoordinates());
                                });
                                var lineStyle = new ol.style.Style({
                                    stroke: new ol.style.Stroke({
                                        color: '#ff0000',
                                        width: 4
                                    })
                                });
                                var lineString = new ol.Feature({
                                    geometry: new ol.geom.LineString(coordinates)
                                });
                                lineString.setStyle(lineStyle);
                                pointLayer.getSource().addFeature(lineString);
                            } else {
                                alert("No information for the past 24 hours was found for vessel")
                            }
                        });
                    break;
            }
        }
    });

    // how the location point should look like
    var pointStyle = new ol.style.Style({
        image: new ol.style.Circle({
            radius: 6,
            fill: new ol.style.Fill({color: '#ff0000'})
        })
    });

    // get the list of all available mmsi numbers from the web service and store it in a local variable
    function getAllVessels() {
        $.getJSON("/allVessels")
            .done(function (data) {
                // check if the list of mmsi numbers has changed since last time
                if (!_.isEqual(data, lastAllVesselsSearch)) {
                    search.empty();
                    data.forEach(function (entry) {
                        var mmsi = entry.key;
                        search.append('<option value="' + mmsi + '">' + mmsi + '</option>');
                    });
                    search.selectpicker('refresh');
                    lastAllVesselsSearch = data;
                }
            });
    }

    // function for making a vessel feature that has a timestamp, an ais message and a position
    function makeVesselFeature(timeStamp, aisMessage, coordinate) {
        var vesselFeature = new ol.Feature({
            timeStamp: timeStamp,
            aisMessage: aisMessage,
            geometry: new ol.geom.Point(coordinate)
        });
        vesselFeature.setStyle(pointStyle);
        return vesselFeature;
    }

    // function for drawing a popup
    function drawPopup(timeStamp, aisMessage, coordinate) {
        var element = popup.getElement();
        // destroy the current popup
        $(element).popover('destroy');
        popup.setPosition(coordinate);
        var hdms = ol.coordinate.toStringHDMS(ol.proj.transform(coordinate, 'EPSG:3857', 'EPSG:4326'));
        var speed = aisMessage.sog / 10.0;
        var course = aisMessage.cog / 10.0;
        var navStatus = aisMessage.navStatus;
        // set the content of the popup
        $(element).popover({
            'placement': 'top',
            'animation': false,
            'html': true,
            'content': '<p> MMSI: ' + aisMessage.userId + '</p>' +
                '<p>Position: ' + hdms + '</p>' +
                '<p>Speed: ' + speed + ' knots' + '</p>' +
                '<p>Course: ' + course + '&deg;' + '</p>' +
                '<p>Navigational Status: ' + getNavStatus(navStatus) + '</p>' +
                '<p>Message Received: ' + timeStamp + '</p>'
        });
        // show the popup
        $(element).popover('show');
    }

    // function for getting the navigational status based on a integer
    function getNavStatus(statusInt) {
        switch (statusInt) {
            case 0:
                return "Under way using engine";
            case 1:
                return "At anchor";
            case 2:
                return "Not under command";
            case 3:
                return "Restricted manoeuverability";
            case 4:
                return "Constrained by her draught";
            case 5:
                return "Moored";
            case 6:
                return "Aground";
            case 7:
                return "Engaged in fishing";
            case 8:
                return "Under way sailing";
            default:
                return "Not defined";
        }
    }

    // click listener on the map
    map.on('singleclick', function (evt) {
        // search for a feature in the point layer at the click location with a hit tolerance of 10
        var feature = map.forEachFeatureAtPixel(evt.pixel, function (feature) {
            return feature;
        }, 10, function (layer) {
            return layer === pointLayer;
        });

        // check if there is a feature at the position
        if (feature) {
            // check if the feature is a vessel feature
            if (feature.get('timeStamp')) {
                var timeStamp = feature.get('timeStamp');
                var aisMessage = feature.get('aisMessage');
                var coordinate = feature.getGeometry().getCoordinates();

                drawPopup(timeStamp, aisMessage, coordinate);
            }
        } else {
            var element = popup.getElement();
            $(element).popover('destroy');
        }
    });
});
