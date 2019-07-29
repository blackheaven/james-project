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

package org.apache.james.task

import java.time.ZonedDateTime
import java.util.Optional

import org.apache.james.task.TaskManager.Status._

object TaskExecutionDetails {

  trait AdditionalInformation {}

  def from(task: Task, id: TaskId) = new TaskExecutionDetails(id, task.`type`, task.details, WAITING,
    Optional.of(ZonedDateTime.now), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty())
}

case class TaskExecutionDetails(taskId: TaskId, `type`: String,
                                additionalInformation: Optional[TaskExecutionDetails.AdditionalInformation],
                                status: TaskManager.Status,
                                submitDate: Optional[ZonedDateTime] = Optional.empty(),
                                startedDate: Optional[ZonedDateTime] = Optional.empty(),
                                completedDate: Optional[ZonedDateTime] = Optional.empty(),
                                canceledDate: Optional[ZonedDateTime] = Optional.empty(),
                                failedDate: Optional[ZonedDateTime] = Optional.empty()) {
  def getTaskId: TaskId = taskId

  def getType: String = `type`

  def getStatus: TaskManager.Status = status

  def getAdditionalInformation: Optional[TaskExecutionDetails.AdditionalInformation] = additionalInformation

  def getSubmitDate: Optional[ZonedDateTime] = submitDate

  def getStartedDate: Optional[ZonedDateTime] = startedDate

  def getCompletedDate: Optional[ZonedDateTime] = completedDate

  def getCanceledDate: Optional[ZonedDateTime] = canceledDate

  def getFailedDate: Optional[ZonedDateTime] = failedDate

  def started: TaskExecutionDetails = status match {
    case WAITING => start
    case _ => this
  }

  def completed: TaskExecutionDetails = status match {
    case IN_PROGRESS => complete
    case CANCEL_REQUESTED => complete
    case WAITING => complete
    case _ => this
  }

  def failed: TaskExecutionDetails = status match {
    case IN_PROGRESS => fail
    case CANCEL_REQUESTED => fail
    case _ => this
  }

  def cancelRequested: TaskExecutionDetails = status match {
    case IN_PROGRESS => requestCancel
    case WAITING => requestCancel
    case _ => this
  }

  def cancelEffectively: TaskExecutionDetails = status match {
    case CANCEL_REQUESTED => cancel
    case IN_PROGRESS => cancel
    case WAITING => cancel
    case _ => this
  }

  private def start = new TaskExecutionDetails(taskId, `type`, additionalInformation, IN_PROGRESS,
    submitDate = submitDate,
    startedDate = Optional.of(ZonedDateTime.now))
  private def complete = new TaskExecutionDetails(taskId, `type`, additionalInformation, TaskManager.Status.COMPLETED,
    submitDate = submitDate,
    startedDate = startedDate,
    completedDate = Optional.of(ZonedDateTime.now))
  private def fail = new TaskExecutionDetails(taskId, `type`, additionalInformation, TaskManager.Status.FAILED,
    submitDate = submitDate,
    startedDate = startedDate,
    Optional.of(ZonedDateTime.now))
  private def requestCancel = new TaskExecutionDetails(taskId, `type`, additionalInformation, TaskManager.Status.CANCEL_REQUESTED,
    submitDate = submitDate,
    startedDate = startedDate,
    canceledDate = Optional.of(ZonedDateTime.now))
  private def cancel = new TaskExecutionDetails(taskId, `type`, additionalInformation, TaskManager.Status.CANCELLED,
    submitDate = submitDate,
    startedDate = startedDate,
    canceledDate = Optional.of(ZonedDateTime.now))
}
