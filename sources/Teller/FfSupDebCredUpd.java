package com.temenos.fusion;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.system.Session;

/**
 * @author jh116454
 * @Attached To: VERSION > FUNDS.TRANSFER,FF.FT.REV.SUSP.AMT
 * @Attached As: Default Routine > EB.API > FF.SUP.DEB.CRED.UPD
 * @Description: defaulting the Debit and Credit Account
 * 
 */

public class FfSupDebCredUpd extends RecordLifecycle {
    private static final FusionFileLogger ySupDebCredUpdLog = FusionFileLogger.getLogger(FfSupDebCredUpd.class);

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
            yDebAcct = yDebCode + yCompanyCode;
            ySupDebCredUpdLog.info("Debit Acct -> " + yDebAcct);
            yCreAcct = yCreCode + yCompanyCode;
            ySupDebCredUpdLog.info("Creit Acct -> " + yCreAcct);

            yFunTransRec.setDebitAcctNo(yDebAcct);
            yFunTransRec.setCreditAcctNo(yCreAcct);
        } catch (Exception e) {
            ySupDebCredUpdLog.info("Rec not Found");
        }

        currentRecord.set(yFunTransRec.toStructure());
    }

}
