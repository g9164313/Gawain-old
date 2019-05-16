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
	int pid, mid;
};

#define PIPE_PREP(nodeName) \
	key_t kid = ftok(nodeName,1); \
	int pid = msgget(kid, IPC_CREAT|0666); \
	MSG_PACK pack; \
	while(false)

#define PACK_MSG_DATA \
	uint32_t* _dat_ = (uint32_t*)(pack.data); \
	_dat_[0] = width; \
	_dat_[1] = height; \
	_dat_[2] = cvtype; \
	_dat_[3] = snap; \
	_dat_[4] = lenPool; \
	_dat_[5] = lenOver; \
	memcpy(_dat_+6, mark, sizeof(uint32_t)*4*8); \
	while(false)

#define UNPACK_MSG_DATA \
	uint32_t* _dat_ = (uint32_t*)(pack.data); \
	int width  = _dat_[0]; \
	int height = _dat_[1]; \
	int cvtype = _dat_[2]; \
	int snap   = _dat_[3]; \
	int lenPool= _dat_[4]; \
	int lenOver= _dat_[5]; \
	Rect[] mark = { \
		Rect(_dat_[ 6], _dat_[ 7], _dat_[ 8], _dat_[ 9]), \
		Rect(_dat_[11], _dat_[12], _dat_[13], _dat_[14]), \
		Rect(_dat_[15], _dat_[16], _dat_[17], _dat_[18]), \
		Rect(_dat_[19], _dat_[20], _dat_[21], _dat_[22]), \
		Rect(_dat_[23], _dat_[24], _dat_[25], _dat_[26]), \
		Rect(_dat_[27], _dat_[28], _dat_[29], _dat_[30]), \
		Rect(_dat_[31], _dat_[32], _dat_[33], _dat_[34]), \
		Rect(_dat_[35], _dat_[36], _dat_[37], _dat_[38]), \
	}; while(false)

#define PIPE_OPEN \
	pack.type = MSG_COMMIT; \
	msgsnd(pid, &pack, 0, 0); \
	msgrcv(pid, &pack, MSG_LENGTH, MSG_SHAKE1, 0); \
	UNPACK_MSG_DATA \
	int mid = shmget(kid, lenPool+lenOver, IPC_CREAT|0666); \
	uint8_t* smem = (uint8_t*)shmat(mid, NULL, 0); \
	uint8_t* pool = smem + 0; \
	uint8_t* over = smem + lenPool; \
	while(false)

#define PIPE_CLOSE \
	shmdt(smem); \
	pack.type = MSG_SHAKE2; \
	msgsnd(pid, &pack, MSG_LENGTH, 0); \
	while(false)

#endif /* VISION_PIPEIMAGE_HPP_ */
