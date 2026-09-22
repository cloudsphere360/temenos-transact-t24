package com.temenos.fusion;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.temenos.api.TField;
import com.temenos.api.exceptions.T24CoreException;
import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.t24.api.records.aaaccountdetails.AaAccountDetailsRecord;
import com.temenos.t24.api.records.aaaccountdetails.BillIdClass;
import com.temenos.t24.api.records.aaaccountdetails.BillPayDateClass;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aabilldetails.AaBillDetailsRecord;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.records.company.CompanyRecord;
import com.temenos.t24.api.records.companyconsol.CompanyConsolRecord;
import com.temenos.t24.api.records.ebffcentredetail.EbFfCentreDetailRecord;
import com.temenos.t24.api.records.ebffloandpd.DateClass;
import com.temenos.t24.api.records.ebffloandpd.EbFfLoanDpdRecord;
import com.temenos.t24.api.records.ebffparameter.EbFfParameterRecord;
import com.temenos.t24.api.records.ebffparameter.ParamDescClass;
import com.temenos.t24.api.records.ebffrouser.EbFfRoUserRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/**
 * 
 * @author Balaji JV Date Created: 11-MARCH-2026 Attached as : NOFILE ENQUIRY
 *         EB.API :
 * 
 *         NOFILE.FF.MISSED.INSTALLMENT Attached to : STANDARD.SELECTION >
 *         NOFILE.FF.MISSED.INSTALLMENTS
 * 
 *         Enquiry Name: NOFILE.FF.MISSED.INSTALLMENTS.SUMMARY
 * 
 *         Description: Branch Online Report generation -> Last Installment
 *         Missed
 *
 *         ------------------------------------------------------------------------------
 *         Modification History : Initial Draft
 *         -----------------------------------------------------------------------------
 *         10-MARCH-2026 Development Initial Version
 *         -----------------------------------------------------------------------------
 * 
 */

public class FfLastInstallmentMissednoFileEnq extends Enquiry {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    Session session = new Session(this);
    String todayDate = session.getCurrentVariable("!TODAY");

    public static final String DATE_RANGE_ERR = "EB-FF.DATE.RANGE.GREATER";
    public static final String FILE_NAME = "LastInsMissedRep_Sum";
    public static final String NO_REC_ERR = "EB-NO.REC.SELECT";

    DataAccess da = new DataAccess(this);

    Set<String> centreSet = new HashSet<>();
    Set<String> villageSet = new HashSet<>();
    Set<String> roSet = new HashSet<>();
    Set<String> productSet = new HashSet<>();

    List<String> finalArrIdList = new ArrayList<>();
    List<String> returnVal = new ArrayList<>();

    String finMnemonic = "";
    String selGroup = "";
    String mnemonic = "";
    String branchName = "";
    String branchCode = "";
    String zoneName = "";
    String regionName = "";
    String divisionName = "";
    String clusterName = "";
    String selDate = "";
    String selDateOp = "";
    String selMonth = "";
    String selRo = "";
    String selProduct = "";
    String selCenterName = "";
    String selVillage = "";
    String selBranch = "";
    String starDate = "";
    String endDate = "";
    String missedInstallmentAmt = "";
    String ro = "";
    String arrAgeStatus = "";
    String coCode = "";
    CompanyRecord compyRec = null;

    String arrId = "";
    String centre = "";
    String product = "";
    String roCount = "";
    String centreCount = "";
    String villageCount = "";
    String productCount = "";
    String missedInstallmentAmount = "";
    String companyIds = "";
    String companyId = "";
    String startDate = "";
    String selFilterField = "";
    String missedInstallment = "";
    String stDate = "";
    String dateRangeVal = "";
    String daetFilterVal = "";
    String accNum = "";
    String centreName = "";
    String roName = "";
    String seluser = "";

    boolean dateGrp = false;
    boolean dateRangeErrFlag = false;
    boolean dateFilterErrFlag = false;
    boolean noRecErrFlag = false;
    boolean dateErrFlag = false;

    String aaArrDpdId = "";
    String startDateArrAcc = "";
    String selectionFilter = "";
    String enquiryName = "";
    List<String> finalArrList = new ArrayList<>();

    List<String> dateBaseArrId = new ArrayList<>();
    List<String> outvalues = new ArrayList<>();
    List<String> preFinalSet = new ArrayList<>();

