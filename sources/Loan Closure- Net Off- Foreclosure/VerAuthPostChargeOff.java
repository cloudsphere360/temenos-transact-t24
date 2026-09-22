package com.temenos.fusion;

import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.logging.facade.Logger;
import com.temenos.logging.facade.LoggerFactory;
import com.temenos.t24.api.arrangement.Product;
import com.temenos.t24.api.arrangement.PropertyClass;
import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaarrangementactivity.AaArrangementActivityRecord;
import com.temenos.t24.api.records.aaarrangementactivity.FieldNameClass;
import com.temenos.t24.api.records.aaproductcatalog.AaProductCatalogRecord;
import com.temenos.t24.api.records.ebffwrtchgoffactivity.EbFfWrtChgOffActivityRecord;
import com.temenos.t24.api.records.ebffwrtchgoffactivity.WaiveOffOptionsClass;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;


public class VerAuthPostChargeOff extends RecordLifecycle{
    private static final String L3API = "L3API";
    private static final Logger verLogger = LoggerFactory.getLogger(L3API);
    String amortActivityId="LENDING-ADJUST.AMORT-BAL.MAINTAIN";
    String fullChgActivity="LENDING-CHARGEOFF-ARRANGEMENT";
    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {
        
        try {
            verLogger.info("inside chargeoff");
            Session ssObj=new Session(this);
            DataAccess daObj=new DataAccess(this);
            Contract contractObj = new Contract(this);
            String waiveOption="";
            String compMne=ssObj.getCompanyRecord().getFinancialMne().getValue();
            EbFfWrtChgOffActivityRecord wrtChgOffRecObj=new EbFfWrtChgOffActivityRecord(currentRecord);
            String arrId=wrtChgOffRecObj.getArrangementId().getValue();
            List<WaiveOffOptionsClass> waiveOptions = wrtChgOffRecObj.getWaiveOffOptions();
            if(!waiveOptions.isEmpty()) {
            waiveOption= waiveOptions.get(waiveOptions.size()-1).getWaiveOffOptions().getValue();
            }
           String fullOutWaiver=wrtChgOffRecObj.getFullOutstandingWaiver().getValue();

           if(waiveOption.equals("PROCESSING FEE WAIVER")) {
               String processingFee= waiveOptions.get(waiveOptions.size()-1).getAmount().getValue();
               verLogger.info("processingFee "+processingFee);
               
               if(!processingFee.isEmpty()&&!arrId.isEmpty()) {
                   
               
                       AaArrangementRecord aaObj = new AaArrangementRecord(
                               daObj.getRecord(compMne, "AA.ARRANGEMENT", "", arrId));
                       contractObj.setContractId(arrId);
                       triggerAmortActivity(currentRecords,transactionData,
                               amortActivityId,  aaObj,  arrId,processingFee);
                       
                   
               }
           
           
           
           }else if(fullOutWaiver.equals("YES")) {
               AaArrangementRecord aaObj = new AaArrangementRecord(
                       daObj.getRecord(compMne, "AA.ARRANGEMENT", "", arrId));
               contractObj.setContractId(arrId);
               triggerFullChgOffActivity(currentRecords,transactionData,
                       fullChgActivity,  aaObj,  arrId,contractObj);
               
           }
            
        }catch (Exception e) {
            verLogger.info(e.getMessage());
        }
    
    }
    private void triggerAmortActivity(List<TStructure> currentRecords, List<TransactionData> transactionData,
            String activityId, AaArrangementRecord aaObj, String arrId, String processingFee) {
        try {
            String coCode = aaObj.getCoCodeRec().getValue();
            String prodId = aaObj.getProduct().get(0).getProduct().getValue();
        
            AaArrangementActivityRecord aaaObj = new AaArrangementActivityRecord(this);
            aaaObj.setArrangement(arrId);
            aaaObj.setActivity(activityId);
            Product prod = new Product(this);
            prod.setProductId(prodId);
            AaProductCatalogRecord aaPrd = prod.getProduct();
            PropertyClass propClass = new PropertyClass(this);
            propClass.setPropertyClassId("BALANCE.MAINTENANCE");
            List<String> propList = propClass.getPropertyIdsForProduct(aaPrd);
    
            com.temenos.t24.api.records.aaarrangementactivity.PropertyClass balClass = new com.temenos.t24.api.records.aaarrangementactivity.PropertyClass();
           
            balClass.setProperty(propList.get(0));
            FieldNameClass fld1 = new FieldNameClass();
                   
                    fld1.setFieldName("NEW.AMORT.AMT:1." +1 );
                    fld1.setFieldValue(processingFee);
                   
                    balClass.setFieldName(fld1, 0);
            TransactionData syncTransactionData = new TransactionData();
            syncTransactionData.setVersionId("AA.ARRANGEMENT.ACTIVITY,INSURANCE.UPD");
            syncTransactionData.setTransactionId("/");
            syncTransactionData.setSourceId("OFS.INSURANCE.UPD");
            syncTransactionData.setCompanyId(coCode);
            transactionData.add(syncTransactionData);
            aaaObj.setProperty(balClass, 0);
            currentRecords.add(aaaObj.toStructure());
        } catch (Exception e) {
            verLogger.info(e.getMessage());

        }

    }

    private void triggerFullChgOffActivity(List<TStructure> currentRecords, List<TransactionData> transactionData,
            String activityId, AaArrangementRecord aaObj, String arrId, Contract contractObj) {
        try {
            contractObj.setContractId(arrId);
            String coCode = aaObj.getCoCodeRec().getValue();
            String prodId = aaObj.getProduct().get(0).getProduct().getValue();
          
            AaArrangementActivityRecord aaaObj = new AaArrangementActivityRecord(this);
            aaaObj.setArrangement(arrId);
            aaaObj.setActivity(activityId);
            Product prod = new Product(this);
            prod.setProductId(prodId);
            TransactionData syncTransactionData = new TransactionData();
            syncTransactionData.setVersionId("AA.ARRANGEMENT.ACTIVITY,INSURANCE.UPD");
            syncTransactionData.setTransactionId("/");
            syncTransactionData.setSourceId("OFS.INSURANCE.UPD");
            syncTransactionData.setCompanyId(coCode);
            transactionData.add(syncTransactionData);
            currentRecords.add(aaaObj.toStructure());
        } catch (Exception e) {
            verLogger.info(e.getMessage());

        }

    }

    
    

}
