package com.temenos.fusion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.api.TValidationResponse;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.records.dates.DatesRecord;
import com.temenos.t24.api.records.ebffbankdepositsvaultconcat.EbFfBankDepositsVaultConcatRecord;
import com.temenos.t24.api.records.ebffcollpostingscreen.EbFfCollPostingScreenRecord;
import com.temenos.t24.api.records.ebffcollpostingscreen.EmployeeIdClass;
import com.temenos.t24.api.records.ebffeodscreen.EbFfEodScreenRecord;
import com.temenos.t24.api.records.ebffftcollrevconcat.EbFfFtCollRevConcatRecord;
import com.temenos.t24.api.records.ebffftpettycashlimit.EbFfFtPettyCashLimitRecord;
import com.temenos.t24.api.records.ebffsnatchfraudamtupd.DateOfTxnClass;
import com.temenos.t24.api.records.ebffsnatchfraudamtupd.EbFfSnatchFraudAmtUpdRecord;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Date;
import com.temenos.t24.api.system.Session;

/**
 *
 * @author jh116454
 * @Attached To: VERSION > FUNDS.TRANSFER,FF.SNATCHING.FRAUD.COLL
 * @Attached As: INPUT ROUTINE > EB.API > FF.SNATCH.FRAUD.RTN
 * @Description: while selecting the Credit Account
 * 
 *               1)if Cash in transit Account - Compare the version EMPID/NAME
 *               with the EB.FF.COLL.POSTING.SCREEN. If the entered amount
 *               exceeds the Total Pending Collections, the system shall prevent
 *               the transaction and display the error message.
 * 
 *               2) If Branch Cash Account - Vault Opening Balance + Cash
 *               received in Branch - Bank/BC deposit (Vault) - Petty Cash
 *               Withdrawal - Branch / Admin Expenses - Incidents — Snatching /
 *               Fraud - Control Account Posting + Control Account Reversal +
 *               Reversal of Bank/BC deposits. If the entered amount exceeds the
 *               above value, the system shall prevent the transaction and
 *               display the error message
 * 
 */
public class FfSnatchFraudRtn extends RecordLifecycle {

    private static final FusionFileLogger ySnatchFraudLog = FusionFileLogger.getLogger(FfSnatchFraudRtn.class);

    DataAccess yDataAcc = new DataAccess(this);
    Date date = new Date(this);
    
    FundsTransferRecord yFtRec = null;

    String yRoEmpID = "";
    String yCollContID = "";
    String yRoEmpName = "";
    String yEmpIDNameVer = "";

    String today = "";
    String companyId = "";
    String id = "";
    String vaultOpenBal = "";
    String cashColl = "";
    String ftCredAmount = "";
    String zid = "";
    String monthYear = "";
    String pettyCash = "";
    String yid = "";
    String lastWorkingDay = "";
    String bankDepImgUpld = "";
    String finMnemonic = "";
    String yStrTodayVal = "";
            
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

    Session ss = new Session(this);

    double fraud = 0;
    double suspense = 0;
    double reverse = 0;

