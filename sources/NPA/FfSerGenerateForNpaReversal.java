package com.temenos.fusion;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.temenos.api.TField;
import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.eb.servicehook.ServiceData;
import com.temenos.t24.api.hook.system.ServiceLifecycle;
import com.temenos.t24.api.records.aaaccountdetails.AaAccountDetailsRecord;
import com.temenos.t24.api.records.aaaccountdetails.SuspStatusClass;
import com.temenos.t24.api.records.aaactivityhistory.AaActivityHistoryRecord;
import com.temenos.t24.api.records.aaactivityhistory.ActivityRefClass;
import com.temenos.t24.api.records.aaactivityhistory.EffectiveDateClass;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaarrangementactivity.AaArrangementActivityRecord;
import com.temenos.t24.api.records.categentry.CategEntryRecord;
import com.temenos.t24.api.records.company.CompanyRecord;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.records.ebffparameter.EbFfParameterRecord;
import com.temenos.t24.api.records.ebffparameter.ParamDescClass;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

public class FfSerGenerateForNpaReversal extends ServiceLifecycle {

    public static final String FUNDS_TRANSFER = "FUNDS.TRANSFER";
    public static final String BOOKING = "BOOKING";
    public static final String ACCOUNT = "ACCOUNT";
    public static final String PENDING_CLOSURE = "PENDING.CLOSURE";
    public static final String CLOSE = "CLOSE";
    public static final String AA_ACCOUNT_DETAILS = "AA.ACCOUNT.DETAILS";
    DataAccess da = new DataAccess(this);
    List<String> arrList = new ArrayList<>();
    List<String> finalConsolIdList = new ArrayList<>();
    List<List<String>> recoveryDetails = new ArrayList<>();
    EbFfParameterRecord ebFfParamRec = null;
    List<ParamDescClass> paramDescList = new ArrayList<>();

    String paraDesc = "";
    String paramPath = "";
    String finamountLcy = "";
    String companyId = "";
    String arrId = "";
    String finMnemonic = "";
    String mnemonic = "";

    String accId = "";
    String arrStatus = "";

    String branchName = "";
    String accountNumber = "";
    String legacyAcctNo = "";
    String customerNumber = "";
    String customerName = "";
    String loanStartDate = "";
    String loanMaturityDate = "";
    String interestRateAsOnDateOfNpa = "";
    String npaDate = "";
    String accountStatus = "";
    String accountCloseFlag = "";
    String closingDate = "";
    String closureType = "";
    String outstandingAmountAsOnNpaDate = "";
    String allAgeStatus = "";
    boolean migratedContractFlg = false;
    boolean activityFound = false;

    boolean writeOffTriggered = false;
    boolean writeOffSettTriggered = false;
    boolean insSettTriggered = false;
    boolean settleClosureTriggered = false;
    boolean repaymentTriggered = false;
    boolean maturityTriggered = false;
    boolean previousMonth = false;
    String writeOffClosureDt = "";
    String writeOffSettClosureDt = "";
    String insSettClosureDt = "";
    String settleClosureDt = "";
    String repaymentClosureDt = "";
    String maturityClosureDt = "";

    String writeOffSettContractId = "";
    String insSettContractId = "";
    String settleClosureContractId = "";
    String repaymentContractId = "";
    String reversalDt = "";
    String writeOffActRefId = "";
    String startDate = "";
    String filePath = "";
    Session session = new Session(this);
    String entryNo = "";
    String ourReference = "";
    String todayDate = session.getCurrentVariable("!TODAY");
    String arrangementid = "";
    String transactionCode = "";
    String amountLcy = "";
    String processingDate = "";
    String plCategory = "";
    String legacyAccNo = "";
    public static final DateTimeFormatter T24_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    public static final DateTimeFormatter OUT_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    List<String> eligibleArrangements = new ArrayList<>();
    String monthRange = "";

