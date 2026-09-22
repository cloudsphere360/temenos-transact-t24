package com.temenos.fusion;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDate;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.temenos.api.TField;
import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.aa.contractapi.BalanceMovement;
import com.temenos.t24.api.complex.eb.servicehook.ServiceData;
import com.temenos.t24.api.hook.system.ServiceLifecycle;
import com.temenos.t24.api.records.aaactivitybalances.AaActivityBalancesRecord;
import com.temenos.t24.api.records.aaactivitybalances.PropertyClass;
import com.temenos.t24.api.records.aaactivityhistory.AaActivityHistoryRecord;
import com.temenos.t24.api.records.aaactivityhistory.ActivityRefClass;
import com.temenos.t24.api.records.aaactivityhistory.EffectiveDateClass;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaarrangement.LinkedApplClass;
import com.temenos.t24.api.records.aaarrangementactivity.AaArrangementActivityRecord;
import com.temenos.t24.api.records.aaprddesaccount.AaPrdDesAccountRecord;
import com.temenos.t24.api.records.aaprddesaccount.AltIdTypeClass;

import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.records.categentry.CategEntryRecord;
import com.temenos.t24.api.records.company.CompanyRecord;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.records.ebcontractbalances.CategProcessDateClass;
import com.temenos.t24.api.records.ebcontractbalances.EbContractBalancesRecord;
import com.temenos.t24.api.records.ebffcentredetail.EbFfCentreDetailRecord;
import com.temenos.t24.api.records.ebffloandetails.EbFfLoanDetailsRecord;
import com.temenos.t24.api.records.ebffloandetails.FmEntityNumberClass;
import com.temenos.t24.api.records.ebffloanpaymenthis.DemandDateClass;

import com.temenos.t24.api.records.ebffloanpaymenthis.EbFfLoanPaymentHisRecord;
import com.temenos.t24.api.records.ebffnpawriteoffmig.EbFfNpaWriteoffMigRecord;
import com.temenos.t24.api.records.ebffnpawriteoffmig.RecoveryDateClass;
import com.temenos.t24.api.records.ebffparameter.EbFfParameterRecord;
import com.temenos.t24.api.records.ebffparameter.ParamDescClass;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Date;
import com.temenos.t24.api.system.Session;

/*-----------------------------------------------------------------------------
 * @author Harshini Sakthivel
 * Date Created: 21-Nov-2025
 * Attached as : Post Routine
 * EB.API : FF.B.LOAN.WRT.OFF.RECOV.REPT.SELECT
 * EB.API :FF.B.LOAN.WRT.OFF.RECOV.REPT
 * Attached to BATCH :BNK/FF.B.LOAN.WRT.OFF.RECOV.REPT
 * Description: Generation of Report file through COB process 
 *------------------------------------------------------------------------------ 
 * Modification History : Initial Draft
 *----------------------------------------------------------------------------- 
 *21-Nov-2025   Development      Initial Version
 *-----------------------------------------------------------------------------
 *13.04.2026    final remapping   Balaji JV
 *13.04.2026    SonarQube Clear   Balaji JV
 *13.04.2026    For all branch    Balaji JV
 */
public class FfBLoanWriteOffRecReport extends ServiceLifecycle {

    private static final String ACCOUNT = "ACCOUNT";

    public static final String FILE_NAME = "LoanWrite-off&Write-offRecoveryReport";
    public static final String TRADE = "TRADE";
    public static final String CHARGEACC = "LENDING-CHARGEOFF-ACCOUNT";
    public static final String CHARGEARR = "LENDING-CHARGEOFF-ARRANGEMENT";
    private static final DateTimeFormatter INPUT_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private static final DateTimeFormatter OUTPUT_DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    DataAccess da = new DataAccess(this);
    Session ss = new Session(this);
    Date dd = new Date(this);
    String todayDate = ss.getCurrentVariable("!TODAY");

