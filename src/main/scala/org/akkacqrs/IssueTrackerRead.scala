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

import java.time.LocalDate
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

/**
  * Creates "read side" Cassandra keyspace and table.
  * Subscribes to the IssueTrackerEvent's from mediator and manages issues in Cassandra.
  */
object IssueTrackerRead {
  final val Name = "issue-tracker-read"

  sealed trait ReadCommand

  case object CreateKeyspace                                      extends ReadCommand
  case object CreateIssueTable                                    extends ReadCommand
  final case class GetIssuesByDate(date: LocalDate)               extends ReadCommand
  final case class GetIssueByDateAndId(date: LocalDate, id: UUID) extends ReadCommand

  sealed trait ReadEvent

  final case object TableCreated extends ReadEvent

  object CQLStatements {
    import Settings.CassandraDb._

    final val CreateKeyspaceStatement: String =
      s"""
         |CREATE KEYSPACE IF NOT EXISTS $keyspace
         |  WITH REPLICATION = { 'class' : '$keyspaceReplicationStrategy', 'replication_factor' : $keyspaceReplicationFactor };
    """.stripMargin

    final val CreateTableStatement: String =
      s"""
         |CREATE TABLE IF NOT EXISTS $keyspace.issues (
         |  id timeuuid,
         |  summary varchar,
         |  description text,
         |  date_updated varchar,
         |  issue_status text,
         |  PRIMARY KEY ((date_updated), id)
         |) WITH CLUSTERING ORDER BY (id ASC);
         |
       |""".stripMargin

    final val InsertStatement: String =
      s"""
         |INSERT INTO $keyspace.issues (id, summary, description, date_updated, issue_status)
         | VALUES (?, ?, ?, ?, ?);
    """.stripMargin

    final val SelectByDateAndIdStatement: String = s"SELECT * FROM $keyspace.issues WHERE date_updated = ? AND id = ?;"

    final val SelectByDateStatement: String = s"SELECT * FROM $keyspace.issues WHERE date_updated = ?;"

    final val CloseStatement: String =
      s"UPDATE $keyspace.issues SET issue_status = ? WHERE date_updated = ? AND id = ?;"

    final val UpdateStatement: String =
      s"UPDATE $keyspace.issues SET summary =?, description = ? WHERE date_updated = ? and id = ?;"

    final val DeleteStatement: String = s"DELETE FROM $keyspace.issues WHERE date_updated = ? and id = ?;"
  }

  def props(publishSubscribeMediator: ActorRef, readJournal: EventsByTagQuery2) =
    Props(new IssueTrackerRead(publishSubscribeMediator, readJournal))
}

class IssueTrackerRead(publishSubscribeMediator: ActorRef, readJournal: EventsByTagQuery2)
    extends CassandraActor
    with ActorLogging {

  import IssueTrackerRead._
  import IssueTrackerRead.CQLStatements._
  import context.dispatcher

  implicit val materializer = ActorMaterializer()
  readJournal
    .eventsByTag("issue-tracker-tag", TimeBasedUUID(UUIDs.timeBased()))
    .map {
      case EventEnvelope2(_, _, _, event: IssueTrackerEvent) => event
    }
    .runWith(Sink.actorRef(self, "completed"))

  override def receive: Receive = {
    case CreateKeyspace =>
      session.executeAsync(CreateKeyspaceStatement).toFuture pipeTo self
    case rs: ResultSet =>
      log.info(rs.getExecutionInfo.getStatement.toString)
      context.become(keyspaceInitialized)
      self ! CreateIssueTable
  }

  private def keyspaceInitialized: Receive = {
    case CreateIssueTable =>
      session.executeAsync(CreateTableStatement).toFuture pipeTo self
    case rs: ResultSet =>
      log.info(rs.getExecutionInfo.getStatement.toString)
      context.become(ready)
      context.parent ! TableCreated
  }

  private def ready: Receive = {
    case event: IssueCreated =>
      publishSubscribeMediator ! Publish(className[IssueTrackerEvent], event)
      session.executeAsync(InsertStatement,
                           event.id,
                           event.summary,
                           event.description,
                           event.date.toString,
                           event.status.toString)

    case event: IssueClosed =>
      publishSubscribeMediator ! Publish(className[IssueTrackerEvent], event)
      session.executeAsync(CloseStatement, IssueClosedStatus.toString, event.date.toString, event.id)

    case event: IssueUpdated =>
      publishSubscribeMediator ! Publish(className[IssueTrackerEvent], event)
      session.executeAsync(UpdateStatement, event.summary, event.description, event.date.toString, event.id)

    case event: IssueDeleted =>
      publishSubscribeMediator ! Publish(className[IssueTrackerEvent], event)
      session.executeAsync(DeleteStatement, event.date.toString, event.id)

    case GetIssueByDateAndId(date, id) =>
      session.executeAsync(SelectByDateAndIdStatement, date.toString, id).toFuture pipeTo sender()

    case GetIssuesByDate(date) => session.executeAsync(SelectByDateStatement, date.toString).toFuture pipeTo sender()
  }
}
