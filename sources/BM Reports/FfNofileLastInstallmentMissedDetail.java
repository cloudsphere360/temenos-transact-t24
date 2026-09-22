package com.temenos.fusion;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;

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
import com.temenos.t24.api.records.aaaccountdetails.BillIdClass;
import com.temenos.t24.api.records.aaaccountdetails.BillPayDateClass;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaarrangement.LinkedApplClass;
import com.temenos.t24.api.records.aaarrtermamount.AaArrTermAmountRecord;
import com.temenos.t24.api.records.aabilldetails.AaBillDetailsRecord;
import com.temenos.t24.api.records.aabilldetails.PropertyClass;
import com.temenos.t24.api.records.aacustomerarrangement.AaCustomerArrangementRecord;
import com.temenos.t24.api.records.aacustomerarrangement.ArrangementClass;
import com.temenos.t24.api.records.aacustomerarrangement.ProductLineClass;
import com.temenos.t24.api.records.aaprddesofficers.AaPrdDesOfficersRecord;
import com.temenos.t24.api.records.aaprddespaymentschedule.AaPrdDesPaymentScheduleRecord;
import com.temenos.t24.api.records.aaprddespaymentschedule.PaymentTypeClass;
import com.temenos.t24.api.records.aaproduct.AaProductRecord;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.records.account.AltAcctTypeClass;
import com.temenos.t24.api.records.company.CompanyRecord;
import com.temenos.t24.api.records.companyconsol.CompanyConsolRecord;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.records.deptacctofficer.DeptAcctOfficerRecord;
import com.temenos.t24.api.records.ebffcentredetail.EbFfCentreDetailRecord;
import com.temenos.t24.api.records.ebffcollectiondets.EbFfCollectionDetsRecord;
import com.temenos.t24.api.records.ebffcollectiondetshistory.EbFfCollectionDetsHistoryRecord;
import com.temenos.t24.api.records.ebffloandetails.AddressTypeClass;
import com.temenos.t24.api.records.ebffloandetails.EbFfLoanDetailsRecord;
import com.temenos.t24.api.records.ebffloandpd.DateClass;
import com.temenos.t24.api.records.ebffloandpd.EbFfLoanDpdRecord;
import com.temenos.t24.api.records.ebffloanstatusdetails.EbFfLoanStatusDetailsRecord;
import com.temenos.t24.api.records.ebffparameter.EbFfParameterRecord;
import com.temenos.t24.api.records.ebffparameter.ParamDescClass;
import com.temenos.t24.api.records.ebffrouser.EbFfRoUserRecord;
import com.temenos.t24.api.records.user.UserRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/**
 * 
 *
 * @author Souvagyaranjan Rout Created:10-JAN-2026 Attached as : NO FILE ENQUIRY
 *         Routine JAR NAME: FfNofileLastInstallmentMissedDetail.jar
 *         EB.API>FF.E.LAST.INST.MISSED.DETAIL.RPT
 *         STANDARD.SELECTION>NOFILE.FF.LAST.INST.MISSED.DETAIL.RPT Attached
 *         to:ENQUIRY>NOFILE.FF.BM.LAST.INST.MISD.DETAIL.RPT
 *         -----------------------------------------------------------------------------
 *         Modification History :
 *         -----------------------------------------------------------------------------
 *         02-FEB-2026 Remaping Souvagyaranjan Rout
 *         --------------------------------------------
 *
 */
public class FfNofileLastInstallmentMissedDetail extends Enquiry {

    public static final String DATE_RANGE_ERR = "EB-FF.DATE.RANGE.GREATER";
    public static final String NO_REC_ERR = "EB-NO.REC.SELECT";
    public static final String SEL_APP_CUS = "CUSTOMER";
    public static final String SEL_APP_ARR = "AA.ARRANGEMENT";
    public static final String SEL_APP_ARR_ACCOUNT = "AA.ARR.ACCOUNT";
    private static final String FILE_NAME = "LastInsMissedRep_Det";
    public static final String SEL_APP_COMP = "COMPANY";
    public static final String FF_RELIG_GROUP = "FF.RELIG.GROUP";
    public static final String FF_LOAN_PURP = "FF.LOAN.PURP";
    public static final String FF_LOAN_CYCLE = "FF.LOAN.CYCLE";
    DataAccess da = new DataAccess(this);
    Session ss = new Session(this);
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    // newchange

