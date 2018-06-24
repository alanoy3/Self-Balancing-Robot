
#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>

#include "inv_mpu.h"
#include "inv_mpu_dmp_motion_driver.h"
#include "invensense.h"
#include "invensense_adv.h"
#include "eMPL_outputs.h"
#include "mltypes.h"
#include "mpu.h"


#define MPU_OK        		0
#define MPU_ERR      	   -1
#define DMP_Interrupts		1									//設定 DMP 中斷(1 設定 一組資料由DMP到FIFO )
#define DEFAULT_MPU_HZ  (100-1)									//設定MPU6050採樣率 單位hz
#define lpf 			  20									//低通濾波 ，The following LPF settings are supported: 188, 98, 42, 20, 10, 5.
#define set_gyro_fsr 	 2000									//設定陀螺儀量測每秒最大度數 (2000 , 1000 , 500 , 250)
#define set_accel_fsr 		2									//設定加速計量測最大重力值 (2, 4 , 8 , 16)

unsigned char *mpl_key = (unsigned char*)"eMPL 6.12";		//MPL 使用版本

unsigned short gyro_rate = NULL,							//取得目前MPU6050採樣率
							 gyro_fsr;											//取得目前陀螺儀每秒最大度數
unsigned char accel_fsr;											//取得目前加速計最大值

/* Platform-specific information. Kinda like a boardfile. */
struct platform_data_s {
    signed char orientation[9];
};


/* The sensors can be mounted onto the board in any orientation. The mounting
 * matrix seen below tells the MPL how to rotate the raw data from the
 * driver(s).
 * TODO: The following matrices refer to the configuration on internal test
 * boards at Invensense. If needed, please modify the matrices to match the
 * chip-to-body matrix for your particular set up.
 */
static struct platform_data_s gyro_pdata = {
    .orientation = { 0, -1, 0,
                     -1, 0, 0,
                     0, 0, -1}
};

static inline void run_self_test(void)
{
    int result;
    long gyro[3], accel[3];

#if defined (MPU6500) || defined (MPU9250)
    result = mpu_run_6500_self_test(gyro, accel, 0);
#elif defined (MPU6050) || defined (MPU9150)
    result = mpu_run_self_test(gyro, accel);
#endif
    if (result == 0x7) {
	printf("Passed!\n\r ");
        printf("accel: %7.4f %7.4f %7.4f\n\r ",
                    accel[0]/65536.f,
                    accel[1]/65536.f,
                    accel[2]/65536.f);
        printf("gyro: %7.4f %7.4f %7.4f\n\r ",
                    gyro[0]/65536.f,
                    gyro[1]/65536.f,
                    gyro[2]/65536.f);
        /* Test passed. We can trust the gyro data here, so now we need to update calibrated data*/

#ifdef USE_CAL_HW_REGISTERS
        /*
         * This portion of the code uses the HW offset registers that are in the MPUxxxx devices
         * instead of pushing the cal data to the MPL software library
         */
        unsigned char i = 0;

        for(i = 0; i<3; i++) {
        	gyro[i] = (long)(gyro[i] * 32.8f); //convert to +-1000dps
        	accel[i] *= 2048.f; //convert to +-16G
        	accel[i] = accel[i] >> 16;
        	gyro[i] = (long)(gyro[i] >> 16);
        }

        mpu_set_gyro_bias_reg(gyro);

#if defined (MPU6500) || defined (MPU9250)
        mpu_set_accel_bias_6500_reg(accel);
#elif defined (MPU6050) || defined (MPU9150)
        mpu_set_accel_bias_6050_reg(accel);
#endif
#else
        /* Push the calibrated data to the MPL library.
         *
         * MPL expects biases in hardware units << 16, but self test returns
		 * biases in g's << 16.
		 */
    	unsigned short accel_sens;
    	float gyro_sens;

		mpu_get_accel_sens(&accel_sens);
		accel[0] *= accel_sens;
		accel[1] *= accel_sens;
		accel[2] *= accel_sens;
		inv_set_accel_bias(accel, 3);
		mpu_get_gyro_sens(&gyro_sens);
		gyro[0] = (long) (gyro[0] * gyro_sens);
		gyro[1] = (long) (gyro[1] * gyro_sens);
		gyro[2] = (long) (gyro[2] * gyro_sens);
		inv_set_gyro_bias(gyro, 3);
#endif
    }
    else {
            if (!(result & 0x1))
                printf("Gyro failed.\n\r ");
            if (!(result & 0x2))
                printf("Accel failed.\n\r ");
            if (!(result & 0x4))
                printf("Compass failed.\n\r ");
     }

}


/* ****************************************************************************************
* MPU6050 DMP的初始化
* @brief  start mpu6050 dmp
* @description
*  This function is used to initialize mpu6050 dmp.
***************************************************************************************** */

