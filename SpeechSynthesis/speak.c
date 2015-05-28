#include <string.h>
#include <stdlib.h>

void speak(char* words)
{
    char str[1000];
    strcpy(str," padsp flite -voice slt -t '");//kal16
    strcat(str,words);
    strcat(str,"'");
    system(str);

}
void hush(void)
{
	;
}
//int main(){speak("Im still trying my best to make it speak");return 0;}
