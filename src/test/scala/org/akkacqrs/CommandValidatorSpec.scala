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

import cats.data.NonEmptyList
import cats.data.Validated.{ Invalid, Valid }
import org.akkacqrs.IssueRepository.CreateIssue
import org.scalatest.{ Matchers, WordSpec }

class CommandValidatorSpec extends WordSpec with Matchers {
  import CommandValidator._

  "Command validator" should {
    "return errors if name in CreateIssue is empty" in {
      validateCreateIssue(
        CreateIssue(UUID.randomUUID(), null, "test description", LocalDate.now(), IssueRepository.OpenedStatus)
      ) should be(
        Invalid(new NonEmptyList(ValidationError("summary", "Summary cannot be null."), Nil))
      )
    }

    "return errors if name in CreateIssue has too many characters" in {
      validateCreateIssue(
        CreateIssue(
          UUID.randomUUID(),
          "abcdefghijklmnopqrstuvwxyzabcdefabcdefghijklmnopqrstuvwxyzabcdefabcdefghijklmnopqrstuvwxyzabcdefabcde",
          "test description",
          LocalDate.now(),
          IssueRepository.OpenedStatus
        )
      ) should be(
        Invalid(new NonEmptyList(ValidationError("summary", "Summary cannot have more than 100 characters."), Nil))
      )
    }

    "return valid command" in {
      val createIssue =
        CreateIssue(UUID.randomUUID(),
                    "test summary",
                    "test description",
                    LocalDate.now(),
                    IssueRepository.OpenedStatus)
      validateCreateIssue(createIssue) should be(Valid(createIssue))
    }
  }
}
