package com.temenos.fusion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.temenos.api.TField;

import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.aa.contractapi.BalanceMovement;
import com.temenos.t24.api.complex.eb.servicehook.ServiceData;
import com.temenos.t24.api.hook.system.ServiceLifecycle;
import com.temenos.t24.api.records.aaaccountdetails.AaAccountDetailsRecord;
import com.temenos.t24.api.records.aaaccountdetails.BillIdClass;
import com.temenos.t24.api.records.aaaccountdetails.BillPayDateClass;
import com.temenos.t24.api.records.aaactivityhistory.AaActivityHistoryRecord;
import com.temenos.t24.api.records.aaactivityhistory.ActivityRefClass;
import com.temenos.t24.api.records.aaactivityhistory.EffectiveDateClass;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaarrangement.LinkedApplClass;
import com.temenos.t24.api.records.aabilldetails.AaBillDetailsRecord;
import com.temenos.t24.api.records.aabilldetails.PropertyClass;
import com.temenos.t24.api.records.aabilldetails.SettleStatusClass;
import com.temenos.t24.api.records.aaoverduestats.AaOverdueStatsRecord;
import com.temenos.t24.api.records.aaprddesaccount.AaPrdDesAccountRecord;
import com.temenos.t24.api.records.aaprddesaccount.AltIdTypeClass;
import com.temenos.t24.api.records.aaprddescharge.AaPrdDesChargeRecord;
import com.temenos.t24.api.records.aaprddesinterest.AaPrdDesInterestRecord;
import com.temenos.t24.api.records.aaprddespaymentschedule.AaPrdDesPaymentScheduleRecord;

import com.temenos.t24.api.records.aaprddestermamount.AaPrdDesTermAmountRecord;
import com.temenos.t24.api.records.aaproduct.AaProductRecord;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.records.company.CompanyRecord;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.records.customer.Phone1Class;
import com.temenos.t24.api.records.ebffcentredetail.EbFfCentreDetailRecord;
import com.temenos.t24.api.records.ebffcollectiondets.EbFfCollectionDetsRecord;
import com.temenos.t24.api.records.ebffcollectiondetshistory.EbFfCollectionDetsHistoryRecord;
import com.temenos.t24.api.records.ebffcustdpd.EbFfCustDpdRecord;
import com.temenos.t24.api.records.ebffgroups.EbFfGroupsRecord;
import com.temenos.t24.api.records.ebffloandetails.AddressTypeClass;
import com.temenos.t24.api.records.ebffloandetails.DocEntityNumberClass;
import com.temenos.t24.api.records.ebffloandetails.EbFfLoanDetailsRecord;
import com.temenos.t24.api.records.ebffloandetails.FmEntityNumberClass;
import com.temenos.t24.api.records.ebffloandetails.InEntityNumberClass;

import com.temenos.t24.api.records.ebffloandpd.EbFfLoanDpdRecord;
import com.temenos.t24.api.records.ebffloanpaymenthis.DemandDateClass;
import com.temenos.t24.api.records.ebffloanpaymenthis.EbFfLoanPaymentHisRecord;
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
 * EB.API : EB.FF.POOL.INST.RPT.SELECT, EB.FF.POOL.INST.RPT
 * Attached to : BATCH > BNK/FF.DAILY.REPORT.EXTRACT   MFI/FF.DAILY.REPORT.EXTRACT
 * Description: COB Report generation -> Sample Pool and Instalment Report
 *------------------------------------------------------------------------------ 
 * Modification History : NA
 *----------------------------------------------------------------------------- 
 *20-Nov-2025   Development      Initial Version
 *-----------------------------------------------------------------------------
 *27-Jan-2025   Defect           Kavya N
 *-----------------------------------------------------------------------------
 **-----------------------------------------------------------------------------
 *26-Feb-2025   Remapping          Sathish Kumar K / Yuvasri S
 *-----------------------------------------------------------------------------
 */

public class FfRptGenForInstallment extends ServiceLifecycle {

    private static final String INTEREST = "INTEREST";

    private static final String ACTIVE = "ACTIVE";

    private static final String REPAID = "REPAID";

    private static final String SETTLED = "SETTLED";

    private static final String DUE = "DUE";

    private static final String INSTALLMENT = "INSTALLMENT";
    public static final String CUSTOMER = "CUSTOMER";
    public static final String TRADE = "TRADE";
    public static final String FUNDS_TRANSFER = "FUNDS.TRANSFER";
    private static final FusionFileLogger FfRptGenForInstallmentRep = FusionFileLogger
            .getLogger(FfRptGenForInstallment.class);
    DataAccess da = new DataAccess(this);
    String finMnemonic = "";

    String unspecifiedCredit = "";
    String interestAccrued = "";
    String interestCredit = "";

    String comNameSub = "";

    String mnemonic = "";
    String arrId = "";
    String arrCompId = "";
    String customerNumber = "";
    String village = "";
    String loanNumber = "";
    String legacyAcctNo = "";
    String custNumber = "";
    String memberName = "";
    String dateOfBirth = "";
    String gender = "";
    String religion = "";
    String caste = "";
    String occupation = "";
    String residenceAddress = "";
    String gramPanchayat = "";
    String villageLocale = "";
    String blockMunicipality = "";
    String district = "";
    String state = "";
    String pincode = "";
    String address1 = "";
    String address2 = "";
    String city = "";
    String landmark = "";
    String residencePhoneNumber = "";
    String mobileNumber = "";
    String grossIncome = "";
    String spouseName = "";
    String guarantorName = "";
    String guarantorRelation = "";
    String guarantorDateOfBirth = "";
    String voterIdCard = "";
    String rationCard = "";
    String aadhaarNumber = "";
    String officeName = "";
    String officeCode = "";
    String centerName = "";
    String centerCode = "";
    String centerFormationDate = "";
    String centerMeetingDay = "";
    String meetingTime = "";
    String groupName = "";
    String groupCode = "";
    String officerName = "";
    String officerEmployeeNumber = "";
    String purpose = "";
    String landHoldingAcres = "";
    String subPurpose = "";
    String businessType = "";
    String productName = "";
    String productInterestRate = "";
    String loanAmount = "";
    String approvedAmount = "";
    String categoryOfLoan = "";
    String cycleNumber = "";
    String approvalDate = "";
    String disbursementDate = "";
    String firstInstallmentDate = "";
    String lastInstallmentDate = "";
    String processingFees = "";
    String insuranceCharges = "";
    String otherCharges = "";
    String installmentAmountFirstSlab = "";
    String loanTenure = "";
    String numberOfInstallments = "";
    String installmentFrequency = "";

    String principalOverdue = "";
    String interestOverdue = "";
    String principalOutstanding = "";
    String interestOutstanding = "";

    String totalInterestCollected = "";
    String overdueDays = "";
    String numberOfInstallmentsPaid = "";
    String completedInstallmentNumber = "";
    String lastPaymentDate = "";
    String lastPaymentAmount = "";
    String parkedAmount = "";
    String loanStatus = "";
    String fundSource = "";
    String accountDpdClassification = "";
    String customerDpd = "";
    String customerDpdClassification = "";
    String creditInsurancePolicyNumberGuarantor = "";
    String creditInsurancePolicyNumberCustomer = "";
    String count = "";
    String salesOfficerName = "";
    String salesOfficerCode = "";
    String vendorType = "";
    String productCode = "";
    String currentOd = "";
    String covid19Moratorium = "";
    String isRestructured = "";
    String barcode = "";
    String accountNo = "";
    String todayDate = "";
    String instHeader = "";
    String latestFtTxn = "";
    String relation = "";
    String filePath = "";
    List<String> arrList = new ArrayList<>();
    List<String> loanDpdList = new ArrayList<>();
    List<String> billIdList = new ArrayList<>();
    String curDpd = "";
    String cusAddress = "";
    String street = "";
    String town = "";
    String postCode = "";
    String arrStDt = "";
    String accNum = "";
    String roId = "";
    String loanArrStatus = "";
    String isGuarantor = "";
    double interestAmount = 0.0;

