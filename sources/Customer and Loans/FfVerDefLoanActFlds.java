package com.temenos.fusion;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.ebffloanactivity.EbFfLoanActivityRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;


public class FfVerDefLoanActFlds extends RecordLifecycle {
    String arrId = "";
    @Override
    public void defaultFieldValues(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
        DataAccess da = new DataAccess(this);
        Session session = new Session(this);
        String finMnemonic = session.getCompanyRecord().getFinancialMne().getValue();
        try {
            EbFfLoanActivityRecord loanActRec = new EbFfLoanActivityRecord(currentRecord);
            arrId = loanActRec.getArrangementId().getValue();
            if(!arrId.equals("")) {
                AaArrangementRecord arrRec = new AaArrangementRecord(da.getRecord(finMnemonic, "AA.ARRANGEMENT", "", arrId));
                loanActRec.setLoanCreationDate(arrRec.getStartDate().getValue());
            }
            currentRecord.set(loanActRec.toStructure());
        } catch (Exception e) {
            e.getMessage();
        }
    }

}