void mpu_start(void)
{
    int result = mpu_init(NULL);				//MPU6050 初始化
        while(result != MPU_OK)
        {
           result = mpu_init(NULL);
        }
    if(result == MPU_OK)
    {
			 printf("MPU6050 初始化完成......\n\r  ");   
			 result = 1;
			while( result != MPU_OK )
			{
				if(mpu_set_sensors(INV_XYZ_GYRO | INV_XYZ_ACCEL) == MPU_OK)							//mpu_set_sensor
				{
            printf("MPU6050 感測器 陀螺儀 與 加速計 設定 完成 ......\n\r ");
						result = MPU_OK;
				}
        else
				{
            printf("MPU6050 感測器 陀螺儀 與 加速計 設定 錯誤 ......\n\r ");
						result = MPU_ERR;
				}
				
        if(mpu_configure_fifo(INV_XYZ_GYRO | INV_XYZ_ACCEL) == MPU_OK)                     //mpu_configure_fifo
				{
            printf("MPU6050 將陀螺儀 與 加速計 資料給 FIFO 設定 完成 ......\n\r ");
						//result = MPU_OK;
				}
        else
				{
            printf("MPU6050 將陀螺儀 與 加速計 資料給 FIFO 設定 錯誤 ......\n\r ");
						result = MPU_ERR;
				}

        if(mpu_set_sample_rate(DEFAULT_MPU_HZ) == MPU_OK)             //設定採樣率
				{
					inv_set_gyro_sample_rate(1000000L / DEFAULT_MPU_HZ);				//設定陀螺儀採樣率，以us為單位
					inv_set_accel_sample_rate(1000000L / DEFAULT_MPU_HZ);				//設定加速計採樣率，以us為單位
          printf("MPU6050 採樣率 設定 完成 ......\n\r ");
					//result = MPU_OK;
				}
        else
				{
            printf("MPU6050 採樣率 設定 錯誤 ......\n\r ");
						result = MPU_ERR;
				}
				
				if(mpu_get_sample_rate(&gyro_rate) == MPU_OK)											//取得目前MPU6050採樣率
				{
						printf("目前MPU6050採樣率 = ");
						printf("%d Hz\n\r ",gyro_rate);
						//result = MPU_OK;
				}
				else
				{
						printf("無法取得目前MPU6050採樣率\n\r ");
						result = MPU_ERR;
				}
/*
				if(mpu_set_lpf (lpf)== MPU_OK)											//設定低通濾波
				{
						printf("設定 低通濾波 完成 \n\r ");
						//result = MPU_OK;
				}
				else
				{
						printf("設定 低通濾波 錯誤 \n\r ");
						result = MPU_ERR;
				}
*/
				if(mpu_set_gyro_fsr(set_gyro_fsr) == MPU_OK)                     //設定陀螺儀每秒最大度數
				{
            printf("陀螺儀每秒最大度數 設定 完成 ......\n\r ");
						//result = MPU_OK;
				}
        else
				{
            printf("陀螺儀每秒最大度數 設定 錯誤 ......\n\r ");
						result = MPU_ERR;
				}
				
				
				if(mpu_set_accel_fsr(set_accel_fsr) == MPU_OK)                     //設定加速計量測最大重力值
				{
            printf("設定加速計量測最大重力值 設定 完成 ......\n\r ");
						//result = MPU_OK;
				}
        else
				{
            printf("設定加速計量測最大重力值 設定 錯誤 ......\n\r ");
						result = MPU_ERR;
				}

				if(mpu_get_gyro_fsr(&gyro_fsr) == MPU_OK)													//取得目前陀螺儀每秒最大度數
				{
						printf("目陀螺儀每秒最大度數 = %d 度 \n\r ",gyro_fsr);
						//result = MPU_OK;
				}
				else
				{
						printf("無法取得目陀螺儀每秒最大度數 \n\r ");
						result = MPU_ERR;
				}
				
				if(mpu_get_accel_fsr(&accel_fsr) == MPU_OK)											//取得目前加速計量測最大重力值
				{
						printf("目前加速計量測最大重力值 = %d G \n\r ",accel_fsr);
						//result = MPU_OK;
				}
				else
				{
						printf("無法取得目前加速計量測最大重力值  \n\r ");
						result = MPU_ERR;
				}
				
				inv_set_gyro_orientation_and_scale(														//設定陀螺儀方向與靈敏度
            inv_orientation_matrix_to_scalar(gyro_pdata.orientation),
            (long)gyro_fsr<<15);
				
				inv_set_accel_orientation_and_scale(													//設定加速計方向與靈敏度
            inv_orientation_matrix_to_scalar(gyro_pdata.orientation),
            (long)accel_fsr<<15);

				if(dmp_set_interrupt_mode(DMP_Interrupts) == MPU_OK)                 //設定 DMP 中斷
				{
            printf("MPU6050 設定 DMP 中斷腳位 完成 ......\n\r ");
						//result = MPU_OK;
				}
        else
				{
            printf("MPU6050 設定 DMP 中斷腳位 錯誤 ......\n\r ");
						result = MPU_ERR;
				}


				
				
				
				/* ---------------------------------DMP-------------------------------------------- */
				
        if(dmp_load_motion_driver_firmware() == MPU_OK)             //dmp_load_motion_driver_firmvare
				{
            printf("載入 DMP 運動驅動韌體 完成 ......\n\r ");
						//result = MPU_OK;
				}
        else
				{
            printf("載入 DMP 運動驅動韌體 錯誤 ......\n\r ");
						result = MPU_ERR;
				}
				
        if(dmp_set_orientation(inv_orientation_matrix_to_scalar(gyro_pdata.orientation)) == MPU_OK)           //dmp_set_orientation
				{
            printf("設定 DMP 各軸方向 完成 ......\n\r ");
						//result = MPU_OK;
				}
        else
				{
            printf("設定 DMP 各軸方向 錯誤 ......\n\r ");
						result = MPU_ERR;
				}
				
        if(dmp_enable_feature(DMP_FEATURE_6X_LP_QUAT | 								//dmp_enable_feature
															DMP_FEATURE_TAP |
                              DMP_FEATURE_ANDROID_ORIENT | 
															DMP_FEATURE_SEND_RAW_ACCEL | 
															DMP_FEATURE_SEND_CAL_GYRO |
                              DMP_FEATURE_GYRO_CAL) == MPU_OK)      
				{				
            printf("啟用 DMP 功能 完成 ......\n\r ");
						//result = MPU_OK;
				}
        else
				{
            printf("啟用 DMP 功能 錯誤 ......\n\r ");
						result = MPU_ERR;
				}
				
				if(dmp_enable_6x_lp_quat(1) == MPU_OK)             //dmp_set_fifo_rate
				{
            printf("啟用 6軸 低通濾波 合成 四位元數 完成 ......\n\r ");
						//result = MPU_OK;
				}
        else
				{
            printf("啟用 6軸 低通濾波 合成 四位元數 錯誤 ......\n\r ");
						result = MPU_ERR;
				}
				
				
        if(dmp_set_fifo_rate(DEFAULT_MPU_HZ) == MPU_OK)             //dmp_set_fifo_rate
				{
            printf("設定 DMP FIFO 採樣率 完成 ......\n\r ");
						//result = MPU_OK;
				}
        else
				{
            printf("設定 DMP FIFO 採樣率 錯誤 ......\n\r ");
						result = MPU_ERR;
				}
				
        run_self_test();
				
        if(mpu_set_dmp_state(1) == MPU_OK)
				{
            printf("DMP 設定 狀態 完成 ......\n\r ");
						//result = MPU_OK;
				}
        else
				{
            printf("DMP 設定 狀態 錯誤 ......\n\r ");
						result = MPU_ERR;
				}
				
				
				/* -------------------------MPL------------------------------- */
/*
				if(inv_init_mpl() == MPU_OK)             
				{
            printf("MPL 初始化 完成 ......\n\r ");
						//result = MPU_OK;
				}
        else
				{
            printf("MPL 初始化 錯誤 ......\n\r ");
						result = MPU_ERR;
				}
				
				if(inv_enable_quaternion() == MPU_OK)             
				{
            printf("啟用 MPL 6軸 四元數 完成 ......\n\r ");
						//result = MPU_OK;
				}
        else
				{
            printf("啟用 MPL 6軸 四元數 錯誤 ......\n\r ");
						result = MPU_ERR;
				}

				if(inv_enable_gyro_tc() == MPU_OK)             
				{
            printf("啟用 MPL 溫度補償 完成 ......\n\r ");
						//result = MPU_OK;
				}
        else
				{
            printf("啟用 MPL 溫度補償 錯誤 ......\n\r ");
						result = MPU_ERR;
				}

				if(inv_enable_eMPL_outputs() == MPU_OK)             
				{
            printf("啟用 MPL 資料輸出 完成 ......\n\r ");
						//result = MPU_OK;
				}
        else
				{
            printf("啟用 MPL 資料輸出 錯誤 ......\n\r ");
						result = MPU_ERR;
				}

				if(inv_start_mpl() == MPU_OK)             
				{
            printf("MPL 啟用 完成 ......\n\r ");
						//result = MPU_OK;
				}
        else
				{
            printf("MPL 啟用 錯誤 ......\n\r ");
						result = MPU_ERR;
				}
*/
        }
   }
}