    int years = 0;
    int months = 0;
    int weeks = 0;
    int days = 0;
    int fortnights = 0;
    int migratedCompletedInstallments = 0;
    int migratedInstallmentsPaid = 0;
    int nonMigratedCompletedInstallments = 0;
    int nonMigratedInstallmentsPaid = 0;
    List<List<String>> paymentDetails = new ArrayList<>();
    List<LinkedApplClass> linkedAppList = null;

    Set<LocalDate> uniqueDemandDates = new HashSet<>();

    public static final DateTimeFormatter T24_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    public static final DateTimeFormatter OUT_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    boolean migratedContractFlg = false;
    public static final String FILE_NAME = "SamplePoolandInstallmentReport";
    private static final String ACCOUNT = "ACCOUNT";
    private boolean filePathFlag = false;
    String acctId = "";

    Session session = new Session(this);

    boolean lastPaymentFlg = false;

    @Override
    public List<String> getIds(ServiceData serviceData, List<String> controlList) {
        try {

            initialiseCompanyInfo(serviceData, arrCompId);
            arrList = da.selectRecords(finMnemonic, "AA.ARRANGEMENT", "",
                    "WITH ARR.STATUS NE CLOSE AND ARR.STATUS NE PENDING.CLOSURE");

        } catch (Exception e) {
            FfRptGenForInstallmentRep.error("error in getid" + e);

        }
        return arrList;
    }

    @Override
    public void process(String id, ServiceData serviceData, String controlItem) {
        try {

            todayDate = session.getCurrentVariable("!TODAY");

            Contract contract = new Contract(this);
            arrId = id;
            contract.setContractId(arrId);

            getArrangementDetails(contract);
            initialiseCompanyInfo(serviceData, arrCompId);
            getAaArrAccountDetails(contract);
            getAaAccountDetails(contract);
            getAaArrTermAmountDetails(contract);
            getAaArrInterestDetails(contract);
            getAaArrChargeDetails(contract);
            getAaPrdDesPaymentScheduleDetails(arrId, contract);
            getPaymentdetails(arrId);
            getEbFfLoanHisLastPayment(arrId);
            getCustomerDetails(customerNumber);
            getEbFfLoanDetails(arrId, finMnemonic);

            getEbFfCollectionDets(arrId);

            getInterestAmount(arrId);

            getEcbDetails(contract);

            getDpdBalanceDetails(arrId);
            getCusDpd(customerNumber);

            StringBuilder rowBuilder = new StringBuilder(4096);
            rowBuilder.append(arrId).append(",").append(legacyAcctNo).append(",").append(custNumber).append(",")
                    .append(memberName).append(",").append(convertDate(dateOfBirth)).append(",").append(gender)
                    .append(",").append(religion).append(",").append(caste).append(",").append(occupation).append(",")
                    .append(residenceAddress).append(",").append(village).append(",").append(gramPanchayat).append(",")
                    .append(villageLocale).append(",").append(blockMunicipality).append(",").append(district)
                    .append(",").append(state).append(",").append(pincode).append(",").append(residencePhoneNumber)
                    .append(",").append(mobileNumber).append(",").append(grossIncome).append(",").append(spouseName)
                    .append(",").append(guarantorName).append(",").append(guarantorRelation).append(",")
                    .append(convertDate(guarantorDateOfBirth)).append(",").append(voterIdCard).append(",")
                    .append(rationCard).append(",").append(aadhaarNumber).append(",").append(officeName).append(",")
                    .append(officeCode).append(",").append(centerName).append(",").append(centerCode).append(",")
                    .append(centerFormationDate).append(",").append(centerMeetingDay).append(",").append(meetingTime)
                    .append(",").append(groupName).append(",").append(groupCode).append(",").append(officerName)
                    .append(",").append(officerEmployeeNumber).append(",").append(purpose).append(",")
                    .append(landHoldingAcres).append(",").append(subPurpose).append(",").append(businessType)
                    .append(",").append(productName).append(",").append(productInterestRate).append(",")
                    .append(loanAmount).append(",").append(approvedAmount).append(",").append(categoryOfLoan)
                    .append(",").append(cycleNumber).append(",").append(convertDate(approvalDate)).append(",")
                    .append(convertDate(disbursementDate)).append(",").append(convertDate(firstInstallmentDate))
                    .append(",").append(convertDate(lastInstallmentDate)).append(",").append(processingFees).append(",")
                    .append(insuranceCharges).append(",").append(otherCharges).append(",")
                    .append(installmentAmountFirstSlab).append(",").append(loanTenure).append(",")
                    .append(numberOfInstallments).append(",").append(installmentFrequency).append(",")
                    .append(principalOverdue).append(",").append(totalInterestCollected).append(",")
                    .append(interestOverdue).append(",").append(overdueDays).append(",")
                    .append(numberOfInstallmentsPaid).append(",").append(completedInstallmentNumber).append(",")
                    .append(convertDate(lastPaymentDate)).append(",").append(lastPaymentAmount).append(",")
                    .append(parkedAmount).append(",").append(principalOutstanding).append(",")
                    .append(interestOutstanding).append(",").append(loanStatus).append(",").append(fundSource)
                    .append(",").append(accountDpdClassification).append(",").append(customerDpd).append(",")
                    .append(customerDpdClassification).append(",").append(creditInsurancePolicyNumberCustomer)
                    .append(",").append(creditInsurancePolicyNumberGuarantor).append(",").append(count).append(",")
                    .append(unspecifiedCredit).append(",").append(interestAccrued).append(",").append(interestCredit);

            if (!paymentDetails.isEmpty()) {
                for (List<String> payments : paymentDetails) {
                    rowBuilder.append(",").append(payments.get(0)).append(",").append(payments.get(1)).append(",")
                            .append(convertDate(payments.get(2))).append(",").append(payments.get(3));
                }
            }
            String finalLine = rowBuilder.toString();

            if (!filePathFlag) {
                String paramId = "FF.COB.REPORT.EXTRACT";
                EbFfParameterRecord paramRec = new EbFfParameterRecord(da.getRecord("EB.FF.PARAMETER", paramId));
                for (ParamDescClass paramDesc : paramRec.getParamDesc()) {
                    if (paramDesc.getParamName().getValue().equals("Reports Temp Path")) {
                        filePath = paramDesc.getParamValue().getValue();
                    }
                }
                filePathFlag = true;
            }
            String outputPath = filePath + FILE_NAME + "_" + finMnemonic + "_" + todayDate + "_" + "temp" + "_"
                    + session.getSessionNumber() + ".csv";

            writeToFile(finalLine, outputPath);

        } catch (Exception e) {
            FfRptGenForInstallmentRep.error("error in process" + e);
        }
    }

    public String convertDate(String inDate) {
        String outDate = "";
        try {
            LocalDate date = LocalDate.parse(inDate, T24_FORMATTER);
            outDate = date.format(OUT_FORMATTER);
            return outDate;
        } catch (Exception e) {
            e.getMessage();
        }
        return outDate;
    }

