//This program uses the following libraries -
//DHT
//OneWire
//LowPower

#include "DHT.h"
#include "OneWire.h"
#include "LowPower.h"
#define WATERPIN 6
#define DHTPIN 12
#define DHTTYPE DHT11

#define NODE_ID "DeckSensor"
#define READ_DELAY_MS 250
#define SLEEP_MINS 10
#define XBEE_SWITCH 2
#define XBEE_BOOT_MS 10000
#define XBEE_ON HIGH
#define XBEE_OFF LOW

DHT dht(DHTPIN, DHTTYPE);
OneWire ds(WATERPIN);

float humidity = 0.0;
float temp = 0.0;
float heatIndex = 0.0;
float waterTemp = -9999.99;

void setup() {
  pinMode(XBEE_SWITCH, OUTPUT);
  digitalWrite(XBEE_SWITCH, XBEE_ON);
  delay(XBEE_BOOT_MS);
  Serial.begin(115200);
  dht.begin();

  Serial.print("{");
  Serial.print("\"init\": \"");
  Serial.print(NODE_ID);
  Serial.println("\"}");
  Serial.flush();
  
  readDHT();
  //readWaterTemp();
  delay(500);
}

void loop() {
  delay(250);
  readDHT();
  //readWaterTemp();
  printJson();
  // sleep is for 10 seconds each, so this is
  // 10 minutes
  sleep(SLEEP_MINS * 6);
  //sleep(1);
}

void sleep(int repeat) {
  delay(1000);
  digitalWrite(XBEE_SWITCH, XBEE_OFF);
  
  if (repeat < 1) {
    repeat = 1;
  }
  
  for (int i = 0; i < repeat; i++) {
    LowPower.powerDown(SLEEP_8S, ADC_OFF, BOD_OFF);
    LowPower.powerDown(SLEEP_2S, ADC_OFF, BOD_OFF);
  }

  digitalWrite(XBEE_SWITCH, XBEE_ON);
  delay(XBEE_BOOT_MS);
}

void readWaterTemp() {
  delay(READ_DELAY_MS);
  byte data[12];
  byte addr[8];

  if ( !ds.search(addr)) {
    //no more sensors on chain, reset search
    ds.reset_search();
    waterTemp - 1000;
  }

  if ( OneWire::crc8( addr, 7) != addr[7]) {
    Serial.println("CRC is not valid!");
    waterTemp - 1000;
  }

  if ( addr[0] != 0x10 && addr[0] != 0x28) {
    Serial.print("Device is not recognized");
    waterTemp - 1000;
  }

  ds.reset();
  ds.select(addr);
  ds.write(0x44, 1);

  byte present = ds.reset();
  ds.select(addr);
  ds.write(0xBE);


  for (int i = 0; i < 9; i++) {
    data[i] = ds.read();
  }

  ds.reset_search();

  byte MSB = data[1];
  byte LSB = data[0];

  float tRead = (MSB << 8) | LSB;
  waterTemp = (tRead / 16) * 1.8 + 32;

  delay(READ_DELAY_MS);
}

void printJson() {

  if (isnan(temp)) {
    temp = -1000.0;
  }

  if (isnan(waterTemp)) {
    waterTemp = -1000.0;
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
  Serial.print("\"waterTemp\" : ");
  Serial.print(waterTemp);
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

