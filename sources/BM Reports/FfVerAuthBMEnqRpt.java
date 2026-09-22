package com.temenos.fusion;

import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.enquiryreport.EnquiryReportRecord;

public class FfVerAuthBMEnqRpt extends RecordLifecycle {

    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {
        try {
            EnquiryReportRecord enqReportRec = new EnquiryReportRecord(currentRecord);
            TransactionData txnData = new TransactionData();
            txnData.setVersionId("ENQUIRY.REPORT,FF.BM.INPUT");
            txnData.setTransactionId(currentRecordId);
            txnData.setFunction("VERIFY");
            transactionData.add(txnData);
            txnData.setSourceId("FF.BM.RPT");
            currentRecords.add(enqReportRec.toStructure());
        } catch (Exception e) {
            e.getMessage();
        }
    }
    
    
    

}
