package com.shieldgate.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String QUEUE = "threat-events-queue";
    public static final String EXCHANGE = "threat-events-exchange";
    public static final String ROUTING_KEY = "threat.event";

    @Bean
    public Queue threatEventsQueue() {
        return new Queue(QUEUE, true);
    }

    @Bean
    public DirectExchange threatEventsExchange() {
        return new DirectExchange(EXCHANGE);
    }

    @Bean
    public Binding binding(Queue threatEventsQueue, DirectExchange threatEventsExchange) {
        return BindingBuilder.bind(threatEventsQueue)
                .to(threatEventsExchange)
                .with(ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
