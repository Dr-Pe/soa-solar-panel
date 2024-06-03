#include <Servo.h>
#include <SoftwareSerial.h>

// Parameters
//

#define ZENITH 90
#define MAX_TIME 500
#define ANG_MOVE 5
#define DIFF_MIN_LDR 200

// Pin map
//

#define LDR0 A0
#define LDR1 A1

#define LED 2
#define SERV_ALT 5
#define SERV_ALT_NEG 6
#define RESTART_PIN 7
#define RX 10
#define TX 11

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
	RESTART,
} new_event;

struct arrayofsensors
{
	int east;
	int west;
} array_of_sensors;

// Prototypes
//

event get_event();
void change_machine_state(machinestate new_state);
void start();
void led_on();
void led_off();
void move_serv(int offset);
void reset_timer();
void cont();
void error();
void wait();
void go_east();
void go_west();
void fsm();

// Matrix
//

#define NUM_OF_STATES 5
#define NUM_OF_EVENTS 5

typedef void (*transition)();
transition state_table[NUM_OF_STATES][NUM_OF_EVENTS] =
	{
		{wait, error, error, error, start},		// state SETUP
		{wait, cont, go_east, go_west, start},	// state WAITING
		{wait, error, error, error, start},		// state STEP_EAST
		{wait, error, error, error, start},		// state STEP_WEST
		{error, cont, go_east, go_west, start}, // state BALANCE
};

// Global variables
//

unsigned long time_from;
unsigned long now;

Servo serv_alt;
Servo serv_alt_neg;

SoftwareSerial bt(RX, TX);

// Functions
//

event get_event()
{
	if (digitalRead(RESTART_PIN) == LOW)
		return RESTART;
  
  if(bt.available() && bt.read() == 'R')
    return RESTART;

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

void change_machine_state(machinestate new_state)
{
	if (machine_state != new_state)
	{
		switch (new_state)
		{
		case SETUP:
			Serial.println("SETUP");
			break;
		case WAITING:
			Serial.println("WAITING");
			break;
		case STEP_EAST:
			Serial.println("STEP_EAST");
			break;
		case STEP_WEST:
			Serial.println("STEP_WEST");
			break;
		case BALANCE:
			Serial.println("BALANCE");
			break;
		}
	}
	machine_state = new_state;
}

void start()
{
	digitalWrite(LED, HIGH);
	serv_alt.write(ZENITH);
	serv_alt_neg.write(ZENITH);
	reset_timer();
	change_machine_state(SETUP);
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
  int serv_alt_val = serv_alt.read();
  int serv_alt_neg_val = serv_alt_neg.read();
  if(serv_alt_val >= 175 || serv_alt_val <= 5)
    return;

	serv_alt.write(serv_alt_val + offset);
	serv_alt_neg.write(serv_alt_neg_val - offset);
  
}

void reset_timer()
{
	time_from = millis();
}

void cont()
{
	led_off();
	change_machine_state(BALANCE);
  // bt.print("[");
  // bt.print(array_of_sensors.east);
  // bt.print(",");
  // bt.print(array_of_sensors.west);
  // bt.print("]");
}

void error()
{
	// Ini-Debug
	Serial.println("ERROR");
	// End-Debug
}

void wait()
{
	change_machine_state(WAITING);
}

void go_east()
{
	change_machine_state(STEP_EAST);
	led_on();
	move_serv(-ANG_MOVE);
}

void go_west()
{
	change_machine_state(STEP_WEST);
	led_on();
	move_serv(ANG_MOVE);
}

void fsm()
{
	new_event = get_event();
	state_table[machine_state][new_event]();
}

// Main
//

void setup()
{
	Serial.begin(9600);
  bt.begin(9600);

	pinMode(LED, OUTPUT);
	pinMode(RESTART_PIN, INPUT);

	serv_alt.attach(SERV_ALT);
	serv_alt_neg.attach(SERV_ALT_NEG);

	start();
}

void loop()
{
	fsm();
}