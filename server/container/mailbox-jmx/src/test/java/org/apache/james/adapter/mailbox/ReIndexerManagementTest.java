package org.apache.james.adapter.mailbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.indexer.ReIndexer;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.task.Task;
import org.apache.james.task.TaskId;
import org.apache.james.task.TaskManager;
import org.junit.Before;
import org.junit.Test;

public class ReIndexerManagementTest {
    private ReIndexerManagement testee;
    private TaskManager taskManager;
    private ReIndexer reIndexer;
    
    @Before
    public void setUp() {
        taskManager = mock(TaskManager.class);
        reIndexer = mock(ReIndexer.class);
        testee = new ReIndexerManagement(taskManager, reIndexer);
    }
    
    @Test
    public void reIndexMailboxWaitsForExecution() throws MailboxException {
        TaskId taskId = TaskId.generateTaskId();
        Task task = mock(Task.class);
        String namespace = "namespace";
        String user = "user";
        String name = "name";
        when(reIndexer.reIndex(any(MailboxPath.class))).thenReturn(task);
        when(taskManager.submit(task)).thenReturn(taskId);

        assertThat(testee.reIndex(namespace, user, name)).isSameAs(taskId);
        verify(reIndexer).reIndex(new MailboxPath(namespace, user, name));
        verify(taskManager).submit(task);
        verify(taskManager).await(taskId);
    }

    @Test
    public void reIndexWaitsForExecution() throws MailboxException {
        TaskId taskId = TaskId.generateTaskId();
        Task task = mock(Task.class);
        when(reIndexer.reIndex()).thenReturn(task);
        when(taskManager.submit(task)).thenReturn(taskId);

        assertThat(testee.reIndex()).isSameAs(taskId);
        verify(reIndexer).reIndex();
        verify(taskManager).submit(task);
        verify(taskManager).await(taskId);
    }
}
