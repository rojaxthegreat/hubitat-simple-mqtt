/**
 * Hubitat Device Driver
 * Simple MQTT Client
 * v0.0.1
 * https://github.com/sethkinast/hubitat-simple-mqtt/
 *
 * Simple MQTT client that can publish to a topic and subscribe
 * to multiple topics. For use with Rule Machine and other apps
 * that can dispatch to Notification devices.
 *
 * MIT License
 *
 * Copyright (c) 2025 Seth Kinast
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import com.hubitat.app.DeviceWrapper
import com.hubitat.app.ChildDeviceWrapper

metadata {
    definition(
        name: 'Simple MQTT Client',
        namespace: 'cogdev',
        author: 'Seth Kinast <seth@cogdev.net>',
        importUrl:
            'https://raw.githubusercontent.com/sethkinast/hubitat-simple-mqtt/master/hubitat-simple-mqtt.groovy'
    ) {
        capability 'Initialize'

        command 'addTopic', [
            [name: 'Topic*', type: 'STRING'],
            [name: 'Label', type: 'STRING', description: 'Description of the topic']
        ]
    }
}

preferences {
    section('MQTT') {
        input name: 'brokerIP', type: 'string', title: 'MQTT Broker IP Address', required: true
        input name: 'brokerPort', type: 'string', title: 'MQTT Broker Port', required: true, defaultValue: 1883
        input name: 'topicPrefix', type: 'string', title: 'MQTT Topic Prefix',
            description: 'namespace containing driver events', required: true, defaultValue: 'hubitat'
        input name: 'brokerUsername', type: 'string', title: 'MQTT User'
        input name: 'brokerPassword', type: 'password', title: 'MQTT Password'
    }
    section('Advanced') {
        input name: 'debugLoggingEnabled', type: 'bool', title: 'Enable debug logging', defaultValue: true
    }
}

void initialize() {
    state.remove('connectDelay')
    connect()

    if (debugLoggingEnabled) {
        runIn(3600, disableDebugLogging)
    }
}

void updated() {
    initialize()
}

void parse(String message) {
    Map parsedMessage = interfaces.mqtt.parseMessage(message)
    logDebug parsedMessage.toString()

    ChildDeviceWrapper targetDevice = getChildDevices().find { cd -> cd.getDataValue('topic') == parsedMessage.topic }
    if (targetDevice) {
        events = [[name: 'pushed', value: 0, isStateChange: true]]
        if (parsedMessage.payload == 'true' || parsedMessage.payload == 'on') {
            events += [name: 'switch', value: 'on']
        } else if (parsedMessage.payload == 'false' || parsedMessage.payload == 'off') {
            events += [name: 'switch', value: 'off']
        } else if (parsedMessage.payload) {
            events += [name: 'variable', value: parsedMessage.payload]
        }
        targetDevice.parse(events)
    }
}

/* MQTT */

void connect() {
    try {
        logDebug "Connecting to MQTT broker at ${brokerIP}:${brokerPort}"
        interfaces.mqtt.connect(getMQTTConnectURI(), "hubitat_simple_mqtt_${device.id}", brokerUsername, brokerPassword)
    } catch (e) {
        log.error "Error connecting to MQTT broker: ${e.message}"
        reconnect()
    }
}

void reconnect() {
    state.connectDelay = state.connectDelay ?: 0
    state.connectDelay = Math.min(state.connectDelay + 1, 5)

    runIn(state.connectDelay * 60, connect)
}

void addTopic(String topic, String label = null) {
    String fullTopic = getTopic(topic)
    ChildDeviceWrapper cd = addChildDevice('cogdev', 'Simple MQTT Client Topic', fullTopic, [
        label: label ?: fullTopic,
        name: label ? fullTopic : null,
    ])
    cd.updateDataValue('topic', fullTopic)
    subscribeToChild(cd)
}

void subscribe() {
    getChildDevices().each { cd ->
        subscribeToChild(cd)
    }
}

void subscribeToChild(ChildDeviceWrapper cd) {
    topic = cd.getDataValue('topic')
    logDebug 'Subscribing to ' + topic
    interfaces.mqtt.subscribe(topic)
}

void mqttClientStatus(String status) {
    logDebug status
    state.status = status
    if (status.startsWith('Error')) {
        try {
            interfaces.mqtt.disconnect()
        } finally { reconnect() }
    } else {
        state.remove('connectDelay')
        runIn(1, subscribe)
    }
}

void componentDeviceNotification(DeviceWrapper cd, String message) {
    interfaces.mqtt.publish(
        cd.getDataValue('topic'),
        message ?: ''
    )
}

/* Helpers */

String getMQTTConnectURI() {
    "tcp://${brokerIP}:${brokerPort}"
}

String getTopic(String topicSuffix) {
    (topicPrefix == null || topicPrefix == '') ? topicSuffix : topicPrefix + '/' + topicSuffix
}

void disableDebugLogging() {
    device.updateSetting('debugLoggingEnabled', [value: false, type: 'bool'])
}

void logDebug(String msg) {
    if (debugLoggingEnabled) {
        log.debug msg
    }
}
