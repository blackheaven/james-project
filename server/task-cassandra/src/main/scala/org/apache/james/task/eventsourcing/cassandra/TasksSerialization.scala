/** **************************************************************
  * Licensed to the Apache Software Foundation (ASF) under one   *
  * or more contributor license agreements.  See the NOTICE file *
  * distributed with this work for additional information        *
  * regarding copyright ownership.  The ASF licenses this file   *
  * to you under the Apache License, Version 2.0 (the            *
  * "License"); you may not use this file except in compliance   *
  * with the License.  You may obtain a copy of the License at   *
  *                                                              *
  * http://www.apache.org/licenses/LICENSE-2.0                   *
  *                                                              *
  * Unless required by applicable law or agreed to in writing,   *
  * software distributed under the License is distributed on an  *
  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
  * KIND, either express or implied.  See the License for the    *
  * specific language governing permissions and limitations      *
  * under the License.                                           *
  * ***************************************************************/

package org.apache.james.task.eventsourcing.cassandra

import org.apache.james.eventsourcing.eventstore.cassandra.JsonEventSerializer
import org.apache.james.eventsourcing.eventstore.cassandra.dto.{EventDTO, EventDTOModule}
import org.apache.james.eventsourcing.{Event, EventId}
import org.apache.james.server.task.json.JsonTaskSerializer
import org.apache.james.task.eventsourcing._
import org.apache.james.task.{Task, TaskId}
import play.api.libs.json._

object DTO {
  implicit def scopeTaskAggregateIdReads: Reads[TaskAggregateId] = str => str.validate[String].map(value => TaskAggregateId(TaskId.fromString(value)))
  implicit def scopeTaskAggregateIdWrite: Writes[TaskAggregateId] = taskAggregateId => JsString(taskAggregateId.taskId.asString())
  implicit val scopeTaskAggregateIdFormat: Format[TaskAggregateId] = Format(scopeTaskAggregateIdReads, scopeTaskAggregateIdWrite)

  implicit def scopeEventIdReads: Reads[EventId] = str => str.validate[Int].map(value => EventId.fromSerialized(value))
  implicit def scopeEventIdWrite: Writes[EventId] = eventId => JsNumber(eventId.serialize())
  implicit val scopeEventIdFormat: Format[EventId] = Format(scopeEventIdReads, scopeEventIdWrite)

  implicit def scopeResultReads: Reads[Task.Result] = str => str.validate[String].map {
    case "COMPLETED" => Task.Result.COMPLETED
    case "PARTIAL" => Task.Result.PARTIAL
  }
  implicit def scopeResultWrite: Writes[Task.Result] = {
    case Task.Result.COMPLETED => JsString("COMPLETED")
    case Task.Result.PARTIAL => JsString("PARTIAL")
  }
  implicit val scopeResultFormat: Format[Task.Result] = Format(scopeResultReads, scopeResultWrite)

  case class SerializedTask(task: String) extends AnyVal
  object SerializedTask {
    implicit def reads: Reads[SerializedTask] = str => str.validate[JsObject].map(value => SerializedTask(Json.stringify(value)))
    implicit def write: Writes[SerializedTask] = serializedTask => Json.parse(serializedTask.task).asInstanceOf[JsObject]
    implicit val format: Format[SerializedTask] = Format(reads, write)
  }

  sealed abstract class TaskEventDTO(typeName: String, aggregateId: TaskAggregateId, val eventId: EventId) extends EventDTO {
    def toDomainObject(serializer: JsonTaskSerializer): TaskEvent
  }

  case class CreatedDTO(typeName: String, aggregateId: TaskAggregateId, override val eventId: EventId, task: SerializedTask) extends TaskEventDTO(typeName, aggregateId, eventId) {
    override def toDomainObject(serializer: JsonTaskSerializer): Created = Created(aggregateId, eventId, serializer.deserialize(task.task))
  }

  object CreatedDTO {
    implicit val format: OFormat[CreatedDTO] = Json.format[CreatedDTO]
  }

  case class StartedDTO(typeName: String, aggregateId: TaskAggregateId, override val eventId: EventId) extends TaskEventDTO(typeName, aggregateId, eventId) {
    override def toDomainObject(serializer: JsonTaskSerializer): Started = Started(aggregateId, eventId)
  }

  object StartedDTO {
    implicit val format: OFormat[StartedDTO] = Json.format[StartedDTO]
  }

  case class CancelRequestedDTO(typeName: String, aggregateId: TaskAggregateId, override val eventId: EventId) extends TaskEventDTO(typeName, aggregateId, eventId) {
    override def toDomainObject(serializer: JsonTaskSerializer): CancelRequested = CancelRequested(aggregateId, eventId)
  }

  object CancelRequestedDTO {
    implicit val format: OFormat[CancelRequestedDTO] = Json.format[CancelRequestedDTO]
  }

  case class CompletedDTO(typeName: String, aggregateId: TaskAggregateId, override val eventId: EventId, result: Task.Result) extends TaskEventDTO(typeName, aggregateId, eventId) {
    override def toDomainObject(serializer: JsonTaskSerializer): Completed = Completed(aggregateId, eventId, result)
  }

  object CompletedDTO {
    implicit val format: OFormat[CompletedDTO] = Json.format[CompletedDTO]
  }

  case class FailedDTO(typeName: String, aggregateId: TaskAggregateId, override val eventId: EventId) extends TaskEventDTO(typeName, aggregateId, eventId) {
    override def toDomainObject(serializer: JsonTaskSerializer): Failed = Failed(aggregateId, eventId)
  }

