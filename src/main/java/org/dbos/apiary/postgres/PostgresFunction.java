package org.dbos.apiary.postgres;

import org.dbos.apiary.function.ApiaryFunction;
import org.dbos.apiary.function.ApiaryContext;
import org.dbos.apiary.function.ApiaryTransactionalContext;
import org.dbos.apiary.utilities.ApiaryConfig;
import org.dbos.apiary.utilities.Utilities;

/**
 * All Postgres functions should extend this class and implement <code>runFunction</code>.
 */
public class PostgresFunction implements ApiaryFunction {
    @Override
    public void recordInvocation(ApiaryContext ctxt, String funcName) {
        if (ctxt.provBuff == null) {
            // If no OLAP DB available.
            return;
        }
        long timestamp = Utilities.getMicroTimestamp();
        long txid = ((ApiaryTransactionalContext) ctxt).apiaryGetTransactionId();
        ctxt.provBuff.addEntry(ApiaryConfig.tableFuncInvocations, txid, timestamp, ctxt.execID, ctxt.service, funcName);
    }
}
