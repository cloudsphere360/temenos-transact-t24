package com.temenos.fusion;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.temenos.api.TField;
import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.aa.contractapi.BalanceMovement;
import com.temenos.t24.api.complex.eb.servicehook.ServiceData;
import com.temenos.t24.api.hook.system.ServiceLifecycle;
import com.temenos.t24.api.records.aaaccountdetails.AaAccountDetailsRecord;
import com.temenos.t24.api.records.aaaccountdetails.BillIdClass;
import com.temenos.t24.api.records.aaaccountdetails.BillPayDateClass;
import com.temenos.t24.api.records.aaactivitybalances.AaActivityBalancesRecord;
import com.temenos.t24.api.records.aaactivityhistory.AaActivityHistoryRecord;
import com.temenos.t24.api.records.aaactivityhistory.ActivityRefClass;
import com.temenos.t24.api.records.aaactivityhistory.EffectiveDateClass;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaarrangementactivity.AaArrangementActivityRecord;
import com.temenos.t24.api.records.aaarrbalancemaintenance.AaArrBalanceMaintenanceRecord;
import com.temenos.t24.api.records.aaarrbalancemaintenance.AdjBalTypeClass;
import com.temenos.t24.api.records.aaarrbalancemaintenance.AdjustPropClass;
import com.temenos.t24.api.records.aabilldetails.AaBillDetailsRecord;
import com.temenos.t24.api.records.aabilldetails.PropertyClass;
import com.temenos.t24.api.records.aabilldetails.RepayRefClass;
import com.temenos.t24.api.records.aaoverduestats.AaOverdueStatsRecord;
import com.temenos.t24.api.records.aaprddesaccount.AaPrdDesAccountRecord;
import com.temenos.t24.api.records.aaprddesaccount.AltIdTypeClass;
import com.temenos.t24.api.records.aaprddestermamount.AaPrdDesTermAmountRecord;
import com.temenos.t24.api.records.aaproduct.AaProductRecord;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.records.company.CompanyRecord;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.records.dates.DatesRecord;
import com.temenos.t24.api.records.ebffcentredetail.EbFfCentreDetailRecord;
import com.temenos.t24.api.records.ebffcollectiondets.EbFfCollectionDetsRecord;
import com.temenos.t24.api.records.ebffcollectiondetshistory.EbFfCollectionDetsHistoryRecord;
import com.temenos.t24.api.records.ebffcustdpd.EbFfCustDpdRecord;
import com.temenos.t24.api.records.ebffloandetails.AddressTypeClass;
import com.temenos.t24.api.records.ebffloandetails.EbFfLoanDetailsRecord;
import com.temenos.t24.api.records.ebffloandpd.EbFfLoanDpdRecord;
import com.temenos.t24.api.records.ebffloanpaymenthis.DemandDateClass;
import com.temenos.t24.api.records.ebffloanpaymenthis.EbFfLoanPaymentHisRecord;
import com.temenos.t24.api.records.ebffparameter.EbFfParameterRecord;
import com.temenos.t24.api.records.ebffparameter.ParamDescClass;
import com.temenos.t24.api.records.ebffrouser.EbFfRoUserRecord;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Date;
import com.temenos.t24.api.system.Session;

/**
 * -----------------------------------------------------------------------------
 * 
 * @author DEEPAKUMAR S / SOUVAGYARANJAN Date Created:26-NOV-2025 Attached as :
 *         Service Routine JAR NAME: L3clientWiseReport.jar
 *         EB.API>FF.CLIENTWISE.REPORT EB.API>FF.CLIENTWISE.REPORT.SELECT
 *         PGM.FILE>FF.CLIENTWISE.REPORT Attached to
 *         :BATCH>BNK/FF.CLIENTWISE.REPORT Description :ClientwiseDisbursement
 *         Report
 *         ------------------------------------------------------------------------------
 *         Modification History :
 *         -----------------------------------------------------------------------------
 *         26-NOV-2025 Development Initial Version
 *         -----------------------------------------------------------------------------
 *         7-JAN-2026 Defect Meera/rishab
 *         -------------------------------------------------------------------------------
 *         22-JAN-2026 Defect Jerome/Rishab
 *         --------------------------------------------------------------------------------
 *         05-FEB-2026 Remapping Jerome
 *         ----------------------------------------------------------------------------------
 *         26-FEB-2026 Remapping SOUVAGYARANJAN ROUT
 *         -----------------------------------------------------------------------------------
 *         08-APRIL-2026 Remapping Meera
 *         ----------------------------------------------------------------------------------
 */
public class FfClientWiseCadReport extends ServiceLifecycle {

    private static final String PENDING_CLOSURE = "PENDING.CLOSURE";
    private static final String CLOSE = "CLOSE";
    private static final String CURRENT = "CURRENT";
    private static final String FF_FUND_SOURCE = "FF.FUND.SOURCE";
    private static final String FF_CENTRE = "FF.CENTRE";
    private static final String CUSTOMER = "CUSTOMER";
    private static final String UNPAID = "UNPAID";

    private static final String ACCOUNT = "ACCOUNT";
    private static final String AA_ACCOUNT_DETAILS = "AA.ACCOUNT.DETAILS";
    private static final String AA_BILL_DETAILS = "AA.BILL.DETAILS";
    private static final String PRINTEREST = "PRINTEREST";
    private static final String FUNDS_TRANSFER = "FUNDS.TRANSFER";
    private static final String TRADE = "TRADE";// BOOKING
    public static final String LENDING_APPLYPAYMENT_WRITEOFF_SETTLEMENT = "LENDING-APPLYPAYMENT-WRITEOFF.SETTLEMENT";
    public static final String LENDING_APPLYPAYMENT_INSURANCE_SETTLEMENT = "LENDING-APPLYPAYMENT-INSURANCE.SETTLEMENT";
    public static final String LENDING_APPLYPAYMENT_PR_OUTSTANDING_PAYOFF = "LENDING-APPLYPAYMENT-PR.OUTSTANDING.PAYOFF";
    public static final String LENDING_APPLYPAYMENT_PR_CURR_BALANCE = "LENDING-APPLYPAYMENT-PR.CURR.BALANCE";
    public static final String LENDING_APPLYPAYMENT_PR_INSURANCE_BALANCES = "LENDING-APPLYPAYMENT-PR.INSURANCE.BALANCES";
    public static final String LENDING_WRITE_OFF_BAL_MAINTAIN = "LENDING-WRITE.OFF-BAL.MAINTAIN";
    public static final String LENDING_SETTLE_FORECLOSURE = "LENDING-SETTLE-FORECLOSURE";
    public static final String LENDING_APPLYPAYMENT_PR_COLLECTION = "LENDING-APPLYPAYMENT-PR.COLLECTION";
    public static final String LENDING_MATURE_ARRANGEMENT = "LENDING-MATURE-ARRANGEMENT";
    public static final String LENDING_SETTLE_PR_COLLECTION = "LENDING-SETTLE-PR.COLLECTION";

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

    List<String> arrList = new ArrayList<>();
    DataAccess da = new DataAccess(this);
    Session ses = new Session(this);

