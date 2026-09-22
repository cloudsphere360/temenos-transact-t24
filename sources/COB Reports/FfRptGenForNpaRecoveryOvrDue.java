package com.temenos.fusion;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import java.util.Set;

import com.temenos.api.TField;

import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.aa.contractapi.BalanceMovement;
import com.temenos.t24.api.complex.eb.servicehook.ServiceData;
import com.temenos.t24.api.hook.system.ServiceLifecycle;
import com.temenos.t24.api.records.aaaccountdetails.AaAccountDetailsRecord;
import com.temenos.t24.api.records.aaaccountdetails.AgeAllBillTypeClass;
import com.temenos.t24.api.records.aaaccountdetails.AgeAllDateClass;
import com.temenos.t24.api.records.aaaccountdetails.BillIdClass;
import com.temenos.t24.api.records.aaaccountdetails.BillPayDateClass;
import com.temenos.t24.api.records.aaactivityhistory.AaActivityHistoryRecord;
import com.temenos.t24.api.records.aaactivityhistory.ActivityRefClass;
import com.temenos.t24.api.records.aaactivityhistory.EffectiveDateClass;

import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaarrangementactivity.AaArrangementActivityRecord;
import com.temenos.t24.api.records.aaarrtermamount.AaArrTermAmountRecord;


import com.temenos.t24.api.records.aaprddesaccount.AaPrdDesAccountRecord;
import com.temenos.t24.api.records.aaprddesaccount.AltIdTypeClass;
import com.temenos.t24.api.records.aaprddescharge.AaPrdDesChargeRecord;
import com.temenos.t24.api.records.aaprddesinterest.AaPrdDesInterestRecord;
import com.temenos.t24.api.records.aaprddespaymentschedule.AaPrdDesPaymentScheduleRecord;

import com.temenos.t24.api.records.aaoverduestats.AaOverdueStatsRecord;

import com.temenos.t24.api.records.aaprddestermamount.AaPrdDesTermAmountRecord;
import com.temenos.t24.api.records.aaproduct.AaProductRecord;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.records.company.CompanyRecord;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.records.customer.Phone1Class;

import com.temenos.t24.api.records.ebffcentredetail.EbFfCentreDetailRecord;
import com.temenos.t24.api.records.ebffcollectiondets.EbFfCollectionDetsRecord;
import com.temenos.t24.api.records.ebffcollectiondetshistory.EbFfCollectionDetsHistoryRecord;
import com.temenos.t24.api.records.ebffgroups.EbFfGroupsRecord;
import com.temenos.t24.api.records.ebffloandetails.AddressTypeClass;
import com.temenos.t24.api.records.ebffloandetails.EbFfLoanDetailsRecord;
import com.temenos.t24.api.records.ebffloandetails.FmEntityNumberClass;
import com.temenos.t24.api.records.ebffloandpd.EbFfLoanDpdRecord;
import com.temenos.t24.api.records.ebffloanpaymenthis.DemandDateClass;
import com.temenos.t24.api.records.ebffloanpaymenthis.EbFfLoanPaymentHisRecord;
import com.temenos.t24.api.records.ebffnpawriteoffmig.EbFfNpaWriteoffMigRecord;
import com.temenos.t24.api.records.ebffparameter.EbFfParameterRecord;
import com.temenos.t24.api.records.ebffparameter.ParamDescClass;
import com.temenos.t24.api.records.ebffrouser.EbFfRoUserRecord;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/*-----------------------------------------------------------------------------
 * @author Sathish Kumar K
 * Date Created:
 * Attached as : Service Routine
 * EB.API : FF.B.OVERDUE.NPA.REPORT
 * Attached to : BATCH > BNK/FF.DAILY.REPORT.EXTRACT
 * Description: COB Report generation -> NPA Recovery and OverDue Report
 *------------------------------------------------------------------------------ 
 * Modification History : NA
 *----------------------------------------------------------------------------- 
 *26-Nov-2025   Development      Initial Version
 *-----------------------------------------------------------------------------
 *27-Jan-2026   Defect           Meera/Yuvasri
 *------------------------------------------------------------------------------
 *05-Feb-2026   Multivalue       Sathish
 *
 *              Header Fix
 *              
 *27-Feb-2026   Re-mapping       Sri Rahul R              
 *------------------------------------------------------------------------------
 */
public class FfRptGenForNpaRecoveryOvrDue extends ServiceLifecycle {

    DataAccess da = new DataAccess(this);
    List<String> arrList = new ArrayList<>();
    List<String> finalArrList = new ArrayList<>();

    String arrId = "";
    String finMnemonic = "";
    String mnemonic = "";
    String todayDate = "";
    String accountNumber = "";
    String customerNumber = "";
    String telephone = "";
    String guarantorsName = "";
    String branchName = "";
    String branchCode = "";
    String officer = "";
    String officerEmployeeNumber = "";
    String center = "";
    String centerCode = "";
    String group = "";
    String dateOfBirth = "";
    String religiousGroup = "";
    String caste = "";
    String locale = "";
    String state = "";
    String product = "";
    String loanAmount = "";
    String loanCycle = "";
    String lastPaymentDate = "";
    String lastPaymentAmount = "";
    String repaymentFrequency = "";
    String maturityDate = "";
    String interestRate = "";
    String emi = "";
    String purposeCategory = "";
    String purpose = "";
    String fundSource = "";
    String principalOutstanding = "";
    String interestOutstanding = "";
    String feeAndInsuranceOutstanding = "";
    String disbursementDate = "";
    int installmentsDue = 0;
    String earliestUnpaidDemandDate = "";
    String latestUnpaidDemandDate = "";
    String principalDefault = "";
    String interestDefault = "";
    String minimumPrincipalOverdueDays = "";
    String maximumPrincipalOverdueDays = "";
    String deathCaseRemark = "";
    String deathFlaggedDate = "";
    String dpdClassification = "";
    String npaDate = "";
    String loanStartDate = "";
    String loanMaturityDate = "";
    String interestRateAsOnDateOfNpa = "";
    String accountStatus = "";
    String accountCloseFlag = "";
    String closingDate = "";
    String closureType = "";
    String outstandingAmountAsOnNpaDate = "";
    String finalRecoveryDets = "";

