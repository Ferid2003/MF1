package farid.aghazada.resource_processor.RabbitMQ;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public Exchange exchange(@Value("${rabbitmq.exchange}") String exchange) {
        return new DirectExchange(exchange);
    }

    @Bean
    public Queue queue(@Value("${rabbitmq.queue}") String queue) {
        return new Queue(queue, true);
    }

    @Bean
    public Binding binding(Queue queue, Exchange exchange, @Value("${rabbitmq.routing-key}") String routingKey) {
        return BindingBuilder.bind(queue)
            .to(exchange)
            .with(routingKey)
            .noargs();
    }
}
