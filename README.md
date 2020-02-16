# Hubitat MQTT SmartBlinds
Hubitat driver and NodeMCU (ESP8266) firmware for smart blinds.

This is a work in progress.  I don't expect anyone else will ever build one of these, but I wanted to document it, for my own ease of replicating it again.

The hardware and firmware is heavily based on the work of Smart Home Hookup here:  https://github.com/thehookup/Motorized_MQTT_Blinds  Much thanks to that dude!

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

----------

## Steps

1. Open the blue cover on the stepper motor and use a screwdriver to scratch out the middle trace.  Use a multimeter to make sure continuity is broken.
2. Shorten the motor wires for now.  You can completely remove the middle one that went to the middle trace.
3. Print the 2" blinds base:  https://github.com/thehookup/Motorized_MQTT_Blinds/blob/master/BlindsBase.stl.  It can be 30% infill.  Test-fit the stepper in it.  It should fit snugly.  You won't need screws or glue to hold it in, when in place in the blinds.


