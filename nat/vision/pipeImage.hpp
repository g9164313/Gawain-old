/*
 * pipe_data.hpp
 *
 *  Created on: 2019年5月2日
 *      Author: qq
 */

#ifndef VISION_PIPEIMAGE_HPP_
#define VISION_PIPEIMAGE_HPP_

#include <sys/types.h>
#include <sys/ipc.h>
#include <sys/msg.h>
#include <sys/stat.h>
#include <sys/mman.h>
#include <sys/shm.h>

#define MSG_COMMIT (0x55AA)
#define MSG_SHAKE1 (0x8001)
#define MSG_SHAKE2 (0x8002)
#define MSG_LENGTH 1024
struct MSG_PACK {
	long type;
	uint8_t data[MSG_LENGTH];
};

struct PIPE_KEY {
	int pid;
	int mid;
};

#endif /* VISION_PIPEIMAGE_HPP_ */
