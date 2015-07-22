#include "vector.h"

Vector::Vector() {
  arraySize = 8;
  elements = 0;
  arr = new void * [arraySize];
  pthread_mutex_init(&mutex, 0);
  monitor = new Monitor();
}

Vector::~Vector() {
  /*for(int i=0; i<elements; i++)
    if(arr[i])
    delete arr[i];*/
  delete arr;
  pthread_mutex_destroy(&mutex);
  delete monitor;
}

bool Vector::add(void * obj) {
  if(elements == arraySize) {
    arraySize *= 2;
    void ** newArr = new void *[arraySize];
    if(newArr == 0) {
      arraySize /= 2;
      return false;
    }
    memcpy(newArr, arr, sizeof(void *)*elements);
    delete arr;
    arr = newArr;
  }
  arr[elements++] = obj;
  return true;
}

void * Vector::get(int index) {
  if(index < 0 || index >= elements)
    return NULL;
  return arr[index];
}

void * Vector::remove(int index) {
  if(index < 0 || index >= elements)
    return NULL;
  void * retVal = arr[index];
  int elementsToMove = elements - index - 1;
  if(elementsToMove > 0)
    memmove(arr+index, arr+index+1, elementsToMove*sizeof(void *));
  elements--;
  return retVal;
}

void * Vector::removeObject(void *obj) {
  for(int i=0; i<elements; i++)
    if(arr[i] == obj) 
      return remove(i);
  return 0;
}

int Vector::size() {
  return elements;
}

bool Vector::isEmpty() {
  return elements == 0;
}
