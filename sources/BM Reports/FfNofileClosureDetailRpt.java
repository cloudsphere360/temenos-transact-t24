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
import com.temenos.t24.api.records.aaaccountdetails.RepayReferenceClass;
import com.temenos.t24.api.records.aaactivitybalances.AaActivityBalancesRecord;
import com.temenos.t24.api.records.aaactivityhistory.AaActivityHistoryRecord;
import com.temenos.t24.api.records.aaactivityhistory.ActivityRefClass;
import com.temenos.t24.api.records.aaactivityhistory.EffectiveDateClass;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaarrbalancemaintenance.AaArrBalanceMaintenanceRecord;
import com.temenos.t24.api.records.aaarrbalancemaintenance.AdjBalTypeClass;
import com.temenos.t24.api.records.aaarrbalancemaintenance.AdjustPropClass;
import com.temenos.t24.api.records.aaarrtermamount.AaArrTermAmountRecord;
import com.temenos.t24.api.records.aabilldetails.AaBillDetailsRecord;
import com.temenos.t24.api.records.aabilldetails.PayPropertyClass;
import com.temenos.t24.api.records.aabilldetails.PaymentTypeClass;

import com.temenos.t24.api.records.aacustomerarrangementhist.AaCustomerArrangementHistRecord;
import com.temenos.t24.api.records.aacustomerarrangementhist.ArrangementClass;
import com.temenos.t24.api.records.aacustomerarrangementhist.ProductLineClass;
import com.temenos.t24.api.records.aaprddesaccount.AaPrdDesAccountRecord;
import com.temenos.t24.api.records.aaproduct.AaProductRecord;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.records.account.AltAcctTypeClass;
import com.temenos.t24.api.records.company.CompanyRecord;
import com.temenos.t24.api.records.companyconsol.CompanyConsolRecord;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.records.ebffcentredetail.EbFfCentreDetailRecord;
import com.temenos.t24.api.records.ebffloandetails.AddressTypeClass;
import com.temenos.t24.api.records.ebffloandetails.EbFfLoanDetailsRecord;

import com.temenos.t24.api.records.ebffloandpd.EbFfLoanDpdRecord;
import com.temenos.t24.api.records.ebffparameter.EbFfParameterRecord;
import com.temenos.t24.api.records.ebffparameter.ParamDescClass;
import com.temenos.t24.api.records.ebffrouser.EbFfRoUserRecord;
import com.temenos.t24.api.records.ebffvillage.EbFfVillageRecord;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.records.user.UserRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/*-----------------------------------------------------------------------------
 * @author Sathish Kumar K
 * Date Created:
 * Attached as : Nofile Routine
 * EB.API : FF.E.CLOSURE.LOAN.DETAIL.RPT
 * Attached to : STANDARD.SELECTION > NOFILE.FF.CLOSURE.LOAN.DETAIL.RPT
 * Description: Branch Online Report generation -> Closure Report
 *------------------------------------------------------------------------------ 
 * Modification History : NA
 *----------------------------------------------------------------------------- 
 *15-Dec-2025   Development      Initial Version
 *-----------------------------------------------------------------------------
 * *-----------------------------------------------------------------------------
*30-Mar-2026   Defect fixed and Remapping  Renuka M
*-----------------------------------------------------------------------------
 */

public class FfNofileClosureDetailRpt extends Enquiry {

    public static final String MAX_DATE_RANGE_ERR = "EB-FF.BM.PAST.MAX.DT.RANGE";

    public static final String NO_REC_ERR = "EB-NO.REC.SELECT";
    public static final String CENTRE = "FF.CENTRE";
    public static final String VILLAGE = "FF.VILLAGE";
    public static final String DISTRICT_NAME = "DISTRICT.NAME";
    public static final String LOAN_CYCLE = "FF.LOAN.CYCLE";
    public static final String LOAN_PURP = "FF.LOAN.PURP";
    public static final String RELIG_GROUP = "FF.RELIG.GROUP";
    public static final String CASTE = "FF.CASTE";

    List<String> retvalues = new ArrayList<>();
    List<String> finalArrList = new ArrayList<>();
    DataAccess da = new DataAccess(this);

    Set<String> filterValSet = new HashSet<>();

    String selDate = "";
    String selDateOp = "";
    String selProduct = "";
    String selCenterName = "";
    String selVillage = "";
    String selDistrict = "";
    String selCycle = "";
    String selPurpose = "";
    String selReligion = "";
    String selCaste = "";
    String startDateAA = "";
    String endDate = "";
    String zoneName = "";
    String regionName = "";
    String divisionName = "";
    String clusterName = "";
    String branchName = "";
    String branchDistrict = "";
    String branchState = "";
    String branchCode = "";
    String centerName = "";
    String centerCode = "";
    String customerName = "";
    String customerNumber = "";
    String customerAge = "";
    String loanAccountNumber = "";
    String religion = "";
    String occupation = "";
    String purpose = "";
    String loanDate = "";
    String loanAmount = "";
    String productName = "";
    String cycle = "";
    String roOfficerName = "";
    String roMobileNumber = "";
    String branchManagerName = "";
    String branchManagerMobileNumber = "";
    String disbursementDate = "";
    String maturityDate = "";
    String closureDate = "";
    String closureReason = "";

    int dpdAtClosure = 0;
    String newLoanAccount = "";

    String newLoanDate = "";
    String finMnemonic = "";
    String mnemonic = "";

    String fldName = "";
    String selBranch = "";
    String companyIds = "";

    boolean onlyDateFilter = false;
    boolean dateErrFlag = false;
    boolean noRecErrFlag = false;
    public static final String FILE_NAME = "ClosureRep_Det";
    List<String> outvalues = new ArrayList<>();

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    DateTimeFormatter outDateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HHmmss");

    private static final String COMPANY = "COMPANY";

