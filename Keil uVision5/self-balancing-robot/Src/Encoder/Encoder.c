#include "stm32f0xx_hal.h"
#include "stm32f0xx.h"

/*-----------------------------------------------------------------------------
			讀取 編碼器 
			輸入 TIMER 結構
-----------------------------------------------------------------------------*/
int Read_Encoder (TIM_HandleTypeDef *htim)
{
	long enc1 =__HAL_TIM_GET_COUNTER (htim );		//取得 編碼器 計數值
	
	__HAL_TIM_SET_COUNTER( htim , 0);					//編碼器 計數值 歸0

	if( enc1>(UINT16_MAX/2) )							//當 反向時
	{
		enc1= enc1-UINT16_MAX;							//計數值 - 2的16次方
        return (int)enc1;
	}
	else
    {
        return (int) enc1;
    }
}