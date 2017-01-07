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

import java.time.{ LocalDate, LocalDateTime }
import java.util.UUID

import akka.actor.Props
import akka.persistence.fsm.PersistentFSM
import akka.persistence.fsm.PersistentFSM.FSMState
import org.akkacqrs.IssueTrackerAggregate._

import scala.reflect._

object IssueTrackerAggregate {

  sealed trait IssueTrackerCommand

  final case class CreateIssue(id: UUID, description: String, date: LocalDate) extends IssueTrackerCommand
  final case class UpdateIssueDescription(id: UUID, description: String, dateTime: LocalDate)
      extends IssueTrackerCommand
  final case class CloseIssue(id: UUID, date: LocalDate)  extends IssueTrackerCommand
  final case class DeleteIssue(id: UUID, date: LocalDate) extends IssueTrackerCommand

  sealed trait IssueTrackerEvent

  final case class IssueCreated(id: UUID, description: String, date: LocalDate)            extends IssueTrackerEvent
  final case class IssueDescriptionUpdated(id: UUID, description: String, date: LocalDate) extends IssueTrackerEvent
  final case class IssueUnprocessed(message: String)                                       extends IssueTrackerEvent
  final case class IssueClosed(id: UUID, date: LocalDate)                                  extends IssueTrackerEvent
  final case class IssueDeleted(id: UUID, date: LocalDate)                                 extends IssueTrackerEvent

  sealed trait IssueTrackerState extends FSMState

  case object Idle extends IssueTrackerState {
    override def identifier = "idle"
  }
  case object IssueCreatedState extends IssueTrackerState {
    override def identifier = "issueCreated"
  }
  case object IssueClosedState extends IssueTrackerState {
    override def identifier = "issueClosed"
  }
  case object IssueDeletedState extends IssueTrackerState {
    override def identifier = "issueDeleted"
  }

  sealed trait IssueTrackerData

  case object Empty extends IssueTrackerData

  def props(id: UUID) = Props(new IssueTrackerAggregate(id))
}

class IssueTrackerAggregate(id: UUID)(implicit val domainEventClassTag: ClassTag[IssueTrackerEvent])
    extends PersistentFSM[IssueTrackerState, IssueTrackerData, IssueTrackerEvent] {

  override def persistenceId: String = id.toString

  override def applyEvent(domainEvent: IssueTrackerEvent, currentData: IssueTrackerData): IssueTrackerData = {
    domainEvent match {
      case _ => Empty
    }
  }

  startWith(Idle, Empty)

  when(Idle) {
    case Event(CreateIssue(`id`, description, date), _) =>
      val issueCreated = IssueCreated(id, description, date)
      goto(IssueCreatedState) applying issueCreated replying issueCreated

    case Event(_, _) =>
      stay replying IssueUnprocessed("Create an issue first.")
  }

  when(IssueCreatedState) {
    case Event(UpdateIssueDescription(`id`, description, date), _) =>
      val issueDescriptionUpdated = IssueDescriptionUpdated(id, description, date)
      stay applying issueDescriptionUpdated replying issueDescriptionUpdated

    case Event(CloseIssue(`id`, date), _) =>
      val issueClosed = IssueClosed(id, date)
      goto(IssueClosedState) applying issueClosed replying issueClosed

    case Event(DeleteIssue(`id`, date), _) =>
      val issueDeleted = IssueDeleted(id, date)
      goto(IssueDeletedState) applying issueDeleted replying issueDeleted
  }

  when(IssueClosedState) {
    case Event(DeleteIssue(`id`, date), _) =>
      val issueDeleted = IssueDeleted(id, date)
      goto(IssueDeletedState) applying issueDeleted replying issueDeleted

    case Event(_, _) =>
      stay replying IssueUnprocessed("Issue has been closed. Cannot update or close again.")
  }

  when(IssueDeletedState) {
    case Event(_, _) =>
      stay replying IssueUnprocessed("Issue has been deleted. Cannot update, close or delete again.")
  }

  whenUnhandled {
    case Event(CreateIssue(`id`, _, _), _) =>
      stay replying IssueUnprocessed("Issue has been already created.")
  }
}
