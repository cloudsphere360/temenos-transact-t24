package com.temenos.fusion;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import com.temenos.api.TField;
import com.temenos.api.exceptions.T24CoreException;
import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.t24.api.records.aaaccountdetails.AaAccountDetailsRecord;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaarrangement.LinkedApplClass;
import com.temenos.t24.api.records.aaarrtermamount.AaArrTermAmountRecord;
import com.temenos.t24.api.records.aacustomerarrangement.AaCustomerArrangementRecord;
import com.temenos.t24.api.records.aacustomerarrangement.ArrangementClass;
import com.temenos.t24.api.records.aacustomerarrangement.ProductLineClass;
import com.temenos.t24.api.records.aaprddesaccount.AaPrdDesAccountRecord;
import com.temenos.t24.api.records.aaprddesaccount.AltIdTypeClass;
import com.temenos.t24.api.records.aaprddescharge.AaPrdDesChargeRecord;
import com.temenos.t24.api.records.aaprddesinterest.AaPrdDesInterestRecord;
import com.temenos.t24.api.records.company.CompanyRecord;
import com.temenos.t24.api.records.companyconsol.CompanyConsolRecord;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.records.ebffcentredetail.EbFfCentreDetailRecord;
import com.temenos.t24.api.records.ebffcollectiondets.EbFfCollectionDetsRecord;
import com.temenos.t24.api.records.ebffgroups.EbFfGroupsRecord;
import com.temenos.t24.api.records.ebffloandetails.AddressTypeClass;
import com.temenos.t24.api.records.ebffloandetails.EbFfLoanDetailsRecord;
import com.temenos.t24.api.records.ebffloandetails.FmEntityNumberClass;
import com.temenos.t24.api.records.ebffparameter.EbFfParameterRecord;
import com.temenos.t24.api.records.ebffparameter.ParamDescClass;
import com.temenos.t24.api.records.ebffrouser.EbFfRoUserRecord;
import com.temenos.t24.api.records.user.UserRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;
import com.temenos.t24.api.records.aaprddespaymentschedule.AaPrdDesPaymentScheduleRecord;
import com.temenos.t24.api.records.aaprddespaymentschedule.PaymentTypeClass;

/**
 * @author Kavin Prabha Date Created: 12.12.2025 Attached as
 *         :NofileEnquiryRoutine EB.API : EB.FF.DISB.FEE.INS
 *         STANDARD.SELECTION >NOFILE.FF.DISB.FEE.INSURANCE.REPORT Description: BM  
 *         Online Report generation ->Disbursement-Fee-Insurance Detail Report
 *         Modification History : Initial Draft
 *
 *
 *         12-DEC-2025 Development Initial Version
 *         -----------------------------------------------------------------------------
 *         12-Feb-2026 Remapping Field added Renuka M 11-Mar-2026 Sonar Testing
 *         Company & Branch wise added Sathish Kumar N B 25-Mar-2026 Updated to
 *         handle DATE.FROM added Sathish Kumar N B and DATE.TO separate
 *         selection fields
 * 
 * 
 */
public class FfNofileDisbFee extends Enquiry {
    
    
    public static final String DATE_RANGE_ERR = "EB-FF.DATE.RANGE.GREATER";
    public static final String NO_REC_ERR = "EB-NO.REC.SELECT";
    public static final String SEL_APP_CUS = "CUSTOMER";
    private static final String AA_ARRANGEMENT_TBL = "AA.ARRANGEMENT";
    private static final String AA_ARR_ACCOUNT_TBL = "AA.ARR.ACCOUNT";
    public static final String SEL_APP_ACC = "ACCOUNT";
    public static final String SEL_APP_COMP = "COMPANY";
    public static final String EB_FF_PARAMETER = "EB.FF.PARAMETER";
    List<String> outvalues = new ArrayList<>();
    DateTimeFormatter outDateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HHmmss");
    public static final String FILE_NAME = "DisFeeInsuranceRep_Det";
    Session session = new Session(this);

    DataAccess da = new DataAccess(this);
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    boolean legacy = false;
    String legacyAcctnum = "";
    String todayDate = "";
    String finMnemonic = "";
    String cusmnemonic = "";
    String branchName = "";
    String zoneName = "";
    String branchCode = "";
    String regionName = "";
    String divisionName = "";
    String clusterName = "";
    Set<String> filterValSet = new HashSet<>();
    Set<String> finalArrIdList = new HashSet<>();
    String selDate = "";
    String selDateOp = "";
    String startDate = "";
    String endDate = "";
    boolean dateErrFlag = false;
    boolean noRecErrFlag = false;
    String selProduct = "";
    String selCentreName = "";
    String selVillage = "";
    String selDistrict = "";
    String selLoanCycle = "";
    String selLoanPurp = "";
    String selCaste = "";
    String product = "";

    String selReligGrp = "";
    String startDateAccDet = "";
    LocalDate startDateArrAcc;
    String coCode = "";
    String companyName = "";
    String cusId = "";
    String givenName = "";
    String familyName = "";
    String customerName = "";
    String branchDistrict = "";
    String customerAge = "";
    String religGrp = "";
    String fmOccup = "";
    String loanPurp = "";
    List<LinkedApplClass> linkedAppList = null;
    List<ProductLineClass> productLineList = null;
    String accNum = "";
    AaArrTermAmountRecord aaArrTermAmt = null;
    String loanAmount = "";
    String loanCycle = "";
    String officerCode = "";

