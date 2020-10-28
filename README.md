# android-opus


This project is a sample of how to compile the opus codec seperated from an android application
Than an easy to use Java-C jni interface which uses the already built codec library.
This prevents the codec to be needed to be compiled inside the application project

1. use git-submodule init to fetch latest version of opus
2. create a build sub-dir under the opus main directory
3. copy build_opus_android.py to that build directory
4. open the build_opus_android.py and fill the location where your android SDK is installed into the ANDROID_SDK_HOME
	note: you need the cmake and ndk-bundle to be present under that directory
5. open a shell and run python build_opus_android.py
	this will build the opus codec into 4 differnt .a files as specified by the abis array in build_opus_android.py

