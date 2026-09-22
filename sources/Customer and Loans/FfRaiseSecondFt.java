package com.temenos.fusion;

import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebffparameter.EbFfParameterRecord;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

public class FfRaiseSecondFt extends RecordLifecycle {

    private static final FusionFileLogger FfRaiseSecondFt = FusionFileLogger.getLogger(FfRaiseSecondFt.class);
    Session ses = new Session(this);
    DataAccess da = new DataAccess(this);
    String ymnemonic = ses.getCompanyRecord().getFinancialMne().toString();
    String companyCode = ses.getCompanyId();

    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {

        FfRaiseSecondFt.info("CurrentRecord of FT post " + currentRecordId.toString());
        try {
            FfRaiseSecondFt.info("Inside FT Post Routine try block");
            FundsTransferRecord fundsTransferObj = new FundsTransferRecord(currentRecord);
            FfRaiseSecondFt.info("FundsTransfer Record: " + fundsTransferObj.toString());
            if (!fundsTransferObj.getLocalRefField("FF.NETOFF.CLOSE").toString().isEmpty()) {
                FfRaiseSecondFt.info("Inside if of close loan FT");
                FundsTransferRecord fundsTransSecObj = new FundsTransferRecord(this);
                String closedebitAcc = getcredAcc();
                FfRaiseSecondFt.info("Close Debit Account " + closedebitAcc);
                fundsTransSecObj.setCreditAcctNo(fundsTransferObj.getLocalRefField("FF.NETOFF.CLOSE").getValue());
                fundsTransSecObj.setDebitAcctNo(closedebitAcc);
                fundsTransSecObj.getLocalRefField("FF.NETOFF.CLOSE")
                        .set(fundsTransferObj.getLocalRefField("FF.NETOFF.CLOSE").getValue());
                fundsTransSecObj.setCreditAmount(fundsTransferObj.getCreditAmount().getValue());
                fundsTransSecObj.getLocalRefField("FF.NETOFF.LOAN")
                        .set(fundsTransferObj.getLocalRefField("FF.NETOFF.LOAN").getValue());
                fundsTransSecObj.getLocalRefField("FF.NETOFF.FIRST").set(currentRecordId);
                fundsTransSecObj.setDebitCurrency("INR");
                String credDate = fundsTransferObj.getCreditValueDate().getValue();
                String debDate = fundsTransferObj.getDebitValueDate().getValue();
                FfRaiseSecondFt.info("Credit value date close " + credDate);
                FfRaiseSecondFt.info("Debit value date close " + debDate);
                fundsTransSecObj.setCreditValueDate(credDate);
                fundsTransSecObj.setDebitValueDate(debDate);
                fundsTransSecObj.setTransactionType("ACP2");
                fundsTransSecObj.setCreditCurrency("INR");
                fundsTransSecObj.setOrderingBank(ymnemonic, 0);
                currentRecords.add(fundsTransSecObj.toStructure());
                TransactionData transactionDataObj = new TransactionData();
                transactionDataObj.setVersionId("FUNDS.TRANSFER,NETOFF.SEC");
                transactionDataObj.setFunction("INPUT");
                transactionDataObj.setSourceId("NETOFF.OFS");
                transactionDataObj.setNumberOfAuthoriser("0");
                transactionDataObj.setTransactionId("/");
                FfRaiseSecondFt.info("txnData close " + transactionDataObj.toString());
                transactionData.add(transactionDataObj);
            }
            if (!fundsTransferObj.getLocalRefField("FF.NETOFF.REPAY").toString().isEmpty()) {
                FfRaiseSecondFt.info("Inside if of Repay loan FT");
                FundsTransferRecord fundsTransSecObj = new FundsTransferRecord(this);

                String repaydebitAcc = getcredAcc();
                FfRaiseSecondFt.info("Repay debit Account " + repaydebitAcc);
                fundsTransSecObj.setCreditAcctNo(fundsTransferObj.getLocalRefField("FF.NETOFF.REPAY").getValue());
                fundsTransSecObj.setDebitAcctNo(repaydebitAcc);
                fundsTransSecObj.getLocalRefField("FF.NETOFF.REPAY")
                        .set(fundsTransferObj.getLocalRefField("FF.NETOFF.REPAY").getValue());
                fundsTransSecObj.setCreditAmount(fundsTransferObj.getCreditAmount().getValue());
                fundsTransSecObj.getLocalRefField("FF.NETOFF.LOAN")
                        .set(fundsTransferObj.getLocalRefField("FF.NETOFF.LOAN").getValue());
                fundsTransSecObj.getLocalRefField("FF.NETOFF.FIRST").set(currentRecordId);
                fundsTransSecObj.setDebitCurrency("INR");
                String credDate = fundsTransferObj.getCreditValueDate().getValue();
                String debDate = fundsTransferObj.getDebitValueDate().getValue();
                FfRaiseSecondFt.info("Credit value date repay " + credDate);
                FfRaiseSecondFt.info("Debit value date repay " + debDate);
                fundsTransSecObj.setCreditValueDate(credDate);
                fundsTransSecObj.setDebitValueDate(debDate);
                fundsTransSecObj.setTransactionType("ACRP");
                fundsTransSecObj.setCreditCurrency("INR");
                fundsTransSecObj.setOrderingBank(ymnemonic, 0);
                currentRecords.add(fundsTransSecObj.toStructure());
                TransactionData transactionDataObj = new TransactionData();
                transactionDataObj.setVersionId("FUNDS.TRANSFER,NETOFF.SEC");
                transactionDataObj.setFunction("INPUT");
                transactionDataObj.setSourceId("NETOFF.OFS");
                transactionDataObj.setNumberOfAuthoriser("0");
                transactionDataObj.setTransactionId("/");
                FfRaiseSecondFt.info("txnData repay " + transactionDataObj.toString());
                transactionData.add(transactionDataObj);

            }
        } catch (Exception e) {
            FfRaiseSecondFt.error("payOffCloseLoan:postCoreTableUpdate: ", e);
        }
    }

    /**
     * @return
     */
    private String getcredAcc() {
        EbFfParameterRecord yParamRec = null;
        String yTransAcct = "";
        try {
            yParamRec = new EbFfParameterRecord(da.getRecord("EB.FF.PARAMETER", "FF.NETOFF.INT.ACCT"));
            String yTransID = yParamRec.getParamDesc().get(0).getParamValue().getValue();
            FfRaiseSecondFt.info(" TransID -> " + yTransID);

            String ySubCode = companyCode.substring(5, 9);
            yTransAcct = yTransID + ySubCode;
            FfRaiseSecondFt.info(" yTransAcct -> " + yTransAcct);

        } catch (Exception e) {
            FfRaiseSecondFt.error("Param Rec Missing");
        }
        return yTransAcct;
    }

}