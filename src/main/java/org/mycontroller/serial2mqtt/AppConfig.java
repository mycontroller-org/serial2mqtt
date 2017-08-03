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

import java.io.IOException;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.mycontroller.serial2mqtt.model.Configuration;
import org.mycontroller.serial2mqtt.model.MqttConfig;
import org.mycontroller.serial2mqtt.model.SerialPortConfig;
import org.mycontroller.standalone.AppProperties.NETWORK_TYPE;
import org.mycontroller.standalone.gateway.GatewayUtils.SERIAL_PORT_DRIVER;
import org.mycontroller.standalone.utils.McUtils;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Jeeva Kandasamy (jkandasa)
 * @since 1.0.0
 */

@ToString
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Slf4j
public class AppConfig {
    public static final String APPLICATION_NAME = "serial2mqtt";
    private static AppConfig _instance = new AppConfig();

    private String tmpLocation;
    private String serviceConfFile;
    private String appDirectory;
    private Properties properties;
    private Configuration configuration;

    public static AppConfig getInstance() {
        return _instance;
    }

    public void loadProperties(Properties properties) throws IOException {
        this.properties = properties;
        //Application Directory
        try {
            appDirectory = McUtils.getDirectoryLocation(FileUtils.getFile(McUtils.getDirectoryLocation("../"))
                    .getCanonicalPath());
        } catch (IOException ex) {
            appDirectory = McUtils.getDirectoryLocation("../");
            _logger.error("Unable to set application directory!", ex);
        }
        //Create tmp location
        tmpLocation = McUtils.getDirectoryLocation(appDirectory + "/tmp");

        createDirectoryLocation(tmpLocation);
        configuration = Configuration
                .builder()
                .networkType(NETWORK_TYPE.valueOf(property("serial2mqtt.network.type", "MY_SENSORS").toUpperCase()))
                .messageQueueSize(Integer.valueOf(property("serial2mqtt.message.queue.size", "1000")))
                .serial(SerialPortConfig
                        .builder()
                        .driver(SERIAL_PORT_DRIVER.valueOf(property("serial.driver", "AUTO").toUpperCase()))
                        .baudRate(Integer.valueOf(property("serial.baud.rate", "115200")))
                        .port(property("serial.port", "/dev/ttyUSB0"))
                        .retryFrequency(Integer.valueOf(property("serial.connection.retry", "60")))
                        .build())
                .mqtt(MqttConfig.builder()
                        .qos(Integer.valueOf(property("mqtt.qos", "0")))
                        .clientId(property("mqtt.client.id", "serial2mqtt-adapter"))
                        .broker(property("mqtt.broker", "tcp://localhost:1883"))
                        .topicPublish(property("mqtt.topic.publish", "mygateway-out"))
                        .topicSubscribe(property("mqtt.topic.subscribe", "mygateway-in"))
                        .username(property("mqtt.username", null))
                        .password(property("mqtt.password", null))
                        .retryFrequency(Integer.valueOf(property("mqtt.connection.retry", "60")))
                        .build())
                .build();

    }

    private String property(String key, String defaultValue) {
        if (this.properties.getProperty(key) != null) {
            return this.properties.getProperty(key);
        }
        return defaultValue;
    }

    private void createDirectoryLocation(String directoryLocation) {
        if (!FileUtils.getFile(directoryLocation).exists()) {
            if (FileUtils.getFile(directoryLocation).mkdirs()) {
                _logger.info("Created directory location: {}", directoryLocation);
            } else {
                _logger.error("Unable to create directory location: {}", directoryLocation);
            }
        }
    }

}
