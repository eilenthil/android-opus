import os, sys, shutil

from subprocess import PIPE, Popen

def cmdline(command):
    output = sys.stdout

    process = Popen(
        args=command,
        stdout=output,
        shell=True
    )
    return process.communicate()[0]

def main():
	abis = [ 'armeabi-v7a', 'arm64-v8a', 'x86', 'x86_64']

	# FILL SDK LOCATION HERE
	ANDROID_SDK_HOME = '' 
	# FILL SDK LOCATION HERE

	NDK_HOME = ANDROID_SDK_HOME + '/ndk-bundle'
	NINJA_LOC = ANDROID_SDK_HOME + '/cmake/3.10.2.4988404/bin/ninja'
	
	print ("making output dir android")
	try:
		os.mkdir('android')
	except:
		shutil.rmtree('android', ignore_errors=True)


	for abi in abis:
		outdir = 'android/' + abi
		os.mkdir(outdir)
		try:
			shutil.rmtree('CMakeFiles', ignore_errors=True)
			os.remove('CMakeCache.txt')
		except:
			print("Whatever")
			
		android_abi_str = '-DANDROID_ABI=' + abi
		android_ndk_home_str = '-DANDROID_NDK=' + NDK_HOME
		output_dir_str = '-DCMAKE_LIBRARY_OUTPUT_DIRECTORY=' + outdir
		output_dir_ar_str = '-DCMAKE_ARCHIVE_OUTPUT_DIRECTORY=' + outdir
		android_api_str = '-DANDROID_NATIVE_API_LEVEL=' + str(18)
		cmake_toolchain_str = '-DCMAKE_TOOLCHAIN_FILE='  + NDK_HOME + '/build/cmake/android.toolchain.cmake'
		ninja_str = '-DCMAKE_MAKE_PROGRAM=' + NINJA_LOC 

		build_cmd = ' '.join([ 
				'cmake',
				'-G\"Ninja\"',
				'-DCMAKE_BUILD_TYPE=Release',
				android_abi_str,
				android_ndk_home_str,
				output_dir_str,
				output_dir_ar_str,
				android_api_str,
				'-DANDROID_TOOLCHAIN=clang',
				ninja_str,
				cmake_toolchain_str,
				'-DBUILD_SHARED_LIBS=OFF',
				'-DOPUS_CUSTOM_MODES=ON',
				'-DCMAKE_POSITION_INDEPENDENT_CODE=ON',
				'..'])
		print(build_cmd + "\n")
		cmdline(build_cmd)
    
        
		print(NINJA_LOC+'\n')
		cmdline(NINJA_LOC + ' -v')
		print("===================== BUILD FOR " + abi + " DONE ==========================================\n")
		
		
if __name__ == "__main__":
    main()
