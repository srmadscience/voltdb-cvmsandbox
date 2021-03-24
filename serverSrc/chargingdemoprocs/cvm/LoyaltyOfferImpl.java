package chargingdemoprocs.cvm;

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

import chargingdemoprocs.ExtraUserData;
import chargingdemoprocs.ReportQuotaUsage;

/**
 * Send a offer for our Platinum Call Package IFF:
 * 
 * <p> 1. The latest transaction is for Calls.
 * <p> 2. The user has spent >= callThreshold on calls recently.
 * <p> 3. The user is a Platinum member of our loyalty club.
 * <p> 4. The user is not in our special platinum call plan
 * <p> 5. We haven't made a Platinum Call Plan offer recently
 *
 */
public class LoyaltyOfferImpl implements CVMInterface {
    
    public static final String OFFER_TYPE = "Loyalty";
    
    /**
     * How much has to be spent on calls before we try and make this offer...
     */
    int callThreshold;

    public LoyaltyOfferImpl(int callThreshold) {
        super();
        this.callThreshold = callThreshold;
    }

    @Override
    public boolean foundOpportunity(ReportQuotaUsage reportQuotaUsage, long userId, VoltTable userTable,
            VoltTable userOffersTable, VoltTable allTxnsTable, VoltTable userBalanceTable,
            VoltTable userAllocatedTable) {

        // Is this transaction for a call?
        if (allTxnsTable.advanceRow() && allTxnsTable.getString("user_txn_id").endsWith("Web")) {

            // Have we spent more than callThreshold units on calls recently?

            // This latest call cost...
            long totalSpentOnCalls = allTxnsTable.getLong("spent_amount");

            // And we'll add all the other ones..
            while (allTxnsTable.advanceRow()) {
                if (allTxnsTable.getString("user_txn_id").endsWith("Calls")) {
                    totalSpentOnCalls = totalSpentOnCalls + allTxnsTable.getLong("spent_amount");
                }
            }

            
            totalSpentOnCalls = totalSpentOnCalls * -1;
            
            ExtraUserData eud = reportQuotaUsage.getExtraUserData(userTable);
            
            // If they are a platinum member who has finished a call...
            if (eud.loyaltySchemeTier.equals("Platinum")) {

                // Ands they are not in our loyalty scheme...
                if (eud.inPlatinumPlan.equals("No")) {

                    // And they have spent a lot of money on calls...
                    if (totalSpentOnCalls > callThreshold) {

                        boolean offerMade = reportQuotaUsage.checkForOfferType(userOffersTable,OFFER_TYPE);

                        // No offer has been made, so make one...
                        if (offerMade == false) {

                            LoyaltyOfferPOJO offer = new LoyaltyOfferPOJO(OFFER_TYPE, "CHEAPWEB",
                                    "As a loyal user who has recently spent " + totalSpentOnCalls
                                     + " on calls we're delighted to offer you a chance to "
                                     + "join our Platinum plan!");

                            String payload = reportQuotaUsage.getGson().toJson(offer);
                            reportQuotaUsage.makeCVMOffer(userId, OFFER_TYPE, payload);
                            return true;
                        }
                    }

                }
            }

        }

        return false;
    }

 
}
