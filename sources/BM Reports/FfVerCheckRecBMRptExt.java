package com.temenos.fusion;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebffbmreportext.EbFfBmReportExtRecord;

public class FfVerCheckRecBMRptExt extends RecordLifecycle {

    @Override
    public void defaultFieldValues(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
        try {
            if (transactionContext.getCurrentFunction().equals("INPUT")) {
                EbFfBmReportExtRecord ffBmRptExtRec = new EbFfBmReportExtRecord(currentRecord);
                
                ffBmRptExtRec.getGroupBy().setValue("");
                ffBmRptExtRec.getDateFrom().setValue("");
                ffBmRptExtRec.getDateTo().setValue("");
                ffBmRptExtRec.getRo().setValue("");
                ffBmRptExtRec.getProduct().setValue("");
                ffBmRptExtRec.getCenter().setValue("");
                ffBmRptExtRec.getVillage().setValue("");
                ffBmRptExtRec.getDistrict().setValue("");
                ffBmRptExtRec.getCycle().setValue("");
                ffBmRptExtRec.getPurpose().setValue("");
                ffBmRptExtRec.getReligion().setValue("");
                ffBmRptExtRec.getCaste().setValue("");
                ffBmRptExtRec.getLoanWise().setValue("");
                ffBmRptExtRec.getTimeStamp().setValue(String.valueOf(System.currentTimeMillis()));
                currentRecord.set(ffBmRptExtRec.toStructure());
            }
        } catch (Exception e) {
            e.getMessage();
        }
        
    }
}