    public void getAaArrTermAmountDetails(Contract contract) {
        try {
            AaPrdDesTermAmountRecord aaArrTermAmtRec = new AaPrdDesTermAmountRecord(
                    contract.getConditionForProperty("COMMITMENT"));
            if (!aaArrTermAmtRec.toString().isEmpty()) {
                if (loanTenure == null || loanTenure.isEmpty()) {
                    loanTenure = aaArrTermAmtRec.getTerm().getValue();
                }

                loanAmount = aaArrTermAmtRec.getAmount().getValue();
                approvedAmount = loanAmount;
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void getAaAccountDetails(Contract contract) {

        double intPropAmt = 0.0;
        double intPropAmt2 = 0.0;
        double totIntPropAmt = 0.0;

        int numberOfInstallmentsPaidInt = 0;
        String loanStat = "";

        try {
            AaAccountDetailsRecord aaAcctDets = contract.getAccountDetailsRecord();

            // last payment date getting method added new

            getlastPaymentDate(aaAcctDets);
            loanStat = getAccountDpdStatus(aaAcctDets.getArrAgeStatus().getValue());

            getAccountStatus(aaAcctDets.getArrAgeStatus().getValue());

            accountDpdClassification = loanStat;

//            if (migratedContractFlg) {

            intPropAmt = processNonMigratedContract(aaAcctDets);

            intPropAmt2 = processMigratedContract(arrId, contract);

            numberOfInstallmentsPaidInt = migratedInstallmentsPaid + nonMigratedInstallmentsPaid;

            totIntPropAmt = intPropAmt + intPropAmt2;

//            else {
//                totIntPropAmt = processNonMigratedContract(aaAcctDets);
//
//                numberOfInstallmentsPaidInt = nonMigratedInstallmentsPaid;
//            }

            totalInterestCollected = String.format("%.2f", totIntPropAmt);
            numberOfInstallmentsPaid = String.valueOf(numberOfInstallmentsPaidInt);

        } catch (Exception e) {
            e.getMessage();
        }
    }

    private double processMigratedContract(String arrId, Contract contract) {
        double ebLoanintPropAmt = 0.0;
        // migratedCompletedInstallments = 0;
        migratedInstallmentsPaid = 0;

        try {
            EbFfLoanPaymentHisRecord payHistRec = new EbFfLoanPaymentHisRecord(
                    da.getRecord("", "EB.FF.LOAN.PAYMENT.HIS", "", arrId));
            loanTenure = payHistRec.getLoanTenure().getValue() + " " + payHistRec.getTenureUnit().getValue();

            LocalDate currentDate = LocalDate.parse(todayDate, T24_FORMATTER);

            for (DemandDateClass demand : payHistRec.getDemandDate()) {
                LocalDate demandDate = LocalDate.parse(demand.getDemandDate().getValue(), T24_FORMATTER);
                LocalDate payDate = LocalDate.parse(demand.getPymtDate().getValue(), T24_FORMATTER);
                if (demandDate.isAfter(currentDate)) {
                    continue;
                }

                String transType = demand.getTransType().getValue();

                if ((payDate.isBefore(currentDate) || currentDate.equals(payDate))
                        && (transType.equalsIgnoreCase(INTEREST) || transType.equalsIgnoreCase("OVERDUE INTEREST"))) {
                    ebLoanintPropAmt += Double.parseDouble(demand.getPymtAmt().getValue());
                }

                if ((demandDate.isBefore(currentDate) || demandDate.equals(currentDate))
                        && (transType.equalsIgnoreCase("PRINCIPAL") || transType.equalsIgnoreCase(INTEREST))) {

                    uniqueDemandDates.add(demandDate);

                    // migratedCompletedInstallments++;
                }
            }

            migratedInstallmentsPaid = uniqueDemandDates.size();

        } catch (NumberFormatException e) {
            e.getMessage();
        }
        return ebLoanintPropAmt;
    }

    private double processNonMigratedContract(AaAccountDetailsRecord aaAcctDets) {

        // nonMigratedCompletedInstallments = 0;
        nonMigratedInstallmentsPaid = 0;
        double totalInterestCollected = 0.0;

        try {

            for (BillPayDateClass billPayDate : aaAcctDets.getBillPayDate()) {

                for (BillIdClass billId : billPayDate.getBillId()) {

                    // Process only INSTALLMENT bills
                    if (!INSTALLMENT.equals(billId.getBillType().getValue())) {
                        continue;
                    }

                    // Process only SETTLED/REPAID bills
                    if (!(SETTLED.equals(billId.getSetStatus().getValue())
                            || REPAID.equals(billId.getSetStatus().getValue()))) {
                        continue;
                    }

                    AaBillDetailsRecord billDetailRecord = new AaBillDetailsRecord(
                            da.getRecord(finMnemonic, "AA.BILL.DETAILS", "", billId.getBillId().getValue()));

                    boolean validInstallment = false;

                    for (PropertyClass prop : billDetailRecord.getProperty()) {

                        String property = prop.getProperty().getValue();

                        // Sum PRINTEREST amount
                        if ("PRINTEREST".equalsIgnoreCase(property)) {

                            totalInterestCollected += Double.parseDouble(prop.getOrPropAmount().getValue());

                            validInstallment = true;
                        }

                        // ACCOUNT property also qualifies for installment count
                        if ("ACCOUNT".equalsIgnoreCase(property)) {
                            validInstallment = true;
                        }
                    }

                    // Count installments paid only for PAY.METHOD = DUE
                    if (DUE.equalsIgnoreCase(billId.getPayMethod().getValue()) && validInstallment) {
                        nonMigratedInstallmentsPaid++;
                    }

                    // nonMigratedCompletedInstallments++;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return totalInterestCollected;
    }

    // LAST PAYMENT DATE

    public void getlastPaymentDate(AaAccountDetailsRecord aaAcctDets) {
        try {
            for (BillPayDateClass billPayDate : aaAcctDets.getBillPayDate()) {
                for (BillIdClass billId : billPayDate.getBillId()) {
                    if (billId.getBillType().getValue().equals(INSTALLMENT)) {

                        // billDate <= today and status is SETTLED or REPAID

                        LocalDate billDate = LocalDate.parse(billId.getBillDate().getValue(), T24_FORMATTER);
                        LocalDate today = LocalDate.parse(todayDate, T24_FORMATTER);

                        if ((billDate.isBefore(today) || billDate.isEqual(today))
                                && (SETTLED.equals(billId.getSetStatus().getValue())
                                        || REPAID.equals(billId.getSetStatus().getValue()))) {

                            String bilId = billId.getBillId().getValue();
                            getbillDetails(bilId);

                        }

                    }

                }
            }

        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void getbillDetails(String bilId) {
        try {

            AaBillDetailsRecord billDetailRecord = new AaBillDetailsRecord(
                    da.getRecord(finMnemonic, "AA.BILL.DETAILS", "", bilId));

            // Check whether PROPERTY = PROCESSINGFEE exists

            boolean isProcessingFee = false;
            List<PropertyClass> property = billDetailRecord.getProperty();

            for (PropertyClass prop : property) {
                if ("PROCESSINGFEE".equalsIgnoreCase(prop.getProperty().getValue())) {
                    isProcessingFee = true;
                    break;
                }
            }

            // Execute existing logic only if PROPERTY != PROCESSINGFEE

            if (!isProcessingFee) {
                for (SettleStatusClass setStat : billDetailRecord.getSettleStatus()) {
                    if (setStat.getSettleStatus().getValue().equals(SETTLED)
                            || setStat.getSettleStatus().getValue().equals(REPAID)) {
                        lastPaymentDate = setStat.getSetStChgDt().getValue();

                    }
                }

            }

            if (lastPaymentDate == null || lastPaymentDate.trim().isEmpty()) {

                lastPaymentFlg = true;

            }

        } catch (Exception e) {
            e.getMessage();
        }
    }

    public String getAccountStatus(String arrAgeStatus) {
        if (loanStatus == null || loanStatus.isEmpty()) {
            if (loanArrStatus.equals("CURRENT") || loanArrStatus.equals("EXPIRED")) {
                switch (arrAgeStatus) {
                case "SM0":
                    loanStatus = "ACTIVE-SMA0";
                    break;
                case "SM1":
                    loanStatus = "ACTIVE-SMA1";
                    break;
                case "SM2":
                    loanStatus = "ACTIVE-SMA2";
                    break;
                case "NPA":
                    loanStatus = "ACTIVE-NPA";
                    break;
                default:
                    loanStatus = ACTIVE;
                }
            } else if (loanArrStatus.equals("CLOSE") || loanArrStatus.equals("PENDING.CLOSURE")
                    || loanArrStatus.equals("MATURED")) {
                loanStatus = "CLOSED";
            }
        }
        return loanStatus;
    }

    public String getAccountDpdStatus(String arrAgeStatus) {
        String accDpdstat = "";
        if (arrAgeStatus == null || arrAgeStatus.trim().isEmpty()) {
            return ACTIVE;
        }
        switch (arrAgeStatus) {
        case "SM0":
            accDpdstat = "SMA0";
            break;
        case "SM1":
            accDpdstat = "SMA1";
            break;
        case "SM2":
            accDpdstat = "SMA2";
            break;
        case "NPA":
            accDpdstat = "NPA";
            break;
        case "CUR":
            accDpdstat = ACTIVE;
            break;
        default:
            accDpdstat = "";
        }

        return accDpdstat;
    }

    public String getcustomerDpdStatus(String dpdStatus) {
        String cusDpdstat = "";
        if (dpdStatus != null && !dpdStatus.isEmpty()) {
            switch (dpdStatus) {
            case "SM0":
                cusDpdstat = "SMA0";
                break;
            case "SM1":
                cusDpdstat = "SMA1";
                break;
            case "SM2":
                cusDpdstat = "SMA2";
                break;
            case "NPA":
                cusDpdstat = "NPA";
                break;
            case "CUR":
                cusDpdstat = ACTIVE;
                break;

            default:
                cusDpdstat = "";
            }
        }

        return cusDpdstat;
    }

    public void getAaArrInterestDetails(Contract contract) {
        try {
            AaPrdDesInterestRecord aaprdDesIntRec = new AaPrdDesInterestRecord(
                    contract.getConditionForProperty("PRINTEREST"));

            productInterestRate = aaprdDesIntRec.getFixedRate(0).getFixedRate().getValue();

        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void getAaArrChargeDetails(Contract contract) {
        try {
            List<String> chgPropList = contract.getPropertyIdsForPropertyClass("CHARGE");

            for (String chgProperty : chgPropList) {
                AaPrdDesChargeRecord aaArrChgRec = new AaPrdDesChargeRecord(
                        contract.getConditionForProperty(chgProperty));
                switch (chgProperty) {
                case "PROCESSINGFEE":
                    processingFees = aaArrChgRec.getFixedAmount().getValue();
                    break;
                case "INSURANCEFEE":
                    insuranceCharges = aaArrChgRec.getFixedAmount().getValue();
                    break;
                default:
                    break;
                }
            }

        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void getAaArrAccountDetails(Contract contract) {
        try {
            AaPrdDesAccountRecord aaArrAccRec = new AaPrdDesAccountRecord(contract.getConditionForProperty(ACCOUNT));

            if (migratedContractFlg) {
                for (AltIdTypeClass altType : aaArrAccRec.getAltIdType()) {
                    if (altType.getAltIdType().getValue().equals("LEGACY")) {
                        legacyAcctNo = altType.getAltId().getValue();
                    }
                }
            }

        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void getPaymentdetails(String arrId) {
        try {
            FundsTransferRecord ftRec = null;
            AaActivityHistoryRecord aaActHistRec = new AaActivityHistoryRecord(
                    da.getRecord(finMnemonic, "AA.ACTIVITY.HISTORY", "", arrId));
            for (EffectiveDateClass effectiveDtList : aaActHistRec.getEffectiveDate()) {
                for (ActivityRefClass actRefList : effectiveDtList.getActivityRef()) {
                    if (actRefList.getActivity().getValue().equals("LENDING-APPLYPAYMENT-PR.COLLECTION")
                            && actRefList.getActStatus().getValue().equals("AUTH")) {

                        String contractId = actRefList.getContractId().getValue();

                        if (contractId.startsWith("FT")) {
                            String[] removeArg = contractId.split("\\\\");
                            String ftId = removeArg[0];
                            try {
                                ftRec = new FundsTransferRecord(da.getRecord(finMnemonic, FUNDS_TRANSFER, "", ftId));
                            } catch (Exception e) {
                                e.getMessage();
                                try {
                                    ftRec = new FundsTransferRecord(da.getHistoryRecord(FUNDS_TRANSFER, ftId));

                                } catch (Exception e1) {
                                    e1.getMessage();
                                }
                            }
                        }
                        if (ftRec != null) {
                            lastPaymentAmount = getFundsTransferDetails(ftRec, todayDate);

                        }
                    }
                }
            }

        } catch (Exception e) {
            e.getMessage();
        }
    }

    private void getEbFfLoanHisLastPayment(String arrId2) {
        double lastPayAt = 0.0;
        try {

            LocalDate currentDate = LocalDate.parse(todayDate, T24_FORMATTER);
            EbFfLoanPaymentHisRecord payHistRec = new EbFfLoanPaymentHisRecord(
                    da.getRecord("", "EB.FF.LOAN.PAYMENT.HIS", "", arrId2));

            LocalDate latestPayDate = null;

            // Step 1 : Find latest payment date
            for (DemandDateClass demand : payHistRec.getDemandDate()) {

                if (demand.getPymtDate().getValue() == null || demand.getPymtDate().getValue().trim().isEmpty()) {
                    continue;
                }

                LocalDate payDate = LocalDate.parse(demand.getPymtDate().getValue(), T24_FORMATTER);

                if ((payDate.isBefore(currentDate) || payDate.equals(currentDate))
                        && (latestPayDate == null || payDate.isAfter(latestPayDate))) {
                    latestPayDate = payDate;
                }
            }

            // Step 2 : Sum only records of latest payment date

            if (latestPayDate != null) {

                for (DemandDateClass demand : payHistRec.getDemandDate()) {

                    LocalDate payDate = LocalDate.parse(demand.getPymtDate().getValue(), T24_FORMATTER);

                    String transType = demand.getTransType().getValue();

                    if (payDate.equals(latestPayDate)
                            && (transType.equalsIgnoreCase("PRINCIPAL") || transType.equalsIgnoreCase(INTEREST)
                                    || transType.equalsIgnoreCase("OVERDUE INTEREST"))) {

                        lastPayAt += Double.parseDouble(demand.getPymtAmt().getValue());
                    }
                }

                lastPaymentDate = latestPayDate.format(T24_FORMATTER);
                lastPaymentAmount = String.format("%.2f", Math.abs(lastPayAt));

            }

        } catch (Exception e) {
            e.getMessage();
        }

    }

    private String getFundsTransferDetails(FundsTransferRecord ftRec, String todayDate2) {
        String amt = "";
        String crDate = "";
        try {
            LocalDate tdyDate = LocalDate.parse(todayDate2, T24_FORMATTER);
            crDate = ftRec.getCreditValueDate().getValue();
            LocalDate crDt = LocalDate.parse(crDate, T24_FORMATTER);
            if (tdyDate.isBefore(crDt) || tdyDate.equals(crDt)) {
                double amount = Double.parseDouble(ftRec.getCreditAmount().getValue());
                amt = String.format("%.2f", Math.abs(amount));
            }

        } catch (Exception e) {
            e.getMessage();
        }
        return amt;
    }

    public void getArrangementDetails(Contract contract) {
        String product = "";
        try {
            AaArrangementRecord arrRec = contract.getContract();
            loanNumber = arrId;
            customerNumber = arrRec.getCustomer().get(0).getCustomer().getValue();
            loanArrStatus = arrRec.getArrStatus().getValue();
            arrStDt = arrRec.getStartDate().getValue();
            linkedAppList = arrRec.getLinkedAppl();
            for (LinkedApplClass linkedApp : linkedAppList) {
                if (linkedApp.getLinkedAppl().getValue().equals(ACCOUNT)) {
                    acctId = linkedApp.getLinkedApplId().getValue();

                }
            }

            getAccountDetails(acctId);
            product = arrRec.getProduct().get(0).getProduct().getValue();
            getAaProductDetails(product);
            if (product.equals("FF.INCOME.GEN.LOAN")) {
                categoryOfLoan = "Group Loan";
            } else {
                categoryOfLoan = "Individual Loan";
            }
            productCode = arrRec.getProductGroup().getValue();
            arrCompId = arrRec.getCoCodeRec().getValue();
            if (arrRec.getOrigContractDate().getValue() != null && !arrRec.getOrigContractDate().getValue().isEmpty()) {
                migratedContractFlg = true;
                disbursementDate = arrRec.getOrigContractDate().getValue();

            } else {
                disbursementDate = arrRec.getStartDate().getValue();
            }
            approvalDate = getAaArrAccount(contract);
            if (approvalDate == null || approvalDate.trim().isEmpty()) {
                approvalDate = disbursementDate;
            }
        } catch (Exception e) {
            e.getMessage();
        }

    }

    private String getAaArrAccount(Contract contract) {
        String date = "";
        try {

            AaPrdDesAccountRecord aaPrdDes = new AaPrdDesAccountRecord(contract.getConditionForProperty(ACCOUNT));
            date = aaPrdDes.getLocalRefField("FF.SANC.DATE").getValue();

        } catch (Exception e) {
            e.getMessage();
        }
        return date;
    }

    private void getAccountDetails(String acctId) {

        try {
            AccountRecord accRec = new AccountRecord(da.getRecord(finMnemonic, ACCOUNT, "", acctId));
            centerCode = accRec.getLocalRefField("FF.CENTRE").getValue();
            if (centerCode != null && !centerCode.isEmpty()) {
                getEbFfCentreDetails(centerCode);
            }
            groupCode = accRec.getLocalRefField("FF.GROUP").getValue();
            if (groupCode != null && !groupCode.isEmpty()) {

                getEbFfGroupDetails(groupCode);
            }
            purpose = (accRec.getLocalRefField("FF.LOAN.PURP").getValue() != null)
                    ? accRec.getLocalRefField("FF.LOAN.PURP").getValue()
                    : "";
            subPurpose = (accRec.getLocalRefField("FF.LOAN.SUBPUR").getValue() != null)
                    ? accRec.getLocalRefField("FF.LOAN.SUBPUR").getValue()
                    : "";
            getAccountLocalField(accRec);
            getAccountLocalField2(accRec);
        } catch (Exception e) {
            e.getMessage();
        }

    }

    private void getAccountLocalField2(AccountRecord accRec) {

        try {
            gramPanchayat = (accRec.getLocalRefField("FF.PANCHAYAT").getValue() != null)
                    ? accRec.getLocalRefField("FF.PANCHAYAT").getValue()
                    : "";

            fundSource = (accRec.getLocalRefField("FF.FUNDER.NAME").getValue() != null)
                    ? accRec.getLocalRefField("FF.FUNDER.NAME").getValue()
                    : "";

            businessType = (accRec.getLocalRefField("FF.BIZ.TYPE").getValue() != null)
                    ? accRec.getLocalRefField("FF.BIZ.TYPE").getValue()
                    : "";

        } catch (Exception e) {
            e.getMessage();

        }

    }

    private void getAccountLocalField(AccountRecord accRec) {
        try {

            cycleNumber = (accRec.getLocalRefField("FF.LOAN.CYCLE").getValue() != null)
                    ? accRec.getLocalRefField("FF.LOAN.CYCLE").getValue()
                    : "";
            loanStatus = (accRec.getLocalRefField("FF.LOAN.STATUS").getValue() != null)
                    ? accRec.getLocalRefField("FF.LOAN.STATUS").getValue()
                    : "";

        } catch (Exception e) {
            e.getMessage();

        }
    }

    public void getEbFfCentreDetails(String centerCode) {
        try {
            EbFfCentreDetailRecord ffcentreDetsRec = new EbFfCentreDetailRecord(
                    da.getRecord("EB.FF.CENTRE.DETAIL", centerCode));
            if (!ffcentreDetsRec.toString().isEmpty()) {
                centerName = ffcentreDetsRec.getCenterName().getValue();
                centerMeetingDay = ffcentreDetsRec.getFfMeetDay().getValue();
                meetingTime = ffcentreDetsRec.getFfMeetingTime().getValue();
                officerEmployeeNumber = ffcentreDetsRec.getCurrentRo().getValue();
                getEbFfRoDetails(officerEmployeeNumber);

            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void getEbFfGroupDetails(String groupCode2) {
        try {
            EbFfGroupsRecord ffgrpDetsRec = new EbFfGroupsRecord(da.getRecord("EB.FF.GROUPS", groupCode2));
            if (!ffgrpDetsRec.toString().isEmpty()) {
                groupName = ffgrpDetsRec.getGroupName().getValue();

            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void getEbFfRoDetails(String officerEmployeeNumber2) {
        try {
            EbFfRoUserRecord ffRoDetsRec = new EbFfRoUserRecord(da.getRecord("EB.FF.RO.USER", officerEmployeeNumber2));
            if (!ffRoDetsRec.toString().isEmpty()) {
                officerName = ffRoDetsRec.getRoName().getValue();

            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    private void getAaProductDetails(String product) {
        try {
            AaProductRecord aaProRec = new AaProductRecord(da.getRecord("AA.PRODUCT", product));
            productName = aaProRec.getDescription(0).getValue();
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void getCustomerDetails(String customerNumber) {
        StringBuilder memName = new StringBuilder();
        try {
            CustomerRecord cusRec = new CustomerRecord(da.getRecord(mnemonic, CUSTOMER, "", customerNumber));
            if (migratedContractFlg) {
                custNumber = cusRec.getMnemonic().getValue();
            } else {
                custNumber = customerNumber;
            }

            TField name1Field = (cusRec.getName1() != null && !cusRec.getName1().isEmpty()) ? cusRec.getName1().get(0)
                    : null;

            TField name2Field = (cusRec.getName2() != null && !cusRec.getName2().isEmpty()) ? cusRec.getName2().get(0)
                    : null;

            List<String> cusNameVal = Arrays.asList(checkFiled(name1Field), checkFiled(name2Field),
                    checkFiled(cusRec.getFamilyName()));
            for (String cusNameValList : cusNameVal) {
                appendIfNotEmpty(memName, cusNameValList);
            }
            memberName = memName.toString();

            dateOfBirth = cusRec.getDateOfBirth().getValue();
            gender = cusRec.getGender().getValue();
            religion = cusRec.getLocalRefField("FF.RELIG.GROUP").getValue();
            caste = cusRec.getLocalRefField("FF.CASTE").getValue();
            salesOfficerName = cusRec.getAccountOfficer().getValue();
            salesOfficerCode = cusRec.getAccountOfficer().getValue();
            if (cusRec.getPhone1() != null && !cusRec.getPhone1().isEmpty()) {
                Phone1Class phoneList = cusRec.getPhone1().get(0);
                residencePhoneNumber = checkFiled(phoneList.getPhone1());
                mobileNumber = checkFiled(phoneList.getSms1());
            }

            String landHld = cusRec.getLocalRefField("FF.LAND.HOLD").getValue();
            if (landHld != null && !landHld.isEmpty()) {
                landHoldingAcres = getLandHoldDets(Double.parseDouble(landHld));
            }

        } catch (Exception e) {
            e.getMessage();
        }
    }

    public String checkFiled(TField field) {
        try {
            return (field != null) ? field.getValue() : null;
        } catch (Exception e) {
            return null;
        }
    }

    public void appendIfNotEmpty(StringBuilder valName, String value) {
        if (value != null && !value.isEmpty()) {
            if (valName.length() > 0) {
                valName.append(" ");
            }
            valName.append(value);
        }
    }

    private String checkValue(String value) {
        if (value == null) {
            return "";
        }
        value = value.replace(",", " ");
        return value;
    }

    public void getAddressDetails(EbFfLoanDetailsRecord ffLoanDetsRec) {
        FfRptGenForInstallmentRep.info("getAddressDetails METHOD :");
        StringBuilder address = new StringBuilder();
        try {
            if (ffLoanDetsRec.getAddressType() != null && !ffLoanDetsRec.getAddressType().isEmpty()) {

                FfRptGenForInstallmentRep.info("getAddressDetails METHOD INSIDE if :");
                AddressTypeClass addrList = null;
                addrList = addressdet(ffLoanDetsRec, addrList);

                FfRptGenForInstallmentRep.info("getAddressDetails METHOD addrList :" + addrList);

                if (addrList != null) {
                    FfRptGenForInstallmentRep.info("getAddressDetails  addrList != null  :" + addrList);

                    village = checkValue(addrList.getVillageName().getValue());
                    district = checkValue(addrList.getDistrictName().getValue());
                    state = checkValue(addrList.getStateName().getValue());
                    pincode = checkValue(addrList.getPincode().getValue());

                    List<String> addrVal = Arrays.asList(checkValue(checkFiled(addrList.getAddress1())),
                            checkValue(checkFiled(addrList.getAddress2())), checkValue(checkFiled(addrList.getCity())),
                            checkValue(checkFiled(addrList.getVillageName())),
                            checkValue(checkFiled(addrList.getDistrictName())),
                            checkValue(checkFiled(addrList.getStateName())),

                            checkValue(checkFiled(addrList.getLandmark())));

                    for (String addrValList : addrVal) {
                        appendIfNotEmpty(address, addrValList);
                    }
                }
            }
            residenceAddress = address.toString();
        } catch (Exception e) {
            e.getMessage();
        }
    }

    private AddressTypeClass addressdet(EbFfLoanDetailsRecord ffLoanDetsRec, AddressTypeClass addrList) {
        try {
            for (int i = 0; i < ffLoanDetsRec.getAddressType().size(); i++) {
                AddressTypeClass addr = ffLoanDetsRec.getAddressType().get(i);
                if (i < ffLoanDetsRec.getFmEntityNumber().size()) {

                    if ("CURRENT".equalsIgnoreCase(checkValue(addr.getAddressType().getValue()))) {

                        FfRptGenForInstallmentRep
                                .info("private AddressTypeClass addressdet METHOD inside if-  addr :" + addr);
                        addrList = addr;

                        FfRptGenForInstallmentRep
                                .info("private AddressTypeClass addressdet METHOD inside addrList :" + addrList);

                        break;

                    }

                }

            }

            FfRptGenForInstallmentRep.info("private AddressTypeClass addressdet   :" + addrList);

        } catch (Exception e) {
            e.getMessage();
        }
        return addrList;
    }

    public String getLandHoldDets(double landHold) {
        try {
            if (landHold == 0) {
                return "No Land";
            } else if (landHold > 0 && landHold <= 2.5) {
                return "Marginal";
            } else if (landHold > 2.5 && landHold <= 5) {
                return "Small Farmer";
            } else if (landHold > 5) {
                return "Others";
            }

        } catch (Exception e) {
            e.getMessage();
        }
        return "";
    }

    public void getEbFfLoanDetails(String arrId, String finMnemonic) {
        try {
            EbFfLoanDetailsRecord ffLoanDetsRec = new EbFfLoanDetailsRecord(
                    da.getRecord(finMnemonic, "EB.FF.LOAN.DETAILS", "", arrId));

            if (ffLoanDetsRec.toString().isEmpty()) {
                return;
            }

            processFmEntities(ffLoanDetsRec);
            getAddressDetails(ffLoanDetsRec);
            processDocEntities(ffLoanDetsRec);
            getFamilyIncDet(ffLoanDetsRec);
            // setApprovalDate(ffLoanDetsRec);

        } catch (Exception e) {
            e.getMessage();
        }
    }

    private void processFmEntities(EbFfLoanDetailsRecord ffLoanDetsRec) {
        try {
            for (int i = 0; i < ffLoanDetsRec.getFmEntityNumber().size(); i++) {
                FmEntityNumberClass entity = ffLoanDetsRec.getFmEntityNumber().get(i);
                relation = entity.getRelation().getValue();
                isGuarantor = entity.getIsGuarantor().getValue();

                if (relation.equalsIgnoreCase("SELF")) {
                    handleSelfEntity(ffLoanDetsRec, i, entity);
                    occupation = entity.getOccupation().getValue();
                } else if (relation.equalsIgnoreCase("HUSBAND")) {
                    spouseName = entity.getLegalName().getValue();
                }

                if (isGuarantor.equalsIgnoreCase("YES")) {
                    guarantorName = entity.getLegalName().getValue();
                    guarantorRelation = entity.getRelation().getValue();
                    guarantorDateOfBirth = entity.getDateOfBirth().getValue();
                }
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    private void handleSelfEntity(EbFfLoanDetailsRecord rec, int index, FmEntityNumberClass entity) {
        try {

//            if (index < rec.getInEntityNumber().size()) {
//                InEntityNumberClass incomeEntity = rec.getInEntityNumber().get(index);
//                String incomefreq = incomeEntity.getIncomeFrequency().getValue();
//                String incomeperPeriod = incomeEntity.getIncomePerPeriod().getValue();
//                if (incomefreq != null && !incomefreq.isEmpty()) {
//                    grossIncome = String.format("%.2f",
//                            getAnnualIncome(incomefreq, Double.parseDouble(incomeperPeriod)));
//                }
//            }
        } catch (NumberFormatException e) {
            e.getMessage();
        }
    }

    private void getFamilyIncDet(EbFfLoanDetailsRecord ebLoanDetRec) {

        try {

            double totalMonthlyIncome = 0.0;

            List<FmEntityNumberClass> fmEntityNumList = ebLoanDetRec.getFmEntityNumber();

            if (fmEntityNumList == null)

                return;

            for (FmEntityNumberClass fmEntityNum : fmEntityNumList) {

                if (fmEntityNum == null || fmEntityNum.getIsEligibleHouseholdMember() == null

                        || fmEntityNum.getIsEligibleHouseholdMember().getValue() == null

                        || !"YES".equalsIgnoreCase(fmEntityNum.getIsEligibleHouseholdMember().getValue())

                        || fmEntityNum.getFmEntityNumber() == null

                        || fmEntityNum.getFmEntityNumber().getValue() == null) {

                    continue;

                }
                if ("YES".equalsIgnoreCase(fmEntityNum.getIsEligibleHouseholdMember().getValue())) {
                    String fmEntNum = fmEntityNum.getFmEntityNumber().getValue();

                    List<InEntityNumberClass> incomeList = ebLoanDetRec.getInEntityNumber();

                    totalMonthlyIncome = getTotalInc(totalMonthlyIncome, fmEntNum, incomeList);

                }

            }

            grossIncome = String.valueOf((int) Math.round(totalMonthlyIncome));

        } catch (Exception e) {
            e.getMessage();
        }

    }

    public double getTotalInc(double totalMonthlyIncome, String fmEntNum, List<InEntityNumberClass> incomeList) {

        try {

            if (incomeList != null) {

                for (InEntityNumberClass incomeRecord : incomeList) {

                    if (incomeRecord == null || incomeRecord.getInEntityNumber() == null

                            || incomeRecord.getInEntityNumber().getValue() == null

                            || !fmEntNum.equals(incomeRecord.getInEntityNumber().getValue())) {

                        continue;

                    }

                    totalMonthlyIncome += convertToMonthly(

                            incomeRecord.getIncomePerPeriod() != null ? incomeRecord.getIncomePerPeriod().getValue()

                                    : null,

                            incomeRecord.getIncomeFrequency() != null ? incomeRecord.getIncomeFrequency().getValue()

                                    : null);

                }

            }

        } catch (Exception e) {
            e.getMessage();

        }

        return totalMonthlyIncome;

    }

    private double convertToMonthly(String amountValue, String frequency) {

        if (amountValue == null || amountValue.trim().isEmpty()) {

            return 0.0;

        }

        double amount;

        try {

            amount = Double.parseDouble(amountValue.trim().replace(",", ""));

        } catch (NumberFormatException e) {

            return 0.0;

        }

        if (frequency == null) {

            return amount;

        }

        String frequencyUpper = frequency.toUpperCase();

        if (frequencyUpper.contains("ANNUAL"))

            return amount / 12;

        if (frequencyUpper.contains("HALF"))

            return amount / 6;

        if (frequencyUpper.contains("QUARTER"))

            return amount / 3;

        return amount;

    }

    private void processDocEntities(EbFfLoanDetailsRecord rec) {
        try {

            String selfEntityNumber = null;

            for (FmEntityNumberClass fm : rec.getFmEntityNumber()) {
                if ("SELF".equalsIgnoreCase(fm.getRelation().getValue())) {
                    selfEntityNumber = fm.getFmEntityNumber().getValue();
                    break;
                }
            }

            if (selfEntityNumber == null) {
                return;
            }

            for (DocEntityNumberClass doc : rec.getDocEntityNumber()) {

                if (!selfEntityNumber.equals(doc.getDocEntityNumber().getValue())) {
                    continue;
                }

                String docType = doc.getDocumentType().getValue().toUpperCase();
                String docNumber = doc.getDocumentNumber().getValue();

                if (docType.contains("AADHAAR")) {
                    aadhaarNumber = docNumber;

                    if (aadhaarNumber != null && aadhaarNumber.length() >= 4) {
                        aadhaarNumber = aadhaarNumber.substring(aadhaarNumber.length() - 4);
                    }

                } else if (docType.contains("VOTER")) {

                    if (voterIdCard == null || voterIdCard.isEmpty()) {
                        voterIdCard = docNumber;
                    }

                } else if (docType.contains("RATION") && (rationCard == null || rationCard.isEmpty())) {
                    rationCard = docNumber;
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void initialiseCompanyInfo(ServiceData serviceData, String companyId) {
        try {
            if (companyId == null || companyId.isEmpty()) {
                companyId = serviceData.getCompanyId();
            }
            CompanyRecord companyObj = new CompanyRecord(da.getRecord("COMPANY", companyId));

            officeName = companyObj.getCompanyName().get(0).getValue();

            finMnemonic = companyObj.getFinancialMne().getValue();
            mnemonic = companyObj.getCustomerMnemonic().getValue();
            officeCode = companyId;
            String[] officeNamePart = officeName.split("-");
            officeName = officeNamePart[0];
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void getEbFfCollectionDets(String arrId) {

        try {
            if (migratedContractFlg) {
                handleMigratedContract(arrId);
            } else {
                handleNonMigratedContract(arrId);
            }

            handleNonMigratedContract(arrId);

            accountNo = loanNumber;
            count = numberOfInstallments;
        } catch (NumberFormatException e) {
            e.getMessage();
        }
    }

    private void handleMigratedContract(String arrId) {

        try {
            String ebFfCollDetsHistRecId = arrId + "-" + arrStDt + ".01";
            EbFfCollectionDetsHistoryRecord ebFfCollDetsHistRec = new EbFfCollectionDetsHistoryRecord(
                    da.getRecord(finMnemonic, "EB.FF.COLLECTION.DETS.HISTORY", "", ebFfCollDetsHistRecId));

            if (ebFfCollDetsHistRec.toString().isEmpty()) {
                return;
            }

            processCollectionDetails(ebFfCollDetsHistRec.getPrincipalAmt(), ebFfCollDetsHistRec.getInterestAmt(),
                    ebFfCollDetsHistRec.getDueDate(), ebFfCollDetsHistRec.getTotalDue(), true);
        } catch (Exception e) {
            e.getMessage();
        }
    }

    private void handleNonMigratedContract(String arrId) {
        try {
            EbFfCollectionDetsRecord ebFfCollDetsRec = new EbFfCollectionDetsRecord(
                    da.getRecord(finMnemonic, "EB.FF.COLLECTION.DETS", "", arrId));

            if (ebFfCollDetsRec.toString().isEmpty()) {
                return;
            }

            processCollectionDetails(ebFfCollDetsRec.getPrincipalAmt(), ebFfCollDetsRec.getInterestAmt(),
                    ebFfCollDetsRec.getDueDate(), ebFfCollDetsRec.getTotalDue(), false);

        } catch (Exception e) {
            e.getMessage();
        }

    }

    private void processCollectionDetails(List<TField> principalAmt, List<TField> interestAmt, List<TField> dueDates,
            List<TField> totalDue, boolean migrated) {
        try {

            if (dueDates == null || dueDates.isEmpty()) {
                return;
            }

            int startIndex = migrated ? 0 : 1;
            for (int i = startIndex; i < dueDates.size(); i++) {
                paymentDetails.add(Arrays.asList(principalAmt.get(i).getValue(), interestAmt.get(i).getValue(),
                        dueDates.get(i).getValue(), totalDue.get(i).getValue()));

            }

            setInstallmentSummary(dueDates, totalDue, migrated);

            completedInstallmentNumber = getCompletedInstallmentNumber(dueDates, migrated);

        } catch (Exception e) {
            e.getMessage();
        }
    }

    private void getInterestAmount(String arrId2) {

        interestAmount = 0.0;

        try {
            EbFfCollectionDetsRecord ebFfCollDetsRec = new EbFfCollectionDetsRecord(
                    da.getRecord(finMnemonic, "EB.FF.COLLECTION.DETS", "", arrId2));
            for (int i = 0; i < ebFfCollDetsRec.getDueDate().size(); i++) {

                // for interest Outstanding

                if (ebFfCollDetsRec.getDueDate(i).getValue().compareTo(todayDate) > 0) {
                    interestAmount += Double.parseDouble(ebFfCollDetsRec.getInterestAmt(i).getValue());

                }
            }
        } catch (Exception e) {
            e.getMessage();
        }

    }

    private void setInstallmentSummary(List<TField> dueDates, List<TField> totalDue, boolean migrated) {
        try {
            int startIndex = migrated ? 0 : 1;

            firstInstallmentDate = dueDates.get(startIndex).getValue();

            lastInstallmentDate = dueDates.get(dueDates.size() - 1).getValue();

            installmentAmountFirstSlab = totalDue.get(migrated ? 1 : 2).getValue();

            numberOfInstallments = String.valueOf(migrated ? dueDates.size() : dueDates.size() - 1);

        } catch (Exception e) {
            e.getMessage();
        }
    }

    private String getCompletedInstallmentNumber(List<TField> dueDates, boolean migrated) {

        try {
            if (dueDates == null || dueDates.isEmpty()) {
                return "0";
            }

            String today = new Session(this).getCurrentVariable("!TODAY");
            int completedCount = 0;

            int startIndex = migrated ? 0 : 1;

            for (int i = startIndex; i < dueDates.size(); i++) {

                String dueDate = dueDates.get(i).getValue();

                if (dueDate != null && !dueDate.isEmpty() && dueDate.compareTo(today) <= 0) {

                    completedCount++;
                }
            }

            return String.valueOf(completedCount);

        } catch (Exception e) {
            return "0";
        }
    }

    public void getEcbDetails(Contract contract) {
        double uncAccount = 0.0;
        double intCredit = 0.0;
        double interestAcc = 0.0;
        double currOd = 0.0;
        double prinOverdue = 0.0;
        double intOverdue = 0.0;
        double prinOutstanding = 0.0;
        double intOutstanding = 0.0;
        double intOutstanding1 = 0.0;
        double accPrInterestCust = 0.0;
        double accPrInterest = 0.0;
        try {

            uncAccount = Double.valueOf(getBalance(contract, "UNCACCOUNT", TRADE));
            parkedAmount = String.format("%.2f", Math.abs(uncAccount));

            unspecifiedCredit = String.format("%.2f", Math.abs(uncAccount));

            intCredit = Double.valueOf(getBalance(contract, "ACCADVPAYREFUND", TRADE));
            interestCredit = String.format("%.2f", Math.abs(intCredit));

            interestAcc = Double.valueOf(getBalance(contract, "FFACCINTEREST", TRADE));
            interestAccrued = String.format("%.2f", Math.abs(interestAcc));

            currOd = Double.valueOf(getBalance(contract, "FFCURODAMT", TRADE));
            currentOd = String.format("%.2f", Math.abs(currOd));

            // new mapping

            prinOverdue = Double.valueOf(getBalance(contract, "FFODPRINAMT", TRADE));
            principalOverdue = String.format("%.2f", Math.abs(prinOverdue));

            intOverdue = Double.valueOf(getBalance(contract, "FFINTERDEF", TRADE));
            interestOverdue = String.format("%.2f", Math.abs(intOverdue));

            prinOutstanding = Double.valueOf(getBalance(contract, "FFPRIOUTAMT", TRADE));
            principalOutstanding = String.format("%.2f", Math.abs(prinOutstanding));

            intOutstanding = Math.abs(Double.valueOf(getBalance(contract, "FFINTODFUTAMT", TRADE)));
            accPrInterest = Math.abs(Double.valueOf(getBalance(contract, "ACCPRINTEREST", TRADE)));
            accPrInterestCust = Math.abs(Double.valueOf(getBalance(contract, "ACCPRINTERESTCUST", TRADE)));

            intOutstanding1 = intOutstanding + interestAmount - accPrInterest - accPrInterestCust;

            interestOutstanding = String.format("%.2f", Math.abs(intOutstanding1));

        } catch (NumberFormatException e) {
            e.getMessage();
        }
    }

    public String getBalance(Contract contract, String accountType, String bookingType) {
        List<BalanceMovement> movements = contract.getContractBalanceMovements(accountType, bookingType);
        return (movements != null && !movements.isEmpty()) ? movements.get(0).getBalance().toString() : "0";
    }

    public void getDpdBalanceDetails(String arrId) {

        LocalDate currDate = LocalDate.parse(todayDate, T24_FORMATTER);
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

                overdueDays = loanDpdRec.getDate().get(size - 1).getCurDpd().getValue();

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

    }

    public void getCusDpd(String customerNumber) {

        try {
            LocalDate currDate = LocalDate.parse(todayDate, T24_FORMATTER);
            String formatted = currDate.getMonth().toString().substring(0, 3) + currDate.getYear();
            String customerDpdId = "CUS" + customerNumber + "-" + formatted;
            EbFfCustDpdRecord ebFfCustDpdRecord = new EbFfCustDpdRecord(
                    da.getRecord(mnemonic, "EB.FF.CUST.DPD", "", customerDpdId));
            int dateListSize = ebFfCustDpdRecord.getDate().size();
            customerDpd = ebFfCustDpdRecord.getDate().get(dateListSize - 1).getCurDpd().getValue();
            customerDpdClassification = getcustomerDpdStatus(
                    ebFfCustDpdRecord.getDate().get(dateListSize - 1).getDpdStatus().getValue());

        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void getAaPrdDesPaymentScheduleDetails(String arrId2, Contract contract) {

        String validValues = "";
        String rawValue = "";
        String installmentFreq = "";

        try {

            try {
                EbFfLoanPaymentHisRecord payHistRec = new EbFfLoanPaymentHisRecord(
                        da.getRecord("", "EB.FF.LOAN.PAYMENT.HIS", "", arrId2));

                installmentFreq = payHistRec.getRepayFreqMatExp().getValue();

            } catch (Exception e) {
                installmentFreq = "";
            }

            if (installmentFreq != null && !installmentFreq.isEmpty()) {

                validValues = parsePaymentFrequency(installmentFreq);
                installmentFrequency = String.join(" ", validValues);

            } else {

                AaPrdDesPaymentScheduleRecord aaPrdPay = new AaPrdDesPaymentScheduleRecord(
                        contract.getConditionForProperty("PAYMENT.SCHEDULE"));

                List<com.temenos.t24.api.records.aaprddespaymentschedule.PaymentTypeClass> paymentTypeList = aaPrdPay
                        .getPaymentType();

                for (com.temenos.t24.api.records.aaprddespaymentschedule.PaymentTypeClass paymentType : paymentTypeList) {

                    if (paymentType.getBillType().getValue().equalsIgnoreCase(INSTALLMENT)
                            && paymentType.getPaymentType().getValue().equalsIgnoreCase("CONSTANT")
                            && paymentType.getPaymentMethod().getValue().equalsIgnoreCase("DUE")) {

                        rawValue = paymentType.getPaymentFreq().getValue();

                        if (rawValue != null && !rawValue.isEmpty()) {

                            validValues = parsePaymentFrequency(rawValue);
                            installmentFrequency = String.join(" ", validValues);

                        }
                    }
                }

            }

        } catch (Exception e) {
            e.getMessage();

        }
    }

    private String parsePaymentFrequency(String rawValue) {

        try {

            years = months = weeks = days = fortnights = 0;
            for (String val : rawValue.split("\\s+")) {
                if (val.startsWith("e")) {
                    processToken(val.substring(1));
                }
            }
        } catch (Exception e) {
            e.getMessage();

        }
        return mapToExpectedFormat();
    }

    private String mapToExpectedFormat() {

        try {
            if ((weeks == 2 && isOthersZero()) || (days == 14 && isOthersZero())) {
                return "FORTNIGHTLY";
            }
            if ((weeks == 4 && isOthersZero()) || (days == 28 && isOthersZero())) {
                return "ONCE EVERY 28 DAYS";
            }
            if (months == 1 && years == 0 && weeks == 0 && days == 0) {
                return "MONTHLY";
            }

        } catch (Exception e) {
            e.getMessage();
        }

        return "OTHERS";
    }

    private void processToken(String cleaned) {

        try {
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
        } catch (Exception e) {
            e.getMessage();
        }
    }

    private boolean isOthersZero() {
        return months == 0 && years == 0 && days == 0;
    }

    public void writeToFile(String finalLine, String filePath) {

        try {
            File file = new File(filePath);
            file.getParentFile().mkdirs();
            boolean fileExists = file.exists();

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true), 65536)) {
                if (!fileExists) {
                    String header = String.join(",", "LoanNumber", "LegacyLoanNumber", "CustomerNumber", "MemberName",
                            "DateOfBirth", "Gender", "Religion", "Caste", "Occupation", "ResidenceAddress", "Village",
                            "GramPanchayat", "VillageLocale", "Block/Municipality", "District", "State", "Pincode",
                            "ResidencePhoneNumber", "MobileNumber", "GrossIncome", "SpouseName", "GuarantorName",
                            "GuarantorRelation", "GuarantorDateOfBirth", "VoterIDCard", "RationCard", "AadhaarNumber",
                            "OfficeName", "OfficeCode", "CenterName", "CenterCode", "CenterFormationDate",
                            "CenterMeetingDay", "MeetingTime", "GroupName", "GroupCode", "OfficerName",
                            "OfficerEmployeeNumber", "Purpose", "LandHolding(Acres)", "SubPurpose", "BusinessType",
                            "ProductName", "ProductInterestRate(%)", "LoanAmount", "ApprovedAmount", "CategoryOfLoan",
                            "CycleNumber", "ApprovalDate", "DisbursementDate", "FirstInstallmentDate",
                            "LastInstallmentDate", "ProcessingFees", "InsuranceCharges", "OtherCharges",
                            "InstallmentAmount(FirstSlab)", "LoanTenure", "NumberOfInstallments",
                            "InstallmentFrequency", "PrincipalOverdue", "TotalInterestCollected", "InterestOverDue",
                            "OverdueDays", "NumberOfInstallments(Paid)", "CompletedInstallmentNumber",
                            "LastPaymentDate", "LastPaymentAmount", "ParkedAmount", "PrincipalOutstanding",
                            "InterestOutstanding", "LoanStatus", "FundSource", "AccountDPDClassification",
                            "CustomerDPD", "CustomerDPDClassification", "CreditInsurancePolicyNumber(Customer)",
                            "CreditInsurancePolicyNumber(Guarantor)", "Count", "UnspecifiedCredit",
                            "InterestAccruedTillDate ", "InterestCreditGivenToCustomerTillDate");

                    int paymentCount = 86;
                    StringBuilder paymentHeaders = new StringBuilder();
                    for (int k = 1; k <= paymentCount; k++) {
                        paymentHeaders.append(",Principal Demand-").append(k);
                        paymentHeaders.append(",Interest Demand-").append(k);
                        paymentHeaders.append(",Demand Date-").append(k);
                        paymentHeaders.append(",Total Scheduled Demand-").append(k);
                    }

                    writer.write(header + paymentHeaders);
                    writer.newLine();
                }
                writer.write(finalLine);
                writer.newLine();
            }

        } catch (Exception e) {
            e.getMessage();
        }
    }
}
