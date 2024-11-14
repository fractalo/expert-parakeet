package com.github.fractalo.streaming_settlement.config.datasource;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class TransactionRoutingDataSource extends AbstractRoutingDataSource {
    @Override
    protected Object determineCurrentLookupKey() {
        if (TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
            return DataSourceRole.READ_ONLY;
        } else {
            return DataSourceRole.READ_WRITE;
        }
    }
}
