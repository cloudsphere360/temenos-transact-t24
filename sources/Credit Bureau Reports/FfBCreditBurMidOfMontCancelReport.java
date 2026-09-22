package com.temenos.fusion;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.temenos.api.TField;
import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.eb.servicehook.ServiceData;
import com.temenos.t24.api.hook.system.ServiceLifecycle;
import com.temenos.t24.api.records.aaactivityhistory.AaActivityHistoryRecord;
import com.temenos.t24.api.records.aaarrangement.LinkedApplClass;
import com.temenos.t24.api.records.aaarrangementactivity.AaArrangementActivityRecord;
import com.temenos.t24.api.records.aaarrbalancemaintenance.AaArrBalanceMaintenanceRecord;
import com.temenos.t24.api.records.aaarrtermamount.AaArrTermAmountRecord;
import com.temenos.t24.api.records.aaprddespaymentschedule.AaPrdDesPaymentScheduleRecord;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.records.category.CategoryRecord;
import com.temenos.t24.api.records.company.CompanyRecord;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.records.customer.Phone1Class;
import com.temenos.t24.api.records.ebffcentredetail.EbFfCentreDetailRecord;
import com.temenos.t24.api.records.ebffcollectiondets.EbFfCollectionDetsRecord;
import com.temenos.t24.api.records.ebffcollectiondetshistory.EbFfCollectionDetsHistoryRecord;
import com.temenos.t24.api.records.ebffcustomerdetails.EbFfCustomerDetailsRecord;
import com.temenos.t24.api.records.ebffloanactivity.EbFfLoanActivityRecord;
import com.temenos.t24.api.records.ebffloandetails.AddressTypeClass;
import com.temenos.t24.api.records.ebffloandetails.DocEntityNumberClass;
import com.temenos.t24.api.records.ebffloandetails.EbFfLoanDetailsRecord;
import com.temenos.t24.api.records.ebffloandetails.ExEntityNuberClass;
import com.temenos.t24.api.records.ebffloandetails.FmEntityNumberClass;
import com.temenos.t24.api.records.ebffloandetails.InEntityNumberClass;
import com.temenos.t24.api.records.ebffloanpaymenthis.EbFfLoanPaymentHisRecord;
import com.temenos.t24.api.records.ebffnpawriteoffmig.EbFfNpaWriteoffMigRecord;
import com.temenos.t24.api.records.ebffparameter.EbFfParameterRecord;
import com.temenos.t24.api.records.ebffparameter.MatchingItemClass;
import com.temenos.t24.api.records.ebffparameter.ParamDescClass;
import com.temenos.t24.api.records.ebffrouser.EbFfRoUserRecord;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/*-----------------------------------------------------------------------------
* @author Deepakumar S
* Date Created: 18-06-2025
* Attached as : Verification job in 
* EB.API>FF.B.CB.MOM.CANCEL.REPORT
* EB.API>FF.B.CB.MOM.CANCEL.REPORT.SELECT
* PGM.FILE>FF.B.CB.MOM.CANCEL.REPORT
* BATCH>MFI/FF.CREDIT.BUREAU.MID.REPORT
* BATCH>BNK/FF.CREDIT.BUREAU.MID.REPORT
* TSA.SERVICE>BNK/FF.CREDIT.BUREAU.MID.REPORT
* TSA.SERVICE>MFI/FF.CREDIT.BUREAU.MID.REPORT
* EB.FF.PARAMETER>FF.CBR.REPORT.PATH        --->For path
* 
* Description: Extract the CRB Mid Month Cancel Report for the Required field mapping
*-----------------------------------------------------------------------------*/
public class FfBCreditBurMidOfMontCancelReport extends ServiceLifecycle {
    DataAccess da = new DataAccess(this);
    Session ss = new Session(this);
    String todayDate = ss.getCurrentVariable("!TODAY");
    public static final String YYYYMMDD = "yyyyMMdd";
    public static final String DDMMYYYY = "ddMMyyyy";
    private static final DateTimeFormatter FORMA = DateTimeFormatter.ofPattern(YYYYMMDD);
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(YYYYMMDD);

    String aaArrRec = null;
    EbFfCustomerDetailsRecord cusDetRec = null;
    EbFfLoanDetailsRecord ebLoanDetRec = null;

    CategoryRecord catRec = null;
    List<String> arrList = null;

