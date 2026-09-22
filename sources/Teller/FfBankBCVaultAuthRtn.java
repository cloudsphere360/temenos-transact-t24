package com.temenos.fusion;

import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebffbranchadminexpdaily.EbFfBranchAdminExpDailyRecord;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;
import com.temenos.t24.api.tables.ebffbranchadminexpdaily.EbFfBranchAdminExpDailyTable;

/**
 *
 * @author ar116388
 *
 */
public class FfBankBCVaultAuthRtn extends RecordLifecycle {

    private static final FusionFileLogger FfBankBCVaultAuth = FusionFileLogger.getLogger(FfBankBCVaultAuthRtn.class);

    DataAccess dataAccess = new DataAccess(this);
    Session session = new Session(this);
    EbFfBranchAdminExpDailyRecord branchExpenseRecord = null;

    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {

        FfBankBCVaultAuth.info("===== START : FfBankBCVaultAuthRtn.updateRecord =====");

        try {

            FfBankBCVaultAuth.info("Application       : " + application);
            FfBankBCVaultAuth.info("Current Record Id : " + currentRecordId);

            // FT Record
            FundsTransferRecord ftRec = new FundsTransferRecord(currentRecord);
            FfBankBCVaultAuth.info("FundsTransferRecord created successfully");

            // Check transaction type
            String txnType = ftRec.getTransactionType().getValue();
            FfBankBCVaultAuth.info("Transaction Type : " + txnType);

            if ("ACVB".equalsIgnoreCase(txnType)) {

                FfBankBCVaultAuth.info("Transaction type matched with ACVB");

                // Get today's date and company id
                String today = session.getCurrentVariable("!TODAY");
                String companyId = session.getCompanyId();

                FfBankBCVaultAuth.info("Today's Date : " + today);
                FfBankBCVaultAuth.info("Company Id   : " + companyId);

                // Build transaction id
                String id = companyId + "-" + today;
                FfBankBCVaultAuth.info("Generated Transaction Id : " + id);

                // Fetch credit amount
                String creditAmount = ftRec.getCreditAmount().getValue();
                FfBankBCVaultAuth.info("Credit Amount : " + creditAmount);

                // Read/Create record
                branchExpenseRecord = getBranchExpenseRecord(id);

                branchExpenseRecord.setBankBcDepositVault(creditAmount);

                FfBankBCVaultAuth.info("Writing in the table");

                EbFfBranchAdminExpDailyTable branchadmexptab = new EbFfBranchAdminExpDailyTable(this);
                branchadmexptab.write(id, branchExpenseRecord);

                FfBankBCVaultAuth.info("branchadmexptab is :" + branchadmexptab.toString());

                FfBankBCVaultAuth.info("BankBcDepositVault field updated");

            }

        } catch (Exception e) {
            FfBankBCVaultAuth.info("Exception is: " + e);
        }
    }

    private EbFfBranchAdminExpDailyRecord getBranchExpenseRecord(String id) {

        try {

            FfBankBCVaultAuth.info("Entering try block to read Branch Admin Daily table");

            EbFfBranchAdminExpDailyRecord branchRecord = new EbFfBranchAdminExpDailyRecord(
                    dataAccess.getRecord("EB.FF.BRANCH.ADMIN.EXP.DAILY", id));

            FfBankBCVaultAuth.info("branchRecord is :" + branchRecord.toString());

            return branchRecord;

        } catch (Exception e) {

            FfBankBCVaultAuth.info("Entering catch block to read Branch Admin Daily table");

            EbFfBranchAdminExpDailyRecord branchRecord = new EbFfBranchAdminExpDailyRecord();

            FfBankBCVaultAuth.info("branchRecord is :" + branchRecord.toString());

            return branchRecord;
        }
    }

}
