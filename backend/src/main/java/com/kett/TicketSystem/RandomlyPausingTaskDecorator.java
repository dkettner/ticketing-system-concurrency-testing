package com.kett.TicketSystem;

import org.slf4j.Logger;
import org.springframework.core.task.TaskDecorator;

import java.util.Random;

public class RandomlyPausingTaskDecorator implements TaskDecorator {
    Logger logger = org.slf4j.LoggerFactory.getLogger(RandomlyPausingTaskDecorator.class);

    @Override
    public Runnable decorate(Runnable runnable) {
        return () -> {
            try {
                Random random = new Random();
                if (random.nextBoolean()) {
                    int pauseTime = 600; //random.nextInt(600);
                    logger.trace("Pausing thread for " + pauseTime + " ms");
                    Thread.sleep(pauseTime);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            runnable.run();
        };
    }
}