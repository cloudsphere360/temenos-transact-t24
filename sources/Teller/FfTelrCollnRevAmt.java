package com.temenos.fusion;

import java.math.BigDecimal;
import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.dates.DatesRecord;
import com.temenos.t24.api.records.ebffrocollconcat.EbFfRoCollConcatRecord;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Date;
import com.temenos.t24.api.system.Session;
import com.temenos.t24.api.tables.ebffrocollconcat.EbFfRoCollConcatTable;

/**
 * @author jh116454
 * @Attached To: VERSION > FUNDS.TRANSFER,EM.FF.LOAN.COLLECTION.REV.API.1.0.0
 * @Attached As: AUTH Routine > EB.API > FF.TELR.COLLN.REV.AMT
 * @Description: fetching the amount reversed from the FT and updating it in
 *               EB.FF.RO.COLL.CONCAT table when paymt type is cash, upi and
 *               bbps. the difference in the amount should be updted
 * 
 * 
 */

public class FfTelrCollnRevAmt extends RecordLifecycle {

    private static final FusionFileLogger yTelCollRevAmtLog = FusionFileLogger.getLogger(FfTelrCollnRevAmt.class);

    DataAccess yDataAcc = new DataAccess(this);
    BigDecimal yRevAmtB = BigDecimal.ZERO;
    BigDecimal yCashAmtF = BigDecimal.ZERO;
    BigDecimal yBbpsAmtF = BigDecimal.ZERO;
    BigDecimal yTotAmtB = BigDecimal.ZERO;
    BigDecimal yCashAmtB = BigDecimal.ZERO;
    BigDecimal yBbpsAmtB = BigDecimal.ZERO;
    
    String yRoEmpID = "";
    String yCollContID = "";
    String yPymtMode = "";
    String yRevAmt = "";
    String yCashAmt = "";
    String yBbpsAmt = "";
    
   
    EbFfRoCollConcatRecord yEbFfCollContRec = null;
    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {
        yTelCollRevAmtLog.info("---------- starts---------------");

        String yFtID = currentRecordId;

        Session ySession = new Session(this);
        String yCocode = ySession.getCompanyId();
        yTelCollRevAmtLog.info(" Co code -> " + yCocode);

        Date yDate = new Date(this);
        DatesRecord yDateRec = yDate.getDates();
        String yTodDt = yDateRec.getToday().getValue();
        yTelCollRevAmtLog.info(" Tod Dt -> " + yTodDt);

        try {
            FundsTransferRecord yFundTransRec = new FundsTransferRecord(yDataAcc.getRecord("FUNDS.TRANSFER", yFtID));

            yRoEmpID = yFundTransRec.getLocalRefField("FF.POSTED.BY").getValue();
            yTelCollRevAmtLog.info(" Ro EmpID -> " + yRoEmpID);
            yCollContID = yRoEmpID + "-" + yCocode + "-" + yTodDt;
            yTelCollRevAmtLog.info(" Coll ContID -> " + yCollContID);
            yPymtMode = yFundTransRec.getLocalRefField("FF.PYMT.MODE").getValue();
            yTelCollRevAmtLog.info(" Pymt Mode-> " + yPymtMode);

            yRevAmt = yFundTransRec.getDebitAmount().getValue();
            if (yRevAmt.equals("")) {
                yRevAmt = yFundTransRec.getCreditAmount().getValue();
            }
            yRevAmtB = new BigDecimal(yRevAmt);
            yTelCollRevAmtLog.info(" Rev AmtB -> " + yRevAmtB);
        } catch (Exception e) {
            yTelCollRevAmtLog.info("Rev Amt Missing");
        }

        try {
            yEbFfCollContRec = new EbFfRoCollConcatRecord(
                    yDataAcc.getRecord("EB.FF.RO.COLL.CONCAT", yCollContID));
            EbFfRoCollConcatTable yEbFfCollContTable = new EbFfRoCollConcatTable(this);

            getAmount();           

            switch (yPymtMode) {

            case "CASH":
                yCashAmtF = yCashAmtB.subtract(yRevAmtB);
                yCashAmt = String.valueOf(yCashAmtF);

                yTotAmtB = yCashAmtF.add(yBbpsAmtB);
                yTelCollRevAmtLog.info(" yTotAmtB -> " + yTotAmtB);
                yEbFfCollContRec.getCashCollected().setValue(yCashAmt);
                break;

            case "BBPS":
            case "UPI":

                yBbpsAmtF = yBbpsAmtB.subtract(yRevAmtB);
                yBbpsAmt = String.valueOf(yBbpsAmtF);

                yTotAmtB = yBbpsAmtF.add(yCashAmtB);
                yTelCollRevAmtLog.info(" yTotAmtB -> " + yTotAmtB);
                yEbFfCollContRec.getDigitalCollected().set(yBbpsAmt);
                break;

            default:
                break;
            }          
           
            yTelCollRevAmtLog.info(" yTotAmtB -> " + yTotAmtB);
            String yTotval = String.valueOf(yTotAmtB);

            double ytotAmtD = Math.abs(Double.valueOf(yTotval));

            yTotval = String.valueOf(ytotAmtD);
            yTelCollRevAmtLog.info(" yTotval -> " + yTotval);

            yEbFfCollContRec.getTotalCollected().setValue(yTotval);

            yEbFfCollContTable.write(yCollContID, yEbFfCollContRec);
            yTelCollRevAmtLog.info(" File written");
        } catch (Exception e) {
            yTelCollRevAmtLog.info("Rec Missing");
        }
        yTelCollRevAmtLog.info("---------- Ends---------------");
    }
    private void getAmount() {

        try {
            yCashAmt = yEbFfCollContRec.getCashCollected().getValue();
            yTelCollRevAmtLog.info(" yCashAmt -> " + yCashAmt);
           yCashAmtB = new BigDecimal(yCashAmt);
        } catch (Exception e) {
            yTelCollRevAmtLog.info("cash Amt Missing");
        }
        
        try {
            yBbpsAmt = yEbFfCollContRec.getDigitalCollected().getValue();
            yTelCollRevAmtLog.info(" yBbpsAmt -> " + yBbpsAmt);
            yBbpsAmtB = new BigDecimal(yBbpsAmt);
        } catch (Exception e) {
            yTelCollRevAmtLog.info("Digi Amt Missing");
        }
        yTelCollRevAmtLog.info(" yCashAmt -> " + yCashAmt);
        yTelCollRevAmtLog.info(" yBbpsAmt -> " + yBbpsAmt);
        
    }
}
