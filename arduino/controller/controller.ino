#include <Servo.h>

// Constants
//

#define MAX_TIME 10000 // 10s
#define ZENITH 90
#define ZERO 0
#define QUARTER 90
#define LEAN_45 45
#define LEAN_135 135

// Pin map
//

#define LDR_Z A0
#define LDR_X A1
#define LDR_Y A2

#define LED 2
#define SERV_AZ 3
#define SERV_ALT 5

// Data types
//

enum machinestate
{
	SETUP,
	ON_Q1_Q3_AXIS,
	ON_Q2_Q4_AXIS,
	ON_Q0,
	ON_Q1,
	ON_Q2,
	ON_Q3,
	ON_Q4,
} machine_state;

enum event
{
	CONTINUE,
	Q0_IS_BIGGEST,
	Q1_IS_BIGGEST,
	Q2_IS_BIGGEST,
	Q3_IS_BIGGEST,
	Q4_IS_BIGGEST,
	WAKE_UP,
} new_event;

struct skystate
{
	int q0;
	int q1;
	int q2;
	int q3;
	int q4;
} sky_state;

struct arrayofsensors
{
	int z;
	int x;
	int y;
} array_of_sensors;

Servo serv_az;
Servo serv_alt;
unsigned long time_from;
unsigned long now;

// Prototypes
//

event biggest_qn();
event get_event();
void aim_to_Q1_Q3_AXIS();
void aim_to_Q2_Q4_AXIS();
void aim_to_Q0();
void aim_to_Q1();
void aim_to_Q2();
void aim_to_Q3();
void aim_to_Q4();
void error();

// Matrix
//

#define NUM_OF_STATES 8
#define NUM_OF_EVENTS 7

typedef void (*transition)();
transition state_table[NUM_OF_STATES][NUM_OF_EVENTS] =
	{
		{error, error, error, error, error, error, error},					   // state SETUP
		{aim_to_Q2_Q4_AXIS, error, error, error, error, error, error},		   // state ON_Q1_Q3_AXIS
		{sleeping, aim_to_Q0, aim_to_Q1, aim_to_Q2, aim_to_Q3, aim_to_Q4, error}, // state ON_Q2_Q4_AXIS
		{sleeping, error, error, error, error, error, aim_to_Q1_Q3_AXIS},		   // state ON_Q0
		{sleeping, error, error, error, error, error, aim_to_Q1_Q3_AXIS},		   // state ON_Q1
		{sleeping, error, error, error, error, error, aim_to_Q1_Q3_AXIS},		   // state ON_Q2
		{sleeping, error, error, error, error, error, aim_to_Q1_Q3_AXIS},		   // state ON_Q3
		{sleeping, error, error, error, error, error, aim_to_Q1_Q3_AXIS}		   // state ON_Q4

};

// Main
//

void setup()
{
	machine_state = SETUP;

	Serial.begin(9600);

	pinMode(LED, OUTPUT);
	digitalWrite(LED, HIGH);

	serv_az.attach(SERV_AZ);
	serv_alt.attach(SERV_ALT);

	aim_to_Q1_Q3_AXIS();
	delay(1000);
}

void loop()
{
	new_event = get_event();
	state_table[machine_state][new_event]();
	Serial.println(new_event);

	delay(1000);
}

// Captura de eventos
//

event get_event()
{
	if (digitalRead(LED) == LOW)
	{
		now = millis();
		if (now - time_from >= MAX_TIME)
			return WAKE_UP;
		else
			return CONTINUE;
	}

	array_of_sensors.z = analogRead(LDR_Z);
	array_of_sensors.x = analogRead(LDR_X);
	array_of_sensors.y = analogRead(LDR_Y);

	sky_state.q0 = array_of_sensors.z;
	if (serv_az.read() == ZERO)
	{
		sky_state.q1 = array_of_sensors.x;
		sky_state.q3 = array_of_sensors.y;
		return CONTINUE;
	}
	else // serv_az.read() == 90, etc
	{
		sky_state.q2 = array_of_sensors.x;
		sky_state.q4 = array_of_sensors.y;
		return biggest_qn();
	}
}

event biggest_qn()
{
	event max_q = Q0_IS_BIGGEST;
	int max_value = sky_state.q0;
	if (max_value < sky_state.q1)
	{
		max_q = Q1_IS_BIGGEST;
		max_value = sky_state.q1;
	}
	if (max_value < sky_state.q2)
	{
		max_q = Q2_IS_BIGGEST;
		max_value = sky_state.q2;
	}
	if (max_value < sky_state.q3)
	{
		max_q = Q3_IS_BIGGEST;
		max_value = sky_state.q3;
	}
	if (max_value < sky_state.q4)
	{
		max_q = Q4_IS_BIGGEST;
	}
	return max_q;
}

// Apuntado
//

void aim_to_Q1_Q3_AXIS()
{
	serv_az.write(ZERO);
	serv_alt.write(ZENITH);
	digitalWrite(LED, HIGH);
	machine_state = ON_Q1_Q3_AXIS;
	Serial.println("ON_Q1_Q3_AXIS");
}

void aim_to_Q2_Q4_AXIS()
{
	serv_az.write(QUARTER);
	serv_alt.write(ZENITH);
	machine_state = ON_Q2_Q4_AXIS;
	Serial.println("ON_Q2_Q4_AXIS");
}

void aim_to_Q0()
{
	serv_az.write(ZERO); // Opcional
	serv_alt.write(QUARTER);
	digitalWrite(LED, LOW);
	time_from = millis();
	machine_state = ON_Q0;
	Serial.println("ON_Q0");
}

void aim_to_Q1()
{
	serv_az.write(ZERO);
	serv_alt.write(LEAN_45);
	digitalWrite(LED, LOW);
	time_from = millis();
	machine_state = ON_Q1;
	Serial.println("ON_Q1");
}

void aim_to_Q2()
{
	serv_az.write(QUARTER);
	serv_alt.write(LEAN_135);
	digitalWrite(LED, LOW);
	time_from = millis();
	machine_state = ON_Q2;
	Serial.println("ON_Q2");
}

void aim_to_Q3()
{
	serv_az.write(ZERO);
	serv_alt.write(LEAN_135);
	digitalWrite(LED, LOW);
	time_from = millis();
	machine_state = ON_Q3;
	Serial.println("ON_Q3");
}

void aim_to_Q4()
{
	serv_az.write(QUARTER);
	serv_alt.write(LEAN_45);
	digitalWrite(LED, LOW);
	time_from = millis();
	machine_state = ON_Q4;
	Serial.println("ON_Q4");
}

// Error
//

void error()
{
	Serial.println("ERROR");
	return;
}

// Sleeping

void sleeping()
{
	Serial.println("SLEEPING");
	return;
}