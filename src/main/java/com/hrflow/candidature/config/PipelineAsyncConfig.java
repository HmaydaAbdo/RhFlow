package com.hrflow.candidature.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Executor dédié au pipeline IA CV.
 *
 * Les appels LLM sont I/O-bound (réseau), pas CPU-bound → on peut avoir
 * plusieurs threads en attente simultanément sans saturer le CPU.
 *
 * corePoolSize=2    : 2 CVs traités en parallèle en régime normal
 * maxPoolSize=5     : jusqu'à 5 threads en pic de charge
 * queueCapacity=100 : 100 uploads en attente avant activation de la CallerRunsPolicy
 * CallerRunsPolicy  : si la queue est pleine, le thread HTTP appelant exécute la tâche
 *                     → back-pressure naturelle, pas de rejet brutal des uploads
 */
@Configuration
public class PipelineAsyncConfig {

    @Bean("pipelineExecutor")
    public TaskExecutor pipelineExecutor() {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(2);
        exec.setMaxPoolSize(5);
        exec.setQueueCapacity(100);
        exec.setThreadNamePrefix("cv-pipeline-");
        exec.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        exec.initialize();
        return exec;
    }
}
