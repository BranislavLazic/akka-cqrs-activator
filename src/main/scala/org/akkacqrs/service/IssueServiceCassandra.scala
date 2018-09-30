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

package org.akkacqrs.service

import java.time.LocalDate
import java.util.UUID

import akka.actor.ActorRef
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import akka.persistence.query.scaladsl.EventsByTagQuery
import akka.persistence.query.{ EventEnvelope, TimeBasedUUID }
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import com.datastax.driver.core.utils.UUIDs
import com.datastax.driver.core.{ ResultSet, Session }
import org.akkacqrs.IssueRepository._
import org.akkacqrs.{ className, Settings }
import org.akkacqrs._
import scala.collection.JavaConverters._
import scala.concurrent.{ ExecutionContext, Future }

/**
  * Subscribes to the IssueEvent's from mediator and manages issues in Cassandra.
  */
object IssueServiceCassandra {

  def apply(session: Session, publishSubscribeMediator: ActorRef, readJournal: EventsByTagQuery)(
      implicit materializer: ActorMaterializer,
      executionContext: ExecutionContext
  ): IssueServiceCassandra = {
    val issueView = new IssueServiceCassandra(session)
    issueView.subscribeToEvents(publishSubscribeMediator, readJournal)
    issueView
  }
}

final class IssueServiceCassandra(session: Session)(implicit materializer: ActorMaterializer,
                                                    executionContext: ExecutionContext)
    extends IssueService {
  import Settings.CassandraDb._

  override def getIssueByDateAndId(date: LocalDate, id: UUID): Future[Vector[IssueResponse]] =
    session
      .executeAsync(s"SELECT * FROM $keyspace.issues WHERE date_updated = ? AND id = ?;", date.toString, id)
      .toFuture
      .map(resultSetToIssueResponse)

  override def getIssueByDate(date: LocalDate): Future[Vector[IssueResponse]] =
    session
      .executeAsync(s"SELECT * FROM $keyspace.issues WHERE date_updated = ?;", date.toString)
      .toFuture
      .map(resultSetToIssueResponse)

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

  private def handleEvent(event: Any): Future[IssueEvent] = event match {
    case event: IssueCreated =>
      session
        .executeAsync(
          s"""
          |INSERT INTO $keyspace.issues (id, summary, description, date_updated, issue_status)
          | VALUES (?, ?, ?, ?, ?);
          """.stripMargin,
          event.id,
          event.summary,
          event.description,
          event.date.toString,
          event.status.toString
        )
        .toFuture
        .map(_ => event)
    case event: IssueClosed =>
      session
        .executeAsync(s"UPDATE $keyspace.issues SET issue_status = ? WHERE date_updated = ? AND id = ?;",
                      IssueClosedStatus.toString,
                      event.date.toString,
                      event.id)
        .toFuture
        .map(_ => event)
    case event: IssueUpdated =>
      session
        .executeAsync(
          s"UPDATE $keyspace.issues SET summary =?, description = ? WHERE date_updated = ? and id = ?;",
          event.summary,
          event.description,
          event.date.toString,
          event.id
        )
        .toFuture
        .map(_ => event)
    case event: IssueDeleted =>
      session
        .executeAsync(s"DELETE FROM $keyspace.issues WHERE date_updated = ? and id = ?;", event.date.toString, event.id)
        .toFuture
        .map(_ => event)
  }

  private def subscribeToEvents(
      publishSubscribeMediator: ActorRef,
      readJournal: EventsByTagQuery
  ) =
    readJournal
      .eventsByTag("issue-tag", TimeBasedUUID(UUIDs.timeBased()))
      .map {
        case EventEnvelope(_, _, _, event: IssueEvent) => event
        case EventEnvelope(_, _, _, event)             => event
      }
      .mapAsync(2)(handleEvent)
      .map(Publish(className[IssueEvent], _))
      .runWith(Sink.actorRef(publishSubscribeMediator, "completed"))

}
