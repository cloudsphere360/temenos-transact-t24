package com.temenos.fusion;

import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.t24.api.arrangement.accounting.Contract;

import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.aaprddesinterest.AaPrdDesInterestRecord;
import com.temenos.t24.api.records.aaprddespaymentschedule.AaPrdDesPaymentScheduleRecord;
import com.temenos.t24.api.records.aaprddespaymentschedule.PaymentTypeClass;
import com.temenos.t24.api.records.aaprddespaymentschedule.PercentageClass;
import com.temenos.t24.api.records.company.CompanyRecord;
import com.temenos.t24.api.records.ebffloanactivity.EbFfLoanActivityRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;
import com.temenos.t24.api.records.aaarrangementactivity.AaArrangementActivityRecord;
import com.temenos.t24.api.records.aaarrangementactivity.FieldNameClass;
import com.temenos.t24.api.records.aaarrangementactivity.PropertyClass;

/*-----------------------------------------------------------------------------
 * @author Vinothini P
 * Date Created:
 * Attached as : Auth Routine
 * EB.API : NA
 * Attached to :NA
 * Description: this routine is used change the interest for the given period.
 *------------------------------------------------------------------------------ 
 * Modification History :
 *----------------------------------------------------------------------------- 
 *22-Aug-2025   Development      Initial Version
 *-----------------------------------------------------------------------------
 */
public class FfLoanInterestAdjust extends RecordLifecycle {
    private static final FusionFileLogger FfLoanInterestAdjustLog = FusionFileLogger.getLogger(FfLoanInterestAdjust.class);
    private static final String PRINTEREST = "PRINTEREST";
    private static final String PAYMENTSCHEDULE = "PAYMENT.SCHEDULE";
    String fixedRate;

    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {
        DataAccess dataAccess = new DataAccess(this);
        Contract contractObj = new Contract(this);
        Session sessionObj = new Session(this);
        FfLoanInterestAdjustLog.info(":postUpdateRequest triggered");
        try {
            EbFfLoanActivityRecord ebFfLoanActivity = new EbFfLoanActivityRecord(currentRecord);

            String arrangmentId = ebFfLoanActivity.getArrangementId().getValue();
            String startDate = ebFfLoanActivity.getLoanCreationDate().getValue();
            FfLoanInterestAdjustLog.info(":postUpdateRequest startDate:" + startDate);
            String intStartDate = ebFfLoanActivity.getIntStartDate().getValue();
            FfLoanInterestAdjustLog.info(":postUpdateRequest intStartDate:" + intStartDate);

            CompanyRecord companyObj = new CompanyRecord(dataAccess.getRecord("COMPANY", sessionObj.getCompanyId()));
            String finMnemonic = companyObj.getFinancialMne().getValue();

            contractObj.setContractId(arrangmentId);

            if (!startDate.equals(intStartDate)) {
                getFixedRate(contractObj, dataAccess, finMnemonic, startDate);

                String actualAmt = getActualAmt(contractObj);

                AaArrangementActivityRecord aaaRec = new AaArrangementActivityRecord(this);
                aaaRec.setActivity("LENDING-UPDATE-ACCOUNT");
                aaaRec.setArrangement(arrangmentId);
                aaaRec.setEffectiveDate(startDate);

                PropertyClass propertyRec = new PropertyClass();
                propertyRec.setProperty("ACCOUNT");

                FieldNameClass fieldNameRec = new FieldNameClass();
                fieldNameRec.setFieldName("FF.LN.CRD.DATE:1:1");
                fieldNameRec.setFieldValue(intStartDate);

                propertyRec.addFieldName(fieldNameRec);
                aaaRec.addProperty(propertyRec);

                triggerActivity(transactionData, aaaRec, currentRecords);

                AaArrangementActivityRecord zeroIntRec = new AaArrangementActivityRecord(this);
                zeroIntRec.setActivity("LENDING-CHANGE-PRINTEREST");
                zeroIntRec.setArrangement(arrangmentId);
                zeroIntRec.setEffectiveDate(startDate);// back date
                // setting Zero to fixed rate
                PropertyClass zeroInterestProp = new PropertyClass();
                zeroInterestProp.setProperty(PRINTEREST);
                zeroInterestProp.setEffective(startDate);
                FieldNameClass zeroRate = new FieldNameClass();
                zeroRate.setFieldName("FIXED.RATE:1:1");
                zeroRate.setFieldValue("0.00");

                PropertyClass paymentSchedulePropZero = new PropertyClass();
                paymentSchedulePropZero.setProperty(PAYMENTSCHEDULE);
                paymentSchedulePropZero.setEffective(startDate);
                // ACTUAL.AMT:2:1
                FieldNameClass actualAmtClsZero = new FieldNameClass();
                actualAmtClsZero.setFieldName("ACTUAL.AMT:2:1");
                actualAmtClsZero.setFieldValue(actualAmt);

                PropertyClass effInterestProp = new PropertyClass();
                effInterestProp.setProperty(PRINTEREST);
                effInterestProp.setEffective(intStartDate);

                FieldNameClass restoreRate = new FieldNameClass();
                restoreRate.setFieldName("FIXED.RATE:1:1");
                restoreRate.setFieldValue(fixedRate);

                PropertyClass paymentScheduleProp = new PropertyClass();
                paymentScheduleProp.setProperty(PAYMENTSCHEDULE);
                paymentScheduleProp.setEffective(intStartDate);

                // ACTUAL.AMT:2:1
                FieldNameClass actualAmtCls = new FieldNameClass();
                actualAmtCls.setFieldName("ACTUAL.AMT:2:1");
                actualAmtCls.setFieldValue(actualAmt);

                paymentSchedulePropZero.addFieldName(actualAmtClsZero);
                paymentScheduleProp.addFieldName(actualAmtCls);
                zeroInterestProp.addFieldName(zeroRate);
                effInterestProp.addFieldName(restoreRate);
                zeroIntRec.addProperty(zeroInterestProp);
                zeroIntRec.addProperty(effInterestProp);
                zeroIntRec.addProperty(paymentScheduleProp);
                zeroIntRec.addProperty(paymentSchedulePropZero);

                FfLoanInterestAdjustLog.info(":postUpdateRequest zeroRate " + zeroRate.toString());
                triggerActivity(transactionData, zeroIntRec, currentRecords);

            }
        } catch (Exception e) {
            FfLoanInterestAdjustLog.error(":postUpdateRequest error:" + e.getMessage());

        }

    }