    String filePath = "";
    String officers = "";

    double sumofAllPrincipalOutstanding = 0.0;
    String overAllPrincipalOutstanding = "";

    double sumofAllInterestOutstanding = 0.0;
    String overAllInterestOutstanding = "";

    double sumofAllPrincipalDefault = 0.0;
    String overAllPrincipalDefault = "";

    double sumofAllInterestDefault = 0.0;
    String overAllInterestDefault = "";

    String legacyAccountNumber = "";
    String origContractDate = "";
    String aAcustomerNumber = "";

    String customerName = "";
    String cusFullname = "";
    String phone1 = "";
    String sms = "";
    String subpurpose = "";
    String ro = "";
    String altId = "";
    String companyName = "";
    String centre = "";
    String lastPaidDate = "";
    String cusNo = "";
    String familyName = "";
    String arrAgeStatus = "";
    String coCode = "";
    boolean legacy = false;
    String aaArrangement = "AA.ARRANGEMENT";
    String aaAccountDetails = "AA.ACCOUNT.DETAILS";
    String commitment = "COMMITMENT";
    String aaBillDetails = "AA.BILL.DETAILS";
    String trade = "TRADE";
    String ffCentre = "FF.CENTRE";
    String account = "ACCOUNT";
    String aaActivityHistory = "AA.ACTIVITY.HISTORY";
    String companyIdInfo = "";

    String acctFfCentre = "";
    String officerEmpNum = "";
    String centerName = "";
    String groupCode = "";
    String dpd = "";
    String currentRo = "";
    String groupName = "";
    String repayFrequencyMatExp = "";

    String totalDue = "";
    int weeks = 0;
    int days = 0;
    int months = 0;
    int years = 0;
    int fortnights = 0;

    AaOverdueStatsRecord overdueList = null;

    public static final String FILE_NAME = "OverdueReport";

    private static final String INSTALLMENT = "INSTALLMENT";
    private static final String MONTHLY = "MONTHLY";
    private static final String FORTNIGHTLY = "FORTNIGHTLY";
    private static final String ONCE_28_DAYS = "ONCE EVERY 28 DAYS";
    private static final String OTHERS = "OTHERS";

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

    DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    @Override
    public List<String> getIds(ServiceData serviceData, List<String> controlList) {

        try {
            Set<String> validStatuses = Set.of("SM0", "SM1", "SM2", "NPA");
            initialiseCompanyInfo(serviceData);
            arrList = da.selectRecords(finMnemonic, aaArrangement, "", "");
            for (String contractId : arrList) {
                AaAccountDetailsRecord aaAccountDets = new AaAccountDetailsRecord(
                        da.getRecord(finMnemonic, aaAccountDetails, "", contractId));
                if (validStatuses.contains(aaAccountDets.getArrAgeStatus().getValue())) {
                    finalArrList.add(contractId);
                }
            }
        } catch (Exception e) {
            e.getMessage();
        }
        return finalArrList;
    }

    @Override
    public void process(String id, ServiceData serviceData, String controlItem) {
        try {
            List<String> outvalues = new ArrayList<>();
            Contract contract = new Contract(this);
            Session session = new Session(this);
            todayDate = session.getCurrentVariable("!TODAY");

            arrId = id;
            contract.setContractId(arrId);
            initialiseCompanyInfo(serviceData);
            getDisbursementDateVal(arrId);

            getLegacyLoanNo(contract);

            getProductCompanyCoCode();

            getEmi(arrId);
            getEarlyLastDemandDt(arrId);

            getLoanAmount(contract);

            getInstallmentsDue();

            getDpdClassification();

            getAccountStatus(arrId);

            getCustomerDetails(contract);

            getEcbDetails(contract,arrId);
            getAaArrInterestDetails(contract);

            getAccountDetails(arrId);

            getNpaDate(arrId);
            getAaArrTermAmountDetails(contract);
            getAaArrChargeDetails(contract);
            getEbFfLoanDetails(arrId, finMnemonic);
            getDpd(arrId);
            getPaymentdetails(arrId, contract);

            List<String> row = new ArrayList<>();
            row.add(arrId);
            row.add(legacyAccountNumber);
            row.add(customerName);// customerName
            row.add(cusNo);// customerNumber
            row.add(telephone);
            row.add(guarantorsName);
            row.add(companyName);
            row.add(coCode);
            row.add(ro);// officer
            row.add(officerEmpNum);// officerEmployeeNumber
            row.add(centerName);
            row.add(acctFfCentre);
            row.add(groupName);
            row.add(formatDate(dateOfBirth));
            row.add(religiousGroup);
            row.add(caste);
            row.add(locale);
            row.add(state);
            row.add(product);
            row.add(loanAmount);
            row.add(loanCycle);
            row.add(formatDate(lastPaymentDate));
            row.add(lastPaymentAmount);
            row.add(repaymentFrequency);
            row.add(formatDate(maturityDate));
            row.add(interestRate);
            row.add(totalDue); // emi
            row.add(purpose);
            row.add(subpurpose);
            row.add(fundSource);
            row.add(principalOutstanding);
            row.add(interestOutstanding);

            row.add(formatDate(disbursementDate));
            row.add(String.valueOf(installmentsDue));
            row.add(formatDate(earliestUnpaidDemandDate));
            row.add(formatDate(latestUnpaidDemandDate));
            row.add(overAllPrincipalDefault);
            row.add(overAllInterestDefault);

            row.add(accountStatus);
            row.add(deathCaseRemark);
            row.add(formatDate(deathFlaggedDate));
            row.add(dpd);
            row.add(dpdClassification);
            row.add(formatDate(npaDate));

            outvalues.add(String.join(",", row));
            if (!outvalues.isEmpty()) {
                String paramId = "FF.COB.REPORT.EXTRACT";
                EbFfParameterRecord paramRec = new EbFfParameterRecord(da.getRecord("EB.FF.PARAMETER", paramId));
                for (ParamDescClass paramDesc : paramRec.getParamDesc()) {
                    if (paramDesc.getParamName().getValue().equals("Reports Temp Path")) {
                        filePath = paramDesc.getParamValue().getValue();
                    }
                }
                String outputPath = filePath + FILE_NAME + "_" + finMnemonic + "_" + todayDate + "_" + "temp" + "_"
                        + session.getSessionNumber() + ".csv";

                writeToFile(outvalues, outputPath);
            }
        } catch (Exception e) {
            e.getMessage();
        }

    }

