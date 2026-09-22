package com.temenos.fusion;

import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebffcashreceivedbranchtxndet.EbFfCashReceivedBranchTxnDetRecord;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.system.Session;
import com.temenos.t24.api.tables.ebffcashreceivedbranchtxndet.EbFfCashReceivedBranchTxnDetTable;

/**
 *
 * @author hs115664
 *
 */
public class FfTellerFtDetailsUpdateAuth extends RecordLifecycle {

    Session ss = new Session(this);
    private static final FusionFileLogger tellerFtDetailsUpdateAuth = FusionFileLogger
            .getLogger(FfTellerFtDetailsUpdateAuth.class);

    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {

        tellerFtDetailsUpdateAuth.info("FfTellerFtDetailsUpdateAuth is triggered " + currentRecordId);
        try {
            FundsTransferRecord ftRec = new FundsTransferRecord(currentRecord);

            updateTheEbFfCashReceivedBranchTxnDet(ftRec, currentRecordId);

            String txntype = ftRec.getTransactionType().getValue();
            String crAcct = ftRec.getCreditAcctNo().getValue();
            String dtAcc = ftRec.getDebitAcctNo().getValue();
            String crValueDate = ftRec.getCreditValueDate().getValue();
            String processDate = ftRec.getProcessingDate().getValue();
            tellerFtDetailsUpdateAuth.info("txnDets " + txntype + "**" + crAcct + "**" + crAcct + "**" + dtAcc + "**"
                    + crValueDate + "**" + processDate);
        } catch (Exception e) {
            tellerFtDetailsUpdateAuth.info("FfTellerFtDetailsUpdateAuth is triggered " + e.getMessage());
        }

    }

    /**
     * @param ftRec
     * @param currentRecordId
     * @return
     */
    private void updateTheEbFfCashReceivedBranchTxnDet(FundsTransferRecord ftRec, String currentRecordId) {
        tellerFtDetailsUpdateAuth.info("updateTheEbFfCashReceivedBranchTxnDet is triggered " + ftRec.toString());
        try {
            EbFfCashReceivedBranchTxnDetRecord txnDetRec = new EbFfCashReceivedBranchTxnDetRecord(this);
            EbFfCashReceivedBranchTxnDetTable txnDetTable = new EbFfCashReceivedBranchTxnDetTable(this);

            String txnDetId = ss.getCompanyId() + "-" + ftRec.getProcessingDate().getValue();
            tellerFtDetailsUpdateAuth.info("txnDetId:" + txnDetId);
            txnDetRec.setTransactionType(ftRec.getTransactionType().getValue());
            txnDetRec.setCreditAcctNum(ftRec.getCreditAcctNo().getValue());
            txnDetRec.setDebitAcctNum(ftRec.getDebitAcctNo().getValue());
            txnDetRec.setCreditAmount(ftRec.getCreditAmount().getValue());
            txnDetRec.setCreditValueDate(ftRec.getProcessingDate().getValue());
            txnDetRec.setFtTxnId(currentRecordId);
            txnDetRec.setBranchId(ss.getCompanyId());
            tellerFtDetailsUpdateAuth.info("txnDetRec:" + txnDetRec.toString());
            txnDetTable.write(txnDetId, txnDetRec);
        } catch (Exception e) {
            tellerFtDetailsUpdateAuth.info("updateTheEbFfCashReceivedBranchTxnDet error:" + e.getMessage());
        }

    }

}
