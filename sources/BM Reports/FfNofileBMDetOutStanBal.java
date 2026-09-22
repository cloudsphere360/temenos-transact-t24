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

import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaarrangement.LinkedApplClass;
import com.temenos.t24.api.records.aaarrtermamount.AaArrTermAmountRecord;
import com.temenos.t24.api.records.aabilldetails.AaBillDetailsRecord;
import com.temenos.t24.api.records.aabilldetails.BillStatusClass;
import com.temenos.t24.api.records.aabilldetails.PropertyClass;
import com.temenos.t24.api.records.aacustomerarrangement.AaCustomerArrangementRecord;
import com.temenos.t24.api.records.aacustomerarrangement.ArrangementClass;
import com.temenos.t24.api.records.aacustomerarrangement.ProductLineClass;

import com.temenos.t24.api.records.aaprddesofficers.AaPrdDesOfficersRecord;
import com.temenos.t24.api.records.aaproduct.AaProductRecord;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.records.account.AltAcctTypeClass;
import com.temenos.t24.api.records.company.CompanyRecord;
import com.temenos.t24.api.records.companyconsol.CompanyConsolRecord;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.records.deptacctofficer.DeptAcctOfficerRecord;
import com.temenos.t24.api.records.ebffcentredetail.EbFfCentreDetailRecord;
import com.temenos.t24.api.records.ebffloandetails.AddressTypeClass;

import com.temenos.t24.api.records.ebffloandetails.EbFfLoanDetailsRecord;
import com.temenos.t24.api.records.ebffloandetails.FmEntityNumberClass;
import com.temenos.t24.api.records.ebffloandpd.DateClass;
import com.temenos.t24.api.records.ebffloandpd.EbFfLoanDpdRecord;
import com.temenos.t24.api.records.ebffloanpaymenthis.DemandDateClass;
import com.temenos.t24.api.records.ebffloanpaymenthis.EbFfLoanPaymentHisRecord;
import com.temenos.t24.api.records.ebffparameter.EbFfParameterRecord;
import com.temenos.t24.api.records.ebffparameter.ParamDescClass;
import com.temenos.t24.api.records.ebffrouser.EbFfRoUserRecord;
import com.temenos.t24.api.records.user.UserRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/**
 * *
 * ------------------------------------------------------------------------------
 * 
 * @author Deepakumar S Date Created: 22122025 Attached as :
 *         EB.API>FF.E.NOFILE.BM.OUTSTAND.BAL.DETAILS
 *         STANDARD.SELECTION>NOFILE.FF.BM.OUTSTANDING.BAL.DETAILS
 *         ENQUIRY>FF.NOFILE.BM.OUTSTANDING.BAL.DETAILS Description: Branch
 *         Online Report generation -> OUTSTANDING.BLANCE.DETAILS
 * 
 *         --------------------------------------------------------------------------------
 *         22-12-2025 Development Initial Version
 * 
 *         15-02-2026 Grouping Logic Jerome
 * 
 *         27-3-2026 PostMigrationDataMapping Jerome
 *         --------------------------------------------------------------------------------
 */
public class FfNofileBMDetOutStanBal extends Enquiry {

    private static final String COMPANY = "COMPANY";
    private static final String TRADE = "TRADE";
    private static final String CUSTOMER = "CUSTOMER";
    private static final String EB_FF_LOAN_DPD = "EB.FF.LOAN.DPD";
    private static final String AA_ARR_ACCOUNT = "AA.ARR.ACCOUNT";
    private static final String ACCOUNT = "ACCOUNT";
    private static final String AA_ARRANGEMENT = "AA.ARRANGEMENT";
    public static final String DATE_RANGE_ERR = "EB-FF.BM.DATE.FILTER.RANGE";
    public static final String NO_REC_ERR = "EB-NO.REC.SELECT";
    public static final String FILE_NAME = "OutStandingBalRep_Det";
    public static final String SEL_APP_CUS = CUSTOMER;
    public static final String CENTRE = "FF.CENTRE";
    public static final String VILLAGE = "FF.VILLAGE";
    public static final String DISTRICT_NAME = "DISTRICT.NAME";
    public static final String LOAN_CYCLE = "FF.LOAN.CYCLE";
    public static final String LOAN_PURP = "FF.LOAN.PURP";
    public static final String RELIG_GROUP = "FF.RELIG.GROUP";
    public static final String CASTE = "FF.CASTE";

    DataAccess da = new DataAccess(this);
    Session ss = new Session(this);
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

    Session session = new Session(this);
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
    List<String> ffLoanDpdList = null;
    Set<String> aaArrIdInLoanDpd = new HashSet<>();
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

