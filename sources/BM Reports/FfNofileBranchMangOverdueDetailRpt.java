package com.temenos.fusion;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import com.temenos.api.TField;
import com.temenos.api.exceptions.T24CoreException;
import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.aa.contractapi.BalanceMovement;
import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.t24.api.records.aaaccountdetails.AaAccountDetailsRecord;
import com.temenos.t24.api.records.aaaccountdetails.BillIdClass;
import com.temenos.t24.api.records.aaaccountdetails.BillPayDateClass;
import com.temenos.t24.api.records.aaactivityhistory.AaActivityHistoryRecord;
import com.temenos.t24.api.records.aaactivityhistory.ActivityRefClass;
import com.temenos.t24.api.records.aaactivityhistory.EffectiveDateClass;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaarrangement.LinkedApplClass;
import com.temenos.t24.api.records.aaarrtermamount.AaArrTermAmountRecord;
import com.temenos.t24.api.records.aabilldetails.AaBillDetailsRecord;
import com.temenos.t24.api.records.aabilldetails.BillStatusClass;
import com.temenos.t24.api.records.aabilldetails.PropertyClass;
import com.temenos.t24.api.records.aacustomerarrangement.AaCustomerArrangementRecord;
import com.temenos.t24.api.records.aacustomerarrangement.ArrangementClass;
import com.temenos.t24.api.records.aacustomerarrangement.ProductLineClass;
import com.temenos.t24.api.records.aaprddesaccount.AaPrdDesAccountRecord;
import com.temenos.t24.api.records.aaprddesofficers.AaPrdDesOfficersRecord;
import com.temenos.t24.api.records.aaproduct.AaProductRecord;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.records.account.AltAcctTypeClass;
import com.temenos.t24.api.records.company.CompanyRecord;
import com.temenos.t24.api.records.companyconsol.CompanyConsolRecord;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.records.deptacctofficer.DeptAcctOfficerRecord;
import com.temenos.t24.api.records.ebffcentredetail.EbFfCentreDetailRecord;
import com.temenos.t24.api.records.ebffrouser.EbFfRoUserRecord;
import com.temenos.t24.api.records.user.UserRecord;
import com.temenos.t24.api.records.ebffloandetails.AddressTypeClass;
import com.temenos.t24.api.records.ebffloandetails.EbFfLoanDetailsRecord;
import com.temenos.t24.api.records.ebffloandetails.FfCurDodStsClass;
import com.temenos.t24.api.records.ebffloandetails.FmEntityNumberClass;
import com.temenos.t24.api.records.ebffloandpd.DateClass;
import com.temenos.t24.api.records.ebffloandpd.EbFfLoanDpdRecord;
import com.temenos.t24.api.records.ebffloanpaymenthis.DemandDateClass;
import com.temenos.t24.api.records.ebffloanpaymenthis.EbFfLoanPaymentHisRecord;
import com.temenos.t24.api.records.ebffparameter.EbFfParameterRecord;
import com.temenos.t24.api.records.ebffparameter.ParamDescClass;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/*-----------------------------------------------------------------------------
 * @author Harshini Sakthivel
 * Date Created: 23-Dec-2025 
 * Attached as : Nofile Enquiry Routine
 * ENQUIRY :  NOFILE.FF.BM.OVERDUE.DET.RPT
 * EB.API : FF.E.BM.OVERDUE.DET.RPT
 * Attached to : STANDARD.SELECTION > NOFILE.FF.BM.OVERDUE.DET.RPT
 * Description: Branch Online Report generation -> OverDue detail Report
 *------------------------------------------------------------------------------ 
 * Modification History : NA 
 *----------------------------------------------------------------------------- 
*23-Dec-2025   Development      Initial Version
*-----------------------------------------------------------------------------
*/
public class FfNofileBranchMangOverdueDetailRpt extends Enquiry {
    /**
     * 
     */
    private static final String COMPANY = "COMPANY";
    public static final String DATE_RANGE_ERR = "EB-FF.DATE.RANGE.GREATER";
    public static final String NO_REC_ERR = "EB-NO.REC.SELECT";
    public static final String DATE_FILTER_ERR = "EB-FF.BM.DATE.FILTER.RANGE";
    public static final String SEL_APP_CUS = "CUSTOMER";
    public static final String TRADE = "TRADE";
    public static final String ACCOUNT = "ACCOUNT";
    public static final String AA_ARRANGEMENT = "AA.ARRANGEMENT";
    public static final String LOAN_DPD = "EB.FF.LOAN.DPD";
    public static final String AA_ARR_ACCOUNT = "AA.ARR.ACCOUNT";
    private static final String FILE_NAME = "OverdueRep_Det";
    public static final String CHARGEACC = "LENDING-CHARGEOFF-ACCOUNT";
    public static final String CHARGEARR = "LENDING-CHARGEOFF-ARRANGEMENT";
    public static final String WRITEOFF = "LENDING-WRITE.OFF-BAL.MAINTAIN";
    public static final String RELIG_GROUP = "FF.RELIG.GROUP";
    public static final String CASTE = "FF.CASTE";
    public static final String DISTRICT_NAME = "DISTRICT.NAME";

