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
import com.temenos.t24.api.complex.aa.contractapi.BalanceMovement;
import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;

import com.temenos.t24.api.records.aaproduct.AaProductRecord;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.records.company.CompanyRecord;
import com.temenos.t24.api.records.companyconsol.CompanyConsolRecord;
import com.temenos.t24.api.records.ebffcentredetail.EbFfCentreDetailRecord;
import com.temenos.t24.api.records.ebffcollectiondets.EbFfCollectionDetsRecord;
import com.temenos.t24.api.records.ebffparameter.EbFfParameterRecord;
import com.temenos.t24.api.records.ebffparameter.ParamDescClass;
import com.temenos.t24.api.records.ebffrouser.EbFfRoUserRecord;
import com.temenos.t24.api.records.ebffvillage.EbFfVillageRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/*-----------------------------------------------------------------------------
 * @author Renuka M
 * Date Created:
 * Attached as : 
 * EB.API : EB.FF.CENTER.MEETING.REPORT
 * Attached to : STANDARD.SELECTION >NOFILE.FF.CENTER.MEETING.REPORT
 * Description: Branch Online Report generation -> 
 *------------------------------------------------------------------------------ 
 * Modification History : NA
 *----------------------------------------------------------------------------- 
 *05-Dec-2025   Development      Initial Version
 *
 *06-Mar-2026   Re-mapping       Sri Rahul R
 *-----------------------------------------------------------------------------
 */
public class FfNofileCenterMeetingReportSummary extends Enquiry {

    public static final String DATE_RANGE_ERR = "EB-FF.DATE.RANGE.GREATER";
    public static final String NO_REC_ERR = "EB-NO.REC.SELECT";
    private static final String REQ_TYPE_TRADE = "TRADE";
    public static final String FILE_NAME = "CenterMeetingSummary";
    public static final String FUTURE_DATE_RANGE_ERR = "EB-FF.BM.FUTURE.MAX.DT.RANGE";
    public static final String DATE_FILTER_RANGE_ERR = "EB-FF.BM.DATE.FILTER.RANGE";
    public static final String EB_FF_PARAMETER = "EB.FF.PARAMETER";
    public static final String CENTER_MEETING = "CENTER.MEETING";
    private static final String DEFAULT_REPORT_DATE = "FF.BM.REPORT.DATE.DEFAULT";
    private static final String DATE_GROUP = "DATE";

    List<String> retvalues = new ArrayList<>();
    List<String> outvalues = new ArrayList<>();
    List<String> finalArrList = new ArrayList<>();

    DataAccess da = new DataAccess(this);

    Set<String> filterValSet = new HashSet<>();
    Set<String> centreSet = new HashSet<>();
    Set<String> villageSet = new HashSet<>();
    Set<String> roSet = new HashSet<>();
    Set<String> productSet = new HashSet<>();

    String selDate = "";
    String selDateOp = "";
    String selMonth = "";
    String selRo = "";
    String selProduct = "";
    String selCenterName = "";
    String selVillage = "";
    String startDate = "";
    String endDate = "";
    String zoneName = "";
    String regionName = "";
    String divisionName = "";
    String clusterName = "";
    String branchCode = "";
    String branchName = "";
    String finMnemonic = "";
    String mnemonic = "";
    String todayDate = "";

    String customer = "";
    String selBranch = "";

    String companyIds = "";
    String ro = "";
    String product = "";
    String centre = "";
    String village = "";
    String coCode = "";

    String lastdueDatesValue = "";
    String lastdueDates = "";
    String dueDateVal = "";
    double outstanding = 0.0;

    boolean dateGrp = false;
    boolean centreFlag = false;

    String selFilterField = "";
    String selDateTo = "";

    String dateRangeVal = "";
    String dateFilterVal = "";

    String productName = "";
    String centreName = "";
    String villageName = "";
    String roName = "";
    String accNum = "";
    String selGroup = "";
    String companyName = "";
    String selUser = "";
    String dueDate = "";
    int collectionDetsCnts = 0;

    String outputPath = "";

    boolean dateFilterErrFlag = false;
    boolean dateRangeErrFlag = false;

    DateTimeFormatter t24DateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    DateTimeFormatter outDateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HHmmss");
    // Method for Set Id's

