package com.temenos.fusion;

import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.t24.api.system.DataAccess;

public class FfconvUserCompanyCode extends Enquiry {
 
    DataAccess da = new DataAccess(this);
    String companyCodeVal = "";

    @Override
    public String setValue(String value, String currentId, TStructure currentRecord,
            List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {


        if (value.equals("ALL")) {
            logger.info("Inside if");
            companyCodeVal = "HEAD.OFFICE";

        } else {
            logger.info("Inside else");
            companyCodeVal = value;


        }

        return companyCodeVal;
    }
}

