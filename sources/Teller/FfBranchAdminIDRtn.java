package com.temenos.fusion;

import com.temenos.api.exceptions.T24CoreException;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.dates.DatesRecord;
import com.temenos.t24.api.records.ebffftbranchadminexpensesupd.EbFfFtBranchAdminExpensesUpdRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Date;
import com.temenos.t24.api.system.Session;

/**
 *
 * @author ar116388
 *
 */
public class FfBranchAdminIDRtn extends RecordLifecycle {

    private static final FusionFileLogger BranchAdminRtn = FusionFileLogger.getLogger(FfBranchAdminIDRtn.class);

    Session session = new Session(this);
    DataAccess da = new DataAccess(this);

    String today = "";
    String companyId = "";
    String liveRecordID = "";
    String yCurrFunc = "";
    String companycode = "";
    String todayDate = "";
    String id = "";

    @Override
    public String checkId(String currentRecordId, TransactionContext transactionContext) {

        BranchAdminRtn.info("Current Record ID is: " + currentRecordId);
        BranchAdminRtn.info("Live Record ID is: " + transactionContext.getLiveRecordId());

        companycode = session.getCompanyId();

        Date dates = new Date(this);
        DatesRecord dateRecord = dates.getDates();
        todayDate = dateRecord.getToday().getValue();
        id = companycode + "-" + todayDate;
        yCurrFunc = transactionContext.getCurrentFunction();

        BranchAdminRtn.info("Current FUNCTION is: " + yCurrFunc);
        BranchAdminRtn.info("Current Record ID - id is: " + id);

        EbFfFtBranchAdminExpensesUpdRecord branchAdminExpRec = null;

        if (yCurrFunc.equals("INPUT")) {
            BranchAdminRtn.info("Entering if");
            try {
                BranchAdminRtn.info("Entering try");
                branchAdminExpRec = new EbFfFtBranchAdminExpensesUpdRecord(
                        da.getRecord("EB.FF.FT.BRANCH.ADMIN.EXPENSES.UPD", id));
                BranchAdminRtn.info("Record present");
            } catch (Exception e) {
                BranchAdminRtn.info("Record NOT present");
            }

            if (branchAdminExpRec != null && !branchAdminExpRec.toString().trim().isEmpty()) {
                BranchAdminRtn.info("Throw error");
                throw new T24CoreException("", "Record ID already Exists");
            } else {
                currentRecordId = id;
                BranchAdminRtn.info("New Rec Open");
            }
        }

        return currentRecordId;

    }

}
