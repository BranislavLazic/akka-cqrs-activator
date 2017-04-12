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

import akka.persistence.journal.{ Tagged, WriteEventAdapter }

/**
  * Creates a tag with the unique name for IssueEvent's.
  * Events are being persisted with the tag which corresponds to the tag
  * specified in IssueTaggingEventAdapter.
  */
class IssueTaggingEventAdapter extends WriteEventAdapter {
  import IssueRepository._
  override def manifest(event: Any): String = ""

  def withTag(event: IssueEvent, tag: String) = Tagged(event, Set(tag))

  override def toJournal(event: Any): Any = {
    event match {
      case evt: IssueEvent => withTag(evt, "issue-tag")
    }
  }
}
