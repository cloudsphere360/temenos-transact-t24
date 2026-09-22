package com.temenos.fusion;

import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.aaarrangementactivity.AaArrangementActivityRecord;
import com.temenos.t24.api.records.ebffloanactivity.EbFfLoanActivityRecord;

/*-----------------------------------------------------------------------------
 * @author Vinothini P
 * Date Created:
 * Attached as : Auth Routine
 * EB.API : NA
 * Attached to :NA
 * Description: this routine is used trigger the change schedule.
 *------------------------------------------------------------------------------ 
 * Modification History :
 *----------------------------------------------------------------------------- 
 *22-Aug-2025   Development      Initial Version
 *-----------------------------------------------------------------------------
 */
public class FfPostChgPaymtDate extends RecordLifecycle {

    private static final FusionFileLogger FfPostChgPaymtDatelog = FusionFileLogger.getLogger(FfPostChgPaymtDate.class);

    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {
        FfPostChgPaymtDatelog.info("FfPostChgPaymtDate:postUpdateRequest  routine called");
        try {
            AaArrangementActivityRecord aaaRec = new AaArrangementActivityRecord(this);
            EbFfLoanActivityRecord loanActivityObj = new EbFfLoanActivityRecord(currentRecord);
            String arrangement = loanActivityObj.getArrangementId().toString();
            String chgPaymentDate = loanActivityObj.getChgPaymentDate().getValue();
            String activityType = loanActivityObj.getActivityType().getValue();
            String nextpaymentDate = loanActivityObj.getDueDate().getValue();
            FfPostChgPaymtDatelog.info(" FfPostChgPaymtDate:postUpdateRequest activityType " + activityType);
            String effectiveDate = loanActivityObj.getLoanCreationDate().toString();

            if (activityType.equals("CHG.SCHEDULE")) {
                aaaRec.setActivity("LENDING-CHANGE-SCHD.FULL");
                FfPostChgPaymtDatelog
                        .info("FfPostChgPaymtDate:postUpdateRequest Condition triggered for an activityType "
                                + activityType + "and activity " + aaaRec.getActivity().toString());
                triggerActivity(transactionData, aaaRec, currentRecords, arrangement, chgPaymentDate, effectiveDate,
                        nextpaymentDate);
            }
            if (activityType.equals("CHG.SCHED.SINGLE")) {
                aaaRec.setActivity("LENDING-CHANGE-PAYMENT.SCHEDULE");

                FfPostChgPaymtDatelog
                        .info("FfPostChgPaymtDate:postUpdateRequest Condition triggered for an activityType "
                                + activityType + "and activity " + aaaRec.getActivity().toString());
                triggerActivity(transactionData, aaaRec, currentRecords, arrangement, chgPaymentDate, effectiveDate,
                        nextpaymentDate);
            }
        } catch (Exception e) {
            FfPostChgPaymtDatelog.error("FfPostChgPaymtDate:postUpdateRequest error:" + e.getMessage());
        }
    }

    private void triggerActivity(List<TransactionData> transactionData, AaArrangementActivityRecord aaaRec,
            List<TStructure> currentRecords, String arrangement, String chgPaymentDate, String effectiveDate,
            String nextpaymentDate) {
        FfPostChgPaymtDatelog.info("FfPostChgPaymtDate:triggerActivity  routine called");
        try {
            // updatedate-changepaymentDate
            String updChgDate = nextpaymentDate + "-" + chgPaymentDate;
            aaaRec.setArrangement(arrangement);
            if (!effectiveDate.isEmpty()) {
                aaaRec.setEffectiveDate(effectiveDate);
            } else {
                aaaRec.setEffectiveDate(chgPaymentDate);
            }
            aaaRec.setRemarks(updChgDate);

            FfPostChgPaymtDatelog.info("FfPostChgPaymtDate:triggerActivity trigger activity method called");
            TransactionData transactionDataObj = new TransactionData();
            transactionDataObj.setVersionId("AA.ARRANGEMENT.ACTIVITY,FF.PRINCIPLE.UPDATE");
            transactionDataObj.setFunction("INPUT");
            transactionDataObj.setSourceId("FF.OFS.UPD");
            transactionDataObj.setNumberOfAuthoriser("0");
            transactionDataObj.setTransactionId("");
            transactionData.add(transactionDataObj);
            currentRecords.add(aaaRec.toStructure());

            FfPostChgPaymtDatelog.info(
                    "FfPostChgPaymtDate:triggerActivity final data transactionData " + transactionData.toString());
        } catch (Exception e) {
            FfPostChgPaymtDatelog.error("FfPostChgPaymtDate:triggerActivity triggerActivity error:" + e.getMessage());
        }

    }

}
