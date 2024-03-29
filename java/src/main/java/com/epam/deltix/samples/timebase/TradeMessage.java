/*
 * Copyright 2021 EPAM Systems, Inc
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.epam.deltix.samples.timebase;

import com.epam.deltix.timebase.messages.*;

/**
 * Basic information about a market trade.
 */
@SchemaElement(
        name = "TradeMessage",
        title = "Trade Message"
)
public class TradeMessage extends InstrumentMessage {
    public static final String CLASS_NAME = TradeMessage.class.getName();

    /**
     * Exchange code compressed to long using ALPHANUMERIC(10) encoding
     */
    protected long exchangeId = TypeConstants.EXCHANGE_NULL;

    /**
     * The trade price.
     */
    protected double price = TypeConstants.IEEE64_NULL;

    /**
     * The trade size.
     */
    protected double size = TypeConstants.IEEE64_NULL;

    /**
     * Market specific trade condition.
     */
    protected CharSequence condition = null;

    /**
     * Net change from previous days closing price vs. last traded price.
     */
    protected double netPriceChange = TypeConstants.IEEE64_NULL;

    /**
     * Exchange code compressed to long using ALPHANUMERIC(10) encoding
     * @return Exchange Id
     */
    @SchemaType(
            encoding = "ALPHANUMERIC(10)",
            dataType = SchemaDataType.VARCHAR
    )
    @SchemaElement
    @OldElementName("exchangeCode")
    public long getExchangeId() {
        return exchangeId;
    }

    /**
     * Exchange code compressed to long using ALPHANUMERIC(10) encoding
     * @param value - Exchange Id
     */
    public void setExchangeId(long value) {
        this.exchangeId = value;
    }

    /**
     * Exchange code compressed to long using ALPHANUMERIC(10) encoding
     * @return true if Exchange Id is not null
     */
    public boolean hasExchangeId() {
        return exchangeId != TypeConstants.EXCHANGE_NULL;
    }

    /**
     * Exchange code compressed to long using ALPHANUMERIC(10) encoding
     */
    public void nullifyExchangeId() {
        this.exchangeId = TypeConstants.EXCHANGE_NULL;
    }

    /**
     * The trade price.
     * @return Price
     */
    @SchemaType(
            encoding = "DECIMAL(8)",
            dataType = SchemaDataType.FLOAT
    )
    @SchemaElement
    public double getPrice() {
        return price;
    }

    /**
     * The trade price.
     * @param value - Price
     */
    public void setPrice(double value) {
        this.price = value;
    }

    /**
     * The trade price.
     * @return true if Price is not null
     */
    public boolean hasPrice() {
        return !Double.isNaN(price);
    }

    /**
     * The trade price.
     */
    public void nullifyPrice() {
        this.price = TypeConstants.IEEE64_NULL;
    }

    /**
     * The trade size.
     * @return Size
     */
    @SchemaType(
            encoding = "DECIMAL(8)",
            dataType = SchemaDataType.FLOAT
    )
    @SchemaElement
    public double getSize() {
        return size;
    }

    /**
     * The trade size.
     * @param value - Size
     */
    public void setSize(double value) {
        this.size = value;
    }

    /**
     * The trade size.
     * @return true if Size is not null
     */
    public boolean hasSize() {
        return !Double.isNaN(size);
    }

    /**
     * The trade size.
     */
    public void nullifySize() {
        this.size = TypeConstants.IEEE64_NULL;
    }

    /**
     * Market specific trade condition.
     * @return Condition
     */
    @SchemaType(
            encoding = "UTF8",
            dataType = SchemaDataType.VARCHAR
    )
    @SchemaElement
    public CharSequence getCondition() {
        return condition;
    }

    /**
     * Market specific trade condition.
     * @param value - Condition
     */
    public void setCondition(CharSequence value) {
        this.condition = value;
    }

    /**
     * Market specific trade condition.
     * @return true if Condition is not null
     */
    public boolean hasCondition() {
        return condition != null;
    }

    /**
     * Market specific trade condition.
     */
    public void nullifyCondition() {
        this.condition = null;
    }

    /**
     * Net change from previous days closing price vs. last traded price.
     * @return Net Price Change
     */
    @SchemaElement
    public double getNetPriceChange() {
        return netPriceChange;
    }

    /**
     * Net change from previous days closing price vs. last traded price.
     * @param value - Net Price Change
     */
    public void setNetPriceChange(double value) {
        this.netPriceChange = value;
    }

    /**
     * Net change from previous days closing price vs. last traded price.
     * @return true if Net Price Change is not null
     */
    public boolean hasNetPriceChange() {
        return !Double.isNaN(netPriceChange);
    }

    /**
     * Net change from previous days closing price vs. last traded price.
     */
    public void nullifyNetPriceChange() {
        this.netPriceChange = TypeConstants.IEEE64_NULL;
    }

    /**
     * Creates new instance of this class.
     * @return new instance of this class.
     */
    @Override
    protected TradeMessage createInstance() {
        return new TradeMessage();
    }

    /**
     * Method nullifies all instance properties
     */
    @Override
    public TradeMessage nullify() {
        super.nullify();
        nullifyExchangeId();
        nullifyPrice();
        nullifySize();
        nullifyCondition();
        nullifyNetPriceChange();
        return this;
    }

