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

package shared

import models.*
import org.scalacheck.Gen
import java.time.LocalDate

object HipResponseGenerator {

  val localDateGen: Gen[LocalDate] = for {
    year <- Gen.choose(LocalDate.now().minusYears(7).getYear, LocalDate.now().getYear)
    month <- Gen.choose(1, 12)
    day <- Gen.choose(1, 28)
  } yield LocalDate.of(year, month, day)

  val codedOutDetailGen: Gen[CodedOutDetail] = for {
    totalAmount <- Gen.choose(0.0, 10000.0)
    startDate <- localDateGen
    daysToAdd <- Gen.choose(1, 365)
    endDate = startDate.plusDays(daysToAdd)
  } yield CodedOutDetail(
    totalAmount = totalAmount,
    effectiveStartDate = startDate,
    effectiveEndDate = endDate
  )

  val balanceDetailsGen: Gen[BalanceDetails] = for {
    totalOverdueBalance <- Gen.choose(0.0, 50000.0)
    totalPayableBalance <- Gen.choose(0.0, 100000.0)
    earliestPayableDueDate <- Gen.option(localDateGen)
    totalPendingBalance <- Gen.choose(0.0, 25000.0)
    earliestPendingDueDate <- Gen.option(localDateGen)
    totalBalance <- Gen.choose(0.0, 150000.0)
    totalCreditAvailable <- Gen.choose(0.0, 20000.0)
    codedOutDetails <- Gen.option(Gen.containerOf[Set, CodedOutDetail](codedOutDetailGen))
  } yield BalanceDetails(
    totalOverdueBalance = totalOverdueBalance,
    totalPayableBalance = totalPayableBalance,
    earliestPayableDueDate = earliestPayableDueDate,
    totalPendingBalance = totalPendingBalance,
    earliestPendingDueDate = earliestPendingDueDate,
    totalBalance = totalBalance,
    totalCreditAvailable = totalCreditAvailable,
    codedOutDetail = codedOutDetails
  )

  val accruingInterestPeriodGen: Gen[AccruingInterestPeriod] = for {
    startDate <- localDateGen
    daysToAdd <- Gen.choose(1, 365)
    endDate = startDate.plusDays(daysToAdd)
  } yield AccruingInterestPeriod(
    interestStartDate = startDate,
    interestEndDate = endDate
  )

  val amendmentsGen: Gen[Amendment] = for {
    amendmentDate <- localDateGen
    amendmentAmount <- Gen.choose(-10000.0, 10000.0)
    amendmentReason <- Gen.oneOf(
      "Correction",
      "Late Filing Penalty",
      "Interest Adjustment",
      "Manual Override"
    )
    updatedChargeAmount <- Gen.option(Gen.choose(0.0, 50000.0))
    paymentMethod <- Gen.option(Gen.oneOf("Bank Transfer", "Credit Card", "Cheque", "Direct Debit"))
    paymentDate <- Gen.option(localDateGen)
  } yield Amendment(
    amendmentDate = amendmentDate,
    amendmentAmount = amendmentAmount,
    amendmentReason = amendmentReason,
    updatedChargeAmount = updatedChargeAmount,
    paymentMethod = paymentMethod,
    paymentDate = paymentDate
  )

  val chargeDetailsGen: Gen[ChargeDetails] = for {
    chargeId <- Gen.alphaNumStr.suchThat(_.nonEmpty)
    creationDate <- localDateGen
    chargeType <- Gen.oneOf("Income Tax", "VAT", "Corporation Tax", "PAYE", "National Insurance")
    chargeAmount <- Gen.choose(1.0, 100000.0)
    outstandingAmount <- Gen.choose(0.0, 50000.0)
    taxYear <- Gen.choose(2020, 2025).map(year => s"$year-${year + 1}")
    dueDate <- localDateGen
    outstandingInterestDue <- Gen.option(Gen.choose(0.0, 5000.0))
    accruingInterest <- Gen.option(Gen.choose(0.0, 1000.0))
    accruingInterestPeriod <- Gen.option(accruingInterestPeriodGen)
    accruingInterestRate <- Gen.option(Gen.choose(0.0, 15.0))
    amendments <- Gen.option(Gen.containerOf[Set, Amendment](amendmentsGen))
  } yield ChargeDetails(
    chargeId = chargeId,
    creationDate = creationDate,
    chargeType = chargeType,
    chargeAmount = chargeAmount,
    outstandingAmount = outstandingAmount,
    taxYear = taxYear,
    dueDate = dueDate,
    outstandingInterestDue = outstandingInterestDue,
    accruingInterest = accruingInterest,
    accruingInterestPeriod = accruingInterestPeriod,
    accruingInterestRate = accruingInterestRate,
    amendments = amendments
  )

  val refundDetailsGen: Gen[RefundDetails] = for {
    refundDate <- localDateGen
    refundMethod <- Gen.option(Gen.oneOf("Bank Transfer", "Cheque", "Credit Card Refund"))
    refundRequestDate <- Gen.option(localDateGen)
    refundRequestAmount <- Gen.choose(1.0, 50000.0)
    refundDescription <- Gen.option(
      Gen.oneOf("Overpayment", "Double Payment", "Incorrect Charge", "System Error")
    )
    interestAddedToRefund <- Gen.option(Gen.choose(0.0, 1000.0))
    totalRefundAmount <- Gen.choose(1.0, 55000.0)
    refundStatus <- Gen.option(Gen.oneOf("Pending", "Approved", "Processed", "Rejected"))
  } yield RefundDetails(
    refundDate = refundDate,
    refundMethod = refundMethod,
    refundRequestDate = refundRequestDate,
    refundRequestAmount = refundRequestAmount,
    refundDescription = refundDescription,
    interestAddedToRefund = interestAddedToRefund,
    totalRefundAmount = totalRefundAmount,
    refundStatus = refundStatus
  )

  val paymentHistoryDetailsGen: Gen[PaymentHistoryDetails] = for {
    paymentAmount <- Gen.choose(1.0, 100000.0)
    paymentReference <- Gen.alphaNumStr.suchThat(_.nonEmpty)
    paymentMethod <- Gen.option(Gen.oneOf("Bank Transfer", "Credit Card", "Direct Debit", "Cheque"))
    paymentDate <- localDateGen
    processedDate <- Gen.option(localDateGen)
    allocationReference <- Gen.option(Gen.listOf(Gen.alphaNumStr))
  } yield PaymentHistoryDetails(
    paymentAmount = paymentAmount,
    paymentReference = paymentReference,
    paymentMethod = paymentMethod,
    paymentDate = paymentDate,
    processedDate = processedDate,
    allocationReference = allocationReference
  )

  val hipResponseGen: Gen[HipResponse] = for {
    balanceDetails <- balanceDetailsGen
    chargeDetails <- Gen.option(Gen.containerOf[Set, ChargeDetails](chargeDetailsGen))
    refundDetails <- Gen.option(Gen.containerOf[Set, RefundDetails](refundDetailsGen))
    paymentHistoryDetails <- Gen.option(
      Gen.containerOf[Set, PaymentHistoryDetails](paymentHistoryDetailsGen)
    )
  } yield HipResponse(
    balanceDetails = balanceDetails,
    chargeDetails = chargeDetails,
    refundDetails = refundDetails,
    paymentHistoryDetails = paymentHistoryDetails
  )
}
