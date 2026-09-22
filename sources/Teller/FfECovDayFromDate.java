package com.temenos.fusion;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.t24.api.records.ebffeodscreen.EbFfEodScreenRecord;

/**
 *
 * @author hs115664
 *
 */
public class FfECovDayFromDate extends Enquiry {

    private static final FusionFileLogger dayFromDate = FusionFileLogger.getLogger(FfECovDayFromDate.class);
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public String setValue(String value, String currentId, TStructure currentRecord,
            List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {
        dayFromDate.info("FfECovDayFromDate is triggered" + currentId);

        try {
            EbFfEodScreenRecord eodRec = new EbFfEodScreenRecord(currentRecord);
            String eodDate = eodRec.getEodDate().getValue();
            dayFromDate.info("eodDate" + eodDate);
            LocalDate date = LocalDate.parse(eodDate, formatter);
            DayOfWeek dayOfWeek = date.getDayOfWeek();
            dayFromDate.info("dayOfWeek" + dayOfWeek.toString());
            value = dayOfWeek.toString();
            dayFromDate.info("value" + value);
        } catch (Exception e) {
            dayFromDate.error("FfECovDayFromDate error" + e.getMessage());
        }

        return value;
    }
}