    /**
     * Resets all instance properties to their default values
     */
    @Override
    public TradeMessage reset() {
        super.reset();
        exchangeId = TypeConstants.EXCHANGE_NULL;
        price = TypeConstants.IEEE64_NULL;
        size = TypeConstants.IEEE64_NULL;
        condition = null;
        netPriceChange = TypeConstants.IEEE64_NULL;
        return this;
    }

    /**
     * Method copies state to a given instance
     */
    @Override
    public TradeMessage clone() {
        TradeMessage t = createInstance();
        t.copyFrom(this);
        return t;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        boolean superEquals = super.equals(obj);
        if (!superEquals) return false;
        if (!(obj instanceof TradeMessage)) return false;
        TradeMessage other = (TradeMessage)obj;
        if (hasExchangeId() != other.hasExchangeId()) return false;
        if (hasExchangeId() && getExchangeId() != other.getExchangeId()) return false;
        if (hasPrice() != other.hasPrice()) return false;
        if (hasPrice() && getPrice() != other.getPrice()) return false;
        if (hasSize() != other.hasSize()) return false;
        if (hasSize() && getSize() != other.getSize()) return false;
        if (hasCondition() != other.hasCondition()) return false;
        if (hasCondition()) {
            if (getCondition().length() != other.getCondition().length()) return false; else {
                CharSequence s1 = getCondition();
                CharSequence s2 = other.getCondition();
                if (!com.epam.deltix.containers.CharSequenceUtils.equals(s1, s2))
                    return false;
            }
        }
        if (hasNetPriceChange() != other.hasNetPriceChange()) return false;
        if (hasNetPriceChange() && getNetPriceChange() != other.getNetPriceChange()) return false;
        return true;
    }

    /**
     * Returns a hash code value for the object. This method is * supported for the benefit of hash tables such as those provided by.
     */
    @Override
    public int hashCode() {
        int hash = super.hashCode();
        if (hasExchangeId()) {
            hash = hash * 31 + ((int)(getExchangeId() ^ (getExchangeId() >>> 32)));
        }
        if (hasPrice()) {
            hash = hash * 31 + ((int)(Double.doubleToLongBits(getPrice()) ^ (Double.doubleToLongBits(getPrice()) >>> 32)));
        }
        if (hasSize()) {
            hash = hash * 31 + ((int)(Double.doubleToLongBits(getSize()) ^ (Double.doubleToLongBits(getSize()) >>> 32)));
        }
        if (hasCondition()) {
            hash = hash * 31 + getCondition().hashCode();
        }
        if (hasNetPriceChange()) {
            hash = hash * 31 + ((int)(Double.doubleToLongBits(getNetPriceChange()) ^ (Double.doubleToLongBits(getNetPriceChange()) >>> 32)));
        }
        return hash;
    }

    /**
     * Method copies state to a given instance
     * @param template class instance that should be used as a copy source
     */
    @Override
    public TradeMessage copyFrom(RecordInfo template) {
        super.copyFrom(template);
        if (template instanceof TradeMessage) {
            TradeMessage t = (TradeMessage)template;
            if (t.hasExchangeId()) {
                setExchangeId(t.getExchangeId());
            } else {
                nullifyExchangeId();
            }
            if (t.hasPrice()) {
                setPrice(t.getPrice());
            } else {
                nullifyPrice();
            }
            if (t.hasSize()) {
                setSize(t.getSize());
            } else {
                nullifySize();
            }
            if (t.hasCondition()) {
                if (hasCondition() && getCondition() instanceof StringBuilder) {
                    ((StringBuilder)getCondition()).setLength(0);
                } else {
                    setCondition(new StringBuilder());
                }
                ((StringBuilder)getCondition()).append(t.getCondition());
            } else {
                nullifyCondition();
            }
            if (t.hasNetPriceChange()) {
                setNetPriceChange(t.getNetPriceChange());
            } else {
                nullifyNetPriceChange();
            }
        }
        return this;
    }

    /**
     * @return a string representation of this class object.
     */
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        return toString(str).toString();
    }

    /**
     * @return a string representation of this class object.
     */
    @Override
    public StringBuilder toString(StringBuilder str) {
        str.append("{ \"$type\":  \"TradeMessage\"");
        if (hasExchangeId()) {
            str.append(", \"exchangeId\": ").append(getExchangeId());
        }
        if (hasPrice()) {
            str.append(", \"price\": ").append(getPrice());
        }
        if (hasSize()) {
            str.append(", \"size\": ").append(getSize());
        }
        if (hasCondition()) {
            str.append(", \"condition\": \"").append(getCondition()).append("\"");
        }
        if (hasNetPriceChange()) {
            str.append(", \"netPriceChange\": ").append(getNetPriceChange());
        }

        if (hasTimeStampMs()) {
            str.append(", \"timestamp\": \"").append(formatNanos(getTimeStampMs(), (int)getNanoTime())).append("\"");
        }
        if (hasSymbol()) {
            str.append(", \"symbol\": \"").append(getSymbol()).append("\"");
        }
        str.append("}");
        return str;
    }
}

