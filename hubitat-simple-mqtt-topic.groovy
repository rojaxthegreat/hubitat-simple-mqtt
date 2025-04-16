/**
 * Hubitat Device Driver
 * Simple MQTT Topic
 * v0.0.1
 * https://github.com/sethkinast/hubitat-simple-mqtt/
 *
 * Represents a single topic that can be subscribed and
 * published to the broker.
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

metadata {
    definition(
        name: 'Simple MQTT Client Topic',
        namespace: 'cogdev',
        author: 'Seth Kinast <seth@cogdev.net>',
        importUrl:
            'https://raw.githubusercontent.com/sethkinast/hubitat-simple-mqtt/master/hubitat-simple-mqtt-topic.groovy'
    ) {
        capability 'Notification'
        capability 'Actuator'
        capability 'Momentary'
        capability 'PushableButton' // due to RM currently not allowing Momentary as a trigger type
        capability 'Switch'
        capability 'Variable'

        command 'deviceNotification', [
            [name: 'Payload', type: 'STRING', description: "Broadcast on this device's topic"]
        ]
    }
}

void installed() {
    sendEvent([name: 'numberOfButtons', value: 1])
}

void parse(List<Map> events) {
    events.each { event ->
        sendEvent(event)
    }
}

/* Notification */
void deviceNotification(String message) {
    parent?.componentDeviceNotification(this.device, message)
}

/* Momentary */
void push() {
    unimplemented()
}

/* groovylint-disable-next-line UnusedMethodParameter */
void push(Number button) {
    push()
}

/* Switch */
void on() {
    unimplemented()
}

void off() {
    unimplemented()
}

/* Variable */
/* groovylint-disable-next-line UnusedMethodParameter */
void setVariable(String value) {
    unimplemented()
}

void unimplemented() {
    log.error 'You cannot directly command this device. Send MQTT events to its topic to generate events.'
}
