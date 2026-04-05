package com.nguyenxuanviet.backend.service;

import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Service;

@Service
public class MqttPublisherService {

    private final MessageChannel mqttOutboundChannel;

    public MqttPublisherService(MessageChannel mqttOutboundChannel) {
        this.mqttOutboundChannel = mqttOutboundChannel;
    }

    public void publish(String topic, String payload) {
        mqttOutboundChannel.send(
                MessageBuilder.withPayload(payload)
                        .setHeader("mqtt_topic", topic)
                        .build()
        );
    }
}
