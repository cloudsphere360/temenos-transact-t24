package com.temenos.fusion;

import java.math.BigDecimal;
import java.util.List;

import com.temenos.api.LocalRefList;
import com.temenos.api.TStructure;
import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.aa.activityhook.ArrangementContext;
import com.temenos.t24.api.hook.arrangement.ActivityLifecycle;
import com.temenos.t24.api.records.aaaccountdetails.AaAccountDetailsRecord;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaarrangementactivity.AaArrangementActivityRecord;
import com.temenos.t24.api.records.aaprddesaccount.AaPrdDesAccountRecord;
import com.temenos.t24.api.records.aaprddespaymentschedule.AaPrdDesPaymentScheduleRecord;
import com.temenos.t24.api.records.aaprddespaymentschedule.PaymentTypeClass;
import com.temenos.t24.api.records.aaprddespaymentschedule.PercentageClass;
import com.temenos.t24.api.records.aaproductcatalog.AaProductCatalogRecord;
import com.temenos.t24.api.records.aasimulationrunner.AaSimulationRunnerRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

public class FfNetoffPrevalRout extends ActivityLifecycle {
    private static final FusionFileLogger FfNetoffPrevalRout = FusionFileLogger.getLogger(FfNetoffPrevalRout.class);

    DataAccess da = new DataAccess(this);
    Session ses = new Session(this);

    Contract contracRec = new Contract(this);
    FfGetPayOutAmt getPayObj = new FfGetPayOutAmt();

    @Override
    public void defaultFieldValues(AaAccountDetailsRecord accountDetailRecord,
            AaArrangementActivityRecord arrangementActivityRecord, ArrangementContext arrangementContext,
            AaArrangementRecord arrangementRecord, AaArrangementActivityRecord masterActivityRecord,
            TStructure productPropertyRecord, AaProductCatalogRecord productRecord, TStructure currecord) {

        String actStatus = arrangementContext.getActivityStatus();
        String propId = arrangementContext.getPropertyId();
        FfNetoffPrevalRout.info("PropertyId: " + propId);
        FfNetoffPrevalRout.info("Activity Status:" + actStatus);
        String yArrid = arrangementContext.getArrangementId();
        FfNetoffPrevalRout.info("Arrangement Id:" + yArrid);
        contracRec.setContractId(yArrid);
        FfNetoffPrevalRout.info("accountDetailRecord :" + accountDetailRecord.toString());
        FfNetoffPrevalRout.info("arrangementActivityRecord :" + arrangementActivityRecord.toString());
        FfNetoffPrevalRout.info("arrangementContext :" + arrangementContext.toString());
        FfNetoffPrevalRout.info("arrangementRecord :" + arrangementRecord.toString());
        FfNetoffPrevalRout.info("masterActivityRecord :" + masterActivityRecord.toString());

        String executeFlag = "";
        String simRunnerRef = "";
        String clsLoan = "";
        String repayLoan = "";
        AaPrdDesAccountRecord accRec = null;

        try {

            simRunnerRef = arrangementActivityRecord.getSimRunRef().toString();
            FfNetoffPrevalRout.info("simRunnerRef :" + arrangementActivityRecord.getSimRunRef().toString());
            Boolean flag1 = false;
            flag1 = matchesFirstFive("AAACT", simRunnerRef.substring(0, 5));
            FfNetoffPrevalRout.info("flag" + flag1);

            String mne = ses.getCompanyRecord().getFinancialMne().toString();

            AaSimulationRunnerRecord asr = new AaSimulationRunnerRecord(
                    da.getRecord(mne, "AA.SIMULATION.RUNNER", "", simRunnerRef));
            executeFlag = asr.getExecuteSimulation().getValue();
            FfNetoffPrevalRout.info("executeFlag" + executeFlag);

            try {
                accRec = new AaPrdDesAccountRecord(contracRec.getConditionForProperty("ACCOUNT"));
                FfNetoffPrevalRout.info("accRec " + accRec.toString());
            } catch (Exception e) {
                e.getMessage();
            }

            /*
             * clsLoan =
             * accRec.getLocalRefGroups("FF.CLS.LOAN").get(0).getLocalRefField("FF.CLS.LOAN"
             * ).getValue(); FfNetoffPrevalRout.info("clsLoan " + clsLoan); repayLoan =
             * accRec.getLocalRefGroups("FF.REPAY.LOAN").get(0).getLocalRefField(
             * "FF.REPAY.LOAN").getValue(); FfNetoffPrevalRout.info("repayLoan " +
             * repayLoan);
             */

            LocalRefList repayGroups = accRec.getLocalRefGroups("FF.REPAY.LOAN");
            FfNetoffPrevalRout.info("repayLoan List " + repayGroups.toString());
            if (repayGroups != null) {
                for (int i = 0; i < repayGroups.size(); i++) {

                    String val = accRec.getLocalRefGroups("FF.REPAY.LOAN").get(i).getLocalRefField("FF.REPAY.LOAN")
                            .getValue();

                    if (val != null && !val.trim().isEmpty()) {
                        repayLoan = val;
                        break; // important
                    }
                }
            }
            FfNetoffPrevalRout.info("repayLoan " + repayLoan);
            LocalRefList clsGroups = accRec.getLocalRefGroups("FF.CLS.LOAN");
            FfNetoffPrevalRout.info("closeLoan List " + clsGroups.toString());
            if (clsGroups != null) {
                for (int i = 0; i < clsGroups.size(); i++) {

                    String val = accRec.getLocalRefGroups("FF.CLS.LOAN").get(i).getLocalRefField("FF.CLS.LOAN")
                            .getValue();

                    if (val != null && !val.trim().isEmpty()) {
                        clsLoan = val;
                        break;
                    }
                }
            }

            FfNetoffPrevalRout.info("clsLoan " + clsLoan);

        } catch (Exception e) {
            e.getMessage();
        }

        if (actStatus.equals("UNAUTH") && "PAYMENT.SCHEDULE".equals(propId)
                && ("YES".equalsIgnoreCase(executeFlag) || simRunnerRef == null || simRunnerRef.isEmpty())
                && ((clsLoan != null && !clsLoan.isEmpty()) || (repayLoan != null && !repayLoan.isEmpty()))) {
            FfNetoffPrevalRout.info("Inside if ph");
            try {

                AaPrdDesPaymentScheduleRecord schedRec = new AaPrdDesPaymentScheduleRecord(currecord);
                FfNetoffPrevalRout.info("schedRec: " + schedRec);

                FfNetoffPrevalRout.info("Inside schedule record method");

                process(schedRec);

                currecord.set(schedRec.toStructure());

            } catch (Exception e) {
                e.getMessage();
            }
        }

    }

