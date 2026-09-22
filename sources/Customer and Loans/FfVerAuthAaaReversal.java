package com.temenos.fusion;

import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.aaactivityhistory.AaActivityHistoryRecord;
import com.temenos.t24.api.records.aaactivityhistory.ActivityRefClass;
import com.temenos.t24.api.records.aaactivityhistory.EffectiveDateClass;
import com.temenos.t24.api.records.ebffloanactivity.EbFfLoanActivityRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

public class FfVerAuthAaaReversal extends RecordLifecycle {
    String actRef = "";
    String arrId = "";

    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {
        DataAccess da = new DataAccess(this);
        Session session = new Session(this);
        String finMnemonic = session.getCompanyRecord().getFinancialMne().getValue();
        TransactionData txnData = new TransactionData();
        EbFfLoanActivityRecord loanActRec = new EbFfLoanActivityRecord(currentRecord);
        arrId = loanActRec.getArrangementId().getValue();
        if (!arrId.equals("")) {
            try {
                AaActivityHistoryRecord aaActHistRec = new AaActivityHistoryRecord(
                        da.getRecord(finMnemonic, "AA.ACTIVITY.HISTORY", "", arrId));
                for (EffectiveDateClass effectiveDate : aaActHistRec.getEffectiveDate()) {
                    for (ActivityRefClass actRefCls : effectiveDate.getActivityRef()) {
                        if (actRefCls.getActivity().getValue().equals("LENDING-NEW-ARRANGEMENT")) {
                            actRef = actRefCls.getActivityRef().getValue();

                            txnData.setFunction("REVERSE");
                            txnData.setNumberOfAuthoriser("0");
                            txnData.setSourceId("FF.OFS.UPD");
                            txnData.setCompanyId(session.getCompanyId());
                            txnData.setVersionId("AA.ARRANGEMENT.ACTIVITY,CANCEL");
                            txnData.setTransactionId(actRef);

                            transactionData.add(txnData);
                            currentRecords.add(loanActRec.toStructure());
                        }
                    }
                }
            } catch (Exception e) {
                e.getMessage();
            }

        }
    }

}
