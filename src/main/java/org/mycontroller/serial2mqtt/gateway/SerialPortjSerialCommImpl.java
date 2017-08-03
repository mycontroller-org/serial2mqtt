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

import java.util.concurrent.atomic.AtomicBoolean;

import org.mycontroller.serial2mqtt.AppConfig;
import org.mycontroller.serial2mqtt.model.Configuration;
import org.mycontroller.standalone.gateway.IGateway;
import org.mycontroller.standalone.gateway.model.Gateway;
import org.mycontroller.standalone.message.RawMessage;

import com.fazecast.jSerialComm.SerialPort;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Jeeva Kandasamy (jkandasa)
 * @since 1.0.0
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SerialPortjSerialCommImpl implements Runnable, IGateway {
    private SerialPort serialPort;
    private Configuration config = null;
    private static SerialPortjSerialCommImpl _instance = new SerialPortjSerialCommImpl();
    public static final AtomicBoolean TERMINATE = new AtomicBoolean(false);
    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);

    public static SerialPortjSerialCommImpl getInstance() {
        return _instance;
    }

    public void write(RawMessage rawMessage) {
        try {
            serialPort.writeBytes(rawMessage.getGWBytes(), rawMessage.getGWBytes().length);
            _logger.debug("{} written on serial", rawMessage);
        } catch (Exception ex) {
            _logger.error("Error: {}", rawMessage, ex);
        }
    }

    public void close() {
        if (this.serialPort.closePort()) {
            _logger.debug("serialPort{} closed", serialPort.getDescriptivePortName());
        } else {
            _logger.warn("Failed to close serialPort{}", serialPort.getDescriptivePortName());
        }
    }

    private void initialize() {
        RUNNING.set(false);
        SerialPort[] serialPorts = SerialPort.getCommPorts();
        _logger.debug("Number of serial port available:{}", serialPorts.length);
        for (int portNo = 0; portNo < serialPorts.length; portNo++) {
            _logger.debug("SerialPort[{}]:[{},{}]", portNo + 1, serialPorts[portNo].getSystemPortName(),
                    serialPorts[portNo].getDescriptivePortName());
        }

        // create an instance of the serial communications class
        serialPort = SerialPort.getCommPort(config.getSerial().getPort());

        serialPort.openPort();//Open port
        if (!serialPort.isOpen()) {
            _logger.error("Unable to open serial port:[{}]", config.getSerial().getPort());
            return;
        }
        serialPort.setComPortParameters(
                config.getSerial().getBaudRate(),
                8,  // data bits
                SerialPort.ONE_STOP_BIT,
                SerialPort.NO_PARITY);

        // create and register the serial data listener
        serialPort.addDataListener(new SerialDataListenerjSerialComm(serialPort, config));
        _logger.debug("Serial port initialized with {}", config.getSerial());
        RUNNING.set(true);
    }

    @Override
    public Gateway getGateway() {
        return null;
    }

    @Override
    public void run() {
        config = AppConfig.getInstance().getConfiguration();
        boolean mainLoop = true;
        while (mainLoop) {
            if (!RUNNING.get()) {
                initialize();
            }

            long sleepTime = config.getSerial().getRetryFrequency() * 1000L;
            while (sleepTime > 0) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    _logger.error("Exception,", ex);
                }
                sleepTime -= 100L;
                if (TERMINATE.get()) {
                    if (RUNNING.get()) {
                        this.close();
                    }
                    mainLoop = false;
                    break;
                }
            }
        }
    }

}
