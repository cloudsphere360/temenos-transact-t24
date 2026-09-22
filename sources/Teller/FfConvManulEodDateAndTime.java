package com.temenos.fusion;

import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.t24.api.records.ebffeodscreen.EbFfEodScreenRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 * TODO: Document me!
 *
 * @author hs115664
 *
 */
public class FfConvManulEodDateAndTime extends Enquiry {

    DataAccess da = new DataAccess(this);
    String commitTime = "";
    EbFfEodScreenRecord eodRecHis = null;
    String timeFormatted = "";

    private static final FusionFileLogger manulEodDateAndTime = FusionFileLogger
            .getLogger(FfConvManulEodDateAndTime.class);

    @Override
    public String setValue(String value, String currentId, TStructure currentRecord,
            List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {
        manulEodDateAndTime.info("FfConvManulEodDateAndTime is triggered" + currentId + "***" + "Value:" + value);
        try {
            manulEodDateAndTime.info("Entring the Try and checking the His rec");
            eodRecHis = new EbFfEodScreenRecord(da.getHistoryRecord("EB.FF.EOD.SCREEN", currentId));
            manulEodDateAndTime.info("eodRecHis:" + eodRecHis.toString());

            String hisCommitTime = eodRecHis.getDateTime(0);
            manulEodDateAndTime.info("hisCommitTime His Rec:" + hisCommitTime + "length:" + hisCommitTime.length());

            String time = hisCommitTime.substring(hisCommitTime.length() - 4);
            timeFormatted = time.substring(0, 2) + ":" + time.substring(2);

            manulEodDateAndTime.info("timeFormatted His Rec:" + timeFormatted);

        } catch (Exception e) {
            EbFfEodScreenRecord eodRec = new EbFfEodScreenRecord(da.getRecord("EB.FF.EOD.SCREEN", currentId));
            String liveCommitTime = eodRec.getDateTime(0);
            manulEodDateAndTime.info("liveCommitTime Live Rec:" + liveCommitTime + "**" + e.getMessage());

            String liveTime = liveCommitTime.substring(liveCommitTime.length() - 4);
            timeFormatted = liveTime.substring(0, 2) + ":" + liveTime.substring(2);
            manulEodDateAndTime.info("timeFormatted Live Rec:" + timeFormatted);
        }
        return timeFormatted;
    }
}
