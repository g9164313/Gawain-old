
DIR_JNI=/home/lib/jdk/include
DIR_CLASS=../Gawain/bin:/home/lib/common/swt-debug.jar:/home/lib/opencv/share/OpenCV/java/opencv-245.jar

TARGET_EXE=gawain
TARGET_JNI=wrap.h
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

ROOT_MULTICAM:=/home/lib/multicam

INC_MULTICAM:=-I$(ROOT_MULTICAM)/include

LIB_MULTICAM:= -L$(ROOT_MULTICAM) -lMultiCam 

#################

CC=g++

CC_FLAG = -g -DVISION -I. $(INC_JNI)
CC_FLAG += $(shell pkg-config --cflags opencv) 
CC_FLAG += $(shell pkg-config --cflags libmodbus) 
#CC_FLAG += $(shell pkg-config --cflags qt5) 
CC_FLAG += $(INC_EBUS)
CC_FLAG += $(INC_PYLON)
#CC_FLAG += $(INC_MULTICAM)

LD_LIBS = $(shell pkg-config --libs opencv) 
LD_LIBS += $(shell pkg-config --libs libmodbus) 
#LD_LIBS += $(shell pkg-config --libs Qt5Core Qt5Gui Qt5Widgets Qt5Test) 
LD_LIBS += $(LIB_EBUS)
LD_LIBS += $(LIB_PYLON)
LD_LIBS += -lgclibo -lgclib
#LD_LIBS += $(LIB_MULTICAM)

OBJECTS = $(SOURCES:.cpp=.o)

#%.o:%.cpp
#	$(CC) -c  $^ $(CC_FLAG) -o $@

#################

EXEC_ING := $(word 2,$(shell pgrep -f "narl.itrc.Gawain"))

#$(TARGET_SO):  utils_jni.cpp dev_modbus.cpp dev_dio3232.cpp 
$(TARGET_SO):  utils_jni.cpp utils_cv.cpp  CamBundle.cpp CamVidcap.cpp  CamPylon.cpp  CamEbus.cpp SlantEdge.cpp
	$(CC) $(CC_FLAG)  -fPIC -shared -o $@ $^ $(LD_LIBS)
	cp -rf $@ ../bin/narl/itrc/nat
	cp -rf $@ ../src/narl/itrc/nat
ifeq ($(EXEC_ING),)
	@echo "no process, so we ovewrite it~~"
	cp -rf $@ ../gawain
else
	@echo "process is running!!!"
endif
	

$(TARGET_EXE):  ./wrap-unix/main1.cpp utils_jni.cpp utils_cv.cpp SlantEdge.cpp
	$(CC) $(CC_FLAG) -o $@ $^ $(LD_LIBS)

#$(TARGET_EXE):  /home/qq/labor/opencv-3.1/contrib/modules/text/samples/character_recognition.cpp
#	$(CC) $(CC_FLAG) -o $@ $^ $(LD_LIBS)

#$(TARGET_JNI):
#	javah  -o ./src/$@ -cp $(DIR_CLASS) $(NAT_CLASS)

#all: $(TARGET_SO)
all: $(TARGET_EXE)
#all: $(TARGET_SO) $(TARGET_EXE)
#all: clean $(TARGET_SO) $(TARGET_EXE)

clean:
	rm -rf $(TARGET_EXE) $(TARGET_SO) $(DIR_SRC)/*.o ./*.o
	
.PHONY: all clean prepare $(TARGET_JNI) $(TARGET_EXE) $(TARGET_SO)


