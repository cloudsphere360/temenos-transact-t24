package com.temenos.fusion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.temenos.api.TDate;
import com.temenos.api.exceptions.T24CoreException;
import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.t24.api.records.dates.DatesRecord;
import com.temenos.t24.api.records.ebffcollpostingscreen.EbFfCollPostingScreenRecord;
import com.temenos.t24.api.records.ebffcollpostingscreen.EmployeeIdClass;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Date;
import com.temenos.t24.api.system.Session;

/**
 * @author jh116454
 * @Attached To: ENQUIRY > FF.RO.WAL.BRAN.RPT, ENQUIRY > FF.RO.WAL.DAY.RPT SS >
 *           NOFILE.FF.RO.WAL.DAY.RPT, SS > NOFILE.FF.RO.WAL.BRAN.RPT
 * @Attached As: Nofile Rtn > EB.API > E.NOFILE.FF.RO.WAL.BRANCH
 * @Description: Fetching the details RO Wallet Pending Collection in Branch
 *               Wise and Date WiseS
 * 
 *               11-08-26 - Since Date range is not working for RO Branch so
 *               changed to single date
 * 
 *               14-08-26 - RO.CASH.AMEND field changed to RO.CASH.COLLECTED for
 *               the field LOS cash collected - Branch wise report
 * 
 *               21-08-26 - RO.CASH.AMEND field for field LOS cash collected and RO.CASH.COLLECTED for Cash handled in vault -
 *               Branch HO wise report
 */

public class ENofileFfRoWalBranch extends Enquiry {

    private static final FusionFileLogger yRoWalBranLog = FusionFileLogger.getLogger(ENofileFfRoWalBranch.class);

    List<String> yROEmpArry = new ArrayList<>();

    DataAccess yDataAcc = new DataAccess();

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    Session sess = new Session(this);

    String yRoDate = "";
    String yBranchID = "";
    String yRoName = "";
    String yRoEmpID = "";
    int yDaysOutSt = 0;
    String yRoBranch = "";
    String yCollPostID = "";
    String yValueDt = "";
    String yPendVal = "";
    String yTodDt = "";
    String yBranch = "";

    String prevDate = "";
    String yDayOutDate = "";
    String yPrevDayOutID = "";
    String yActualDate = "";
    String yPrevDate = "";

    String yFromDate = "";
    String yEndDate = "";
    String yEnqBranch = "";
    String yEodIds = "";
    String yRecIds = "";
    String yDay = "";
    String yDayType = "";
    String yTotROCashCollected = "";
    String yTotROCashAmend = "";
    String yTotDepositBCpoint = "";
    String yPrevpendColl = "";
    String yTotPendingColl = "";

    EbFfCollPostingScreenRecord yEbCollPostRec = new EbFfCollPostingScreenRecord();

    DateTimeFormatter yFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    LocalDate yFormDate = null;

    List<String> yAllDs = new ArrayList<>();
    String yEnqName = "";

    LocalDate yFromDateF = null;
    LocalDate yEndDateF = null;
    LocalDate yLastMonthDate = null;
    LocalDate yTodayDtF = null;
    LocalDate yValueDtF = null;
    LocalDate yActDateF = null;

    String yCoCode = "";
    List<EmployeeIdClass> yEmpIDList = null;
    int j = 0;

    BigDecimal yROPendCashB = BigDecimal.ZERO;
    BigDecimal yTotPrevPendCollB = BigDecimal.ZERO;
    BigDecimal yCummAmt = BigDecimal.ZERO;
    BigDecimal yTodayPendingColl = BigDecimal.ZERO;

    String yROPrependVal = "";
    String yDaysOutS = "";
    String yCurrID = "";
    String yEmpID = "";
    String yEmpIDPrevDay = "";

    EbFfCollPostingScreenRecord yEbColPostPrevRec = null;
    BigDecimal yTotPendCashB = BigDecimal.ZERO;
    BigDecimal yPrevpendAmtB = BigDecimal.ZERO;
    List<FilterCriteria> yCriteria = null;

