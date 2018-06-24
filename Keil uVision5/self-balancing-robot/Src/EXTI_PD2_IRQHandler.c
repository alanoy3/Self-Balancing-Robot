#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <math.h>

#include "stm32f0xx_hal.h"
#include "stm32f0xx.h"
#include "stm32f0xx_it.h"
#include "tim.h"
#include "override_printf_&_scanf.h"		/* printf & scanf 複寫 */


extern void MPU6050_GET_DATA (void);					//取得 MPU6050 資料，在 MPU6050_get_data.c 中
extern int Read_Encoder (TIM_HandleTypeDef *);		    // 取得 編碼器 資料，在 Encoder.c 中
extern TIM_OC_InitTypeDef 	   sConfigOC;				//建立計時器 PWM 設定 結構，在 self-balancing-robot_main_Init.c 中


uint8_t Car_switch = 0;

#define PWM_MAX     4000    //4800-1              //PWM 最大值


//------------------PID參數--------------
        //重心偏移
#define ZHONGZHI 1.4;       // 1.4
        //平衡
double  Balance_Kp=3000,       //3000
        Balance_Kd=6,       //5
        //編碼器速度
        Velocity_Kp=200,       //20
        Velocity_Ki=1,        //0.8
		//轉向
		Kp=42,
		Kd=0; 



//遙控車子的變數
extern double Movement ,			// 前進、後退 變數，在 EXTI_USART1_BT.c 中
						 Turn_Amplitude;		//轉向 變數，在 EXTI_USART1_BT.c 中



//在 MPU6050_get_data.c 中變數
extern double Pitch;							//俯仰角(X軸角度)
extern short gyro[3],						//陀螺儀角速度資料
			 accel_short[3];				//加速計資料


double  Angle_Balance;						//X軸平衡角
double  Gyro_Balance,							//X軸平衡角速度
        Gyro_Turn,							//Z軸轉向角速度
        Acceleration_Z;						//Z軸加速度
int Encoder_Left,						    // 左輪編碼器 計數值
	Encoder_Right,						    // 右輪編碼器 計數值
    Balance_Pwm,                            //車體平衡 PWM 值
    Velocity_Pwm,                           //車體速度 PWM 值
		Turn_Pwm,							//車體轉向 PWM 值
    Motor_Left_PWM,                         //左輪 PWM 值
    Motor_Right_PWM,                        //右輪 PWM 值
    error_motor_L,                          //左輪誤差
    error_motor_R;                          //右輪誤差




/*--------------------------------------------------------------------------
函數功能：直立PD控制
輸入參數：角度、角速度
回傳值：直立控制PWM
----------------------------------------------------------------------------*/
int balance(float Angle,float Gyro)
{  
   float Bias;
	 int balance;
	 Bias=Angle-ZHONGZHI;       //===求出平衡的角度中性值 和機械相關
	 balance=Balance_Kp*Bias+Gyro*Balance_Kd;   //===計算平衡控制的馬達PWM PD控制 kp是P係數 kd是D係數 
	 return balance;
}





/**************************************************************************
函數功能：速度PI控制 修改前進後退速度
輸入參數：左輪編碼器、右輪編碼器
回傳值：速度控制PWM
**************************************************************************/
int velocity(int Encoder_Left,int Encoder_Right)
{  
     static float Velocity,Encoder_Least,Encoder;
	  static float Encoder_Integral,Target_Velocity;

   //=============速度PI控制器=======================//	
		Encoder_Least =(Encoder_Left+Encoder_Right)-0;                    //===獲取最新速度偏差==測量速度（左右編碼器之和）-目標速度（此處為零） 
		Encoder *= 0.8;		                                                //===一階低通濾波器       
		Encoder += Encoder_Least*0.2;	                                    //===一階低通濾波器   
		Encoder_Integral +=Encoder;                                       //===積分出位移 積分時間：10ms
		Encoder_Integral=Encoder_Integral-Movement;                       //===接收遙控器數據，控制前進後退
		if(Encoder_Integral>10000)  	Encoder_Integral=10000;             //===積分限制
		if(Encoder_Integral<-10000)	    Encoder_Integral=-10000;              //===積分限制
		Velocity=Encoder*Velocity_Kp+Encoder_Integral*Velocity_Ki;                          //===速度控制	
//		if(Turn_Off(Angle_Balance,Voltage)==1||Flag_Stop==1)   Encoder_Integral=0;      //===馬達關閉後清除積分
	  return Velocity;
}




