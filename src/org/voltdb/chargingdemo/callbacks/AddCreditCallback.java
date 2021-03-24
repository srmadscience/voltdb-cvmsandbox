package org.voltdb.chargingdemo.callbacks;

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

import java.text.SimpleDateFormat;
import java.util.Date;

import org.voltdb.VoltTable;
import org.voltdb.chargingdemo.UserTransactionState;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.ProcedureCallback;

import chargingdemoprocs.ReferenceData;

public class AddCreditCallback implements ProcedureCallback {

	UserTransactionState userTransactionState;

	public AddCreditCallback(UserTransactionState userTransactionState) {
		this.userTransactionState = userTransactionState;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.voltdb.chargingdemo.ReportLatencyCallback#clientCallback(org.voltdb.
	 * client.ClientResponse)
	 */
	@Override
	public void clientCallback(ClientResponse arg0) throws Exception {

		if (arg0.getStatus() == ClientResponse.SUCCESS) {

			if (arg0.getAppStatus() == ReferenceData.STATUS_CREDIT_ADDED) {

				userTransactionState.endTran();

				VoltTable balanceTable = arg0.getResults()[arg0.getResults().length - 2];
				VoltTable reservationTable = arg0.getResults()[arg0.getResults().length - 1];

				if (balanceTable.advanceRow()) {

					long balance = balanceTable.getLong("balance");
					long reserved = 0;

					if (reservationTable.advanceRow()) {
						reserved = reservationTable.getLong("allocated_amount");
						if (reservationTable.wasNull()) {
							reserved = 0;
						}
					}

					userTransactionState.currentlyReserved = reserved;
					userTransactionState.spendableBalance = balance - reserved;

				}
			} else {
				msg("AddCreditCallback user=" + userTransactionState.id + ":" + arg0.getAppStatusString());
			}
		} else {
			msg("AddCreditCallback user=" + userTransactionState.id + ":" + arg0.getStatusString());
		}
	}

	/**
	 * Print a formatted message.
	 * 
	 * @param message
	 */
	public static void msg(String message) {

		SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date now = new Date();
		String strDate = sdfDate.format(now);
		System.out.println(strDate + ":" + message);

	}

}
