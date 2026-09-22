package com.temenos.fusion;

import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebffftbranchadminexpensesupd.EbFfFtBranchAdminExpensesUpdRecord;
import com.temenos.t24.api.records.ebffftbranchadminexpensesupd.FfBaExpTypeClass;
import com.temenos.t24.api.system.Session;

public class FfFtBranchAdmExpUpdDefRtn extends RecordLifecycle {

    private static final FusionFileLogger FfFtBranchAdmExpUpdDef = FusionFileLogger
            .getLogger(FfFtBranchAdmExpUpdDefRtn.class);

    String yCreditAcct = "";
    String coCode = "";

    Session session = new Session(this);

    @Override
    public void defaultFieldValues(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {

        FfFtBranchAdmExpUpdDef.info("Branch Admin Expense Routine Started");

        double totalExpense = 0.0;

        double elecExpAmtD = 0.0;
        double waterExpAmtD = 0.0;
        double teleBBExpAmtD = 0.0;
        double messExpAmtD = 0.0;
        double postCourExpAmtD = 0.0;
        double otherExpAmtD = 0.0;

        coCode = session.getCompanyId();

        EbFfFtBranchAdminExpensesUpdRecord exprecord = new EbFfFtBranchAdminExpensesUpdRecord(currentRecord);

        FfFtBranchAdmExpUpdDef.info("Record Loaded: " + exprecord.toString());

        try {

            List<FfBaExpTypeClass> expGroupList = exprecord.getFfBaExpType();

            FfFtBranchAdmExpUpdDef.info("Total Expense Rows Found: " + expGroupList.size());

            for (int i = 0; i < expGroupList.size(); i++) {

                FfFtBranchAdmExpUpdDef.info("Processing Row Index: " + i);

                String expAmtStr = expGroupList.get(i).getFfBaExpAmt().getValue();
                String expType = expGroupList.get(i).getFfBaExpType().getValue();

                FfFtBranchAdmExpUpdDef.info("Expense Type: " + expType);
                FfFtBranchAdmExpUpdDef.info("Expense Amount String: " + expAmtStr);

                double expAmtStrD = parseAmount(expAmtStr);

                FfFtBranchAdmExpUpdDef.info("Parsed Expense Amount: " + expAmtStrD);

                switch (expType) {

                case "Electricity Exp":

                    FfFtBranchAdmExpUpdDef.info("Assigning Electricity Expense Account");

                    expGroupList.get(i).setFfBaExpAcc("INR1761000021007");
                    elecExpAmtD += expAmtStrD;

                    FfFtBranchAdmExpUpdDef.info("Electricity Total So Far: " + elecExpAmtD);
                    break;

                case "Water Exp":

                    FfFtBranchAdmExpUpdDef.info("Assigning Water Expense Account");

                    expGroupList.get(i).setFfBaExpAcc("INR1761000031007");
                    waterExpAmtD += expAmtStrD;

                    FfFtBranchAdmExpUpdDef.info("Water Total So Far: " + waterExpAmtD);
                    break;

                case "Telephone & Broadband Exp":

                    FfFtBranchAdmExpUpdDef.info("Assigning Telephone/Broadband Expense Account");

                    expGroupList.get(i).setFfBaExpAcc("INR1761000041007");
                    teleBBExpAmtD += expAmtStrD;

                    FfFtBranchAdmExpUpdDef.info("Telephone/Broadband Total So Far: " + teleBBExpAmtD);
                    break;

                case "Postage & Courier Exp":

                    FfFtBranchAdmExpUpdDef.info("Assigning Postage/Courier Expense Account");

                    expGroupList.get(i).setFfBaExpAcc("INR1761000061007");
                    postCourExpAmtD += expAmtStrD;

                    FfFtBranchAdmExpUpdDef.info("Postage/Courier Total So Far: " + postCourExpAmtD);
                    break;

                case "Mess Exp":

                    FfFtBranchAdmExpUpdDef.info("Assigning Mess Expense Account");

                    expGroupList.get(i).setFfBaExpAcc("INR1761000051007");
                    messExpAmtD += expAmtStrD;

                    FfFtBranchAdmExpUpdDef.info("Mess Expense Total So Far: " + messExpAmtD);
                    break;

                case "Other Expense":

                    FfFtBranchAdmExpUpdDef.info("Assigning Other Expense Account");

                    expGroupList.get(i).setFfBaExpAcc("INR1761000061007");
                    otherExpAmtD += expAmtStrD;

                    FfFtBranchAdmExpUpdDef.info("Other Expense Total So Far: " + otherExpAmtD);
                    break;

                default:

                    FfFtBranchAdmExpUpdDef.info("Unknown Expense Type - Clearing Account");

                    expGroupList.get(i).setFfBaExpAcc("");
                    break;
                }

                exprecord.setFfBaExpType(expGroupList.get(i), i);

                FfFtBranchAdmExpUpdDef.info("Row " + i + " Updated Successfully");
            }

            totalExpense = elecExpAmtD + waterExpAmtD + teleBBExpAmtD + messExpAmtD + postCourExpAmtD + otherExpAmtD;

            FfFtBranchAdmExpUpdDef.info("Final Total Expense Calculated: " + totalExpense);

        } catch (Exception e) {

            FfFtBranchAdmExpUpdDef.info("Exception Occurred in Expense Routine: " + e);

        }

        FfFtBranchAdmExpUpdDef.info("Setting Credit Amount: " + totalExpense);

        getCredAcct();

        exprecord.setCreditAmt(String.valueOf(totalExpense));
        exprecord.setCreditAcNo(yCreditAcct);
        currentRecord.set(exprecord.toStructure());

        FfFtBranchAdmExpUpdDef.info("Branch Admin Expense Routine Completed");
    }

    /**
     * Parses the amount safely
     */
    private double parseAmount(String expAmtStr) {

        try {

            if (expAmtStr != null && !expAmtStr.isEmpty()) {

                FfFtBranchAdmExpUpdDef.info("Parsing Amount: " + expAmtStr);

                return Double.parseDouble(expAmtStr);
            }

        } catch (Exception e) {

            FfFtBranchAdmExpUpdDef.info("Invalid Amount Format Encountered: " + expAmtStr);
        }

        return 0.0;
    }

    private void getCredAcct() {
        String yCredAcct = "INR100010001";
        String ySubCode = coCode.substring(5, 9);
        yCreditAcct = yCredAcct + ySubCode;
        FfFtBranchAdmExpUpdDef.info(" CreditAcct -> " + yCreditAcct);
    }
}