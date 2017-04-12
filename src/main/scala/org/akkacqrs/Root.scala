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

import akka.actor.{ Actor, ActorContext, ActorLogging, ActorRef, Props, Terminated }
import akka.cluster.pubsub.DistributedPubSub
import akka.persistence.query.scaladsl.EventsByTagQuery2
import org.akkacqrs.IssueView.{ CreateKeyspace, TableCreated }

import scala.concurrent.duration.FiniteDuration

object Root {
  final val Name = "root"

  private def createIssueRepositoryManager(context: ActorContext) = {
    context.actorOf(IssueRepositoryManager.props, IssueRepositoryManager.Name)
  }

  private def createIssueView(context: ActorContext,
                              publishSubscribeMediator: ActorRef,
                              readJournal: EventsByTagQuery2) = {
    context.actorOf(IssueView.props(publishSubscribeMediator, readJournal), IssueView.Name)
  }

  private def createHttpApi(context: ActorContext,
                            host: String,
                            port: Int,
                            requestTimeout: FiniteDuration,
                            eventBufferSize: Int,
                            heartbeatInterval: FiniteDuration,
                            issueRepositoryManager: ActorRef,
                            issueRead: ActorRef,
                            publishSubscribeMediator: ActorRef) = {
    context.actorOf(HttpApi.props(host,
                                  port,
                                  requestTimeout,
                                  eventBufferSize,
                                  heartbeatInterval,
                                  issueRepositoryManager,
                                  issueRead,
                                  publishSubscribeMediator),
                    HttpApi.Name)
  }

  def props(readJournal: EventsByTagQuery2) = Props(new Root(readJournal))
}

class Root(readJournal: EventsByTagQuery2) extends Actor with ActorLogging {
  import Root._
  import Settings.Http._

  val publishSubscribeMediator: ActorRef = context.watch(DistributedPubSub(context.system).mediator)
  val issueRepositoryManager: ActorRef   = context.watch(createIssueRepositoryManager(context))
  val issueView: ActorRef =
    context.watch(createIssueView(context, publishSubscribeMediator, readJournal))
  issueView ! CreateKeyspace

  override def receive: Receive = {
    case TableCreated =>
      context.watch(
        createHttpApi(context,
                      host,
                      port,
                      requestTimeout,
                      eventBufferSize,
                      heartbeatInterval,
                      issueRepositoryManager,
                      issueView,
                      publishSubscribeMediator)
      )
    case Terminated(actor) =>
      log.info(s"Actor has been terminated: ${ actor.path }")
      context.system.terminate()
  }
}
