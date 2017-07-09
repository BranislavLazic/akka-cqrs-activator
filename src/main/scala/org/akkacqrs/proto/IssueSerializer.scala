/*
 * Copyright 2017 Branislav Lazic
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.akkacqrs.proto

import java.io.NotSerializableException
import java.time.{ Instant, LocalDate, LocalDateTime }
import java.util.UUID

import akka.serialization.SerializerWithStringManifest
import org.akkacqrs.IssueRepository
import org.akkacqrs.IssueRepository._
import org.akkacqrs.proto.issue.{
  Timestamp,
  IssueClosed => IssueClosedPb,
  IssueCreated => IssueCreatedPb,
  IssueDeleted => IssueDeletedPb,
  IssueUpdated => IssueUpdatedPb
}

final class IssueSerializer extends SerializerWithStringManifest {

  override def identifier: Int = getClass.getName.hashCode

  private final val IssueCreatedManifest = "IssueCreated"
  private final val IssueUpdatedManifest = "IssueUpdated"
  private final val IssueClosedManifest  = "IssueClosed"
  private final val IssueDeletedManifest = "IssueDeleted"

  override def manifest(o: AnyRef): String = o match {
    case serializable: Serializable =>
      serializable match {
        case _: IssueCreated => IssueCreatedManifest
        case _: IssueUpdated => IssueUpdatedManifest
        case _: IssueClosed  => IssueClosedManifest
        case _: IssueDeleted => IssueDeletedManifest
      }
  }

  override def toBinary(o: AnyRef): Array[Byte] = {

    def getUUIDPb(uuid: UUID): Option[org.akkacqrs.proto.issue.UUID] =
      Some(org.akkacqrs.proto.issue.UUID(uuid.toString))

    def getTimestampPb(date: LocalDate): Option[Timestamp] = {
      import java.time.ZoneOffset
      val instant = date.atStartOfDay.toInstant(ZoneOffset.UTC)
      Some(Timestamp(instant.getEpochSecond, instant.getNano))
    }

    val protobuf = o match {
      case serializable: Serializable =>
        serializable match {
          case IssueCreated(id, summary, description, date, status) =>
            IssueCreatedPb(getUUIDPb(id), summary, description, getTimestampPb(date), status.toString)

          case IssueClosed(id, date) => IssueClosedPb(getUUIDPb(id), getTimestampPb(date))

          case IssueUpdated(id, summary, description, date) =>
            IssueUpdatedPb(getUUIDPb(id), summary, description, getTimestampPb(date))

          case IssueDeleted(id, date) => IssueDeletedPb(getUUIDPb(id), getTimestampPb(date))
        }
      case _ => throw new IllegalArgumentException(s"Unknown class: ${ o.getClass }!")
    }
    protobuf.toByteArray
  }

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = {

    def getIssueStatus(status: String) = status match {
      case IssueRepository.ClosedStatus => IssueClosedStatus
      case IssueRepository.OpenedStatus => IssueOpenedStatus
    }

    def timestampToLocalDate(timestamp: Timestamp): LocalDate = {
      import java.time.ZoneId
      LocalDateTime
        .ofInstant(Instant.ofEpochSecond(timestamp.epochSecond, timestamp.nano), ZoneId.of("UTC"))
        .toLocalDate
    }

    def getIssueCreated(issueCreatedPb: IssueCreatedPb) = {
      IssueCreated(UUID.fromString(issueCreatedPb.id.get.value),
                   issueCreatedPb.summary,
                   issueCreatedPb.description,
                   timestampToLocalDate(issueCreatedPb.date.get),
                   getIssueStatus(issueCreatedPb.issueStatus))
    }

    def getIssueUpdated(issueUpdatedPb: IssueUpdatedPb) =
      IssueUpdated(UUID.fromString(issueUpdatedPb.id.get.value),
                   issueUpdatedPb.summary,
                   issueUpdatedPb.description,
                   timestampToLocalDate(issueUpdatedPb.date.get))

    def getIssueClosed(issueClosedPb: IssueClosedPb) =
      IssueClosed(UUID.fromString(issueClosedPb.id.get.value), timestampToLocalDate(issueClosedPb.date.get))

    def getIssueDeleted(issueDeletedPb: IssueDeletedPb) =
      IssueDeleted(UUID.fromString(issueDeletedPb.id.get.value), timestampToLocalDate(issueDeletedPb.date.get))

    manifest match {
      case IssueCreatedManifest => getIssueCreated(IssueCreatedPb.parseFrom(bytes))
      case IssueUpdatedManifest => getIssueUpdated(IssueUpdatedPb.parseFrom(bytes))
      case IssueClosedManifest  => getIssueClosed(IssueClosedPb.parseFrom(bytes))
      case IssueDeletedManifest => getIssueDeleted(IssueDeletedPb.parseFrom(bytes))
      case _                    => throw new NotSerializableException(manifest)
    }
  }
}
