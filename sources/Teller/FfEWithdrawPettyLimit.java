package com.temenos.fusion;

import java.util.List;

import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.t24.api.system.Session;

/**
 *
 * @author hs115664
 *
 */
public class FfEWithdrawPettyLimit extends Enquiry {

    private static final FusionFileLogger withdrawPettyLimit = FusionFileLogger.getLogger(FfEWithdrawPettyLimit.class);

    Session ss = new Session(this);

    @Override
    public List<FilterCriteria> setFilterCriteria(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {
        withdrawPettyLimit.info("FfEWithdrawPettyLimit is triggered" + filterCriteria.toString());
        try {
            FilterCriteria fc = new FilterCriteria();

            String todayDate = ss.getCurrentVariable("!TODAY");
            String month = todayDate.substring(4, 6);
            String year = todayDate.substring(0, 4);
            String mmyr = month + year;
            withdrawPettyLimit.info("mmyr" + mmyr);

            fc.setFieldname("@ID");
            fc.setOperand("CT");
            fc.setValue(mmyr);
            withdrawPettyLimit.info("fc" + fc.toString());

            filterCriteria.add(fc);
        } catch (Exception e) {
            withdrawPettyLimit.error("FfEWithdrawPettyLimit is error" + e.getMessage());
        }

        return filterCriteria;
    }

}