    String selReligGrp = "";
    String startDateAccDet = "";

    String arrAgeStatus = "";
    String ffZone = "";
    String ffRegion = "";
    String ffDivision = "";
    String ffCluster = "";
    String ffCenter = "";
    String coCode = "";
    String companyName = "";
    String cusId = "";
    String givenName = "";
    String familyName = "";
    String cusName = "";
    String dateOfBirth = "";
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
    String loanAmount = "";
    String loanCycle = "";
    AaPrdDesOfficersRecord aaprdDesOffRec = null;
    String officerCode = "";
    List<String> loanDpdIdList = null;
    List<DateClass> loanDpdDate = null;
    List<DateClass> loanDpdDatelist = null;
    String curDpd = "";
    double overdueAmt = 0;
    String loanDpdCnt = "";
    boolean flag = false;
    String overdueLoanAmt = "";
    List<BillPayDateClass> billPayDateList = null;
    String billStatus = "";
    LocalDate billPayDate;
    String billType = "";
    String billid = "";

    List<PropertyClass> billPropertylist = null;
    String orPropAmt = "";

    String totalOverdueAmount = "";
    double outstandingBalance = 0.0;
    AaArrangementRecord aaArrRec = null;
    List<String> outvalues = new ArrayList<>();

    CustomerRecord cusRec = null;
    AaBillDetailsRecord aaBillDetRec = null;

    List<String> returnVal = new ArrayList<>();
    List<String> finalArrayList = new ArrayList<>();

    DeptAcctOfficerRecord deptOfferRec = null;
    String productDet = "";
    String officerName = "";
    String relationshipOfficerMobileNumber = "";
    String branchManagerName = "";
    String branchManagerMobileNum = "";
    String dpdBucket = "";
    String branchManagerMobileNumber = "";
    String writeOff = "";
    String lastInstallmentPaidDate = "";
    BillPayDateClass billPayDateSet;
    double currentInstalmentAmount = 0;
    double outstndgBal = 0;
    String productGrp = "";
    String parkedAmount = "";
    String principalDefault = "";
    String totalDefault = "";
    String overdueIntrestDenand = "";
    String overdueInstallment = "";
    String currentInstallmentDemandDate = "";
    String principalPrepayment = "";
    String intrestPrepayment = "";
    String currentPrincipalInstallment = "";
    String currentInterestInstallment = "";
    String currentOd = "";
    String principalOverdue = "";
    String totalInterestCollected = "";
    String interestOverdue = "";
    String principalOutstanding = "";
    String interestOutstanding = "";
    String interestDefault = "";
    String outstandingDefault = "";
    double sumofAllPrincipalOutstanding = 0.0;
    double sumofAllinterestOutstanding = 0.0;
    double sumofAllPrincipalDefault = 0.0;
    double sumofAllInterestDefault = 0.0;
    double sumofAllTotalDefault = 0.0;
    double sumofAllOutstandingDefault = 0.0;
    String overAllPrincipalOutstanding = "";
    String overallAllinterestOutstanding = "";
    String overAllPrincipalDefault = "";
    String overAllInterestDefault = "";
    String overAllTotalDefault = "";
    String oversumofAllOutstandingDefault = "";
    String dpd = "";

    DeptAcctOfficerRecord primOfficer = null;
    EbFfLoanDpdRecord loanDpdRec = null;
    String funderName = "";
    List<String> lastInstallmentPaidDateList = new ArrayList<>();
    String branch = "";
    String companyId = "";
    String companyIds = "";
    String legacyAcctNumber = "";
    String relationOfficerName = "";

    boolean paramFlag = false;
    String filePath = "";
    String loanDate = "";
    String accountNo = "";
    String centreName = "";
    String roOfficerName = "";
    String roMobileNumber = "";
    int itergatingCount = 0;
    String selBranch = "";
    String fldName = "";

    String todayDate = session.getCurrentVariable("!TODAY");
    LocalDate todateformatted = LocalDate.parse(todayDate, formatter);
    String dateformatted = todateformatted.getMonth().toString().substring(0, 3) + todateformatted.getYear();
    String seluser = "";
    String selDistrict = "";
    String selCycle = "";
    String selPurpose = "";
    String selReligion = "";

    // Main enquiry execution method that retrieves arrangements, applies filters,
    // calculates balances, and prepares the final report output rows.

