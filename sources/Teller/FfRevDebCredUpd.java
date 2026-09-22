package com.temenos.fusion;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.system.Session;

/**
 * @author jh116454
 * @Attached To: VERSION > FUNDS.TRANSFER,FF.FT.SUSP.AMT
 * @Attached As: DEFAULT ROUTINE > EB.API > FF.REV.DEB.CRED.UPD
 * @Description: To default the Account No for Credit and Debit 
 * 
 */

public class FfRevDebCredUpd extends RecordLifecycle {
    private static final FusionFileLogger yRevDebCredUpdLog = FusionFileLogger.getLogger(FfRevDebCredUpd.class);

    @Override
    public void defaultFieldValues(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {

        FundsTransferRecord yFunTransRec = new FundsTransferRecord(currentRecord);
        String yDebAcct = "";
        String yCreAcct = "";

        Session ySession = new Session(this);

        String yCompCode = ySession.getCompanyId();
        String yCompanyCode = yCompCode.substring(2, 9);

        String yDebCode = "INR100010";
        String yCreCode = "INR103010";

        try {
            yCreAcct = yDebCode + yCompanyCode;
            yRevDebCredUpdLog.info("Debit Acct -> " + yDebAcct);
            yDebAcct = yCreCode + yCompanyCode;
            yRevDebCredUpdLog.info("Creit Acct -> " + yCreAcct);

            yFunTransRec.setDebitAcctNo(yDebAcct);
            yFunTransRec.setCreditAcctNo(yCreAcct);
        } catch (Exception e) {
            yRevDebCredUpdLog.info("Rec not Found");
        }

        currentRecord.set(yFunTransRec.toStructure());
    }

}
