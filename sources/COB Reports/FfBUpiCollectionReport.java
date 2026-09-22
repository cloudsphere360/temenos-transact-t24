package com.temenos.fusion;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.temenos.api.TField;
import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.eb.servicehook.ServiceData;
import com.temenos.t24.api.hook.system.ServiceLifecycle;
import com.temenos.t24.api.records.aaactivityhistory.AaActivityHistoryRecord;
import com.temenos.t24.api.records.aaactivityhistory.ActivityRefClass;
import com.temenos.t24.api.records.aaactivityhistory.EffectiveDateClass;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaprddesaccount.AaPrdDesAccountRecord;
import com.temenos.t24.api.records.aaprddesaccount.AltIdTypeClass;
import com.temenos.t24.api.records.aaproduct.AaProductRecord;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.records.company.CompanyRecord;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.records.ebffcentredetail.EbFfCentreDetailRecord;
import com.temenos.t24.api.records.ebffgroups.EbFfGroupsRecord;
import com.temenos.t24.api.records.ebffparameter.EbFfParameterRecord;
import com.temenos.t24.api.records.ebffparameter.ParamDescClass;
import com.temenos.t24.api.records.ebffrouser.EbFfRoUserRecord;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/*-----------------------------------------------------------------------------
* @author Kavin Prabha M
* Date Created:19-NOV-2025
* Attached as : Service Routine
* EB.API :FF.B.PAYMENT.COLLECTION
* EB.API :FF.B.PAYMENT.COLLECTION.SELECT
* Attached to :BATCH>BNK/FF.B.PAYMENT.COLLECTION
* PGM.FILE:FF.B.PAYMENT.COLLECTION
* BATCH:MFI/FF.DAILY.REPORT.EXTRACT
* Description: UPICollection_LoanPayment Report
* 
* 
* BATCH>MFI/FF.UPI.COLLECTION
* TSA.SERVICE>MFI/FF.UPI.COLLECTION
*------------------------------------------------------------------------------ 
* Modification History :
*----------------------------------------------------------------------------- 
*19-Nov-2025   Development              Kavin Prabha M
*-----------------------------------------------------------------------------
*28-JAN-2026   Defect                   Kavin Prabha M
*-----------------------------------------------------------------------------
*04-MAR-2026   Remapping fields         Kavin Prabha M
*-----------------------------------------------------------------------------
*07-APR-2026   Remapping fields(CENTER,OFFICER)      Kavin Prabha M
*-----------------------------------------------------------------------------
*05-JUN-2026  New Mapping(Narration)    Kavin Prabha M
*-----------------------------------------------------------------------------
*/
public class FfBUpiCollectionReport extends ServiceLifecycle {
    public static final String FILE_NAME = "UPICollectionLoanPayReport";
    DataAccess da = new DataAccess(this);
    Session session = new Session(this);
    List<String> arrList = new ArrayList<>();
    String arrId = "";
    String companyName = "";
    String finMnemonic = "";
    String cusmnemonic = "";
    String companyId = "";
    String todayDate = "";
    String outputPath = "";
    List<ParamDescClass> paramDescList = null;
    String paraDesc = "";
    String paramPath = "";
    String mnemonic = "";
    List<String> arrangementListUpi = new ArrayList<>();
    String productId = "";

    String branchName = "";
    String branchCode = "";
    String centerName = "";// center
    String centerId = "";// centerCode
    String meetingTime = "";
    String officerName = "";
    String officerCode = "";
    String paymentOfficerName = "";
    String username = "";
    String acctNo = "";
    String legacyLoanNumber = "";
    String customerName = "";
    String customerNumber = "";
    String receiptNo = "";
    String amount = "";
    String paidBy = "";
    String paymentDate = "";
    String paymentStatus = "";
    String disbursementDate = "";
    String mode = "";
    String productName = "";
    String narration = "";
    String upiTransactionNumber = "";
    String upiRrn = "";
    String appropriatedAmount = "";
    String type = "";
    String origContractDate = "";
    String startDate = "";

    String fstName = "";
    String scdName = "";
    String familyName = "";
    String customerid = "";
    boolean legacy = false;
    String formattedDate = "";
    AaArrangementRecord arrRec = null;
    FundsTransferRecord ftRec = null;
    String groupCode = "";
    String groupName = "";
    String paidByparty = "";
    String creditTheirRef = "";
    String ffNarration = "";

