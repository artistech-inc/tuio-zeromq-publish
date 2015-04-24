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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.zeromq.ZMQ;

public class TuioPublish {
    
    private final static Log logger = LogFactory.getLog(TuioPublish.class);

    public static void main(String[] argv) throws InterruptedException {
        //read off the TUIO port from the command line
        int port = 3333;
        if (argv.length == 1) {
            try {
                port = Integer.parseInt(argv[1]);
            } catch (NumberFormatException e) {
                System.out.println(MessageFormat.format("Port value '{0}' not recognized.", argv[1]));
            }
        }

        //start up the zmq publisher
        ZMQ.Context context = ZMQ.context(1);
        ZMQ.Socket publisher = context.socket(ZMQ.PUB);

        // We send updates via this socket
        publisher.bind("tcp://*:5565");

        //create a new TUIO sink connected at the specified port
        TuioSink sink = new TuioSink();
        sink.setSerializationType(TuioSink.SerializeType.OBJECT);
        TuioClient client = new TuioClient(port);

        logger.info(MessageFormat.format("Listening to TUIO message at port: {0}", Integer.toString(port)));
        client.addTuioListener(sink);
        client.connect();

        //while not halted (infinite loop...)
        //read any available messages and publish
        while(!sink.mailbox.isHalted()) {
            byte[] msg = sink.mailbox.getMessage();
            publisher.send(msg, 0);
        }

        //cleanup
        publisher.close();
        context.term();
    }
}
