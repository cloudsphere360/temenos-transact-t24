package com.temenos.fusion;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.t24.api.records.dates.DatesRecord;
import com.temenos.t24.api.records.ebffloansluc.EbFfLoansLucRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/**
 *
 * @author GokarajuHemalatha
 *
 */

public class FfEnqLucExcepRep extends Enquiry {
  
    private static final String FLD_NAME = "@ID";
    private static final String FLD_OPERAND = "EQ";
    DataAccess daObj = new DataAccess(this);
    Session ssObj = new Session(this);    

    DatesRecord datesRec = null;
   

    @Override
    public List<FilterCriteria> setFilterCriteria(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {
        List<FilterCriteria> finalList = new ArrayList<>();
        FilterCriteria filterVal = new FilterCriteria();
        String fieldVal = "";
        String ludarId = "" ;
        try {
            String compMne = ssObj.getCompanyRecord().getFinancialMne().getValue();           
            List<String> recList = daObj.selectRecords(compMne,"EB.FF.LOANS.LUC" , "","");              
           
            if (!filterCriteria.isEmpty()) {
               
               for (FilterCriteria lucffil : filterCriteria) {
                   if (lucffil.getFieldname().equals("@ID")){
                        ludarId = lucffil.getValue();                         
                        recList.clear();
                        recList.add(ludarId);
                        fieldVal = getLucExcept( recList);
                   }                 
               }
            } else {               
            
                fieldVal = getLucExcept( recList);
              
               
            }

                if ( fieldVal!= null && !fieldVal.isEmpty()) {
                    filterVal.setFieldname(FLD_NAME);
                    filterVal.setOperand(FLD_OPERAND);
                    filterVal.setValue(fieldVal);
                    finalList.add(filterVal);
                } else {
                    filterVal.setFieldname(FLD_NAME);
                    filterVal.setOperand(FLD_OPERAND);
                    filterVal.setValue("No records selected for this selection");
                    finalList.add(filterVal);
                }
            
  

        } catch (Exception e) {
            e.getMessage();
        }

        return finalList;
    }

    private String getLucExcept( List<String> recList) {
        List<String> ebFfIds = new ArrayList<>();
        String todaySysDate = ssObj.getCurrentVariable("!TODAY");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDate todayDate = LocalDate.parse(todaySysDate, formatter);   
        LocalDate dateBeforeToday = todayDate.minusDays(14);
        
        
            for (String id : recList) {

                     
                try {
                    
                EbFfLoansLucRecord ffLucRec = new EbFfLoansLucRecord(daObj.getRecord("EB.FF.LOANS.LUC",id));             
                
                String roLucRating = ffLucRec.getRoLucRating().getValue();            
                String bmLucRating = ffLucRec.getBmLucRating().getValue();  
            
              
            
                if ("Bad".equals(bmLucRating) || "Bad".equals(roLucRating)){                       

                        String loanCrDate = ffLucRec.getLoanCreationDate().getValue();                      
                        LocalDate loanCreatDate = LocalDate.parse(loanCrDate, formatter);                
              
                       if (loanCreatDate.isBefore(dateBeforeToday)){
                    ebFfIds.add(id);
                 
                }

            }
                }
             catch (Exception e) {
                e.getMessage();  
            }
            }
        return String.join(" ", ebFfIds);
    }


}