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
import com.temenos.t24.api.records.ebffcollpostingscreen.EbFfCollPostingScreenRecord;
import com.temenos.t24.api.records.ebffeodscreen.EbFfEodScreenRecord;
import com.temenos.t24.api.records.ebffftbranchadminexpensesupd.EbFfFtBranchAdminExpensesUpdRecord;
import com.temenos.t24.api.records.ebffftcollrevconcat.EbFfFtCollRevConcatRecord;
import com.temenos.t24.api.records.ebffftpettycashlimit.EbFfFtPettyCashLimitRecord;
import com.temenos.t24.api.records.ebffsnatchfraudamtupd.DateOfTxnClass;
import com.temenos.t24.api.records.ebffsnatchfraudamtupd.EbFfSnatchFraudAmtUpdRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/**
 *
 * @author ar116388
 *
 */
public class FfBranchAdminExpInpRtn extends RecordLifecycle {

    private static final FusionFileLogger FfBranchAdminExpInp = FusionFileLogger
            .getLogger(FfBranchAdminExpInpRtn.class);

    DataAccess dataAccess = new DataAccess(this);
    Session session = new Session(this);

    String today = "";
    String companyId = "";
    String id = "";
    String vaultOpenBal = "";
    String cashColl = "";
    String branchAdmAmt = "";
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
    BigDecimal branchAdmAmtBD = BigDecimal.ZERO;
    BigDecimal pettyCashBD = BigDecimal.ZERO;
    BigDecimal difference = BigDecimal.ZERO;
    BigDecimal bankDepImgUpldBD = BigDecimal.ZERO;
    BigDecimal totalBD = BigDecimal.ZERO;
    BigDecimal fraudAmountBD = BigDecimal.ZERO;
    BigDecimal suspenseAmountBD = BigDecimal.ZERO;
    BigDecimal suspenseReverseAmountBD = BigDecimal.ZERO;

    EbFfCollPostingScreenRecord collectionRec = null;
    EbFfEodScreenRecord eodScreenRec = null;
    EbFfFtPettyCashLimitRecord pettyCashRecord = null;
    EbFfBankDepositsVaultConcatRecord vaultDeprec = null;

