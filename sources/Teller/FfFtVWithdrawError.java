package com.temenos.fusion;

import com.temenos.api.exceptions.T24CoreException;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.dates.DatesRecord;
import com.temenos.t24.api.records.ebffftpettycashlimit.EbFfFtPettyCashLimitRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/**
 * -----------------------------------------------------------------------------
 * Modification History :
 * -----------------------------------------------------------------------------
 * Description : Once the withdrawal transaction has been processed for this
 * month, this Routine used to sets an error when trying to input the record.
 * 
 * Developed By : Preethi Selvam
 *
 * Development Reference : Teller Report
 *
 * Attached To : VERSION>FUNDS.TRANSFER,LIMIT.WITHDRAW >FF.FT.ID.ERROR
 * 
 * Attached As : Id Routine
 * 
 * -----------------------------------------------------------------------------
 */
public class FfFtVWithdrawError extends RecordLifecycle {

    DataAccess da = new DataAccess(this);
    Session ses = new Session(this);
    String companyID = "";
    String withdrawTxn = "";
    private static final FusionFileLogger ftVWithdrawError = FusionFileLogger.getLogger(FfFtVWithdrawError.class);

    @Override
    public String checkId(String currentRecordId, TransactionContext transactionContext) {
        ftVWithdrawError.info("FfFtVWithdrawError is triggered successfully" + currentRecordId);
    
            companyID = ses.getCompanyId();
            String curFunc = transactionContext.getCurrentFunction();
            DatesRecord dateRec = new DatesRecord(da.getRecord("DATES", companyID));
            String todayDate = dateRec.getToday().getValue();

            String month = todayDate.substring(4, 6);
            String year = todayDate.substring(0, 4);

            String mmyr = month + year;
            String idConcat = companyID + "-" + mmyr;
            ftVWithdrawError.info("idConcat" + idConcat);

            EbFfFtPettyCashLimitRecord limitStruct = getFfFtPettyCashLimitRecord(idConcat, ses);
            ftVWithdrawError.info("limitStruct" + limitStruct);
            if ((curFunc.equals("INPUT") && (limitStruct != null))) {
                withdrawTxn = limitStruct.getWithdrawTransaction().getValue();
                ftVWithdrawError.info("withdrawTxn" + withdrawTxn);
                if (!withdrawTxn.isEmpty()) {
                    throw new T24CoreException("",
                            "The Withdrawal Transaction already Processed for this month and updated in EB.FF.FT.PETTY.CASH.LIMIT");
                }
            }
        return currentRecordId;

    }

    /**
     * @param idConcat
     * @param ses
     * @return
     */
    private EbFfFtPettyCashLimitRecord getFfFtPettyCashLimitRecord(String idConcat, Session ses) {
        ftVWithdrawError.info("getFfFtPettyCashLimitRecord is triggered");
        EbFfFtPettyCashLimitRecord ftPettyCashLimitRec = null;
        String customerMnemonic = ses.getCompanyRecord().getCustomerMnemonic().getValue();
        try {
            ftPettyCashLimitRec = new EbFfFtPettyCashLimitRecord(
                    da.getRecord(customerMnemonic, "EB.FF.FT.PETTY.CASH.LIMIT", "", idConcat));
        } catch (Exception e) {
            ftVWithdrawError.error("getFfFtPettyCashLimitRecord error" + e);
        }
        return ftPettyCashLimitRec;
    }

}
