#include <math.h>
#include <Servo.h>

// Pin map
//

#define LDR_Z A0
#define LDR_X A1
#define LDR_Y A2

#define SERV_AZ 5
#define SERV_ALT 6

// C++ code
//

enum MachineState
{
	SETUP,
	ON_Q1_Q3_AXIS,
	ON_Q2_Q4_AXIS,
	ON_Q0,
	ON_Q1,
	ON_Q2,
	ON_Q3,
	ON_Q4
};

enum Event
{
	PASS
}

typedef struct
{
	int q0;
	int q1;
	int q2;
	int q3;
	int q4;
} SkyState;

typedef struct
{
	int z;
	int x;
	int y;
} Scanner;

Servo serv_az;
Servo serv_alt;
MachineState machine_state;
SkyState sky_state;
Scanner scanner;

void setup()
{
	state = SETUP;
	Serial.begin(9600);
	serv_az.attach(SERV_AZ);
	serv_alt.attach(SERV_ALT);
}

void loop()
{
	Event event = get_event();

	// Para debugging
	Serial.print(sun_pos_plane.x);
	Serial.print("x\t");
	Serial.print(sun_pos_plane.y);
	Serial.print("y\t");
	Serial.print(sun_pos_plane.z);
	Serial.print("z\t");
	Serial.print(theta_deg); // TODO
	Serial.print("d\t");
	Serial.print(phi_deg); // TODO
	Serial.println("d");

	delay(1000);
}

// Captura de eventos
//

Event get_event()
{
	scanner.z = readSensor(LDR_Z);
	scanner.x = readSensor(LDR_X);
	scanner.y = readSensor(LDR_Y);

	sky_state.q0 = scanner.z;
	if (serv_az.read() == 0)
	{
		sky_state.q1 = scanner.x;
		sky_state.q3 = scanner.y;
		return PASS;
	}
	else // serv_az.read() == 90
	{
		sky_state.q2 = scanner.x;
		sky_state.q4 = scanner.y;
		// TODO: Ver qué estado Qn_IS_BIGGEST devolver
	}

	// TODO: Timer
}