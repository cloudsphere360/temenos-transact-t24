package com.temenos.fusion;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import java.math.BigDecimal;
import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.servicehook.ServiceData;
import com.temenos.t24.api.complex.eb.servicehook.SynchronousTransactionData;
import com.temenos.t24.api.complex.eb.servicehook.TransactionControl;
import com.temenos.t24.api.hook.system.ServiceLifecycle;
import com.temenos.t24.api.records.company.CompanyRecord;
import com.temenos.t24.api.records.dates.DatesRecord;
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
import com.temenos.t24.api.system.Date;
import com.temenos.t24.api.system.Session;

public class FfAutoEOD extends ServiceLifecycle {

    private static final FusionFileLogger FfAutoEodRtn = FusionFileLogger.getLogger(FfAutoEOD.class);

    DataAccess da = new DataAccess(this);
    Session session = new Session(this);

    String today = "";
    String eodId = "";

    String monthYear = "";
    String lastWorkingDay = "";
    String id = "";
    String yid = "";
    String zid = "";
    String finMnemonic = "";
    String debitAmount = "";
    String bankDepositsVault = "";
    String companyID = "";
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
    public List<String> getIds(ServiceData serviceData, List<String> controlList) {

        FfAutoEodRtn.info("===== ENTER getIds =====");

        List<String> companies = new ArrayList<>();

        try {
            companies = da.selectRecords("", "COMPANY", "", "");
            FfAutoEodRtn.info("Fetched Companies: " + companies);
        } catch (Exception e) {
            FfAutoEodRtn.info("Error fetching companies: " + e);
        }

        FfAutoEodRtn.info("Total companies count: " + companies.size());
        FfAutoEodRtn.info("===== EXIT getIds =====");

        return companies;
    }

    @Override
    public void updateRecord(String id, ServiceData serviceData, String controlItem,
            TransactionControl transactionControl, List<SynchronousTransactionData> transactionData,
            List<TStructure> records) {

        FfAutoEodRtn.info("===== START updateRecord =====");
        FfAutoEodRtn.info("Input Company ID: " + id);

        Date dates = new Date(this);
        String dattee = dates.getDates().getToday().getValue();
        String lassttdate = dates.getDates().getLastWorkingDay().getValue();

        FfAutoEodRtn.info("dattee is : " + dattee.toString());
        FfAutoEodRtn.info("lassttdate is : " + lassttdate.toString());

        companyID = id;

        try {

            // Reading dates record for the current company ID

            TStructure datesRec = da.getRecord("DATES", companyID);
            DatesRecord datesRecord = new DatesRecord(datesRec);
            String coBatchStatus = datesRecord.getCoBatchStatus().getValue();

            FfAutoEodRtn.info("datesRec is : " + datesRec.toString());
            FfAutoEodRtn.info("datesRecord is : " + datesRecord.toString());
            FfAutoEodRtn.info("coBatchStatus is : " + coBatchStatus.toString());

            // -------------------------------------------------
            // IF BATCH STATUS = O
            // USE CURRENT COMPANY DATES RECORD
            // -------------------------------------------------

            if ("O".equalsIgnoreCase(coBatchStatus)) {

                today = datesRecord.getToday().getValue();
                lastWorkingDay = datesRecord.getLastWorkingDay().getValue();

                FfAutoEodRtn.info("Using ONLINE dates record");

            } else {
                // -------------------------------------------------
                // ELSE READ COB COMPANY DATES RECORD
                // Example: IN-001-0001-COB
                // -------------------------------------------------
                String cobCompany = companyID + "-COB";

                FfAutoEodRtn.info("Reading COB dates record : " + cobCompany);

                TStructure cobDatesRec = da.getRecord("DATES", cobCompany);

                DatesRecord cobDatesRecord = new DatesRecord(cobDatesRec);

                today = cobDatesRecord.getToday().getValue();
                lastWorkingDay = cobDatesRecord.getLastWorkingDay().getValue();

                FfAutoEodRtn.info("Using COB dates record");
            }

            LocalDate sessionDate = LocalDate.parse(today, DateTimeFormatter.ofPattern("yyyyMMdd"));
            monthYear = sessionDate.format(DateTimeFormatter.ofPattern("MMyyyy"));

            FfAutoEodRtn.info("TODAY: " + today);
            FfAutoEodRtn.info("LAST WORKING DAY: " + lastWorkingDay);
            FfAutoEodRtn.info("MONTH YEAR: " + monthYear);

            eodId = companyID + "-" + today;
            yid = companyID + "-" + lastWorkingDay;
            zid = companyID + "-" + monthYear;

            FfAutoEodRtn.info("Generated EOD ID: " + eodId);
            FfAutoEodRtn.info("Generated EOD YID: " + yid);
            FfAutoEodRtn.info("Generated EOD ZID: " + zid);

            FfAutoEodRtn.info("Fetching collection posting...");
            EbFfCollPostingScreenRecord collectionRecord = getCollectionPosting();

            FfAutoEodRtn.info("Fetching previous vault balance...");
            vaultOpeningBalanceBD = getPreviousVaultBalance();

            FfAutoEodRtn.info("Fetching branch admin expenses...");
            debitAmountBD = getBranchAdminExpenses();
            FfAutoEodRtn.info("Debit Amount is:" + debitAmountBD);

            FfAutoEodRtn.info("Fetching Bank/BC Deposit from Vault...");
            bankDepositsVaultBD = getBankDepositsVault();
            FfAutoEodRtn.info("Bank Deposits Vault BD is :" + bankDepositsVaultBD);

            FfAutoEodRtn.info("Fetching petty cash limit...");
            pettyCashLimitBD = getPettyCashLimit();

            FfAutoEodRtn.info("Fetching BC reversal amount...");
            finalBCReversalAmountBD = getBCReversalAmount();

            FfAutoEodRtn.info("Calculating fraud/suspense...");
            calculateFraudAndSuspense();

            FfAutoEodRtn.info("Reading collection values...");
            readCollectionValues(collectionRecord);

            FfAutoEodRtn.info("Calculating closing vault...");
            calculateClosingVault();

        } catch (Exception e) {
            FfAutoEodRtn.info("Exception in main processing: " + e.toString());
        }

        FfAutoEodRtn.info("Creating EOD record object...");
        EbFfEodScreenRecord eodRecord = frameEbFfEodScreenRecord(today, vaultOpeningBalanceBD, totalCashAmountBD,
                totalPendingCashBD, bankDepositsVaultBD, pettyCashLimitBD, debitAmountBD, suspenseAmountBD,
                suspenseReverseAmountBD, fraudAmountBD, finalBCReversalAmountBD, closingVaultBalanceBD);

        FfAutoEodRtn.info("Checking if record is empty using equals...");

        if (!eodRecord.equals(new EbFfEodScreenRecord(this))) {

            FfAutoEodRtn.info("Record has data. Creating transaction...");

            SynchronousTransactionData txnData = frameTxnData();
            records.add(eodRecord.toStructure());
            transactionData.add(txnData);

        } else {
            FfAutoEodRtn.info("Record is empty. Skipping transaction.");
        }

        FfAutoEodRtn.info("===== END updateRecord =====");
    }

