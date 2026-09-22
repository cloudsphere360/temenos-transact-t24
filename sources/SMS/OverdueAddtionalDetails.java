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
import com.temenos.t24.api.records.aaaccountdetails.AaAccountDetailsRecord;
import com.temenos.t24.api.records.aaaccountdetails.BillIdClass;
import com.temenos.t24.api.records.aaaccountdetails.BillPayDateClass;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaarrangement.LinkedApplClass;
import com.temenos.t24.api.records.aaarrangementactivity.AaArrangementActivityRecord;
import com.temenos.t24.api.records.aabilldetails.AaBillDetailsRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

public class OverdueAddtionalDetails extends Delivery {

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
            String billAmt = "";
            String activity = "";
            String customerNo = "";

            String Mne = sn.getCompanyRecord().getFinancialMne().toString();
            System.out.println("Mne" + Mne);

            arrId = aAARecord.getArrangement().getValue();
            System.out.println("arrId" + arrId);

            contractApi.setContractId(arrId);
            String accountId = getLinkedAppl(aARecord);

            activity = aAARecord.getActivity().getValue();
            System.out.println("activity" + activity);

            customerNo = aARecord.getCustomer().get(0).getCustomer().getValue();
            System.out.println("customerNo" + customerNo);

            String dueBillDate = "";

            String effectiveDate = aAARecord.getEffectiveDate().getValue();
            System.out.println("effectiveDate" + effectiveDate);

            DateTimeFormatter dformatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            LocalDate dueBillDueDate = LocalDate.parse(effectiveDate, dformatter);
            LocalDate minuOneDay = dueBillDueDate.minusDays(1);
            dueBillDate = minuOneDay.format(dformatter);
            System.out.println("dueBillDate" + dueBillDate);

            List<String> billIds = new ArrayList<>();
            try {
                System.out.println("arrId " + arrId);
                AaAccountDetailsRecord aad = new AaAccountDetailsRecord(
                        da.getRecord(Mne, "AA.ACCOUNT.DETAILS", "", arrId));
                for (BillPayDateClass aabill : aad.getBillPayDate()) {
                    for (BillIdClass aabills : aabill.getBillId()) {
                        String bills = aabills.getBillId().getValue().replace("/", "");
                        billIds.add(bills);
                    }
                }
                System.out.println("billIds" + billIds.toString());
            } catch (Exception e3) {
                System.out.println("Exception" + e3);
            }
            for (String billId : billIds) {

                AaBillDetailsRecord aab = new AaBillDetailsRecord(da.getRecord(Mne, "AA.BILL.DETAILS", "", billId));
                if (aab.getPaymentDate().getValue().equals(dueBillDate)) {
                    billAmt = aab.getOrTotalAmount().getValue();
                    System.out.println("billAmt" + billAmt);
                }
            }

            MultiValue ArrId = new MultiValue();
            ArrId.setValue(arrId);
            field1.addMultiValues(ArrId);

            MultiValue loanAcct = new MultiValue();
            loanAcct.setValue(accountId);
            field2.addMultiValues(loanAcct);

            MultiValue dueAmt = new MultiValue();
            dueAmt.setValue(billAmt);
            field3.addMultiValues(dueAmt);

            MultiValue activityName = new MultiValue();
            activityName.setValue(activity);
            field4.addMultiValues(activityName);

            MultiValue dueDate = new MultiValue();
            LocalDate billDates = LocalDate.parse(dueBillDate, DateTimeFormatter.ofPattern("yyyyMMdd"));
            String orgBillDate = billDates.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            dueDate.setValue(orgBillDate);
            field5.addMultiValues(dueDate);

            MultiValue effDate = new MultiValue();
            LocalDate ebillDates = LocalDate.parse(effectiveDate, DateTimeFormatter.ofPattern("yyyyMMdd"));
            String eorgBillDate = ebillDates.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            effDate.setValue(eorgBillDate);
            field6.addMultiValues(effDate);

            MultiValue CustNo = new MultiValue();
            CustNo.setValue(customerNo);
            field7.addMultiValues(CustNo);

            finalarray.add(field1);
            finalarray.add(field2);
            finalarray.add(field3);
            finalarray.add(field4);
            finalarray.add(field5);
            finalarray.add(field6);
            finalarray.add(field7);

        } catch (Exception e) {
            System.out.println("Exception" + e);
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
           System.out.println("Exception" + e1);
       }
        return linkedApplId;
    }

}
