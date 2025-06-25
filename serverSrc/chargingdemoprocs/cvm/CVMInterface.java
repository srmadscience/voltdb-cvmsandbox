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
 * Interface implemented by CVM classes. Instances of it have a 'foundOpportunity' method that is 
 * invoked at the end of each call to ReportQuotaUsage.run().
 *
 */
public interface CVMInterface {  

    /**
     * 
     * See if we can identify and act on an opportunity. Note that we are passed a handle 
     * to reportQuotaUsage, which has methods  such as makeCVMOffer we can use to 
     * change database state. See GamingOfferImpl for an example of how to do this.
     * 
     * @param reportQuotaUsage - instance of Procedure class.
     * @param userId - id of user
     * @param userTable - contains user_table row for userid
     * @param userOffersTable - contain user_cvm_offers rows for this userid.
     * @param allTxnsTable - constraints all recent transactions for this user
     * @param userBalanceTable - contains current balance
     * @param userAllocatedTable - contains current reservations
     * @return true if an opportunity has been identified and acted upon. 
     */
    public boolean foundOpportunity(ReportQuotaUsage reportQuotaUsage, long userId, VoltTable userTable, VoltTable userOffersTable,
            VoltTable allTxnsTable, VoltTable userBalanceTable, VoltTable userAllocatedTable);
    

}