/**************************************************************************
函數功能：轉向控制 修改轉向速度，請修改Turn_Amplitude即可
入口參數：左輪編碼器、右輪編碼器、Z軸陀螺儀
回傳值：轉向控制PWM
**************************************************************************/
int turn(int encoder_left,int encoder_right,float gyro)//转向控制
{
	 static float Turn_Target,Turn,Encoder_temp,Turn_Convert=0.9,Turn_Count;

	  //=============遙控左右旋轉部分=======================//
  	if(Turn_Amplitude!=0)                      //這一部分主要是根據旋轉前的速度調整速度的起始速度，增加小車的適應性
		{
			if(++Turn_Count==1)
			Encoder_temp=abs(encoder_left+encoder_right);
			Turn_Convert=50/Encoder_temp;
			if(Turn_Convert<0.6)Turn_Convert=0.6;
			if(Turn_Convert>3)Turn_Convert=3;
		}	
	  else
		{
			Turn_Convert=0.9;
			Turn_Count=0;
			Encoder_temp=0;
		}			
		if(Turn_Amplitude>0)	           Turn_Target-=Turn_Convert;
		else if(Turn_Amplitude<0)	     Turn_Target+=Turn_Convert; 
		else Turn_Target=0;
    if(Turn_Target>Turn_Amplitude)  Turn_Target=Turn_Amplitude;    //===转向速度限幅
	  if(Turn_Target<-Turn_Amplitude) Turn_Target=-Turn_Amplitude;
		if( Movement != 0 )  Kd=0.5;        
		else Kd=0;   //轉向的時候取消陀螺儀的修正 有點模糊PID的想法
  	//=============轉向PD控制器=======================//
		Turn=-Turn_Target*Kp -gyro*Kd;                 //===結合Z軸陀螺儀進行PD控制
	  return Turn;
}





