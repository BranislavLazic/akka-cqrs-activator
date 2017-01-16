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

import akka.actor.{ Actor, ActorRef, Props }

import scala.reflect._

/**
  * Used to manage IssueTrackerAggregate actors.
  * If an IssueTrackerAggregate actor doesn't exist already as a child of an IssueTrackerAggregateManager,
  * then it will be created. All command messages are received by IssueTrackerManager and forwarded to the
  * child actors.
  */
object IssueTrackerAggregateManager {
  final val Name = "issue-tracker-aggregate-manager"
  def props      = Props(new IssueTrackerAggregateManager)
}

class IssueTrackerAggregateManager extends Actor {
  import IssueTrackerAggregate._

  implicit val domainEventClassTag: ClassTag[IssueTrackerEvent] = classTag[IssueTrackerEvent]

  override def receive: Receive = {
    case createIssue @ CreateIssue(id, _, _, date, _) => getIssueTrackerWrite(id, date) forward createIssue
    case updateIssueDescription @ UpdateIssue(id, _, _, date) =>
      getIssueTrackerWrite(id, date) forward updateIssueDescription
    case closeIssue @ CloseIssue(id, date)   => getIssueTrackerWrite(id, date) forward closeIssue
    case deleteIssue @ DeleteIssue(id, date) => getIssueTrackerWrite(id, date) forward deleteIssue
  }

  private def getIssueTrackerWrite(id: UUID, date: LocalDate): ActorRef = {
    val name = s"${ id.toString }-${ date.toString }"
    context.child(name).getOrElse(context.actorOf(IssueTrackerAggregate.props(id, date), name))
  }
}
