#include <SimpleTimer.h>    // https://github.com/marcelloromani/Arduino-SimpleTimer/tree/master/SimpleTimer
#include <ESP8266WiFi.h>    // if you get an error here you need to install the ESP8266 board manager 
#include <ESP8266mDNS.h>    // if you get an error here you need to install the ESP8266 board manager 
#include <PubSubClient.h>   // https://github.com/knolleary/pubsubclient
#include <ArduinoOTA.h>     // https://github.com/esp8266/Arduino/tree/master/libraries/ArduinoOTA
#include <AH_EasyDriver.h>  // http://www.alhin.de/arduino/downloads/AH_EasyDriver_20120512.zip


/*****************  START USER CONFIG SECTION *********************************/

#define WIFI_SSID                 "*****"
#define WIFI_PASSWORD             "*****"
#define USER_MQTT_SERVER          "mqtt.local"
#define USER_MQTT_PORT            1883

// Uncomment if your MQTT broker requires authentication
//#define USER_MQTT_USERNAME        "YOUR_MQTT_USER_NAME"
//#define USER_MQTT_PASSWORD        "YOUR_MQTT_PASSWORD"

#define USER_DEVICE_NETWORK_ID     "officeBlindsRight"         // This must match the Device Network ID in Hubitat

#define STEPPER_SPEED             17                  // Defines the speed in RPM for your stepper motor
#define STEPPER_STEPS_PER_REV     1028                // Defines the number of pulses that is required for the stepper to rotate 360 degrees
#define STEPPER_MICROSTEPPING     0                   // Defines microstepping 0 = no microstepping, 1 = 1/2 stepping, 2 = 1/4 stepping 
#define DRIVER_INVERTED_SLEEP     1                   // Defines sleep while pin high.  If your motor will not rotate freely when on boot, comment this line out.

#define STEPS_TO_CLOSE            8                  // Defines the number of steps needed to open or close fully
#define CLOSE_DIRECTION           0                   // Switch between 1 and 0 to make the blinds close the opposite direction

#define STEPPER_DIR_PIN           D6
#define STEPPER_STEP_PIN          D7
#define STEPPER_SLEEP_PIN         D5
#define STEPPER_MICROSTEP_1_PIN   14
#define STEPPER_MICROSTEP_2_PIN   12
 
/*****************  END USER CONFIG SECTION *********************************/


WiFiClient espClient;
PubSubClient client(espClient);
SimpleTimer timer;
AH_EasyDriver shadeStepper(STEPPER_STEPS_PER_REV, STEPPER_DIR_PIN ,STEPPER_STEP_PIN,STEPPER_MICROSTEP_1_PIN,STEPPER_MICROSTEP_2_PIN,STEPPER_SLEEP_PIN);

// Global Variables
bool boot = true;
int currentPosition = 0;
int newPosition = 0;
char positionPublish[50];
bool moving = false;
char charPayload[50];

const char* ssid = WIFI_SSID ; 
const char* password = WIFI_PASSWORD ;
const char* mqtt_server = USER_MQTT_SERVER ;
const int mqtt_port = USER_MQTT_PORT ;

// Uncomment if your MQTT broker requires authentication
//const char *mqtt_user = USER_MQTT_USERNAME ;
//const char *mqtt_pass = USER_MQTT_PASSWORD ;

const char *mqtt_device_network_id = USER_DEVICE_NETWORK_ID ; 


//Functions
void setup_wifi()
{
  // We start by connecting to a WiFi network
  Serial.println();
  Serial.print("Connecting to ");
  Serial.println(ssid);

  WiFi.begin(ssid, password);

  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }

  Serial.println("");
  Serial.println("WiFi connected");
  Serial.println("IP address: ");
  Serial.println(WiFi.localIP());
}