    String lstdate = "";
    String productGroup = "";
    String product = "";
    String ffFundSource = "";
    String district = "";
    String accountStatus = "";
    String finMnemonic = "";
    String mnemonic = "";
    String officeName = "";
    String officeCode = "";
    String arrId = "";
    String state = "";
    String addrType = "";
    String loanAmount = "";
    String compCode = "";
    String acctNumber = "";
    String customerNumber = "";

    String cycleNumber = "";
    String ffLoanPurp = "";
    String officerCode = "";
    String primOfficerName = "";

    String ffCentre = "";
    String ffGrpCode = "";
    String parkedAmount = "";
    String currentOd = "";
    String overdueIntrestDemand = "";
    String principalOverdue = "";
    String totalInterestCollected = "";
    String interestOverdue = "";
    String principalOutstanding = "";
    String maximumPrincipalOverdueDays = "";
    String interestOutstanding = "";

    String disbursementDate = "";
    String overdueInstallment = "";

    String accountDpdClassification = "";
    String currentPrincipalInstallment = "";
    String currentInterestInstallment = "";
    String currentInstallmentDemandDate = "";

    String overduePrincipalDemnad = "";
    String overdueInstallmentPrincipalCollection = "";
    String overdueInstallmentInterestCollection = "";
    String currentInstallmentPrincipalCollection = "";
    String currentInstallmentInterestCollection = "";
    String office = "";
    String loanNumber = "";
    String centerCode = "";
    String centerName = "";
    String caste = "";
    String religion = "";
    String occupation = "";
    String gramPanchayat = "";
    String companyId = "";
    String approvalDate = "";
    String salesOfficerName = "";
    String salesOfficerCode = "";
    String subPurpose = "";
    String vendorType = "";
    String businessType = "";
    String categoryOfLoan = "";
    String idComp2 = "";
    String overdueDays = "";
    String rationCard = "";
    String loanStatus = "";
    String acctStatus = "";
    String fundSource = "";
    String customerDpd = "";
    String customerDpdClassification = "";
    String cusMenonic = "";
    List<ParamDescClass> paramDescList = new ArrayList<>();
    Date date = new Date(this);
    String legacyAccNo = "";
    String accountCloseFlag = "";
    String closingDate = "";
    String billStatus = "";
    String billid = "";
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    DateTimeFormatter outDateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    BillPayDateClass billPayDateSet;
    String customerNumber2 = "";
    String origContractdate = "";
    String monthPrincipalInstallment = "";
    String monthlyCurrentInterest = "";
    String installInterest = "";
    List<BillPayDateClass> billPayDateList = null;
    String totalOverInstallment = "";
    String fundingSourceType = "";
    String principalCollection = "";
    String billType = "";
    boolean migratedContractFlg = false;
    String productName = "";
    String cocode = "";
    String todayYearMonth = "";
    String formatted = "";
    String currInstDemandDate = "";
    String ovrDueInstPrinColl = "";
    String ovrDueInstInterestColl = "";
    String currInstPrinColl = "";
    String currInstInterestColl = "";
    String principalPrePayment = "";
    String interestPrePayment = "";
    String monthYear = "";
    double prinPropAmt = 0.0;
    double intPropAmt = 0.0;
    double prinPropAmt1 = 0.0;
    double intPropAmt1 = 0.0;

    EbFfParameterRecord ebFfParamRec = null;
    boolean paramFlag = false;
    String paramPath = "";
    String paraDesc = "";
    String todayDate = ses.getCurrentVariable("!TODAY");
    LocalDate todateformatted = LocalDate.parse(todayDate, formatter);
    String dateformatted = todateformatted.getMonth().toString().substring(0, 3) + todateformatted.getYear();

    double prinPaidAtClosure = 0.0;
    double intPaidAtClosure = 0.0;
    double totIntCollAmt = 0.0;
    double totPrinCollAmt = 0.0;

    boolean writeOffTriggered = false;
    boolean writeOffSettTriggered = false;
    boolean insSettTriggered = false;
    boolean settleClosureTriggered = false;
    boolean repaymentTriggered = false;
    boolean maturityTriggered = false;

    String writeOffSettContractId = "";
    String insSettContractId = "";
    String settleClosureContractId = "";
    String repaymentContractId = "";

    String writeOffActRefId = "";
    String writeOffSettActRefId = "";
    String insSettActRefId = "";
    String settleClosureActRefId = "";
    String repaymentActRefId = "";
    String latestDueDate = "";
    String writeOffClosureDt = "";
    String writeOffSettClosureDt = "";
    String insSettClosureDt = "";
    String settleClosureDt = "";
    String repaymentClosureDt = "";
    String maturityClosureDt = "";
    boolean activityFound = false;
    double prinPropAmt2 = 0.0;
    double intPropAmt2 = 0.0;

    double overDuePropAmt = 0.0;
    double overDueIntPropAmt = 0.0;

    double aAOverDueInsPrnColl = 0.0;
    double aAOverDueInsInteColl = 0.0;
    double aACurInsPrnColl = 0.0;
    double aACurInsInteColl = 0.0;

    double hisOverDueInsPrinColl = 0.0;
    double hisOverDueInsInteColl = 0.0;
    double hisCurInsPrinColl = 0.0;
    double hisCurInsInterColl = 0.0;
    String currentDate = "";
    String currentInstallmentPrincipal = "";
    String currentInstallmentInterest = "";
    String interestCapitalized = "";
    String currentMonthODInstallment = "";
    String currentMonthODPrincipal = "";
    String currentMonthODInterest = "";
    String currentOdPrinCollection = "";
    String currentOdIntCollection = "";
    String currentOdInstalCollection = "";

    @Override
    public List<String> getIds(ServiceData serviceData, List<String> controlList) {

        try {
            initialiseCompanyInfo(serviceData);

            arrList = da.selectRecords(finMnemonic, "AA.ARRANGEMENT", "", "");

        } catch (Exception e) {
            e.getMessage();

        }
        return arrList;
    }

