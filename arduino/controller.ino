#include <math.h>
#include <Servo.h>

// Pin map
//

#define LDR0 A0
#define LDR1 A1
#define LDR2 A2
#define LDR3 A3
#define LDR4 A4

#define SERV_THETA 5
#define SERV_ANTI_PHI 6

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
	double x;
	double y;
  	double z;
} Cartesian;

typedef struct
{
	double theta;
	double phi;
} Spherical;

int ldr_array[5];
Cartesian sun_pos_plane;
Spherical sun_pos_sphere;
int theta_deg;
int phi_deg;
int flag = 0;

Servo servTheta;
Servo servPhi;

void setup()
{
	Serial.begin(9600);
	servTheta.attach(SERV_THETA);
	servPhi.attach(SERV_ANTI_PHI);
}

void loop()
{
	readSensors();
	calculateSunPosition();
	calculateSphericalCoordinates();
	calculateAngles();
	positionServos();

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

	flag = 1;
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
	sun_pos_plane.x = ldr_array[1] - ldr_array[3]; // TODO evaluar posibilidad de resto haciendo lio
	sun_pos_plane.y = ldr_array[2] - ldr_array[4];
  	sun_pos_plane.z = ldr_array[0];
}

void calculateSphericalCoordinates()
{
	sun_pos_sphere.theta = atan(sqrt(sun_pos_plane.x * sun_pos_plane.x + sun_pos_plane.y * sun_pos_plane.y) / sun_pos_plane.z);
	if(sun_pos_plane.x > 0 && sun_pos_plane.y > 0) // 1Q
	{
		sun_pos_sphere.phi = atan(sun_pos_plane.y / sun_pos_plane.x);
	}
	else if(sun_pos_plane.x < 0) // 2Q y 3Q
	{
		sun_pos_sphere.phi = atan(sun_pos_plane.y / sun_pos_plane.x) + M_PI;
	}
	else // 4Q
	{
		sun_pos_sphere.phi = 2 * M_PI + atan(sun_pos_plane.y / sun_pos_plane.x);
	}
}

void calculateAngles()
{
	theta_deg = sun_pos_sphere.theta * 180 / M_PI;
	phi_deg = sun_pos_sphere.phi * 180 / M_PI;
}

void positionServos()
{
	servTheta.write(theta_deg + flag * (theta_deg - servTheta.read()));
	servPhi.write(phi_deg + flag * (phi_deg - servPhi.read()));
}