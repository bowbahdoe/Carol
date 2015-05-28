all:
	cd SpeechSynthesis;make;

clean:
	find . -type f -name '*.pyc' -delete
	find . -type f -name '*.o' -delete
	find . -type f -name '*.so' -delete
	find . -type f -name '*~' -delete