    @Override
    public void process(String id, ServiceData serviceData, String controlItem) {
        List<String> outvalues = new ArrayList<>();

        getResetVariables();

        monthYear = todayDate.substring(0, 6);

        todayYearMonth = todayDate.substring(0, 6);

        String year = monthYear.substring(0, 4);

        String month = monthYear.substring(4, 6);

        formatted = month + "-" + year;

        try {

            initialiseCompanyInfo(serviceData);

            Contract contract = new Contract(this);
            arrId = id;

            loanNumber = arrId;
            contract.setContractId(arrId);

            getEbLoanDetails(arrId);
            getEcbDetails(contract);

            getArrDets(arrId);

            getaAPrdDesAccount(contract);
            getloandpdStatus(arrId);
            getCustdpdStatus(customerNumber);
            getaAArrTermAmount(contract);
            getEbCollectionDetsndHisDetails(arrId);
            getOverdueInstalmentDets(arrId);
            getAaActivtyBalances(arrId, todayDate);
            getAaActivityHistoryDets(arrId);
            getCurrentMonthODInstallment(arrId);
            getCurrentDate(arrId);
            getInterestCapitalized(arrId);
            getCurrentMonthCollected(arrId);

            getAccount(acctNumber);

            List<String> row = new ArrayList<>();

            row.add(formatted);
            row.add(loanNumber);
            row.add(legacyAccNo);

            row.add(customerNumber2);
            row.add(principalOutstanding);
            row.add(overdueDays);
            row.add(primOfficerName);
            row.add(officerCode);
            row.add(ffGrpCode);
            row.add(centerCode);

            row.add(office);
            row.add(officeName);
            row.add(productName);
            row.add(district);
            row.add(state);
            row.add(ffFundSource);
            row.add(fundingSourceType);
            row.add(accountStatus);
            row.add(formatDate(origContractdate));
            row.add(cycleNumber);
            row.add(loanAmount);
            row.add(ffLoanPurp);
            row.add(subPurpose);
            row.add(principalOverdue);
            row.add(interestOverdue);
            row.add(totalOverInstallment);

            row.add(currInstDemandDate);
            row.add(monthPrincipalInstallment);
            row.add(monthlyCurrentInterest);

            row.add(ovrDueInstPrinColl);
            row.add(ovrDueInstInterestColl);
            row.add(currInstPrinColl);
            row.add(currInstInterestColl);
            row.add(principalPrePayment);
            row.add(interestPrePayment);
            row.add(currentMonthODPrincipal);
            row.add(currentMonthODInterest);
            row.add(currentMonthODInstallment);
            row.add(currentOdPrinCollection);
            row.add(currentOdIntCollection);
            row.add(currentOdInstalCollection);
            row.add(interestCapitalized);
            row.add(currentDate);
            row.add(currentInstallmentPrincipal);
            row.add(currentInstallmentInterest);

            outvalues.add(String.join(",", row));

            if (!outvalues.isEmpty()) {

                String outputPath = paramPath + "CAD&CompressedReport" + "_" + finMnemonic + "_" + todayDate + "_temp_"
                        + ses.getSessionNumber() + ".csv";
                writeToFile(outvalues, outputPath);
            }

        } catch (Exception e) {
            e.getMessage();

        }
    }

    public void getResetVariables() {

        formatted = "";
        loanNumber = "";
        legacyAccNo = "";
        customerNumber2 = "";
        principalOutstanding = "";
        overdueDays = "";
        primOfficerName = "";
        officerCode = "";
        ffGrpCode = "";
        centerCode = "";
        office = "";
        officeName = "";
        productName = "";
        district = "";
        state = "";
        ffFundSource = "";
        fundingSourceType = "";
        accountStatus = "";
        origContractdate = "";
        cycleNumber = "";
        loanAmount = "";
        ffLoanPurp = "";
        subPurpose = "";
        principalOverdue = "";
        interestOverdue = "";
        totalOverInstallment = "";
        currInstDemandDate = "";
        monthPrincipalInstallment = "";
        monthlyCurrentInterest = "";
        ovrDueInstPrinColl = "";
        ovrDueInstInterestColl = "";
        currInstPrinColl = "";
        currInstInterestColl = "";
        principalPrePayment = "";
        interestPrePayment = "";
        currentDate = "";
        currentInstallmentPrincipal = "";
        currentInstallmentInterest = "";
        interestCapitalized = "";
        currentMonthODInstallment = "";
        currentMonthODPrincipal = "";
        currentMonthODInterest = "";
        currentOdPrinCollection = "";
        currentOdIntCollection = "";
        currentOdInstalCollection = "";

    }

    private void getEbCollectionDetsndHisDetails(String arrId) {

        try {
            double principalInstallment = 0.0;
            double interestInstallment = 0.0;

            EbFfCollectionDetsRecord ebffCollection = null;
            EbFfCollectionDetsHistoryRecord ebffHistory = null;

            List<TField> dueDates = null;

            if (migratedContractFlg) {
                ebffHistory = new EbFfCollectionDetsHistoryRecord(da.getRecord(finMnemonic,
                        "EB.FF.COLLECTION.DETS.HISTORY", "", arrId + "-" + approvalDate + ".01"));
                dueDates = ebffHistory.getDueDate();

            } else {

                ebffCollection = new EbFfCollectionDetsRecord(

                        da.getRecord(finMnemonic, "EB.FF.COLLECTION.DETS", "", arrId));

                dueDates = ebffCollection.getDueDate();
            }

            if (dueDates != null && !dueDates.isEmpty()) {

                latestDueDate = getLatestDueDate(principalInstallment, interestInstallment, latestDueDate,
                        ebffCollection, ebffHistory, dueDates);

            }
            if (latestDueDate != null && !latestDueDate.isEmpty()) {

                currInstDemandDate = formatDate(latestDueDate);

            }

        } catch (

        Exception e) {

            e.getMessage();

        }

    }