    String finMnemonic = "";
    List<String> arrtActivityList = new ArrayList<>();
    AaArrangementActivityRecord aArrActivityRec = null;
    String aaId = "";
    AaArrangementRecord aaRec = null;
    String coCode = "";
    CompanyRecord companyRec = null;
    String companyName = "";
    String cusId = "";
    CustomerRecord cusRec = null;
    List<LinkedApplClass> linkedAppList = null;
    String accNum = "";
    String accArrId = "";
    String givenName = "";
    String familyName = "";
    String cusName = "";
    String centerName = "";
    String legalName = "";
    String ebCusDetId = "";
    List<FmEntityNumberClass> fMEntityNumlist = null;
    String isGuarantor = "";
    String prepaymentofPrincipal = "";
    String loanPurp = "";
    String fundSrc = "";
    AaActivityHistoryRecord aaActHisRec = null;
    String initial = "";
    String contractId = "";
    String curActivity = "";
    String finMnmc = "";
    String writeoffAmt = "0.00";
    String creditValueDate = "";
    List<String> finalReport = new ArrayList<>();
    String principalOutstandng = "";

    EbFfParameterRecord paramRec = null;

    List<ParamDescClass> paramDescList = null;
    List<DemandDateClass> demdAmtList = null;

    List<RecoveryDateClass> recoveryDate = null;
    String recvAmount = null;
    String paraDesc = "";
    String paraDescName = "";
    String paramPath = "";
    String mnemonic = "";
    String nabacc = "";
    String orgdate = "";
    List<String> arrList = null;
    List<EffectiveDateClass> effectiveDateList;
    List<ActivityRefClass> activityRefList;

    String guarantorName = "";
    AaPrdDesAccountRecord aaAcc = null;
    LocalDate latestLoanPaymentDate = null;
    LocalDate latestCreditValueDate = null;
    String centerCode = "";
    String writtenoffdate = "";
    String branchName = "";
    String paymentDate = "";
    String principalAmountReceived = "";
    String interestAmountReceived = "";
    String totalAmountReceived = "";
    String overdueprincipal = "";
    String reason = "";
    String toRoundOff = "";
    String activityId = "";
    String cusNo = "";
    String loanid = "";
    String legacyLoanNumber = "";
    String totalPaymentRecived = "";
    String curAccountCode = "";
    String curMntPrinAmt = "";
    String curMntIntAmt = "";
    String curTotalAmt = "";

    String orgConcDate = "";
    List<CategProcessDateClass> catgList = null;
    double actAmt = 0.0;
    double principalTotal = 0.0;
    double currentprincipalTotal = 0.0;
    double currentinterestTotal = 0.0;
    double interestTotal = 0.0;
    double principalFromCategEntry = 0.0;
    double curPrincipalCategEntry = 0.0;
    double curIntrestCategEntry = 0.0;
    double intrestFromCategEntry = 0.0;
    boolean paramflag = false;
    boolean isMigratedLoan = false;
    LocalDate latestPaymentDate = null;

    @Override
    public List<String> getIds(ServiceData serviceData, List<String> controlList) {

        try {
            initialiseCompanyInfo(serviceData);

            arrList = da.selectRecords(finMnemonic, "AA.ARRANGEMENT", "", "");

            for (String aarid : arrList) {

                getFinalChargeOffAccList(aarid);
            }

        } catch (Exception e) {

            e.getMessage();
        }
        return arrtActivityList;
    }

