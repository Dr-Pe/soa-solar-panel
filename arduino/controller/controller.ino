#include <Servo.h>

// Constants
//

#define ZENITH 90
#define EAST 40
#define WEST 140
#define DEVIATION 200 // Cota para que cambie el estado al cambiar LDRs

// Pin map
//

#define LDR0 A0
#define LDR1 A1

#define LED 2
#define SERV_ALT 5

// Data types
//

enum machinestate
{
	SETUP,
	AIM_ZENITH,
	AIM_EAST,
	AIM_WEST,
} machine_state;

enum event
{
	CONTINUE,
	EAST_IS_BIGGER,
	WEST_IS_BIGGER,
	RESET,
} new_event;

struct arrayofsensors
{
	int east;
	int west;
} array_of_sensors;

Servo serv_alt;

// Prototypes
//

event get_event();
event calculate_event();
void aim_to_zenith();
void aim_to_east();
void aim_to_west();
void error();

// Matrix
//

#define NUM_OF_STATES 4
#define NUM_OF_EVENTS 4

typedef void (*transition)();
transition state_table[NUM_OF_STATES][NUM_OF_EVENTS] =
	{
		{cont, error, error, reset},			 // state SETUP
		{cont, aim_to_east, aim_to_west, reset}, // state AIM_ZENITH
		{cont, cont, aim_to_zenith, reset},		 // state AIM_EAST
		{cont, aim_to_zenith, cont, reset},		 // state AIM_WEST

};

// Main
//

void setup()
{
	machine_state = SETUP;

	Serial.begin(9600);

	pinMode(LED, OUTPUT);
	digitalWrite(LED, HIGH);

	serv_alt.attach(SERV_ALT);

	aim_to_zenith();
}

void loop()
{
	new_event = get_event();
	state_table[machine_state][new_event]();
}

// Captura de eventos
//

event get_event()
{
	// if (digitalRead(LED) == LOW)
	// {
	// 	now = millis();
	// 	if (now - time_from >= MAX_TIME)
	// 		return WAKE_UP;
	// 	else
	// 		return CONTINUE;
	// }

	array_of_sensors.east = analogRead(LDR0);
	array_of_sensors.west = analogRead(LDR1);
	return calculate_event();
}

event calculate_event()
{
	if (array_of_sensors.east <= array_of_sensors.west * (1 + DEVIATION) && array_of_sensors.east >= array_of_sensors.west * (1 - DEVIATION))
		return CONTINUE;
	else if (array_of_sensors.east > array_of_sensors.west)
		return EAST_IS_BIGGER;
	else
		return WEST_IS_BIGGER;
}

// Apuntado
//

void aim_to_zenith()
{
	serv_alt.write(ZENITH);
	digitalWrite(LED, LOW);
	machine_state = AIM_ZENITH;
	Serial.println("AIM_ZENITH");
}

void aim_to_east()
{
	serv_alt.write(EAST);
	digitalWrite(LED, LOW);
	machine_state = AIM_EAST;
	Serial.println("AIM_EAST");
}

void aim_to_west()
{
	serv_alt.write(WEST);
	digitalWrite(LED, LOW);
	machine_state = AIM_WEST;
	Serial.println("AIM_WEST");
}

// Error
//

void error()
{
	Serial.println("ERROR");
	return;
}

// Continue
//

void cont()
{
	Serial.println("CONTINUE");
	return;
}

// Reset
//

void reset()
{
	Serial.println("RESET");
	return;
}