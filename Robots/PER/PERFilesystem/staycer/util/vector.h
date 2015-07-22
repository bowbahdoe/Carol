#ifndef _VECTOR_H_
#define _VECTOR_H_

#include <pthread.h>
#include <string.h>

#include "monitor.h"

class Vector {
 public:
  Vector();
  ~Vector();
  
  bool add(void * obj);
  void * get(int index);
  void * remove(int index);
  void * removeObject(void *obj);

  int size();
  bool isEmpty();

  pthread_mutex_t mutex;
  Monitor *monitor;

 private:
  void ** arr;
  int arraySize;
  int elements;
};

#endif //_VECTOR_H_
