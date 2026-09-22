package com.temenos.fusion;

import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.t24.api.records.dates.DatesRecord;
import com.temenos.t24.api.records.ebffbankdepositsvaultconcat.EbFfBankDepositsVaultConcatRecord;
import com.temenos.t24.api.records.ebffeodscreen.EbFfEodScreenRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Date;
import com.temenos.t24.api.system.Session;

/**
 *
 * @author hs115664
 *
 */
public class FfNofileBranchCashInOutFlowReport extends Enquiry {

    private static final FusionFileLogger branchCashInOutFlowReport = FusionFileLogger
            .getLogger(FfNofileBranchCashInOutFlowReport.class);

    DataAccess da = new DataAccess(this);
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    List<String> eodScreenIdList = new ArrayList<>();
    List<String> finalArray = new ArrayList<>();
    Session ss = new Session(this);
    String selBranch = "";
    String endDate = "";
    String dateCondition = "";
    String dateSel = "";
    String branch = "";
    String finMnemonic = "";
    String vaultDepositBank = "";
    String vaultDepositBC = "";

    String vaultDebitBcDeposit = "";

    String vaultDebitBankDeposit = "";

    String eodScreenId = "";

    @Override
    public List<String> setIds(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {
        branchCashInOutFlowReport.info("FfNofileBranchCashInOutFlowReport is triggered" + filterCriteria.toString());
        try {
            getFilterCriteriaDets(filterCriteria);

            Date yDate = new Date(this);
            DatesRecord yDateRec = yDate.getDates();
            String yTodDt = yDateRec.getToday().getValue();
            branchCashInOutFlowReport.info("yTodDt" + yTodDt);
            if ((!branch.isEmpty())) {
                branchCashInOutFlowReport.info("branch" + branch);
                getTheDetailsBasedOnBranch(branch, yTodDt);
            }

        } catch (Exception e) {
            branchCashInOutFlowReport.error("FfNofileBranchCashInOutFlowReport error");
        }
        return finalArray;
    }

    /**
     * @param yTodDt
     * @param branch2
     */
    private void getTheDetailsBasedOnBranch(String branch, String yTodDt) {
        branchCashInOutFlowReport.info("getTheDetailsBasedOnBranch is triggered");
        try {
            SimpleDateFormat ySdf = new SimpleDateFormat("yyyyMMdd");
            Calendar cal = Calendar.getInstance();
            cal.setTime(ySdf.parse(yTodDt));
            branchCashInOutFlowReport.info("yTodDt -> " + yTodDt);

            for (int i = 0; i <= 30; i++) {
                cal.add(Calendar.DAY_OF_MONTH, -1);

                String prevDate = ySdf.format(cal.getTime());
                branchCashInOutFlowReport.info("prevDate -> " + prevDate);
                eodScreenId = branch + "-" + prevDate;
                branchCashInOutFlowReport.info(" yCollPostID -> " + eodScreenId);
                getTheEodScreenDetails(eodScreenId);

            }
        } catch (Exception e) {
            branchCashInOutFlowReport.info("PendVal Missing");
        }

    }

    /**
     * @param eodScreenId
     */
    private void getTheEodScreenDetails(String eodScreenId) {
        branchCashInOutFlowReport.info("getTheEodScreenDetails is triggered");
        try {
            EbFfEodScreenRecord eodScreenRec = new EbFfEodScreenRecord(da.getRecord("EB.FF.EOD.SCREEN", eodScreenId));
            String eodDate = eodScreenRec.getEodDate().getValue();
            String dayFromDate = getDateFromDate(eodDate);
            String vaultOpeningBal = eodScreenRec.getVaultOpeningBalance().getValue();
            String vaultTotalCashAmt = eodScreenRec.getTotalCashAmt().getValue();
            branchCashInOutFlowReport.info(eodDate + "**" + vaultOpeningBal + "**" + vaultTotalCashAmt);

            getDepositedAmountInBcVault(eodScreenId);

            String pettyCash = eodScreenRec.getPettyCash().getValue();
            String branchAdminExp = eodScreenRec.getBranchAdminExpenses().getValue();
            String suspAmt = eodScreenRec.getSuspenseAmount().getValue();
            String suspRevAmt = eodScreenRec.getSuspenseReversalAmount().getValue();
            String reverOfBank = eodScreenRec.getReversalOfBank().getValue();
            String incidentType = eodScreenRec.getIncidentType().getValue();
            branchCashInOutFlowReport.info("pettyCash" + pettyCash + "**" + branchAdminExp + "**" + suspAmt);

            String eodStatus = getTheEodStatus(eodScreenRec);

            branchCashInOutFlowReport.info("eodStatus" + eodStatus);
            String closeVaultBalance = eodScreenRec.getClosingVaultBalance().getValue();

            List<String> row = new ArrayList<>();

            row.add(eodDate);
            row.add(dayFromDate);
            row.add(vaultOpeningBal);
            row.add(vaultTotalCashAmt);
            row.add(vaultDebitBankDeposit);
            row.add(vaultDebitBcDeposit);
            row.add(pettyCash);
            row.add(branchAdminExp);
            row.add(suspAmt);
            row.add(suspRevAmt);
            row.add(reverOfBank);
            row.add(incidentType);
            row.add(eodStatus);
            row.add(closeVaultBalance);
            branchCashInOutFlowReport.info("row" + row);
            finalArray.add(String.join("*", row));

        } catch (Exception e) {
            branchCashInOutFlowReport.error("getTheEodScreenDetails error" + e.getMessage());
        }

    }

    /**
     * @param eodScreenId
     */
    private void getDepositedAmountInBcVault(String eodScreenId) {
        branchCashInOutFlowReport.info("getDepositedAmountInBcVault is triggered");
        try {
            EbFfBankDepositsVaultConcatRecord bcConcatRec = new EbFfBankDepositsVaultConcatRecord(
                    da.getRecord("EB.FF.BANK.DEPOSITS.VAULT.CONCAT", eodScreenId));
            String txnId = bcConcatRec.getVaultFtTxnId().getValue();
            if (!txnId.isEmpty()) {
                String debitAcNum = bcConcatRec.getVaultDebitAcctNo().getValue();
                branchCashInOutFlowReport.info("debitAcNum " + debitAcNum.substring(0, 8));
                if (debitAcNum.substring(0, 8).equals("INR12120")) {
                    vaultDebitBcDeposit = bcConcatRec.getCreditAmt().getValue();
                    branchCashInOutFlowReport.info("vaultDebitBcDeposit " + vaultDebitBcDeposit);
                } else if (debitAcNum.substring(0, 8).equals("INR10440")) {
                    vaultDebitBankDeposit = bcConcatRec.getCreditAmt().getValue();
                }
            }

        } catch (Exception e) {
            branchCashInOutFlowReport.error("getDepositedAmountInBcVault error" + e.getMessage());
        }
    }

    /**
     * @param eodDate
     * @return
     */
    private String getDateFromDate(String eodDate) {
        String value = "";
        try {

            branchCashInOutFlowReport.info("eodDate" + eodDate);
            LocalDate date = LocalDate.parse(eodDate, formatter);
            DayOfWeek dayOfWeek = date.getDayOfWeek();
            branchCashInOutFlowReport.info("dayOfWeek" + dayOfWeek.toString());
            value = dayOfWeek.toString();
            branchCashInOutFlowReport.info("value" + value);
        } catch (Exception e) {
            branchCashInOutFlowReport.error("FfECovDayFromDate error" + e.getMessage());
        }

        return value;
    }

    /**
     * @param eodScreenRec
     * 
     */
    private String getTheEodStatus(EbFfEodScreenRecord eodScreenRec) {
        branchCashInOutFlowReport.info("getTheEodStatus is triggered");
        String value = "";
        try {
            String recSts = eodScreenRec.getRecordStatus().toString();
            branchCashInOutFlowReport.info("recSts" + recSts);
            if (recSts.isEmpty()) {
                String inputter = eodScreenRec.getInputter(0).toString();
                branchCashInOutFlowReport.info("inputter" + inputter);
                if (inputter.contains("FF.AUTO.EOD")) {
                    value = "AUTO-EOD";
                } else if (inputter.contains("BROWSERTC")) {
                    value = "MANUAL-EOD";
                }
            }
            branchCashInOutFlowReport.info("value" + value);
        } catch (Exception e) {
            branchCashInOutFlowReport.error("getTheEodStatus error" + e.getMessage());
        }
        return value;
    }

    /**
     * @param filterCriteria
     * @return
     * @return
     */
    private void getFilterCriteriaDets(List<FilterCriteria> filterCriteria) {
        branchCashInOutFlowReport.info("getFilterCriteriaDets is triggered" + filterCriteria);
        try {
            for (FilterCriteria filter : filterCriteria) {
                String value = filter.getValue();

                if (filter.getFieldname().equals("BRANCH")) {
                    branch = value;
                }
            }
        } catch (Exception e) {
            branchCashInOutFlowReport.error("dateSel error" + e.getMessage());
        }
    }

}
