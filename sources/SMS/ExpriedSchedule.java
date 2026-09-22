package com.temenos.fusion;

import java.util.ArrayList;
import java.util.List;
import com.temenos.api.TStructure;
import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.eb.servicehook.ServiceData;
import com.temenos.t24.api.complex.eb.servicehook.SynchronousTransactionData;
import com.temenos.t24.api.complex.eb.servicehook.TransactionControl;
import com.temenos.t24.api.hook.system.ServiceLifecycle;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaarrangementactivity.AaArrangementActivityRecord;
import com.temenos.t24.api.records.aaprddespaymentschedule.AaPrdDesPaymentScheduleRecord;
import com.temenos.t24.api.records.aaprddespaymentschedule.PaymentTypeClass;
import com.temenos.t24.api.records.ebffaacheckexpiredarrangement.EbFfAaCheckExpiredArrangementRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;
import com.temenos.t24.api.tables.ebffaacheckexpiredarrangement.EbFfAaCheckExpiredArrangementTable;

public class ExpriedSchedule extends ServiceLifecycle {

    private static final FusionFileLogger logger = FusionFileLogger.getLogger(ExpriedSchedule.class);

    Session sn = new Session(this);
    DataAccess da = new DataAccess(this);
    String mne = sn.getCompanyRecord().getFinancialMne().toString();
    Contract api = new Contract(this);

    @Override
    public List<String> getIds(ServiceData serviceData, List<String> controlList) {

        List<String> selectionRecord = da.selectRecords(mne, "EB.FF.AA.CHECK.EXPIRED.ARRANGEMENT", "",
                "WITH STATUS EQ PENDING ");
        List<String> updateRecord = new ArrayList<>();

        updateRecord.addAll(selectionRecord);

        logger.info("MNE " + mne);
        logger.info("updateRecord " + updateRecord);
        return updateRecord;
    }

    @Override
    public void updateRecord(String id, ServiceData serviceData, String controlItem,
            TransactionControl transactionControl, List<SynchronousTransactionData> transactionData,
            List<TStructure> records) {
        
        api.setContractId(id);
       
        AaArrangementRecord aar = new AaArrangementRecord(da.getRecord(mne, "AA.ARRANGEMENT", "", id));
        
        String aaStatus = aar.getArrStatus().getValue();
        String coCode = aar.getCoCodeRec().getValue();
        String orgContract = aar.getOrigContractDate().getValue();
        boolean flagExp = false;
        try {
        AaPrdDesPaymentScheduleRecord aaSchedule = new AaPrdDesPaymentScheduleRecord(api.getConditionForProperty("PAYMENT.SCHEDULE"));
        List<PaymentTypeClass> paymentType = aaSchedule.getPaymentType();
        String scheduledpaymentType = "";
        for (int i = paymentType.size() - 1; i >= 0; i--) {
            scheduledpaymentType = paymentType.get(i).getPaymentType().getValue();
            logger.info("scheduledpaymentType " + scheduledpaymentType);
            if(scheduledpaymentType != null && !scheduledpaymentType.isBlank()) {
                flagExp =true;
                break;
            }
            
        }
        }catch(Exception e10){
           logger.info("e10"+e10); 
        }
        boolean isExpired = "EXPIRED".equalsIgnoreCase(aaStatus);
        
        
        logger.info("coCode " + coCode);
        logger.info("orgContract " + orgContract);
        
        if(isExpired && (orgContract == null || orgContract.isBlank() )) {
            
          
            process(transactionData,id,records,coCode);
        }
        if(isExpired && (orgContract != null && !orgContract.isBlank())&& flagExp ) {
            
            process(transactionData,id,records,coCode);
            
        }
        
        EbFfAaCheckExpiredArrangementRecord ebCheckArr = new  EbFfAaCheckExpiredArrangementRecord(da.getRecord(mne,"EB.FF.AA.CHECK.EXPIRED.ARRANGEMENT","" ,id));
        EbFfAaCheckExpiredArrangementTable ebCheckArrTab = new  EbFfAaCheckExpiredArrangementTable(this);
        ebCheckArr.setStatus("COMPLETED");
        try {
            ebCheckArrTab.write(id, ebCheckArr);
        } catch (Exception e10) {
           logger.info("e10"+e10);
        }
       
        
    }

    /**
     * @param aaaRec
     * @param transactionDataObj
     * @param transactionData
     * @param id
     */
    private void process(List<SynchronousTransactionData> transactionData, String id,List<TStructure> records,String coCode) {
        
        AaArrangementActivityRecord aaaRec = new AaArrangementActivityRecord(this);
        
        aaaRec.setArrangement(id);
        
        aaaRec.setActivity("LENDING-CHG.EXP-PAYMENT.SCHEDULE");
        
        records.add(aaaRec.toStructure());

        SynchronousTransactionData transactionDataObj = new SynchronousTransactionData();
        transactionDataObj.setVersionId("AA.ARRANGEMENT.ACTIVITY,AA.EXPIRED");
        transactionDataObj.setFunction("INPUT");
        transactionDataObj.setNumberOfAuthoriser("0");
        transactionDataObj.setSourceId("EXPIRED.OFS");
        transactionDataObj.setCompanyId(coCode);
        transactionData.add(transactionDataObj);
        
    }
}
