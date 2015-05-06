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
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TuioSink implements TuioListener {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Log logger = LogFactory.getLog(TuioSink.class);
    protected final Mailbox<byte[]> mailbox = new Mailbox<>();
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

    private void broadcast(String action, Object obj) {
        HashMap<String, Object> ret = new HashMap<>();
        try {
            PropertyDescriptor[] props = Introspector.getBeanInfo(obj.getClass()).getPropertyDescriptors();
            ret.put("action", action);
            for (PropertyDescriptor pd : props) {
                if (pd.getReadMethod() != null) {
                    try {
                        ret.put(pd.getDisplayName(), pd.getReadMethod().invoke(obj));
                    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                        Logger.getLogger(TuioSink.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        } catch (IntrospectionException ex) {
            Logger.getLogger(TuioSink.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            switch (serialization) {
                case PROTOBUF: {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    try {
                        boolean success = false;
                        for (ProtoConverter service : services) {
                            if (service.supportsConversion(obj)) {
                                service.convertToProtobuf(obj).build().writeTo(baos);
                                success = true;
                                break;
                            }
                        }
                        if (success) {
//                        convertToProtobuf(obj).build().writeTo(baos);
                            baos.flush();
                            mailbox.addMessage(baos.toByteArray());
                        }
                        baos.close();
                    } catch (IOException ex) {
                        Logger.getLogger(TuioSink.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                break;
                case JSON:
                    mailbox.addMessage(mapper.writeValueAsString(ret).getBytes());
                    break;
                case OBJECT:
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    ObjectOutput out = null;
                    try {
                        out = new ObjectOutputStream(bos);
                        out.writeObject(obj);
                        byte[] yourBytes = bos.toByteArray();
                        mailbox.addMessage(yourBytes);
                    } catch (java.io.IOException ex) {
                        logger.error(null, ex);
                    } finally {
                        try {
                            if (out != null) {
                                out.close();
                            }
                        } catch (IOException ex) {
                        }
                        try {
                            bos.close();
                        } catch (IOException ex) {
                        }
                    }
                    break;
            }
        } catch (JsonProcessingException ex) {
            Logger.getLogger(TuioSink.class.getName()).log(Level.SEVERE, null, ex);
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
        broadcast("add", tobj);
    }

    /**
     * Update Object Event.
     *
     * @param tobj
     */
    @Override
    public void updateTuioObject(TuioObject tobj) {
        logger.debug(MessageFormat.format("update tuio object symbol id: {0}", tobj.getSymbolID()));
        broadcast("update", tobj);
    }

    /**
     * Remove Object Event.
     *
     * @param tobj
     */
    @Override
    public void removeTuioObject(TuioObject tobj) {
        logger.debug(MessageFormat.format("remove tuio object symbol id: {0}", tobj.getSymbolID()));
        broadcast("remove", tobj);
    }

    /**
     * Refresh Event.
     *
     * @param bundleTime
     */
    @Override
    public void refresh(TuioTime bundleTime) {
        logger.debug(MessageFormat.format("refresh frame id: {0}", bundleTime.getFrameID()));
        broadcast("refresh", bundleTime);
    }

    /**
     * Add Cursor Event.
     *
     * @param tcur
     */
    @Override
    public void addTuioCursor(TuioCursor tcur) {
        logger.trace(MessageFormat.format("add tuio cursor id: {0}", tcur.getCursorID()));
        broadcast("add", tcur);
    }

    /**
     * Update Cursor Event.
     *
     * @param tcur
     */
    @Override
    public void updateTuioCursor(TuioCursor tcur) {
        logger.trace(MessageFormat.format("update tuio cursor id: {0}", tcur.getCursorID()));
        broadcast("update", tcur);
    }

    /**
     * Remove Cursor Event.
     *
     * @param tcur
     */
    @Override
    public void removeTuioCursor(TuioCursor tcur) {
        logger.trace(MessageFormat.format("remove tuio cursor id: {0}", tcur.getCursorID()));
        broadcast("remove", tcur);
    }

    /**
     * Add Blob Event.
     *
     * @param tblb
     */
    @Override
    public void addTuioBlob(TuioBlob tblb) {
        logger.debug(MessageFormat.format("Added Blob: {0}", tblb.getBlobID()));
        broadcast("add", tblb);
    }

    /**
     * Update Blob Event.
     *
     * @param tblb
     */
    @Override
    public void updateTuioBlob(TuioBlob tblb) {
        logger.debug(MessageFormat.format("Update Blob: {0}", tblb.getBlobID()));
        broadcast("update", tblb);
    }

    /**
     * Remove Blob Event.
     *
     * @param tblb
     */
    @Override
    public void removeTuioBlob(TuioBlob tblb) {
        logger.debug(MessageFormat.format("Remove Blob: {0}", tblb.getBlobID()));
        broadcast("remove", tblb);
    }

    public SerializeType getSerializationType() {
        return serialization;
    }

    public void setSerializationType(SerializeType value) {
        serialization = value;
    }
}
