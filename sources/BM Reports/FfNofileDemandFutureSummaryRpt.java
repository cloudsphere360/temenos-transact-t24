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

import com.ibm.icu.math.BigDecimal;
import com.temenos.api.TField;
import com.temenos.api.exceptions.T24CoreException;
import com.temenos.t24.api.arrangement.accounting.Contract;
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
import com.temenos.t24.api.records.ebffloandetails.CcProductClass;
import com.temenos.t24.api.records.ebffloandetails.EbFfLoanDetailsRecord;
import com.temenos.t24.api.records.ebffparameter.EbFfParameterRecord;
import com.temenos.t24.api.records.ebffparameter.ParamDescClass;
import com.temenos.t24.api.records.ebffrouser.EbFfRoUserRecord;
import com.temenos.t24.api.records.ebffvillage.EbFfVillageRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/*-----------------------------------------------------------------------------
* @author Kavin Prabha M
* Date Created:12.12.2025
* Attached as : NofileEnquiry Routine
* EB.API :FF.E.NOFILE.DE.FUTURE.SUM.RPT
* ENQUIRY:FF.DMD.FUT.RPT.SUM-------------NEW 06-MAY-2026
* STANDARD.SELECTION > NOFILE.FF.DE.FUTURE.SUM.RPT
* ENQUIRY.REPORT>FF.DMD.FUT.RPT.SUM
* VERSION>EB.FF.BM.REPORT.EXT,DMD.FUT.SUM
* Description: BM Online Report generation -> Demand Future Summary Report
*------------------------------------------------------------------------------ 
* Modification History :
*----------------------------------------------------------------------------- 
*12-DEC-2025   Development    Kavin Prabha M
*-----------------------------------------------------------------------------
*04-MAR-2026   Remapping and Grouping logicadded     Kavin Prabha M
*-----------------------------------------------------------------------------
*/
public class FfNofileDemandFutureSummaryRpt extends Enquiry {
    public static final String FUTURE_DATE_RANGE_ERR = "EB-FF.BM.FUTURE.MAX.DT.RANGE";
    public static final String DATE_FILTER_ERR = "EB-FF.BM.DATE.FILTER.RANGE";
    public static final String BOOKING = "BOOKING";
    public static final String NO_REC_ERR = "EB-NO.REC.SELECT";
    public static final String FILE_NAME = "DemandFutRep_Sum";
    private static final String AA_ARRANGEMENT_RECORD = "AA.ARRANGEMENT";
    DataAccess da = new DataAccess(this);
    private static final DateTimeFormatter formatter =DateTimeFormatter.ofPattern("yyyyMMdd");
    List<String> outArrList = new ArrayList<>();
    List<String> retvalues = new ArrayList<>();
    List<String> outvalues = new ArrayList<>();

    Set<String> filterValSet = new HashSet<>();
    Set<String> centreSet = new HashSet<>();
    Set<String> villageSet = new HashSet<>();
    Set<String> roSet = new HashSet<>();
    Set<String> productSet = new HashSet<>();

    String todayDate = "";
    String finMnemonic = "";
    String selDate = "";
    String selDateOp = "";
    String selMonth = "";
    String selRo = "";
    String selProduct = "";
    String selCenterName = "";
    String selVillage = "";
    String productLine = "";
    String startDate = "";
    String endDate = "";
    String selBranch = "";
    String branchName = "";
    String branchCode = "";
    String zoneName = "";
    String regionName = "";
    String divisionName = "";
    String clusterName = "";

    String coCode = "";
    CompanyRecord compyRec = null;
    String companyName = "";

    double principalDemand = 0.0;
    double interestDemand = 0.0;
    double feeDemand = 0.0;
    double totalDemand = 0.0;
    String roCount = "";
    String productCount = "";
    String centreCount = "";
    String villageCount = "";
    String loanStatus = "";
    AaArrangementRecord aaRec = null;
    AaArrangementRecord aaArrRecDets = null;
    boolean onlyDateFilter = false;
    boolean noRecErrFlag = false;
    boolean dateErrFlag = false;
    boolean dateFilterErrFlag = false;
    boolean dateRangeErrFlag = false;
    boolean branchSelection = false;
    String ro = "";
    String product = "";
    String centre = "";
    String village = "";
    String selectionFilter = "";
    boolean dateGrp = false;
    String date = "";
    String companyIds = "";
    String cashCarryDemand = "";
    String enquiryName = "";

