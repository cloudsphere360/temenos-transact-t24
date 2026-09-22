package com.temenos.fusion;

import java.util.ArrayList;
import java.util.List;

import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.t24.api.records.company.CompanyRecord;
import com.temenos.t24.api.records.ebffftpettycashlimit.EbFfFtPettyCashLimitRecord;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/**
 * -----------------------------------------------------------------------------
 * Modification History :
 * -----------------------------------------------------------------------------
 * Description : This Routine used to Display the Petty Cash Withdrawal Report,
 * which displays the withdraw transaction details in both live and matured
 * status for all branches
 *
 * Developed By : Harshini Sakthivel
 *
 * Development Reference : Teller Report
 *
 * Attached To : EB.API - FF.FT.PETTY.CASH.WITHDRAW.RPT; STANDARD.SELECTION -
 * NOFILE.FF.FT.VIEW.WITHDRAW ; ENQUIRY - FF.FT.VIEW.WITHDRAW
 * 
 * Attached As : Nofile Routine
 * 
 * -----------------------------------------------------------------------------
 */

public class FfNofilePettyCashWithdrawRpt extends Enquiry {
    private static final FusionFileLogger pettyCashWithdrawRpt = FusionFileLogger
            .getLogger(FfNofilePettyCashWithdrawRpt.class);

    DataAccess da = new DataAccess(this);
    Session ss = new Session(this);

    List<String> withdrawFtRecList = new ArrayList<>();
    List<String> finalArray = new ArrayList<>();

    String finMnemonic = "";
    String txnId = "";
    String selDate = "";

    String cusMnemonic = "";

    @Override
    public List<String> setIds(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {
        pettyCashWithdrawRpt.info("FfNofilePettyCashWithdrawRpt is triggered");
        try {
            getSelectionFilterCriteriaValue(filterCriteria);
            pettyCashWithdrawRpt.info("txnId" + txnId + "**" + selDate);

            if ((!txnId.isEmpty())) {
                getFtWithDrawTxnDetails(txnId);
                pettyCashWithdrawRpt.info("txnId if class" + txnId + "**" + withdrawFtRecList);

            } else {
                String todayDate = ss.getCurrentVariable("!TODAY");
                String month = todayDate.substring(4, 6);
                String year = todayDate.substring(0, 4);
                String mmyr = month + year;
                pettyCashWithdrawRpt.info("mmyr" + mmyr);

                List<String> companyIdList = da.selectRecords("", "COMPANY", "", "");
                pettyCashWithdrawRpt.info("companyIdList" + companyIdList);
                for (String companyId : companyIdList) {
                    txnId = "";
                    initialiseCompanyInfo(companyId);
                    pettyCashWithdrawRpt.info("companyId" + companyId);

                    String idConcat = companyId + "-" + mmyr;
                    pettyCashWithdrawRpt.info("idConcat" + idConcat);

                    EbFfFtPettyCashLimitRecord pettyCashLimitRec = getPettyCashRecord(idConcat);

                    if (pettyCashLimitRec != null) {
                        pettyCashWithdrawRpt.info("pettyCashLimitRec" + pettyCashLimitRec.toString());
                        txnId = pettyCashLimitRec.getWithdrawTransaction().getValue();
                        getFtWithDrawTxnDetails(txnId);
                    }

                }
            }

        } catch (Exception e) {
            pettyCashWithdrawRpt.error("FfNofilePettyCashWithdrawRpt Error" + e);
        }
        return finalArray;
    }

    /**
     * @param idConcat
     * @return
     */
    private EbFfFtPettyCashLimitRecord getPettyCashRecord(String idConcat) {

        EbFfFtPettyCashLimitRecord pettyCashLimitRec = null;
        try {
            pettyCashLimitRec = new EbFfFtPettyCashLimitRecord(
                    da.getRecord(cusMnemonic, "EB.FF.FT.PETTY.CASH.LIMIT", "", idConcat));
        } catch (Exception e) {
            pettyCashWithdrawRpt.error("EbFfFtPettyCashLimitRecord Error" + e);
        }
        return pettyCashLimitRec;
    }

    /**
     * @param txnId2
     */
    private void getFtWithDrawTxnDetails(String txnId) {
        try {
            FundsTransferRecord ftRec = getFtRecord(txnId);

            if ((ftRec != null) && (!ftRec.toString().isEmpty())) {

                String creditAccNum = ftRec.getCreditAcctNo().getValue();
                String debitAmt = ftRec.getDebitAmount().getValue();
                String inputter = ftRec.getInputter(0).toString();
                String authoriser = ftRec.getAuthoriser().toString();
                String creditVauedate = ftRec.getCreditValueDate().getValue();
                pettyCashWithdrawRpt.info("Ft Details " + creditAccNum + "**" + debitAmt + "**" + inputter + "**"
                        + authoriser + "**" + creditVauedate);

                List<String> row = new ArrayList<>(); 
                row.add(txnId);
                row.add(creditAccNum);
                row.add(debitAmt);
                row.add(inputter);
                row.add(authoriser);
                row.add(creditVauedate);

                finalArray.add(String.join("*", row));
                pettyCashWithdrawRpt.info("finalArray " + finalArray);
            }

        } catch (Exception e) {
            pettyCashWithdrawRpt.error("getFtWithDrawTxnDetails Error" + e);
        }

    }

    /**
     * @param txnId2
     * @return
     */
    private FundsTransferRecord getFtRecord(String txnId) {

        pettyCashWithdrawRpt.info("txnId" + txnId);
        FundsTransferRecord ftRec = null;
        try {
            ftRec = new FundsTransferRecord(da.getRecord(finMnemonic, "FUNDS.TRANSFER", "", txnId));
            pettyCashWithdrawRpt.info("ftRecLive" + ftRec.toString());
        } catch (Exception e) {
            try {
                ftRec = new FundsTransferRecord(da.getRecord(finMnemonic, "FUNDS.TRANSFER", "$HIS", txnId + ";1"));
                pettyCashWithdrawRpt.info("ftRecHis" + ftRec.toString());
            } catch (Exception e1) {
                pettyCashWithdrawRpt.error("ftRecHis Error" + e1);
            }
        }
        return ftRec;
    }

    /**
     * @param filterCriteria
     */
    private void getSelectionFilterCriteriaValue(List<FilterCriteria> filterCriteria) {
        pettyCashWithdrawRpt.info("getSelectionFilterCriteriaValue is triggered");
        try {
            for (FilterCriteria filter : filterCriteria) {
                String value = filter.getValue();
                if (value == null || value.isEmpty())
                    continue;

                switch (filter.getFieldname()) {
                case "TRANSACTION.ID":
                    txnId = value;
                    break;
                case "CREDIT.VALUE.DATE":
                    selDate = value;
                    break;
                default:
                    break;
                }
                pettyCashWithdrawRpt.info("getSelectionFilterCriteriaValue " + txnId + "**" + selDate);
            }
        } catch (Exception e) {
            pettyCashWithdrawRpt.error("getSelectionFilterCriteriaValue Error" + e);
        }

    }

    /**
     * @param companyId
     */
    private void initialiseCompanyInfo(String companyId) {
        try {
            CompanyRecord companyObj = new CompanyRecord(da.getRecord("COMPANY", companyId));
            finMnemonic = companyObj.getFinancialMne().getValue();
            cusMnemonic = companyObj.getCustomerMnemonic().getValue();

        } catch (Exception e) {
            pettyCashWithdrawRpt.error("initialiseCompanyInfo Error" + e);
        }
    }

}
