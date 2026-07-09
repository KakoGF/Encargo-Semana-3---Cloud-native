package cl.duoc.guias.service;

import java.time.LocalDateTime;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import cl.duoc.guias.dto.GuiaDespachoMessage;
import cl.duoc.guias.model.ResumenGuia;
import cl.duoc.guias.repository.ResumenGuiaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GuiaConsumidorService {

	private final ResumenGuiaRepository resumenGuiaRepository;

	@RabbitListener(queues = "${app.rabbitmq.guias.queue}")
	public void recibirResumen(GuiaDespachoMessage mensaje) {
		log.info("Mensaje recibido desde la cola para la guia {}", mensaje.getNumeroGuia());

		if (resumenGuiaRepository.existsByNumeroGuia(mensaje.getNumeroGuia())) {
			log.warn("La guia {} ya fue procesada anteriormente, se descarta el mensaje", mensaje.getNumeroGuia());
			return;
		}

		ResumenGuia resumen = new ResumenGuia();
		resumen.setNumeroGuia(mensaje.getNumeroGuia());
		resumen.setTransportista(mensaje.getTransportista());
		resumen.setFecha(mensaje.getFecha());
		resumen.setDestinatario(mensaje.getDestinatario());
		resumen.setDireccionDespacho(mensaje.getDireccionDespacho());
		resumen.setTotalProductos(mensaje.getTotalProductos());
		resumen.setNombreArchivo(mensaje.getNombreArchivo());
		resumen.setKeyS3(mensaje.getKeyS3());
		resumen.setFechaCreacion(mensaje.getFechaCreacion());
		resumen.setFechaProcesado(LocalDateTime.now());

		ResumenGuia guardado = resumenGuiaRepository.save(resumen);
		log.info("Resumen de la guia {} guardado en la tabla resumen_guias con id={}",
				guardado.getNumeroGuia(), guardado.getId());
	}
}
