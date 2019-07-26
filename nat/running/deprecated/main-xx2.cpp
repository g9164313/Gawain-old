#include <sys/ipc.h>
#include <sys/msg.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <jni.h>
#include <iostream>
#include <algorithm>

using namespace std;
//--------------------------------//

#include <libusb.h>

int main1(int argc, char* argv[]) {

	int resp = libusb_init(NULL);

	/*libusb_device **devs;

	ssize_t cnt = libusb_get_device_list(NULL, &devs);

	libusb_device* dev = NULL;

	for(int i=0; devs[i]; ++i){

		struct libusb_device_descriptor desc;

		resp = libusb_get_device_descriptor(devs[i], &desc);

		if(desc.idVendor==0x8866 && desc.idProduct==0x6688){
			dev = devs[i];
			break;
		}
	}

	if(dev==NULL){
		libusb_free_device_list(devs, 1);
		libusb_exit(NULL);
		return -1;
	}

	cout<<"find device..."<<endl;*/

	libusb_device_handle* handle = libusb_open_device_with_vid_pid(NULL,0x8866,0x8866);

	libusb_device* dev = libusb_get_device(handle);

	//struct libusb_device_descriptor desc;
	//libusb_get_device_descriptor(dev, &desc);

	int num_conf, res;
	libusb_get_configuration(handle, &num_conf);

	libusb_config_descriptor* conf;

	libusb_get_active_config_descriptor(dev, &conf);

	const libusb_interface_descriptor* face = conf->interface[0].altsetting;

	resp = libusb_claim_interface(handle, face->bInterfaceNumber);

	const libusb_endpoint_descriptor* end0 = face->endpoint + 0;

	const libusb_endpoint_descriptor* end1 = face->endpoint + 1;

	uint8_t buff[500]={0};

	buff[0] = 0xAA;//HEAD

	buff[1] = 0x00;

	buff[2] = 0x00;//package length
	buff[3] = 0x08;

	buff[4] = 0x01;//command

	buff[5] = 0x00;//CRC
	buff[6] = 0xA3;

	buff[7] = 0x7E;//END

	libusb_bulk_transfer(handle,
		end0->bEndpointAddress,
		buff, 8,
		&resp, 0
	);

	libusb_bulk_transfer(handle,
		end1->bEndpointAddress,
		buff, 500,
		&resp, 500
	);

	buff[resp] = 0;

	resp = libusb_release_interface(handle, face->bInterfaceNumber);

	libusb_close(handle);
	libusb_exit(NULL);
	return 0;
}
//--------------------------------//

#include <modbus.h>

int main(int argc, char* argv[]) {

	modbus_t *ctx;
	uint16_t tab_reg[64];
	int rc;
	int i;

	//ctx = modbus_new_rtu("/dev/ttyUSB0",9600,'N',8,1);
	ctx = modbus_new_tcp("172.16.2.144", MODBUS_TCP_DEFAULT_PORT);

	if(modbus_connect(ctx) == -1){
		fprintf(stderr, "Connection failed: %s\n", modbus_strerror(errno));
		modbus_free(ctx);
		return -1;
	}

	timeval timeout;
	modbus_get_response_timeout(ctx, &timeout);
	timeout.tv_sec = 1;
	timeout.tv_usec= 0;
	modbus_set_response_timeout(ctx, &timeout);


	modbus_set_debug(ctx,ON);

	//uint8_t id=0xFF;
	//modbus_report_slave_id(ctx,&id);
	modbus_set_slave(ctx,1);

	uint16_t val[] = { 0x01, 0x00FF };
	rc = modbus_write_registers(ctx, 8001, 1, val);

	//rc = modbus_write_register(ctx, 8001,0x04);

	//uint8_t bits[] = {ON,ON,ON};
	//rc = modbus_write_register(ctx, 8001, 1);

	//uint16_t src[1]={0x03};
	//uint16_t dst[1]={0x04};
	//modbus_write_and_read_registers(
	//	ctx,
	//	8001, 1, src,
	//	8001, 1, dst
	//);

	//fprintf(stderr, "write failed: %s\n", modbus_strerror(errno));

	/*rc = modbus_read_input_registers(ctx,100,1,tab_reg);
	if (rc == -1) {
		fprintf(stderr, "%s\n",modbus_strerror(errno));
		return -1;
	}
	for (i = 0; i < rc; i++) {
		printf("reg[%d]=%d (0x%X)\n", i, tab_reg[i], tab_reg[i]);
	}*/

	modbus_close(ctx);
	modbus_free(ctx);
	return 0;
}
