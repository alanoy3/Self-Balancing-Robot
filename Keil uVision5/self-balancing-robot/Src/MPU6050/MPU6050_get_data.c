#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <math.h>

#include "stm32f0xx_hal.h"
#include "stm32f0xx.h"
#include "override_printf_&_scanf.h"		/* printf & scanf 複寫 */

//	MPU6050
#include "inv_mpu_dmp_motion_driver.h"
#include "inv_mpu.h"




short 				 gyro[3],					//陀螺儀角速度資料
					 accel_short[3];			//加速計資料
long 			     quat[4] = {0};				//四元數
unsigned long  		 sensor_timestamp;			//擷取時間(無用)
short 				 sensors;					//從FIFO讀取資料的遮罩值
unsigned char  		 more;						//MPU6050剩下的資料封包量
double 				 q0=1.0f,					//四元數(以除以2的30次方值)
					 q1=0.0f,
					 q2=0.0f,
					 q3=0.0f; 	
double 				 Pitch; 					//俯仰角(X軸角度)
long 				 temperature;				//原始溫度值(需除以100000.0)



/*-------------------------------------------------------------------
//					取得  MPU6050 資料 四元數俯仰角(X軸角度)
-------------------------------------------------------------------*/

void MPU6050_GET_DATA (void)
{
	dmp_read_fifo(gyro,								//取得MPU6050 DMP FIFO 資料
				  accel_short,
				  quat,
				  &sensor_timestamp,
				  &sensors,
				  &more);
	
	//mpu_reset_fifo();									//清除 MPU6050 FIFO 資料
	
	q0=quat[0] / 1073741824.0;
	q1=quat[1] / 1073741824.0;
	q2=quat[2] / 1073741824.0;
	q3=quat[3] / 1073741824.0;
	
	Pitch = asin(-2 * q1 * q3 + 2 * q0* q2)* 57.29577951;
}


/*------------------------------------------------------------
//				取得 溫度值
------------------------------------------------------------*/
void MPU6050_GET_Temperature (void)
{
	mpu_get_temperature(&temperature, &sensor_timestamp);
}

