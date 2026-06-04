package com.laundry.config;

import com.laundry.event.OrderEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.Map;

/**
 * Kafka Producer Configuration — Stream-Based Application
 *
 * Spring IoC: all @Bean methods are managed by Spring container.
 * KafkaTemplate is injected wherever needed via constructor injection.
 */
@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${laundry.kafka.topic.order-events}")
    private String orderEventsTopic;

    // Spring IoC: @Bean — KafkaTemplate<String, OrderEvent>
    @Bean
    public ProducerFactory<String, OrderEvent> orderProducerFactory() {
        return new DefaultKafkaProducerFactory<>(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,  bootstrapServers,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,   StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class,
                ProducerConfig.ACKS_CONFIG,               "all",
                ProducerConfig.RETRIES_CONFIG,             3,
                ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true,
                JsonSerializer.ADD_TYPE_INFO_HEADERS,     false
        ));
    }

    @Bean
    public KafkaTemplate<String, OrderEvent> kafkaTemplate() {
        return new KafkaTemplate<>(orderProducerFactory());
    }

    // Auto-create topic with 3 partitions for parallel consumption
    @Bean
    public NewTopic orderEventsTopic() {
        return TopicBuilder.name(orderEventsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