    DataAccess da = new DataAccess(this);
    Session ss = new Session(this);
    Session session = new Session(this);
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    DateTimeFormatter outDateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HHmmss");
    String prdDes = "";
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
    boolean dateFilterErrFlag = false;
    boolean dateRangeErrFlag = false;
    boolean legacy = false;
    String selProduct = "";
    String selCenterName = "";
    String selVillage = "";
    String selDist = "";
    String selLoanCycle = "";
    String selLoanPurp = "";
    String selCaste = "";
    String selBranch = "";
    String selReligGrp = "";
    String selUser = "";
    String startDateAccDet = "";
    LocalDate startDateArrAcc;
    String arrAgeStatus = "";
    String ffZone = "";
    String ffRegion = "";
    String ffDivision = "";
    String ffCluster = "";
    String ffCenter = "";
    String coCode = "";
    String companyIds = "";
    String companyName = "";
    String cusId = "";
    String givenName = "";
    String familyName = "";
    String cusName = "";
    String dateOfBirth = "";
    String dateRangeVal = "";
    String dateFilterVal = "";
    int age = 0;
    String districtName = "";
    String stateName = "";
    int customerAge = 0;
    String religGrp = "";
    String fmOccup = "";
    String loanPurp = "";
    String branch = "";
    String todayDate = session.getCurrentVariable("!TODAY");
    LocalDate date = LocalDate.parse(todayDate, formatter);
    List<LinkedApplClass> linkedAppList = null;
    List<ProductLineClass> productLineList = null;
    String accNum = "";
    AaArrTermAmountRecord aaArrTermAmt = null;
    String loanAmount = "";
    String loanCycle = "";
    AaPrdDesOfficersRecord aaprdDesOffRec = null;
    String officerCode = "";
    List<String> loanDpdIdList = null;
    List<DateClass> loanDpdDatelist = null;
    String curDpd = "";
    String loanDpdCnt = "";
    String deathFlaged = "";
    List<BillPayDateClass> billPayDateList = null;
    String billStatus = "";
    LocalDate billPayDate;
    String billType = "";
    String billid = "";
    List<PropertyClass> billPropertylist = null;
    String orPropAmt = "";
    double loanOverdueCnt0to30 = 0.0;
    double loanOverdueCnt31to60 = 0.0;
    String formattedloanOverdueCnt31to60 = "";
    double loanOverdueCnt61to90 = 0.0;
    double loanOverdueCnt91to120 = 0.0;
    double loanOverdueCnt121to180 = 0.0;
    double loanOverdueCnt181t365 = 0.0;
    double loanOverdueCntAbove365 = 0.0;
    double totalOverdueAmount = 0.0;
    double outstandingBalance = 0.0;
    String ob = "";
    LocalDate paiddate = null;
    AaArrangementRecord aaArrRec = null;
    CompanyRecord companyRec = null;
    CustomerRecord cusRec = null;
    AaBillDetailsRecord aaBillDetRec = null;

    List<String> returnVal = new ArrayList<>();
    List<String> finalArrayList = new ArrayList<>();
    List<String> outvalues = new ArrayList<>();

    DeptAcctOfficerRecord deptOfferRec = null;
    String productDet = "";
    String officerName = "";
    String relationshipOfficerMobileNumber = "";
    String branchManagerName = "";
    String branchManagerMobileNum = "";
    String dpdBucket = "";
    String branchManagerMobileNumber = "";
    String writeOffVal = "";
    String lastInstallmentPaidDate = "";
    BillPayDateClass billPayDateSet;
    double currentInstalmentAmount = 0;
    String companyId = "";
    String overdueLoanAmt1 = "";
    String ffCentreCode = "";
    String centreName = "";
    String currentRo = "";
    String selDateFrom = "";
    String selDateTo = "";
    String accNumId = "";
    String cusMnemonic = "";
    String legAccNum = "";
    String legAccNum1 = "";
    boolean dateSelected = false;
    boolean dateSelected1 = false;
    String name2 = "";
    String name1 = "";
    String curDpd1 = "";
    String filePath = "";
    String branchState = "";
    String lastInstallmentPaidDate1 = "";
    List<String> lastInstallmentPaidDateList = new ArrayList<>();
    int overallSelectSize = 0;
    int count = 0;

