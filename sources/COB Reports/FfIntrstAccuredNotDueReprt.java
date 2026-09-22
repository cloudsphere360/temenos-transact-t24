package com.temenos.fusion;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.temenos.api.TField;
import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.aa.contractapi.BalanceMovement;
import com.temenos.t24.api.complex.eb.servicehook.ServiceData;
import com.temenos.t24.api.hook.system.ServiceLifecycle;
import com.temenos.t24.api.records.aaaccountdetails.AaAccountDetailsRecord;
import com.temenos.t24.api.records.aaaccountdetails.BillIdClass;
import com.temenos.t24.api.records.aaaccountdetails.BillPayDateClass;
import com.temenos.t24.api.records.aaactivityhistory.AaActivityHistoryRecord;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaarrtermamount.AaArrTermAmountRecord;
import com.temenos.t24.api.records.aaprddesaccount.AaPrdDesAccountRecord;
import com.temenos.t24.api.records.aaprddesinterest.AaPrdDesInterestRecord;
import com.temenos.t24.api.records.aaprddestermamount.AaPrdDesTermAmountRecord;
import com.temenos.t24.api.records.aaproduct.AaProductRecord;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.records.account.AltAcctTypeClass;
import com.temenos.t24.api.records.company.CompanyRecord;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.records.ebffcollectiondets.EbFfCollectionDetsRecord;
import com.temenos.t24.api.records.ebffcollectiondetshistory.EbFfCollectionDetsHistoryRecord;
import com.temenos.t24.api.records.ebffloanpaymenthis.DemandDateClass;
import com.temenos.t24.api.records.ebffloanpaymenthis.EbFfLoanPaymentHisRecord;
import com.temenos.t24.api.records.ebffparameter.EbFfParameterRecord;
import com.temenos.t24.api.records.ebffparameter.ParamDescClass;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/*-----------------------------------------------------------------------------
* @author Harshini Sakthivel
* Date Created: 11-Nov-2025
* Attached as : Post Routine
* EB.API : FF.B.INT.ACCURED.NOT.DUE.SELECT
* EB.API :FF.B.INT.ACCURED.NOT.DUE
* Attached to BATCH :BNK/FF.B.INT.ACCURED.NOT.DUE
* Description: Generation of Report file through COB process 
*------------------------------------------------------------------------------ 
* Modification History : Initial Draft
*----------------------------------------------------------------------------- 
*11-Nov-2025   Development                       Initial Version
*-----------------------------------------------------------------------------
*08-Feb-2026   Remapping                          Jerome
*24-Feb-2026   final Remapping                    Jeevitha B
*12.03.2026    SonarQube Clear                     Jeevitha B
*16.03.2026    For all branch                     Jeevitha B
*07.04.2-26    Remapping 
*              found source and Interest          Jeevitha B
*              
*
*/
public class FfIntrstAccuredNotDueReprt extends ServiceLifecycle {


    private static final String TRADE = "TRADE";

    public static final String FILE_NAME = "InterestAccruedNotDueReport";
    DataAccess da = new DataAccess(this);
    Session ss = new Session(this);
    String todayDate = ss.getCurrentVariable("!TODAY");

    List<String> finalReport = new ArrayList<>();
    String aaaId = "";
    String accountNumber = "";
    String legacyLoanNumber = "";
    String branchName = "";
    String branchcode = "";
    String custId = "";
    String givenName = "";
    String familyName = "";
    String fstName = "";
    String scdName = "";
    String startDate = "";
    boolean legacy = false;
    boolean lastInstallmentDateset = false;
    String strDate = "";

