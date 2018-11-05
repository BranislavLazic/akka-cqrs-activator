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

import java.util.UUID

import cats.data._
import cats.implicits._

object CommandValidator {
  import org.akkacqrs.IssueRepository._
  case class ValidationError(code: String, message: String)

  private def validateSummary(summary: String): ValidatedNel[ValidationError, String] =
    summary match {
      case null => Validated.invalidNel(ValidationError("summary", "Summary cannot be null."))
      case s if s.length < 2 =>
        Validated.invalidNel(ValidationError("summary", "Summary must have at least 2 characters."))
      case s if s.length >= 100 =>
        Validated.invalidNel(ValidationError("summary", "Summary cannot have more than 100 characters."))
      case s => Validated.valid(s)
    }

  private def validateDescription(description: String): ValidatedNel[ValidationError, String] =
    if (description.length <= 1500) Validated.valid(description)
    else Validated.invalidNel(ValidationError("description", "Description cannot have more than 1500 characters."))

  def validateCreateIssue(createIssue: CreateIssue): ValidatedNel[ValidationError, CreateIssue] = {
    val validId: ValidatedNel[ValidationError, UUID] = Validated.valid(createIssue.id)
    val validSummary                                 = validateSummary(createIssue.summary)
    val validDescription                             = validateDescription(createIssue.description)
    val validDate                                    = Validated.valid(createIssue.date)
    val validStatus                                  = Validated.valid(createIssue.status)

    (validId, validSummary, validDescription, validDate, validStatus) mapN CreateIssue
  }

  def validateUpdateIssue(updateIssue: UpdateIssue): ValidatedNel[ValidationError, UpdateIssue] = {
    val validId: ValidatedNel[ValidationError, UUID] = Validated.valid(updateIssue.id)
    val validSummary                                 = validateSummary(updateIssue.summary)
    val validDescription                             = validateDescription(updateIssue.description)
    val validDate                                    = Validated.valid(updateIssue.date)

    (validId, validSummary, validDescription, validDate) mapN UpdateIssue
  }
}
