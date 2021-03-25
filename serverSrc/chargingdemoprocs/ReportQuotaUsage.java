package chargingdemoprocs;

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

import org.voltdb.SQLStmt;
import org.voltdb.VoltProcedure;
import org.voltdb.VoltTable;
import org.voltdb.types.TimestampType;

import com.google.gson.Gson;

import chargingdemoprocs.cvm.CVMInterface;
import chargingdemoprocs.cvm.*;

public class ReportQuotaUsage extends VoltProcedure {

    // @formatter:off

	public static final SQLStmt getUser = new SQLStmt(
			"SELECT * FROM user_table WHERE userid = ?;");

    public static final SQLStmt getTxn = new SQLStmt("SELECT txn_time FROM user_recent_transactions "
            + "WHERE userid = ? AND user_txn_id = ?;");

    public static final SQLStmt getAllTxns = new SQLStmt("SELECT * FROM user_recent_transactions "
            + "WHERE userid = ? ORDER BY txn_time DESC, user_txn_id ;");

	public static final SQLStmt getUserBalance = new SQLStmt("SELECT balance, CAST(? AS BIGINT) sessionid FROM user_balance WHERE userid = ?;");

	public static final SQLStmt getCurrrentlyAllocated = new SQLStmt(
			"select nvl(sum(allocated_amount),0)  allocated_amount from user_usage_table where userid = ?;");

	public static final SQLStmt addTxn = new SQLStmt("INSERT INTO user_recent_transactions "
			+ "(userid, user_txn_id, txn_time, approved_amount,spent_amount,purpose,sessionid) VALUES (?,?,NOW,?,?,?,?);");

	public static final SQLStmt delOldUsage = new SQLStmt(
			"DELETE FROM user_usage_table WHERE userid = ? AND sessionid = ?;");

	public static final SQLStmt reportFinancialEvent = new SQLStmt(
			"INSERT INTO user_financial_events (userid,amount,user_txn_id,message) VALUES (?,?,?,?);");

	public static final SQLStmt createAllocation = new SQLStmt("INSERT INTO user_usage_table "
			+ "(userid, allocated_amount,sessionid, lastdate) VALUES (?,?,?,NOW);");

    public static final SQLStmt getUserOffers = new SQLStmt(
            "SELECT * "
            + "FROM user_cvm_offers "
            + "WHERE userid = ? "
            + "ORDER BY userid,offer_date,offer_type;");

    public static final SQLStmt recordOffer = new SQLStmt(
            "INSERT INTO  user_cvm_offers "
            + "(userid,offer_date,offer_type,offer_payload)"
            + "VALUES"
            + "(?,NOW,?,?)");
    
    Gson gson = new Gson();

    CVMInterface[] cvmOpportunities = { 
             new GamingOfferImpl()
            ,new LoyaltyOfferImpl(100)
            //,new LoyaltyOfferRound2Impl(2) // will uncomment later in the Run Book
            };

    // @formatter:on