    public void getDpd(String arrId) {

        LocalDate currDate = LocalDate.parse(todayDate, formatter);
        String formatted = currDate.getMonth().toString().substring(0, 3) + currDate.getYear();
        String dpdRecId = arrId + "-" + formatted;
        EbFfLoanDpdRecord loanDpdRec = null;

        try {

            loanDpdRec = new EbFfLoanDpdRecord(da.getRecord(finMnemonic, "EB.FF.LOAN.DPD", "", dpdRecId));
        } catch (Exception e) {

            e.getMessage();
        }
        if (loanDpdRec != null && !loanDpdRec.toString().isEmpty()) {

            int size = loanDpdRec.getDate().size();

            if (size > 0) {

                dpd = loanDpdRec.getDate().get(size - 1).getCurDpd().getValue();

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

                dpd = "";
            } else {

                dpd = "0";
            }

        }

    }

    private void getDisbursementDateVal(String arrId) {
        try {
            AaArrangementRecord arrRec = new AaArrangementRecord(da.getRecord(aaArrangement, arrId));

            origContractDate = arrRec.getOrigContractDate().getValue();

            if (origContractDate != null && !origContractDate.isEmpty()) {

                disbursementDate = origContractDate;

            } else {

                disbursementDate = arrRec.getStartDate().getValue();

            }

        } catch (Exception e) {

            e.getMessage();
        }
    }

    private void getLegacyLoanNo(Contract contract) {
        try {

            AaPrdDesAccountRecord aaArrAccRec = new AaPrdDesAccountRecord(contract.getConditionForProperty(account));

            if (origContractDate != null && !origContractDate.isEmpty()) {

                for (AltIdTypeClass altType : aaArrAccRec.getAltIdType()) {
                    if (altType.getAltIdType().getValue().equals("LEGACY")) {
                        legacyAccountNumber = altType.getAltId().getValue();

                    }
                }
            }

        } catch (Exception e) {

            e.getMessage();

        }
    }

    private void getDpdClassification() {
        try {

            AaAccountDetailsRecord aaAcctDets = new AaAccountDetailsRecord(da.getRecord(aaAccountDetails, arrId));
            arrAgeStatus = aaAcctDets.getArrAgeStatus().getValue();

            if (arrAgeStatus != null) {

                switch (arrAgeStatus.toUpperCase()) {

                case "CUR":
                    dpdClassification = "active";
                    break;

                case "SM0":
                    dpdClassification = "sma0";
                    break;

                case "SM1":
                    dpdClassification = "sma1";
                    break;

                case "SM2":
                    dpdClassification = "sma2";
                    break;

                case "NPA":
                    dpdClassification = "npa";
                    break;

                default:
                    dpdClassification = "";
                    break;
                }
            }

        }

        catch (Exception e) {

            e.getMessage();
        }
    }

    private void getInstallmentsDue() {

        try {

            int billcount = 0;

            AaAccountDetailsRecord accRec = new AaAccountDetailsRecord(da.getRecord(aaAccountDetails, arrId));

            for (BillPayDateClass billPayDate : accRec.getBillPayDate()) {

                for (BillIdClass billId : billPayDate.getBillId()) {

                    String billStatus = billId.getBillStatus().getValue();
                    String setStatus = billId.getSetStatus().getValue();

                    if ("AGING".equalsIgnoreCase(billStatus) && !"REPAID".equalsIgnoreCase(setStatus)
                            && !"SETTLED".equalsIgnoreCase(setStatus)) {

                        billcount++;
                    }
                }
            }

            installmentsDue = billcount;

        } catch (Exception e) {

            e.getMessage();
        }
    }

    private void getLoanAmount(Contract contract) {
        try {
            AaArrTermAmountRecord aaArrTermAmt = new AaArrTermAmountRecord(
                    contract.getConditionForProperty(commitment));

            loanAmount = aaArrTermAmt.getAmount().getValue();

        } catch (Exception e) {

            e.getMessage();
        }
    }

    private void getEarlyLastDemandDt(String arrId) {
        try {

            String overdueId = arrId + "-INSTALLMENT-DPD.STAGES";

            AaOverdueStatsRecord overdueObj = new AaOverdueStatsRecord(da.getRecord("AA.OVERDUE.STATS", overdueId));

            if (!overdueObj.toString().isEmpty()) {
                earliestUnpaidDemandDate = null;
                AaAccountDetailsRecord aaAcctDets = new AaAccountDetailsRecord(da.getRecord(aaAccountDetails, arrId));
                forLoopBillPay(aaAcctDets);
            }

        } catch (Exception e) {

            e.getMessage();
        }
    }

