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
import com.datastax.driver.core.utils.UUIDs
import de.heikoseeberger.akkasse.ServerSentEvent
import org.akkacqrs.IssueRepository._
import org.akkacqrs.IssueView.{ GetIssueByDateAndId, GetIssuesByDate }

import scala.concurrent.ExecutionContext
import scala.reflect.ClassTag

object HttpApi {

  final case class CreateIssueRequest(date: String, summary: String, description: String)
  final case class UpdateRequest(summary: String, description: String)
  final case class CloseIssueRequest(id: UUID)

  final val Name = "http-server"

  def routes(issueRepositoryManager: ActorRef,
             issueView: ActorRef,
             publishSubscribeMediator: ActorRef,
             requestTimeout: FiniteDuration,
             eventBufferSize: Int,
             heartbeatInterval: FiniteDuration)(implicit executionContext: ExecutionContext): Route = {

    import de.heikoseeberger.akkahttpcirce.CirceSupport._
    import de.heikoseeberger.akkasse.EventStreamMarshalling._
    import io.circe.generic.auto._
    import io.circe.syntax._
    implicit val timeout = Timeout(requestTimeout)

    /**
      * Subscribes to the stream of incoming events from IssueVew.
      *
      * @param toServerSentEvent the function that converts incoming event to the server sent event
      * @tparam A the issue  event type
      * @return the source of server sent events
      */
    def fromEventStream[A: ClassTag](toServerSentEvent: A => ServerSentEvent) = {
      Source
        .actorRef[A](eventBufferSize, OverflowStrategy.dropHead)
        .map(toServerSentEvent)
        .mapMaterializedValue(publishSubscribeMediator ! Subscribe(className[A], _))
        .keepAlive(heartbeatInterval, () => ServerSentEvent.heartbeat)
    }

    /**
      * Converts incoming event to the server sent event.
      *
      * @param event the incoming issueEvent
      * @return server sent event instance
      */
    def eventToServerSentEvent(event: IssueEvent): ServerSentEvent = {
      event match {
        case issueCreated: IssueCreated =>
          ServerSentEvent(issueCreated.asJson.noSpaces, "issue-created")
        case issueUpdated: IssueUpdated =>
          ServerSentEvent(issueUpdated.asJson.noSpaces, "issue-updated")
        case issueClosed: IssueClosed =>
          ServerSentEvent(issueClosed.asJson.noSpaces, "issue-closed")
        case issueDeleted: IssueDeleted =>
          ServerSentEvent(issueDeleted.asJson.noSpaces, "issue-deleted")
        case unprocessedIssue: IssueUnprocessed =>
          ServerSentEvent(unprocessedIssue.message.asJson.noSpaces, "issue-unprocessed")
      }
    }

    def assets: Route = getFromResourceDirectory("web") ~ pathSingleSlash {
      get {
        redirect("index.html", StatusCodes.PermanentRedirect)
      }
    }

    def api: Route = pathPrefix("issues") {
      post {
        entity(as[CreateIssueRequest]) {
          case CreateIssueRequest(_, summary, _) if summary.isEmpty =>
            complete(StatusCodes.UnprocessableEntity, "Summary cannot be empty.")
          case CreateIssueRequest(_, summary, _) if summary.length > 100 =>
            complete(StatusCodes.UnprocessableEntity,
                     "Name of the summary is too long. Maximum length is 100 characters.")
          case CreateIssueRequest(date: String, summary: String, description: String) =>
            onSuccess(
              issueRepositoryManager ? CreateIssue(UUIDs.timeBased(),
                                                   summary,
                                                   description,
                                                   date.toLocalDate,
                                                   IssueOpenedStatus)
            ) {
              case IssueCreated(_, _, _, _, _) => complete("Issue created.")
              case IssueUnprocessed(message) =>
                complete(StatusCodes.UnprocessableEntity, message)
            }
        }
      } ~
        // Server sent events
        path("event-stream") {
          complete {
            fromEventStream(eventToServerSentEvent)
          }
        } ~
        // Requests for issues by specific date
        pathPrefix(Segment) { date =>
          // Requests for an issue specified by its UUID
          path(JavaUUID) { id =>
            put {
              entity(as[UpdateRequest]) {
                case UpdateRequest(summary, description) =>
                  onSuccess(issueRepositoryManager ? UpdateIssue(id, summary, description, date.toLocalDate)) {
                    case IssueUpdated(_, _, _, _) => complete("Issue updated.")
                    case IssueUnprocessed(message) =>
                      complete(StatusCodes.UnprocessableEntity -> message)
                  }
              }
            } ~
              put {
                onSuccess(issueRepositoryManager ? CloseIssue(id, date.toLocalDate)) {
                  case IssueClosed(_, _) => complete("Issue has been closed.")
                  case IssueUnprocessed(message) =>
                    complete(StatusCodes.UnprocessableEntity -> message)
                }
              } ~
              get {
                onSuccess(issueView ? GetIssueByDateAndId(date.toLocalDate, `id`)) {
                  case issues: Vector[IssueView.IssueResponse] @unchecked =>
                    complete(issues.head)
                }
              } ~
              delete {
                onSuccess(issueRepositoryManager ? DeleteIssue(id, date.toLocalDate)) {
                  case IssueDeleted(_, _) =>
                    complete("Issue has been deleted.")
                  case IssueUnprocessed(message) =>
                    complete(StatusCodes.UnprocessableEntity -> message)
                }
              }
          } ~
            get {
              onSuccess(issueView ? GetIssuesByDate(date.toLocalDate)) {
                case issues: Vector[IssueView.IssueResponse] @unchecked =>
                  complete(issues)
              }
            }
        }
    }
    api ~ assets
  }

  def props(host: String,
            port: Int,
            requestTimeout: FiniteDuration,
            eventBufferSize: Int,
            heartbeatInterval: FiniteDuration,
            issueRepositoryManager: ActorRef,
            issueRead: ActorRef,
            publishSubscribeMediator: ActorRef) =
    Props(
      new HttpApi(host,
                  port,
                  requestTimeout,
                  eventBufferSize,
                  heartbeatInterval: FiniteDuration,
                  issueRepositoryManager,
                  issueRead,
                  publishSubscribeMediator)
    )
}

final class HttpApi(host: String,
                    port: Int,
                    requestTimeout: FiniteDuration,
                    eventBufferSize: Int,
                    heartbeatInterval: FiniteDuration,
                    issueRepositoryManager: ActorRef,
                    issueRead: ActorRef,
                    publishSubscribeMediator: ActorRef)
    extends Actor
    with ActorLogging {
  import context.dispatcher
  import HttpApi._
  implicit val materializer = ActorMaterializer()

  Http(context.system)
    .bindAndHandle(routes(issueRepositoryManager,
                          issueRead,
                          publishSubscribeMediator,
                          requestTimeout,
                          eventBufferSize,
                          heartbeatInterval),
                   host,
                   port)
    .pipeTo(self)

  override def receive: Receive = {
    case Http.ServerBinding(socketAddress) =>
      log.info(s"Server started at: $socketAddress")
  }
}
