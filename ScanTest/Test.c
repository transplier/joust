#include <stdio.h>
#include <string.h>

//read_comport returned data type
#define EMPTY    0
#define DATA     1
#define PROMPT   2

#define SPECIAL_DELIMITER   '\t'

//comport status
#define READY          0
#define NOT_OPEN       1
#define USER_IGNORED   2

//process_response return values
#define HEX_DATA           0
#define BUS_BUSY           1
#define BUS_ERROR          2
#define BUS_INIT_ERROR     3
#define UNABLE_TO_CONNECT  4
#define CAN_ERROR          5
#define DATA_ERROR         6
#define DATA_ERROR2        7
#define ERR_NO_DATA        8
#define BUFFER_FULL        9
#define SERIAL_ERROR       10
#define UNKNOWN_CMD        11
#define RUBBISH            12

#define INTERFACE_ID       13
#define INTERFACE_ELM320   13
#define INTERFACE_ELM322   14
#define INTERFACE_ELM323   15
#define INTERFACE_ELM327   16

// timeouts
#define OBD_REQUEST_TIMEOUT   9900
#define ATZ_TIMEOUT           1500
#define AT_TIMEOUT            130
#define ECU_TIMEOUT           5000

#define TRUE 1
#define FALSE 0

// DO NOT TRANSLATE ANY STRINGS IN THIS FUNCTION!
int process_response(char *msg_received)
{
   int i = 0;
   char *msg = msg_received;
   int echo_on = TRUE; //echo status
   int is_hex_num = TRUE;
   char temp_buf[80];
   while(*msg && (*msg <= ' '))
      msg++;
   if (strncmp(msg, "SEARCHING...", 12) == 0)
      msg += 13;
   else if (strncmp(msg, "BUS INIT: OK", 12) == 0)
      msg += 13;
   else if (strncmp(msg, "BUS INIT: ...OK", 15) == 0)
      msg += 16;
   for(i = 0; *msg; msg++) //loop to copy data
   {
      if (*msg > ' ')  // if the character is not a special character or space
      {
         if (*msg == '<') // Detect <DATA_ERROR
         {
            if (strncmp(msg, "<DATA ERROR", 10) == 0)
               return DATA_ERROR2;
            else
               return RUBBISH;
         }
         msg_received[i] = *msg; // rewrite response
         if (!isxdigit(*msg) && *msg != ':')
            is_hex_num = FALSE;
         i++;
      }
      else if (((*msg == '\n') || (*msg == '\r')) && (msg_received[i-1] != SPECIAL_DELIMITER)) // if the character is a CR or LF
         msg_received[i++] = SPECIAL_DELIMITER; // replace CR with SPECIAL_DELIMITER
   }   
   if (i > 0)
      if (msg_received[i-1] == SPECIAL_DELIMITER)
         i--;
   msg_received[i] = '\0'; // terminate the string

   if (is_hex_num)
      return HEX_DATA;

   if (strcmp(msg_received, "NODATA") == 0)
      return ERR_NO_DATA;
   if (strcmp(msg_received + strlen(msg_received) - 15, "UNABLETOCONNECT") == 0)
      return UNABLE_TO_CONNECT;
   if (strcmp(msg_received + strlen(msg_received) - 7, "BUSBUSY") == 0)
      return BUS_BUSY;
   if (strcmp(msg_received + strlen(msg_received) - 9, "DATAERROR") == 0)
      return DATA_ERROR;
   if (strcmp(msg_received + strlen(msg_received) - 8, "BUSERROR") == 0 ||
       strcmp(msg_received + strlen(msg_received) - 7, "FBERROR") == 0)
      return BUS_ERROR;
   if (strcmp(msg_received + strlen(msg_received) - 8, "CANERROR") == 0)
      return CAN_ERROR;
   if (strcmp(msg_received + strlen(msg_received) - 10, "BUFFERFULL") == 0)
      return BUFFER_FULL;
   if (strcmp(msg_received, "BUSINIT:ERROR") == 0 ||
       strcmp(msg_received, "BUSINIT:...ERROR") == 0)
      return BUS_INIT_ERROR;
   if (strcmp(msg_received, "BUS INIT:") == 0 ||
       strcmp(msg_received, "BUS INIT:...") == 0)
      return SERIAL_ERROR;
   if (strcmp(msg_received, "?") == 0)
      return UNKNOWN_CMD;
   if (strncmp(msg_received, "ELM320", 6) == 0)
      return INTERFACE_ELM320;
   if (strncmp(msg_received, "ELM322", 6) == 0)
      return INTERFACE_ELM322;
   if (strncmp(msg_received, "ELM323", 6) == 0)
      return INTERFACE_ELM323;
   if (strncmp(msg_received, "ELM327", 6) == 0)
      return INTERFACE_ELM327;

   return RUBBISH;
}

int main(){
   char buf[200];
   strcpy(buf, "ELM320");
   int ret = process_response(buf);
   printf("%d\n", ret==INTERFACE_ELM320);

   strcpy(buf, "ELM323");
   ret = process_response(buf);
   printf("%d\n", ret==INTERFACE_ELM323);
   
   strcpy(buf, "9f");
   ret = process_response(buf);
   printf("%d\n", ret==HEX_DATA);
   
   strcpy(buf, "9F");
   ret = process_response(buf);
   printf("%d\n", ret==HEX_DATA);
   
   strcpy(buf, "97");
   ret = process_response(buf);
   printf("%d\n", ret==HEX_DATA);
   
   strcpy(buf, "AF");
   ret = process_response(buf);
   printf("%d\n", ret==HEX_DATA);
   
   strcpy(buf, "FA");
   ret = process_response(buf);
   printf("%d\n", ret==HEX_DATA);
   
   strcpy(buf, "<DATA ERROR");
   ret = process_response(buf);
   printf("%d\n", ret==DATA_ERROR2);
   
}