    AaArrangementRecord aaArrRec = null;
    CompanyRecord companyRec = null;
    CustomerRecord cusRec = null;

    List<String> returnVal = new ArrayList<>();
    List<String> finalArrayList = new ArrayList<>();

    String productDet = "";
    String relationShipOfficerName = "";
    String relationshipOfficerMobileNumber = "";
    String branchManagerName = "";
    String branchManagerMobileNumber = "";

    String groupCode = "";
    String groupName = "";
    String caste = "";
    String disbursementMode = "";
    String insuranceAmount = "";
    String lpfAmount = "";
    String fixedRate = "";
    String firstDueDate = "";
    String lastDueDate = "";
    String insuranceName = "";
    String downpaymentName = "";
    String repaymentFrequency = "";
    String centerCode = "";
    String centerName = "";
    String companyId = "";
    String branch = "";
    String displayDate = "";
    String companyIds = "";
    String branchState = "";
    boolean onlyDateFilter = false;
    String loanTenure = "";
    String centerId = "";
    String ebgroupName = "";
    String mnemonic;
    String fstName = "";
    String scdName = "";
    String origContractDate = "";
    String disbursementDate = "";
    int years;
    int months;
    int weeks;
    int days;
    int fortnights;
    String datedefaultRange;
    int defaultDate;
    int daterange;
    String pastMonth;
    String customerNumber = "";
    LocalDate orgContractDate;
    String t24StartDt = "";
    String selUser = "";
    String selGroup = "";

