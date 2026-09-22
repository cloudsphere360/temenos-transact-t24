package com.temenos.fusion;

import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebffftbranchadminexpensesupd.EbFfFtBranchAdminExpensesUpdRecord;
import com.temenos.t24.api.records.ebffftbranchadminexpensesupd.FfBaExpTypeClass;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.system.Session;

public class FfFtBranchAdminExpUpdAuthRtn extends RecordLifecycle {

    private static final FusionFileLogger FfFtBranchAdminExpUpdAuth = FusionFileLogger
            .getLogger(FfFtBranchAdminExpUpdAuthRtn.class);

    /* SONAR CONSTANTS */
    private static final String OTHER_BILL_REF = "OTHER.BILL.REF";
    private static final String FT_VERSION = "FUNDS.TRANSFER,BRANCH.ADMIN";
    private static final String FUNCTION_INPUT = "INPUT";
    private static final String SOURCE_ID = "FF.BRANCH.ADM";

    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {

        FfFtBranchAdminExpUpdAuth.info("===== Branch Admin Expenses Auth Routine STARTED =====");

        Session sess = new Session(this);
        String coCode = sess.getCompanyId();
        String yMne = sess.getCompanyRecord().getFinancialMne().toString();

        FfFtBranchAdminExpUpdAuth.info("Company ID : " + coCode);
        FfFtBranchAdminExpUpdAuth.info("Financial MNE : " + yMne);

        try {

            EbFfFtBranchAdminExpensesUpdRecord expRecord = new EbFfFtBranchAdminExpensesUpdRecord(currentRecord);

            String creditAccNo = expRecord.getCreditAcNo().getValue();

            FfFtBranchAdminExpUpdAuth.info("Credit Account Number : " + creditAccNo);

            List<FfBaExpTypeClass> expTypeList = expRecord.getFfBaExpType();

            FfFtBranchAdminExpUpdAuth.info("Total Expense Records Found : " + expTypeList.size());

            for (FfBaExpTypeClass expRow : expTypeList) {

                String expType = expRow.getFfBaExpType().getValue();

                FfFtBranchAdminExpUpdAuth.info("--------------------------------------------------");
                FfFtBranchAdminExpUpdAuth.info("Processing Expense Type : " + expType);

                switch (expType) {

                case "Electricity Exp":

                    FfFtBranchAdminExpUpdAuth.info("Matched Expense Type : Electricity Exp");
                    createFundsTransfer(expRow, creditAccNo, yMne, coCode, "Elec", transactionData, currentRecords);
                    break;

                case "Water Exp":

                    FfFtBranchAdminExpUpdAuth.info("Matched Expense Type : Water Exp");
                    createFundsTransfer(expRow, creditAccNo, yMne, coCode, "Water", transactionData, currentRecords);
                    break;

                case "Telephone & Broadband Exp":

                    FfFtBranchAdminExpUpdAuth.info("Matched Expense Type : Telephone & Broadband Exp");
                    createFundsTransfer(expRow, creditAccNo, yMne, coCode, "TeleBB", transactionData, currentRecords);
                    break;

                case "Mess Exp":

                    FfFtBranchAdminExpUpdAuth.info("Matched Expense Type : Mess Exp");
                    createFundsTransfer(expRow, creditAccNo, yMne, coCode, "Mess", transactionData, currentRecords);
                    break;

                case "Postage & Courier Exp":

                    FfFtBranchAdminExpUpdAuth.info("Matched Expense Type : Postage & Courier Exp");
                    createFundsTransfer(expRow, creditAccNo, yMne, coCode, "PostCour", transactionData, currentRecords);
                    break;

                case "Other Expense":

                    FfFtBranchAdminExpUpdAuth.info("Matched Expense Type : Other Expense");
                    createFundsTransfer(expRow, creditAccNo, yMne, coCode, "OthersExp", transactionData,
                            currentRecords);
                    break;

                default:

                    FfFtBranchAdminExpUpdAuth.info("Unknown Expense Type Encountered : " + expType);
                    break;
                }
            }

        } catch (Exception e) {

            FfFtBranchAdminExpUpdAuth.info("Exception occurred while processing routine");
            FfFtBranchAdminExpUpdAuth.info("Exception Details : " + e.getMessage());
        }

        FfFtBranchAdminExpUpdAuth.info("===== Branch Admin Expenses Auth Routine ENDED =====");
    }

    /* COMMON FT CREATION METHOD */

    private void createFundsTransfer(FfBaExpTypeClass expRow, String creditAcc, String yMne, String coCode,
            String billRef, List<TransactionData> transactionData, List<TStructure> currentRecords) {

        String amount = expRow.getFfBaExpAmt().getValue();
        String debitAcc = expRow.getFfBaExpAcc().getValue();

        FfFtBranchAdminExpUpdAuth.info("Preparing Funds Transfer...");
        FfFtBranchAdminExpUpdAuth.info("Debit Account : " + debitAcc);
        FfFtBranchAdminExpUpdAuth.info("Credit Account : " + creditAcc);
        FfFtBranchAdminExpUpdAuth.info("Amount : " + amount);
        FfFtBranchAdminExpUpdAuth.info("Bill Reference : " + billRef);

        FundsTransferRecord ftRecord = new FundsTransferRecord(this);
        TransactionData transData = new TransactionData();

        ftRecord.setCreditAcctNo(creditAcc);
        ftRecord.setCreditAmount(amount);
        ftRecord.setCreditCurrency("INR");

        ftRecord.setDebitAcctNo(debitAcc);
        ftRecord.setDebitCurrency("INR");

        ftRecord.setTransactionType("ACDC");
        ftRecord.setOrderingBank(yMne, 0);

        ftRecord.getProfitCentreDept().setValue("1");
        ftRecord.getLocalRefField(OTHER_BILL_REF).set(billRef);

        FfFtBranchAdminExpUpdAuth.info("FT Record fields populated successfully");

        transData.setVersionId(FT_VERSION);
        transData.setFunction(FUNCTION_INPUT);
        transData.setSourceId(SOURCE_ID);
        transData.setNumberOfAuthoriser("0");
        transData.setTransactionId("/");
        transData.setCompanyId(coCode);

        FfFtBranchAdminExpUpdAuth.info("TransactionData populated");

        transactionData.add(transData);
        currentRecords.add(ftRecord.toStructure());

        FfFtBranchAdminExpUpdAuth.info("Funds Transfer record added to transactionData and currentRecords");
        FfFtBranchAdminExpUpdAuth.info("FT Creation Completed Successfully");
    }
}