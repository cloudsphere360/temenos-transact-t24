package com.temenos.fusion;

import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.dates.DatesRecord;
import com.temenos.t24.api.records.ebffftbranchadminexpensesupd.EbFfFtBranchAdminExpensesUpdRecord;
import com.temenos.t24.api.records.ebffftbranchadminexpensesupd.FfBaExpTypeClass;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Date;
import com.temenos.t24.api.system.Session;
import com.temenos.t24.api.tables.ebffftbranchadminexpensesupd.EbFfFtBranchAdminExpensesUpdTable;

public class FfBranchAdminFtIdUpd extends RecordLifecycle {

    private static final FusionFileLogger FfBranchAdminFtIdUpdRtn = FusionFileLogger
            .getLogger(FfBranchAdminFtIdUpd.class);

    DataAccess da = new DataAccess(this);

    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {

        FfBranchAdminFtIdUpdRtn.info("---- START postUpdateRequest ----");

        Session ses = new Session(this);
        String coCode = ses.getCompanyId();
        FfBranchAdminFtIdUpdRtn.info("Company Code: " + coCode);

        Date yDate = new Date(this);
        DatesRecord yDateRec = yDate.getDates();
        String today = yDateRec.getToday().getValue();
        FfBranchAdminFtIdUpdRtn.info("Today's Date: " + today);

        String fundTransId = currentRecordId;
        FfBranchAdminFtIdUpdRtn.info("Funds Transfer ID: " + fundTransId);

        String branchCashId = coCode + "-" + today;
        FfBranchAdminFtIdUpdRtn.info("Branch Cash Record ID: " + branchCashId);

        EbFfFtBranchAdminExpensesUpdRecord barec = new EbFfFtBranchAdminExpensesUpdRecord(
                da.getRecord("EB.FF.FT.BRANCH.ADMIN.EXPENSES.UPD", branchCashId));

        FfBranchAdminFtIdUpdRtn.info("Branch Admin Expense Record fetched");

        EbFfFtBranchAdminExpensesUpdTable batable = new EbFfFtBranchAdminExpensesUpdTable(this);

        try {

            FundsTransferRecord fundsTransferRec = new FundsTransferRecord(currentRecord);
            FfBranchAdminFtIdUpdRtn.info("FundsTransferRecord object created");

            String branchCashExpense = fundsTransferRec.getLocalRefField("OTHER.BILL.REF").getValue();

            FfBranchAdminFtIdUpdRtn.info("Local Reference OTHER.BILL.REF value: " + branchCashExpense);

            updateExpenseReference(barec, branchCashExpense, fundTransId);

        } catch (Exception e) {

            FfBranchAdminFtIdUpdRtn.info("Exception in expense update: " + e);

        }

        try {

            FfBranchAdminFtIdUpdRtn.info("Attempting to write record: " + branchCashId);

            batable.write(branchCashId, barec);

            FfBranchAdminFtIdUpdRtn.info("Record written successfully");

        } catch (Exception e) {

            FfBranchAdminFtIdUpdRtn.info("Exception during write: " + e);

        }

        FfBranchAdminFtIdUpdRtn.info("---- END postUpdateRequest ----");
    }

    /**
     * Updates Expense Reference based on Expense Type
     */
    private void updateExpenseReference(EbFfFtBranchAdminExpensesUpdRecord barec, String branchCashExpense,
            String fundTransId) {

        FfBranchAdminFtIdUpdRtn.info("Entering updateExpenseReference method");

        List<FfBaExpTypeClass> expTypes = barec.getFfBaExpType();
        FfBranchAdminFtIdUpdRtn.info("Total Expense Types in record: " + expTypes.size());

        for (FfBaExpTypeClass exp : expTypes) {

            String expType = exp.getFfBaExpType().getValue();

            FfBranchAdminFtIdUpdRtn.info("Checking Expense Type: " + expType);

            if (isMatchingExpense(expType, branchCashExpense)) {

                FfBranchAdminFtIdUpdRtn.info("MATCH FOUND -> Expense Type: " + expType);
                FfBranchAdminFtIdUpdRtn.info("Updating TxnRef with: " + fundTransId);

                exp.setFfBaExpTxnRef(fundTransId);
            }
        }

        FfBranchAdminFtIdUpdRtn.info("Exiting updateExpenseReference method");
    }

    /**
     * Matches Expense Type with FT Local Reference
     */
    private boolean isMatchingExpense(String expType, String branchCashExpense) {

        FfBranchAdminFtIdUpdRtn.info("Comparing ExpType: " + expType + " with FT LocalRef: " + branchCashExpense);

        return (expType.equals("Electricity Exp") && branchCashExpense.equals("Elec"))
                || (expType.equals("Water Exp") && branchCashExpense.equals("Water"))
                || (expType.equals("Telephone & Broadband Exp") && branchCashExpense.equals("TeleBB"))
                || (expType.equals("Mess Exp") && branchCashExpense.equals("Mess"))
                || (expType.equals("Postage & Courier Exp") && branchCashExpense.equals("PostCour"))
                || (expType.equals("Other Expense") && branchCashExpense.equals("OthersExp"));
    }
}