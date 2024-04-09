#define LDR0 A0
#define LDR1 A1
#define LDR2 A2
#define LDR3 A3
#define LDR4 A4

#define MAX_VAL 1023

#define BASE0 1000
#define BASE1 400
#define BASE2 400
#define BASE3 400
#define BASE4 400


// C++ code
//

typedef struct {
	int x;
  	int y;
} Point;

int lum_t1[5];
Point sol;

void setup()
{
	Serial.begin(9600);
}

void loop()
{
  	lum_t1[0] = analogRead(LDR0);
  	lum_t1[1] = analogRead(LDR1);
  	lum_t1[2] = analogRead(LDR2);
  	lum_t1[3] = analogRead(LDR3);
  	lum_t1[4] = analogRead(LDR4);
    
  	calcular();
  
  	Serial.print(sol.x);
  	Serial.print("\t");
  	Serial.println(sol.y);
  
  	delay(1000); // Delay a little bit to improve simulation performance
}

void calcular() {
	sol.x = lum_t1[1] - BASE1 - (lum_t1[3] - BASE3);
    sol.y = lum_t1[2] - BASE2 - (lum_t1[4] - BASE4);
}
