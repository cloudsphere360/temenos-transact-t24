package com.temenos.fusion;

import java.math.BigDecimal;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.company.CompanyRecord;
import com.temenos.t24.api.records.dates.DatesRecord;
import com.temenos.t24.api.records.ebffftpettycashlimit.EbFfFtPettyCashLimitRecord;
import com.temenos.t24.api.records.ebpettycashlimit.EbPettyCashLimitRecord;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/**
 * -----------------------------------------------------------------------------
 * Modification History :
 * -----------------------------------------------------------------------------
 * Description : This Routine used to default the values for Debit and Credit
 * Account and Debit Amount fields.
 *
 * Developed By : Preethi Selvam
 *
 * Development Reference : Teller Report
 *
 * Attached To : VERSION> FUNDS.TRANSFER,LIMIT.WITHDRAW > FF.FT.DEF.LIMIT
 * 
 * Attached As : Auto New Content Routine
 * 
 * -----------------------------------------------------------------------------
 */
public class FfFtVerDefDebitAmt extends RecordLifecycle {

    private static final String CREDIT = "INR100010001";
    private static final String DEBIT = "INR100700001";
    private static final FusionFileLogger ftDefDebitAmt = FusionFileLogger.getLogger(FfFtVerDefDebitAmt.class);

    DataAccess da = new DataAccess(this);
    Session ses = new Session(this);

    int year = 0;
    int month = 0;
    String id = "";
    String months = "";
    String companyID = "";
    String subDivCode = BigDecimal.ZERO.toString();
    String availAmt = BigDecimal.ZERO.toString();
    String cashLimit = BigDecimal.ZERO.toString();

    @Override
    public void defaultFieldValues(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
        ftDefDebitAmt.info("FfFtVerDefDebitAmt is triggered");
        try {
            FundsTransferRecord fundTransferRec = new FundsTransferRecord(currentRecord);
            companyID = ses.getCompanyId();
            DatesRecord dateRec = new DatesRecord(da.getRecord("DATES", companyID));
            String todayDate = dateRec.getToday().getValue();
            if (todayDate != null && todayDate.length() >= 6) {
                String monthStr = todayDate.substring(4, 6);
                String yearStr = todayDate.substring(0, 4);

                if (monthStr.equalsIgnoreCase("01")) {// change to equals
                    year = Integer.parseInt(yearStr) - 1;
                    months = "12";
                    id = companyID + "-" + months + year;

                } else {
                    month = Integer.parseInt(monthStr) - 1;
                    String month3 = String.format("%02d", month);
                    id = companyID + "-" + month3 + yearStr;

                }
            }
            EbFfFtPettyCashLimitRecord limitStruct = getpettyCashLimit(id);
            cashLimit = getEbPettyCashLimit(companyID);
            subDivCode = getCompanyRec(companyID);

            if ((limitStruct != null) && (!limitStruct.getAvailableLimit().getValue().isEmpty())) {
                availAmt = limitStruct.getAvailableLimit().getValue();
            }

            double availAmtDouble = Math.abs(Double.parseDouble(availAmt));
            double cashLimitDouble = Math.abs(Double.parseDouble(cashLimit));
            double calAmt = cashLimitDouble - availAmtDouble;
            if (calAmt == Math.floor(calAmt)) {
                fundTransferRec.setDebitAmount(String.format("%.0f", calAmt));
            } else {
                fundTransferRec.setDebitAmount(String.format("%.2f", calAmt));
            }
            String debitAcNum = DEBIT + subDivCode;
            String creditAcNum = CREDIT + subDivCode;

            fundTransferRec.setDebitAcctNo(debitAcNum);
            fundTransferRec.setCreditAcctNo(creditAcNum);

            currentRecord.set(fundTransferRec.toStructure());

        } catch (Exception e) {
            ftDefDebitAmt.error("FfFtVerDefDebitAmt is triggered" + e);
        }
    }

    /**
     * @param companyID2
     * @return
     */
    private String getCompanyRec(String companyID) {

        CompanyRecord compRec = null;
        try {
            compRec = new CompanyRecord(da.getRecord("COMPANY", companyID));
            subDivCode = compRec.getSubDivisionCode().getValue();
        } catch (Exception e) {
            e.getMessage();
        }
        return subDivCode;
    }

    /**
     * @param companyID2
     * @return
     */
    private String getEbPettyCashLimit(String companyID) {

        EbPettyCashLimitRecord pettyRec = null;
        try {
            pettyRec = new EbPettyCashLimitRecord(da.getRecord("EB.PETTY.CASH.LIMIT", companyID));
            cashLimit = pettyRec.getPettyCash().getValue(); // 5000
        } catch (Exception e) {
            e.getMessage();
        }
        return cashLimit;
    }

    /**
     * @param id2
     * @return
     */
    private EbFfFtPettyCashLimitRecord getpettyCashLimit(String id) {

        EbFfFtPettyCashLimitRecord limitStruct = null;
        try {
            limitStruct = new EbFfFtPettyCashLimitRecord(da.getRecord("EB.FF.FT.PETTY.CASH.LIMIT", id));
        } catch (Exception e) {
            e.getMessage();
        }
        return limitStruct;
    }
}
