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

import akka.actor.Props
import akka.persistence.fsm.PersistentFSM
import akka.persistence.fsm.PersistentFSM.FSMState
import org.akkacqrs.IssueRepository._
import java.io.{ Serializable => JavaSerializable }

import scala.reflect._
/*
Lifecycle of an issue:

                                       + <-----------+
                                       |              ^
                                       | Update issue |
                                       |              |
                                       v              +
                                       +------------->

+--------------------+           +-------------------------+             +-------------------------+       +--------------------+
|                    |           |                         |             |                         |       |                    |
|                    |           |                         |             |                         |       |                    |
|                    |           |                         |             |                         |       |                    |
|                    |           |                         |             |                         |       |                    |
|     Idle state     +---------> |     Created state       +-----------> |     Closed state        +-----> |   Deleted state    |
|                    |           |                         |             |                         |       |                    |
|                    |           |                         |             |                         |       |                    |
|                    |           |                         |             |                         |       |                    |
|                    |           |                         |             |                         |       |                    |
|                    |           |                         |             |                         |       |                    |
+--------+-----------+           +---+-------------------+-+             +------------+------------+       +--------------------+
         ^                           ^                   ^                            ^
         |                           |                   |                            |
         |                           |                   |                            |
         |                           |                   |                            |
         |                           |                   |                            |
         |                           |                   |                            |
         +                           +                   +                            +
    Create issue                 Close issue         Delete issue                Delete issue
 */
object IssueRepository {

  final val OpenedStatus = "OPENED"
  final val ClosedStatus = "CLOSED"

  sealed trait IssueCommand
  sealed trait Serializable extends JavaSerializable

  final case class CreateIssue(id: UUID, summary: String, description: String, date: LocalDate, status: IssueStatus)
      extends IssueCommand
  final case class UpdateIssue(id: UUID, summary: String, description: String, dateTime: LocalDate)
      extends IssueCommand
  final case class CloseIssue(id: UUID, date: LocalDate)  extends IssueCommand
  final case class DeleteIssue(id: UUID, date: LocalDate) extends IssueCommand

  sealed trait IssueEvent

  final case class IssueCreated(id: UUID, summary: String, description: String, date: LocalDate, status: IssueStatus)
      extends IssueEvent
      with Serializable
  final case class IssueUpdated(id: UUID, summary: String, description: String, date: LocalDate)
      extends IssueEvent
      with Serializable
  final case class IssueClosed(id: UUID, date: LocalDate)  extends IssueEvent with Serializable
  final case class IssueDeleted(id: UUID, date: LocalDate) extends IssueEvent with Serializable
  final case class IssueUnprocessed(message: String)       extends IssueEvent

  sealed trait IssueState extends FSMState

  case object Idle extends IssueState {
    override def identifier = "idle"
  }
  case object IssueCreatedState extends IssueState {
    override def identifier = "issueCreated"
  }
  case object IssueClosedState extends IssueState {
    override def identifier = "issueClosed"
  }
  case object IssueDeletedState extends IssueState {
    override def identifier = "issueDeleted"
  }

  sealed trait IssueStatus

  case object IssueOpenedStatus extends IssueStatus {
    override def toString: String = OpenedStatus
  }
  case object IssueClosedStatus extends IssueStatus {
    override def toString: String = ClosedStatus
  }

  sealed trait IssueData

  case object Empty extends IssueData

  def props(id: UUID, date: LocalDate) = Props(new IssueRepository(id, date))
}

final class IssueRepository(id: UUID, date: LocalDate)(implicit val domainEventClassTag: ClassTag[IssueEvent])
    extends PersistentFSM[IssueState, IssueData, IssueEvent] {

  override def persistenceId: String = s"${ id.toString }-${ date.toString }"

  override def applyEvent(domainEvent: IssueEvent, currentData: IssueData): IssueData = {
    domainEvent match {
      case _ => Empty
    }
  }

  startWith(Idle, Empty)

  when(Idle) {
    case Event(CreateIssue(`id`, summary, description, `date`, status), _) =>
      val issueCreated = IssueCreated(id, summary, description, date, status)
      goto(IssueCreatedState) applying issueCreated replying issueCreated

    case Event(_, _) =>
      stay replying IssueUnprocessed("Create an issue first.")
  }

  when(IssueCreatedState) {
    case Event(UpdateIssue(`id`, summary, description, `date`), _) =>
      val issueDescriptionUpdated = IssueUpdated(id, summary, description, date)
      stay applying issueDescriptionUpdated replying issueDescriptionUpdated

    case Event(CloseIssue(`id`, `date`), _) =>
      val issueClosed = IssueClosed(id, date)
      goto(IssueClosedState) applying issueClosed replying issueClosed

    case Event(DeleteIssue(`id`, `date`), _) =>
      val issueDeleted = IssueDeleted(id, date)
      goto(IssueDeletedState) applying issueDeleted replying issueDeleted
  }

  when(IssueClosedState) {
    case Event(DeleteIssue(`id`, `date`), _) =>
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
    case Event(CreateIssue(`id`, _, _, _, _), _) =>
      stay replying IssueUnprocessed("Issue has been already created.")
  }
}