    private String getLatestDueDate(double principalInstallment, double interestInstallment, String latestDueDate,
            EbFfCollectionDetsRecord ebffCollection, EbFfCollectionDetsHistoryRecord ebffHistory,
            List<TField> dueDates) {

        try {

            if (dueDates == null || dueDates.isEmpty()) {
                return latestDueDate;
            }

            String firstDueDate = dueDates.get(0).getValue();

            for (int k = migratedContractFlg ? 0 : 1; k < dueDates.size(); k++) {

                String dueDate = dueDates.get(k).getValue();
                // boolean dateLogic = getCurrentWorkDate(dueDate);

                if (!getCurrentWorkDate(dueDate)) {
//                if (dueDate == null || dueDate.isEmpty() || !todayYearMonth.equals(dueDate.substring(0, 6))
//                        || dueDate.compareTo(todayDate) > 0 || dueDate.equals(todayDate)
//                        || (!migratedContractFlg && dueDate.equals(firstDueDate))) {
//                    continue;
//                }
                    continue;
                }
                if (!migratedContractFlg && dueDate.equals(firstDueDate)) {
                    continue;
                }

                if (migratedContractFlg) {
                    principalInstallment += getDoubleParse(ebffHistory.getPrincipalAmt().get(k).getValue());
                    interestInstallment += getDoubleParse(ebffHistory.getInterestAmt().get(k).getValue());
                } else {
                    principalInstallment += getDoubleParse(ebffCollection.getPrincipalAmt().get(k).getValue());
                    interestInstallment += getDoubleParse(ebffCollection.getInterestAmt().get(k).getValue());
                }

                if (latestDueDate == null || dueDate.compareTo(latestDueDate) > 0) {
                    latestDueDate = dueDate;
                }
            }

            monthPrincipalInstallment = String.format("%.2f", principalInstallment);
            monthlyCurrentInterest = String.format("%.2f", interestInstallment);

        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        return latestDueDate;
    }

    private boolean getCurrentWorkDate(String dueDate) {
        if (dueDate == null || dueDate.isEmpty()) {
            return false;
        }

        String lastWorkingDay = date.getDates().getLastWorkingDay().getValue();

        LocalDate reportDate = LocalDate.parse(lastWorkingDay, formatter);
        LocalDate reportPeriodStart = reportDate.withDayOfMonth(1);
        LocalDate dueDateValue = LocalDate.parse(dueDate, formatter);

        // 1st day of month <= due date <= last working day
        return !dueDateValue.isBefore(reportPeriodStart) && !dueDateValue.isAfter(reportDate);
    }

    private String formatDate(String inputDate) {
        String outDate = "";
        try {
            LocalDate date1 = LocalDate.parse(inputDate, formatter);
            outDate = date1.format(outDateFormatter);
            return outDate;
        } catch (Exception e) {
            e.getMessage();
        }
        return outDate;
    }

    public void appendIfNotEmpty(StringBuilder valName, String value) {
        if (value != null && !value.isEmpty()) {
            if (valName.length() > 0) {
                valName.append(" ");
            }
            valName.append(value);
        }
    }

    private void getEbLoanDetails(String arrId) {

        try {

            DatesRecord dateRec = date.getDates();
            lstdate = dateRec.getLastWorkingDay().getValue();

            EbFfLoanDetailsRecord ffLoanDetsRec = new EbFfLoanDetailsRecord(
                    da.getRecord(finMnemonic, "EB.FF.LOAN.DETAILS", "", arrId));

            for (AddressTypeClass addressType : ffLoanDetsRec.getAddressType()) {

                addrType = addressType.getAddressType().getValue();

                if (addrType.equalsIgnoreCase(CURRENT)) {

                    state = addressType.getStateName().getValue();

                    district = addressType.getDistrictName().getValue();
                }

            }

        } catch (Exception e) {
            e.getMessage();
        }

    }

    private void getArrDets(String arrId) {
        String accStat = "";

        try {
            AaArrangementRecord aaRec = null;
            aaRec = new AaArrangementRecord(da.getRecord(finMnemonic, "AA.ARRANGEMENT", "", arrId));

            loanStatus = aaRec.getArrStatus().getValue();
            acctStatus = aaRec.getArrStatus().getValue();
            approvalDate = aaRec.getStartDate().getValue();

            compCode = aaRec.getCoCodeRec().getValue();
            office = compCode;
            getCompanyInfo(compCode);

            acctNumber = aaRec.getLinkedAppl(0).getLinkedApplId().getValue();

            customerNumber = aaRec.getCustomer().get(0).getCustomer().getValue();

            accStat = aaRec.getArrStatus().getValue();
            getAccountStatus(accStat);
            productGroup = aaRec.getProductGroup().getValue();
            CustomerRecord cusDetails = new CustomerRecord(da.getRecord(CUSTOMER, customerNumber));
            religion = cusDetails.getLocalRefField("FF.RELIG.GROUP").getValue();
            caste = cusDetails.getLocalRefField("FF.CASTE").getValue();
            occupation = cusDetails.getLocalRefField("FF.FM.OCCUP").getValue();
            rationCard = cusDetails.getLocalRefField("FF.RATION.CARD").getValue();
            if (aaRec.getOrigContractDate().getValue() != null && !aaRec.getOrigContractDate().getValue().isEmpty()) {
                migratedContractFlg = true;
                origContractdate = aaRec.getOrigContractDate().getValue();
                customerNumber2 = cusDetails.getMnemonic().getValue();
            } else {
                origContractdate = aaRec.getStartDate().getValue();
                customerNumber2 = customerNumber;
            }

            product = aaRec.getProduct().get(0).getProduct().getValue();

            AaProductRecord aaProRec = new AaProductRecord(da.getRecord("AA.PRODUCT", product));

            productName = aaProRec.getDescription(0).getValue();

            aaBillDetails(arrId);

        } catch (Exception e) {
            e.getMessage();
        }

    }

    private void getCompanyInfo(String compCode2) {
        try {
            String comNameSub = "";
            CompanyRecord companyObj = new CompanyRecord(da.getRecord("COMPANY", compCode2));
            comNameSub = companyObj.getCompanyName(0).getValue();
            if (comNameSub != null && comNameSub.length() > 4) {
                comNameSub = comNameSub.substring(0, comNameSub.length() - 4);
            }
            officeName = comNameSub;
        } catch (Exception e) {
            e.getMessage();
        }

    }

    private void aaBillDetails(String arrId2) {
        try {
            String startDate = "";
            AaAccountDetailsRecord aaAccDetails = new AaAccountDetailsRecord(
                    da.getRecord(finMnemonic, AA_ACCOUNT_DETAILS, "", arrId2));

            startDate = aaAccDetails.getStartDate().toString();

            disbursementDate = convertDateFormat(startDate);
            accountDpdClassification = aaAccDetails.getArrAgeStatus().getValue();

        } catch (Exception e) {
            e.getMessage();

        }
    }

    private void getOverdueInstalmentDets(String arrId) {
        try {
            double ovrDueInstAmt = 0.0;
            AaAccountDetailsRecord aaAccDetails = new AaAccountDetailsRecord(
                    da.getRecord(finMnemonic, AA_ACCOUNT_DETAILS, "", arrId));

            for (BillPayDateClass billPayDtList : aaAccDetails.getBillPayDate()) {
                for (BillIdClass billIdList : billPayDtList.getBillId()) {
                    if (billIdList.getBillType().getValue().equals("INSTALLMENT")
                            && billIdList.getSetStatus().getValue().equals(UNPAID)
                            && billIdList.getBillStatus().getValue().equals("AGING")) {
                        String overDueBillId = billIdList.getBillId().getValue();
                        AaBillDetailsRecord aaBillDetRec = new AaBillDetailsRecord(
                                da.getRecord(finMnemonic, AA_BILL_DETAILS, "", overDueBillId));

                        String osPropAmt = aaBillDetRec.getOrTotalAmount().getValue();

                        ovrDueInstAmt += getDoubleParse(osPropAmt);

                    }
                }
            }
            totalOverInstallment = String.format("%.2f", ovrDueInstAmt);

        } catch (NumberFormatException e) {
            e.getMessage();
        }
    }

    private void getAccountStatus(String accStat) {
        String arrAge = "";
        if (!accStat.equals(CLOSE) && !accStat.equals(PENDING_CLOSURE)) {
            AaAccountDetailsRecord aaArrAcctDet = new AaAccountDetailsRecord(da.getRecord(AA_ACCOUNT_DETAILS, arrId));
            arrAge = aaArrAcctDet.getArrAgeStatus().getValue();

            switch (arrAge) {
            case "SM0":
                accountStatus = "ACTIVE-SMA0";
                break;
            case "SM1":
                accountStatus = "ACTIVE-SMA1";
                break;
            case "SM2":
                accountStatus = "ACTIVE-SMA2";
                break;
            case "NPA":
                accountStatus = "ACTIVE-NPA";
                break;
            default:
                accountStatus = "ACTIVE";
            }
            accountCloseFlag = "0";
        }
        if (accStat.equals(CLOSE) || accStat.equals(PENDING_CLOSURE)) {

            accountCloseFlag = "1";
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

    private void getaAArrTermAmount(Contract contract) {
        try {
            AaPrdDesTermAmountRecord aaArrTermAmtRec = new AaPrdDesTermAmountRecord(
                    contract.getConditionForProperty("COMMITMENT"));

            loanAmount = aaArrTermAmtRec.getAmount().getValue();

        } catch (Exception e) {
            e.getMessage();
        }

    }

    private void getaAPrdDesAccount(Contract contract) {
        List<String> aaArrAccountPrptyList = new ArrayList<>();
        aaArrAccountPrptyList.add(ACCOUNT);

        for (String aaArrAcctid : aaArrAccountPrptyList) {

            try {
                AaPrdDesAccountRecord aaprdDesAccRec = new AaPrdDesAccountRecord(
                        contract.getConditionForProperty(aaArrAcctid));//
                for (AltIdTypeClass altType : aaprdDesAccRec.getAltIdType()) {

                    if (altType.getAltIdType().getValue().equals("LEGACY")) {

                        legacyAccNo = altType.getAltId().getValue();

                    }

                }

            } catch (Exception e) {
                e.getMessage();
            }
        }

    }

    private void getFfCentreDetail(String ffCentre2) {

        try {

            EbFfCentreDetailRecord centreDet = new EbFfCentreDetailRecord(

                    da.getRecord("EB.FF.CENTRE.DETAIL", ffCentre2));

            officerCode = centreDet.getCurrentRo().getValue();

            if (officerCode != null && !officerCode.isEmpty()) {

                getFfRoUser(officerCode);
            }

        } catch (Exception e) {

            e.getMessage();

        }

    }

    private void getFfRoUser(String officerCode) {
        try {

            EbFfRoUserRecord centreDet = new EbFfRoUserRecord(

                    da.getRecord("EB.FF.RO.USER", officerCode));

            primOfficerName = centreDet.getRoName().getValue();

        } catch (Exception e) {

            e.getMessage();

        }

    }

    private void getAccount(String id) {
        String ffLoanStatus = "";
        try {
            AccountRecord accRec = new AccountRecord(da.getRecord(finMnemonic, ACCOUNT, "", id));

            if (!accRec.toString().isEmpty()) {
                ffGrpCode = accRec.getLocalRefField("FF.GROUP").getValue();

                centerCode = accRec.getLocalRefField(FF_CENTRE).getValue();
                if (centerCode != null && !centerCode.isEmpty()) {
                    getFfCentreDetail(centerCode);

                }
                getAccountLocalRef(accRec);
                ffLoanPurp = accRec.getLocalRefField("FF.LOAN.PURP").getValue();
                subPurpose = accRec.getLocalRefField("FF.LOAN.SUBPUR").getValue();
                ffLoanStatus = accRec.getLocalRefField("FF.LOAN.STATUS").getValue();
                if (ffLoanStatus != null && !ffLoanStatus.isEmpty()) {
                    accountStatus = ffLoanStatus;
                }
            }
        } catch (Exception e) {

            e.getMessage();

        }

    }

    private void getAccountLocalRef(AccountRecord accRec) {
        try {
            cycleNumber = accRec.getLocalRefField("FF.LOAN.CYCLE").getValue();
            fundingSourceType = accRec.getLocalRefField(FF_FUND_SOURCE).getValue();
            ffFundSource = accRec.getLocalRefField("FF.FUNDER.NAME").getValue();
        } catch (Exception e) {
            e.getMessage();
        }

    }

    public void initialiseCompanyInfo(ServiceData serviceData) {

        try {
            companyId = serviceData.getCompanyId();

            CompanyRecord companyObj = new CompanyRecord(da.getRecord("COMPANY", companyId));

            finMnemonic = companyObj.getFinancialMne().getValue();
            cusMenonic = companyObj.getCustomerMnemonic().getValue();

            if (!paramFlag) {
                ebFfParamRec = new EbFfParameterRecord(da.getRecord("EB.FF.PARAMETER", "FF.COB.REPORT.EXTRACT"));
                if (!ebFfParamRec.getParamDesc().isEmpty()) {
                    paramDescList = ebFfParamRec.getParamDesc();
                    for (ParamDescClass paramDesc : paramDescList) {
                        paraDesc = paramDesc.getParamDesc().getValue();
                        if (paraDesc.equals("Custom Path for COB Reports")) {
                            paramPath = paramDesc.getParamValue().getValue();

                        }

                    }
                }
                paramFlag = true;
            }
        } catch (Exception e) {
            e.getMessage();
        }

    }

    public void getCustdpdStatus(String customerNumber) {

        try {

            String recId = "CUS" + customerNumber + "-" + dateformatted;
            EbFfCustDpdRecord ebcus = new EbFfCustDpdRecord(da.getRecord(cusMenonic, "EB.FF.CUST.DPD", "", recId));
            List<com.temenos.t24.api.records.ebffcustdpd.DateClass> dateClass = ebcus.getDate();
            customerDpd = dateClass.get(dateClass.size() - 1).getCurDpd().getValue();
            customerDpdClassification = dateClass.get(dateClass.size() - 1).getDpdStatus().getValue();

        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void getloandpdStatus(String arrId) {

        String lrecIds = arrId + "-" + dateformatted;
        EbFfLoanDpdRecord loanDpd = null;
        try {
            loanDpd = new EbFfLoanDpdRecord(da.getRecord(finMnemonic, "EB.FF.LOAN.DPD", "", lrecIds));
        } catch (Exception e) {
            e.getMessage();
        }
        // List<DateClass> datesClass = loanDpd.getDate();

        if (loanDpd != null && !loanDpd.toString().isEmpty()) {
            int size = loanDpd.getDate().size();
            if (size > 0) {
                overdueDays = loanDpd.getDate().get(size - 1).getCurDpd().getValue();
            }
        } else {
            String overdueId = arrId + "-INSTALLMENT-DPD.STAGES";
            AaOverdueStatsRecord overdueRec = null;
            try {
                overdueRec = new AaOverdueStatsRecord(da.getRecord("AA.OVERDUE.STATS", overdueId));
            } catch (Exception e) {
                e.getMessage();
            }
            if (overdueRec != null && !overdueRec.toString().isEmpty()) {
                overdueDays = "";
            } else {
                overdueDays = "0";
            }
        }

        // overdueDays = datesClass.get(datesClass.size() - 1).getCurDpd().getValue();
        // maximumPrincipalOverdueDays = datesClass.get(datesClass.size() -
        // 1).getDpdStatus().getValue();

    }

    public void getEcbDetails(Contract contract) {
        double principaloutstand = 0.0;
        double principalOver = 0.0;
        double interestOver = 0.0;
        try {
            principaloutstand = Double.valueOf(getBalance(contract, "FFPRIOUTAMT", TRADE));
            principalOutstanding = String.format("%.2f", Math.abs(principaloutstand));
            overduePrincipalDemnad = principalOutstanding;

            principalOver = Double.valueOf(getBalance(contract, "FFPRINCDEF", TRADE));
            principalOverdue = String.format("%.2f", Math.abs(principalOver));

            interestOver = Double.valueOf(getBalance(contract, "FFINTERDEF", TRADE));
            interestOverdue = String.format("%.2f", Math.abs(interestOver));

            interestOutstanding = principalOverdue;
            overdueInstallmentPrincipalCollection = principalOverdue;
            overdueInstallmentInterestCollection = currentOd;
            currentInstallmentPrincipalCollection = currentPrincipalInstallment;
            currentInstallmentInterestCollection = currentInterestInstallment;

        } catch (NumberFormatException e) {
            e.getMessage();
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

    private void getAaActivtyBalances(String arrId2, String todayDate2) {
        aAOverDueInsPrnColl = 0.0;
        aAOverDueInsInteColl = 0.0;
        aACurInsPrnColl = 0.0;
        aACurInsInteColl = 0.0;

        hisOverDueInsPrinColl = 0.0;
        hisOverDueInsInteColl = 0.0;
        hisCurInsPrinColl = 0.0;
        hisCurInsInterColl = 0.0;

        try {

            AaActivityBalancesRecord aaActBal = new AaActivityBalancesRecord(
                    da.getRecord(finMnemonic, "AA.ACTIVITY.BALANCES", "", arrId2));

            // String todayYearMonth1 = todayDate2.substring(0, 6);

            for (com.temenos.t24.api.records.aaactivitybalances.ActivityRefClass activityList : aaActBal
                    .getActivityRef()) {

                String activity = activityList.getActivity().getValue();

                if (!(LENDING_APPLYPAYMENT_PR_COLLECTION.equals(activity)
                        || LENDING_SETTLE_PR_COLLECTION.equals(activity))) {
                    continue;
                }

//                if (!LENDING_APPLYPAYMENT_PR_COLLECTION.equals(activityList.getActivity().getValue())) {
//                    continue;
//                }

                String actDate = activityList.getActivityDate().getValue();

//                if (!todayYearMonth1.equals(actDate.substring(0, 6)) || actDate.compareTo(todayDate2) >= 0) {
//                    continue;
//                }
                if (!getCurrentWorkDate(actDate)) {
                    continue;
                }

                for (com.temenos.t24.api.records.aaactivitybalances.PropertyClass propertyList : activityList
                        .getProperty()) {

                    String property = propertyList.getProperty().getValue();

                    if (isOverDuePrincipalProperty(property)) {

                        aAOverDueInsPrnColl += getDoubleParse(propertyList.getPropertyAmt().getValue());

                    }
                    if (isPRInterestProperty(property)) {

                        aAOverDueInsInteColl += getDoubleParse(propertyList.getPropertyAmt().getValue());

                    }
                    if (isDueAccountProperty(property)) {

                        aACurInsPrnColl += getDoubleParse(propertyList.getPropertyAmt().getValue());

                    }
                    if (isDueInterestProperty(property)) {

                        aACurInsInteColl += getDoubleParse(propertyList.getPropertyAmt().getValue());

                    }

                }
            }
            getLoanPaymentHistory(arrId2);

            ovrDueInstPrinColl = String.format("%.2f", Math.abs(aAOverDueInsPrnColl + hisOverDueInsPrinColl));

            ovrDueInstInterestColl = String.format("%.2f", Math.abs(aAOverDueInsInteColl + hisOverDueInsInteColl));

            currInstPrinColl = String.format("%.2f", Math.abs(aACurInsPrnColl + hisCurInsPrinColl));

            currInstInterestColl = String.format("%.2f", Math.abs(aACurInsInteColl + hisCurInsInterColl));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isPRInterestProperty(String property) {

        return property.contains("SM0PRINTEREST") || property.contains("SM1PRINTEREST")
                || property.contains("SM2PRINTEREST") || property.contains("NPAPRINTEREST")
                || property.contains("SM0PRINTERESTCUST") || property.contains("SM1PRINTERESTCUST")
                || property.contains("SM2PRINTERESTCUST") || property.contains("NPAPRINTERESTCUST");
    }

    private boolean isDueAccountProperty(String property) {

        return property.contains("DUEACCOUNT") || property.contains("DUEACCOUNTCUST");
    }

    private boolean isDueInterestProperty(String property) {

        return property.contains("DUEPRINTEREST") || property.contains("DUEPRINTERESTCUST");
    }

    private boolean isOverDuePrincipalProperty(String property) {

        return property.contains("SM0ACCOUNT") || property.contains("SM1ACCOUNT") || property.contains("SM2ACCOUNT")
                || property.contains("NPAACCOUNT") || property.contains("SM0ACCOUNTCUST")
                || property.contains("SM1ACCOUNTCUST") || property.contains("SM2ACCOUNTCUST")
                || property.contains("NPAACCOUNTCUST");
    }

    public void getLoanPaymentHistory(String arrId) {

        try {

            EbFfLoanPaymentHisRecord paymentHis = new EbFfLoanPaymentHisRecord(
                    da.getRecord("", "EB.FF.LOAN.PAYMENT.HIS", "", arrId));

            for (DemandDateClass demandDateList : paymentHis.getDemandDate()) {

                String pymtDate = demandDateList.getPymtDate().getValue();
                String dmtDate = demandDateList.getDemandDate().getValue();
                String transType = demandDateList.getTransType().getValue();
                String pymtamt = demandDateList.getPymtAmt().getValue();

                if (pymtDate == null || pymtDate.isEmpty() || dmtDate == null || dmtDate.isEmpty()) {
                    continue;
                }

                // String pymtDateFormat = pymtDate.substring(0, 6);

                // boolean validPaymentDate = todayYearMonth.equals(pymtDateFormat) &&
                // pymtDate.compareTo(todayDate) < 0;

                boolean validPaymentDate = getCurrentWorkDate(pymtDate);

                if ("PRINCIPAL".equalsIgnoreCase(transType) && dmtDate.compareTo(pymtDate) < 0 && validPaymentDate) {
                    hisOverDueInsPrinColl += getDoubleParse(pymtamt);
                }

                if (("INTEREST".equalsIgnoreCase(transType) || "OVERDUE INTEREST".equalsIgnoreCase(transType))
                        && dmtDate.compareTo(pymtDate) < 0 && validPaymentDate) {

                    hisOverDueInsInteColl += getDoubleParse(pymtamt);
                }

                if ("PRINCIPAL".equalsIgnoreCase(transType) && dmtDate.equals(pymtDate) && validPaymentDate) {

                    hisCurInsPrinColl += getDoubleParse(pymtamt);
                }

                if ("INTEREST".equalsIgnoreCase(transType) && dmtDate.equals(pymtDate) && validPaymentDate) {

                    hisCurInsInterColl += getDoubleParse(pymtamt);
                }

            }

        } catch (Exception e) {
            e.getMessage();
        }
    }

    private Double getDoubleParse(String pymtamt) {
        double amt = 0.0;
        try {
            amt = Double.parseDouble(pymtamt);
        } catch (NumberFormatException e) {
            e.getMessage();
        }
        return amt;
    }

    public void getAaActivityHistoryDets(String arrId) {

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

        intPaidAtClosure = 0.0;
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
                accountStatus = "WRITE.OFF CLOSURE";
                // getAaArrBalMaintDets(contract);

                getInputterNameFromAAA(writeOffActRefId);
            } else if (writeOffSettTriggered) {

                String ftId = writeOffSettContractId.split("\\\\")[0];
                getFundsTransferDetails(ftId);

            } else if (insSettTriggered) {

                accountStatus = "DEATH CLOSURE";
                String ftId = insSettContractId.split("\\\\")[0];
                getFundsTransferDetails(ftId);

            } else if (settleClosureTriggered) {

                accountStatus = "FORECLOSURE";
                String ftId = settleClosureContractId.split("\\\\")[0];
                getFundsTransferDetails(ftId);

            } else if ((maturityTriggered && repaymentTriggered)
                    && (maturityClosureDt.equals(repaymentClosureDt) && (prinPaidAtClosure == 0.0))) {

                accountStatus = "MATURITY CLOSURE";
                String ftId = repaymentContractId.split("\\\\")[0];
                getFundsTransferDetails(ftId);

                intPaidAtClosure = 0.0;

                activityList.clear();
                activityList.add(LENDING_APPLYPAYMENT_PR_COLLECTION);

                getAaActivityBalDetails(arrId, activityList, activityProps, maturityClosureDt);

            }

            principalPrePayment = String.format("%.2f", Math.abs(prinPaidAtClosure));
            interestPrePayment = String.format("%.2f", Math.abs(intPaidAtClosure));

        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void processBasedOnActivity(EffectiveDateClass effectiveDate, ActivityRefClass activeRef) {
        String activity = activeRef.getActivity().getValue();
        String actStatus = activeRef.getActStatus().getValue();
        String initiation = activeRef.getInitiation().getValue();

        if (initiation.equalsIgnoreCase("SECONDARY")) {
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
            // activityFound = true; // change
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
            activityFound = true;
            break;

        default:
            break;
        }
    }

    public void getFundsTransferDetails(String ftId) {
        FundsTransferRecord ftRec = null;
        try {
            ftRec = new FundsTransferRecord(da.getRecord(finMnemonic, FUNDS_TRANSFER, "", ftId));
        } catch (Exception e) {
            try {
                ftRec = new FundsTransferRecord(da.getHistoryRecord(FUNDS_TRANSFER, ftId));
            } catch (Exception e1) {
                e.getMessage();
            }
        }
        if (ftRec != null) {

            accountStatus = ftRec.getLocalRefField("FF.COLL.TYPE").getValue();
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
                    if (propName.startsWith(PRINTEREST)) {
                        intPaidAtClosure += propAmt;
                    }
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

    public void getInputterNameFromAAA(String activityRefId) {

        try {
            AaArrangementActivityRecord aaArrAct = new AaArrangementActivityRecord(
                    da.getRecord(finMnemonic, "AA.ARRANGEMENT.ACTIVITY", "", activityRefId));

            accountStatus = aaArrAct.getNarrative().get(0).getValue();

        } catch (Exception e) {
            e.getMessage();
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
        }
    }

    public void proceesToGetAdjBalDets(AdjustPropClass adjProp) {
        if (adjProp.getAdjustProp().getValue().equals(ACCOUNT)
                || adjProp.getAdjustProp().getValue().equals(PRINTEREST)) {
            for (AdjBalTypeClass adjBal : adjProp.getAdjBalType()) {
                if (adjBal.getAdjBalType().getValue().equals("CURACCOUNT")) {
                    prinPaidAtClosure = getDoubleParse(adjBal.getOrigBalAmt().getValue())
                            - getDoubleParse(adjBal.getNewBalAmt().getValue());
                }
                if (adjBal.getAdjBalType().getValue().equals("ACCPRINTEREST")) {
                    intPaidAtClosure = getDoubleParse(adjBal.getOrigBalAmt().getValue())
                            - getDoubleParse(adjBal.getNewBalAmt().getValue());
                }
            }
        }
    }

    private void getCurrentDate(String arrId) {

        try {
            double principalInstallment = 0.0;
            double interestInstallment = 0.0;

            latestDueDate = "";
            currentDate = "";
            currentInstallmentPrincipal = "";
            currentInstallmentInterest = "";

            EbFfCollectionDetsRecord ebffCollection = null;
            EbFfCollectionDetsHistoryRecord ebffHistory = null;

            List<TField> dueDates = null;

            if (migratedContractFlg) {
                ebffHistory = new EbFfCollectionDetsHistoryRecord(da.getRecord(finMnemonic,
                        "EB.FF.COLLECTION.DETS.HISTORY", "", arrId + "-" + approvalDate + ".01"));
                dueDates = ebffHistory.getDueDate();

            } else {

                ebffCollection = new EbFfCollectionDetsRecord(

                        da.getRecord(finMnemonic, "EB.FF.COLLECTION.DETS", "", arrId));

                dueDates = ebffCollection.getDueDate();
            }

            if (dueDates != null && !dueDates.isEmpty()) {

                latestDueDate = getCurrentDateValue(principalInstallment, interestInstallment, latestDueDate,
                        ebffCollection, ebffHistory, dueDates);

            }
            if (latestDueDate != null && !latestDueDate.isEmpty()) {

                currentDate = formatDate(latestDueDate);

            }

        } catch (

        Exception e) {

            e.getMessage();

        }

    }

    private String getCurrentDateValue(double principalInstallment, double interestInstallment, String latestDueDate,
            EbFfCollectionDetsRecord ebffCollection, EbFfCollectionDetsHistoryRecord ebffHistory,
            List<TField> dueDates) {

        try {

            if (dueDates == null || dueDates.isEmpty()) {
                return latestDueDate;
            }

            String firstDueDate = dueDates.get(0).getValue();

            for (int k = migratedContractFlg ? 0 : 1; k < dueDates.size(); k++) {

                String dueDate = dueDates.get(k).getValue();

                if (dueDate == null || dueDate.isEmpty() || !todayYearMonth.equals(dueDate.substring(0, 6))
                        || !dueDate.equals(todayDate) || (!migratedContractFlg && dueDate.equals(firstDueDate))) {
                    continue;
                }

                if (migratedContractFlg) {
                    principalInstallment += getDoubleParse(ebffHistory.getPrincipalAmt().get(k).getValue());
                    interestInstallment += getDoubleParse(ebffHistory.getInterestAmt().get(k).getValue());
                } else {
                    principalInstallment += getDoubleParse(ebffCollection.getPrincipalAmt().get(k).getValue());
                    interestInstallment += getDoubleParse(ebffCollection.getInterestAmt().get(k).getValue());
                }

                if (latestDueDate == null || dueDate.compareTo(latestDueDate) > 0) {
                    latestDueDate = dueDate;
                }
            }

            currentInstallmentPrincipal = String.format("%.2f", principalInstallment);
            currentInstallmentInterest = String.format("%.2f", interestInstallment);

        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        return latestDueDate;
    }

    private void getCurrentMonthODInstallment(String arrId) {

        try {

            double odPrincipal = 0.0;
            double odInterest = 0.0;
            double odInstallment = 0.0;

            // String firstDayOfMonth = todayDate.substring(0, 6) + "01";

            AaAccountDetailsRecord aaAccDetails = new AaAccountDetailsRecord(
                    da.getRecord(finMnemonic, AA_ACCOUNT_DETAILS, "", arrId));

            for (BillPayDateClass billPayDt : aaAccDetails.getBillPayDate()) {

                for (BillIdClass billId : billPayDt.getBillId()) {

                    String billDate = billId.getBillDate().getValue();

                    if (billDate == null || billDate.isEmpty()) {
                        continue;
                    }

//                    if (billDate.compareTo(firstDayOfMonth) < 0 || billDate.compareTo(todayDate) >= 0) {
//                        continue;
//                    }
                    if (!getCurrentWorkDate(billDate)) {
                        continue;
                    }

                    if ("INSTALLMENT".equalsIgnoreCase(billId.getBillType().getValue())
                            && "DUE".equalsIgnoreCase(billId.getPayMethod().getValue())
                            && "AGING".equalsIgnoreCase(billId.getBillStatus().getValue())) {

                        String billIdValue = billId.getBillId().getValue();

                        AaBillDetailsRecord aaBill = new AaBillDetailsRecord(
                                da.getRecord(finMnemonic, AA_BILL_DETAILS, "", billIdValue));

                        // Current month OD Installment
                        odInstallment += getDoubleParse(aaBill.getOsTotalAmount().getValue());

                        // Principal & Interest
                        for (PropertyClass property : aaBill.getProperty()) {

                            String prop = property.getProperty().getValue();

                            if ("ACCOUNT".equalsIgnoreCase(prop)) {
                                odPrincipal += getDoubleParse(property.getOsPropAmount().getValue());
                            }

                            if ("PRINTEREST".equalsIgnoreCase(prop)) {
                                odInterest += getDoubleParse(property.getOsPropAmount().getValue());
                            }
                        }
                    }
                }
            }

            currentMonthODInstallment = String.format("%.2f", odInstallment);
            currentMonthODPrincipal = String.format("%.2f", odPrincipal);
            currentMonthODInterest = String.format("%.2f", odInterest);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getInterestCapitalized(String arrId) {
        try {
            double interestCapital = 0.0;
            // String firstDayOfMonth = todayDate.substring(0, 6) + "01";

            AaAccountDetailsRecord aaAccDetails = new AaAccountDetailsRecord(
                    da.getRecord(finMnemonic, AA_ACCOUNT_DETAILS, "", arrId));

            for (BillPayDateClass billPayDtList : aaAccDetails.getBillPayDate()) {
                for (BillIdClass billIdList : billPayDtList.getBillId()) {

                    String billDate = billIdList.getBillDate().getValue();

                    if (billDate == null || billDate.isEmpty()) {
                        continue;
                    }

//                    if (billDate.compareTo(firstDayOfMonth) < 0 || billDate.compareTo(todayDate) >= 0) {
//                        continue;
//                    }
                    if (!getCurrentWorkDate(billDate)) {
                        continue;
                    }

                    if (billIdList.getBillType().getValue().equals("INSTALLMENT")

                            && billIdList.getPayMethod().getValue().equals("CAPITALISE")
                            && billIdList.getBillStatus().getValue().equals("CAPITALISE")) {
                        String overDueBillId = billIdList.getBillId().getValue();

                        AaBillDetailsRecord aaBillDetRec = new AaBillDetailsRecord(
                                da.getRecord(finMnemonic, AA_BILL_DETAILS, "", overDueBillId));

                        for (PropertyClass propertyList : aaBillDetRec.getProperty()) {

                            String property = propertyList.getProperty().getValue();

                            if (property.equalsIgnoreCase("ADVPAYREFUND")) {
                                String orPropAmt = propertyList.getOrPropAmount().getValue();
                                interestCapital += getDoubleParse(orPropAmt);
                            }

                        }
                    }
                }
            }
            interestCapitalized = String.format("%.2f", interestCapital);

        } catch (NumberFormatException e) {
            e.getMessage();
        }
    }

    private void getCurrentMonthCollected(String arrId) {
        try {
            double repayAmt = 0.0;
            String repayDate = "";
            double intRepayAmt = 0.0;
            // String firstDayOfMonth = todayDate.substring(0, 6) + "01";

            AaAccountDetailsRecord aaAccDetails = new AaAccountDetailsRecord(
                    da.getRecord(finMnemonic, AA_ACCOUNT_DETAILS, "", arrId));

            for (BillPayDateClass billPayDtList : aaAccDetails.getBillPayDate()) {
                for (BillIdClass billIdList : billPayDtList.getBillId()) {

                    String billDate = billIdList.getBillDate().getValue();

                    if (billDate == null || billDate.isEmpty()) {
                        continue;
                    }

//                    if (billDate.compareTo(firstDayOfMonth) < 0 || billDate.compareTo(todayDate) >= 0) {
//                        continue;
//                    }

                    if (!getCurrentWorkDate(billDate)) {
                        continue;
                    }

                    if (billIdList.getBillType().getValue().equals("INSTALLMENT")
                            && billIdList.getPayMethod().getValue().equals("DUE")
                            && billIdList.getBillStatus().getValue().contains("AGING")
                            && (billIdList.getSetStatus().getValue().equals("UNPAID")
                                    || billIdList.getSetStatus().getValue().equals("SETTLED")))

                    {
                        String overDueBillId = billIdList.getBillId().getValue();

                        AaBillDetailsRecord aaBillDetRec = new AaBillDetailsRecord(
                                da.getRecord(finMnemonic, AA_BILL_DETAILS, "", overDueBillId));

                        for (PropertyClass propertyList : aaBillDetRec.getProperty()) {

                            String property = propertyList.getProperty().getValue();

                            if (property.equalsIgnoreCase("ACCOUNT")) {
                                for (RepayRefClass refClass : propertyList.getRepayRef()) {
                                    String repayRef = refClass.getRepayRef().getValue();
                                    if (repayRef == null || repayRef.isEmpty()) {
                                        continue;
                                    }
                                    repayDate = repayRef.substring(repayRef.lastIndexOf("-") + 1);
                                    if (repayDate.compareTo(billDate) > 0 && getCurrentWorkDate(repayDate)) {
                                        repayAmt += getDoubleParse(refClass.getRepayAmount().getValue());

                                    }

                                }

                            }
                            if (property.equalsIgnoreCase("PRINTEREST")) {
                                for (RepayRefClass refClass : propertyList.getRepayRef()) {
                                    String repayRef = refClass.getRepayRef().getValue();
                                    if (repayRef == null || repayRef.isEmpty()) {
                                        continue;
                                    }
                                    repayDate = repayRef.substring(repayRef.lastIndexOf("-") + 1);
                                    if (repayDate.compareTo(billDate) > 0 && getCurrentWorkDate(repayDate)) {
                                        intRepayAmt += getDoubleParse(refClass.getRepayAmount().getValue());

                                    }

                                }

                            }

                        }
                    }
                }
            }
            currentOdPrinCollection = String.format("%.2f", repayAmt);
            currentOdIntCollection = String.format("%.2f", intRepayAmt);
            currentOdInstalCollection = String.format("%.2f", repayAmt + intRepayAmt);

        } catch (NumberFormatException e) {
            e.getMessage();
        }

    }

    public void writeToFile(List<String> data, String filePath) {
        try {
            File file = new File(filePath);

            file.getParentFile().mkdirs();
            boolean fileExists = file.exists();
            try (FileWriter writer = new FileWriter(file, true)) {

                if (!fileExists) {

                    String header = String.join(",", "Month-Year", "Account Number", "Legacy Account Number",
                            "Customer Number", "Principal Outstanding", "DPD", "Officer Name", "Officer Code",
                            "Group Code", "Center Code", "Office", "Office Name", "Product", "District", "State",
                            "Funding Source", "Funding Source Type", "Account Status", "Disbursement Date",
                            "Cycle Number", "Loan Amount", "Loan Purpose", "Sub-Purpose", "Overdue Principal Demand",
                            "Overdue Interest Demand", "Overdue Installment", "current installment demanDate",
                            "Current Principal Installment", "Current Interest Installment",
                            "Overdue Installment Principal Collection", "Overdue Installment Interest Collection",
                            "Current Installment Principal Collection", "Current Installment Interest Collection",
                            "Principal Prepayment", "Interest Prepayment", "Current Month OD Instal Principal",
                            "Current Month OD Instal Interest", "Current Month OD Instalment",
                            "Current Month OD Instal Principal Collected", "Current Month OD Instal Interest Collected",
                            "Current Month OD Instalment Collected", "Interest Capitalized", "Current Date Installment",
                            "Current Date Installment Principal", "Current Date Installment Interest");

                    writer.write(header + System.lineSeparator());
                }

                for (String line : data) {
                    writer.write(line + System.lineSeparator());
                }
            }

        } catch (Exception e) {
            e.getMessage();
        }
    }

}
