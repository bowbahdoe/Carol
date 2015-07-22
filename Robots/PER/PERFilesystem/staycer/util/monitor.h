#ifndef _MONITOR_H_
#define _MONITOR_H_

#include <pthread.h>
#include <time.h>

class Monitor {
 public:
  Monitor();
  ~Monitor();

  int notify();
  int notifyAll();
  int wait(pthread_mutex_t *mutex);
  int timedwait(pthread_mutex_t *mutex, unsigned int millis);

 private:
  pthread_cond_t cond;
};

#endif // _MONITOR_H_
