#include <stdio.h>
#include <string.h>
#include "stm32f0xx.h"          // File name depends on device used
#include  "MPU6050.h"
#include "I2Cdev.h"
#include "Driver_I2C.h"

/* I2C Driver */
extern ARM_DRIVER_I2C Driver_I2C0;
static ARM_DRIVER_I2C * I2Cdrv = &Driver_I2C0;