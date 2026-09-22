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
import com.temenos.t24.api.records.aaprddessettlement.AaPrdDesSettlementRecord;
import com.temenos.t24.api.records.aaprddessettlement.PayoutCurrencyClass;
import com.temenos.t24.api.records.aaproductcatalog.AaProductCatalogRecord;
import com.temenos.t24.api.records.aasimulationrunner.AaSimulationRunnerRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

public class FfSettlePrevalRout extends ActivityLifecycle {

    DataAccess da = new DataAccess(this);
    Session ses = new Session(this);
    FfGetPayOutAmt getPayObj = new FfGetPayOutAmt();
    Contract contracRec = new Contract(this);
    private static final FusionFileLogger FfSettlePrevalRout = FusionFileLogger.getLogger(FfSettlePrevalRout.class);

    @Override
    public void defaultFieldValues(AaAccountDetailsRecord accountDetailRecord,
            AaArrangementActivityRecord arrangementActivityRecord, ArrangementContext arrangementContext,
            AaArrangementRecord arrangementRecord, AaArrangementActivityRecord masterActivityRecord,
            TStructure productPropertyRecord, AaProductCatalogRecord productRecord, TStructure currecord) {

        String actStatus = arrangementContext.getActivityStatus();
        String propId = arrangementContext.getPropertyId();
        FfSettlePrevalRout.info("PropertyId: " + propId);
        FfSettlePrevalRout.info("Activity Status:" + actStatus);
        FfSettlePrevalRout.info("arrangementContext:" + arrangementContext);
        FfSettlePrevalRout.info("arrangementActivityRecord:" + arrangementActivityRecord);
        FfSettlePrevalRout.info("masterActivityRecord:" + masterActivityRecord);
        String yArrid = arrangementContext.getArrangementId();
        FfSettlePrevalRout.info("Arrangement Id:" + yArrid);
        contracRec.setContractId(yArrid);
        String simExecuteFlag = "";
        String aaSimRunnerRef = "";
        String clsLoan = "";
        String repayLoan = "";
        AaPrdDesAccountRecord accRec = null;
        try {
            aaSimRunnerRef = arrangementActivityRecord.getSimRunRef().toString();
            FfSettlePrevalRout.info("simRunnerRef :" + arrangementActivityRecord.getSimRunRef().toString());

            String comMne = ses.getCompanyRecord().getFinancialMne().toString();

            AaSimulationRunnerRecord aaasr = new AaSimulationRunnerRecord(
                    da.getRecord(comMne, "AA.SIMULATION.RUNNER", "", aaSimRunnerRef));
            simExecuteFlag = aaasr.getExecuteSimulation().getValue();
            FfSettlePrevalRout.info("executeFlag" + simExecuteFlag);
            try {
                accRec = new AaPrdDesAccountRecord(contracRec.getConditionForProperty("ACCOUNT"));
                FfSettlePrevalRout.info("accRec " + accRec.toString());
            } catch (Exception e) {
                e.getMessage();
            }

            /*
             * clsLoan =
             * accRec.getLocalRefGroups("FF.CLS.LOAN").get(0).getLocalRefField("FF.CLS.LOAN"
             * ).getValue(); FfSettlePrevalRout.info("clsLoan " + clsLoan); repayLoan =
             * accRec.getLocalRefGroups("FF.REPAY.LOAN").get(0).getLocalRefField(
             * "FF.REPAY.LOAN").getValue(); FfSettlePrevalRout.info("repayLoan " +
             * repayLoan);
             */

            LocalRefList repayGroups = accRec.getLocalRefGroups("FF.REPAY.LOAN");
            FfSettlePrevalRout.info("repayLoan List " + repayGroups.toString());
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
            FfSettlePrevalRout.info("repayLoan " + repayLoan);
            LocalRefList clsGroups = accRec.getLocalRefGroups("FF.CLS.LOAN");
            FfSettlePrevalRout.info("closeLoan List " + clsGroups.toString());
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

            FfSettlePrevalRout.info("clsLoan " + clsLoan);
        } catch (Exception e) {
            e.getMessage();
        }

        if ("UNAUTH".equals(actStatus) && "SETTLE.INSTRUCTIONS".equals(propId)
                && ("YES".equalsIgnoreCase(simExecuteFlag) || aaSimRunnerRef == null || aaSimRunnerRef.isEmpty())
                && ((clsLoan != null && !clsLoan.isEmpty()) || (repayLoan != null && !repayLoan.isEmpty()))) {
            try {
                AaPrdDesSettlementRecord settleRec = null;
                String payoutAmtSettle = getPayObj.getpayOutAmt(contracRec);
                FfSettlePrevalRout.info("The Payout amount of Settlement: " + payoutAmtSettle);
                settleRec = new AaPrdDesSettlementRecord(currecord);
                FfSettlePrevalRout.info("settleRec" + settleRec);
                String termAmt = contracRec.getTermAmount().toString();
                if (!payoutAmtSettle.equals("0.00") && !termAmt.equals(payoutAmtSettle)
                        && new BigDecimal(payoutAmtSettle.replace(",", "")).compareTo(BigDecimal.ZERO) > 0) {
                    List<PayoutCurrencyClass> payoutList = settleRec.getPayoutCurrency();
                    PayoutCurrencyClass payoutObj = payoutList.get(0);
                    FfSettlePrevalRout.info("payoutObj " + payoutObj);
                    payoutObj.getPayoutAccount(0).getPayoutAmount().set(payoutAmtSettle);

                }

                currecord.set(settleRec.toStructure());

            } catch (Exception e) {
                e.getMessage();

            }
        }

    }

}
