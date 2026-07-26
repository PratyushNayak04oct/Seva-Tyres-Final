package com.finixis.db;

import org.h2.api.Trigger;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * H2 trigger that keeps Transaction_Credit.balance in sync with
 * total_amount - paid_amount on every INSERT/UPDATE.
 * Columns (0-based):  0=transaction_id, 1=customer_id,
 *                     2=total_amount,   3=paid_amount,
 *                     4=balance,        5=transaction_date,
 *                     6=is_settled,     7=notes
 */
public class BalanceTrigger implements Trigger {

    @Override
    public void init(Connection conn, String schemaName, String triggerName,
                     String tableName, boolean before, int type) {}

    @Override
    public void fire(Connection conn, Object[] oldRow, Object[] newRow) throws SQLException {
        if (newRow == null) return;
        double total  = newRow[2] == null ? 0.0 : ((Number) newRow[2]).doubleValue();
        double paid   = newRow[3] == null ? 0.0 : ((Number) newRow[3]).doubleValue();
        double balance = total - paid;
        newRow[4] = balance;
        newRow[6] = balance <= 0.0; // is_settled
    }

    @Override
    public void close() {}

    @Override
    public void remove() {}
}
