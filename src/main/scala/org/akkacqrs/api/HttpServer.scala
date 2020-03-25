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

package org.akkacqrs.api

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import akka.http.scaladsl.Http
import akka.pattern._
import akka.stream.ActorMaterializer
import org.akkacqrs.read.IssueService

import scala.concurrent.duration._

object HttpServer extends CORSHandler {

  final val Name = "http-server"

  def props(
      host: String,
      port: Int,
      requestTimeout: FiniteDuration,
      eventBufferSize: Int,
      heartbeatInterval: FiniteDuration,
      issueRepositoryManager: ActorRef,
      issueRead: IssueService,
      publishSubscribeMediator: ActorRef
  ): Props =
    Props(
      new HttpServer(
        host,
        port,
        requestTimeout,
        eventBufferSize,
        heartbeatInterval: FiniteDuration,
        issueRepositoryManager,
        issueRead,
        publishSubscribeMediator
      )
    )
}

final class HttpServer(
    host: String,
    port: Int,
    requestTimeout: FiniteDuration,
    eventBufferSize: Int,
    heartbeatInterval: FiniteDuration,
    issueRepositoryManager: ActorRef,
    issueService: IssueService,
    publishSubscribeMediator: ActorRef
) extends Actor
    with ActorLogging {
  import context.dispatcher
  import org.akkacqrs.api.IssueRoutes._
  private implicit val materializer: ActorMaterializer = ActorMaterializer()

  Http(context.system)
    .bindAndHandle(
      routes(
        issueRepositoryManager,
        issueService,
        publishSubscribeMediator,
        requestTimeout,
        eventBufferSize,
        heartbeatInterval
      ),
      host,
      port
    )
    .pipeTo(self)

  override def receive: Receive = {
    case Http.ServerBinding(socketAddress) =>
      log.info(s"Server started at: $socketAddress")
  }
}
