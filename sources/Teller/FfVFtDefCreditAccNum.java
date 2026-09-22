package com.temenos.fusion;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.company.CompanyRecord;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/**
 * -----------------------------------------------------------------------------
 * Modification History :
 * -----------------------------------------------------------------------------
 * Description : This Routine used to default the value for Credit Account
 * field.
 *
 * Developed By : Preethi Selvam
 *
 * Development Reference : Teller Report
 *
 * Attached To : VERSION>FUNDS.TRANSFER,CASH.BCP.COLLECTION
 * >FF.FT.DEF.CREDIT.AC.NUM VERSION>FUNDS.TRANSFER,CASH.OBS.COLLECTION
 * >FF.FT.DEF.CREDIT.AC.NUM
 * 
 * Attached As : Auto New Content Routine
 * 
 * -----------------------------------------------------------------------------
 */
public class FfVFtDefCreditAccNum extends RecordLifecycle {
    private static final FusionFileLogger ftDefCreditAccNum = FusionFileLogger.getLogger(FfVFtDefCreditAccNum.class);

    private final Session ses = new Session();
    private final DataAccess da = new DataAccess(this);
    private static final String ACNUM = "INR100040001";
    String subDivCode = "";

    @Override
    public void defaultFieldValues(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
        ftDefCreditAccNum.info("FfVFtDefCreditAccNum is triggered Successfully");
        try {
            FundsTransferRecord ftRec = new FundsTransferRecord(currentRecord);
            String compCode = ses.getCompanyId();
            CompanyRecord compRec = null;
                compRec = new CompanyRecord(da.getRecord("COMPANY", compCode));
                subDivCode = compRec.getSubDivisionCode().getValue();
            
            String creditAcNum = ACNUM + subDivCode;
            ftDefCreditAccNum.info("creditAcNum" + creditAcNum);
            ftRec.setCreditAcctNo(creditAcNum);
            currentRecord.set(ftRec.toStructure());
        } catch (Exception e) {
            ftDefCreditAccNum.error("FfVFtDefCreditAccNum Code Error " + e);
        }
    }

}
