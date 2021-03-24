package chargingdemoprocs.cvm;

import java.util.Date;

/* This file is part of VoltDB.
 * Copyright (C) 2008-2021 VoltDB Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

import org.voltdb.VoltTable;
import org.voltdb.types.TimestampType;

import chargingdemoprocs.ExtraUserData;
import chargingdemoprocs.ReportQuotaUsage;

/**
 * Send a offer for our gaming package IFF:
 * 
 * <p>
 * 1. The latest transaction is for Calls.
 * <p>
 * 2. We made a Platinum Call Plan offer more than messageInterval seconds ago.
 * <p>
 * 3. They are still not in our special platinum call plan.
 *
 */
public class LoyaltyOfferRound2Impl implements CVMInterface {

    public static final String OFFER_TYPE = "Loyalty2";

    /**
     * How many seconds we wait before sending them a second offer.
     */
    int messageInterval;

    public LoyaltyOfferRound2Impl(int messageInterval) {
        super();
        this.messageInterval = messageInterval;
    }

    @Override
    public boolean foundOpportunity(ReportQuotaUsage reportQuotaUsage, long userId, VoltTable userTable,
            VoltTable userOffersTable, VoltTable allTxnsTable, VoltTable userBalanceTable,
            VoltTable userAllocatedTable) {

        // Is this transaction for a call?
        if (allTxnsTable.advanceRow() && allTxnsTable.getString("user_txn_id").endsWith("Web")) {

            // See what offers we have made...
            TimestampType firstLoyaltyOfferTimestamp = reportQuotaUsage.checkForLastOfferTime(userOffersTable,
                    LoyaltyOfferImpl.OFFER_TYPE);
            boolean secondLoyaltyOfferMade = reportQuotaUsage.checkForOfferType(userOffersTable,
                    LoyaltyOfferRound2Impl.OFFER_TYPE);

            final Date cutofftime = new Date(
                    reportQuotaUsage.getTransactionTime().getTime() - (messageInterval * 1000 * 60));

            if (firstLoyaltyOfferTimestamp != null && (!secondLoyaltyOfferMade)
                    && firstLoyaltyOfferTimestamp.asExactJavaDate().before(cutofftime)) {

                // Also See if they are in our loyalty scheme before we make second
                // offer - they may have accepted it..
                ExtraUserData eud = reportQuotaUsage.getExtraUserData(userTable);

                // If they are a platinum member who has finished a call...
                if (eud.loyaltySchemeTier.equals("Platinum")) {

                    // Ands they are not in our loyalty scheme...
                    if (eud.inPlatinumPlan.equals("No")) {

                        LoyaltyOfferPOJO offer = new LoyaltyOfferPOJO(OFFER_TYPE, "CHEAPWEB2",
                                "As a loyal user who has recently callously ignored our "
                                        + "generous offer to join our Platinum plan "
                                        + "we're going to ask you to do so again!");

                        String payload = reportQuotaUsage.getGson().toJson(offer);
                        reportQuotaUsage.makeCVMOffer(userId, OFFER_TYPE, payload);
                        return true;

                    }
                }
            }
        }

        return false;
    }

}
