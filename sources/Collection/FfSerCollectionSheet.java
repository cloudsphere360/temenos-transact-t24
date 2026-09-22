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
import com.temenos.t24.api.records.aaaccountdetails.AaAccountDetailsRecord;
import com.temenos.t24.api.records.aaaccountdetails.BillIdClass;
import com.temenos.t24.api.records.aaaccountdetails.BillPayDateClass;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.company.CompanyRecord;
import com.temenos.t24.api.records.ebffcentredetail.EbFfCentreDetailRecord;
import com.temenos.t24.api.records.ebffcollectiondets.EbFfCollectionDetsRecord;
import com.temenos.t24.api.records.ebffcustdpd.EbFfCustDpdRecord;
import com.temenos.t24.api.records.ebffldeceasedinfo.EbFflDeceasedInfoRecord;
import com.temenos.t24.api.records.ebffldeceasedinfo.FfCurDodStsClass;
import com.temenos.t24.api.records.ebffloandetails.EbFfLoanDetailsRecord;
import com.temenos.t24.api.records.ebffloandpd.DateClass;
import com.temenos.t24.api.records.ebffloandpd.EbFfLoanDpdRecord;
import com.temenos.t24.api.records.ebffparameter.EbFfParameterRecord;
import com.temenos.t24.api.records.ebffparameter.ParamDescClass;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Date;
import com.temenos.t24.api.system.Session;

public class FfSerCollectionSheet extends ServiceLifecycle {

    private static final FusionFileLogger FfSerCollectionSheetLog = FusionFileLogger
            .getLogger(FfSerCollectionSheet.class);

    static final String BOOKING = "BOOKING";

    private final DataAccess dataAccess = new DataAccess(this);
    private String finMnemonic;
    private String mnemonic;

    // ----------------------------------------------------------
    // GET IDS
    // ----------------------------------------------------------
    @Override
    public List<String> getIds(ServiceData serviceData, List<String> controlList) {

        List<String> arrangementList = new ArrayList<>();

        try {
            initialiseCompanyInfo(serviceData);
            String query = "WITH ARR.STATUS EQ CURRENT OR ARR.STATUS EQ EXPIRED";

            arrangementList = dataAccess.selectRecords(finMnemonic, "AA.ARRANGEMENT", "", query);

        } catch (Exception e) {
            FfSerCollectionSheetLog.info("Error in getIds: " + e);
        }

        return arrangementList;
    }

    // ----------------------------------------------------------
    // PROCESS
    // ----------------------------------------------------------
    @Override
    public void process(String id, ServiceData serviceData, String controlItem) {

        try {

            initialiseCompanyInfo(serviceData);

            Session session = new Session(this);
            Date date = new Date(this);

            Contract contract = new Contract(this);
            contract.setContractId(id);

            AaAccountDetailsRecord accountDetails = new AaAccountDetailsRecord(
                    dataAccess.getRecord(finMnemonic, "AA.ACCOUNT.DETAILS", "", id));

            String paymentDate = accountDetails.getPaymentStartDate().toString();

            EbFfCollectionDetsRecord collection = loadCollectionDetails(id);

            String outpath = getReportTempPath();

            if (outpath != null) {

                String inProgressPath = outpath + "inprogress";

                List<String> finalRetvalue = buildCollectionSheetData(collection, id, paymentDate, contract, session);

                if (!finalRetvalue.isEmpty()) {

                    String outputPath = inProgressPath + File.separator + "Collectionsheet_" + finMnemonic + "_"
                            + date.getDates().getToday().toString() + "_temp_" + session.getSessionNumber() + ".csv";

                    writeToFile(finalRetvalue, outputPath);
                }
            }

        } catch (Exception e) {
            FfSerCollectionSheetLog.info("Error in process: " + e);
        }
    }

    // ----------------------------------------------------------
    // INITIALISE COMPANY
    // ----------------------------------------------------------
    private void initialiseCompanyInfo(ServiceData serviceData) {

        try {
            String companyId = serviceData.getCompanyId();

            CompanyRecord companyObj = new CompanyRecord(dataAccess.getRecord("COMPANY", companyId));

            finMnemonic = companyObj.getFinancialMne().getValue();

            mnemonic = companyObj.getCustomerMnemonic().getValue();

        } catch (Exception e) {
            FfSerCollectionSheetLog.info("Error in initialiseCompanyInfo: " + e);
        }
    }

