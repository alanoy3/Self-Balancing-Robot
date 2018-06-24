/* 
	提示：printf和scanf都是用輪詢方式，沒有使用中斷。調用scanf在串口助手中輸入數據時，必須以空格結束，然後點擊發送，否則無法完成發送。
 */
#include "stm32f0xx_hal.h"

extern UART_HandleTypeDef huart2;


/* 
	移植printf()函數，重定向C庫函數printf到USART

  * @brief Sends an amount of data in blocking mode.

  * @param huart: Pointer to a UART_HandleTypeDefstructure that contains

  * the configuration information for thespecified UART module.

  * @param pData: Pointer to data buffer

  * @param Size: Amount of data to be sent

  * @param Timeout: Timeout duration 

  * @retval HAL status
	
  * 	HAL_StatusTypeDef HAL_UART_Transmit(UART_HandleTypeDef *huart,uint8_t *pData, uint16_t Size, uint32_t Timeout)
 */

int fputc(int ch, FILE *f)
{
	HAL_UART_Transmit(&huart2, (uint8_t *)&ch,1, 0xFFFF);

	return ch;
}



/*
 	移植scanf()函數，重定向C庫函數scanf到USART

  * @brief Receives an amount of data in blocking mode.

  * @param huart: Pointer to a UART_HandleTypeDefstructure that contains

  * the configuration informationfor the specified UART module.

  * @param pData: Pointer to data buffer

  * @param Size: Amount of data to be received

  * @param Timeout: Timeout duration

  * @retval HAL status

  * HAL_StatusTypeDef HAL_UART_Receive(UART_HandleTypeDef *huart,uint8_t *pData, uint16_t Size, uint32_t Timeout)
 
 */
  
int fgetc(FILE *f)
{
	uint8_t ch;

	HAL_UART_Receive(&huart2,(uint8_t *)&ch, 1, 0xFFFF);
	
	return ch;
}