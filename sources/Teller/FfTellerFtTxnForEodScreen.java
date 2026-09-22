package com.temenos.fusion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.servicehook.ServiceData;
import com.temenos.t24.api.complex.eb.servicehook.SynchronousTransactionData;
import com.temenos.t24.api.complex.eb.servicehook.TransactionControl;
import com.temenos.t24.api.hook.system.ServiceLifecycle;
import com.temenos.t24.api.records.company.CompanyRecord;
import com.temenos.t24.api.records.dates.DatesRecord;
import com.temenos.t24.api.records.ebffeodscreen.EbFfEodScreenRecord;
import com.temenos.t24.api.records.ebffparameter.EbFfParameterRecord;
import com.temenos.t24.api.records.ebffparameter.ParamDescClass;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Date;
import com.temenos.t24.api.system.Session;

/**
 *
 * @author hs115664
 *
 */
public class FfTellerFtTxnForEodScreen extends ServiceLifecycle {

    DataAccess da = new DataAccess(this);
    private static final FusionFileLogger tellerFtTxnForEodScreen = FusionFileLogger
            .getLogger(FfTellerFtTxnForEodScreen.class);

    Session ss = new Session(this);
    String companyID = "";
    String today = "";
    String lastWorkingDay = "";
    String monthYear = "";
    String eodId = "";
    String subDivCode = "";
    String bcFieldDebitAccNum = "";
    String bcVaultCreditAcNum = ""; 

    @Override
    public List<String> getIds(ServiceData serviceData, List<String> controlList) {
        tellerFtTxnForEodScreen.info("===== ENTER getIds =====");

        List<String> companies = new ArrayList<>();

        try {
            companies = da.selectRecords("", "COMPANY", "", "");
            tellerFtTxnForEodScreen.info("Fetched Companies: " + companies);
        } catch (Exception e) {
            tellerFtTxnForEodScreen.info("Error fetching companies: " + e);
        }

        tellerFtTxnForEodScreen.info("Total companies count: " + companies.size());
        tellerFtTxnForEodScreen.info("===== EXIT getIds =====");

        return companies;
    }

    @Override
    public void updateRecord(String id, ServiceData serviceData, String controlItem,
            TransactionControl transactionControl, List<SynchronousTransactionData> transactionData,
            List<TStructure> records) {

        tellerFtTxnForEodScreen.info("===== START updateRecord =====");
        tellerFtTxnForEodScreen.info("Input Company ID: " + id);

        Date dates = new Date(this);
        String dattee = dates.getDates().getToday().getValue();
        String lassttdate = dates.getDates().getLastWorkingDay().getValue();

        tellerFtTxnForEodScreen.info("dattee is : " + dattee.toString());
        tellerFtTxnForEodScreen.info("lassttdate is : " + lassttdate.toString());

        companyID = id;

        try {

            // Reading dates record for the current company ID

            TStructure datesRec = da.getRecord("DATES", companyID);
            DatesRecord datesRecord = new DatesRecord(datesRec);
            String coBatchStatus = datesRecord.getCoBatchStatus().getValue();

            tellerFtTxnForEodScreen.info("datesRec is : " + datesRec.toString());
            tellerFtTxnForEodScreen.info("datesRecord is : " + datesRecord.toString());
            tellerFtTxnForEodScreen.info("coBatchStatus is : " + coBatchStatus.toString());

            // -------------------------------------------------
            // IF BATCH STATUS = O
            // USE CURRENT COMPANY DATES RECORD
            // -------------------------------------------------

            if ("O".equalsIgnoreCase(coBatchStatus)) {

                today = datesRecord.getToday().getValue();
                lastWorkingDay = datesRecord.getLastWorkingDay().getValue();

                tellerFtTxnForEodScreen.info("Using ONLINE dates record");

            } else {

                String cobCompany = companyID + "-COB";

                tellerFtTxnForEodScreen.info("Reading COB dates record : " + cobCompany);

                TStructure cobDatesRec = da.getRecord("DATES", cobCompany);

                DatesRecord cobDatesRecord = new DatesRecord(cobDatesRec);

                today = cobDatesRecord.getToday().getValue();
                lastWorkingDay = cobDatesRecord.getLastWorkingDay().getValue();

                tellerFtTxnForEodScreen.info("Using COB dates record");
            }

            eodId = companyID + "-" + today;

            getTheTxnDetails(eodId, records, transactionData);

        } catch (Exception e) {
            tellerFtTxnForEodScreen.error("MONTH YEAR: " + e.getMessage());
        }
    }

