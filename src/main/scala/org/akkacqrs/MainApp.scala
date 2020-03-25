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

import akka.actor.ActorSystem
import akka.cluster.pubsub.DistributedPubSub
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.PersistenceQuery
import akka.stream.ActorMaterializer
import org.akkacqrs.api.HttpServer
import org.akkacqrs.read.IssueService
import org.akkacqrs.write.IssueRepositoryManager

import scala.concurrent.ExecutionContextExecutor

object MainApp {

  def main(args: Array[String]): Unit = {
    import Settings.Http._
    implicit val system: ActorSystem               = ActorSystem("issue-tracker-system")
    implicit val execCtx: ExecutionContextExecutor = system.dispatcher

    SchemaInitializator(Vector("/schema.cql"))
    val readJournal = PersistenceQuery(system).readJournalFor[CassandraReadJournal](CassandraReadJournal.Identifier)

    implicit val materializer: ActorMaterializer = ActorMaterializer()
    val session                                  = CassandraConnector.openConnection()
    val publishSubscribeMediator                 = DistributedPubSub(system).mediator
    val issueRepositoryManager                   = system.actorOf(IssueRepositoryManager.props, IssueRepositoryManager.Name)
    val issueService                             = new IssueService(session, publishSubscribeMediator, readJournal)

    system.actorOf(
      HttpServer.props(
        host,
        port,
        requestTimeout,
        eventBufferSize,
        heartbeatInterval,
        issueRepositoryManager,
        issueService,
        publishSubscribeMediator
      ),
      HttpServer.Name
    )
  }
}
