package com.temenos.fusion;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.company.CompanyRecord;
import com.temenos.t24.api.records.ebffbankdepositsvaultconcat.EbFfBankDepositsVaultConcatRecord;
import com.temenos.t24.api.records.ebffbranchadminexpdaily.EbFfBranchAdminExpDailyRecord;
import com.temenos.t24.api.records.ebffcollpostingscreen.EbFfCollPostingScreenRecord;
import com.temenos.t24.api.records.ebffeodscreen.EbFfEodScreenRecord;
import com.temenos.t24.api.records.ebffftcollrevconcat.EbFfFtCollRevConcatRecord;
import com.temenos.t24.api.records.ebffftpettycashlimit.EbFfFtPettyCashLimitRecord;
import com.temenos.t24.api.records.ebffsnatchfraudamtupd.DateOfTxnClass;
import com.temenos.t24.api.records.ebffsnatchfraudamtupd.EbFfSnatchFraudAmtUpdRecord;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

public class EODScreen extends RecordLifecycle {

    private static final FusionFileLogger EODScreenRtn = FusionFileLogger.getLogger(EODScreen.class);

    DataAccess dataAccess = new DataAccess(this);
    Session session = new Session(this);

    String today = "";
    String monthYear = "";
    String companyId = "";
    String lastWorkingDay = "";
    String id = "";
    String yid = "";
    String zid = "";
    String finMnemonic = "";
    String bankDepositsVault = "";
    String finMne = "";

    BigDecimal vaultOpeningBalanceBD = BigDecimal.ZERO;
    BigDecimal totalCollectionAmountBD = BigDecimal.ZERO;
    BigDecimal totalCashAmountBD = BigDecimal.ZERO;
    BigDecimal totalDigitalAmountBD = BigDecimal.ZERO;
    BigDecimal totalPendingCashBD = BigDecimal.ZERO;
    BigDecimal totalDepositBankBD = BigDecimal.ZERO;
    BigDecimal totalDepositBcPointBD = BigDecimal.ZERO;
    BigDecimal pettyCashLimitBD = BigDecimal.ZERO;
    BigDecimal pendingCollectionsBD = BigDecimal.ZERO;
    BigDecimal closingVaultBalanceBD = BigDecimal.ZERO;
    BigDecimal finalBCReversalAmountBD = BigDecimal.ZERO;
    BigDecimal fraudAmountBD = BigDecimal.ZERO;
    BigDecimal suspenseAmountBD = BigDecimal.ZERO;
    BigDecimal suspenseReverseAmountBD = BigDecimal.ZERO;
    BigDecimal debitAmountBD = BigDecimal.ZERO;
    BigDecimal bankDepositsVaultBD = BigDecimal.ZERO;

