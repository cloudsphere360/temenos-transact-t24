package com.temenos.fusion;

import java.util.ArrayList;
import java.util.List;

import java.math.BigDecimal;
import java.math.RoundingMode;
import com.temenos.api.TStructure;
import com.temenos.api.TValidationResponse;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebffeodscreen.EbFfEodScreenRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

public class FfTellerEodInputRtn extends RecordLifecycle {

    private static final FusionFileLogger FfTellerEodInput = FusionFileLogger.getLogger(FfTellerEodInputRtn.class);

    DataAccess da = new DataAccess(this);
    Session sess = new Session(this);

    String closingVaultBalance = "";
    BigDecimal closingVaultBalanceBD = BigDecimal.ZERO;
    String reviewed = "";

    String controlAccountYest = null;
    String controlReversalacc = null;
    String companyID = "";
    String lastWorkingDay = "";
    String yid = "";
    String currNumber = "";

    String closingVaultbal = "";
    String denomTypeTotal = "";

    String vaultOB = "";
    String cashRecBM = "";
    String BankBCDepRev = "";
    String bankBCDepVault = "";
    String pettyCash = "";
    String branchAdmExp = "";
    String incidents = "";
    String controlAcc = "";

    BigDecimal controlAccountYestBD = BigDecimal.ZERO;
    BigDecimal controlReversalaccBD = BigDecimal.ZERO;
    BigDecimal controldiff = BigDecimal.ZERO;
    BigDecimal closingVaultbalBD = BigDecimal.ZERO;
    BigDecimal denomTypeTotalBD = BigDecimal.ZERO;
    BigDecimal pettyCashBD = BigDecimal.ZERO;
    BigDecimal branchAdmExpBD = BigDecimal.ZERO;
    BigDecimal pettyBranchAdm = BigDecimal.ZERO;
    BigDecimal vaultOBBD = BigDecimal.ZERO;
    BigDecimal cashRecBMBD = BigDecimal.ZERO;
    BigDecimal BankBCDepRevBD = BigDecimal.ZERO;
    BigDecimal innerSumOne = BigDecimal.ZERO;
    BigDecimal bankBCDepVaultBD = BigDecimal.ZERO;
    BigDecimal incidentsBD = BigDecimal.ZERO;
    BigDecimal controlAccBD = BigDecimal.ZERO;
    BigDecimal innerSumTwo = BigDecimal.ZERO;
    BigDecimal innerSum = BigDecimal.ZERO;

    EbFfEodScreenRecord ebodrec = null;
    EbFfEodScreenRecord ebodrecyesterday = null;

    String denom = "";

