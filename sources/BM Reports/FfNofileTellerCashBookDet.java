package com.temenos.fusion;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
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
 * 
 * Description : This Routine used to Display the Petty Cash Withdrawal Report,
 * which displays the withdraw transaction details in both live and matured
 * status for all branches
 *
 * Developed By : Meera
 *
 * Development Reference : Teller Report
 *
 * Attached To : EB.API - NOFILE.CASHBOOK.DET; STANDARD.SELECTION -
 * NOFILE.CASHBOOK.DET ; ENQUIRY - CASHBOOK.DET
 * 
 * Attached As : Nofile Routine
 * 
 *
 */
public class FfNofileTellerCashBookDet extends Enquiry {

    private static final String EB_FF_EOD_SCREEN = "EB.FF.EOD.SCREEN";

    public static final String FILE_NAME = "CashBookRep_Det";
    public static final String DATE_RANGE_ERR = "EB-FF.BM.FUTURE.MAX.DT.RANGE";
    public static final String DATE_FILTER_ERR = "EB-FF.BM.DATE.FILTER.RANGE";
    public static final String NO_REC_ERR = "EB-NO.REC.SELECT";

    DataAccess da = new DataAccess(this);
    Session ss = new Session(this);

    List<String> outvalues = new ArrayList<>();
    List<String> returnValueList = new ArrayList<>();
    Set<String> ffEodScreenList = new HashSet<>();

    String finMnemonic = "";
    String zoneName = "";
    String regionName = "";
    String divisionName = "";
    String clusterName = "";
    String stateName = "";
    String branchCode = "";
    String branchName = "";
    double closingBalance = 0.0;
    double bcDeposit = 0.0;
    double bankDeposit = 0.0;
    double creditAmtBranchAdmin = 0.0;
    double creditAmtPettyRo = 0.0;
    double pettyCashLimit = 0.0;
    double openingBalance = 0.0;
    String companyIds = "";
    String todayDate = "";
    String selBranch = "";
    String startDate = "";
    String endDate = "";
    String selDate = "";
    String selDateOp = "";
    String eodDate = "";
    String ffeodDate = "";
    String branchDistrict = "";
    String branchManagerName = "";
    String branchManagerMobileNumber = "";
    String companyName = "";
    String selUser = "";

    boolean dateErrFlag;
    boolean noRecErrFlag = false;
    boolean dateRangeErrFlag = false;
    boolean dateFilterErrFlag = false;
    String dateRangeVal = "";
    String dateFilterVal = "";

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    DateTimeFormatter outDateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HHmmss");

