package br.com.rafaelbarros.sse.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Habilita o processamento assíncrono e define o pool de threads
 * utilizado pelos métodos anotados com {@code @Async}.
 *
 * O envio de eventos SSE ocorre nessas threads, sem bloquear
 * as threads HTTP do servidor.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(5);
        exec.setMaxPoolSize(20);
        exec.setQueueCapacity(100);
        exec.setThreadNamePrefix("sse-");
        exec.initialize();
        return exec;
    }
}
