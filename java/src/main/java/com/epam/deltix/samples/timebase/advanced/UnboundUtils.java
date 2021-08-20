package com.epam.deltix.samples.timebase.advanced;

import com.epam.deltix.qsrv.hf.pub.NullValueException;
import com.epam.deltix.qsrv.hf.pub.ReadableValue;
import com.epam.deltix.qsrv.hf.pub.WritableValue;
import com.epam.deltix.qsrv.hf.pub.codec.UnboundDecoder;
import com.epam.deltix.qsrv.hf.pub.codec.UnboundEncoder;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.util.time.GMT;

import java.text.ParseException;

/**
 *
 */
public abstract class UnboundUtils {

    static String       toString(ReadableValue udec) throws NullValueException {
        try {
            final UnboundDecoder decoder = udec.getFieldDecoder();
            final StringBuilder sb = new StringBuilder();
            sb.append(decoder.getClassInfo().getDescriptor().getName()).append(":[");
            // dump field/value pairs
            while (decoder.nextField()) {
                sb.append(decoder.getField().getName()).append("=");
                try {
                    sb.append(decoder.getString()).append(',');
                } catch (NullValueException e) {
                    sb.append("null,");
                }
            }
            sb.append(']');
            return sb.toString();
        } catch (NullValueException e) {
            return "null";
        }
    }

    static String       toString(ArrayDataType type, ReadableValue udec) throws NullValueException {
        final StringBuilder sb = new StringBuilder();
        final int len = udec.getArrayLength();
        final DataType underlineType = type.getElementDataType();

        sb.append('[');
        for (int i = 0; i < len; i++) {
            try {
                final ReadableValue rv = udec.nextReadableElement();

                if (underlineType instanceof FloatDataType)
                    sb.append(rv.getDouble());
                else if (underlineType instanceof IntegerDataType)
                    sb.append(rv.getLong());
                else if (underlineType instanceof ClassDataType)
                    sb.append(toString(rv));
                else
                    throw new IllegalArgumentException(underlineType + " is not expected");

                sb.append(',');
            } catch (NullValueException e) {
                sb.append("null,");
            }
        }

        sb.append(']');
        return sb.toString();
    }
}