    @Override
    /**
     * METHOD:getIds DESCRIPTION:Fetches active arrangement IDs for the specified
     * company.
     */
    public List<String> getIds(ServiceData serviceData, List<String> controlList) {
        try {
            initialiseCompanyInfo(serviceData);
            arrangementListUpi = da.selectRecords(finMnemonic, "AA.ARRANGEMENT", "",
                    "WITH ARR.STATUS NE CLOSE AND ARR.STATUS NE PENDING.CLOSURE");
        } catch (Exception e1) {
            e1.getMessage();
        }
        return arrangementListUpi;

    }

    /**
     * METHOD:initialiseCompanyInfo DESCRIPTION:This method is used to get the
     * values.
     */
    private void initialiseCompanyInfo(ServiceData serviceData) {
        companyId = serviceData.getCompanyId();
        CompanyRecord companyObj = new CompanyRecord(da.getRecord("COMPANY", companyId));
        finMnemonic = companyObj.getFinancialMne().getValue();
        cusmnemonic = companyObj.getCustomerMnemonic().getValue();

    }

    /**
     * METHOD:process DESCRIPTION:Retrieves arrangement, account, and transaction
     * details for the given arrangement ID and prepares data for COB report
     * generation.
     */
    public void process(String id, ServiceData serviceData, String controlItem) {
        initialiseCompanyInfo(serviceData);
        try {
            arrId = id;
            Contract contract = new Contract(this);
            contract.setContractId(arrId);
            getArrangementDetails(arrId);
            getAccountDetails(acctNo);
            getAaArrAccountDetails(contract);
            getFtDetails(arrId);

        } catch (Exception e2) {
            e2.getMessage();
        }

    }

