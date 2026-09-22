package com.temenos.fusion;

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
import com.temenos.t24.api.system.Session;

/**
 * @author jh116454
 * @Attached To: ENQUIRY > FF.RO.WAL.DAY.RPT ENQUIRY >
 *           FF.BRANCH.CASH.IN.OUT.FLOW.RPT ENQUIRY >
 *           FF.EOD.STATUS.AUTO.EOD.TRACKING.RPT
 * @Attached As: Build Routine > EB.API > FF.PETTY.ENQ.BUILD.RTN
 * @Description: To Display records according to selection changed on 09-07-26
 * 
 */

public class FfPettyEnqBuildRtn extends Enquiry {

    private static final FusionFileLogger yPettyBuildEnqLog = FusionFileLogger.getLogger(FfPettyEnqBuildRtn.class);

    String yCoCode = "";
    String yRecIds = "";
    String yEodIds = "";
    String yFromDate = "";
    String yEndDate = "";
    String yEnqID = "";
    String yEnqName = "";

    Date yDate = new Date(this);

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    Session sess = new Session(this);

    FilterCriteria yFilCre = null;

    List<String> yAllDs = new ArrayList<>();

    LocalDate yFromDateF = null;
    LocalDate yEndDateF = null;
    LocalDate yLastMonthDate = null;
    LocalDate yTodayDtF = null;

    String yEnqDate = "";
    String yEnqBranch = "";

    String yEnqFieldName = "";
    String yEnqFieldValue = "";

    List<FilterCriteria> yCriteria = null;

    DataAccess yDataAccRec = new DataAccess(this);
    String ySelcmd = "";

    int yDateLen = 0;