    // ----------------------------------------------------------
    // LOAD COLLECTION
    // ----------------------------------------------------------
    private EbFfCollectionDetsRecord loadCollectionDetails(String id) {

        try {
            return new EbFfCollectionDetsRecord(dataAccess.getRecord(finMnemonic, "EB.FF.COLLECTION.DETS", "", id));
        } catch (Exception e) {
            FfSerCollectionSheetLog.info("Unable to read EB.FF.COLLECTION.DETS for arrangement " + id + ": " + e);
            return null;
        }
    }
    

    // ----------------------------------------------------------
    // GET REPORT PATH
    // ----------------------------------------------------------
    private String getReportTempPath() {

        try {

            String paramId = "FF.COB.REPORT.EXTRACT";

            EbFfParameterRecord paramRec = new EbFfParameterRecord(dataAccess.getRecord("EB.FF.PARAMETER", paramId));

            for (ParamDescClass paramDesc : paramRec.getParamDesc()) {

                if ("Reports Temp Path".equals(paramDesc.getParamName().getValue())) {

                    return paramDesc.getParamValue().getValue();
                }
            }

        } catch (Exception e) {
            FfSerCollectionSheetLog.info("Error fetching report temp path: " + e);
        }

        return null;
    }

    // ----------------------------------------------------------
    // BUILD DATA
    // ----------------------------------------------------------
    private List<String> buildCollectionSheetData(EbFfCollectionDetsRecord collection, String id, String paymentDate,
            Contract contract, Session session) {

        List<String> finalRetvalue = new ArrayList<>();

        String sestoday = session.getCurrentVariable("!TODAY");

        int index = -1;

        for (int i = 0; i < collection.getDueDate().size(); i++) {

            String dueDate = collection.getDueDate().get(i).getValue();

            if (sestoday.equals(dueDate) || dueDate.compareTo(sestoday) > 0) {

                index = i;

                paymentDate = dueDate.compareTo(sestoday) > 0 ? dueDate : sestoday;

                break;
            }
        }

        String deathStatus = getDeathStatus(id);

        String customerId = collection.getCustomer().toString();

        if ("CONFIRMED".equalsIgnoreCase(deathStatus)) {
            customerId += "(Death)";
        }

        String centreId = collection.getCentre().toString();

        String roEmployeeId = getCentreRoEmployeeId(centreId);

        LocalDate date = LocalDate.parse(sestoday, DateTimeFormatter.ofPattern("yyyyMMdd"));

        String formatted = date.getMonth().toString().substring(0, 3) + date.getYear();

        String tableId = id + "-" + formatted;

        String currentDpd = "";
        String peakDpd = "";
        String avgDpd = "";

        try {

            EbFfLoanDpdRecord loandpdRec = new EbFfLoanDpdRecord(
                    dataAccess.getRecord(finMnemonic, "EB.FF.LOAN.DPD", "", tableId));

            DateClass last = loandpdRec.getDate().get(loandpdRec.getDate().size() - 1);

            currentDpd = last.getCurDpd().getValue();

            peakDpd = last.getPeakDpd().getValue();

            avgDpd = last.getAvgDpd().getValue();

        } catch (Exception e) {
            FfSerCollectionSheetLog.error("Error loandpdRec: " + e);
        }

        String customerDPD = getCustomerDPD(customerId, formatted);

        double dueTotalAmt = index >= 0 ? safeParse(collection.getTotalDue().get(index).getValue()) : 0;

        double dueInterestAmt = getFinalInterestAmt(contract);

        double duePrincipalAmt = getFinalprinAmt(contract);

        double totalcurrAccount = safeParse(getBalance(contract, "CURACCOUNT", BOOKING))
                + safeParse(getBalance(contract, "CURACCOUNTCUST", BOOKING));

        String collectionOfficerId = getCollectionOfficer(id);

        String nextDemandDate = findNextDemandDate(collection, paymentDate);

        String loanStatus = "";
        String branchId = "";

        try {

            AaArrangementRecord arrangement = new AaArrangementRecord(
                    dataAccess.getRecord(finMnemonic, "AA.ARRANGEMENT", "", id));

            loanStatus = arrangement.getArrStatus().toString();

            branchId = arrangement.getCoCodeRec().toString();

        } catch (Exception e) {
            FfSerCollectionSheetLog.error("Error arrangement: " + e);
        }

        String parkedAmount = getBalance(contract, "UNCACCOUNT", BOOKING);

        double totalAccuredInterest = safeParse(getBalance(contract, "ACCPRINTEREST", BOOKING))
                + safeParse(getBalance(contract, "ACCPRINTERESTCUST", BOOKING));

        String totalEmiPending = getEmiPending(id, finMnemonic);

        List<String> row = new ArrayList<>();

        row.add(customerId);
        row.add(centreId);
        row.add(branchId);
        row.add(roEmployeeId);
        row.add(id);
        row.add(loanStatus);
        row.add(String.format("%.2f", totalcurrAccount));
        row.add(String.format("%.2f", duePrincipalAmt));
        row.add(String.format("%.2f", dueInterestAmt));
        row.add(String.valueOf(dueTotalAmt));
        row.add(totalEmiPending);
        row.add(parkedAmount);
        row.add(currentDpd);
        row.add(customerDPD);
        row.add(paymentDate);
        row.add(nextDemandDate);
        row.add(collectionOfficerId);
        row.add(String.format("%.2f", totalAccuredInterest));
        row.add(peakDpd);
        row.add(avgDpd);

        finalRetvalue.add(String.join(",", row));

        return finalRetvalue;
    }

