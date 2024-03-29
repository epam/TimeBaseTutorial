/*
 * Copyright 2023 EPAM Systems, Inc
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
package com.epam.deltix.computations.mul;

import com.epam.deltix.computations.api.annotations.BuiltInTimestampMs;
import com.epam.deltix.computations.api.annotations.Compute;
import com.epam.deltix.computations.api.annotations.Function;
import com.epam.deltix.computations.api.generated.IntToDecimalStatefulFunctionBase;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.qsrv.hf.pub.md.TimebaseTypes;

@Function("MUL")
public class MulInt extends IntToDecimalStatefulFunctionBase {

    @Compute
    @Override
    public void compute(@BuiltInTimestampMs long timestamp, int v) {
        if (TimebaseTypes.isNull(v)) {
            return;
        }
        if (TimebaseTypes.isDecimalNull(value)) {
            value = Decimal64Utils.fromInt(v);
        } else {
            value = Decimal64Utils.multiply(value, Decimal64Utils.fromInt(v));
        }
    }
}