    String todayDate = "";
    String finMnemonic = "";
    String mnemonic = "";
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
    boolean onlyDateFilter = false;
    boolean dateErrFlag = false;
    boolean noRecErrFlag = false;
    String selProduct = "";
    String selCenterName = "";
    String selVillage = "";
    String selDist = "";
    String selLoanCycle = "";
    String selLoanPurp = "";
    String selCaste = "";
    int totalDueCount = 0;
    int count = 0;
    String selReligGrp = "";
    String startDateAccDet = "";
    LocalDate startDateArrAcc;
    String arrAgeStatus = "";
    String ffZone = "";
    String ffRegion = "";
    String ffDivision = "";
    String ffCluster = "";
    String ffCenter = "";
    String coCode = "";

    String cusId = "";
    String givenName = "";
    String familyName = "";
    String cusName = "";
    String dateOfBirth = "";
    String branch = "";
    int age = 0;
    String districtName = "";
    String stateName = "";
    int customerAge = 0;
    String religGrp = "";
    String fmOccup = "";
    String loanPurp = "";
    List<LinkedApplClass> linkedAppList = null;
    List<ProductLineClass> productLineList = null;
    String accNum = "";
    AaArrTermAmountRecord aaArrTermAmt = null;
    EbFfLoanStatusDetailsRecord ebFfsatusDetail = null;
    String loanAmount = "";
    String loanCycle = "";
    AaPrdDesOfficersRecord aaprdDesOffRec = null;
    String officerCode = "";

    List<BillPayDateClass> billPayDateList = null;
    String billStatus = "";
    LocalDate billPayDate;
    LocalDate billPayDates;
    LocalDate prevDate;
    String fastInstallmentDate = "";
    String billType = "";
    String billid = "";
    List<PropertyClass> billProperty = null;
    List<PropertyClass> billPropertylist = null;
    String osPropAmt = "";
    String previousMissedInstallmentDate = "";

    String companyIds = "";
    AaArrangementRecord aaArrRec = null;

    CustomerRecord cusRec = null;

    List<String> installmentNo = new ArrayList<>();
    List<String> returnVal = new ArrayList<>();
    List<String> finalArrayList = new ArrayList<>();
    String selDateTo = "";
    DeptAcctOfficerRecord deptOfferRec = null;
    String productDet = "";
    String officerName = "";
    String relationshipOfficerMobileNumber = "";
    String branchManagerName = "";
    String branchManagerMobileNum = "";
    String dpdBucket = "";
    String branchManagerMobileNumber = "";
    String writeOff = "";
    String currentMissedInstallmentDate = "";
    BillPayDateClass billPayDateSet;
    String replaymentFrrequency = "";
    EbFfLoanDetailsRecord loanDetRec = null;
    String previousInstallmentDate = "";
    double sumOfMissedinstallmentAmount = 0.0;
    double outstndgBal = 0;
    String companyId = "";
    String paramPath = "";
    String paraDesc = "";
    List<ParamDescClass> paramDescList = new ArrayList<>();
    DateTimeFormatter outDateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HHmmss");
    Session session = new Session(this);
    String field = "";
    String startDateArr = "";
    String seluser = "";

    String origContractDate = "";
    String disbursementDate = "";
    boolean legacy = false;
    String legacyAcctnum = "";
    String centerId = "";
    String relationshipOfficerName = "";
    String loanAccountNumber = "";
    String scdName = "";
    String fstName = "";
    String loanDate = "";
    String acNum = "";
    String userName = "";
    String customerNumber = "";
    String fldName = "";
    int counts = 0;
    List<String> outvalues = new ArrayList<>();