    // ----------------------------------------------------------
    // REMAINING HELPER METHODS (UNCHANGED)
    // ----------------------------------------------------------

    private String getDeathStatus(String id) {
        try {
            EbFflDeceasedInfoRecord deceasedObj = new EbFflDeceasedInfoRecord(
                    dataAccess.getRecord(finMnemonic, "EB.FFL.DECEASED.INFO", "", id));

            List<FfCurDodStsClass> deathStatusList = deceasedObj.getFfCurDodSts();

            return deathStatusList.get(deathStatusList.size() - 1).getFfCurDodSts().toString();

        } catch (Exception e) {
            return null;
        }
    }

    private String getCentreRoEmployeeId(String centreId) {
        try {
            EbFfCentreDetailRecord centreDetails = new EbFfCentreDetailRecord(
                    dataAccess.getRecord("", "EB.FF.CENTRE.DETAIL", "", centreId));
            return centreDetails.getCurrentRo().toString();
        } catch (Exception e) {
            return "";
        }
    }

    private String getCollectionOfficer(String id) {
        try {
            EbFfLoanDetailsRecord loandetailsRec = new EbFfLoanDetailsRecord(
                    dataAccess.getRecord(finMnemonic, "EB.FF.LOAN.DETAILS", "", id));
            return loandetailsRec.getCollectionOfficer().getValue();
        } catch (Exception e) {
            return "";
        }
    }

    private String getCustomerDPD(String customerId, String formatted) {
        try {
            EbFfCustDpdRecord custdpdRec = new EbFfCustDpdRecord(
                    dataAccess.getRecord(mnemonic, "EB.FF.CUST.DPD", "", "CUS" + customerId + "-" + formatted));

            List<com.temenos.t24.api.records.ebffcustdpd.DateClass> dateLis = custdpdRec.getDate();

            return dateLis.get(dateLis.size() - 1).getCurDpd().getValue();

        } catch (Exception e) {
            return "";
        }
    }

