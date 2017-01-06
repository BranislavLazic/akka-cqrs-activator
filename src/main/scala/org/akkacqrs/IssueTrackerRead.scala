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

package org.akkacqrs

import java.util.UUID

import akka.actor.{ ActorLogging, ActorRef, Props }
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import akka.pattern._
import akka.persistence.query.{ EventEnvelope2, TimeBasedUUID }
import akka.persistence.query.scaladsl.EventsByTagQuery2
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import com.datastax.driver.core.ResultSet
import com.datastax.driver.core.utils.UUIDs
import org.akkacqrs.IssueTrackerAggregate._

object IssueTrackerRead {
  final val Name = "issue-tracker-read"

  sealed trait ReadCommand

  case object CreateKeyspace                                        extends ReadCommand
  case object CreateIssueTable                                      extends ReadCommand
  final case class GetIssueById(id: UUID)                           extends ReadCommand
  final case class CloseIssue(id: UUID)                             extends ReadCommand
  final case class UpdateDescription(id: UUID, description: String) extends ReadCommand

  sealed trait ReadEvent

  final case object TableCreated extends ReadEvent

  def props(publishSubscribeMediator: ActorRef, readJournal: EventsByTagQuery2) =
    Props(new IssueTrackerRead(publishSubscribeMediator, readJournal))
}

class IssueTrackerRead(publishSubscribeMediator: ActorRef, readJournal: EventsByTagQuery2)
    extends CassandraActor
    with ActorLogging {

  import Settings.CassandraDb._
  import IssueTrackerRead._
  import context.dispatcher

  implicit val materializer = ActorMaterializer()
  readJournal
    .eventsByTag("issue-tracker-tag", TimeBasedUUID(UUIDs.timeBased()))
    .map {
      case EventEnvelope2(_, _, _, event: IssueTrackerEvent) => event
    }
    .runWith(Sink.actorRef(self, "completed"))

  final val CreateKeyspaceStatement: String =
    s"""
       |CREATE KEYSPACE IF NOT EXISTS $keyspace
       |  WITH REPLICATION = { 'class' : '$keyspaceReplicationStrategy', 'replication_factor' : $keyspaceReplicationFactor };
    """.stripMargin

  final val CreateTableStatement: String =
    s"""
       |CREATE TABLE IF NOT EXISTS $keyspace.issues (
       |  id uuid,
       |  description text,
       |  date_updated date,
       |  issue_status text,
       |  PRIMARY KEY (id, date_updated)
       |) WITH CLUSTERING ORDER BY (date_updated ASC)
       |""".stripMargin

  final val InsertStatement: String =
    s"""
      |INSERT INTO $keyspace.issues (id, description, date_updated, status)
      | VALUES (?, ?, ?, ?)
    """.stripMargin

  final val SelectByIdStatement: String =
    s"""
      | SELECT * FROM $keyspace.issues WHERE id = ?
    """.stripMargin

  override def receive: Receive = {
    case CreateKeyspace =>
      session.executeAsync(CreateKeyspaceStatement).toFuture pipeTo self
    case rs: ResultSet =>
      log.info(rs.getExecutionInfo.getStatement.toString)
      context.become(keyspaceInitialized)
      self ! CreateIssueTable
  }

  private def keyspaceInitialized: Receive = {
    case CreateIssueTable => session.executeAsync(CreateTableStatement).toFuture pipeTo self
    case rs: ResultSet =>
      log.info(rs.getExecutionInfo.getStatement.toString)
      context.become(ready)
      context.parent ! TableCreated
  }

  private def ready: Receive = {
    case event: IssueCreated => publishSubscribeMediator ! Publish(className[IssueTrackerEvent], event)
//      session.executeAsync(InsertIntoTableStatement, event.id, event.description, event.date)
    case event: IssueClosed => publishSubscribeMediator ! Publish(className[IssueTrackerEvent], event)
    case event: IssueDescriptionUpdated =>
      publishSubscribeMediator ! Publish(className[IssueTrackerEvent], event)
    case event: IssueDeleted =>
      publishSubscribeMediator ! Publish(className[IssueTrackerEvent], event)

    case GetIssueById(id)        => session.executeAsync(SelectByIdStatement, id).toFuture pipeTo sender()
    case CloseIssue(_)           =>
    case UpdateDescription(_, _) =>
  }
}
