#include "monitor.h"

Monitor::Monitor() {
  pthread_cond_init(&cond, 0);
}

Monitor::~Monitor() {
  pthread_cond_destroy(&cond);
}

int Monitor::notify() {
  return pthread_cond_signal(&cond);
}

int Monitor::notifyAll() {
  return pthread_cond_broadcast(&cond);
}

int Monitor::wait(pthread_mutex_t *mutex) {
  return pthread_cond_wait(&cond, mutex);
}

int Monitor::timedwait(pthread_mutex_t *mutex, unsigned int millis) {
  struct timespec abstime;
  abstime.tv_sec = millis / 1000;
  abstime.tv_nsec = (long) (millis % 1000) * 1000000;
  return pthread_cond_timedwait(&cond, mutex, (const struct timespec*)&abstime);
}