    @Override
    public TValidationResponse validateRecord(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {

        yFtRec = new FundsTransferRecord(currentRecord);       

        String yAcctNo = yFtRec.getCreditAcctNo().getValue();
        ySnatchFraudLog.info(" yAcctNo -> " + yAcctNo);

        try {
            AccountRecord yAccRec = new AccountRecord(yDataAcc.getRecord("ACCOUNT", yAcctNo));

            String yCate = yAccRec.getCategory().getValue();
            ySnatchFraudLog.info(" yCate -> " + yCate);

            if (yCate.equals("10004")) {
                getCashinTransit();
            } else {
                getBranchCash();
            }

        } catch (Exception e) {
            ySnatchFraudLog.info("Acct Rec Missing -> " + e);
        }
       
        getDateofInc();
        
        currentRecord.set(yFtRec.toStructure());
        return yFtRec.getValidationResponse();
    }

    private void getDateofInc() {
        try {
            String yDateofInc = yFtRec.getLocalRefField("DATE.OF.INCIDENT").getValue();
            ySnatchFraudLog.info(" yDateofInc -> " + yDateofInc);
            
            DatesRecord dateRec = date.getDates();
             yStrTodayVal = dateRec.getToday().getValue();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

            LocalDate ySysDt = LocalDate.parse(yStrTodayVal, formatter);
            LocalDate yGivenDate = LocalDate.parse(yDateofInc, formatter);
            LocalDate yPrevMonth = ySysDt.minusMonths(1);

            ySnatchFraudLog.info("Today -> " + ySysDt);
            ySnatchFraudLog.info("Given Date -> " + yGivenDate);
            ySnatchFraudLog.info("Last Month -> " + yPrevMonth);

            if (yGivenDate.isAfter(ySysDt)) {
                ySnatchFraudLog.info("Greater than Today");
                yFtRec.getLocalRefField("DATE.OF.INCIDENT").setError("EB-SNAT.FRD.GT.TODAY");
            } else if (yGivenDate.isBefore(yPrevMonth)) {
                ySnatchFraudLog.info("Less than Previous Month");
                yFtRec.getLocalRefField("DATE.OF.INCIDENT").setError("EB-SNAT.FRD.LT.MNTH");
            }
            
        } catch (Exception e) {
            ySnatchFraudLog.info("Value Date Details missing -> " + e);
        }
    }

    private void getCashinTransit() {
        try {
           
            DatesRecord dateRec = date.getDates();
            yStrTodayVal = dateRec.getToday().getValue();

            String yCoCode = ss.getCompanyId();
            String yCollPostID = yCoCode + "-" + yStrTodayVal;
            ySnatchFraudLog.info(" Coll Scrn ID -> " + yCollPostID);

            yRoEmpID = yFtRec.getLocalRefField("FF.POSTED.BY").getValue();
            ySnatchFraudLog.info(" Ro EmpID -> " + yRoEmpID);
            yRoEmpName = yFtRec.getLocalRefField("FF.POST.LGLNAME").getValue();
            ySnatchFraudLog.info(" RO Name -> " + yRoEmpName);
            yEmpIDNameVer = yRoEmpID + "/" + yRoEmpName;
            ySnatchFraudLog.info(" yROIDnName -> " + yEmpIDNameVer);

            EbFfCollPostingScreenRecord yEbCollPostRec = new EbFfCollPostingScreenRecord(
                    yDataAcc.getRecord("EB.FF.COLL.POSTING.SCREEN", yCollPostID));

            List<EmployeeIdClass> yEmpIDList = yEbCollPostRec.getEmployeeId();

            for (int i = 0; i < yEmpIDList.size(); i++) {
                String yEmpIDname = yEmpIDList.get(i).getEmployeeId().getValue();
                ySnatchFraudLog.info(" yEmpID in Screen-> " + yEmpIDname);

                if (yEmpIDNameVer.equalsIgnoreCase(yEmpIDname)) {
                    String yRoPendCash = yEmpIDList.get(i).getRoPendingCollection().getValue();
                    ySnatchFraudLog.info(" yRoPendCash -> " + yRoPendCash);
                    if (yRoPendCash.equals("") || yRoPendCash.isEmpty() || yRoPendCash.equals("0.00")) {
                        ySnatchFraudLog.info("No Amt ");
                        yFtRec.getDebitAmount().setError("EB-SNAT.NOAMT.ERR");
                    } else {
                        ySnatchFraudLog.info("inside amt");
                        double yTotPendCashD = Double.parseDouble(yRoPendCash);
                        ySnatchFraudLog.info(" yTotPendCashD -> " + yTotPendCashD);
                        double yDebAmt = Double.parseDouble(yFtRec.getDebitAmount().getValue());
                        ySnatchFraudLog.info(" yDebAmtB -> " + yDebAmt);
                        if (yDebAmt > yTotPendCashD) {
                            ySnatchFraudLog.info("yDebAmtB > yTotPendCashB");
                            yFtRec.getDebitAmount().setError("EB-SNAT.COL.AMT.ERR");
                        }
                    }
                }
            }
        } catch (Exception e) {
            ySnatchFraudLog.info("Coll Posting Rec Missing -> " + e);
        }

    }

    private void getBranchCash() {

        today = ss.getCurrentVariable("!TODAY");
        companyId = ss.getCompanyId();

        LocalDate sessionDate = LocalDate.parse(today, DateTimeFormatter.ofPattern("yyyyMMdd"));

        monthYear = sessionDate.format(DateTimeFormatter.ofPattern("MMyyyy"));
        lastWorkingDay = ss.getCurrentVariable("!LAST.WORKING.DAY");
        finMnemonic = ss.getCompanyRecord().getFinancialMne().getValue();

        id = companyId + "-" + today;
        yid = companyId + "-" + lastWorkingDay;
        zid = companyId + "-" + monthYear;

        ySnatchFraudLog.info("Constructed Record Id : " + id);
        ySnatchFraudLog.info("Constructed Record YID : " + yid);
        ySnatchFraudLog.info("Constructed Record ZId : " + zid);

        // Vault Opening Balance
        getVaultOpnBal();

        // Cash received in Branch
        getCashRevBranch();

        // Bank/BC deposit (Vault)
        getBCBankDeposit();

        // Petty Cash Withdrawal
        getPettycashWithd();

        // Reversal of Bank/BC deposits
        getRevbankBcPoint();

        // Branch Admin Expenses
        // Vault FT Credit Amount
        getBranchAdminExp();

        // Incidents — Snatching / Fraud : Control Account Posting : Control Account
        // Reversal
        getIncSnatchExp();

        fraudAmountBD = BigDecimal.valueOf(fraud).abs();
        suspenseAmountBD = BigDecimal.valueOf(suspense).abs();
        suspenseReverseAmountBD = BigDecimal.valueOf(reverse).abs();

        ySnatchFraudLog.info("fraudAmountBD : " + fraudAmountBD);
        ySnatchFraudLog.info("suspenseAmountBD : " + suspenseAmountBD);
        ySnatchFraudLog.info("suspenseReverseAmountBD : " + suspenseReverseAmountBD);

        ySnatchFraudLog.info("Successfully fetched records");

        // Add everything
        sum = vaultOpenBalBD.add(cashCollBD).subtract(bankDepImgUpldBD).subtract(pettyCashBD).subtract(fraudAmountBD)
                .subtract(suspenseAmountBD).add(suspenseReverseAmountBD).add(totalBD);

        ySnatchFraudLog.info("Final Sum Value : " + sum);

        // Validation
        if (ftCredAmountBD.compareTo(sum) > 0) {
            ySnatchFraudLog.error("Validation Failed : Branch Admin expenses exceed allowed amount");
            yFtRec.getDebitAmount().setError("EB-SNAT.FRAUD.VAULT.ERR");

        } else {
            ySnatchFraudLog.info("Validation Passed Successfully");
        }
    }

    private void getVaultOpnBal() {

        try {
            vaultOpenBal = "";
            EbFfEodScreenRecord eodScreenRec = new EbFfEodScreenRecord(yDataAcc.getRecord("EB.FF.EOD.SCREEN", yid));
            vaultOpenBal = eodScreenRec.getClosingVaultBalance().getValue();
            vaultOpenBalBD = new BigDecimal((vaultOpenBal == null || vaultOpenBal.isEmpty()) ? "0" : vaultOpenBal);
        } catch (Exception e) {
            ySnatchFraudLog.info("EB.FF.EOD.SCREEN record missing -> " + e);
            vaultOpenBalBD = BigDecimal.ZERO;
        }
        ySnatchFraudLog.info("Vault Opening Balance -> " + vaultOpenBalBD.toString());

    }

    private void getCashRevBranch() {
        try {
            cashColl = "";
            EbFfCollPostingScreenRecord collectionRec = new EbFfCollPostingScreenRecord(
                    yDataAcc.getRecord("EB.FF.COLL.POSTING.SCREEN", id));
            cashColl = collectionRec.getTotalCashAmt().getValue();
            cashCollBD = new BigDecimal((cashColl == null || cashColl.isEmpty()) ? "0" : cashColl);
        } catch (Exception e) {
            ySnatchFraudLog.info("EB.FF.COLL.POSTING.SCREEN record Missing -> " + e);
            cashCollBD = BigDecimal.ZERO;
        }
        ySnatchFraudLog.info("Cash Collection Amount -> " + cashCollBD.toString());
    }

    private void getBCBankDeposit() {
        try {
            bankDepImgUpld = "";
            EbFfBankDepositsVaultConcatRecord vaultDeprec = new EbFfBankDepositsVaultConcatRecord(
                    yDataAcc.getRecord("EB.FF.BANK.DEPOSITS.VAULT.CONCAT", id));
            bankDepImgUpld = vaultDeprec.getBcDepositVaultConcatImg().getValue();
            bankDepImgUpldBD = new BigDecimal(
                    (bankDepImgUpld == null || bankDepImgUpld.isEmpty()) ? "0" : bankDepImgUpld);
        } catch (Exception e) {
            ySnatchFraudLog.info("EB.FF.BANK.DEPOSITS.VAULT.CONCAT Missing -> " + e);
            bankDepImgUpldBD = BigDecimal.ZERO;
        }
        ySnatchFraudLog.info("bankDepImgUpldBD : " + bankDepImgUpldBD);
    }

    private void getPettycashWithd() {
        try {
            pettyCash = "";
            EbFfFtPettyCashLimitRecord pettyCashRecord = new EbFfFtPettyCashLimitRecord(

                    yDataAcc.getRecord("EB.FF.FT.PETTY.CASH.LIMIT", zid));
            pettyCash = pettyCashRecord.getPettyCashLimit().getValue();
            pettyCashBD = new BigDecimal((pettyCash == null || pettyCash.isEmpty()) ? "0" : pettyCash);
        } catch (Exception e) {
            ySnatchFraudLog.info("EB.FF.FT.PETTY.CASH.LIMIT record Missing -> " + e);
            pettyCashBD = BigDecimal.ZERO;
        }
        ySnatchFraudLog.info("Petty Cash Expense Amount : " + pettyCashBD.toString());

    }

    private void getRevbankBcPoint() {
        double total = 0;
        try {

            ySnatchFraudLog.info("Calculating BC reversal amounts");
            List<String> ids = yDataAcc.selectRecords(finMnemonic, "EB.FF.FT.COLL.REV.CONCAT", "",
                    "WITH @ID LIKE ..." + id);
            for (String recId : ids) {

                EbFfFtCollRevConcatRecord revConcatRecord = new EbFfFtCollRevConcatRecord(
                        yDataAcc.getRecord("EB.FF.FT.COLL.REV.CONCAT", recId));

                String amt = revConcatRecord.getBcPointRevCollected().getValue();
                total += Double.parseDouble(amt);
                ySnatchFraudLog.info("BC reversal record " + recId + " amount = " + amt);
            }
        } catch (Exception e) {
            ySnatchFraudLog.info("Catch Block of BC Reversal Amount -> " + e);
        }
        totalBD = BigDecimal.valueOf(total).abs();
        ySnatchFraudLog.info("totalBD -> " + totalBD);
    }

    private void getBranchAdminExp() {
        ftCredAmount = yFtRec.getDebitAmount().getValue();
        ftCredAmountBD = new BigDecimal((ftCredAmount == null || ftCredAmount.isEmpty()) ? "0" : ftCredAmount);
        ySnatchFraudLog.info("Branch Admin Expense Amount : " + ftCredAmountBD.toString());
    }

    private void getIncSnatchExp() {

        try {
            ySnatchFraudLog.info("Reading fraud / suspense records");

            EbFfSnatchFraudAmtUpdRecord fraudRecord = new EbFfSnatchFraudAmtUpdRecord(
                    yDataAcc.getRecord("EB.FF.SNATCH.FRAUD.AMT.UPD", id));
            for (DateOfTxnClass txn : fraudRecord.getDateOfTxn()) {
                String type = txn.getIncidentTyp().getValue();
                double amount = parseAmount(txn.getFfSnaFrdAmt().getValue());
                ySnatchFraudLog.info("Incident type = " + type + " amount = " + amount);
                if ("FRAUD".equalsIgnoreCase(type) || "SNATCHING".equalsIgnoreCase(type)) {
                    fraud += amount;
                } else if ("SUSPENSE".equalsIgnoreCase(type)) {
                    suspense += amount;
                } else if ("REVERSE".equalsIgnoreCase(type)) {
                    reverse += amount;
                }
            }
        } catch (Exception e) {
            ySnatchFraudLog.info("Catch Block of Fraud, Suspense and Reversal");
        }

    }

    private double parseAmount(String value) {
        try {
            ySnatchFraudLog.info("Reading amount - Present");
            return Double.parseDouble(value);

        } catch (NumberFormatException e) {
            ySnatchFraudLog.info("Amount not present");
            return 0;
        }
    }
}