    private void getFixedRate(Contract contractObj, DataAccess dataAccess, String finMnemonic, String startDate) {
        FfLoanInterestAdjustLog.info(":getFixedRate triggered");
        try {
            AaPrdDesInterestRecord aaPrdDesInterestObject = new AaPrdDesInterestRecord(
                    contractObj.getConditionForProperty(PRINTEREST));
            FfLoanInterestAdjustLog.info(":getFixedRate aaPrdDesInterestObject:" + aaPrdDesInterestObject.toString());
            String arrIntId = aaPrdDesInterestObject.getIdComp1().getValue() + "-"
                    + aaPrdDesInterestObject.getIdComp2().getValue() + "-" + startDate + ".1";

            FfLoanInterestAdjustLog.info(":getFixedRate arrIntId:" + arrIntId);
            AaPrdDesInterestRecord aaArrInterest = new AaPrdDesInterestRecord(
                    dataAccess.getRecord(finMnemonic, "AA.ARR.INTEREST", "", arrIntId));

            fixedRate = aaArrInterest.getFixedRate(0).getFixedRate().toString();
            FfLoanInterestAdjustLog.info(":getFixedRate fixedRate:" + fixedRate);
        } catch (Exception e) {
            FfLoanInterestAdjustLog.error(":getFixedRate getFixedRate:" + e.getMessage());

        }
    }

    private String getActualAmt(Contract contractObj) {
        String actualAmt = null;
        FfLoanInterestAdjustLog.info(":getActualAmt triggered");
        try {
            AaPrdDesPaymentScheduleRecord aaPrdDesPaymentScheduleObj = new AaPrdDesPaymentScheduleRecord(
                    contractObj.getSimulationConditionForProperty(PAYMENTSCHEDULE));
            FfLoanInterestAdjustLog
                    .info(":getActualAmt aaPrdDesPaymentScheduleObj " + aaPrdDesPaymentScheduleObj.toString());
            List<PaymentTypeClass> paymentTypeList = aaPrdDesPaymentScheduleObj.getPaymentType();
            FfLoanInterestAdjustLog.info(":getActualAmt paymentTypeList " + paymentTypeList.toString());
            for (PaymentTypeClass paymentType : paymentTypeList) {
                if ("DUE".equalsIgnoreCase(paymentType.getPaymentMethod().getValue())
                        && ("CONSTANT".equalsIgnoreCase(paymentType.getPaymentType().getValue()))) {
                    FfLoanInterestAdjustLog
                            .info(":getActualAmt paymentmethod " + paymentType.getPaymentMethod().getValue());
                    List<PercentageClass> percentageList = paymentType.getPercentage();
                    for (PercentageClass percentageVal : percentageList) {
                        actualAmt = percentageVal.getActualAmt().getValue();

                    }

                }
            }
        } catch (Exception e) {
            FfLoanInterestAdjustLog.error(":getActualAmt error:" + e.getMessage());
        }
        return actualAmt;
    }

    private void triggerActivity(List<TransactionData> transactionData, AaArrangementActivityRecord aaaRec,
            List<TStructure> currentRecords) {
        try {
            FfLoanInterestAdjustLog.info(":triggerActivity triggered");

            TransactionData transactionDataObj = new TransactionData();
            transactionDataObj.setVersionId("AA.ARRANGEMENT.ACTIVITY,FF.PRINCIPLE.UPDATE");
            transactionDataObj.setFunction("INPUT");
            transactionDataObj.setSourceId("FF.OFS.UPD");
            transactionDataObj.setNumberOfAuthoriser("0");
            transactionDataObj.setTransactionId("");
            transactionData.add(transactionDataObj);
            currentRecords.add(aaaRec.toStructure());

            FfLoanInterestAdjustLog.info(":triggerActivity final data transactionData " + transactionData.toString());
        } catch (Exception e) {
            FfLoanInterestAdjustLog.error(":triggerActivity error:" + e.getMessage());
        }

    }

}
