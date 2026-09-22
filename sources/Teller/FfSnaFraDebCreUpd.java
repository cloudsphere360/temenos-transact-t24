package com.temenos.fusion;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.system.Session;

/**
 * @author jh116454
 * @Attached To: VERSION > FUNDS.TRANSFER,FF.SNATCHING.FRAUD.COLL
 * @Attached As: DEFAULT ROUTINE > EB.API > FF.SNA.FRA.DEB.CRE.UPD
 * @Description: To default the Account No for Credit and Debit 
 * 
 */

public class FfSnaFraDebCreUpd extends RecordLifecycle {
    private static final FusionFileLogger ySnaFraDebCreUpdLog = FusionFileLogger.getLogger(FfSnaFraDebCreUpd.class);

    @Override
    public void defaultFieldValues(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {

        FundsTransferRecord yFunTransRec = new FundsTransferRecord(currentRecord);
        String yDebAcct = "";

        Session ySession = new Session(this);

        String yCompCode = ySession.getCompanyId();
        String yCompanyCode1 = yCompCode.substring(5, 9);

        String yDebCode = "INR176200002";
 
        try {
            yDebAcct = yDebCode + yCompanyCode1;
            ySnaFraDebCreUpdLog.info("Debit Acct -> " + yDebAcct);

            yFunTransRec.setDebitAcctNo(yDebAcct);
     
        } catch (Exception e) {
            ySnaFraDebCreUpdLog.info("Rec not Found");
        }

        currentRecord.set(yFunTransRec.toStructure());
    }

}
