package com.temenos.fusion;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import com.temenos.api.TStructure;
import com.temenos.api.TValidationResponse;
import com.temenos.logging.facade.Logger;
import com.temenos.logging.facade.LoggerFactory;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebffloansluc.EbFfLoansLucRecord;
import com.temenos.t24.api.records.ebffreadlistcustomer.EbFfReadlistCustomerRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;


public class FfVerLucVerficaCheck extends RecordLifecycle{ 
    String roLnUtilStatus = "" ;
    String bmLnUtlStatus = "" ;
    String roLnActCat = "" ;
    DataAccess daObj = new DataAccess(this);  
    Session sn = new Session(this);
    EbFfReadlistCustomerRecord ffreadrec = new EbFfReadlistCustomerRecord(this);
    
    private static final String L3API = "L3API";
    private static final Logger logger = LoggerFactory.getLogger(L3API);
   
    
    @Override
    public void defaultFieldValues(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) { 
   
        
        EbFfLoansLucRecord ffLucRec = new EbFfLoansLucRecord(currentRecord); 
        
        try {    
            
            if (!ffLucRec.getCurrNo().equals("")) {           
            
                EbFfLoansLucRecord centreLiveObj  = new EbFfLoansLucRecord(
                        daObj.getRecord("EB.FF.LOANS.LUC", currentRecordId)); 
                
                logger.info("centreLiveObj " + centreLiveObj);  
                
            roLnUtilStatus =  ffLucRec.getRoLnUtilStatus().getValue(); 

            logger.info("RoLnUtilStatus " + ffLucRec + "$$$$$$$$$$$$$" +roLnUtilStatus);     
                    
            if (roLnUtilStatus.equals("Fully Utilised")) {
                ffLucRec.setRoLucRating("Good");           
            }
            if(roLnUtilStatus.equals("Partially Utilised")){
                ffLucRec.setRoLucRating("Satisfactory");  
            }
            if(roLnUtilStatus.equals("Zero Utilised")){
                ffLucRec.setRoLucRating("Bad");  
            }

            bmLnUtlStatus = ffLucRec.getBmLnUtlStatus().getValue();

            logger.info("BmLnUtlStatus " + ffLucRec + "$$$$$$$$$$$$$" +bmLnUtlStatus);

            try{
                
            if (bmLnUtlStatus.equals("Fully Utilised")) {
                ffLucRec.setBmLucRating("Good");          
                
            }
            if(bmLnUtlStatus.equals("Partially Utilised")){
                ffLucRec.setBmLucRating("Satisfactory");  
            }
            if(bmLnUtlStatus.equals("Zero Utilised")){
                ffLucRec.setBmLucRating("Bad");  
            }
            }catch (Exception e){

                logger.info("utlierror LUC ", e);
                 e.getMessage();

            }
            
            }
        } catch (Exception e)             

        {
            logger.info("utlierror LUC ", e);
            e.getMessage();
            
        }
     
        logger.info("RoLnUtilStatus " + ffLucRec);
        currentRecord.set(ffLucRec.toStructure());
        
        
    }
    
    

    @Override
    public TValidationResponse validateRecord(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
     
                  
        
        EbFfLoansLucRecord ffLucRec = new EbFfLoansLucRecord(currentRecord);

        if (!ffLucRec.getRoLucDate().getValue().isEmpty()) {            
        String roLucDate = ffLucRec.getRoLucDate().getValue();
      
        String roLnCreatDate = ffLucRec.getLoanCreationDate().getValue();
    
        Date loancreationDt = changeDate(roLnCreatDate);
        Date roLucDt = changeDate(roLucDate);
        
        if ((roLucDt != null) && (loancreationDt.compareTo(roLucDt) > 0)) {
            ffLucRec.getRoLucDate().setError("EB-FF.LUC.DATE.LT.CREATION");
        }
        }
        
       
            try {
                if (!ffLucRec.getRoLnUtilAmt().getValue().isEmpty()) {
                      String roLnUtilAmt = ffLucRec.getRoLnUtilAmt().getValue();
                String roLnDisbAmt = ffLucRec.getRoLnDisbAmt().getValue();
                    BigDecimal exceedsEligibleAmountBy = new BigDecimal(roLnUtilAmt).subtract(new BigDecimal(roLnDisbAmt));
                    logger.info("exceedsEligibleAmountBy"+exceedsEligibleAmountBy);
                    if (exceedsEligibleAmountBy.doubleValue() > 0) {
                    
                    ffLucRec.getRoLnUtilAmt().setError("EB-FF.LUC.UTILISE.AMT.ERROR");
                    }
                }
            } catch (Exception e) {
                logger.error("Error : " + e.getMessage(), e);
            }
       
     currentRecord.set(ffLucRec.toStructure());
    return ffLucRec.getValidationResponse();

    }  
    
     private Date changeDate(String finalDate) {
    Date formattedDate = new Date();
    try {
        String dataPattern = "yyyyMMdd";
        formattedDate = new SimpleDateFormat(dataPattern).parse(finalDate);        
       
        return formattedDate;
    } catch (ParseException e) {
       
    }
    return formattedDate;
     }
}
  