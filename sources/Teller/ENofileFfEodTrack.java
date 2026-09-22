package com.temenos.fusion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.temenos.api.exceptions.T24CoreException;
import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.t24.api.records.dates.DatesRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Date;

/**
 *
 * @author jh116454
 *
 */
public class ENofileFfEodTrack extends Enquiry {

    private static final FusionFileLogger yEODTrackLog = FusionFileLogger.getLogger(ENofileFfEodTrack.class);

    List<String> yEODTrackArry = new ArrayList<>();
    List<String> yAllEODIds = null;

    DataAccess yDataAcc = new DataAccess();

    String yBranchID = "";
    String yValueDt = "";

    String yEODRecID = "";
    String yTodDt = "";

    DateTimeFormatter yFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    LocalDate yFormDate = null;

    LocalDate yFormRODate = null;
    String yPreviousMonth = "";
    LocalDate yFormPrevMth = null;
    LocalDate yFormTodDate = null;

    BigDecimal yTotPendCashB = BigDecimal.ZERO;
    BigDecimal yTotPrevPendCollB = BigDecimal.ZERO;

    @Override
    public List<String> setIds(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {

        String yEnqName = enquiryContext.getEnquiryId();
        yEODTrackLog.info(" ------- STARTS -------");
        yEODTrackLog.info(" Enquiry Name -> " + yEnqName);

        Date yDate = new Date(this);
        DatesRecord yDateRec = yDate.getDates();
        yTodDt = yDateRec.getToday().getValue();
        yFormTodDate = LocalDate.parse(yTodDt, yFormatter);

        for (FilterCriteria criteria : filterCriteria) {

            if (criteria.getFieldname().equals("VALUE.DATE")) {
                yValueDt = criteria.getValue();
                yEODTrackLog.info(" Value Dt -> " + yValueDt);
            }

            if (criteria.getFieldname().equals("BRANCH.ID")) {
                yBranchID = criteria.getValue();
                yEODTrackLog.info(" Branch ID -> " + yBranchID);
            }
        }

        if ((!yValueDt.equals("")) && (yBranchID.equals(""))) {
            if (yValueDt.equals(yTodDt)) {
                yEODTrackLog.info(" ------- Inside today Date-------");
                throw new T24CoreException("", "Date should be Lesser than Today");
            } else {

                yAllEODIds = yDataAcc.selectRecords("", "EB.FF.EOD.SCREEN", "", "WITH EOD.DATE EQ " + yValueDt);
                yEODTrackLog.info(" yAllRoIds -> " + yAllEODIds);

                for (String yEODID : yAllEODIds) {
                    yEODRecID = yEODID;
                    yEODTrackLog.info(" yEODRecID only Date -> " + yEODRecID);

                    yEODTrackArry.add(yEODRecID + "*" + yEODRecID);
                }
            }
        }

        if ((!yValueDt.equals("")) && (!yBranchID.equals(""))) {

            yEODRecID = yBranchID + "-" + yValueDt;
            yEODTrackLog.info(" yEODRecID Branch n Date-> " + yEODRecID);

            yEODTrackArry.add(yEODRecID + "*" + yEODRecID);
        }
        yEODTrackLog.info(" yEODTrackArry -> " + yEODTrackArry);
        yEODTrackLog.info(" ------- ENDS 10/06/26 13.23-------");
        return yEODTrackArry;
    }
}
