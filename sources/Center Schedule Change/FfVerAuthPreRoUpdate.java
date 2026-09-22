package com.temenos.fusion;

import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import com.temenos.api.TStructure;
import com.temenos.logging.facade.Logger;
import com.temenos.logging.facade.LoggerFactory;
import com.temenos.t24.api.hook.system.RecordLifecycle;

import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;
import com.temenos.t24.api.tables.ebffcentreroperiod.EbFfCentreRoPeriodTable;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.records.ebffcentredetail.EbFfCentreDetailRecord;
import com.temenos.t24.api.records.ebffcentreroperiod.EbFfCentreRoPeriodRecord;

public class FfVerAuthPreRoUpdate extends RecordLifecycle{  
    
    private static final String L3API = "L3API";
    private static final Logger logger = LoggerFactory.getLogger(L3API);
    
    
    
    @Override
    public void defaultFieldValues(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
        
        Session ssObj = new Session(this);   
        DataAccess daObj = new DataAccess(this);  
        EbFfCentreDetailRecord ffCentRec = new EbFfCentreDetailRecord(currentRecord);    
        try {
       
        if (!ffCentRec.getCurrNo().equals("")) {           
        
        EbFfCentreDetailRecord centreLiveObj  = new EbFfCentreDetailRecord(
                daObj.getRecord("EB.FF.CENTRE.DETAIL", currentRecordId));     
        
        logger.info("ffCentRec " + ffCentRec);        
       
            String currentRo = ffCentRec.getCurrentRo().getValue(); 
            String previousRo = centreLiveObj.getCurrentRo().getValue();        
            String todayValue = ssObj.getCurrentVariable("!TODAY");
            String  roEndDate = subtractOneDay(todayValue);
            if (!currentRo.equals(previousRo) ) {
                ffCentRec.setPreviousRo(previousRo);                
                ffCentRec.setPreRoEndDate(roEndDate);               
            
            }
        }
        } catch (Exception e) {
            e.getMessage();
            
        }       
        
        currentRecord.set(ffCentRec.toStructure());
    }





    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {
        
        
    try {
        
        logger.info("ffCentRec routine triggered ");
        
             Session ssObj = new Session(this);   
        DataAccess daObj = new DataAccess(this);             
        EbFfCentreDetailRecord centrecrutObj  = new EbFfCentreDetailRecord(currentRecord);
        if (!centrecrutObj.getCurrNo().equals("")) {  
        
        
        EbFfCentreDetailRecord centreLiveObj  = new EbFfCentreDetailRecord(
                daObj.getRecord("EB.FF.CENTRE.DETAIL", currentRecordId));
        
        EbFfCentreRoPeriodTable roPeriodTable = new EbFfCentreRoPeriodTable(this);
       
        EbFfCentreDetailRecord ffCentRec = new EbFfCentreDetailRecord(currentRecord);  
        EbFfCentreRoPeriodRecord ffRoPeriodRec = new EbFfCentreRoPeriodRecord();
        
        logger.info("ffCentRec " + ffCentRec);
        
        String currentRo = ffCentRec.getCurrentRo().getValue();   
       
        String previousRo = centreLiveObj.getCurrentRo().getValue();
        String preStartDate = centreLiveObj.getStartDate().getValue();  
        String todayValue = ssObj.getCurrentVariable("!TODAY");
        String  roEndDate = subtractOneDay(todayValue);
        
        //CENTRE ID-PREVIOUS.RO-PREVIOUS START.DATE
        
        String centRoTabeId = currentRecordId+"-"+previousRo+"-"+preStartDate;
        
        logger.info("centRoTabeId " + centRoTabeId);
        
        if (!currentRo.equals(previousRo) ) {
            
            ffRoPeriodRec.setCentre(currentRecordId);
            ffRoPeriodRec.setPreviousRo(previousRo);
            ffRoPeriodRec.setStartDate(preStartDate);
            ffRoPeriodRec.setEndDate(roEndDate);
            roPeriodTable.write(centRoTabeId, ffRoPeriodRec);
        
        
        }       
        }
         
       } catch (Exception e) {
            e.getMessage();
            

        } 
        
    }
    
    public String subtractOneDay(String yyyymmdd) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

        // Parse the input string to a Date object

        Date date = sdf.parse(yyyymmdd);
 
        // Use Calendar to subtract one day

        Calendar cal = Calendar.getInstance();

        cal.setTime(date);

        cal.add(Calendar.DAY_OF_MONTH, -1);
 
        // Format back to YYYYMMDD

        return sdf.format(cal.getTime());
 
    }
}
