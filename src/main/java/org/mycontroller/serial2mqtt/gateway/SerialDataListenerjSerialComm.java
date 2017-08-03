/*
 * Copyright 2017 Jeeva Kandasamy (jkandasa@gmail.com)
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mycontroller.serial2mqtt.gateway;

import org.mycontroller.serial2mqtt.model.Configuration;
import org.mycontroller.standalone.gateway.serialport.SerialPortCommon;
import org.mycontroller.standalone.message.RawMessage;
import org.mycontroller.standalone.utils.McUtils;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Jeeva Kandasamy (jkandasa)
 * @since 1.0.0
 */
@Slf4j
public class SerialDataListenerjSerialComm implements SerialPortDataListener {

    public static final int SERIAL_DATA_MAX_SIZE = 1000;
    private SerialPort serialPort;
    private Configuration config = null;
    private StringBuilder message = new StringBuilder();
    private boolean failedStatusWritten = false;

    public SerialDataListenerjSerialComm(SerialPort serialPort, Configuration config) {
        this.serialPort = serialPort;
        this.config = config;
    }

    @Override
    public int getListeningEvents() {
        return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
    }

    @Override
    public void serialEvent(SerialPortEvent event) {
        if (event.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE) {
            return;
        }
        try {
            byte[] buffer = new byte[serialPort.bytesAvailable()];
            serialPort.readBytes(buffer, buffer.length);
            for (byte b : buffer) {
                if ((b == SerialPortCommon.MESSAGE_SPLITTER) && message.length() > 0) {
                    String toProcess = message.toString();
                    _logger.debug("Received a message:[{}]", toProcess);
                    //Send Message to mqtt broker

                    if (toProcess.endsWith("\n")) {
                        toProcess = toProcess.substring(0, toProcess.length() - 1);
                    }

                    RawMessageQueue.getInstance().putMessage(RawMessage.builder()
                            .data(toProcess)
                            .networkType(config.getNetworkType())
                            .timestamp(System.currentTimeMillis())
                            .isTxMessage(false)
                            .build());
                    message.setLength(0);
                } else if (b != SerialPortCommon.MESSAGE_SPLITTER) {
                    _logger.trace("Received a char:[{}]", ((char) b));
                    message.append((char) b);
                } else if (message.length() >= SERIAL_DATA_MAX_SIZE) {
                    _logger.warn(
                            "Serial receive buffer size reached to MAX level[{} chars], "
                                    + "Now clearing the buffer. Existing data:[{}]",
                            SERIAL_DATA_MAX_SIZE, message.toString());
                    message.setLength(0);
                } else {
                    _logger.debug("Received MESSAGE_SPLITTER and current message length is ZERO! Nothing to do");
                }
            }
            failedStatusWritten = false;
        } catch (Exception ex) {
            if (ex.getMessage() != null) {
                _logger.error("Exception, ", ex);
            }
            if (!failedStatusWritten) {
                failedStatusWritten = true;
                _logger.error("Exception, ", ex);
            }
            message.setLength(0);
            try {
                //If serial port removed in between throws 'java.lang.NegativeArraySizeException: null' continuously
                //This continuous exception eats CPU heavily, to reduce CPU usage on this state added Thread.sleep
                Thread.sleep(McUtils.TEN_MILLISECONDS);
            } catch (InterruptedException tE) {
                _logger.error("Exception,", tE);
            }
        }

    }
}