    Double principalDemandBal = 0.0;
    Double interestDemandBal = 0.0;
    Double feeDemandBal = 0.0;
    Double cashCarryTotal = 0.0;
    Double totalDemandBal = 0.0;

    String mnemonic = "";
    String selFilterField = "";
    String dueDate = "";
    Session session = new Session(this);
    Map<String, Double> principalDemandmap = new HashMap<>();
    Map<String, Double> interestDemandmap = new HashMap<>();
    Map<String, Double> feeDemandmap = new HashMap<>();
    Map<String, Double> cashCarrymap = new HashMap<>();
    Map<String, Double> totalDemandmap = new HashMap<>();
    DateTimeFormatter outDateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HHmmss");
    String ccDate = "";
    String ccAmount = "";
    List<String> arrIdList = new ArrayList<>();
    String dateRangeVal = "";
    String dateFilterVal = "";
//TODAY:07/04/2026    
    String accNum = "";
    String productName = "";
    String villageName = "";
    String roName = "";
    String centreName = "";
    String selGroup = "";
    String selUser = "";

    @Override
    /**
     * METHOD:setIds DESCRIPTION:Entry point of the enquiry that processes filters,
     * generates report data, writes the CSV file, and returns the result list.
     */
    public List<String> setIds(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {

        todayDate = session.getCurrentVariable("!TODAY");
        Contract contract = new Contract(this);
        try {
            getFilterCriteriaDets(filterCriteria);
            processDateValidation(startDate, endDate);

            if (dateFilterErrFlag) {
                throw new T24CoreException(covertParamValue(dateFilterVal), DATE_FILTER_ERR);
            }
            if (dateRangeErrFlag) {
                throw new T24CoreException(covertParamValue(dateRangeVal), FUTURE_DATE_RANGE_ERR);
            }
            Set<String> demandFutureArrSetObj = new LinkedHashSet<>(da.selectRecords(finMnemonic, AA_ARRANGEMENT_RECORD,
                    "",
                    "WITH ARR.STATUS NE CLOSE AND ARR.STATUS NE PENDING.CLOSURE AND ARR.STATUS NE EXPIRED AND CO.CODE EQ "
                            + companyIds));

            LocalDate today = LocalDate.parse(todayDate, formatter);
            LocalDate start = startDate.isEmpty() ? today : LocalDate.parse(startDate, formatter);
            LocalDate end = endDate.isEmpty() ? getEndDateBasedOnParamRec(dateFilterVal, today)
                    : LocalDate.parse(endDate, formatter);
            for (String arrId : demandFutureArrSetObj) {
                getGroupingfield(contract, start, end, arrId);
            }
            List<String> sortedKeys = new ArrayList<>(principalDemandmap.keySet());
            if (dateGrp) {
                Collections.sort(sortedKeys);
            }
            for (String currentGrp : sortedKeys) {
                addingValues(currentGrp);
            }

            getCsvFile();
        } catch (T24CoreException e) {
            throw e;
        } catch (Exception e1) {
            e1.getMessage();
        }
        if (retvalues.isEmpty()) {
            throw new T24CoreException("", NO_REC_ERR);
        } else {
            return retvalues;
        }
    }

    /**
     * @param dateFilterVal2
     * @return
     */
    private String covertParamValue(String value) {
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

    /**
     * @param startDate2
     * @param endDate2
     */
    private void processDateValidation(String startDate, String endDate) {
        try {

            String dateParamId = "FF.BM.REPORT.DATE.RANGE";/// 1M
            String dateParamName = "DEMAND.FUTURE";
            String defDtParamId = "FF.BM.REPORT.DATE.DEFAULT";// 7DAYS

            dateRangeVal = getEbFfParamRecDets(dateParamId, dateParamName);
            dateFilterVal = getEbFfParamRecDets(defDtParamId, dateParamName);

            if ((startDate != null && !startDate.isEmpty()) && (endDate != null && !endDate.isEmpty())) {
                LocalDate todayDt = LocalDate.parse(todayDate, formatter);
                LocalDate stDt = LocalDate.parse(startDate, formatter);
                LocalDate endDt = LocalDate.parse(endDate, formatter);

                LocalDate expectedEndDt = getEndDateBasedOnParamRec(dateRangeVal, todayDt);

                boolean isRangeValid = !stDt.isBefore(todayDt) && !endDt.isAfter(expectedEndDt);
                if (!isRangeValid) {
                    dateRangeErrFlag = true;
                }

                LocalDate defExpEnddate = getEndDateBasedOnParamRec(dateFilterVal, stDt);
                if (endDt.isAfter(defExpEnddate)) {
                    dateFilterErrFlag = true;
                }
            }

        } catch (Exception e2) {
            e2.getMessage();
        }

    }

    /**
     * @param dateRangeVal2
     * @param todayDt
     * @return
     */
    private LocalDate getEndDateBasedOnParamRec(String paramValue, LocalDate stDt) {
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
        } catch (NumberFormatException e6) {
            e6.getMessage();
        }
        return expectedEndDt;
    }

    /**
     * @param dateParamId
     * @param dateParamName
     * @return
     */
    private String getEbFfParamRecDets(String paramId, String paramName) {
        String paramVal = "";
        try {
            EbFfParameterRecord paramRec = new EbFfParameterRecord(da.getRecord("EB.FF.PARAMETER", paramId));
            for (ParamDescClass paramDesc : paramRec.getParamDesc()) {
                if (paramDesc.getParamName().getValue().equals(paramName)) {
                    paramVal = paramDesc.getParamValue().getValue();
                    return paramVal;
                }
            }
        } catch (Exception e7) {
            e7.getMessage();
        }
        return paramVal;
    }

    /**
     * METHOD:getGroupingfield DESCRIPTION:Validates arrangement date range,
     * retrieves related details, determines grouping key, and updates demand values
     * for the report.
     */
    public void getGroupingfield(Contract contract, LocalDate start, LocalDate end, String arrId) {
        try {
            principalDemandBal = 0.0;
            interestDemandBal = 0.0;
            feeDemandBal = 0.0;
            cashCarryTotal = 0.0;
            totalDemandBal = 0.0;
            dueDate = getdueDateFromArrangement(arrId);
            if (dueDate != null && !dueDate.isEmpty()) {
                LocalDate dueDt = LocalDate.parse(dueDate, formatter);
                if ((dueDt.isEqual(start) || dueDt.isAfter(start)) && (dueDt.isEqual(end) || dueDt.isBefore(end))) {
                    arrIdList.add(arrId);
                    ro = "";
                    product = "";
                    centre = "";
                    village = "";
                    contract.setContractId(arrId);
                    getAaArrangementDets(arrId);
                    getAccountDets(accNum);
                    getEbFfCollectionDets(arrId, start, end);
                    getEbFfLoanDetails(arrId);
                    String groupValue = getGroupBasedValue(arrId);

                    boolean selectionCheck = chkSelectionBased();
                    if (selectionCheck) {
                        return;
                    }

                    if (groupValue != null && !groupValue.isEmpty()) {
                        getGroupingValues(groupValue);
                    }
                }
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    /**
     * METHOD:addingValues DESCRIPTION:Constructs a report row for the grouping key
     * and adds it to the enquiry result list and CSV output list.
     */
    public void addingValues(String currentGrp) {
        List<String> row = new ArrayList<>();
        row.add(zoneName);
        row.add(regionName);
        row.add(divisionName);
        row.add(clusterName);
        row.add(branchCode);
        row.add(branchName);
        if (dateGrp) {
            row.add(convertDateFormat(currentGrp));
        } else {
            row.add(currentGrp);
        }
        row.add(String.format("%.2f", principalDemandmap.get(currentGrp)));
        row.add(String.format("%.2f", interestDemandmap.get(currentGrp)));
        row.add(String.format("%.2f", feeDemandmap.get(currentGrp)));
        row.add(String.format("%.2f", cashCarrymap.get(currentGrp)));
        row.add(String.format("%.2f", totalDemandmap.get(currentGrp)));

        retvalues.add(String.join("*", row));
        outvalues.add(String.join(",", row));
    }
    /**
     * convertDateFormat DESCRIPTION:
     */   
    public static String convertDateFormat(String inputDate) {

        if (inputDate == null || inputDate.isEmpty()) {
            return "";
        }

        try {
            DateTimeFormatter inputFormatter =
                    DateTimeFormatter.ofPattern("yyyyMMdd");

            DateTimeFormatter outputFormatter =
                    DateTimeFormatter.ofPattern("dd-MM-yyyy");

            return LocalDate.parse(inputDate, inputFormatter)
                            .format(outputFormatter);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return inputDate;
    }
    /**
     * METHOD:getCsvFile DESCRIPTION:Retrieves the output file path from
     * EB.FF.PARAMETER and generates the CSV file name using report details
     */
    public void getCsvFile() {
        String fileParamId = "FF.BM.REPORT.EXTRACT";
        String fileParamName = "Path";
        String filePath = getEbFfParamRecDets(fileParamId, fileParamName);

        LocalDateTime currDtTime = LocalDateTime.now();
        String currDate = currDtTime.format(outDateFormatter);
        String currTime = currDtTime.format(timeFormatter);
        String outputPath = filePath + FILE_NAME + "_" + selGroup + "-WISE" + "_" + branchName + "_" + selUser + "_"
                + currDate + "_" + currTime + ".csv";
        writeToFile(outvalues, outputPath);
    }

    /**
     * METHOD:getGroupingValues DESCRIPTION:Updates demand maps by accumulating
     * values based on the grouping key for report generation.
     */
    public void getGroupingValues(String groupValue) {
        principalDemandmap.put(groupValue,
                principalDemandmap.getOrDefault(groupValue, 0.0) + Math.abs(principalDemandBal));
        interestDemandmap.put(groupValue,
                interestDemandmap.getOrDefault(groupValue, 0.0) + Math.abs(interestDemandBal));
        feeDemandmap.put(groupValue, feeDemandmap.getOrDefault(groupValue, 0.0) + Math.abs(feeDemandBal));
        cashCarrymap.put(groupValue, cashCarrymap.getOrDefault(groupValue, 0.0) + Math.abs(cashCarryTotal));
        totalDemandmap.put(groupValue, totalDemandmap.getOrDefault(groupValue, 0.0) + Math.abs(totalDemandBal));

    }

    /**
     * METHOD:getEbFfLoanDetails DESCRIPTION:Fetches loan details and calculates the
     * Cash Carry total based on CC.DATE and CC.AMOUNT.
     */
    private void getEbFfLoanDetails(String arrId) {
        try {
            todayDate = session.getCurrentVariable("!TODAY");
            LocalDate today = LocalDate.parse(todayDate, formatter);
            EbFfLoanDetailsRecord loanDetRec = new EbFfLoanDetailsRecord(
                    da.getRecord(finMnemonic, "EB.FF.LOAN.DETAILS", "", arrId));
            if (loanDetRec.getCcProduct() == null || loanDetRec.getCcProduct().isEmpty()) {
                return;
            }
            for (CcProductClass productobj : loanDetRec.getCcProduct()) {
                ccDate = productobj.getCcDate().getValue();
                if (ccDate != null && !ccDate.isEmpty()) {
                    LocalDate ccDatestr = LocalDate.parse(ccDate, formatter);
                    if (!ccDatestr.isAfter(today)) {
                        ccAmount = productobj.getCcAmount().getValue();
                        if (ccAmount != null && !ccAmount.trim().isEmpty()) {
                            cashCarryTotal += Double.parseDouble(ccAmount);
                        }

                    }
                }
            }
        } catch (Exception e2) {
            e2.getMessage();
        }
    }

    /**
     * METHOD:getAaArrangementDets DESCRIPTION: Fetches arrangement details and sets
     * product and company info.
     */
    private void getAaArrangementDets(String arrId) {
        try {
            AaArrangementRecord arrRec = new AaArrangementRecord(
                    da.getRecord(finMnemonic, AA_ARRANGEMENT_RECORD, "", arrId));
            coCode = arrRec.getCoCodeRec().getValue();
            product = arrRec.getProduct().get(0).getProduct().getValue();
            accNum = arrRec.getLinkedAppl().get(0).getLinkedApplId().getValue();
            getAaProductDetails(product);
        } catch (Exception e3) {
            e3.getMessage();
        }
    }

    /**
     * @param product2
     */
    private void getAaProductDetails(String productId) {
        try {
            AaProductRecord aaProRec = new AaProductRecord(da.getRecord("AA.PRODUCT", productId));
            productName = aaProRec.getDescription(0).getValue();
        } catch (Exception e) {
            e.getMessage();
        }

    }

    /**
     * METHOD:getAccountDets DESCRIPTION: This method is used to get the values.
     */
    public void getAccountDets(String accNum) {
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

    /**
     * @param village2
     */
    private void getEbFfVillageDetails(String village) {
        try {
            EbFfVillageRecord villageRec = new EbFfVillageRecord(da.getRecord("", "EB.FF.VILLAGE", "", village));
            villageName = villageRec.getVillageName().getValue();
        } catch (Exception e) {
            e.getMessage();
        }

    }

    /**
     * METHOD:getEbFfCentreDetails DESCRIPTION: This method is used to get the
     * values from roName.
     */
    private void getEbFfCentreDetails(String centre) {

        try {
            EbFfCentreDetailRecord centreRec = new EbFfCentreDetailRecord(
                    da.getRecord("", "EB.FF.CENTRE.DETAIL", "", centre));
            centreName = centreRec.getCenterName().getValue();
            ro = centreRec.getCurrentRo().getValue();
            if (ro != null && !ro.isEmpty()) {
                getEbFfRoUserDets(ro);
            }

        } catch (Exception e5) {
            e5.getMessage();
        }

    }

    /**
     * @param ro2
     */
    private void getEbFfRoUserDets(String ro) {
        try {
            EbFfRoUserRecord roUserRec = new EbFfRoUserRecord(da.getRecord("", "EB.FF.RO.USER", "", ro));
            roName = roUserRec.getRoName().getValue();
        } catch (Exception e) {
            e.getMessage();
        }
    }

    /**
     * METHOD:getdueDateFromArrangement DESCRIPTION: This method is used to get the
     * values from futStartDate.
     */
    private String getdueDateFromArrangement(String arrId) {
        String furduetDate = "";
        try {

            EbFfCollectionDetsRecord colldetsRec = new EbFfCollectionDetsRecord(
                    da.getRecord(finMnemonic, "EB.FF.COLLECTION.DETS", "", arrId));
            LocalDate today = LocalDate.parse(todayDate, formatter);
            LocalDate next7Days = today.plusDays(7);

            for (int i = 0; i < colldetsRec.getDueDate().size(); i++) {

                String dueDt = colldetsRec.getDueDate(i).getValue();

                if (dueDt != null && !dueDt.isEmpty()) {

                    LocalDate dueDateVal =
                            LocalDate.parse(dueDt, formatter);

                    // Check whether due date falls within next 7 days
                    if ((!dueDateVal.isBefore(today))
                            && (!dueDateVal.isAfter(next7Days))) {

                        furduetDate = dueDt;
                        break;
                    }
                }
            }

        } catch (Exception e6) {
            e6.getMessage();
        }
        return furduetDate;
    }

    /**
     * METHOD:chkSelectionBased DESCRIPTION: Validates arrangement against selected
     * filters and returns
     */
    private boolean chkSelectionBased() {
        return (selRo != null && !selRo.isEmpty() && !selRo.equals(ro))
                || (selProduct != null && !selProduct.isEmpty() && !selProduct.equals(product))
                || (selCenterName != null && !selCenterName.isEmpty() && !selCenterName.equals(centre))
                || (selVillage != null && !selVillage.isEmpty() && !selVillage.equals(village));

    }

    /**
     * METHOD:getGroupBasedValue DESCRIPTION: This method determines the grouping
     * key for the report based on the selected grouping filter (DATE, RO, PRODUCT,
     * CENTERNAME, or VILLAGE).
     */
    private String getGroupBasedValue(String arrId) {
        String groupKey = "";
        switch (selGroup) {
        case "BRANCH":
            selFilterField = "Branch";
            branchSelection = true;
            groupKey = getCompanyDescription(coCode);
            break;

        case "DATE":
            selFilterField = "Date";
            dueDate = getdueDateFromArrangement(arrId);
            groupKey = dueDate;
            dateGrp = true;
            break;
        case "RO":
            selFilterField = "RO";
            getAccountDets(accNum);
            groupKey = roName;
            break;
        case "PRODUCT":
            selFilterField = "Product";
            groupKey = productName;
            break;
        case "CENTER":
            selFilterField = "CenterName";
            getAccountDets(accNum);
            groupKey = centreName;
            break;
        case "VILLAGE":
            selFilterField = "Village";
            getAccountDets(accNum);
            groupKey = villageName;
            break;
        default:
        }
        return groupKey;

    }

    /**
     * METHOD:getFilterCriteriaDets DESCRIPTION: This method retrieves the filter
     * criteria provided in the enquiry input and assigns the corresponding values.
     */
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
        } catch (Exception e7) {
            e7.getMessage();
        }
    }

    /**
     * METHOD:initialiseCompanyInfo DESCRIPTION: This method is used to get the
     * values.
     */
    private void initialiseCompanyInfo(String companyId) {
        try {

            CompanyRecord companyObj = new CompanyRecord(da.getRecord("COMPANY", companyId));
            finMnemonic = companyObj.getFinancialMne().getValue();
            mnemonic = companyObj.getCustomerMnemonic().getValue();
            branchCode = companyId;
            branchName = getCompanyDescription(companyId);
            zoneName = getCompanyDescription(companyObj.getLocalRefField("FF.ZONE").getValue());
            regionName = getCompanyDescription(companyObj.getLocalRefField("FF.REGION").getValue());
            divisionName = getCompanyDescription(companyObj.getLocalRefField("FF.DIVISION").getValue());
            clusterName = getCompanyDescription(companyObj.getLocalRefField("FF.CLUSTER").getValue());
        } catch (Exception e8) {
            e8.getMessage();
        }

    }

    /**
     * METHOD:getCompanyDescription DESCRIPTION: This method is used to get the
     * values and split using.
     */
    private String getCompanyDescription(String companyCode) {
        try {
            if (companyCode != null && !companyCode.isEmpty()) {
                CompanyRecord companyRec = new CompanyRecord(da.getRecord("COMPANY", companyCode));
                companyName = companyRec.getCompanyName().get(0).getValue();
                String[] compNamePart = companyName.split("-");
                companyName = compNamePart[0];
                return companyName;
            }
        } catch (Exception e9) {
            e9.getMessage();
        }
        return companyName;

    }

    /**
     * METHOD:getLinkedCompIds DESCRIPTION: This method is used to get the values.
     */
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

        } catch (Exception e10) {
            e10.getMessage();
        }

    }

    /**
     * METHOD:getEbFfCollectionDets DESCRIPTION: This method is used to get the
     * values.
     * 
     * @param end
     * @param start
     */
    private void getEbFfCollectionDets(String arrId, LocalDate start, LocalDate end) {
        principalDemandBal = 0.0;
        interestDemandBal = 0.0;
        feeDemandBal = 0.0;
        totalDemandBal = 0.0;

        BigDecimal duePrincipal = BigDecimal.ZERO;
        BigDecimal dueInterest = BigDecimal.ZERO;
        BigDecimal dueFee = BigDecimal.ZERO;

        try {
            EbFfCollectionDetsRecord ffCollDetsRec = new EbFfCollectionDetsRecord(
                    da.getRecord(finMnemonic, "EB.FF.COLLECTION.DETS", "", arrId));
            for (int i = 0; i < ffCollDetsRec.getDueDate().size(); i++) {

                dueDate = ffCollDetsRec.getDueDate().get(i).getValue();
                if (dueDate != null && !dueDate.isEmpty()) {
                    LocalDate dueDateVal = LocalDate.parse(dueDate, formatter);
                    if (!dueDateVal.isBefore(start) && !dueDateVal.isAfter(end)) {
                        String principalAmt = ffCollDetsRec.getPrincipalAmt(i).getValue();
                        String interestAmt = ffCollDetsRec.getInterestAmt().get(i).getValue();
                        String feeAmt = ffCollDetsRec.getChargeAmt().get(i).getValue();

                        principalAmt = (principalAmt == null || principalAmt.trim().isEmpty()) ? "0" : principalAmt;
                        interestAmt = (interestAmt == null || interestAmt.trim().isEmpty()) ? "0" : interestAmt;
                        feeAmt = (feeAmt == null || feeAmt.trim().isEmpty()) ? "0" : feeAmt;

                        duePrincipal = duePrincipal.add(new BigDecimal(principalAmt));
                        dueInterest = dueInterest.add(new BigDecimal(interestAmt));
                        dueFee = dueFee.add(new BigDecimal(feeAmt));

                    }
                }
            }
            principalDemandBal = duePrincipal.doubleValue();
            interestDemandBal = dueInterest.doubleValue();
            feeDemandBal = dueFee.doubleValue();
            totalDemandBal = principalDemandBal + interestDemandBal;

        } catch (Exception e12) {
            e12.getMessage();
        }

    }

    /**
     * METHOD:writeToFile DESCRIPTION: his method creates the report output file in
     * the specified file path and writes the report data into it.
     */
    private void writeToFile(List<String> data, String filePath) {
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
                                "BranchCode", "BranchName", selFilterField, "Principal Demand", "Interest Demand",
                                "Fee Demand", "Cash Carry Demand", "Total Demand");

                        writer.write(header + System.lineSeparator());
                    }
                    for (String line : data) {
                        writer.write(line + System.lineSeparator());
                    }
                }
            }
        } catch (Exception e13) {
            e13.getMessage();
        }

    }
}