    String finMnemonic = "";
    String mnemonic = "";
    String segmentIdentifier = "";
    String arrId = "";
    String coCode = "";
    String center = "";
    String memberName1 = "";
    String groupIdentifier = "";
    String memberName2 = "";
    String memberName3 = "";
    String alternateNameofMember = "";
    String dateOfBirth = "";
    int customerAge = 0;
    int memberAgeCus = 0;
    String gender = "";
    String maritalStatusType = "";
    String voterId = "";
    String aadharId = "";
    String pan = "";
    String rationCard = "";
    String uniqueAccountReferenceNumber = "";
    String categoryId = "";
    String accType = "";
    String emailId = "";
    String permanentAddress = "";
    String stateCode = "";
    String pinCode = "";
    String currentAddress = "";
    String currentStateCode = "";
    String currentPinCode = "";
    String bnkSavingsAccNum = "";
    String occupation = "";
    String monthlyFamilyExpenses = "";
    String memberCaste = "";
    String groupLeaderIndicator = "";
    String centerLeaderIndicator = "";
    String keyPersonsname = "";
    String keyPersonsrelationship = "";
    String memberRelationshipName1 = "";
    String memberRelationshipType1 = "";
    String memberRelationshipName2 = "";
    String memberRelationshipType2 = "";
    String memberRelationshipName3 = "";
    String memberRelationshipType3 = "";
    String memberRelationshipName4 = "";
    String memberRelationshipType4 = "";
    String nomineeName = "";
    String nomineerelationship = "";
    String nomineeAge = "";
    String memberOtherID1Typedescription = "";
    String memberOtherID1 = "";
    String memberOtherID2Typedescription = "";
    String memberOtherID2 = "";
    String memberOtherID3Typedescription = "";
    String memberOtherID3 = "";
    String povertyIndex = "";
    String assetOwnershipIndicator = "";
    int numberOfDependents = 0;
    String bankAccountAndBankName = "";
    String bankAccountAndBranchName = "";
    String dummy1 = "";
    String dummy2 = "";
    List<ParamDescClass> paramDescList = null;
    String paraDesc = "";
    String paramPath = "";
    String headerPart = "";
    String footerPart = "";
    String primaryOfficer = "";
    String loanPurp = "";
    String loanCycle = "";
    String accountStatus = "S15";
    String dateClosed = "";
    String applicationDate = "";
    String sanctionedDate = "";
    String repaymentFrequency = "";
    String startDateAccDet = "";
    String dateOpendDisbursed = "";
    AaArrTermAmountRecord aaArrTermAmt = null;
    String loanAmount = "";
    String dueDate = "";
    String dateOfAccountInformation = "";
    String loanCategory = "";
    int currentBalance = 0;
    int amountOverDue = 0;
    String noOfMeetingHeld = "";
    String noOfMeetingsMissed = "";
    String typeOfInsurance = "";
    String sumAssuredCoverage = "";
    int arrangementCnt = 0;
    int arrRecCntFromProcess = 0;
    String cusgender = "";
    String cusMaritalStatusType = "";
    String phoneNumber = "";
    String sms = "";
    String telephoneNumber1typeIndicator = "";
    String memberTelephoneNumber1 = "";
    String baSBAccountNumber = "";
    String membersReligion = "";
    String companyName = "";
    String creditValueDate = "";
    String intallmentAmount = "";
    int curDpd = 0;
    double writeOffAmount = 0.0;
    String dateWriteOff = "";
    String writeoffReason = "";
    String telephoneNumber2typeIndicator = "";
    String memberTelephoneNumber2 = "";
    String agreedMeetingDayOfTheWeek = "";
    String agreedMeetingTimeOfTheDay = "";
    EbFfParameterRecord paraRec = null;
    double mainIncome = 0.0;
    String totalFamilymainIncome = "";
    String companyId = "";
    String cusDateOfBirth;
    int memberAge = 0;
    boolean relation = false;
    String loanOfficerOriginatingTheLoan = "";
    String memberIdentifier = "";
    String arrDateClosed = "";
    String memberAgeOntheDate = "";
    String addressStateName = "";
    String arrCoCode = "";
    String txnType = "";
    String relat = "";
    int numberofInstallments = 0;
    String loanStatus = "";
    String unpaidBillDate = "";
    List<String> billIdList = new ArrayList<>();
    String totalInterestCollected = "";
    String numberOfInstallmentsPaid = "";
    String completedInstallmentNumber = "";
    List<List<String>> paymentDetails = new ArrayList<>();
    String firstInstallmentDate = "";
    String firstInstallmentDat = "";
    String installmentAmountFirstSlab = "";
    String lastInstallmentDate = "";
    String lastInstallmentDat = "";
    String writeOffaccPrinterest = "";
    String writeOffcurrAccount = "";
    AaArrBalanceMaintenanceRecord aaArrBalMainRec;
    String fmEntityNumber = "";
    String fmRelation = "";
    String docEntiryNumber = "";
    String address = "";
    String villageName = "";
    String districtName = "";
    String landMark = "";
    String addressType = "";
    String tehsil = "";
    String city = "";
    String address2 = "";
    String bankAccountStatement = "BANK";
    String billDate = "";
    String closDate = "";
    String dueDates = "";
    String transDate = "";
    double demandAmt = 0.0;
    String demandAmount = "";
    String dueDate2 = "";
    String acNum = "";
    String relationshipOfficerName = "";
    String orgWriteOffDt = "";
    String orgWriteOffAmt = "";
    int realWriteOffAmount = 0;
    String orgWriteOffAmount = "";
    String narrative = "";
    String transacType = "";
    String datelastpayment = "NO";
    String strtDate = "";
    String arrStatus = "";
    String agestatus = "";
    String ffLoanStatus = "";
    String ftId = "";
    String transType = "";
    int years = 0;
    int months = 0;
    int weeks = 0;
    int days = 0;
    int fortnights = 0;
    String cancelDisbursedAmount = "";
    String cancelAccountNo = "";
    String cancelRepaymentFrequency = "";
    String cancelArrId = "";
    String isEligibleHouseholdMember = "";
    String cancelcustomerId = "";
    String cancelOpeningDate = "";
    String ffSancDate = "";
    String updateDate = "";
    String memberStatus="";
    LocalDate todate1 = null;
    AaArrangementActivityRecord aaaRec = null;
    AaPrdDesPaymentScheduleRecord aaArrPaySchRec = null;
    FundsTransferRecord ftRec = null;
    List<LinkedApplClass> linkedAppList = null;
    EbFfNpaWriteoffMigRecord npaWriteOffMigRec = null;
    EbFfLoanPaymentHisRecord ffLoanPayHistRec = null;
    AaActivityHistoryRecord aaActHistRec = null;
    EbFfCollectionDetsRecord ebffCollection = null;
    EbFfCollectionDetsHistoryRecord histRec = null;
    List<String> loanActivityRecList = null;
    EbFfLoanActivityRecord loanActivityRec = null;
    public static final String MONTHLY = "MONTHLY";
    public static final String TRADE = "TRADE";
    public static final String EBFFCOLLECTIONDETSHISTORY = "EB.FF.COLLECTION.DETS.HISTORY";
    public static final String ANNUALLY = "ANNUALLY";
    public static final String AAARRANGEMENT = "AA.ARRANGEMENT";
    public static final String EB_FF_PARAMETER = "EB.FF.PARAMETER";
    public static final String FUNDS_TRANSFER = "FUNDS.TRANSFER";
    public static final String ACCOUNT = "ACCOUNT";
    public static final String EBFFCOLLECTIONDETS = "EB.FF.COLLECTION.DETS";
    public static final String CURRENT = "CURRENT";
    public static final String FORTNIGHTLY = "F02";
    public static final String F03 = "F03";
    public static final String OTHER = "F10";
    String residence = "Residence";
    String reportedDate = "";
    String monthEndDate = "";
    private static final int[] FIELD_LENGTHS = { 6, 35, 30, 30, 20, 100, 50, 50, 30, 8, 3, 8, 1, 3, 100, 3, 100, 3, 100,
            3, 100, 3, 100, 3, 100, 3, 3, 20, 40, 15, 20, 20, 30, 20, 30, 20, 30, 3, 15, 3, 15, 20, 1, 2, 50, 50, 35,
            50, 9, 9, 3, 30, 1, 1, 70, 6, 200, 2, 10, 200, 2, 10, 30, 6, 35, 35, 30, 30, 30, 8, 3, 20, 30, 20, 3, 8, 8,
            8, 8, 8, 9, 9, 9, 3, 3, 9, 9, 9, 3, 9, 8, 20, 3, 3, 1, 3, 10, 3, 5, 30, 5 };