/**************************************************************************
函數功能：輸出PWM給馬達
輸入參數：左輪PWM、右輪PWM
**************************************************************************/
void Set_Pwm(int motor_L,int motor_R)
{
    HAL_TIM_PWM_Stop(&htim1 ,TIM_CHANNEL_1);                    //停止 左輪 PWM
	HAL_TIM_PWM_Stop(&htim1 ,TIM_CHANNEL_2);                    //停止 右輪 PWM
    
    //----------- 限制 PWM 最大值 -------------------
    if(motor_L < -PWM_MAX) motor_L = -PWM_MAX;	
    if(motor_L >  PWM_MAX) motor_L =  PWM_MAX;	
	if(motor_R < -PWM_MAX) motor_R = -PWM_MAX;	
	if(motor_R >  PWM_MAX) motor_R =  PWM_MAX;
    
    
//-----------------------------車輪同步誤差處理--------------------------------------
    int ABS_Encoder_Left = abs(Encoder_Left),
        ABS_Encoder_Right = abs(Encoder_Right);
    
    if( ABS_Encoder_Left > ABS_Encoder_Right )
    {
        error_motor_L = (ABS_Encoder_Left - ABS_Encoder_Right) * 20;
    }
    if ( ABS_Encoder_Left > ABS_Encoder_Right )
    {
        error_motor_R = ( ABS_Encoder_Right-ABS_Encoder_Left ) * 20;
    }
    if(motor_L < 0) motor_L =  motor_L + error_motor_L;	
    if(motor_L > 0) motor_L =  motor_L - error_motor_L;	
	if(motor_R < 0) motor_R =  motor_R + error_motor_R;	
	if(motor_R > 0) motor_R =  motor_R - error_motor_R;


	
    
    
    if(motor_L<0)                                               //往後傾斜時，左輪往後走
    {
        HAL_GPIO_WritePin(GPIOC, GPIO_PIN_0, GPIO_PIN_RESET);
        HAL_GPIO_WritePin(GPIOC, GPIO_PIN_1, GPIO_PIN_SET);
    }
	else                                                        //往前傾斜時，左輪往後前
    {
        HAL_GPIO_WritePin(GPIOC, GPIO_PIN_0, GPIO_PIN_SET);
        HAL_GPIO_WritePin(GPIOC, GPIO_PIN_1, GPIO_PIN_RESET);
    }        
    sConfigOC.Pulse = abs(motor_L);			                        //設定 脈衝寬度
    HAL_TIM_PWM_ConfigChannel(&htim1, &sConfigOC, TIM_CHANNEL_1);	//將 左輪 PWM設定載入
    
	if(motor_R<0)
    {
        HAL_GPIO_WritePin(GPIOC, GPIO_PIN_2, GPIO_PIN_RESET);
        HAL_GPIO_WritePin(GPIOC, GPIO_PIN_3, GPIO_PIN_SET);
    }
    else
    {
        HAL_GPIO_WritePin(GPIOC, GPIO_PIN_2, GPIO_PIN_SET);
        HAL_GPIO_WritePin(GPIOC, GPIO_PIN_3, GPIO_PIN_RESET);
    }
	sConfigOC.Pulse = abs(motor_R);				                    //設定 脈衝寬度
    HAL_TIM_PWM_ConfigChannel(&htim1, &sConfigOC, TIM_CHANNEL_2);	//將 右輪 PWM設定載入
    
    HAL_TIM_PWM_Start(&htim1 ,TIM_CHANNEL_1);						//啟動 左輪 PWM
	HAL_TIM_PWM_Start(&htim1 ,TIM_CHANNEL_2);						//啟動 右輪 PWM
}



//-----------GPIO 中斷副程式 PIN_2(MPU6050 新資料中斷)    ，   PIN_13(開關)
void HAL_GPIO_EXTI_Callback(uint16_t GPIO_Pin)
{
	switch (GPIO_Pin)
	{
		case GPIO_PIN_2 :

                HAL_NVIC_DisableIRQ(EXTI2_3_IRQn);		//關閉 PD2 中斷
            
                MPU6050_GET_DATA();						//取得 MPU6050 資料
                
  //            printf ("Pitch=%f  \n\r " , Pitch );
                
                Angle_Balance=Pitch; 					//更新X軸平衡角
                Gyro_Balance=gyro[1];					//更新X軸平衡角速度
                Gyro_Turn=gyro[2];						//更新Z軸轉向角速度
                Acceleration_Z=accel_short[2];			//更新Z軸加速度
                
                Encoder_Left=-Read_Encoder( & htim2 ); 	//讀取 左輪編碼器 計數值
                Encoder_Right=Read_Encoder( & htim3 );  //讀取 右輪編碼器 計數值
                
                Balance_Pwm =balance(Angle_Balance,Gyro_Balance);   //車體平衡PID 控制
                Velocity_Pwm=velocity(Encoder_Left,Encoder_Right);  //車體速度PID 控制
                Turn_Pwm    =turn(Encoder_Left,Encoder_Right,Gyro_Turn);		//車體轉向PID 控制
                

                Motor_Left_PWM  = Balance_Pwm - Velocity_Pwm + Turn_Pwm;       //計算 左輪 PWM 值
                Motor_Right_PWM = Balance_Pwm - Velocity_Pwm - Turn_Pwm;       //計算 右輪 PWM 值
            
                Set_Pwm( Motor_Left_PWM , Motor_Right_PWM );        //輸出 PWM 給馬達
                
            
                HAL_NVIC_ClearPendingIRQ(EXTI2_3_IRQn);		// 清除 PD2 中斷暫存器
                HAL_NVIC_EnableIRQ(EXTI2_3_IRQn);			//開啟 PD2 中斷
//            }

			
			break;
            
	}
}