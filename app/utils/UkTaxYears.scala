/*
 * Copyright 2025 HM Revenue & Customs
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

package utils

import java.time.{LocalDate, Month, ZoneOffset}

object UkTaxYears {
  def getPastTwoUkTaxYears(
      currentDate: LocalDate = LocalDate.now(ZoneOffset.UTC)
  ): (LocalDate, LocalDate) = {
    val currentYearStart = LocalDate.of(currentDate.getYear, Month.APRIL, 6)
    if (currentDate.isBefore(currentYearStart)) {
      (LocalDate.of(currentDate.getYear - 2, Month.APRIL, 6), currentDate)
    } else {
      (LocalDate.of(currentDate.getYear - 1, Month.APRIL, 6), currentDate)
    }
  }

  def isInvalidDate(
      currentDate: LocalDate = LocalDate.now(ZoneOffset.UTC),
      dateToValidate: LocalDate
  ): Boolean = {
    val isBeforeTax: Boolean =
      if (currentDate.isBefore(LocalDate.of(currentDate.getYear, 4, 6))) true else false
    val startOfSevenTaxYearsAgo = {
      if (isBeforeTax) LocalDate.of(currentDate.getYear - 7, Month.APRIL, 6)
      else LocalDate.of(currentDate.getYear - 6, Month.APRIL, 6)
    }
    dateToValidate.isBefore(startOfSevenTaxYearsAgo) || dateToValidate.isAfter(currentDate)
  }
}
