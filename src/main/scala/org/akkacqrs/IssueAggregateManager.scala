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
  * Used to manage IssueAggregate actors.
  * If an IssueAggregate actor doesn't exist already as a child of an IssueAggregateManager,
  * then it will be created. All command messages are received by IssueManager and forwarded to the
  * child actors.
  */
object IssueAggregateManager {
  final val Name = "issue-aggregate-manager"
  def props      = Props(new IssueAggregateManager)
}

class IssueAggregateManager extends Actor {
  import IssueAggregate._

  implicit val domainEventClassTag: ClassTag[IssueEvent] = classTag[IssueEvent]

  override def receive: Receive = {
    case createIssue @ CreateIssue(id, _, _, date, _) =>
      forwardToIssueAggregate(id, date, createIssue)
    case updateIssueDescription @ UpdateIssue(id, _, _, date) =>
      forwardToIssueAggregate(id, date, updateIssueDescription)
    case closeIssue @ CloseIssue(id, date) =>
      forwardToIssueAggregate(id, date, closeIssue)
    case deleteIssue @ DeleteIssue(id, date) =>
      forwardToIssueAggregate(id, date, deleteIssue)
  }

  private def forwardToIssueAggregate(id: UUID, date: LocalDate, issueCommand: IssueCommand): Unit = {
    val name = s"${ id.toString }-${ date.toString }"
    context.child(name).getOrElse(context.actorOf(IssueAggregate.props(id, date), name)).forward(issueCommand)
  }
}
