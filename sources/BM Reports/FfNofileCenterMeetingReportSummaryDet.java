package com.temenos.fusion;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

import com.temenos.api.TField;
import com.temenos.api.exceptions.T24CoreException;
import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.aa.contractapi.BalanceMovement;
import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.t24.api.records.aaaccountdetails.AaAccountDetailsRecord;

import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;

import com.temenos.t24.api.records.aacustomerarrangement.AaCustomerArrangementRecord;
import com.temenos.t24.api.records.aacustomerarrangement.ArrangementClass;
import com.temenos.t24.api.records.aacustomerarrangement.ProductLineClass;
import com.temenos.t24.api.records.aaprddesaccount.AaPrdDesAccountRecord;
import com.temenos.t24.api.records.aaprddespaymentschedule.AaPrdDesPaymentScheduleRecord;

import com.temenos.t24.api.records.company.CompanyRecord;
import com.temenos.t24.api.records.companyconsol.CompanyConsolRecord;

import com.temenos.t24.api.records.ebffcentredetail.EbFfCentreDetailRecord;

import com.temenos.t24.api.records.ebffcollectiondets.EbFfCollectionDetsRecord;
import com.temenos.t24.api.records.ebffcollectiondetshistory.EbFfCollectionDetsHistoryRecord;
import com.temenos.t24.api.records.ebffloandetails.EbFfLoanDetailsRecord;

import com.temenos.t24.api.records.ebffparameter.EbFfParameterRecord;
import com.temenos.t24.api.records.ebffparameter.ParamDescClass;
import com.temenos.t24.api.records.ebffrouser.EbFfRoUserRecord;
import com.temenos.t24.api.records.user.UserRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/*-----------------------------------------------------------------------------
 * @author 
 * Date Created:
 * Attached as : 
 * EB.API : EB.FF.CENTER.MEETING.REPORT.DET
 * STANDARD.SELECTION > NOFILE.FF.CENTER.MEETING.REPORT.DET
 * ENQUIRY :FF.CENTER.MEETING.REPORT.DET
 * Description: Branch Online Report generation -> Center Meeting Report
 *------------------------------------------------------------------------------ 
 * Modification History : NA
 *----------------------------------------------------------------------------- 
 *05-Dec-2025   Development      Initial Version
 *-----------------------------------------------------------------------------
 */
public class FfNofileCenterMeetingReportSummaryDet extends Enquiry {

    private static final String EB_FF_PARAMETER = "EB.FF.PARAMETER";
    private static final String EB_FF_COLLECTION_DETS = "EB.FF.COLLECTION.DETS";
    DataAccess da = new DataAccess(this);
    String datefrom = "";
    String dateto = "";
    String dateOperand = "";
    String month = "";
    String monthOperand = "";
    String fieldRo = "";
    String finMnemonic = "";
    String product = "";
    String centerName = "";
    String village = "";

    boolean noRecErrFlag = false;
    String zoneName = "";
    String regionName = "";
    String divisionName = "";
    String clusterName = "";
    String datedefaultRange = "";
    Session session = new Session(this);
    String mnemonic = "";
    String arrId = "";
    AaAccountDetailsRecord accountDetailsRecord = null;
    String startDate = "";
    String endDate = "";
    int daysBetweenToday = 0;
    String todayDate = session.getCurrentVariable("!TODAY");
    AaArrangementRecord arrangementRecord = null;
    String coCode = "";
    CompanyRecord companyRecord = null;
    String companyName = "";
    String noOfDaysBetween = "";
    List<String> arrangementList = new ArrayList<>();
    List<String> customerList = new ArrayList<>();
    List<String> returnValues = new ArrayList<>();
    List<BalanceMovement> balMvmtClasssList = new ArrayList<>();
    Double curAcctTypeAmt = 0.0;
    boolean settlePayoff = false;
    String branchDistrict = "";
    String billId = "";
    String branchState = "";
    String branchCode = "";
    String center = "";
    String centerCode = "";
    String sumAmount = "";
    String customerNumber = "";
    String relationshipOfficerName = "";
    String relationshipOfficerMobileNumber = "";
    String branchManagerName = "";
    String branchManagerMobileNumber = "";
    String repaymentFrequency = "";
    String activeMemberCount = "";
    String activeLoanCount = "";
    String outstandingAmount = "";
    String meetingDate = "";
    String meetingDay = "";
    String meetingTime = "";
    String branchName = "";
    String startDateAccDet = "";
    List<ProductLineClass> productLineList = null;
    List<String> loglist = new ArrayList<>();
    List<String> arrgementlist = new ArrayList<>();
    List<String> outvalues = new ArrayList<>();
    String cycle = "";
    String purpose = "";
    String religion = "";
    String caste = "";
    String district = "";
    String branch = "";
    String selBranch = "";
    String companyIds = "";
    String futureMonth = "";
    String selUser = "";