    @Override
    public List<String> setIds(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {

        Session session = new Session(this);
        todayDate = session.getCurrentVariable("!TODAY");

        getFilterCriteriaDets(filterCriteria);

        session.setCurrentVariable("!COMPANY", selBranch);

        session.setCurrentVariable("ID.COMPANY", selBranch);

        try {

            Set<String> futureArrSet = new LinkedHashSet<>(da.selectRecords(finMnemonic, "AA.ARRANGEMENT", "",
                    "WITH ARR.STATUS NE CLOSE PENDING.CLOSURE EXPIRED AND CO.CODE EQ " + companyIds));

            processFinalArrList(futureArrSet);

            String filePath = "";
            String paramId = "FF.BM.REPORT.EXTRACT";
            EbFfParameterRecord paramRec = new EbFfParameterRecord(da.getRecord(EB_FF_PARAMETER, paramId));
            for (ParamDescClass paramDesc : paramRec.getParamDesc()) {
                if (paramDesc.getParamName().getValue().equals("Path")) {
                    filePath = paramDesc.getParamValue().getValue();

                }
            }

            LocalDateTime currDtTime = LocalDateTime.now();
            String currDate = currDtTime.format(outDateFormatter);
            String currTime = currDtTime.format(timeFormatter);
            outputPath = filePath + FILE_NAME + "_" + selGroup + "-WISE" + "_" + branchName + "_" + selUser + "_"
                    + currDate + "_" + currTime + ".csv";
            writeToFile(outvalues, outputPath);

        } catch (Exception e) {
            e.getMessage();
        }

        if (retvalues.isEmpty()) {
            throw new T24CoreException("", NO_REC_ERR);
        } else {
            return retvalues;
        }
    }

    // Method for Fetching values from respective application
    public void processFinalArrList(Set<String> futureArrSet) {
        Map<String, Integer> activeLoanCount = new HashMap<>();
        Map<String, Set<String>> groupToCenters = new HashMap<>();
        Map<String, Set<String>> groupToMembers = new HashMap<>();
        Map<String, Double> groupToOutstanding = new HashMap<>();

        Contract contract = new Contract(this);

        resetArrangementDetails();

        try {
            LocalDate today = LocalDate.parse(todayDate, t24DateFormatter);
            setDateFilter();

            LocalDate start = getStartDate(today);
            LocalDate end = getEndDate(today);

            processFutureArrangements(futureArrSet, start, end, contract);

            processSortedResults(activeLoanCount, groupToCenters, groupToMembers, groupToOutstanding);

        } catch (Exception e) {
            throw new IllegalStateException("Error while processing final arrangement list", e);
        }
    }

    private void processSortedResults(Map<String, Integer> activeLoanCount, Map<String, Set<String>> groupToCenters,
            Map<String, Set<String>> groupToMembers, Map<String, Double> groupToOutstanding) {

        List<String> sortedKeys = processSortingForDateGrp(activeLoanCount);

        for (String currentGrp : sortedKeys) {
            processGroupResult(currentGrp, activeLoanCount, groupToCenters, groupToMembers, groupToOutstanding);
        }
    }

    private void processGroupResult(String currentGrp, Map<String, Integer> activeLoanCount,
            Map<String, Set<String>> groupToCenters, Map<String, Set<String>> groupToMembers,
            Map<String, Double> groupToOutstanding) {

        int centerCount = groupToCenters.getOrDefault(currentGrp, Collections.emptySet()).size();

        int memberCount = groupToMembers.getOrDefault(currentGrp, Collections.emptySet()).size();

        int loanCount = activeLoanCount.getOrDefault(currentGrp, 0);

        double totalOutstanding = groupToOutstanding.getOrDefault(currentGrp, 0.0);

        double avgCenterSize = calculateAverage(memberCount, centerCount);

        double avgPortfolioSizePerCenter = calculateAverage(totalOutstanding, centerCount);

        addResultRow(currentGrp, centerCount, memberCount, loanCount, avgCenterSize, avgPortfolioSizePerCenter);
    }

    private double calculateAverage(double value, int count) {
        return count > 0 ? value / count : 0.0;
    }

    private void addResultRow(String currentGrp, int centerCount, int memberCount, int loanCount, double avgCenterSize,
            double avgPortfolioSizePerCenter) {

        List<String> row = new ArrayList<>();

        row.add(zoneName);
        row.add(regionName);
        row.add(divisionName);
        row.add(clusterName);
        row.add(branchCode);
        row.add(branchName);

        row.add(getGroupValue(currentGrp));

        row.add(String.valueOf(centerCount));
        row.add(String.valueOf(memberCount));
        row.add(String.valueOf(loanCount));
        row.add(String.format("%.2f", avgCenterSize));
        row.add(String.format("%.2f", avgPortfolioSizePerCenter));

        retvalues.add(String.join("*", row));
        outvalues.add(String.join(",", row));
    }

    private String getGroupValue(String currentGrp) {
        if (DATE_GROUP.equalsIgnoreCase(selGroup)) {
            return formatDate();
        }

        return currentGrp;
    }

    /**
     * @param currentGrp
     * @return
     */
    private String formatDate() {

        return null;
    }

    private void setDateFilter() {
        dateFilterVal = getEbFfParamRecDets(DEFAULT_REPORT_DATE, CENTER_MEETING);
    }

    private LocalDate getStartDate(LocalDate today) {
        if (startDate.isEmpty()) {
            return today;
        }

        return LocalDate.parse(startDate, t24DateFormatter);
    }

    private LocalDate getEndDate(LocalDate today) {
        if (endDate.isEmpty()) {
            return getEndDateBasedOnParamRec(dateFilterVal, today);
        }

        return LocalDate.parse(endDate, t24DateFormatter);
    }

    private void processFutureArrangements(Set<String> futureArrSet, LocalDate start, LocalDate end,
            Contract contract) {

        for (String arrId : futureArrSet) {
            processArrangement(arrId, start, end, contract);
        }
    }

    private void processArrangement(String arrId, LocalDate start, LocalDate end, Contract contract) {
        

            extracted(arrId, start, end, contract);
        
    }

    /**
     * @param arrId
     * @param start
     * @param end
     * @param contract
     */
    private void extracted(String arrId, LocalDate start, LocalDate end, Contract contract) {
        String dueDt = getDueDateFromCollectionDets(arrId, start, end);

        if (dueDt == null || dueDt.isEmpty()) {
            return;
        }

        resetArrangementDetails();

        contract.setContractId(arrId);

        getAaArrangementDets(arrId);
        getAaArrAccountDets(accNum);
        getBalanceDetails(contract);
     

    
    }

    private void resetArrangementDetails() {
        ro = "";
        product = "";
        centre = "";
        village = "";
        roName = "";
        productName = "";
        centreName = "";
        villageName = "";
    }

   
    public String getDueDateFromCollectionDets(String arrId, LocalDate start, LocalDate end) {

        try {

            EbFfCollectionDetsRecord ebFfCollectionDetsRecord = new EbFfCollectionDetsRecord(
                    da.getRecord(finMnemonic, "EB.FF.COLLECTION.DETS", "", arrId));

            customer = ebFfCollectionDetsRecord.getCustomer().getValue();

            collectionDetsCnts = ebFfCollectionDetsRecord.getDueDate().size();

            for (int i = 0; i < ebFfCollectionDetsRecord.getDueDate().size(); i++) {

                String dueDt = ebFfCollectionDetsRecord.getDueDate(i).getValue();

                if (dueDt != null && !dueDt.isEmpty()) {

                    LocalDate dueDtVal = LocalDate.parse(dueDt, t24DateFormatter);

                    if ((!dueDtVal.isBefore(start)) && (!dueDtVal.isAfter(end))) {

                        dueDate = dueDt;
                        return dueDate;
                    }
                }

            }

        } catch (Exception e) {
            e.getMessage();
        }
        return dueDate;

    }

    public void getAaArrangementDets(String arrId) {

        try {

            AaArrangementRecord arrRec = new AaArrangementRecord(
                    da.getRecord(finMnemonic, "AA.ARRANGEMENT", "", arrId));
            coCode = arrRec.getCoCodeRec().getValue();

            product = arrRec.getProduct().get(0).getProduct().getValue();

            getAaProductDetails(product);

            accNum = arrRec.getLinkedAppl().get(0).getLinkedApplId().getValue();

        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void getAaProductDetails(String productId) {
        try {
            AaProductRecord aaProRec = new AaProductRecord(da.getRecord("AA.PRODUCT", productId));
            productName = aaProRec.getDescription(0).getValue();
        } catch (Exception e) {
            e.getMessage();
        }
    }

    // Method for fetching company info

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

    public boolean chkSelectionBased() {
        return (selRo != null && !selRo.isEmpty() && !selRo.equals(ro))
                || (selProduct != null && !selProduct.isEmpty() && !selProduct.equals(product))
                || (selCenterName != null && !selCenterName.isEmpty() && !selCenterName.equals(centre))
                || (selVillage != null && !selVillage.isEmpty() && !selVillage.equals(village));
    }

    public List<String> processSortingForDateGrp(Map<String, Integer> activeLoanCount) {
        List<String> sortedKeys = new ArrayList<>(activeLoanCount.keySet());
        if (dateGrp) {
            Collections.sort(sortedKeys);
        }
        return sortedKeys;
    }

    // Method for group based value

    public String getGroupBasedValue(String accNum) {

        String groupKey = "";

        switch (selGroup) {
        case "BRANCH":
            selFilterField = "Branch";
            groupKey = getCompanyDescription(coCode);
            break;
        case "DATE":
            selFilterField = "Date";
            dueDateVal = dueDate;

            groupKey = dueDateVal;
            dateGrp = true;
            break;
        case "RO":
            selFilterField = "RO";

            getAaArrAccountDets(accNum);
            getEbFfRoUserDets(ro);

            groupKey = roName;
            break;
        case "PRODUCT":
            selFilterField = "Product";
            groupKey = productName;
            break;
        case "CENTER":

            selFilterField = "CenterName";

            groupKey = centreName;
            break;
        case "VILLAGE":
            selFilterField = "Village";

            getAaArrAccountDets(accNum);

            groupKey = villageName;
            break;
        default:
        }
        return groupKey;
    }

    // Method for filter criteria

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

                case "USER":
                    selUser = value;
                    break;

                case "GROUP.BY":
                    selGroup = value;
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

                case "VILLAGE":
                    selVillage = value;
                    break;
                default:
                }
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    /**
     * 
     */

    // Method for fetching centre, village & RO

    public void getAaArrAccountDets(String accNum) {

        try {
            AccountRecord accRec = new AccountRecord(da.getRecord(finMnemonic, "ACCOUNT", "", accNum));
            centre = accRec.getLocalRefField("FF.CENTRE").getValue();
            village = accRec.getLocalRefField("FF.VILLAGE").getValue();

            if (village != null && !village.isEmpty()) {
                getEbFfVillageDetails(village);

            }
            if (centre != null && !centre.isEmpty()) {
                getEbFfCentreDetails(centre);

            }

        } catch (Exception e) {
            e.getMessage();

        }
    }

    public void getEbFfVillageDetails(String village) {
        try {
            EbFfVillageRecord villageRec = new EbFfVillageRecord(da.getRecord("", "EB.FF.VILLAGE", "", village));
            villageName = villageRec.getVillageName().getValue();
        } catch (Exception e) {
            e.getMessage();
        }
    }

    // Method for fetching RO Name

    public String getEbFfCentreDetails(String centre) {

        try {
            EbFfCentreDetailRecord centreRec = new EbFfCentreDetailRecord(
                    da.getRecord("", "EB.FF.CENTRE.DETAIL", "", centre));
            ro = centreRec.getCurrentRo().getValue();
            centreName = centreRec.getCenterName().getValue();

        } catch (Exception e) {
            e.getMessage();
        }
        return ro;
    }

    public void getEbFfRoUserDets(String ro) {
        try {
            EbFfRoUserRecord roUserRec = new EbFfRoUserRecord(da.getRecord("", "EB.FF.RO.USER", "", ro));
            roName = roUserRec.getRoName().getValue();
        } catch (Exception e) {
            e.getMessage();
        }
    }

    // Method for parsing values of Double data type

    public static double safeParse(String val) {
        try {
            return (val == null || val.trim().isEmpty()) ? 0.0 : Double.parseDouble(val.trim());
        } catch (Exception e) {
            e.getMessage();
            return 0.0;
        }
    }

    public String getBalance(Contract contract, String accountType, String bookingType) {

        List<BalanceMovement> movements = null;
        try {

            movements = contract.getContractBalanceMovements(accountType, bookingType);

        } catch (Exception e) {
            e.getMessage();

        }

        return (movements != null && !movements.isEmpty()) ? movements.get(0).getBalance().toString() : "0";
    }

    public void getBalanceDetails(Contract contract) {

        try {

            String sumoutstanding = getBalance(contract, "FFPRINTAMT", REQ_TYPE_TRADE);

            outstanding = Math.abs(Double.parseDouble(sumoutstanding));

        } catch (NumberFormatException e) {
            e.getMessage();

        }
    }

    // Method for file generation

    public LocalDate getEndDateBasedOnParamRec(String paramValue, LocalDate stDt) {
        LocalDate expectedEndDt = null;
        try {
            if (paramValue.endsWith("D")) {
                int allowedDays = Integer.parseInt(paramValue.replace("D", ""));
                expectedEndDt = stDt.plusDays(allowedDays);

                return expectedEndDt;
            } else if (paramValue.endsWith("M")) {
                int allowedMonths = Integer.parseInt(paramValue.replace("M", ""));
                expectedEndDt = stDt.plusMonths(allowedMonths);

                return expectedEndDt;
            }
        } catch (NumberFormatException e) {
            e.getMessage();
        }
        return expectedEndDt;
    }

    public String getEbFfParamRecDets(String paramId, String paramName) {
        String paramVal = "";
        try {
            EbFfParameterRecord paramRec = new EbFfParameterRecord(da.getRecord(EB_FF_PARAMETER, paramId));
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
                                "Branch Code", "Branch Name", selFilterField, "Active center meeting Count",
                                "Active Member count", "Active loan count", "Average center size",
                                "Average portfolio size");
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