    @Override
    public List<FilterCriteria> setFilterCriteria(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {

        yFilCre = new FilterCriteria();
        yPettyBuildEnqLog.info("------- Starts ------");

        DatesRecord yDateRec = yDate.getDates();
        String yTodayDt = yDateRec.getToday().getValue();

        yTodayDtF = LocalDate.parse(yTodayDt, formatter);
        yPettyBuildEnqLog.info(" yTodayDtF -> " + yTodayDtF);

        yEnqName = enquiryContext.getEnquiryId();
        yPettyBuildEnqLog.info(" yEnqName -> " + yEnqName);
        yCriteria = filterCriteria;

        getSelnValue();

        filterCriteria.clear();
        filterCriteria.add(yFilCre);

        yPettyBuildEnqLog.info("filterCriteria -> " + filterCriteria);
        yPettyBuildEnqLog.info("------- Ends ------");
        return filterCriteria;
    }

    private void getSelnValue() {
        try {

            yPettyBuildEnqLog.info(" criteria -> " + yCriteria);
            for (FilterCriteria criteria : yCriteria) {

                if (criteria.getFieldname().equals("DATE.TIME")) {
                    yFromDate = criteria.getValue();
                    yPettyBuildEnqLog.info(" From Date -> " + yFromDate);
                }

                if (criteria.getFieldname().equals("EOD.DATE")) {
                    yEndDate = criteria.getValue();
                    yPettyBuildEnqLog.info(" End Date -> " + yEndDate);
                }

                if (!yEnqName.equals("FF.BRANCH.CASH.IN.OUT.FLOW.RPT") && criteria.getFieldname().equals("CO.CODE")) {
                    yEnqBranch = criteria.getValue();
                    yPettyBuildEnqLog.info(" Branch -> " + yEnqBranch);
                }
            }
        } catch (Exception e) {
            yPettyBuildEnqLog.info("Selction Missing");
        }

        if (yEnqName.equals("FF.BRANCH.CASH.IN.OUT.FLOW.RPT")) {
            getSelnBranchFlow();
        }

        if (yEnqName.equals("FF.BRANCH.RECONCILIATION.HO.VIEW.RPT")
                || yEnqName.equals("FF.EOD.STATUS.AUTO.EOD.TRACKING.RPT")) {
            getSelnReconView();
        }

        yFilCre.setFieldname("@ID");
        yFilCre.setOperand("EQ");
        yFilCre.setValue(yRecIds);

    }

    private void getSelnBranchFlow() {
        yPettyBuildEnqLog.info(" Branch Null");

        if (yFromDate.isEmpty() && yEndDate.isEmpty()) {
            throw new T24CoreException("", "EB-FRM.DATE.MISS.ERR");
        } else if (yEndDate.isEmpty()) {
            throw new T24CoreException("", "EB-END.DATE.MISS.ERR");
        } else {
            yPettyBuildEnqLog.info("-----From & To Date ----");
            getFrmToDateDetails();
        }
    }

    private void getSelnReconView() {

        if (yFromDate.isEmpty() && yEndDate.isEmpty()) {
            throw new T24CoreException("", "EB-FRM.DATE.MISS.ERR");

        } else if (yEndDate.isEmpty() && yEnqBranch.isEmpty()) {
            yPettyBuildEnqLog.info("---- End Date & Branch -Empty----");

            throw new T24CoreException("", "EB-END.DATE.MISS.ERR");
        } else if (!yEndDate.isEmpty() && yEnqBranch.isEmpty()) {
            getFromandToDate();

        } else if (!yFromDate.isEmpty() && !yEndDate.isEmpty() && !yEnqBranch.isEmpty()) {
            getAllVal();

        }

    }

    private void getFrmToDateDetails() {

        yFromDateF = LocalDate.parse(yFromDate, formatter);
        yPettyBuildEnqLog.info(" yFromDateF -> " + yFromDateF);

        yLastMonthDate = yFromDateF.minusMonths(1);
        yPettyBuildEnqLog.info("yLastMonthDate -> " + yLastMonthDate);

        yEndDateF = LocalDate.parse(yEndDate, formatter);
        yPettyBuildEnqLog.info(" yEndDateF -> " + yEndDateF);

        if (yFromDateF.isAfter(yTodayDtF) || yEndDateF.isAfter(yTodayDtF)) {
            throw new T24CoreException("", "EB-ENQ.END.DATE");
        }

        if (yEndDateF.isBefore(yFromDateF)) {
            throw new T24CoreException("", "EB-ENQ.BFR.DATE.ERR");
        }
        
        if (yEndDateF.isBefore(yFromDateF) || yEndDateF.isBefore(yLastMonthDate)) {
            throw new T24CoreException("", "EB-ENQ.FROM.DATE");
        }

        try {
            while (!yFromDateF.isAfter(yEndDateF)) {
                yCoCode = sess.getCompanyId();
                yEodIds = yCoCode + "-" + yFromDateF.toString().replace("-", "");
                yAllDs.add(yEodIds);
                yFromDateF = yFromDateF.plusDays(1);
            }
            yRecIds = yAllDs.toString().replace("[", "").replace("]", "").replace(",", "");
            yPettyBuildEnqLog.info(" yRecIds -> " + yRecIds);
        } catch (Exception e) {
            yPettyBuildEnqLog.info(" getFrmToDateDetails Missing FOR Branch alone -> " + e);
        }
    }

    private void getFromandToDate() {

        yPettyBuildEnqLog.info("---- Branch -Empty FF.BRANCH.RECONCILIATION.HO.VIEW.RPT----");

        yFromDateF = LocalDate.parse(yFromDate, formatter);
        yPettyBuildEnqLog.info(" yFromDateF -> " + yFromDateF);

        yLastMonthDate = yFromDateF.minusMonths(1);
        yPettyBuildEnqLog.info("yLastMonthDate -> " + yLastMonthDate);

        yEndDateF = LocalDate.parse(yEndDate, formatter);
        yPettyBuildEnqLog.info(" yEndDateF -> " + yEndDateF);

        if (yFromDateF.isAfter(yTodayDtF) || yEndDateF.isAfter(yTodayDtF)) {
            throw new T24CoreException("", "EB-ENQ.END.DATE");
        }

        if (yEndDateF.isBefore(yFromDateF)) {
            throw new T24CoreException("", "EB-ENQ.BFR.DATE.ERR");
        }
        
        if (yEndDateF.isBefore(yFromDateF) || yEndDateF.isBefore(yLastMonthDate)) {
            throw new T24CoreException("", "EB-ENQ.FROM.DATE");
        }

        ySelcmd = " WITH EOD.DATE GE " + yFromDate + " AND EOD.DATE LE " + yEndDate;
        yAllDs = yDataAccRec.selectRecords("", "EB.FF.EOD.SCREEN", "", ySelcmd);
        yPettyBuildEnqLog.info(" Select Cmd FOR Both -> " + ySelcmd);

        yRecIds = yAllDs.toString().replace("[", "").replace("]", "").replace(",", "");
        yPettyBuildEnqLog.info(" yRecIds For From&To Date-> " + yRecIds);
    }

    private void getAllVal() {

        yPettyBuildEnqLog.info(" All 3 given FF.BRANCH.RECONCILIATION.HO.VIEW.RPT");

        yFromDateF = LocalDate.parse(yFromDate, formatter);
        yPettyBuildEnqLog.info(" yFromDateF -> " + yFromDateF);

        yLastMonthDate = yFromDateF.minusMonths(1);
        yPettyBuildEnqLog.info(" yLastMonthDate -> " + yLastMonthDate);
        
        yEndDateF = LocalDate.parse(yEndDate, formatter);
        yPettyBuildEnqLog.info(" yEndDateF -> " + yEndDateF);

        if (yFromDateF.isAfter(yTodayDtF) || yEndDateF.isAfter(yTodayDtF)) {
            throw new T24CoreException("", "EB-ENQ.END.DATE");
        }

        if (yEndDateF.isBefore(yFromDateF)) {
            throw new T24CoreException("", "EB-ENQ.BFR.DATE.ERR");
        }

        if (yEndDateF.isBefore(yLastMonthDate)) {
            throw new T24CoreException("", "EB-ENQ.FROM.DATE");
        }

        try {
            ySelcmd = " WITH CO.CODE EQ " + yEnqBranch + " AND WITH EOD.DATE GE " + yFromDate + " AND EOD.DATE LE "
                    + yEndDate;
            yAllDs = yDataAccRec.selectRecords("", "EB.FF.EOD.SCREEN", "", ySelcmd);
            yRecIds = yAllDs.toString().replace("[", "").replace("]", "").replace(",", "");

            yPettyBuildEnqLog.info(" Select Cmd Both Date n Branch -> " + ySelcmd);
            yPettyBuildEnqLog.info(" yAllIds FOR Both Date n Branch -> " + yRecIds);
        } catch (Exception e) {
            yPettyBuildEnqLog.info(" getFrmToDateDetails Missing -> " + e);
        }
    }

}