void reconnect() 
{
  int retries = 0;
  while (!client.connected())
  {
    if(retries < 150)
    {
      Serial.print("Attempting MQTT connection...");
      if (client.connect(mqtt_device_network_id))   // Password option:  client.connect(mqtt_device_network_id, mqtt_user, mqtt_pass)
      {
        Serial.println("connected");
        if(boot == false)
        {
          client.publish("hubitat/"USER_DEVICE_NETWORK_ID"/status/device", "Reconnected"); 
        }
        if(boot == true)
        {
          client.publish("hubitat/"USER_DEVICE_NETWORK_ID"/status/device", "Rebooted");
          client.publish("hubitat/"USER_DEVICE_NETWORK_ID"/windowShade/attributes/position", "0"); 
          boot = false;
        }
        // ... and resubscribe
        client.subscribe("hubitat/"USER_DEVICE_NETWORK_ID"/windowShade/commands/setPosition");
      } 
      else 
      {
        Serial.print("failed, rc=");
        Serial.print(client.state());
        Serial.println(" try again in 5 seconds");
        retries++;
        // Wait 5 seconds before retrying
        delay(5000);
      }
    }

    if(retries > 149)
    {
      ESP.restart();
    }
  }
}


void mqttCallback(char* topic, byte* payload, unsigned int length) 
{
  Serial.print("Message arrived [");

  String newTopic = topic;
  Serial.print(topic);
  Serial.print("] ");
  payload[length] = '\0';
  String newPayload = String((char *)payload);
  int intPayload = newPayload.toInt();
  Serial.println(newPayload);
  Serial.println();
  newPayload.toCharArray(charPayload, newPayload.length() + 1);

  if (newTopic == "hubitat/"USER_DEVICE_NETWORK_ID"/windowShade/commands/setPosition")
  {
    newPosition = intPayload;
  }
}


void processStepper()
{
  if (newPosition > currentPosition)
  {
    #if DRIVER_INVERTED_SLEEP == 1
    shadeStepper.sleepON();
    #endif
    #if DRIVER_INVERTED_SLEEP == 0
    shadeStepper.sleepOFF();
    #endif

    #if CLOSE_DIRECTION == 1
    shadeStepper.move(80, FORWARD);
    #endif
    #if CLOSE_DIRECTION == 0
    shadeStepper.move(80, BACKWARD);
    #endif

    currentPosition++;
    moving = true;
  }

  if (newPosition < currentPosition)
  {
    #if DRIVER_INVERTED_SLEEP == 1
    shadeStepper.sleepON();
    #endif
    #if DRIVER_INVERTED_SLEEP == 0
    shadeStepper.sleepOFF();
    #endif

    #if CLOSE_DIRECTION == 1
    shadeStepper.move(80, BACKWARD);
    #endif
    #if CLOSE_DIRECTION == 0
    shadeStepper.move(80, FORWARD);
    #endif

    currentPosition--;
    moving = true;
  }

  if (newPosition == currentPosition && moving == true)
  {
    #if DRIVER_INVERTED_SLEEP == 1
    shadeStepper.sleepOFF();
    #endif
    #if DRIVER_INVERTED_SLEEP == 0
    shadeStepper.sleepON();
    #endif
    String temp_str = String(currentPosition);
    temp_str.toCharArray(positionPublish, temp_str.length() + 1);
    client.publish("hubitat/"USER_DEVICE_NETWORK_ID"/windowShade/attributes/position", positionPublish); 
    moving = false;
  }
  
  Serial.println(currentPosition);
  Serial.println(newPosition);
}


void checkIn()
{
  client.publish("hubitat/"USER_DEVICE_NETWORK_ID"/status/device", "OK"); 
}


void setup() {
  Serial.begin(115200);
  
  shadeStepper.setMicrostepping(STEPPER_MICROSTEPPING);            // 0 -> Full Step                                
  shadeStepper.setSpeedRPM(STEPPER_SPEED);     // set speed in RPM, rotations per minute

  #if DRIVER_INVERTED_SLEEP == 1
  shadeStepper.sleepOFF();
  #endif
  #if DRIVER_INVERTED_SLEEP == 0
  shadeStepper.sleepON();
  #endif
  
  WiFi.mode(WIFI_STA);
  setup_wifi();
  
  client.setServer(mqtt_server, mqtt_port);
  client.setCallback(mqttCallback);
  ArduinoOTA.setHostname(USER_DEVICE_NETWORK_ID);
  ArduinoOTA.begin();

  delay(10);
  
  timer.setInterval(((1 << STEPPER_MICROSTEPPING)*5800)/STEPPER_SPEED/2, processStepper);   
  timer.setInterval(90000, checkIn);
}


void loop() 
{
  if (!client.connected()) 
  {
    reconnect();
  }

  client.loop();
  ArduinoOTA.handle();
  timer.run();
}
