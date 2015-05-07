# tuio-zeromq-publish
A bridge for receiving [TUIO](http://tuio.org/) messages and then publishing them via [ZeroMQ](http://zeromq.org/).

###Execution
To use the tuio publisher:
 1. git clone https://github.com/artistech-inc/tuio-zeromq-publish.git
 2. cd tuio-zeromq-publish
 3. mvn package
 4. java -jar target/tuio-zeromq-publish-1.0-SNAPSHOT.jar

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
