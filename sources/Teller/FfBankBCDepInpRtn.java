package com.temenos.fusion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.temenos.api.TStructure;
import com.temenos.api.TValidationResponse;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebffcollpostingscreen.EbFfCollPostingScreenRecord;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Date;
import com.temenos.t24.api.system.Session;

/**
 *
 * @author ar116388
 *
 */
public class FfBankBCDepInpRtn extends RecordLifecycle {

    private static final FusionFileLogger FfBankBCDepInp = FusionFileLogger.getLogger(FfBankBCDepInpRtn.class);

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    Session ss = new Session(this);
    DataAccess da = new DataAccess(this);

    @Override
    public TValidationResponse validateRecord(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {

        FfBankBCDepInp.info("Entered validateRecord() for Record Id : " + currentRecordId + "**"
                + transactionContext.getCurrentVersionId());

        FundsTransferRecord ftRecord = new FundsTransferRecord(currentRecord);

        String valueDate = ftRecord.getDebitValueDate().getValue();
        FfBankBCDepInp.info("DEBIT.VALUE.DATE : " + valueDate);

        if (valueDate != null && !valueDate.isEmpty()) {

            try {

                Date dates = new Date(this);
                String todayDate = dates.getDates().getToday().getValue();

                FfBankBCDepInp.info("T24 BUSINESS Date : " + todayDate);

                LocalDate depositDate = LocalDate.parse(valueDate, FORMATTER);
                LocalDate today = LocalDate.parse(todayDate, FORMATTER);
                LocalDate minDate = today.minusDays(30);

                FfBankBCDepInp.info("Deposit Date : " + depositDate);
                FfBankBCDepInp.info("Minimum Allowed Date : " + minDate);

                if (depositDate.isAfter(today)) {

                    FfBankBCDepInp.warn("Validation failed. Future date selected : " + depositDate);

                    ftRecord.getCreditValueDate().setError("Date of Deposit cannot be a future date.");

                } else if (depositDate.isBefore(minDate)) {

                    FfBankBCDepInp.warn("Validation failed. Date older than 30 days : " + depositDate);

                    ftRecord.getCreditValueDate().setError("Date of Deposit cannot be older than 30 days.");

                } else {

                    FfBankBCDepInp.info("Date validation successful.");
                }

            } catch (Exception e) {

                FfBankBCDepInp.info("Value date missing");
            }

        } else {

            FfBankBCDepInp.info("DEBIT.VALUE.DATE is blank.");
        }
        String currVersionId = transactionContext.getCurrentVersionId();
        FfBankBCDepInp.info("currVersionId:" + currVersionId);

        if (currVersionId.contains("BC.DEPOSITS.VAULT")) {
            validationForCollectionEodRecord(ftRecord);
            FfBankBCDepInp.info("Exiting validateRecord().");
        }

        return ftRecord.getValidationResponse();
    }

    /**
     * @param ftRecord
     * 
     */
    private void validationForCollectionEodRecord(FundsTransferRecord ftRecord) {

        FfBankBCDepInp.info("validationForCollectionEodRecord is triggered");
        String currCompany = ss.getCompanyId();
        String todayDate = ss.getCurrentVariable("!TODAY");
        BigDecimal totCashAmt = BigDecimal.ZERO;

        String recId = currCompany + "-" + todayDate;

        FfBankBCDepInp.info("recId is : " + recId);
        try {
            EbFfCollPostingScreenRecord collRec = new EbFfCollPostingScreenRecord(
                    da.getRecord("EB.FF.COLL.POSTING.SCREEN", recId));

            String totalCashAmt = collRec.getTotalCashAmt().getValue();
            totCashAmt = new BigDecimal(totalCashAmt);
            FfBankBCDepInp.info("totalCashAmt is : " + totalCashAmt);

            if (totCashAmt.compareTo(BigDecimal.ZERO) <= 0) {
                FfBankBCDepInp.info("totalCashAmt is GT 0: " + totalCashAmt);

                ftRecord.getCreditAmount().setError("EB-COLL.CASH.REV.BRANCH");
            }

        } catch (Exception e) {
            ftRecord.getCreditAmount().setError("EB-TELLER.COLL.MIS.ERROR");
            FfBankBCDepInp.info("validationForCollectionEodRecord error : " + e.getMessage());
        }

    }

}