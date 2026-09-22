package com.temenos.fusion;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.temenos.api.TField;
import com.temenos.api.exceptions.T24CoreException;
import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.t24.api.records.company.CompanyRecord;
import com.temenos.t24.api.records.companyconsol.CompanyConsolRecord;
import com.temenos.t24.api.records.ebffbranchadminexpdaily.EbFfBranchAdminExpDailyRecord;
import com.temenos.t24.api.records.ebffcollpostingscreen.EbFfCollPostingScreenRecord;
import com.temenos.t24.api.records.ebffeodscreen.EbFfEodScreenRecord;
import com.temenos.t24.api.records.ebffftpettycashlimit.EbFfFtPettyCashLimitRecord;
import com.temenos.t24.api.records.ebffparameter.EbFfParameterRecord;
import com.temenos.t24.api.records.ebffparameter.ParamDescClass;
import com.temenos.t24.api.records.ebffpettycashroupld.EbFfPettyCashRoUpldRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/**
 * -----------------------------------------------------------------------------
 * Modification History :
 * -----------------------------------------------------------------------------
 * Description : This Routine used to Display the Petty Cash Withdrawal Report,
 * which displays the withdraw transaction details in both live and matured
 * status for all branches
 *
 * Developed By : Balaji JV
 *
 * Development Reference : Teller Report
 *
 * Attached To : EB.API - FF.FT.PETTY.CASH.WITHDRAW.RPT; STANDARD.SELECTION -
 * NOFILE.FF.FT.VIEW.WITHDRAW ; ENQUIRY - FF.FT.VIEW.WITHDRAW
 * 
 * Attached As : Nofile Routine
 * 
 * -----------------------------------------------------------------------------
 */

public class FfNofileTellerCashBookReport extends Enquiry {

    public static final String DATE_RANGE_ERR = "EB-FF.BM.FUTURE.MAX.DT.RANGE";
    public static final String DATE_FILTER_ERR = "EB-FF.BM.DATE.FILTER.RANGE";
    public static final String NO_REC_ERR = "EB-NO.REC.SELECT";
    public static final String FILE_NAME = "CashbookRep_Sum";
    private static final String APPLI_NAME = "EB.FF.EOD.SCREEN";

    DataAccess da = new DataAccess(this);

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    DateTimeFormatter outDateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HHmmss");
    String enquiryName = "";
    String selectionFilter = "";
    String selBranch = "";
    String selDate = "";
    String selDateOp = "";
    String finMnemonic = "";
    String branchCode = "";
    String branchName = "";
    String zoneName = "";
    String regionName = "";
    String divisionName = "";
    String clusterName = "";
    String companyIds = "";
    String startDate = "";
    String endDate = "";
    String todayDate = "";
    String eodDate = "";
    String selFilterField = "";
    String cusMnemonic = "";
    String dateRangeVal = "";
    String dateFilterVal = "";
    String selGroup = "";
    String seluser = "";
    String processingBranch = "";
    String actualBranch = "";
    String tempBranchCode = "";
    String tempBranchName = "";
    String actualBranchName = "";

    boolean dateErrFlag;
    boolean dateGrp;
    double pettyCash = 0.0;
    double closingBalance = 0.0;
    double bcDeposit;
    double bankDeposit;
    double creditAmt;
    boolean noRecErrFlag = false;
    boolean dateRangeErrFlag = false;
    boolean dateFilterErrFlag = false;
    boolean isParentBranchSelection = false;
    double openingBalance = 0.0;
    double creditAmtBranchAdmin;

    Map<String, Double> bcDepositMap = new HashMap<>();
    Map<String, Double> bankDepositMap = new HashMap<>();
    Map<String, Double> closingBalanceMap = new HashMap<>();
    Map<String, Double> creditAmtMap = new HashMap<>();
    Map<String, Double> pettyCashMap = new HashMap<>();
    Map<String, Double> creditAmtPettyCashRoMap = new HashMap<>();

    Map<String, Double> openingBalanceMap = new HashMap<>();
    Map<String, String> eodDateMap = new HashMap<>();

    List<String> retvalues = new ArrayList<>();
    List<String> outvalues = new ArrayList<>();