    /**
     * @param schedRec,
     * 
     */
    private void process(AaPrdDesPaymentScheduleRecord schedRec) {
        String payoutAmtSched = "";
        List<PaymentTypeClass> paymentTypeList = schedRec.getPaymentType();
        payoutAmtSched = getPayObj.getpayOutAmt(contracRec);
        String termAmt = contracRec.getTermAmount().toString();
        if (!payoutAmtSched.equals("0.00") && !termAmt.equals(payoutAmtSched)
                && new BigDecimal(payoutAmtSched.replace(",", "")).compareTo(BigDecimal.ZERO) > 0) {
            FfNetoffPrevalRout.info("The PayoutAmt from the schedule: " + payoutAmtSched);
            for (int i = 0; i < paymentTypeList.size(); i++) {
                FfNetoffPrevalRout.info("Inside schedule record loop");
                PaymentTypeClass payTypeObj = paymentTypeList.get(i);
                String paymentType = payTypeObj.getPaymentType().toString();
                FfNetoffPrevalRout.info("paymentType: " + paymentType);
                List<PercentageClass> percentList = payTypeObj.getPercentage();
                PercentageClass actAmtrec = percentList.get(0);
                FfNetoffPrevalRout.info("actAmtrec: " + actAmtrec);
                if (paymentType.equals("DISBURSEMENT.%")) {
                    FfNetoffPrevalRout.info("Inside if of Schedule");
                    payTypeObj.setPaymentType("DISBURSEMENT.AMT");
                    actAmtrec.setPercentage("");
                    actAmtrec.setActualAmt(payoutAmtSched);
                    FfNetoffPrevalRout.info("The Actual Amount field value: " + actAmtrec.getActualAmt().toString());
                    FfNetoffPrevalRout.info("The Payment Type: " + payTypeObj.getPaymentType().toString());
                    FfNetoffPrevalRout.info("The Percentage value: " + actAmtrec.getPercentage().toString());
                    break;
                }
            }
        }

    }

    public static boolean matchesFirstFive(String word, String prefix) {
        return word.startsWith(prefix);
    }
}