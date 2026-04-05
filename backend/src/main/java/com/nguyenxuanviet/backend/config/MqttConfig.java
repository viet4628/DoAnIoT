package com.nguyenxuanviet.backend.config;

import com.nguyenxuanviet.backend.model.Device;
import com.nguyenxuanviet.backend.model.Telemetry;
import com.nguyenxuanviet.backend.repository.DeviceRepository;
import com.nguyenxuanviet.backend.repository.TelemetryRepository;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

import java.time.LocalDateTime;
import java.util.List;

@Configuration
@IntegrationComponentScan
public class MqttConfig {

    @Value("${mqtt.broker.url:tcp://localhost:1883}")
    private String brokerUrl;

    @Value("${mqtt.client.id:doaniot-backend-client}")
    private String clientId;

    private final DeviceRepository deviceRepository;
    private final TelemetryRepository telemetryRepository;

    public MqttConfig(DeviceRepository deviceRepository, TelemetryRepository telemetryRepository) {
        this.deviceRepository = deviceRepository;
        this.telemetryRepository = telemetryRepository;
    }

    // ─── Shared client factory ───────────────────────────────────────────────

    @Bean
    public DefaultMqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[]{brokerUrl});
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        options.setConnectionTimeout(10);
        factory.setConnectionOptions(options);
        return factory;
    }

    // ─── Outbound: publish to MQTT ───────────────────────────────────────────

    @Bean
    public MessageChannel mqttOutboundChannel() {
        return new DirectChannel();
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttOutboundChannel")
    public MessageHandler mqttOutbound() {
        MqttPahoMessageHandler handler =
                new MqttPahoMessageHandler(clientId + "-outbound", mqttClientFactory());
        handler.setAsync(true);
        handler.setDefaultTopic("/default/control");
        return handler;
    }

    // ─── Inbound: subscribe to device status topics ──────────────────────────

    @Bean
    public MqttPahoMessageDrivenChannelAdapter mqttInboundAdapter() {
        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(
                        clientId + "-inbound", mqttClientFactory());
        adapter.setCompletionTimeout(5000);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(1);
        adapter.setOutputChannel(mqttInboundChannel());
        return adapter;
    }

    @Bean
    public MessageChannel mqttInboundChannel() {
        return new DirectChannel();
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttInboundChannel")
    public MessageHandler mqttInboundHandler() {
        return (Message<?> message) -> {
            try {
                String topic   = (String) message.getHeaders().get("mqtt_receivedTopic");
                String payload = message.getPayload().toString().trim();
                System.out.println("[MQTT] Received on '" + topic + "': " + payload);

                deviceRepository.findByStatusTopic(topic).ifPresentOrElse(
                    device -> {
                        boolean isOn = "0".equals(payload);
                        device.setOn(isOn);
                        device.setOnline(true);
                        deviceRepository.save(device);

                        Telemetry t = new Telemetry();
                        t.setDevice(device);
                        t.setValue(payload);
                        t.setTimestamp(LocalDateTime.now());
                        telemetryRepository.save(t);

                        System.out.println("[DB] Device '" + device.getName() + "' isOn=" + isOn);
                    },
                    () -> System.out.println("[MQTT] No device for topic: " + topic)
                );
            } catch (Exception e) {
                System.err.println("[MQTT] Error processing message: " + e.getMessage());
            }
        };
    }

    @EventListener(ApplicationReadyEvent.class)
    public void subscribeToExistingDeviceTopics() {
        try {
            List<Device> devices = deviceRepository.findAll();
            for (Device device : devices) {
                String topic = device.getStatusTopic();
                if (topic != null && !topic.isBlank()) {
                    try {
                        mqttInboundAdapter().addTopic(topic, 1);
                        System.out.println("[STARTUP] Subscribed to: " + topic);
                    } catch (Exception e) {
                        System.err.println("[STARTUP] Could not subscribe to '" + topic + "': " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[STARTUP] MQTT subscription init failed: " + e.getMessage());
        }
    }
}
