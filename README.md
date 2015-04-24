# tuio-zeromq-publish
A bridge for receiving TUIO messages and then publishing them via ZeroMQ

As configured, this application will listen on port 3333 for TUIO messages.
Once received, these messages are serialized (Java Objects) and published on port 5565.
A client (https://github.com/artistech-inc/tuio-mouse-driver) can be used to receive these messages, deserialize, and process.
--
Matt