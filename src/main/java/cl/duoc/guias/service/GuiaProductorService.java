package cl.duoc.guias.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import cl.duoc.guias.dto.GuiaDespachoMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GuiaProductorService {

	private final RabbitTemplate rabbitTemplate;

	@Value("${app.rabbitmq.guias.exchange}")
	private String exchange;

	@Value("${app.rabbitmq.guias.routing-key}")
	private String routingKey;

	public void enviarResumen(GuiaDespachoMessage mensaje) {
		log.info("Publicando resumen de la guia {} en el exchange {}", mensaje.getNumeroGuia(), exchange);
		rabbitTemplate.convertAndSend(exchange, routingKey, mensaje);
	}
}
