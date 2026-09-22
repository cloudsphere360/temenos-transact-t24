package com.temenos.fusion;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import com.temenos.api.TStructure;
import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.de.deliveryhook.Field;
import com.temenos.t24.api.complex.de.deliveryhook.MultiValue;
import com.temenos.t24.api.hook.system.Delivery;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaarrangement.LinkedApplClass;
import com.temenos.t24.api.records.aaarrangementactivity.AaArrangementActivityRecord;
import com.temenos.t24.api.records.company.CompanyRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;
 
public class NpaAccount extends Delivery {
    private static final FusionFileLogger logger = FusionFileLogger.getLogger(NpaAccount.class);
 
    Session sn = new Session(this);
    DataAccess da = new DataAccess(this);
    Contract contractApi = new Contract(this);
 
    @Override
    public List<Field> mapAdditionalDataToMessageType(TStructure record1, TStructure record2, TStructure record3,
            TStructure record4, TStructure record5, TStructure record6, TStructure record7, TStructure record8,
            String mappingKey, TStructure record11) {
 
        List<Field> finalarray = new ArrayList<>();
 
        Field field1 = new Field();
        Field field2 = new Field();
        Field field3 = new Field();
        Field field4 = new Field();
        Field field5 = new Field();
        Field field6 = new Field();
        Field field7 = new Field();
 
        try {
 
            AaArrangementRecord aARecord = new AaArrangementRecord(record1);
            AaArrangementActivityRecord aAARecord = new AaArrangementActivityRecord(record2);
 
            String arrId = "";
            String activity = "";
            String customerNo = "";
            String coCode = "";
 
            String mne = sn.getCompanyRecord().getFinancialMne().toString();
            logger.info("Mne" + mne);
 
            arrId = aAARecord.getArrangement().getValue();
            logger.info("arrId" + arrId);
 
            contractApi.setContractId(arrId);
            String accountId = getLinkedAppl(aARecord);    
 
            activity = aAARecord.getActivity().getValue();
            logger.info("activity" + activity);
 
            customerNo = aARecord.getCustomer().get(0).getCustomer().getValue();
            logger.info("customerNo" + customerNo);
 
 
            String effectiveDate = aAARecord.getEffectiveDate().getValue();
            LocalDate ebillDates = LocalDate.parse(effectiveDate, DateTimeFormatter.ofPattern("yyyyMMdd"));
            String eorgBillDate = ebillDates.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            logger.info("eorgBillDate" + eorgBillDate);
            
            String inputter = aAARecord.getInputter().get(0);
            logger.info("inputter" + inputter);
            
            coCode = aARecord.getCoCodeRec().getValue();
            String branchName = getcoCode(coCode);
 
 
            MultiValue arId = new MultiValue();
            arId.setValue(arrId);
            field1.addMultiValues(arId);

            MultiValue loanAcct = new MultiValue();
            loanAcct.setValue(accountId);
            field2.addMultiValues(loanAcct);
 
            MultiValue activityName = new MultiValue();
            activityName.setValue(activity);
            field3.addMultiValues(activityName);
 
            MultiValue effDate = new MultiValue();
            effDate.setValue(eorgBillDate);
            field4.addMultiValues(effDate);
 
            MultiValue custNo = new MultiValue();
            custNo.setValue(customerNo);
            field5.addMultiValues(custNo);
            
            MultiValue input = new MultiValue();
            input.setValue(inputter);
            field6.addMultiValues(input);
            
            MultiValue company = new MultiValue();
            company.setValue(branchName);
            field7.addMultiValues(company);
 
            finalarray.add(field1);
            finalarray.add(field2);
            finalarray.add(field3);
            finalarray.add(field4);
            finalarray.add(field5);
            finalarray.add(field6);
            finalarray.add(field7);
            
        } catch (Exception e) {
            logger.info("Exception" + e);
        }
 
        return finalarray;
    }
 
    public String getLinkedAppl(AaArrangementRecord aARecord) {
        String linkedApplId = "";
       try{
 
        for (LinkedApplClass linkappl : aARecord.getLinkedAppl()) {
            linkedApplId = linkappl.getLinkedApplId().getValue();
        }
       }catch(Exception e1) {
           logger.info("Exception" + e1);
       }
        return linkedApplId;
    }
    
    public String getcoCode(String coCode) {
    CompanyRecord co = new CompanyRecord(da.getRecord("COMPANY", coCode));
    String branch = "";
    String branchName = "";
        try{ 
            branch = co.getCompanyName().get(0).getValue();
            String[] part = branch.split("\\-");
            branchName = part[0];
           
    }catch(Exception e100){
        logger.info("e100" + e100);
    }
        return branchName;
    }
}