    @Override
    public List<String> getIds(ServiceData serviceData, List<String> controlList) {

        List<String> arrList = new ArrayList<>();
        try {
            initialiseCompanyInfo(serviceData, companyId);
            arrList = da.selectRecords(finMnemonic, "AA.ARRANGEMENT", "",
                    "WITH ARR.STATUS EQ EXPIRED OR ARR.STATUS EQ CURRENT OR ARR.STATUS EQ PENDING.CLOSURE");
            monthRange = getPreviousMonthRange();
            for (String aaId : arrList) {
                String suspDate = getValidSuspensionDate(aaId);
               
                suspendActivity(aaId, suspDate);
            }

        } catch (Exception e) {

        }

        return eligibleArrangements;
    }

    private String getValidSuspensionDate(String arrId) {
        try {
            AaAccountDetailsRecord aaAccDetRec = new AaAccountDetailsRecord(
                    da.getRecord(finMnemonic, "AA.ACCOUNT.DETAILS", "", arrId));
            String suspended = aaAccDetRec.getSuspended().getValue();
            List<SuspStatusClass> suspStatusList = aaAccDetRec.getSuspStatus();
          
            for (SuspStatusClass suspStatusCls : suspStatusList) {
                
                String suspStatus = suspStatusCls.getSuspStatus().getValue();
                
                String suspDate = suspStatusCls.getSuspDate().getValue();
          
                if (suspStatus.equalsIgnoreCase("SUSPEND") || (suspended.equalsIgnoreCase("YES"))) {
                    if (isPreviousMonth(suspDate)) {
                       
                        return suspDate;
                    }
                }
            }
        }

        catch (Exception e) {

        }

        return null;
    }

    private boolean isPreviousMonth(String suspDate) {
        try {
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            LocalDate suspDt = LocalDate.parse(suspDate, formatter);
            LocalDate currentDt = LocalDate.parse(todayDate, formatter);
            LocalDate previousMonthVal = currentDt.minusMonths(1);

            return suspDt.getMonth() == previousMonthVal.getMonth() && suspDt.getYear() == previousMonthVal.getYear();

        } catch (Exception e) {

        }

        return false;
    }

