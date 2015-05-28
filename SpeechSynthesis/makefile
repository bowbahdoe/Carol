all:
	gcc -c -fPIC speak.c -o speak.o
	gcc -shared -o libspeak.so speak.o -lc
clean:
	rm speak.o
	rm libspeak.so
	