  object FailedDTO {
    implicit val format: OFormat[FailedDTO] = Json.format[FailedDTO]
  }

  case class CancelledDTO(typeName: String, aggregateId: TaskAggregateId, override val eventId: EventId) extends TaskEventDTO(typeName, aggregateId, eventId) {
    override def toDomainObject(serializer: JsonTaskSerializer): Cancelled = Cancelled(aggregateId, eventId)
  }

  object CancelledDTO {
    implicit val format: OFormat[CancelledDTO] = Json.format[CancelledDTO]
  }
}

class JsonTaskEventSerializer(taskSerializer: JsonTaskSerializer, taskEventDTOModule: TaskEventDTOModule) extends JsonEventSerializer {
  implicit val reads: Reads[DTO.TaskEventDTO] = Json.reads[DTO.TaskEventDTO]

  override def serialize(event: Event): String = event.asInstanceOf[TaskEvent] match {
    case created : Created => Json.toJson(TaskSerializer.fromDomainObject(created, getTypeNameForEvent(created), taskSerializer)).toString()
    case started : Started => Json.toJson(TaskSerializer.fromDomainObject(started, getTypeNameForEvent(started))).toString()
    case cancelRequested: CancelRequested => Json.toJson(TaskSerializer.fromDomainObject(cancelRequested, getTypeNameForEvent(cancelRequested))).toString()
    case completed: Completed => Json.toJson(TaskSerializer.fromDomainObject(completed, getTypeNameForEvent(completed))).toString()
    case failed: Failed => Json.toJson(TaskSerializer.fromDomainObject(failed, getTypeNameForEvent(failed))).toString()
    case cancelled: Cancelled => Json.toJson(TaskSerializer.fromDomainObject(cancelled, getTypeNameForEvent(cancelled))).toString()
  }

  override def deserialize(value: String): Event = {
    val rawJson = Json.parse(value)
    val dto = rawJson.\("typeName").validate[String] match {
      case JsSuccess(taskEventDTOModule.createdTypeName, _) => Json.fromJson[DTO.CreatedDTO](rawJson)
      case JsSuccess(taskEventDTOModule.startedTypeName, _) => Json.fromJson[DTO.StartedDTO](rawJson)
      case JsSuccess(taskEventDTOModule.cancelRequestedTypeName, _) => Json.fromJson[DTO.CancelRequestedDTO](rawJson)
      case JsSuccess(taskEventDTOModule.completedTypeName, _) => Json.fromJson[DTO.CompletedDTO](rawJson)
      case JsSuccess(taskEventDTOModule.failedTypeName, _) => Json.fromJson[DTO.FailedDTO](rawJson)
      case JsSuccess(taskEventDTOModule.cancelledTypeName, _) => Json.fromJson[DTO.CancelledDTO](rawJson)
    }

     dto.map(_.toDomainObject(taskSerializer)).get
  }

  private def getTypeNameForEvent(event: TaskEvent): String = taskEventDTOModule.getTypeName(event)
}

object TaskSerializer {
  def fromDomainObject(event: Created, typeName: String, serializer: JsonTaskSerializer): DTO.CreatedDTO =
    new DTO.CreatedDTO(typeName, event.aggregateId, event.eventId, DTO.SerializedTask(serializer.serialize(event.task)))
  def fromDomainObject(event: Started, typeName: String): DTO.StartedDTO =
    new DTO.StartedDTO(typeName, event.aggregateId, event.eventId)
  def fromDomainObject(event: CancelRequested, typeName: String): DTO.CancelRequestedDTO =
    new DTO.CancelRequestedDTO(typeName, event.aggregateId, event.eventId)
  def fromDomainObject(event: Completed, typeName: String): DTO.CompletedDTO =
    new DTO.CompletedDTO(typeName, event.aggregateId, event.eventId, event.result)
  def fromDomainObject(event: Failed, typeName: String): DTO.FailedDTO =
    new DTO.FailedDTO(typeName, event.aggregateId, event.eventId)
  def fromDomainObject(event: Cancelled, typeName: String): DTO.CancelledDTO =
    new DTO.CancelledDTO(typeName, event.aggregateId, event.eventId)
}

class TaskEventDTOModule(created: EventDTOModule[Created, DTO.CreatedDTO],
                         started: EventDTOModule[Started, DTO.StartedDTO],
                         cancelRequested: EventDTOModule[CancelRequested, DTO.CancelRequestedDTO],
                         cancelled: EventDTOModule[Cancelled, DTO.CancelledDTO],
                         completed: EventDTOModule[Completed, DTO.CompletedDTO],
                         failed: EventDTOModule[Failed, DTO.FailedDTO]) {
  val createdTypeName = created.getDomainObjectType
  val startedTypeName = started.getDomainObjectType
  val cancelRequestedTypeName = cancelRequested.getDomainObjectType
  val cancelledTypeName = cancelled.getDomainObjectType
  val completedTypeName = completed.getDomainObjectType
  val failedTypeName = failed.getDomainObjectType

  def getTypeName(event: TaskEvent): String = event match {
    case _ : Created => createdTypeName
    case _ : Started => startedTypeName
    case _ : CancelRequested => cancelRequestedTypeName
    case _ : Completed => completedTypeName
    case _ : Failed => failedTypeName
    case _ : Cancelled => cancelledTypeName
  }
}