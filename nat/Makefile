
DIR_JNI=/home/qq/.local/jdk8/include

TARGET_EXE=gawain

TARGET_SO=libspringboard.so
	
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

#### Euresys Multicam ####

ROOT_MULTICAM := /opt/euresys/multicam

INC_MULTICAM := -I$(ROOT_MULTICAM)/include

LIB_MULTICAM := -L$(ROOT_MULTICAM) -lMultiCam 

#########################
####  configuration  ####
#########################

CC=g++ 

CC_FLAG = -g -I. -I./vision -I./SFR -I$(DIR_JNI) -I$(DIR_JNI)/linux
CC_FLAG += -DVISION $(shell pkg-config --cflags opencv) 
#CC_FLAG += -march=native ???
#CC_FLAG += $(shell pkg-config --cflags libmodbus) 
#CC_FLAG += $(shell pkg-config --cflags qt5) 
#CC_FLAG += $(INC_EBUS)
CC_FLAG += $(INC_FLYCAPTURE)
CC_FLAG += $(INC_XIMEA)
#CC_FLAG += $(INC_PYLON)
#CC_FLAG += $(INC_MULTICAM)

LD_LIBS = $(shell pkg-config --libs opencv)  -lmodbus
#LD_LIBS += $(shell pkg-config --libs libmodbus) 
#LD_LIBS += $(shell pkg-config --libs Qt5Core Qt5Gui Qt5Widgets Qt5Test)
#LD_LIBS += $(LIB_EBUS)
LD_LIBS += $(LIB_FLYCAPTURE)
LD_LIBS += $(LIB_XIMEA)
#LD_LIBS += $(LIB_PYLON)
#LD_LIBS += $(LIB_MULTICAM)

########################

#CORE_FILE = util_jni.cpp DevTTY.cpp
CORE_FILE = util_jni.cpp DevTTY.cpp 

VISION_FILE =  ./vision/CamBundle.cpp util_cv.cpp registration.cpp

#IMG_FILTER =
LIB_SFRISO = ./SFR/FltrSlangEdge.cpp ./SFR/sfr_iso.c 

#DEV_CAMERA = ./vision/CamVidcap.cpp 
#DEV_CAMERA += ./vision/CamMulticam.cpp 
#DEV_CAMERA += ./vision/CamFlyCapture.cpp
#DEV_CAMERA += ./vision/CamXIMEA.cpp

#PRJ_CUSTOM = PrjLetterpress.cpp
########################

OBJECTS = $(SOURCES:.cpp=.o)

CHECK_EXEC := $(word 2,$(shell pgrep -f "narl.itrc.Gawain"))

$(TARGET_SO): $(CORE_FILE)
	$(CC) -fPIC -shared -o $@ $^ $(LD_LIBS) $(CC_FLAG)
	cp -rf $@ ../bin/narl/itrc/nat
	cp -rf $@ ../src/narl/itrc/nat
	#cp -rf $@ /home/qq/.local/bin/gawain
ifeq ($(CHECK_EXEC),)
	@echo "no process, so we ovewrite it~~"
	cp -rf $@ ~/.gawain/$@
else
	@echo "process is running!!!"
endif
	

$(TARGET_EXE):  ./running/main2.cpp ./running/misc.cpp ./util_cv.cpp $(CORE_FILE) $(LIB_SFRISO)
#$(TARGET_EXE):  ./running/main-glance.cpp
#$(TARGET_EXE):  $(wildcard ./traincascade/*.cpp)
#$(TARGET_EXE):  ./running/FlyCap2.cpp
	$(CC) -o $@ $^ $(CC_FLAG) $(LD_LIBS)

#all: $(TARGET_SO)
all: $(TARGET_EXE)
#all: $(TARGET_EXE) $(TARGET_SO) 

clean:
	rm -rf $(TARGET_EXE) $(TARGET_SO) $(DIR_SRC)/*.o ./*.o
	
.PHONY: all clean prepare $(TARGET_JNI) $(TARGET_EXE) $(TARGET_SO)


