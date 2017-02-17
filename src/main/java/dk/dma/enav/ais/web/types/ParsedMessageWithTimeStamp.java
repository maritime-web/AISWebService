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

package dk.dma.enav.ais.web.types;

import dk.dma.ais.message.AisMessage;

/**
 * Created by Oliver on 04-12-2016.
 */
public class ParsedMessageWithTimeStamp {
    private AisMessage aisMessage;
    private String timeStamp;

    public ParsedMessageWithTimeStamp(AisMessage aisMessage, String timeStamp) {
        this.aisMessage = aisMessage;
        this.timeStamp = timeStamp;
    }

    public AisMessage getAisMessage() {
        return aisMessage;
    }

    public void setAisMessage(AisMessage aisMessage) {
        this.aisMessage = aisMessage;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }
}