    /**
     * @param loanNumber2
     */
    private void getAccountDetails(String acctNo) {
        try {
            AccountRecord accRec = new AccountRecord(da.getRecord(finMnemonic, "ACCOUNT", "", acctNo));
            centerId = accRec.getLocalRefField("FF.CENTRE").getValue();// centerCode
            if (centerId != null && !centerId.isEmpty()) {
                getEbFfCentreDetails(centerId);
            }
            groupCode = accRec.getLocalRefField("FF.GROUP").getValue();
            if (groupCode != null && !groupCode.isEmpty()) {
                getffgroupName(groupCode);
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    /**
     * @param groupCode2
     */
    private void getffgroupName(String groupCode) {
        try {
            EbFfGroupsRecord ffGroupRec = new EbFfGroupsRecord(da.getRecord("", "EB.FF.GROUPS", "", groupCode));
            groupName = ffGroupRec.getGroupName().getValue();
        } catch (Exception e22) {
            e22.getMessage();
        }
    }

    /**
     * @param centerId2
     */
    private void getEbFfCentreDetails(String centerId) {
        try {
            EbFfCentreDetailRecord centerRec = new EbFfCentreDetailRecord(
                    da.getRecord("", "EB.FF.CENTRE.DETAIL", "", centerId));
            centerName = centerRec.getCenterName().getValue();// center
            officerCode = centerRec.getCurrentRo().getValue();// officerCode
            meetingTime = centerRec.getFfMeetingTime().getValue();
            if (officerCode != null && !officerCode.isEmpty()) {
                getEbFfRoUserDets(officerCode);
            }

        } catch (Exception e) {
            e.getMessage();
        }
    }

    /**
     * @param ro
     */
    private void getEbFfRoUserDets(String officerCode) {
        try {
            EbFfRoUserRecord roUserRec = new EbFfRoUserRecord(da.getRecord("", "EB.FF.RO.USER", "", officerCode));
            officerName = roUserRec.getRoName().getValue();// officerName
        } catch (Exception e) {
            e.getMessage();
        }
    }

    /**
     * METHOD:getCobReportPath DESCRIPTION:Builds the COB report row, retrieves the
     * output path from parameter configuration, and writes the data to a CSV file.
     */
    private void getCobReportPath() {
        List<String> outvaluesupi = new ArrayList<>();
        EbFfParameterRecord paramRec = null;
        if (amount != null && !amount.trim().isEmpty()) {
            List<String> row = new ArrayList<>();
            row.add(branchName);
            row.add(branchCode);
            row.add(centerName);
            row.add(centerId);
            row.add(meetingTime);
            row.add(officerName);
            row.add(officerCode);
            row.add(paymentOfficerName);
            row.add(paymentOfficerName);
            row.add(username);
            row.add(arrId);
            row.add(legacyLoanNumber);
            row.add(customerName);
            row.add(customerNumber);
            row.add(receiptNo);
            row.add(amount);
            row.add(paidBy);
            row.add(convertDateFormat(paymentDate));
            row.add(paymentStatus);
            row.add(convertDateFormat(disbursementDate));
            row.add(mode);
            row.add(productName);
            row.add(narration);
            row.add(upiTransactionNumber);
            row.add(upiRrn);
            row.add(convertDateFormat(paymentDate));
            row.add(appropriatedAmount);
            row.add(type);
            todayDate = session.getCurrentVariable("!TODAY");
            outvaluesupi.add(String.join(",", row));

        }
        try {
            paramRec = new EbFfParameterRecord(da.getRecord("", "EB.FF.PARAMETER", "", "FF.COB.REPORT.EXTRACT"));

            if ((paramRec.getParamDesc() != null) && (!paramRec.getParamDesc().isEmpty())) {
                paramDescList = paramRec.getParamDesc();

                for (ParamDescClass paramDesc : paramDescList) {
                    paraDesc = paramDesc.getParamDesc().getValue();
                    if (paraDesc.equals("Custom Path for COB Reports")) {
                        paramPath = paramDesc.getParamValue().getValue();
                        break;
                    }
                }

            }
        } catch (Exception e3) {
            e3.getMessage();
        }
        if (!outvaluesupi.isEmpty()) {
            outputPath = paramPath + FILE_NAME + "_" + finMnemonic + "_" + todayDate + "_temp_"
                    + session.getSessionNumber() + ".csv";

            writeToFile(outvaluesupi, outputPath);
        }
    }

    /**
     * METHOD:convertDateFormat DESCRIPTION:This method is used to get the date
     * values.
     */
    private String convertDateFormat(String inputDate) {

        if (inputDate == null || inputDate.isEmpty()) {
            return "";
        }
        try {
            DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
            LocalDate date = LocalDate.parse(inputDate, inputFormatter);
            formattedDate = date.format(outputFormatter);

        } catch (Exception e4) {
            e4.getMessage();

        }
        return formattedDate;
    }

    /**
     * METHOD:getArrangementDetails DESCRIPTION:This method is used to get the
     * AA.ARRANGEMENT values.
     */
    private void getArrangementDetails(String arrId) {
        try {
            arrRec = new AaArrangementRecord(da.getRecord(finMnemonic, "AA.ARRANGEMENT", "", arrId));
            customerid = arrRec.getCustomer().get(0).getCustomer().getValue();
            acctNo = arrRec.getLinkedAppl().get(0).getLinkedApplId().getValue();
            productId = arrRec.getProduct().get(0).getProduct().getValue();
        } catch (Exception e4) {
            e4.getMessage();
        }
        if (arrRec != null && arrRec.getCoCodeRec() != null) {
            branchCode = arrRec.getCoCodeRec().getValue();
        } else {
            branchCode = null;
        }
        CompanyRecord comRec = new CompanyRecord(da.getRecord("COMPANY", branchCode));
        companyName = comRec.getCompanyName(0).toString();
        String[] removeArg = companyName.split("-");
        branchName = removeArg[0];
        getAaProductDetails(productId);

        origContractDate = arrRec.getOrigContractDate().getValue();
        startDate = arrRec.getStartDate().getValue();
        if (!origContractDate.isEmpty()) {
            disbursementDate = origContractDate;
            legacy = true;
        } else {
            disbursementDate = startDate;
        }
        getCustomerDetails(customerid);
    }

    /**
     * METHOD:getAaProductDetails DESCRIPTION:This method is used to get the
     * productName values.
     */
    private void getAaProductDetails(String productId) {
        try {
            AaProductRecord aaProRec = new AaProductRecord(da.getRecord("AA.PRODUCT", productId));
            productName = aaProRec.getDescription(0).getValue();
        } catch (Exception e5) {
            e5.getMessage();

        }
    }

    /**
     * METHOD:getCustomerDetails DESCRIPTION:This method is used to get the customer
     * application values.
     */
    private void getCustomerDetails(String customerid) {
        CustomerRecord cusRec = null;
        try {
            cusRec = new CustomerRecord(da.getRecord(cusmnemonic, "CUSTOMER", "", customerid));
            for (TField name1 : cusRec.getName1()) {
                fstName = name1.getValue();
            }

            for (TField name2 : cusRec.getName2()) {
                scdName = name2.getValue();
            }

            familyName = cusRec.getFamilyName().getValue();
            customerName = String.join(" ", fstName, scdName, familyName).trim().replaceAll("\\s+", " ");

        } catch (Exception e6) {
            e6.getMessage();
        }

        if (cusRec != null && cusRec.getMnemonic() != null) { // changed
            mnemonic = cusRec.getMnemonic().getValue();
        }

        if ((origContractDate != null)) {
            customerNumber = mnemonic;
        } else {
            customerNumber = customerid;
        }

    }

    /**
     * METHOD:getAaArrAccountDetails DESCRIPTION:This method is used to get the
     * AaArrAccountDetails values.
     */
    private void getAaArrAccountDetails(Contract contract) {

        try {
            List<String> aaArrAccountPrptyList = new ArrayList<>();
            aaArrAccountPrptyList.add("ACCOUNT");
            aaArrAccountPrptyList.add("LOANACCOUNT");
            for (String aaArrAcctid : aaArrAccountPrptyList) {
                AaPrdDesAccountRecord aaPrdDesAccountRecord = new AaPrdDesAccountRecord(
                        contract.getConditionForProperty(aaArrAcctid));
                if (legacy) {
                    for (AltIdTypeClass altType : aaPrdDesAccountRecord.getAltIdType()) {
                        if (altType.getAltIdType().getValue().equals("LEGACY")) {
                            legacyLoanNumber = altType.getAltId().getValue();
                        }
                    }
                }
            }
        } catch (Exception e6) {
            e6.getMessage();

        }
    }

    /**
     * METHOD:getFtDetails DESCRIPTION:Retrieves and processes all activity history
     * details for a given arrangement ID.
     */
    private void getFtDetails(String arrId) {

        try {
            AaActivityHistoryRecord aaActHisRec = new AaActivityHistoryRecord(
                    da.getRecord(finMnemonic, "AA.ACTIVITY.HISTORY", "", arrId));
            List<EffectiveDateClass> effectiveDateList = aaActHisRec.getEffectiveDate();
            for (EffectiveDateClass effectiveDate : effectiveDateList) {
                List<ActivityRefClass> activeRefList = effectiveDate.getActivityRef();
                for (ActivityRefClass activeRef : activeRefList) {
                    String activity = activeRef.getActivity().getValue();
                    if ((activity.equals("LENDING-APPLYPAYMENT-WRITEOFF.SETTLEMENT"))
                            || (activity.equals("LENDING-APPLYPAYMENT-INSURANCE.SETTLEMENT"))
                            || (activity.equals("LENDING-SETTLE-FORECLOSURE"))
                            || (activity.equals("LENDING-APPLYPAYMENT-PR.COLLECTION"))
                            || (activity.equals("LENDING-CREDIT-ARRANGEMENT"))) {

                        processActivityRef(activeRef);
                    }
                }
            }
        } catch (Exception e8) {
            e8.getMessage();

        }

    }

    /**
     * METHOD:processActivityRef DESCRIPTION:Processes a single activity reference
     * from the activity history. If the activity matches specific collection or
     * settlement types ("LENDING-APPLYPAYMENT-PR.COLLECTION",
     * "LENDING-SETTLE-PAYOFF", or "LENDING-SETTLE-FORECLOSURE") and the contract ID
     * starts with "FT",
     */
    private void processActivityRef(ActivityRefClass activeRef) {
        try {
            String contractId = activeRef.getContractId().getValue();
            if (contractId == null || contractId.isEmpty()) {
                return;
            }
            if (!contractId.startsWith("FT")) {
                return;
            }
            String[] removeArg = contractId.split("\\\\");
            String ftId = removeArg[0];
            resetUpiVariables();
            try {
                ftRec = new FundsTransferRecord(da.getRecord(finMnemonic, "FUNDS.TRANSFER", "", ftId));
            } catch (Exception e) {
                e.getMessage();
                try {
                    ftRec = new FundsTransferRecord(da.getHistoryRecord("FUNDS.TRANSFER", ftId));
                } catch (Exception e1) {
                    e1.getMessage();
                }
            }
            if (ftRec == null) {
                return;
            }
            logFundsTransferDetails(ftRec);
            getCobReportPath();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 
     */
    private void resetUpiVariables() {

        amount = "";
        appropriatedAmount = "";
        paymentDate = "";
        paymentOfficerName = "";
        username = "";
        receiptNo = "";
        mode = "";
        upiTransactionNumber = "";
        paymentStatus = "";
        narration = "";
        paidBy = "";
        upiRrn = "";

    }

    /**
     * METHOD:logFundsTransferDetails DESCRIPTION:Extracts and logs key details from
     * a Funds Transfer record, including credit amount, payment date, payment
     * officer, receipt number, payment mode, UPI transaction details, payment
     * status, narration, and information about the payer.
     */
    private void logFundsTransferDetails(FundsTransferRecord ftRec) {

        try {
            amount = ftRec.getCreditAmount().getValue();
            appropriatedAmount = ftRec.getCreditAmount().getValue();
            paymentDate = ftRec.getCreditValueDate().getValue();
            creditTheirRef = ftRec.getCreditTheirRef().getValue();
            if (ftRec.getLocalRefField("FF.NARRATION") != null) {
                ffNarration = ftRec.getLocalRefField("FF.NARRATION").getValue();
            }
            if (creditTheirRef != null && !creditTheirRef.trim().isEmpty() && ffNarration != null
                    && !ffNarration.trim().isEmpty()) {
                narration = creditTheirRef.trim() + " " + ffNarration.trim();
            } else if (creditTheirRef != null && !creditTheirRef.trim().isEmpty()) {
                narration = creditTheirRef.trim();
            } else if (ffNarration != null && !ffNarration.trim().isEmpty()) {
                narration = ffNarration.trim();
            } else {
                narration = "";
            }
            paymentOfficerName = ftRec.getLocalRefField("FF.POST.LGLNAME").getValue();
            username = ftRec.getLocalRefField("FF.PMTUSER.NAME").getValue();
            receiptNo = ftRec.getLocalRefField("FF.COLL.RCPT.NO").getValue();
            mode = ftRec.getLocalRefField("FF.PYMT.MODE").getValue();
            upiTransactionNumber = ftRec.getLocalRefField("FF.TRANS.REF.NO").getValue();
            paymentStatus = ftRec.getLocalRefField("FF.COLL.STATUS").getValue();
            paidBy = ftRec.getLocalRefField("FF.PAIDBY.THIRDPARTY").getValue();
            upiRrn = ftRec.getLocalRefField("FF.UPI.RRN").getValue();

        } catch (Exception e9) {
            e9.getMessage();

        }

    }

    /**
     * METHOD:writeToFile DESCRIPTION:his method creates the report output file in
     * the specified file path and writes the report data into it.
     */
    private void writeToFile(List<String> data, String filePath) {
        try {
            File file = new File(filePath);
            file.getParentFile().mkdirs();
            boolean fileExists = file.exists();

            try (FileWriter writer = new FileWriter(file, true)) {
                if (!fileExists) {
                    String header = String.join(",", "BranchName", "BranchCode", "Center", "CenterCode", "MeetingTime",
                            "OfficerName", "OfficerCode", "PaymentOfficerName", "PaymentOfficerCode", "Username",
                            "LoanNumber", "LegacyLoanNumber", "CustomerName", "CustomerNumber", "ReceiptNo", "Amount",
                            "PaidBy", "PaymentDate", "PaymentStatus", "DisbursementDate", "Mode", "Product",
                            "Narration", "UPITransactionNumber", "UPIRRN", "TransactionDate", "AppropriateAmount",
                            "Type");
                    writer.write(header + System.lineSeparator());
                }

                for (String line : data) {
                    writer.write(line + System.lineSeparator());
                }
            }

        } catch (Exception e10) {
            e10.getMessage();
        }
    }
}
