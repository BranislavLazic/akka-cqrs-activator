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
import akka.actor.ActorSystem
import akka.testkit.TestProbe
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpec }

class IssueAggregateSpec extends WordSpec with Matchers with BeforeAndAfterAll {

  import org.akkacqrs.IssueAggregate._

  implicit val system = ActorSystem("issue-tracker-spec-system")

  "When issue is not created then IssueAggregate actor" should {
    val sender             = TestProbe()
    implicit val senderRef = sender.ref
    val uuid               = UUID.randomUUID()
    val summary            = "Test summary"
    val description        = "Test description"
    val date               = LocalDate.now()
    val status             = IssueOpenedStatus

    val issueAggregate = system.actorOf(IssueAggregate.props(uuid, date))

    "correctly create a new issue" in {
      issueAggregate ! CreateIssue(uuid, summary, description, date, status)
      sender.expectMsg(IssueCreated(uuid, summary, description, date, status))
    }

    "not create an issue with same id again" in {
      issueAggregate ! CreateIssue(uuid, summary, description, date, status)
      sender.expectMsg(IssueUnprocessed("Issue has been already created."))
    }

    "correctly update an issue" in {
      val updatedIssueDescription = "Updated issue description"
      val updatedIssueSummary     = "Updated summary"
      val date                    = LocalDate.now()
      issueAggregate ! UpdateIssue(uuid, updatedIssueSummary, updatedIssueDescription, date)
      sender.expectMsg(IssueUpdated(uuid, updatedIssueSummary, updatedIssueDescription, date))
    }

    "correctly update an issue again" in {
      val updatedIssueDescription = "Updated issue description second time"
      val updatedIssueSummary     = "Updated summary"
      val date                    = LocalDate.now()
      issueAggregate ! UpdateIssue(uuid, updatedIssueSummary, updatedIssueDescription, date)
      sender.expectMsg(IssueUpdated(uuid, updatedIssueSummary, updatedIssueDescription, date))
    }

    "correctly close an issue" in {
      issueAggregate ! CloseIssue(uuid, date)
      sender.expectMsg(IssueClosed(uuid, date))
    }

    "not close an issue again" in {
      issueAggregate ! CloseIssue(uuid, date)
      sender.expectMsg(IssueUnprocessed("Issue has been closed. Cannot update or close again."))
    }

    "correctly delete an issue" in {
      issueAggregate ! DeleteIssue(uuid, date)
      sender.expectMsg(IssueDeleted(uuid, date))
    }

    "not delete an issue again" in {
      issueAggregate ! DeleteIssue(uuid, date)
      sender.expectMsg(IssueUnprocessed("Issue has been deleted. Cannot update, close or delete again."))
    }
  }

  "When issue is not being created then IssueAggregate actor" should {
    val sender             = TestProbe()
    implicit val senderRef = sender.ref
    val uuid               = UUID.randomUUID()
    val date               = LocalDate.now()

    val issueAggregate = system.actorOf(IssueAggregate.props(uuid, date))

    "not update an issue description" in {
      issueAggregate ! UpdateIssue(uuid, "Updated summary", "Updated description", date)
      sender.expectMsg(IssueUnprocessed("Create an issue first."))
    }

    "not close an issue" in {
      issueAggregate ! CloseIssue(uuid, date)
      sender.expectMsg(IssueUnprocessed("Create an issue first."))
    }

    "not delete an issue" in {
      issueAggregate ! DeleteIssue(uuid, date)
      sender.expectMsg(IssueUnprocessed("Create an issue first."))
    }
  }

  override protected def afterAll(): Unit = system.terminate()
}
