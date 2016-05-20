//This program uses the following libraries -
//DHT

#include "DHT.h"
#define DHTPIN 12
#define DHTTYPE DHT11

#define NODE_ID "LivingRoom"
#define READ_DELAY_MS 250

long SLEEP_MINS = 10;

DHT dht(DHTPIN, DHTTYPE);

float humidity = 0.0;
float temp = 0.0;
float heatIndex = 0.0;

void setup() {
  Serial.begin(115200);
  dht.begin();

  Serial.print("{");
  Serial.print("\"init\": \"");
  Serial.print(NODE_ID);
  Serial.println("\"}");
  Serial.flush();
  
  readDHT();
  delay(500);
}

void loop() {
  delay(250);
  readDHT();
  printJson();
  delay(SLEEP_MINS * 1000 * 60);
}

void printJson() {

  if (isnan(temp)) {
    temp = -1000.0;
  }

  if (isnan(humidity)) {
    humidity = -1000.0;
  }

  if (isnan(heatIndex)) {
    heatIndex = -1000.0;
  }

  Serial.print("{ \"nodeId\" : \"");
  Serial.print(NODE_ID);
  Serial.print("\", ");
  Serial.print("\"airTemp\" : ");
  Serial.print(temp);
  Serial.print(", ");
  Serial.print("\"humidity\" : ");
  Serial.print(humidity);
  Serial.print(", ");
  Serial.print("\"heatIndex\" : ");
  Serial.print(heatIndex);
  Serial.print(" }");
  Serial.println();
  Serial.flush();
}

void readDHT() {
  // wait a moment
  delay(READ_DELAY_MS);
  humidity = dht.readHumidity();
  temp = dht.readTemperature(true);
  heatIndex = dht.computeHeatIndex(temp, humidity);
}

