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

import akka.actor.{ Actor, Props }

import scala.reflect._

/**
  * Used to manage IssueRepository actors.
  * If an IssueRepository actor doesn't exist already as a child of an IssueRepositoryManager,
  * then it will be created. All command messages are received by IssueRepositoryManager and forwarded to the
  * child actors.
  */
object IssueRepositoryManager {
  final val Name = "issue-repository-manager"
  def props      = Props(new IssueRepositoryManager)
}

class IssueRepositoryManager extends Actor {
  import IssueRepository._

  implicit val domainEventClassTag: ClassTag[IssueEvent] = classTag[IssueEvent]

  override def receive: Receive = {
    case createIssue @ CreateIssue(id, _, _, date, _) =>
      forwardToIssueRepository(id, date, createIssue)
    case updateIssueDescription @ UpdateIssue(id, _, _, date) =>
      forwardToIssueRepository(id, date, updateIssueDescription)
    case closeIssue @ CloseIssue(id, date) =>
      forwardToIssueRepository(id, date, closeIssue)
    case deleteIssue @ DeleteIssue(id, date) =>
      forwardToIssueRepository(id, date, deleteIssue)
  }

  private def forwardToIssueRepository(id: UUID, date: LocalDate, issueCommand: IssueCommand): Unit = {
    val name = s"${ id.toString }-${ date.toString }"
    context.child(name).getOrElse(context.actorOf(IssueRepository.props(id, date), name)).forward(issueCommand)
  }
}
