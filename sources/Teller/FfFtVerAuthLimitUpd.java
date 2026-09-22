package com.temenos.fusion;

import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebffftpettycashlimit.EbFfFtPettyCashLimitRecord;
import com.temenos.t24.api.records.ebpettycashlimit.EbPettyCashLimitRecord;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;
import com.temenos.t24.api.tables.ebffftpettycashlimit.EbFfFtPettyCashLimitTable;

/**
 * -----------------------------------------------------------------------------
 * Modification History :
 * -----------------------------------------------------------------------------
 * Description : This Routine used to update FUNDS.TRANSFER application @ID
 * updated in the EB.FF.FT.PETTY.CASH.LIMIT Withdraw Transaction field and Petty
 * Cash Limit during authorisation.
 *
 * Developed By : Preethi Selvam
 *
 * Development Reference : Teller Report
 *
 * Attached To : VERSION>FUNDS.TRANSFER,LIMIT.WITHDRAW >FF.FT.UPD.LIMIT
 * 
 * Attached As : Auth Routine
 * 
 * -----------------------------------------------------------------------------
 */

public class FfFtVerAuthLimitUpd extends RecordLifecycle {

    DataAccess da = new DataAccess(this);
    private final Session ses = new Session();
    FundsTransferRecord ftRecord = null;
    String debitAmt = "";

    String cashLimit = "";

    public static final String FTTABLE = "FUNDS.TRANSFER";
    private static final FusionFileLogger ftVerAuthLimitUpd = FusionFileLogger.getLogger(FfFtVerAuthLimitUpd.class);

    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {

        try {
            ftVerAuthLimitUpd.info("FfFtVerAuthLimitUpd: Routine triggered for ETD Field update.");

            String coCode = safeString(ses.getCompanyId());

            String todayDate = ses.getCurrentVariable("!TODAY");

            String month = todayDate.substring(4, 6);
            String year = todayDate.substring(0, 4);

            String mmyr = month + year;
            String idConcat = coCode + "-" + mmyr;
            ftVerAuthLimitUpd.info("setid:{}" + idConcat);
            EbFfFtPettyCashLimitRecord limit = new EbFfFtPettyCashLimitRecord();

            getEbPettyCashLimit(coCode);

            if (!cashLimit.isEmpty()) {
                limit.setPettyCashLimit(cashLimit);
            }
            limit.setWithdrawTransaction(currentRecordId);

            getIntoTheWriteMethod(idConcat, limit);

        } catch (Exception e) {
            ftVerAuthLimitUpd.error("FfFtVerAuthLimitUpd:- Error fetching record for Image Application {}: {}", e);
        }

    }

    /**
     * @param idConcat
     * @param limit
     */
    private void getIntoTheWriteMethod(String idConcat, EbFfFtPettyCashLimitRecord limit) {

        EbFfFtPettyCashLimitTable limitTable = new EbFfFtPettyCashLimitTable(this);
        try {
            limitTable.write(idConcat, limit);
        } catch (Exception e) {
            e.getMessage();
        }

    }

    /**
     * @param coCode
     */
    private void getEbPettyCashLimit(String coCode) {

        EbPettyCashLimitRecord pettyRec = null;
        try {
            pettyRec = new EbPettyCashLimitRecord(da.getRecord("EB.PETTY.CASH.LIMIT", coCode));
            cashLimit = pettyRec.getPettyCash().getValue();
        } catch (Exception e) {
            e.getMessage();
        }
    }

    private String safeString(Object value) {
        return value == null ? "" : value.toString().trim();
    }

}