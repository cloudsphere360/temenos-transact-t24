package com.temenos.fusion;

import java.util.List;

import com.temenos.api.LocalRefList;
import com.temenos.api.TStructure;
import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.aa.activityhook.ArrangementContext;
import com.temenos.t24.api.complex.aa.activityhook.TransactionData;
import com.temenos.t24.api.hook.arrangement.ActivityLifecycle;
import com.temenos.t24.api.records.aaaccountdetails.AaAccountDetailsRecord;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaarrangement.LinkedApplClass;
import com.temenos.t24.api.records.aaarrangementactivity.AaArrangementActivityRecord;
import com.temenos.t24.api.records.aaprddesaccount.AaPrdDesAccountRecord;
import com.temenos.t24.api.records.aaproductcatalog.AaProductCatalogRecord;
import com.temenos.t24.api.records.ebffparameter.EbFfParameterRecord;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/**
 * TODO: Document me!
 *
 * @author vp115418
 *
 */
public class AaPostCloseAccountUpd extends ActivityLifecycle {
    private static final FusionFileLogger AaPostCloseAccountUpd = FusionFileLogger
            .getLogger(AaPostCloseAccountUpd.class);
    static final String ACCOUNT_VAL = "ACCOUNT";
    Session ses = new Session(this);
    DataAccess da = new DataAccess(this);
    String ymnemonic = ses.getCompanyRecord().getFinancialMne().toString();
    String companyCode = ses.getCompanyId();

    @Override
    public void postCoreTableUpdate(AaAccountDetailsRecord accountDetailRecord,
            AaArrangementActivityRecord arrangementActivityRecord, ArrangementContext arrangementContext,
            AaArrangementRecord arrangementRecord, AaArrangementActivityRecord masterActivityRecord,
            TStructure productPropertyRecord, AaProductCatalogRecord productRecord, TStructure record,
            List<TransactionData> transactionData, List<TStructure> transactionRecord) {
        String propId = arrangementContext.getPropertyId();
        String actStatus = arrangementContext.getActivityStatus();
        String transDate = arrangementActivityRecord.getEffectiveDate().getValue();
        if (actStatus.equals("AUTH") && propId.equals("SETTLE.INSTRUCTIONS")) {
            try {
                AaPostCloseAccountUpd.info("Routine triggered");
                String linkedApplId = "";
                AaPrdDesAccountRecord aaPrdDesAccountObj = null;
                AaPostCloseAccountUpd.info("AaPostCloseAccountUpd:postCoreTableUpdate routine triggered");
                Contract contractObj = new Contract(this);
                String arrId = arrangementActivityRecord.getArrangement().getValue();
                AaPostCloseAccountUpd.info("Arrangement id:" + arrId);
                contractObj.setContractId(arrId);
                List<LinkedApplClass> linkedappls = arrangementRecord.getLinkedAppl();
                linkedApplId = getlinkeApplId(linkedappls); // stores the account number
                AaPostCloseAccountUpd.info("Arrangement Account id:" + linkedApplId);
                try {
                    aaPrdDesAccountObj = new AaPrdDesAccountRecord(contractObj.getConditionForProperty("ACCOUNT"));
                    AaPostCloseAccountUpd.info("aaPrdDesAccountObj: " + aaPrdDesAccountObj);
                } catch (Exception e) {
                    e.getMessage();
                }

                LocalRefList loanField = aaPrdDesAccountObj.getLocalRefGroups("FF.CLS.LOAN"); // 3000
                AaPostCloseAccountUpd.info("loanField List" + loanField);
                LocalRefList repayField = aaPrdDesAccountObj.getLocalRefGroups("FF.REPAY.LOAN"); // 2000
                AaPostCloseAccountUpd.info("repayField List" + repayField);

                for (int i = 0; i < loanField.size(); i++) {
                    AaPostCloseAccountUpd.info("Check 1 Loop 1");
                    String loanAct = loanField.get(i).getLocalRefField("FF.CLS.LOAN").getValue();
                    String amount = loanField.get(i).getLocalRefField("FF.CLS.AMT").getValue();
                    AaPostCloseAccountUpd.info(" CLOSE loanAct " + loanAct + "CLOSE amount : " + amount);
                    disbursePrevCloseLoan(linkedApplId, arrId, loanAct, amount, transactionData, transactionRecord,transDate);
                }

                for (int j = 0; j < repayField.size(); j++) {
                    AaPostCloseAccountUpd.info("Check 2 Loop 2");
                    String loanAct = repayField.get(j).getLocalRefField("FF.REPAY.LOAN").getValue();
                    String amount = repayField.get(j).getLocalRefField("FF.REPAY.AMT").getValue();
                    AaPostCloseAccountUpd.info("REPAY loanAct " + loanAct + "REPAY amount : " + amount);

                    disbursePrevRepayLoan(linkedApplId, arrId, loanAct, amount, transactionData, transactionRecord,transDate);

                }

            } catch (Exception e) {
                AaPostCloseAccountUpd.error("AaPostCloseAccountUpd:postCoreTableUpdate: ", e);
            }
        }
    }

