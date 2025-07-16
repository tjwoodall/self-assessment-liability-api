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

package models

import play.api.libs.json.{Json, OFormat}

case class HipResponse(
    balanceDetails: BalanceDetails,
    chargeDetails: Option[Set[ChargeDetails]],
    refundDetails: Option[Set[RefundDetails]],
    paymentHistoryDetails: Option[Set[PaymentHistoryDetails]]
)

object HipResponse {
  implicit val format: OFormat[HipResponse] = Json.format[HipResponse]
}

case class BalanceDetails(
    totalOverdueBalance: Double,
    totalPayableBalance: Double,
    payableDueDate: String,
    totalPendingBalance: Double,
    pendingDueDate: String,
    totalBalance: Double,
    totalCodedOut: Double,
    totalCreditAvailable: Double
)

object BalanceDetails {
  implicit val format: OFormat[BalanceDetails] = Json.format[BalanceDetails]
}

case class ChargeDetails(
    chargeId: String,
    creationDate: String,
    chargeType: String,
    chargeAmount: Double,
    outstandingAmount: Double,
    taxYear: String,
    dueDate: String,
    interestAmountDue: Option[Double],
    accruingInterest: Option[Double],
    accruingInterestDateRange: Option[AccruingInterestDateRange],
    accruingInterestRate: Option[Double],
    amendments: Option[Set[Amendments]],
    codedOutDetail: Option[Set[CodedOutDetail]]
)

object ChargeDetails {
  implicit val format: OFormat[ChargeDetails] = Json.format[ChargeDetails]
}

case class AccruingInterestDateRange(
    interestStartDate: String,
    interestEndDate: String
)

object AccruingInterestDateRange {
  implicit val format: OFormat[AccruingInterestDateRange] = Json.format[AccruingInterestDateRange]
}

case class Amendments(
    amendmentDate: String,
    amendmentAmount: Double,
    amendmentReason: String,
    newChargeBalance: Option[Double],
    paymentMethod: Option[String],
    paymentDate: Option[String]
)

object Amendments {
  implicit val format: OFormat[Amendments] = Json.format[Amendments]
}

case class CodedOutDetail(
    amount: Option[Double],
    effectiveDate: Option[String],
    taxYear: Option[String],
    effectiveTaxYear: Option[String]
)

object CodedOutDetail {
  implicit val format: OFormat[CodedOutDetail] = Json.format[CodedOutDetail]
}

case class RefundDetails(
    issueDate: String,
    refundMethod: Option[String],
    refundRequestDate: Option[String],
    refundRequestAmount: Double,
    refundReference: Option[String],
    interestAddedToRefund: Option[Double],
    refundActualAmount: Double,
    refundStatus: Option[String]
)

object RefundDetails {
  implicit val format: OFormat[RefundDetails] = Json.format[RefundDetails]
}

case class PaymentHistoryDetails(
    paymentAmount: Double,
    paymentReference: String,
    paymentMethod: String,
    paymentDate: String,
    dateProcessed: String,
    allocationReference: Option[String]
)

object PaymentHistoryDetails {
  implicit val format: OFormat[PaymentHistoryDetails] = Json.format[PaymentHistoryDetails]
}