    private static final String AA_ARRANGEMENT_RECORD = "AA.ARRANGEMENT";
    String fstName = "";
    String scdName = "";
    String familyName = "";
    boolean legacy = false;
    String origContractDate = "";
    String legacyAcctnum = "";
    String centerId = "";
    long waitingDays;
    String dropoutReason = "";
    String relationshipOfficerMobileNumber = "";
    String relationshipOfficerName = "";
    boolean dateRangeFlag = false;
    private static final String EB_FF_PARAMETER = "EB.FF.PARAMETER";
    int daterange = 0;
    int defaultDate = 0;
    Session session = new Session(this);
    String todayDate = session.getCurrentVariable("!TODAY");
    LocalDate today = LocalDate.parse(todayDate, formatter);
    String startDateAaDet = "";
    String startDate = "";
    String maxMonth = "";
    String defaultMonth = "";
    String centre = "";
    public static final DateTimeFormatter T24_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    public static final DateTimeFormatter MONTH_YEAR_FORMAT = DateTimeFormatter.ofPattern("MMMuuuu");
    Set<String> aaArrIdInLoanDpd = new HashSet<>();
    String arrId = "";
    FundsTransferRecord ftRecord = null;
    String ffCollType = "";
    String product = "";
    String productNameDescription = "";
    String village = "";
    String villageName = "";
    String selUser = "";
    String newLoanNumber = "";
    String newLoanProduct = "";
    String newLoanAmount = "";
    String newLoanDisbursementDate = "";
    AaArrangementRecord arrRec = null;
    AaArrTermAmountRecord aaArrTermAmt = null;
    double prinPaidAtClosure = 0.0;

    boolean settleClosureTriggered = false;

    boolean writeOffTriggered = false;
    boolean maturedTriggered = false;
    boolean writeOffSettAct = false;
    boolean insSettTriggered = false;
    boolean repaymentTriggered = false;
    boolean maturityTriggered = false;
    boolean activityFound = false;
    boolean writeOffSettTriggered = false;

    boolean dateGrp = false;
    String settleClsContId = "";
    String settleClsActRefId = "";
    String writeOffClosureDt = "";
    String writeOffActRefId = "";
    String matureActRefId = "";
    String writeOffSettContractId = "";
    String insSettContractId = "";
    String repaymentContractId = "";
    String writeOffSettActRefId = "";
    String insSettActRefId = "";
    String repaymentActRefId = "";
    String insSettClosureDt = "";
    String settleClosureDt = "";
    String repaymentClosureDt = "";
    String maturityClosureDt = "";
    String writeOffSettClosureDt = "";
    public static final String LENDING_APPLYPAYMENT_WRITEOFF_SETTLEMENT = "LENDING-APPLYPAYMENT-WRITEOFF.SETTLEMENT";
    public static final String LENDING_APPLYPAYMENT_INSURANCE_SETTLEMENT = "LENDING-APPLYPAYMENT-INSURANCE.SETTLEMENT";
    public static final String LENDING_APPLYPAYMENT_PR_OUTSTANDING_PAYOFF = "LENDING-APPLYPAYMENT-PR.OUTSTANDING.PAYOFF";
    public static final String LENDING_APPLYPAYMENT_PR_CURR_BALANCE = "LENDING-APPLYPAYMENT-PR.CURR.BALANCE";
    public static final String LENDING_APPLYPAYMENT_PR_INSURANCE_BALANCES = "LENDING-APPLYPAYMENT-PR.INSURANCE.BALANCES";
    public static final String LENDING_WRITE_OFF_BAL_MAINTAIN = "LENDING-WRITE.OFF-BAL.MAINTAIN";
    public static final String LENDING_SETTLE_FORECLOSURE = "LENDING-SETTLE-FORECLOSURE";
    public static final String LENDING_APPLYPAYMENT_PR_COLLECTION = "LENDING-APPLYPAYMENT-PR.COLLECTION";
    public static final String LENDING_MATURE_ARRANGEMENT = "LENDING-MATURE-ARRANGEMENT";
    String settleClosureActRefId = "";
    String settleClosureContractId = "";
    private static final String SECONDARY = "SECONDARY";
    public static final String ACCOUNT = "ACCOUNT";
    public static final String PRINTEREST = "PRINTEREST";
    public static final String CENTER = "EB.FF.CENTRE.DETAIL";
    String companyId = "";
    String coCode = "";
    String pastMonth = "";
    String datedefaultRange = "";
    String companyName = "";
    Set<String> activityList = new HashSet<>(
            Set.of(LENDING_APPLYPAYMENT_WRITEOFF_SETTLEMENT, LENDING_APPLYPAYMENT_PR_CURR_BALANCE,
                    LENDING_APPLYPAYMENT_INSURANCE_SETTLEMENT, LENDING_APPLYPAYMENT_PR_INSURANCE_BALANCES,
                    LENDING_SETTLE_FORECLOSURE, LENDING_APPLYPAYMENT_PR_OUTSTANDING_PAYOFF));

    Set<String> activityProps = new HashSet<>(Set.of("PRINTEREST.SM0PRINTEREST", "PRINTEREST.SM1PRINTEREST",
            "PRINTEREST.SM2PRINTEREST", "PRINTEREST.NPAPRINTEREST", "PRINTEREST.DUEPRINTEREST",
            "PRINTEREST.ACCPRINTEREST", "PRINTEREST.SM0PRINTERESTCUST", "PRINTEREST.SM1PRINTERESTCUST",
            "PRINTEREST.SM2PRINTERESTCUST", "PRINTEREST.NPAPRINTERESTCUST", "PRINTEREST.DUEPRINTERESTCUST",
            "PRINTEREST.ACCPRINTERESTCUST", "ACCOUNT.SM0ACCOUNT", "ACCOUNT.SM1ACCOUNT", "ACCOUNT.SM2ACCOUNT",
            "ACCOUNT.NPAACCOUNT", "ACCOUNT.DUEACCOUNT", "ACCOUNT.CURACCOUNT", "ACCOUNT.SM0ACCOUNTCUST",
            "ACCOUNT.SM1ACCOUNTCUST", "ACCOUNT.SM2ACCOUNTCUST", "ACCOUNT.NPAACCOUNTCUST", "ACCOUNT.DUEACCOUNTCUST",
            "ACCOUNT.CURACCOUNTCUST"));
    String closingPrincipal = "";
    String branchFile = "";