    List<String> arrIdList = new ArrayList<>();
    String creditValueDate = "";
    String accId = "";
    String lastInstallmentDate = "";
    String fundSrc = "";
    String loanAmount = "";
    String maturityDate = "";
    AaPrdDesAccountRecord aaAcc = null;
    EbFfCollectionDetsRecord collDetRec = null;
    String dueDate = "";
    LocalDate paiddate = null;
    String fixedRate = "";
    String lastPaidDate = "";
    AaActivityHistoryRecord aaActHisRec = null;
    String initial = "";
    String contractId = "";
    String ftId = "";
    FundsTransferRecord ftRec = null;
    String finMnemonic = "";
    String mnemonic = "";
    String productDet = "";
    String tYfinal = "";
    String arrId = "";
    String finMnmc = "";
    String pathValue = "";
    EbFfParameterRecord paramRec = null;
    List<ParamDescClass> paramDescList = null;
    String paramPath = "";
    String paraDesc = "";
    String paraDescName = "";
    String principalOutstanding = "";
    CustomerRecord cusRec = null;
    String customerName = "";
    AaArrTermAmountRecord aaArrTermAmt = null;
    List<TField> totalDueList = null;
    String disbursementDate = "";
    String accuredInterest = "";
    String accountId="";

    EbFfParameterRecord ebFfParamRec = null;
    boolean paramflag = false;
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    LocalDate totayformatted = LocalDate.parse(todayDate, formatter);
    boolean migratedContractFlg = false;