    @Override
    public void defaultFieldValues(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {

        try {

            EODScreenRtn.info("EOD Screen routine started");

            initialiseSessionValues();

            EbFfEodScreenRecord eodScreenRecord = new EbFfEodScreenRecord(currentRecord);

            EbFfCollPostingScreenRecord collectionRecord = getCollectionPosting();

            vaultOpeningBalanceBD = getPreviousVaultBalance();

            debitAmountBD = getBranchAdminExpenses();
            EODScreenRtn.info("Debit Amount is:" + debitAmountBD);

            bankDepositsVaultBD = getBankDepositsVault();
            EODScreenRtn.info("Bank Deposits Vault BD is :" + bankDepositsVaultBD);

            pettyCashLimitBD = getPettyCashLimit();

            EODScreenRtn.info("pettyCashLimitBD is:" + pettyCashLimitBD);

            finalBCReversalAmountBD = getBCReversalAmount();
            EODScreenRtn.info("finalBCReversalAmountBD is: " + finalBCReversalAmountBD);

            calculateFraudAndSuspense();
            readCollectionValues(collectionRecord);
            calculateClosingVault();

            populateRecord(eodScreenRecord);

            currentRecord.set(eodScreenRecord.toStructure());

            EODScreenRtn.info("EOD Screen routine completed successfully");

        } catch (Exception e) {

            EODScreenRtn.info("Exception in EODScreen hook " + e);
        }
    }

    private void initialiseSessionValues() {

        today = session.getCurrentVariable("!TODAY");

        LocalDate sessionDate = LocalDate.parse(today, DateTimeFormatter.ofPattern("yyyyMMdd"));

        monthYear = sessionDate.format(DateTimeFormatter.ofPattern("MMyyyy"));

        lastWorkingDay = session.getCurrentVariable("!LAST.WORKING.DAY");

        companyId = session.getCompanyId();

        finMnemonic = session.getCompanyRecord().getFinancialMne().getValue();

        id = companyId + "-" + today;
        yid = companyId + "-" + lastWorkingDay;
        zid = companyId + "-" + monthYear;

        EODScreenRtn.info("Session values initialised : today=" + today + " lastWorkingDay=" + lastWorkingDay
                + " companyId=" + companyId);
    }

    private EbFfCollPostingScreenRecord getCollectionPosting() {

        try {

            EODScreenRtn.info("Reading collection posting record : " + id);

            return new EbFfCollPostingScreenRecord(dataAccess.getRecord("EB.FF.COLL.POSTING.SCREEN", id));

        } catch (Exception e) {

            EODScreenRtn.info("Collection posting record not found");
            return null;
        }
    }

    private BigDecimal getPreviousVaultBalance() {

        try {

            EODScreenRtn.info("Reading previous vault balance : " + yid);

            EbFfEodScreenRecord eodScreenRecord = new EbFfEodScreenRecord(
                    dataAccess.getRecord("EB.FF.EOD.SCREEN", yid));

            BigDecimal value = new BigDecimal(eodScreenRecord.getClosingVaultBalance().getValue()).abs();

            EODScreenRtn.info("Previous vault balance = " + value);

            return value;

        } catch (Exception e) {

            EODScreenRtn.info("Previous vault record missing");
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal getBranchAdminExpenses() {

        BigDecimal amount = BigDecimal.ZERO;

        try {

            EODScreenRtn.info("Reading branch admin expenses : " + id);

            EbFfBranchAdminExpDailyRecord branchExpenseRecord = new EbFfBranchAdminExpDailyRecord(
                    dataAccess.getRecord("EB.FF.BRANCH.ADMIN.EXP.DAILY", id));

            amount = new BigDecimal(branchExpenseRecord.getCreditAmt().getValue());

            EODScreenRtn.info("Branch admin expense amount = " + amount);

        } catch (Exception e) {

            EODScreenRtn.info("Branch admin expenses missing");

        }

        return amount;
    }

    private BigDecimal getBankDepositsVault() {

        BigDecimal bankDepImgUpld = BigDecimal.ZERO;

        try {
            EbFfBankDepositsVaultConcatRecord branchExpenseRec = new EbFfBankDepositsVaultConcatRecord(
                    dataAccess.getRecord("EB.FF.BANK.DEPOSITS.VAULT.CONCAT", id));

            bankDepImgUpld = new BigDecimal(branchExpenseRec.getBcDepositVaultConcatImg().getValue());

            EODScreenRtn.info("Bank Deposits Vault is : " + bankDepImgUpld);

        } catch (Exception e) {

            EODScreenRtn.info("Bank/BC Deposits from Vault expenses missing");
        }

        return bankDepImgUpld;
    }

    private BigDecimal getPettyCashLimit() {

        try {

            EODScreenRtn.info("Reading petty cash limit : " + zid);

            EbFfFtPettyCashLimitRecord pettyCashRecord = new EbFfFtPettyCashLimitRecord(
                    dataAccess.getRecord("EB.FF.FT.PETTY.CASH.LIMIT", zid));

            EODScreenRtn.info("pettyCashRecord is: " + pettyCashRecord.toString());

            String ftid = pettyCashRecord.getWithdrawTransaction().getValue();

            EODScreenRtn.info("ftid is: " + ftid);

            getCompanyMnemonic(companyId);

            FundsTransferRecord ftrec = new FundsTransferRecord(
                    dataAccess.getRecord(finMne, "FUNDS.TRANSFER", "", ftid));

            String withdrawalDate = ftrec.getDebitValueDate().getValue();

            EODScreenRtn.info("Funds Transfer record is: " + ftrec.toString());
            EODScreenRtn.info("Withdrawal date is: " + withdrawalDate);
            EODScreenRtn.info("Today is: " + today);

            if (withdrawalDate.equalsIgnoreCase(today)) {
                BigDecimal value = new BigDecimal(pettyCashRecord.getPettyCashLimit().getValue()).abs();

                EODScreenRtn.info("Petty cash limit = " + value);

                return value;

            } else {
                EODScreenRtn.info("Petty cash not done today");
                return BigDecimal.ZERO;
            }

        } catch (Exception e) {

            EODScreenRtn.info("Petty cash limit not found");
            return BigDecimal.ZERO;
        }

    }

    private void getCompanyMnemonic(String companyId) {

        try {
            CompanyRecord compRec = new CompanyRecord(dataAccess.getRecord("COMPANY", companyId));
            finMne = compRec.getFinancialMne().getValue();

            EODScreenRtn.info("compRec is: " + compRec.toString());
            EODScreenRtn.info("finMne is: " + finMne);
        } catch (Exception e) {
            EODScreenRtn.error("getCompanyMnemonic Error" + e.getMessage());
        }
    }

    private BigDecimal getBCReversalAmount() {

        double total = 0;

        try {

            EODScreenRtn.info("Calculating BC reversal amounts");

            List<String> ids = dataAccess.selectRecords(finMnemonic, "EB.FF.FT.COLL.REV.CONCAT", "",
                    "WITH @ID LIKE ..." + id);

            for (String recId : ids) {

                EbFfFtCollRevConcatRecord revConcatRecord = new EbFfFtCollRevConcatRecord(
                        dataAccess.getRecord("EB.FF.FT.COLL.REV.CONCAT", recId));

                String amt = revConcatRecord.getBcPointRevCollected().getValue();

                total += Double.parseDouble(amt);

                EODScreenRtn.info("BC reversal record " + recId + " amount = " + amt);
            }

        } catch (Exception e) {

            EODScreenRtn.info("BC reversal calculation error");
        }

        EODScreenRtn.info("Total BC reversal amount = " + total);

        return BigDecimal.valueOf(total).abs();
    }

    private void calculateFraudAndSuspense() {

        double fraud = 0;
        double suspense = 0;
        double reverse = 0;

        try {

            EODScreenRtn.info("Reading fraud / suspense records");

            EbFfSnatchFraudAmtUpdRecord fraudRecord = new EbFfSnatchFraudAmtUpdRecord(
                    dataAccess.getRecord("EB.FF.SNATCH.FRAUD.AMT.UPD", id));

            for (DateOfTxnClass txn : fraudRecord.getDateOfTxn()) {

                String type = txn.getIncidentTyp().getValue();

                double amount = parseAmount(txn.getFfSnaFrdAmt().getValue());

                EODScreenRtn.info("Incident type = " + type + " amount = " + amount);

                if ("FRAUD".equalsIgnoreCase(type) || "SNATCHING".equalsIgnoreCase(type)) {

                    fraud += amount;

                } else if ("SUSPENSE".equalsIgnoreCase(type)) {

                    suspense += amount;

                } else if ("REVERSE".equalsIgnoreCase(type)) {

                    reverse += amount;
                }
            }

        } catch (Exception e) {

            EODScreenRtn.info("Fraud calculation error");
        }

        fraudAmountBD = BigDecimal.valueOf(fraud).abs();
        suspenseAmountBD = BigDecimal.valueOf(suspense).abs();
        suspenseReverseAmountBD = BigDecimal.valueOf(reverse).abs();

        EODScreenRtn.info("Fraud amount = " + fraudAmountBD);
        EODScreenRtn.info("Suspense amount = " + suspenseAmountBD);
        EODScreenRtn.info("Suspense reverse amount = " + suspenseReverseAmountBD);
    }

    private double parseAmount(String value) {

        try {

            EODScreenRtn.info("Reading amount - Present");

            return Double.parseDouble(value);

        } catch (NumberFormatException e) {

            EODScreenRtn.info("Amount not present");

            return 0;
        }
    }

    private void readCollectionValues(EbFfCollPostingScreenRecord collectionRecord) {

        if (collectionRecord == null) {

            EODScreenRtn.info("Collection record is null");
            return;
        }

        totalCashAmountBD = new BigDecimal(collectionRecord.getTotalCashAmt().getValue()).abs();

        totalPendingCashBD = new BigDecimal(collectionRecord.getTotalPendingCash().getValue()).abs();

        EODScreenRtn.info("Collection values read successfully");
    }

    private void calculateClosingVault() {

        EODScreenRtn.info("Calculating Closing vault balance: ");

        closingVaultBalanceBD = vaultOpeningBalanceBD.add(totalCashAmountBD).subtract(bankDepositsVaultBD)
                .subtract(pettyCashLimitBD).subtract(debitAmountBD).subtract(fraudAmountBD).subtract(suspenseAmountBD)
                .add(suspenseReverseAmountBD).add(finalBCReversalAmountBD);

        EODScreenRtn.info("Closing vault balance calculated = " + closingVaultBalanceBD);
    }

    private String formatAmount(BigDecimal amount) {

        EODScreenRtn.info("Formatting Decimals");

        return amount.setScale(2, RoundingMode.HALF_UP).toString();
    }

    private void populateRecord(EbFfEodScreenRecord eodScreenRecord) {

        EODScreenRtn.info("Populating EOD screen record");

        eodScreenRecord.setVaultOpeningBalance(formatAmount(vaultOpeningBalanceBD));

        eodScreenRecord.setTotalCashAmt(formatAmount(totalCashAmountBD));

        eodScreenRecord.setTotalPendingCash(formatAmount(totalPendingCashBD));

        eodScreenRecord.setBankBcDepositVault(formatAmount(bankDepositsVaultBD));

        eodScreenRecord.setPettyCash(formatAmount(pettyCashLimitBD));

        eodScreenRecord.setIncidentType(formatAmount(fraudAmountBD));

        eodScreenRecord.setReversalOfBank(formatAmount(finalBCReversalAmountBD));

        eodScreenRecord.setBranchAdminExpenses(formatAmount(debitAmountBD));

        eodScreenRecord.setSuspenseAmount(formatAmount(suspenseAmountBD));

        eodScreenRecord.setSuspenseReversalAmount(formatAmount(suspenseReverseAmountBD));

        eodScreenRecord.setClosingVaultBalance(formatAmount(closingVaultBalanceBD));
    }
}