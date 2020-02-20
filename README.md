# Hubitat MQTT SmartBlinds
Hubitat driver and NodeMCU (ESP8266) firmware for smart blinds.

This is a work in progress.  I don't expect anyone else will ever build one of these, but I wanted to document it, for my own ease of replicating it again.

The hardware and firmware is heavily based on the work of Smart Home Hookup here:  https://github.com/thehookup/Motorized_MQTT_Blinds, and here:  http://www.thesmarthomehookup.com/automated-motorized-window-blinds-horizontal-blinds/  Much thanks to that dude!

The Hubitat driver will be my own creation, using the MQTT support that Hubitat added last year.

----------

## Parts List
Stepper Motors: https://amzn.to/2D5rVsF

Stepper Drivers: https://amzn.to/2OZqW1W

NodeMCU: https://amzn.to/2I89xDF

Buck Converter: https://amzn.to/2UsQ7jA

12V Power Supply: https://www.amazon.com/gp/product/B01N3SNRE4

Female barrel connector:  https://www.amazon.com/gp/product/B07RKMC4S1

Flat ethernet cable:  https://www.amazon.com/gp/product/B017P34WZI

Velcro strips:  https://www.amazon.com/Command-Picture-Decorate-Damage-Free-PH206-14NA/dp/B073XR4X72

----------

## Preparing the Motor Assembly

#### The motor assembly goes into the blinds header.  It has a 3d printed base, the stepper motor itself, an adapter to connect the motor to the rod, and the driver board.

1. Open the blue cover on the stepper motor and use a screwdriver to scratch out the middle trace.  Use a multimeter to make sure continuity is broken.
2. Shorten the motor wires.  You can completely remove the middle one that went to the middle trace.  Shorten the others to TODO inches.
3. Prepare the stepper driver.  You need to create a solder bridge between the reset pin and the sleep pin (the green jumper in this pic), and trim off any unused pins.  ![alt text](https://github.com/joelwetzel/Hubitat-MQTT-SmartBlinds/blob/master/SolderBridgeOnDriver.png)
4. Use a crimper tool to put a 4-terminal female Dupont connector on the blue, yellow, orange, and purple stepper wires, as shown in the picture.  Remember that you only need like 1mm of bare wire sticking out of each lead when using the crimper.  I found this video very helpful for learning how to use my Dupont crimper:  https://www.youtube.com/watch?v=-u1t7Cdf6RE&t=376s
5. Print the 2" blinds base:  https://github.com/thehookup/Motorized_MQTT_Blinds/blob/master/BlindsBase.stl.  It can be 30% infill.  Test-fit the stepper in it.  It should fit snugly.  You won't need screws or glue to hold it in, when in place in the blinds.
6. You'll need to print an adapter to connect the stepper shaft to the rod in the blinds.  The rods can have lots of different shapes.  Smart Home Hookup provided several options.  I designed my own, since my rod had a profile like the superman emblem.  I've uploaded that STL here.  Print it at 100% infill.  (By this point, you should have your blinds partially dismantled.  Smart Home Hookup has a good youtube video on doing that.)


## Preparing the cable
1. TODO

## Preparing the Breadboard Unit
1. Solder pins into the buck converter.  Easiest way to do this is to stick it onto a breadboard, so that forces the pins to align well.
2. At this point, you MUST calibrate the buck converter to output 5V.  If you don't, you'll burn up your NodeMCU.  (Yes, technically an ESP8266 is spec'ed for 3.3V, but it'll work at 5.)  ![alt text](https://github.com/joelwetzel/Hubitat-MQTT-SmartBlinds/blob/master/BuckConverter5V.png)
3. Assemble the breadboard.  The goals are:  Get 12V in.  Convert it down to 5V.  Get that 5V in to the NodeMCU.  Have the 12V available for the stepper driver.  Make it easy to connect the stepper driver.  Here's what it should look like:
![alt text](https://github.com/joelwetzel/Hubitat-MQTT-SmartBlinds/blob/master/AssembledBreadboard.jpg)
4. Clip off any tabs that are sticking out of the breadboard.
5. 3D Print the bottom of the box that the breadboard goes into.  It's TODO.stl.
6. Use a sticky pad to attach the breadboard inside the box.  Make sure the micro USB port lines up with the opening for it, so you can reprogram the NodeMCU.

## Installing the Motor Assembly in the blinds
1. Pull down your blinds.
2. Remove any end caps, if it has them.
3. Remove the pull string assembly that turns the rod when you pull the strings.
4. After putting the blinds base, the motor, and the adapter in, I had to shorten my rod.  I let the excess go out the opposite side, and used a dremel to cut through it.
5. Assemble the Motor Assembly inside the blinds.  The driver gets tucked into the cavity in the blinds base.

## Making the custom cable to connect the breadboard to the motor assembly
1. The cable is made from a section of Ethernet cable.  Prepare the end that will be in the breadbox first.  Here are the Dupont connectors you'll want on that end:
![alt text](https://github.com/joelwetzel/Hubitat-MQTT-SmartBlinds/blob/master/ConnectionsOnBreadboard2.jpeg)
2. The connections are:
  - Double orange and Double red go to +12V and ground.
  - Green goes to the +5V.
  - Green-strip, blue, and blue-strip go to D5, D6, and D7.
3. Thread the other end of the cable out of the hole in the box before preparing the other end.
