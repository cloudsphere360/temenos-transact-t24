package com.temenos.fusion;

import java.math.BigDecimal;
import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebffbankdepositsvaultconcat.EbFfBankDepositsVaultConcatRecord;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.records.imdocumentimage.ImDocumentImageRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;
import com.temenos.t24.api.tables.ebffbankdepositsvaultconcat.EbFfBankDepositsVaultConcatTable;

public class FfBCVaultImgUpdAuthRtn extends RecordLifecycle {

    private static final FusionFileLogger FfBCVaultImgUpdAuth = FusionFileLogger
            .getLogger(FfBCVaultImgUpdAuthRtn.class);

    DataAccess da = new DataAccess(this);

    String ytoday = "";
    String id = "";
    String yCreditAmt = "";
    ImDocumentImageRecord imgRecord = null;
    FundsTransferRecord ftRecord = null;
    String imgReference = "";
    String applicationImg = "";
    String imgType = "";

    public static final String FTTABLE = "FUNDS.TRANSFER";

    EbFfBankDepositsVaultConcatRecord bankVaultDepRec = null;

    double vaultDepositedAmt = 0.0;

    String debitAccTNum = "";

    double vaultBCDepositedAmt = 0.0;
    String ymnemonic = "";

    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {

        Session sess = new Session(this);
        DataAccess dataAccess = new DataAccess(this);
        ymnemonic = sess.getCompanyRecord().getFinancialMne().getValue();
        String coCode = sess.getCompanyId();

        ytoday = sess.getCurrentVariable("!TODAY");
        id = coCode + "-" + ytoday;

        FfBCVaultImgUpdAuth.info("========== FfBCVaultImgUpdAuth Started ==========");
        FfBCVaultImgUpdAuth.info("Company : " + coCode);
        FfBCVaultImgUpdAuth.info("Today : " + ytoday);
        FfBCVaultImgUpdAuth.info("Concat Record Id : " + id);
        FfBCVaultImgUpdAuth.info("Current Record Id : " + currentRecordId);

        try {

            imgRecord = new ImDocumentImageRecord(dataAccess.getRecord("IM.DOCUMENT.IMAGE", currentRecordId));

            imgReference = imgRecord.getImageReference().getValue();
            applicationImg = imgRecord.getImageApplication().getValue();
            imgType = imgRecord.getImageType().getValue();

            FfBCVaultImgUpdAuth.info("Image Reference : " + imgReference);
            FfBCVaultImgUpdAuth.info("Image Application : " + applicationImg);
            FfBCVaultImgUpdAuth.info("Image Type : " + imgType);

            if (imgType.equals("PHOTOS")) {

                FfBCVaultImgUpdAuth.info("Valid Funds Transfer Photo.");

                try {

                    try {
                        FfBCVaultImgUpdAuth.info("inside the try block:");
                        ftRecord = new FundsTransferRecord(da.getRecord(ymnemonic, FTTABLE, "$NAU", imgReference));
                        FfBCVaultImgUpdAuth.info("inside the try block:" + ftRecord.toString());
                    } catch (Exception e) {
                        FfBCVaultImgUpdAuth.info("inside the catch block:");
                        ftRecord = new FundsTransferRecord(da.getRecord(ymnemonic, FTTABLE, "", imgReference));
                        FfBCVaultImgUpdAuth.info("inside the catch block:" + ftRecord.toString());
                    }

                    yCreditAmt = ftRecord.getCreditAmount().getValue();

                    debitAccTNum = ftRecord.getDebitAcctNo().getValue();
                    FfBCVaultImgUpdAuth.info("Credit Amount : " + yCreditAmt);

                    try {

                        bankVaultDepRec = new EbFfBankDepositsVaultConcatRecord(
                                dataAccess.getRecord("EB.FF.BANK.DEPOSITS.VAULT.CONCAT", id));

                        String existingAmount = bankVaultDepRec.getBcDepositVaultConcatImg().getValue();

                        FfBCVaultImgUpdAuth.info("Existing Amount : " + existingAmount);

                        if (existingAmount == null || existingAmount.trim().isEmpty()) {

                            bankVaultDepRec.setBcDepositVaultConcatImg(yCreditAmt);

                            FfBCVaultImgUpdAuth.info("Field Empty. Amount set to : " + yCreditAmt);

                        } else {

                            BigDecimal existingBD = new BigDecimal(existingAmount);

                            BigDecimal currentBD = new BigDecimal(yCreditAmt);

                            BigDecimal total = existingBD.add(currentBD);

                            bankVaultDepRec.setBcDepositVaultConcatImg(total.toString());

                            FfBCVaultImgUpdAuth.info("Updated Amount : " + total);

                        }

                    } catch (Exception e) {

                        FfBCVaultImgUpdAuth.info("Concat Record not found. Creating new.");

                        bankVaultDepRec = new EbFfBankDepositsVaultConcatRecord(this);

                        bankVaultDepRec.setBcDepositVaultConcatImg(yCreditAmt);

                        vaultDepositedAmt = 0.0;
                        vaultBCDepositedAmt = 0.0;

                        FfBCVaultImgUpdAuth.info("New Record Amount : " + yCreditAmt);

                    }

                    updateTheVaultTxnDeatilsInConcatTable(bankVaultDepRec, debitAccTNum);

                } catch (Exception e) {

                    FfBCVaultImgUpdAuth.info("Error reading Funds Transfer : " + e);

                }

            } else {

                FfBCVaultImgUpdAuth.info("Image is not a Funds Transfer Photo. Skipping.");

            }

        } catch (Exception e) {

            FfBCVaultImgUpdAuth.info("Error reading IM.DOCUMENT.IMAGE : " + e);

        }

        try {

            if (bankVaultDepRec != null) {

                EbFfBankDepositsVaultConcatTable bankDepVault = new EbFfBankDepositsVaultConcatTable(this);

                bankDepVault.write(id, bankVaultDepRec);

                FfBCVaultImgUpdAuth.info("Record written successfully.");

                FfBCVaultImgUpdAuth.info("Written Amount : " + bankVaultDepRec.getBcDepositVaultConcatImg().getValue());

            } else {

                FfBCVaultImgUpdAuth.info("Nothing to write. Record is null.");

            }

        } catch (Exception e) {

            FfBCVaultImgUpdAuth.info("Error writing record : " + e);

        }

        FfBCVaultImgUpdAuth.info("========== Routine Completed ==========");

    }