    @Override
    public TValidationResponse validateRecord(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {

        FfBranchAdminExpInp.info("===== Validation Started =====");

        EbFfFtBranchAdminExpensesUpdRecord exprec = new EbFfFtBranchAdminExpensesUpdRecord(currentRecord);

        try {

            today = session.getCurrentVariable("!TODAY");
            companyId = session.getCompanyId();

            LocalDate sessionDate = LocalDate.parse(today, DateTimeFormatter.ofPattern("yyyyMMdd"));
            monthYear = sessionDate.format(DateTimeFormatter.ofPattern("MMyyyy"));

            lastWorkingDay = session.getCurrentVariable("!LAST.WORKING.DAY");

            finMnemonic = session.getCompanyRecord().getFinancialMne().getValue();

            FfBranchAdminExpInp.info("Today : " + today);
            FfBranchAdminExpInp.info("Company Id : " + companyId);
            FfBranchAdminExpInp.info("monthYear : " + monthYear);
            FfBranchAdminExpInp.info("finMnemonic : " + finMnemonic);

            id = companyId + "-" + today;
            yid = companyId + "-" + lastWorkingDay;
            zid = companyId + "-" + monthYear;

            FfBranchAdminExpInp.info("Constructed Record Id : " + id);
            FfBranchAdminExpInp.info("Constructed Record YID : " + yid);
            FfBranchAdminExpInp.info("Constructed Record ZId : " + zid);

            // Cash received in Branch

            try {

                collectionRec = new EbFfCollPostingScreenRecord(dataAccess.getRecord("EB.FF.COLL.POSTING.SCREEN", id));
                cashColl = collectionRec.getTotalCashAmt().getValue();
                cashCollBD = new BigDecimal((cashColl == null || cashColl.isEmpty()) ? "0" : cashColl);

            } catch (Exception e) {
                FfBranchAdminExpInp.info(
                        "EB.FF.COLL.POSTING.SCREEN record not found for ID : " + id + ". Using Collection Amount = 0");
                cashCollBD = BigDecimal.ZERO;
            }

            FfBranchAdminExpInp.info("Cash Collection Amount : " + cashCollBD.toString());

            // Vault Opening Balance

            try {
                eodScreenRec = new EbFfEodScreenRecord(dataAccess.getRecord("EB.FF.EOD.SCREEN", yid));
                vaultOpenBal = eodScreenRec.getClosingVaultBalance().getValue();
                vaultOpenBalBD = new BigDecimal((vaultOpenBal == null || vaultOpenBal.isEmpty()) ? "0" : vaultOpenBal);

            } catch (Exception e) {
                FfBranchAdminExpInp.info(
                        "EB.FF.EOD.SCREEN record not found for ID : " + yid + ". Using Vault Opening Balance = 0");
                vaultOpenBalBD = BigDecimal.ZERO;
            }

            FfBranchAdminExpInp.info("Vault Opening Balance : " + vaultOpenBalBD.toString());

            // Petty Cash Withdrawal

            try {
                pettyCashRecord = new EbFfFtPettyCashLimitRecord(
                        dataAccess.getRecord("EB.FF.FT.PETTY.CASH.LIMIT", zid));
                pettyCash = pettyCashRecord.getPettyCashLimit().getValue();
                pettyCashBD = new BigDecimal((pettyCash == null || pettyCash.isEmpty()) ? "0" : pettyCash);
            } catch (Exception e) {
                FfBranchAdminExpInp.info("EB.FF.FT.PETTY.CASH.LIMIT record not found for ID : " + id
                        + ". Using Petty Cash Withdrawal = 0");
                pettyCashBD = BigDecimal.ZERO;

            }
            FfBranchAdminExpInp.info("Petty Cash Expense Amount : " + pettyCashBD.toString());

            // Bank/BC deposit (Vault)

            try {

                vaultDeprec = new EbFfBankDepositsVaultConcatRecord(
                        dataAccess.getRecord("EB.FF.BANK.DEPOSITS.VAULT.CONCAT", id));

                bankDepImgUpld = vaultDeprec.getBcDepositVaultConcatImg().getValue();

                bankDepImgUpldBD = new BigDecimal(
                        (bankDepImgUpld == null || bankDepImgUpld.isEmpty()) ? "0" : bankDepImgUpld);

            } catch (Exception e) {
                FfBranchAdminExpInp.info("EB.FF.BANK.DEPOSITS.VAULT.CONCAT record not found for ID : " + id
                        + ". Using Bank Deposit Vault = 0");
                bankDepImgUpldBD = BigDecimal.ZERO;
            }

            FfBranchAdminExpInp.info("bankDepImgUpldBD : " + bankDepImgUpldBD);

            // Reversal of Bank/BC deposits

            double total = 0;

            try {

                FfBranchAdminExpInp.info("Calculating BC reversal amounts");

                List<String> ids = dataAccess.selectRecords(finMnemonic, "EB.FF.FT.COLL.REV.CONCAT", "",
                        "WITH @ID LIKE ..." + id);

                for (String recId : ids) {

                    EbFfFtCollRevConcatRecord revConcatRecord = new EbFfFtCollRevConcatRecord(
                            dataAccess.getRecord("EB.FF.FT.COLL.REV.CONCAT", recId));

                    String amt = revConcatRecord.getBcPointRevCollected().getValue();

                    total += Double.parseDouble(amt);

                    FfBranchAdminExpInp.info("BC reversal record " + recId + " amount = " + amt);

                }

            } catch (Exception e) {
                FfBranchAdminExpInp.info("Catch Block of BC Reversal Amount");
            }

            totalBD = BigDecimal.valueOf(total).abs();

            FfBranchAdminExpInp.info("totalBD : " + totalBD);

            // Incidents — Snatching / Fraud : Control Account Posting : Control Account
            // Reversal

            double fraud = 0;
            double suspense = 0;
            double reverse = 0;

            try {

                FfBranchAdminExpInp.info("Reading fraud / suspense records");

                EbFfSnatchFraudAmtUpdRecord fraudRecord = new EbFfSnatchFraudAmtUpdRecord(
                        dataAccess.getRecord("EB.FF.SNATCH.FRAUD.AMT.UPD", id));

                for (DateOfTxnClass txn : fraudRecord.getDateOfTxn()) {

                    String type = txn.getIncidentTyp().getValue();

                    double amount = parseAmount(txn.getFfSnaFrdAmt().getValue());

                    FfBranchAdminExpInp.info("Incident type = " + type + " amount = " + amount);

                    if ("FRAUD".equalsIgnoreCase(type) || "SNATCHING".equalsIgnoreCase(type)) {

                        fraud += amount;

                    } else if ("SUSPENSE".equalsIgnoreCase(type)) {

                        suspense += amount;

                    } else if ("REVERSE".equalsIgnoreCase(type)) {
                        reverse += amount;
                    }
                }

            } catch (Exception e) {
                FfBranchAdminExpInp.info("Catch Block of Fraud, Suspense and Reversal");
            }

            fraudAmountBD = BigDecimal.valueOf(fraud).abs();
            suspenseAmountBD = BigDecimal.valueOf(suspense).abs();
            suspenseReverseAmountBD = BigDecimal.valueOf(reverse).abs();

            FfBranchAdminExpInp.info("fraudAmountBD : " + fraudAmountBD);
            FfBranchAdminExpInp.info("suspenseAmountBD : " + suspenseAmountBD);
            FfBranchAdminExpInp.info("suspenseReverseAmountBD : " + suspenseReverseAmountBD);

            FfBranchAdminExpInp.info("Successfully fetched records");
            
            //Branch Admin Expenses

            branchAdmAmt = exprec.getCreditAmt().getValue();
            branchAdmAmtBD = new BigDecimal((branchAdmAmt == null || branchAdmAmt.isEmpty()) ? "0" : branchAdmAmt);

            FfBranchAdminExpInp.info("Branch Admin Expense Amount : " + branchAdmAmtBD.toString());

            // Add everything
            sum = vaultOpenBalBD.add(cashCollBD).subtract(bankDepImgUpldBD).subtract(pettyCashBD)
                    .subtract(fraudAmountBD).subtract(suspenseAmountBD).add(suspenseReverseAmountBD).add(totalBD);

            FfBranchAdminExpInp.info("Final Sum Value : " + sum);

            // Validation
            if (sum.compareTo(branchAdmAmtBD) < 0) {

                FfBranchAdminExpInp.error("Validation Failed : Branch Admin expenses exceed allowed amount");

                exprec.getCreditAmt().setError(
                        "Insufficient vault balance. Closing balance is less than Branch Admin Expense amount.");

            } else {

                FfBranchAdminExpInp.info("Validation Passed Successfully");
            }

            FfBranchAdminExpInp.info("===== Validation Completed =====");

        } catch (Exception e) {

            FfBranchAdminExpInp.error("Exception occurred during validation : " + e.getMessage());
        }

        return exprec.getValidationResponse();
    }

    private double parseAmount(String value) {
        try {

            FfBranchAdminExpInp.info("Reading amount - Present");

            return Double.parseDouble(value);

        } catch (NumberFormatException e) {

            FfBranchAdminExpInp.info("Amount not present");

            return 0;
        }
    }
}