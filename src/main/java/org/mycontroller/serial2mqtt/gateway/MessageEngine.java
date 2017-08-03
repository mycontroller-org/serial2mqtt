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

import org.mycontroller.serial2mqtt.AppConfig;
import org.mycontroller.standalone.message.RawMessage;
import org.mycontroller.standalone.message.RawMessageException;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Jeeva Kandasamy (jkandasa)
 * @since 1.0.0
 */

@Slf4j
public class MessageEngine {
    private StringBuilder builder = new StringBuilder();
    private String topicPublish = null;

    public MessageEngine() {
        topicPublish = AppConfig.getInstance().getConfiguration().getMqtt().getTopicPublish();
        if (!topicPublish.endsWith("/")) {
            topicPublish += "/";
        }
    }

    public void postOnMqtt(RawMessage rawMessage) throws RawMessageException {
        String gateWayMessage = (String) rawMessage.getData();
        String payload = "";
        if (gateWayMessage.endsWith("\n")) {
            gateWayMessage = gateWayMessage.substring(0, gateWayMessage.length() - 1);
        }
        String[] msgArry = gateWayMessage.split(";");
        if (msgArry.length == 6) {
            payload = msgArry[5];
        }
        if (msgArry.length >= 5) {
            Integer nodeId = Integer.valueOf(msgArry[0]);
            Integer childSensorId = Integer.valueOf(msgArry[1]);
            Integer messageType = Integer.valueOf(msgArry[2]);
            Integer ack = Integer.valueOf(msgArry[3]);
            Integer subType = Integer.valueOf(msgArry[4]);

            builder.setLength(0);
            builder.append(topicPublish).append(nodeId).append("/").append(childSensorId).append("/")
                    .append(messageType).append("/").append(ack).append("/").append(subType);
            McMqttClient.getInstance().publish(builder.toString(), payload);
        } else {
            _logger.debug("Unknown message format: [{}]", gateWayMessage);
            throw new RawMessageException("Unknown message format:[" + gateWayMessage + "]");
        }

    }

    public void postOnSerial(RawMessage rawMessage) throws RawMessageException {
        // Topic structure:
        // MY_MQTT_TOPIC_PREFIX/NODE-KEY_ID/SENSOR_VARIABLE-KEY_ID/CMD-OPERATION_TYPE/ACK-FLAG/SUB-OPERATION_TYPE
        String[] msgArry = rawMessage.getSubData().split("/");
        int index = msgArry.length - 5;
        if (msgArry.length >= 6) {
            Integer nodeId = Integer.valueOf(msgArry[index]);
            Integer childSensorId = Integer.valueOf(msgArry[index + 1]);
            Integer messageType = Integer.valueOf(msgArry[index + 2]);
            Integer ack = Integer.valueOf(msgArry[index + 3]);
            Integer subType = Integer.valueOf(msgArry[index + 4]);

            builder.setLength(0);
            builder.append(nodeId).append(";").append(childSensorId).append(";").append(messageType).append(";")
                    .append(ack).append(";").append(subType).append(";").append(rawMessage.getData()).append("\n");
            SerialPortjSerialCommImpl.getInstance().write(
                    RawMessage.builder().data(builder.toString()).networkType(rawMessage.getNetworkType()).build());
        } else {
            _logger.debug("Unknown message format, Topic:[{}], PayLoad:[{}]", rawMessage.getSubData(),
                    rawMessage.getData());
            throw new RawMessageException("Unknown message format, Topic:" + rawMessage.getSubData() + ", PayLoad:"
                    + rawMessage.getData());
        }
    }
}
