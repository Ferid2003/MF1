package farid.aghazada.resource_service.RabbitMQ;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public Exchange exchange(@Value("${rabbitmq.exchange}") String exchange) {
        return new DirectExchange(exchange);
    }
}
