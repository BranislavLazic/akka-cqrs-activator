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
import akka.persistence.query.{ EventEnvelope, TimeBasedUUID }
import akka.persistence.query.scaladsl.EventsByTagQuery
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import com.datastax.driver.core.ResultSet
import com.datastax.driver.core.utils.UUIDs
import org.akkacqrs.IssueRepository._

import collection.JavaConverters._

/**
  * Creates "read side" Cassandra keyspace and table.
  * Subscribes to the IssueEvent's from mediator and manages issues in Cassandra.
  */
object IssueView {
  final val Name = "issue-view"

  sealed trait ReadCommand

  final case class GetIssuesByDate(date: LocalDate)               extends ReadCommand
  final case class GetIssueByDateAndId(date: LocalDate, id: UUID) extends ReadCommand

  final case class IssueResponse(id: UUID, date: String, summary: String, description: String, status: String)

  object CQLStatements {
    import Settings.CassandraDb._

    final val InsertStatement: String =
      s"""
         |INSERT INTO $keyspace.issues (id, summary, description, date_updated, issue_status)
         | VALUES (?, ?, ?, ?, ?);
    """.stripMargin

    final val SelectByDateAndIdStatement: String =
      s"SELECT * FROM $keyspace.issues WHERE date_updated = ? AND id = ?;"

    final val SelectByDateStatement: String =
      s"SELECT * FROM $keyspace.issues WHERE date_updated = ?;"

    final val CloseStatement: String =
      s"UPDATE $keyspace.issues SET issue_status = ? WHERE date_updated = ? AND id = ?;"

    final val UpdateStatement: String =
      s"UPDATE $keyspace.issues SET summary =?, description = ? WHERE date_updated = ? and id = ?;"

    final val DeleteStatement: String =
      s"DELETE FROM $keyspace.issues WHERE date_updated = ? and id = ?;"
  }

  def props(publishSubscribeMediator: ActorRef, readJournal: EventsByTagQuery) =
    Props(new IssueView(publishSubscribeMediator, readJournal))
}

final class IssueView(publishSubscribeMediator: ActorRef, readJournal: EventsByTagQuery)
    extends CassandraActor
    with ActorLogging {

  import IssueView._
  import IssueView.CQLStatements._
  import context.dispatcher

  private implicit val materializer = ActorMaterializer()

  readJournal
    .eventsByTag("issue-tag", TimeBasedUUID(UUIDs.timeBased()))
    .map {
      case EventEnvelope(_, _, _, event: IssueEvent) => event
      case EventEnvelope(_, _, _, event)             => event
    }
    .runWith(Sink.actorRef(self, "completed"))

  override def receive: Receive = {
    case event: IssueCreated =>
      publishSubscribeMediator ! Publish(className[IssueEvent], event)
      session.executeAsync(InsertStatement,
                           event.id,
                           event.summary,
                           event.description,
                           event.date.toString,
                           event.status.toString)

    case event: IssueClosed =>
      publishSubscribeMediator ! Publish(className[IssueEvent], event)
      session.executeAsync(CloseStatement, IssueClosedStatus.toString, event.date.toString, event.id)

    case event: IssueUpdated =>
      publishSubscribeMediator ! Publish(className[IssueEvent], event)
      session.executeAsync(UpdateStatement, event.summary, event.description, event.date.toString, event.id)

    case event: IssueDeleted =>
      publishSubscribeMediator ! Publish(className[IssueEvent], event)
      session.executeAsync(DeleteStatement, event.date.toString, event.id)

    case GetIssueByDateAndId(date, id) =>
      session
        .executeAsync(SelectByDateAndIdStatement, date.toString, id)
        .toFuture
        .map(resultSetToIssueResponse)
        .pipeTo(sender())

    case GetIssuesByDate(date) =>
      session
        .executeAsync(SelectByDateStatement, date.toString)
        .toFuture
        .map(resultSetToIssueResponse)
        .pipeTo(sender())

    case _ => log.warning(s"Unknown event received")
  }
  private def resultSetToIssueResponse(rs: ResultSet) =
    rs.all()
      .asScala
      .map(row => {
        val id          = row.getUUID("id")
        val summary     = row.getString("summary")
        val dateUpdated = row.getString("date_updated")
        val description = row.getString("description")
        val status      = row.getString("issue_status")
        IssueResponse(id, dateUpdated, summary, description, status)
      })
      .toVector
}
