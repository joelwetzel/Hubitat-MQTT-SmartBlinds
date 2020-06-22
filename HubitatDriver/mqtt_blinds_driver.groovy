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

import groovy.time.*
    
metadata {
        definition (name: "MQTT Window Blinds", namespace: "joelwetzel", author: "Joel Wetzel") {
		capability "Actuator"
        capability "WindowShade"
        capability "IlluminanceMeasurement"
        capability "Refresh"
        capability "Initialize"
            
        attribute "driverStatus", "string"
        attribute "deviceStatus", "string"
            
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
            input (
                type: "bool",
                name: "autoClose",
                title: "Auto open/close blinds?",
                required: true,
                defaultValue: false
            )
            input (
                type: "Number",
                name: "illuminanceThreshold",
                title: "Autoclose if brighter than",
                required: false,
                defaultValue: 500
            )
   			input (
				type: "bool",
				name: "enableDebugLogging",
				title: "Enable Debug Logging?",
				required: true,
				defaultValue: true
			)
        }
    }
}


def parse(String description) {
    use (groovy.time.TimeCategory) {
        //log.debug "parse(${description})"
    
        def decoded = interfaces.mqtt.parseMessage(description)
        //log.debug "parse(${decoded})"
    
        if (decoded.topic == topicNameAttribute("position")) {
            def deviceReportedPosition = decoded.payload.toInteger()
            def targetPosition = device.currentValue("setPosition").toDouble().toInteger()
            def deviceReportedPositionConvertedToHubitatValue = convertEsp8266PositionToHubitatPosition(deviceReportedPosition)
        
            sendEvent(name: "position", value: deviceReportedPositionConvertedToHubitatValue, isStateChange: true)
        
            //log.debug "${deviceReportedPosition} ${targetPosition}"
        
            updateWindowShadeAttribute(deviceReportedPosition, targetPosition, deviceReportedPositionConvertedToHubitatValue)
        }
        else if (decoded.topic == "hubitat/${device.getDeviceNetworkId()}/illuminanceMeasurement/attributes/illuminance") {
            def deviceReportedIlluminance = decoded.payload.toInteger()
        
            sendEvent(name: "illuminance", value: deviceReportedIlluminance, isStateChange: true)
        
            // Do a filter when illuminance drops
            if (deviceReportedIlluminance > state.filteredIlluminance) {
                state.filteredIlluminance = state.filteredIlluminance + 0.8 * (deviceReportedIlluminance - state.filteredIlluminance)
            }
            else {
                state.filteredIlluminance = state.filteredIlluminance - (state.filteredIlluminance - deviceReportedIlluminance) / 3.0
            }
            state.filteredIlluminance = Math.round(state.filteredIlluminance * 100) / 100        // Only keep 2 decimal places
        
            if (autoClose && illuminanceThreshold &&
                (new Date() - toDateTime(state.lastManualClose)).minutes > 30) {    // Manual open/close disables auto-close for a half hour.
                if (state.filteredIlluminance > illuminanceThreshold 
                        && device.currentValue("windowShade") != "closed"
                        && (new Date() - toDateTime(state.lastAutoClose)).minutes > 3) {
                    internalSetPosition(0)                    // Don't just call close().  I want to be able to distinguish between auto-close and user-initiated actions.
                    state.lastAutoClose = new Date()
                }
                else if (state.filteredIlluminance <= illuminanceThreshold
                            && device.currentValue("windowShade") == "closed"
                            && (new Date() - toDateTime(state.lastAutoClose)).minutes > 6) {        // If we auto-close, don't reopen for at least 10 minutes
                    internalSetPosition(100)                    // Don't just call open().  I want to be able to distinguish between auto-close and user-initiated actions.
                    state.lastAutoClose = new Date()
                }
            }
        }
        else if (decoded.topic == "hubitat/${device.getDeviceNetworkId()}/status/device") {
            sendEvent(name: "deviceStatus", value: decoded.payload, isStateChange: true)
        }
    }
}


def updateWindowShadeAttribute(deviceReportedPosition, targetPosition, knownPositionInHubitatScale) {
    if (knownPositionInHubitatScale == 0) {
        sendEvent(name: "windowShade", value: "closed", isStateChange: true)
    }
    else if (knownPositionInHubitatScale >= 99) {
        sendEvent(name: "windowShade", value: "open", isStateChange: true)
    }
    else if (deviceReportedPosition == targetPosition) {
        //log.debug "Reached target position."

        if (knownPositionInHubitatScale == null) {
            sendEvent(name: "windowShade", value: "unknown", isStateChange: true)
        }
        else {
            sendEvent(name: "windowShade", value: "partially open", isStateChange: true)
        }
    }
}


