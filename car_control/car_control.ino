#include "Timers.h"
#include <Servo.h>

/*defination for steper input*/
int Pin0 = 2;
int Pin1 = 3;
int Pin2 = 4;
int Pin3 = 5;

int Pin4 = 28;
int Pin5 = 30;
int Pin6 = 32;
int Pin7 = 34;

int _step = 0;
int stepperSpeed = 20; // one ms one step

/*defination for distance sensor*/
#define TRIG_PIN 22
#define ECHO_PIN 24
char val; 
float cm;
long a; 

Servo myservo;
char inByte = 0; //串口接收的数据  
int angle = 0;  //角度值  
String temp = "";//临时字符变量，又或者说是缓存用的吧

void setup() {
  Serial.begin(115200);
  Serial.print("\r\nArduino Started");
  /*distance pin init*/
  pinMode(TRIG_PIN,OUTPUT);
  pinMode(ECHO_PIN,INPUT);
  /* start the tesk of measure distance each 1s*/
  TCs.AddFunc(0, distance, 1000);
  /* steper pin init*/
  pinMode(Pin0, OUTPUT);
  pinMode(Pin1, OUTPUT);
  pinMode(Pin2, OUTPUT);
  pinMode(Pin3, OUTPUT);
  pinMode(Pin4, OUTPUT);
  pinMode(Pin5, OUTPUT);
  pinMode(Pin6, OUTPUT);
  pinMode(Pin7, OUTPUT);
  myservo.attach(9);
}

/*
      1、使用电压：DC5V；         
      2、静态电流：小于2mA；        
      3、电平输出：高5V；              
      4、电平输出：底0V；
      5、感应角度：不大于15度；
      6、探测距离：2cm-450cm  7:高精度可达0.2cm； 
      7、接线方式，VCC、trig（控制端）、echo（接收端）、GND。
*/
void distance()
{
  digitalWrite(TRIG_PIN, LOW); 
  delayMicroseconds(2); 
  digitalWrite(TRIG_PIN, HIGH); 
  delayMicroseconds(10); 
  digitalWrite(TRIG_PIN, LOW); 
  cm = pulseIn(ECHO_PIN, HIGH) / 58.0; //算成厘米 
  Serial.print(cm); 
  Serial.print("cm"); 
  Serial.println(); 
}

void _forward() {
  switch(_step){
    case 0:
    //stepperSpeed++;
      digitalWrite(Pin0, HIGH);
      digitalWrite(Pin1, LOW);
      digitalWrite(Pin2, LOW);
      digitalWrite(Pin3, LOW);//32A

      digitalWrite(Pin4, HIGH);
      digitalWrite(Pin5, LOW);
      digitalWrite(Pin6, LOW);
      digitalWrite(Pin7, LOW);//32A
    break;
    case 1:
      digitalWrite(Pin0, HIGH);
      digitalWrite(Pin1, LOW);//10B
      digitalWrite(Pin2, HIGH);
      digitalWrite(Pin3, LOW);

      digitalWrite(Pin4, HIGH);
      digitalWrite(Pin5, LOW);//10B
      digitalWrite(Pin6, HIGH);
      digitalWrite(Pin7, LOW);
    break;
    case 2:
      digitalWrite(Pin0, LOW);
      digitalWrite(Pin1, LOW);
      digitalWrite(Pin2, HIGH);
      digitalWrite(Pin3, LOW);

      digitalWrite(Pin4, LOW);
      digitalWrite(Pin5, LOW);
      digitalWrite(Pin6, HIGH);
      digitalWrite(Pin7, LOW);
      
    break;
    case 3:
      digitalWrite(Pin0, LOW);
      digitalWrite(Pin1, HIGH);
      digitalWrite(Pin2, HIGH);
      digitalWrite(Pin3, LOW);

      digitalWrite(Pin4, LOW);
      digitalWrite(Pin5, HIGH);
      digitalWrite(Pin6, HIGH);
      digitalWrite(Pin7, LOW);
    break;
    case 4:
      digitalWrite(Pin0, LOW);
      digitalWrite(Pin1, HIGH);
      digitalWrite(Pin2, LOW);
      digitalWrite(Pin3, LOW);

       digitalWrite(Pin4, LOW);
      digitalWrite(Pin5, HIGH);
      digitalWrite(Pin6, LOW);
      digitalWrite(Pin7, LOW);
    break;
    case 5:
      digitalWrite(Pin0, LOW);
      digitalWrite(Pin1, HIGH);
      digitalWrite(Pin2, LOW);
      digitalWrite(Pin3, HIGH);

      digitalWrite(Pin4, LOW);
      digitalWrite(Pin5, HIGH);
      digitalWrite(Pin6, LOW);
      digitalWrite(Pin7, HIGH);
    break;
      case 6:
      digitalWrite(Pin0, LOW);
      digitalWrite(Pin1, LOW);
      digitalWrite(Pin2, LOW);
      digitalWrite(Pin3, HIGH);

      digitalWrite(Pin4, LOW);
      digitalWrite(Pin5, LOW);
      digitalWrite(Pin6, LOW);
      digitalWrite(Pin7, HIGH);
    break;
    case 7:
      digitalWrite(Pin0, HIGH);
      digitalWrite(Pin1, LOW);
      digitalWrite(Pin2, LOW);
      digitalWrite(Pin3, HIGH);

      digitalWrite(Pin4, HIGH);
      digitalWrite(Pin5, LOW);
      digitalWrite(Pin6, LOW);
      digitalWrite(Pin7, HIGH);
    break;
    default:
      digitalWrite(Pin0, LOW);
      digitalWrite(Pin1, LOW);
      digitalWrite(Pin2, LOW);
      digitalWrite(Pin3, LOW);

      digitalWrite(Pin4, LOW);
      digitalWrite(Pin5, LOW);
      digitalWrite(Pin6, LOW);
      digitalWrite(Pin7, LOW);
    break;
  }
   _step++;
 
  if(_step>7){    _step=0;  }
 
  delay(stepperSpeed);
}