    private String getPreviousMonthRange() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDate currentDate = LocalDate.parse(todayDate, formatter);
        LocalDate previousMon = currentDate.minusMonths(1);
        LocalDate fromDate = previousMon.withDayOfMonth(1);
        LocalDate toDate = previousMon.withDayOfMonth(previousMon.lengthOfMonth());
        return fromDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "|"
                + toDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }

    private void suspendActivity(String arrId, String suspDate) {

        try {
            AaActivityHistoryRecord aaActHisRec = new AaActivityHistoryRecord(
                    da.getRecord(finMnemonic, "AA.ACTIVITY.HISTORY", "", arrId));
            List<EffectiveDateClass> effDateList = aaActHisRec.getEffectiveDate();
            for (EffectiveDateClass effDateCls : effDateList) {
                for (ActivityRefClass actRefCls : effDateCls.getActivityRef()) {
                    String activity = actRefCls.getActivity().getValue();
                    String effDate = effDateCls.getEffectiveDate().getValue();
                    if (activity.equalsIgnoreCase("LENDING-SUSPEND-ARRANGEMENT") && (suspDate.equals(effDate))) {
                        List<ActivityRefClass> activityRefList = effDateCls.getActivityRef();
                        for (ActivityRefClass actRef : activityRefList) {
                            String aaaId = actRef.getActivityRef().getValue();
                            validateCategEntry(aaaId, arrId);
                        }
                    }
                }
            }

        } catch (Exception e) {

        }
    }

    private void validateCategEntry(String aaaId, String arrId) {

        try {
            AaArrangementActivityRecord aaaRec = new AaArrangementActivityRecord(
                    da.getRecord(finMnemonic, "AA.ARRANGEMENT.ACTIVITY", "", aaaId));

            String stmtNo = aaaRec.getStmtNos(0).getValue();
            String stmtRange = aaaRec.getStmtNos(1).getValue();
            stmtRange = stmtRange.replace("*", "");
            int start;
            int end;
            if (stmtRange.contains("-")) {
                String[] range = stmtRange.split("-");
                start = Integer.parseInt(range[0]);
                end = Integer.parseInt(range[1]);
            } else {
                start = Integer.parseInt(stmtRange);
                end = start;
            }

            for (int i = start; i <= end; i++) {
                String categEntryId = stmtNo + String.format("%04d", i);
                CategEntryRecord catRec = new CategEntryRecord(da.getRecord("CATEG.ENTRY", categEntryId));
                String trnsCode = catRec.getTransactionCode().getValue();
                double amtLcy = Double.parseDouble(catRec.getAmountLcy().getValue());
                if (trnsCode.equals("827") && amtLcy < 0) { // amtLcy<0 need to change
                    eligibleArrangements.add(arrId + "|" + categEntryId + "|" + monthRange);
                    
                }
            }

        } catch (Exception e) {

        }

    }

    @Override
    public void process(String id, ServiceData serviceData, String controlItem) {
        List<String> outvalues = new ArrayList<>();
        try {
                        
            initialiseCompanyInfo(serviceData, companyId);
            String[] ids = id.split("\\|");
            arrId = ids[0];
            entryNo = ids[1];
            
            reversalDt = "";
            npaDate = "";
            allAgeStatus = "";
            
            String fromDate = ids[2];
            String toDate = ids[3];           

            Contract contract = new Contract(this);
            contract.setContractId(arrId);

            getArrangementDetails(contract);
            initialiseCompanyInfo(serviceData, companyId);
            getCustomerDetails(customerNumber);
            getAaAccountDetails(contract);

            if (npaDate == null || npaDate.trim().isEmpty()) {
               
                return;
            }

            
            getCategory(entryNo);

            if (reversalDt == null || reversalDt.isEmpty()) {
                
                return;
            }

            List<String> row = new ArrayList<>();
            row.add(convertDate(fromDate));
            row.add(convertDate(toDate));
            row.add(branchName);
            row.add(accountNumber);
            row.add(customerNumber);
            row.add(customerName);
            row.add(accountStatus);
            row.add(convertDate(reversalDt));
            row.add(plCategory);
            row.add(finamountLcy);

            outvalues.add(String.join(",", row));

            if (!outvalues.isEmpty() && allAgeStatus.equals("NPA")) //allAgeStatus.equals("NPA")) condition added
                
            {
                ebFfParamRec = new EbFfParameterRecord(da.getRecord("EB.FF.PARAMETER", "FF.COB.REPORT.EXTRACT"));
                if (ebFfParamRec != null && !ebFfParamRec.getParamDesc().isEmpty()) {
                    paramDescList = ebFfParamRec.getParamDesc();
                    for (ParamDescClass paramDesc : paramDescList) {
                        paraDesc = paramDesc.getParamDesc().getValue();
                        if (paraDesc.equals("Custom Path for COB Reports")) {

                            paramPath = paramDesc.getParamValue().getValue();
                        }
                    }
                }

            }

            String outputPath = paramPath + "NPAPLReversalReport" + "_" + finMnemonic + "_" + todayDate
                    + session.getSessionNumber() + ".csv";
            writeToFile(outvalues, outputPath);

        } catch (Exception e) {
            e.getMessage();
        }

    }

    /**
     * @param entryNo2
     */
    private String getCategory(String entryNo) {
        ourReference = "";
        try {
            CategEntryRecord categEntry = new CategEntryRecord(da.getRecord(finMnemonic, "CATEG.ENTRY", "", entryNo));// entryNo
            ourReference = categEntry.getOurReference().getValue();
            plCategory = categEntry.getPlCategory().getValue();
            processingDate = categEntry.getProcessingDate().getValue();
            amountLcy = categEntry.getAmountLcy().getValue();
            double amount = Double.parseDouble(categEntry.getAmountLcy().getValue());
            finamountLcy = String.valueOf(Math.abs(amount));
           
            transactionCode = categEntry.getTransactionCode().getValue();
        } catch (Exception e) {
            e.getMessage();
        }
        return ourReference;
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

    public void getArrangementDetails(Contract contract) {
        try {
            AaArrangementRecord arrRec = contract.getContract();
            companyId = arrRec.getCoCodeRec().getValue();
            accountNumber = arrId;
            accId = arrRec.getLinkedAppl().get(0).getLinkedApplId().getValue();
            customerNumber = arrRec.getCustomer().get(0).getCustomer().getValue();
            arrStatus = arrRec.getArrStatus().getValue();
            startDate = arrRec.getStartDate().getValue();
        } catch (

        Exception e) {
            e.getMessage();
        }

    }

    public void getCustomerDetails(String custNumber) {
        try {
            StringBuilder custNameBuild = new StringBuilder();
            CustomerRecord custRec = new CustomerRecord(da.getRecord(mnemonic, "CUSTOMER", "", custNumber));
            if (migratedContractFlg) {
                customerNumber = custRec.getMnemonic().getValue();
            }

            TField name1Field = (custRec.getName1() != null && !custRec.getName1().isEmpty())
                    ? custRec.getName1().get(0)
                    : null;

            TField name2Field = (custRec.getName2() != null && !custRec.getName2().isEmpty())
                    ? custRec.getName2().get(0)
                    : null;

            List<String> cusNameVal = Arrays.asList(checkFiled(name1Field), checkFiled(name2Field),
                    checkFiled(custRec.getFamilyName()));
            for (String cusNameValList : cusNameVal) {
                appendIfNotEmpty(custNameBuild, cusNameValList);
            }
            customerName = custNameBuild.toString();

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

    public void appendIfNotEmpty(StringBuilder customerName, String value) {
        if (value != null && !value.isEmpty()) {
            if (customerName.length() > 0) {
                customerName.append(" ");
            }
            customerName.append(value);
        }
    }

    public void getAaAccountDetails(Contract contract) {
        try {
//            String allAgeStatus = "";

            AaAccountDetailsRecord aaAcctDets = contract.getAccountDetailsRecord();
            accountStatus = aaAcctDets.getArrAgeStatus().getValue();

            int aaAgeAllDt = aaAcctDets.getAgeAllDate().size();
            for (int i = 0; i < aaAgeAllDt; i++) {
                int allAgeStatuscnt = aaAcctDets.getAgeAllDate(i).getAgeAllBillType().size();
          
                for (int j = 0; j < allAgeStatuscnt; j++) {
                    allAgeStatus = aaAcctDets.getAgeAllDate(i).getAgeAllBillType(j).getAgeAllStatus().getValue();
                   
                    if (!allAgeStatus.isEmpty() && allAgeStatus.equalsIgnoreCase("NPA")) {
                        npaDate = aaAcctDets.getAgeAllDate(i).getAgeAllDate().getValue();
                        
                        if (isPreviousMonth(npaDate)) {
                            reversalDt = npaDate;
                           
                        }

                    }

                }
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void initialiseCompanyInfo(ServiceData serviceData, String companyId) {
        try {
            if (companyId == null || companyId.isEmpty()) {
                companyId = serviceData.getCompanyId();
            }
            CompanyRecord companyObj = new CompanyRecord(da.getRecord("COMPANY", companyId));
            finMnemonic = companyObj.getFinancialMne().getValue();
            mnemonic = companyObj.getCustomerMnemonic().getValue();
            branchName = companyObj.getCompanyName().get(0).getValue();
            String[] branchNamePart = branchName.split("-");
            branchName = branchNamePart[0];

        } catch (Exception e) {
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
                    String header = String.join(",", "FromDate", "TODate", "BranchName", "AccountNumber",
                            "CustomerNumber", "CustomerName", "AccountStatus", "NPADate", "PLCategoryCode",
                            "InterestReversalAmount");
                    writer.write(header + System.lineSeparator());// recoveryHeaders +
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
