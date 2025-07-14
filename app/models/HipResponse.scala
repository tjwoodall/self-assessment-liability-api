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
    chargeDetails: Set[ChargeDetails],
    refundDetails: Set[RefundDetails],
    paymentHistoryDetails: Set[PaymentHistoryDetails]
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
    amendments: Set[Amendments],
    codedOutDetail: Set[CodedOutDetail]
)

object ChargeDetails {
  implicit val format: OFormat[ChargeDetails] = Json.format[ChargeDetails]
}

case class Amendments(
    amendmentId: String,
    amendmentType: String,
    amendmentDate: String,
    amendmentAmount: Double
)

object Amendments {
  implicit val format: OFormat[Amendments] = Json.format[Amendments]
}

case class CodedOutDetail(
    amount: Double,
    codedChargeType: String,
    effectiveDate: String,
    taxYear: String,
    effectiveTaxYear: String
)

object CodedOutDetail {
  implicit val format: OFormat[CodedOutDetail] = Json.format[CodedOutDetail]
}

case class RefundDetails(
    issueDate: String,
    refundMethod: String,
    refundRequestDate: String,
    refundRequestAmount: Double,
    refundReference: String,
    interestAddedToRefund: Double,
    refundActualAmount: Double,
    refundStatus: String
)

object RefundDetails {
  implicit val format: OFormat[RefundDetails] = Json.format[RefundDetails]
}

case class PaymentHistoryDetails(
    paymentAmount: Double,
    paymentDate: String,
    dateProcessed: String
)

object PaymentHistoryDetails {
  implicit val format: OFormat[PaymentHistoryDetails] = Json.format[PaymentHistoryDetails]
}
