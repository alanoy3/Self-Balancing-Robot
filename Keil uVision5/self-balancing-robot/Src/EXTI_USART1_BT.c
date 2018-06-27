#include "stm32f0xx_hal.h"
#include "stm32f0xx.h"

#define Target_Velocity 100;											//前進、後退速度
#define Turn_Velocity 	 40;											//轉向速度
extern uint8_t aRxBuffer1[1];											//藍芽接收的資料變數，從 self-balancing-robot_main_Init.c 來
volatile double Movement=0,												//前進、後退 變數
							 Turn_Amplitude=0;									//轉向 變數


void HAL_UART_RxCpltCallback(UART_HandleTypeDef *huart)
{
    if (huart->Instance == USART1)
    {
        HAL_NVIC_DisableIRQ(USART1_IRQn);		//關閉 USART1 中斷
        printf("%s",aRxBuffer1);
        switch(*aRxBuffer1)
            {
					
                case 'F':
				case 'f':
                    Movement = Target_Velocity;
					break;
					
					
				case 'B':
				case 'b':
					Movement = -Target_Velocity;
					break;
					

				case 'L':
				case 'l':
					Turn_Amplitude = Turn_Velocity;
					break;
					
					
				case 'R':
				case 'r':
					Turn_Amplitude = -Turn_Velocity;
					break;
					

				case 'S':
				case 's':
					Movement = 0;
					Turn_Amplitude = 0;
					break;
					
					
				default:
					Movement = 0;
					Turn_Amplitude = 0;
					break;
			}
        HAL_NVIC_ClearPendingIRQ(USART1_IRQn);		// 清除 USART1 中斷暫存器
        HAL_NVIC_EnableIRQ(USART1_IRQn);			//開啟 USART1 中斷
    }
}