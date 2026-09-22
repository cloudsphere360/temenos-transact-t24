package com.temenos.fusion;

import java.math.BigDecimal;
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
import com.temenos.t24.api.records.ebffsnatchfraudamtupd.DateOfTxnClass;
import com.temenos.t24.api.records.ebffsnatchfraudamtupd.EbFfSnatchFraudAmtUpdRecord;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/**
 *
 * @author ar116388
 *
 */
public class FfPettyLimitInpRtn extends RecordLifecycle {

    private static final FusionFileLogger FfPettyLimitInp = FusionFileLogger.getLogger(FfPettyLimitInpRtn.class);

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
    String yid = "";
    String lastWorkingDay = "";
    String pettyAmt = "";
    String bankDepImgUpld = "";
    String finMnemonic = "";

    BigDecimal vaultOpenBalBD = BigDecimal.ZERO;
    BigDecimal cashCollBD = BigDecimal.ZERO;
    BigDecimal sum = BigDecimal.ZERO;
    BigDecimal branchAdmAmtBD = BigDecimal.ZERO;
    BigDecimal difference = BigDecimal.ZERO;
    BigDecimal pettyAmtBD = BigDecimal.ZERO;

    BigDecimal bankDepImgUpldBD = BigDecimal.ZERO;
    BigDecimal totalBD = BigDecimal.ZERO;
    BigDecimal fraudAmountBD = BigDecimal.ZERO;
    BigDecimal suspenseAmountBD = BigDecimal.ZERO;
    BigDecimal suspenseReverseAmountBD = BigDecimal.ZERO;

    EbFfCollPostingScreenRecord collectionRec = null;
    EbFfEodScreenRecord eodScreenRec = null;
    EbFfBranchAdminExpDailyRecord exprec = null;
    EbFfBankDepositsVaultConcatRecord vaultDeprec = null;