    @Override
    public List<String> setIds(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {

        try {

            Contract contract = new Contract(this);

            getBranch(filterCriteria);

            getParamPath();

            Set<String> overAllArrAccDetIdList = new LinkedHashSet<>(da.selectRecords(finMnemonic, AA_ARRANGEMENT, "",
                    "WITH ARR.STATUS NE CLOSE AND ARR.STATUS NE PENDING.CLOSURE AND CO.CODE EQ " + companyIds));

            Set<String> selectionSet = getFilterCriteriaDets(filterCriteria);

            Set<String> preFinalSet = getPreFinalset(overAllArrAccDetIdList, selectionSet);

            preFinalSet.retainAll(overAllArrAccDetIdList);

            validateDateRange();

            LocalDate today = LocalDate.parse(todayDate, formatter);
            LocalDate start = startDate.isEmpty() ? today : LocalDate.parse(startDate, formatter);
            LocalDate end = endDate.isEmpty() ? today : LocalDate.parse(endDate, formatter);

            for (String selectionArrId : preFinalSet) {
                zoneName = "";
                regionName = "";
                divisionName = "";
                clusterName = "";
                branchName = "";
                districtName = "";
                stateName = "";
                coCode = "";
                centreName = "";
                ffCenter = "";
                cusName = "";
                cusId = "";
                customerAge = 0;
                religGrp = "";
                fmOccup = "";
                loanPurp = "";
                accountNo = "";
                legacyAcctNumber = "";
                loanDate = "";
                loanAmount = "";
                productDet = "";
                loanCycle = "";
                roOfficerName = "";
                roMobileNumber = "";
                branchManagerName = "";
                branchManagerMobileNumber = "";
                funderName = "";
                overAllPrincipalOutstanding = "";
                overallAllinterestOutstanding = "";
                dpd = "";
                overAllPrincipalDefault = "";
                overAllInterestDefault = "";
                overAllTotalDefault = "";
                lastInstallmentPaidDate = "";

                itergatingCount++;

                if (onlyDateFilter) {

                    AaArrangementRecord aaArRec = new AaArrangementRecord(
                            da.getRecord(finMnemonic, AA_ARRANGEMENT, "", selectionArrId));

                    startDateAccDet = aaArRec.getStartDate().getValue();

                    if (startDateAccDet != null && !startDateAccDet.isEmpty()) {

                        LocalDate startDateArrAcc = LocalDate.parse(startDateAccDet, formatter);

                        if ((!startDateArrAcc.isBefore(start)) && (!startDateArrAcc.isAfter(end))) {
                            processArrId(contract, today, selectionArrId);
                        }
                    }

                } else {
                    processArrId(contract, today, selectionArrId);
                }

            }

            fileWrite();

        } catch (Exception e) {

            e.getMessage();

        }
        if (dateErrFlag) {
            throw new T24CoreException("", DATE_RANGE_ERR);
        } else if (noRecErrFlag) {
            throw new T24CoreException("", NO_REC_ERR);
        } else {
            return returnVal;
        }
    }

    private void getBranch(List<FilterCriteria> filterCriteria) {
        for (FilterCriteria filter : filterCriteria) {
            if (filter.getFieldname().equalsIgnoreCase("BRANCH")) {
                selBranch = filter.getValue();
                initialiseCompanyInfo(selBranch);
                getLinkedCompIds(selBranch);
                break;
            }
        }
    }

    private void processArrId(Contract contract, LocalDate today, String selectionArrId) {
        try {

            finalArrayList.add(selectionArrId);
            contract.setContractId(selectionArrId);
            getArrangementFieldMappingDet(selectionArrId);
            getEbFfLoanDetails(selectionArrId);
            getEcbBalanceBasedOnDPDcnt(selectionArrId);
            getArrTermAmountDet(contract);
            getDefaultAmounts(selectionArrId);
            getRecentBillBasedOnCurrntDate(selectionArrId, today);
            getOutstandingDetails(contract);
            List<String> row = new ArrayList<>();
            row.add(zoneName);
            row.add(regionName);
            row.add(divisionName);
            row.add(clusterName);
            row.add(branchName);
            row.add(districtName);
            row.add(stateName);
            row.add(coCode);
            row.add(centreName);
            row.add(ffCenter);
            row.add(cusName);
            row.add(cusId);
            row.add(String.valueOf(customerAge));
            row.add(religGrp);
            row.add(fmOccup);
            row.add(loanPurp);
            row.add(accountNo);
            row.add(legacyAcctNumber);
            row.add(loanDate);
            row.add(loanAmount);
            row.add(productDet);
            row.add(loanCycle);
            row.add(roOfficerName);
            row.add(roMobileNumber);
            row.add(branchManagerName);
            row.add(branchManagerMobileNumber);
            row.add(funderName);
            row.add(overAllPrincipalOutstanding);
            row.add(overallAllinterestOutstanding);
            row.add(String.valueOf(dpd));
            row.add(overAllPrincipalDefault);
            row.add(overAllInterestDefault);
            row.add(overAllTotalDefault);
            row.add(lastInstallmentPaidDate);
            returnVal.add(String.join("*", row));
            outvalues.add(String.join(",", row));
        } catch (Exception e) {
            e.getMessage();
        }
    }

    private void getOutstandingDetails(Contract overdueContract) {
        try {

            String currAccount = getOverDueBalance(overdueContract, "FFPRINOUTAMT", TRADE);
            String accPrinterest = getOverDueBalance(overdueContract, "FFINTODFUTAMT", TRADE);
            principalOutstanding = String.valueOf(Double.parseDouble(currAccount));
            interestOutstanding = String.valueOf(Double.parseDouble(accPrinterest));
            sumofAllPrincipalOutstanding = Math.abs(Double.parseDouble(principalOutstanding));
            sumofAllinterestOutstanding = Math.abs(Double.parseDouble(interestOutstanding));
            overAllPrincipalOutstanding = String.valueOf(sumofAllPrincipalOutstanding);

            overallAllinterestOutstanding = String.format("%.2f", sumofAllinterestOutstanding);

        } catch (Exception e) {

            e.getMessage();
        }

    }

    public String getOverDueBalance(Contract overdueContract, String accountType, String bookingType) {
        List<BalanceMovement> movements = overdueContract.getContractBalanceMovements(accountType, bookingType);
        return (movements != null && !movements.isEmpty()) ? movements.get(0).getBalance().toString() : "0";
    }

    private void getDefaultAmounts(String selectionArrId) {
        try {
            Contract defaultContract = new Contract(this);
            if (aaArrIdInLoanDpd.contains(selectionArrId)) {
                defaultContract.setContractId(selectionArrId);
                getEcbDetails(defaultContract);
            }
        } catch (Exception e) {
            e.getMessage();

        }

    }

    private void fileWrite() {
        try {
            DateTimeFormatter outDateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HHmmss");

            LocalDateTime currDtTime = LocalDateTime.now();
            String currDate = currDtTime.format(outDateFormatter);
            String currTime = currDtTime.format(timeFormatter);
            String outputPath = filePath + FILE_NAME + branchName + "_" + seluser + "_" + currDate + "_" + currTime
                    + ".csv";

            writeToFile(outvalues, outputPath);

        } catch (Exception e) {
            e.getMessage();

        }
    }

    private void getParamPath() {

        try {
            if (!paramFlag) {
                String paramId = "FF.BM.REPORT.EXTRACT";
                EbFfParameterRecord paramRec = new EbFfParameterRecord(da.getRecord("EB.FF.PARAMETER", paramId));
                for (ParamDescClass paramDesc : paramRec.getParamDesc()) {
                    if (paramDesc.getParamName().getValue().equals("Path")) {
                        filePath = paramDesc.getParamValue().getValue();
                    }
                }
                paramFlag = true;

            }
        } catch (Exception e) {

            e.getMessage();
        }

    }

//Retrieves linked company IDs for the selected branch using COMPANY.CONSOL
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

    private void validateDateRange() {
        try {
            LocalDate today = LocalDate.parse(todayDate, formatter);
            LocalDate stDt = startDate.isEmpty() ? today : LocalDate.parse(startDate, formatter);
            LocalDate endDt = endDate.isEmpty() ? today : LocalDate.parse(endDate, formatter);

            if (stDt.isAfter(endDt)) {
                dateErrFlag = true;

            }

        } catch (Exception e) {

            e.getStackTrace();
        }
    }

//Retrieves the current DPD (Days Past Due) value from EB.FF.LOAN.DPD records.
    private void getEcbBalanceBasedOnDPDcnt(String selectionArrId) {

        try {
            Double loanDpd = 0.0;
            List<DateClass> ffLoaanDpdDate;
            LocalDate latestDate = null;

            loanDpdRec = new EbFfLoanDpdRecord(
                    da.getRecord(finMnemonic, EB_FF_LOAN_DPD, "", selectionArrId + "-" + dateformatted));
            ffLoaanDpdDate = loanDpdRec.getDate();

            for (int i = 0; i < ffLoaanDpdDate.size(); i++) {

                String dateStr = ffLoaanDpdDate.get(i).getDate().getValue();
                LocalDate transDate = LocalDate.parse(dateStr, formatter);

                if (!transDate.isAfter(todateformatted) && (latestDate == null || transDate.isAfter(latestDate))) {

                    latestDate = transDate;

                    dpd = ffLoaanDpdDate.get(i).getCurDpd().getValue();
                    loanDpd = Double.parseDouble(dpd);
                }

            }

            if (loanDpd != 0.0) {
                aaArrIdInLoanDpd.add(selectionArrId);
            }

        } catch (Exception e) {

            e.getMessage();
        }

    }

//Calculates outstanding balances, default amounts, and related ECB balance details.
    private void getEcbDetails(Contract contract) {

        try {

            principalDefault = getBalance(contract, "FFPRINODAMT", TRADE);

            interestDefault = getBalance(contract, "FFINTODAMT", TRADE);

            totalDefault = getBalance(contract, "FFALLOVRDUE", TRADE);

            String totOutstandingAmt = getBalance(contract, "FFTOTPRINTAMT", TRADE);

            sumofAllPrincipalDefault = Math.abs(Double.parseDouble(principalDefault));
            sumofAllInterestDefault = Math.abs(Double.parseDouble(interestDefault));
            sumofAllTotalDefault = Math.abs(Double.parseDouble(totalDefault));
            sumofAllOutstandingDefault = Math.abs(Double.parseDouble(totOutstandingAmt));

            overAllPrincipalDefault = String.valueOf(sumofAllPrincipalDefault);

            overAllInterestDefault = String.format("%.2f", sumofAllInterestDefault);

            overAllTotalDefault = String.valueOf(sumofAllTotalDefault);
            oversumofAllOutstandingDefault = String.valueOf(sumofAllOutstandingDefault);

        } catch (NumberFormatException e) {

            e.getMessage();

        }
    }

//Retrieves balance for a specific account type and booking type from contract movements.
    public String getBalance(Contract contract, String accountType, String bookingType) {

        List<BalanceMovement> movements = null;
        try {
            movements = contract.getContractBalanceMovements(accountType, bookingType);
        } catch (Exception e) {

            e.getMessage();

        }
        return (movements != null && !movements.isEmpty()) ? movements.get(0).getBalance().toString() : "0";
    }

//Identifies the most recent installment details and last paid installment date.
    private void getRecentBillBasedOnCurrntDate(String selectionArrId, LocalDate today) {

        currentInstalmentAmount = 0.0;
        lastInstallmentPaidDate = "";

        try {
            AaAccountDetailsRecord aaAccountDet = new AaAccountDetailsRecord(
                    da.getRecord(finMnemonic, "AA.ACCOUNT.DETAILS", "", selectionArrId));

            if (flag) {
                EbFfLoanPaymentHisRecord loanpayhisRec = new EbFfLoanPaymentHisRecord(
                        da.getRecord("EB.FF.LOAN.PAYMENT.HIS", selectionArrId));
                List<DemandDateClass> demandDateList = loanpayhisRec.getDemandDate();
                getPaidDate(demandDateList, today);

            } else {
                billPayDateList = aaAccountDet.getBillPayDate();

                for (BillPayDateClass payDate : billPayDateList) {
                    List<BillIdClass> billIdList = payDate.getBillId();
                    for (BillIdClass billIds : billIdList) {
                        checkInstallment(today, billIds);

                    }
                }

            }
        } catch (Exception e) {

            e.getMessage();
        }

    }

    public void getPaidDate(List<DemandDateClass> demandDateList, LocalDate today) {

        try {
            LocalDate latestPrincipalDate = null;
            LocalDate latestInterestDate = null;
            LocalDate transDate = null;
            for (DemandDateClass demand : demandDateList) {
                String transDateStr = demand.getTransDate().getValue();

                transDate = LocalDate.parse(transDateStr, formatter);

                if (!transDate.isAfter(today)) {
                    String transType = demand.getTransType().getValue();

                    if (transType.equalsIgnoreCase("principal")) {
                        if (latestPrincipalDate == null || transDate.isAfter(latestPrincipalDate)) {
                            latestPrincipalDate = transDate;
                        }
                    } else if (transType.equalsIgnoreCase("interest")
                            && (latestInterestDate == null || transDate.isAfter(latestInterestDate))) {
                        latestInterestDate = transDate;
                    }

                }
            }
            pickLatestDate(latestPrincipalDate, latestInterestDate);
        } catch (Exception e) {
            e.getMessage();
        }
    }

    private void pickLatestDate(LocalDate latestPrincipalDate, LocalDate latestInterestDate) {
        if (latestPrincipalDate != null && latestInterestDate != null
                && latestPrincipalDate.equals(latestInterestDate)) {
            lastInstallmentPaidDate = convertDateFormat(latestPrincipalDate.toString());
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
            if (!billPayDate.isAfter(today) && ((billStatus.equals("SETTLED")) || (billStatus.equals("REPAID")))) {
                billid = billIds.getBillId().getValue().replace("/", "");
                aaBillDetRec = new AaBillDetailsRecord(da.getRecord(finMnemonic, "AA.BILL.DETAILS", "", billid));

                for (BillStatusClass aaBillStaatus : aaBillDetRec.getBillStatus()) {
                    if (aaBillStaatus.getBillStatus().getValue().equals("SETTLED")) {
                        lastPaidDate = aaBillStaatus.getBillStChgDt().getValue();
                        lastInstallmentPaidDate = convertDateFormat(lastPaidDate);
                    }

                }

            }
        } catch (Exception e) {
            e.getMessage();
        }

    }

//Retrieves arrangement details such as product, customer ID, account number, and start date.
    private void getArrangementFieldMappingDet(String selectionArrId) {
        String date = "";
        try {
            aaArrRec = new AaArrangementRecord(da.getRecord(finMnemonic, AA_ARRANGEMENT, "", selectionArrId));
            accountNo = selectionArrId;
            coCode = aaArrRec.getCoCodeRec().getValue();
            initialiseCompanyInfo(coCode);

            if (aaArrRec.getOrigContractDate().getValue() != null
                    && !aaArrRec.getOrigContractDate().getValue().isEmpty()) {
                flag = true;
                date = aaArrRec.getOrigContractDate().getValue();
                loanDate = convertDateFormat(date);
            } else {
                date = aaArrRec.getStartDate().getValue();
                loanDate = convertDateFormat(date);
            }

            linkedAppList = aaArrRec.getLinkedAppl();
            for (LinkedApplClass linkedApp : linkedAppList) {
                if (linkedApp.getLinkedAppl().getValue().equals(ACCOUNT)) {
                    accNum = linkedApp.getLinkedApplId().getValue();

                }
            }

            getAccountDetails(accNum);

            cusId = aaArrRec.getCustomer(0).getCustomer().getValue();

            getStateName();

            getCustomerDetails(cusId, flag);

            productGrp = aaArrRec.getProductGroup().getValue();
            String product = (aaArrRec.getProduct().get(0).getProduct().getValue() != null)
                    ? aaArrRec.getProduct().get(0).getProduct().getValue()
                    : "";
            getProductName(product);

        } catch (Exception e) {

            e.getMessage();
        }
    }

    private void getAccountDetails(String acctNo) {
        try {
            AccountRecord acctRec = new AccountRecord(da.getRecord(finMnemonic, ACCOUNT, "", acctNo));
            ffCenter = acctRec.getLocalRefField(CENTRE).getValue();
            getFfCentreDetail(ffCenter);
            if (flag) {
                for (AltAcctTypeClass altType : acctRec.getAltAcctType()) {
                    if (altType.getAltAcctType().getValue().equals("LEGACY")) {
                        legacyAcctNumber = altType.getAltAcctId().getValue();
                    }
                }
            }
            loanPurp = acctRec.getLocalRefField(LOAN_PURP).getValue();
            loanCycle = acctRec.getLocalRefField(LOAN_CYCLE).getValue();
            funderName = acctRec.getLocalRefField("FF.FUND.SOURCE").getValue();
        } catch (Exception e) {

            e.getMessage();
        }

    }

    private void getProductName(String product) {
        try {
            AaProductRecord prodRec = new AaProductRecord(da.getRecord("AA.PRODUCT", product));
            productDet = (prodRec.getDescription(0).getValue() != null) ? prodRec.getDescription(0).getValue() : "";

        } catch (Exception e) {

            e.getMessage();
        }
    }

    public static String convertDateFormat(String inputDate) {
        if (inputDate == null || inputDate.isEmpty()) {
            return "";
        }
        try {
            DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

            return LocalDate.parse(inputDate, inputFormatter).format(outputFormatter);
        } catch (Exception e) {
            e.getMessage();
        }
        return inputDate;
    }

    private void getStateName() {
        try {
            CompanyRecord companyRec = new CompanyRecord(da.getRecord(COMPANY, coCode));
            companyName = companyRec.getCompanyName().get(0).getValue();
            stateName = companyRec.getLocalRefField("FF.STATE").getValue();
        } catch (Exception e) {

            e.getMessage();
        }
    }

//Fetches loan related address details like state information from EB.FF.LOAN.DETAILS.
    private void getEbFfLoanDetails(String selectionArrId) {

        try {
            String relation = "";
            EbFfLoanDetailsRecord loanDetRec = new EbFfLoanDetailsRecord(
                    da.getRecord(finMnemonic, "EB.FF.LOAN.DETAILS", "", selectionArrId));

            if (!loanDetRec.toString().isEmpty()) {
                List<AddressTypeClass> addrsTypelist = loanDetRec.getAddressType();
                for (AddressTypeClass addrsType : addrsTypelist) {
                    districtName = addrsType.getDistrictName().getValue();

                }
                for (FmEntityNumberClass fmEntity : loanDetRec.getFmEntityNumber()) {
                    relation = fmEntity.getRelation().getValue();
                    if (relation.equalsIgnoreCase("SELF")) {
                        fmOccup = fmEntity.getOccupation().getValue();
                    }
                }

            }
        } catch (Exception e) {

            e.getMessage();

        }
    }

//Retrieves customer information including name, age, center, religion, and occupation.
    private void getCustomerDetails(String custId, boolean flag2) {

        religGrp = "";

        String fstName = "";
        String scdName = "";
        try {
            cusRec = new CustomerRecord(da.getRecord(mnemonic, CUSTOMER, "", custId));
            if (flag2) {
                cusId = cusRec.getMnemonic().getValue();
            }

            cusName = getCustomerName(fstName, scdName, cusRec);

            dateOfBirth = cusRec.getDateOfBirth().getValue();

            customerAge = customerAgeCalculation(dateOfBirth);

            religGrp = cusRec.getLocalRefField(RELIG_GROUP).getValue();

        } catch (Exception e) {

            e.getMessage();
        }

    }

    private String getCustomerName(String fstName, String scdName, CustomerRecord cusRec) {
        StringBuilder sb = new StringBuilder();
        try {
            for (TField name1 : cusRec.getName1()) {
                fstName = name1.getValue();
            }

            for (TField name2 : cusRec.getName2()) {
                scdName = name2.getValue();
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

//Retrieves the loan commitment amount from AA.ARR.TERM.AMOUNT record.
    private void getArrTermAmountDet(Contract contract) {

        try {
            aaArrTermAmt = new AaArrTermAmountRecord(contract.getConditionForProperty("COMMITMENT"));

            loanAmount = aaArrTermAmt.getAmount().getValue();

        } catch (Exception e) {

            e.getMessage();
        }

    }

//Calculates the customer's age based on the date of birth.
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

    private void getFfCentreDetail(String ffCentre2) {
        String branchManager = "";
        try {
            EbFfCentreDetailRecord centreDet = new EbFfCentreDetailRecord(
                    da.getRecord("EB.FF.CENTRE.DETAIL", ffCentre2));
            centreName = centreDet.getCenterName().getValue();
            officerName = centreDet.getCurrentRo().getValue();
            getEbFfRoUserDets(officerName);
            branchManager = centreDet.getBranchManager().getValue();
            getUserTable(branchManager);

        } catch (Exception e) {

            e.getMessage();
        }

    }

    public void getEbFfRoUserDets(String ro) {
        try {
            EbFfRoUserRecord roUserRec = new EbFfRoUserRecord(da.getRecord("", "EB.FF.RO.USER", "", ro));
            roOfficerName = roUserRec.getRoName().getValue();
            roMobileNumber = roUserRec.getRoMobileNumber().getValue();
        } catch (Exception e) {

            e.getMessage();
        }
    }

    private void getUserTable(String branchManager) {
        try {

            UserRecord user = new UserRecord(da.getRecord("USER", branchManager));
            branchManagerName = user.getUserName().getValue();
            branchManagerMobileNumber = user.getLocalRefField("FF.MOBILE.NO").getValue();
        } catch (Exception e) {

            e.getMessage();
        }

    }

//Determines the final arrangement ID set based on overall records and filter selections.
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

//Processes enquiry filter criteria and retrieves arrangement IDs based on filters.
    public Set<String> getFilterCriteriaDets(List<FilterCriteria> filterCriteria) {
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
                        break;

                    case "DATE.TO":
                        endDate = value;
                        break;

                    case "PRODUCT":
                        filterValSet.add(value);
                        selProduct = value;
                        currentFilterSet.addAll(da.selectRecords(finMnemonic, AA_ARRANGEMENT, "",
                                "WITH ARR.STATUS NE PENDING.CLOSURE CLOSE AND PRODUCT EQ " + selProduct));
                        break;

                    case "CENTER":
                        filterValSet.add(value);
                        selCenterName = value;
                        fldName = CENTRE;
                        currentFilterSet.addAll(getArrAccountList(fldName, selCenterName));
                        break;

                    case "VILLAGE":
                        filterValSet.add(value);
                        selVillage = value;
                        fldName = VILLAGE;
                        currentFilterSet.addAll(getArrAccountList(fldName, selVillage));
                        break;

                    case "DISTRICT":
                        filterValSet.add(value);
                        selDistrict = value;
                        fldName = DISTRICT_NAME;
                        List<String> cusIdsBsDistName = da.selectRecords(finMnemonic, "EB.FF.LOAN.DETAILS", "",
                                "WITH DISTRICT.NAME EQ " + selDistrict);
                        currentFilterSet.addAll(cusIdsBsDistName);
                        break;

                    case "CYCLE":
                        filterValSet.add(value);
                        selCycle = value;
                        fldName = LOAN_CYCLE;
                        currentFilterSet.addAll(getArrAccountList(fldName, selCycle));
                        break;

                    case "PURPOSE":
                        filterValSet.add(value);
                        selPurpose = value;
                        fldName = LOAN_PURP;
                        currentFilterSet.addAll(getArrAccountList(fldName, selPurpose));
                        break;

                    case "RELIGION":
                        filterValSet.add(value);
                        selReligion = value;
                        fldName = RELIG_GROUP;
                        currentFilterSet.addAll(getCustomerArrList(fldName, selReligion));
                        break;

                    case "CASTE":
                        filterValSet.add(value);
                        selCaste = value;
                        fldName = CASTE;
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

    public List<String> getArrAccountList(String fieldName, String fieldValue) {
        List<String> arrAccList = da.selectRecords(finMnemonic, AA_ARR_ACCOUNT, "",
                "WITH " + fieldName + " EQ " + fieldValue);
        return getArrListFromSelection(arrAccList);
    }

    public List<String> getArrListFromSelection(List<String> aaArrAccList) {
        List<String> arrIdList = new ArrayList<>();

        try {
            if (aaArrAccList != null && !aaArrAccList.isEmpty()) {
                for (String arrId : aaArrAccList) {
                    String[] parts = arrId.split("-");
                    String aaId = parts[0];
                    if (!arrIdList.contains(aaId)) {
                        arrIdList.add(aaId);
                    }
                }
            }
        } catch (Exception e) {
            e.getMessage();
        }
        return arrIdList;
    }

    public List<String> getCustomerArrList(String fieldName, String fieldValue) {
        Set<String> customerSet = new LinkedHashSet<>(
                da.selectRecords(mnemonic, CUSTOMER, "", "WITH " + fieldName + " EQ " + fieldValue));
        Set<String> arrCustomerSet = new LinkedHashSet<>(
                da.selectRecords(finMnemonic, "AA.CUSTOMER.ARRANGEMENT", "", ""));
        customerSet.retainAll(arrCustomerSet);
        return getArrListFromCusSelection(customerSet);
    }

    public List<String> getArrListFromCusSelection(Set<String> custList) {
        List<String> aaIdList = new ArrayList<>();
        try {
            for (String custId : custList) {
                AaCustomerArrangementRecord custArrRec = new AaCustomerArrangementRecord(
                        da.getRecord(finMnemonic, "AA.CUSTOMER.ARRANGEMENT", "", custId));
                for (ProductLineClass prdLine : custArrRec.getProductLine()) {
                    if (prdLine.getProductLine().getValue().equals("LENDING")) {
                        for (ArrangementClass arrIds : prdLine.getArrangement()) {
                            aaIdList.add(arrIds.getArrangement().getValue());
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.getMessage();
        }
        return aaIdList;
    }

    // Loads company information such as mnemonic, branch name, zone, region, and
    // division.
    private void initialiseCompanyInfo(String companyId) {

        try {

            CompanyRecord companyObj = new CompanyRecord(da.getRecord(COMPANY, companyId));

            finMnemonic = companyObj.getFinancialMne().getValue();
            mnemonic = companyObj.getCustomerMnemonic().getValue();

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

    public String getCompanyDescription(String companyCode) {

        String compName = "";
        try {
            if (companyCode != null && !companyCode.isEmpty()) {
                CompanyRecord companyRec = new CompanyRecord(da.getRecord(COMPANY, companyCode));
                compName = companyRec.getCompanyName().get(0).getValue();
                String[] compNamePart = compName.split("-");
                compName = compNamePart[0];
                return compName;
            }
        } catch (Exception e) {

            e.getMessage();
        }
        return compName;
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
                                "BranchName", "BranchDistrict", "BranchState", "BranchCode", "CenterName", "CenterCode",
                                "CustomerName", "CustomerNumber", "CustomerAge", "Religion", "Occupation", "Purpose",
                                "LoanAccountNumber", "LegacyAccountNumber", "LoanDate", "LoanAmount", "ProductName",
                                "Cycle", "RelationshipOfficerName", "RelationshipOfficerMoblieNumber",
                                "BranchManagerName", "BranchManagerMoblieNumber", "FunderName", "PrincipalOutstanding",
                                "InterestOutstanding", "Dpd(Days)", "PrincipalDefault", "InterestDefault",
                                "TotalDefault", "LastInstallmentPaid");
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