    private String formatField(String value, int length) {
        if (value == null)
            value = "";

        if (value.length() > length) {
            return value.substring(0, length);
        } else {
            return value;
        }
    }

    @Override
    public List<String> getIds(ServiceData serviceData, List<String> controlList) {
        try {
            initialiseCompanyInfo(serviceData);
            loanActivityRecList = da.selectRecords(finMnemonic, "EB.FF.LOAN.ACTIVITY", "",
                    "WITH ACTIVITY.TYPE EQ CANCELLATION");
        } catch (Exception e) {
            e.getMessage();
        }
        return loanActivityRecList;
    }

    private void initialiseCompanyInfo(ServiceData serviceData) {
        try {
            companyId = serviceData.getCompanyId();
            CompanyRecord companyObj = new CompanyRecord(da.getRecord("COMPANY", companyId));
            companyName = companyObj.getCompanyName(0).getValue();
            finMnemonic = companyObj.getFinancialMne().getValue();
            mnemonic = companyObj.getCustomerMnemonic().getValue();
        } catch (Exception e) {
            e.getMessage();
        }
    }

    @Override
    public void process(String id, ServiceData serviceData, String controlItem) {

        LocalDate aaArrStartDate = null;
        try {
            initialiseCompanyInfo(serviceData);
            getEbFfLoanActivity(id);
            getAccountRecord(cancelAccountNo);
            reportedDate = determineReportedDateFull(todayDate);
            Contract contract = new Contract(this);
            contract.setContractId(arrId);

            String arrStartDate = updateDate;
            if (arrStartDate == null || arrStartDate.trim().isEmpty()) {
                return;
            }
            aaArrStartDate = LocalDate.parse(arrStartDate, formatter);
            dateOfAccountInformation = reportedDate;
            todate1 = LocalDate.parse(todayDate, formatter);
            LocalDate today2 = LocalDate.parse(todayDate, formatter);
            LocalDate startDate = today2.minusDays(90);
            if (aaArrStartDate.isBefore(startDate) || aaArrStartDate.equals(todate1)) {
                generatingTheCBRFile(arrId, contract, serviceData);
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    private void getAccountRecord(String cancelAccountNo) {
        try {
            acNum = getSafeValue(cancelAccountNo);
            AccountRecord accRec = new AccountRecord(da.getRecord(finMnemonic, ACCOUNT, "", acNum));
            ffLoanStatus = getSafeValue(accRec.getLocalRefField("FF.LOAN.STATUS").getValue());
            cancelcustomerId = getSafeValue(accRec.getCustomer().getValue());
            cancelOpeningDate = getSafeValue(accRec.getOpeningDate().getValue());
            ffSancDate = getSafeValue(accRec.getLocalRefField("FF.SANC.DATE").getValue());
            coCode = accRec.getCoCode();
            loanCycle = getSafeValue(accRec.getLocalRefField("FF.LOAN.CYCLE").getValue());
            loanPurp = getSafeValue(accRec.getLocalRefField("FF.LOAN.PURP").getValue());
            center = getSafeValue(accRec.getLocalRefField("FF.CENTRE").getValue());
            if (!center.isEmpty()) {
                getEbFfCenterTable(center);
            }
            if (!ffSancDate.isEmpty()) {
                sanctionedDate = ffSancDate;
            } else {
                sanctionedDate = cancelOpeningDate;
            }
            dateOpendDisbursed = cancelOpeningDate;
            if ("T01".equals(loanCategory) || "T02".equals(loanCategory) || "T04".equals(loanCategory)) {
                groupIdentifier = getSafeValue(accRec.getLocalRefField("FF.GROUP").getValue());
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    private String getSafeValue(String value) {
        if (value == null) {
            return "";
        }
        return value;
    }

    public static String determineReportedDateFull(String todayDate) {
        try {
            return LocalDate.parse(todayDate, FORMA).minusDays(1).format(FORMA);
        } catch (Exception e) {
            return todayDate;
        }
    }

    private void generatingTheCBRFile(String arrId, Contract contract, ServiceData serviceData) {
        try {
            initialiseCompanyInfo(serviceData);
            List<String> outvalues = new ArrayList<>();
            segmentIdentifier = "CNSCRD";
            String addressSegmentIdentifier = "ADRCRD";
            String accountSegIndentifier = "ACTCRD";
            String insuranceIndicator = "Y";
            typeOfInsurance = "L02";
            memberAgeOntheDate = updateDate;
            memberIdentifier = cancelcustomerId;

            uniqueAccountReferenceNumber = arrId;
            contract.setContractId(arrId);
            getEbFfCollectionDetsRecord();
            getEbFfCollectionDetsHistoryRecord();

            getTheDetailsFromEbFfLoanDetailsRecord(arrId);
            getTheDetailsFromCustomerRecord(updateDate, cancelcustomerId);
            getEbFfCollectionDetails();
            getInstallmentAmount();

            List<String> row = new ArrayList<>();
            row.add(segmentIdentifier);
            row.add(memberIdentifier);
            row.add(coCode);//
            row.add(center);
            row.add(groupIdentifier);
            row.add(memberName1);
            row.add(memberName2);
            row.add(memberName3);
            row.add(alternateNameofMember);
            row.add(formatDateAsDdMMyyyy(dateOfBirth));
            row.add(String.valueOf(memberAgeCus));
            row.add(formatDateAsDdMMyyyy(memberAgeOntheDate));//
            row.add(gender);
            row.add(maritalStatusType);
            row.add(keyPersonsname);
            row.add(keyPersonsrelationship);
            row.add(memberRelationshipName1);
            row.add(memberRelationshipType1);
            row.add(memberRelationshipName2);
            row.add(memberRelationshipType2);
            row.add(memberRelationshipName3);
            row.add(memberRelationshipType3);
            row.add(memberRelationshipName4);
            row.add(memberRelationshipType4);
            row.add(nomineeName);
            row.add(nomineerelationship);
            row.add(nomineeAge);
            row.add(voterId);
            row.add(aadharId);
            row.add(pan);
            row.add(rationCard);
            row.add(memberOtherID1Typedescription);
            row.add(memberOtherID1);
            row.add(memberOtherID2Typedescription);
            row.add(memberOtherID2);
            row.add(memberOtherID3Typedescription);
            row.add(memberOtherID3);
            row.add(telephoneNumber1typeIndicator);
            row.add(memberTelephoneNumber1);
            row.add(telephoneNumber2typeIndicator);
            row.add(memberTelephoneNumber2);
            row.add(povertyIndex);
            row.add(assetOwnershipIndicator);
            row.add(String.valueOf(numberOfDependents));
            row.add(bankAccountAndBankName);
            row.add(bankAccountAndBranchName);
            row.add(baSBAccountNumber);
            row.add(occupation);
            row.add(totalFamilymainIncome);
            row.add(monthlyFamilyExpenses);
            row.add(membersReligion);
            row.add(memberCaste);
            row.add(groupLeaderIndicator);
            row.add(centerLeaderIndicator);
            row.add(memberStatus);
            row.add(addressSegmentIdentifier);
            row.add(permanentAddress);
            row.add(stateCode);
            row.add(pinCode);
            row.add(emailId);
            row.add(currentAddress);
            row.add(currentStateCode);
            row.add(currentPinCode);
            row.add(dummy1);
            row.add(accountSegIndentifier);
            row.add(uniqueAccountReferenceNumber);//
            row.add(uniqueAccountReferenceNumber);//
            row.add(coCode);//
            row.add(center);
            row.add(relationshipOfficerName);
            row.add(formatDateAsDdMMyyyy(dateOfAccountInformation));
            row.add(loanCategory);
            row.add(groupIdentifier);
            row.add(loanCycle);
            row.add(loanPurp);
            row.add(accountStatus);
            row.add(applicationDate);
            row.add(formatDateAsDdMMyyyy(sanctionedDate));
            row.add(formatDateAsDdMMyyyy(dateOpendDisbursed));
            row.add(formatDateAsDdMMyyyy(arrDateClosed));//
            row.add(formatDateAsDdMMyyyy(creditValueDate));//
            row.add(loanAmount);
            row.add(loanAmount);
            row.add(cancelDisbursedAmount);
            row.add(String.valueOf(numberofInstallments));
            row.add(repaymentFrequency);
            row.add(installmentAmountFirstSlab);
            row.add(String.valueOf(currentBalance));
            row.add(String.valueOf(amountOverDue));
            row.add(String.format("%03d", curDpd));
            row.add(String.valueOf(realWriteOffAmount));
            row.add(dateWriteOff);
            row.add(writeoffReason);
            row.add(noOfMeetingHeld);
            row.add(noOfMeetingsMissed);
            row.add(insuranceIndicator);
            row.add(typeOfInsurance);
            row.add(loanAmount);
            row.add(agreedMeetingDayOfTheWeek);
            row.add(agreedMeetingTimeOfTheDay);
            row.add(dummy2);
            row.add(dummy2);

            List<String> formattedRow = new ArrayList<>();
            for (int i = 0; i < row.size(); i++) {
                formattedRow.add(formatField(row.get(i), FIELD_LENGTHS[i]));
            }
            outvalues.add(String.join("|", formattedRow));
            getEbParameterRecordDetails();
            String outputPath = paramPath + "temp" + "_" + "CBRMidReportExtract" + "_" + finMnemonic + "_"
                    + ss.getSessionNumber() + ".cdf";
            writeToFile(outvalues, outputPath);
        } catch (Exception e) {
            e.getMessage();
        }
    }

    private void getEbFfLoanActivity(String id) {
        try {
            loanActivityRec = new EbFfLoanActivityRecord(da.getRecord(finMnemonic, "EB.FF.LOAN.ACTIVITY", "", id));
            cancelArrId = loanActivityRec.getArrangementId().getValue();
            arrId = cancelArrId;
            cancelRepaymentFrequency = loanActivityRec.getReserve6().getValue();
            cancelAccountNo = loanActivityRec.getReserve8().getValue();
            updateDate = loanActivityRec.getUpdateDate().getValue();
            String reserve7Value = loanActivityRec.getReserve7().getValue();
            if (reserve7Value != null && !reserve7Value.trim().isEmpty()) {
                cancelDisbursedAmount = String.valueOf((int) Math.round(Double.parseDouble(reserve7Value)));
            } else {
                cancelDisbursedAmount = "";
            }
            loanAmount = cancelDisbursedAmount;
            String rawValue = cancelRepaymentFrequency;
            if (rawValue != null && !rawValue.trim().isEmpty()) {
                String validValues = parsePaymentFrequency(rawValue);
                repaymentFrequency = String.join(" ", validValues);
            } else {
                repaymentFrequency = "";
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    private void getEbFfCollectionDetsHistoryRecord() {
        try {
            histRec = new EbFfCollectionDetsHistoryRecord(
                    da.getRecord(finMnemonic, EBFFCOLLECTIONDETSHISTORY, "", arrId + "-" + strtDate + ".01"));
        } catch (Exception e) {
            e.getMessage();
        }
    }

    private void getEbFfCollectionDetsRecord() {
        try {
            ebffCollection = new EbFfCollectionDetsRecord(
                    da.getRecord(finMnemonic, EBFFCOLLECTIONDETS, "", "" + arrId));
            String aaProduct = getSafeValue(ebffCollection.getLosProdDetails().getValue());
            loanCategory = getTheCodeFromEbParameterTable(aaProduct);
        } catch (Exception e) {
            e.getMessage();
        }
    }

    private String parsePaymentFrequency(String rawValue) {
        years = months = weeks = days = fortnights = 0;
        try {
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
        if ((weeks == 2 && isOthersZero()) || (days == 14 && isOthersZero())) {
            return FORTNIGHTLY;
        }
        if ((weeks == 4 && isOthersZero()) || (days == 28 && isOthersZero())) {
            return F03;
        }
        if (months == 1 && years == 0 && weeks == 0 && days == 0) {
            return F03;
        }
        return OTHER;
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
        } catch (NumberFormatException e) {
            e.getMessage();
        }
    }

    private boolean isOthersZero() {
        return months == 0 && years == 0 && days == 0;
    }

    private void getEbFfCollectionDetails() {
        try {
            int size = ebffCollection.getDueDate().size();
            numberofInstallments = (size > 0) ? size - 1 : 0;
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void getInstallmentAmount() {
        try {
            if (ebffCollection.getTotalDue().size() > 1) {
                String result = ebffCollection.getTotalDue().get(2).getValue();
                installmentAmountFirstSlab = result.replace("-", "").split("\\.")[0];
            } else {

                installmentAmountFirstSlab = "0";
            }

            datelastpayment = "NO";
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public static int findGreatestPosition(List<String> ffCollDetsHis) {

        double maxValue = Double.NEGATIVE_INFINITY;
        int maxPosition = 0;
        for (int i = 0; i < ffCollDetsHis.size(); i++) {
            String[] parts = ffCollDetsHis.get(i).split("-");
            double value = Double.parseDouble(parts[1]);
            if (value > maxValue) {
                maxValue = value;
                maxPosition = i;
            }
        }
        return maxPosition;
    }

    private void getTheDetailsFromCustomerRecord(String updateDate, String cancelcustomerId) {

        List<String> phoneNumberList = new ArrayList<>();
        List<String> smsList = new ArrayList<>();

        boolean flage = true;
        boolean flage1 = true;

        telephoneNumber1typeIndicator = "";
        memberTelephoneNumber1 = "";
        telephoneNumber2typeIndicator = "";
        memberTelephoneNumber2 = "";
        emailId = "";

        try {

            CustomerRecord cusRec = new CustomerRecord(da.getRecord(mnemonic, "CUSTOMER", "", cancelcustomerId));
            getCustomerName(cusRec);
            if (cusRec.getDateOfBirth() != null) {
                cusDateOfBirth = getSafeValue(cusRec.getDateOfBirth().getValue());
            } else {
                cusDateOfBirth = "";
            }

            dateOfBirth = cusDateOfBirth;

            getCusDateOfBirth(updateDate, cusDateOfBirth, cusRec);

            List<Phone1Class> phoneNumList = cusRec.getPhone1();

            getPhoneNum(phoneNumberList, smsList, phoneNumList);

            if (!smsList.isEmpty()) {

                String smsNumberType = "Mobile";

                if (smsList.size() >= 2) {

                    telephoneNumber1typeIndicator = getTheCodeFromEbParameterTable(smsNumberType);
                    memberTelephoneNumber1 = smsList.get(0);

                    telephoneNumber2typeIndicator = getTheCodeFromEbParameterTable(smsNumberType);
                    memberTelephoneNumber2 = smsList.get(1);

                    flage1 = false;

                } else {

                    telephoneNumber1typeIndicator = getTheCodeFromEbParameterTable(smsNumberType);
                    memberTelephoneNumber1 = smsList.get(0);

                    flage = false;
                }
            }

            if (flage1) {
                getFlage1(phoneNumberList, flage);
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void getPhoneNum(List<String> phoneNumberList, List<String> smsList, List<Phone1Class> phoneNumList) {
        try {
            if (phoneNumList != null && !phoneNumList.isEmpty()) {

                for (Phone1Class phoneNum : phoneNumList) {
                    if (phoneNum != null) {
                        getPhoneNum(phoneNumberList, smsList, phoneNum);
                    }
                }

                Phone1Class firstPhone = phoneNumList.get(0);
                if (firstPhone != null && firstPhone.getEmail1() != null) {
                    emailId = getSafeValue(firstPhone.getEmail1().getValue());
                }
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void getCustomerName(CustomerRecord cusRec) {
        try {
            String fstName = "";
            String scdName = "";
            String familyName = "";

            for (TField name1 : cusRec.getName1()) {
                fstName = name1.getValue();
            }
            for (TField name2 : cusRec.getName2()) {
                scdName = name2.getValue();
            }
            familyName = cusRec.getFamilyName().getValue();
            memberName1 = String.join(" ", fstName, scdName, familyName).trim().replaceAll("\\s+", " ");
        } catch (Exception e) {
            e.getMessage();
        }
    }

    private void getFlage1(List<String> phoneNumberList, boolean flage) {
        try {
            if ((phoneNumberList != null) && (!phoneNumberList.isEmpty())) {
                if ((phoneNumberList.size() == 2) && (flage)) {
                    String phoneNumberType = residence;
                    telephoneNumber1typeIndicator = getTheCodeFromEbParameterTable(phoneNumberType);
                    memberTelephoneNumber1 = phoneNumberList.get(0);
                    telephoneNumber2typeIndicator = getTheCodeFromEbParameterTable(phoneNumberType);
                    memberTelephoneNumber2 = phoneNumberList.get(1);
                } else {
                    getNotFlage(phoneNumberList, flage);
                }
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    private void getNotFlage(List<String> phoneNumberList, boolean flage) {
        try {
            if (!flage) {
                String phoneNumberType = residence;
                telephoneNumber2typeIndicator = getTheCodeFromEbParameterTable(phoneNumberType);
                memberTelephoneNumber2 = phoneNumberList.get(0);
            } else {
                String phoneNumberType = residence;
                telephoneNumber1typeIndicator = getTheCodeFromEbParameterTable(phoneNumberType);
                memberTelephoneNumber1 = phoneNumberList.get(0);
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    private void getCusDateOfBirth(String cancelOpeningDate, String cusDateOfBirth, CustomerRecord cusRec) {
        try {
            if (!cusDateOfBirth.isEmpty()) {
                customerAge = customerAgeCalculation(cusDateOfBirth);
                if (cancelOpeningDate != null) {
                    memberAgeCus = memberAgeCalculation(cancelOpeningDate, cusDateOfBirth);
                }
            }
            cusgender = cusRec.getGender().getValue();
            gender = getTheCodeFromEbParameterTable(cusgender);
            cusMaritalStatusType = cusRec.getMaritalStatus().getValue();
            if (cusMaritalStatusType.isEmpty()) {
                maritalStatusType = "M06";
            } else {
                maritalStatusType = getTheCodeFromEbParameterTable(cusMaritalStatusType);
            }
            String religGrp = cusRec.getLocalRefField("FF.RELIG.GROUP").getValue();
            membersReligion = getTheCodeFromEbParameterTable(religGrp);
            getmeberCaste(cusRec);
            if (occupation.isEmpty()) {
                String occup1 = cusRec.getLocalRefField("FF.FM.OCCUP").getValue().toUpperCase().trim();
                if (occup1.contains("OTHER")) {
                    occupation = "Z06";
                } else {
                    occupation = getTheCodeFromEbParameterTable(occup1);
                }
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }
    private void getmeberCaste(CustomerRecord cusRec) {
        try {
            String membCaste = cusRec.getLocalRefField("FF.CASTE").getValue().toUpperCase().trim();
            if (membCaste.contains(OTHER)) {
                memberCaste = "V08";
            } else if (membCaste.contains("UNTAGGED")) {
                memberCaste = "V09";
            } else {
                memberCaste = getTheCodeFromEbParameterTable(membCaste);
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }
    private void getPhoneNum(List<String> phoneNumberList, List<String> smsList, Phone1Class phoneNum) {
        try {
            phoneNumber = phoneNum.getPhone1().getValue();
            sms = phoneNum.getSms1().getValue();
            if (!phoneNumber.isEmpty()) {
                phoneNumberList.add(phoneNumber);
            }
            if (!sms.isEmpty()) {
                smsList.add(sms);
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    private int memberAgeCalculation(String updateDate, String dateOfBirth) {
        try {
            if (!dateOfBirth.isEmpty() && (!updateDate.isEmpty())) {
                LocalDate birthDate;
                birthDate = LocalDate.parse(dateOfBirth, formatter);
                LocalDate arrDate = LocalDate.parse(updateDate, formatter);
                memberAge = Period.between(birthDate, arrDate).getYears();
            }
        } catch (Exception e) {
            e.getMessage();
        }
        return memberAge;
    }

    private String formatDateAsDdMMyyyy(String date) {
        try {
            return LocalDate.parse(date, formatter).format(DateTimeFormatter.ofPattern(DDMMYYYY));
        } catch (Exception e) {
            return "";
        }
    }

    private void getEbFfCenterTable(String center) {
        try {
            EbFfCentreDetailRecord centerRec = new EbFfCentreDetailRecord(
                    da.getRecord("", "EB.FF.CENTRE.DETAIL", "", center));
            loanOfficerOriginatingTheLoan = centerRec.getCurrentRo().getValue();
            if (loanOfficerOriginatingTheLoan != null && !loanOfficerOriginatingTheLoan.isEmpty()) {
                getRoName(loanOfficerOriginatingTheLoan);
            }
            String meetingDay = centerRec.getFfMeetDay().getValue();
            agreedMeetingDayOfTheWeek = getTheCodeFromEbParameterTable(meetingDay);
            agreedMeetingTimeOfTheDay = centerRec.getFfMeetingTime().getValue();
        } catch (Exception e) {
            e.getMessage();
        }
    }

    private void getRoName(String loanOfficerOriginatingTheLoan) {
        try {
            EbFfRoUserRecord roRec = new EbFfRoUserRecord(
                    da.getRecord("", "EB.FF.RO.USER", "", loanOfficerOriginatingTheLoan));
            relationshipOfficerName = roRec.getRoName().getValue();
        } catch (Exception e) {
            e.getMessage();
        }
    }

    private String getTheCodeFromEbParameterTable(String value) {
        String code = "";
        try {
            if (!value.isEmpty()) {
                paraRec = new EbFfParameterRecord(
                        da.getRecord("", EB_FF_PARAMETER, "", "FF.CREDIT.BUREAU.REPORT.EXTRACT"));
                List<MatchingItemClass> matchingItemList = paraRec.getMatchingItem();
                for (MatchingItemClass matchingItem : matchingItemList) {
                    if (matchingItem.getMatchingItem().getValue().equalsIgnoreCase(value)) {
                        code = matchingItem.getMatchingValue().getValue();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.getMessage();
        }
        return code;
    }

    private void getTheDetailsFromEbFfLoanDetailsRecord(String arrId) {

        List<String> memberRelationshipName = new ArrayList<>();
        List<String> memberRelationshipType = new ArrayList<>();
        numberOfDependents = 0;
        relation = false;
        keyPersonsrelationship = "";
        try {
            ebLoanDetRec = new EbFfLoanDetailsRecord(da.getRecord(finMnemonic, "EB.FF.LOAN.DETAILS", "", arrId));

            if (!ebLoanDetRec.toString().isEmpty()) {
                List<FmEntityNumberClass> fmEntityNumList1 = ebLoanDetRec.getFmEntityNumber();
                if (fmEntityNumList1 != null && !fmEntityNumList1.isEmpty()) {
                    for (FmEntityNumberClass fmEntityNum : fmEntityNumList1) {
                        fmRelation = fmEntityNum.getRelation().getValue();
                        if (fmRelation != null && fmRelation.toUpperCase().contains("SELF")) {
                            fmEntityNumber = fmEntityNum.getFmEntityNumber().getValue();
                            break;
                        }
                    }
                }
                List<DocEntityNumberClass> docEntityNumList = ebLoanDetRec.getDocEntityNumber();
                if ((!docEntityNumList.isEmpty())) {
                    getDocEntiryNumber(docEntityNumList);
                }
                getMemOtherIdCheck(ebLoanDetRec);
                getFmEntityNumberClass(memberRelationshipName, memberRelationshipType);
                getMemberRelationShipName(memberRelationshipName, memberRelationshipType);
                getAddressTypeClass(ebLoanDetRec);
                getFamilyIncDet(ebLoanDetRec);
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    private void getAddressTypeClass(EbFfLoanDetailsRecord ebLoanDetRec) {
        try {
            List<AddressTypeClass> addTypeList = ebLoanDetRec.getAddressType();
            for (AddressTypeClass addType : addTypeList) {
                addressType = addType.getAddressType().getValue();
                if (CURRENT.equalsIgnoreCase(addressType)) {
                    address = addType.getAddress1().getValue();
                    villageName = addType.getVillageName().getValue();
                    districtName = addType.getDistrictName().getValue();
                    landMark = addType.getLandmark().getValue();
                    city = addType.getCity().getValue();
                    tehsil = addType.getTehsil().getValue();
                    address2 = addType.getAddress2().getValue();
                    permanentAddress = address + " " + address2 + " " + city + " " + tehsil + " " + villageName + " "
                            + districtName + " " + landMark;
                    addressStateName = addType.getStateName().getValue();
                    stateCode = getStateCodeFromParameter(addressStateName);
                    pinCode = addType.getPincode().getValue();
                    currentAddress = permanentAddress;
                    currentStateCode = stateCode;
                    currentPinCode = pinCode;
                    break;
                }
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    private void getFmEntityNumberClass(List<String> memberRelationshipName, List<String> memberRelationshipType) {
        try {
            List<FmEntityNumberClass> fmEntityNumList = ebLoanDetRec.getFmEntityNumber();
            for (FmEntityNumberClass fmEntityNum : fmEntityNumList) {
                isEligibleHouseholdMember = fmEntityNum.getIsEligibleHouseholdMember().getValue();
                String isGuarantor = fmEntityNum.getIsGuarantor().getValue();
                relat = fmEntityNum.getRelation().getValue();
                if (isGuarantor.equalsIgnoreCase("YES")) {
                    keyPersonsname = fmEntityNum.getLegalName().getValue();
                    String keyPersonrelationship = fmEntityNum.getRelation().getValue();
                    keyPersonsrelationship = getTheCodeFromEbParameterTable(keyPersonrelationship);
                    nomineeName = keyPersonsname;
                    nomineerelationship = keyPersonsrelationship;

                    if (keyPersonsrelationship.isEmpty()) {
                        keyPersonsrelationship = "K15";
                        nomineerelationship = "K15";
                    }
                } else if (!"SELF".equalsIgnoreCase(relat)) {
                    memberRelationshipName.add(fmEntityNum.getLegalName().getValue());
                    memberRelationshipType.add(fmEntityNum.getRelation().getValue());

                }
                if (!fmEntityNum.getRelation().getValue().equalsIgnoreCase("SELF")) {
                    numberOfDependents = numberOfDependents + 1;
                } else if (fmEntityNum.getRelation().getValue().equalsIgnoreCase("SELF")) {
                    relation = true;
                    getOccupation(fmEntityNum);
                }
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void getOccupation(FmEntityNumberClass fmEntityNum) {
        try {
            String occup = fmEntityNum.getOccupation().getValue().toUpperCase().trim();
            if (occup.contains("OTHER")) {
                occupation = "Z06";
            } else {
                occupation = getTheCodeFromEbParameterTable(occup);
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    private void getMemberRelationShipName(List<String> memberRelationshipName, List<String> memberRelationshipType) {
        try {
            if ((!memberRelationshipName.isEmpty()) && (!memberRelationshipType.isEmpty())) {
                for (int i = 0; i < memberRelationshipName.size(); i++) {
                    getMemberRelationship(memberRelationshipName, memberRelationshipType, i);
                }
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    private void getMemberRelationship(List<String> memberRelationshipName, List<String> memberRelationshipType,
            int i) {
        try {
            if (i == 0) {
                memberRelationshipName1 = memberRelationshipName.get(0);
                String memberRelationshipTyp1 = memberRelationshipType.get(0);
                memberRelationshipType1 = getTheCodeFromEbParameterTable(memberRelationshipTyp1);
                if (memberRelationshipType1.isEmpty()) {
                    memberRelationshipType1 = "K15";
                }
            } else if (i == 1) {
                memberRelationshipName2 = memberRelationshipName.get(1);
                String memberRelationshipTyp2 = memberRelationshipType.get(1);
                memberRelationshipType2 = getTheCodeFromEbParameterTable(memberRelationshipTyp2);
                if (memberRelationshipType2.isEmpty()) {
                    memberRelationshipType2 = "K15";
                }
            } else if (i == 2) {
                memberRelationshipName3 = memberRelationshipName.get(2);
                String memberRelationshipTyp3 = memberRelationshipType.get(2);
                memberRelationshipType3 = getTheCodeFromEbParameterTable(memberRelationshipTyp3);
                if (memberRelationshipType3.isEmpty()) {
                    memberRelationshipType3 = "K15";
                }
            } else if (i == 3) {
                memberRelationshipName4 = memberRelationshipName.get(3);
                String memberRelationshipTyp4 = memberRelationshipType.get(3);
                memberRelationshipType4 = getTheCodeFromEbParameterTable(memberRelationshipTyp4);
                if (memberRelationshipType4.isEmpty()) {
                    memberRelationshipType4 = "K15";
                }
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    private void getDocEntiryNumber(List<DocEntityNumberClass> docEntityNumList) {
        try {
            for (DocEntityNumberClass docEntityNum : docEntityNumList) {
                docEntiryNumber = docEntityNum.getDocEntityNumber().getValue();
                if (fmEntityNumber != null && fmEntityNumber.equals(docEntiryNumber)) {
                    String docType = docEntityNum.getDocumentType().getValue();
                    if (docType.toUpperCase().contains("VOTER")) {
                        voterId = docEntityNum.getDocumentNumber().getValue();
                    } else if (docType.toUpperCase().contains("AADHAAR")) {
                        aadharId = docEntityNum.getDocumentNumber().getValue();
                    } else if (docType.toUpperCase().contains("PAN")) {
                        pan = docEntityNum.getDocumentNumber().getValue();
                    } else if (docType.toUpperCase().contains("RATION")) {
                        rationCard = docEntityNum.getDocumentNumber().getValue();
                    }
                }
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    private void getMemOtherIdCheck(EbFfLoanDetailsRecord ebLoanDetRec) {
        try {
            List<DocEntityNumberClass> docEntityNumList = ebLoanDetRec.getDocEntityNumber();
            if (docEntityNumList != null && !docEntityNumList.isEmpty()) {
                for (DocEntityNumberClass docEntityNum : docEntityNumList) {
                    docEntiryNumber = docEntityNum.getDocEntityNumber().getValue();
                    if (fmEntityNumber != null && fmEntityNumber.equals(docEntiryNumber)) {
                        String docType = docEntityNum.getDocumentType().getValue();
                        getDocType(docEntityNum, docType);
                    }
                }
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    private void getDocType(DocEntityNumberClass docEntityNum, String docType) {
        try {
            if (docType != null) {
                String docTypeUpper = docType.toUpperCase();
                if (docTypeUpper.contains("G-RAM-G")) {
                    memberOtherID1Typedescription = docEntityNum.getDocumentNumber().getValue();
                } else if (docTypeUpper.contains("CKYC")) {
                    memberOtherID1 = docEntityNum.getDocumentNumber().getValue();
                } else if ((docTypeUpper.contains("LICENSE"))) {
                    memberOtherID2Typedescription = docEntityNum.getDocumentNumber().getValue();
                } else if (docTypeUpper.contains("PASSPORT")) {
                    memberOtherID2 = docEntityNum.getDocumentNumber().getValue();
                }
                if (memberOtherID3Typedescription.toUpperCase().contains(bankAccountStatement)) {
                    memberOtherID3Typedescription = "";
                    memberOtherID3 = "";
                }
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    private String getStateCodeFromParameter(String addressStateName) {
        String code = "";
        try {
            if (!addressStateName.isEmpty()) {
                paraRec = new EbFfParameterRecord(da.getRecord("", EB_FF_PARAMETER, "", "FF.CBR.STATE.CODE"));
                List<MatchingItemClass> matchingItemList = paraRec.getMatchingItem();
                for (MatchingItemClass matchingItem : matchingItemList) {
                    if (matchingItem.getMatchingItem().getValue().equalsIgnoreCase(addressStateName)) {
                        code = matchingItem.getMatchingValue().getValue();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.getMessage();
        }
        return code;
    }

    private void getFamilyIncDet(EbFfLoanDetailsRecord ebLoanDetRec) {
        try {
            double totalMonthlyIncome = 0.0;
            double totalMonthlyExpense = 0.0;
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
                String fmEntNum = fmEntityNum.getFmEntityNumber().getValue();
                List<InEntityNumberClass> incomeList = ebLoanDetRec.getInEntityNumber();
                totalMonthlyIncome = getTotalInc(totalMonthlyIncome, fmEntNum, incomeList);
                List<ExEntityNuberClass> expenseList = ebLoanDetRec.getExEntityNuber();
                if (expenseList != null) {
                    totalMonthlyExpense = getTotalExp(totalMonthlyExpense, fmEntNum, expenseList);
                }
            }
            totalFamilymainIncome = String.valueOf((int) Math.round(totalMonthlyIncome));
            monthlyFamilyExpenses = String.valueOf((int) Math.round(totalMonthlyExpense));
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public double getTotalExp(double totalMonthlyExpense, String fmEntNum, List<ExEntityNuberClass> expenseList) {
        try {
            for (ExEntityNuberClass expenseRecord : expenseList) {
                if (expenseRecord == null || expenseRecord.getExEntityNuber() == null
                        || expenseRecord.getExEntityNuber().getValue() == null
                        || !fmEntNum.equals(expenseRecord.getExEntityNuber().getValue())) {
                    continue;
                }
                totalMonthlyExpense += convertToMonthly(
                        expenseRecord.getExpensePerPeriod() != null ? expenseRecord.getExpensePerPeriod().getValue()
                                : null,
                        expenseRecord.getExpenseFrequency() != null ? expenseRecord.getExpenseFrequency().getValue()
                                : null);
            }
        } catch (Exception e) {
            e.getMessage();
        }
        return totalMonthlyExpense;
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

    private void writeToFile(List<String> outvalues, String outputPath) {
        try {
            File file = new File(outputPath);
            file.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(file, true)) {
                for (String line : outvalues) {
                    writer.write(line + System.lineSeparator());
                }
            }
        } catch (Exception e) {
            e.getMessage();
        }

    }

    private void getEbParameterRecordDetails() {
        try {
            EbFfParameterRecord paramRec = null;
            paramRec = getParamRec(paramRec);
            if (paramRec != null && !paramRec.getParamDesc().isEmpty()) {
                paramDescList = paramRec.getParamDesc();
                for (ParamDescClass paramDesc : paramDescList) {
                    paraDesc = paramDesc.getParamDesc().getValue();
                    if (paraDesc.equals("Custom Path for CBR Reports")) {
                        paramPath = paramDesc.getParamValue().getValue();
                    }
                }
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    private EbFfParameterRecord getParamRec(EbFfParameterRecord paramRec) {
        try {
            paramRec = new EbFfParameterRecord(da.getRecord(EB_FF_PARAMETER, "FF.CBR.REPORT.PATH"));
        } catch (Exception e) {
            e.getMessage();
        }
        return paramRec;
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

}