    public void disbursePrevCloseLoan(String acctReference, String arrId, String loanAct, String amount,
            List<TransactionData> transactionData, List<TStructure> transactionRecord, String transDate) {
        try {
            AaPostCloseAccountUpd.info("disbursePrevCloseLoan:postCoreTableUpdate Method triggered");
            String closecreditAcc = getcredAcc();
            FundsTransferRecord fundsTransferObj = new FundsTransferRecord(this);
            fundsTransferObj.setCreditAcctNo(closecreditAcc);
            fundsTransferObj.setCreditAmount(amount);
            fundsTransferObj.setCreditCurrency("INR");
            fundsTransferObj.setDebitAcctNo(acctReference);
            fundsTransferObj.setDebitCurrency("INR");
            fundsTransferObj.setCreditValueDate(transDate);
            fundsTransferObj.setDebitValueDate(transDate);
            fundsTransferObj.setTransactionType("ACDI");
            fundsTransferObj.setOrderingBank(ymnemonic, 0);
            fundsTransferObj.getLocalRefField("FF.NETOFF.CLOSE").set(loanAct);
            fundsTransferObj.getLocalRefField("FF.NETOFF.LOAN").set(arrId);
            transactionRecord.add(fundsTransferObj.toStructure());
            TransactionData transactionDataObj = new TransactionData();
            transactionDataObj.setVersionId("FUNDS.TRANSFER,NETOFF");
            transactionDataObj.setFunction("INPUT");
            transactionDataObj.setSourceId("NETOFF.OFS");
            transactionDataObj.setNumberOfAuthoriser("0");
            transactionDataObj.setTransactionId("/");
            transactionData.add(transactionDataObj);
            AaPostCloseAccountUpd.info("disbursePrevCloseLoan:postCoreTableUpdate routine triggered transactionData "
                    + transactionData.toString());

        } catch (Exception e) {
            AaPostCloseAccountUpd.error("disbursePrevCloseLoan:postCoreTableUpdate: ", e);
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
            AaPostCloseAccountUpd.info(" TransID -> " + yTransID);

            String ySubCode = companyCode.substring(5,9);
            yTransAcct = yTransID + ySubCode;
            AaPostCloseAccountUpd.info(" yTransAcct -> " + yTransAcct);

        } catch (Exception e) {
            AaPostCloseAccountUpd.error("Param Rec Missing");
        }
        return yTransAcct;
    }

    public void disbursePrevRepayLoan(String acctReference, String arrId, String loanAct, String amount,
            List<TransactionData> transactionData, List<TStructure> transactionRecord,String transDate) {
        try {
            AaPostCloseAccountUpd.info("disbursePrevRepayLoan:postCoreTableUpdate Method triggered");
            String repaycreditAcc = getcredAcc();
            FundsTransferRecord fundsTransferObj = new FundsTransferRecord(this);
            fundsTransferObj.setCreditAcctNo(repaycreditAcc);
            fundsTransferObj.setCreditAmount(amount);
            fundsTransferObj.setCreditCurrency("INR");
            fundsTransferObj.setDebitAcctNo(acctReference);
            fundsTransferObj.setDebitCurrency("INR");
            fundsTransferObj.setCreditValueDate(transDate);
            fundsTransferObj.setDebitValueDate(transDate);
            fundsTransferObj.setTransactionType("ACDI");
            fundsTransferObj.setOrderingBank(ymnemonic, 0);
            fundsTransferObj.getLocalRefField("FF.NETOFF.REPAY").set(loanAct);
            fundsTransferObj.getLocalRefField("FF.NETOFF.LOAN").set(arrId);
            transactionRecord.add(fundsTransferObj.toStructure());
            TransactionData transactionDataObj = new TransactionData();
            transactionDataObj.setVersionId("FUNDS.TRANSFER,NETOFF");
            transactionDataObj.setFunction("INPUT");
            transactionDataObj.setSourceId("NETOFF.OFS");
            transactionDataObj.setNumberOfAuthoriser("0");
            transactionDataObj.setTransactionId("/");
            transactionData.add(transactionDataObj);
            AaPostCloseAccountUpd.info("disbursePrevRepayLoan:postCoreTableUpdate routine triggered transactionData "
                    + transactionData.toString());

        } catch (Exception e) {
            AaPostCloseAccountUpd.error("disbursePrevRepayLoan:postCoreTableUpdate: ", e);
        }
    }

    public String getlinkeApplId(List<LinkedApplClass> linkedappls) {
        String linkedApplId = "";
        for (LinkedApplClass linkappl : linkedappls) {
            if (linkappl.getLinkedAppl().toString().equals(ACCOUNT_VAL)) {
                linkedApplId = linkappl.getLinkedApplId().getValue();
            }
        }
        return linkedApplId;
    }

}
