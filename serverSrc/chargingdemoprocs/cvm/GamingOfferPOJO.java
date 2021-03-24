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

/**
 * Class to hold Gaming offer data for JSON purposes,
 *
 */
public class GamingOfferPOJO {
    
    public String offerType;
    
    public String offerCode;

    
    public GamingOfferPOJO(String offerType, String offerCode) {
        super();
        this.offerType = offerType;
        this.offerCode = offerCode;
    }


    /**
     * @return the offerType
     */
    public String getOfferType() {
        return offerType;
    }

    /**
     * @param offerType the offerType to set
     */
    public void setOfferType(String offerType) {
        this.offerType = offerType;
    }

    /**
     * @return the offerCode
     */
    public String getOfferCode() {
        return offerCode;
    }

    /**
     * @param offerCode the offerCode to set
     */
    public void setOfferCode(String offerCode) {
        this.offerCode = offerCode;
    }
    

}
