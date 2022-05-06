# ass1Dos1
assignment 1 for distributed operating systems 1 course 4th year 1st semester 
Amir Mor, id 322521642
Shelly Talis, id 322931510

How to run our project:
open cmd on the working directory and run the following command- "java -jar application.jar <inputFileName>  <outputFileName> <n>".
The relevant jars of the manager and the worker are already uploaded to s3.
The application sends a request message with input file. The manager's main thread accepts the request and executes a 
thread from the pool to procces the local app request. The manager sends all the pdf files to be downloaded to the workers input queue and once finds 
all the relevant messages' responses, he creats a summery file and uploads to s3. The application recieves it in its result queue,
downloads it and creates the result file.

Type of instance we used:
ami = ami-01cc34ab2709337aa
type = T1_MICRO

It took 2 minutes and 47 seconds to our program to finish working on the input files. We used n=15.

Security - the credentials are in sipher text. AWS supports their own security measures for us.

Scalability - our program should work properly when multiple clients are connected at the same time.
We are lifting up to 19 workers and the assigments are divided between them.
We keep nothing in the memory, everything is kept in aws resources - S3.
All the resources we use, except for the workers, scale up as the clients increasing in count.

Persistence - In case of terminate mode, local application sends a termination
message to the Manager. The Manager does not accept any more input files from local applications. The Manager
▪ Waits for all the workers to finish their job, and then terminates them.
▪ Creates response messages for the jobs, if needed.
▪ Terminates.
If an exception occurs, then the worker recover from it, send a message to the
manager and continue working on the next message.
If a worker stops working unexpectedly before finishing its work on a message, then some
other worker will handle that message.

Threads-
The local application run sequentially and does not require multiple threads.
As for the manager, we accepted every input messsage with a main thread, and every local application request is handled 
by a thread which is "borrowed" from a pool of threads (limited in its size).
The workers run on different machines and behave already as "worker thread" and we saw no reason to "thread" them too.
We support running more than one client at the same time, and the manager threads allow the work partition to be graceful in some way.
The work is divided more rationally.

We ran more than one client at the same time and they work properly, finish properly, and the results are correct.

Full run - the local application communicates with the manager with bucket "manager-input-bucket" in S3 and sends messages to the manager
with SQS queue that called "manager-input-queue". Local application recieves the result message from the manager with SQS queue 
that called "local-app-result-queue". 
The manager recieves and sends messages to the workers with SQS queue that called "workers-queue".

 
We manage the termination process - we always did.. and we will always will with a lot of will.

System limitations - we are lifting up to 19 workers.

All the workers work hard. They all work equally. 
with some diffrence of one message here one message there due to network and clusters in aws we assume.
in average the work equally.

Local application, manager and workers - each of them does the work of their own.
None of them doing more work than one's supposed to. Each part of our system has properly defined tasks as described in the task.
The application creates the result file and the manager mediates with the most little effort because it is the 
over worked componnent in the system.

Distribute - 
A distributed system is a computing environment in which various components are spread across multiple computers
(or other computing devices) on a network.
Some components wait for others, every thread in the manager waits for the workers to finish.
The applications wait for the manager to answer them too. 
