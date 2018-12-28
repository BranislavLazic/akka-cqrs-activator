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

package org.akkacqrs

import akka.actor.{ Actor, ActorContext, ActorLogging, ActorRef, Props, Terminated }
import akka.cluster.pubsub.DistributedPubSub
import akka.persistence.query.scaladsl.EventsByTagQuery
import akka.stream.ActorMaterializer
import org.akkacqrs.service.{ IssueService, IssueServiceImpl }

import scala.concurrent.duration.FiniteDuration

object Root {
  final val Name = "root"

  private def createIssueRepositoryManager(context: ActorContext) =
    context.actorOf(IssueRepositoryManager.props, IssueRepositoryManager.Name)

  private def createHttpApi(context: ActorContext,
                            host: String,
                            port: Int,
                            requestTimeout: FiniteDuration,
                            eventBufferSize: Int,
                            heartbeatInterval: FiniteDuration,
                            issueRepositoryManager: ActorRef,
                            issueService: IssueService,
                            publishSubscribeMediator: ActorRef) =
    context.actorOf(HttpApi.props(host,
                                  port,
                                  requestTimeout,
                                  eventBufferSize,
                                  heartbeatInterval,
                                  issueRepositoryManager,
                                  issueService,
                                  publishSubscribeMediator),
                    HttpApi.Name)

  def props(readJournal: EventsByTagQuery) = Props(new Root(readJournal))
}

final class Root(readJournal: EventsByTagQuery) extends Actor with ActorLogging {
  import Root._
  import Settings.Http._
  import context.dispatcher
  private implicit val materializer: ActorMaterializer = ActorMaterializer()
  private val session                                  = CassandraConnector.openConnection()

  private val publishSubscribeMediator: ActorRef = context.watch(DistributedPubSub(context.system).mediator)
  private val issueRepositoryManager: ActorRef   = context.watch(createIssueRepositoryManager(context))
  private val issueService                       = new IssueServiceImpl(session, publishSubscribeMediator, readJournal)

  createHttpApi(context,
                host,
                port,
                requestTimeout,
                eventBufferSize,
                heartbeatInterval,
                issueRepositoryManager,
                issueService,
                publishSubscribeMediator)

  override def receive: Receive = {
    case Terminated(actor) =>
      log.info(s"Actor has been terminated: ${actor.path}")
      CassandraConnector.closeConnection(session)
      context.system.terminate()
  }
}