    private void getFinalChargeOffAccList(String aarid) {
        try {
            aaActHisRec = new AaActivityHistoryRecord(da.getRecord(finMnemonic, "AA.ACTIVITY.HISTORY", "", aarid));

            boolean found = false;
            effectiveDateList = aaActHisRec.getEffectiveDate();
            for (EffectiveDateClass effectiveDate : effectiveDateList) {
                activityRefList = effectiveDate.getActivityRef();

                for (ActivityRefClass activityRef : activityRefList) {
                    String activity = activityRef.getActivity().getValue();
                    String status = activityRef.getActStatus().getValue();

                    if ("AUTH".equalsIgnoreCase(status) && (activity.equals(CHARGEACC) || activity.equals(CHARGEARR))) {

                        arrtActivityList.add(aarid);

                        found = true;
                        break;
                    }
                }
                if (found) {
                    break;
                }
            }
        } catch (Exception e) {

            e.getMessage();

        }
    }

    // get fin and cus mnemonic
    private void initialiseCompanyInfo(ServiceData serviceData) {

        try {
            String companyId = serviceData.getCompanyId();
            CompanyRecord companyObj = new CompanyRecord(da.getRecord("COMPANY", companyId));
            finMnemonic = companyObj.getFinancialMne().getValue();
            mnemonic = companyObj.getCustomerMnemonic().getValue();
            getEbParameterRecordDetails();

        } catch (Exception e) {
            e.getMessage();
        }
    }

    @Override
    public void process(String id, ServiceData serviceData, String controlItem) {

        List<String> outvalues = new ArrayList<>();
        String acctNo = "";

        principalTotal = 0.0;
        currentprincipalTotal = 0.0;
        currentinterestTotal = 0.0;
        interestTotal = 0.0;
        principalFromCategEntry = 0.0;
        intrestFromCategEntry = 0.0;
        curPrincipalCategEntry = 0.0;
        curIntrestCategEntry = 0.0;

        latestLoanPaymentDate = null;
        latestCreditValueDate = null;

        latestPaymentDate = null;
        paymentDate = "";
        principalAmountReceived = "";
        interestAmountReceived = "";
        totalPaymentRecived = "";
        curTotalAmt = "";
        curMntIntAmt = "";
        curMntPrinAmt = "";
        writeoffAmt = "0.00";
        orgdate = "";

        try {
            initialiseCompanyInfo(serviceData);
            Contract contract = new Contract(this);

            aaId = id;

            contract.setContractId(aaId);

            aaRec = new AaArrangementRecord(da.getRecord(finMnemonic, "AA.ARRANGEMENT", "", aaId));
            orgConcDate = aaRec.getOrigContractDate().getValue();
            if (orgConcDate != null || !orgConcDate.isEmpty()) {
                isMigratedLoan = true;

            }

            coCode = aaRec.getCoCodeRec().getValue();

            linkedAppList = aaRec.getLinkedAppl();

            for (LinkedApplClass linkedApp : linkedAppList) {

                if (linkedApp.getLinkedAppl().getValue().equals(ACCOUNT)) {
                    acctNo = linkedApp.getLinkedApplId().getValue();
                    getAccountDetails(acctNo);
                }
            }

            companyRec = new CompanyRecord(da.getRecord("COMPANY", coCode));

            String comNameSub = companyRec.getCompanyName(0).getValue();
            if (comNameSub != null && comNameSub.length() > 4) {
                branchName = comNameSub.substring(0, comNameSub.length() - 4);
            }

            getAaActHisDate(aaId);
            getCustomerDetails(aaRec);
            getprincipalOutstandng(contract, aaRec);
            getEbLoanDetails(aaId, finMnemonic);
            getAaArrAccount(contract);

            if (prepaymentofPrincipal == null || prepaymentofPrincipal.trim().isEmpty()) {
                if (curAccountCode != null && !curAccountCode.trim().isEmpty()) {

                    prepaymentofPrincipal = curAccountCode;

                } else {
                    prepaymentofPrincipal = "0.00";

                }
            }

            List<String> row = new ArrayList<>();

            row.add(branchName);
            row.add(coCode);
            row.add(aaId);
            row.add(legacyLoanNumber);
            row.add(cusNo);
            row.add(cusName);
            row.add(centerName);
            row.add(centerCode);
            row.add(guarantorName);
            row.add(writtenoffdate);
            row.add(reason);
            row.add(overdueprincipal);
            row.add(prepaymentofPrincipal);
            row.add(principalOutstandng);
            row.add(writeoffAmt);
            row.add(loanPurp);
            row.add(fundSrc);
            row.add(paymentDate);
            row.add(checkAmount(principalAmountReceived));
            row.add(checkAmount(interestAmountReceived));
            row.add(totalPaymentRecived);
            row.add(curMntPrinAmt);
            row.add(curMntIntAmt);
            row.add(curTotalAmt);

            String rowData = String.join(",", row);
            outvalues.add(rowData);

            String outputPath = paramPath + FILE_NAME + "_" + finMnemonic + "_" + todayDate + "_" + "temp" + "_"
                    + ss.getSessionNumber() + ".csv";

            writeToFile(outvalues, outputPath);

        } catch (Exception e) {

            e.getMessage();
        }

    }

