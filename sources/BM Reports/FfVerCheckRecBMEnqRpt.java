package com.temenos.fusion;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.enquiryreport.EnquiryClass;
import com.temenos.t24.api.records.enquiryreport.EnquiryReportRecord;

public class FfVerCheckRecBMEnqRpt extends RecordLifecycle {

    @Override
    public void defaultFieldValues(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
        try {
            if (transactionContext.getCurrentFunction().equals("INPUT")) {
                EnquiryReportRecord enqRptRec = new EnquiryReportRecord(currentRecord);
                EnquiryClass enqCls = enqRptRec.getEnquiry(0);
                enqCls.setEnquiry(currentRecordId);
                enqCls.clearSelection();
                enqRptRec.setEnquiry(enqCls, 0);
                currentRecord.set(enqRptRec.toStructure());
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

}
