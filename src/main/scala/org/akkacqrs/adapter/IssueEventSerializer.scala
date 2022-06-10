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

import java.io.{ ByteArrayOutputStream, NotSerializableException }
import akka.serialization.SerializerWithStringManifest
import com.sksamuel.avro4s.{ AvroInputStream, AvroOutputStream, AvroSchema }

final class IssueEventSerializer extends SerializerWithStringManifest {
  import org.akkacqrs.write.IssueRepository._

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

  override def toBinary(o: AnyRef): Array[Byte] = {
    val byteArrayStream = new ByteArrayOutputStream()
    o match {
      case serializable: Serializable =>
        serializable match {
          case ic @ IssueCreated(_, _, _, _, _) =>
            serialize(ic, byteArrayStream, AvroOutputStream.binary[IssueCreated].to(byteArrayStream).build())
          case iu @ IssueUpdated(_, _, _, _) =>
            serialize(iu, byteArrayStream, AvroOutputStream.binary[IssueUpdated].to(byteArrayStream).build())
          case ic @ IssueClosed(_, _) =>
            serialize(ic, byteArrayStream, AvroOutputStream.binary[IssueClosed].to(byteArrayStream).build())
          case id @ IssueDeleted(_, _) =>
            serialize(id, byteArrayStream, AvroOutputStream.binary[IssueDeleted].to(byteArrayStream).build())
        }
      case _ => throw new IllegalArgumentException(s"Unknown class: ${o.getClass}!")
    }
  }

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef =
    manifest match {
      case IssueCreatedManifest =>
        deserialize(AvroInputStream.binary[IssueCreated].from(bytes).build(AvroSchema[IssueCreated]))
      case IssueUpdatedManifest =>
        deserialize(AvroInputStream.binary[IssueUpdated].from(bytes).build(AvroSchema[IssueUpdated]))
      case IssueClosedManifest =>
        deserialize(AvroInputStream.binary[IssueClosed].from(bytes).build(AvroSchema[IssueClosed]))
      case IssueDeletedManifest =>
        deserialize(AvroInputStream.binary[IssueDeleted].from(bytes).build(AvroSchema[IssueDeleted]))
      case _ => throw new NotSerializableException(manifest)
    }

  private def serialize[T](t: T, byteArrayOutputStream: ByteArrayOutputStream, os: AvroOutputStream[T]): Array[Byte] = {
    os.write(t)
    os.close()
    byteArrayOutputStream.toByteArray
  }

  private def deserialize[T](is: AvroInputStream[T]): T = {
    val obj = is.iterator.toSet
    is.close()
    obj.head
  }
}
