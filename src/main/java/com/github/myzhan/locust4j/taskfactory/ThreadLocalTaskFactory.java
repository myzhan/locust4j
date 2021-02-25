package com.github.myzhan.locust4j.taskfactory;

import com.github.myzhan.locust4j.AbstractTask;

/**
 * @author myzhan
 * @since 1.0.13
 *
 * ThreadLocalTaskFactory is a implementation of AbstractTaskFactory.
 *
 * Because {@link com.github.myzhan.locust4j.Locust#run(AbstractTask...) } only accepts instances of AbstractTask and
 * uses them across threads, the implementation of AbstractTask must be thread-safe. It's not easy if we want to keep
 * some thread-local fields, like tcp connections.
 *
 * Now, we can use ThreadLocalTaskFactory. Instances of AbstractTask are created after the creation of ThreadPool and
 * made thread-local, so the implementation of AbstractTask can keep some thread-local fields.
 */
public abstract class ThreadLocalTaskFactory extends AbstractTaskFactory {

    private final ThreadLocal<AbstractTask> currentThreadTask = new ThreadLocal<>();

    @Override
    public void onStart() {
        AbstractTask task = currentThreadTask.get();
        if (null == task) {
            task = createTask();
            currentThreadTask.set(task);
        }
    }

    @Override
    public void execute() throws Exception {
        AbstractTask task = currentThreadTask.get();
        task.execute();
    }

    @Override
    public void onStop() {
        currentThreadTask.remove();
    }
}
