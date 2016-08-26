
#DIR_JNI=/home/lib/jdk/include
DIR_JNI=/home/qq/.local/jdk8/include

TARGET_EXE=gawain

TARGET_SO=libspringboard.so

#### COMMON ####

INC_JNI := -I$(DIR_JNI) -I$(DIR_JNI)/linux
	
#### Pleora eBus ####

ROOT_EBUS:=/home/lib/eBus

INC_EBUS := -I$(ROOT_EBUS)/include \
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

#### Basler Pylon ####

ROOT_PYLON :=  /home/lib/pylon
GENICAM_ROOT := $(ROOT_PYLON)/genicam

INC_PYLON := -I$(ROOT_PYLON)/include \
	-I$(GENICAM_ROOT)/library/CPP/include \
	-DUSE_GIGE 

LIB_PYLON :=  -L$(ROOT_PYLON)/lib64 \
	-L$(GENICAM_ROOT)/bin/Linux64_x64 \
	-L$(GENICAM_ROOT)/bin/Linux64_x64/GenApi/Generic \
	-L$(ROOT_PYLON)/lib32 \
	-L$(GENICAM_ROOT)/bin/Linux32_i86 \
	-L$(GENICAM_ROOT)/bin/Linux32_i86/GenApi/Generic \
	-lpylonbase -lpylonutility \
	-lGenApi_gcc40_v2_3 -lGCBase_gcc40_v2_3 -lLog_gcc40_v2_3 -lMathParser_gcc40_v2_3 -lXerces-C_gcc40_v2_7_1 -llog4cpp_gcc40_v2_3 \
	-Wl,-E
	
#### Euresys Multicam ####

ROOT_MULTICAM:=/opt/euresys/multicam

INC_MULTICAM:=-I$(ROOT_MULTICAM)/include

LIB_MULTICAM:= -L$(ROOT_MULTICAM) -lMultiCam 

#### FlyCapture ####

INC_FLYCAPTURE:=-I/usr/include/flycapture

LIB_FLYCAPTURE:=-L/usr/lib  -lflycapture  -lflycapturegui

### decide which configuration ####

CC=g++

CC_FLAG = -g -I. $(INC_JNI)
CC_FLAG += -DVISION $(shell pkg-config --cflags opencv) 
#CC_FLAG += $(shell pkg-config --cflags libmodbus) 
#CC_FLAG += $(shell pkg-config --cflags qt5) 
#CC_FLAG += $(INC_EBUS)
#CC_FLAG += $(INC_PYLON)
#CC_FLAG += $(INC_MULTICAM)
#CC_FLAG += $(INC_FLYCAPTURE)

LD_LIBS = $(shell pkg-config --libs opencv) 
#LD_LIBS += $(shell pkg-config --libs libmodbus) 
#LD_LIBS += $(shell pkg-config --libs Qt5Core Qt5Gui Qt5Widgets Qt5Test)
#LD_LIBS += $(LIB_EBUS)
#LD_LIBS += $(LIB_PYLON)
#LD_LIBS += $(LIB_MULTICAM)
#LD_LIBS += $(LIB_FLYCAPTURE)


OBJECTS = $(SOURCES:.cpp=.o)

#%.o:%.cpp
#	$(CC) -c  $^ $(CC_FLAG) -o $@

#################

CHECK_EXEC := $(word 2,$(shell pgrep -f "narl.itrc.Gawain"))

#SFR_MITRE =  ./SFR/sfr_iso.c ./SFR/find_area.c
#IMG_FILTER = FilterMisc.cpp FilterIsBlur.cpp
DEV_CAMERA = CamBundle.cpp CamVidcap.cpp 
#DEV_CAMERA = CamBundle.cpp CamVidcap.cpp CamMulticam.cpp 

$(TARGET_SO):  utils_jni.cpp DevTTY.cpp  $(IMG_FILTER) $(DEV_CAMERA) 
	$(CC) $(CC_FLAG)  -fPIC -shared -o $@ $^ $(LD_LIBS)
	cp -rf $@ ../bin/narl/itrc/nat
	cp -rf $@ ../src/narl/itrc/nat
ifeq ($(CHECK_EXEC),)
	@echo "no process, so we ovewrite it~~"
	cp -rf $@ ../gawain/$@
else
	@echo "process is running!!!"
endif
	

$(TARGET_EXE):  ./wrap-unix/main_xx.cpp FilterIsBlur.cpp
	$(CC) $(CC_FLAG) -o $@ $^ $(LD_LIBS)

#$(TARGET_EXE):  /home/qq/labor/opencv-3.1/contrib/modules/text/samples/character_recognition.cpp
#	$(CC) $(CC_FLAG) -o $@ $^ $(LD_LIBS)

#all: $(TARGET_SO)
all: $(TARGET_EXE)
#all: $(TARGET_EXE) $(TARGET_SO) 

clean:
	rm -rf $(TARGET_EXE) $(TARGET_SO) $(DIR_SRC)/*.o ./*.o
	
.PHONY: all clean prepare $(TARGET_JNI) $(TARGET_EXE) $(TARGET_SO)


