package com.temenos.fusion;

import java.util.List;

import com.temenos.api.exceptions.T24CoreException;
import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.t24.api.records.dates.DatesRecord;
import com.temenos.t24.api.system.Date;
import com.temenos.t24.api.system.Session;

/**
 *
 * @author hs115664
 *
 */
public class FfEBuildPreviousEodData extends Enquiry {

    private static final FusionFileLogger buildPreviousEodData = FusionFileLogger
            .getLogger(FfEBuildPreviousEodData.class);
    Session ses = new Session(this);
    Date yDate = new Date(this);
    String fieldName = "";
    String fieldVault = "";
    String lastworkingDate = "";
    DatesRecord yDateRec = yDate.getDates();
    String yTodDt = yDateRec.getToday().getValue();

    @Override
    public List<FilterCriteria> setFilterCriteria(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {

        buildPreviousEodData.info("FfEBuildPreviousEodData is triggered" + filterCriteria);

        for (FilterCriteria filter : filterCriteria) {

            fieldName = filter.getFieldname();
            fieldVault = filter.getValue();
            buildPreviousEodData.info("fieldName" + fieldName + "**" + fieldVault);

            if (fieldName.equals("CREDIT.VALUE.DATE") && (!fieldVault.isEmpty()) && (fieldVault.equals(yTodDt))) {
                throw new T24CoreException("", "Date should be Lesser than Today");
            }
        }

        return filterCriteria;
    }

}
