package com.github.manu156.pinpointintegration.common.constant;

import javax.naming.OperationNotSupportedException;
import java.time.format.DateTimeFormatter;

public class Constants {

    private Constants() throws OperationNotSupportedException {
        throw new OperationNotSupportedException("Constants Class!");
    }

    public static final String TRANSACTION_METADATA_PINPOINT = "/transactionmetadata.pinpoint";
    public static final String[] TRANSACTIONS_LIST_COLUMN_HEADERS = new String[]{
            "StartTime", "Path", "Response Time", "Exception", "AgentId", "EndPoint",
            "ClientIp", "Transaction", "AgentName"
    };
    public static final DateTimeFormatter DATE_TIME_FORMAT_TRANSACTION_LIST =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS dd-MM-yyyy");

}
