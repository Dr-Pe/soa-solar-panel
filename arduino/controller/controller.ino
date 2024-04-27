#include <Servo.h>

// Parameters
//

#define ZENITH 90
#define MAX_TIME 1000
#define ANG_MOVE 5
#define DIFF_MIN_LDR 50

// Pin map
//

#define LDR0 A0
#define LDR1 A1

#define LED 2
#define SERV_ALT 5
#define SERV_ALT_NEG 6

// Data types
//

enum machinestate
{
	SETUP,
	WAITING,
	STEP_EAST,
	STEP_WEST,
	BALANCE,
} machine_state;

enum event
{
	WAIT,
	NONE_IS_BIGGER,
	EAST_IS_BIGGER,
	WEST_IS_BIGGER,
} new_event;

struct arrayofsensors
{
	int east;
	int west;
} array_of_sensors;

// Prototypes
//

event get_event();
void go_east();
void go_west();
void cont();
void error();
void wait();
void init_serv();
void led_off();
void led_om();
void move_serv(int);
void reset_timer();
void fsm();

// Matrix
//

#define NUM_OF_STATES 5
#define NUM_OF_EVENTS 4

typedef void (*transition)();
transition state_table[NUM_OF_STATES][NUM_OF_EVENTS] =
	{
		{wait, error, error, error},	 // state SETUP
		{wait, cont, go_east, go_west},	 // state WAITING
		{wait, error, error, error},	 // state STEP_EAST
		{wait, error, error, error},	 // state STEP_WEST
		{error, cont, go_east, go_west}, // state BALANCE

};

// Global variables
//

unsigned long time_from;
unsigned long now;

Servo serv_alt;
Servo serv_alt_neg;

// Functions
//

event get_event()
{
	now = millis();
	if ((now - time_from) >= MAX_TIME)
	{
		array_of_sensors.east = analogRead(LDR0);
		array_of_sensors.west = analogRead(LDR1);
		if (abs(array_of_sensors.east - array_of_sensors.west) > DIFF_MIN_LDR)
		{
			reset_timer();
			if (array_of_sensors.east > array_of_sensors.west)
				return EAST_IS_BIGGER;
			else
				return WEST_IS_BIGGER;
		}
		return NONE_IS_BIGGER;
	}
	return WAIT;
}

void init_serv()
{
	serv_alt.write(ZENITH);
	serv_alt_neg.write(ZENITH);
}

void led_on()
{
	digitalWrite(LED, HIGH);
}

void led_off()
{
	digitalWrite(LED, LOW);
}

void move_serv(int offset)
{
	serv_alt.write(serv_alt.read() + offset);
	serv_alt_neg.write(serv_alt_neg.read() - offset);
}

void reset_timer()
{
	time_from = millis();
}

void cont()
{
	led_off();
	machine_state = BALANCE;
}

void error()
{
	// Ini-Debug
	Serial.println("ERROR");
	// End-Debug
}

void wait()
{
	machine_state = WAITING;
}

void go_east()
{
	machine_state = STEP_EAST;
	led_on();
	move_serv(-ANG_MOVE);
}

void go_west()
{
	machine_state = STEP_WEST;
	led_on();
	move_serv(ANG_MOVE);
}

void fsm()
{
	new_event = get_event();
	Serial.println(machine_state);
	state_table[machine_state][new_event]();
}

// Main
//

void setup()
{
	// Ini-Debug
	Serial.begin(9600);
	// End-Debug

	pinMode(LED, OUTPUT);
	digitalWrite(LED, HIGH);

	serv_alt.attach(SERV_ALT);
	serv_alt_neg.attach(SERV_ALT_NEG);

	init_serv();
	reset_timer();
	machine_state = WAITING;
}

void loop()
{
	fsm();
}