    private double safeParse(String value) {
        try {
            return value != null && !value.isEmpty() ? Double.parseDouble(value) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private String getEmiPending(String id, String finMnemonic) {
        int emiPending = 0;
        try {
            AaAccountDetailsRecord accountDetsRecord = new AaAccountDetailsRecord(
                    dataAccess.getRecord(finMnemonic, "AA.ACCOUNT.DETAILS", "", id));

            for (BillPayDateClass billPayDateVal : accountDetsRecord.getBillPayDate()) {

                for (BillIdClass billIdRec : billPayDateVal.getBillId()) {

                    if ("UNPAID".equalsIgnoreCase(billIdRec.getSetStatus().getValue())
                            && "AGING".equalsIgnoreCase(billIdRec.getBillStatus().getValue())) {

                        emiPending++;
                    }
                }
            }

        } catch (Exception e) {
            FfSerCollectionSheetLog.error("Error getEmiPending: " + e);
        }

        return String.valueOf(emiPending);
    }

    private double getFinalprinAmt(Contract contract) {
        double duePrincipalAmt = 0;
        try {
            duePrincipalAmt = safeParse(getBalance(contract, "S0ACCOUNT", BOOKING))
                    + safeParse(getBalance(contract, "S1ACCOUNT", BOOKING))
                    + safeParse(getBalance(contract, "S2ACCOUNT", BOOKING))
                    + safeParse(getBalance(contract, "NPAACCOUNT", BOOKING))
                    + safeParse(getBalance(contract, "DUEACCOUNT", BOOKING))
                    + safeParse(getBalance(contract, "S0ACCOUNTCUST", BOOKING))
                    + safeParse(getBalance(contract, "S1ACCOUNTCUST", BOOKING))
                    + safeParse(getBalance(contract, "S2ACCOUNTCUST", BOOKING))
                    + safeParse(getBalance(contract, "NPAACCOUNTCUST", BOOKING))
                    + safeParse(getBalance(contract, "DUEACCOUNTCUST", BOOKING));

        } catch (Exception e) {
            FfSerCollectionSheetLog.error("Error duePrincipalAmt: " + e);
        }
        return duePrincipalAmt;
    }

    private double getFinalInterestAmt(Contract contract) {
        double dueInterestAmt = 0;
        try {
            dueInterestAmt = safeParse(getBalance(contract, "S0PRINTEREST", BOOKING))
                    + safeParse(getBalance(contract, "S1PRINTEREST", BOOKING))
                    + safeParse(getBalance(contract, "S2PRINTEREST", BOOKING))
                    + safeParse(getBalance(contract, "NPAPRINTEREST", BOOKING))
                    + safeParse(getBalance(contract, "DUEPRINTEREST", BOOKING))
                    + safeParse(getBalance(contract, "S0PRINTERESTCUST", BOOKING))
                    + safeParse(getBalance(contract, "S1PRINTERESTCUST", BOOKING))
                    + safeParse(getBalance(contract, "S2PRINTERESTCUST", BOOKING))
                    + safeParse(getBalance(contract, "NPAPRINTERESTCUST", BOOKING))
                    + safeParse(getBalance(contract, "DUEPRINTERESTCUST", BOOKING));

        } catch (Exception e) {
            FfSerCollectionSheetLog.error("Error getFinalInterestAmt: " + e);
        }
        return dueInterestAmt;
    }

    private String findNextDemandDate(EbFfCollectionDetsRecord collection, String paymentDate) {

        String nextDemandDate = null;

        for (TField due : collection.getDueDate()) {

            String val = due.getValue();

            if (val.compareTo(paymentDate) > 0 && (nextDemandDate == null || val.compareTo(nextDemandDate) < 0)) {

                nextDemandDate = val;
            }
        }

        return nextDemandDate;
    }

    private String getBalance(Contract contract, String accountType, String bookingType) {

        List<BalanceMovement> movements = contract.getContractBalanceMovements(accountType, bookingType);
        return (!movements.isEmpty()) ? movements.get(0).getBalance().toString() : "0";
    }

    private void writeToFile(List<String> data, String filePath) {

        try {

            File file = new File(filePath);
            file.getParentFile().mkdirs();
            boolean fileExists = file.exists();

            try (FileWriter writer = new FileWriter(file, true)) {

                if (!fileExists) {

                    String header = String.join(",", "customerId", "centerId", "branchId", "roEmployeeID",
                            "loanAccountNumber", "loanStatus", "principalOutstanding", "duePrincipal", "dueInterest",
                            "emiAmount", "totalEmiPending", "parkedAmount", "loanDpd", "customerDpd", "demandDate",
                            "nextDemandDate", "collectionOfficerId", "accruedInterestButNotDue", "peakDPD",
                            "AverageDPD");

                    writer.write(header + System.lineSeparator());
                }

                for (String line : data) {
                    writer.write(line + System.lineSeparator());
                }
            }

        } catch (Exception e) {
            FfSerCollectionSheetLog.error("Error writing to file: " + e);
        }
    }
}