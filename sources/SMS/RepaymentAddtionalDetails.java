package com.temenos.fusion;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.temenos.api.TStructure;
//import com.temenos.logging.facade.Logger;
//import com.temenos.logging.facade.LoggerFactory;
import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.de.deliveryhook.Field;
import com.temenos.t24.api.complex.de.deliveryhook.MultiValue;
import com.temenos.t24.api.hook.system.Delivery;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaarrangement.LinkedApplClass;
import com.temenos.t24.api.records.aaarrangementactivity.AaArrangementActivityRecord;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

public class RepaymentAddtionalDetails extends Delivery{

    Session sn = new Session(this);
    DataAccess da = new DataAccess(this);
    Contract contractApi = new Contract(this);
    //private static final String L3API = "L3API";
    //private static final Logger LOGGER = LoggerFactory.getLogger(L3API);   
    
    @Override
    
    
    public List<Field> mapAdditionalDataToMessageType(TStructure record1, TStructure record2, TStructure record3,
            TStructure record4, TStructure record5, TStructure record6, TStructure record7, TStructure record8,
            String mappingKey, TStructure record11) {
        // TODO Auto-generated method stub
        
       System.out.println("rtn tiggered");
        
        List<Field> finalarray = new ArrayList<>();

        Field field1 = new Field(); //Account No
        Field field2 = new Field(); //Repayment Amount
        Field field3 = new Field(); //Txn Contract Id
        Field field4 = new Field(); //Activity
        Field field5 = new Field(); //payment mode
        Field field6 = new Field(); //Recipit no
        Field field7 = new Field(); //customer Name
        Field field8 = new Field(); //customer id
        Field field9 = new Field(); //Arr id
        Field field10 = new Field();
        Field field11 = new Field();
        
        try {
            
        AaArrangementRecord aARecord = new AaArrangementRecord(record1);
        AaArrangementActivityRecord aAARecord = new AaArrangementActivityRecord(record2);
        
        String arrId = "";
        String activity = "";
        String txnContId = "";
        String origAmtLcy = "";
        String paymentMode = "";
        String recNo = "";
        String cusName = "";
        String cusSname = "";
        String cusId = "";
        String effDate = "";
        String UrlLink = "";
        
        String Mne = sn.getCompanyRecord().getFinancialMne().toString();
        System.out.println("Mne"+ Mne);
        
        arrId = aAARecord.getArrangement().getValue();
        System.out.println("arrId"+ arrId);
        
        txnContId = aAARecord.getTxnContractId().getValue();
        System.out.println("txnContId" + txnContId);
        
        origAmtLcy = aAARecord.getOrigTxnAmtLcy().getValue();
        System.out.println("origAmtLcy"+ origAmtLcy);
        
        activity = aAARecord.getActivity().getValue();
        System.out.println("activity"+ activity);
        
        contractApi.setContractId(arrId);
        String accountId = getLinkedAppl(aARecord);
        System.out.println("accountId"+ accountId);
        
        cusId = aARecord.getCustomer().get(0).getCustomer().getValue();
        System.out.println("cusId"+ cusId);
        
        effDate = aAARecord.getEffectiveDate().getValue();
        System.out.println("cusId"+ cusId);
        
        FundsTransferRecord fT = new FundsTransferRecord(da.getRecord(Mne, "FUNDS.TRANSFER", "", txnContId));
        
        paymentMode = fT.getLocalRefField("FF.PYMT.MODE").getValue();
        System.out.println("paymentMode"+ paymentMode);
        
        recNo = fT.getLocalRefField("FF.COLL.RCPT.NO").getValue();
        System.out.println("recNo" + recNo);
        
        UrlLink = fT.getLocalRefField("FF.URL.LINK").getValue();
        System.out.println("UrlLink"+ UrlLink);
        
        CustomerRecord cust = new CustomerRecord(da.getRecord("CUSTOMER", cusId ));
        
        cusSname = cust.getShortName().get(0).getValue();
        cusName = cust.getName1().get(0).getValue();
        
        String fullName = cusSname.concat(" ").concat(cusName);
        System.out.println("fullName"+ fullName);
        
        MultiValue repayAmt = new MultiValue();
        repayAmt.setValue(origAmtLcy);
        field1.addMultiValues(repayAmt);
        
        MultiValue loanAcct = new MultiValue();
        loanAcct.setValue(accountId);
        field2.addMultiValues(loanAcct);
        
        MultiValue tContId = new MultiValue();
        tContId.setValue(txnContId);
        field3.addMultiValues(tContId);
        
        MultiValue actActivity = new MultiValue();
        actActivity.setValue(activity);
        field4.addMultiValues(actActivity);
        
        MultiValue payMethod = new MultiValue();
        payMethod.setValue(paymentMode);
        field5.addMultiValues(payMethod);
        
        MultiValue payRec = new MultiValue();
        payRec.setValue(recNo);
        field6.addMultiValues(payRec);
        
        MultiValue custFullName = new MultiValue();
        custFullName.setValue(fullName);
        field7.addMultiValues(custFullName);
        
        MultiValue custId = new MultiValue();
        custId.setValue(cusId);
        field8.addMultiValues(custId);

        MultiValue aaId = new MultiValue();
        aaId.setValue(arrId);
        field9.addMultiValues(aaId);
        
        MultiValue effectiveDate = new MultiValue();
        LocalDate billDates = LocalDate.parse(effDate, DateTimeFormatter.ofPattern("yyyyMMdd"));
        String orgBillDate = billDates.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        effectiveDate.setValue(orgBillDate);
        field10.addMultiValues(effectiveDate);
        
        MultiValue fturlLink = new MultiValue();
        fturlLink.setValue(UrlLink);
        field11.addMultiValues(fturlLink);
        
        finalarray.add(field1);
        finalarray.add(field2);
        finalarray.add(field3);
        finalarray.add(field4);
        finalarray.add(field5);
        finalarray.add(field6);
        finalarray.add(field7);
        finalarray.add(field8);
        finalarray.add(field9);
        finalarray.add(field10);
        finalarray.add(field11);
        }
        
        catch(Exception e) {
            e.getMessage();
        }
        
        return finalarray;
    }
    
    public String getLinkedAppl(AaArrangementRecord aARecord) {
        String linkedApplId = "";
        
        for (LinkedApplClass linkappl : aARecord.getLinkedAppl()) {
            linkedApplId = linkappl.getLinkedApplId().getValue();
        }
        return linkedApplId;
    }

}
