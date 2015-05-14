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

import TUIO.TuioBlob;
import TUIO.TuioCursor;
import TUIO.TuioListener;
import TUIO.TuioObject;
import TUIO.TuioTime;
import com.artistech.protobuf.ProtoConverter;
import com.artistech.utils.Mailbox;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.text.MessageFormat;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TuioSink implements TuioListener {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Log logger = LogFactory.getLog(TuioSink.class);
    protected final Mailbox<ImmutablePair<String, byte[]>> mailbox = new Mailbox<>();
    private SerializeType serialization = SerializeType.JSON;
    private final ServiceLoader<ProtoConverter> services;

    public enum SerializeType {

        OBJECT,
        JSON,
        PROTOBUF
    }

    /**
     * Constructor
     */
    public TuioSink() {
        services = ServiceLoader.load(ProtoConverter.class);
    }

    private void broadcast(Object obj) {
        String type = obj.getClass().getSimpleName();
        switch (serialization) {
            case PROTOBUF: {
                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    boolean success = false;
                    for (ProtoConverter service : services) {
                        if (service.supportsConversion(obj)) {
                            service.convertToProtobuf(obj).build().writeTo(baos);
                            baos.flush();
                            success = true;
                            break;
                        }
                    }
                    if (success) {
                        mailbox.addMessage(new ImmutablePair<>(type, baos.toByteArray()));
                    }
                } catch (IOException ex) {
                    Logger.getLogger(TuioSink.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            break;
            case JSON:
                try {
                    mailbox.addMessage(new ImmutablePair<>(type, mapper.writeValueAsString(obj).getBytes()));
                } catch (JsonProcessingException ex) {
                    Logger.getLogger(TuioSink.class.getName()).log(Level.SEVERE, null, ex);
                }
                break;
            case OBJECT:
                try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        ObjectOutput out = new ObjectOutputStream(bos)) {
                    out.writeObject(obj);
                    mailbox.addMessage(new ImmutablePair<>(type, bos.toByteArray()));
                } catch (java.io.IOException ex) {
                    logger.error(null, ex);
                }
                break;
        }
    }

    /**
     * Add Object Event.
     *
     * @param tobj
     */
    @Override
    public void addTuioObject(TuioObject tobj) {
        logger.debug(MessageFormat.format("add tuio object symbol id: {0}", tobj.getSymbolID()));
        broadcast(tobj);
    }

    /**
     * Update Object Event.
     *
     * @param tobj
     */
    @Override
    public void updateTuioObject(TuioObject tobj) {
        logger.debug(MessageFormat.format("update tuio object symbol id: {0}", tobj.getSymbolID()));
        broadcast(tobj);
    }

    /**
     * Remove Object Event.
     *
     * @param tobj
     */
    @Override
    public void removeTuioObject(TuioObject tobj) {
        logger.debug(MessageFormat.format("remove tuio object symbol id: {0}", tobj.getSymbolID()));
        broadcast(tobj);
    }

    /**
     * Refresh Event.
     *
     * @param bundleTime
     */
    @Override
    public void refresh(TuioTime bundleTime) {
        logger.debug(MessageFormat.format("refresh frame id: {0}", bundleTime.getFrameID()));
        broadcast(bundleTime);
    }

    /**
     * Add Cursor Event.
     *
     * @param tcur
     */
    @Override
    public void addTuioCursor(TuioCursor tcur) {
        logger.trace(MessageFormat.format("add tuio cursor id: {0}", tcur.getCursorID()));
        broadcast(tcur);
    }

    /**
     * Update Cursor Event.
     *
     * @param tcur
     */
    @Override
    public void updateTuioCursor(TuioCursor tcur) {
        logger.trace(MessageFormat.format("update tuio cursor id: {0}", tcur.getCursorID()));
        broadcast(tcur);
    }

    /**
     * Remove Cursor Event.
     *
     * @param tcur
     */
    @Override
    public void removeTuioCursor(TuioCursor tcur) {
        logger.trace(MessageFormat.format("remove tuio cursor id: {0}", tcur.getCursorID()));
        broadcast(tcur);
    }

    /**
     * Add Blob Event.
     *
     * @param tblb
     */
    @Override
    public void addTuioBlob(TuioBlob tblb) {
        logger.debug(MessageFormat.format("Added Blob: {0}", tblb.getBlobID()));
        broadcast(tblb);
    }

    /**
     * Update Blob Event.
     *
     * @param tblb
     */
    @Override
    public void updateTuioBlob(TuioBlob tblb) {
        logger.debug(MessageFormat.format("Update Blob: {0}", tblb.getBlobID()));
        broadcast(tblb);
    }

    /**
     * Remove Blob Event.
     *
     * @param tblb
     */
    @Override
    public void removeTuioBlob(TuioBlob tblb) {
        logger.debug(MessageFormat.format("Remove Blob: {0}", tblb.getBlobID()));
        broadcast(tblb);
    }

    public SerializeType getSerializationType() {
        return serialization;
    }

    public void setSerializationType(SerializeType value) {
        serialization = value;
    }
}