    public static String checkAmount(String amount) {
        if (amount == null || amount.trim().isEmpty()) {
            return "0.00";
        }
        return amount.trim();
    }

//  check activity history table
    private void getAaActHisDate(String aaId) {

        try {

            DateTimeFormatter inputFormatter = INPUT_DATE;
            DateTimeFormatter outputFormatter = OUTPUT_DATE;

            aaActHisRec = new AaActivityHistoryRecord(da.getRecord(finMnemonic, "AA.ACTIVITY.HISTORY", "", aaId));

            for (EffectiveDateClass effectiveDate : aaActHisRec.getEffectiveDate()) {
                for (ActivityRefClass activeRef : effectiveDate.getActivityRef()) {
                    if ("AUTH".equalsIgnoreCase(activeRef.getActStatus().getValue())) {
                        curActivity = activeRef.getActivity().getValue();
                        if (CHARGEARR.equals(curActivity) || CHARGEACC.equals(curActivity)) {

                            String activityRef = activeRef.getActivityRef().getValue();
                            getMigratedData(aaId, inputFormatter, outputFormatter);
                            getActivityBalance(aaId);
                            getAaArrangementActivityDets(inputFormatter, outputFormatter, activityRef);

                            return;
                        }

                    }
                }
            }

        } catch (Exception e) {

            e.getMessage();
        }
    }

    public void getMigratedData(String aaId, DateTimeFormatter inputFormatter, DateTimeFormatter outputFormatter) {
        if (isMigratedLoan) {
            getEbFfNpaMig(inputFormatter, outputFormatter, aaId);
        }
    }

    private void getEbFfNpaMig(DateTimeFormatter inputFormatter, DateTimeFormatter outputFormatter, String aaId) {

        try {

            EbFfNpaWriteoffMigRecord npaRec = new EbFfNpaWriteoffMigRecord(
                    da.getRecord(mnemonic, "EB.FF.NPA.WRITEOFF.MIG", "", aaId));

            orgdate = npaRec.getOrigWriteoffDt().getValue();

            String origWrtAmt = npaRec.getOrigWriteoffAmt().getValue();

            recoveryDate = npaRec.getRecoveryDate();
            recvAmount = npaRec.getWriteoffRecovAmt().getValue();

            for (RecoveryDateClass rcDat : recoveryDate) {
                String recvDte = rcDat.getRecoveryDate().getValue();

                LocalDate dates = LocalDate.parse(recvDte, inputFormatter);
                if (latestPaymentDate == null || dates.isAfter(latestPaymentDate)) {
                    latestPaymentDate = dates;
                }
            }

            if (origWrtAmt != null && !origWrtAmt.trim().isEmpty()) {
                prepaymentofPrincipal = "0.00";
            } else {
                prepaymentofPrincipal = curAccountCode;
            }
            if (orgdate != null && !orgdate.isEmpty()) {
                // new changes
                double orgWrtAmt = (origWrtAmt != null && !origWrtAmt.isEmpty()) ? Double.parseDouble(origWrtAmt) : 0.0;
                writeoffAmt = String.format("%.2f", orgWrtAmt);

                LocalDate date = LocalDate.parse(orgdate, inputFormatter);
                writtenoffdate = date.format(outputFormatter);

                LocalDate writeOff = LocalDate.parse(writtenoffdate, outputFormatter);
                getLoanpayHis(outputFormatter, writeOff);
            }

        } catch (Exception e) {

            e.getMessage();
        }

    }

