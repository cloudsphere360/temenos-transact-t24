package com.bct.fusion.bulk.upload;

import java.util.List;

import com.temenos.api.TStructure;

import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.aaarrangementactivity.AaArrangementActivityRecord;
import com.temenos.t24.api.records.aaarrangementactivity.FieldNameClass;
import com.temenos.t24.api.records.aaarrangementactivity.PropertyClass;
import com.temenos.t24.api.records.ebffaabulkupload.EbFfAaBulkUploadRecord;
import com.temenos.t24.api.system.Session;


/*---------------------------------------------------------------------------------
* * Product          :
* * Developed by     : gopal
* * Routine Type     : 
* * Date             :  
* * Description      :  
* * Attached To      : 
* * EB.API Record ID :
* * In Parameters    : 
* * Out Parameters   :
* * Reference        :
* *--------------------------------------------------------------------------------
* * Revision History :
* *-----------------
* * Date          - <Developer> -  Description
* *---------------------------------------------------------------------------------
* *--------------------------------------------------------------------------------*/
public class FusionBefWaiveOffUpdate extends RecordLifecycle {
Session session = new Session(this);
    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {
        
        EbFfAaBulkUploadRecord aaBulkRec = new EbFfAaBulkUploadRecord(currentRecord);
        String idFormat = aaBulkRec.getArrangement().getValue();
        String arrId = idFormat.split("[-]")[0];
        String effDate = aaBulkRec.getEffectiveDate().getValue();
        String narrative = aaBulkRec.getNarrative().getValue();

        AaArrangementActivityRecord aaaRec = new AaArrangementActivityRecord(this);
        aaaRec.setArrangement(arrId);
        aaaRec.setNarrative(narrative, 0);
        aaaRec.setEffectiveDate(effDate);
        aaaRec.setActivity("LENDING-WRITE.OFF-BAL.MAINTAIN");  

        PropertyClass propertyClass = new PropertyClass();
        FieldNameClass fieldNameClass = new FieldNameClass();
        fieldNameClass.setFieldName("WRITE.OFF");
        fieldNameClass.setFieldValue("YES");
        propertyClass.addFieldName(fieldNameClass);
        propertyClass.setProperty("BAL.MAINTAIN");
        aaaRec.addProperty(propertyClass);

        currentRecords.add(aaaRec.toStructure());

        TransactionData txnData = new TransactionData();
        txnData.setFunction("I");
        txnData.setNumberOfAuthoriser("0");
        txnData.setVersionId("AA.ARRANGEMENT.ACTIVITY,"+idFormat.split("[-]")[1]);
        txnData.setSourceId("FF.BULK.OFS");
        txnData.setCompanyId(session.getCompanyId());
        
        transactionData.add(txnData);
    }


}
