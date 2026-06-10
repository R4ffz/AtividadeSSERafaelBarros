package br.com.rafaelbarros.sse.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import br.com.rafaelbarros.sse.service.EventoService;

/**
 * Endpoint SSE. O Content-Type {@code text/event-stream} ativa o
 * protocolo Server-Sent Events na resposta HTTP.
 */
@RestController
@RequestMapping("/sensores")
@CrossOrigin(origins = "*")
public class SensorController {

    private final EventoService eventoService;

    public SensorController(EventoService eventoService) {
        this.eventoService = eventoService;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId) {

        SseEmitter emitter = new SseEmitter(-1L); // -1 = sem timeout

        eventoService.registrar(emitter, lastEventId);

        // Callbacks para limpeza ao encerrar a conexão
        emitter.onCompletion(() -> eventoService.remover(emitter));
        emitter.onTimeout(() -> eventoService.remover(emitter));
        emitter.onError((e) -> eventoService.remover(emitter));

        return emitter;
    }
}