    @Override
    public List<String> getIds(ServiceData serviceData, List<String> controlList) {

        List<String> arrangementList = new ArrayList<>();

        try {
            initialiseCompanyInfo(serviceData);
            arrangementList = da.selectRecords(finMnemonic, "AA.ARRANGEMENT", "",
                    "WITH ARR.STATUS NE CLOSE AND ARR.STATUS NE PENDING.CLOSURE");

        } catch (Exception e) {
            e.getMessage();

        }
        return arrangementList;
    }

    
// get mnemonic
    private void initialiseCompanyInfo(ServiceData serviceData) {

        try {
            String companyId = serviceData.getCompanyId();
            CompanyRecord companyObj = new CompanyRecord(da.getRecord("COMPANY", companyId));
            finMnemonic = companyObj.getFinancialMne().getValue();
            mnemonic = companyObj.getCustomerMnemonic().getValue();
            if (!paramflag) {
                ebFfParamRec = new EbFfParameterRecord(da.getRecord("EB.FF.PARAMETER", "FF.COB.REPORT.EXTRACT"));
                if (!ebFfParamRec.getParamDesc().isEmpty()) {
                    paramDescList = ebFfParamRec.getParamDesc();
                    for (ParamDescClass paramDesc : paramDescList) {
                        paraDesc = paramDesc.getParamDesc().getValue();
                        if (paraDesc.equals("Custom Path for COB Reports")) {
                            paramPath = paramDesc.getParamValue().getValue();
                            paramflag = true;
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {

            e.getMessage();
        }
    }

    @Override
    public void process(String id, ServiceData serviceData, String controlItem) {        
        initialiseCompanyInfo(serviceData);
        getVariables();
        try {

            Contract contract = new Contract(this);

            contract.setContractId(id);
            arrId = id;
            accountNumber = arrId;
            
            getAaArrangement(id);
            getBillDetails(arrId);
            getEcbDetails(contract);
            getCustomer(custId);
            getAccountDetails(accountId);
            getAaArrTermAmount(contract);
            getAaPrdDesInterest(contract);
            legacylastInstallmentDate(id);
      

            disbursementDate = convertDateFormat(disbursementDate);
            maturityDate = convertDateFormat(maturityDate);
            lastInstallmentDate = convertDateFormat(lastInstallmentDate);
            lastPaidDate = convertDateFormat(lastPaidDate);

            finalReport.add(String.join(",", branchName, branchcode, customerName, accountNumber, legacyLoanNumber,
                    productDet, loanAmount, disbursementDate, fundSrc, maturityDate, fixedRate, principalOutstanding,
                    lastInstallmentDate, lastPaidDate, accuredInterest));

            writeReport();

        } catch (Exception e) {

            e.getMessage();

        }

    }
    

    private void writeReport() {
        if (finalReport.isEmpty())
            return;

        String outputPath = paramPath + FILE_NAME + "_" + finMnemonic + "_" + todayDate + "_temp_"
                + ss.getSessionNumber() + ".csv";

        writeToFile(finalReport, outputPath);
    }
    
    public void getVariables()  {
      legacy = false;
      legacyLoanNumber="";
      lastInstallmentDate = "";
      lastPaidDate ="";
    }

  
    private void getCustomer(String custId2) {
        try {

             cusRec = new CustomerRecord(da.getRecord(mnemonic, "CUSTOMER", "", custId2));

            String first = cusRec.getName1().stream().map(TField::getValue).findFirst().orElse("");

            String second = cusRec.getName2().stream().map(TField::getValue).findFirst().orElse("");

            String family = cusRec.getFamilyName().getValue();

            customerName = getGivenName(first,second, family);

        } catch (Exception e1) {
            e1.getMessage();
        }
    }
 
    private String getGivenName(String first, String second, String family) {
        
        StringBuilder sb = new StringBuilder();
        sb.append(first);
        if (!second.equals("")) {
            sb.append(" ");
            sb.append(second);
        }
        if (!family.equals("")) {
            sb.append(" ");
            sb.append(family);
        }
 
        return sb.toString();
    }
    
    private void getAaArrTermAmount(Contract contract) {
        try {
            AaPrdDesTermAmountRecord aaArrTermAmtRec = new AaPrdDesTermAmountRecord(
                    contract.getConditionForProperty("COMMITMENT"));

            loanAmount = aaArrTermAmtRec.getAmount().getValue();
            maturityDate = aaArrTermAmtRec.getMaturityDate().getValue();

        } catch (Exception e) {

            e.getMessage();
        }
    }

// get branch name, branch code, disbursement date
    private void getAaArrangement(String id) {

        try {
            AaArrangementRecord aaRec = new AaArrangementRecord(da.getRecord(finMnemonic, "AA.ARRANGEMENT", "", id));
            branchcode = aaRec.getCoCodeRec().getValue();
            strDate = aaRec.getStartDate().getValue();

            accountId=aaRec.getLinkedAppl().get(0).getLinkedApplId().getValue();
            CompanyRecord compRec = new CompanyRecord(da.getRecord("COMPANY", branchcode));
            String comNameSub = compRec.getCompanyName(0).getValue();
            if (comNameSub != null && comNameSub.length() > 4) {
                branchName = comNameSub.substring(0, comNameSub.length() - 4);
            }

            String productId = aaRec.getProduct().get(0).getProduct().getValue();
            AaProductRecord aaProRec = new AaProductRecord(da.getRecord("AA.PRODUCT", productId));
            productDet = aaProRec.getDescription(0).getValue();

            custId = aaRec.getCustomer().get(0).getCustomer().getValue();
            if (aaRec.getOrigContractDate().getValue() != null && !aaRec.getOrigContractDate().getValue().isEmpty()) {
                legacy = true;
                disbursementDate = aaRec.getOrigContractDate().getValue();
              
            } else {
                startDate = aaRec.getStartDate().getValue();
                disbursementDate = startDate;
            }
        } catch (Exception e) {
            e.getMessage();
        }

    }
 
    private void getAccountDetails(String accountId) {

        try {
            AccountRecord accRec = new AccountRecord(da.getRecord(finMnemonic, "ACCOUNT", "", accountId));
            fundSrc=accRec.getLocalRefField("FF.FUNDER.NAME").getValue();
            if (legacy) {
                for (AltAcctTypeClass altType : accRec.getAltAcctType()) {
                    if (altType.getAltAcctType().getValue().equalsIgnoreCase("LEGACY")) {
                        legacyLoanNumber = altType.getAltAcctId().getValue();
                    }
                }
            }
            
        } catch (Exception e) {
            e.getMessage();
        }

    }


// get bill details
    private void getBillDetails(String id) {

        try {

            AaAccountDetailsRecord aaAccDetails = new AaAccountDetailsRecord(
                    da.getRecord(finMnemonic, "AA.ACCOUNT.DETAILS", "", id));

            List<BillPayDateClass> payDateList = aaAccDetails.getBillPayDate();
            
            getlastinspaiddate(totayformatted, payDateList);

          legacylastpaiddate(id);

          
        }

        catch (Exception e) {
            e.getMessage();
        }

    }

    private void getlastinspaiddate(LocalDate totayformat, List<BillPayDateClass> payDateList) {
        for (BillPayDateClass payDate : payDateList) {

            getpaidlastinstdate(totayformat, payDate);
            

    }
    }
    
    private void getpaidlastinstdate(LocalDate totayformat, BillPayDateClass payDate) {
        for (BillIdClass billId : payDate.getBillId()) {

            if (billId.getBillType().getValue().equals("INSTALLMENT")
                    && billId.getSetStatus().getValue().equals("SETTLED")) {
                String billpayDate = payDate.getBillPayDate().getValue();
                LocalDate billpayDatefor = LocalDate.parse(billpayDate, formatter);

                if (billpayDatefor.isBefore(totayformat) || billpayDatefor.isEqual(totayformat)) {
                    lastPaidDate = billpayDate;
                }
            }
        }
    }

    
    private void legacylastpaiddate(String id) {

        if (legacy) {
            
            try {
                EbFfLoanPaymentHisRecord loanpayhisRec = new EbFfLoanPaymentHisRecord(
                        da.getRecord("EB.FF.LOAN.PAYMENT.HIS", id));
                List<DemandDateClass> demandDateList = loanpayhisRec.getDemandDate();
                List<LocalDate> transDateList = new ArrayList<>();

                if (demandDateList != null && !demandDateList.isEmpty()) {

                    gettransdate(demandDateList, transDateList);
                }

                for (LocalDate date : transDateList) {
                    if (!date.isAfter(totayformatted) && (paiddate == null || date.isAfter(paiddate))) {
                        paiddate = date;

                    }

                }

                getpaiddate(demandDateList);
            } catch (Exception e) {
                e.getMessage();
            }
        }
    }

    private void gettransdate(List<DemandDateClass> demandDateList, List<LocalDate> transDateList) {
        for (DemandDateClass demand : demandDateList) {

            if (demand != null && demand.getTransDate() != null && demand.getTransDate().getValue() != null) {

                String transDateStr = demand.getTransDate().getValue();
                LocalDate transDate = LocalDate.parse(transDateStr, formatter);

                transDateList.add(transDate);
            }
        }
    }

    private void getpaiddate(List<DemandDateClass> demandDateList) {
        for (DemandDateClass demand1 : demandDateList) {

            String transDate = demand1.getTransDate().getValue();
            LocalDate transDateform1 = LocalDate.parse(transDate, formatter);
            if (transDateform1.isEqual(paiddate)) {
                String transType = demand1.getTransType().getValue();
                if ((transType.equalsIgnoreCase("interest") || transType.equalsIgnoreCase("principal"))) {
                    lastPaidDate = paiddate.toString();
                }
            }
        }
    }

// for legacy loan install date
    private void legacylastInstallmentDate(String id) {

      
            try {
                EbFfCollectionDetsRecord colldetsRec = new EbFfCollectionDetsRecord(
                        da.getRecord("EB.FF.COLLECTION.DETS", id));
            
                getlastInsDate(colldetsRec);
            } catch (Exception e) {
                e.getMessage();
            }
            if (legacy && !lastInstallmentDateset) {

                String hisrecid = id + "-" + strDate + ".01";

                try {
                    getcollectionhis(hisrecid);
                } catch (Exception e) {
                    e.getMessage();
                }
            }
    }
   
    
    private void getlastInsDate(EbFfCollectionDetsRecord colldetsRec) {
        
        List<TField> dueDateList = colldetsRec.getDueDate();
     
        if (dueDateList == null || dueDateList.isEmpty()) {
            return;
        }
     
        String dueDate1 = dueDateList.get(0).getValue();

        for (int i = dueDateList.size() - 1; i >= 0; i--) {
     
            String dueDate2 = dueDateList.get(i).getValue();
            LocalDate due = LocalDate.parse(dueDate, formatter);
     
            if (due.isBefore(totayformatted) && !dueDate2.equals(dueDate1)) {
     
                lastInstallmentDate = dueDate;
                lastInstallmentDateset = true;
                break;
            }
        }
    }
     
    private void getcollectionhis(String hisrecid) {
     
        EbFfCollectionDetsHistoryRecord hisRec =
            new EbFfCollectionDetsHistoryRecord(
                da.getRecord(finMnemonic,
                    "EB.FF.COLLECTION.DETS.HISTORY", "", hisrecid));
     
        List<TField> dueDateList = hisRec.getDueDate();
        
        if(dueDateList == null || dueDateList.isEmpty()) {
            return;
        }
     
        for (int i = dueDateList.size() - 1; i >= 0; i--) {
     
            String dueDate3 = dueDateList.get(i).getValue();
     
            LocalDate due = LocalDate.parse(dueDate3, formatter);
     
            if (due.isBefore(totayformatted)) {
                lastInstallmentDate = dueDate;
                lastInstallmentDateset = true;
                break;
            }
        }
    }
     
    
// AaPrdDesInterest 
    private void getAaPrdDesInterest(Contract contract) {

        try {

            AaPrdDesInterestRecord aaPrdDesInterestRecord = new AaPrdDesInterestRecord(
                    contract.getConditionForProperty("PRINTEREST"));

            String arrIntId = String.join("-", aaPrdDesInterestRecord.getIdComp1().getValue(),
                    aaPrdDesInterestRecord.getIdComp2().getValue(), aaPrdDesInterestRecord.getIdComp3().getValue());
            AaPrdDesInterestRecord prdDesInt = new AaPrdDesInterestRecord(
                    da.getRecord(finMnemonic, "AA.ARR.INTEREST", "", arrIntId));

            fixedRate = prdDesInt.getFixedRate().get(0).getEffectiveRate().getValue();

        } catch (Exception e) {

            e.getMessage();
        }
    }
  
   
    // ECB details
    public void getEcbDetails(Contract contract) {
        try {          
            
            double total = parse(getBalance(contract, "FFPRINODINTACCUR", TRADE)) ; //FFPRINODFUTAMT>FFPRINODINTACCUR 

            principalOutstanding = String.valueOf(total);
            principalOutstanding = principalOutstanding.replace("-", "");

            String interest = getBalance(contract, "FFACCINTEREST", TRADE);
            accuredInterest = interest != null ? interest.replace("-", "") : "0";

        } catch (NumberFormatException e) {
            e.getMessage();
        }
    }

    private double parse(String val) {
        try {
            return (val == null || val.isEmpty()) ? 0 : Double.parseDouble(val);
        } catch (Exception e) {
            return 0;
        }
    }

// get balance
    private String getBalance(Contract contract, String accountType, String bookingType) {

        List<BalanceMovement> movements = contract.getContractBalanceMovements(accountType, bookingType);
        return (movements != null && !movements.isEmpty()) ? movements.get(0).getBalance().toString() : "0";
    }

    // convert date format
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

// write to file
    private void writeToFile(List<String> finalReport2, String outputPath) {

        try {
            File file = new File(outputPath);
            file.getParentFile().mkdirs();
            boolean fileExists = file.exists();

            try (FileWriter writer = new FileWriter(file, true)) {
                if (!fileExists) {

                    String header = String.join(",", "Branch", "Branchcode", "CustomerName", "AccountNumber",
                            "LegacyLoanNumber", "ProductName", "LoanAmount", "DisbursementDate", "FundingSource",
                            "MaturityDate", "Interest%", "principalOutstanding", "LastInstallmentDate", "LastPaidDate",
                            "AccruedInterest");
                    writer.write(header + System.lineSeparator());
                }

                for (String line : finalReport2) {
                    writer.write(line + System.lineSeparator());
                }
            }

        } catch (Exception e) {

            e.getMessage();
        }

    }

}
