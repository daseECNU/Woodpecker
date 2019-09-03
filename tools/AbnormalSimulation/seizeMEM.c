#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <signal.h>
#include <sys/time.h>
char (*a)[1024*1024];
static bool is_End=false;
void test_func(int count)
{ 
  is_End=true;
}

void init_sigaction()
{
    struct sigaction act;
          
    act.sa_handler = test_func; //设置处理信号的函数
    act.sa_flags  = 0;

    sigemptyset(&act.sa_mask);
    sigaction(SIGPROF, &act, NULL);//时间到发送SIGROF信号
}

void init_time(int t)
{
    struct itimerval val;
         
    val.it_value.tv_sec = 10; //t秒后启用定时器
    val.it_value.tv_usec = 0;

    //val.it_interval = val.it_value; //定时器间隔为ts
    val.it_interval.tv_sec = 0;
    val.it_interval.tv_usec = 0;
    setitimer(ITIMER_PROF, &val, NULL);
}
int main(int argc, char* argv[])
{
   init_sigaction();
   int t = atoi(argv[1]); //定时时间
   init_time(t);
   int size= atoi(argv[2]);
   long count=size*1024;
   a=new char[count][1024*1024];
   for(int i=0; i< count; i++)
     memset(a[i], 0, sizeof(a[i]));
   while(true)
   {
    if(is_End)
	break;
   }
   delete[] a;
   return 0;
}
