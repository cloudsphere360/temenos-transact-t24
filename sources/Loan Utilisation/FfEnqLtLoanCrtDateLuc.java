package com.temenos.fusion;


import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import java.util.Date;
import java.util.List;
import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.t24.api.records.dates.DatesRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;
/**
 *
 * @author Hemalatha G
 *
 */
public class FfEnqLtLoanCrtDateLuc extends Enquiry {

    DataAccess da = new DataAccess(this);
    Session ss = new Session(this);
    DatesRecord datesRec = null;
    String companyId = "";
    String todaySysDate = "";
    Date todaydate = null;
   
    @Override
    public List<FilterCriteria> setFilterCriteria(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {
      
        companyId = ss.getCompanyId();
       
        try {    
            
            todaySysDate = ss.getCurrentVariable("!TODAY");
           
          
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            LocalDate todayDate = LocalDate.parse(todaySysDate, formatter);
            
            LocalDate dateBeforeToday = todayDate.minusDays(13);             
            String beforetodayDate  = dateBeforeToday.format(formatter); 
            
            
            FilterCriteria fc = new FilterCriteria();
            
            fc.setFieldname("LOAN.CREATION.DATE");
            fc.setOperand("RG");
            fc.setValue(beforetodayDate+" "+todaySysDate);
            filterCriteria.add(fc);            
      
            
        } catch (Exception e) {
            e.getMessage();
        }
        return filterCriteria; 
    }
    
    
    }


 

