# openTelemetryTestA2
For Assignment 2, Question 2 of Brock University's COSC 5P07 Course
Please note that, due to an error, I lost much of the source code for the project and had to recover it from the Jar files I had created. 
As such, there may be some strangeness/inconsistencies in the source code after going through the compiler and back.


## Files
The main program files are fileSender.java, threadServer.java, and forkFileReader.java.

#### fileSender.java
Connects to an instance of threadServer through java.net Sockets, and sequentially sends 10 input text files.
During this process, the threadServer will be handing the socket connections off to forkFileReader threads, meaning the fileSender reconnects to the main server for each file.
Each file is sent through the socket's input and outputstreams in a binary format, 4092 bits at a time using a buffer.

#### threadServer.java
Connects to a fileSender through java.net Sockets, and hands the connection off to a forkFileReader before waiting for a new connection.

#### forkFileReader.java
Is handed a socket connection to the fileSender, which is used to receive a text file through the inputStream in 4092 bit chunks.
Once the file is complete, the connection is closed and the thread ends.

## OpenTelemetry
Each file is instrumented using OpenTelemetry spans. The fileSender jar file and the threadServer jar file are run alongside the opentelemetry-javaagent.jar automatic instrumenter/outputter
to a Zipkin server hosted on Docker for visualization, using these commands:

```java -javaagent:opentelemetry-javaagent.jar -Dotel.metrics.exporter=none -Dotel.traces.exporter=zipkin -Dotel.exporter.zipkin.endpoint=http://localhost:9411/api/v2/spans -jar threadServer.jar```

```java -javaagent:opentelemetry-javaagent.jar -Dotel.metrics.exporter=none -Dotel.traces.exporter=zipkin -Dotel.exporter.zipkin.endpoint=http://localhost:9411/api/v2/spans -jar fileSender.jar```

## Visualization
The image below shows each span observed by the system.

![Screenshot_2588](https://user-images.githubusercontent.com/36076870/208008490-edbdcba8-d54b-4e4e-973d-b3296d2a04c9.png)

### threadServer establishpool

![Screenshot_2589](https://user-images.githubusercontent.com/36076870/208008443-9dc5d57e-6e8e-4363-92d9-aefd07c5c019.png)

It takes approximately 241 milliseconds between the thread pool being established and forks beginning to be created. The majority of this time is due to the delay between the threadServer
and the fileSender .jars being started and the sockets connecting, as each subsequent fork takes significantly less time to resolve.

### threadServer startLoop

![Screenshot_2590](https://user-images.githubusercontent.com/36076870/208012327-928d9b12-4e6c-44ae-b8f8-156fdb385592.png)

The loop comprises the entirety of runtime.

![Screenshot_2591](https://user-images.githubusercontent.com/36076870/208012397-67feaecb-99d5-4289-9433-d8a140c96090.png)
![Screenshot_2592](https://user-images.githubusercontent.com/36076870/208012408-c358553b-96ea-4b6e-a652-a7c67a4bd9ca.png)

Each dot on the above timeline represents one of the sockets being opened and connected. There is a notable delay in the time taken to establish the first one, after which subsequent sockets open significantly faster. 

### fileSender

![Screenshot_2593](https://user-images.githubusercontent.com/36076870/208014619-0db3478e-a77f-429f-8ddd-f9726b19e2ea.png)

The same delay occurs in the first file in the fileSender as well. There is some variation in the other files but not as much, a difference likely down to minor differences in file size.
