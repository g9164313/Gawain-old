
DIR_JNI=/home/qq/.local/jdk8/include

TARGET_EXE = gawain

TARGET_PART= libdevice.so

#### Pleora eBus ####

ROOT_EBUS:=/home/lib/eBus

INC_EBUS := -I$(ROOT_EBUS)/include -I/usr/include/modbus \
	-D_UNIX_ -D_LINUX_ -DPV_GUI_NOT_AVAILABLE \
	-DPV_NO_GEV1X_PIXEL_TYPES -DPV_NO_DEPRECATED_PIXEL_TYPES

LIB_EBUS := -L$(ROOT_EBUS)/lib \
	-lPvBase \
	-lPvDevice \
	-lPvBuffer \
	-lPvGUIUtils \
	-lPvPersistence \
	-lPvGenICam \
	-lPvStreamRaw \
	-lPvStream \
	-lPvTransmitterRaw \
	-lPvVirtualDevice

#### FlyCapture ####

INC_FLYCAPTURE:=-I/usr/include/flycapture

LIB_FLYCAPTURE:=-L/usr/lib -lflycapture -lflycapturegui

#### XIMEA ####

ROOT_XIMEA:=/opt/XIMEA

INC_XIMEA:=-I$(ROOT_XIMEA)/include

LIB_XIMEA:=-I$(ROOT_XIMEA)/lib -lm3api

#### Basler Pylon ####

ROOT_PYLON :=  /home/lib/pylon
ROOT_GENICAM := $(ROOT_PYLON)/genicam

INC_PYLON := -I$(ROOT_PYLON)/include \
	-I$(ROOT_GENICAM)/library/CPP/include \
	-DUSE_GIGE 

LIB_PYLON :=  -L$(ROOT_PYLON)/lib64 \
	-L$(ROOT_GENICAM)/bin/Linux64_x64 \
	-L$(ROOT_GENICAM)/bin/Linux64_x64/GenApi/Generic \
	-L$(ROOT_PYLON)/lib32 \
	-L$(ROOT_GENICAM)/bin/Linux32_i86 \
	-L$(ROOT_GENICAM)/bin/Linux32_i86/GenApi/Generic \
	-lpylonbase -lpylonutility \
	-lGenApi_gcc40_v2_3 -lGCBase_gcc40_v2_3 -lLog_gcc40_v2_3 -lMathParser_gcc40_v2_3 -lXerces-C_gcc40_v2_7_1 -llog4cpp_gcc40_v2_3 \
	-Wl,-E


#########################
####  configuration  ####
#########################

CC = g++ -DENABLE_LOG -O0

CC_FLAG = -g -I. -I$(DIR_JNI) -I$(DIR_JNI)/linux
CC_FLAG += -DVISION $(shell pkg-config --cflags opencv) 
CC_FLAG += $(shell pkg-config --cflags libusb-1.0) 
CC_FLAG += $(shell pkg-config --cflags libmodbus)

LD_LIBS = $(shell pkg-config --libs opencv)
LD_LIBS += $(shell pkg-config --libs libusb-1.0) 
LD_LIBS += $(shell pkg-config --libs libmodbus)

########################

CORE_SO = libspringboard.so
CORE_FILE = util_miscellany.cpp wrap_PanPuppetter.cpp

VISION_SO = libvision.so
VISION_FILE =  util_jni.cpp util_cv.cpp ./vision/CamBundle.cpp ./vision/CamDummy.cpp 
VISION_FILE += ./vision/CamVidcap.cpp 
#VISION_FILE += ./vision/CamMulticam.cpp 
#VISION_FILE += ./vision/CamFlyCapture.cpp
#VISION_FILE += ./vision/CamXIMEA.cpp

#IMG_FILTER =
LIB_SFRISO = ./SFR/FltrSlangEdge.cpp ./SFR/sfr_iso.c 

########################

OBJECTS = $(SOURCES:.cpp=.o)

CHECK_EXEC := $(word 2,$(shell pgrep -f "narl.itrc.Gawain"))

$(CORE_SO): $(CORE_FILE)
	$(CC) -fPIC -shared -o $@ $^ $(LD_LIBS) $(CC_FLAG)
	cp -rf $@ ../bin/narl/itrc/nat
	cp -rf $@ ../src/narl/itrc/nat
ifeq ($(CHECK_EXEC),)
	@echo "no process is running, so we move it~~"
	cp -rf $@ ~/.gawain/$@
else
	@echo "process is running!!!"
endif

####################################################
$(VISION_SO): $(VISION_FILE) 
	$(CC) -fPIC -shared -o $@ $^ $(LD_LIBS) $(CC_FLAG)
	cp -rf $@ ../bin/narl/itrc/nat
	cp -rf $@ ../src/narl/itrc/nat
ifeq ($(CHECK_EXEC),)
	@echo "no process is running, so we move it~~"
	cp -rf $@ ~/.gawain/$@
else
	@echo "process is running!!!"
endif

####################################################
libdevice.so: ./deprecated/util_jni.cpp ./device/DevTTY.cpp ./device/DevModbus.cpp ./device/DevUSB.cpp
	$(CC) -fPIC -shared -o $@ $^ $(LD_LIBS) $(CC_FLAG)
	cp -rf $@ ../bin/narl/itrc/nat
	cp -rf $@ ../src/narl/itrc/nat
ifeq ($(CHECK_EXEC),)
	@echo "no process is running, so we rewrite it~~"
	cp -rf $@ ~/.gawain/$@
else
	@echo "process is running!!!"
endif

####################################################

$(TARGET_EXE):  ./test/misc.cpp ./vision/util_cv.cpp ./test/main-xx4.cpp 
	$(CC) -o $@ $^ $(CC_FLAG) $(LD_LIBS)
	
####################################################


all: $(TARGET_EXE) libdevice.so

clean:
	rm -rf $(TARGET_EXE) $(TARGET_SO) $(DIR_SRC)/*.o ./*.o
	
.PHONY: all clean prepare libdevice.so $(TARGET_EXE) $(CORE_SO) $(VISION_SO)


