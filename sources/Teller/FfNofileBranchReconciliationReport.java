package com.temenos.fusion;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.temenos.api.exceptions.T24CoreException;
import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.t24.api.records.dates.DatesRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Date;
import com.temenos.t24.api.system.Session;

/**
 *
 * @author hs115664
 *
 */
public class FfNofileBranchReconciliationReport extends Enquiry {

    private static final FusionFileLogger branchReconciliationReport = FusionFileLogger
            .getLogger(FfNofileBranchReconciliationReport.class);

    DataAccess da = new DataAccess(this);
    Session ss = new Session(this);
    Date yDate = new Date(this);
    DatesRecord yDateRec = yDate.getDates();
    String yTodDt = yDateRec.getToday().getValue();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    List<String> eodScreenIdList = new ArrayList<>();
    List<String> finalArray = new ArrayList<>();
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

    String startDate = "";

    @Override
    public List<String> setIds(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {

        getFilterCriteriaDets(filterCriteria);
        if (startDate.equals(yTodDt)) {
            throw new T24CoreException("", "Date should be Lesser than Today");
        }
        try {
            if ((!startDate.isEmpty())) {
                eodScreenIdList = da.selectRecords("", "EB.FF.EOD.SCREEN", "", "WITH EOD.DATE EQ " + startDate);
                branchReconciliationReport.info("Give Both selection" + startDate + "***" + eodScreenIdList.toString());

            }
            for (String eodScreenId : eodScreenIdList) {
                branchReconciliationReport.info("eodScreenId" + eodScreenId);
                getTheEodScreenDetails(eodScreenId);
            }
        } catch (Exception e) {
            branchReconciliationReport.error("FfNofileBranchCashInOutFlowReport error" + e.getMessage());
        }
        return finalArray;

    }

    /**
     * @param eodScreenId
     */
    private void getTheEodScreenDetails(String eodScreenId) {
        try {

            List<String> row = new ArrayList<>();

            row.add(eodScreenId);

            branchReconciliationReport.info("row" + row);
            finalArray.add(String.join("*", row));
        } catch (Exception e) {
            branchReconciliationReport.error("FfNofileBranchCashInOutFlowReport error");

        }
    }

    /**
     * @param filterCriteria
     */
    private void getFilterCriteriaDets(List<FilterCriteria> filterCriteria) {
        branchReconciliationReport.info("getFilterCriteriaDets is triggered" + filterCriteria);

        for (FilterCriteria filter : filterCriteria) {
            if (filter.getFieldname().equals("EOD.DATE")) {
                String value = filter.getValue();
                branchReconciliationReport.info("value" + value + "**" + yTodDt);
                startDate = value;
                branchReconciliationReport.info("startDate" + startDate);
                break;
            }
        }
    }
}
