all:
	cd Games/2048;make;
	cd Utilities/Words;make;
	cd Robots/PER/PERFilesystem;make;

clean:
	cd Games/2048;make clean;
	cd Utilities/Words;make clean;
	find . -type f -name '*.pyc' -delete
	find . -type f -name '*.o' -delete
	find . -type f -name '*.so' -delete
	find . -type f -name '*~' -delete
	find . -type f -name '*py.class' -delete
	find . -type f -name '*.class' -delete
