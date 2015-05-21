# tuio-zeromq-publish
A bridge for receiving [TUIO](http://tuio.org/) messages and then publishing them via [ZeroMQ](http://zeromq.org/).

###Execution
To use the tuio publisher:
 1. git clone https://github.com/artistech-inc/tuio-zeromq-publish.git
 2. cd tuio-zeromq-publish
 3. mvn package
 4. java -jar target/tuio-zeromq-publish-1.1-SNAPSHOT.jar

As configured, this application will listen (by default) on port 3333 for TUIO messages.
Once received, these messages are serialized (by default) using [Google Protocol Buffers](https://developers.google.com/protocol-buffers/) and published (by default) on port 5565.
A [companion client](https://github.com/artistech-inc/tuio-mouse-driver) can be used to receive these messages, deserialize, and process.

To change these options the following command line options are available:
```
-h,--help                     Show this message.
-s,--serialize-method <arg>   Serialization Method (JSON, OBJECT, Default
                             = PROTOBUF).
-t,--tuio-port <arg>          TUIO Port to listen on. (Default = 3333)
-z,--zeromq-port <arg>        ZeroMQ Port to publish on. (Default = 5565)
```

###Dependencies
ZeroMQ support is dependent on available native libraries.  When compiling, maven will search for these files and provide any jar depenedencies suitable.
 1. Linux:
   1. Searches for /usr/lib/libjzmq.so
   2. If this file exists, the dependency jar [jzmq.jar](https://github.com/zeromq/jzmq) is imported.
   3. If this file is missing, the dependency jar [jeromq.jar](https://github.com/zeromq/jeromq) is imported.
 2. Mac OS X:
   1. Searches for /usr/lib/libjzmq.dynlib
   2. If this file exists, the dependency jar [jzmq.jar](https://github.com/zeromq/jzmq) is imported.
   3. If this file is missing, the dependency jar [jeromq.jar](https://github.com/zeromq/jeromq) is imported.

The two jar files provide identical support.  However, the [jzmq.jar](https://github.com/zeromq/jzmq) file uses JNI to provide faster support where [jeromq.jar](https://github.com/zeromq/jeromq) is a pure java implementation.  The jzmq.jar requires libjzmq.so which in turn requires libzmq.so to be available.

###ZeroMQ Transmssion/Serialization
Transmission of the TUIO objects via ZeroMQ is provided by 3 different mechanisms.
 1. Java Object Serialization
 2. JSON Serialization (using [Jackson](https://github.com/FasterXML/jackson))
 3. [Google Protocol Buffer](https://developers.google.com/protocol-buffers/)
