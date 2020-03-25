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

import java.time.LocalDate
import java.util.UUID

import akka.actor.ActorRef
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.model.{ StatusCodes, Uri }
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.ActorMaterializer
import akka.testkit.{ TestActor, TestProbe }
import com.datastax.driver.core.utils.UUIDs
import org.akkacqrs.read.{ IssueResponse, IssueService }
import org.akkacqrs.write.IssueRepository
import org.mockito.integrations.scalatest.MockitoFixture
import org.scalatest.{ Matchers, WordSpec }

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContextExecutor, Future }

class IssueRoutesSpec extends WordSpec with Matchers with ScalatestRouteTest with MockitoFixture {

  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
  import io.circe.generic.auto._
  import org.akkacqrs.write.IssueRepository._
  import org.akkacqrs.api.IssueRoutes._

  implicit val context: ExecutionContextExecutor = system.dispatcher

  private val timeout           = 3.seconds
  private val eventBufferSize   = 100
  private val heartbeatInterval = 15.seconds

  private val id: UUID        = UUIDs.timeBased()
  private val date: LocalDate = LocalDate.now()
  private val summary         = "Test summary"
  private val description     = "Test description"

  private val issueRead = mock[IssueService]
  when(issueRead.getIssueByDate(any[LocalDate])).thenReturn(
    Future.successful(
      Vector(
        IssueResponse(UUID.randomUUID(), LocalDate.now().toString, "Summary", "No desc", IssueRepository.OpenedStatus)
      )
    )
  )
  when(issueRead.getIssueByDateAndId(any[LocalDate], any[UUID])).thenReturn(
    Future.successful(
      Vector(
        IssueResponse(UUID.randomUUID(), LocalDate.now().toString, "Summary", "No desc", IssueRepository.OpenedStatus)
      )
    )
  )

  "HttpApi" should {
    "result in status code PermanentRedirect to index.html upon GET /" in {
      val pubSubMediator                           = TestProbe()
      val issueRepositoryManager                   = TestProbe()
      implicit val materializer: ActorMaterializer = ActorMaterializer()

      Get("/") ~> routes(
        issueRepositoryManager.ref,
        issueRead,
        pubSubMediator.ref,
        timeout,
        eventBufferSize,
        heartbeatInterval
      ) ~> check {
        status shouldBe StatusCodes.PermanentRedirect
        header[Location] shouldBe Some(Location(Uri("index.html")))
      }
    }

    "result in status code Created upon sending POST /issues" in {
      val pubSubMediator         = TestProbe()
      val issueRepositoryManager = TestProbe()

      issueRepositoryManager.setAutoPilot((sender: ActorRef, msg: Any) =>
        (msg match {
          case CreateIssue(_, `summary`, `description`, `date`, IssueRepository.OpenedStatus) =>
            sender ! IssueCreated(id, summary, description, date, IssueRepository.OpenedStatus)
            TestActor.NoAutoPilot
        })
      )

      Post("/issues", CreateIssueRequest(date.toString, summary, description)) ~>
      routes(issueRepositoryManager.ref, issueRead, pubSubMediator.ref, timeout, eventBufferSize, heartbeatInterval) ~> check {
        status shouldBe StatusCodes.Created
      }
    }

    s"result in status code OK upon sending PUT /issues/${date.toString}/${id.toString} when updating an issue" in {
      val pubSubMediator         = TestProbe()
      val issueRepositoryManager = TestProbe()

      issueRepositoryManager.setAutoPilot((sender: ActorRef, msg: Any) =>
        (msg match {
          case UpdateIssue(`id`, `summary`, `description`, `date`) =>
            sender ! IssueUpdated(id, summary, description, date)
            TestActor.NoAutoPilot
        })
      )

      Put(s"/issues/${date.toString}/${id.toString}", UpdateRequest(summary, description)) ~>
      routes(issueRepositoryManager.ref, issueRead, pubSubMediator.ref, timeout, eventBufferSize, heartbeatInterval) ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    s"result in status code OK and message upon sending PUT /issues/${date.toString}/${id.toString} when closing an issue" in {
      val pubSubMediator         = TestProbe()
      val issueRepositoryManager = TestProbe()

      issueRepositoryManager.setAutoPilot((sender: ActorRef, msg: Any) =>
        (msg match {
          case CloseIssue(`id`, `date`) =>
            sender ! IssueClosed(id, date)
            TestActor.NoAutoPilot
        })
      )

      Put(s"/issues/${date.toString}/${id.toString}") ~>
      routes(issueRepositoryManager.ref, issueRead, pubSubMediator.ref, timeout, eventBufferSize, heartbeatInterval) ~> check {
        status shouldBe StatusCodes.OK
        responseAs[String] shouldBe "Issue has been closed."
      }
    }

    s"result in status code OK and message upon sending DELETE /issues/${date.toString}/${id.toString}" in {
      val pubSubMediator         = TestProbe()
      val issueRepositoryManager = TestProbe()

      issueRepositoryManager.setAutoPilot((sender: ActorRef, msg: Any) =>
        (msg match {
          case DeleteIssue(`id`, `date`) =>
            sender ! IssueDeleted(id, date)
            TestActor.NoAutoPilot
        })
      )

      Delete(s"/issues/${date.toString}/${id.toString}") ~>
      routes(issueRepositoryManager.ref, issueRead, pubSubMediator.ref, timeout, eventBufferSize, heartbeatInterval) ~> check {
        status shouldBe StatusCodes.OK
        responseAs[String] shouldBe "Issue has been deleted."
      }
    }
  }
}
