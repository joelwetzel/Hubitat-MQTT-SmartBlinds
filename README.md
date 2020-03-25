# Hubitat MQTT SmartBlinds
**Hardware, Firmware, and Hubitat Driver for MQTT Smart Blinds**

This is a work in progress.  I don't expect anyone else will ever build one of these, but I wanted to document it, for my own ease of replicating it again.

The hardware and firmware is heavily based on the work of Smart Home Hookup here:  https://github.com/thehookup/Motorized_MQTT_Blinds, and here:  http://www.thesmarthomehookup.com/automated-motorized-window-blinds-horizontal-blinds/  Much thanks to that dude!  Watch his youtube video before following my instructions.

The Hubitat driver is my own creation, using the MQTT support that Hubitat added last year.

What I've tried to do that is beyond what Smart Home Hookup did is two things:
- Make it work well with Hubitat
- "Productize" it.  Hide all the hardware details and make it clean and minimal, repeatable to build, and easy to maintain.

----------

## Parts List
Stepper Motors: https://amzn.to/2D5rVsF

Stepper Drivers: https://amzn.to/2OZqW1W

NodeMCU: https://amzn.to/2I89xDF

Buck Converter: https://amzn.to/2UsQ7jA

12V Power Supply: https://www.amazon.com/gp/product/B07THWH1KZ

DC extension cables and barrel connector:  https://www.amazon.com/gp/product/B01EAHRZEC

Cheap ethernet cable to cut up:  https://www.amazon.com/gp/product/B074NBWQST

Ethernet coupler:  https://www.amazon.com/gp/product/B01CLFCX7E

Velcro strips:  https://www.amazon.com/Command-Picture-Decorate-Damage-Free-PH206-14NA/dp/B073XR4X72

Dupont Crimper toolset:  https://www.amazon.com/gp/product/B07GS1Z3M3

----------

## Preparing the Motor Assembly

#### The motor assembly goes into the blinds header.  It has a 3d printed base, the stepper motor itself, a 3d printed adapter to connect the motor to the rod, the driver board, and an ethernet cable sticking out.

1. Print the 2" blinds base:  https://github.com/thehookup/Motorized_MQTT_Blinds/blob/master/BlindsBase.stl.  It can be 30% infill.  Test-fit the stepper in it.  It should fit snugly.  You won't need screws or glue to hold it in, when in place in the blinds.

2. Open the blue cover on the stepper motor and use a screwdriver to scratch out the middle trace.  Use a multimeter to make sure continuity is broken.

3. Shorten the motor wires.  You can completely remove the middle one that went to the middle trace.  Shorten the others to 2.5 inches.  Attach a 4-pin female Dupont connector as shown.  I found this video very helpful for learning how to use my Dupont crimper:  https://www.youtube.com/watch?v=-u1t7Cdf6RE&t=376s