    /**
     * @param transactionData
     * @param records
     * @param eodId2
     */
    private void getTheTxnDetails(String eodId, List<TStructure> records,
            List<SynchronousTransactionData> transactionData) {

        tellerFtTxnForEodScreen.info("getTheTxnDetails " + eodId + "**" + companyID);
        try {
            EbFfEodScreenRecord eodRec = new EbFfEodScreenRecord(da.getRecord("", "EB.FF.EOD.SCREEN", "", eodId));

            BigDecimal branchcashReceivedAmt = safeBigDecimal(eodRec.getTotalCashAmt().getValue());
            tellerFtTxnForEodScreen.info("branchcashReceivedAmt " + branchcashReceivedAmt);
            if (branchcashReceivedAmt.compareTo(BigDecimal.ZERO) > 0) {
                subDivCode = getCompanyRec(companyID);
                postTheFtTxn(branchcashReceivedAmt, subDivCode, records, transactionData);
            }
        } catch (Exception e) {
            tellerFtTxnForEodScreen.error("getTheTxnDetails " + e.getMessage());
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
            tellerFtTxnForEodScreen.info("Error in safeBigDecimal");
        }
        return BigDecimal.ZERO;
    }

    /**
     * @param companyID2
     * @return
     */
    private String getCompanyRec(String companyID) {
        tellerFtTxnForEodScreen.info("getCompanyRec triggered" + companyID);
        CompanyRecord compRec = null;
        try {
            compRec = new CompanyRecord(da.getRecord("COMPANY", companyID));
            subDivCode = compRec.getSubDivisionCode().getValue();
        } catch (Exception e) {
            tellerFtTxnForEodScreen.info("getCompanyRec triggered" + e.getMessage());
        }
        return subDivCode;
    }

    /**
     * @param branchcashReceivedAmt
     * @param subDivCode
     * @param transactionData
     * @param records
     */
    private void postTheFtTxn(BigDecimal branchcashReceivedAmt, String subDivCode, List<TStructure> records,
            List<SynchronousTransactionData> transactionData) {
        tellerFtTxnForEodScreen.info("postTheFtTxn IS triggered :" + branchcashReceivedAmt);
        try {
            // String debitAccNum = DEBIT + subDivCode;
            // String creditAccNum = CREDIT + subDivCode;

            getCreditAndDebitAccNum();

            String debitAccNum = bcFieldDebitAccNum + subDivCode;
            String creditAccNum = bcVaultCreditAcNum + subDivCode;

            tellerFtTxnForEodScreen.info("debitAccNum" + debitAccNum);
            tellerFtTxnForEodScreen.info("creditAccNum" + debitAccNum);

            FundsTransferRecord ftRec = new FundsTransferRecord(this);

            ftRec.setTransactionType("ACED");
            ftRec.setDebitAcctNo(debitAccNum);
            ftRec.setCreditAcctNo(creditAccNum);
            ftRec.setCreditAmount(formatAmount(branchcashReceivedAmt));

            records.add(ftRec.toStructure());

            tellerFtTxnForEodScreen.info("ftRec IS :" + ftRec.toString());
            SynchronousTransactionData txnData = new SynchronousTransactionData();

            txnData.setFunction("INPUT");
            txnData.setCompanyId(companyID);
            txnData.setVersionId("FUNDS.TRANSFER,FF.TELLER.BRANCH.CASH");
            txnData.setNumberOfAuthoriser("0");
            txnData.setSourceId("FF.FT.CASH.TXN");

            transactionData.add(txnData);

            tellerFtTxnForEodScreen.info("txnData IS :" + txnData.toString());
        } catch (Exception e) {
            tellerFtTxnForEodScreen.info("ftRec IS :" + e.getMessage());
        }
    }

    /**
     * 
     */
    private void getCreditAndDebitAccNum() {
        tellerFtTxnForEodScreen.info("getCreditAndDebitAccNum is triggered");
        try {
            EbFfParameterRecord paramRec = new EbFfParameterRecord(
                    da.getRecord("", "EB.FF.PARAMETER", "", "FF.TELLER.ACCT.NUM"));
            List<ParamDescClass> paramDesList = paramRec.getParamDesc();
            for (ParamDescClass paramDes : paramDesList) {
                String paramDescrip = paramDes.getParamDesc().getValue();
                if (paramDescrip.equalsIgnoreCase("BC.VAULT.CREDIT.ACCT")) {
                    bcVaultCreditAcNum = paramDes.getParamValue().getValue();// INR100010001
                } else if (paramDescrip.equalsIgnoreCase("BC.FIELD.CREDIT.ACCT")) {
                    bcFieldDebitAccNum = paramDes.getParamValue().getValue(); // INR100040001
                }
            }
        } catch (Exception e) {
            tellerFtTxnForEodScreen.info("ftRec IS :" + e.getMessage());
        }

    }

    /**
     * @param branchcashReceivedAmt
     * @return
     */
    private String formatAmount(BigDecimal amount) {

        tellerFtTxnForEodScreen.info("Formatting Decimals");

        return amount.setScale(2, RoundingMode.HALF_UP).toString();
    }

}
