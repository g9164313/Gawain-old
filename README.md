# Gawain
This is a JavaFX Project bundle.It includes applications to process images or operate some machines(like remote terminal unit).There are three important packages(File Directory), narl/itrc, narl/itrc/nat, and prj."nat" means "native code". All native code(C/C++) are put in this directory including Makefile or Visual Studio project files."prj" means "project".Every project has its "package name".For example, "prj/reheating" is an console for controling heater and reading value of temperature sensor.The project,"prj/seesaw" has no relationship with the others.

# How to start?
The main entry of this project is in "narl/itrc/Gawain.java", then it will load the config file,"narl/itrc/res/conf.properties".Find the key,"LAUNCH" and use this property value as class path to set all stage or scene.



