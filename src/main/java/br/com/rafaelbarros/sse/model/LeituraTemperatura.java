package br.com.rafaelbarros.sse.model;

/**
 * Representa uma leitura de temperatura emitida por um sensor.
 *
 * @param sensor    nome do sensor (sala, server ou externo)
 * @param valor     temperatura em graus Celsius
 * @param timestamp instante da leitura em milissegundos (epoch)
 */
public record LeituraTemperatura(String sensor, double valor, long timestamp) {
}
