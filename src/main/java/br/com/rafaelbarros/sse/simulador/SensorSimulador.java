package br.com.rafaelbarros.sse.simulador;

import java.util.Random;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import br.com.rafaelbarros.sse.model.LeituraTemperatura;
import br.com.rafaelbarros.sse.service.EventoService;

/**
 * Simula leituras periódicas de três sensores de temperatura e as
 * publica como eventos SSE do tipo "temperatura".
 */
@Component
public class SensorSimulador {

    private final EventoService eventoService;

    private final Random random = new Random();
    private final String[] sensores = {"sala", "server", "externo"};

    public SensorSimulador(EventoService eventoService) {
        this.eventoService = eventoService;
    }

    @Scheduled(fixedRate = 2000) // a cada 2 segundos
    public void emitirLeituras() {
        for (String sensor : sensores) {
            LeituraTemperatura leitura = new LeituraTemperatura(
                    sensor,
                    20.0 + random.nextDouble() * 10,
                    System.currentTimeMillis());

            eventoService.publicar("temperatura", leitura);
        }
    }
}