    private void getLoanpayHis(DateTimeFormatter outputFormatter, LocalDate writtenoffdate2) {
        EbFfLoanPaymentHisRecord payHis = new EbFfLoanPaymentHisRecord(
                da.getRecord(mnemonic, "EB.FF.LOAN.PAYMENT.HIS", "", aaId));

        demdAmtList = payHis.getDemandDate();

        for (DemandDateClass demDate : demdAmtList) {

            String payDate = demDate.getPymtDate().getValue();
            String transType = demDate.getTransType().getValue();
            String payAmt = demDate.getPymtAmt().getValue();

            LocalDate currentPaymentDate = LocalDate.parse(payDate, outputFormatter);

            getLatestPaymDate(writtenoffdate2, transType, payAmt, currentPaymentDate);

        }

    }

    public void getLatestPaymDate(LocalDate writtenoffdate2, String transType, String payAmt,
            LocalDate currentPaymentDate) {

        if (currentPaymentDate.isAfter(writtenoffdate2)) {

            if (latestPaymentDate == null || currentPaymentDate.isAfter(latestPaymentDate)) {
                latestPaymentDate = currentPaymentDate;
            }
            double payAmount = (payAmt != null && !payAmt.isEmpty()) ? Double.parseDouble(payAmt) : 0.0;
            // newchanges23july
            getCurntMonthValues(transType, currentPaymentDate, payAmount);
            // oldcode from here
            if ("PRINCIPAL".equalsIgnoreCase(transType)) {
                principalTotal += payAmount;

            }
            if ("INTEREST".equalsIgnoreCase(transType) || "Overdue Interest".equalsIgnoreCase(transType)) {
                interestTotal += payAmount;

            }
        }
    }

