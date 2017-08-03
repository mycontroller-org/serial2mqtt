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

import java.util.ArrayList;

import org.mycontroller.serial2mqtt.AppConfig;
import org.mycontroller.standalone.message.RawMessage;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Jeeva Kandasamy (jkandasa)
 * @since 1.0.0
 */
@Slf4j
public class RawMessageQueue {
    private ArrayList<RawMessage> rawMessagesQueue;
    private static final int INITAL_SIZE = 30;
    private Integer MAX_QUEUE_SIZE;

    //Do not load until some calls getInstance
    private static class RawMessageQueueHelper {
        private static final RawMessageQueue INSTANCE = new RawMessageQueue();
    }

    public static RawMessageQueue getInstance() {
        return RawMessageQueueHelper.INSTANCE;
    }

    private RawMessageQueue() {
        rawMessagesQueue = new ArrayList<RawMessage>(INITAL_SIZE);
        MAX_QUEUE_SIZE = AppConfig.getInstance().getConfiguration().getMessageQueueSize();
    }

    public synchronized void putMessage(RawMessage rawMessage) {
        if (rawMessagesQueue.size() < MAX_QUEUE_SIZE) {
            rawMessagesQueue.add(rawMessage);
            _logger.debug("Added {}, queue size:{}", rawMessage, rawMessagesQueue.size());
        } else {
            _logger.warn("Dropped {}, queue size:{}", rawMessage, rawMessagesQueue.size());
        }

    }

    public synchronized RawMessage getMessage() {
        if (!rawMessagesQueue.isEmpty()) {
            RawMessage rawMessage = rawMessagesQueue.get(0);
            rawMessagesQueue.remove(0);
            _logger.debug("Removed a {}, queue size:{}", rawMessage, rawMessagesQueue.size());
            return rawMessage;
        } else {
            _logger.warn("There is no message in the queue, returning null");
            return null;
        }
    }

    public int getQueueSize() {
        return rawMessagesQueue.size();
    }

    public synchronized boolean isEmpty() {
        return rawMessagesQueue.isEmpty();
    }
}
