package org.dbos.apiary.procedures.voltdb.tests;

import org.dbos.apiary.function.ApiaryFuture;
import org.dbos.apiary.function.ApiaryTransactionalContext;
import org.dbos.apiary.voltdb.VoltFunction;
import org.voltdb.SQLStmt;
import org.voltdb.VoltTable;

import java.lang.reflect.InvocationTargetException;

public class CounterFunction extends VoltFunction {

    public final SQLStmt getValue = new SQLStmt(
            "SELECT KVValue FROM KVTable WHERE KVKey=?;"
    );

    public VoltTable[] run(int pkey, VoltTable voltInput) throws InvocationTargetException, IllegalAccessException {
        return super.run(pkey, voltInput);
    }

    public ApiaryFuture runFunction(ApiaryTransactionalContext context, String keyString) {
        int key = Integer.parseInt(keyString);

        VoltTable res = ((VoltTable[]) context.apiaryExecuteQuery(getValue, key))[0];
        int value;
        if (res.getRowCount() > 0) {
            value = (int) res.fetchRow(0).getLong(0);
        } else {
            value = 0;
        }
        ApiaryFuture incrementedValue = context.apiaryQueueFunction("StatelessIncrement", String.valueOf(value));
        context.apiaryQueueFunction("InsertFunction", keyString, incrementedValue);
        return incrementedValue;
    }
}
