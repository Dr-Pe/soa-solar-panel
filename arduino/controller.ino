#include <Servo.h>

// Pin map
//

#define LDR0 A0
#define LDR1 A1
#define LDR2 A2
#define LDR3 A3
#define LDR4 A4

#define SERV_X 5
#define SERV_Y 6

// Magic numbers
//

#define MAX_VAL 1023

#define BASE0 1000
#define BASE1 400
#define BASE2 400
#define BASE3 400
#define BASE4 400

// C++ code
//

typedef struct
{
	int x;
	int y;
} Point2D;

int ldr_array[5];
Point2D sun_pos;
Servo servX;
Servo servY;

void setup()
{
	Serial.begin(9600);
	servX.attach(SERV_X);
	servY.attach(SERV_Y);
}

void loop()
{
	readSensors();
	calculateSunPosition();
	positionServos();

	// Para debugging
	Serial.print(sun_pos.x);
	Serial.print("\t");
	Serial.print(sun_pos.y);
	Serial.print("\t");
	Serial.print(sun_pos.x * 180 / MAX_VAL); // TODO
	Serial.print("d");
	Serial.print("\t");
	Serial.print(sun_pos.y * 180 / MAX_VAL); // TODO
	Serial.println("d");

	delay(1000);
}

void readSensors()
{
	ldr_array[0] = analogRead(LDR0);
	ldr_array[1] = analogRead(LDR1);
	ldr_array[2] = analogRead(LDR2);
	ldr_array[3] = analogRead(LDR3);
	ldr_array[4] = analogRead(LDR4);
}

void calculateSunPosition()
{
	sun_pos.x = (ldr_array[1] - BASE1) - (ldr_array[3] - BASE3);
	sun_pos.y = (ldr_array[2] - BASE2) - (ldr_array[4] - BASE4);
}

void positionServos()
{
	servX.write(sun_pos.x * 180 / MAX_VAL); // TODO transformar coordenadas
	servY.write(sun_pos.y * 180 / MAX_VAL); // TODO
}