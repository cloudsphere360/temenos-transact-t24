package com.temenos.fusion;

import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebffpettycashroupld.EbFfPettyCashRoUpldRecord;
import com.temenos.t24.api.records.ebffpettycashroupld.FfRoExpTypeClass;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/**
 * @author jh116454
 * @Attached To: VERSION > EB.FF.PETTY.CASH.RO.UPLD,RO.PETTY.CASH
 * @Attached As: AUTH Routine > EB.API > FF.PETTY.CASH.DETAILS
 * @Description: To put FT for all given values in the above version through OFS
 * 
 * 
 */

public class FfPettyCashDetails extends RecordLifecycle {

    private static final FusionFileLogger yPettyCashDetLog = FusionFileLogger.getLogger(FfPettyCashDetails.class);

    DataAccess yDataAcc = new DataAccess(this);

    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {

        Session session = new Session(this);
        String coCode = session.getCompanyId();
        String yMne = session.getCompanyRecord().getFinancialMne().toString();

        yPettyCashDetLog.info("!---Petty Details Starts ---!");

        String yCreditAccNo = "";
        String yExpType = "";
        String yPrinStaExpAmt = "";
        String yPrinStaDebitAcNo = "";
        String yCourExpAmt = "";
        String yCourDebitAcNo = "";
        String yStaffExpAmt = "";
        String yStaffDebitAcNo = "";
        String yRepMainExpAmt = "";
        String yRepMainDebitAcNo = "";
        String yConvExpAmt = "";
        String yConvDebitAcNo = "";
        String yOfficeExpAmt = "";
        String yOfficeDebitAcNo = "";
        String yPettyOthExpAmt = "";
        String yPettyOthDebitAcNo = "";

        String yOfsSource = "FF.PETY.UPD";
        String yLocFld = "FF.PCASH.EXP";
        String yInput = "INPUT";
        String yVerName = "FUNDS.TRANSFER,PETTY.AUTH";

        try {
            EbFfPettyCashRoUpldRecord yEbFfPettyCashDetRec = new EbFfPettyCashRoUpldRecord(currentRecord);
            yCreditAccNo = yEbFfPettyCashDetRec.getCreditAcNo().getValue();

            List<FfRoExpTypeClass> yExpTypeList = yEbFfPettyCashDetRec.getFfRoExpType();
            for (int i = 0; i < yExpTypeList.size(); i++) {
                yExpType = yEbFfPettyCashDetRec.getFfRoExpType().get(i).getFfRoExpType().getValue();
                yPettyCashDetLog.info("EXP Type -> " + yExpType);

                switch (yExpType) {
                case "Printing & Station Exp":
                    yPettyCashDetLog.info("Printing & Station Exp");
                    yPrinStaExpAmt = yEbFfPettyCashDetRec.getFfRoExpType().get(i).getFfRoExpAmt().getValue();
                    yPettyCashDetLog.info("yPrinStaExpAmt -> " + yPrinStaExpAmt);
                    yPrinStaDebitAcNo = yEbFfPettyCashDetRec.getFfRoExpType().get(i).getFfRoExpAcc().getValue();
                    yPettyCashDetLog.info("yPrinStaDebitAcNo ->"+ yPrinStaDebitAcNo);
                    FundsTransferRecord yPrinStaFunTranRec = new FundsTransferRecord(this);
                    TransactionData yPrinTransData = new TransactionData();

                    yPrinStaFunTranRec.setCreditAcctNo(yCreditAccNo);
                    yPrinStaFunTranRec.setCreditAmount(yPrinStaExpAmt);
                    yPrinStaFunTranRec.setCreditCurrency("INR");
                    yPrinStaFunTranRec.setDebitAcctNo(yPrinStaDebitAcNo);
                    yPrinStaFunTranRec.setDebitCurrency("INR");
                    yPrinStaFunTranRec.setTransactionType("ACTC");
                    yPrinStaFunTranRec.setOrderingBank(yMne, 0);
                    yPrinStaFunTranRec.getProfitCentreDept().setValue("1");
                    yPrinStaFunTranRec.getLocalRefField(yLocFld).set("PrinSta");
                    yPettyCashDetLog.info("yPrinStaFunTranRec -> "+ yPrinStaFunTranRec);
                    
                    yPrinTransData.setVersionId(yVerName);
                    yPrinTransData.setFunction(yInput);
                    yPrinTransData.setSourceId(yOfsSource);
                    yPrinTransData.setNumberOfAuthoriser("0");
                    yPrinTransData.setTransactionId("/");
                    yPrinTransData.setCompanyId(coCode);
                    transactionData.add(yPrinTransData);
                    yPettyCashDetLog.info(" yPrinTransData -> "+ yPrinTransData);
                    yPettyCashDetLog.info(" transactionData -> "+ transactionData);
                    currentRecords.add(yPrinStaFunTranRec.toStructure());
                    yPettyCashDetLog.info(" Post Sucessfull");    
                    break;

                case "Courier & Postage Exp":
                    yPettyCashDetLog.info("Courier & Postage Exp");
                    yCourExpAmt = yEbFfPettyCashDetRec.getFfRoExpType().get(i).getFfRoExpAmt().getValue();
                    yPettyCashDetLog.info(" yCourExpAmt -> "+ yCourExpAmt);
                    yCourDebitAcNo = yEbFfPettyCashDetRec.getFfRoExpType().get(i).getFfRoExpAcc().getValue();
                    yPettyCashDetLog.info(" yCourDebitAcNo -> "+ yCourDebitAcNo);
                    FundsTransferRecord yCourFunTranRec = new FundsTransferRecord(this);
                    TransactionData yCourTransData1 = new TransactionData();

                    yCourFunTranRec.setCreditAcctNo(yCreditAccNo);
                    yCourFunTranRec.setCreditAmount(yCourExpAmt);
                    yCourFunTranRec.setCreditCurrency("INR");
                    yCourFunTranRec.setDebitAcctNo(yCourDebitAcNo);
                    yCourFunTranRec.setDebitCurrency("INR");
                    yCourFunTranRec.setTransactionType("ACTC");
                    yCourFunTranRec.setOrderingBank(yMne, 0);
                    yCourFunTranRec.getProfitCentreDept().setValue("1");
                    yCourFunTranRec.getLocalRefField(yLocFld).set("CourPost");
                    yPettyCashDetLog.info(" yCourFunTranRec -> "+ yCourFunTranRec);
                    
                    yCourTransData1.setVersionId(yVerName);
                    yCourTransData1.setFunction(yInput);
                    yCourTransData1.setSourceId(yOfsSource);
                    yCourTransData1.setNumberOfAuthoriser("0");
                    yCourTransData1.setTransactionId("/");
                    yCourTransData1.setCompanyId(coCode);
                    transactionData.add(yCourTransData1);
                    yPettyCashDetLog.info(" yCourTransData1 -> "+ yCourTransData1);
                    currentRecords.add(yCourFunTranRec.toStructure());
                    yPettyCashDetLog.info(" transactionData -> "+ transactionData);
                    break;

                case "Staff Welfare Exp":
                    yPettyCashDetLog.info("Staff Welfare Expense");

                    yStaffExpAmt = yEbFfPettyCashDetRec.getFfRoExpType().get(i).getFfRoExpAmt().getValue();
                    yPettyCashDetLog.info(" yStaffExpAmt -> " + yStaffExpAmt);
                    yStaffDebitAcNo = yEbFfPettyCashDetRec.getFfRoExpType().get(i).getFfRoExpAcc().getValue();
                    yPettyCashDetLog.info(" yStaffDebitAcNo -> "+ yStaffDebitAcNo);
                    FundsTransferRecord yStafWelFunTranRec = new FundsTransferRecord(this);
                    TransactionData yStafTransData = new TransactionData();

                    yStafWelFunTranRec.setCreditAcctNo(yCreditAccNo);
                    yStafWelFunTranRec.setCreditAmount(yStaffExpAmt);
                    yStafWelFunTranRec.setCreditCurrency("INR");
                    yStafWelFunTranRec.setDebitAcctNo(yStaffDebitAcNo);
                    yStafWelFunTranRec.setDebitCurrency("INR");
                    yStafWelFunTranRec.setTransactionType("ACDI");
                    yStafWelFunTranRec.setOrderingBank(yMne, 0);
                    yStafWelFunTranRec.getProfitCentreDept().setValue("1");
                    yStafWelFunTranRec.getLocalRefField(yLocFld).set("StafWel");
                    yPettyCashDetLog.info(" yStafWelFunTranRec -> "+ yStafWelFunTranRec);
                    
                    yStafTransData.setVersionId(yVerName);
                    yStafTransData.setFunction(yInput);
                    yStafTransData.setSourceId(yOfsSource);
                    yStafTransData.setNumberOfAuthoriser("0");
                    yStafTransData.setTransactionId("/");
                    yStafTransData.setCompanyId(coCode);
                    transactionData.add(yStafTransData);
                    currentRecords.add(yStafWelFunTranRec.toStructure());
                    yPettyCashDetLog.info(" yStafTransData -> "+ yStafTransData);
                    yPettyCashDetLog.info(" transactionData -> "+ transactionData);
                    break;

                case "Repair & Maintenance Exp":
                    yPettyCashDetLog.info("Repair & Maintenance Exp");
                    yRepMainExpAmt = yEbFfPettyCashDetRec.getFfRoExpType().get(i).getFfRoExpAmt().getValue();
                    yPettyCashDetLog.info(" yRepMainExpAmt -> "+ yRepMainExpAmt);
                    yRepMainDebitAcNo = yEbFfPettyCashDetRec.getFfRoExpType().get(i).getFfRoExpAcc().getValue();
                    yPettyCashDetLog.info(" yRepMainDebitAcNo -> "+ yRepMainDebitAcNo);
                    
                    FundsTransferRecord yRepMainFunTranRec = new FundsTransferRecord(this);
                    TransactionData yRepMainTransData = new TransactionData();

                    yRepMainFunTranRec.setCreditAcctNo(yCreditAccNo);
                    yRepMainFunTranRec.setCreditAmount(yRepMainExpAmt);
                    yRepMainFunTranRec.setCreditCurrency("INR");
                    yRepMainFunTranRec.setDebitAcctNo(yRepMainDebitAcNo);
                    yRepMainFunTranRec.setDebitCurrency("INR");
                    yRepMainFunTranRec.setTransactionType("ACDI");
                    yRepMainFunTranRec.setOrderingBank(yMne, 0);
                    yRepMainFunTranRec.getProfitCentreDept().setValue("1");
                    yRepMainFunTranRec.getLocalRefField(yLocFld).set("RepMain");
                    yPettyCashDetLog.info(" yRepMainFunTranRec -> "+ yRepMainFunTranRec);
                    
                    yRepMainTransData.setVersionId(yVerName);
                    yRepMainTransData.setFunction(yInput);
                    yRepMainTransData.setSourceId(yOfsSource);
                    yRepMainTransData.setNumberOfAuthoriser("0");
                    yRepMainTransData.setTransactionId("/");
                    yRepMainTransData.setCompanyId(coCode);
                    transactionData.add(yRepMainTransData);
                    yPettyCashDetLog.info(" yRepMainTransData -> "+ yRepMainTransData);                   
                    yPettyCashDetLog.info(" transactionData -> "+ transactionData);                    
                    currentRecords.add(yRepMainFunTranRec.toStructure());
                    break;

                case "Conveyance Exp":
                    yPettyCashDetLog.info("Conveyance Exp");
                    yConvExpAmt = yEbFfPettyCashDetRec.getFfRoExpType().get(i).getFfRoExpAmt().getValue();
                    yPettyCashDetLog.info(" yConvExpAmt -> "+ yConvExpAmt);
                    yConvDebitAcNo = yEbFfPettyCashDetRec.getFfRoExpType().get(i).getFfRoExpAcc().getValue();
                    yPettyCashDetLog.info(" yConvDebitAcNo -> "+ yConvDebitAcNo);
                    FundsTransferRecord yConvFunTranRec = new FundsTransferRecord(this);
                    TransactionData yConvTransData = new TransactionData();

                    yConvFunTranRec.setCreditAcctNo(yCreditAccNo);
                    yConvFunTranRec.setCreditAmount(yConvExpAmt);
                    yConvFunTranRec.setCreditCurrency("INR");
                    yConvFunTranRec.setDebitAcctNo(yConvDebitAcNo);
                    yConvFunTranRec.setDebitCurrency("INR");
                    yConvFunTranRec.setTransactionType("ACDI");
                    yConvFunTranRec.setOrderingBank(yMne, 0);
                    yConvFunTranRec.getProfitCentreDept().setValue("1");
                    yConvFunTranRec.getLocalRefField(yLocFld).set("Conven");
                    yPettyCashDetLog.info(" yConvFunTranRec -> "+ yConvFunTranRec);
                    
                    yConvTransData.setVersionId(yVerName);
                    yConvTransData.setFunction(yInput);
                    yConvTransData.setSourceId(yOfsSource);
                    yConvTransData.setNumberOfAuthoriser("0");
                    yConvTransData.setTransactionId("/");
                    yConvTransData.setCompanyId(coCode);
                    transactionData.add(yConvTransData);
                    yPettyCashDetLog.info("  yConvTransData -> "+  yConvTransData);
                    yPettyCashDetLog.info(" transactionData -> "+ transactionData);
                    currentRecords.add(yConvFunTranRec.toStructure());
                    break;

                case "Office Exp":
                    yPettyCashDetLog.info("Office Exp");
                    yOfficeExpAmt = yEbFfPettyCashDetRec.getFfRoExpType().get(i).getFfRoExpAmt().getValue();
                    yPettyCashDetLog.info(" yOfficeExpAmt -> "+ yOfficeExpAmt);
                    yOfficeDebitAcNo = yEbFfPettyCashDetRec.getFfRoExpType().get(i).getFfRoExpAcc().getValue();
                    yPettyCashDetLog.info(" yOfficeDebitAcNo -> "+ yOfficeDebitAcNo);
                    
                    FundsTransferRecord yOffiFunTranRec = new FundsTransferRecord(this);
                    TransactionData yOffTransData = new TransactionData();

                    yOffiFunTranRec.setCreditAcctNo(yCreditAccNo);
                    yOffiFunTranRec.setCreditAmount(yOfficeExpAmt);
                    yOffiFunTranRec.setCreditCurrency("INR");
                    yOffiFunTranRec.setDebitAcctNo(yOfficeDebitAcNo);
                    yOffiFunTranRec.setDebitCurrency("INR");
                    yOffiFunTranRec.setTransactionType("ACDI");
                    yOffiFunTranRec.setOrderingBank(yMne, 0);
                    yOffiFunTranRec.getProfitCentreDept().setValue("1");
                    yOffiFunTranRec.getLocalRefField(yLocFld).set("Office");
                    yPettyCashDetLog.info(" yOffiFunTranRec -> "+ yOffiFunTranRec);
                    
                    yOffTransData.setVersionId(yVerName);
                    yOffTransData.setFunction(yInput);
                    yOffTransData.setSourceId(yOfsSource);
                    yOffTransData.setNumberOfAuthoriser("0");
                    yOffTransData.setTransactionId("/");
                    yOffTransData.setCompanyId(coCode);
                    transactionData.add(yOffTransData);
                    yPettyCashDetLog.info(" yOffTransData -> "+ yOffTransData);
                    yPettyCashDetLog.info(" transactionData -> "+ transactionData);
                    currentRecords.add(yOffiFunTranRec.toStructure());
                    break;

                case "Petty Other's Exp":
                    yPettyCashDetLog.info("Petty Other's Exp");
                    yPettyOthExpAmt = yEbFfPettyCashDetRec.getFfRoExpType().get(i).getFfRoExpAmt().getValue();
                    yPettyCashDetLog.info(" yPettyOthExpAmt -> "+ yPettyOthExpAmt);
                    yPettyOthDebitAcNo = yEbFfPettyCashDetRec.getFfRoExpType().get(i).getFfRoExpAcc().getValue();
                    yPettyCashDetLog.info(" yPettyOthDebitAcNo -> "+ yPettyOthDebitAcNo);
                    
                    FundsTransferRecord yPettOthFunTranRec = new FundsTransferRecord(this);
                    TransactionData yPettyOthTransData = new TransactionData();

                    yPettOthFunTranRec.setCreditAcctNo(yCreditAccNo);
                    yPettOthFunTranRec.setCreditAmount(yPettyOthExpAmt);
                    yPettOthFunTranRec.setCreditCurrency("INR");
                    yPettOthFunTranRec.setDebitAcctNo(yPettyOthDebitAcNo);
                    yPettOthFunTranRec.setDebitCurrency("INR");
                    yPettOthFunTranRec.setTransactionType("ACDI");
                    yPettOthFunTranRec.setOrderingBank(yMne, 0);
                    yPettOthFunTranRec.getProfitCentreDept().setValue("1");
                    yPettOthFunTranRec.getLocalRefField(yLocFld).set("PettyOth");
                    yPettyCashDetLog.info(" yPettOthFunTranRec -> "+ yPettOthFunTranRec);
                    
                    yPettyOthTransData.setVersionId(yVerName);
                    yPettyOthTransData.setFunction(yInput);
                    yPettyOthTransData.setSourceId(yOfsSource);
                    yPettyOthTransData.setNumberOfAuthoriser("0");
                    yPettyOthTransData.setTransactionId("/");
                    yPettyOthTransData.setCompanyId(coCode);
                    transactionData.add(yPettyOthTransData);
                    yPettyCashDetLog.info(" yPettyOthTransData -> "+ yPettyOthTransData);
                    yPettyCashDetLog.info(" transactionData -> "+ transactionData);
                    currentRecords.add(yPettOthFunTranRec.toStructure());
                    break;

                default:
                    break;
                }

            }

        } catch (Exception e) {
            yPettyCashDetLog.info("Record Not Found-> " + e);
        }

        yPettyCashDetLog.info("!-- Petty Details Ends ---!");
    }
}
