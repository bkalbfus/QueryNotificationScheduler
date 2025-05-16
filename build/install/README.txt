Query Notification Scheduler
Author:  Brian Kalbfus, bkalbfus@akcourts.us

The function of this tool is:
   1. Request a SQL Server query notification via ODBC
   2. Listen to it via JDBC with SQL like "WAITFOR(RECEIVE...."
   3. Execute a command based on the message text of the received records.

Jobs.xml contains the definitions for the the notification and commands.


-------------------
Installation
-------------------
SQL Server:

USE master
ALTER DATABASE <databasename> SET ENABLE_BROKER

USE <databasename>
CREATE QUEUE <queuename>
CREATE SERVICE <servicename> ON QUEUE <queuename>
([http://schemas.microsoft.com/SQL/Notifications/PostQueryNotification]);

GRANT RECEIVE ON QueryNotificationErrorsQueue TO <username>
GRANT SUBSCRIBE QUERY NOTIFICATIONS TO <username>
GRANT RECEIVE ON <queuename> TO <username>
GRANT CONTROL ON SERVICE::<servicename> TO <username>

---------------------
Windows Service:

   1.  Copy the build/install folder to the location you want to run from.
   2.  from the nssm-2.24/win64 folder, run  nssm install <service name> <path to java executable> -jar <path to install location>\QueryNotificationScheduler.jar

---------------------

Configuration:
   edit scheduler.properties with the values you have for your deployment.



