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

import org.apache.commons.lang.RandomStringUtils;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.mycontroller.serial2mqtt.AppConfig;
import org.mycontroller.serial2mqtt.model.MqttConfig;
import org.mycontroller.standalone.gateway.IGateway;
import org.mycontroller.standalone.gateway.model.Gateway;
import org.mycontroller.standalone.message.RawMessage;
import org.mycontroller.standalone.provider.mc.McpUtils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Jeeva Kandasamy (jkandasa)
 * @since 1.0.0
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class McMqttClient implements Runnable, IGateway {

    private static McMqttClient _instance = new McMqttClient();

    public static final long DISCONNECT_TIME_OUT = 1000 * 3;
    public static final int CONNECTION_TIME_OUT = 1000 * 5;
    public static final int KEEP_ALIVE = 1000 * 5;

    private IMqttClient mqttClient;
    private McMqttCallbackListener mcMqttCallbackListener;
    private boolean isRunning = false;
    private boolean terminate = false;

    public static McMqttClient getInstance() {
        return _instance;
    }

    public void startMqttClient() {
        if (isRunning()) {
            _logger.info("MQTT client already running. Nothing to do..");
            return;
        }
        try {
            MqttConfig _config = AppConfig.getInstance().getConfiguration().getMqtt();
            mqttClient = new MqttClient(_config.getBroker(), _config.getClientId() + "_"
                    + RandomStringUtils.randomAlphanumeric(5));
            MqttConnectOptions connectOptions = new MqttConnectOptions();
            connectOptions.setConnectionTimeout(CONNECTION_TIME_OUT);
            connectOptions.setKeepAliveInterval(KEEP_ALIVE);
            if (_config.getUsername() != null && _config.getUsername().length() > 0) {
                connectOptions.setUserName(_config.getUsername());
                connectOptions.setPassword(_config.getPassword().toCharArray());
            }
            mqttClient.connect(connectOptions);
            mcMqttCallbackListener = new McMqttCallbackListener(mqttClient, connectOptions);
            mqttClient.setCallback(mcMqttCallbackListener);
            mqttClient.subscribe(_config.getTopicSubscribe() + "/#");
            _logger.info("MQTT client[server: {}, topicSubscribe:{}] connected successfully..",
                    mqttClient.getServerURI(), _config.getTopicSubscribe());
            isRunning = true;
        } catch (MqttException ex) {
            _logger.error("Unable to connect with MQTT broker [{}], Reason Code: {}, ",
                    mqttClient.getServerURI(), ex.getReasonCode(), ex);
        }
    }

    public synchronized void publish(String topic, String payload) {
        if (payload == null) {
            payload = McpUtils.EMPTY_DATA;
        }
        _logger.debug("Message about to send, Topic:[{}], Payload:[{}]", topic, payload);
        try {
            MqttMessage message = new MqttMessage(payload.getBytes());
            message.setQos(AppConfig.getInstance().getConfiguration().getMqtt().getQos());
            mqttClient.publish(topic, message);
        } catch (MqttException ex) {
            if (ex.getMessage().contains("Timed out waiting for a response from the server")) {
                _logger.debug(ex.getMessage());
            } else {
                _logger.error("Exception, Reason Code:{}", ex.getReasonCode(), ex);
            }
        }
    }

    public void stop() {
        try {
            if (mcMqttCallbackListener != null) {
                mcMqttCallbackListener.stopReconnect();
            }
            if (mqttClient != null) {
                if (mqttClient.isConnected()) {
                    mqttClient.disconnect(DISCONNECT_TIME_OUT);
                }
                mqttClient.close();
            }
            isRunning = false;
        } catch (Exception ex) {
            _logger.error("Exception,", ex);
        }
    }

    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public void run() {
        try {
            while (!isTerminate() && !isRunning()) {
                startMqttClient();
                long waitTime = AppConfig.getInstance().getConfiguration().getMqtt().getRetryFrequency() * 1000L;
                while (!isTerminate() && waitTime > 0) {
                    try {
                        Thread.sleep(100);
                        waitTime -= 100;
                    } catch (InterruptedException ex) {
                        _logger.error("Error,", ex);
                    }
                }
            }
        } catch (Exception ex) {
            _logger.error("Exception, ", ex);
        }

    }

    public boolean isTerminate() {
        return terminate;
    }

    public void setTerminate(boolean terminate) {
        this.terminate = terminate;
    }

    @Override
    public void close() {
        stop();

    }

    @Override
    public Gateway getGateway() {
        return null;
    }

    @Override
    public void write(RawMessage rawMessage) {
        publish(rawMessage.getSubData(), (String) rawMessage.getData());

    }

}
