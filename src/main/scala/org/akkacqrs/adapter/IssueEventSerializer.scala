/*
 * Copyright 2018 Branislav Lazic
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

package org.akkacqrs.adapter

import java.io.NotSerializableException
import java.time.LocalDate
import java.util.UUID

import akka.serialization.SerializerWithStringManifest

final class IssueEventSerializer extends SerializerWithStringManifest {
  import cats.syntax.invariant._
  import org.akkacqrs.IssueRepository._
  import pbdirect._

  private implicit val localDateFormat: PBFormat[LocalDate] =
    PBFormat[String].imap(LocalDate.parse)(_.toString)

  private implicit val uuidFormat: PBFormat[UUID] =
    PBFormat[String].imap(UUID.fromString)(_.toString)

  override def identifier: Int = getClass.getName.hashCode

  private final val IssueCreatedManifest = "IssueCreated"
  private final val IssueUpdatedManifest = "IssueUpdated"
  private final val IssueClosedManifest  = "IssueClosed"
  private final val IssueDeletedManifest = "IssueDeleted"
  private final val EmptyManifest        = ""

  override def manifest(o: AnyRef): String = o match {
    case serializable: Serializable =>
      serializable match {
        case _: IssueCreated => IssueCreatedManifest
        case _: IssueUpdated => IssueUpdatedManifest
        case _: IssueClosed  => IssueClosedManifest
        case _: IssueDeleted => IssueDeletedManifest
        case _               => EmptyManifest
      }
  }

  override def toBinary(o: AnyRef): Array[Byte] =
    o match {
      case serializable: Serializable =>
        serializable match {
          case evt: IssueEvent => evt.toPB
        }
      case _ => throw new IllegalArgumentException(s"Unknown class: ${o.getClass}!")
    }

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef =
    manifest match {
      case IssueCreatedManifest => bytes.pbTo[IssueCreated]
      case IssueUpdatedManifest => bytes.pbTo[IssueUpdated]
      case IssueClosedManifest  => bytes.pbTo[IssueClosed]
      case IssueDeletedManifest => bytes.pbTo[IssueDeleted]
      case _                    => throw new NotSerializableException(manifest)
    }
}