    public void getCurntMonthValues(String transType, LocalDate currentPaymentDate, double payAmount) {
        String lastWorkingDay = dd.getDates().getLastWorkingDay().getValue();

        LocalDate reportDate = LocalDate.parse(lastWorkingDay, INPUT_DATE);

        LocalDate reportPeriodStart = reportDate.withDayOfMonth(1);

        boolean isCurrentPeriod = !currentPaymentDate.isBefore(reportPeriodStart)
                && !currentPaymentDate.isAfter(reportDate);

        if (isCurrentPeriod) {

            if ("PRINCIPAL".equalsIgnoreCase(transType)) {
                currentprincipalTotal += payAmount;
            }
            if ("INTEREST".equalsIgnoreCase(transType) || "Overdue Interest".equalsIgnoreCase(transType)) {
                currentinterestTotal += payAmount;

            }

        }
    }

// writeofffate, reason
    private void getAaArrangementActivityDets(DateTimeFormatter inputFormatter, DateTimeFormatter outputFormatter,
            String activityRef) {

        try {
            String writtendate = "";
            aArrActivityRec = new AaArrangementActivityRecord(
                    da.getRecord(finMnemonic, "AA.ARRANGEMENT.ACTIVITY", "", activityRef));

            writtendate = aArrActivityRec.getEffectiveDate().getValue();

            String narrative = "";
            if (aArrActivityRec.getNarrative() != null && !aArrActivityRec.getNarrative().isEmpty()) {
                narrative = aArrActivityRec.getNarrative().get(0).getValue();
                reason = narrative;

            }
            if ((writtenoffdate == null || writtenoffdate.isEmpty()) && writtendate != null && !writtendate.isEmpty()) {
                LocalDate date = LocalDate.parse(writtendate, inputFormatter);
                writtenoffdate = date.format(outputFormatter);

            }
        } catch (Exception e) {

            e.getMessage();

        }

    }

// file path
    private void getEbParameterRecordDetails() {

        try {
            if (!paramflag) {
                paramRec = new EbFfParameterRecord(da.getRecord("EB.FF.PARAMETER", "FF.COB.REPORT.EXTRACT"));

                if (!paramRec.getParamDesc().isEmpty()) {
                    paramDescList = paramRec.getParamDesc();
                    for (ParamDescClass paramDesc : paramDescList) {
                        paraDesc = paramDesc.getParamDesc().getValue();
                        if (paraDesc.equals("Custom Path for COB Reports")) {
                            paramPath = paramDesc.getParamValue().getValue();

                        }
                    }
                }
                paramflag = true;
            }
        } catch (Exception e) {

            e.getMessage();
        }

    }

// type of balance, categ entry - principal, interest amt
    private void getprincipalOutstandng(Contract contract, AaArrangementRecord aaRecord) {

        try {

            String dueAccount = getBalance(contract, "FFOVRDUEPRIAMT", TRADE);

            String curAccountCo = getBalance(contract, "CURACCOUNTCUST", TRADE);

            String prcpOutsta = getBalance(contract, "FFPRIOUTAMT", TRADE);

            double curAccount = Math.abs(Double.valueOf(curAccountCo));

            curAccountCode = String.format("%.2f", curAccount);

            String overdueprincipalmins = dueAccount.replace("-", "");

            String prpBalanc = prcpOutsta.replace("-", "");

            overdueprincipal = String.format("%.2f", Double.valueOf(overdueprincipalmins));

            principalOutstandng = String.format("%.2f", Double.valueOf(prpBalanc));

            accNum = aaRecord.getLinkedAppl().get(0).getLinkedApplId().getValue();

            EbContractBalancesRecord ebCon = new EbContractBalancesRecord(da.getRecord("EB.CONTRACT.BALANCES", accNum));

            List<TField> categEtId = ebCon.getCategEntIds();

            for (TField entryIds : categEtId) {

                String categEntId = entryIds.getValue().split("/")[0];

                CategEntryRecord cerec = new CategEntryRecord(da.getRecord("CATEG.ENTRY", categEntId));

                String trnsCode = cerec.getTransactionCode().getValue();

                String amountLcy = cerec.getAmountLcy().getValue();

                String plCteg = cerec.getPlCategory().getValue();

                getPrinInterEntry(cerec, trnsCode, amountLcy, plCteg);
            }

            if (latestPaymentDate != null) {
                paymentDate = latestPaymentDate.format(OUTPUT_DATE);
            }
            double recvAmt = (recvAmount != null && !recvAmount.isEmpty()) ? Double.parseDouble(recvAmount) : 0.0;
            double finalPrincipal = principalTotal + principalFromCategEntry + recvAmt;

            double finalInterest = interestTotal + intrestFromCategEntry;

            double finalCurPrincipal = curPrincipalCategEntry + currentprincipalTotal;

            double finalCurInterest = curIntrestCategEntry + currentinterestTotal;

            curTotalAmt = String.format("%.2f", finalCurPrincipal + finalCurInterest);

            curMntIntAmt = String.format("%.2f", finalCurInterest);

            curMntPrinAmt = String.format("%.2f", finalCurPrincipal);

            totalPaymentRecived = String.format("%.2f", finalPrincipal + finalInterest);

            principalAmountReceived = String.format("%.2f", finalPrincipal);

            interestAmountReceived = String.format("%.2f", finalInterest);

        } catch (Exception e) {

            e.getMessage();
        }

    }

