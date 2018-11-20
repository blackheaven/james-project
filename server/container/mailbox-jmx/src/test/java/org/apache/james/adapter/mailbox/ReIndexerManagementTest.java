package org.apache.james.adapter.mailbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.indexer.ReIndexer;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.task.MemoryTaskManager;
import org.apache.james.task.Task;
import org.apache.james.task.TaskId;
import org.apache.james.task.TaskManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ReIndexerManagementTest {
    private ReIndexerManagement testee;
    private TaskManager taskManager;
    private ReIndexer reIndexer;

    @BeforeEach
    void setUp() {
        taskManager = new MemoryTaskManager();
        reIndexer = mock(ReIndexer.class);
        testee = new ReIndexerManagement(taskManager, reIndexer);
    }

    @Test
    void reIndexMailboxWaitsForExecution() throws MailboxException {
        TaskId.generateTaskId();
        Task task = mock(Task.class);
        String namespace = "namespace";
        String user = "user";
        String name = "name";
        when(reIndexer.reIndex(any(MailboxPath.class))).thenReturn(task);

        assertThat(taskManager.list()).isEmpty();
        testee.reIndex(namespace, user, name);
        verify(reIndexer).reIndex(new MailboxPath(namespace, user, name));
        assertThat(taskManager.list()).hasSize(1);
    }

    @Test
    void reIndexWaitsForExecution() throws MailboxException {
        Task task = mock(Task.class);
        when(reIndexer.reIndex()).thenReturn(task);

        assertThat(taskManager.list()).isEmpty();
        testee.reIndex();
        verify(reIndexer).reIndex();
        assertThat(taskManager.list()).hasSize(1);
    }
}
