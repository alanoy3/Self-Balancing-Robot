#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <math.h>


#include "stm32f0xx_hal.h"
#include "stm32f0xx.h"
#include "i2c.h"
#include "tim.h"
#include "usart.h"
#include "gpio.h"

#include "MPU6050_set_Init.h"							//MPU6050初始化


TIM_OC_InitTypeDef 	   sConfigOC;						//建立計時器 PWM 設定 結構


uint8_t aRxBuffer1[1]={0};											//藍芽接收的資料變數


void self_balancing_robot_main_Init (void)
{
	HAL_NVIC_DisableIRQ(EXTI2_3_IRQn);					//關閉 PD2 中斷

	mpu_start();										// MPU6050初始化
    
    __HAL_UART_ENABLE_IT(&huart1,UART_IT_RXNE);         // 開啟UART1的中斷功能，設定接收到資料時產生中斷
	HAL_UART_Receive_DMA(&huart1,aRxBuffer1,1);					//開啟UART1的DMA 接收，設定變數矩陣 aRxBuffer1 接收資料，每次接收 1biy
	
	
	sConfigOC.OCMode 	 = TIM_OCMODE_PWM1;							//使用 PWM 1 模式
	sConfigOC.Pulse 	 = 0;									    //設定 脈衝寬度為 0
	sConfigOC.OCPolarity = TIM_OCPOLARITY_HIGH;						//脈衝電位為高電位
	sConfigOC.OCFastMode = TIM_OCFAST_DISABLE;						//FastMode 不啟用
	HAL_TIM_PWM_ConfigChannel(&htim1, &sConfigOC, TIM_CHANNEL_1);	//將 左輪 PWM設定載入
	HAL_TIM_PWM_ConfigChannel(&htim1, &sConfigOC, TIM_CHANNEL_2);	//將 右輪 PWM設定載入
	HAL_TIM_PWM_Start(&htim1 ,TIM_CHANNEL_1);						//啟動 左輪 PWM
	HAL_TIM_PWM_Start(&htim1 ,TIM_CHANNEL_2);						//啟動 右輪 PWM
	
	
	HAL_TIM_Encoder_Start (& htim2 , TIM_CHANNEL_1 );	//開啟 左輪 編碼器 A相
	HAL_TIM_Encoder_Start (& htim2 , TIM_CHANNEL_2 );	//開啟 左輪 編碼器 B相
	HAL_TIM_Encoder_Start (& htim3 , TIM_CHANNEL_1 );	//開啟 右輪 編碼器 A相
	HAL_TIM_Encoder_Start (& htim3 , TIM_CHANNEL_2 );	//開啟 右輪 編碼器 B相

    
	
	HAL_NVIC_ClearPendingIRQ(EXTI2_3_IRQn);		    // 清除 PD2 中斷暫存器
	HAL_NVIC_EnableIRQ(EXTI2_3_IRQn);				//開啟 PD2 中斷
    
    HAL_GPIO_WritePin(GPIOA, GPIO_PIN_5, GPIO_PIN_SET);     // 主機板 LED2 開啟
    
}