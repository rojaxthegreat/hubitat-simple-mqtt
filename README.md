# hubitat-simple-mqtt

This driver allows your hub to subscribe and publish to topics via an MQTT broker.
Each topic is represented as a child device, which makes it easy to incorporate topics into
Rule Machine, dashboards, and more.

## Intended use

The goal of this driver is to act as a simple bridge between Hubitat and other MQTT-speaking devices.
The driver is not aware of what devices are on your hub and it does not attempt to publish their
state changes. If your goal is to map all of your hub's devices to MQTT topics, you likely want
to use a different driver like [MQTT Link](https://github.com/mydevbox/hubitat-mqtt-link).

The driver has no logic inside of it and there is no companion app to install. This keeps the code
minimal and light on hub resources. Instead, logic should be implemented in Rule Machine rules or similar.

As with all Hubitat MQTT drivers, you need to have an MQTT broker running on your network. A popular
choice is Mosquitto, which I run on a Raspberry Pi 3B.

## Topics

Instead of devices, this driver is built around MQTT topics. You can register as many topics as you want
and performance will not be affected, as a single MQTT connection is shared by all child devices.

Once you register a topic, it appears as a child device under the MQTT client. This allows you to use
the topic in several ways.

### Publishing to the topic

Each topic has the Notification capability, which lets Hubitat send a notification to the topic. The
notification is sent as the payload of an MQTT message to the topic.

### Subscribing to the topic

Each topic acts as a Button, a Switch, and a Variable for use in Rule Machine or other apps.

- **Button**: Any message received to the topic (even a null message) will fire a `Button 1 pushed` event.
  This event fires every time a message is received, even if the message is the same.
- **Switch**: If the message is `on`, `off`, `true`, or `false`, the topic's switch will turn on or off.
  If the switch is already on, Hubitat doesn't treat turning it on again as a new event, which means you
  can repeatedly publish the state of your device without firing a rule over and over.
- **Variable**: Any other message will be stored in the `variable` field on the device, allowing you to
  listen for a `Custom Attribute` change in Rule Machine, or display the variable on a dashboard. This
  allows you to track water or fuel levels, temperatures, and more.

### Bidirectional communication

You can use the same topic to both send and receive. However, note that the device will also receive
its OWN messages (until [Hubitat supports MQTT v5](https://community.hubitat.com/t/support-mqtt-v5/152664)).
For many use cases this is OK, but be careful not to create an infinite loop. If you need better support for
this scenario, please open an issue to discuss.

## Installation

1. Install the two drivers for the MQTT Client and the MQTT Client Topic (child device).
2. Create a new virtual device with the type `Simple MQTT Client`. You only need one of these for your
   hub, no matter how many topics you want to publish or subscribe to.
3. In the device preferences, set your broker IP/hostname and port. Username and password are optional
   if your broker does not require authentication.
4. (Optional) set a topic prefix that all of your topics will use as a namespace. For example, setting the
   prefix to `hubitat` means that all your topics will look like `hubitat/topicOne`. Leave blank to manage
   the namespaces on a per-topic basis.
5. Create a new topic using the driver's Add Topic command. The label will show up in Rule Machine.
   (If no label is provided, the topic name is used instead)

The client will automatically reconnect if the hub is restarted, the broker goes down, or the network
is disconnected. You can always force a reconnect if anything goes wrong by invoking the `Initialize`
command.

## Examples

### Pause ad blocking on Pihole

I had a virtual Button in a dashboard that pauses the Pihole for 5 minutes. In the same Rule Machine rule, I
can now also Listen for `Button 1 pushed` on `hubitat/pause_pihole`, and then use Rule Machine to POST to
https://pi.hole/api/dns/blocking

### Decide if it's a work-from-home day

In Rule Machine, trigger at 7am M-F. If my presence sensor is Present, send a Notification to `hubitat/work_from_home`, which
other devices on my network are listening for (like the coffee machine and the air conditioner).

### Monitor solar production

My solar monitoring system publishes the total energy produced that day to `hubitat/solar_production`. I can use the `variable`
attribute on this topic to display the value in a dashboard.

## Known limitations

- Sending a notification on a device also triggers its button push (see _Bidirectional communication_ section above)
- JSON payloads are received but not parsed, because only one string variable attribute is available. Parsing
  arbitrary payloads in a way that is useful requires more thought.
- Changing the topic prefix does not update child devices since that would break existing automations.
- Rule Machine does not support Momentary buttons, which is why you have to check "Button 1 pushed" in rules instead.