def open() {
    sendEvent(name: "open", value: 1, isStateChange: true)
    state.lastManualClose = new Date()
    internalSetPosition(100)
}


def close() {
    sendEvent(name: "close", value: 1, isStateChange: true)
    state.lastManualClose = new Date()
    internalSetPosition(0)
}


def setPosition(position) {
    def espPosition = convertHubitatPositionToEsp8266Position(position)
    sendEvent(name: "setPosition", value: espPosition, isStateChange: true)
    
    internalSetPosition(position)
}


def convertHubitatPositionToEsp8266Position(hubitatPosition) {
    def inverted = 100 - hubitatPosition
    def scaled = inverted / 100 * stepsToClose
    return scaled
}

def convertEsp8266PositionToHubitatPosition(espPosition) {
    def scaled = espPosition / stepsToClose * 100
    def inverted = 100 - scaled
    return inverted.toInteger()
}


def internalSetPosition(position) {
    //log.debug "internalSetPosition(${position})"
    
    // Position in Hubitat is a number from 0 to 100.  Open is 100.  Closed is 0.
    // On the ESP8266, position is 0 at open, and something like 8 at closed.
    // So we need a mapping function to translate.
    // In pure interface terms, it would be cleaner for this mapping to happen
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
    interfaces.mqtt.publish(topicNameCommand("setPosition"), espPosition.toString())
}


def installed() {
    log.info "installed()"
    
    initialize()
    runEvery1Hour(initialize)
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
    log "initialize()"
    
    if (interfaces.mqtt.isConnected()) {
        log "Disconnecting..."
        interfaces.mqtt.disconnect()
    }
        
    //updateWindowShadeAttribute(device.currentValue("position"))
    
    if (!state.filteredIlluminance) {
        state.filteredIlluminance = 0
    }
    
    if (!state.lastAutoClose) {
        state.lastAutoClose = new Date()
    }
    
    if (!state.lastManualClose) {
        state.lastManualClose = new Date()
    }
    
    runIn(1, connectToMqtt)
}


def uninstalled() {
    interfaces.mqtt.disconnect()
}


def mqttClientStatus(String status) {
    log "mqttClientStatus(${status})"

    if (status.take(6) == "Error:") {
        log.error "Connection error..."
        sendEvent(name: "driverStatus", value: "ERROR", isStateChange: true)
        
        try {
            interfaces.mqtt.disconnect()  // clears buffers
        }
        catch (e) {
        }
        
        log.info("Attempting to reconnect in 5 seconds...");
        runIn(5, connectToMqtt)
    }
    else {
        log.info "Connected!"
        sendEvent(name: "driverStatus", value: "Connected", isStateChange: true)
    }
}


def connectToMqtt() {
    log "connectToMqtt()"
    
    if (!interfaces.mqtt.isConnected()) {        
        log "Connecting to MQTT..."
        interfaces.mqtt.connect("tcp://${mqttHubIp}:1883", device.getDeviceNetworkId() + "driver", null, null)
        
        runIn(1, subscribe)
    }
}


def subscribe() {
    log "Subscribing..."
    
    // Track device status
    interfaces.mqtt.subscribe("hubitat/${device.getDeviceNetworkId()}/status/device")
    
    // Subscribe to attributes
    interfaces.mqtt.subscribe(topicNameAttribute("position"))
    interfaces.mqtt.subscribe("hubitat/${device.getDeviceNetworkId()}/illuminanceMeasurement/attributes/illuminance")
        
    // Subscribe to commands.  Commented out because I'm not letting commands come in through MQTT on this driver.
    //interfaces.mqtt.subscribe(topicNameCommand("on"))
    //interfaces.mqtt.subscribe(topicNameCommand("off"))
}


def topicNameCommand(String command) {
    // We're "sort of" using the homie mqtt convention.
    // https://homieiot.github.io
        
    def topicBase = "hubitat/${device.getDeviceNetworkId()}"
    
    def topicName = "${topicBase}/windowShade/commands/${command}"
    
    //log.debug topicName
    return topicName
}

def topicNameAttribute(String attribute) {
    // We're "sort of" using the homie mqtt convention.
    // https://homieiot.github.io
        
    def topicBase = "hubitat/${device.getDeviceNetworkId()}"
    
    def topicName = "${topicBase}/windowShade/attributes/${attribute}"
    
    //log.debug topicName
    return topicName
}


def log(msg) {
	if (enableDebugLogging) {
		log.debug(msg)	
	}
}




