package com.temenos.fusion;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import com.temenos.t24.api.complex.eb.servicehook.ServiceData;
import com.temenos.t24.api.hook.system.ServiceLifecycle;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.aa.contractapi.BalanceMovement;
import com.temenos.t24.api.records.aaactivityhistory.AaActivityHistoryRecord;
import com.temenos.t24.api.records.aaactivityhistory.ActivityRefClass;
import com.temenos.t24.api.records.aaactivityhistory.EffectiveDateClass;
import com.temenos.t24.api.records.company.CompanyRecord;
import com.temenos.t24.api.system.Session;
import com.temenos.t24.api.system.DataAccess;

public class FfMServControlTotalsSingF extends ServiceLifecycle {
    List<String> retvalues = new ArrayList<>();
    List<String> finalArrList = new ArrayList<>();
    List<String> data = new ArrayList<>();
    List<String> data1 = new ArrayList<>();
    List<String> testp = new ArrayList<>();
    List<String> currentArrList = new ArrayList<>();
    List<String> writeArrList = new ArrayList<>();
    List<String> extraList = new ArrayList<>();
    DataAccess da = new DataAccess(this);

    List<String> nofWriteOffLns = new ArrayList<>();

    String trade = "TRADE";
    String branch = "";
    String branchName = "";
    String todayDate = "";
    String finMnemonic = "";
    String mnemonic = "";
    String filePath = "/shares/tafjud/Fusion/Reports/";
    String outputPath = "";
    String outputPath1 = "";
    double totwriteoffAmt = 0.0;
    double principalAmt = 0.0;

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public void processSingleThreaded(ServiceData serviceData) {
        Session session = new Session(this);
        todayDate = session.getCurrentVariable("!TODAY");
        Contract contract = new Contract(this);  
        
        String filePath1 = filePath+"ContTot_Writeoff.csv";       
        
        data.add(filePath1);
      
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath1))) {
            String line;
            while ((line = reader.readLine()) != null) {                          
                extraList.add(line);           
                
            }
        } catch (Exception e) {
            e.getMessage();
        }
 
        try {

            for (String companyIds : extraList) {
                branch = companyIds;
                branchName = "";
        
                initialiseCompanyInfo(companyIds);

                currentArrList = da.selectRecords(finMnemonic, "AA.ARRANGEMENT", "",
                        "WITH ARR.STATUS EQ AUTH AND CO.CODE EQ " + companyIds);

                for (String contractId : currentArrList) {

                    AaArrangementRecord aaArrRec = new AaArrangementRecord(da.getRecord("AA.ARRANGEMENT", contractId));

                    finalArrList.add(contractId);

                }

                for (String currAaId : finalArrList) {
                    contract.setContractId(currAaId);   
                    testp.add(currAaId);
                    getAaActivityHistoryDets(contract, currAaId);
                    data1.add(testp.toString());
                    testp.clear();
               
                }
                outputPath1 = filePath + "CT.writeoff" + companyIds + ".csv";
                writeToFile(data1, outputPath1);
                 data1.clear();


                List<String> row = new ArrayList<>();

                row.add(branch);
                row.add(branchName);
                row.add(String.valueOf(nofWriteOffLns.size()));
                row.add(String.format("%.2f", totwriteoffAmt));
                row.add(String.format("%.2f", principalAmt));

                retvalues.add(String.join("*", row));

                data.add(row.toString());
                row.clear();

                finalArrList.clear();
                nofWriteOffLns.clear();
                totwriteoffAmt = 0.0;
            }


        } catch (Exception e) {
            e.getMessage();
        }

        outputPath = filePath + "ContTotf" + ".csv";
        writeToFile(data, outputPath);
        data.clear();
      
        
    }

    public void initialiseCompanyInfo(String companyIds) {

        try {
            CompanyRecord companyObj = new CompanyRecord(da.getRecord("COMPANY", companyIds));
            branch = companyIds;
            finMnemonic = companyObj.getFinancialMne().getValue();
            mnemonic = companyObj.getCustomerMnemonic().getValue();
            branchName = companyObj.getCompanyName().get(0).getValue();

        } catch (Exception e1) {
            e1.getMessage();
        }

    }

    public void getAaActivityHistoryDets(Contract contract, String contractId) {

        contract.setContractId(contractId);
        try {
            AaActivityHistoryRecord aaActHisRec = new AaActivityHistoryRecord(
                    da.getRecord(finMnemonic, "AA.ACTIVITY.HISTORY", "", contractId));
            for (EffectiveDateClass effectiveDate : aaActHisRec.getEffectiveDate()) {
                for (ActivityRefClass activeRef : effectiveDate.getActivityRef()) {
                    String activity = activeRef.getActivity().getValue();
                    if (activity.equals("LENDING-CHARGEOFF-ARRANGEMENT")) {
                        nofWriteOffLns.add(activity);
                        String curaAccountCo = getBalance(contract, "CURACCOUNTCO", "BOOKING");
                        testp.add(curaAccountCo);
                        totwriteoffAmt += Double.parseDouble(curaAccountCo);

                    }
                }
            }
        } catch (NumberFormatException e) {
            e.getMessage();
        }

    }

    public String getBalance(Contract contract, String accountType, String bookingType) {

        List<BalanceMovement> movements = contract.getContractBalanceMovements(accountType, bookingType);
        return (movements != null && !movements.isEmpty()) ? movements.get(0).getBalance().toString() : "0";
    }

    public void writeToFile(List<String> data, String filePath) {
        try {
            File file = new File(filePath);
            file.getParentFile().mkdirs();
            boolean fileExists = file.exists();

            try (FileWriter writer = new FileWriter(file, true)) {

                if (!fileExists) {
                    String header = String.join(",", "controlTOT wtr off br", "test doc @@@@");
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
