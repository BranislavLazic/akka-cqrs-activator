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

import java.time.LocalDateTime
import java.util.UUID

import akka.actor.Props
import akka.persistence.fsm.PersistentFSM
import akka.persistence.fsm.PersistentFSM.FSMState
import org.akkacqrs.IssueTrackerWrite._

import scala.reflect._

object IssueTrackerWrite {

  sealed trait IssueTrackerCommand

  final case class CreateIssue(id: UUID, description: String, date: LocalDateTime) extends IssueTrackerCommand
  final case class UpdateIssueDescription(id: UUID, description: String, date: LocalDateTime)
      extends IssueTrackerCommand
  final case class CloseIssue(id: UUID)  extends IssueTrackerCommand
  final case class DeleteIssue(id: UUID) extends IssueTrackerCommand

  sealed trait IssueTrackerEvent

  final case class IssueCreated(id: UUID, description: String, date: LocalDateTime) extends IssueTrackerEvent
  final case class IssueDescriptionUpdated(id: UUID, description: String, date: LocalDateTime)
      extends IssueTrackerEvent
  final case class IssueUnprocessed(message: String) extends IssueTrackerEvent
  final case class IssueClosed(id: UUID)             extends IssueTrackerEvent
  final case class IssueDeleted(id: UUID)            extends IssueTrackerEvent

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

  def props(id: UUID) = Props(new IssueTrackerWrite(id))
}

class IssueTrackerWrite(id: UUID)(implicit val domainEventClassTag: ClassTag[IssueTrackerEvent])
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
  }

  when(IssueCreatedState) {
    case Event(UpdateIssueDescription(`id`, description, date), _) =>
      val issueDescriptionUpdated = IssueDescriptionUpdated(id, description, date)
      stay() applying issueDescriptionUpdated replying issueDescriptionUpdated

    case Event(CloseIssue(`id`), _) =>
      val issueClosed = IssueClosed(id)
      goto(IssueClosedState) applying issueClosed replying issueClosed

    case Event(DeleteIssue(`id`), _) =>
      val issueDeleted = IssueDeleted(id)
      goto(IssueDeletedState) applying issueDeleted replying issueDeleted
  }

  when(IssueClosedState) {
    case Event(DeleteIssue(`id`), _) =>
      val issueDeleted = IssueDeleted(id)
      goto(IssueDeletedState) applying issueDeleted replying issueDeleted
  }

  when(IssueDeletedState) {
    case Event(_, _) =>
      stay() replying IssueUnprocessed("Issue has been deleted.")
  }

  whenUnhandled {
    case Event(UpdateIssueDescription(_, _, _), _) =>
      stay() replying IssueUnprocessed("Please create an issue first.")

    case Event(CloseIssue(_), _) =>
      stay() replying IssueUnprocessed("Cannot close an issue. Issue has been closed already or not created.")

    case Event(DeleteIssue(_), _) =>
      stay() replying IssueUnprocessed("Cannot delete a non existing issue.")
  }
}