void _back() {
  switch(_step){
    case 0:
    //stepperSpeed++;
      digitalWrite(Pin0, LOW);
      digitalWrite(Pin1, LOW);
      digitalWrite(Pin2, LOW);
      digitalWrite(Pin3, HIGH);//32A

      digitalWrite(Pin4, LOW);
      digitalWrite(Pin5, LOW);
      digitalWrite(Pin6, LOW);
      digitalWrite(Pin7, HIGH);//32A
    break;
    case 1:
      digitalWrite(Pin0, LOW);
      digitalWrite(Pin1, HIGH);//10B
      digitalWrite(Pin2, LOW);
      digitalWrite(Pin3, HIGH);

      digitalWrite(Pin4, LOW);
      digitalWrite(Pin5, HIGH);//10B
      digitalWrite(Pin6, LOW);
      digitalWrite(Pin7, HIGH);
    break;
    case 2:
      digitalWrite(Pin0, LOW);
      digitalWrite(Pin1, HIGH);
      digitalWrite(Pin2, LOW);
      digitalWrite(Pin3, LOW);

      digitalWrite(Pin4, LOW);
      digitalWrite(Pin5, HIGH);
      digitalWrite(Pin6, LOW);
      digitalWrite(Pin7, LOW);
      
    break;
    case 3:
      digitalWrite(Pin0, LOW);
      digitalWrite(Pin1, HIGH);
      digitalWrite(Pin2, HIGH);
      digitalWrite(Pin3, LOW);

      digitalWrite(Pin4, LOW);
      digitalWrite(Pin5, HIGH);
      digitalWrite(Pin6, HIGH);
      digitalWrite(Pin7, LOW);
    break;
    case 4:
      digitalWrite(Pin0, LOW);
      digitalWrite(Pin1, LOW);
      digitalWrite(Pin2, HIGH);
      digitalWrite(Pin3, LOW);

       digitalWrite(Pin4, LOW);
      digitalWrite(Pin5, LOW);
      digitalWrite(Pin6, HIGH);
      digitalWrite(Pin7, LOW);
    break;
    case 5:
      digitalWrite(Pin0, HIGH);
      digitalWrite(Pin1, LOW);
      digitalWrite(Pin2, HIGH);
      digitalWrite(Pin3, LOW);

      digitalWrite(Pin4, HIGH);
      digitalWrite(Pin5, LOW);
      digitalWrite(Pin6, HIGH);
      digitalWrite(Pin7, LOW);
    break;
      case 6:
      digitalWrite(Pin0, HIGH);
      digitalWrite(Pin1, LOW);
      digitalWrite(Pin2, LOW);
      digitalWrite(Pin3, LOW);

      digitalWrite(Pin4, HIGH);
      digitalWrite(Pin5, LOW);
      digitalWrite(Pin6, LOW);
      digitalWrite(Pin7, LOW);
    break;
    case 7:
      digitalWrite(Pin0, HIGH);
      digitalWrite(Pin1, LOW);
      digitalWrite(Pin2, LOW);
      digitalWrite(Pin3, HIGH);

      digitalWrite(Pin4, HIGH);
      digitalWrite(Pin5, LOW);
      digitalWrite(Pin6, LOW);
      digitalWrite(Pin7, HIGH);
    break;
    default:
      digitalWrite(Pin0, LOW);
      digitalWrite(Pin1, LOW);
      digitalWrite(Pin2, LOW);
      digitalWrite(Pin3, LOW);

      digitalWrite(Pin4, LOW);
      digitalWrite(Pin5, LOW);
      digitalWrite(Pin6, LOW);
      digitalWrite(Pin7, LOW);
    break;
  }
   _step++;
 
  if(_step>7){    _step=0;  }
 
  delay(stepperSpeed);
}