    int years = 0;
    int months = 0;
    int weeks = 0;
    int days = 0;
    int fortnights = 0;
    int daterange = 0;
    int defaultDate = 0;
    int processedCount = 0;
    int matchedCount = 0;
    Set<String> uniqueCustomers = new HashSet<>();
    Set<String> uniqueloans = new HashSet<>();
    Set<String> filterValSet = new HashSet<>();

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    DateTimeFormatter outDateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HHmmss");
    LocalDate today = LocalDate.parse(todayDate, formatter);
    public static final String FILE_NAME = "CenterMeetingReport_DET";
    public static final String ACCOUNT = "ACCOUNT";
    public static final String AA_ARRANGEMENT = "AA.ARRANGEMENT";
    public static final String COMPANY = "COMPANY";
    public static final String CUSTOMER_VAR = "CUSTOMER";
    public static final String TRADE = "TRADE";
    private static final String AA_ARR_ACCOUNT = "AA.ARR.ACCOUNT";
    public static final String DATE_RANGE_ERR = "EB-FF.BM.DATE.FILTER.RANGE";
    public static final String NO_REC_ERR = "EB-NO.REC.SELECT";

    class CentreSummary {

        String centreCode = "";
        String centreName = "";

        String meetingDay = "";
        String meetingTime = "";

        String relationshipOfficerName = "";
        String relationshipOfficerMobileNumber = "";

        String branchManagerName = "";
        String branchManagerMobileNumber = "";

        String branchDistrict = "";

        Set<String> customerSet = new HashSet<>();
        Set<String> loanSet = new HashSet<>();

        double outstandingAmount = 0.0;
    }

