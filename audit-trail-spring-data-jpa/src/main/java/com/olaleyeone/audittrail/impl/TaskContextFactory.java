package com.olaleyeone.audittrail.impl;

import com.olaleyeone.audittrail.context.Action;
import com.olaleyeone.audittrail.embeddable.Duration;
import com.olaleyeone.audittrail.entity.Task;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

@RequiredArgsConstructor
public class TaskContextFactory {

    final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final TaskContextHolder taskContextHolder;
    private final TaskTransactionContextFactory taskTransactionContextFactory;
    private final TaskContextSaver taskContextSaver;

    public TaskContextImpl start(Task task) {
        TaskContextImpl taskContext = new TaskContextImpl(task, null, taskContextHolder, taskTransactionContextFactory);
        taskContextHolder.registerContext(taskContext);
        return taskContext;
    }

    public Task startBackgroundTask(String name, String description, Action action) {
        Task task = new Task();
        task.setType(Task.BACKGROUND_JOB);
        task.setName(name);
        task.setDescription(description);
        Duration duration = Duration.builder()
                .startedOn(OffsetDateTime.now())
                .build();
        task.setDuration(duration);
        TaskContextImpl taskContext = start(task);
        try {
            action.execute();
        } catch (Throwable ex) {
            logger.error(ex.getMessage(), ex);
        } finally {
            duration.setNanoSecondsTaken(duration.getStartedOn().until(OffsetDateTime.now(), ChronoUnit.NANOS));
            taskContextSaver.save(taskContext);
        }
        return task;
    }

}
