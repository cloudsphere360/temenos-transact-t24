package com.temenos.fusion;

import java.io.File;
import java.io.FileWriter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;

import java.util.List;
import java.util.Locale;

import com.temenos.api.TField;

import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.eb.servicehook.ServiceData;
import com.temenos.t24.api.hook.system.ServiceLifecycle;
import com.temenos.t24.api.records.aaaccountdetails.AaAccountDetailsRecord;
import com.temenos.t24.api.records.aaaccountdetails.BillIdClass;
import com.temenos.t24.api.records.aaaccountdetails.BillPayDateClass;

import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaarrangement.LinkedApplClass;
import com.temenos.t24.api.records.aabilldetails.AaBillDetailsRecord;

import com.temenos.t24.api.records.aabilldetails.PropertyClass;
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
import com.temenos.t24.api.records.ebffgroups.EbFfGroupsRecord;
import com.temenos.t24.api.records.ebffloandetails.AddressTypeClass;
import com.temenos.t24.api.records.ebffloandetails.EbFfLoanDetailsRecord;
import com.temenos.t24.api.records.ebffloandetails.FmEntityNumberClass;
import com.temenos.t24.api.records.ebffloanpaymenthis.EbFfLoanPaymentHisRecord;
import com.temenos.t24.api.records.ebffloanstatusdetails.EbFfLoanStatusDetailsRecord;
import com.temenos.t24.api.records.ebffparameter.EbFfParameterRecord;
import com.temenos.t24.api.records.ebffparameter.ParamDescClass;
import com.temenos.t24.api.records.ebffrouser.EbFfRoUserRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/**
 *
 * @author DEEPAKUMAR S / SOUVAGYARANJAN Date Created:26-NOV-2025 Attached as
 *         :Service Routine
 * 
 *         JAR NAME: L3clientWiseReport.jar EB.API>FF.B.CUSTOMER.REPORT
 *         EB.API>FF.B.CUSTOMER.REPORT.SELECT PGM.FILE>FF.B.CUSTOMER.REPORT
 *         Attached to BATCH>BNK/FF.B.CUSTOMER.REPORT Description
 *         :CustomerWiseDisbursement Report
 * 
 *         ------------------------------------------------------------------------------
 *         Modification History :
 *         -----------------------------------------------------------------------------
 *         26-NOV-2025 Development Initial Version
 * 
 *         05-MAR-2026 Remapping Jerome
 * 
 *         08-APR-2026 Remapping Jerome
 * 
 *         15-APR-2026 Remapping Jerome
 *         -----------------------------------------------------------------------------
 * 
 * 
 *
 */
public class FfCustomerDisbReport extends ServiceLifecycle {

    private static final String ACCOUNT = "ACCOUNT";
    private static final String REPAID = "REPAID";
    private static final String SETTLE = "SETTLE";
    private static final String FF_INSUR_COMP = "FF.INSUR.COMP";

    private static final String INSURANCEFEE = "INSURANCEFEE";
    private static final String PROCESSINGFEE = "PROCESSINGFEE";
    private static final String AA_BILL_DETAILS = "AA.BILL.DETAILS";
    private static final String AA_ARRANGEMENT = "AA.ARRANGEMENT";

    int years = 0;
    int months = 0;
    int weeks = 0;
    int days = 0;
    int fortnights = 0;

    List<String> arrList = new ArrayList<>();
    List<String> customerPhoneNumList = new ArrayList<>();
    DataAccess da = new DataAccess(this);
    Session ses = new Session();
    String finMnemonic = "";
    String mnemonic = "";
    String officeName = "";
    String officeCode = "";
    String guarantorName = "";
    String guarantorDateOfBirth = "";
    String arrId = "";
    String state = "";
    String branchstate = "";
    int gAge = 0;
    int cAge = 0;
    String compCode = "";
    String compName = "";
    String loanTenure = "";
    String acctNumber = "";
    String customerNumber = "";
    String productName = "";
    String productCode = "";
    String interestRate = "";
    String processFee = "";
    String processFeeFixedAmt = "";
    String insurFee = "";
    String insurFeeFixedAmt = "";
    String billDate = "";
    String payAmt = "";
    String disbursedTime = "";
    String startDate = "";
    String caste = "";
    String ffDisbMode = "";
    String ffLoanCycle = "";
    String ffLoanPurp = "";
    String officerCode = "";
    String primOfficerName = "";
    String todayDate = "";
    String ffCentre = "";
    String ffGrpCode = "";
    String ffRelGrp = "";
    String cusName = "";
    String cusDob = "";
    String cusSms = "";
    String dueDate = "";
    int totInstallments = 0;
    String fee1Unpaid = "";
    String fee2Unpaid = "";
    String custMnemonic = "";
    String bankAccnbr = "";
    EbFfLoanStatusDetailsRecord ebFfLoanStaDetRec = null;
    String repaymentFrequency = "";
    String groupType = "";
    String salesOfficerName = "";
    String salesOfficerCode = "";
    String fee1Name = "";
    String fee1 = "";
    String fee2 = "";
    String bankName = "";
    String insurance2Unpaid = "";
    String altId = "";
    String ffloancat = "";
    String insurance1Name = "";
    String gst1Name = "";
    String gst1 = "";
    String insurunpaid = "";
    String insurance2Name = "";
    String insurance2 = "";
    String legacyAccNo = "";
    int totalNoOfLoans = 0;
    Double orTotal = 0.0;
    String insurance1 = "";
    boolean flag = false;
    boolean legacy = false;
    List<ParamDescClass> paramDescList = new ArrayList<>();
    List<LinkedApplClass> linkedAppList = null;
    String centreName = "";
    String paramPath = "";
    String paraDesc = "";
    boolean paramFlag = false;
    String accNum = "";
    String groupName = "";