    /**
     * This setIds method used to filter the overall records from AA.ARRANGEMENT.
     */
    public List<String> setIds(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {

        try {

            Contract contract = new Contract(this);

            for (FilterCriteria filter : filterCriteria) {
                if (filter.getFieldname().equalsIgnoreCase("BRANCH")) {
                    selBranch = filter.getValue();

                    initialiseCompanyInfo(selBranch);

                    getLinkedCompIds(selBranch);
                    branchFile = getCompanyDescription(selBranch);
                    break;
                }
            }

            Set<String> closedArrSet = new LinkedHashSet<>(da.selectRecords(finMnemonic, AA_ARRANGEMENT_RECORD, "",
                    "WITH ARR.STATUS EQ CLOSE PENDING.CLOSURE AND CO.CODE EQ " + companyIds));

            getMaxdateRangedet();
            getDefaultDatRange();
            validateDateRange(daterange, defaultDate);

            Set<String> selectionSet = getFilterCriteriaDets(filterCriteria);

            Set<String> preFinalSet = getPreFinalset(closedArrSet, selectionSet);
            preFinalSet.retainAll(closedArrSet);
            // dateValidation();

            LocalDate start = startDate.isEmpty() ? today.minusDays(defaultDate)
                    : LocalDate.parse(startDate, formatter);
            LocalDate end = endDate.isEmpty() ? today : LocalDate.parse(endDate, formatter);

            for (String contractId : preFinalSet) {

                getAaActivityHistoryDetails(contractId);
                if (closureDate != null && !closureDate.isEmpty()) {

                    LocalDate closedDt = LocalDate.parse(closureDate, formatter);

                    if (!closedDt.isBefore(start) && !closedDt.isAfter(end)) {

                        processReturnList(contractId, contract);
                    }
                }
            }

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

            String outputPath = filePath + FILE_NAME + "_" + branchFile + "_" + selUser + "_" + currDate + "_"
                    + currTime + ".csv";

            writeToFile(outvalues, outputPath);

        } catch (Exception e1) {

            e1.printStackTrace();

        }
        if (dateErrFlag) {
            throw new T24CoreException("", "EB-FF.BM.DATE.FILTER.RANGE");
        } else if (noRecErrFlag || retvalues.isEmpty()) {
            throw new T24CoreException("", NO_REC_ERR);
        } else {
            return retvalues;
        }
    }

    /**
     * @param daterange
     * @param defaultDate
     */
    private void validateDateRange(int maxHistory, int maxRange) {
        try {
            LocalDate today = LocalDate.parse(todayDate, formatter);
            LocalDate maxBackDate = today.minusDays(maxHistory);

            if (startDate.isEmpty() && endDate.isEmpty()) {
                this.startDate = today.minusDays(maxRange).format(formatter);
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

                throw new T24CoreException(covertParamValue(pastMonth), MAX_DATE_RANGE_ERR);
            }

            if (daysBetween > maxRange) {

                throw new T24CoreException(covertParamValue(datedefaultRange), "EB-FF.BM.DATE.FILTER.RANGE");
            }

            this.startDate = stDt.format(formatter);
            this.endDate = endDt.format(formatter);
        } catch (Exception e2) {

            e2.printStackTrace();
        }
    }

    public String covertParamValue(String value) {
        try {
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

            }
        } catch (NumberFormatException e3) {

            e3.printStackTrace();
        }
        return value;
    }

    private void getDefaultDatRange() {
        try {

            String paramId = "FF.BM.REPORT.DATE.DEFAULT";

            EbFfParameterRecord paramRec = new EbFfParameterRecord(da.getRecord(EB_FF_PARAMETER, paramId));

            for (ParamDescClass paramDesc : paramRec.getParamDesc()) {
                if (paramDesc.getParamName().getValue().equals("CLOSURE")) {
                    defaultMonth = paramDesc.getParamValue().getValue();

                    if (defaultMonth.contains("1M")) {

                        defaultDate = 31;
                    }
                }
            }
        } catch (Exception e4) {

            e4.printStackTrace();

        }
    }

    private void getMaxdateRangedet() {
        try {

            String paramId = "FF.BM.REPORT.DATE.RANGE";

            EbFfParameterRecord paramRec = new EbFfParameterRecord(da.getRecord(EB_FF_PARAMETER, paramId));

            for (ParamDescClass paramDesc : paramRec.getParamDesc()) {
                if (paramDesc.getParamName().getValue().equals("CLOSURE")) {

                    maxMonth = paramDesc.getParamValue().getValue();

                    if (maxMonth.contains("3M")) {

                        daterange = 90;
                    }
                }
            }
        } catch (Exception e3) {

            e3.printStackTrace();

        }
    }

    /**
     * This processReturnList method used to fetch and add the field values.
     */
    public void processReturnList(String contractId, Contract contract) {
        try {

            branchName = "";
            branchDistrict = "";
            branchState = "";
            centerName = "";
            centerCode = "";
            customerName = "";
            customerNumber = "";
            customerAge = "";

            religion = "";
            occupation = "";
            purpose = "";
            loanDate = "";
            loanAmount = "";
            productName = "";
            cycle = "";
            relationshipOfficerName = "";
            relationshipOfficerMobileNumber = "";
            branchManagerName = "";
            branchManagerMobileNumber = "";
            disbursementDate = "";
            maturityDate = "";

            dpdAtClosure = 0;
            newLoanNumber = "";
            newLoanProduct = "";
            newLoanAmount = "";
            newLoanDisbursementDate = "";
            waitingDays = 0;
            dropoutReason = "";

            contract.setContractId(contractId);
            arrId = contractId;

            loanAmount = contract.getTermAmount().toString();

            maturityDate = convertDateFormat(contract.getMaturityDate().toString());

            getArrangementDetails(contract);
            getCustomerDetails(customerNumber);
            getAaArrAccountDetails(contract);
            getAaArrAccountDets(contract);

            getEbFfLoanDetails(arrId);

            getLoanDpd(arrId);
            getAaActivityHistoryDets(contractId, arrRec);

            List<String> row = new ArrayList<>();
            row.add(zoneName);
            row.add(regionName);
            row.add(divisionName);
            row.add(clusterName);
            row.add(branchName);
            row.add(branchDistrict);
            row.add(branchState);
            row.add(branchCode);
            row.add(centerName);
            row.add(centerCode);
            row.add(customerName);
            row.add(customerNumber);
            row.add(customerAge);
            row.add(arrId);
            row.add(legacyAcctnum);
            row.add(religion);
            row.add(occupation);
            row.add(purpose);
            row.add(loanDate);
            row.add(loanAmount);
            row.add(productNameDescription);
            row.add(cycle);
            row.add(relationshipOfficerName);
            row.add(relationshipOfficerMobileNumber);
            row.add(branchManagerName);
            row.add(branchManagerMobileNumber);
            row.add(disbursementDate);
            row.add(maturityDate);
            row.add(convertDateFormat(closureDate));
            row.add(closureReason);
            row.add(String.valueOf(prinPaidAtClosure));
            row.add(String.valueOf(dpdAtClosure));
            row.add(newLoanNumber);
            row.add(newLoanProduct);
            row.add(newLoanAmount);
            row.add(newLoanDisbursementDate);
            row.add(String.valueOf(waitingDays));
            row.add(dropoutReason);

            retvalues.add(String.join("*", row));
            outvalues.add(String.join(",", row));
        } catch (Exception e36) {

            e36.printStackTrace();

        }

    }

