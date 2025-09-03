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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.{LocalDate, Month, ZoneOffset}

class UkTaxYearsSpec extends AnyWordSpec with Matchers {
  val today: LocalDate = LocalDate.now(ZoneOffset.UTC)
  "UkTaxYears.GetPastTwoUkTaxYears" should {

    "return correct dates when current date is before April 6th (in current tax year)" in {
      val testDate = LocalDate.of(2025, Month.MARCH, 1)
      val result = UkTaxYears.getPastTwoUkTaxYears(testDate)
      result._1 shouldBe LocalDate.of(2023, Month.APRIL, 6)
      result._2 shouldBe testDate
    }

    "return correct dates when current date is on April 6th (start of new tax year)" in {
      val testDate = LocalDate.of(2025, Month.APRIL, 6)
      val result = UkTaxYears.getPastTwoUkTaxYears(testDate)
      result._1 shouldBe LocalDate.of(2024, Month.APRIL, 6)
      result._2 shouldBe testDate
    }

    "return correct dates when current date is after April 6th (in new tax year)" in {
      val testDate = LocalDate.of(2025, Month.JULY, 15)
      val result = UkTaxYears.getPastTwoUkTaxYears(testDate)
      result._1 shouldBe LocalDate.of(2024, Month.APRIL, 6)
      result._2 shouldBe testDate
    }

    "return correct dates when current date is April 5th (last day of tax year)" in {
      val testDate = LocalDate.of(2025, Month.APRIL, 5)
      val result = UkTaxYears.getPastTwoUkTaxYears(testDate)
      result._1 shouldBe LocalDate.of(2023, Month.APRIL, 6)
      result._2 shouldBe testDate
    }

    "handle year boundaries correctly in January" in {
      val testDate = LocalDate.of(2025, Month.JANUARY, 15)
      val result = UkTaxYears.getPastTwoUkTaxYears(testDate)

      result._1 shouldBe LocalDate.of(2023, Month.APRIL, 6)
      result._2 shouldBe testDate
    }

    "use current date when no parameter is provided" in {
      val result = UkTaxYears.getPastTwoUkTaxYears()
      result._2.isAfter(LocalDate.now().minusDays(1)) shouldBe true
      result._2.isBefore(LocalDate.now().plusDays(1)) shouldBe true
      result._1.getMonth shouldBe Month.APRIL
      result._1.getDayOfMonth shouldBe 6
    }
  }

  "UkTaxYears.isInvalidDate" should {

    "return false for current date" in {
      UkTaxYears.isInvalidDate(dateToValidate = today) shouldBe false
    }

    "return true for yesterday" in {
      val yesterday = today.minusDays(1)
      UkTaxYears.isInvalidDate(dateToValidate = yesterday) shouldBe false
    }

    "return true for a date in the future" in {
      val futureDate = today.plusDays(1)
      UkTaxYears.isInvalidDate(dateToValidate = futureDate) shouldBe true
    }

    "handle leap years correctly" in {
      val date = LocalDate.of(2024, Month.FEBRUARY, 29)
      UkTaxYears.isInvalidDate(dateToValidate = date) shouldBe false
    }

    "handle edge case around current date in different tax years" in {
      val dateAfterStartOfNewTaxYear = LocalDate.of(today.getYear, Month.MAY, 1)
      val moreThanSevenTaxYearsAgo = dateAfterStartOfNewTaxYear.minusYears(7)

      val dateBeforeApril = LocalDate.of(today.getYear, Month.FEBRUARY, 1)
      val lessThanSevenTaxYearsAgo = dateBeforeApril.minusYears(6)

      UkTaxYears.isInvalidDate(dateAfterStartOfNewTaxYear, moreThanSevenTaxYearsAgo) shouldBe true
      UkTaxYears.isInvalidDate(dateBeforeApril, lessThanSevenTaxYearsAgo) shouldBe false
    }
  }
}