    // get Company Details
    public void initialiseCompanyInfo(ServiceData serviceData) {
        try {
            String companyId = serviceData.getCompanyId();
            CompanyRecord companyObj = new CompanyRecord(da.getRecord("COMPANY", companyId));
            finMnemonic = (companyObj.getFinancialMne().getValue() != null) ? companyObj.getFinancialMne().getValue()
                    : "";
            mnemonic = (companyObj.getCustomerMnemonic().getValue() != null)
                    ? companyObj.getCustomerMnemonic().getValue()
                    : "";
            officeName = (companyObj.getCompanyName().get(0).getValue() != null)
                    ? companyObj.getCompanyName().get(0).getValue()
                    : "";
            officeCode = companyObj.getCoCode();

            if (!paramFlag) {

                EbFfParameterRecord ebFfParamRec = new EbFfParameterRecord(
                        da.getRecord("EB.FF.PARAMETER", "FF.COB.REPORT.EXTRACT"));

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

        } catch (

        Exception e) {
            e.getMessage();
        }

    }

    // getIds Reading the AA.AARANGEMENT Table based on condition
    @Override
    public List<String> getIds(ServiceData serviceData, List<String> controlList) {

        try {
            initialiseCompanyInfo(serviceData);

            arrList = da.selectRecords(finMnemonic, AA_ARRANGEMENT, "", "");

        } catch (Exception e) {
            e.getMessage();

        }
        return arrList;
    }

    // Main Method
    @Override
    public void process(String id, ServiceData serviceData, String controlItem) {
        List<String> outvalues = new ArrayList<>();

        try {
            // get Company Details
            initialiseCompanyInfo(serviceData);

            Contract contract = new Contract(this);
            arrId = id;

            contract.setContractId(arrId);

            getArrDets(arrId);
            getAaPrdInterest(contract);
            getAaAccountDetails(arrId);
            getaAArrTermAmount(contract, arrId);
            getDisbursement(contract);

            getAaArrAccount(contract);
            getAaArrAccountLocalField(contract);

            getAaChargeProcessingFee(contract);
            getAaChargeInsuranceFee(contract);
            getAaChargeHospicashFee(contract);

            getAaChargeGstAmt(contract);

            getCustomerDets(arrId);
            getEbLoanDetails(arrId, finMnemonic);
            getEbFfCollectionDets(arrId);

            getAaPrdDesPaymentScheduleDetails(contract, arrId);
            todayDate = ses.getCurrentVariable("!TODAY");

            List<String> row = new ArrayList<>();

            row.add(compName);
            row.add(compCode);
            row.add(branchstate);
            row.add(centreName);
            row.add(ffCentre);
            row.add(groupName);
            row.add(ffGrpCode);
            row.add(cusName);
            if (flag) {
                customerNumber = custMnemonic;
            }
            row.add(customerNumber);
            row.add(state);
            row.add(String.valueOf(cAge));
            row.add(caste);
            row.add(ffRelGrp);
            row.add(cusSms);
            row.add(ffloancat);
            row.add(guarantorName);
            row.add(String.valueOf(gAge));
            row.add(acctNumber);
            row.add(legacyAccNo);
            row.add(primOfficerName);
            row.add(officerCode);
            row.add(bankName);
            row.add(bankAccnbr);
            row.add(productName);
            row.add(loanTenure);
            row.add(ffLoanPurp);
            row.add(interestRate);
            row.add(startDate);
            row.add(disbursedTime);
            row.add(payAmt);
            row.add(ffDisbMode);
            row.add(ffLoanCycle);
            row.add(String.valueOf(totInstallments));
            row.add(repaymentFrequency);
            row.add(dueDate);
            row.add(fee1Name);
            row.add(fee1);
            row.add(fee1Unpaid);
            row.add(insurFee);
            row.add(fee2);
            row.add(fee2Unpaid);
            row.add(insurance1Name);
            row.add(insurance1);
            row.add(insurunpaid);
            row.add(insurance2Name);
            row.add(insurance2);
            row.add(insurance2Unpaid);
            row.add(gst1Name);
            row.add(gst1);

            outvalues.add(String.join(",", row));

            if (!outvalues.isEmpty()) {
                String outputPath = paramPath + "CustomerwiseDisbursement" + "_" + finMnemonic + "_" + todayDate
                        + "_temp_" + ses.getSessionNumber() + ".csv";

                writeToFile(outvalues, outputPath);
            }
        } catch (Exception e) {
            e.getMessage();

        }
    }

    public void getAaPrdDesPaymentScheduleDetails(Contract contract, String aaId) {
        String repay = "";
        String validValues = "";
        String rawValue = "";
        try {
            repay = getRepayFreqMat(aaId);
            if (repay != null && !repay.isEmpty()) {
                validValues = parsePaymentFrequency(repay);
                repaymentFrequency = String.join(" ", validValues);

            } else {

                AaPrdDesPaymentScheduleRecord aaPrdPay = new AaPrdDesPaymentScheduleRecord(
                        contract.getConditionForProperty("PAYMENT.SCHEDULE"));

                List<com.temenos.t24.api.records.aaprddespaymentschedule.PaymentTypeClass> paymentTypeList = aaPrdPay
                        .getPaymentType();
                for (com.temenos.t24.api.records.aaprddespaymentschedule.PaymentTypeClass paymentType : paymentTypeList) {
                    if (paymentType.getBillType().getValue().equalsIgnoreCase("INSTALLMENT")
                            && paymentType.getPaymentType().getValue().equalsIgnoreCase("CONSTANT")
                            && paymentType.getPaymentMethod().getValue().equalsIgnoreCase("DUE")) {
                        rawValue = paymentType.getPaymentFreq().getValue();
                        if (rawValue != null && !rawValue.isEmpty()) {

                            validValues = parsePaymentFrequency(rawValue);

                            repaymentFrequency = String.join(" ", validValues);

                        }
                    }
                }
            }

        } catch (Exception e) {
            e.getMessage();
        }
    }

    private String getRepayFreqMat(String aaId) {
        String repay = "";
        try {
            EbFfLoanPaymentHisRecord ebffloan = new EbFfLoanPaymentHisRecord(
                    da.getRecord("", "EB.FF.LOAN.PAYMENT.HIS", "", aaId));
            repay = ebffloan.getRepayFreqMatExp().getValue();
        } catch (Exception e) {
            repay = "";
        }
        return repay;
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

    // Reading the AA.ARR.TERM.AMOUNT Table
    private void getaAArrTermAmount(Contract contract, String arrId2) {
        try {
            String loanTen = "";
            String tenureUnit = "";

            if (legacy) {

                EbFfLoanPaymentHisRecord ffLoanHis = new EbFfLoanPaymentHisRecord(
                        da.getRecord("", "EB.FF.LOAN.PAYMENT.HIS", "", arrId2));

                loanTen = ffLoanHis.getLoanTenure().getValue();
                tenureUnit = ffLoanHis.getTenureUnit().getValue();

                loanTenure = formatTenure(loanTen, tenureUnit);

            } else {
                AaPrdDesTermAmountRecord aaArrTermAmtRec = new AaPrdDesTermAmountRecord(
                        contract.getConditionForProperty("COMMITMENT"));

                loanTen = aaArrTermAmtRec.getTerm().getValue();
                loanTenure = formatCombinedTenure(loanTen);

            }

        } catch (Exception e) {
            e.getMessage();

        }

    }

    private String formatTenure(String loanTen, String tenureUnit) {
        if (loanTen == null || tenureUnit == null) {
            return "";
        }

        String unitFormatted = "";

        switch (tenureUnit.trim().toLowerCase()) {
        case "weekly":
            unitFormatted = "Weeks";
            break;
        case "monthly":
            unitFormatted = "Months";
            break;
        case "yearly":
        case "annual":
            unitFormatted = "Years";
            break;
        case "daily":
            unitFormatted = "Days";
            break;
        default:
            unitFormatted = tenureUnit;
        }

        return loanTen.trim() + " " + unitFormatted;
    }

    private String getFullForm(String unit) {
        switch (unit) {
        case "D":
            return "Days";
        case "W":
            return "Weeks";
        case "M":
            return "Months";
        case "Y":
            return "Years";
        default:
            return "";
        }
    }

    // Converts a loan tenure value (e.g., Y/M/W/D)
    // Main method to convert tenure
    public String formatCombinedTenure(String term) {
        if (term == null || term.trim().isEmpty())
            return "";

        term = term.trim().toUpperCase();

        String numberPart = term.replaceAll("\\D", "");
        String unitPart = term.replaceAll("\\d", "");

        if (numberPart.isEmpty())
            return "";

        String normalizedUnit = normalizeUnit(unitPart);
        return numberPart + " " + getFullForm(normalizedUnit);
    }

    private String normalizeUnit(String unit) {
        if (unit == null)
            return "";

        unit = unit.trim().toUpperCase();

        switch (unit) {
        case "D":
        case "DAY":
        case "DAYS":
        case "DAILY":
            return "D";

        case "W":
        case "WEEK":
        case "WEEKS":
        case "WEEKLY":
            return "W";

        case "M":
        case "MONTH":
        case "MONTHS":
        case "MONTHLY":
            return "M";

        case "Y":
        case "YEAR":
        case "YEARS":
        case "YEARLY":
        case "ANNUAL":
            return "Y";

        default:
            return "";
        }
    }

    // GET CUSTOMER Details
    public void getCustomerDets(String arrId) {

        try {
            String fstName = "";
            String scdName = "";

            AaArrangementRecord aaRecCus = null;
            CustomerRecord cusRec = null;

            aaRecCus = new AaArrangementRecord(da.getRecord(finMnemonic, AA_ARRANGEMENT, "", arrId));

            customerNumber = (aaRecCus.getCustomer().get(0).getCustomer().getValue() != null)
                    ? aaRecCus.getCustomer().get(0).getCustomer().getValue()
                    : "";

            if (aaRecCus.getOrigContractDate().getValue() != null
                    && !aaRecCus.getOrigContractDate().getValue().isEmpty()) {

                flag = true;

            }

            cusRec = new CustomerRecord(da.getRecord(mnemonic, "CUSTOMER", "", customerNumber));

            custMnemonic = cusRec.getMnemonic().getValue();

            cusName = getCustomerName(fstName, scdName, cusRec);

            cusDob = cusRec.getDateOfBirth().getValue();

            if (cusDob != null && !cusDob.isEmpty()) {
                getCustomerDOB(cusDob);
            }

            getPhoneSms(cusRec);

            caste = (cusRec.getLocalRefField("FF.CASTE") != null) ? cusRec.getLocalRefField("FF.CASTE").getValue() : "";
            ffRelGrp = (cusRec.getLocalRefField("FF.RELIG.GROUP") != null)
                    ? cusRec.getLocalRefField("FF.RELIG.GROUP").getValue()
                    : "";

        } catch (Exception e) {
            e.getMessage();

        }

    }

// Reading the phone and SMS field 
    private void getPhoneSms(CustomerRecord cusRec) {

        String moblie = "";
        String customersmsNum = "";

        try {
            for (Phone1Class phone1 : cusRec.getPhone1()) {
                customersmsNum = phone1.getSms1().getValue();

            }
        } catch (Exception e) {
            e.getMessage();
        }

        try {
            for (Phone1Class mob : cusRec.getPhone1()) {
                moblie = mob.getPhone1().getValue();

            }
        } catch (Exception e) {
            e.getMessage();
        }
        if (!customersmsNum.equals("")) {

            cusSms = customersmsNum;

        } else {

            cusSms = moblie;
        }

    }

    // concat the name1,name2,familyname
    private String getCustomerName(String fstName, String scdName, CustomerRecord cusRec) {
        StringBuilder sb = new StringBuilder();
        try {
            for (TField name1 : cusRec.getName1()) {
                fstName = name1.getValue();
            }

            for (TField name2 : cusRec.getName2()) {
                scdName = name2.getValue();
            }

            String familyName = cusRec.getFamilyName().getValue();

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

    // get customer age
    private void getCustomerDOB(String cusDob2) {
        try {
            cAge = getAgeFromDob(cusDob2);
        } catch (IllegalArgumentException e) {
            e.getMessage();

        }
    }

    // Reading the AA.ARR.ACCOUNT TABLE local Field
    private void getAaArrAccountLocalField(Contract contract) {
        try {

            AaPrdDesAccountRecord aaprdDesAccRec = new AaPrdDesAccountRecord(contract.getConditionForProperty(ACCOUNT));

            if (legacy) {
                for (AltIdTypeClass altType : aaprdDesAccRec.getAltIdType()) {
                    if (altType.getAltIdType().getValue().equals("LEGACY")) {
                        legacyAccNo = altType.getAltId().getValue();

                    }
                }

            }

        } catch (Exception e) {
            e.getMessage();

        }

    }

    // get current RO
    private void getFfCentreDetail(String ffCentre2) {
        try {
            EbFfCentreDetailRecord centreDet = new EbFfCentreDetailRecord(
                    da.getRecord("EB.FF.CENTRE.DETAIL", ffCentre2));
            officerCode = centreDet.getCurrentRo().getValue();
            if (officerCode != null && !officerCode.isEmpty()) {
                getEbFfRoUserDets(officerCode);
            }
            centreName = centreDet.getCenterName().getValue();

        } catch (Exception e) {
            e.getMessage();
        }

    }

    // Reading the EB.FF.COLLECTION.DETS installment count and due date
    public void getEbFfCollectionDets(String arrId) {

        try {

            AaArrangementRecord aaRec = new AaArrangementRecord(da.getRecord(finMnemonic, AA_ARRANGEMENT, "", arrId));

            int hisDueCount = 0;
            int livDueCount = 0;
            String hisDueDate = "";
            String livDueDate = "";
            String strtDate = "";
            strtDate = aaRec.getStartDate().getValue();

            if (aaRec.getOrigContractDate().getValue() != null && !aaRec.getOrigContractDate().getValue().isEmpty()) {

                EbFfCollectionDetsHistoryRecord collHis = new EbFfCollectionDetsHistoryRecord(
                        da.getRecord(finMnemonic, "EB.FF.COLLECTION.DETS.HISTORY", "", arrId + "-" + strtDate + ".01"));

                hisDueCount = collHis.getDueDate().size();

                hisDueDate = collHis.getDueDate(0).getValue();
                dueDate = convertDateFormat(hisDueDate);

                totInstallments = hisDueCount;

            }

            else {

                EbFfCollectionDetsRecord collDetsRec = new EbFfCollectionDetsRecord(
                        da.getRecord(finMnemonic, "EB.FF.COLLECTION.DETS", "", arrId));

                livDueCount = collDetsRec.getDueDate().size() - 1;

                livDueDate = collDetsRec.getDueDate(1).getValue();
                dueDate = convertDateFormat(livDueDate);

                totInstallments = livDueCount;

            }

        } catch (

        Exception e) {
            e.getMessage();

        }
    }

    // find the latest date
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

    // GET State from EB.FF.LOAN.DETAILS
    public void getEbLoanDetails(String arrId, String finMnemonic) {
        try {
            EbFfLoanDetailsRecord ffLoanDetsRec = null;

            ffLoanDetsRec = new EbFfLoanDetailsRecord(da.getRecord(finMnemonic, "EB.FF.LOAN.DETAILS", "", arrId));

            getEntityNumber(ffLoanDetsRec);

            for (AddressTypeClass addressType : ffLoanDetsRec.getAddressType()) {
                if (addressType.getAddressType().getValue().equalsIgnoreCase("CURRENT")) {
                    state = addressType.getStateName().getValue();
                }

            }

        } catch (Exception e) {
            e.getMessage();

        }

    }

    // Reading Entity Number Class get guarantorName,gAge
    private void getEntityNumber(EbFfLoanDetailsRecord ffLoanDetsRec) {

        for (FmEntityNumberClass entityNumber : ffLoanDetsRec.getFmEntityNumber()) {

            if (entityNumber.getIsGuarantor().getValue().equalsIgnoreCase("YES")) {
                guarantorName = (entityNumber.getLegalName().getValue() != null)
                        ? entityNumber.getLegalName().getValue()
                        : "";

                guarantorDateOfBirth = (entityNumber.getDateOfBirth().getValue() != null)
                        ? entityNumber.getDateOfBirth().getValue()
                        : "";

                if (guarantorDateOfBirth != null && !guarantorDateOfBirth.trim().isEmpty()) {

                    gAge = getAgeFromDob(guarantorDateOfBirth);

                } else {

                    gAge = 0;
                }

            }
        }
    }

    // Reading the AA.ARRANGMENT TABLE
    public void getArrDets(String arrId) {
        try {
            String date = "";
            String comNameSub = "";
            AaArrangementRecord aaRec = null;

            aaRec = new AaArrangementRecord(da.getRecord(finMnemonic, AA_ARRANGEMENT, "", arrId));

            acctNumber = arrId;

            if (aaRec.getOrigContractDate().getValue() != null && !aaRec.getOrigContractDate().getValue().isEmpty()) {
                legacy = true;
                date = aaRec.getOrigContractDate().getValue();
                startDate = convertDateFormat(date);

            } else {
                date = aaRec.getStartDate().getValue();
                startDate = convertDateFormat(date);

            }

            linkedAppList = aaRec.getLinkedAppl();
            for (LinkedApplClass linkedApp : linkedAppList) {
                if (linkedApp.getLinkedAppl().getValue().equals(ACCOUNT)) {
                    accNum = linkedApp.getLinkedApplId().getValue();

                }
            }

            getAccountDetails(accNum);

            compCode = (aaRec.getCoCodeRec().getValue() != null) ? aaRec.getCoCodeRec().getValue() : "";
            customerNumber = aaRec.getCustomer().get(0).getCustomer().getValue();

            productCode = (aaRec.getProduct().get(0).getProduct().getValue() != null)
                    ? aaRec.getProduct().get(0).getProduct().getValue()
                    : "";
            getProductName();

            CompanyRecord companyObj1 = new CompanyRecord(da.getRecord("COMPANY", compCode));

            comNameSub = companyObj1.getCompanyName(0).toString();

            if (comNameSub != null && comNameSub.length() > 4) {
                comNameSub = comNameSub.substring(0, comNameSub.length() - 4);
            }
            compName = comNameSub;

            branchstate = companyObj1.getLocalRefField("FF.STATE").getValue();

        } catch (Exception e) {
            e.getMessage();
        }
    }

    private void getAccountDetails(String acctNo) {
        try {
            AccountRecord acctRec = new AccountRecord(da.getRecord(finMnemonic, ACCOUNT, "", acctNo));

            ffCentre = acctRec.getLocalRefField("FF.CENTRE").getValue();

            if (ffCentre != null && !ffCentre.isEmpty()) {
                getFfCentreDetail(ffCentre);
            }

            ffGrpCode = (acctRec.getLocalRefField("FF.GROUP").getValue() != null)
                    ? acctRec.getLocalRefField("FF.GROUP").getValue()
                    : "";

            if (ffGrpCode != null && !ffGrpCode.isEmpty()) {
                getEbffGroups(ffGrpCode);

            }

            ffLoanCycle = (acctRec.getLocalRefField("FF.LOAN.CYCLE").getValue() != null)
                    ? acctRec.getLocalRefField("FF.LOAN.CYCLE").getValue()
                    : "";

            getAccountLocalField(acctRec);

        } catch (Exception e) {

            e.getMessage();
        }

    }

    private void getAccountLocalField(AccountRecord acctRec) {
        try {

            ffLoanPurp = (acctRec.getLocalRefField("FF.LOAN.SUBPUR").getValue() != null)
                    ? acctRec.getLocalRefField("FF.LOAN.SUBPUR").getValue()
                    : "";
            ffloancat = (acctRec.getLocalRefField("FF.BIZ.TYPE").getValue() != null)
                    ? acctRec.getLocalRefField("FF.BIZ.TYPE").getValue()
                    : "";
        } catch (Exception e) {
            e.getMessage();
        }
    }

    private void getEbffGroups(String ffGrpCode2) {
        try {
            EbFfGroupsRecord ffGrp = new EbFfGroupsRecord(da.getRecord("", "EB.FF.GROUPS", "", ffGrpCode2));
            groupName = ffGrp.getGroupName().getValue();
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void getEbFfRoUserDets(String ro) {
        try {
            EbFfRoUserRecord roUserRec = new EbFfRoUserRecord(da.getRecord("", "EB.FF.RO.USER", "", ro));
            primOfficerName = roUserRec.getRoName().getValue();
        } catch (Exception e) {
            e.getMessage();
        }
    }

    // Date Conversion
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

    // Reading the AA.ARR.ACCOUNT TABLE
    private void getAaArrAccount(Contract contract) {
        AaPrdDesAccountRecord aaprdDesAccRec;
        try {
            aaprdDesAccRec = new AaPrdDesAccountRecord(contract.getConditionForProperty(ACCOUNT));

            ffDisbMode = (aaprdDesAccRec.getLocalRefField("FF.DISB.MODE").getValue() != null)
                    ? aaprdDesAccRec.getLocalRefField("FF.DISB.MODE").getValue()
                    : "";

            insurance1Name = (aaprdDesAccRec.getLocalRefField(FF_INSUR_COMP).getValue() != null)
                    ? aaprdDesAccRec.getLocalRefField(FF_INSUR_COMP).getValue()
                    : "";

            insurance2Name = (aaprdDesAccRec.getLocalRefField(FF_INSUR_COMP) != null)
                    ? aaprdDesAccRec.getLocalRefField(FF_INSUR_COMP).getValue()
                    : "";

        } catch (Exception e) {
            e.getMessage();

        }
    }

    // Reading the AA.ARR.CHARGE Table

    private void getAaChargeProcessingFee(Contract contract) {

        try {
            AaPrdDesChargeRecord aaprdDesCharRec = new AaPrdDesChargeRecord(
                    contract.getConditionForProperty(PROCESSINGFEE));
            fee1Name = aaprdDesCharRec.getIdComp2().getValue();
            fee1 = aaprdDesCharRec.getFixedAmount().getValue();
        } catch (Exception e) {
            e.getMessage();
        }

    }

    // READING THE AA.ARR.CHARGE TABLE FOR INSURANCE FEE
    private void getAaChargeInsuranceFee(Contract contract) {

        try {
            AaPrdDesChargeRecord aaprdDesCharRec = new AaPrdDesChargeRecord(
                    contract.getConditionForProperty(INSURANCEFEE));
            insurance1 = aaprdDesCharRec.getFixedAmount().getValue();

        } catch (Exception e) {
            e.getMessage();
        }

    }

    // Reading the AA.ARR.CHARGE for HOSPICASH
    private void getAaChargeHospicashFee(Contract contract) {

        try {
            AaPrdDesChargeRecord aaprdDesCharRec = new AaPrdDesChargeRecord(
                    contract.getConditionForProperty("HOSPICASH"));
            insurance2 = aaprdDesCharRec.getFixedAmount().getValue();

        } catch (Exception e) {
            e.getMessage();
        }

    }

    // READING AA.ARR.CHARGE for CGST IGST SGST NAME
    private void getAaChargeGstName(Contract contract) {
        String cgstName = "";
        String sgstName = "";
        String igstName = "";

        try {
            AaPrdDesChargeRecord aaprdDesCgstRec = new AaPrdDesChargeRecord(contract.getConditionForProperty("CGST"));
            AaPrdDesChargeRecord aaprdDesSgstRec = new AaPrdDesChargeRecord(contract.getConditionForProperty("SGST"));
            AaPrdDesChargeRecord aaprdDesIgstRec = new AaPrdDesChargeRecord(contract.getConditionForProperty("IGST"));

            cgstName = aaprdDesCgstRec.getIdComp2().getValue();
            sgstName = aaprdDesSgstRec.getIdComp2().getValue();
            igstName = aaprdDesIgstRec.getIdComp2().getValue();

            gst1Name = String.join(" ", cgstName, sgstName, igstName);

        } catch (Exception e) {
            e.getMessage();
        }

    }

    // READING AA.ARR.CHARGE for CGST IGST SGST AMOUNT
    private void getAaChargeGstAmt(Contract contract) {

        String cgstAmt = "";
        String sgstAmt = "";
        String igstAmt = "";

        try {
            AaPrdDesChargeRecord aaprdDesCgstRec = new AaPrdDesChargeRecord(contract.getConditionForProperty("CGST"));
            AaPrdDesChargeRecord aaprdDesSgstRec = new AaPrdDesChargeRecord(contract.getConditionForProperty("SGST"));
            AaPrdDesChargeRecord aaprdDesIgstRec = new AaPrdDesChargeRecord(contract.getConditionForProperty("IGST"));

            cgstAmt = aaprdDesCgstRec.getFixedAmount().getValue();
            sgstAmt = aaprdDesSgstRec.getFixedAmount().getValue();
            igstAmt = aaprdDesIgstRec.getFixedAmount().getValue();

            gst1 = String
                    .valueOf(Double.parseDouble(cgstAmt) + Double.parseDouble(sgstAmt) + Double.parseDouble(igstAmt));
            if (Double.parseDouble(gst1) > 0.0) {
                getAaChargeGstName(contract);
            } else if (Double.parseDouble(gst1) == 0.0) {
                gst1Name = "";
            }

        } catch (Exception e) {
            e.getMessage();
        }

    }

    // GET DisBursement time and Loan Amount
    private void getDisbursement(Contract contract) {
        try {
            AaAccountDetailsRecord aaAccDetails = contract.getAccountDetailsRecord();
            List<BillPayDateClass> payDateList = aaAccDetails.getBillPayDate();

            for (BillPayDateClass PayDate : payDateList) {

                for (BillIdClass billIdList : PayDate.getBillId()) {

                    if (billIdList.getBillType().getValue().equals("DISBURSEMENT")) {

                        AaBillDetailsRecord billDets = new AaBillDetailsRecord(
                                da.getRecord(finMnemonic, AA_BILL_DETAILS, "", billIdList.getBillId().getValue()));
                        String dateTime = billDets.getLastUpdateDate().getValue();

                        disbursedTime = extractTime(dateTime);

                        if (legacy) {
                            disbursedTime = "";
                        }

                        payAmt = billDets.getOrTotalAmount().getValue();

                    }

                }
            }

            AaPrdDesTermAmountRecord aaArrTermAmtRec = new AaPrdDesTermAmountRecord(
                    contract.getConditionForProperty("COMMITMENT"));

            if (legacy) {
                payAmt = aaArrTermAmtRec.getAmount().getValue();
            }

        } catch (Exception e) {
            e.getMessage();
        }
    }

    // Converstion of Time
    public String extractTime(String dateTimeStr) {

        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Date string is null or empty");
        }

        dateTimeStr = dateTimeStr.trim();

        DateTimeFormatter[] formatters = new DateTimeFormatter[] {
                DateTimeFormatter.ofPattern("dd MMM yy HH:mm", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("yyyyMMdd HH:mm"), DateTimeFormatter.ofPattern("yyMMddHHmm"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm") };

        for (DateTimeFormatter formatter : formatters) {
            try {
                LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr, formatter);
                return dateTime.format(DateTimeFormatter.ofPattern("HH:mm"));
            } catch (Exception e) {
                e.getMessage();
            }
        }

        throw new IllegalArgumentException("Unsupported date format: " + dateTimeStr);
    }

    // READING THE AA.ACCCOUNT.DETAILS TABLE
    private void getAaAccountDetails(String arrId) {
        try {
            AaAccountDetailsRecord aaAccDetails = new AaAccountDetailsRecord(da.getRecord("AA.ACCOUNT.DETAILS", arrId));

            for (BillPayDateClass billPayDate : aaAccDetails.getBillPayDate()) {
                for (BillIdClass billId : billPayDate.getBillId()) {

                    if (billId.getBillType().getValue().equals("ACT.CHARGE")) {

                        getProcessingFee(billId);
                        getInsuranceFee(billId);
                        getInsuranceFee2(billId);

                    }

                }
            }
        } catch (Exception e) {
            e.getMessage();
        }

    }

    // get Insurance2Unpaid from AA.BILL.DETAILS
    private void getInsuranceFee2(BillIdClass billId) {

        try {
            AaBillDetailsRecord billDetailRecord = new AaBillDetailsRecord(
                    da.getRecord(AA_BILL_DETAILS, billId.getBillId().getValue()));
            List<PropertyClass> property = billDetailRecord.getProperty();

            if (!billId.getSetStatus().getValue().equals(SETTLE) || !billId.getSetStatus().getValue().equals(REPAID)) {
                for (PropertyClass prop : property) {
                    if (prop.getProperty().getValue().equals("HOSPICASHFEE")) {
                        insurance2Unpaid = billDetailRecord.getOrTotalAmount().getValue();

                    }
                }
            }
        } catch (Exception e) {
            e.getMessage();
        }

    }

    // get Fee1Unpaid from AA.BILL.DETAILS
    private void getProcessingFee(BillIdClass billId) {
        try {
            AaBillDetailsRecord billDetailRecord = new AaBillDetailsRecord(
                    da.getRecord(AA_BILL_DETAILS, billId.getBillId().getValue()));
            List<PropertyClass> property = billDetailRecord.getProperty();

            if (!billId.getSetStatus().getValue().equals(SETTLE) || !billId.getSetStatus().getValue().equals(REPAID)) {
                for (PropertyClass prop : property) {
                    if (prop.getProperty().getValue().equals(PROCESSINGFEE)) {
                        fee1Unpaid = billDetailRecord.getOrTotalAmount().getValue();
                    }
                }
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

// get InsurancePaid from AA.BILL.DETAILS
    private void getInsuranceFee(BillIdClass billId) {
        try {
            AaBillDetailsRecord billDetailRecord = new AaBillDetailsRecord(
                    da.getRecord(AA_BILL_DETAILS, billId.getBillId().getValue()));
            List<PropertyClass> property = billDetailRecord.getProperty();

            if (!billId.getSetStatus().getValue().equals(SETTLE) || !billId.getSetStatus().getValue().equals(REPAID)) {

                for (PropertyClass prop : property) {

                    if (prop.getProperty().getValue().equals(INSURANCEFEE)) {

                        insurunpaid = billDetailRecord.getOrTotalAmount().getValue();
                    }
                }

            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    // Reading the AA.ARR.INTEREST TABLE
    private void getAaPrdInterest(Contract contract) {
        AaPrdDesInterestRecord aaprdDesIntRec;
        try {
            aaprdDesIntRec = new AaPrdDesInterestRecord(contract.getConditionForProperty("PRINTEREST"));

            interestRate = (aaprdDesIntRec.getFixedRate(0).getEffectiveRate().getValue() != null)
                    ? aaprdDesIntRec.getFixedRate(0).getEffectiveRate().getValue()
                    : "";

        } catch (Exception e) {
            e.getMessage();
        }
    }

// GET Product Name
    private void getProductName() {
        try {
            AaProductRecord prodRec = new AaProductRecord(da.getRecord("AA.PRODUCT", productCode));
            productName = (prodRec.getDescription(0).getValue() != null) ? prodRec.getDescription(0).getValue() : "";

        } catch (Exception e) {
            e.getMessage();
        }
    }

    public String convertUtcToIstTime(String utcDateStr) {
        utcDateStr = utcDateStr.trim();

        DateTimeFormatter format1 = DateTimeFormatter.ofPattern("dd MMM yy HH:mm", Locale.ENGLISH);
        DateTimeFormatter format2 = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm");
        DateTimeFormatter format3 = DateTimeFormatter.ofPattern("yyMMddHHmm");

        LocalDateTime utcDateTime;
        try {
            if (utcDateStr.matches("\\d{10}")) {
                utcDateTime = LocalDateTime.parse(utcDateStr, format3);
            } else if (utcDateStr.contains(" ")) {
                utcDateTime = LocalDateTime.parse(utcDateStr, format1);
            } else {
                utcDateTime = LocalDateTime.parse(utcDateStr, format2);
            }
        } catch (Exception e) {

            throw new IllegalArgumentException("Invalid date format: " + utcDateStr);
        }

        ZonedDateTime utcZoned = utcDateTime.atZone(ZoneId.of("UTC"));
        ZonedDateTime istZoned = utcZoned.withZoneSameInstant(ZoneId.of("Asia/Kolkata"));
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        return istZoned.format(timeFormatter);
    }

// return age from date 
    public int getAgeFromDob(String guarantorDateOfBirth) {
        LocalDate birthDate = parseDate(guarantorDateOfBirth);

        LocalDate today = LocalDate.now();

        return Period.between(birthDate, today).getYears();
    }

    public LocalDate parseDate(String guarantorDateOfBirth) {
        DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
        DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("yyyyMMdd");

        try {
            if (guarantorDateOfBirth.matches("\\d{8}")) {
                return LocalDate.parse(guarantorDateOfBirth, formatter2);
            } else {
                return LocalDate.parse(guarantorDateOfBirth.toUpperCase(Locale.ENGLISH), formatter1);
            }
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format: " + guarantorDateOfBirth);
        }
    }

// Writing the CSV file
    public void writeToFile(List<String> data, String filePath) {
        try {
            File file = new File(filePath);

            file.getParentFile().mkdirs();
            boolean fileExists = file.exists();

            try (FileWriter writer = new FileWriter(file, true)) {

                if (!fileExists) {

                    String header = String.join(",", "Branch Name", "Branch Code", "Branch State", "Center Name",
                            "Center Code", "Group Name", "Group Code", "Customer Name", "Customer Number",
                            "Customer State", "Customer Age", "Customer Caste", "Customer Religious", "Telephone",
                            "Category of Loaner", "Guarantor Name", "Guarantor Age", "Account Number",
                            "Legacy Loan Number", "Officer Name", "Officer Number", "Bank Name", "Bank Account Number",
                            "Product Name", "Loan Tenure", "Loan Purpose", "Rate of Interest", "Disbursement Date",
                            "Disbursement Time", "Loan Amount Disbursed", "Disbursement Mode", "Cycle Number",
                            "Installments", "Installment Frequency", "First Repayment Date", "Fee1 Name", "Fee1",
                            "Fee1(UnpaidAmount)", "Fee2 Name", "Fee2", "Fee2(UnpaidAmount)", "Insurance1 Name",
                            "Insurance1", "Insurance1Unpaid", "Insurance2 Name", "Insurance2",
                            "Insurance2 (Unpaid Amount)", "GST1 Name", "GST1");

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