    private EbFfCollPostingScreenRecord getCollectionPosting() {

        try {
            FfAutoEodRtn.info("Reading COLLECTION record for ID: " + id);
            return new EbFfCollPostingScreenRecord(da.getRecord("EB.FF.COLL.POSTING.SCREEN", eodId));
        } catch (Exception e) {
            FfAutoEodRtn.info("Collection record NOT FOUND for ID: " + id);
            return null;
        }
    }

    private BigDecimal getPreviousVaultBalance() {

        try {
            FfAutoEodRtn.info("Reading previous vault for: " + yid);
            EbFfEodScreenRecord rec = new EbFfEodScreenRecord(da.getRecord("EB.FF.EOD.SCREEN", yid));

            BigDecimal val = new BigDecimal(rec.getClosingVaultBalance().getValue());
            FfAutoEodRtn.info("Previous vault balance: " + val);
            return val;

        } catch (Exception e) {
            FfAutoEodRtn.info("Previous vault NOT FOUND");
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal getBranchAdminExpenses() {

        BigDecimal amt = BigDecimal.ZERO;

        try {
            FfAutoEodRtn.info("Reading admin expense for: " + id);

            EbFfBranchAdminExpDailyRecord rec = new EbFfBranchAdminExpDailyRecord(
                    da.getRecord("EB.FF.BRANCH.ADMIN.EXP.DAILY", eodId));

            amt = new BigDecimal(rec.getCreditAmt().getValue());
            FfAutoEodRtn.info("Admin expense: " + amt);

        } catch (Exception e) {
            FfAutoEodRtn.info("Admin expense NOT FOUND");

        }

        return amt;
    }

    private BigDecimal getBankDepositsVault() {

        BigDecimal bankDepImgUpld = BigDecimal.ZERO;

        try {
            EbFfBankDepositsVaultConcatRecord branchExpenseRec = new EbFfBankDepositsVaultConcatRecord(
                    da.getRecord("EB.FF.BANK.DEPOSITS.VAULT.CONCAT", eodId));

            bankDepImgUpld = new BigDecimal(branchExpenseRec.getBcDepositVaultConcatImg().getValue());

            FfAutoEodRtn.info("Bank/BC Deposits from Vault expenses amount = " + bankDepImgUpld);

        } catch (Exception e) {

            FfAutoEodRtn.info("Bank/BC Deposits from Vault expenses missing");
        }

        return bankDepImgUpld;
    }

    private BigDecimal getPettyCashLimit() {

        try {

            FfAutoEodRtn.info("Reading petty cash limit : " + zid);

            EbFfFtPettyCashLimitRecord pettyCashRecord = new EbFfFtPettyCashLimitRecord(
                    da.getRecord("EB.FF.FT.PETTY.CASH.LIMIT", zid));

            FfAutoEodRtn.info("pettyCashRecord is: " + pettyCashRecord.toString());

            String ftid = pettyCashRecord.getWithdrawTransaction().getValue();

            FfAutoEodRtn.info("ftid is: " + ftid);

            getCompanyMnemonic(companyID);

            FundsTransferRecord ftrec = new FundsTransferRecord(da.getRecord(finMne, "FUNDS.TRANSFER", "", ftid));

            String withdrawalDate = ftrec.getDebitValueDate().getValue();

            FfAutoEodRtn.info("Funds Transfer record is: " + ftrec.toString());
            FfAutoEodRtn.info("Withdrawal date is: " + withdrawalDate);
            FfAutoEodRtn.info("Today is: " + today);

            if (withdrawalDate.equalsIgnoreCase(today)) {
                BigDecimal value = new BigDecimal(pettyCashRecord.getPettyCashLimit().getValue()).abs();

                FfAutoEodRtn.info("Petty cash limit = " + value);

                return value;

            } else {
                FfAutoEodRtn.info("Petty cash not done today");
                return BigDecimal.ZERO;
            }

        } catch (Exception e) {

            FfAutoEodRtn.info("Petty cash limit not found");
            return BigDecimal.ZERO;
        }

    }

    private void getCompanyMnemonic(String companyId) {

        try {
            CompanyRecord compRec = new CompanyRecord(da.getRecord("COMPANY", companyId));
            finMne = compRec.getFinancialMne().getValue();

            FfAutoEodRtn.info("compRec is: " + compRec.toString());
            FfAutoEodRtn.info("finMne is: " + finMne);
        } catch (Exception e) {
            FfAutoEodRtn.error("getCompanyMnemonic Error" + e.getMessage());
        }
    }

    private BigDecimal getBCReversalAmount() {

        double total = 0;

        try {
            FfAutoEodRtn.info("Fetching BC reversal records...");
            
            getCompanyMnemonic(companyID);
            
            List<String> ids = da.selectRecords(finMne, "EB.FF.FT.COLL.REV.CONCAT", "",
                    "WITH @ID LIKE ..." + eodId);

            for (String recId : ids) {

                EbFfFtCollRevConcatRecord rec = new EbFfFtCollRevConcatRecord(
                        da.getRecord("EB.FF.FT.COLL.REV.CONCAT", recId));

                String amt = rec.getBcPointRevCollected().getValue();
                total += Double.parseDouble(amt);

                FfAutoEodRtn.info("BC reversal record: " + recId + " Amt: " + amt);
            }

        } catch (Exception e) {
            FfAutoEodRtn.info("BC reversal error: " + e);
        }

        FfAutoEodRtn.info("Total BC reversal: " + total);

        return BigDecimal.valueOf(total).abs();
    }

    private void calculateFraudAndSuspense() {

        double fraud = 0;
        double suspense = 0;
        double reverse = 0;

        try {
            FfAutoEodRtn.info("Reading fraud records for: " + id);

            EbFfSnatchFraudAmtUpdRecord rec = new EbFfSnatchFraudAmtUpdRecord(
                    da.getRecord("EB.FF.SNATCH.FRAUD.AMT.UPD", eodId));

            for (DateOfTxnClass txn : rec.getDateOfTxn()) {

                String type = txn.getIncidentTyp().getValue();

                double amount = parseAmount(txn.getFfSnaFrdAmt().getValue());

                FfAutoEodRtn.info("Type: " + type + " Amount: " + amount);

                if ("FRAUD".equalsIgnoreCase(type) || "SNATCHING".equalsIgnoreCase(type))
                    fraud += amount;
                else if ("SUSPENSE".equalsIgnoreCase(type))
                    suspense += amount;
                else if ("REVERSE".equalsIgnoreCase(type))
                    reverse += amount;
            }

        } catch (Exception e) {
            FfAutoEodRtn.info("Fraud read error");
        }

        fraudAmountBD = BigDecimal.valueOf(fraud).abs();
        suspenseAmountBD = BigDecimal.valueOf(suspense).abs();
        suspenseReverseAmountBD = BigDecimal.valueOf(reverse).abs();

        FfAutoEodRtn.info(
                "Fraud=" + fraudAmountBD + " Suspense=" + suspenseAmountBD + " Reverse=" + suspenseReverseAmountBD);
    }

    private double parseAmount(String value) {

        try {

            FfAutoEodRtn.info("Reading amount - Present");

            return Double.parseDouble(value);

        } catch (NumberFormatException e) {

            FfAutoEodRtn.info("Amount not present");

            return 0;
        }
    }

    private void readCollectionValues(EbFfCollPostingScreenRecord collectionRecord) {

        if (collectionRecord == null) {

            FfAutoEodRtn.info("Collection record is null");
            return;
        }

        totalCashAmountBD = new BigDecimal(collectionRecord.getTotalCashAmt().getValue()).abs();

        totalPendingCashBD = new BigDecimal(collectionRecord.getTotalPendingCash().getValue()).abs();

        FfAutoEodRtn.info("Collection values read successfully");
    }

    private void calculateClosingVault() {

        FfAutoEodRtn.info("Calculating Closing vault balance: ");

        closingVaultBalanceBD = vaultOpeningBalanceBD.add(totalCashAmountBD).subtract(bankDepositsVaultBD)
                .subtract(pettyCashLimitBD).subtract(debitAmountBD).subtract(fraudAmountBD).subtract(suspenseAmountBD)
                .add(suspenseReverseAmountBD).add(finalBCReversalAmountBD);

        FfAutoEodRtn.info("Closing vault balance calculated = " + closingVaultBalanceBD);
    }

    private String formatAmount(BigDecimal amount) {

        FfAutoEodRtn.info("Formatting Decimals");

        return amount.setScale(2, RoundingMode.HALF_UP).toString();
    }

    private SynchronousTransactionData frameTxnData() {

        FfAutoEodRtn.info("Framing transaction data...");

        SynchronousTransactionData txnData = new SynchronousTransactionData();
        txnData.setFunction("INPUT");
        txnData.setCompanyId(companyID);
        txnData.setVersionId("EB.FF.EOD.SCREEN,AUTO.INPUT");
        txnData.setNumberOfAuthoriser("0");
        txnData.setTransactionId(eodId);
        txnData.setSourceId("FF.AUTO.EOD");

        FfAutoEodRtn.info("txnData IS :" + txnData.toString());

        return txnData;

    }

    private EbFfEodScreenRecord frameEbFfEodScreenRecord(String today, BigDecimal vaultOpeningBalanceBD,
            BigDecimal totalCashAmountBD, BigDecimal totalPendingCashBD, BigDecimal bankDepositsVaultBD,
            BigDecimal pettyCashLimitBD, BigDecimal debitAmountBD, BigDecimal suspenseAmountBD,
            BigDecimal suspenseReverseAmountBD, BigDecimal fraudAmountBD, BigDecimal finalBCReversalAmountBD,
            BigDecimal closingVaultBalanceBD) {

        FfAutoEodRtn.info("Framing EOD record...");

        EbFfEodScreenRecord rec = new EbFfEodScreenRecord(this);
        try {
            rec.setEodDate(today);
            rec.setVaultOpeningBalance(formatAmount(vaultOpeningBalanceBD));
            rec.setTotalCashAmt(formatAmount(totalCashAmountBD));
            rec.setTotalPendingCash(formatAmount(totalPendingCashBD));
            rec.setBankBcDepositVault(formatAmount(bankDepositsVaultBD));
            rec.setPettyCash(formatAmount(pettyCashLimitBD));
            rec.setBranchAdminExpenses(formatAmount(debitAmountBD));
            rec.setSuspenseAmount(formatAmount(suspenseAmountBD));
            rec.setSuspenseReversalAmount(formatAmount(suspenseReverseAmountBD));
            rec.setIncidentType(formatAmount(fraudAmountBD));
            rec.setReversalOfBank(formatAmount(finalBCReversalAmountBD));
            rec.setClosingVaultBalance(formatAmount(closingVaultBalanceBD));
            rec.setAutoEod("YES");
        } catch (Exception e) {
            FfAutoEodRtn.info("Framing EOD record - Catch Block..." + e.toString());
        }

        return rec;
    }
}