void _left() {
  switch(_step){
    case 0:
    //stepperSpeed++;
      digitalWrite(Pin0, HIGH);
      digitalWrite(Pin1, LOW);
      digitalWrite(Pin2, LOW);
      digitalWrite(Pin3, LOW);//32A
    break;
    case 1:
      digitalWrite(Pin0, HIGH);
      digitalWrite(Pin1, LOW);//10B
      digitalWrite(Pin2, HIGH);
      digitalWrite(Pin3, LOW);
    break;
    case 2:
      digitalWrite(Pin0, LOW);
      digitalWrite(Pin1, LOW);
      digitalWrite(Pin2, HIGH);
      digitalWrite(Pin3, LOW);
    break;
    case 3:
      digitalWrite(Pin0, LOW);
      digitalWrite(Pin1, HIGH);
      digitalWrite(Pin2, HIGH);
      digitalWrite(Pin3, LOW);
    break;
    case 4:
      digitalWrite(Pin0, LOW);
      digitalWrite(Pin1, HIGH);
      digitalWrite(Pin2, LOW);
      digitalWrite(Pin3, LOW);
    break;
    case 5:
      digitalWrite(Pin0, LOW);
      digitalWrite(Pin1, HIGH);
      digitalWrite(Pin2, LOW);
      digitalWrite(Pin3, HIGH);
    break;
      case 6:
      digitalWrite(Pin0, LOW);
      digitalWrite(Pin1, LOW);
      digitalWrite(Pin2, LOW);
      digitalWrite(Pin3, HIGH);
    break;
    case 7:
      digitalWrite(Pin0, HIGH);
      digitalWrite(Pin1, LOW);
      digitalWrite(Pin2, LOW);
      digitalWrite(Pin3, HIGH);
    break;
    default:
      digitalWrite(Pin0, LOW);
      digitalWrite(Pin1, LOW);
      digitalWrite(Pin2, LOW);
      digitalWrite(Pin3, LOW);
    break;
  }
    _step++;
 
  if(_step>7){    _step=0;  }
 
  delay(stepperSpeed);
}

void _right() {
  switch(_step){
    case 0:
    //stepperSpeed++;
      digitalWrite(Pin4, HIGH);
      digitalWrite(Pin5, LOW);
      digitalWrite(Pin6, LOW);
      digitalWrite(Pin7, LOW);//32A
    break;
    case 1:
      digitalWrite(Pin4, HIGH);
      digitalWrite(Pin5, LOW);//10B
      digitalWrite(Pin6, HIGH);
      digitalWrite(Pin7, LOW);
    break;
    case 2:
      digitalWrite(Pin4, LOW);
      digitalWrite(Pin5, LOW);
      digitalWrite(Pin6, HIGH);
      digitalWrite(Pin7, LOW);
    break;
    case 3:
      digitalWrite(Pin4, LOW);
      digitalWrite(Pin5, HIGH);
      digitalWrite(Pin6, HIGH);
      digitalWrite(Pin7, LOW);
    break;
    case 4:
      digitalWrite(Pin4, LOW);
      digitalWrite(Pin5, HIGH);
      digitalWrite(Pin6, LOW);
      digitalWrite(Pin7, LOW);
    break;
    case 5:
      digitalWrite(Pin4, LOW);
      digitalWrite(Pin5, HIGH);
      digitalWrite(Pin6, LOW);
      digitalWrite(Pin7, HIGH);
    break;
      case 6:
      digitalWrite(Pin4, LOW);
      digitalWrite(Pin5, LOW);
      digitalWrite(Pin6, LOW);
      digitalWrite(Pin7, HIGH);
    break;
    case 7:
      digitalWrite(Pin4, HIGH);
      digitalWrite(Pin5, LOW);
      digitalWrite(Pin6, LOW);
      digitalWrite(Pin7, HIGH);
    break;
    default:
      digitalWrite(Pin4, LOW);
      digitalWrite(Pin5, LOW);
      digitalWrite(Pin6, LOW);
      digitalWrite(Pin7, LOW);
    break;
  }
    _step++;
 
  if(_step>7){    _step=0;  }
 
  delay(stepperSpeed);
}

void loop() {
  Serial.print("\r\nloop");
  if (distance > 20) {
  _forward();
  } else 
  
  while (Serial.available() > 0) //判断串口是否有数据  
  {  
    inByte = Serial.read();//读取数据，串口一次只能读1个字符  
    temp += inByte;//把读到的字符存进临时变量里面缓存，  
                   //再继续判断串口还有没有数据，知道把所有数据都读取出来  
   }  
  
   if(temp != "")   //判断临时变量是否为空  
   {  
    angle = temp.toInt();    //把变量字符串类型转成整型  
    Serial.println(angle);  //输出数据到串口上，以便观察  
   }  
  temp = "";//clear临时变量  
  myservo.write(angle);  //控制舵机转动相应的角度。  
  delay(100);//延时100毫秒 
}