    @Override
    public TValidationResponse validateRecord(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {

        FfPettyLimitInp.info("===== Validation Started =====");

        FundsTransferRecord ftrec = new FundsTransferRecord(currentRecord);

        try {
            today = session.getCurrentVariable("!TODAY");
            companyId = session.getCompanyId();
            lastWorkingDay = session.getCurrentVariable("!LAST.WORKING.DAY");

            FfPettyLimitInp.info("Today : " + today);
            FfPettyLimitInp.info("Company Id : " + companyId);

            id = companyId + "-" + today;
            yid = companyId + "-" + lastWorkingDay;

            finMnemonic = session.getCompanyRecord().getFinancialMne().getValue();

            FfPettyLimitInp.info("Constructed Record Id : " + id);
            FfPettyLimitInp.info("Constructed Record YID : " + yid);
            FfPettyLimitInp.info("finMnemonic : " + finMnemonic);
            
            //Cash received in Branch

            try {

                collectionRec = new EbFfCollPostingScreenRecord(dataAccess.getRecord("EB.FF.COLL.POSTING.SCREEN", id));
                cashColl = collectionRec.getTotalCashAmt().getValue();
                cashCollBD = new BigDecimal((cashColl == null || cashColl.isEmpty()) ? "0" : cashColl);

            } catch (Exception e) {

                FfPettyLimitInp.info(
                        "EB.FF.COLL.POSTING.SCREEN record not found for ID : " + id + ". Using Collection Amount = 0");
                cashCollBD = BigDecimal.ZERO;
            }

            FfPettyLimitInp.info("Cash Collection Amount : " + cashCollBD);
            
            //Vault Opening Balance

            try {
                eodScreenRec = new EbFfEodScreenRecord(dataAccess.getRecord("EB.FF.EOD.SCREEN", yid));
                vaultOpenBal = eodScreenRec.getClosingVaultBalance().getValue();
                vaultOpenBalBD = new BigDecimal((vaultOpenBal == null || vaultOpenBal.isEmpty()) ? "0" : vaultOpenBal);
            } catch (Exception e) {

                FfPettyLimitInp.info(
                        "EB.FF.EOD.SCREEN record not found for ID : " + yid + ". Using Vault Opening Balance = 0");
                vaultOpenBalBD = BigDecimal.ZERO;
            }

            FfPettyLimitInp.info("Vault Opening Balance : " + vaultOpenBalBD);
            
            //Branch/Admin Expenses

            try {
                exprec = new EbFfBranchAdminExpDailyRecord(dataAccess.getRecord("EB.FF.BRANCH.ADMIN.EXP.DAILY", id));
                branchAdmAmt = exprec.getCreditAmt().getValue();
                branchAdmAmtBD = new BigDecimal((branchAdmAmt == null || branchAdmAmt.isEmpty()) ? "0" : branchAdmAmt);
            } catch (Exception e) {

                FfPettyLimitInp.info("EB.FF.BRANCH.ADMIN.EXP.DAILY record not found for ID : " + id
                        + ". Using Branch Admin Expense = 0");
                branchAdmAmtBD = BigDecimal.ZERO;
            }

            FfPettyLimitInp.info("Branch Admin Expense : " + branchAdmAmtBD);
            
            //Bank/BC deposit (Vault)

            try {

                vaultDeprec = new EbFfBankDepositsVaultConcatRecord(
                        dataAccess.getRecord("EB.FF.BANK.DEPOSITS.VAULT.CONCAT", id));

                bankDepImgUpld = vaultDeprec.getBcDepositVaultConcatImg().getValue();

                bankDepImgUpldBD = new BigDecimal(
                        (bankDepImgUpld == null || bankDepImgUpld.isEmpty()) ? "0" : bankDepImgUpld);

            } catch (Exception e) {
                FfPettyLimitInp.info("EB.FF.BANK.DEPOSITS.VAULT.CONCAT record not found for ID : " + id
                        + ". Using Bank Deposit Vault = 0");
                bankDepImgUpldBD = BigDecimal.ZERO;
            }

            FfPettyLimitInp.info("bankDepImgUpldBD : " + bankDepImgUpldBD);
            
            //Reversal of Bank/BC deposits

            double total = 0;

            try {

                FfPettyLimitInp.info("Calculating BC reversal amounts");

                List<String> ids = dataAccess.selectRecords(finMnemonic, "EB.FF.FT.COLL.REV.CONCAT", "",
                        "WITH @ID LIKE ..." + id);

                for (String recId : ids) {

                    EbFfFtCollRevConcatRecord revConcatRecord = new EbFfFtCollRevConcatRecord(
                            dataAccess.getRecord("EB.FF.FT.COLL.REV.CONCAT", recId));

                    String amt = revConcatRecord.getBcPointRevCollected().getValue();

                    total += Double.parseDouble(amt);

                    FfPettyLimitInp.info("BC reversal record " + recId + " amount = " + amt);

                }

            } catch (Exception e) {
                FfPettyLimitInp.info("Catch Block of BC Reversal Amount");
            }

            totalBD = BigDecimal.valueOf(total).abs();

            FfPettyLimitInp.info("totalBD : " + totalBD);
            
            //Incidents — Snatching / Fraud : Control Account Posting : Control Account Reversal

            double fraud = 0;
            double suspense = 0;
            double reverse = 0;

            try {

                FfPettyLimitInp.info("Reading fraud / suspense records");

                EbFfSnatchFraudAmtUpdRecord fraudRecord = new EbFfSnatchFraudAmtUpdRecord(
                        dataAccess.getRecord("EB.FF.SNATCH.FRAUD.AMT.UPD", id));

                for (DateOfTxnClass txn : fraudRecord.getDateOfTxn()) {

                    String type = txn.getIncidentTyp().getValue();

                    double amount = parseAmount(txn.getFfSnaFrdAmt().getValue());

                    FfPettyLimitInp.info("Incident type = " + type + " amount = " + amount);

                    if ("FRAUD".equalsIgnoreCase(type) || "SNATCHING".equalsIgnoreCase(type)) {

                        fraud += amount;

                    } else if ("SUSPENSE".equalsIgnoreCase(type)) {

                        suspense += amount;

                    } else if ("REVERSE".equalsIgnoreCase(type)) {

                        reverse += amount;
                    }
                }

            } catch (Exception e) {
                FfPettyLimitInp.info("Catch Block of Fraud, Suspense and Reversal");
            }

            fraudAmountBD = BigDecimal.valueOf(fraud).abs();
            suspenseAmountBD = BigDecimal.valueOf(suspense).abs();
            suspenseReverseAmountBD = BigDecimal.valueOf(reverse).abs();

            FfPettyLimitInp.info("fraudAmountBD : " + fraudAmountBD);
            FfPettyLimitInp.info("suspenseAmountBD : " + suspenseAmountBD);
            FfPettyLimitInp.info("suspenseReverseAmountBD : " + suspenseReverseAmountBD);

            FfPettyLimitInp.info("Successfully fetched records");
            
            //Petty Cash Withdrawal

            pettyAmt = ftrec.getDebitAmount().getValue();
            pettyAmtBD = new BigDecimal((pettyAmt == null || pettyAmt.isEmpty()) ? "0" : pettyAmt);

            FfPettyLimitInp.info("pettyAmtBD : " + pettyAmtBD);

            // Add Vault Opening Balance + Collection Amount
            sum = vaultOpenBalBD.add(cashCollBD).subtract(bankDepImgUpldBD).subtract(branchAdmAmtBD)
                    .subtract(fraudAmountBD).subtract(suspenseAmountBD).add(suspenseReverseAmountBD).add(totalBD);

            FfPettyLimitInp.info("Final Sum Value : " + sum);

            // Validation
            if (sum.compareTo(pettyAmtBD) < 0) {

                FfPettyLimitInp.error("Validation Failed : Petty Cash Withdrawal exceed allowed amount");

                ftrec.getDebitAmount().setError(
                        "Insufficient vault balance. Closing balance is less than Petty Cash Withdrawal amount.");

            } else {

                FfPettyLimitInp.info("Validation Passed Successfully");
            }

            FfPettyLimitInp.info("===== Validation Completed =====");
        } catch (Exception e) {
            FfPettyLimitInp
                    .error("Exception occurred during validation : " + e.getClass().getName() + " : " + e.getMessage());
            e.printStackTrace();
        }
        return ftrec.getValidationResponse();
    }

    private double parseAmount(String value) {
        try {

            FfPettyLimitInp.info("Reading amount - Present");

            return Double.parseDouble(value);

        } catch (NumberFormatException e) {

            FfPettyLimitInp.info("Amount not present");

            return 0;
        }
    }

}
