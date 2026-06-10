package br.com.rafaelbarros.sse.service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Mantém a lista de conexões SSE ativas e centraliza o envio de eventos.
 *
 * A lista de emitters é uma {@link CopyOnWriteArrayList} para garantir
 * thread-safety: o broadcast (@Async), o heartbeat (@Scheduled) e o
 * registro de novos clientes (threads HTTP) acessam-na concorrentemente.
 */
@Service
public class EventoService {

    private static final Logger log = LoggerFactory.getLogger(EventoService.class);

    /** Quantidade de eventos retidos para replay via Last-Event-ID. */
    private static final int TAMANHO_HISTORICO = 50;

    // CopyOnWriteArrayList garante thread-safety
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    /** Sequência monotônica usada como campo "id" dos eventos. */
    private final AtomicLong sequencia = new AtomicLong();

    /** Últimos eventos publicados, para replay na reconexão (Last-Event-ID). */
    private final Deque<EventoArmazenado> historico = new ArrayDeque<>();

    private record EventoArmazenado(long id, String tipo, Object payload) {
    }

    /**
     * Registra um novo cliente. Se o browser reenviou o header
     * Last-Event-ID, os eventos perdidos desde aquele ID são
     * retransmitidos antes de o emitter entrar no broadcast.
     */
    public void registrar(SseEmitter emitter, String lastEventId) {
        if (lastEventId != null) {
            reenviarPerdidos(emitter, lastEventId);
        }
        emitters.add(emitter);
        log.info("Cliente conectado (Last-Event-ID={}). Total: {}", lastEventId, emitters.size());
    }

    public void remover(SseEmitter emitter) {
        emitters.remove(emitter);
        log.info("Cliente desconectado. Total: {}", emitters.size());
    }

    /**
     * Publica um evento para todos os clientes conectados, em uma
     * thread do pool "sse-" (não bloqueia a thread chamadora).
     */
    @Async
    public void publicar(String tipo, Object payload) {
        long id = sequencia.incrementAndGet();
        armazenar(new EventoArmazenado(id, tipo, payload));

        SseEmitter.SseEventBuilder evento = SseEmitter.event()
                .id(String.valueOf(id))
                .name(tipo)
                .data(payload)
                .reconnectTime(3000);

        broadcast(evento);
    }

    /**
     * Comentário SSE periódico (": heartbeat") para impedir que proxies
     * e firewalls encerrem a conexão por inatividade.
     */
    @Scheduled(fixedRate = 20000)
    public void heartbeat() {
        if (emitters.isEmpty()) {
            return;
        }
        broadcast(SseEmitter.event().comment("heartbeat"));
    }

    /**
     * Envia o evento a todos os emitters. Emitters cujo cliente já se
     * desconectou lançam exceção no send() — comportamento esperado —
     * e são removidos da lista para evitar vazamento de memória.
     */
    private void broadcast(SseEmitter.SseEventBuilder evento) {
        List<SseEmitter> mortos = new ArrayList<>();
        for (SseEmitter em : emitters) {
            try {
                em.send(evento);
            } catch (Exception e) {
                mortos.add(em); // Conexão encerrada
            }
        }
        emitters.removeAll(mortos);
    }

    private synchronized void armazenar(EventoArmazenado evento) {
        historico.addLast(evento);
        if (historico.size() > TAMANHO_HISTORICO) {
            historico.removeFirst();
        }
    }

    private void reenviarPerdidos(SseEmitter emitter, String lastEventId) {
        long ultimoRecebido;
        try {
            ultimoRecebido = Long.parseLong(lastEventId);
        } catch (NumberFormatException e) {
            return;
        }

        List<EventoArmazenado> perdidos;
        synchronized (this) {
            perdidos = historico.stream()
                    .filter(ev -> ev.id() > ultimoRecebido)
                    .toList();
        }

        for (EventoArmazenado ev : perdidos) {
            try {
                emitter.send(SseEmitter.event()
                        .id(String.valueOf(ev.id()))
                        .name(ev.tipo())
                        .data(ev.payload()));
            } catch (Exception e) {
                return; // cliente caiu durante o replay
            }
        }
        log.info("Replay de {} evento(s) a partir do ID {}", perdidos.size(), ultimoRecebido);
    }
}