    @Override
    public List<String> setIds(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {

        yRoWalBranLog.info(" ------- STARTS -------");

        yEnqName = enquiryContext.getEnquiryId();
        yRoWalBranLog.info(" Enquiry Name - > " + yEnqName);

        Date yDate = new Date(this);
        DatesRecord yDateRec = yDate.getDates();
        yTodDt = yDateRec.getToday().getValue();

        yTodayDtF = LocalDate.parse(yTodDt, formatter);

        try {
            for (FilterCriteria criteria : filterCriteria) {
                if (criteria.getFieldname().equals("DATE.TIME")) {
                    yFromDate = criteria.getValue();
                    yRoWalBranLog.info(" From Date -> " + yFromDate);
                }

                if (criteria.getFieldname().equals("EOD.DATE")) {
                    yEndDate = criteria.getValue();
                    yRoWalBranLog.info(" End Date -> " + yEndDate);
                }

                if (yEnqName.equals("FF.RO.WAL.DAY.RPT") && criteria.getFieldname().equals("CO.CODE")) {
                    yEnqBranch = criteria.getValue();
                    yRoWalBranLog.info(" Branch -> " + yEnqBranch);
                }
            }

        } catch (Exception e) {
            yRoWalBranLog.info("Selction Missing -> " + e.getMessage());
        }

        if (yEnqName.equals("FF.RO.WAL.BRAN.RPT")) {
            yRoWalBranLog.info(" Branch Flow ---");
            getSelnBranchFlow();
        }

        if (yEnqName.equals("FF.RO.WAL.DAY.RPT")) {
            yRoWalBranLog.info(" Day Flow ---");
            getSelnDayFlow();
        }

        yRoWalBranLog.info(" ------- ENDS 10/06/26 13.23-------");

        return yROEmpArry;
    }

    private void getSelnBranchFlow() {
        if (yFromDate.isEmpty() && yEndDate.isEmpty()) {
            yRoWalBranLog.info("------ All Null----");
            throw new T24CoreException("", "EB-FRM.DATE.MISS.ERR");
        } else if (yEndDate.isEmpty()) {
            yRoWalBranLog.info("------ Branch - From ----");
            throw new T24CoreException("", "EB-END.DATE.MISS.ERR");
        } else {
            yRoWalBranLog.info("------ Branch - From & To----");
            getBranchFromandToDateWise();
        }
    }

    private void getSelnDayFlow() {

        if (yFromDate.isEmpty()) {
            yRoWalBranLog.info("----- From Empty----");
            throw new T24CoreException("", "EB-FRM.DATE.MISS.ERR");
        } else if (!yFromDate.isEmpty()) {
            yRoWalBranLog.info("-----Day - From& to  date----");
            getDayDateWise();
        }
    }

    private void getDayDateWise() {

        yFromDateF = LocalDate.parse(yFromDate, formatter);
        yRoWalBranLog.info(" yFromDateF -> " + yFromDateF);

        if (yFromDateF.isAfter(yTodayDtF)) {
            throw new T24CoreException("", "EB-ENQ.END.DATE");
        }

        try {

            if (!yEnqBranch.equals("")) {

                yEodIds = yEnqBranch + "-" + yFromDate;
                yRoWalBranLog.info(" Seln Branch -> " + yEodIds);
                getCollScrnRec();

                yRoWalBranLog.info(" yROEmpArry inside  getDayDateWise -> " + yROEmpArry);

            } else {
                yRoWalBranLog.info(" Seln Branch without Company");
                yAllDs = yDataAcc.selectRecords("", "EB.FF.COLL.POSTING.SCREEN", "", " WITH @ID LIKE ..." + yFromDate);
                yRoWalBranLog.info(" Select Cmd FOR Both -> " + yAllDs);
                yRoWalBranLog.info(" Branch Count -> " + yAllDs.size());

                for (String yCompID : yAllDs) {
                    yRoWalBranLog.info(" yCompID -> " + yCompID);
                    yEodIds = yCompID;
                    yRoWalBranLog.info(" Branch yEodIds -> " + yEodIds);
                    getCollScrnRec();
                    yRoWalBranLog.info(" Array inside All Branch -> " + yROEmpArry);

                    yRoWalBranLog.info("----Nxt Company ID----");
                }
            }

        } catch (Exception e) {
            yRoWalBranLog.info(" getFrmToDateDetails Missing -> " + e);
        }

    }

    private void getBranchFromandToDateWise() {
        yFromDateF = LocalDate.parse(yFromDate, formatter);
        yRoWalBranLog.info(" yFromDateF -> " + yFromDateF);

        yLastMonthDate = yFromDateF.minusMonths(1);
        yRoWalBranLog.info(" yLastMonthDate -> " + yLastMonthDate);

        yEndDateF = LocalDate.parse(yEndDate, formatter);
        yRoWalBranLog.info(" yEndDateF -> " + yEndDateF);

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

                getCollScrnRec();
                yRoWalBranLog.info(" yROEmpArry inside getFrmToDateDetails() -> " + yROEmpArry);
                yFromDateF = yFromDateF.plusDays(1);
                yRoWalBranLog.info(" Next Day -> " + yFromDateF);
            }
        } catch (Exception e) {
            yRoWalBranLog.info(" getFrmToDateDetails Missing -> " + e);
        }
    }

    private void getCollScrnRec() {

        yRoWalBranLog.info(" Collection Post ID -> " + yEodIds);
        try {
            yEbCollPostRec = new EbFfCollPostingScreenRecord(yDataAcc.getRecord("EB.FF.COLL.POSTING.SCREEN", yEodIds));

            yEmpIDList = yEbCollPostRec.getEmployeeId();
            for (j = 0; j < yEmpIDList.size(); j++) {
                yEmpID = yEmpIDList.get(j).getEmployeeId().getValue();
                yRoWalBranLog.info(" yEmpID -> " + yEmpID);
                yRoName = yEmpID.split("/")[1];
                yRoWalBranLog.info(" yRoName -> " + yRoName);
                yRoEmpID = yEmpID.split("/")[0];
                yRoWalBranLog.info(" yRoEmpID -> " + yRoEmpID);
                yTotROCashCollected = yEmpIDList.get(j).getRoCashCollected().getValue();
                yRoWalBranLog.info(" yTotROCashCollected LOS -> " + yTotROCashCollected);
                yTotROCashAmend = yEmpIDList.get(j).getRoCashAmend().getValue();
                yRoWalBranLog.info(" yTotROCashAmend LOS-> " + yTotROCashAmend);
                yTotDepositBCpoint = yEmpIDList.get(j).getDepositAtBcpoint().getValue();
                yRoWalBranLog.info(" yTotDepositBCpoint -> " + yTotDepositBCpoint);

                getPendingColl();
                getROPrevPendingColl();

                yROEmpArry.add(yEodIds + "*" + yRoName + "*" + yRoEmpID + "*" + yTotROCashCollected + "*"
                        + yTotDepositBCpoint + "*" + yPrevpendColl + "*" + yTodayPendingColl + "*" + yCummAmt + "*"
                        + yTotPendingColl + "*" + yDaysOutS + "*" + yDay + "*" + yTotROCashAmend);
            }
        } catch (Exception e) {
            yRoWalBranLog.info("Coll Posting Screen Rec Missing -> " + e.getMessage());
        }
    }

    private void getPendingColl() {
        yRoWalBranLog.info(" ---getPendingColl---");
        try {
            yPrevpendColl = yEmpIDList.get(j).getPrevPendingCollection().getValue();
            yRoWalBranLog.info(" yPrevpendColl -> " + yPrevpendColl);
            yTotPrevPendCollB = new BigDecimal(yPrevpendColl);

        } catch (Exception e) {
            yTotPrevPendCollB = BigDecimal.ZERO;
            yRoWalBranLog.info("prev Pend Missing");
        }

        try {
            yTotPendingColl = yEmpIDList.get(j).getRoPendingCollection().getValue();
            yRoWalBranLog.info(" yTotPendingColl -> " + yTotPendingColl);
            yROPendCashB = new BigDecimal(yTotPendingColl);
        } catch (Exception e) {
            yROPendCashB = BigDecimal.ZERO;
            yRoWalBranLog.info("Ro Pend Missing");
        }

        yTodayPendingColl = yROPendCashB.subtract(yTotPrevPendCollB).abs();
        yRoWalBranLog.info(" yToTalPendingColl -> " + yTodayPendingColl);

        yCummAmt = yROPendCashB.abs();
        yRoWalBranLog.info(" yCummAmt -> " + yCummAmt);
    }

    private void getROPrevPendingColl() {
        yRoWalBranLog.info(" ---yDaysOutSt clear---");

        yRoWalBranLog.info(" yEmpID -> " + yEmpID);
        yDaysOutSt = 0;
        yDaysOutS = "0";

        String[] ySpilt = yEodIds.split("-");
        yBranch = ySpilt[0]; // IN0011007
        yValueDt = ySpilt[1];

        yValueDtF = LocalDate.parse(yValueDt, yFormatter);
        yRoWalBranLog.info(" yValueDtF -> " + yValueDtF);

        yDay = yValueDtF.getDayOfWeek().toString();
        yRoWalBranLog.info(" yDay -> " + yDay);

        try {
            getDaysOutstanding();

            yRoWalBranLog.info(" yDaysOut FinalS Int -> " + yDaysOutSt);
            yRoWalBranLog.info(" yDaysOut FinalS  -> " + yDaysOutS);
        } catch (Exception e) {
            yDaysOutS = "0";
            yRoWalBranLog.info("Day Out Missing -> " + e);
        }
    }

    private void getDaysOutstanding() {
        yDaysOutSt = 0;
        yRoWalBranLog.info(" yDaysOut Int  -> " + yDaysOutSt);

        yDaysOutS = "";
        yRoWalBranLog.info(" yDaysOut String  -> " + yDaysOutS);
        for (int z = 1; z <= 20; z++) {

            String yPrevDay = yValueDtF.minusDays(z).format(yFormatter);
            yRoWalBranLog.info(" yPrevDay  -> " + yPrevDay);
            yRoWalBranLog.info(" z  -> " + z);
            yPrevDate = yPrevDay.replace("-", "");
            yPrevDayOutID = yBranch + "-" + yPrevDate;
            yRoWalBranLog.info(" yPrevDayOutID  -> " + yPrevDayOutID);
            getBranchHoliday();
            if (yDayType.equals("WORKING_DAY") && z <= 4) {

                getPrevPendingCollDetails();

                if (yPrevpendAmtB.compareTo(BigDecimal.ZERO) > 0) {
                    yDaysOutSt++;
                    yDaysOutS = String.valueOf(yDaysOutSt);
                    yRoWalBranLog.info(" yDaysOutString  -> " + yDaysOutSt);
                }

                if (yDaysOutSt > 3) {
                    yDaysOutS = "More than 3 days";
                    yRoWalBranLog.info(" yDaysOutString more than 3 days -> " + yDaysOutSt);
                    break;
                }
                if (yDaysOutSt == 0) {
                    yDaysOutS = "0";
                    yRoWalBranLog.info(" yDaysOutString  0 -> " + yDaysOutSt);
                    break;
                }
            }

        }

    }

    private void getPrevPendingCollDetails() {

        try {
            yEbColPostPrevRec = new EbFfCollPostingScreenRecord(
                    yDataAcc.getRecord("EB.FF.COLL.POSTING.SCREEN", yPrevDayOutID));
            for (int k = 0; k < yEmpIDList.size(); k++) {
                yEmpIDPrevDay = yEmpIDList.get(k).getEmployeeId().getValue();
                yRoWalBranLog.info(" yEmpIDPrevDay -> " + yEmpIDPrevDay);
                if (yEmpIDPrevDay.equals(yEmpID)) {
                    yROPrependVal = yEbColPostPrevRec.getEmployeeId().get(k).getPrevPendingCollection().getValue();
                    // yROPrependVal =
                    // yEbColPostPrevRec.getEmployeeId().get(k).getRoCashAmend().getValue();
                    yRoWalBranLog.info(" yROPrependVal -> " + yROPrependVal);
                    yPrevpendAmtB = new BigDecimal(yROPrependVal);
                    yRoWalBranLog.info(" yROPrependVal for each Emp ID Big -> " + yPrevpendAmtB);
                }
            }
        } catch (Exception e) {
            yPrevpendAmtB = BigDecimal.ZERO;
            yRoWalBranLog.info("  yROPrependVal B-> " + yROPrependVal);
            yRoWalBranLog.info("Prev Pending Coll Missing -> " + e);
        }

    }

    private void getBranchHoliday() {

        yRoWalBranLog.info("Inside method checkBranchHoliday");

        String dayTyp = "";

        try {

            com.temenos.t24.api.system.Date t24Date = new com.temenos.t24.api.system.Date(this);

            TDate futValDate = new TDate(yPrevDate);
            yRoWalBranLog.info(" futValDate -> " + futValDate);

            dayTyp = t24Date.getDayType(futValDate);
            yDayType = String.valueOf(dayTyp);
            yRoWalBranLog.info(" dayTyp -> " + dayTyp);

        } catch (Exception e) {

            yRoWalBranLog.error("Exception in checkBranchHoliday " + e.getMessage());
        }

    }
}