    @Override
    public List<String> setIds(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {

        try {
            Session session = new Session(this);
            todayDate = session.getCurrentVariable("!TODAY");

            getSelBranch(filterCriteria);
            getLinkedCompIds(selBranch);

            List<String> ffEodScreen = da.selectRecords("", EB_FF_EOD_SCREEN, "", "");

            Set<String> selectionSet = getFilterCriteriaDets(filterCriteria);

            processDateValidation(startDate, endDate);
            if (dateFilterErrFlag) {
                throw new T24CoreException(covertParamValue(dateFilterVal), DATE_FILTER_ERR);
            }
            if (dateRangeErrFlag) {
                throw new T24CoreException(covertParamValue(dateRangeVal), DATE_RANGE_ERR);
            }

            Set<String> finalSet = selectionSet.isEmpty() ? new LinkedHashSet<>(ffEodScreen)
                    : new LinkedHashSet<>(selectionSet);

            finalSet.retainAll(ffEodScreen);

            LocalDate today = LocalDate.parse(todayDate, formatter);
            LocalDate start = startDate.isEmpty() ? getEndDateBasedOnParamRec(dateFilterVal, today)
                    : LocalDate.parse(startDate, formatter);
            LocalDate end = endDate.isEmpty() ? today : LocalDate.parse(endDate, formatter);

            Set<String> companySet = new HashSet<>(Arrays.asList(companyIds.split(" ")));

            for (String eodScreenId : finalSet) {

                String branchFromId = eodScreenId.split("-")[0];

                if (!companySet.contains(branchFromId)) {

                    continue;
                }

                eodDate = getEodDate(eodScreenId);

                getProcessEodId(start, end, eodScreenId);
            }

            getParameter();

        } catch (T24CoreException e) {
            throw e;
        } catch (Exception e) {
            throw new T24CoreException(e.getMessage());
        }

        if (noRecErrFlag || returnValueList.isEmpty()) {
            throw new T24CoreException("", NO_REC_ERR);
        } else {
            return returnValueList;
        }

    }

    public void processDateValidation(String startDate, String endDate) {

        try {

            String dateParamId = "FF.BM.REPORT.DATE.RANGE";
            String dateParamName = "CASHBOOK";
            String defDtParamId = "FF.BM.REPORT.DATE.DEFAULT";

            dateRangeVal = getEbFfParamRecDets(dateParamId, dateParamName);
            dateFilterVal = getEbFfParamRecDets(defDtParamId, dateParamName);

            if ((startDate != null && !startDate.isEmpty()) && (endDate != null && !endDate.isEmpty())) {
                LocalDate todayDt = LocalDate.parse(todayDate, formatter);
                LocalDate stDt = LocalDate.parse(startDate, formatter);
                LocalDate endDt = LocalDate.parse(endDate, formatter);

                LocalDate expectedStartDt = getEndDateBasedOnParamRec(dateRangeVal, todayDt);

                boolean isRangeValid = !stDt.isBefore(expectedStartDt) && !endDt.isAfter(todayDt);

                if (!isRangeValid) {
                    dateRangeErrFlag = true;
                }

                LocalDate defExpStDt = getEndDateBasedOnParamRec(dateFilterVal, endDt);

                if (stDt.isBefore(defExpStDt)) {
                    dateFilterErrFlag = true;
                }
            }

        } catch (Exception e) {
            e.getMessage();
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

    public String covertParamValue(String value) {

        if (value == null || value.isEmpty()) {
            return "";
        }
        String numberPart = value.replaceAll("\\D", "");
        String unitPart = value.replaceAll("\\d", "");

        if (numberPart.isEmpty() || unitPart.isEmpty()) {
            return value;
        }

        int num = Integer.parseInt(numberPart);
        char unit = Character.toUpperCase(unitPart.charAt(0));

        switch (unit) {
        case 'D':
            return num + (num == 1 ? " Day" : " Days");
        case 'M':
            return num + (num == 1 ? " Month" : " Months");
        default:
            return value;
        }
    }

    private void getProcessEodId(LocalDate start, LocalDate end, String eodScreenId) {

        try {
            if (eodDate != null && !eodDate.isEmpty()) {

                LocalDate eodDt = LocalDate.parse(eodDate, formatter);

                if (!eodDt.isBefore(start) && !eodDt.isAfter(end)) {

                    branchCode = eodScreenId.split("-")[0];

                    getClosingVaultBalance(eodScreenId);
                    getEbFfCollPostingScreen(eodScreenId);
                    getEbFfBranchAdminExpDaily(eodScreenId);
                    getEbFfFtPettyCashLimit(eodScreenId);
                    getEbFfPettyCashRoUpld(eodScreenId);
                    getClosingBalance(eodScreenId);

                    List<String> row = new ArrayList<>();
                    row.add(zoneName);
                    row.add(regionName);
                    row.add(divisionName);
                    row.add(clusterName);
                    row.add(branchName);
                    row.add(branchDistrict); // no mapping
                    row.add(stateName);
                    row.add(branchCode);
                    row.add(branchManagerName); // no mapping
                    row.add(branchManagerMobileNumber); // no mapping
                    row.add(eodDate);
                    row.add(String.valueOf(Math.abs(openingBalance)));
                    row.add(String.valueOf(Math.abs(bcDeposit)));
                    row.add(String.valueOf(Math.abs(bankDeposit)));
                    row.add(String.valueOf(Math.abs(creditAmtBranchAdmin)));
                    row.add(String.valueOf(Math.abs(pettyCashLimit)));
                    row.add(String.valueOf(Math.abs(creditAmtPettyRo)));
                    row.add(String.valueOf(Math.abs(closingBalance)));

                    returnValueList.add(String.join("*", row));

                    outvalues.add(String.join(",", row));

                }
            }
        } catch (Exception e) {
            e.getMessage();

        }
    }

    private void getSelBranch(List<FilterCriteria> filterCriteria) {
        for (FilterCriteria filter : filterCriteria) {
            if (filter.getFieldname().equals("BRANCH")) {
                selBranch = filter.getValue();
                initialiseCompanyInfo(selBranch);

                break;
            }
        }
    }

    private void getParameter() {
        try {

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
            String outputPath = filePath + FILE_NAME + "_" + branchName + "_" + selUser + "_" + currDate + "_"
                    + currTime + ".csv";

            writeToFile(outvalues, outputPath);

        } catch (Exception e) {
            e.getMessage();

        }
    }

    public void initialiseCompanyInfo(String companyId) {

        try {
            CompanyRecord companyObj = new CompanyRecord(da.getRecord("COMPANY", companyId));

            finMnemonic = companyObj.getFinancialMne().getValue();

            branchName = getCompanyDescription(companyId);

            branchCode = companyId;

            zoneName = getCompanyDescription(companyObj.getLocalRefField("FF.ZONE").getValue());

            regionName = getCompanyDescription(companyObj.getLocalRefField("FF.REGION").getValue());

            divisionName = getCompanyDescription(companyObj.getLocalRefField("FF.DIVISION").getValue());

            clusterName = getCompanyDescription(companyObj.getLocalRefField("FF.CLUSTER").getValue());

            stateName = companyObj.getLocalRefField("FF.STATE").getValue();

        } catch (Exception e) {
            e.getMessage();

        }
    }

    private String getCompanyDescription(String companyCode) {

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

    public void getLinkedCompIds(String selBranch) {

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

    private String getEodDate(String eodScreenId) {

        try {
            EbFfEodScreenRecord eodScreenRec = new EbFfEodScreenRecord(
                    da.getRecord("", EB_FF_EOD_SCREEN, "", eodScreenId));

            ffeodDate = eodScreenRec.getEodDate().getValue();

        } catch (Exception e) {
            e.getMessage();

        }
        return ffeodDate;

    }

    private void getClosingBalance(String eodScreenId) {

        closingBalance = 0.0;

        try {
            EbFfEodScreenRecord eodScreenRec = new EbFfEodScreenRecord(
                    da.getRecord("", EB_FF_EOD_SCREEN, "", eodScreenId));

            closingBalance = Double.parseDouble(eodScreenRec.getClosingVaultBalance().getValue());

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
                    da.getRecord("", EB_FF_EOD_SCREEN, "", previousDateEodScreenId));

            openingBalance = Double.parseDouble(eodScreenRec.getClosingVaultBalance().getValue());

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

    private void getEbFfFtPettyCashLimit(String eodScreenId) {

        pettyCashLimit = 0.0;

        String eodScreenDate = eodScreenId.split("-")[1];

        LocalDate currentDate = LocalDate.parse(eodScreenDate, formatter);

        String currentYear = String.valueOf(currentDate.getYear());

        String currentmonth = String.format("%02d", currentDate.getMonth().getValue());

        String mmyyy = currentmonth + currentYear;

        String pettyCashLimitId = eodScreenId.split("-")[0] + "-" + mmyyy;

        try {
            EbFfFtPettyCashLimitRecord ffFtPettyCashLimit = new EbFfFtPettyCashLimitRecord(
                    da.getRecord("", "EB.FF.FT.PETTY.CASH.LIMIT", "", pettyCashLimitId));

            pettyCashLimit = Double.parseDouble(ffFtPettyCashLimit.getPettyCashLimit().getValue());

        } catch (Exception e) {
            e.getMessage();

        }

    }

    private void getEbFfPettyCashRoUpld(String eodScreenId) {

        creditAmtPettyRo = 0.0;

        try {
            EbFfPettyCashRoUpldRecord ffPettyCashRoUpld = new EbFfPettyCashRoUpldRecord(
                    da.getRecord("", "EB.FF.PETTY.CASH.RO.UPLD", "", eodScreenId));

            creditAmtPettyRo = Double.parseDouble(ffPettyCashRoUpld.getCreditAmt().getValue());

        } catch (Exception e) {
            e.getMessage();

        }

    }

    private Set<String> getFilterCriteriaDets(List<FilterCriteria> filterCriteria) {

        Set<String> selectionSet = new LinkedHashSet<>();

        try {
            for (FilterCriteria filter : filterCriteria) {
                String value = filter.getValue();
                if (value == null || value.isEmpty()) {
                    continue;
                }
                switch (filter.getFieldname()) {
                case "BRANCH":
                    selBranch = value;
                    if ("CURRENT.ID".equalsIgnoreCase(selBranch)) {
                        Session session = new Session(this);
                        selBranch = session.getCompanyId();

                    }
                    initialiseCompanyInfo(selBranch);
                    getLinkedCompIds(selBranch);
                    break;

                case "DATE.FROM":
                    startDate = value;
                    break;

                case "DATE.TO":
                    endDate = value;
                    break;

                case "USER":
                    selUser = value;
                    break;
                default:
                }
            }

        } catch (Exception e) {
            e.getMessage();

        }
        return selectionSet;
    }

    public void writeToFile(List<String> data, String filePath) {

        try {

            File file = new File(filePath);
            file.getParentFile().mkdirs();
            boolean fileExists = file.exists();

            try (FileWriter writer = new FileWriter(file, true)) {
                if (data == null || data.isEmpty()) {
                    writer.write("No records matched the selection criteria" + System.lineSeparator());
                } else {

                    if (!fileExists) {
                        String header = String.join(",", "ZoneName", "RegionName", "DivisionName", "ClusterName",
                                "BranchName", "Branchdistrict", "BranchState", "BranchCode", "BranchManagerName",
                                "BranchManagerMobileNumber", "Date", "OpeningBalance", "BcDeposit", "BankDeposit",
                                "BranchAdminExpense", "PettyCashWithdraw", "PettyCashExpenses", "ClosingBalance");
                        writer.write(header + System.lineSeparator());
                    }

                    for (String line : data) {
                        writer.write(line + System.lineSeparator());
                    }
                }
            }
        } catch (Exception e) {
            e.getMessage();

        }
    }

}
