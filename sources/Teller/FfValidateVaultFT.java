package com.temenos.fusion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.api.TValidationResponse;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebffbankdepositsvaultconcat.EbFfBankDepositsVaultConcatRecord;
import com.temenos.t24.api.records.ebffbranchadminexpdaily.EbFfBranchAdminExpDailyRecord;
import com.temenos.t24.api.records.ebffcollpostingscreen.EbFfCollPostingScreenRecord;
import com.temenos.t24.api.records.ebffeodscreen.EbFfEodScreenRecord;
import com.temenos.t24.api.records.ebffftcollrevconcat.EbFfFtCollRevConcatRecord;
import com.temenos.t24.api.records.ebffftpettycashlimit.EbFfFtPettyCashLimitRecord;
import com.temenos.t24.api.records.ebffpettycashexpdetupd.EbFfPettyCashExpDetUpdRecord;
import com.temenos.t24.api.records.ebffrouser.EbFfRoUserRecord;
import com.temenos.t24.api.records.ebffsnatchfraudamtupd.DateOfTxnClass;
import com.temenos.t24.api.records.ebffsnatchfraudamtupd.EbFfSnatchFraudAmtUpdRecord;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/**
 * TODO: Document me!
 *
 * @author mp116300
 *
 */
public class FfValidateVaultFT extends RecordLifecycle {
    private static final FusionFileLogger FfValidateVaultFT = FusionFileLogger.getLogger(FfValidateVaultFT.class);

    DataAccess dataAccess = new DataAccess(this);
    Session session = new Session(this);

    String today = "";
    String companyId = "";
    String id = "";
    String vaultOpenBal = "";
    String branchAdmExp = "";
    String cashColl = "";
    String ftCredAmount = "";
    String zid = "";
    String monthYear = "";
    String pettyCash = "";
    String yid = "";
    String lastWorkingDay = "";
    String bankDepImgUpld = "";
    String finMnemonic = "";

    BigDecimal vaultOpenBalBD = BigDecimal.ZERO;
    BigDecimal cashCollBD = BigDecimal.ZERO;
    BigDecimal sum = BigDecimal.ZERO;
    BigDecimal ftCredAmountBD = BigDecimal.ZERO;
    BigDecimal pettyCashBD = BigDecimal.ZERO;
    BigDecimal difference = BigDecimal.ZERO;
    BigDecimal bankDepImgUpldBD = BigDecimal.ZERO;
    BigDecimal totalBD = BigDecimal.ZERO;
    BigDecimal fraudAmountBD = BigDecimal.ZERO;
    BigDecimal suspenseAmountBD = BigDecimal.ZERO;
    BigDecimal suspenseReverseAmountBD = BigDecimal.ZERO;
    BigDecimal branchAdmExpBD = BigDecimal.ZERO;

    EbFfCollPostingScreenRecord collectionRec = null;
    EbFfEodScreenRecord eodScreenRec = null;
    EbFfFtPettyCashLimitRecord pettyCashRecord = null;
    EbFfBankDepositsVaultConcatRecord vaultDeprec = null;
    EbFfBranchAdminExpDailyRecord branchAdmExpRec = null;

