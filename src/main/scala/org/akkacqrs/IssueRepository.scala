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

import java.time.LocalDate
import java.util.UUID
import akka.actor.{ ActorLogging, Props }
import org.akkacqrs.IssueRepository._
import java.io.{ Serializable => JavaSerializable }
import akka.persistence.{ PersistentActor, RecoveryCompleted, SnapshotOffer }
import cats.data.Validated.{ Invalid, Valid }
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

  sealed trait Serializable extends JavaSerializable

  sealed trait IssueCommand

  final case class CreateIssue(id: UUID, summary: String, description: String, date: LocalDate, status: String)
      extends IssueCommand
  final case class UpdateIssue(id: UUID, summary: String, description: String, date: LocalDate) extends IssueCommand
  final case class CloseIssue(id: UUID, date: LocalDate)                                        extends IssueCommand
  final case class DeleteIssue(id: UUID, date: LocalDate)                                       extends IssueCommand

  sealed trait IssueEvent extends Serializable

  final case class IssueCreated(id: UUID, summary: String, description: String, date: LocalDate, status: String)
      extends IssueEvent
  final case class IssueUpdated(id: UUID, summary: String, description: String, date: LocalDate) extends IssueEvent
  final case class IssueClosed(id: UUID, date: LocalDate)                                        extends IssueEvent
  final case class IssueDeleted(id: UUID, date: LocalDate)                                       extends IssueEvent
  final case class IssueUnprocessed(message: String)                                             extends IssueEvent

  def props(id: UUID, date: LocalDate) = Props(new IssueRepository(id, date))
}

final class IssueRepository(id: UUID, date: LocalDate)(implicit val domainEventClassTag: ClassTag[IssueEvent])
    extends PersistentActor
    with ActorLogging {
  import org.akkacqrs.validator.CommandValidator._

  // Take snapshot after 5 persisted events
  private val snapshotInterval = 5

  override def persistenceId: String = s"${id.toString}-${date.toString}"

  override def receiveRecover: Receive = {
    case SnapshotOffer(_, event: IssueEvent) => handleRecovery(event)
    case event: IssueEvent                   => handleRecovery(event)
    case RecoveryCompleted                   => log.info(s"Recovery completed for issue with id ${self.path.name}")
  }

  override def receiveCommand: Receive = idle

  private def handleRecovery(event: IssueEvent): Unit = event match {
    case IssueCreated(_, _, _, _, _) => context.become(created)
    case IssueClosed(_, _)           => context.become(closed)
    case IssueDeleted(_, _)          => context.become(deleted)
    case _: IssueEvent               =>
  }

  private def idle: Receive = {
    case createIssue @ CreateIssue(`id`, summary, description, `date`, status) =>
      validateCreateIssue(createIssue) match {
        case Valid(_) =>
          persist(IssueCreated(id, summary, description, date, status)) { issueCreatedEvt =>
            receiveRecover(issueCreatedEvt)
            checkForSnapshot(issueCreatedEvt)
            sender() ! issueCreatedEvt
          }
        case Invalid(errors) => sender() ! errors
      }
    case _: IssueCommand => sender() ! IssueUnprocessed("Create an issue first.")
  }

  private def created: Receive = {
    case updateIssue @ UpdateIssue(`id`, summary, description, `date`) =>
      validateUpdateIssue(updateIssue) match {
        case Valid(_) =>
          persist(IssueUpdated(id, summary, description, date)) { issueUpdatedEvt =>
            receiveRecover(issueUpdatedEvt)
            checkForSnapshot(issueUpdatedEvt)
            sender() ! issueUpdatedEvt
          }
        case Invalid(errors) => sender() ! errors
      }
    case CloseIssue(`id`, `date`) =>
      persist(IssueClosed(id, date)) { issueClosedEvt =>
        receiveRecover(issueClosedEvt)
        checkForSnapshot(issueClosedEvt)
        sender() ! issueClosedEvt
      }
    case DeleteIssue(`id`, `date`) =>
      persist(IssueDeleted(id, date)) { issueDeletedEvt =>
        receiveRecover(issueDeletedEvt)
        checkForSnapshot(issueDeletedEvt)
        sender() ! issueDeletedEvt
      }
    case CreateIssue(`id`, _, _, _, _) => sender() ! IssueUnprocessed("Issue has been already created.")
  }

  private def closed: Receive = {
    case DeleteIssue(`id`, `date`) =>
      persist(IssueDeleted(id, date)) { issueDeletedEvt =>
        receiveRecover(issueDeletedEvt)
        checkForSnapshot(issueDeletedEvt)
        sender() ! issueDeletedEvt
      }
    case _: IssueCommand => sender() ! IssueUnprocessed("Issue has been closed. Cannot update or close again.")
  }

  private def deleted: Receive = {
    case _: IssueCommand =>
      sender() ! IssueUnprocessed("Issue has been deleted. Cannot update, close or delete again.")
  }

  private def checkForSnapshot(event: IssueEvent): Unit =
    if (lastSequenceNr % snapshotInterval == 0 && lastSequenceNr != 0) saveSnapshot(event)

}
