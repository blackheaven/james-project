/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.task;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public interface TaskManager {
    boolean FINISHED = true;

    enum Status {
        WAITING("waiting", !FINISHED),
        IN_PROGRESS("inProgress", !FINISHED),
        CANCEL_REQUESTED("canceledRequested", !FINISHED),
        COMPLETED("completed", FINISHED),
        CANCELLED("canceled", FINISHED),
        FAILED("failed", FINISHED);

        public static Status fromString(String value) {
            return Arrays.stream(values())
                .filter(status -> status.value.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                    String.format("Unknown status value '%s'", value)));
        }

        private final String value;
        private final boolean finished;

        Status(String value, boolean finished) {
            this.value = value;
            this.finished = finished;
        }

        public String getValue() {
            return value;
        }

        public boolean isFinished() {
            return finished;
        }
    }

    interface AwaitedTaskExecutionDetails {
        AwaitedTaskExecutionDetails onUnknown(Supplier<RuntimeException> exceptionSupplier);

        AwaitedTaskExecutionDetails onTimeout(Supplier<RuntimeException> exceptionSupplier);

        TaskExecutionDetails unwrap();
    }

    class UnknownAwaitedTaskExecutionDetails implements AwaitedTaskExecutionDetails {
        public AwaitedTaskExecutionDetails onUnknown(Supplier<RuntimeException> exceptionSupplier) {
            throw exceptionSupplier.get();
        }

        public AwaitedTaskExecutionDetails onTimeout(Supplier<RuntimeException> exceptionSupplier) {
            return this;
        }

        public TaskExecutionDetails unwrap() {
            throw new RuntimeException("await has failed due to unknown task");
        }
    }

    class TimeoutAwaitedTaskExecutionDetails implements AwaitedTaskExecutionDetails {
        public AwaitedTaskExecutionDetails onUnknown(Supplier<RuntimeException> exceptionSupplier) {
            return this;
        }

        public AwaitedTaskExecutionDetails onTimeout(Supplier<RuntimeException> exceptionSupplier) {
            throw exceptionSupplier.get();
        }

        public TaskExecutionDetails unwrap() {
            throw new RuntimeException("await has failed due to timeout");
        }
    }

    class TerminatedAwaitedTaskExecutionDetails implements AwaitedTaskExecutionDetails {
        private final TaskExecutionDetails executionDetails;

        public TerminatedAwaitedTaskExecutionDetails(TaskExecutionDetails executionDetails) {
            this.executionDetails = executionDetails;
        }

        public AwaitedTaskExecutionDetails onUnknown(Supplier<RuntimeException> exceptionSupplier) {
            return this;
        }

        public AwaitedTaskExecutionDetails onTimeout(Supplier<RuntimeException> exceptionSupplier) {
            return this;
        }

        public TaskExecutionDetails unwrap() {
            return executionDetails;
        }
    }

    TaskId submit(Task task);

    TaskExecutionDetails getExecutionDetails(TaskId id);

    List<TaskExecutionDetails> list();

    List<TaskExecutionDetails> list(Status status);

    void cancel(TaskId id);

    AwaitedTaskExecutionDetails await(TaskId id, Duration timeout);
}