    public VoltTable[] run(long userId, int unitsUsed, int unitsWanted, long inputSessionId, String txnId)
            throws VoltAbortException {

        long sessionId = inputSessionId;

        if (sessionId <= 0) {
            sessionId = this.getUniqueId();
        }

        voltQueueSQL(getUser, userId);
        voltQueueSQL(getTxn, userId, txnId);

        VoltTable[] results1 = voltExecuteSQL();

        // Sanity check: Does this user exist?
        if (!results1[0].advanceRow()) {
            throw new VoltAbortException("User " + userId + " does not exist");
        }

        // Sanity Check: Is this a re-send of a transaction we've already done?
        if (results1[1].advanceRow()) {
            this.setAppStatusCode(ReferenceData.STATUS_TXN_ALREADY_HAPPENED);
            this.setAppStatusString(
                    "Event already happened at " + results1[1].getTimestampAsTimestamp("txn_time").toString());
            return voltExecuteSQL(true);
        }

        long amountSpent = unitsUsed * -1;
        String decision = "Spent " + amountSpent;

        // Update balance
        voltQueueSQL(reportFinancialEvent, userId, amountSpent, txnId, "Spent " + amountSpent);

        // Delete old usage record
        voltQueueSQL(delOldUsage, userId, sessionId);
        voltQueueSQL(getUserBalance, sessionId, userId);
        voltQueueSQL(getCurrrentlyAllocated, userId);

        this.setAppStatusCode(ReferenceData.STATUS_OK);

        if (unitsWanted > 0) {

            VoltTable[] results2 = voltExecuteSQL();

            VoltTable userBalance = results2[2];
            VoltTable allocated = results2[3];

            // Calculate how much money is actually available...

            userBalance.advanceRow();
            long availableCredit = userBalance.getLong("balance");

            if (allocated.advanceRow()) {
                availableCredit = availableCredit - allocated.getLong("allocated_amount");
            }

            long amountApproved = 0;

            if (availableCredit < 0) {

                decision = decision + "; Negative balance: " + availableCredit;
                this.setAppStatusCode(ReferenceData.STATUS_NO_MONEY);

            } else if (unitsWanted > availableCredit) {

                amountApproved = availableCredit;
                decision = decision + "; Allocated " + availableCredit + " units of " + unitsWanted + " asked for";
                this.setAppStatusCode(ReferenceData.STATUS_SOME_UNITS_ALLOCATED);

            } else {

                amountApproved = unitsWanted;
                decision = decision + "; Allocated " + unitsWanted;
                this.setAppStatusCode(ReferenceData.STATUS_ALL_UNITS_ALLOCATED);

            }

            voltQueueSQL(createAllocation, userId, amountApproved, sessionId);

            this.setAppStatusString(decision);
            // Note that transaction is now 'official'

            voltQueueSQL(addTxn, userId, txnId, amountApproved, amountSpent, decision, sessionId);

        }

        VoltTable[] transactionResults = getSessionState(userId, sessionId);

        for (int i = 0; i < cvmOpportunities.length; i++) {

            VoltTable userTable = transactionResults[transactionResults.length - 5];
            VoltTable userOffersTable = transactionResults[transactionResults.length - 4];
            VoltTable allTxnsTable = transactionResults[transactionResults.length - 3];
            VoltTable userBalanceTable = transactionResults[transactionResults.length - 2];
            VoltTable userAllocatedTable = transactionResults[transactionResults.length - 1];

            if (cvmOpportunities[i].foundOpportunity(this, userId, userTable, userOffersTable, allTxnsTable,
                    userBalanceTable, userAllocatedTable)) {
                transactionResults = getSessionState(userId, sessionId);
            } else {
                for (int j = 0; j < transactionResults.length; j++) {
                    transactionResults[j].resetRowPosition();
                }
            }
        }

        return transactionResults;

    }

    private VoltTable[] getSessionState(long userId, long sessionId) {

        voltQueueSQL(getUser, userId);
        voltQueueSQL(getUserOffers, userId);
        voltQueueSQL(getAllTxns, userId);
        voltQueueSQL(getUserBalance, sessionId, userId);
        voltQueueSQL(getCurrrentlyAllocated, userId);

        return (voltExecuteSQL());

    }

    public void makeCVMOffer(long userId, String offerType, String payload) {
        voltQueueSQL(recordOffer, userId, offerType, payload);
    }

    /**
     * @return the gson
     */
    public Gson getGson() {
        return gson;
    }

    /**
     * Used by instances of CVMInterface
     * 
     * @param userOffersTable
     * @param offerType       - type of offer we are looking for
     * @return true if offer found
     */
    public boolean checkForOfferType(VoltTable userOffersTable, String offerType) {

        userOffersTable.resetRowPosition();

        // Have we made such an offer recently?
        boolean offerMade = false;

        while (userOffersTable.advanceRow()) {
            if (userOffersTable.getString("offer_type").equalsIgnoreCase(offerType)) {
                offerMade = true;
                break;
            }
        }
        return offerMade;
    }

    /**
     * Used by instances of CVMInterface
     * 
     * @param userOffersTable - we assume the rows are in date order...
     * @param offerType
     * @return TimestampType last time offer 'offertype' was made, or null...
     */
    public TimestampType checkForLastOfferTime(VoltTable userOffersTable, String offerType) {

        TimestampType lastOfferDate = null;

        userOffersTable.resetRowPosition();

        while (userOffersTable.advanceRow()) {
            if (userOffersTable.getString("offer_type").equalsIgnoreCase(offerType)) {
                lastOfferDate = userOffersTable.getTimestampAsTimestamp("offer_date");
            }
        }

        return lastOfferDate;
    }

    /**
     * Instantiate ExtraUserData from JSON
     * 
     * @param reportQuotaUsage
     * @param userTable
     * @return ExtraUserData
     */
    public ExtraUserData getExtraUserData(VoltTable userTable) {
        userTable.advanceRow();
        String userDataAsJson = userTable.getString("user_json_object");
        ExtraUserData eud = getGson().fromJson(userDataAsJson, ExtraUserData.class);
        return eud;
    }

}
