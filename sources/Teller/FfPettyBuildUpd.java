 package com.temenos.fusion;

import java.util.List;

import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.t24.api.records.dates.DatesRecord;
import com.temenos.t24.api.system.Date;
import com.temenos.t24.api.system.Session;

/**
 * @author jh116454
 * @Attached To: ENQUIRY > FF.PETTY.CASH.DETAILS, ENQUIRY >
 *           FF.PETTY.CASH.IMG.UPD
 * @Attached As: Build Routine > EB.API > FF.PETTY.BUILD.UPD
 * @Description: To Display records for the current company according to selection
 * 
 */

public class FfPettyBuildUpd extends Enquiry {

    private static final FusionFileLogger yPettyBuildLog = FusionFileLogger.getLogger(FfPettyBuildUpd.class);

    @Override
    public List<FilterCriteria> setFilterCriteria(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {

        String yTxnID = "";
        String yDebitDt = "";

        FilterCriteria yTxnIDs = new FilterCriteria();

        for (FilterCriteria criteria : filterCriteria) {
            if (criteria.getFieldname().equals("@ID")) {
                yTxnID = criteria.getValue();
            }
            if (criteria.getFieldname().equals("DEBIT.DATE")) {
                yDebitDt = criteria.getValue();
            }
        }

        yPettyBuildLog.info("Txn ID -> " + yTxnID);
        yPettyBuildLog.info("yDebitDt -> " + yDebitDt);

        Session ySession = new Session(this);
        String companycode = ySession.getCompanyId();

        Date yDate = new Date(this);
        DatesRecord yDateRec = yDate.getDates();
        String yTodDt = yDateRec.getToday().getValue();

        String yTodayID = companycode + "-" + yTodDt;

        if ((!yDebitDt.equals("")) && (yTxnID.equals(""))) {
            String yDateVal = companycode + "-" + yDebitDt;
            yTxnIDs.setFieldname("@ID");
            yTxnIDs.setOperand("EQ");
            yTxnIDs.setValue(yDateVal);
            filterCriteria.clear();
            filterCriteria.add(yTxnIDs);
            yPettyBuildLog.info("If Date not Empty -> " + filterCriteria);
        }

        if ((yDebitDt.equals("")) && (!yTxnID.equals(""))) {
            yTxnIDs.setFieldname("@ID");
            yTxnIDs.setOperand("EQ");
            yTxnIDs.setValue(yTxnID);
            filterCriteria.clear();
            filterCriteria.add(yTxnIDs);
            yPettyBuildLog.info("If Txn not Empty -> " + filterCriteria);
        }

        if ((yDebitDt.equals("")) && (yTxnID.equals(""))) {
            yTxnIDs.setFieldname("@ID");
            yTxnIDs.setOperand("EQ");
            yTxnIDs.setValue(yTodayID);
            filterCriteria.clear();
            filterCriteria.add(yTxnIDs);
            yPettyBuildLog.info("If BOTH is Empty -> " + filterCriteria);
        }

        return filterCriteria;
    }
}