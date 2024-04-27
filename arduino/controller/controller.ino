#include <Servo.h>

// Pin map
#define LDR0 A0
#define LDR1 A1

#define LED 4
#define SERV_ALT 5

// Data types

enum machinestate
{
	SETUP,
	BALANCE,
	GO_SOUTH,
	GO_NORTH,
} machine_state;

enum event
{
	SET_SENSOR_FINISHED,
	LDRS_EQUALS,
	LDR_SOUTH_BIGGEST,
	LDR_NORTH_BIGGEST
} new_event;

struct arrayofsensors
{
	int south;
	int north;
} array_of_sensors;

Servo serv_alt;

// Prototypes
//

void get_event();
void go_south();
void go_north();
void cont();
void error();
void init_serv();
void led_off();
void led_om();
void move_serv(int);
void reset_timer();
void fsm();
void wait();

// Matrix

#define NUM_OF_STATES 4
#define NUM_OF_EVENTS 4

typedef void (*transition)();
transition state_table[NUM_OF_STATES][NUM_OF_EVENTS] =
	{
		{wait, cont, go_south, go_north},  // state SETUP
		{error, cont, go_south, go_north}, // state BALANCE
		{error, cont, go_south, go_north}, // state GO_SOUTH
		{error, cont, go_south, go_north}, // state GO_NORTH

};

// parameters
#define SERV_INIT 90
#define MAX_TIME 500
#define ANG_MOVE 5
#define DIFF_MIN_LDR 100

// globalVar
unsigned long time_from;
unsigned long now;

// functions
void init_serv()
{
	serv_alt.write(SERV_INIT);
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
}

void reset_timer()
{
	time_from = millis();
}

void cont()
{
	led_off();
	if ((now - time_from) >= MAX_TIME)
	{
		reset_timer();
	}
	machine_state = BALANCE;
}

void wait()
{
}

void error()
{
	// Ini-Debug
	Serial.print("PROBLEMA");
	// End-Debug
}

void go_south()
{
	machine_state = GO_SOUTH;
	led_on();
	move_serv(-ANG_MOVE);
	if ((now - time_from) >= MAX_TIME)
	{
		reset_timer();
	}
}

void go_north()
{
	machine_state = GO_NORTH;
	led_on();
	move_serv(ANG_MOVE);
	if ((now - time_from) >= MAX_TIME)
	{
		reset_timer();
	}
}

void get_event()
{
	now = millis();

	if ((now - time_from) >= MAX_TIME)
	{

		array_of_sensors.south = analogRead(LDR0);
		array_of_sensors.north = analogRead(LDR1);

		if (abs(array_of_sensors.south - array_of_sensors.north) > DIFF_MIN_LDR)
		{
			if (array_of_sensors.south > array_of_sensors.north)
			{
				new_event = LDR_SOUTH_BIGGEST;
			}
			else
			{
				new_event = LDR_NORTH_BIGGEST;
			}
		}
		else
		{
			new_event = LDRS_EQUALS;
		}
	}
	else
	{
		// new_event = CONTINUE;
	}
}

void fsm()
{
	while (1)
	{
		get_event();
		state_table[machine_state][new_event]();
	}
}

// principal
void setup()
{
	// Ini-Debug
	Serial.begin(9600);
	// End-Debug

	machine_state = SETUP;
	pinMode(LED, OUTPUT);
	digitalWrite(LED, HIGH);
	serv_alt.attach(SERV_ALT);
	init_serv();
	reset_timer();
}

void loop()
{
	fsm();
}