    public void getPrinInterEntry(CategEntryRecord cerec, String trnsCode, String amountLcy, String plCteg) {

        if (plCteg.equals("51008") &&

                (trnsCode.equals("860") || trnsCode.equals("827"))) {

            String valueDate = cerec.getValueDate().getValue();

            LocalDate currentDate = LocalDate.parse(valueDate, INPUT_DATE);

            if (latestPaymentDate == null || currentDate.isAfter(latestPaymentDate)) {
                latestPaymentDate = currentDate;

            }

            getCategEntryTransCode(trnsCode, amountLcy, currentDate);
        }
    }

    public void getCategEntryTransCode(String trnsCode, String amountLcy, LocalDate currentDate) {
        double curAmount = Double.parseDouble(amountLcy);

        String lastWorkingDay = dd.getDates().getLastWorkingDay().getValue();

        LocalDate catReportDate = LocalDate.parse(lastWorkingDay, INPUT_DATE);

        LocalDate reportPeriodStart = catReportDate.withDayOfMonth(1);

        // Check: 1st of month <= payment date <= last working day
        boolean isCurrentPeriod = !currentDate.isBefore(reportPeriodStart) && !currentDate.isAfter(catReportDate);

        if (trnsCode.equals("860")) {

            principalFromCategEntry += curAmount;

            if (isCurrentPeriod) {
                curPrincipalCategEntry += curAmount;

            }

        }
        if (trnsCode.equals("827")) {
            intrestFromCategEntry += curAmount;

            if (isCurrentPeriod) {
                curIntrestCategEntry += curAmount;

            }

        }
    }

    public void getActivityBalance(String aaId) {
        AaActivityBalancesRecord aaAcBln = new AaActivityBalancesRecord(
                da.getRecord(finMnemonic, "AA.ACTIVITY.BALANCES", "", aaId));
        List<com.temenos.t24.api.records.aaactivitybalances.ActivityRefClass> activity = aaAcBln.getActivityRef();
        if (orgdate == null || orgdate.isEmpty()) {
            for (com.temenos.t24.api.records.aaactivitybalances.ActivityRefClass actName : activity) {
                String activityName = actName.getActivity().getValue();
                if ((CHARGEACC.equals(activityName) || CHARGEARR.equals(activityName))) {
                    getActivityProperty(actName);
                }
            }
        }
    }

    public void getActivityProperty(com.temenos.t24.api.records.aaactivitybalances.ActivityRefClass actName) {
        List<PropertyClass> prop = actName.getProperty();
        for (PropertyClass property : prop) {
            String proprtyName = property.getProperty().getValue();
            String proprtyAmt = property.getPropertyAmt().getValue();

            if ("ACCOUNT.CURACCOUNTCO".equals(proprtyName)) {
                double curAccountAmt = Math.abs(Double.parseDouble(proprtyAmt));
                writeoffAmt = String.format("%.2f", curAccountAmt);
                break;

            }
        }
    }

// get balance from ECB
    private String getBalance(Contract contract, String accountType, String bookingType) {

        List<BalanceMovement> movements = null;

        try {
            movements = contract.getContractBalanceMovements(accountType, bookingType);

        } catch (Exception e) {

            e.getMessage();
        }
        return (movements != null && !movements.isEmpty()) ? movements.get(0).getBalance().toString() : "0";
    }

