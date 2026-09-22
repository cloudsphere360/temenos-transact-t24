package com.temenos.fusion;

import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.dates.DatesRecord;
import com.temenos.t24.api.records.ebffftpettycashlimit.DateOfTransactionClass;
import com.temenos.t24.api.records.ebffftpettycashlimit.EbFfFtPettyCashLimitRecord;
import com.temenos.t24.api.records.ebffpettycashroupld.EbFfPettyCashRoUpldRecord;
import com.temenos.t24.api.records.ebffpettycashroupld.FfRoExpTypeClass;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Date;
import com.temenos.t24.api.system.Session;
import com.temenos.t24.api.tables.ebffftpettycashlimit.EbFfFtPettyCashLimitTable;

/**
 * @author jh116454
 * @Attached To: VERSION > IM.DOCUMENT.UPLOAD,PETTY.CASH.IMG
 * @Attached As: Auth ROUTINE > EB.API > FF.PETTY.LIMIT.FT.UPD
 * @Description: To Update the all EXP values from EB.FF.PETTY.CASH.UPD to
 *               EB.FF.FT.PETTY.CASH.LIMIT.
 * 
 */

public class FfPettyLimitFtUpd extends RecordLifecycle {

    private static final FusionFileLogger yPettyLimitFtUpdLog = FusionFileLogger.getLogger(FfPettyLimitFtUpd.class);
    DataAccess da = new DataAccess(this);

    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {

        yPettyLimitFtUpdLog.info("!--- Petty Limit Details UPD Starts 04----!");

        String yTodDt = "";
        String yPettyCashID = "";
        String yDebitvalueDt = "";
        String yPrinStaExp = "";
        String yCourPost = "";
        String yStafWelExp = "";
        String yRepMainExp = "";
        String yConvExp = "";
        String yOffice = "";
        String ypettyOthr = "";
        String yPettyLimit = "";
        String yAvailAmt = "";
        String yPettyCashLimitID = "";
        String yExpType = "";
        String yCreditAmt = "";
        String yAvailLimit = "";

        EbFfFtPettyCashLimitRecord yEbPettyCashLimRec = null;

        Session session = new Session(this);
        String coCode = session.getCompanyId();
        Date yDate = new Date(this);
        DatesRecord yDateRec = yDate.getDates();
        yTodDt = yDateRec.getToday().getValue();

        String yYear = yTodDt.substring(0, 4);
        String yMnth = yTodDt.substring(4, 6);

        yPettyCashID = coCode + "-" + yTodDt;
        yPettyCashLimitID = coCode + "-" + yMnth + yYear;
        yPettyLimitFtUpdLog.info("yPettyCashLimitID -> " + yPettyCashLimitID);

        try {
            EbFfPettyCashRoUpldRecord yEbFfPettyCashDetRec = new EbFfPettyCashRoUpldRecord(
                    da.getRecord("EB.FF.PETTY.CASH.RO.UPLD", yPettyCashID));

            yCreditAmt = yEbFfPettyCashDetRec.getCreditAmt().getValue();
            yDebitvalueDt = yEbFfPettyCashDetRec.getDebitDate().getValue();

            List<FfRoExpTypeClass> yExpTypeList = yEbFfPettyCashDetRec.getFfRoExpType();

            for (int i = 0; i < yExpTypeList.size(); i++) {

                FfRoExpTypeClass yExpList = yExpTypeList.get(i);

                yExpType = yExpList.getFfRoExpType().getValue();
                String yExpAmt = yExpList.getFfRoExpAmt().getValue();

                yPettyLimitFtUpdLog.info("EXP Type -> " + yExpType);

                switch (yExpType) {

                case "Printing & Station Exp":
                    yPrinStaExp = yExpAmt;
                    yPettyLimitFtUpdLog.info("Printing & Station Exp - Amt -> " + yPrinStaExp);
                    break;

                case "Courier & Postage Exp":
                    yCourPost = yExpAmt;
                    yPettyLimitFtUpdLog.info("Courier & Postage Exp - Amt -> " + yCourPost);
                    break;

                case "Staff Welfare Exp":
                    yStafWelExp = yExpAmt;
                    yPettyLimitFtUpdLog.info("Staff Welfare Expense - Amt -> " + yStafWelExp);
                    break;

                case "Repair & Maintenance Exp":
                    yRepMainExp = yExpAmt;
                    yPettyLimitFtUpdLog.info("Repair & Maintenance Exp - Amt -> " + yRepMainExp);
                    break;

                case "Conveyance Exp":
                    yConvExp = yExpAmt;
                    yPettyLimitFtUpdLog.info("Conveyance Exp - Amt -> " + yConvExp);
                    break;

                case "Office Exp":
                    yOffice = yExpAmt;
                    yPettyLimitFtUpdLog.info("Office Exp - Amt -> " + yOffice);
                    break;

                case "Petty Other's Exp":
                    ypettyOthr = yExpAmt;
                    yPettyLimitFtUpdLog.info("Petty Other's Exp - Amt -> " + ypettyOthr);
                    break;

                default:
                    break;
                }
            }
        } catch (Exception e) {
            yPettyLimitFtUpdLog.info("EB.FF.PETTY.CASH.RO.UPLD - Rec Missing");
        }

        try {
            yEbPettyCashLimRec = new EbFfFtPettyCashLimitRecord(
                    da.getRecord("EB.FF.FT.PETTY.CASH.LIMIT", yPettyCashLimitID));
            yPettyLimitFtUpdLog.info("yEbPettyCashLimRec previous-> " + yEbPettyCashLimRec);
            yPettyLimit = yEbPettyCashLimRec.getPettyCashLimit().getValue();
            yAvailLimit = yEbPettyCashLimRec.getAvailableLimit().getValue();

            if (yPettyLimit != null && !yPettyLimit.isEmpty()) {

                double yPettyLimD = Double.parseDouble(yPettyLimit);
                double yDebAmtD = Double.parseDouble(yCreditAmt);

                if (yAvailLimit != null && !yAvailLimit.isEmpty()) {
                    double yOldAvailLimitD = Double.parseDouble(yAvailLimit);
                    double yAvailLimitD = yOldAvailLimitD - yDebAmtD;
                    yPettyLimitFtUpdLog.info("yAvailLimitD List not Empty-> " + yAvailLimitD);
                    yAvailAmt = String.valueOf(yAvailLimitD);
                } else {
                    double yAvailLimitD = yPettyLimD - yDebAmtD;
                    yPettyLimitFtUpdLog.info("yAvailLimitD -> " + yAvailLimitD);
                    yAvailAmt = String.valueOf(yAvailLimitD);
                }

                DateOfTransactionClass yDateTxnClassN = new DateOfTransactionClass();

                yDateTxnClassN.setDateOfTransaction(yDebitvalueDt);
                yDateTxnClassN.setTotalTxnAmount(yCreditAmt);
                yDateTxnClassN.setPrintStationeryExp(yPrinStaExp);
                yDateTxnClassN.setCourierPostageExp(yCourPost);
                yDateTxnClassN.setStaffWelfareExp(yStafWelExp);
                yDateTxnClassN.setRepairMaintenanceExp(yRepMainExp);
                yDateTxnClassN.setConveyanceExp(yConvExp); // corrected
                yDateTxnClassN.setOfficeExp(yOffice);
                yDateTxnClassN.setOthersExp(ypettyOthr);

                yEbPettyCashLimRec.insertDateOfTransaction(yDateTxnClassN, 0);
                yPettyLimitFtUpdLog.info("Final Array Lim Rec -> " + yEbPettyCashLimRec);

                yPettyLimitFtUpdLog.info("Petty Limit ID ->" + yPettyCashLimitID);
                yEbPettyCashLimRec.setAvailableLimit(yAvailAmt);
                yPettyLimitFtUpdLog.info("Eb Petty Cash Limit Rec latest->" + yEbPettyCashLimRec);

            }
        } catch (Exception e) {
            yPettyLimitFtUpdLog.info("EB.FF.PETTY.CASH.RO.UPLD - Rec Missing");
        }

        try {

            EbFfFtPettyCashLimitTable yEbFfPettyCashLimitTable = new EbFfFtPettyCashLimitTable(this);
            yEbFfPettyCashLimitTable.write(yPettyCashLimitID, yEbPettyCashLimRec);

        } catch (Exception e) {
            yPettyLimitFtUpdLog.info("Petty Limit missing");
        }

        yPettyLimitFtUpdLog.info("!--- Petty Limit Details UPD Ends 04----!");
    }

}
