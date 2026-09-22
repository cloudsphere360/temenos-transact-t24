package com.temenos.fusion;


import java.util.ArrayList;
import java.util.List;
import com.temenos.api.TStructure;
import com.temenos.logging.facade.Logger;
import com.temenos.logging.facade.LoggerFactory;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.records.ebffloansluc.EbFfLoansLucRecord;
import com.temenos.t24.api.records.ebffreadlistcustomer.EbFfReadlistCustomerRecord;
import com.temenos.t24.api.records.user.UserRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/**
 *
 * @author Hemalatha G
 *
 */
public class FfVerFfReadListCustomer extends RecordLifecycle{
    
    EbFfLoansLucRecord ffLucRec = null ;
    String roLnActCat = "" ;
    String bmLnActCat = "" ; 
    String roLucPurpose = "" ;
    String bmLucPurpose = "" ;
    
    DataAccess daObj = new DataAccess(this);
    String cusId = "";
    String shortName = "";
    String name = "";
    String date;
    String coCode = "";
    String entryDate = "";
    String userID = "" ;
    String username = "" ;
    Session sn = new Session(this);
    
    List<String> finalArrList = new ArrayList<>();
   
    private static final String L3API = "L3API";
    private static final Logger logger = LoggerFactory.getLogger(L3API);

    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {
        
        ffLucRec = new EbFfLoansLucRecord(currentRecord);
        
             
        try {    
            
            if (!ffLucRec.getCurrNo().equals("")) {           
            
                roLnActCat = ffLucRec.getRoLnActCat().getValue();
                roLucPurpose = ffLucRec.getRoLucPurpose().getValue();
                
                cusId = ffLucRec.getCustomer().getValue();
                logger.info("RoLnActCat " + ffLucRec + "$$$$$$$$$$$$$" +roLnActCat);
                
                
                
                bmLnActCat = ffLucRec.getBmLnActCat().getValue() ; 
                bmLucPurpose = ffLucRec.getBmLucPurpose().getValue();
               

                    boolean shouldCall =
                            ("WRONG".equalsIgnoreCase(roLnActCat) && "NO".equalsIgnoreCase(roLucPurpose))
                         || ("WRONG".equalsIgnoreCase(bmLnActCat) && "NO".equalsIgnoreCase(bmLucPurpose));
                    
                    logger.info("shouldCall: ", shouldCall);
                    
                    if (shouldCall) {
                        ofsdatafrom(currentRecordId, transactionData, currentRecords);
                    }

            }
            
        }
            catch (Exception e)             

            {
                logger.info("ReadListCustomer LUC ", e);
                e.getMessage();
                
            }
        
    }



    /**
     * @param currentRecordId
     * @param transactionData
     * @param currentRecords
     */
    private void ofsdatafrom(String currentRecordId, List<TransactionData> transactionData,
            List<TStructure> currentRecords) {
     
            
           try {
            AaArrangementRecord aarec = new AaArrangementRecord(daObj.getRecord("AA.ARRANGEMENT", currentRecordId));
               
                    coCode = aarec.getCoCode();
               
                   cusId = aarec.getCustomer().get(0).getCustomer().getValue();
        } catch (Exception e) {   
            
            logger.info("ReadListCustomer LUC aaa ", e);
                e.getMessage();
                
            
        }           
          
            
             entryDate = sn.getCurrentVariable("!TODAY");
             userID = sn.getUserId();
             UserRecord userrec = new UserRecord(daObj.getRecord("USER",userID));
         username = userrec.getUserName().getValue();
                
                 
           CustomerRecord cusrec = new CustomerRecord(daObj.getRecord(coCode,"CUSTOMER","",cusId));
           
               shortName = cusrec.getShortName().get(0).getValue();
               
               name = cusrec.getName1().get(0).getValue();
               
               date = cusrec.getDateOfBirth().getValue();
               
               updateRecord(cusId,shortName,username, name, date, entryDate, transactionData, currentRecords);

        }
    

    /**
     * @param shortName2
     * @param name2
     * @param date2
     * @param transactionData
     * @param transactionRecord2
     */
    public void updateRecord(String cusId ,String shortName,String username, String name, String date, String entryDate, List<TransactionData> transactionData,
            List<TStructure> currentRecords) {
        
        EbFfReadlistCustomerRecord ffreadrec = new EbFfReadlistCustomerRecord(this);
        ffreadrec.setFirstName(shortName);
        ffreadrec.setFullName(name);
        ffreadrec.setDateOfBirth(date);
        ffreadrec.setEntryDate(entryDate);
        ffreadrec.setReviewDate(entryDate);
        ffreadrec.setReviewerName(username);
        currentRecords.add(ffreadrec.toStructure());
        TransactionData transactionDataObj = new TransactionData();
        transactionDataObj.setVersionId("EB.FF.READLIST.CUSTOMER,INPUT");
        transactionDataObj.setFunction("INPUT");
        transactionDataObj.setSourceId("NETOFF.OFS");
        transactionDataObj.setNumberOfAuthoriser("0");
        transactionDataObj.setTransactionId(cusId);
        transactionData.add(transactionDataObj);
        logger.info("transactionData ", transactionData);
        
    }
}