    /**
     * The main entry point for the T24 Enquiry. Initializes the session, handles
     * branch/company logic, and orchestrates the processing of arrangement records.
     */
    @Override
    public List<String> setIds(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {
       
        try {
            outvalues.clear();
            returnVal.clear();
            finalArrayList.clear();
            todayDate = session.getCurrentVariable("!TODAY");
            this.companyId = session.getCompanyId();
            Contract contract = new Contract(this);
            branch = this.companyId;
            // Standard Branch/Company Initialization
            for (FilterCriteria filter : filterCriteria) {
                if ("BRANCH".equals(filter.getFieldname())) {
                    branch = filter.getValue();
                    break;
                }
            }
            initialiseCompanyInfo(branch);
            getLinkedCompIds(branch);

            // --- NEW PARAMETER FETCHING LOGIC ---

            String filePath = "";
            

            String paramId = "FF.BM.REPORT.EXTRACT";

            EbFfParameterRecord paramRec = new EbFfParameterRecord(da.getRecord(EB_FF_PARAMETER, paramId));
            

            for (ParamDescClass paramDesc : paramRec.getParamDesc()) {
                String pName = paramDesc.getParamName().getValue();
                String pVal = paramDesc.getParamValue().getValue();

                if ("Path".equals(pName)) {
                    filePath = pVal;
                }

            }
            // ------------------------------------

            Set<String> selectionSet = getFilterCriteriaDets(filterCriteria);
            
            Set<String> arrList = new LinkedHashSet<>(da.selectRecords("", AA_ARRANGEMENT_TBL, "",
                    "WITH ARR.STATUS NE EXPIRED AND ARR.STATUS NE MATURED AND CO.CODE EQ " + companyIds));
            
            getDefaultDatRange();
            getMaxdateRangedet();

            Set<String> preFinalSet = getpreFinalset(arrList, selectionSet);
            preFinalSet.retainAll(arrList);

            // Pass the dynamic values into your validation method
            validateDateRange(daterange, defaultDate);

            LocalDate today = LocalDate.parse(todayDate, formatter);
            LocalDate start = startDate.isEmpty() ? today.minusDays(defaultDate)
                    : LocalDate.parse(startDate, formatter);
            LocalDate end = endDate.isEmpty() ? today : LocalDate.parse(endDate, formatter);

            for (String selectionArrId : preFinalSet) {
                processSingleArrangement(selectionArrId, start, end, contract);
            }

           
                LocalDateTime currDtTime = LocalDateTime.now();
                String currDate = currDtTime.format(outDateFormatter);
                String currTime = currDtTime.format(timeFormatter);

                // Use the filePath we already fetched at the top
                String outputPath = filePath + FILE_NAME + "_" + branchName + "_" + selUser + "_" + currDate
                        + "_" + currTime + ".csv";
               

                writeToFile(outvalues, outputPath);
            

        } catch (T24CoreException te) {
            // Log exactly what error is being sent to the browser

            throw te;
        } catch (Exception e1) {
            // Log if the code crashed for a technical reason (NullPointer, etc.)

            e1.printStackTrace();
           
        }
        return returnVal;

    }

    private void getDefaultDatRange() {

        String paramId = "FF.BM.REPORT.DATE.DEFAULT";

        EbFfParameterRecord paramRec = new EbFfParameterRecord(da.getRecord(EB_FF_PARAMETER, paramId));

        for (ParamDescClass paramDesc : paramRec.getParamDesc()) {

            if (paramDesc.getParamName().getValue().equals("DISB.FEE.INSURANCE")) {

                datedefaultRange = paramDesc.getParamValue().getValue();

                if (datedefaultRange.contains("1M")) {

                    defaultDate = 31;

                }

            }

        }

    }

    private void getMaxdateRangedet() {

        String paramId = "FF.BM.REPORT.DATE.RANGE";

        EbFfParameterRecord paramRec = new EbFfParameterRecord(da.getRecord(EB_FF_PARAMETER, paramId));

        for (ParamDescClass paramDesc : paramRec.getParamDesc()) {

            if (paramDesc.getParamName().getValue().equals("DISB.FEE.INSURANCE")) {

                pastMonth = paramDesc.getParamValue().getValue();

                if (pastMonth.contains("3M")) {

                    daterange = 90;

                }

            }

        }

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

    /**
     * 
     */
    private void validateDateRange(int maxHistory, int maxRange) {
        LocalDate today = LocalDate.parse(todayDate, formatter);
        LocalDate maxBackDate = today.minusDays(maxHistory);

        // LOGGER: Check what parameters and inputs the method is receiving

        if (startDate.isEmpty() && endDate.isEmpty()) {
            this.startDate = today.minusDays(defaultDate).format(formatter);
            this.endDate = today.format(formatter);

            return;
        }   

        LocalDate stDt = startDate.isEmpty() ? today : LocalDate.parse(startDate, formatter);
        LocalDate endDt = endDate.isEmpty() ? today : LocalDate.parse(endDate, formatter);

        if (stDt.isAfter(endDt)) {

            LocalDate tmp = stDt;
            stDt = endDt;
            endDt = tmp;
        }

        long daysBetween = ChronoUnit.DAYS.between(stDt, endDt);

        if (stDt.isBefore(maxBackDate)) {

            throw new T24CoreException(covertParamValue(pastMonth), "EB-FF.BM.PAST.MAX.DT.RANGE");
        }

        if (daysBetween > maxRange) {

            throw new T24CoreException(covertParamValue(datedefaultRange), "EB-FF.BM.DATE.FILTER.RANGE");
        }

        this.startDate = stDt.format(formatter);
        this.endDate = endDt.format(formatter);
    }

    /**
     * Evaluates a single Arrangement ID. Validates the date range and, if
     * successful, triggers all data collection methods to build a report row.
     */
    private void processSingleArrangement(String selectionArrId, LocalDate start, LocalDate end, Contract contract) {
        try {
            AaAccountDetailsRecord aaAccountDet = new AaAccountDetailsRecord(
                    da.getRecord(finMnemonic, "AA.ACCOUNT.DETAILS", "", selectionArrId));
            AaArrangementRecord arrangement = new AaArrangementRecord(
                    da.getRecord(finMnemonic, AA_ARRANGEMENT_TBL, "", selectionArrId));

            // Guard Clause: Exit early if records are missing
            if (aaAccountDet.toString().isEmpty() || arrangement.toString().isEmpty()) {
                return;
            }

            // 1. Determine if the arrangement matches the date criteria
            if (isArrangementInDateRange(aaAccountDet, arrangement, start, end)) {
                // 2. Perform the actual data processing
                executeDataCollection(selectionArrId, contract);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Extracted logic for prioritized date matching. Complexity reduction: Handled
     * via private boolean method.
     */
    private boolean isArrangementInDateRange(AaAccountDetailsRecord accDet, AaArrangementRecord arrRec, LocalDate start,
            LocalDate end) {
        String legacyStr = arrRec.getOrigContractDate().getValue();
        String liveStr = accDet.getStartDate().getValue();

        LocalDate legacyDate = (legacyStr != null && !legacyStr.isEmpty()) ? LocalDate.parse(legacyStr, formatter)
                : null;
        LocalDate liveDate = (liveStr != null && !liveStr.isEmpty()) ? LocalDate.parse(liveStr, formatter) : null;

        // Prioritized Check: If legacy exists, ignore live date
        if (legacyDate != null) {
            boolean match = !legacyDate.isBefore(start) && !legacyDate.isAfter(end);
            if (match) {
                this.legacy = true;
                this.disbursementDate = legacyStr;
            }
            return match;
        }

        // Fallback: Check live date
        if (liveDate != null) {
            boolean match = !liveDate.isBefore(start) && !liveDate.isAfter(end);
            if (match) {
                this.legacy = false;
                this.disbursementDate = liveStr;
            }
            return match;
        }

        return false;
    }

    /**
     * Extracted orchestration logic for calling sub-routines.
     */
    private void executeDataCollection(String arrangementId, Contract contract) {
        finalArrayList.add(arrangementId);
        contract.setContractId(arrangementId);

        getArrAccountFieldMappingDet(contract);
        getArrangementFieldMappingDet(arrangementId);
        getArrTermAmountDet(contract);
        getValuesFromInterestTable(contract);
        getValuesFromEbCollectionDets(arrangementId);
        getAaPrdDesPaymentScheduleDetails(contract);
        getEbFfLoanDetails(arrangementId);
        getAaArrAccountDetails(contract);

        buildAndAddRow();
    }

    /**
     * @param contract
     */
    private void getAaPrdDesPaymentScheduleDetails(Contract contract) {
        try {
            AaPrdDesPaymentScheduleRecord aaPrdPay = new AaPrdDesPaymentScheduleRecord(
                    contract.getConditionForProperty("PAYMENT.SCHEDULE"));

            List<PaymentTypeClass> paymentTypeList = aaPrdPay.getPaymentType();

            for (PaymentTypeClass paymentType : paymentTypeList) {
                // Filter for the actual repayment frequency
                if (paymentType.getPaymentType().getValue().equals("CONSTANT")
                        && paymentType.getPaymentMethod().getValue().equals("DUE")) {

                    String rawValue = paymentType.getPaymentFreq().getValue();

                    if (rawValue != null && !rawValue.isEmpty()) {
                        // Apply the parser logic
                        repaymentFrequency = parsePaymentFrequency(rawValue);
                        break;
                    }

                }
            }
        } catch (Exception e) {
            e.getMessage();
            repaymentFrequency = "OTHERS";
        }
    }

    private String parsePaymentFrequency(String rawValue) {
        years = months = weeks = days = fortnights = 0;

        for (String val : rawValue.split("\\s+")) {
            if (val.startsWith("e")) {
                processToken(val.substring(1));
            } else {
                processToken(val);
            }
        }
        return mapToExpectedFormat();
    }

    private String mapToExpectedFormat() {
        // 2 Weeks OR 14 Days -> 2W - FORTNIGHTLY
        if ((weeks == 2 && months == 0 && years == 0) || (days == 14 && months == 0)) {
            return "FORTNIGHTLY";
        }

        // 4 Weeks OR 28 Days -> 4W - ONCE EVERY 28 DAYS
        if ((weeks == 4 && months == 0 && years == 0) || (days == 28 && months == 0)) {
            return "ONCE EVERY 28 DAYS";
        }

        // 1 Month -> 1M - MONTHLY
        if (months == 1 && years == 0 && weeks == 0 && days == 0) {
            return "MONTHLY";
        }

        return "OTHERS";
    }

    private void processToken(String cleaned) {
        String numberPart = cleaned.replaceAll("\\D", "");
        String unitPart = cleaned.replaceAll("\\d", "");

        if (numberPart.isEmpty())
            return;

        int number = Integer.parseInt(numberPart);

        switch (unitPart) {
        case "Y":
            years = number;
            break;
        case "M":
            months = number;
            break;
        case "W":
            weeks = number;
            break;
        case "D":
            days = number;
            break;
        case "F":
            fortnights = number;
            break;
        default:
            break;
        }
    }

    /**
     * @param contract
     */
    private void getArrAccountFieldMappingDet(Contract contract) {
        loanPurp = "";
        loanCycle = "";
        disbursementMode = "";
        centerCode = "";
        centerName = "";
        try {
            AaPrdDesAccountRecord aaArrAccRec = new AaPrdDesAccountRecord(
                    contract.getConditionForProperty(SEL_APP_ACC));
            loanPurp = aaArrAccRec.getLocalRefField("FF.LOAN.PURP").getValue();
            loanCycle = aaArrAccRec.getLocalRefField("FF.LOAN.CYCLE").getValue();
            disbursementMode = aaArrAccRec.getLocalRefField("FF.DISB.MODE").getValue();
            insuranceName = aaArrAccRec.getLocalRefField("FF.INSUR.COMP").getValue();

        } catch (Exception e13) {
            e13.getMessage();
        }

    }

    /**
     * @param contract
     */
    private void getAaArrAccountDetails(Contract contract) {

        try {
            List<String> aaArrAccountPrptyList = new ArrayList<>();
            aaArrAccountPrptyList.add(SEL_APP_ACC);
            aaArrAccountPrptyList.add("LOANACCOUNT");
            for (String aaArrAcctid : aaArrAccountPrptyList) {
                AaPrdDesAccountRecord aaPrdDesAccountRecord = new AaPrdDesAccountRecord(
                        contract.getConditionForProperty(aaArrAcctid));

                if (legacy) {
                    for (AltIdTypeClass altType : aaPrdDesAccountRecord.getAltIdType()) {
                        if (altType.getAltIdType().getValue().equals("LEGACY")) {
                            legacyAcctnum = altType.getAltId().getValue();

                        }
                    }
                }
                String arrAccId = aaPrdDesAccountRecord.getIdComp1().getValue() + "-"
                        + aaPrdDesAccountRecord.getIdComp2().getValue() + "-"
                        + aaPrdDesAccountRecord.getIdComp3().getValue();
                AaPrdDesAccountRecord aaArrAccRec = new AaPrdDesAccountRecord(
                        da.getRecord(finMnemonic, AA_ARR_ACCOUNT_TBL, "", arrAccId));
                centerId = aaArrAccRec.getLocalRefField("FF.CENTRE").getValue();
                getEbFfCentreDetails(centerId);
                groupCode = aaArrAccRec.getLocalRefField("FF.GROUP").getValue();
                getffgroupName(groupCode);

            }

        } catch (Exception e) {
            e.getMessage();
        }
    }

    /**
     * @param ebgroupName2
     */
    private void getffgroupName(String groupCode) {
        try {
            EbFfGroupsRecord ffGroupRec = new EbFfGroupsRecord(da.getRecord("", "EB.FF.GROUPS", "", groupCode));
            groupName = ffGroupRec.getGroupName().getValue();
        } catch (Exception e) {
            e.getMessage();
        }

    }

    /**
     * @param centerId2
     */
    private void getEbFfCentreDetails(String centerId) {

        try {
            EbFfCentreDetailRecord centerRec = new EbFfCentreDetailRecord(
                    da.getRecord("", "EB.FF.CENTRE.DETAIL", "", centerId));
            centerName = centerRec.getCenterName().getValue();
            String ro = centerRec.getCurrentRo().getValue();
            String branchManagerId = centerRec.getBranchManager().getValue();

            if (ro != null && !ro.isEmpty()) {
                getEbFfRoUserDets(ro);
            }

            if (branchManagerId != null && !branchManagerId.isEmpty()) {
                getUserDets(branchManagerId);
            }
        } catch (Exception e) {
            e.getMessage();
        }

    }

    /**
     * @param branchManagerId
     */
    private void getUserDets(String branchManagerId) {
        try {
            UserRecord userRec = new UserRecord(da.getRecord("", "USER", "", branchManagerId));
            branchManagerName = userRec.getUserName().getValue();
            branchManagerMobileNumber = userRec.getLocalRefField("FF.MOBILE.NO").getValue();

        } catch (Exception e) {
            e.getMessage();
        }

    }

    /**
     * @param ro
     */
    private void getEbFfRoUserDets(String ro) {
        try {

            EbFfRoUserRecord roUserRec = new EbFfRoUserRecord(da.getRecord("", "EB.FF.RO.USER", "", ro));

            relationShipOfficerName = roUserRec.getRoName().getValue();

            relationshipOfficerMobileNumber = roUserRec.getRoMobileNumber().getValue();

        } catch (Exception e) {

            e.getMessage();

        }

    }

    /**
     * Consolidates all gathered global variables into a single asterisk (*)
     * delimited string to be returned to the T24 Enquiry.
     */
    private void buildAndAddRow() {
        List<String> row = new ArrayList<>();

        // Fields 1-24: Organization & Customer
        row.add(zoneName);
        row.add(regionName);
        row.add(divisionName);
        row.add(clusterName);
        row.add(branchName);
        row.add(branchDistrict);
        row.add(branchState);
        row.add(coCode);
        row.add(centerName);
        row.add(centerId);
        row.add(groupName);
        row.add(groupCode);
        row.add(customerName);
        row.add(cusId);
        row.add(customerAge);
        row.add(caste);
        row.add(fmOccup);
        row.add(loanPurp);
        row.add(accNum);
        row.add(legacyAcctnum);
        row.add(relationShipOfficerName);
        row.add(relationshipOfficerMobileNumber);
        row.add(branchManagerName);
        row.add(branchManagerMobileNumber);

        // Fields 25-39: Loan Financials (Ensuring exact 39 count)
        row.add(formatDate(disbursementDate)); // 25
        row.add(loanAmount); // 26
        row.add(disbursementMode); // 27
        row.add(loanTenure); // 28 (Requirement: replace duplicate amount with Tenure)
        row.add(loanCycle); // 29
        row.add(fixedRate); // 30
        row.add(product); // 31
        row.add(loanPurp); // 32 (Purpose Desc)
        row.add(formatDate(firstDueDate)); // 33
        row.add(lpfAmount); // 34
        row.add(downpaymentName); // 35
        row.add(insuranceName); // 36
        row.add(insuranceAmount); // 37
        row.add(repaymentFrequency); // 38
        row.add(formatDate(lastDueDate)); // 39

        returnVal.add(String.join("*", row));
        outvalues.add(String.join(",", row));
    }

    /**
     * @param firstDueDate2
     * @return
     */
    private String formatDate(String t24Date) {
        if (t24Date == null || t24Date.length() < 8)
            return "";
        try {
            // Converts 20260328 -> 28-03-2026
            LocalDate date = LocalDate.parse(t24Date, formatter);
            return date.format(outDateFormatter);
        } catch (Exception e) {
            return t24Date;
        }
    }

    /**
     * @param outvalues2
     * @param outputPath
     */
    private void writeToFile(List<String> data, String filepath) {
        try {

            File file = new File(filepath);
            file.getParentFile().mkdirs();
            boolean fileExists = file.exists();

            try (FileWriter writer = new FileWriter(file, true)) {
                if(data == null || data.isEmpty()){
                    writer.write("No records matched the selection criteria" + System.lineSeparator());
                }
                else {
                if (!fileExists) {
                    String header = String.join(",", "ZoneName", "RegionName", "DivisionName", "ClusterName",
                            "BranchName", "BranchDistrict", "BranchState", "BranchCode", "CenterName", "CenterId",
                            "GroupName", "GroupCode", "CustomerName", "cusId", "CustomerAge", "Caste", "Occupation",
                            "Purpose", "AccNum", "legacyAcctNum", "RelationShipOfficerName",
                            "RelationShipOfficerMobileNumber", "BranchManagerName", "BranchManagerMobileNumber",
                            "DisbursementDate", "LoanAmount", "DisbursementMode", "LoanTenure", "LoanCycle",
                            "FixedRate", "Product", "LoanPurpose", "FirstRepaymentDate", "LpfAmount", "DownPaymentName",
                            "InsuranceName", "InsuranceAmount", "RepaymentFrequency", "LastDueDate");
                    writer.write(header + System.lineSeparator());
                }
                
                for (String line : data) {
                    writer.write(line + System.lineSeparator());
                }
            }
            }
        } catch (Exception e3) {
            e3.getMessage();
        }
    }

    /**
     * Retrieves all company IDs linked to a specific branch by checking the
     * COMPANY.CONSOL table.
     */
    private void getLinkedCompIds(String branch) {

        StringBuilder company = new StringBuilder();
        try {
            List<String> comConsolRecList = da.selectRecords("", "COMPANY.CONSOL", "",
                    "WITH COM.CONSOL.TO EQ " + branch);
            if (!comConsolRecList.isEmpty()) {
                company.append(branch).append(" ");
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
                companyIds = branch;
            }

        } catch (Exception e4) {
            e4.getMessage();
        }
    }

    private Set<String> getpreFinalset(Set<String> arrList, Set<String> selectionSet) {

        Set<String> preFinalSet;

        if (selectionSet.isEmpty()) {
            if (arrList.isEmpty()) {
                noRecErrFlag = true;
                preFinalSet = new LinkedHashSet<>();
            } else {
                preFinalSet = new LinkedHashSet<>(arrList);
            }
        } else {
            preFinalSet = new LinkedHashSet<>(selectionSet);
        }
        return preFinalSet;
    }

    public void initialiseCompanyInfo(String companyId) {

        try {

            CompanyRecord companyObj = new CompanyRecord(da.getRecord(SEL_APP_COMP, companyId));
            finMnemonic = companyObj.getFinancialMne().getValue();
            branchCode = companyId;
            branchState = companyObj.getLocalRefField("FF.STATE").getValue();
            branchName = getCompanyDescription(companyId);
            zoneName = getCompanyDescription(companyObj.getLocalRefField("FF.ZONE").getValue());
            regionName = getCompanyDescription(companyObj.getLocalRefField("FF.REGION").getValue());
            divisionName = getCompanyDescription(companyObj.getLocalRefField("FF.DIVISION").getValue());
            clusterName = getCompanyDescription(companyObj.getLocalRefField("FF.CLUSTER").getValue());

        } catch (Exception e9) {
            e9.getMessage();
        }
    }

    /**
     * Retrieves company name for a given company code. Extracts readable
     * description from COMPANY record.
     */
    public String getCompanyDescription(String companyCode) {

        try {
            if (companyCode != null && !companyCode.isEmpty()) {
                companyRec = new CompanyRecord(da.getRecord(SEL_APP_COMP, companyCode));
                companyName = companyRec.getCompanyName().get(0).getValue();
                String[] compNamePart = companyName.split("-");
                companyName = compNamePart[0];

                return companyName;
            }
        } catch (Exception e10) {
            e10.getMessage();
        }
        return companyName;
    }

    /**
     * Extracts interest rates and fee amounts (Processing/Insurance) from the
     * arrangement's condition tables.
     */
    private void getValuesFromInterestTable(Contract contract) {
        insuranceAmount = "";
        lpfAmount = "";
        fixedRate = "";
        try {
            AaPrdDesInterestRecord aaPrdDesInterestRecord = new AaPrdDesInterestRecord(
                    contract.getConditionForProperty("PRINTEREST"));

            String arrIntId = aaPrdDesInterestRecord.getIdComp1().getValue() + "-"
                    + aaPrdDesInterestRecord.getIdComp2().getValue() + "-"
                    + aaPrdDesInterestRecord.getIdComp3().getValue();
            AaPrdDesInterestRecord prdDesInt = new AaPrdDesInterestRecord(
                    da.getRecord(finMnemonic, "AA.ARR.INTEREST", "", arrIntId));

            if (!prdDesInt.toString().isEmpty()) {
                fixedRate = prdDesInt.getFixedRate(0).getFixedRate().getValue();
            }

            List<String> chgPropList = contract.getPropertyIdsForPropertyClass("CHARGE");

            for (String chgProperty : chgPropList) {
                AaPrdDesChargeRecord aaArrChgRec = new AaPrdDesChargeRecord(
                        contract.getConditionForProperty(chgProperty));
                String idComp2 = aaArrChgRec.getIdComp2().getValue();

                if ("PROCESSINGFEE".equals(idComp2)) {
                    lpfAmount = aaArrChgRec.getFixedAmount().getValue();
                }
                if ("INSURANCEFEE".equals(idComp2)) {
                    insuranceAmount = aaArrChgRec.getFixedAmount().getValue();
                }
            }
        } catch (Exception e6) {
            e6.getMessage();
        }
    }

    public void getEbFfLoanDetails(String selectionArrId) {
        try {
            EbFfLoanDetailsRecord loanRec = new EbFfLoanDetailsRecord(
                    da.getRecord(finMnemonic, "EB.FF.LOAN.DETAILS", "", selectionArrId));
            List<AddressTypeClass> addressTypeList = loanRec.getAddressType();
            for (AddressTypeClass addressType : addressTypeList) {
                branchDistrict = addressType.getDistrictName().getValue();

            }
            List<FmEntityNumberClass> fmEntityNumList = loanRec.getFmEntityNumber();
            for (FmEntityNumberClass fmEntityNum : fmEntityNumList) {
                fmOccup = fmEntityNum.getOccupation().getValue();
            }
        } catch (Exception e8) {
            e8.getMessage();
        }
    }

    private void getValuesFromEbCollectionDets(String arrId) {
        try {
            // 1. Reset global variables
            this.firstDueDate = "";
            this.lastDueDate = "";

            // 2. PRIMARY SOURCE: Get the Last Repayment Date from the Live Table for ALL
            // contracts
            String liveTable = "EB.FF.COLLECTION.DETS";
            // Use "" mnemonic for global access to ensure no branch isolation issues
            EbFfCollectionDetsRecord liveRec = new EbFfCollectionDetsRecord(da.getRecord("", liveTable, "", arrId));
            List<TField> liveDueDates = liveRec.getDueDate();

            if (liveDueDates != null && !liveDueDates.isEmpty()) {
                // Per Requirement: Last record in the Due.Date multi-value list
                this.lastDueDate = liveDueDates.get(liveDueDates.size() - 1).getValue();

                // If it is a NON-LEGACY loan, we also take the first due date from here
                if (!legacy) {
                    // Skip Index 0 for Live loans; take Due Date 2 if exists
                    this.firstDueDate = (liveDueDates.size() > 1) ? liveDueDates.get(1).getValue()
                            : liveDueDates.get(0).getValue();
                }
            }

            // 3. MIGRATION LOGIC: If it is legacy, we still need to get the ORIGINAL First
            // Due Date from History
            if (legacy) {
                handleLegacyCollectionDets(arrId);
            }

        } catch (Exception e) {
            e.getMessage();
        }
    }

    private void handleLegacyCollectionDets(String arrId) {

        String table = "EB.FF.COLLECTION.DETS.HISTORY";
        String predictedId = arrId + "-" + t24StartDt + ".01";

        this.firstDueDate = "";

        try {
            // We only need the OLDEST iteration to get the original first installment date
            EbFfCollectionDetsRecord oldestRec = new EbFfCollectionDetsRecord(da.getRecord("", table, "", predictedId));

            if (!oldestRec.getDueDate().isEmpty()) {
                this.firstDueDate = oldestRec.getDueDate(0).getValue();

            }

            // Note: lastDueDate is already fetched from the Live table in the calling
            // method

        } catch (Exception e) {
            e.getMessage();
        }
    }

    private void getArrangementFieldMappingDet(String selectionArrId) {
        try {
            aaArrRec = new AaArrangementRecord(da.getRecord("", AA_ARRANGEMENT_TBL, "", selectionArrId));
            coCode = aaArrRec.getCoCodeRec().getValue();
            accNum = selectionArrId;
            product = aaArrRec.getProduct().get(0).getProduct().getValue();
            cusId = aaArrRec.getCustomer(0).getCustomer().getValue();

            String origContractDt = aaArrRec.getOrigContractDate().getValue();
            this.t24StartDt = aaArrRec.getStartDate().getValue();

            // Row 24 Requirement logic
            if (origContractDt != null && !origContractDt.isEmpty()) {
                this.disbursementDate = origContractDt;
                this.legacy = true; // Use your global 'legacy' boolean
            } else {
                this.disbursementDate = this.t24StartDt;
                this.legacy = false;
            }

            getCustomerDetails(cusId);

        } catch (Exception e) {
            e.getMessage();
        }
    }

    /**
     * Fetches and calculates customer demographics. Retrieves Name, Caste, and
     * Group, and calculates current Age from the date of birth.
     */

    private void getCustomerDetails(String cusId) {
        try {
            cusRec = new CustomerRecord(da.getRecord(cusmnemonic, SEL_APP_CUS, "", cusId));

            for (TField name1 : cusRec.getName1()) {
                fstName = name1.getValue();
            }

            for (TField name2 : cusRec.getName2()) {
                scdName = name2.getValue();
            }

            familyName = cusRec.getFamilyName().getValue();

            customerName = String.join(" ", fstName, scdName, familyName).trim().replaceAll("\\s+", " ");

            String customerAgeDob = cusRec.getDateOfBirth().getValue();

            LocalDate today = LocalDate.parse(todayDate, formatter);
            LocalDate dob = LocalDate.parse(customerAgeDob, formatter);
            Period agePeriod = Period.between(dob, today);
            customerAge = String.valueOf(agePeriod.getYears());

            caste = cusRec.getLocalRefField("FF.CASTE").getValue();
            fmOccup = cusRec.getLocalRefField("FF.FM.OCCUP").getValue();
            groupName = cusRec.getLocalRefField("FF.GROUP.CODE").getValue();
            groupCode = cusRec.getLocalRefField("FF.GROUP.CODE").getValue();
            religGrp = cusRec.getLocalRefField("FF.RELIG.GROUP").getValue();

        } catch (Exception e6) {
            e6.getMessage();
        }
    }

    /**
     * Retrieves the Commitment (Loan Amount) for the arrangement. Accesses the
     * AA.PRD.DES.TERM.AMOUNT property through the contract to extract the
     * agreed-upon loan amount. * @param contract The current AA contract object for
     * the arrangement being processed.
     */

    private void getArrTermAmountDet(Contract contract) {
        try {
            AaArrTermAmountRecord termAmtRec = new AaArrTermAmountRecord(
                    contract.getConditionForProperty("COMMITMENT"));

            this.loanAmount = termAmtRec.getAmount().getValue();
            // This gets the tenure (e.g., "2Y" or "24M") from the TERM field
            this.loanTenure = termAmtRec.getTerm().getValue();

        } catch (Exception e) {
            this.loanAmount = "0";
            this.loanTenure = "";
        }
    }

    /**
     * Maps fields from the Account property of the arrangement. Retrieves the
     * primary officer, loan purpose, loan cycle, and disbursement mode from the
     * AA.PRD.DES.ACCOUNT condition and its associated local reference fields.
     * * @param contract The current AA contract object used to fetch the ACCOUNT
     * condition.
     */

    /*
     * Parses the Enquiry selection criteria. Maps user inputs (Date, Product,
     * Village, etc.) into specific T24 selection queries.
     */

    private Set<String> getFilterCriteriaDets(List<FilterCriteria> filterCriteria) {
       
        
        Set<String> currentFilterSet = new LinkedHashSet<>();
        try {
            for (FilterCriteria filter : filterCriteria) {
                String fieldName = filter.getFieldname();
                String value = filter.getValue();

                if (value == null || value.isEmpty() || "BRANCH".equals(fieldName)) {
                    continue;
                }

                switch (fieldName) {
                case "USER":
                    selUser = value;
                    break;
                case "DATE.FROM":
                    filterValSet.add(value);
                    startDate = value;

                    onlyDateFilter = true;
                    break;

                case "DATE.TO":
                    filterValSet.add(value);
                    endDate = value;

                    onlyDateFilter = true;

                    break;
                case "PRODUCT":
                    selProduct = value; 
                    currentFilterSet.addAll(
                            da.selectRecords(finMnemonic, AA_ARRANGEMENT_TBL, "", "WITH PRODUCT EQ " + selProduct));
                    break;
                case "CENTER":
                    selCentreName = value;
                    currentFilterSet.addAll(getArrListFromCentre(selCentreName));
                    break;
                case "VILLAGE":
                    selVillage = value;
                    List<String> villageArrAccList = da.selectRecords(finMnemonic, AA_ARR_ACCOUNT_TBL, "",
                            "WITH FF.VILLAGE EQ " + selVillage);
                    currentFilterSet.addAll(getArrListFromSelection(villageArrAccList));
                    break;
                case "DISTRICT.NAME":
                    selDistrict = value;
                    List<String> cusIdsBsDistName = da.selectRecords(cusmnemonic, SEL_APP_CUS, "",
                            "WITH DISTRICT.NAME EQ " + selDistrict);
                    currentFilterSet.addAll(getArrListFromSel(cusIdsBsDistName));
                    break;
                default:
                    break;
                }
            }

        } catch (Exception e15) {
            e15.getMessage();
        }
        return currentFilterSet;
    }

    /**
     * Scans the AA.CUSTOMER.ARRANGEMENT record for a customer. Specifically
     * extracts Arrangement IDs belonging to the 'LENDING' product line.
     */
    private List<String> getArrListFromSel(List<String> cusIdList) {
        List<String> arrIdList = new ArrayList<>();
        try {
            for (String cusid : cusIdList) {
                AaCustomerArrangementRecord aaCusRec = new AaCustomerArrangementRecord(
                        da.getRecord(finMnemonic, "AA.CUSTOMER.ARRANGEMENT", "", cusid));
                if (!aaCusRec.toString().isEmpty())
                    getProductDet(arrIdList, aaCusRec);
            }
        } catch (Exception e16) {
            e16.getMessage();
        }
        return arrIdList;
    }

    /**
     * Filters and extracts specific Arrangement IDs for a customer. Iterates
     * through the product lines of an AA.CUSTOMER.ARRANGEMENT record and adds all
     * Arrangement IDs belonging to the 'LENDING' product line to the provided
     * identification list. * @param arrIdList The list to be populated with valid
     * Arrangement IDs.
     * 
     * @param aaCusRec The record containing the customer's linked arrangements and
     *                 products.
     */
    private void getProductDet(List<String> arrIdList, AaCustomerArrangementRecord aaCusRec) {
        productLineList = aaCusRec.getProductLine();
        for (ProductLineClass productLine : productLineList) {
            if ("LENDING".equals(productLine.getProductLine().getValue())) {
                for (ArrangementClass arrIdFrmAAcus : productLine.getArrangement()) {
                    arrIdList.add(arrIdFrmAAcus.getArrangement().getValue());
                }
            }
        }
    }

    /**
     * Retrieves all Arrangement IDs associated with a specific Centre. Selects
     * records from the AA.ARR.ACCOUNT table based on the FF.CENTRE local reference
     * field and resolves them into a unique list of Arrangement IDs. * @param
     * centreId The unique identifier of the Centre used for filtering.
     * 
     * @return A list of unique Arrangement IDs belonging to the specified Centre.
     */
    public List<String> getArrListFromCentre(String centreId) {
        List<String> arrAccList = da.selectRecords(finMnemonic, AA_ARR_ACCOUNT_TBL, "",
                "WITH FF.CENTRE EQ " + centreId);
        return getArrListFromSelection(arrAccList);
    }

    /**
     * Resolves and deduplicates Arrangement IDs from a list of property records.
     * Takes a list of IDs (which may include property-specific suffixes like
     * '-COMMITMENT') and extracts the base Arrangement ID to ensure a unique set of
     * records for processing. * @param aaArrAccList A list of raw Arrangement or
     * property-level record IDs.
     * 
     * @return A list of unique, base Arrangement IDs (e.g., 'AA21001XXXXX').
     */
    public List<String> getArrListFromSelection(List<String> aaArrAccList) {
        List<String> arrIdList = new ArrayList<>();
        try {
            for (String arrId : aaArrAccList) {
                String[] parts = arrId.split("-");
                String aaId = parts[0];
                if (!arrIdList.contains(aaId)) {
                    arrIdList.add(aaId);
                }
            }
        } catch (Exception e17) {
            e17.getMessage();
        }
        return arrIdList;
    }

}