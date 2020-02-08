/**
 *  MQTT Window Blinds
 *
 *  Copyright 2019 Joel Wetzel
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */

metadata {
        definition (name: "MQTT Window Blinds", namespace: "joelwetzel", author: "Joel Wetzel") {
		capability "Actuator"
        capability "WindowShade"
        capability "Refresh"
            
        attribute "mqttClientStatus", "string"
        attribute "setPosition", "string"
    }
    preferences {
		section {
            input (
				type: "String",
				name: "mqttHubIp",
				title: "MQTT Hub IP Address",
				required: true,
				defaultValue: "192.168.1.230"
			)
            input (
				type: "Number",
				name: "stepsToClose",
				title: "Number of steps to fully closed",
				required: true,
				defaultValue: 8
			)
        }
    }
}

def parse(String description) {
    //log.debug "parse(${description})"
    
    def decoded = interfaces.mqtt.parseMessage(description)
    log.debug "parse(${decoded})"
    
    if (decoded.topic == topicName("position")) {
        def iPosition = decoded.payload.toDouble()
        def setPosition = device.currentValue("setPosition").toDouble()
        def hPosition = convertEsp8266PositionToHubitatPosition(iPosition)
        
        sendEvent(name: "position", value: hPosition, isStateChange: true)
        
        if (iPosition == setPosition) {
            //log.debug "Reached target position."
            
            
            if (hPosition == 0) {
                sendEvent(name: "windowShade", value: "closed", isStateChange: true)
            }
            else if (hPosition >= 99) {
                sendEvent(name: "windowShade", value: "open", isStateChange: true)
            }
            else {
                sendEvent(name: "windowShade", value: "partially open", isStateChange: true)
            }
        }
    }    
}

def open() {
    setPosition(100)
}

def close() {
    setPosition(0)
}

def convertHubitatPositionToEsp8266Position(hubitatPosition) {
    def inverted = 100 - hubitatPosition
    def scaled = inverted / 100 * stepsToClose
    return scaled
}

def convertEsp8266PositionToHubitatPosition(espPosition) {
    def scaled = espPosition / stepsToClose * 100
    def inverted = 100 - scaled
    return inverted
}

def setPosition(position) {
    // Position in Hubitat is a number from 0 to 100.  Open is 100.  Closed is 0.
    // On the ESP8266, position is 0 at open, and something like 8 at closed.
    // So we need a mapping function to translate.
    // In software architecture terms, it would make more sense for this mapping to happen
    // on the ESP8266, to keep the MQTT messages matching the Hubitat capability definition.
    // However, changing parameters there requires recompiling and deploying firmware, so I
    // have the mapping happening on the hub, where it's easy to change parameters.
    
    if (position > device.currentValue("position")) {
        sendEvent(name: "windowShade", value: "opening", isStateChange: true)
    }
    else if (position < device.currentValue("position")) {
        sendEvent(name: "windowShade", value: "closing", isStateChange: true)
    }
    else {
        return
    }
    
    def espPosition = convertHubitatPositionToEsp8266Position(position)
    
    sendEvent(name: "setPosition", value: espPosition, isStateChange: true)
    interfaces.mqtt.publish(topicName("setPosition"), espPosition.toString())
    
    // Simulate the MQTT device response for now.
    //runIn(2, isSetPosition)
}

def isSetPosition() {
    interfaces.mqtt.publish(topicName("position"), device.currentValue("setPosition"))
}




def installed() {
    log.info "installed()"
    
    initialize()
}

def updated() {
    log.info "updated()"
    
    initialize()
}

def refresh() {
    log.info "refresh()"
    
    initialize()
}

def initialize() {
    log.info "initialize()"
    
    if (interfaces.mqtt.isConnected()) {
        log.info "Disconnecting..."
        interfaces.mqtt.disconnect()
    }
        
    runIn(1, connectToMqtt)
}

def uninstalled() {
    interfaces.mqtt.disconnect()
}

def mqttClientStatus(String status) {
    log.debug "mqttClientStatus(${status})"

    if (status.take(6) == "Error:") {
        log.debug "Connection error..."
        
        try {
            interfaces.mqtt.disconnect()  // clears buffers
        }
        catch (e) {
        }
        
        log.info("Attempting to reconnect in 5 seconds...");
        runIn(5, connectToMqtt)
    }
    else {
        log.debug "Connected!"
    
        runIn(1, open)
    }
    
    if (status != null)
    {
        sendEvent(name: "mqttClientStatus", value: status, isStateChange: true)
    }    
}

def connectToMqtt() {
    if (!interfaces.mqtt.isConnected()) {        
        log.info "Connecting to MQTT..."
        interfaces.mqtt.connect("tcp://${mqttHubIp}:1883", device.getDeviceNetworkId() + "driver", null, null)
        
        // Subscribe to the attributes
        interfaces.mqtt.subscribe(topicName("position"))
        
        // Subscribe to the commands.  Commented out because I'm not letting commands come in through MQTT.
        //interfaces.mqtt.subscribe(topicName("on"))
        //interfaces.mqtt.subscribe(topicName("off"))
    }
}

def topicName(String attributeOrCommand) {
    // We're using the "homie" mqtt convention.
    // https://homieiot.github.io
        
    def topicBase = "hubitat/${device.getDeviceNetworkId()}"
    
    def topicName = "${topicBase}/windowShade/${attributeOrCommand}"
    
    //log.debug topicName
    return topicName
}