    @Override
    public TValidationResponse validateRecord(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {

        EbFfEodScreenRecord currentEodRecord = new EbFfEodScreenRecord(currentRecord);

        FfTellerEodInput.info("Validation Started");

        try {

            companyID = sess.getCompanyId();
            lastWorkingDay = sess.getCurrentVariable("!LAST.WORKING.DAY");
            yid = companyID + "-" + lastWorkingDay;

            FfTellerEodInput.info("Lastwrkday " + lastWorkingDay);
            
            controlReversalacc = currentEodRecord.getSuspenseReversalAmount().getValue();
            FfTellerEodInput.info("controlReversalacc is : " + controlReversalacc);
            
            controlReversalaccBD = new BigDecimal(controlReversalacc);
            FfTellerEodInput.info("controlReversalaccBD is : " + controlReversalaccBD);

            validateControlAccountLogic(currentEodRecord);
            validateClosingVaultBalance(currentEodRecord);

            // Live record override validation

            checkLiveRecordExists(currentEodRecord);

            // Reviewed validation

            String reviewedValue = currentEodRecord.getFfReviewedBy().getValue();

            if (!"YES".equalsIgnoreCase(reviewedValue)) {
                currentEodRecord.getFfReviewedBy().setError("REVIEWED FIELD MUST BE YES");
            }

            // Total Denominations validation

            denom = currentEodRecord.getDenomOverallTotal().getValue();

            if (denom == null || denom.isEmpty()) {
                FfTellerEodInput.info("Getting inside error block - 160");
                currentEodRecord.getDenomOverallTotal()
                        .setError("Enter Denominations that match the Closing Vault Balance");
            }
        } catch (Exception e) {
            FfTellerEodInput.info("Exception in 166: " + e);
        }

        return currentEodRecord.getValidationResponse();
    }

    // Check if live record already exists -> raise override

    private void checkLiveRecordExists(EbFfEodScreenRecord currentEodRecord) {

        FfTellerEodInput.info("currentEodRecord is: " + currentEodRecord);

        try {
            FfTellerEodInput.info("Entering try in 97");

            currNumber = currentEodRecord.getCurrNo();
            int currNumberInt = Integer.parseInt(currNumber);

            FfTellerEodInput.info("currNumber is: " + currNumber);
            FfTellerEodInput.info("currNumberInt is: " + currNumberInt);

            if (currNumberInt >= 1) {

                FfTellerEodInput.info("Live record already exists for this ID");

                currentEodRecord.getDenomOverallTotal()
                        .setOverride("EOD has already been completed for this branch. Do you wish to re-submit?");
            }
        } catch (Exception e) {
            FfTellerEodInput.info("Exception while checking live record: " + e);
        }
    }

    // Validate Day-2 Control Account Logic

    private void validateControlAccountLogic(EbFfEodScreenRecord currentEodRecord) {

        try {

            FfTellerEodInput.info("Getting inside try block for validation of EOD record");

            ebodrecyesterday = new EbFfEodScreenRecord(da.getRecord("EB.FF.EOD.SCREEN", yid));
            FfTellerEodInput.info("ebodrecyesterday FOUND for id");

            controlAccountYest = ebodrecyesterday.getSuspenseAmount().getValue();
            FfTellerEodInput.info("controlAccountYest is : " + controlAccountYest);

            if (controlAccountYest != null && !controlAccountYest.isEmpty()) {

                controlAccountYestBD = new BigDecimal(controlAccountYest);
                controlAccountYestBD = controlAccountYestBD.abs();

                FfTellerEodInput.info("controlAccountYestD is : " + controlAccountYestBD);

                if (controlAccountYestBD.compareTo(BigDecimal.ZERO) == 0) {
                    return;
                }
            }

            if (controlReversalacc != null && !controlReversalacc.isEmpty()) {

                controlReversalaccBD = controlReversalaccBD.abs();

                FfTellerEodInput.info("controlRevAccD is : " + controlReversalacc);
            }

            controldiff = controlReversalaccBD.subtract(controlAccountYestBD);
            controldiff = controldiff.abs();

            FfTellerEodInput.info("controldiff is : " + controldiff);

            if (controldiff.compareTo(BigDecimal.ZERO) != 0) {

                FfTellerEodInput.info("Getting inside error block");

                currentEodRecord.getSuspenseReversalAmount().setError(
                        "Control Reversal should be performed for the previous day to proceed with the commit "
                                + controldiff + " - Amount to be reversed");
            }

        } catch (Exception e) {

            FfTellerEodInput.info("Exception while doing control account for day 2 " + e);
        }
    }

    // Validate Closing Vault Balance vs Denominations

    private void validateClosingVaultBalance(EbFfEodScreenRecord currentEodRecord) {

        try {

            closingVaultbal = currentEodRecord.getClosingVaultBalance().getValue();
            closingVaultbalBD = new BigDecimal(closingVaultbal);
            closingVaultbalBD = closingVaultbalBD.abs();

            FfTellerEodInput.info("closingVaultbalBD" + closingVaultbalBD);

            closingVaultBalanceBD = closingVaultbalBD.setScale(0, RoundingMode.DOWN);

            FfTellerEodInput.info("closingVaultBalanceBD" + closingVaultBalanceBD);

            denomTypeTotal = currentEodRecord.getDenomOverallTotal().getValue();
            denomTypeTotalBD = new BigDecimal(denomTypeTotal);
            denomTypeTotalBD = denomTypeTotalBD.abs();

            FfTellerEodInput.info("denomTypeTotalBD" + denomTypeTotalBD);

            BigDecimal diff = closingVaultBalanceBD.subtract(denomTypeTotalBD);

            FfTellerEodInput.info("diff" + diff);

            if (diff.compareTo(BigDecimal.ZERO) != 0) {

                FfTellerEodInput.info("Getting inside error block - 124");

                List<String> errmsg = new ArrayList<>();
                errmsg.add("EB-TELLID");
                errmsg.add(diff.toString());

                currentEodRecord.getDenomOverallTotal().setError(errmsg.toString());
            }

            // New logic for Petty Cash and Branch Admin Expenses

            vaultOB = currentEodRecord.getVaultOpeningBalance().getValue();
            vaultOBBD = new BigDecimal(vaultOB);

            cashRecBM = currentEodRecord.getTotalCashAmt().getValue();
            cashRecBMBD = new BigDecimal(cashRecBM);

            BankBCDepRev = currentEodRecord.getReversalOfBank().getValue();
            BankBCDepRevBD = new BigDecimal(BankBCDepRev);
            
            

            innerSumOne = vaultOBBD.add(cashRecBMBD).add(controlReversalaccBD).add(BankBCDepRevBD);
            FfTellerEodInput.info("Inner Sum One is : " + innerSumOne);

            bankBCDepVault = currentEodRecord.getBankBcDepositVault().getValue();
            bankBCDepVaultBD = new BigDecimal(bankBCDepVault);

            pettyCash = currentEodRecord.getPettyCash().getValue();
            pettyCashBD = new BigDecimal(pettyCash);

            branchAdmExp = currentEodRecord.getBranchAdminExpenses().getValue();
            branchAdmExpBD = new BigDecimal(branchAdmExp);

            incidents = currentEodRecord.getIncidentType().getValue();
            incidentsBD = new BigDecimal(incidents);

            controlAcc = currentEodRecord.getSuspenseAmount().getValue();
            controlAccBD = new BigDecimal(controlAcc);

            innerSumTwo = bankBCDepVaultBD.add(pettyCashBD).add(branchAdmExpBD).add(incidentsBD).add(controlAccBD);
            FfTellerEodInput.info("Inner Sum Two is : " + innerSumTwo);

            innerSum = innerSumOne.subtract(innerSumTwo);
            FfTellerEodInput.info("Inner Sum : " + innerSum);

            if (innerSum.compareTo(BigDecimal.ZERO) < 0) {
                FfTellerEodInput.info(
                        "Throwing Error as Petty Cash Withdrawal and Branch Admin Expenses combined should be less than Closing Vault Balance");
                currentEodRecord.getClosingVaultBalance().setError(
                        "Insufficient vault balance. Closing balance is less than Petty Cash Withdrawal and Branch Admin Expense amount.");
            } else {
                FfTellerEodInput.info(
                        "Not Throwing Error as Petty Cash Withdrawal and Branch Admin Expenses combined is less than Closing Vault Balance");
            }

        } catch (Exception e) {

            FfTellerEodInput.info("Exception Catch: " + e);
        }
    }
}