    @Override
    public List<String> setIds(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {

        try {

            todayDate = session.getCurrentVariable("!TODAY");
            Contract contract = new Contract(this);
            for (FilterCriteria filter : filterCriteria) {
                if (filter.getFieldname().equals("BRANCH")) {
                    branch = filter.getValue();
                    initialiseCompanyInfo(branch);
                    getLinkedCompIds(branch);
                    break;
                }
            }
            Set<String> selectionSet = getFilterCriteriaDets(filterCriteria);

            Set<String> overAllArrAccDetIdList = new LinkedHashSet<>(da.selectRecords("", SEL_APP_ARR, "",
                    "WITH ARR.STATUS NE PENDING.CLOSURE AND ARR.STATUS NE CLOSE AND CO.CODE EQ " + companyIds));

            Set<String> preFinalSet = getPreFinalset(overAllArrAccDetIdList, selectionSet);

            preFinalSet.retainAll(overAllArrAccDetIdList);

            for (String selectionArrId : preFinalSet) {

                boolean isOverdueAcct = getDpdDteails(selectionArrId);
                if (isOverdueAcct) {
                    contract.setContractId(selectionArrId);
                    getArrangementFieldMappingDet(selectionArrId);

                    getArrTermAmountDet(contract);
                    getPrimaryOfficer(contract);
                    getRecentBillBasedOnCurrntDate(selectionArrId);
                    getDeathCusDetails(selectionArrId);
                    getFfloanStatusDetails(contract);

                    List<String> row = new ArrayList<>();
                    row.add(zoneName);
                    row.add(regionName);

                    row.add(divisionName);
                    row.add(clusterName);

                    row.add(branchName);
                    row.add(districtName);

                    row.add(stateName);
                    row.add(branchCode);

                    row.add(ffCenter);
                    row.add(centerId);

                    row.add(cusName);
                    row.add(customerNumber);

                    row.add(String.valueOf(customerAge));
                    row.add(religGrp);

                    row.add(fmOccup);
                    row.add(loanPurp);

                    row.add(loanAccountNumber);
                    row.add(legacyAcctnum);
                    row.add(loanDate);
                    row.add(loanAmount);
                    row.add(productDet);

                    row.add(loanCycle);
                    row.add(replaymentFrrequency);

                    row.add(relationshipOfficerName);
                    row.add(relationshipOfficerMobileNumber);
                    row.add(userName);
                    row.add(branchManagerMobileNumber);

                    row.add(String.valueOf(totalDueCount));

                    row.add(currentMissedInstallmentDate);
                    row.add(previousInstallmentDate);
                    row.add(fastInstallmentDate);
                    row.add(String.valueOf(sumOfMissedinstallmentAmount));

                    returnVal.add(String.join("*", row));

                    outvalues.add(String.join(",", row));

                }
            }

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
            String outputPath = filePath + FILE_NAME + "_" + branchName + "_" + seluser + "_" + currDate + "_"
                    + currTime + ".csv";

            writeToFile(outvalues, outputPath);

        } catch (Exception e) {
            e.getMessage();
        }

        if (noRecErrFlag) {
            throw new T24CoreException("", NO_REC_ERR);
        } else {
            return returnVal;
        }
    }

    private void getFfloanStatusDetails(Contract contract) {

        try {
            AaPrdDesPaymentScheduleRecord aaArrPaySchRec = new AaPrdDesPaymentScheduleRecord(contract
                    .getConditionForProperty(contract.getPropertyIdsForPropertyClass("PAYMENT.SCHEDULE").get(0)));

            List<PaymentTypeClass> paymentTypeList = aaArrPaySchRec.getPaymentType();
            for (PaymentTypeClass paymentType : paymentTypeList) {
                if (paymentType.getPaymentType().getValue().equals("CONSTANT")
                        && paymentType.getPaymentMethod().getValue().equals("DUE")) {
                    replaymentFrrequency = convertFrequencyToWord(paymentType.getPaymentFreq().getValue());

                    return;
                }
            }
        } catch (Exception e) {
            e.getMessage();
        }

    }

    public String convertFrequencyToWord(String frequencyValue) {
        if (frequencyValue == null || frequencyValue.trim().isEmpty()) {
            return "";
        }

        for (String part : frequencyValue.split("\\s+")) {
            String word = parseFrequencyPart(part);
            if (word != null) {
                return word;
            }
        }
        return "";
    }

    public String parseFrequencyPart(String part) {
        if (part == null) {
            return null;
        }

        part = part.trim().toUpperCase();
        if (part.length() < 2) {
            return null;
        }

        String numberPart = part.substring(1, part.length() - 1);
        char unit = part.charAt(part.length() - 1);

        int value;
        try {
            value = Integer.parseInt(numberPart);
        } catch (NumberFormatException e) {
            return null;
        }

        if (value <= 0) {
            return null;
        }

        switch (unit) {
        case 'D':
            return value + " Day" + (value > 1 ? "s" : "");
        case 'W':
            return value + " Week" + (value > 1 ? "s" : "");
        case 'M':
            return value + " Month" + (value > 1 ? "s" : "");
        case 'Y':
            return value + " Year" + (value > 1 ? "s" : "");
        default:
            return null;
        }
    }

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
        } catch (Exception e) {
            e.getMessage();
        }
    }

    private void getDeathCusDetails(String selectionArrId) {

        totalDueCount = 0;
        try {
            if (legacy) {
                String collectonId = selectionArrId + "-" + startDateArr + ".01";

                EbFfCollectionDetsHistoryRecord ebffCollectionHis = new EbFfCollectionDetsHistoryRecord(
                        da.getRecord(finMnemonic, "EB.FF.COLLECTION.DETS.HISTORY", "", "" + collectonId));

                totalDueCount = ebffCollectionHis.getDueDate().size();

            } else {

                EbFfCollectionDetsRecord ebffCollection = new EbFfCollectionDetsRecord(
                        da.getRecord(finMnemonic, "EB.FF.COLLECTION.DETS", "", "" + selectionArrId));

                int size = ebffCollection.getDueDate().size();

                totalDueCount = (size > 0) ? size - 1 : 0;

            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    private void getRecentBillBasedOnCurrntDate(String selectionArrId) {

        currentMissedInstallmentDate = "";
        previousInstallmentDate = "";
        fastInstallmentDate = "";
        sumOfMissedinstallmentAmount = 0.0;

        try {
            AaAccountDetailsRecord aaAccountDet = new AaAccountDetailsRecord(
                    da.getRecord(finMnemonic, "AA.ACCOUNT.DETAILS", "", selectionArrId));
            LocalDate today = LocalDate.parse(todayDate, formatter);
            List<LocalDate> billDates = new ArrayList<>();

            billPayDateList = aaAccountDet.getBillPayDate();

            for (int i = 0; i < billPayDateList.size(); i++) {

                BillPayDateClass billPayDateObj = billPayDateList.get(i);

                List<BillIdClass> billIdList = billPayDateObj.getBillId();

                for (BillIdClass billIds : billIdList) {

                    LocalDate payDate = LocalDate.parse(billPayDateObj.getBillPayDate().getValue(), formatter);

                    billType = billIds.getBillType().getValue();
                    billStatus = billIds.getSetStatus().getValue();

                    if (payDate != null && (payDate.isBefore(today) || payDate.isEqual(today))
                            && "INSTALLMENT".equals(billType)
                            && ("UNSETTLED".equals(billStatus) || "UNPAID".equals(billStatus))) {

                        billDates.add(payDate);

                        billid = billIds.getBillId().getValue().replace("/", "");
                        AaBillDetailsRecord aaBillDetRec = new AaBillDetailsRecord(
                                da.getRecord(finMnemonic, "AA.BILL.DETAILS", "", billid));

                        osPropAmt = aaBillDetRec.getOrTotalAmount().getValue();
                        sumOfMissedinstallmentAmount += Double.parseDouble(osPropAmt);
                    }
                }
            }

            Collections.sort(billDates);

            int size = billDates.size();

            if (size > 0) {
                fastInstallmentDate = billDates.get(0).toString();

                currentMissedInstallmentDate = billDates.get(size - 1).toString();

                if (size > 1) {
                    previousInstallmentDate = billDates.get(size - 2).toString();

                }
            }

        } catch (Exception e) {
            e.getMessage();
        }
    }

    private void getPrimaryOfficer(Contract contract) {

        try {

            aaprdDesOffRec = new AaPrdDesOfficersRecord(contract.getConditionForProperty("LN.OFFICER"));
            if (!aaprdDesOffRec.toString().isEmpty()) {
                officerCode = aaprdDesOffRec.getPrimaryOfficer().getValue();
                deptOfferRec = new DeptAcctOfficerRecord(da.getRecord("DEPT.ACCT.OFFICER", officerCode));
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    private void getArrangementFieldMappingDet(String selectionArrId) {

        loanDate = "";
        customerNumber = "";
        productDet = "";
        legacyAcctnum = "";
        loanCycle = "";
        loanPurp = "";
        centerId = "";
        try {
            aaArrRec = new AaArrangementRecord(da.getRecord(finMnemonic, SEL_APP_ARR, "", selectionArrId));
            initialiseCompanyInfo(aaArrRec.getCoCodeRec().getValue());
            loanAccountNumber = selectionArrId;

            cusId = aaArrRec.getCustomer(0).getCustomer().getValue();
            CustomerRecord cusRecord = new CustomerRecord(da.getRecord(SEL_APP_CUS, cusId));

            if (aaArrRec.getOrigContractDate().getValue() != null
                    && !aaArrRec.getOrigContractDate().getValue().isEmpty()) {
                legacy = true;
                loanDate = aaArrRec.getOrigContractDate().getValue();
                customerNumber = cusRecord.getMnemonic().getValue();

            } else {
                loanDate = aaArrRec.getStartDate().getValue();

                customerNumber = cusId;

            }
            startDateArr = aaArrRec.getStartDate().getValue();

            String product = aaArrRec.getProduct().get(0).getProduct().getValue();
            AaProductRecord aaProRec = new AaProductRecord(da.getRecord("AA.PRODUCT", product));

            productDet = aaProRec.getDescription(0).getValue();

            getCustomerDetails(cusId);
            getFfLoanDetails(selectionArrId);

            linkedAppList = aaArrRec.getLinkedAppl();
            forLoopAeeDates();

        } catch (Exception e) {
            e.getMessage();
        }
    }

    private void forLoopAeeDates() {

        try {
            for (LinkedApplClass linkedApp : linkedAppList) {
                if (linkedApp.getLinkedAppl().getValue().equals("ACCOUNT")) {
                    acNum = linkedApp.getLinkedApplId().getValue();

                }
            }
            AccountRecord account = new AccountRecord(da.getRecord(finMnemonic, "ACCOUNT", "", acNum));

            loanCycle = account.getLocalRefField(FF_LOAN_CYCLE).getValue();

            loanPurp = account.getLocalRefField(FF_LOAN_PURP).getValue();

            centerId = account.getLocalRefField("FF.CENTRE").getValue();

            if (legacy) {
                for (AltAcctTypeClass altType : account.getAltAcctType()) {

                    if ("LEGACY".equalsIgnoreCase(altType.getAltAcctType().getValue())) {

                        legacyAcctnum = altType.getAltAcctId().getValue();

                        break;
                    }
                }
            }
            if (centerId != null && !centerId.isEmpty()) {
                getofficerName(centerId);
            }
        } catch (Exception e) {
            e.getMessage();
        }

    }

    private void getFfLoanDetails(String selectionArrId) {

        try {
            loanDetRec = new EbFfLoanDetailsRecord(da.getRecord(finMnemonic, "EB.FF.LOAN.DETAILS", "", selectionArrId));
            if (!loanDetRec.toString().isEmpty()) {
                List<AddressTypeClass> addrsTypelist = loanDetRec.getAddressType();
                for (AddressTypeClass addrsType : addrsTypelist) {

                    districtName = addrsType.getDistrictName().getValue();

                }
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    private void getCustomerDetails(String cusId) {

        familyName = "";
        cusName = "";
        dateOfBirth = "";
        customerAge = 0;
        religGrp = "";
        fmOccup = "";
        try {
            cusRec = new CustomerRecord(da.getRecord(mnemonic, SEL_APP_CUS, "", cusId));
            for (TField name1 : cusRec.getName1()) {
                fstName = name1.getValue();
            }

            for (TField name2 : cusRec.getName2()) {
                scdName = name2.getValue();
            }

            familyName = cusRec.getFamilyName().getValue();
            cusName = getGivenName(fstName, scdName, familyName);
            dateOfBirth = cusRec.getDateOfBirth().getValue();
            customerAge = customerAgeCalculation(dateOfBirth);
            religGrp = cusRec.getLocalRefField(FF_RELIG_GROUP).getValue();
            fmOccup = cusRec.getLocalRefField("FF.FM.OCCUP").getValue();
        } catch (Exception e) {
            e.getMessage();
        }

    }

    private String getGivenName(String firstName, String middleName, String lastName) {

        StringBuilder sb = new StringBuilder();
        sb.append(firstName);
        if (!middleName.equals("")) {
            sb.append(" ");
            sb.append(middleName);
        }
        if (!lastName.equals("")) {
            sb.append(" ");
            sb.append(lastName);
        }

        return sb.toString();
    }

    private void getArrTermAmountDet(Contract contract) {

        try {
            aaArrTermAmt = new AaArrTermAmountRecord(contract.getConditionForProperty("COMMITMENT"));

            loanAmount = aaArrTermAmt.getAmount().getValue();

        } catch (Exception e) {
            e.getMessage();
        }

    }

    private int customerAgeCalculation(String dateOfBirth) {

        try {
            if (!dateOfBirth.isEmpty()) {
                LocalDate birthDate;
                birthDate = LocalDate.parse(dateOfBirth, formatter);

                LocalDate today = LocalDate.now();
                customerAge = Period.between(birthDate, today).getYears();

            }
        } catch (Exception e) {
            e.getMessage();
        }
        return customerAge;
    }

    private void getofficerName(String centerId) {

        ffCenter = "";
        branchManagerName = "";
        try {
            EbFfCentreDetailRecord centerRec = new EbFfCentreDetailRecord(
                    da.getRecord("", "EB.FF.CENTRE.DETAIL", "", centerId));

            ffCenter = centerRec.getCenterName().getValue();

            String ebrelationshipOffName = centerRec.getCurrentRo().getValue();

            if (ebrelationshipOffName != null && !ebrelationshipOffName.isEmpty()) {
                getffRoname(ebrelationshipOffName);
            }
            branchManagerName = centerRec.getBranchManager().getValue();

            if (branchManagerName != null && !branchManagerName.isEmpty()) {
                getbranchmobileNo(branchManagerName);
            }
        } catch (Exception e) {
            e.getMessage();
        }

    }

    private void getbranchmobileNo(String branchManagerName2) {

        userName = "";
        branchManagerMobileNumber = "";

        try {
            UserRecord user = new UserRecord(da.getRecord("USER", branchManagerName2));

            userName = user.getUserName().getValue();

            branchManagerMobileNumber = user.getLocalRefField("FF.MOBILE.NO").getValue();

        } catch (Exception e) {
            e.getMessage();
        }

    }

    private void getffRoname(String ebrelationshipOffName) {

        try {
            EbFfRoUserRecord roRec = new EbFfRoUserRecord(da.getRecord("", "EB.FF.RO.USER", "", ebrelationshipOffName));

            relationshipOfficerName = roRec.getRoName().getValue();

            relationshipOfficerMobileNumber = roRec.getRoMobileNumber().getValue();

        } catch (Exception e) {
            e.getMessage();
        }
    }

    private Set<String> getPreFinalset(Set<String> overAllArrAccDetIdList, Set<String> selectionSet) {
        Set<String> preFinalSet = new LinkedHashSet<>();
        if (selectionSet == null || selectionSet.isEmpty()) {
            if (!filterValSet.isEmpty()) {
                noRecErrFlag = true;
            } else {
                preFinalSet = overAllArrAccDetIdList;
            }
        } else {
            preFinalSet = new LinkedHashSet<>(selectionSet);
        }
        return preFinalSet == null ? new LinkedHashSet<>() : preFinalSet;
    }

    private Set<String> getFilterCriteriaDets(List<FilterCriteria> filterCriteria) {

        Set<String> selectionSet = null;
        try {
            for (FilterCriteria filter : filterCriteria) {

                if (!filter.getFieldname().equals("BRANCH")) {
                    Set<String> currentFilterSet = new LinkedHashSet<>();
                    String value = filter.getValue();
                    if (value == null || value.isEmpty())
                        continue;

                    switch (filter.getFieldname()) {
                    case "USER":
                        seluser = value;
                        break;
                    case "DATE.FROM":
                        startDate = value;
                        onlyDateFilter = true;
                        break;
                    case "DATE.TO":
                        endDate = value;
                        break;
                    case "PRODUCT":
                        filterValSet.add(value);
                        selProduct = value;
                        currentFilterSet.addAll(
                                da.selectRecords(finMnemonic, SEL_APP_ARR, "", "WITH PRODUCT EQ " + selProduct));

                        break;
                    case "CENTER":
                        filterValSet.add(value);
                        selCenterName = value;
                        fldName = "FF.CENTRE";
                        currentFilterSet.addAll(getArrListFromCentre(fldName, selCenterName));

                        break;

                    case "VILLAGE":
                        filterValSet.add(value);
                        selVillage = value;
                        fldName = "FF.VILLAGE";
                        currentFilterSet.addAll(getArrListFromCentre(fldName, selVillage));

                        break;
                    case "DISTRICT":
                        filterValSet.add(value);
                        selDist = value;
                        fldName = "DISTRICT.NAME";
                        currentFilterSet.addAll(getCustomerArrList(fldName, selDist));

                        break;

                    case "CYCLE":
                        filterValSet.add(value);
                        selLoanCycle = value;
                        fldName = FF_LOAN_CYCLE;
                        currentFilterSet.addAll(getArrListFromCentre(fldName, selLoanCycle));

                        break;

                    case "PURPOSE":
                        filterValSet.add(value);
                        selLoanPurp = value;
                        fldName = FF_LOAN_PURP;
                        currentFilterSet.addAll(getArrListFromCentre(fldName, selLoanPurp));

                        break;
                    case "RELIGION":
                        filterValSet.add(value);
                        selReligGrp = value;
                        fldName = FF_RELIG_GROUP;

                        currentFilterSet.addAll(getCustomerArrList(fldName, selReligGrp));

                        break;
                    case "CASTE":
                        filterValSet.add(value);
                        selCaste = value;
                        fldName = "FF.CASTE";

                        currentFilterSet.addAll(getCustomerArrList(fldName, selCaste));

                        break;
                    default:

                    }

                    selectionSet = addToFinalSelectionSet(selectionSet, currentFilterSet);

                }
            }

        } catch (Exception e) {
            e.getMessage();
        }
        return selectionSet == null ? new LinkedHashSet<>() : selectionSet;
    }

    public Set<String> addToFinalSelectionSet(Set<String> selectionSet, Set<String> currentFilterSet) {
        if (selectionSet == null) {
            if (!currentFilterSet.isEmpty()) {
                selectionSet = currentFilterSet;
            }
        } else {
            selectionSet.retainAll(currentFilterSet);
        }
        return selectionSet;
    }

    public boolean getDpdDteails(String contractId) {

        try {
            String dpdId = "";
            LocalDate currDate = LocalDate.parse(todayDate, formatter);
            String monthText = currDate.format(DateTimeFormatter.ofPattern("MMM")).toUpperCase();
            String year = String.valueOf(currDate.getYear());

            dpdId = contractId + "-" + monthText + year;

            EbFfLoanDpdRecord ldpd = new EbFfLoanDpdRecord(da.getRecord(finMnemonic, "EB.FF.LOAN.DPD", "", dpdId));

            for (DateClass datecls : ldpd.getDate()) {
                String cudDate = datecls.getDate().getValue();
                LocalDate parsedDate = LocalDate.parse(cudDate, formatter);

                if (parsedDate.equals(currDate)) {

                    int dpdValue = Integer.parseInt(datecls.getCurDpd().getValue());

                    if (dpdValue > 0) {
                        return true;

                    }
                }
            }
        } catch (Exception e) {
            e.getMessage();

        }
        return false;

    }

    public List<String> getCustomerArrList(String fieldName, String fieldValue) {

        Set<String> customerSet = new LinkedHashSet<>(
                da.selectRecords(mnemonic, SEL_APP_CUS, "", "WITH " + fieldName + " EQ " + fieldValue));
        Set<String> arrCustomerSet = new LinkedHashSet<>(
                da.selectRecords(finMnemonic, "AA.CUSTOMER.ARRANGEMENT", "", ""));
        customerSet.retainAll(arrCustomerSet);
        return getArrListFromsel(customerSet);

    }

    private List<String> getArrListFromsel(Set<String> cusIdList) {

        List<String> arrIdList = new ArrayList<>();
        try {
            for (String cusid : cusIdList) {
                AaCustomerArrangementRecord aaCusRec = new AaCustomerArrangementRecord(
                        da.getRecord(finMnemonic, "AA.CUSTOMER.ARRANGEMENT", "", cusid));
                for (ProductLineClass productLine : aaCusRec.getProductLine()) {
                    if (productLine.getProductLine().getValue().equals("LENDING")) {
                        for (ArrangementClass arrIdFrmAAcus : productLine.getArrangement()) {
                            arrIdList.add(arrIdFrmAAcus.getArrangement().getValue());
                        }
                        break;
                    }
                }

            }
        } catch (Exception e) {
            e.getMessage();
        }
        return arrIdList;
    }

    public List<String> getArrListFromCentre(String fieldName, String fieldValue) {
        List<String> arrAccList = da.selectRecords(finMnemonic, SEL_APP_ARR_ACCOUNT, "",
                "WITH " + fieldName + " EQ " + fieldValue);

        return getArrListFromSelection(arrAccList);
    }

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
        } catch (Exception e) {
            e.getMessage();
        }
        return arrIdList;
    }

    private void initialiseCompanyInfo(String companyId) {

        try {

            CompanyRecord companyObj = new CompanyRecord(da.getRecord(SEL_APP_COMP, companyId));
            finMnemonic = companyObj.getFinancialMne().getValue();
            mnemonic = companyObj.getCustomerMnemonic().getValue();
            branchCode = companyId;
            branchName = getCompanyDescription(companyId);
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
        String companyName = "";

        try {
            if (companyCode != null && !companyCode.isEmpty()) {
                CompanyRecord companyRecs = new CompanyRecord(da.getRecord(SEL_APP_COMP, companyCode));
                companyName = companyRecs.getCompanyName().get(0).getValue();
                String[] compNamePart = companyName.split("-");
                companyName = compNamePart[0];
                return companyName;
            }
        } catch (Exception e) {
            e.getMessage();
        }
        return companyName;
    }

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

                        String header = String.join(",", "Zone Name", "Region Name", "Division Name", "Cluster Name",
                                "Branch Name", "Branch District", "Branch State", "Branch Code", "Center Name",
                                "Center Code", "Customer Name", "Customer Number", "Customer Age", "Religion",
                                "Occupation", "Purpose", "Loan Account Number", "Legacy Account Number", "Loan Date",
                                "Loan Amount", "Product Name", "Cycle", "Repayment frequency",
                                "Relationship Officer Name", "Relationship Officer Mobile Number",
                                "Branch Manager Name", "Branch Manager Mobile Number", "Installment No",
                                "Current missed Installment Date", "Previous missed installment date",
                                "First missed installment date", "InstallmentAmount");

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
