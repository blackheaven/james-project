/** **************************************************************
  * Licensed to the Apache Software Foundation (ASF) under one   *
  * or more contributor license agreements.  See the NOTICE file *
  * distributed with this work for additional information        *
  * regarding copyright ownership.  The ASF licenses this file   *
  * to you under the Apache License, Version 2.0 (the            *
  * "License"); you may not use this file except in compliance   *
  * with the License.  You may obtain a copy of the License at   *
  * *
  * http://www.apache.org/licenses/LICENSE-2.0                 *
  * *
  * Unless required by applicable law or agreed to in writing,   *
  * software distributed under the License is distributed on an  *
  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
  * KIND, either express or implied.  See the License for the    *
  * specific language governing permissions and limitations      *
  * under the License.                                           *
  * ***************************************************************/
package org.apache.james.task.eventsourcing

import org.apache.james.eventsourcing.eventstore.History
import org.apache.james.eventsourcing.{CommandHandler, Event}
import org.apache.james.task.eventsourcing.TaskCommand._
import org.apache.james.task.{Hostname, TaskId}
import org.reactivestreams.Publisher

import reactor.core.scala.publisher.{SFlux, SMono}

sealed abstract class TaskCommandHandler[T <: TaskCommand] extends CommandHandler[T] {

  def loadAggregate(loadHistory: TaskAggregateId => SMono[History], taskId: TaskId): SMono[TaskAggregate] = {
    val aggregateId = TaskAggregateId(taskId)
    loadHistory(aggregateId).map(TaskAggregate.fromHistory(aggregateId, _))
  }
}

class CreateCommandHandler(private val loadHistory: TaskAggregateId => SMono[History], hostname: Hostname) extends TaskCommandHandler[Create] {
  override def handledClass: Class[Create] = classOf[Create]

  override def handle(command: Create): Publisher[_ <: Event] = {
    SFlux.fromIterable(TaskAggregate.create(TaskAggregateId(command.id), command.task, hostname))
  }
}

class StartCommandHandler(private val loadHistory: TaskAggregateId => SMono[History],
                          private val hostname: Hostname) extends TaskCommandHandler[Start] {
  override def handledClass: Class[Start] = classOf[Start]

  override def handle(command: Start): Publisher[_ <: Event] = {
    loadAggregate(loadHistory, command.id).map(_.start(hostname)).flatMapMany(SFlux.fromIterable)
  }
}

class RequestCancelCommandHandler(private val loadHistory: TaskAggregateId => SMono[History],
                                  private val hostname: Hostname) extends TaskCommandHandler[RequestCancel] {
  override def handledClass: Class[RequestCancel] = classOf[RequestCancel]

  override def handle(command: RequestCancel): Publisher[_ <: Event] = {
    loadAggregate(loadHistory, command.id).map(_.requestCancel(hostname)).flatMapMany(SFlux.fromIterable)
  }
}

class CompleteCommandHandler(private val loadHistory: TaskAggregateId => SMono[History]) extends TaskCommandHandler[Complete] {
  override def handledClass: Class[Complete] = classOf[Complete]

  override def handle(command: Complete): Publisher[_ <: Event] = {
    loadAggregate(loadHistory, command.id).map(_.complete(command.result, command.additionalInformation)).flatMapMany(SFlux.fromIterable)
  }
}

class CancelCommandHandler(private val loadHistory: TaskAggregateId => SMono[History]) extends TaskCommandHandler[Cancel] {
  override def handledClass: Class[Cancel] = classOf[Cancel]

  override def handle(command: Cancel): Publisher[_ <: Event] = {
    loadAggregate(loadHistory, command.id).map(_.cancel(command.additionalInformation)).flatMapMany(SFlux.fromIterable)
  }
}

class FailCommandHandler(private val loadHistory: TaskAggregateId => SMono[History]) extends TaskCommandHandler[Fail] {
  override def handledClass: Class[Fail] = classOf[Fail]

  override def handle(command: Fail): Publisher[_ <: Event] = {
    loadAggregate(loadHistory, command.id).map(_.fail(command.additionalInformation, command.errorMessage, command.exception)).flatMapMany(SFlux.fromIterable)
  }
}

class UpdateCommandHandler(private val loadHistory: TaskAggregateId => SMono[History]) extends TaskCommandHandler[UpdateAdditionalInformation] {
  override def handledClass: Class[UpdateAdditionalInformation] = classOf[UpdateAdditionalInformation]

  override def handle(command: UpdateAdditionalInformation): Publisher[_ <: Event] = {
    loadAggregate(loadHistory, command.id).map(_.update(command.additionalInformation)).flatMapMany(SFlux.fromIterable)
  }
}