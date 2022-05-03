-- Allow more concurrent connections.
SELECT SET_CONFIG_PARAMETER ('MaxClientSessions', 1000);

DROP TABLE IF EXISTS FUNCINVOCATIONS;

CREATE TABLE FUNCINVOCATIONS (
    APIARY_TRANSACTION_ID BIGINT NOT NULL,
    APIARY_EXPORT_TIMESTAMP BIGINT NOT NULL,
    EXECUTIONID BIGINT NOT NULL,  -- Unique execution ID of the entire workflow.
    SERVICE VARCHAR(1024) NOT NULL,
    PROCEDURENAME VARCHAR(1024) NOT NULL
);

CREATE TABLE KVTABLE (
    APIARY_TRANSACTION_ID BIGINT NOT NULL,
    APIARY_EXPORT_TIMESTAMP BIGINT NOT NULL,
    APIARY_EXPORT_OPERATION BIGINT NOT NULL,  -- 1=insert, 2=delete, 3=update (value after updates), 100=read.
    KVKEY BIGINT NOT NULL,
    KVVALUE BIGINT NOT NULL
);