    /**
     * This initialiseCompanyInfo method used get the all branch wise zoneName,
     * regionName,divisionName and clusterName field values.
     */

    private void initialiseCompanyInfo(String companyId) {
        try {

            CompanyRecord companyObj = new CompanyRecord(da.getRecord(COMPANY, companyId));
            finMnemonic = companyObj.getFinancialMne().getValue();
            mnemonic = companyObj.getCustomerMnemonic().getValue();
            branchCode = companyId;
            branchName = getCompanyDescription(companyId);
            branchState = companyObj.getLocalRefField("FF.STATE").getValue();
            zoneName = getCompanyDescription(companyObj.getLocalRefField("FF.ZONE").getValue());
            regionName = getCompanyDescription(companyObj.getLocalRefField("FF.REGION").getValue());
            divisionName = getCompanyDescription(companyObj.getLocalRefField("FF.DIVISION").getValue());
            clusterName = getCompanyDescription(companyObj.getLocalRefField("FF.CLUSTER").getValue());

        } catch (Exception e4) {

            e4.printStackTrace();

        }
    }

    public String getCompanyDescription(String companyCode) {

        try {
            if (companyCode != null && !companyCode.isEmpty()) {
                CompanyRecord companyRec = new CompanyRecord(da.getRecord(COMPANY, companyCode));
                companyName = companyRec.getCompanyName().get(0).getValue();
                String[] compNamePart = companyName.split("-");
                companyName = compNamePart[0];

                return companyName;
            }
        } catch (Exception e6) {

            e6.printStackTrace();
        }

        return companyName;
    }