    Map<String, Integer> loanCountMap = new HashMap<>();
    Map<String, Integer> billCountMap = new HashMap<>();
    Map<String, Double> missedInstallmentMap = new HashMap<>();

    Map<String, Integer> arrLoanCountMap = new HashMap<>();
    Map<String, Integer> arrBillCountMap = new HashMap<>();
    Map<String, Double> arrMissedAmtMap = new HashMap<>();

    DateTimeFormatter outDateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HHmmss");

    @Override
    public List<String> setIds(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {

        try {

            getFilterCriteriaDets(filterCriteria);

            runBranchSummary();

        } catch (T24CoreException e) {
            throw e;
        } catch (Exception e) {
            throw new T24CoreException(e.getMessage());
        }
        if (noRecErrFlag || returnVal.isEmpty()) {
            throw new T24CoreException("", NO_REC_ERR);
        } else {
            return returnVal;
        }
    }

    private void runBranchSummary() {

        try {

            Contract contract = new Contract(this);

            Set<String> preArrSet = new LinkedHashSet<>(da.selectRecords(finMnemonic, "AA.ARRANGEMENT", "",
                    "WITH ARR.STATUS NE CLOSE AND ARR.STATUS NE PENDING.CLOSURE AND CO.CODE EQ " + companyIds));

            getFinalArrList(preArrSet);

            for (String finalArrId : finalArrList) {

                starDate = "";
                ro = "";
                product = "";
                centre = "";
                centreName = "";
                roName = "";
                contract.setContractId(finalArrId);
                getAaArrangementDets(finalArrId);
                starDate = getSatDateFromAccountDets(finalArrId);
                getoverDueCountAmount(finalArrId);
                String groupValue = getGroupBasedValue(accNum);

                boolean selectionCheck = chkSelectionBased();
                if (selectionCheck) {
                    continue;
                }
                if (groupValue != null && !groupValue.isEmpty()) {
                    int arrLoanCount = arrLoanCountMap.getOrDefault(finalArrId, 0);
                    int arrBillIdCount = arrBillCountMap.getOrDefault(finalArrId, 0);
                    double arrMissedAmt = arrMissedAmtMap.getOrDefault(finalArrId, 0.0);

                    loanCountMap.put(groupValue, loanCountMap.getOrDefault(groupValue, 0) + arrLoanCount);
                    billCountMap.put(groupValue, billCountMap.getOrDefault(groupValue, 0) + arrBillIdCount);
                    missedInstallmentMap.put(groupValue,
                            missedInstallmentMap.getOrDefault(groupValue, 0.0) + arrMissedAmt);

                }
            }

            List<String> sortedKeys = new ArrayList<>(loanCountMap.keySet());
            if (dateGrp) {
                Collections.sort(sortedKeys);
            }

            for (String currentGrp : sortedKeys) {

                List<String> row = new ArrayList<>();

                row.add(zoneName);
                row.add(regionName);
                row.add(divisionName);
                row.add(clusterName);
                row.add(branchCode);
                row.add(branchName);
                row.add(currentGrp);

                row.add(String.valueOf(loanCountMap.get(currentGrp)));
                row.add(String.valueOf(billCountMap.get(currentGrp)));
                row.add(String.format("%.2f", missedInstallmentMap.get(currentGrp)));
                returnVal.add(String.join("*", row));

                outvalues.add(String.join(",", row));

            }

            String fileParamId = "FF.BM.REPORT.EXTRACT";
            String fileParamName = "Path";
            String filePath = getEbFfParamRecDets(fileParamId, fileParamName);

            LocalDateTime currDtTime = LocalDateTime.now();
            String currDate = currDtTime.format(outDateFormatter);
            String currTime = currDtTime.format(timeFormatter);
            String outputPath = filePath + FILE_NAME + "_" + selGroup + "-WISE" + "_" + branchName + "_" + seluser + "_"
                    + currDate + "_" + currTime + ".csv";

            writeToFile(outvalues, outputPath);

        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void getAaArrangementDets(String finalArrId) {
        try {
            AaArrangementRecord arrRec = new AaArrangementRecord(
                    da.getRecord(finMnemonic, "AA.ARRANGEMENT", "", finalArrId));
            coCode = arrRec.getCoCodeRec().getValue();
            product = arrRec.getProduct().get(0).getProduct().getValue();
            accNum = arrRec.getLinkedAppl().get(0).getLinkedApplId().getValue();

        } catch (Exception e) {
            e.getMessage();
        }

    }

    public void getFinalArrList(Set<String> preArrSet) {

        try {

            for (String contractId : preArrSet) {

                getDpdDteails(contractId);

            }
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

    public void initialiseCompanyInfo(String companyId) {

        try {
            CompanyRecord companyObj = new CompanyRecord(da.getRecord("COMPANY", companyId));
            finMnemonic = companyObj.getFinancialMne().getValue();

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

    public void getDpdDteails(String contractId) {
        String dpdId = "";
        try {

            LocalDate currDate = LocalDate.parse(todayDate, FORMATTER);
            String monthText = currDate.format(DateTimeFormatter.ofPattern("MMM")).toUpperCase();
            String year = String.valueOf(currDate.getYear());

            dpdId = contractId + "-" + monthText + year;

            EbFfLoanDpdRecord ldpd = new EbFfLoanDpdRecord(da.getRecord(finMnemonic, "EB.FF.LOAN.DPD", "", dpdId));

            for (DateClass datecls : ldpd.getDate()) {

                String cudDate = datecls.getDate().getValue();

                LocalDate parsedDate = LocalDate.parse(cudDate, FORMATTER);

                if (parsedDate.equals(currDate)) {

                    int dpdValue = Integer.parseInt(datecls.getCurDpd().getValue());

                    if (dpdValue > 0) {
                        finalArrList.add(contractId);

                    }
                }
            }
        } catch (Exception e) {

            e.getMessage();

        }

    }

    public void getoverDueCountAmount(String finalArrId) {

        boolean ageCal = false;
        int loanCount = 0;
        int billIdCount = 0;
        double sumOfMissedinstallment = 0.0;

        try {

            AaAccountDetailsRecord aaAccountDet = new AaAccountDetailsRecord(
                    da.getRecord(finMnemonic, "AA.ACCOUNT.DETAILS", "", finalArrId));

            for (BillPayDateClass billPayDate : aaAccountDet.getBillPayDate()) {
                String billdate = billPayDate.getBillPayDate().getValue();

                for (BillIdClass billId : billPayDate.getBillId()) {
                    String billIds = billId.getBillId().getValue();

                    String setStatus = billId.getSetStatus().getValue();
                    String type = billId.getBillType().getValue();

                    if ("INSTALLMENT".equals(type) && ("UNPAID".equals(setStatus) || "UNSETTLED".equals(setStatus))) {

                        ageCal = getDateComparison(ageCal, billdate);

                        billIdCount = getBillIdCount(billIdCount, type);

                        AaBillDetailsRecord aa = new AaBillDetailsRecord(
                                da.getRecord(finMnemonic, "AA.BILL.DETAILS", "", billIds));

                        sumOfMissedinstallment = getPropAmount(sumOfMissedinstallment, aa);

                    }

                }
            }
            loanCount = getArridCount(ageCal, loanCount);

            arrLoanCountMap.put(finalArrId, loanCount);
            arrBillCountMap.put(finalArrId, billIdCount);
            arrMissedAmtMap.put(finalArrId, sumOfMissedinstallment);
            finalArrIdList.add(finalArrId);

        } catch (Exception e) {
            e.getMessage();

        }

    }

    private boolean getDateComparison(boolean ageCal, String billdate) {

        try {

            LocalDate billDt = LocalDate.parse(billdate, FORMATTER);

            LocalDate todayDt = LocalDate.parse(todayDate, FORMATTER);

            if (!billDt.isAfter(todayDt)) {

                ageCal = true;
            }

        } catch (Exception e) {
            e.getMessage();

        }

        return ageCal;
    }

    private int getBillIdCount(int billIdCount, String type) {
        if ("INSTALLMENT".equals(type)) {
            billIdCount++;
        }
        return billIdCount;
    }

    private int getArridCount(boolean ageCal, int loanCount) {
        if (ageCal) {
            loanCount++;
        }
        return loanCount;
    }

    private double getPropAmount(double sumOfMissedinstallment, AaBillDetailsRecord aa) {
        double osPropAmount = 0.0;

        osPropAmount += Double.parseDouble(aa.getOrTotalAmount().getValue());
        if (osPropAmount > 0) {
            sumOfMissedinstallment += osPropAmount;
        }
        return sumOfMissedinstallment;
    }

    public void getAccountDets(String accNum) {

        try {
            AccountRecord accRec = new AccountRecord(da.getRecord(finMnemonic, "ACCOUNT", "", accNum));

            centre = accRec.getLocalRefField("FF.CENTRE").getValue();

            if (centre != null && !centre.isEmpty()) {
                getEbFfCentreDetails(centre);
            }
        } catch (Exception e) {

            e.getMessage();
        }
    }

    public void getEbFfCentreDetails(String centre) {
        try {

            EbFfCentreDetailRecord centreRec = new EbFfCentreDetailRecord(
                    da.getRecord("", "EB.FF.CENTRE.DETAIL", "", centre));

            centreName = centreRec.getCenterName().getValue();

            ro = centreRec.getCurrentRo().getValue();

            if (ro != null && !ro.isEmpty()) {

                getEbFfRoUserDets(ro);
            }
        } catch (Exception e) {

            e.getMessage();
        }
    }

    public void getEbFfRoUserDets(String ro) {
        try {

            EbFfRoUserRecord roUserRec = new EbFfRoUserRecord(da.getRecord("", "EB.FF.RO.USER", "", ro));
            roName = roUserRec.getRoName().getValue();

        } catch (Exception e) {

            e.getMessage();
        }
    }

    public String getSatDateFromAccountDets(String arrId) {
        String satDate = "";
        try {

            AaAccountDetailsRecord aaAccountDets = new AaAccountDetailsRecord(
                    da.getRecord(finMnemonic, "AA.ACCOUNT.DETAILS", "", arrId));
            satDate = aaAccountDets.getStartDate().getValue();

        } catch (Exception e) {

            e.getMessage();
        }
        return satDate;
    }

    public boolean chkSelectionBased() {
        return (selRo != null && !selRo.isEmpty() && !selRo.equals(ro))
                || (selCenterName != null && !selCenterName.isEmpty() && !selCenterName.equals(centre));
    }

    public String getGroupBasedValue(String accNum) {

        String groupKey = "";
        switch (selGroup) {

        case "DATE":
            selFilterField = "Date";
            groupKey = starDate;

            dateGrp = true;
            break;
        case "RO":
            selFilterField = "RO";
            getAccountDets(accNum);
            groupKey = roName;
            break;
        case "CENTER":

            selFilterField = "CenterName";

            getAccountDets(accNum);
            groupKey = centreName;

            break;
        // newchanges
        case "BRANCH":
            selFilterField = "Branch";
            groupKey = getCompanyDescription(coCode);
            break;

        default:
        }
        return groupKey;
    }

    public void getFilterCriteriaDets(List<FilterCriteria> filterCriteria) {

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

                case "RO":

                    selRo = value;
                    break;
                case "PRODUCT":

                    selProduct = value;
                    break;
                case "CENTER":
                    selCenterName = value;
                    break;
                default:
                }
                if (!startDate.isEmpty() && !endDate.isEmpty()) {

                    LocalDate stDt = LocalDate.parse(startDate, FORMATTER);
                    LocalDate endDt = LocalDate.parse(endDate, FORMATTER);

                    if (stDt.isAfter(endDt)) {
                        dateErrFlag = true;
                    }
                }

            }

        } catch (Exception e) {

            e.getMessage();
        }

    }

    public List<String> getArrListFromSelection(List<String> aaArrAccList) {

        List<String> arrIdList = new ArrayList<>();
        try {

            for (String aarId : aaArrAccList) {
                String[] parts = aarId.split("-");
                String aaId = parts[0];
                if (!arrIdList.contains(aaId)) {
                    arrIdList.add(aaId);
                }
            }
        } catch (Exception e) {

            e.getMessage();
        }
        return arrIdList;
    }

    public List<String> getArrListFromCentre(String centreId) {
        List<String> arrAccList = da.selectRecords(finMnemonic, "AA.ARR.ACCOUNT", "", "WITH FF.CENTRE EQ " + centreId);
        return getArrListFromSelection(arrAccList);
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
                        String header = String.join(",", "Zone Name", "Region Name", "Division Name", "Cluster Name",
                                "Branch Code", "Branch Name", selFilterField, "Loan Count", "Missed Installment",
                                "Missed Installment Amount");
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

}
