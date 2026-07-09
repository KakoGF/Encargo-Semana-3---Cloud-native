package cl.duoc.guias.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${app.rabbitmq.guias.queue}")
    private String colaPrincipal;

    @Value("${app.rabbitmq.guias.exchange}")
    private String exchangePrincipal;

    @Value("${app.rabbitmq.guias.routing-key}")
    private String routingKeyPrincipal;

    @Value("${app.rabbitmq.guias.dlq}")
    private String colaErrores;

    @Value("${app.rabbitmq.guias.dlx}")
    private String exchangeErrores;

    @Value("${app.rabbitmq.guias.dlq-routing-key}")
    private String routingKeyErrores;

    @Bean
    public DirectExchange exchangeGuias() {
        return new DirectExchange(exchangePrincipal, true, false);
    }

    @Bean
    public DirectExchange exchangeErrores() {
        return new DirectExchange(exchangeErrores, true, false);
    }

    @Bean
    public Queue colaGuias() {
        Map<String, Object> argumentos = new HashMap<>();
        argumentos.put("x-dead-letter-exchange", exchangeErrores);
        argumentos.put("x-dead-letter-routing-key", routingKeyErrores);
        return new Queue(colaPrincipal, true, false, false, argumentos);
    }

    @Bean
    public Queue colaErrores() {
        return new Queue(colaErrores, true, false, false);
    }

    @Bean
    public Binding enlaceColaGuias() {
        return BindingBuilder.bind(colaGuias()).to(exchangeGuias()).with(routingKeyPrincipal);
    }

    @Bean
    public Binding enlaceColaErrores() {
        return BindingBuilder.bind(colaErrores()).to(exchangeErrores()).with(routingKeyErrores);
    }

    @Bean
    public Jackson2JsonMessageConverter conversorJson() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate plantilla = new RabbitTemplate(connectionFactory);
        plantilla.setMessageConverter(conversorJson());
        plantilla.setExchange(exchangePrincipal);
        plantilla.setRoutingKey(routingKeyPrincipal);
        return plantilla;
    }
}