    /**
     * This getLinkedCompIds method used get the all branch records.
     */

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

        } catch (Exception e3) {

            e3.printStackTrace();

        }
    }

    /**
     * This getPreFinalset method used check filtercreteria field values.
     */

    public Set<String> getPreFinalset(Set<String> closedArrSet, Set<String> selectionSet) {

        Set<String> preFinalSet = null;

        if (selectionSet.isEmpty()) {
            if (!filterValSet.isEmpty()) {
                noRecErrFlag = true;
            } else {
                preFinalSet = closedArrSet;
            }
        } else {
            preFinalSet = new LinkedHashSet<>(selectionSet);

        }
        return preFinalSet == null ? new LinkedHashSet<>() : preFinalSet;
    }

    /**
     * This getFilterCriteriaDets method used check Selection field values.
     */

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
                    case "DATE.FROM":
                        startDate = value;
                        break;
                    case "DATE.TO":
                        endDate = value;
                        break;
                    case "USER":
                        selUser = value;

                        break;

                    case "PRODUCT":
                        selProduct = value;
                        break;
                    case "CENTER.NAME":

                        getAaArrAccountDet(loanAccountNumber);
                        selCenterName = value;
                        break;

                    case "VILLAGE":
                        getAaArrAccountDet(loanAccountNumber);
                        selVillage = value;

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

                    if (selectionSet == null) {
                        if (!currentFilterSet.isEmpty()) {
                            selectionSet = currentFilterSet;
                        }
                    } else {
                        selectionSet.retainAll(currentFilterSet);
                    }
                }
            }
        } catch (Exception e5) {

            e5.printStackTrace();

        }

        return selectionSet == null ? new LinkedHashSet<>() : selectionSet;
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
            System.out.println("Error while processing convertDateFormat");
            e.printStackTrace();
        }
        return inputDate;
    }

    public List<String> getArrAccountList(String fieldName, String fieldValue) {
        List<String> arrAccList = da.selectRecords(finMnemonic, "AA.ARR.ACCOUNT", "",
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
        } catch (Exception e7) {

            e7.printStackTrace();

        }

        return arrIdList;
    }

    /**
     * This getCustomerArrList method used check customer field values.
     */
    public List<String> getCustomerArrList(String fieldName, String fieldValue) {
        Set<String> customerSet = null;
        try {
            customerSet = new LinkedHashSet<>(
                    da.selectRecords(mnemonic, "CUSTOMER", "", "WITH " + fieldName + " EQ " + fieldValue));

            Set<String> closedArrCustomerSet = new LinkedHashSet<>(
                    da.selectRecords(finMnemonic, "AA.CUSTOMER.ARRANGEMENT", "", ""));

            customerSet.retainAll(closedArrCustomerSet);

        } catch (Exception e) {

            e.printStackTrace();
        }
        return getArrListFromCusSelection(customerSet);
    }

    public List<String> getArrListFromCusSelection(Set<String> custList) {
        List<String> aaIdList = new ArrayList<>();
        try {
            for (String custId : custList) {
                AaCustomerArrangementHistRecord custArrHistRec = new AaCustomerArrangementHistRecord(
                        da.getRecord(finMnemonic, "AA.CUSTOMER.ARRANGEMENT", "", custId));

                for (ProductLineClass prdLine : custArrHistRec.getProductLine()) {
                    if (prdLine.getProductLine().getValue().equals("LENDING")) {
                        for (ArrangementClass arrIds : prdLine.getArrangement()) {
                            aaIdList.add(arrIds.getArrangement().getValue());
                        }
                        break;
                    }
                }
            }
        } catch (Exception e8) {
            e8.printStackTrace();

        }
        return aaIdList;
    }

    /**
     * This getArrangementDetails method used fetch the fields value from
     * AA.ARRANGEMENT Application.
     */

    public void getArrangementDetails(Contract contract) {

        try {
            AaArrangementRecord arrRec = contract.getContract();// changed the object name
            coCode = arrRec.getCoCodeRec().getValue();
            initialiseCompanyInfo(coCode);

            loanAccountNumber = arrRec.getLinkedAppl(0).getLinkedApplId().getValue();
            getAaArrAccountDet(loanAccountNumber);
            customerNumber = arrRec.getCustomer().get(0).getCustomer().getValue();

            product = arrRec.getProduct().get(0).getProduct().getValue();
            getAaProductDetails(product);

            origContractDate = arrRec.getOrigContractDate().getValue();
            startDateAA = arrRec.getStartDate().getValue();
            if (!origContractDate.isEmpty()) {
                disbursementDate = convertDateFormat(origContractDate);

                legacy = true;
                loanDate = convertDateFormat(origContractDate);

            } else {
                disbursementDate = convertDateFormat(startDateAA);
                AccountRecord accountRecord = new AccountRecord(
                        da.getRecord(finMnemonic, "ACCOUNT", "", loanAccountNumber));

                if (legacy) {
                    for (AltAcctTypeClass altType : accountRecord.getAltAcctType()) {
                        if (altType.getAltAcctType().getValue().equals("LEGACY")) {
                            legacyAcctnum = altType.getAltAcctId().getValue();
                        }
                    }
                }

                loanDate = convertDateFormat(arrRec.getStartDate().getValue());

            }

        } catch (Exception e9) {

            e9.printStackTrace();

        }

    }

    /**
     * @param product2
     */
    private void getAaProductDetails(String product) {

        try {
            AaProductRecord aaProRec = new AaProductRecord(da.getRecord("AA.PRODUCT", product));
            productNameDescription = aaProRec.getDescription(0).getValue();

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    private void getAaArrAccountDet(String loanAccountNumber) {

        try {
            AccountRecord accRec = new AccountRecord(da.getRecord(finMnemonic, "ACCOUNT", "", loanAccountNumber));
            centre = accRec.getLocalRefField(CENTRE).getValue();
            village = accRec.getLocalRefField(VILLAGE).getValue();

            if (village != null && !village.isEmpty()) {
                getEbFfVillageDetails(village);
            }
            if (centre != null && !centre.isEmpty()) {
                getEbFfCentreNameDetails(centre);
            }
        } catch (Exception e) {

            e.printStackTrace();
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

            e.printStackTrace();
        }
    }

    public void getEbFfCentreNameDetails(String centre) {

        try {
            EbFfCentreDetailRecord centreRec = new EbFfCentreDetailRecord(da.getRecord("", CENTER, "", centre));
            centerName = centreRec.getCenterName().getValue();

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    /**
     * This getCustomerDetails method used fetch the customer fields value from
     * CUSTOMER Application.
     */

    public void getCustomerDetails(String customerNumber) {

        try {
            CustomerRecord cusRec = new CustomerRecord(da.getRecord(mnemonic, "CUSTOMER", "", customerNumber));

            for (TField name1 : cusRec.getName1()) {
                fstName = name1.getValue();
            }

            for (TField name2 : cusRec.getName2()) {
                scdName = name2.getValue();
            }

            familyName = cusRec.getFamilyName().getValue();
            customerName = String.join(" ", fstName, scdName, familyName).trim().replaceAll("\\s+", " ");

            religion = cusRec.getLocalRefField(RELIG_GROUP).getValue();

            String dob = cusRec.getDateOfBirth().getValue();
            LocalDate todayDt = LocalDate.parse(todayDate, formatter);
            LocalDate dateOfBirth = LocalDate.parse(dob, formatter);
            customerAge = String.valueOf(Period.between(dateOfBirth, todayDt).getYears());

        } catch (Exception e10) {

            e10.printStackTrace();

        }

    }

    /**
     * This getAaArrAccountDetails method used fetch the fields value from Property
     * ACCOUNT Application.
     */

    public void getAaArrAccountDetails(Contract contract) {

        try {
            AaPrdDesAccountRecord aaArrAccRec = new AaPrdDesAccountRecord(contract.getConditionForProperty("ACCOUNT"));
            centerCode = aaArrAccRec.getLocalRefField(CENTRE).getValue();
            cycle = aaArrAccRec.getLocalRefField(LOAN_CYCLE).getValue();
            purpose = aaArrAccRec.getLocalRefField(LOAN_PURP).getValue();
            if (centerCode != null && !centerCode.isEmpty()) {
                getEbFfCentreDetails(centerCode);
            }
            getAaArrAccountDets(contract);

        } catch (Exception e11) {

            e11.printStackTrace();

        }
    }

    /**
     * @param contract
     */

    private void getAaArrAccountDets(Contract contract) {

        try {
            List<String> aaArrAccountPrptyList = new ArrayList<>();
            aaArrAccountPrptyList.add("ACCOUNT");
            aaArrAccountPrptyList.add("LOANACCOUNT");
            for (String aaArrAcctid : aaArrAccountPrptyList) {
                AaPrdDesAccountRecord aaPrdDesAccountRecord = new AaPrdDesAccountRecord(
                        contract.getConditionForProperty(aaArrAcctid));

                centerId = aaPrdDesAccountRecord.getLocalRefField(CENTRE).getValue();

                getofficerName(centerId);
            }
        } catch (Exception e12) {

            e12.printStackTrace();
        }
    }

    /**
     * @param centerId2
     */
    private void getofficerName(String centerId) {

        try {
            EbFfCentreDetailRecord centerRec = new EbFfCentreDetailRecord(da.getRecord("", CENTER, "", centerId));

            String ebrelationshipOffName = centerRec.getCurrentRo().getValue();

            getffRoname(ebrelationshipOffName);
            String ebbranchManagerName = centerRec.getBranchManager().getValue();

            getBranchManagerdetail(ebbranchManagerName);
        } catch (Exception e13) {

            e13.printStackTrace();

        }

    }

    /**
     * @param ebrelationshipOffName
     */
    private void getffRoname(String ebrelationshipOffName) {

        try {

            EbFfRoUserRecord roRecord = new EbFfRoUserRecord(
                    da.getRecord("", "EB.FF.RO.USER", "", ebrelationshipOffName));

            relationshipOfficerName = roRecord.getRoName().getValue();

            relationshipOfficerMobileNumber = roRecord.getRoMobileNumber().getValue();

        } catch (Exception e14) {

            e14.printStackTrace();

        }
    }

    /**
     * @param ebbranchManagerName
     */
    private void getBranchManagerdetail(String ebbranchManagerName) {

        try {
            UserRecord userRec = new UserRecord(da.getRecord("", "USER", "", ebbranchManagerName));
            branchManagerName = userRec.getUserName().getValue();

            branchManagerMobileNumber = userRec.getLocalRefField("FF.MOBILE.NO").getValue();

        } catch (Exception e15) {

            e15.printStackTrace();

        }
    }

    /**
     * This getEbFfCentreDetails method used fetch the roOfficerName fields value
     * from EB.FF.CENTRE.DETAIL Table.
     */

    public void getEbFfCentreDetails(String centre) {

        try {
            EbFfCentreDetailRecord centreRec = new EbFfCentreDetailRecord(da.getRecord("", CENTER, "", centre));
            roOfficerName = centreRec.getCurrentRo().getValue();

        } catch (Exception e16) {

            e16.printStackTrace();

        }
    }

    /**
     * This getEbFfLoanDetails method used fetch the branchState fields value from
     * EB.FF.LOAN.DETAILS Table.
     */

    public void getEbFfLoanDetails(String arrId) {

        try {
            EbFfLoanDetailsRecord ffLoanDetsRec = new EbFfLoanDetailsRecord(
                    da.getRecord(finMnemonic, "EB.FF.LOAN.DETAILS", "", arrId));

            if (!ffLoanDetsRec.toString().isEmpty()) {
                for (AddressTypeClass addressType : ffLoanDetsRec.getAddressType()) {

                    branchDistrict = addressType.getDistrictName().getValue();
                }
                for (int entiNo = 0; entiNo < ffLoanDetsRec.getFmEntityNumber().size(); entiNo++) {
                    String relation = ffLoanDetsRec.getFmEntityNumber().get(entiNo).getRelation().getValue();
                    if (relation.equalsIgnoreCase("SELF")) {
                        occupation = ffLoanDetsRec.getFmEntityNumber().get(entiNo).getOccupation().getValue();

                    }
                }

            }
        }

        catch (Exception e17) {

            e17.printStackTrace();

        }

    }

    /**
     * This getBillAmountFromAccDets method used fetch the billId fields value from
     * AA.ACCOUNT.DETAILS.
     */

    public String getBillAmountFromAccDets(String arrId) {

        String billId = "";
        try {
            AaAccountDetailsRecord aaAccDets = new AaAccountDetailsRecord(
                    da.getRecord(finMnemonic, "AA.ACCOUNT.DETAILS", "", arrId));
            if (aaAccDets.getRepayReference().size() > 0) {
                int repayRefPos = aaAccDets.getRepayReference().size() - 1;
                RepayReferenceClass rePayRef = aaAccDets.getRepayReference().get(repayRefPos);
                if (rePayRef.getRpyBillId().size() > 0) {
                    int billIdPos = rePayRef.getRpyBillId().size() - 1;
                    billId = rePayRef.getRpyBillId().get(billIdPos).getValue();

                }
            }
        } catch (Exception e19) {

            e19.printStackTrace();

        }
        return getBillAmount(billId);
    }

    /**
     * This getBillAmount method used fetch the accPropAmt fields value from
     * AA.BILL.DETAILS.
     */

    public String getBillAmount(String billId) {

        String accPropAmt = "";
        try {
            AaBillDetailsRecord aaBillRec = new AaBillDetailsRecord(
                    da.getRecord(finMnemonic, "AA.BILL.DETAILS", "", billId));
            for (PaymentTypeClass paymentType : aaBillRec.getPaymentType()) {
                for (PayPropertyClass payProp : paymentType.getPayProperty()) {
                    if (payProp.getPayProperty().getValue().equalsIgnoreCase("ACCOUNT")) {
                        accPropAmt = payProp.getOrPrAmt().getValue();

                        return accPropAmt;
                    }
                }
            }
        } catch (Exception e20) {

            e20.printStackTrace();

        }
        return accPropAmt;
    }

    /**
     * This getNewLoanDetails method used fetch the some fields value from
     * AA.CUSTOMER.ARRANGEMENT.
     *//*
        * 
        * public void getNewLoanDetails(String custId, Contract contract) {
        * 
        * try { AaCustomerArrangementRecord custArrRec = new
        * AaCustomerArrangementRecord( da.getRecord(finMnemonic,
        * "AA.CUSTOMER.ARRANGEMENT", "", custId)); for
        * (com.temenos.t24.api.records.aacustomerarrangement.ProductLineClass prdLine :
        * custArrRec .getProductLine()) { if
        * (prdLine.getProductLine().getValue().equals("LENDING")) { for
        * (com.temenos.t24.api.records.aacustomerarrangement.ArrangementClass arrIds :
        * prdLine .getArrangement()) {
        * contract.setContractId(arrIds.getArrangement().getValue());
        * AaArrangementRecord arrRec = contract.getContract(); LocalDate newLoanStDt =
        * LocalDate.parse(arrRec.getStartDate().getValue(), formatter);
        * 
        * LocalDate lastLoanClDt = LocalDate.parse(closureDate, formatter);
        * 
        * } } } } catch (Exception e21) {
        * 
        * e21.printStackTrace();
        * 
        * } }
        */

    /**
     * This getAaActivityHistoryDets method used fetch the activity fields value
     * from AA.ACTIVITY.HISTORY.
     */
    public void getAaActivityHistoryDets(String arrId, AaArrangementRecord arrRec) {

        try {
            AaActivityHistoryRecord aaActHisRec = new AaActivityHistoryRecord(
                    da.getRecord(finMnemonic, "AA.ACTIVITY.HISTORY", "", arrId));
            for (EffectiveDateClass effectiveDate : aaActHisRec.getEffectiveDate()) {
                for (ActivityRefClass activeRef : effectiveDate.getActivityRef()) {
                    String activity = activeRef.getActivity().getValue();
                    if ((activity.equals("LENDING-WRITE.OFF-BAL.MAINTAIN"))
                            || (activity.equals("LENDING-APPLYPAYMENT-WRITEOFF.SETTLEMENT"))
                            || (activity.equals("LENDING-APPLYPAYMENT-INSURANCE.SETTLEMENT"))
                            || (activity.equals("LENDING-SETTLE-FORECLOSURE"))
                            || (activity.equals("LENDING-APPLYPAYMENT-PR.COLLECTION"))) {

                        String actStatus = activeRef.getActStatus().getValue();
                        String initiation = activeRef.getInitiation().getValue();
                        if (actStatus.equalsIgnoreCase("AUTH") && !initiation.equalsIgnoreCase("SECONDARY")) {

                            String contractIds = activeRef.getContractId().getValue();
                            if (contractIds.startsWith("FT")) {
                                String[] removeArg = contractIds.split("\\\\");
                                String ftId = removeArg[0];
                                try {

                                    ftRecord = new FundsTransferRecord(
                                            da.getRecord(finMnemonic, "FUNDS.TRANSFER", "", ftId));
                                } catch (Exception e) {
                                    try {
                                        ftRecord = new FundsTransferRecord(da.getHistoryRecord("FUNDS.TRANSFER", ftId));
                                    } catch (Exception e1) {
                                        e1.getMessage();
                                    }
                                    logFundsTransferDetails(ftRecord);
                                }

                            }
                        }
                    }

                    if (activity.equals("LENDING-SETTLE-FORECLOSURE") && ftRecord != null) {
                        String actStatus = activeRef.getActStatus().getValue();
                        String initiation = activeRef.getInitiation().getValue();
                        if (actStatus.equalsIgnoreCase("AUTH") && !initiation.equalsIgnoreCase("SECONDARY")) {

                            String contractIds = activeRef.getContractId().getValue();
                            if (contractIds.startsWith("FT")) {
                                String[] removeArg = contractIds.split("\\\\");
                                String ftId = removeArg[0];
                                try {

                                    ftRecord = new FundsTransferRecord(
                                            da.getRecord(finMnemonic, "FUNDS.TRANSFER", "", ftId));
                                } catch (Exception e) {
                                    try {
                                        ftRecord = new FundsTransferRecord(da.getHistoryRecord("FUNDS.TRANSFER", ftId));

                                        String netOffClose = ftRecord.getLocalRefField("FF.NETOFF.CLOSE").getValue();
                                        String netOffLoan = ftRecord.getLocalRefField("FF.NETOFF.LOAN").getValue();

                                        if (netOffClose != null && !netOffClose.trim().isEmpty()
                                                && !"NULL".equalsIgnoreCase(netOffClose.trim())) {

                                            newLoanNumber = netOffLoan;

                                            try {

                                                AaArrangementRecord newarrRecord = new AaArrangementRecord(da.getRecord(
                                                        finMnemonic, AA_ARRANGEMENT_RECORD, "", newLoanNumber));

                                                newLoanProduct = newarrRecord.getProduct().get(0).getProduct()
                                                        .getValue();

                                                newLoanDisbursementDate = convertDateFormat(
                                                        newarrRecord.getStartDate().getValue());

                                                Contract newLoanContract = new Contract(this);
                                                newLoanContract.setContractId(newLoanNumber);
                                                getNewLoanArrTermAmountDet(newLoanContract);
                                                LocalDate lastLoanClDt = LocalDate.parse(closureDate, formatter);

                                                LocalDate newLoanDisbDt = LocalDate.parse(newLoanDisbursementDate,
                                                        formatter);

                                                waitingDays = ChronoUnit.DAYS.between(lastLoanClDt, newLoanDisbDt);

                                            }

                                            catch (Exception ex) {

                                                ex.getMessage();
                                            }
                                        }

                                    } catch (Exception exx) {

                                        exx.getMessage();
                                    }

                                }

                            }

                        }
                    }
                }
            }
        } catch (Exception e22) {
            e22.printStackTrace();

        }
    }

    /**
     * @param newLoanContract
     */
    private void getNewLoanArrTermAmountDet(Contract newLoanContract) {

        try {

            AaArrTermAmountRecord aaArrTermAmount = new AaArrTermAmountRecord(
                    newLoanContract.getConditionForProperty("COMMITMENT"));

            if (aaArrTermAmount != null) {
                newLoanAmount = aaArrTermAmount.getAmount().getValue();

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param ftRecord2
     */
    private void logFundsTransferDetails(FundsTransferRecord ftRecord) {

        try {
            ffCollType = ftRecord.getLocalRefField("FF.COLL.TYPE").getValue();
            closureReason = ffCollType;

        } catch (Exception e35) {
            e35.printStackTrace();

        }

    }

    public void getAaActivityHistoryDetails(String arrId) {

        activityFound = false;
        writeOffTriggered = false;
        writeOffSettTriggered = false;
        insSettTriggered = false;
        settleClosureTriggered = false;
        repaymentTriggered = false;
        maturityTriggered = false;

        writeOffSettContractId = "";
        insSettContractId = "";
        settleClosureContractId = "";
        repaymentContractId = "";

        writeOffActRefId = "";
        writeOffSettActRefId = "";
        insSettActRefId = "";
        settleClosureActRefId = "";
        repaymentActRefId = "";

        writeOffClosureDt = "";
        writeOffSettClosureDt = "";
        insSettClosureDt = "";
        settleClosureDt = "";
        repaymentClosureDt = "";
        maturityClosureDt = "";

        prinPaidAtClosure = 0.0;

        try {
            AaActivityHistoryRecord aaActHisRec = new AaActivityHistoryRecord(
                    da.getRecord(finMnemonic, "AA.ACTIVITY.HISTORY", "", arrId));

            for (EffectiveDateClass effectiveDate : aaActHisRec.getEffectiveDate()) {
                for (ActivityRefClass activeRef : effectiveDate.getActivityRef()) {
                    processBasedOnActivity(effectiveDate, activeRef);
                    if (activityFound) {
                        break;
                    }
                }
                if (activityFound) {
                    break;
                }
            }

            getAaActivityBalDetails(arrId, activityList, activityProps, null);

            if (writeOffTriggered) {
                closureDate = writeOffClosureDt;

            } else if (writeOffSettTriggered) {
                closureDate = writeOffSettClosureDt;

            } else if (insSettTriggered) {
                closureDate = insSettClosureDt;

            } else if (settleClosureTriggered) {
                closureDate = settleClosureDt;

            } else if ((maturityTriggered && repaymentTriggered) && (maturityClosureDt.equals(repaymentClosureDt))
                    && (prinPaidAtClosure == 0.0)) {
                closureDate = repaymentClosureDt;

                activityList.clear();
                activityList.add(LENDING_APPLYPAYMENT_PR_COLLECTION);
                getAaActivityBalDetails(arrId, activityList, activityProps, maturityClosureDt);
            }

            prinPaidAtClosure = Math.abs(prinPaidAtClosure);

        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void processBasedOnActivity(EffectiveDateClass effectiveDate, ActivityRefClass activeRef) {
        String activity = activeRef.getActivity().getValue();
        String actStatus = activeRef.getActStatus().getValue();
        String initiation = activeRef.getInitiation().getValue();

        if (initiation.equalsIgnoreCase(SECONDARY)) {
            return;
        }
        if (!actStatus.equalsIgnoreCase("AUTH")) {
            return;
        }

        String contractId = activeRef.getContractId().getValue();
        String activityRefId = activeRef.getActivityRef().getValue();
        String effectiveDt = effectiveDate.getEffectiveDate().getValue();

        switch (activity) {
        case LENDING_WRITE_OFF_BAL_MAINTAIN:
            writeOffTriggered = true;
            writeOffClosureDt = effectiveDt;
            writeOffActRefId = activityRefId;

            break;

        case LENDING_APPLYPAYMENT_WRITEOFF_SETTLEMENT:
            writeOffSettTriggered = true;
            writeOffSettClosureDt = effectiveDt;
            writeOffSettActRefId = activityRefId;
            writeOffSettContractId = contractId;
            activityFound = true;
            break;

        case LENDING_APPLYPAYMENT_INSURANCE_SETTLEMENT:
            insSettTriggered = true;
            insSettClosureDt = effectiveDt;
            insSettActRefId = activityRefId;
            insSettContractId = contractId;
            activityFound = true;
            break;

        case LENDING_SETTLE_FORECLOSURE:
            settleClosureTriggered = true;
            settleClosureDt = effectiveDt;
            settleClosureActRefId = activityRefId;
            settleClosureContractId = contractId;
            activityFound = true;
            break;

        case LENDING_APPLYPAYMENT_PR_COLLECTION:
            repaymentTriggered = true;
            repaymentClosureDt = effectiveDt;
            repaymentActRefId = activityRefId;
            repaymentContractId = contractId;
            break;

        case LENDING_MATURE_ARRANGEMENT:
            maturityTriggered = true;
            maturityClosureDt = effectiveDt;
            if (repaymentTriggered) {
                activityFound = true;
            }
            break;

        default:
            break;
        }
    }

    public void getAaArrBalMaintDets(Contract contract) {
        try {
            AaArrBalanceMaintenanceRecord arrBalMainRec = new AaArrBalanceMaintenanceRecord(
                    contract.getConditionForProperty("BAL.MAINTAIN"));
            for (AdjustPropClass adjProp : arrBalMainRec.getAdjustProp()) {
                proceesToGetAdjBalDets(adjProp);
            }
        } catch (NumberFormatException e) {
            e.getMessage();
        } catch (Exception e1) {
            e1.getMessage();
        }
    }

    public void proceesToGetAdjBalDets(AdjustPropClass adjProp) {
        try {
            if (adjProp.getAdjustProp().getValue().equals(ACCOUNT)
                    || adjProp.getAdjustProp().getValue().equals(PRINTEREST)) {
                for (AdjBalTypeClass adjBal : adjProp.getAdjBalType()) {
                    if (adjBal.getAdjBalType().getValue().equals("CURACCOUNT")) {
                        prinPaidAtClosure = Double.parseDouble(adjBal.getOrigBalAmt().getValue())
                                - Double.parseDouble(adjBal.getNewBalAmt().getValue());
                    }

                }
            }
        } catch (NumberFormatException e) {
            e.getMessage();
        }
    }

    public void getAaActivityBalDetails(String arrId, Set<String> activityList, Set<String> activityProps,
            String matDate) {

        try {
            AaActivityBalancesRecord aaActbalRec = new AaActivityBalancesRecord(
                    da.getRecord(finMnemonic, "AA.ACTIVITY.BALANCES", "", arrId));
            for (com.temenos.t24.api.records.aaactivitybalances.ActivityRefClass accRefList : aaActbalRec
                    .getActivityRef()) {
                if (matDate != null && !matDate.isEmpty() && !matDate.equals(accRefList.getActivityDate().getValue())) {
                    continue;
                }

                String activity = accRefList.getActivity().getValue();
                if (activityList.contains(activity)) {
                    sumPropertyAmounts(accRefList, activityProps);
                }
            }
        } catch (Exception e) {
            e.getMessage();
        }

    }

    public void sumPropertyAmounts(com.temenos.t24.api.records.aaactivitybalances.ActivityRefClass accRefList,
            Set<String> propertyNames) {

        try {
            for (com.temenos.t24.api.records.aaactivitybalances.PropertyClass prop : accRefList.getProperty()) {
                String propName = prop.getProperty().getValue();
                double propAmt = Double.parseDouble(prop.getPropertyAmt().getValue());
                if (propertyNames.contains(propName)) {

                    if (propName.startsWith(ACCOUNT)) {
                        prinPaidAtClosure += propAmt;

                    }
                }
            }

        } catch (NumberFormatException e) {
            e.getMessage();
        } catch (Exception e1) {
            e1.getMessage();
        }
    }

    private void getLoanDpd(String arrId) {

        try {

            LocalDate currDate = LocalDate.parse(todayDate, T24_FORMATTER);
            String currMonthYear = currDate.format(MONTH_YEAR_FORMAT).toUpperCase();
            String loanDpdRecId = arrId + "-" + currMonthYear;
            EbFfLoanDpdRecord loanDpdRecord = new EbFfLoanDpdRecord(
                    da.getRecord(finMnemonic, "EB.FF.LOAN.DPD", "", loanDpdRecId));

            int dateListSize = loanDpdRecord.getDate().size();
            String loanDpd = "";

            if (dateListSize > 1) {

                String latestDpd = loanDpdRecord.getDate().get(dateListSize - 1).getCurDpd().getValue();

                int curDpdInt = Integer.parseInt(latestDpd);

                if (curDpdInt == 0) {

                    loanDpd = loanDpdRecord.getDate().get(dateListSize - 2).getCurDpd().getValue();
                } else {

                    loanDpd = latestDpd;
                }

                dpdAtClosure = Integer.parseInt(loanDpd);

            }
        } catch (Exception e25) {
            e25.printStackTrace();

        }
    }

    /**
     * This writeToFile method used write the fields values displayed in csv file.
     */

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
                                "CustomerName", "CustomerNumber", "CustomerAge", "LoanAccountNumber",
                                "LegacyLoanNumber", "Religion", "Occupation", "Purpose", "LoanDate", "LoanAmount",
                                "ProductName", "Cycle", "relationshipOfficerName", "relationshipOfficerMobileNumber",
                                "BranchManagerName", "BranchManagerMobileNumber", "DisbursementDate", "MaturityDate",
                                "ClosureDate", "ClosureReason", "PrincipalPaidAtClosure", "DpdAtClosure",
                                "NewLoanAccount", "NewLoanProduct", "NewLoanAmount", "NewLoanDate", "waitingDays",
                                "DropoutReason");

                        writer.write(header + System.lineSeparator());
                    }

                    for (String line : data) {
                        writer.write(line + System.lineSeparator());
                    }
                }
            }
        } catch (Exception e24) {
            e24.printStackTrace();

        }

    }

}