![alt text](https://github.com/joelwetzel/Hubitat-MQTT-SmartBlinds/blob/master/images/motorConnector.jpeg)

4. Prepare the stepper driver board.  You need to create a solder bridge between the reset pin and the sleep pin (the green jumper in this pic), and trim off any unused pins.

![alt text](https://github.com/joelwetzel/Hubitat-MQTT-SmartBlinds/blob/master/images/SolderBridgeOnDriver.png)

5. Pull down your blinds.
6. Remove any end caps, if it has them.
7. Remove the pull string assembly that turns the rod when you pull the strings.

8. You'll need to print an adapter to connect the stepper shaft to the rod in the blinds.  The rods can have lots of different shapes.  Smart Home Hookup provided several options.  I designed my own, since my rod had a profile like the superman emblem.  I've uploaded that STL here. (Adapter.stl)  Print it at 100% infill.  (By this point, you should have your blinds partially dismantled.  Smart Home Hookup has a good youtube video on doing that.)

9. Prepare the ethernet cable that will come out of the motor assembly.  It should be about 6 inches long.  Attach Dupont connectors as shown here:

![alt text](https://github.com/joelwetzel/Hubitat-MQTT-SmartBlinds/blob/master/images/motorAssemblyEthernet.jpg)

10. Attach the connectors from both the ethernet cable and the motor onto the driver board, as shown here:

![alt text](https://github.com/joelwetzel/Hubitat-MQTT-SmartBlinds/blob/master/images/driverConnections.jpg)

11. After putting the blinds base, the motor, and the adapter in, I had to shorten my rod.  I let the excess stick out the opposite side, and used a dremel to cut through it.

12. Assemble the Motor Assembly inside the blinds.  The driver gets tucked into the cavity in the blinds base.  You'll need to find a route to get the ethernet cable out of the blinds.  It'll look something like this:

![alt text](https://github.com/joelwetzel/Hubitat-MQTT-SmartBlinds/blob/master/images/finishedMotorAssembly.jpeg)

13. Reinstall the blinds in the window.

----------

## Preparing the Control Unit

#### The Control Unit has a NodeMCU (ESP8266), a buck converter, a two-part 3d printed box, a power cable in, and an ethernet cable out.

1. Solder pins into the buck converter.  Easiest way to do this is to stick it onto a breadboard, so that forces the pins to align well.

2. At this point, you MUST calibrate the buck converter to output 5V.  If you don't, you'll burn up your NodeMCU.  (Yes, technically an ESP8266 chip itself is spec'ed for 3.3V, but this'll work at 5.  I believe it's because the NodeMCU has an extra voltage regulator.)

![alt text](https://github.com/joelwetzel/Hubitat-MQTT-SmartBlinds/blob/master/images/BuckConverter5V.png)

3. Assemble the breadboard.  The goals are:  Get 12V in.  Convert it down to 5V.  Get that 5V in to the NodeMCU.  Have the 12V available for the stepper driver.  Make it easy to connect the stepper driver.  Here's what it should look like:

![alt text](https://github.com/joelwetzel/Hubitat-MQTT-SmartBlinds/blob/master/images/AssembledBreadboard.jpg)

4. Clip off any tabs that are sticking out of the breadboard.

5. 3D Print the bottom of the box that the breadboard goes into.  It's ControlUnitBottom.stl.

6. Use a sticky pad to attach the breadboard inside the box.  Make sure the micro USB port lines up with the opening for it, so you can reprogram the NodeMCU.

7. Prepare the ethernet cable to come out of the box, with MALE Dupont connectors.  You'll need to attach the Dupont connectors while it's already sticking through the hole in the box.  Here are the Dupont connectors you'll want. The connections are:

  - Double orange and Double red go to +12V and ground.
  - Green goes to the +5V.
  - Green-strip, blue, and blue-strip go to D5, D6, and D7.

![alt text](https://github.com/joelwetzel/Hubitat-MQTT-SmartBlinds/blob/master/images/ConnectionsOnBreadboard2.jpeg)

8. Attach the female power plug.  At this point, it should look something like this:

![alt text](https://github.com/joelwetzel/Hubitat-MQTT-SmartBlinds/blob/master/images/assembledControlUnit.jpg)

9. 3D print the top of the box.  It's ControlUnitTop.stl.

10. Assemble.  You should be able to attach a micro USB cable for programming at this point.

![alt text](https://github.com/joelwetzel/Hubitat-MQTT-SmartBlinds/blob/master/images/finishedControlUnit.jpg)

----------

## Final Assembly
Connect the ethernet from the Motor Assembly to the ethernet from the Control Unit using the ethernet female-to-female coupler.  Tuck the control unit up behind the blinds and run power.  It should be almost invisible.  Just a small power cord visible in the lower left of the window opening.

![alt text](https://github.com/joelwetzel/Hubitat-MQTT-SmartBlinds/blob/master/images/finalAssembly.jpg)

----------

## Setting up an MQTT Broker
I'll let others explain this.  There are lots of good tutorials out there.  I use Mosquitto.

----------

## Installing the Firmware
You'll need to install the firmware on the NodeMCU in the Control Unit.  That's why it has the Micro USB port.  I've provided the main.cpp file with the firmware.  I use Platform.io, though I haven't uploaded the entire project.  You can also just use the Arduino IDE.
- You'll need to set your WIFI and MQTT info in main.cpp.
- You need to set USER_DEVICE_NETWORK_ID for the id you're going to assign the device in Hubitat.

----------

## Installing the Hubitat Driver
I've provided the driver that will control the Device in Hubitat.  You need to make sure your Device Network ID in Hubitat matches what you set in the firmware.

----------

## Controlling with Siri/HomeKit
This driver/device works great with the Hubitat plugin for Homebridge.  Using that, you can control the blinds very well from within the Home app on your iPhone, or with Siri.

----------

## Controlling with Alexa
The Hubitat Alexa skill does not yet support window shades and blinds.  To enable Alexa to work with my blinds, I created my own Alexa skill here:  https://github.com/joelwetzel/Alexa-Skill-For-Hubitat-Shades-And-Blinds