    @Override
    public List<String> setIds(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {

        Contract contract = new Contract(this);
        fcChkBranch(filterCriteria);

        Set<String> selectionSet = getFilterCriteriaDets(filterCriteria);

        try {
            Set<String> overAllArrAccDetIdList = new LinkedHashSet<>(da.selectRecords(finMnemonic, AA_ARRANGEMENT, "",
                    "WITH ARR.STATUS NE CLOSE PENDING.CLOSURE AND CO.CODE EQ " + companyIds));

            overallSelectSize = overAllArrAccDetIdList.size();

            Set<String> preFinalSet = getPreFinalset(overAllArrAccDetIdList, selectionSet);
            preFinalSet.retainAll(overAllArrAccDetIdList);
            LocalDate today = LocalDate.parse(todayDate, formatter);
            LocalDate start = selDateFrom.isEmpty() ? today : LocalDate.parse(selDateFrom, formatter);
            LocalDate end = selDateTo.isEmpty() ? today : LocalDate.parse(selDateTo, formatter);
            selArrId(contract, preFinalSet, today, start, end);
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
            String outputPath = filePath + FILE_NAME + "_" + selBranch + "_" + selUser + "_" + currDate + "_" + currTime
                    + ".csv";
            writeToFile(outvalues, outputPath);
        } catch (Exception e) {
            e.getMessage();
        }
        if (noRecErrFlag || returnVal.isEmpty()) {
            throw new T24CoreException("", NO_REC_ERR);

        } else {
            return returnVal;
        }
    }

    /**
     * @param contract
     * @param preFinalSet
     * @param today
     * @param start
     * @param end
     */
    private void selArrId(Contract contract, Set<String> preFinalSet, LocalDate today, LocalDate start, LocalDate end) {

        for (String selectionArrId : preFinalSet) {
            getResetVariables();
            AaAccountDetailsRecord aaAccountDet = null;
            try {
                aaAccountDet = new AaAccountDetailsRecord(
                        da.getRecord(finMnemonic, "AA.ACCOUNT.DETAILS", "", selectionArrId));

            } catch (Exception e) {
                e.getMessage();
            }
            if (aaAccountDet != null && !aaAccountDet.toString().isEmpty()) {

                arrangementStDtChk(contract, today, start, end, selectionArrId, aaAccountDet);
            }

        }
    }

    /**
     * @param contract
     * @param today
     * @param start
     * @param end
     * @param selectionArrId
     * @param aaAccountDet
     */
    private void arrangementStDtChk(Contract contract, LocalDate today, LocalDate start, LocalDate end,
            String selectionArrId, AaAccountDetailsRecord aaAccountDet) {

        arrAgeStatus = aaAccountDet.getArrAgeStatus().getValue();

        if (arrAgeStatus == null || arrAgeStatus.isEmpty()) {
            return;
        }

        if (!onlyDateFilter) {
            totalProcessArrId(contract, today, selectionArrId);
            return;
        }

        startDateAccDet = aaAccountDet.getStartDate().getValue();

        if (startDateAccDet == null || startDateAccDet.isEmpty()) {
            return;
        }

        startDateArrAcc = LocalDate.parse(startDateAccDet, formatter);

        if (start.isAfter(end)) {
            dateErrFlag = true;
        }

        if (startDateArrAcc.compareTo(start) >= 0 && startDateArrAcc.compareTo(end) <= 0) {
            totalProcessArrId(contract, today, selectionArrId);
        }
    }

    private void totalProcessArrId(Contract contract, LocalDate today, String selectionArrId) {

        try {
            if (arrAgeStatus.equals("SM0") || arrAgeStatus.equals("SM1") || arrAgeStatus.equals("SM2")
                    || arrAgeStatus.equals("NPA")) {

                finalArrayList.add(selectionArrId);
                contract.setContractId(selectionArrId);

                getArrangementFieldMappingDet(selectionArrId);
                getEbFfLoanDetails(selectionArrId);
                getArrTermAmountDet(contract);
                getRecentBillBasedOnCurrntDate(selectionArrId, today);
                getEcbBalanceBasedOnDPDcnt(contract, selectionArrId);
                getDeathCusDetails(selectionArrId);
                getWriteOffFld(selectionArrId);
                List<String> row = new ArrayList<>();
                if (!curDpd.isEmpty() && !curDpd.equals("0")) {
                    row.add(zoneName);
                    row.add(regionName);
                    row.add(divisionName);
                    row.add(clusterName);
                    row.add(branchName);
                    row.add(districtName);
                    row.add(branchState);
                    row.add(coCode);
                    row.add(centreName);
                    row.add(ffCentreCode);
                    row.add(cusName);
                    row.add(cusId);
                    row.add(String.valueOf(customerAge));
                    row.add(religGrp);
                    row.add(fmOccup);
                    row.add(loanPurp);
                    row.add(selectionArrId);
                    row.add(legAccNum);
                    row.add(startDate);
                    row.add(loanAmount);
                    row.add(prdDes);
                    row.add(loanCycle);
                    row.add(officerName);
                    row.add(relationshipOfficerMobileNumber);
                    row.add(branchManagerName);
                    row.add(branchManagerMobileNumber);
                    row.add(curDpd);
                    row.add(String.valueOf(currentInstalmentAmount));
                    row.add(String.valueOf(loanOverdueCnt0to30));
                    row.add(String.valueOf(loanOverdueCnt31to60));
                    row.add(String.valueOf(loanOverdueCnt61to90));
                    row.add(String.valueOf(loanOverdueCnt91to120));
                    row.add(String.valueOf(loanOverdueCnt121to180));
                    row.add(String.valueOf(loanOverdueCnt181t365));
                    row.add(String.valueOf(loanOverdueCntAbove365));
                    row.add(String.valueOf(totalOverdueAmount));
                    row.add(ob);
                    row.add(lastInstallmentPaidDate);
                    row.add(deathFlaged);
                    row.add(writeOffVal);
                    returnVal.add(String.join("*", row));
                    outvalues.add(String.join(",", row));

                }

            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    /**
     * @param selectionArrId
     */
    private void getEbFfLoanDetails(String selectionArrId) {

        try {
            String relation = "";
            EbFfLoanDetailsRecord loanDetRec = new EbFfLoanDetailsRecord(
                    da.getRecord(finMnemonic, "EB.FF.LOAN.DETAILS", "", selectionArrId));

            List<AddressTypeClass> addrsTypelist = loanDetRec.getAddressType();
            for (AddressTypeClass addrsType : addrsTypelist) {
                districtName = addrsType.getDistrictName().getValue();
            }
            List<FmEntityNumberClass> fmEntityRel = loanDetRec.getFmEntityNumber();
            for (FmEntityNumberClass fmEntity : fmEntityRel) {
                relation = fmEntity.getRelation().getValue();
                if (relation.equalsIgnoreCase("SELF")) {
                    fmOccup = fmEntity.getOccupation().getValue();
                }
            }

        } catch (Exception e) {

            e.getMessage();
        }

    }

    /**
     * @param selectionArrId
     */
    private void getWriteOffFld(String selectionArrId) {

        try {
            AaActivityHistoryRecord aaActHisRec = new AaActivityHistoryRecord(
                    da.getRecord(finMnemonic, "AA.ACTIVITY.HISTORY", "", selectionArrId));

            List<EffectiveDateClass> effectiveDateList = aaActHisRec.getEffectiveDate();
            for (EffectiveDateClass effectiveDate : effectiveDateList) {
                List<ActivityRefClass> activityRefList = effectiveDate.getActivityRef();
                for (ActivityRefClass activityRef : activityRefList) {
                    String activity = activityRef.getActivity().getValue();
                    if (activity.equals(CHARGEACC) || activity.equals(CHARGEARR) || activity.equals(WRITEOFF)) {
                        writeOffVal = "YES";
                    }
                }
            }

        } catch (Exception e) {
            e.getMessage();
        }
    }

    /**
    * 
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
                        String header = String.join(",", "Zone Name", "Region Name", "Division Name", "Cluster Name",
                                "Branch Name", "Branch District", "Branch State", "Branch Code", "Center Name",
                                "Center Code", "Customer Name", "Customer Number", "Customer Age", "Religion",
                                "Occupation", "Purpose", "Loan Account Number", "Legacy Account Number", "Loan Date",
                                "Loan Amount", "Product Name", "Cycle", "Relationship Officer Name",
                                "Relationship Officer Mobile Number", "Branch Manager Name",
                                "Branch Manager Mobile Number", "DPD bucket", "Current", "LoanCnt1-30", "LoanCnt31-60",
                                "LoanCnt61-90", "LoanCnt91-120", "LoanCnt120-180", "LoanCnt180-365", "LoanCnt>365",
                                "Total overdue amount", "Outstanding", "Last installment paid Date", "Death Flagged",
                                "Write off");
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

    public void getResetVariables() {
        loanOverdueCnt0to30 = 0.0;
        loanOverdueCnt31to60 = 0.0;
        loanOverdueCnt61to90 = 0.0;
        loanOverdueCnt91to120 = 0.0;
        loanOverdueCnt121to180 = 0.0;
        loanOverdueCnt181t365 = 0.0;
        loanOverdueCntAbove365 = 0.0;
        totalOverdueAmount = 0.0;
        currentInstalmentAmount = 0.0;
        outstandingBalance = 0.0;
        ob = "";
        curDpd = "";
        lastInstallmentPaidDate = "";
        deathFlaged = "";
        lastInstallmentPaidDateList.clear();
        stateName = "";
        districtName = "";
        centreName = "";
        cusName = "";
        customerAge = 0;
        cusId = "";
        religGrp = "";
        fmOccup = "";
        loanPurp = "";
        loanCycle = "";
        accNum = "";
        legAccNum = "";
        prdDes = "";
        officerName = "";
        relationshipOfficerMobileNumber = "";
        branchManagerName = "";
        branchManagerMobileNumber = "";
        dpdBucket = "";
        writeOffVal = "";
    }

    /*
     * method:getDeathCusDetails Description: This method is used to check the death
     * customer
     */
    private void getDeathCusDetails(String selectionArrId) {

        try {
            EbFfLoanDetailsRecord loanDetRec = new EbFfLoanDetailsRecord(
                    da.getRecord(finMnemonic, "EB.FF.LOAN.DETAILS", "", selectionArrId));
            if (!loanDetRec.toString().isEmpty()) {
                List<AddressTypeClass> addrsTypelist = loanDetRec.getAddressType();
                for (AddressTypeClass addrsType : addrsTypelist) {
                    stateName = addrsType.getStateName().getValue();
                    if (!stateName.isEmpty()) {
                        break;
                    }
                }
                List<FfCurDodStsClass> currDodStatuslist = loanDetRec.getFfCurDodSts();
                getDeathFlagDets(loanDetRec, currDodStatuslist);
            }
        } catch (Exception e) {
            e.getMessage();
        }

    }

    /**
     * @param loanDetRec
     * @param currDodStatuslist
     */
    private void getDeathFlagDets(EbFfLoanDetailsRecord loanDetRec, List<FfCurDodStsClass> currDodStatuslist) {

        for (FfCurDodStsClass currDodStatus : currDodStatuslist) {
            if (currDodStatus.getFfCurDodSts().getValue().equalsIgnoreCase("DECEASED")) {
                List<FmEntityNumberClass> fmEntitynumlist = loanDetRec.getFmEntityNumber();
                for (FmEntityNumberClass fmEntitynum : fmEntitynumlist) {
                    String dateOfDeath = fmEntitynum.getDateOfDeath().getValue();
                    if (!dateOfDeath.isEmpty()) {
                        deathFlaged = "YES";
                    }
                }
            }
        }
    }

    /*
     * method:getEcbBalanceBasedOnDPDcnt Description: This method is used to get the
     * ECB balance based on the CUR.DPD values
     */
    private void getEcbBalanceBasedOnDPDcnt(Contract contract, String selectionArrId) {

        try {

            String formatted = date.getMonth().toString().substring(0, 3) + date.getYear();
            String loanDpdId = selectionArrId + "-" + formatted;
            EbFfLoanDpdRecord loanDpdRec = new EbFfLoanDpdRecord(da.getRecord(finMnemonic, LOAN_DPD, "", loanDpdId));
            loanDpdDatelist = loanDpdRec.getDate();
            int dpdDateListSize = loanDpdDatelist.size();
            curDpd1 = "";
            curDpd1 = loanDpdRec.getDate().get(dpdDateListSize - 1).getCurDpd().getValue();
            if (!curDpd1.equals("0")) {
                curDpd = curDpd1;
            }

            getLoanCount(contract);
            getTotalOverdueAmount(contract);

        } catch (Exception e) {
            e.getMessage();
        }
    }

    /**
     * @param contract
     */
    private void getLoanCount(Contract contract) {

        String loanCount91to120 = "";
        String loanCount61to90 = "";
        String loanCount31to60 = "";
        String loanCount0to30 = "";

        if ((Integer.parseInt(curDpd)) > 90 && (Integer.parseInt(curDpd)) <= 120) {
            loanCount91to120 = getEcbBalance(contract);
            loanOverdueCnt91to120 = Math.abs(Double.parseDouble(loanCount91to120));
        }

        else if ((Integer.parseInt(curDpd)) > 60 && (Integer.parseInt(curDpd)) <= 90) {
            loanCount61to90 = getEcbBalance(contract);
            loanOverdueCnt61to90 = Math.abs(Double.parseDouble(loanCount61to90));
        }

        else if ((Integer.parseInt(curDpd)) > 30 && (Integer.parseInt(curDpd)) <= 60) {
            loanCount31to60 = getEcbBalance(contract);
            loanOverdueCnt31to60 = Math.abs(Double.parseDouble(loanCount31to60));
        }

        else if ((Double.parseDouble(curDpd)) > 0 && (Double.parseDouble(curDpd)) <= 30) {
            loanCount0to30 = getEcbBalance(contract);
            loanOverdueCnt0to30 = Math.abs(Double.parseDouble(loanCount0to30));

        }
    }

    private void getTotalOverdueAmount(Contract contract) {

        String loanCount121to180 = "";
        String loanCount181to365 = "";
        String loanCount365 = "";
        String totalOverdue = "";

        if ((Integer.parseInt(curDpd)) > 120 && (Integer.parseInt(curDpd)) <= 180) {
            loanCount121to180 = getEcbBalance(contract);
            loanOverdueCnt121to180 = Math.abs(Double.parseDouble(loanCount121to180));

        } else if ((Integer.parseInt(curDpd)) > 180 && (Integer.parseInt(curDpd)) <= 365) {
            loanCount181to365 = getEcbBalance(contract);
            loanOverdueCnt181t365 = Math.abs(Double.parseDouble(loanCount181to365));

        } else if ((Integer.parseInt(curDpd)) >= 365) {
            loanCount365 = getEcbBalance(contract);
            loanOverdueCntAbove365 = Math.abs(Double.parseDouble(loanCount365));
        }
        if ((Integer.parseInt(curDpd)) >= 1) {
            outstandingBalance = getOutstandingBalance(contract);
            ob = String.format("%.2f", Math.abs(outstandingBalance));
            totalOverdue = getEcbBalance(contract);
            totalOverdueAmount = Math.abs(Double.parseDouble(totalOverdue));
        }
    }

    /*
     * method:getOutstandingBalance Description: This method is used to get the ECB
     * balance based on the Balance Type
     */
    private double getOutstandingBalance(Contract contract) {

        double outstndgBal = 0;
        try {
            String accBal1 = getBalance(contract, "FFALLOSTBAL", TRADE);
            outstndgBal = Double.parseDouble(accBal1);
        } catch (Exception e) {
            e.getMessage();
        }
        return outstndgBal;
    }

    /*
     * method:getEcbBalance Description: This method is used to get the ECB balance
     * based on the Balance Type
     */
    private String getEcbBalance(Contract contract) {

        double overdueAmt = 0;
        String overdueLoanAmt = "";
        try {
            String accBal = getBalance(contract, "FFALLOVRDUE", TRADE);
            overdueAmt = Double.parseDouble(accBal);
            overdueLoanAmt = String.format("%.2f", overdueAmt);
        } catch (Exception e) {
            e.getMessage();
        }
        return overdueLoanAmt;
    }

    /*
     * method:getBalance Description: This method is used to get the ECB balance
     * based on the Balance Type
     */
    private String getBalance(Contract contract, String accountType, String bookingType) {
        List<BalanceMovement> movements = null;
        try {
            movements = contract.getContractBalanceMovements(accountType, bookingType);
        } catch (Exception e) {
            e.getMessage();
        }

        return (movements != null && !movements.isEmpty()) ? movements.get(0).getBalance().toString() : "0";
    }

    /*
     * method:getRecentBillBasedOnCurrntDate Description: This method is used to get
     * the Recent Bill based On CurrntDate
     */

    private void getRecentBillBasedOnCurrntDate(String selectionArrId, LocalDate today) {

        currentInstalmentAmount = 0.0;
        lastInstallmentPaidDate = "";

        try {
            AaAccountDetailsRecord aaAccountDet = new AaAccountDetailsRecord(
                    da.getRecord(finMnemonic, "AA.ACCOUNT.DETAILS", "", selectionArrId));
            if (legacy) {
                getDemanddate(selectionArrId);
                billPayDateList = aaAccountDet.getBillPayDate();
                checkPayDateCommonMethod(today, billPayDateList);
            } else {
                lastInstallmentPaidDate1 = "";
                billPayDateList = aaAccountDet.getBillPayDate();
                checkPayDateCommonMethod(today, billPayDateList);
                if (!lastInstallmentPaidDate1.isEmpty()) {
                    lastInstallmentPaidDate = lastInstallmentPaidDate1;
                }
            }

        } catch (Exception e) {

            e.getMessage();
        }
    }

    /**
     * @param today
     * @param billPayDateList2
     */
    private void checkPayDateCommonMethod(LocalDate today, List<BillPayDateClass> billPayDateList) {
        for (BillPayDateClass payDate : billPayDateList) {
            List<BillIdClass> billIdList = payDate.getBillId();
            for (BillIdClass billIds : billIdList) {
                checkInstallment(today, billIds);
            }
        }
    }

    /**
     * @param selectionArrId
     */
    private void getDemanddate(String selectionArrId) {

        EbFfLoanPaymentHisRecord loanpayhisRec = new EbFfLoanPaymentHisRecord(
                da.getRecord("EB.FF.LOAN.PAYMENT.HIS", selectionArrId));

        List<DemandDateClass> demandDateList = loanpayhisRec.getDemandDate();
        List<LocalDate> transDateList = new ArrayList<>();
        if (demandDateList != null && !demandDateList.isEmpty()) {
            gettransdate(demandDateList, transDateList);
        }
        paiddate = null;
        for (LocalDate trsdate : transDateList) {
            if (!trsdate.isAfter(date) && (paiddate == null || trsdate.isAfter(paiddate))) {
                paiddate = trsdate;

            }

        }
        getPaidDate(demandDateList, paiddate);
    }

    /**
     * @param demandDateList
     * @param transDateList
     */
    private void gettransdate(List<DemandDateClass> demandDateList, List<LocalDate> transDateList) {

        for (DemandDateClass demand : demandDateList) {

            if (demand != null && demand.getTransDate() != null && demand.getTransDate().getValue() != null) {

                String transDateStr = demand.getTransDate().getValue();
                LocalDate transDate = LocalDate.parse(transDateStr, formatter);
                transDateList.add(transDate);
            }
        }

    }

    public void getPaidDate(List<DemandDateClass> demandDateList, LocalDate paiddate) {

        try {
            LocalDate transDate = null;
            for (DemandDateClass demand : demandDateList)

            {
                lastInstallmentPaidDate = "";
                String transDateStr = demand.getTransDate().getValue();
                transDate = LocalDate.parse(transDateStr, formatter);
                if (transDate.isEqual(paiddate)) {
                    String transType = demand.getTransType().getValue();
                    if ((transType.equalsIgnoreCase("principal")) || (transType.equalsIgnoreCase("interest"))) {
                        lastInstallmentPaidDate = paiddate.format(outDateFormatter);
                    }
                }
            }

        } catch (

        Exception e) {
            e.getMessage();
        }
    }

    // Checks overdue installment bills and calculates the pending installment
    // amount.
    private void checkInstallment(LocalDate today, BillIdClass billIds) {

        try {
            String lastPaidDate = "";
            billPayDate = LocalDate.parse(billIds.getBillDate().getValue(), formatter);
            billType = billIds.getBillType().getValue();
            billStatus = billIds.getSetStatus().getValue();

            if (!legacy && !billPayDate.isAfter(today)
                    && ((billStatus.equals("SETTLED")) || (billStatus.equals("REPAID")))) {
                billid = billIds.getBillId().getValue().replace("/", "");
                aaBillDetRec = new AaBillDetailsRecord(da.getRecord(finMnemonic, "AA.BILL.DETAILS", "", billid));
                for (BillStatusClass aaBillStaatus : aaBillDetRec.getBillStatus()) {
                    if (aaBillStaatus.getBillStatus().getValue().equals("SETTLED")) {
                        lastPaidDate = aaBillStaatus.getBillStChgDt().getValue();
                        lastInstallmentPaidDate1 = convertDateFormat(lastPaidDate);
                    }
                }
            }

            if (billPayDate.isBefore(today) && (billType.equals("INSTALLMENT"))) {
                billid = billIds.getBillId().getValue().replace("/", "");
                aaBillDetRec = new AaBillDetailsRecord(da.getRecord(finMnemonic, "AA.BILL.DETAILS", "", billid));
                currentInstalmentAmount = Double.parseDouble(aaBillDetRec.getOrTotalAmount().getValue());
            }
        } catch (Exception e) {
            e.getMessage();
        }

    }

    public static String convertDateFormat(String inputDate) {

        if (inputDate != null && !inputDate.isEmpty()) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
                DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
                return LocalDate.parse(inputDate, formatter).format(outputFormatter);
            } catch (Exception e) {
                e.getMessage();
                return inputDate;
            }
        } else {
            return "";
        }
    }

    /*
     * method:getArrangementFieldMappingDet Description: This method is used to get
     * the Arrangement field values
     */
    private void getArrangementFieldMappingDet(String selectionArrId) {

        try {
            aaArrRec = new AaArrangementRecord(da.getRecord(finMnemonic, AA_ARRANGEMENT, "", selectionArrId));
            accNumId = selectionArrId;
            String stdt = "";
            coCode = aaArrRec.getCoCodeRec().getValue();
            cusId = aaArrRec.getCustomer(0).getCustomer().getValue();
            initialiseCompanyInfo(coCode);

            getCustomerDetails(cusId);
            linkedAppList = aaArrRec.getLinkedAppl();
            for (LinkedApplClass linkedApp : linkedAppList) {
                if (linkedApp.getLinkedAppl().getValue().equals(ACCOUNT)) {
                    accNum = linkedApp.getLinkedApplId().getValue();
                }
            }
            if (aaArrRec.getOrigContractDate().getValue() != null
                    && !aaArrRec.getOrigContractDate().getValue().isEmpty()) {
                stdt = aaArrRec.getOrigContractDate().getValue();
                startDate = convertDateFormat(stdt);

                legacy = true;
            } else {
                stdt = aaArrRec.getStartDate().getValue();
                startDate = convertDateFormat(stdt);
            }
            getAccDetails(accNum);
            if (legacy) {
                cusId = cusMnemonic;
            }
            String productGrp = aaArrRec.getProductGroup().getValue();
            String product = aaArrRec.getProduct().get(0).getProduct().getValue();
            getProdDes(product);
            productDet = productGrp + "," + product;
        } catch (Exception e) {
            e.getMessage();
        }
    }

    private void getProdDes(String product) {

        try {
            AaProductRecord aaProdrec = new AaProductRecord(da.getRecord("AA.PRODUCT", product));
            prdDes = aaProdrec.getDescription(0).getValue();
        } catch (Exception e) {
            e.getMessage();
        }
    }

    /**
     * @param accNum2
     */
    private void getAccDetails(String accNum) {

        try {
            AccountRecord acctRec = new AccountRecord(da.getRecord(finMnemonic, ACCOUNT, "", accNum));

            if (legacy) {
                for (AltAcctTypeClass altType : acctRec.getAltAcctType()) {
                    if (altType.getAltAcctType().getValue().equals("LEGACY")) {
                        legAccNum = altType.getAltAcctId().getValue();
                    }
                }
            }
            ffCentreCode = acctRec.getLocalRefField("FF.CENTRE").getValue();
            getCentreName(ffCentreCode);
            loanPurp = acctRec.getLocalRefField("FF.LOAN.PURP").getValue();
            loanCycle = acctRec.getLocalRefField("FF.LOAN.CYCLE").getValue();
        } catch (Exception e) {
            e.getMessage();
        }
    }

    /*
     * method:getCustomerDetails Description: This method is used to get the
     * Customer Details
     */
    private void getCustomerDetails(String cusId) {

        try {
            cusRec = new CustomerRecord(da.getRecord(mnemonic, SEL_APP_CUS, "", cusId));

            cusName = getCustomerName(cusRec);

            dateOfBirth = cusRec.getDateOfBirth().getValue();
            customerAge = customerAgeCalculation(dateOfBirth);
            religGrp = cusRec.getLocalRefField(RELIG_GROUP).getValue();
            cusMnemonic = cusRec.getMnemonic().getValue();
        } catch (Exception e) {
            e.getMessage();
        }

    }

    private String getCustomerName(CustomerRecord cusRec) {

        StringBuilder sb = new StringBuilder();
        String fstName = "";
        String scdName = "";

        try {

            for (TField fName1 : cusRec.getName1()) {

                fstName = fName1.getValue();

            }

            for (TField fName2 : cusRec.getName2()) {

                scdName = fName2.getValue();

            }

            familyName = cusRec.getFamilyName().getValue();

            sb.append(fstName);

            if (!scdName.equals("")) {

                sb.append(" ");

                sb.append(scdName);

            }

            if (!familyName.equals("")) {

                sb.append(" ");

                sb.append(familyName);

            }

            return sb.toString();

        } catch (Exception e) {

            e.getMessage();

        }

        return sb.toString();

    }

    /*
     * method:getArrTermAmountDet Description: This method is used to get the Term
     * Amount
     */
    private void getArrTermAmountDet(Contract contract) {

        try {
            aaArrTermAmt = new AaArrTermAmountRecord(contract.getConditionForProperty("COMMITMENT"));
            loanAmount = aaArrTermAmt.getAmount().getValue();
        } catch (Exception e) {
            e.getMessage();
        }

    }

    /*
     * method:customerAgeCalculation Description: This method is used to get the
     * customer Age based on the DOB
     */
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

    private void getCentreName(String ffCentreCode) {

        try {
            EbFfCentreDetailRecord ebCenDetRec = new EbFfCentreDetailRecord(
                    da.getRecord("EB.FF.CENTRE.DETAIL", ffCentreCode));
            String brMgrName = ebCenDetRec.getBranchManager().getValue();
            getBranchMgrName(brMgrName);
            centreName = ebCenDetRec.getCenterName().getValue();
            currentRo = ebCenDetRec.getCurrentRo().getValue();
            getOfficerName(currentRo);
        } catch (Exception e) {
            e.getMessage();
        }

    }

    /**
     * @param brMgrName
     */
    private void getBranchMgrName(String brMgrName) {

        try {
            UserRecord userRec = new UserRecord(da.getRecord("USER", brMgrName));
            branchManagerName = userRec.getUserName().getValue();
            branchManagerMobileNumber = userRec.getLocalRefField("FF.MOBILE.NO").getValue();
        } catch (

        Exception e) {
            e.getMessage();
        }
    }

    private void getOfficerName(String currentRo) {

        try {
            EbFfRoUserRecord ebFfRoUserRec = new EbFfRoUserRecord(da.getRecord("EB.FF.RO.USER", currentRo));
            officerName = ebFfRoUserRec.getRoName().getValue();
            relationshipOfficerMobileNumber = ebFfRoUserRec.getRoMobileNumber().getValue();
        } catch (Exception e) {
            e.getMessage();
        }
    }

    /*
     * method:getPreFinalset Description: This method to check the date field value
     * from Enquiry selection or not
     */
    private Set<String> getPreFinalset(Set<String> overAllArrAccDetIdList, Set<String> selectionSet) {
        Set<String> preFinalSet = null;
        if (selectionSet.isEmpty()) {
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

    /*
     * method:getFilterCriteriaDets Description: This method is used to get the
     * selection field value from NoFileenquiry
     */
    private Set<String> getFilterCriteriaDets(List<FilterCriteria> filterCriteria) {

        Set<String> currentFilterSet = new LinkedHashSet<>();
        try {
            for (FilterCriteria filter : filterCriteria) {
                String value = filterBranchVal(filter);
                if (value == null || value.isEmpty())
                    continue;
                filterCriteriaCases(currentFilterSet, filter, value);
            }

        } catch (Exception e) {
            e.getMessage();
        }
        return currentFilterSet;
    }

    /**
     * @param currentFilterSet
     * @param filter
     * @param value
     */
    private void filterCriteriaCases(Set<String> currentFilterSet, FilterCriteria filter, String value) {

        switch (filter.getFieldname()) {
        case "USER":
            selUser = value;
            break;

        case "DATE.FROM":
            selDateFrom = value;
            onlyDateFilter = true;
            break;

        case "DATE.TO":
            selDateTo = value;
            break;

        case "PRODUCT":
            selProduct = value;
            filterValSet.add(value);
            currentFilterSet.addAll(da.selectRecords(finMnemonic, AA_ARRANGEMENT, "", "WITH PRODUCT EQ " + selProduct));
            break;
        case "CENTER":
            selCenterName = value;
            filterValSet.add(value);
            currentFilterSet.addAll(getArrListFromCentre(selCenterName));
            break;

        case "VILLAGE":
            selVillage = value;
            filterValSet.add(value);
            List<String> villageArrAccList = da.selectRecords(finMnemonic, AA_ARR_ACCOUNT, "",
                    "WITH FF.VILLAGE EQ " + selVillage);
            currentFilterSet.addAll(getArrListFromSelection(villageArrAccList));
            break;
        case "DISTRICT_NAME":
            selDist = value;
            filterValSet.add(value);
            String fldName = DISTRICT_NAME;
            currentFilterSet.addAll(getCusArrList(fldName, selDist));
            break;
        case "CYCLE":
            selLoanCycle = value;
            filterValSet.add(value);
            List<String> loanCycleList = da.selectRecords(finMnemonic, AA_ARR_ACCOUNT, "",
                    "WITH FF.LOAN.CYCLE EQ " + selLoanCycle);
            currentFilterSet.addAll(getArrListFromSelection(loanCycleList));
            break;
        case "PURPOSE":
            selLoanPurp = value;
            filterValSet.add(value);
            List<String> loanPurposeList = da.selectRecords(finMnemonic, AA_ARR_ACCOUNT, "",
                    "WITH FF.LOAN.PURP EQ " + selLoanPurp);
            currentFilterSet.addAll(getArrListFromSelection(loanPurposeList));
            break;
        case "RELIGION":
            selReligGrp = value;
            filterValSet.add(value);
            fldName = RELIG_GROUP;
            currentFilterSet.addAll(getCusArrList(fldName, selReligGrp));
            break;
        case "CASTE":
            selCaste = value;
            filterValSet.add(value);
            fldName = CASTE;
            currentFilterSet.addAll(getCusArrList(fldName, selCaste));
            break;
        default:
        }
    }

    /**
     * @param filter
     * @return
     */
    private String filterBranchVal(FilterCriteria filter) {

        String value = filter.getValue();
        String field = filter.getFieldname();

        if ("BRANCH".equalsIgnoreCase(field)) {
            branch = value;

        }
        return value;
    }

    /**
     * 
     * 
     * /* method:getArrListFromsel Description: This method is used to get the
     * ArrangementId List based on the selection values
     */
    public List<String> getCusArrList(String fieldName, String fieldValue) {

        Set<String> customerSet = new LinkedHashSet<>(
                da.selectRecords(mnemonic, SEL_APP_CUS, "", "WITH " + fieldName + " EQ " + fieldValue));
        Set<String> arrCustomerSet = new LinkedHashSet<>(
                da.selectRecords(finMnemonic, "AA.CUSTOMER.ARRANGEMENT", "", "WITH CUSTOMER.ID NE NULL"));
        customerSet.retainAll(arrCustomerSet);
        return getArrListFromCusSel(customerSet);

    }

    public List<String> getArrListFromCusSel(Set<String> cusIdList) {

        List<String> aaIdList = new ArrayList<>();
        try {
            for (String cusid : cusIdList) {
                AaCustomerArrangementRecord aaCusRec = new AaCustomerArrangementRecord(
                        da.getRecord(finMnemonic, "AA.CUSTOMER.ARRANGEMENT", "", cusid));
                if (!aaCusRec.toString().isEmpty()) {
                    productLineList = aaCusRec.getProductLine();
                    for (ProductLineClass productLine : productLineList) {
                        getProductLine(aaIdList, productLine);
                    }
                }
            }
        } catch (Exception e) {
            e.getMessage();
        }
        return aaIdList;
    }

    /**
     * @param arrIdList
     * @param productLine
     */
    public void getProductLine(List<String> aaIdList, ProductLineClass productLine) {

        if (productLine.getProductLine().getValue().equals("LENDING")) {
            for (ArrangementClass arrIdFrmAAcus : productLine.getArrangement()) {
                aaIdList.add(arrIdFrmAAcus.getArrangement().getValue());
            }
        }

    }

    public List<String> getArrListFromCentre(String centreId) {

        List<String> arrAccList = da.selectRecords(finMnemonic, AA_ARR_ACCOUNT, "", "WITH FF.CENTRE EQ " + centreId);
        return getArrListFromSelection(arrAccList);
    }

    /*
     * method:getArrListFromSelection Description: This method is used to get the
     * ArrangementId List based on the selection values
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
        } catch (Exception e) {
            e.getMessage();
        }
        return arrIdList;
    }

    /**
     * @param filterCriteria
     */
    private void fcChkBranch(List<FilterCriteria> filterCriteria) {

        for (FilterCriteria filter : filterCriteria) {
            if (filter.getFieldname().equals("BRANCH")) {
                branch = filter.getValue();

                initialiseCompanyInfo(branch);
                getLinkedCompIds(branch);
                selBranch = getCompanyDescription(branch);
                break;
            }
        }
    }

    /*
     * method:initialiseCompanyInfo Description: This method is used to get the
     * field values from company record
     */
    private void initialiseCompanyInfo(String companyId) {

        try {
            CompanyRecord companyObj = new CompanyRecord(da.getRecord(COMPANY, companyId));
            finMnemonic = companyObj.getFinancialMne().getValue();
            mnemonic = companyObj.getCustomerMnemonic().getValue();
            branchName = getCompanyDescription(companyId);
            branchCode = companyId;

            zoneName = getCompanyDescription(companyObj.getLocalRefField("FF.ZONE").getValue());
            regionName = getCompanyDescription(companyObj.getLocalRefField("FF.REGION").getValue());
            divisionName = getCompanyDescription(companyObj.getLocalRefField("FF.DIVISION").getValue());
            clusterName = getCompanyDescription(companyObj.getLocalRefField("FF.CLUSTER").getValue());

            branchState = getCompanyDescription(companyObj.getLocalRefField("FF.STATE").getValue());
        } catch (Exception e) {
            e.getMessage();
        }

    }

    private String getCompanyDescription(String companyCode) {

        String companyNam = "";

        try {
            if (companyCode != null && !companyCode.isEmpty()) {

                CompanyRecord companyObj = new CompanyRecord(da.getRecord(COMPANY, companyCode));

                companyNam = companyObj.getCompanyName().get(0).getValue();

                String[] compNamePart = companyNam.split("-");

                companyNam = compNamePart[0];

                return companyNam;
            }
        } catch (Exception e) {
            e.getMessage();

        }
        return companyNam;
    }

    public void getLinkedCompIds(String branch) {

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
}