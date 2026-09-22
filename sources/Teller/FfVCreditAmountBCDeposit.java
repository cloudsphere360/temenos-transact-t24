package com.temenos.fusion;

import java.math.BigDecimal;

import com.temenos.api.TStructure;
import com.temenos.api.TValidationResponse;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebffbranchadminexpdaily.EbFfBranchAdminExpDailyRecord;
import com.temenos.t24.api.records.ebffrocollconcat.EbFfRoCollConcatRecord;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/**
 *
 * @author hs115664
 *
 */
public class FfVCreditAmountBCDeposit extends RecordLifecycle {

    private static final FusionFileLogger creditAmountBCDeposit = FusionFileLogger
            .getLogger(FfVCreditAmountBCDeposit.class);
    DataAccess da = new DataAccess(this);
    Session ss = new Session(this);
    BigDecimal depositedAmt = BigDecimal.ZERO;
    BigDecimal cashColl = BigDecimal.ZERO;
    BigDecimal bcDepositedAmt = BigDecimal.ZERO;
    BigDecimal remainingCashCollAmt = BigDecimal.ZERO;

    @Override
    public TValidationResponse validateRecord(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
        creditAmountBCDeposit.info("FfVCreditAmountBCDeposit is triggered " + currentRecordId);
        FundsTransferRecord ftRec = new FundsTransferRecord(currentRecord);
        String postBy = ftRec.getLocalRefField("FF.POSTED.BY").getValue();
        creditAmountBCDeposit.info("postBy" + postBy);
        String processDate = ftRec.getProcessingDate().getValue();
        creditAmountBCDeposit.info("processDate" + processDate);
        depositedAmt = safeBigDecimal(ftRec.getCreditAmount().getValue());
        creditAmountBCDeposit.info("depositedAmt" + depositedAmt);

        String roCollConcat = postBy + "-" + ss.getCompanyId() + "-" + processDate;
        creditAmountBCDeposit.info("roCollConcat" + roCollConcat);

        checkTheBCDepositedAmountLimt(roCollConcat, ftRec);

        return ftRec.getValidationResponse();
    }

    /**
     * @param depositedAmt2
     * @param roCollConcat
     * @param ftRec
     * @param transactionContext
     */
    private void checkTheBCDepositedAmountLimt(String roCollConcat, FundsTransferRecord ftRec) {
        creditAmountBCDeposit.info("checkTheBCDepositedAmountLimt is triggered");
        try {
            EbFfRoCollConcatRecord roCollConcatRec = new EbFfRoCollConcatRecord(
                    da.getRecord("EB.FF.RO.COLL.CONCAT", roCollConcat));

            cashColl = safeBigDecimal(roCollConcatRec.getCashCollected().getValue());
            creditAmountBCDeposit.info("cashColl" + cashColl);
            if (cashColl.compareTo(BigDecimal.ZERO) > 0) {
                calculatingTheDepositedAmount(roCollConcatRec, roCollConcat, ftRec);
            }
        } catch (Exception e) {
            creditAmountBCDeposit.error("checkTheBCDepositedAmountLimt error");
        }

    }

    /**
     * @param roCollConcatRec
     * @param roCollConcat
     * @param ftRec
     * 
     */
    private void calculatingTheDepositedAmount(EbFfRoCollConcatRecord roCollConcatRec, String roCollConcat,
            FundsTransferRecord ftRec) {

        bcDepositedAmt = safeBigDecimal(roCollConcatRec.getBcPointCollected().getValue());
        creditAmountBCDeposit.info("cashColl is greater than 0 bcDepositedAmt:" + bcDepositedAmt);
        if (bcDepositedAmt.compareTo(BigDecimal.ZERO) > 0) {
            remainingCashCollAmt = cashColl.subtract(bcDepositedAmt);// 1000-200=800
            creditAmountBCDeposit.info("bcDepositedAmt is greater than 0 remainingCashCollAmt:" + remainingCashCollAmt);
        }

        try {
            EbFfBranchAdminExpDailyRecord branchAdminRec = new EbFfBranchAdminExpDailyRecord(
                    da.getRecord("", "EB.FF.BRANCH.ADMIN.EXP.DAILY", "", roCollConcat));
            BigDecimal vaultDeposit = safeBigDecimal(branchAdminRec.getBankBcDepositVault().getValue());
            if ((bcDepositedAmt.compareTo(BigDecimal.ZERO) > 0)
                    && (remainingCashCollAmt.compareTo(BigDecimal.ZERO) > 0)) {
                remainingCashCollAmt = cashColl.subtract(vaultDeposit);
            } else {
                remainingCashCollAmt = cashColl;
            }
        } catch (Exception e) {
            remainingCashCollAmt = cashColl;
        }

        if (remainingCashCollAmt.compareTo(BigDecimal.ZERO) > 0) {
            creditAmountBCDeposit.info("remainingCashCollAmt is greater than 0");
            if (depositedAmt.compareTo(remainingCashCollAmt) > 0) {
                creditAmountBCDeposit
                        .info("depositedAmt is greater than remainingCashCollAmt depositedAmt:" + depositedAmt);
                ftRec.getCreditAmount()
                        .setError("the given deposited amount should not be greater than " + remainingCashCollAmt);
            }
        } else {
            creditAmountBCDeposit.info("remainingCashCollAmt is less than 0");
            ftRec.getCreditAmount()
                    .setError("The BC/Bank deposit has already been completed for today's cash collection.");
        }

    }

    /**
     * @param value
     * @return
     */
    private BigDecimal safeBigDecimal(String value) {
        try {
            if (value != null && !value.trim().isEmpty()) {
                return new BigDecimal(value.trim());
            }
        } catch (NumberFormatException e) {
            creditAmountBCDeposit.info("Error in safeBigDecimal");
        }
        return BigDecimal.ZERO;
    }

}