    @Override
    public List<String> setIds(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {

        try {

            Session session = new Session(this);
            todayDate = session.getCurrentVariable("!TODAY");

            getFilterCriteriaDets(filterCriteria);

            List<String> ffEodScreenList = da.selectRecords("", APPLI_NAME, "", "");

            processFinalArrList(ffEodScreenList);

            String filePath = "";
            String paramId = "FF.BM.REPORT.EXTRACT";
            EbFfParameterRecord paramRec = new EbFfParameterRecord(da.getRecord("EB.FF.PARAMETER", paramId));
            for (ParamDescClass paramDesc : paramRec.getParamDesc()) {
                if (paramDesc.getParamName().getValue().equals("Path")) {
                    filePath = paramDesc.getParamValue().getValue();
                }
            }

            LocalDateTime currDtTime = LocalDateTime.now();
            String currDate = currDtTime.format(outDateFormatter);
            String currTime = currDtTime.format(timeFormatter);
            String outputPath = filePath + FILE_NAME + "_" + selGroup + "-WISE" + "_" + branchName + "_" + seluser + "_"
                    + currDate + "_" + currTime + ".csv";
            writeToFile(outvalues, outputPath);

        } catch (T24CoreException e) {
            throw e;
        } catch (Exception e) {
            throw new T24CoreException(e.getMessage());
        }
        // Changes
        if (noRecErrFlag || retvalues.isEmpty()) {

            throw new T24CoreException("", NO_REC_ERR);
        } else {

            return retvalues;
        }

    }

    public LocalDate getEndDateBasedOnParamRec(String paramValue, LocalDate stDt) {

        LocalDate expectedStartDt = null;
        try {
            if (paramValue.endsWith("D")) {

                int allowedDays = Integer.parseInt(paramValue.replace("D", ""));

                expectedStartDt = stDt.minusDays(allowedDays);

                return expectedStartDt;

            } else if (paramValue.endsWith("M")) {
                int allowedMonths = Integer.parseInt(paramValue.replace("M", ""));

                expectedStartDt = stDt.minusMonths(allowedMonths);

                return expectedStartDt;
            }
        } catch (NumberFormatException e) {

            e.getMessage();
        }
        return expectedStartDt;
    }

    public String getEbFfParamRecDets(String paramId, String paramName) {

        String paramVal = "";
        try {
            EbFfParameterRecord paramRec = new EbFfParameterRecord(da.getRecord("EB.FF.PARAMETER", paramId));
            for (ParamDescClass paramDesc : paramRec.getParamDesc()) {
                if (paramDesc.getParamName().getValue().equals(paramName)) {

                    paramVal = paramDesc.getParamValue().getValue();

                    return paramVal;

                }
            }
        } catch (Exception e) {

            e.getMessage();
        }
        return paramVal;
    }

    private void writeToFile(List<String> outvalues, String outputPath) {
        try {
            File file = new File(outputPath);
            file.getParentFile().mkdirs();
            boolean fileExists = file.exists();

            try (FileWriter writer = new FileWriter(file, true)) {
                if (outvalues == null || outvalues.isEmpty()) {
                    writer.write("No records matched the selection criteria" + System.lineSeparator());
                } else {
                    if (!fileExists) {
                        String header = String.join(",", "ZoneName", "RegionName", "DivisionName", "ClusterName",
                                "BranchCode", "BranchName", selFilterField, "Opening Balance", "BcDeposit",
                                "BankDeposit", "BranchAdminExpense", "PettyCashWithdrawal", "PettyCashExpenses",
                                "Closing Balance");
                        writer.write(header + System.lineSeparator());
                    }

                    for (String line : outvalues) {
                        writer.write(line + System.lineSeparator());
                    }
                }
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    private void processFinalArrList(List<String> ffEodScreenList) {

        try {
            String dateParamName = "CASHBOOK";
            String defDtParamId = "FF.BM.REPORT.DATE.DEFAULT";
            dateFilterVal = getEbFfParamRecDets(defDtParamId, dateParamName);

            LocalDate today = LocalDate.parse(todayDate, formatter);
            LocalDate start = startDate.isEmpty() ? getEndDateBasedOnParamRec(dateFilterVal, today)
                    : LocalDate.parse(startDate, formatter);

            LocalDate end = endDate.isEmpty() ? today : LocalDate.parse(endDate, formatter);

            Set<String> companySet = new HashSet<>(Arrays.asList(companyIds.split(" ")));

            for (String eodScreenId : ffEodScreenList) {

                String branchFromId = eodScreenId.split("-")[0];

                if (!companySet.contains(branchFromId)) {

                    continue;
                }

                eodDate = getEodDate(eodScreenId);

                if (eodDate != null && !eodDate.isEmpty()) {
                    getTellerEodScreendGrping(start, end, eodScreenId, eodDate);
                }
            }

            List<String> sortedKeys = processSortingForDateGrp();

            for (String currentGrp : sortedKeys) {
                List<String> row = new ArrayList<>();
                row.add(zoneName);
                row.add(regionName);
                row.add(divisionName);
                row.add(clusterName);
                row.add(tempBranchCode);
                row.add(tempBranchName);
                row.add(currentGrp);
                row.add(String.format("%.2f", openingBalanceMap.get(currentGrp)));
                row.add(String.format("%.2f", bcDepositMap.get(currentGrp)));
                row.add(String.format("%.2f", bankDepositMap.get(currentGrp)));
                row.add(String.format("%.2f", creditAmtMap.get(currentGrp)));

                row.add(String.format("%.2f", pettyCashMap.get(currentGrp)));
                row.add(String.format("%.2f", creditAmtPettyCashRoMap.get(currentGrp)));
                row.add(String.format("%.2f", closingBalanceMap.get(currentGrp)));

                retvalues.add(String.join("*", row));

                outvalues.add(String.join(",", row));

            }
        } catch (Exception e) {

            e.getMessage();

        }

    }

    private List<String> processSortingForDateGrp() {

        List<String> sortedKeys = new ArrayList<>(closingBalanceMap.keySet());
        try {
            if (dateGrp) {
                Collections.sort(sortedKeys);
            }
        } catch (Exception e) {

            e.getMessage();

        }
        return sortedKeys;
    }

    private void getTellerEodScreendGrping(LocalDate start, LocalDate end, String eodScreenId, String eodDate) {

        try {

            LocalDate eodDt = LocalDate.parse(eodDate, formatter);

            if (!eodDt.isBefore(start) && !eodDt.isAfter(end)) {

                getEodScreenDetails(eodScreenId);
                String groupValue = getGroupBasedValue(eodDate);

                updateCashBookMaps(groupValue);
            }
        } catch (Exception e) {

            e.getMessage();

        }
    }

    private void updateCashBookMaps(String groupValue) {

        try {
            if (groupValue != null && !groupValue.isEmpty()) {

                bcDepositMap.put(groupValue, bcDepositMap.getOrDefault(groupValue, 0.0) + Math.abs(bcDeposit));

                bankDepositMap.put(groupValue, bankDepositMap.getOrDefault(groupValue, 0.0) + Math.abs(bankDeposit));

                creditAmtMap.put(groupValue,
                        creditAmtMap.getOrDefault(groupValue, 0.0) + Math.abs(creditAmtBranchAdmin));

                closingBalanceMap.put(groupValue,
                        closingBalanceMap.getOrDefault(groupValue, 0.0) + Math.abs(closingBalance));

                pettyCashMap.put(groupValue, pettyCashMap.getOrDefault(groupValue, 0.0) + Math.abs(pettyCash));

                openingBalanceMap.put(groupValue,
                        openingBalanceMap.getOrDefault(groupValue, 0.0) + Math.abs(openingBalance));
                creditAmtPettyCashRoMap.put(groupValue,
                        creditAmtPettyCashRoMap.getOrDefault(groupValue, 0.0) + Math.abs(creditAmt));

            }
        } catch (Exception e) {

            e.getMessage();

        }
    }

    private String getGroupBasedValue(String eodDate) {

        String groupKey = "";

        switch (selGroup) {

        case "DATE":
            selFilterField = "Date";

            LocalDate dt = LocalDate.parse(eodDate, formatter);
            groupKey = dt.format(outDateFormatter);

            dateGrp = true;
            break;

        case "BRANCH":
            selFilterField = "Branch";

            if (isParentBranchSelection) {
                groupKey = actualBranchName;
            } else {
                groupKey = branchName;
            }
            break;
        default:
            break;

        }
        return groupKey;
    }

    private void getEodScreenDetails(String eodScreenId) {

        try {

            processingBranch = eodScreenId.split("-")[0];
            tempBranchCode = selBranch;
            tempBranchName = branchName;

            actualBranch = processingBranch;
            actualBranchName = getCompanyDescription(processingBranch);

            getClosingVaultBalance(eodScreenId);

            getEbFfCollPostingScreen(eodScreenId);

            getEbFfBranchAdminExpDaily(eodScreenId);

            getEbFfPettyCahLimit(eodScreenId);

            getEbFfPettyCashRoUpld(eodScreenId);

            EbFfEodScreenRecord rec = new EbFfEodScreenRecord(da.getRecord("", APPLI_NAME, "", eodScreenId));
            closingBalance = Double.parseDouble(rec.getClosingVaultBalance().getValue());

        } catch (Exception e) {
            e.getMessage();
        }
    }

    private void getClosingVaultBalance(String eodScreenId) {

        openingBalance = 0.0;

        try {
            String compId = eodScreenId.split("-")[0];

            String dateFromId = eodScreenId.split("-")[1];

            LocalDate prevDate = LocalDate.parse(dateFromId, formatter).minusDays(1);
            String previousDateEodScreenId = compId + "-" + prevDate.format(formatter);

            EbFfEodScreenRecord eodScreenRec = new EbFfEodScreenRecord(
                    da.getRecord("", APPLI_NAME, "", previousDateEodScreenId));
            openingBalance = Double.parseDouble(eodScreenRec.getClosingVaultBalance().getValue());

        } catch (Exception e) {
            e.getMessage();
        }
    }

    private void getEbFfPettyCashRoUpld(String eodScreenId) {

        creditAmt = 0.0;
        try {
            EbFfPettyCashRoUpldRecord ebFfPettyCashRoUpldRec = new EbFfPettyCashRoUpldRecord(
                    da.getRecord(cusMnemonic, "EB.FF.PETTY.CASH.RO.UPLD", "", eodScreenId));
            creditAmt = Double.parseDouble(ebFfPettyCashRoUpldRec.getCreditAmt().getValue());

        } catch (Exception e) {
            e.getMessage();
        }
    }

    private void getEbFfPettyCahLimit(String eodScreenId) {

        pettyCash = 0.0;
        String eodScreenDate = eodScreenId.split("-")[1];

        LocalDate currentDate = LocalDate.parse(eodScreenDate, formatter);

        String currentYear = String.valueOf(currentDate.getYear());

        String currentmonth = String.format("%02d", currentDate.getMonth().getValue());

        String mmyyy = currentmonth + currentYear;
        String pettyCashLimitId = eodScreenId.split("-")[0] + "-" + mmyyy;

        try {
            EbFfFtPettyCashLimitRecord ebFfPettyCahLimitRec = new EbFfFtPettyCashLimitRecord(
                    da.getRecord(cusMnemonic, "EB.FF.FT.PETTY.CASH.LIMIT", "", pettyCashLimitId));
            pettyCash = Double.parseDouble(ebFfPettyCahLimitRec.getPettyCashLimit().getValue());

        } catch (Exception e) {
            e.getMessage();
        }
    }

    private void getEbFfBranchAdminExpDaily(String eodScreenId) {

        creditAmtBranchAdmin = 0.0;
        try {
            EbFfBranchAdminExpDailyRecord ffBranchAdminExpDailyRec = new EbFfBranchAdminExpDailyRecord(
                    da.getRecord("", "EB.FF.BRANCH.ADMIN.EXP.DAILY", "", eodScreenId));

            creditAmtBranchAdmin = Double.parseDouble(ffBranchAdminExpDailyRec.getCreditAmt().getValue());

        } catch (Exception e) {
            e.getMessage();
        }

    }

    private void getEbFfCollPostingScreen(String eodScreenId) {

        bcDeposit = 0.0;
        bankDeposit = 0.0;

        try {
            EbFfCollPostingScreenRecord ebFfCollPostScreenRec = new EbFfCollPostingScreenRecord(
                    da.getRecord("", "EB.FF.COLL.POSTING.SCREEN", "", eodScreenId));
            bcDeposit = Double.parseDouble(ebFfCollPostScreenRec.getTotalDepositBcpoint().getValue());
            bankDeposit = Double.parseDouble(ebFfCollPostScreenRec.getTotalDepositBank().getValue());

        } catch (Exception e) {
            e.getMessage();
        }
    }

    private String getEodDate(String eodScreenId) {

        String ffeodDate = "";
        try {
            EbFfEodScreenRecord eodScreenRec = new EbFfEodScreenRecord(da.getRecord("", APPLI_NAME, "", eodScreenId));

            ffeodDate = eodScreenRec.getEodDate().getValue();

        } catch (Exception e) {

            e.getMessage();

        }
        return ffeodDate;
    }

    private void getFilterCriteriaDets(List<FilterCriteria> filterCriteria) {

        try {
            for (FilterCriteria filter : filterCriteria) {
                String value = filter.getValue();
                if (value == null || value.isEmpty()) {
                    continue;
                }
                switch (filter.getFieldname()) {
                case "BRANCH":
                    selBranch = value;
                    initialiseCompanyInfo(selBranch);
                    getLinkedCompIds(selBranch);
                    isParentBranchSelection = companyIds.trim().split("\\s+").length > 1;
                    break;

                case "GROUP.BY":
                    selGroup = value;
                    break;
                case "USER":
                    seluser = value;
                    break;
                case "DATE.FROM":
                    startDate = value;
                    break;
                case "DATE.TO":
                    endDate = value;
                    break;
                default:
                }
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    private void getLinkedCompIds(String selBranch) {

        StringBuilder company = new StringBuilder();

        try {
            List<String> comConsolRecList = da.selectRecords("", "COMPANY.CONSOL", "",
                    "WITH COM.CONSOL.TO EQ " + selBranch);

            if (!comConsolRecList.isEmpty()) {

                company.append(selBranch).append(" ");

                for (String comConsol : comConsolRecList) {

                    CompanyConsolRecord comConsolRec = new CompanyConsolRecord(
                            da.getRecord("COMPANY.CONSOL", comConsol));

                    List<TField> comConsolFromList = comConsolRec.getComConsolFrom();

                    if (comConsolFromList != null && !comConsolFromList.isEmpty()) {

                        for (TField comConsolFrom : comConsolFromList) {

                            company.append(comConsolFrom.getValue()).append(" ");

                        }
                    }
                }
                companyIds = company.toString().trim();

            } else {

                companyIds = selBranch;

            }

        } catch (Exception e) {

            e.getMessage();

        }

    }

    private void initialiseCompanyInfo(String companyId) {

        try {
            CompanyRecord companyObj = new CompanyRecord(da.getRecord("COMPANY", companyId));
            finMnemonic = companyObj.getFinancialMne().getValue();
            cusMnemonic = companyObj.getCustomerMnemonic().getValue();
            branchCode = companyId;

            branchName = getCompanyDescription(companyId);

            zoneName = getCompanyDescription(companyObj.getLocalRefField("FF.ZONE").getValue());

            regionName = getCompanyDescription(companyObj.getLocalRefField("FF.REGION").getValue());

            divisionName = getCompanyDescription(companyObj.getLocalRefField("FF.DIVISION").getValue());

            clusterName = getCompanyDescription(companyObj.getLocalRefField("FF.CLUSTER").getValue());

        } catch (Exception e) {

            e.getMessage();
        }

    }

    private String getCompanyDescription(String companyCode) {

        String companyName = "";

        try {
            if (companyCode != null && !companyCode.isEmpty()) {

                CompanyRecord companyRec = new CompanyRecord(da.getRecord("COMPANY", companyCode));

                companyName = companyRec.getCompanyName().get(0).getValue();

                String[] compNamePart = companyName.split("-");

                companyName = compNamePart[0];

                return companyName;
            }
        } catch (Exception e) {
            e.getMessage();
        }
        return companyName;
    }
}