    private void forLoopBillPay(AaAccountDetailsRecord aaAcctDets) {
        for (BillPayDateClass billPayDate : aaAcctDets.getBillPayDate()) {

            for (BillIdClass billId : billPayDate.getBillId()) {

                if (billId.getBillType().getValue().equals(INSTALLMENT)
                        && billId.getBillStatus().getValue().equals("AGING")
                        && (!billId.getSetStatus().getValue().equals("REPAID")
                                || !billId.getSetStatus().getValue().equals("SETTLED"))) {

                    if (earliestUnpaidDemandDate == null) {
                        earliestUnpaidDemandDate = billId.getBillDate().getValue();
                    }
                    latestUnpaidDemandDate = billId.getBillDate().getValue();
                }
            }
        }
    }

    private void getProductCompanyCoCode() {
        CompanyRecord companyRec;
        try {
            AaArrangementRecord arrRec = new AaArrangementRecord(da.getRecord(aaArrangement, arrId));
            String productId = "";
            productId = arrRec.getProduct().get(0).getProduct().getValue();

            AaProductRecord aaProRec = new AaProductRecord(da.getRecord("AA.PRODUCT", productId));
            product = aaProRec.getDescription(0).getValue();

            coCode = arrRec.getCoCodeRec().getValue();

            companyRec = new CompanyRecord(da.getRecord("COMPANY", coCode));

            companyName = companyRec.getCompanyName().get(0).getValue().split("-")[0];

        } catch (Exception e) {

            e.getMessage();
        }
    }

