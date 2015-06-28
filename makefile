all:
	cd Games/2048;make;

clean:
	find . -type f -name '*.pyc' -delete
	find . -type f -name '*.o' -delete
	find . -type f -name '*.so' -delete
	find . -type f -name '*~' -delete
	find . -type f -name '*py.class' -delete
