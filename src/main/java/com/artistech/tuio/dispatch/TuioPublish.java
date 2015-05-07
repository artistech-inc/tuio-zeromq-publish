/*
 * Copyright 2015 ArtisTech, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.artistech.tuio.dispatch;

import TUIO.TuioClient;
import java.text.MessageFormat;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.zeromq.ZMQ;

public class TuioPublish {

    public static void main(String[] args) throws InterruptedException {
        //read off the TUIO port from the command line
        int tuio_port = 3333;
        int zeromq_port = 5565;
        TuioSink.SerializeType serialize_method = TuioSink.SerializeType.PROTOBUF;

        Options options = new Options();
        options.addOption("t", "tuio-port", true, "TUIO Port to listen on. (Default = 3333)");
        options.addOption("z", "zeromq-port", true, "ZeroMQ Port to publish on. (Default = 5565)");
        options.addOption("s", "serialize-method", true, "Serialization Method (JSON, OBJECT, Default = PROTOBUF).");
        options.addOption("h", "help", false, "Show this message.");
        HelpFormatter formatter = new HelpFormatter();

        try {
            CommandLineParser parser = new org.apache.commons.cli.BasicParser();
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("help")) {
                formatter.printHelp("tuio-zeromq-publish", options);
                return;
            } else {
                if (cmd.hasOption("t") || cmd.hasOption("tuio-port")) {
                    tuio_port = Integer.parseInt(cmd.getOptionValue("t"));
                }
                if (cmd.hasOption("z") || cmd.hasOption("zeromq-port")) {
                    zeromq_port = Integer.parseInt(cmd.getOptionValue("z"));
                }
                if (cmd.hasOption("s") || cmd.hasOption("serialize-method")) {
                    serialize_method = (TuioSink.SerializeType) Enum.valueOf(TuioSink.SerializeType.class, cmd.getOptionValue("s"));
                }
            }
        } catch (ParseException | IllegalArgumentException ex) {
            System.err.println("Error Processing Command Options:");
            formatter.printHelp("tuio-zeromq-publish", options);
            return;
        }

        //start up the zmq publisher
        ZMQ.Context context = ZMQ.context(1);
        // We send updates via this socket
        try (ZMQ.Socket publisher = context.socket(ZMQ.PUB)) {
            // We send updates via this socket
            publisher.bind("tcp://*:" + Integer.toString(zeromq_port));

            //create a new TUIO sink connected at the specified port
            TuioSink sink = new TuioSink();
            sink.setSerializationType(serialize_method);
            TuioClient client = new TuioClient(tuio_port);

            System.out.println(MessageFormat.format("Listening to TUIO message at port: {0}", Integer.toString(tuio_port)));
            System.out.println(MessageFormat.format("Publishing to ZeroMQ at port: {0}", Integer.toString(zeromq_port)));
            System.out.println(MessageFormat.format("Serializing as: {0}", serialize_method));
            client.addTuioListener(sink);
            client.connect();

            //while not halted (infinite loop...)
            //read any available messages and publish
            while (!sink.mailbox.isHalted()) {
                byte[] msg = sink.mailbox.getMessage();
                publisher.send(msg, 0);
            }

            //cleanup
        }
        context.term();
    }
}
