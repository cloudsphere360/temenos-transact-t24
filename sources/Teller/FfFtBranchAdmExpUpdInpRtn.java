package com.temenos.fusion;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import java.math.BigDecimal;
import com.temenos.api.TStructure;
import com.temenos.api.TValidationResponse;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebffftbranchadminexpensesupd.EbFfFtBranchAdminExpensesUpdRecord;
import com.temenos.t24.api.records.ebffftbranchadminexpensesupd.FfBaExpTypeClass;

public class FfFtBranchAdmExpUpdInpRtn extends RecordLifecycle {

    private static final FusionFileLogger FfFtBranchAdmExpUpdInp = FusionFileLogger
            .getLogger(FfFtBranchAdmExpUpdInpRtn.class);

    @Override
    public TValidationResponse validateRecord(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {

        FfFtBranchAdmExpUpdInp.info("Validation Routine Started for Record: " + currentRecordId);

        EbFfFtBranchAdminExpensesUpdRecord exprec = new EbFfFtBranchAdminExpensesUpdRecord(currentRecord);

        try {

            List<FfBaExpTypeClass> expGroupLis = exprec.getFfBaExpType();

            FfFtBranchAdmExpUpdInp.info("Total Expense Rows Found: " + expGroupLis.size());

            Set<String> expTypeSet = new HashSet<>();

            for (int i = 0; i < expGroupLis.size(); i++) {

                FfFtBranchAdmExpUpdInp.info("Processing Row Index: " + i);

                FfBaExpTypeClass expRow = expGroupLis.get(i);

                String expTyp = expRow.getFfBaExpType().getValue();
                String expAmt = expRow.getFfBaExpAmt().getValue();
                String expBill = expRow.getFfBaExpBillRef().getValue();

                FfFtBranchAdmExpUpdInp.info("Expense Type: " + expTyp);
                FfFtBranchAdmExpUpdInp.info("Expense Amount: " + expAmt);
                FfFtBranchAdmExpUpdInp.info("Expense Bill Ref: " + expBill);

                validateExpenseType(expRow, expTyp);
                checkDuplicateExpenseType(expRow, expTyp, expTypeSet);
                validateAmount(expRow, expTyp, expAmt);
                validateNegativeAmount(expRow, expAmt);
                validateBillReference(expRow, expAmt, expBill);

                FfFtBranchAdmExpUpdInp.info("Completed Validation for Row Index: " + i);
            }

        } catch (Exception e) {

            FfFtBranchAdmExpUpdInp.info("Exception occurred during validation: " + e);
        }

        FfFtBranchAdmExpUpdInp.info("Validation Routine Completed");

        return exprec.getValidationResponse();
    }

    private void validateExpenseType(FfBaExpTypeClass expRow, String expTyp) {

        FfFtBranchAdmExpUpdInp.info("Entering validateExpenseType");

        if (expTyp == null || expTyp.isEmpty()) {

            FfFtBranchAdmExpUpdInp.info("Expense Type is Empty - Setting Error");

            expRow.getFfBaExpType().setError("Enter Expense Type");
        }
    }

    private void checkDuplicateExpenseType(FfBaExpTypeClass expRow, String expTyp, Set<String> expTypeSet) {

        FfFtBranchAdmExpUpdInp.info("Entering checkDuplicateExpenseType");

        if (expTyp != null && !expTyp.isEmpty()) {

            if (expTypeSet.contains(expTyp)) {

                FfFtBranchAdmExpUpdInp.info("Duplicate Expense Type Found: " + expTyp);

                expRow.getFfBaExpType().setError("Duplicate Expense Type");

            } else {

                FfFtBranchAdmExpUpdInp.info("Adding Expense Type to Set: " + expTyp);

                expTypeSet.add(expTyp);
            }
        }
    }

    private void validateAmount(FfBaExpTypeClass expRow, String expTyp, String expAmt) {

        FfFtBranchAdmExpUpdInp.info("Entering validateAmount");

        if (expTyp != null && !expTyp.isEmpty() && (expAmt == null || expAmt.isEmpty())) {

            FfFtBranchAdmExpUpdInp.info("Amount Missing for Expense Type: " + expTyp);

            expRow.getFfBaExpAmt().setError("Enter Amount");
        }
    }

    // NEW METHOD FOR NEGATIVE AMOUNT VALIDATION
    private void validateNegativeAmount(FfBaExpTypeClass expRow, String expAmt) {

        FfFtBranchAdmExpUpdInp.info("Entering validateNegativeAmount");

        try {

            if (expAmt != null && !expAmt.isEmpty()) {

                BigDecimal amount = new BigDecimal(expAmt);

                if (amount.compareTo(BigDecimal.ZERO) <= 0) {

                    if (amount.compareTo(BigDecimal.ZERO) == 0) {

                        FfFtBranchAdmExpUpdInp.info("Zero Amount Entered: " + expAmt);

                        expRow.getFfBaExpAmt().setError("Amount cannot be ZERO");

                    } else {

                        FfFtBranchAdmExpUpdInp.info("Negative Amount Entered: " + expAmt);

                        expRow.getFfBaExpAmt().setError("Negative Amount Not Allowed");
                    }
                }

                /*
                 * if (amount.compareTo(BigDecimal.ZERO) < 0) {
                 * 
                 * FfFtBranchAdmExpUpdInp.info("Negative Amount Entered: " + expAmt);
                 * 
                 * expRow.getFfBaExpAmt().setError("Negative Amount Not Allowed"); }
                 * 
                 * if (amount == BigDecimal.ZERO) {
                 * 
                 * FfFtBranchAdmExpUpdInp.info("Negative Amount Entered: " + expAmt);
                 * 
                 * expRow.getFfBaExpAmt().setError("Amount cannot be ZERO"); }
                 */
            }

        } catch (NumberFormatException e) {

            FfFtBranchAdmExpUpdInp.info("Invalid Amount Format: " + expAmt);
        }
    }

    private void validateBillReference(FfBaExpTypeClass expRow, String expAmt, String expBill) {

        FfFtBranchAdmExpUpdInp.info("Entering validateBillReference");

        if (expAmt != null && !expAmt.isEmpty() && (expBill == null || expBill.isEmpty())) {

            FfFtBranchAdmExpUpdInp.info("Bill Reference Missing for Amount: " + expAmt);

            expRow.getFfBaExpBillRef().setError("Input Bill No");
        }
    }
}