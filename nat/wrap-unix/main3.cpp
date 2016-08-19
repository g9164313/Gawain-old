#include <sys/ipc.h>
#include <sys/msg.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <errno.h>
#include <global.hpp>
#include <algorithm>

#include <modbus.h>

int main(int argc, char* argv[]) {

	modbus_t *ctx;
	uint16_t tab_reg[64];
	int rc;
	int i;

	ctx = modbus_new_rtu("/dev/ttyUSB0",9600,'N',8,1);
	if (modbus_connect(ctx) == -1) {
		fprintf(stderr, "Connection failed: %s\n", modbus_strerror(errno));
		modbus_free(ctx);
		return -1;
	}

	modbus_set_slave(ctx,1);

	rc = modbus_read_input_registers(ctx,100,1,tab_reg);
	if (rc == -1) {
		fprintf(stderr, "%s\n",modbus_strerror(errno));
		return -1;
	}

	for (i = 0; i < rc; i++) {
		printf("reg[%d]=%d (0x%X)\n", i, tab_reg[i], tab_reg[i]);
	}

	modbus_close(ctx);
	modbus_free(ctx);
	return 0;
}