    @Override
    public TValidationResponse validateRecord(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {

        FundsTransferRecord ftRec = new FundsTransferRecord(currentRecord);
        FfValidateVaultFT.info("ftRec : " + ftRec.toString());
        String employeeId = ftRec.getLocalRefField("FF.POSTED.BY").getValue();

        try {

            today = session.getCurrentVariable("!TODAY");
            companyId = session.getCompanyId();

            LocalDate sessionDate = LocalDate.parse(today, DateTimeFormatter.ofPattern("yyyyMMdd"));
            monthYear = sessionDate.format(DateTimeFormatter.ofPattern("MMyyyy"));

            lastWorkingDay = session.getCurrentVariable("!LAST.WORKING.DAY");

            finMnemonic = session.getCompanyRecord().getFinancialMne().getValue();

            FfValidateVaultFT.info("Today : " + today);
            FfValidateVaultFT.info("Company Id : " + companyId);
            FfValidateVaultFT.info("monthYear : " + monthYear);
            FfValidateVaultFT.info("finMnemonic : " + finMnemonic);

            id = companyId + "-" + today;
            yid = companyId + "-" + lastWorkingDay;
            zid = companyId + "-" + monthYear;

            FfValidateVaultFT.info("Constructed Record Id : " + id);
            FfValidateVaultFT.info("Constructed Record YID : " + yid);
            FfValidateVaultFT.info("Constructed Record ZId : " + zid);

            // Cash received in Branch

            try {

                collectionRec = new EbFfCollPostingScreenRecord(dataAccess.getRecord("EB.FF.COLL.POSTING.SCREEN", id));
                cashColl = collectionRec.getTotalCashAmt().getValue();
                cashCollBD = new BigDecimal((cashColl == null || cashColl.isEmpty()) ? "0" : cashColl);

            } catch (Exception e) {
                FfValidateVaultFT.info(
                        "EB.FF.COLL.POSTING.SCREEN record not found for ID : " + id + ". Using Collection Amount = 0");
                cashCollBD = BigDecimal.ZERO;
            }

            FfValidateVaultFT.info("Cash Collection Amount : " + cashCollBD.toString());

            // Vault Opening Balance

            try {
                eodScreenRec = new EbFfEodScreenRecord(dataAccess.getRecord("EB.FF.EOD.SCREEN", yid));
                vaultOpenBal = eodScreenRec.getClosingVaultBalance().getValue();
                vaultOpenBalBD = new BigDecimal((vaultOpenBal == null || vaultOpenBal.isEmpty()) ? "0" : vaultOpenBal);

            } catch (Exception e) {
                FfValidateVaultFT.info(
                        "EB.FF.EOD.SCREEN record not found for ID : " + yid + ". Using Vault Opening Balance = 0");
                vaultOpenBalBD = BigDecimal.ZERO;
            }

            FfValidateVaultFT.info("Vault Opening Balance : " + vaultOpenBalBD.toString());

            // Branch Admin Expenses
            try {
                branchAdmExpRec = new EbFfBranchAdminExpDailyRecord(
                        dataAccess.getRecord("EB.FF.BRANCH.ADMIN.EXP.DAILY", id));
                FfValidateVaultFT.info("Branch Admin Expense record " + branchAdmExpRec.toString());
                branchAdmExp = branchAdmExpRec.getCreditAmt().getValue();
                branchAdmExpBD = new BigDecimal((branchAdmExp == null || branchAdmExp.isEmpty()) ? "0" : branchAdmExp);

            } catch (Exception e) {
                FfValidateVaultFT.info("EB.FF.BRANCH.ADMIN.EXP.DAILY record not found for ID : " + id
                        + ". Using Branch Admin Expense = 0");
                branchAdmExpBD = BigDecimal.ZERO;
            }

            FfValidateVaultFT.info("Branch Admin Expense Amount " + branchAdmExpBD.toString());
            // Petty Cash Withdrawal

            /*
             * try { pettyCashRecord = new EbFfFtPettyCashLimitRecord(
             * dataAccess.getRecord("EB.FF.FT.PETTY.CASH.LIMIT", zid)); pettyCash =
             * pettyCashRecord.getPettyCashLimit().getValue(); pettyCashBD = new
             * BigDecimal((pettyCash == null || pettyCash.isEmpty()) ? "0" : pettyCash); }
             * catch (Exception e) {
             * FfValidateVaultFT.info("EB.FF.FT.PETTY.CASH.LIMIT record not found for ID : "
             * + id + ". Using Petty Cash Withdrawal = 0"); pettyCashBD = BigDecimal.ZERO;
             * 
             * } FfValidateVaultFT.info("Petty Cash Expense Amount : " +
             * pettyCashBD.toString());
             */
            getPettycashWithd();
            FfValidateVaultFT.info("Petty Cash Expense Amount : " + pettyCashBD.toString());
            // Bank/BC deposit (Vault)

            try {

                vaultDeprec = new EbFfBankDepositsVaultConcatRecord(
                        dataAccess.getRecord("EB.FF.BANK.DEPOSITS.VAULT.CONCAT", id));

                bankDepImgUpld = vaultDeprec.getBcDepositVaultConcatImg().getValue();

                bankDepImgUpldBD = new BigDecimal(
                        (bankDepImgUpld == null || bankDepImgUpld.isEmpty()) ? "0" : bankDepImgUpld);

            } catch (Exception e) {
                FfValidateVaultFT.info("EB.FF.BANK.DEPOSITS.VAULT.CONCAT record not found for ID : " + id
                        + ". Using Bank Deposit Vault = 0");
                bankDepImgUpldBD = BigDecimal.ZERO;
            }

            FfValidateVaultFT.info("bankDepImgUpldBD : " + bankDepImgUpldBD);

            // Reversal of Bank/BC deposits

            double total = 0;

            try {

                FfValidateVaultFT.info("Calculating BC reversal amounts");

                List<String> ids = dataAccess.selectRecords(finMnemonic, "EB.FF.FT.COLL.REV.CONCAT", "",
                        "WITH @ID LIKE ..." + id);

                for (String recId : ids) {

                    EbFfFtCollRevConcatRecord revConcatRecord = new EbFfFtCollRevConcatRecord(
                            dataAccess.getRecord("EB.FF.FT.COLL.REV.CONCAT", recId));

                    String amt = revConcatRecord.getBcPointRevCollected().getValue();

                    total += Double.parseDouble(amt);

                    FfValidateVaultFT.info("BC reversal record " + recId + " amount = " + amt);

                }

            } catch (Exception e) {
                FfValidateVaultFT.info("Catch Block of BC Reversal Amount");
            }

            totalBD = BigDecimal.valueOf(total).abs();

            FfValidateVaultFT.info("totalBD : " + totalBD);

            // Incidents — Snatching / Fraud : Control Account Posting : Control Account
            // Reversal

            double fraud = 0;
            double suspense = 0;
            double reverse = 0;

            try {

                FfValidateVaultFT.info("Reading fraud / suspense records");

                EbFfSnatchFraudAmtUpdRecord fraudRecord = new EbFfSnatchFraudAmtUpdRecord(
                        dataAccess.getRecord("EB.FF.SNATCH.FRAUD.AMT.UPD", id));

                for (DateOfTxnClass txn : fraudRecord.getDateOfTxn()) {

                    String type = txn.getIncidentTyp().getValue();

                    double amount = parseAmount(txn.getFfSnaFrdAmt().getValue());

                    FfValidateVaultFT.info("Incident type = " + type + " amount = " + amount);

                    if ("FRAUD".equalsIgnoreCase(type) || "SNATCHING".equalsIgnoreCase(type)) {

                        fraud += amount;

                    } else if ("SUSPENSE".equalsIgnoreCase(type)) {

                        suspense += amount;

                    } else if ("REVERSE".equalsIgnoreCase(type)) {
                        reverse += amount;
                    }
                }

            } catch (Exception e) {
                FfValidateVaultFT.info("Catch Block of Fraud, Suspense and Reversal");
            }

            fraudAmountBD = BigDecimal.valueOf(fraud).abs();
            suspenseAmountBD = BigDecimal.valueOf(suspense).abs();
            suspenseReverseAmountBD = BigDecimal.valueOf(reverse).abs();

            FfValidateVaultFT.info("fraudAmountBD : " + fraudAmountBD);
            FfValidateVaultFT.info("suspenseAmountBD : " + suspenseAmountBD);
            FfValidateVaultFT.info("suspenseReverseAmountBD : " + suspenseReverseAmountBD);

            FfValidateVaultFT.info("Successfully fetched records");

            // Vault FT Credit Amount

            ftCredAmount = ftRec.getCreditAmount().getValue();
            ftCredAmountBD = new BigDecimal((ftCredAmount == null || ftCredAmount.isEmpty()) ? "0" : ftCredAmount);

            FfValidateVaultFT.info("Credit Amount from FT : " + ftCredAmountBD.toString());

            // Add everything
            sum = vaultOpenBalBD.add(cashCollBD).subtract(bankDepImgUpldBD).subtract(pettyCashBD)
                    .subtract(branchAdmExpBD).subtract(fraudAmountBD).subtract(suspenseAmountBD)
                    .add(suspenseReverseAmountBD).add(totalBD);

            FfValidateVaultFT.info("Final Sum Value : " + sum);

            checkThevalidationForEmpId(employeeId, ftRec);

            // Validation
            if (ftCredAmountBD.compareTo(sum) > 0) {

                FfValidateVaultFT.error("Validation Failed : Branch Admin expenses exceed allowed amount");
                ftRec.getCreditAmount().setError("EB-BC.DEPOSIT.VAULT.FT.ERR");

            } else {

                FfValidateVaultFT.info("Validation Passed Successfully");
            }

            FfValidateVaultFT.info("===== Validation Completed =====");

        } catch (Exception e) {

            FfValidateVaultFT.error("Exception occurred during validation : " + e.getMessage());
        }
        currentRecord.set(ftRec.toStructure());
        return ftRec.getValidationResponse();
    }

    private double parseAmount(String value) {
        try {

            FfValidateVaultFT.info("Reading amount - Present");

            return Double.parseDouble(value);

        } catch (NumberFormatException e) {

            FfValidateVaultFT.info("Amount not present");

            return 0;
        }
    }

    /**
     * @param employeeId
     * @param ftRec
     */
    private void checkThevalidationForEmpId(String employeeId, FundsTransferRecord ftRec) {
        FfValidateVaultFT.info("checkThevalidationForEmpId method is triggered" + employeeId);
        EbFfRoUserRecord roUserRec = null;
        if (!employeeId.isEmpty()) {
            FfValidateVaultFT.info("Entering if" + employeeId);
            try {
                roUserRec = new EbFfRoUserRecord(dataAccess.getRecord("EB.FF.RO.USER", employeeId));
                String roName = roUserRec.getRoName().getValue();
                String branchId = roUserRec.getBranchId().getValue();
                if ((!branchId.isEmpty()) && (branchId.equals(session.getCompanyId()))) {
                    FfValidateVaultFT.info("roName" + roName);
                    ftRec.getLocalRefField("FF.POST.LGLNAME").setValue(roName);
                } else {
                    ftRec.getLocalRefField("FF.POSTED.BY")
                            .setError("This RO is not allocated for this Branch" + session.getCompanyId());
                }
            } catch (Exception e) {
                FfValidateVaultFT.error("roUserRec doesn't exit" + e);
                ftRec.getLocalRefField("FF.POSTED.BY").setError("EB-EMPLOYEE.VAL.ERR");
            }
        }

    }

    private void getPettycashWithd() {
        try {
            pettyCash = "";
            EbFfPettyCashExpDetUpdRecord pettyCashRecord = new EbFfPettyCashExpDetUpdRecord(
                    dataAccess.getRecord("EB.FF.PETTY.CASH.EXP.DET.UPD", id));
            pettyCash = pettyCashRecord.getPettyCashExpAmt().getValue();
            FfValidateVaultFT.info(" pettyCashWithdraw -> " + pettyCash);
            pettyCashBD = new BigDecimal((pettyCash == null || pettyCash.isEmpty()) ? "0" : pettyCash);
        } catch (Exception e) {
            FfValidateVaultFT.info(" EB.FF.PETTY.CASH.EXP.DET.UPD record Missing -> " + e);
            pettyCashBD = BigDecimal.ZERO;
        }
        FfValidateVaultFT.info("Petty Cash Expense Amount : " + pettyCashBD.toString());

    }
}