    @Override
    public List<String> setIds(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {

        try {

            for (FilterCriteria filter : filterCriteria) {

                if (filter.getFieldname().equals("BRANCH")) {
                    selBranch = filter.getValue();
                    getCompanyDetails(selBranch);
                    getLinkedCompIds(selBranch);

                    break;
                }
            }

            Contract contract = new Contract(this);

            arrgementlist = da.selectRecords(finMnemonic, AA_ARRANGEMENT, "",
                    "WITH ARR.STATUS NE CLOSE AND ARR.STATUS NE PENDING.CLOSURE AND ARR.STATUS NE EXPIRED AND CO.CODE EQ "
                            + companyIds);

            getMaxdateRangedet();

            getDefaultDatRange();

            Set<String> overAllArrAccDetIdList = new LinkedHashSet<>(arrgementlist);

            Set<String> selectionSet = getselectionlistval(filterCriteria);

            Set<String> preFinalSet = getPreFinalset(overAllArrAccDetIdList, selectionSet);

            preFinalSet.retainAll(overAllArrAccDetIdList);

            listMethod(preFinalSet, contract);

        } catch (Exception e) {
            e.getMessage();

        }

        if (noRecErrFlag) {

            throw new T24CoreException("", NO_REC_ERR);
        } else {

            return returnValues;
        }
    }

    private String formatDate(String inputDate) {
        try {

            LocalDate date = LocalDate.parse(inputDate, formatter);

            return date.format(outDateFormatter);

        } catch (Exception e) {
            return inputDate;
        }
    }

    private void getDefaultDatRange() {

        String paramId = "FF.BM.REPORT.DATE.DEFAULT";

        EbFfParameterRecord paramRec = new EbFfParameterRecord(da.getRecord(EB_FF_PARAMETER, paramId));
        for (ParamDescClass paramDesc : paramRec.getParamDesc()) {

            if (paramDesc.getParamName().getValue().equals("CENTER.MEETING")) {
                datedefaultRange = paramDesc.getParamValue().getValue();

                if (datedefaultRange.contains("7D")) {
                    defaultDate = 7;

                }
            }
        }

    }

    private void getMaxdateRangedet() {

        String paramId = "FF.BM.REPORT.DATE.RANGE";

        EbFfParameterRecord paramRec = new EbFfParameterRecord(da.getRecord(EB_FF_PARAMETER, paramId));
        for (ParamDescClass paramDesc : paramRec.getParamDesc()) {

            if (paramDesc.getParamName().getValue().equals("CENTER.MEETING")) {
                futureMonth = paramDesc.getParamValue().getValue();

                if (futureMonth.contains("1M")) {
                    daterange = 31;

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

        String result;
        switch (unit) {
        case 'D':
            result = num + (num == 1 ? " Day" : " Days");
            break;
        case 'M':
            result = num + (num == 1 ? " Month" : " Months");
            break;
        default:
            result = value;
        }

        return result;
    }

    public Set<String> getPreFinalset(Set<String> arrList, Set<String> selectionSet) {

        Set<String> preFinalSet = new LinkedHashSet<>();
        if (selectionSet.isEmpty()) {

            if (!filterValSet.isEmpty()) {

                noRecErrFlag = true;
            } else {

                preFinalSet = arrList;
            }
        } else {

            preFinalSet = new LinkedHashSet<>(selectionSet);
        }

        return preFinalSet == null ? new LinkedHashSet<>() : preFinalSet;
    }

    private Set<String> getselectionlistval(List<FilterCriteria> filterCriteria) {

        Set<String> currentFilterSet = new LinkedHashSet<>();
        try {
            for (FilterCriteria criteria : filterCriteria) {
                String field = criteria.getFieldname();
                String value = criteria.getValue();

                if ("DATE.FROM".equalsIgnoreCase(field)) {
                    startDate = value;

                } else if ("DATE.TO".equalsIgnoreCase(field)) {
                    endDate = value;

                } else {

                    currentFilterSet.addAll(getgroupselecval(criteria));
                }
            }
        } catch (Exception e) {
            e.getMessage();

        }

        return currentFilterSet;
    }

    private Set<String> getgroupselecval(FilterCriteria criteria) {

        Set<String> currFilterSet = new LinkedHashSet<>();

        try {
            String field = criteria.getFieldname();
            String value = criteria.getValue();

            if ("PRODUCT".equalsIgnoreCase(field)) {
                filterValSet.add(value);
                product = value;

                List<String> productList = da.selectRecords(finMnemonic, AA_ARRANGEMENT, "",
                        "WITH PRODUCT EQ " + product);

                currFilterSet.addAll(productList);
            }
            if ("CENTER".equalsIgnoreCase(field)) {
                filterValSet.add(value);
                centerName = value;

                List<String> centrearrAccList = da.selectRecords(finMnemonic, AA_ARR_ACCOUNT, "",
                        "WITH FF.CENTRE EQ " + centerName);

                currFilterSet.addAll(getArrListFromSelection(centrearrAccList));
            }
            if ("VILLAGE".equalsIgnoreCase(field)) {
                filterValSet.add(value);
                village = value;

                List<String> villageArrAccList = da.selectRecords(finMnemonic, AA_ARR_ACCOUNT, "",
                        "WITH FF.VILLAGE EQ " + village);

                currFilterSet.addAll(getArrListFromSelection(villageArrAccList));
            }
            if ("DISTRICT".equalsIgnoreCase(field)) {
                filterValSet.add(value);
                district = value;

                List<String> cusIdsBsDistName = da.selectRecords("", "EB.FF.LOAN.DETAILS", "",
                        "WITH DISTRICT.NAME EQ " + district);

                currFilterSet.addAll(cusIdsBsDistName);
            }
            if ("CYCLE".equalsIgnoreCase(field)) {
                filterValSet.add(value);
                cycle = value;

                List<String> loanCycleList = da.selectRecords(finMnemonic, AA_ARR_ACCOUNT, "",
                        "WITH FF.LOAN.CYCLE EQ " + cycle);

                currFilterSet.addAll(getArrListFromSelection(loanCycleList));
            }
            if ("PURPOSE".equalsIgnoreCase(field)) {
                filterValSet.add(value);
                purpose = value;

                List<String> loanPurposeList = da.selectRecords(finMnemonic, AA_ARR_ACCOUNT, "",
                        "WITH FF.LOAN.PURP EQ " + purpose);

                currFilterSet.addAll(getArrListFromSelection(loanPurposeList));
            }
            if ("RELIGION".equalsIgnoreCase(field)) {
                filterValSet.add(value);
                religion = value;

                List<String> religGrpList = da.selectRecords(mnemonic, CUSTOMER_VAR, "",
                        "WITH FF.RELIG.GROUP EQ " + religion);

                currFilterSet.addAll(getArrListFromsel(religGrpList));
            }
            if ("CASTE".equalsIgnoreCase(field)) {
                filterValSet.add(value);
                caste = value;

                List<String> casteList = da.selectRecords(mnemonic, CUSTOMER_VAR, "", "WITH FF.CASTE EQ " + caste);

                currFilterSet.addAll(getArrListFromsel(casteList));
            }
            if ("USER".equalsIgnoreCase(field)) {
                selUser = value;

            }
        } catch (Exception e) {
            e.getMessage();
        }

        return currFilterSet;
    }

    private List<String> getArrListFromsel(List<String> cusIdList) {

        List<String> arrIdList = new ArrayList<>();
        try {
            for (String cusid : cusIdList) {

                AaCustomerArrangementRecord aaCusRec = new AaCustomerArrangementRecord(
                        da.getRecord(finMnemonic, "AA.CUSTOMER.ARRANGEMENT", "", cusid));
                if (!aaCusRec.toString().isEmpty()) {
                    productLineList = aaCusRec.getProductLine();
                    getProductLine(arrIdList);
                }
            }
        } catch (Exception e) {
            e.getMessage();
        }

        return arrIdList;
    }

    private void getProductLine(List<String> arrIdList) {

        for (ProductLineClass productLine : productLineList) {
            if (productLine.getProductLine().getValue().equals("LENDING")) {

                for (ArrangementClass arrIdFrmAAcus : productLine.getArrangement()) {
                    String arrIdValue = arrIdFrmAAcus.getArrangement().getValue();
                    arrIdList.add(arrIdValue);

                }
            }
        }

    }

    public List<String> getArrListFromSelection(List<String> aaArrAccList) {

        List<String> arrIdList = new ArrayList<>();
        try {
            for (String arrlist : aaArrAccList) {
                String[] parts = arrlist.split("-");
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

    private void listMethod(Set<String> preFinalSet, Contract contract) {

        Map<String, CentreSummary> centreMap = new HashMap<>();

        try {
            LocalDate start = startDate.isEmpty() ? today : LocalDate.parse(startDate, formatter);
            LocalDate end = endDate.isEmpty() ? today.plusDays(defaultDate) : LocalDate.parse(endDate, formatter);

            getfinalarrlist(preFinalSet, start, end);

            activeLoanCount = String.valueOf(uniqueloans.size());
            activeMemberCount = String.valueOf(uniqueCustomers.size());

       
            for (String Id : uniqueloans) {
                processedCount++;

                branchDistrict = "";
                branchState = "";
                relationshipOfficerName = "";
                relationshipOfficerMobileNumber = "";
                branchManagerName = "";
                branchManagerMobileNumber = "";
                center = "";
                centerCode = "";
                meetingDate = "";
                meetingDay = "";
                meetingTime = "";
                repaymentFrequency = "";

                AaArrangementRecord arrangementRec = new AaArrangementRecord(
                        da.getRecord(finMnemonic, AA_ARRANGEMENT, "", Id));
                
                String arrstartDate = arrangementRec.getStartDate().getValue();
                String hisrecid = Id + "-" + arrstartDate + ".01";

                if (arrangementRec.getOrigContractDate().getValue() != null
                        && !arrangementRec.getOrigContractDate().getValue().isEmpty()) {
                    

                    getcollectionhis(hisrecid, start, end);
                } 

                contract.setContractId(Id);

                getDistrictName(Id);

                getAaArrAccountDets(contract);

                getAaPrdDesPaymentScheduleDetails(contract);

                double loanOutstanding = getecbdetails(contract);

                CentreSummary summary = centreMap.get(centerCode);

                if (summary == null) {

                    summary = new CentreSummary();

                    summary.centreCode = centerCode;
                    summary.centreName = center;

                    summary.meetingDay = meetingDay;
                    summary.meetingTime = meetingTime;

                    summary.relationshipOfficerName = relationshipOfficerName;
                    summary.relationshipOfficerMobileNumber = relationshipOfficerMobileNumber;

                    summary.branchManagerName = branchManagerName;
                    summary.branchManagerMobileNumber = branchManagerMobileNumber;

                    summary.branchDistrict = branchDistrict;

                    centreMap.put(centerCode, summary);
                }

                summary.loanSet.add(Id);

                AaArrangementRecord arrangement = new AaArrangementRecord(
                        da.getRecord(finMnemonic, AA_ARRANGEMENT, "", Id));

                if (arrangement.getCustomer() != null && !arrangement.getCustomer().isEmpty()) {

                    String customerId = arrangement.getCustomer().get(0).getCustomer().getValue();

                    summary.customerSet.add(customerId);
                }

                summary.outstandingAmount += loanOutstanding;
            }
            for (CentreSummary summary : centreMap.values()) {

                centerCode = summary.centreCode;
                center = summary.centreName;

                meetingDay = summary.meetingDay;
                meetingTime = summary.meetingTime;

                relationshipOfficerName = summary.relationshipOfficerName;

                relationshipOfficerMobileNumber = summary.relationshipOfficerMobileNumber;

                branchManagerName = summary.branchManagerName;

                branchManagerMobileNumber = summary.branchManagerMobileNumber;

                branchDistrict = summary.branchDistrict;

                activeLoanCount = String.valueOf(summary.loanSet.size());

                activeMemberCount = String.valueOf(summary.customerSet.size());

                outstandingAmount = String.valueOf(summary.outstandingAmount);

                addfinalvalues();
            }

            outvaluesToCsv();

        } catch (Exception e) {
            e.getMessage();
        }

    }

    private void getcollectionhis(String hisrecid, LocalDate start, LocalDate end) {

        try {
            List<String> matchedDates = new ArrayList<>();
            EbFfCollectionDetsHistoryRecord colldethissRec = new EbFfCollectionDetsHistoryRecord(
                    da.getRecord(finMnemonic, "EB.FF.COLLECTION.DETS.HISTORY", "", hisrecid));
            List<TField> duedateList1 = colldethissRec.getDueDate();
            int duedateList1Size = duedateList1.size();

            for (int j = duedateList1Size - 1; j >= 0; j--) {
                String duedate1 = duedateList1.get(j).getValue();
                LocalDate duedatefor1 = LocalDate.parse(duedate1, formatter);

                if ((duedatefor1.isEqual(start) || duedatefor1.isAfter(start))
                        && (duedatefor1.isEqual(end) || duedatefor1.isBefore(end))) {
                    matchedDates.add(duedate1);

                }
                meetingDate = String.join(" ", matchedDates);
            }

        } catch (Exception e) {
            e.getMessage();

        }

    }



    private void getfinalarrlist(Set<String> preFinalSet, LocalDate start, LocalDate end) {

 
        for (String arrangeId : preFinalSet) {
            processedCount++;
            try {

                EbFfCollectionDetsRecord colldetsRecord = new EbFfCollectionDetsRecord(
                        da.getRecord(finMnemonic, EB_FF_COLLECTION_DETS, "", arrangeId));

                boolean instawithindatrange = isDueDateWithinRange(colldetsRecord.getDueDate(), start, end);

                if (!instawithindatrange) {

                    continue;
                }

                matchedCount++;

                AaArrangementRecord arrangement = new AaArrangementRecord(
                        da.getRecord(finMnemonic, AA_ARRANGEMENT, "", arrangeId));

                uniqueloans.add(arrangeId);

                if (arrangement.getCustomer() != null && !arrangement.getCustomer().isEmpty()) {
                    String customerId = arrangement.getCustomer().get(0).getCustomer().getValue();

                    if (customerId != null && !customerId.isEmpty()) {
                        uniqueCustomers.add(customerId);

                    }
                }

            } catch (Exception e) {
                e.getMessage();

            }
        }

    }

    private boolean isDueDateWithinRange(List<TField> dueDateList, LocalDate start, LocalDate end) {

        if (dueDateList == null || dueDateList.isEmpty()) {

            return false;
        }

        for (int i = dueDateList.size() - 1; i >= 0; i--) {
            String dueDate = dueDateList.get(i).getValue();
            LocalDate parsedDate = LocalDate.parse(dueDate, formatter);

            if (!parsedDate.isBefore(start) && !parsedDate.isAfter(end)) {

                return true;
            }
        }

        return false;
    }

    public void getAaPrdDesPaymentScheduleDetails(Contract contract) {

        try {
            AaPrdDesPaymentScheduleRecord aaPrdPay = new AaPrdDesPaymentScheduleRecord(
                    contract.getConditionForProperty("PAYMENT.SCHEDULE"));

            List<com.temenos.t24.api.records.aaprddespaymentschedule.PaymentTypeClass> paymentTypeList = aaPrdPay
                    .getPaymentType();

            for (com.temenos.t24.api.records.aaprddespaymentschedule.PaymentTypeClass paymentType : paymentTypeList) {
                if (paymentType.getPaymentType().getValue().equalsIgnoreCase("CONSTANT")
                        && paymentType.getPaymentMethod().getValue().equalsIgnoreCase("DUE")
                        && paymentType.getBillType().getValue().equalsIgnoreCase("INSTALLMENT")) {
                    String rawValue = paymentType.getPaymentFreq().getValue();

                    if (rawValue != null && !rawValue.isEmpty()) {
                        String validValues = parsePaymentFrequency(rawValue);
                        repaymentFrequency = String.join(" ", validValues);

                    }
                }
            }

        } catch (Exception e) {
            e.getMessage();
        }

    }

    private String parsePaymentFrequency(String rawValue) {

        years = months = weeks = days = fortnights = 0;

        for (String val : rawValue.split("\\s+")) {
            if (val.startsWith("e")) {
                processToken(val.substring(1));
            }
        }

        return mapToExpectedFormat();
    }

    private String mapToExpectedFormat() {

        if ((weeks == 2 && isOthersZero()) || (days == 14 && isOthersZero())) {
            return "FORTNIGHTLY";
        }

        if ((weeks == 4 && isOthersZero()) || (days == 28 && isOthersZero())) {
            return "ONCE EVERY 28 DAYS";
        }

        if (months == 1 && years == 0 && weeks == 0 && days == 0) {
            return "MONTHLY";
        }

        return "OTHERS";
    }

    private void processToken(String cleaned) {

        String numberPart = cleaned.replaceAll("\\D", "");
        String unitPart = cleaned.replaceAll("\\d", "");

        if (numberPart.isEmpty()) {

            return;
        }

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

    private boolean isOthersZero() {
        return months == 0 && years == 0 && days == 0;
    }

    public void getAaArrAccountDets(Contract contract) {

        try {
            AaPrdDesAccountRecord aaArrAccRec = new AaPrdDesAccountRecord(contract.getConditionForProperty(ACCOUNT));
            centerCode = aaArrAccRec.getLocalRefField("FF.CENTRE").getValue();

            if (centerCode != null && !centerCode.isEmpty()) {

                getEbFfCentreDetails(centerCode);
            }
        } catch (Exception e) {
            e.getMessage();
        }

    }

    public void getEbFfCentreDetails(String centre) {

        try {
            EbFfCentreDetailRecord centreRec = new EbFfCentreDetailRecord(
                    da.getRecord("", "EB.FF.CENTRE.DETAIL", "", centre));
            String roName = centreRec.getCurrentRo().getValue();
            center = centreRec.getCenterName().getValue();

            meetingDay = centreRec.getFfMeetDay().getValue();
            meetingTime = centreRec.getFfMeetingTime().getValue();

            String userId = centreRec.getBranchManager().getValue();

            getUserDetails(userId);

            getRoName(roName);

        } catch (Exception e) {
            e.getMessage();

        }

    }

    private void getUserDetails(String userId) {

        if (userId != null && !userId.isEmpty()) {
            try {
                UserRecord userRec = new UserRecord(da.getRecord("", "USER", "", userId));

                branchManagerName = userRec.getUserName().getValue();
                branchManagerMobileNumber = userRec.getLocalRefField("FF.MOBILE.NO").getValue();

            } catch (Exception e) {
                e.getMessage();

            }
        }
    }

    private void getRoName(String roName) {

        if (roName != null && !roName.isEmpty()) {
            try {
                EbFfRoUserRecord roRec = new EbFfRoUserRecord(da.getRecord("", "EB.FF.RO.USER", "", roName));
                relationshipOfficerName = roRec.getRoName().getValue();
                relationshipOfficerMobileNumber = roRec.getRoMobileNumber().getValue();

            } catch (Exception e) {
                e.getMessage();

            }
        }
    }

    private void outvaluesToCsv() {
        try {

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
            String outputPath = filePath + FILE_NAME + "_" + branchName + "_" + selUser + "_" + currDate + "_"
                    + currTime + ".csv";
            writeToFile(outvalues, outputPath);
        } catch (Exception e) {
            e.getMessage();
        }

    }

    private void addfinalvalues() {

        List<String> fieldValues = new ArrayList<>();
        fieldValues.add(zoneName);
        fieldValues.add(regionName);
        fieldValues.add(divisionName);
        fieldValues.add(clusterName);
        fieldValues.add(branchName);
        fieldValues.add(branchDistrict);
        fieldValues.add(branchState);
        fieldValues.add(branchCode);
        fieldValues.add(relationshipOfficerName);
        fieldValues.add(relationshipOfficerMobileNumber);
        fieldValues.add(branchManagerName);
        fieldValues.add(branchManagerMobileNumber);
        fieldValues.add(center);
        fieldValues.add(centerCode);
        fieldValues.add(formatDate(meetingDate));
        fieldValues.add(meetingDay);
        fieldValues.add(meetingTime);
        fieldValues.add(repaymentFrequency);
        fieldValues.add(activeMemberCount);
        fieldValues.add(activeLoanCount);
        fieldValues.add(outstandingAmount);

        String returnValue = String.join("*", fieldValues);
        returnValues.add(returnValue);

        String outValue = String.join(",", fieldValues);
        outvalues.add(outValue);

    }

    private double getecbdetails(Contract contract) {

        double outstanding = 0.0;

        try {
            String balanceStr = getBalance(contract, "FFTOTPRINTAMT", TRADE);
            outstanding = Math.abs(Double.parseDouble(balanceStr));

        } catch (Exception e) {

            e.getMessage();
        }

        return outstanding;
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

    private void getDistrictName(String arrId2) {

        try {
            EbFfLoanDetailsRecord ebFfLoanDetailsRecord = new EbFfLoanDetailsRecord(
                    da.getRecord("EB.FF.LOAN.DETAILS", arrId2));
            branchDistrict = ebFfLoanDetailsRecord.getAddressType().get(0).getDistrictName().getValue();

        } catch (Exception e) {
            e.getMessage();
        }

    }

    public void getCompanyDetails(String selBranch) {

        try {
            companyRecord = new CompanyRecord(da.getRecord(COMPANY, selBranch));
            branchName = getCompanyDescription(selBranch);
            branchCode = selBranch;

            finMnemonic = companyRecord.getFinancialMne().getValue();
            mnemonic = companyRecord.getCustomerMnemonic().getValue();

            zoneName = getCompanyDescription(companyRecord.getLocalRefField("FF.ZONE").getValue());
            regionName = getCompanyDescription(companyRecord.getLocalRefField("FF.REGION").getValue());
            divisionName = getCompanyDescription(companyRecord.getLocalRefField("FF.DIVISION").getValue());
            clusterName = getCompanyDescription(companyRecord.getLocalRefField("FF.CLUSTER").getValue());
            branchState = companyRecord.getLocalRefField("FF.STATE").getValue();

        } catch (Exception e) {
            e.getMessage();

        }

    }

    public String getCompanyDescription(String companyCode) {

        String companyNam = "";
        try {
            if (companyCode != null && !companyCode.isEmpty()) {
                CompanyRecord companyRec = new CompanyRecord(da.getRecord(COMPANY, companyCode));
                companyNam = companyRec.getCompanyName().get(0).getValue();

                String[] compNamePart = companyNam.split("-");
                companyNam = compNamePart[0];

                return companyNam;
            }
        } catch (Exception e) {
            e.getMessage();
        }
        return companyNam;
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
                        String header = String.join(",", "zoneName", "regionName", "divisionName", "clusterName",
                                "branchName", "branchDistrict", "branchState", "branchCode", "relationshipOfficerName",
                                "relationshipOfficerMobileNumber", "branchManagerName", "branchManagerMobileNumber",
                                "centerName", "centerCode", "meetingDate", "meetingDay", "meetingTime",
                                "repaymentFrequency", "activeMemberCount", "activeLoanCount", "outstandingAmount");
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
