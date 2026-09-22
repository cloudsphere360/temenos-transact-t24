package com.temenos.fusion;

import com.temenos.api.exceptions.T24CoreException;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.dates.DatesRecord;
import com.temenos.t24.api.records.ebffbranchadminexpdaily.EbFfBranchAdminExpDailyRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Date;
import com.temenos.t24.api.system.Session;

/**
 *
 * @author ar116388
 *
 */
public class FfBankDepVaultIDRtn extends RecordLifecycle {

    private static final FusionFileLogger FfBankDepVaultID = FusionFileLogger.getLogger(FfBankDepVaultIDRtn.class);

    Session session = new Session(this);
    DataAccess da = new DataAccess(this);

    String today = "";
    String companyId = "";
    String liveRecordID = "";
    String yCurrFunc = "";
    String companycode = "";
    String todayDate = "";
    String id = "";
    String bankBcDepVault = "";

    @Override
    public String checkId(String currentRecordId, TransactionContext transactionContext) {

        FfBankDepVaultID.info("Current Record ID is: " + currentRecordId);

        companycode = session.getCompanyId();

        Date dates = new Date(this);
        DatesRecord dateRecord = dates.getDates();
        todayDate = dateRecord.getToday().getValue();
        id = companycode + "-" + todayDate;
        yCurrFunc = transactionContext.getCurrentFunction();

        FfBankDepVaultID.info("Current FUNCTION is: " + yCurrFunc);
        FfBankDepVaultID.info("Current Record ID - id is: " + id);

        EbFfBranchAdminExpDailyRecord branchAdminExpRec = null;

        if (yCurrFunc.equals("INPUT")) {
            FfBankDepVaultID.info("Entering if");
            try {
                FfBankDepVaultID.info("Entering try");
                branchAdminExpRec = new EbFfBranchAdminExpDailyRecord(da.getRecord("EB.FF.BRANCH.ADMIN.EXP.DAILY", id));
                FfBankDepVaultID.info("Record present");

                bankBcDepVault = branchAdminExpRec.getBankBcDepositVault().getValue();
                FfBankDepVaultID.info("bankBcDepVault is: " + bankBcDepVault);

            } catch (Exception e) {
                FfBankDepVaultID.info("Record NOT present");
            }

            if (bankBcDepVault != null && !bankBcDepVault.isEmpty()) {
                FfBankDepVaultID.info("Throw error");
                throw new T24CoreException("", "Record ID already Exists");
            } else {
                FfBankDepVaultID.info("New Rec Open");
            }
        }

        return currentRecordId;

    }

}