    private void getEmi(String arrId) {

        try {

            AaArrangementRecord aaRec = new AaArrangementRecord(da.getRecord(finMnemonic, aaArrangement, "", arrId));

            String startDate = aaRec.getStartDate().getValue();
            String origContractDates = aaRec.getOrigContractDate().getValue();

            if (isOrigContractPresent(origContractDates)) {
                totalDue = calculateEmiFromHistory(arrId, startDate);
            } else {
                totalDue = getTotalDueFromCollection(arrId);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isOrigContractPresent(String origContractDate) {
        return origContractDate != null && !origContractDate.isEmpty();
    }

    private String calculateEmiFromHistory(String arrId, String startDate) {

        String historyId = arrId + "-" + startDate + ".01";
        String totalDueVal = "";
        try {

            EbFfCollectionDetsHistoryRecord histRec = new EbFfCollectionDetsHistoryRecord(
                    da.getRecord("EB.FF.COLLECTION.DETS.HISTORY", historyId));

            totalDueVal = histRec.getTotalDue(1).getValue();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return totalDueVal;
    }

    private String getTotalDueFromCollection(String arrId) {

        String totalDueVal = "";
        try {

            EbFfCollectionDetsRecord rec = new EbFfCollectionDetsRecord(da.getRecord("EB.FF.COLLECTION.DETS", arrId));

            totalDueVal = rec.getTotalDue().get(2).getValue(); // changes made

        } catch (Exception e) {
            e.printStackTrace();
        }

        return totalDueVal;
    }

    private void getCustomerDetails(Contract contract) {
        try {
            AaArrangementRecord arrRec = contract.getContract();
            aAcustomerNumber = arrRec.getCustomer().get(0).getCustomer().getValue();

            String ordNum = arrRec.getOrigContractDate().getValue();

            CustomerRecord cusRec = new CustomerRecord(da.getRecord("CUSTOMER", aAcustomerNumber));

            String fstName = "";
            String scdName = "";

            String mne = cusRec.getMnemonic().getValue();
            if (ordNum != null && !ordNum.isEmpty()) {
                cusNo = mne;
            } else {
                cusNo = aAcustomerNumber;
            }

            for (TField name1 : cusRec.getName1()) {
                fstName = name1.getValue();
            }

            for (TField name2 : cusRec.getName2()) {
                scdName = name2.getValue();
            }

            familyName = cusRec.getFamilyName().getValue();

            customerName = String.join(" ", fstName, scdName, familyName).trim().replaceAll("\\s+", " ");

            religiousGroup = cusRec.getLocalRefField("FF.RELIG.GROUP").getValue();
            caste = cusRec.getLocalRefField("FF.CASTE").getValue();
            deathCaseRemark = cusRec.getLocalRefField("FF.CUR.DOD.STS").getValue();
            deathFlaggedDate = cusRec.getDeathDate().getValue();

            dateOfBirth = cusRec.getDateOfBirth().getValue();

            for (Phone1Class phone : cusRec.getPhone1()) {
                phone1 = phone.getPhone1().getValue();
                sms = phone.getSms1().getValue();
            }

            if (sms != null && !sms.isEmpty()) {
                telephone = sms;
            } else if (phone1 != null && !phone1.isEmpty()) {
                telephone = phone1;
            } else if ((sms != null && !sms.isEmpty()) && phone1 != null && !phone1.isEmpty()) {
                telephone = sms;
            }

        } catch (Exception e) {
            e.getMessage();
        }
    }

    private void getAccountStatus(String arrId) {

        try {

            AaArrangementRecord aaRec = new AaArrangementRecord(da.getRecord(aaArrangement, arrId));

            String accountId = aaRec.getLinkedAppl(0).getLinkedApplId().getValue();
            String loanStatus = getLoanStatus(accountId);

            String arrStatus = aaRec.getArrStatus().getValue();

            if (isNotEmpty(loanStatus)) {
                accountStatus = loanStatus;
                return;
            }

            if (isClosedStatus(arrStatus)) {
                processClosedArrangement(arrId);
                return;
            }

            processActiveArrangement(arrId);

        } catch (Exception e) {
            e.getMessage();
        }
    }

    private String getLoanStatus(String accountId) {

        AccountRecord accountRec = new AccountRecord(da.getRecord(finMnemonic, account, "", accountId));

        return accountRec.getLocalRefField("FF.LOAN.STATUS").getValue();
    }

    private void processClosedArrangement(String arrId) {

        AaActivityHistoryRecord actHis = new AaActivityHistoryRecord(da.getRecord(aaActivityHistory, arrId));

        List<String> validActivities = Arrays.asList("LENDING-APPLYPAYMENT-WRITEOFF.SETTLEMENT",
                "LENDING-APPLYPAYMENT-INSURANCE.SETTLEMENT", "LENDING-SETTLE-FORECLOSURE", "LENDING-MATURE-ARRANGEMENT",
                "LENDING-WRITE.OFF-BAL.MAINTAIN");

        for (EffectiveDateClass effDate : actHis.getEffectiveDate()) {

            for (ActivityRefClass actRef : effDate.getActivityRef()) {

                if (!isValidActivity(actRef, validActivities)) {
                    continue;
                }

                handleActivity(actRef);
            }
        }
    }

    private boolean isValidActivity(ActivityRefClass actRef, List<String> validActivities) {

        String activity = actRef.getActivity().getValue();
        String actStatus = actRef.getActStatus().getValue();
        String initiation = actRef.getInitiation().getValue();

        return validActivities.contains(activity) && "Auth".equalsIgnoreCase(actStatus)
                && !"Secondary".equalsIgnoreCase(initiation);
    }

    private void handleActivity(ActivityRefClass actRef) {

        String activity = actRef.getActivity().getValue();

        if ("LENDING-WRITE.OFF-BAL.MAINTAIN".equalsIgnoreCase(activity)) {

            String aaaId = actRef.getActivityRef().getValue();

            AaArrangementActivityRecord aaaRec = new AaArrangementActivityRecord(
                    da.getRecord("AA.ARRANGEMENT.ACTIVITY", aaaId));

            accountStatus = aaaRec.getNarrative().get(0).getValue();
            return;
        }

        String contractId = actRef.getContractId().getValue();
        String ftId = contractId.split("\\\\")[0];

        if (isNotEmpty(ftId)) {

            FundsTransferRecord ftRec = new FundsTransferRecord(da.getRecord("FUNDS.TRANSFERT", ftId));

            accountStatus = ftRec.getLocalRefField("FF.COLL.TYPE").getValue();
        }
    }

    private void processActiveArrangement(String arrId) {

        AaAccountDetailsRecord accRec = new AaAccountDetailsRecord(da.getRecord(aaAccountDetails, arrId));

        ifArrAgeStmt(accRec);
    }

    private boolean isClosedStatus(String status) {
        return "CLOSED".equals(status) || "PENDING.CLOSURE".equals(status);
    }

    private boolean isNotEmpty(String value) {
        return value != null && !value.isEmpty();
    }

    /**
     * @param accRec
     */
    private void ifArrAgeStmt(AaAccountDetailsRecord accRec) {
        String arrAgeStatusVal = "";

        if (accRec.getArrAgeStatus() != null) {
            arrAgeStatusVal = accRec.getArrAgeStatus().getValue();

            if ((!arrAgeStatusVal.equals("SM0") && !arrAgeStatusVal.equals("SM1") && !arrAgeStatusVal.equals("SM2")
                    && !arrAgeStatusVal.equals("NPA"))) {

                accountStatus = "ACTIVE";
            }

            else if (arrAgeStatusVal.equals("SM0")) {
                accountStatus = "ACTIVE-SMA0";
            }

            else if (arrAgeStatusVal.equals("SM1")) {
                accountStatus = "ACTIVE-SMA1";
            }

            else if (arrAgeStatusVal.equals("SM2")) {
                accountStatus = "ACTIVE-SMA2";
            }

            else if (arrAgeStatusVal.equals("NPA")) {
                accountStatus = "ACTIVE-NPA";
            }
        }
    }

    public void getEbFfLoanDetails(String arrId, String finMnemonic) {

        try {
            EbFfLoanDetailsRecord ffLoanDetsRec = new EbFfLoanDetailsRecord(
                    da.getRecord(finMnemonic, "EB.FF.LOAN.DETAILS", "", arrId));

            for (AddressTypeClass addressType : ffLoanDetsRec.getAddressType()) {

                String addressTypeValue = addressType.getAddressType().getValue();

                if (addressTypeValue.equals("CURRENT")) {

                    state = addressType.getStateName().getValue();
                }

            }

            if (!ffLoanDetsRec.toString().isEmpty()) {
                for (FmEntityNumberClass entityNumber : ffLoanDetsRec.getFmEntityNumber()) {
                    if (entityNumber.getIsGuarantor().getValue().equalsIgnoreCase("YES")) {
                        guarantorsName = entityNumber.getLegalName().getValue();
                    }
                }
            }
        } catch (Exception e) {
            e.getMessage();
        }

    }

    public void getAccountDetails(String arrId) {

        try {
            String acctId = "";
            AaArrangementRecord arrangementRec = new AaArrangementRecord(
                    da.getRecord(finMnemonic, aaArrangement, "", arrId));
            acctId = arrangementRec.getLinkedAppl().get(0).getLinkedApplId().getValue();

            AccountRecord acctRec = new AccountRecord(da.getRecord(finMnemonic, "ACCOUNT", "", acctId));

            acctFfCentre = acctRec.getLocalRefField(ffCentre).getValue();
            groupCode = acctRec.getLocalRefField("FF.GROUP").getValue();
            loanCycle = acctRec.getLocalRefField("FF.LOAN.CYCLE").getValue();
            purpose = acctRec.getLocalRefField("FF.LOAN.PURP").getValue();
            subpurpose = acctRec.getLocalRefField("FF.LOAN.SUBPUR").getValue();
            fundSource = acctRec.getLocalRefField("FF.FUNDER.NAME").getValue();//FF.FUND.SOURCE

            getCentreName(acctFfCentre);
            getGroupName(groupCode);
        } catch (Exception e) {

            e.getMessage();
        }

    }

    private void getCentreName(String acctFfCentre) {

        try {

            EbFfCentreDetailRecord ebCentreDetail = new EbFfCentreDetailRecord(
                    da.getRecord("EB.FF.CENTRE.DETAIL", acctFfCentre));
            centerName = ebCentreDetail.getCenterName().getValue();

            officerEmpNum = ebCentreDetail.getCurrentRo().getValue();

            EbFfRoUserRecord roUser = new EbFfRoUserRecord(da.getRecord("EB.FF.RO.USER", officerEmpNum));
            ro = roUser.getRoName().getValue();

        } catch (Exception e) {
            e.getMessage();
        }

    }

    private void getGroupName(String groupCode) {

        try {

            EbFfGroupsRecord ebFfGroups = new EbFfGroupsRecord(da.getRecord("EB.FF.GROUPS", groupCode));

            groupName = ebFfGroups.getGroupName().getValue();

        } catch (Exception e) {
            e.getMessage();
        }

    }

    public void getAaArrTermAmountDetails(Contract contract) {

        try {
            AaPrdDesTermAmountRecord aaArrTermAmtRec = new AaPrdDesTermAmountRecord(
                    contract.getConditionForProperty(commitment));
            if (!aaArrTermAmtRec.toString().isEmpty()) {

                maturityDate = aaArrTermAmtRec.getMaturityDate().getValue();
                loanMaturityDate = aaArrTermAmtRec.getMaturityDate().getValue();

            }
        } catch (Exception e) {

            e.getMessage();
        }
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
                        && paymentType.getBillType().getValue().equalsIgnoreCase(INSTALLMENT)) {
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
            return FORTNIGHTLY;
        }

        if ((weeks == 4 && isOthersZero()) || (days == 28 && isOthersZero())) {
            return ONCE_28_DAYS;
        }

        if (months == 1 && years == 0 && weeks == 0 && days == 0) {
            return MONTHLY;
        }

        return OTHERS;
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

    public void getAaArrInterestDetails(Contract contract) {

        try {
            AaPrdDesInterestRecord aaArrIntRec = new AaPrdDesInterestRecord(
                    contract.getConditionForProperty("PRINTEREST"));
            interestRate = aaArrIntRec.getFixedRate(0).getEffectiveRate().getValue();
            interestRateAsOnDateOfNpa = aaArrIntRec.getFixedRate(0).getFixedRate().getValue();

        } catch (Exception e) {

            e.getMessage();
        }
    }

    public void getAaArrChargeDetails(Contract contract) {

        try {
            String processingCharges = "";
            String insuranceCharges = "";
            List<String> chgPropList = contract.getPropertyIdsForPropertyClass("CHARGE");

            for (String chgProperty : chgPropList) {
                AaPrdDesChargeRecord aaArrChgRec = new AaPrdDesChargeRecord(
                        contract.getConditionForProperty(chgProperty));
                switch (chgProperty) {
                case "PROCESSINGFEE":
                    processingCharges = aaArrChgRec.getFixedAmount().getValue();
                    break;
                case "INSURANCEFEE":
                    insuranceCharges = aaArrChgRec.getFixedAmount().getValue();
                    break;
                default:
                }
            }
            feeAndInsuranceOutstanding = String
                    .valueOf(Double.parseDouble(processingCharges) + Double.parseDouble(insuranceCharges));

        } catch (NumberFormatException e) {

            e.getMessage();
        }
    }

    private String getNpaDate(String arrangementId) {

        try {

            // Read AA.ARRANGEMENT
            AaArrangementRecord aaArrangementRec = new AaArrangementRecord(da.getRecord(aaArrangement, arrangementId));

            if (aaArrangementRec.getOrigContractDate() != null) {
                origContractDate = aaArrangementRec.getOrigContractDate().getValue();
            }

            if (origContractDate == null || origContractDate.isEmpty()) {

                npaDate = getOldestAgeAllNpaDate(arrangementId);

            }

            else {

                elseNpaMthd(arrangementId);
            }

        } catch (Exception e) {

            e.getMessage();

        }

        return npaDate;
    }

    /**
     * @param arrangementId
     */
    private void elseNpaMthd(String arrangementId) {
        try {

            EbFfNpaWriteoffMigRecord npaWriteOff = new EbFfNpaWriteoffMigRecord(
                    da.getRecord("EB.FF.NPA.WRITEOFF.MIG", arrangementId));

            if (npaWriteOff.getFirstNpaDate() != null) {

                npaDate = npaWriteOff.getFirstNpaDate().getValue();

            } else {
                npaDate = getOldestAgeAllNpaDate(arrangementId);
            }

        } catch (Exception e) {

            npaDate = getOldestAgeAllNpaDate(arrangementId);

        }
    }

    private String getOldestAgeAllNpaDate(String arrangementId) {

        String oldestNpaDate = "";

        LocalDate oldestDate = null;

        List<String> npaDateList = new ArrayList<>();

        try {

            AaAccountDetailsRecord accountDetails = new AaAccountDetailsRecord(
                    da.getRecord("AA.ACCOUNT.DETAILS", arrangementId));

            for (AgeAllDateClass ageAll : accountDetails.getAgeAllDate()) {

                String date = ageAll.getAgeAllDate().getValue();

                for (AgeAllBillTypeClass status : ageAll.getAgeAllBillType()) {

                    String ageStatus = status.getAgeAllStatus().getValue();

                    if ("NPA".equalsIgnoreCase(ageStatus) && date != null && !date.isEmpty()) {

                        // Store only NPA dates
                        npaDateList.add(date);

                    }

                }

            }

            // Compare NPA dates and get oldest date
            for (String npaDates : npaDateList) {

                LocalDate currentDate = LocalDate.parse(npaDates, formatter);

                if (oldestDate == null || currentDate.isBefore(oldestDate)) {

                    oldestDate = currentDate;
                    oldestNpaDate = npaDates;

                }

            }

        } catch (Exception e) {

            e.getMessage();

        }

        return oldestNpaDate;

    }

    public void getPaymentdetails(String arrId, Contract contract) {

        try {

            if (origContractDate == null || origContractDate.isEmpty()) {
                paymentDetsElseMthd(arrId);
                return;
            }

            EbFfLoanPaymentHisRecord loanpayhisRec = new EbFfLoanPaymentHisRecord(
                    da.getRecord("EB.FF.LOAN.PAYMENT.HIS", arrId));

            if (!loanpayhisRec.toString().isEmpty()) {
                String repaymentFreq = loanpayhisRec.getRepayFreqMatExp().getValue();

                String validValues = parsePaymentFrequency(repaymentFreq);

                repaymentFrequency = String.join(" ", validValues);

            } else {
                getAaPrdDesPaymentScheduleDetails(contract);
            }

            List<DemandDateClass> demandDateList = loanpayhisRec.getDemandDate();

            if (demandDateList == null || demandDateList.isEmpty()) {
                return;
            }

            LocalDate today = LocalDate.parse(todayDate, formatter);

            processDemandDates(demandDateList, today);

        } catch (Exception e) {
            e.getMessage();
        }
    }

    private void processDemandDates(List<DemandDateClass> demandDateList, LocalDate today) {

        for (DemandDateClass demandOuter : demandDateList) {

            String demandDateStr = demandOuter.getDemandDate().getValue();
            LocalDate demandDate = LocalDate.parse(demandDateStr, formatter);

            if (demandDate.isAfter(today)) {
                continue;
            }

            String matchedTransDate = demandOuter.getTransDate().getValue();

            PaymentAmounts amounts = calculateAmounts(demandDateList, demandDateStr);

            ifInterestPrincipalMthd(amounts.interestFound, amounts.principalFound, amounts.interestAmt,
                    amounts.principalAmt, matchedTransDate);
        }
    }

    private PaymentAmounts calculateAmounts(List<DemandDateClass> demandDateList, String demandDateStr) {

        boolean interestFound = false;
        boolean principalFound = false;
        double interestAmt = 0.0;
        double principalAmt = 0.0;

        for (DemandDateClass demandInner : demandDateList) {

            if (isValidDemand(demandInner) && demandDateStr.equals(demandInner.getDemandDate().getValue())) {

                String transType = demandInner.getTransType().getValue();
                double amt = Double.parseDouble(demandInner.getDemandAmt().getValue());

                if ("Interest".equalsIgnoreCase(transType)) {
                    interestFound = true;
                    interestAmt += amt;
                } else if ("Principal".equalsIgnoreCase(transType)) {
                    principalFound = true;
                    principalAmt += amt;
                }
            }
        }

        return new PaymentAmounts(interestFound, principalFound, interestAmt, principalAmt);
    }

    private boolean isValidDemand(DemandDateClass demand) {
        return demand != null && demand.getDemandDate() != null && demand.getDemandDate().getValue() != null
                && demand.getTransType() != null;
    }

    class PaymentAmounts {

        boolean interestFound;
        boolean principalFound;
        double interestAmt;
        double principalAmt;

        PaymentAmounts(boolean interestFound, boolean principalFound, double interestAmt, double principalAmt) {
            this.interestFound = interestFound;
            this.principalFound = principalFound;
            this.interestAmt = interestAmt;
            this.principalAmt = principalAmt;
        }
    }

    private void ifInterestPrincipalMthd(boolean interestFound, boolean principalFound, double interestAmt,
            double principalAmt, String matchedTransDate) {
        if (interestFound && principalFound) {

            double totalAmount = interestAmt + principalAmt;

            lastPaymentDate = matchedTransDate;
            lastPaymentAmount = String.valueOf(totalAmount);

        }
    }

    private void paymentDetsElseMthd(String arrId) {
        AaActivityHistoryRecord aaActHistRec;
        String latestFtTxn = "";
        aaActHistRec = new AaActivityHistoryRecord(da.getRecord(finMnemonic, aaActivityHistory, "", arrId));

        for (EffectiveDateClass effDateList : aaActHistRec.getEffectiveDate()) {

            for (ActivityRefClass actRefList : effDateList.getActivityRef()) {

                if (actRefList.getActivity().getValue().equals("LENDING-APPLYPAYMENT-PR.COLLECTION")) {

                    for (int j = aaActHistRec.getTransRef().size() - 1; j >= 0; j--) {
                        if (aaActHistRec.getTransRef().get(j).getTransRef().getValue().startsWith("FT")) {
                            latestFtTxn = aaActHistRec.getTransRef().get(j).getTransRef().getValue();
                            String[] latestFtId = latestFtTxn.split("\\\\");
                            latestFtTxn = latestFtId[0];
                        }
                    }
                    ifLatestFtTxn(latestFtTxn);

                }
            }
        }
    }

    private void ifLatestFtTxn(String latestFtTxn) {
        if (!latestFtTxn.equals("")) {
            String ftResult = getFtTxnDetails(latestFtTxn);
            String[] ftValue = ftResult.split("\\*");

            lastPaymentDate = ftValue[0];
            lastPaymentAmount = ftValue[1];
        }
    }

    public String getFtTxnDetails(String ftId) {
        String creditAmount = "";
        String creditDate = "";
        FundsTransferRecord ftRec = null;
        try {
            ftRec = new FundsTransferRecord(da.getRecord(finMnemonic, "FUNDS.TRANSFER", "", ftId));
        } catch (Exception e) {
            try {
                ftRec = new FundsTransferRecord(da.getHistoryRecord("FUNDS.TRANSFER", ftId));
            } catch (Exception e1) {
                e1.getMessage();
            }
        }

        if (ftRec != null && ftRec.getTransactionType().getValue().equalsIgnoreCase("ACRP")) {
            creditDate = ftRec.getCreditValueDate().getValue();
            creditAmount = ftRec.getCreditAmount().getValue();
        }
        return creditDate + "*" + creditAmount;
    }

    public void getEcbDetails(Contract contract , String arrId) {

        try {
            String currAccount = getBalance(contract, "FFPRIOUTAMT", trade); // FFPRINODFUTAMT

            principalDefault = getBalance(contract, "FFPRINCDEF", trade); // FFPRINDEFAMT

            String accPrinInt = getBalance(contract, "FFINTODFUTAMT", trade); // FFINTOUTAMT
            interestDefault = getBalance(contract, "FFINTERDEF", trade);

            double prOutvalue = Double.parseDouble(currAccount);
            principalOutstanding = String.valueOf(Math.abs(prOutvalue));
            
            double futureInterest = getFutureInterestAmount(arrId);

            String accPrinInterest =
                    getBalance(contract, "ACCPRINTEREST", trade);

            String accPrinInterestCust =
                    getBalance(contract, "ACCPRINTERESTCUST", trade);

            double result =
                    Math.abs(Double.parseDouble(accPrinInt))
                  + futureInterest
                  - Math.abs(Double.parseDouble(accPrinInterest))
                  - Math.abs(Double.parseDouble(accPrinInterestCust));

            interestOutstanding = String.valueOf(Math.abs(result));

            outstandingAmountAsOnNpaDate = String.format("%.2f", Math.abs(Double.parseDouble(principalDefault)));

            sumofAllPrincipalDefault += Math.abs(Double.parseDouble(principalDefault));
            overAllPrincipalDefault = String.format("%.2f", sumofAllPrincipalDefault);

            sumofAllInterestDefault += Math.abs(Double.parseDouble(interestDefault));
            overAllInterestDefault = String.format("%.2f", sumofAllInterestDefault);
            
           

        } catch (NumberFormatException e) {
            e.getMessage();
        }
    }
    
    private double getFutureInterestAmount(String arrId) {

        double futureInterest = 0.0;

        try {

            Session session = new Session(this);

            LocalDate lmsDate =
                    LocalDate.parse(session.getCurrentVariable("!TODAY"),
                            formatter);

            EbFfCollectionDetsRecord rec =
                    new EbFfCollectionDetsRecord(
                            da.getRecord("EB.FF.COLLECTION.DETS", arrId));

            List<TField> dueDates = rec.getDueDate();
            List<TField> interestAmounts = rec.getInterestAmt();

            for (int i = 0; i < dueDates.size(); i++) {

                String dueDate = dueDates.get(i).getValue();

                if (dueDate == null || dueDate.isEmpty()) {
                    continue;
                }

                LocalDate due =
                        LocalDate.parse(dueDate, formatter);

                if (due.isAfter(lmsDate)) {

                    String interest = "0";

                    if (i < interestAmounts.size()) {
                        interest = interestAmounts.get(i).getValue();
                    }

                    if (!interest.isEmpty()) {
                        futureInterest += Double.parseDouble(interest);
                    }
                }
            }

        } catch (Exception e) {
            e.getMessage();
        }

        return futureInterest;
    }
    
    
    public String getBalance(Contract contract, String accountType, String bookingType) {
        List<BalanceMovement> movements = contract.getContractBalanceMovements(accountType, bookingType);
        return (movements != null && !movements.isEmpty()) ? movements.get(0).getBalance().toString() : "0";
    }

    public void initialiseCompanyInfo(ServiceData serviceData) {

        companyIdInfo = serviceData.getCompanyId();
        CompanyRecord companyObj = new CompanyRecord(da.getRecord("COMPANY", companyIdInfo));
        finMnemonic = companyObj.getFinancialMne().getValue();
        mnemonic = companyObj.getCustomerMnemonic().getValue();
        branchName = companyObj.getCompanyName().get(0).getValue(); // .split("-")[0]
        branchCode = companyObj.getCoCode();

    }

    private String formatDate(String inputDate) {
        try {

            LocalDate date = LocalDate.parse(inputDate, formatter);

            return date.format(outputFormatter);

        } catch (Exception e) {
            return inputDate;
        }
    }

    public void writeToFile(List<String> data, String filePath) {
        try {
            File file = new File(filePath);
            file.getParentFile().mkdirs();
            boolean fileExists = file.exists();

            try (FileWriter writer = new FileWriter(file, true)) {
                if (!fileExists) {
                    String header = String.join(",", "Loan Number", "Legacy Account Number", "Customer",
                            "Customer Number", "Telephone", "GuarantorName", "BranchName", "BranchCode", "Officer",
                            "Officer Employee Number", "Center", "Center Code", "Group Name", "Date Of Birth",
                            "Religion", "Caste", "Locale", "State", "Product", "Loan Amount", "Loan Cycle",
                            "LastPayment Appropriation Date", "LastPaymentAppropriationAmount", "RepaymentFrequency",
                            "MaturityDate", "InterestRate", "EMI", "Purpose", "Sub Purpose", "FundSource",
                            "PrincipalOutstanding", "InterestOutstanding", "DisbursementDate", "InstallmentsDue",
                            "EarliestUnpaidDemandDate", "LatestUnpaidDemandDate", "PrincipalDefault", "InterestDefault",
                            "AccountStatus", "DeathCaseRemark", "DeathFlaggedDate", "DPD", "DPDClassification",
                            "NPADate");

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
