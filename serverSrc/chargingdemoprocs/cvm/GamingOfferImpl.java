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

import chargingdemoprocs.ReportQuotaUsage;

/**
 * Send a offer for our gaming package IFF:
 * <p> 1. The latest transaction is for gaming bandwidth
 * <p> 2. We haven't made a gaming offer recently
 *
 */
public class GamingOfferImpl implements CVMInterface {

    public static final String OFFER_TYPE = "Gaming";

    @Override
    public boolean foundOpportunity(ReportQuotaUsage reportQuotaUsage, long userId, VoltTable userTable, VoltTable userOffersTable,
            VoltTable allTxnsTable, VoltTable userBalanceTable, VoltTable userAllocatedTable) {
 
        // Is this transaction for gaming bandwidth?      
        if (allTxnsTable.advanceRow() && allTxnsTable.getString("user_txn_id").endsWith(OFFER_TYPE)) {
            
            boolean offerMade = reportQuotaUsage.checkForOfferType(userOffersTable,OFFER_TYPE);     
            
            // No offer has been made, so make one...
            if (offerMade == false) {
                
                GamingOfferPOJO offer = new GamingOfferPOJO(OFFER_TYPE, "20PCTOFF");
                String payload = reportQuotaUsage.getGson().toJson(offer);
                
                reportQuotaUsage.makeCVMOffer(userId,OFFER_TYPE,payload);
                return true;
            }
            
        }
        
        return false;
    }

}