    private void getEbLoanDetails(String aaId, String finMnemonic) {

        try {
            EbFfLoanDetailsRecord ffLoanDetsRec = new EbFfLoanDetailsRecord(
                    da.getRecord(finMnemonic, "EB.FF.LOAN.DETAILS", "", aaId));

            for (FmEntityNumberClass fmEntityType : ffLoanDetsRec.getFmEntityNumber()) {
                if ("YES".equalsIgnoreCase((fmEntityType.getIsGuarantor().getValue()))) {
                    guarantorName = fmEntityType.getLegalName().getValue();

                }
            }

        } catch (Exception e) {

            e.getMessage();
        }

    }

// cus details 
    private void getCustomerDetails(AaArrangementRecord aaRec2) {

        String fstName = "";
        String scdName = "";
        try {
            cusId = aaRec2.getCustomer(0).getCustomer().getValue();
            String ordNum = aaRec2.getOrigContractDate().getValue();
            cusRec = new CustomerRecord(da.getRecord("CUSTOMER", cusId));
            String mne = cusRec.getMnemonic().getValue();
            if (ordNum != null && !ordNum.isEmpty()) {
                cusNo = mne;
            } else {
                cusNo = cusId;
            }

            for (TField name1 : cusRec.getName1()) {
                fstName = name1.getValue();
            }

            for (TField name2 : cusRec.getName2()) {
                scdName = name2.getValue();
            }

            familyName = cusRec.getFamilyName().getValue();
            cusName = getGivenName(fstName, scdName, familyName);

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

// Account property details
    private void getAaArrAccount(Contract contract) {

        try {
            List<String> aaArrAccountPrptyList = new ArrayList<>();
            aaArrAccountPrptyList.add(ACCOUNT);
            aaArrAccountPrptyList.add("LOANACCOUNT");
            for (String aaArrAcctid : aaArrAccountPrptyList) {

                aaAcc = new AaPrdDesAccountRecord(contract.getConditionForProperty(aaArrAcctid));

                for (AltIdTypeClass altType : aaAcc.getAltIdType()) {
                    if (altType.getAltIdType().getValue().equals("LEGACY")) {
                        legacyLoanNumber = altType.getAltId().getValue();

                    }
                }
            }
        } catch (Exception e) {

            e.getMessage();
        }

    }

    private void getAccountDetails(String accNum2) {

        try {
            AccountRecord acctRec = new AccountRecord(da.getRecord(finMnemonic, ACCOUNT, "", accNum2));
            centerCode = acctRec.getLocalRefField("FF.CENTRE").getValue();

            if (centerCode != null && !centerCode.isEmpty()) {
                getFfCentreDetail(centerCode);
            }

            loanPurp = acctRec.getLocalRefField("FF.LOAN.PURP").getValue();

            fundSrc = acctRec.getLocalRefField("FF.FUNDER.NAME").getValue();

        } catch (Exception e) {

            e.getMessage();
        }
    }

    private void getFfCentreDetail(String ffCentre2) {

        try {
            EbFfCentreDetailRecord centreDet = new EbFfCentreDetailRecord(
                    da.getRecord("EB.FF.CENTRE.DETAIL", ffCentre2));
            centerName = centreDet.getCenterName().getValue();

        } catch (Exception e) {

            e.getMessage();
        }

    }

// write to file
    private void writeToFile(List<String> outvalues, String outputPath) {

        try {
            File file = new File(outputPath);
            file.getParentFile().mkdirs();
            boolean fileExists = file.exists();

            try (FileWriter writer = new FileWriter(file, true)) {
                if (!fileExists) {

                    String header = String.join(",", "BranchName", "BranchCode", "LoanNumber", "LegacyLoanNumber",
                            "CustomerNumber", "CustomerName", "CenterName", "CenterCode", "GuarantorsName",
                            "Write-OffDate", "Reason", "OverduePrincipal", "PrepaymentOfPrincipal",
                            "PrincipalOutstanding", "WriteoffAmount", "Purpose", "FundingSource", "PaymentDate",
                            "PrincipalAmount", "InterestAmountReceived", "TotalPaymentReceived",
                            "CurrentMonthPrincipalAmountReceived", "CurrentMonthInterestAmountReceived",
                            "CurrentMonthTotalPaymentreceived");
                    writer.write(header + System.lineSeparator());
                }

                for (String line : outvalues) {
                    writer.write(line + System.lineSeparator());
                }
            }

        } catch (Exception e) {

            e.getMessage();
        }

    }

}