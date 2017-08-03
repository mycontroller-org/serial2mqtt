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
package org.mycontroller.serial2mqtt;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import org.mycontroller.serial2mqtt.gateway.McMqttClient;
import org.mycontroller.serial2mqtt.gateway.MessageMonitorThread;
import org.mycontroller.serial2mqtt.gateway.SerialPortjSerialCommImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Jeeva Kandasamy (jkandasa)
 * @since 1.0.0
 */
@Slf4j
public class StartService {
    public static void main(String[] args) {
        try {
            long start = System.currentTimeMillis();
            loadInitialProperties();
            _logger.debug("App configuration: {}", AppConfig.getInstance().toString());
            About about = new About();
            _logger.debug("{}", about);
            startServices();
            _logger.info("{} service started successfully in [{}] ms", AppConfig.APPLICATION_NAME,
                    System.currentTimeMillis() - start);

        } catch (Exception ex) {
            _logger.error("Unable to start {} the service, refer error log,", AppConfig.APPLICATION_NAME, ex);
            System.exit(1);//Terminate jvm, with non zero
        }
    }

    public static boolean loadInitialProperties() {
        String propertiesFile = System.getProperty("serial2mqtt.conf.file");
        try {
            Properties properties = new Properties();
            if (propertiesFile == null) {
                properties
                        .load(ClassLoader.getSystemClassLoader().getResourceAsStream("serial2mqtt.properties"));

            } else {
                properties.load(new FileReader(propertiesFile));
            }
            AppConfig.getInstance().loadProperties(properties);
            _logger.debug("Properties are loaded successfuly...");
            return true;
        } catch (IOException ex) {
            _logger.error("Exception while loading properties file, ", ex);
            return false;
        }
    }

    public static void startServices() {
        // Start service order
        // - Add Shutdown hook
        // - Start message Monitor Thread
        // - Start Serial communication
        // - Start MQTT communication

        //Add Shutdown hook
        new ShutdownHook().attachShutDownHook();

        //Start message Monitor Thread
        //Create new thread to monitor received logs
        MessageMonitorThread messageMonitorThread = new MessageMonitorThread();
        new Thread(messageMonitorThread).start();

        // Start Serial Communication
        new Thread(SerialPortjSerialCommImpl.getInstance()).start();

        // Start MQTT communication
        new Thread(McMqttClient.getInstance()).start();
    }

    public static void stopServices() {
        // Stop service order
        // - Stop MQTT communication
        // - Stop Serial communication
        // - Stop message Monitor Thread
        // - stop device(s) services

        // Stop MQTT communication
        McMqttClient.getInstance().close();

        // Stop Serial communication
        SerialPortjSerialCommImpl.getInstance().close();

        //Stop Message monitor thread
        MessageMonitorThread.shutdown();

    }

}