    /**
     * @param bankVaultDepRec2
     * @param debitAccTNum2
     * 
     */
    private void updateTheVaultTxnDeatilsInConcatTable(EbFfBankDepositsVaultConcatRecord bankVaultDepRec,
            String debitAccTNum) {
        FfBCVaultImgUpdAuth.info("updateTheVaultTxnDeatilsInConcatTable is triggered: " + debitAccTNum);
        try {
            bankVaultDepRec.setVaultFtTxnId(imgReference);
            if (debitAccTNum.contains("INR10440")) {
                FfBCVaultImgUpdAuth.info("INR10440... Acc value updated");
                bankVaultDepRec.setCreditAcNo(debitAccTNum);

                String crtAmt = bankVaultDepRec.getCreditAmt().getValue();
                FfBCVaultImgUpdAuth.info("crtAmt and ftcreditAmt is:" + crtAmt + "**" + yCreditAmt);
                if (!crtAmt.isEmpty()) { // Vault Deposit — Bank 10-440
                    vaultDepositedAmt = Double.parseDouble(crtAmt) + Double.parseDouble(yCreditAmt);
                }

                if (vaultDepositedAmt > 0) { // bank 10440
                    FfBCVaultImgUpdAuth.info("table have a vault amt :" + vaultDepositedAmt);
                    bankVaultDepRec.setCreditAmt(String.valueOf(vaultDepositedAmt));
                } else {
                    FfBCVaultImgUpdAuth.info("table have a vault amt is less then 0:" + vaultDepositedAmt);
                    bankVaultDepRec.setCreditAmt(yCreditAmt);
                }
            } else if (debitAccTNum.contains("INR12120")) { // BC 12120
                FfBCVaultImgUpdAuth.info("INR12120... Acc value updated");
                bankVaultDepRec.setVaultDebitAcctNo(debitAccTNum);

                String debitAmt = bankVaultDepRec.getDebitAmt().getValue();
                FfBCVaultImgUpdAuth.info("debitAmt and ftcreditAmt is:" + debitAmt + "**" + yCreditAmt);

                if (!debitAmt.isEmpty()) { // Vault Deposit — BC 12-120
                    vaultBCDepositedAmt = Double.parseDouble(debitAmt) + Double.parseDouble(yCreditAmt);
                }

                if (vaultBCDepositedAmt > 0) {
                    FfBCVaultImgUpdAuth.info("table have a vault amt :" + vaultBCDepositedAmt);
                    bankVaultDepRec.setDebitAmt(String.valueOf(vaultBCDepositedAmt));
                } else {
                    FfBCVaultImgUpdAuth.info("table have a vault amt is less then 0:" + vaultDepositedAmt);
                    bankVaultDepRec.setDebitAmt(yCreditAmt);
                }
            }

            FfBCVaultImgUpdAuth.info("Updated Vault concat Record : " + bankVaultDepRec.toString());
        } catch (Exception e) {
            FfBCVaultImgUpdAuth.error("updateTheVaultTxnDeatilsInConcatTable error: " + bankVaultDepRec.toString());
        }

    }
}