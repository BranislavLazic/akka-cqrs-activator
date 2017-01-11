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

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import akka.cluster.pubsub.DistributedPubSubMediator.Subscribe
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.stream.{ ActorMaterializer, OverflowStrategy }
import akka.util.Timeout
import akka.http.scaladsl.server.Directives._

import scala.concurrent.duration._
import akka.pattern._
import akka.stream.scaladsl.Source
import com.datastax.driver.core.ResultSet
import com.datastax.driver.core.utils.UUIDs
import de.heikoseeberger.akkasse.ServerSentEvent
import org.akkacqrs.IssueTrackerAggregate._
import org.akkacqrs.IssueTrackerRead.{ GetIssueByDateAndId, GetIssuesByDate }

import collection.JavaConversions._
import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.reflect.ClassTag

object HttpServer {

  final case class CreateIssueRequest(description: String)
  final case class UpdateDescriptionRequest(id: UUID, description: String)
  final case class CloseIssueRequest(id: UUID)
  final case class GetIssueByDateAndIdResponse(id: UUID, date: String, description: String, issueStatus: String)

  final val Name = "http-server"

  def routes(issueTrackerAggregateManager: ActorRef,
             issueTrackerRead: ActorRef,
             publishSubscribeMediator: ActorRef,
             requestTimeout: FiniteDuration,
             eventBufferSize: Int)(implicit executionContext: ExecutionContext): Route = {

    import de.heikoseeberger.akkahttpcirce.CirceSupport._
    import de.heikoseeberger.akkasse.EventStreamMarshalling._
    import io.circe.generic.auto._
    import io.circe.syntax._
    implicit val timeout = Timeout(requestTimeout)

    def fromEventStream[A: ClassTag](toServerSentEvent: A => ServerSentEvent) = {
      Source
        .actorRef[A](eventBufferSize, OverflowStrategy.dropHead)
        .map(toServerSentEvent)
        .mapMaterializedValue(publishSubscribeMediator ! Subscribe(className[A], _))
    }

    def fromIssueTrackerEvent(event: IssueTrackerEvent): ServerSentEvent = {
      event match {
        case issueCreated: IssueCreated => ServerSentEvent(issueCreated.asJson.noSpaces, "issue-created")
        case issueDescriptionUpdated: IssueDescriptionUpdated =>
          ServerSentEvent(issueDescriptionUpdated.asJson.noSpaces, "issue-description-updated")
        case issueClosed: IssueClosed   => ServerSentEvent(issueClosed.asJson.noSpaces, "issue-closed")
        case issueDeleted: IssueDeleted => ServerSentEvent(issueDeleted.asJson.noSpaces, "issue-deleted")
        case unprocessedIssue: IssueUnprocessed =>
          ServerSentEvent(unprocessedIssue.message.asJson.noSpaces, "issue-unprocessed")
      }
    }

    pathPrefix("issues") {
      post {
        entity(as[CreateIssueRequest]) {
          case CreateIssueRequest(description: String) =>
            onSuccess(issueTrackerAggregateManager ? CreateIssue(UUIDs.timeBased(), description, LocalDate.now())) {
              case IssueCreated(_, _, _)     => complete(StatusCodes.OK, "Issue created.")
              case IssueUnprocessed(message) => complete(StatusCodes.UnprocessableEntity, message)
            }
        }
      } ~ path("event-stream") {
        complete {
          fromEventStream(fromIssueTrackerEvent)
        }
      } ~ pathPrefix(Segment) { date =>
        get {
          onSuccess(issueTrackerRead ? GetIssuesByDate(date.toLocalDate)) {
            case rs: ResultSet if rs.nonEmpty => complete(resultSetToIssueResponse(rs))
            case _: ResultSet                 => complete(StatusCodes.NotFound)
          }
        } ~ path(JavaUUID) { id =>
          put {
            entity(as[UpdateDescriptionRequest]) {
              case UpdateDescriptionRequest(`id`, description) =>
                onSuccess(issueTrackerAggregateManager ? UpdateIssueDescription(id, description, date.toLocalDate)) {
                  case IssueDescriptionUpdated(_, _, _) => complete(StatusCodes.OK, "Issue description updated.")
                  case IssueUnprocessed(message)        => complete(StatusCodes.UnprocessableEntity, message)
                }
            }
          } ~ put {
            onSuccess(issueTrackerAggregateManager ? CloseIssue(id, date.toLocalDate)) {
              case IssueClosed(_, _)         => complete("Issue has been closed.")
              case IssueUnprocessed(message) => complete(StatusCodes.UnprocessableEntity, message)
            }
          } ~ get {
            onSuccess(issueTrackerRead ? GetIssueByDateAndId(date.toLocalDate, `id`)) {
              case rs: ResultSet if rs.nonEmpty => complete(resultSetToIssueResponse(rs).head)
              case _: ResultSet                 => complete(StatusCodes.NotFound)
            }
          } ~ delete {
            onSuccess(issueTrackerAggregateManager ? DeleteIssue(id, date.toLocalDate)) {
              case IssueDeleted(_, _)        => complete("Issue has been deleted.")
              case IssueUnprocessed(message) => complete(StatusCodes.UnprocessableEntity, message)
            }
          }
        }

      }
    }
  }

  def resultSetToIssueResponse(rs: ResultSet): mutable.Buffer[GetIssueByDateAndIdResponse] = {
    rs.all()
      .map(row => {
        val id          = row.getUUID("id")
        val dateUpdated = row.getString("date_updated")
        val description = row.getString("description")
        val issueStatus = row.getString("issue_status")
        GetIssueByDateAndIdResponse(id, dateUpdated, description, issueStatus)
      })
  }

  def props(host: String,
            port: Int,
            requestTimeout: FiniteDuration,
            eventBufferSize: Int,
            issueTrackerAggregateManager: ActorRef,
            issueTrackerRead: ActorRef,
            publishSubscribeMediator: ActorRef) =
    Props(
      new HttpServer(host,
                     port,
                     requestTimeout,
                     eventBufferSize,
                     issueTrackerAggregateManager,
                     issueTrackerRead,
                     publishSubscribeMediator)
    )
}

class HttpServer(host: String,
                 port: Int,
                 requestTimeout: FiniteDuration,
                 eventBufferSize: Int,
                 issueTrackerAggregateManager: ActorRef,
                 issueTrackerRead: ActorRef,
                 publishSubscribeMediator: ActorRef)
    extends Actor
    with ActorLogging {
  import context.dispatcher
  import HttpServer._
  implicit val timeout      = Timeout(3.seconds)
  implicit val materializer = ActorMaterializer()

  Http(context.system)
    .bindAndHandle(routes(issueTrackerAggregateManager,
                          issueTrackerRead,
                          publishSubscribeMediator,
                          requestTimeout,
                          eventBufferSize),
                   host,
                   port)
    .pipeTo(self)

  override def receive: Receive = {
    case Http.ServerBinding(socketAddress) => log.info(s"Server started at: $socketAddress")
  }
}
